package net.crimsoningot.dynlair.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.crimsoningot.dynlair.core.api.DynlairApi;
import net.crimsoningot.dynlair.core.api.DocSource;
import net.crimsoningot.dynlair.core.api.ExportResult;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.regex.Pattern;

final class DynlairService implements DynlairApi {

    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9][a-z0-9_-]*");

    private final DynlairCorePlugin plugin;
    private final Map<String, DocSource> sources = new ConcurrentSkipListMap<>();

    DynlairService(DynlairCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void registerSource(@NotNull DocSource source) {
        String id = source.getId();
        if (!ID_PATTERN.matcher(id).matches())
            throw new IllegalArgumentException("Invalid source ID '" + id + "' (must match " + ID_PATTERN.pattern() + ")");
        if (sources.putIfAbsent(id, source) != null)
            throw new IllegalArgumentException("A documentation source with ID '" + id + "' is already registered");
        plugin.getLogger().info("Registered documentation source '" + id + "' (" + source.getDisplayName() + ")");
    }

    @Override
    public boolean unregisterSource(@NotNull String id) {
        boolean removed = sources.remove(id) != null;
        if (removed) plugin.getLogger().info("Unregistered documentation source '" + id + "'");
        return removed;
    }

    @Override
    public @NotNull Optional<DocSource> getSource(@NotNull String id) {
        return Optional.ofNullable(sources.get(id));
    }

    @Override
    public @NotNull Collection<DocSource> getSources() {
        return java.util.List.copyOf(sources.values());
    }

    @Override
    public @NotNull JsonObject exportAll() {
        return export(sources.keySet());
    }

    @Override
    public @NotNull JsonObject export(@NotNull Collection<String> sourceIds) {
        requireMainThread();

        JsonObject root = new JsonObject();
        root.addProperty("schema_version", SCHEMA_VERSION);
        root.addProperty("generated_at", Instant.now().toString());

        JsonObject server = new JsonObject();
        server.addProperty("software", Bukkit.getName());
        server.addProperty("minecraft_version", Bukkit.getMinecraftVersion());
        root.add("server", server);

        JsonObject sourcesJson = new JsonObject();
        JsonArray errors = new JsonArray();

        for (String id : new LinkedHashSet<>(sourceIds)) {
            DocSource source = sources.get(id);
            if (source == null) {
                errors.add(error(id, "No such source"));
                continue;
            }
            try {
                JsonObject section = new JsonObject();
                section.addProperty("display_name", source.getDisplayName());
                section.addProperty("schema_version", source.getSchemaVersion());
                section.add("data", source.export());
                sourcesJson.add(id, section);
            } catch (Exception | LinkageError ex) {
                // LinkageError covers a hook whose optional dependency vanished.
                plugin.getLogger().log(Level.SEVERE, "Documentation source '" + id + "' failed to export", ex);
                errors.add(error(id, ex.getClass().getSimpleName() + ": " + ex.getMessage()));
            }
        }

        root.add("sources", sourcesJson);
        root.add("errors", errors);
        return root;
    }

    @Override
    public @NotNull CompletableFuture<ExportResult> exportToFile(@NotNull Collection<String> sourceIds) {
        JsonObject json = export(sourceIds); // main thread, checked inside
        OutputSettings settings = OutputSettings.from(plugin);
        int exported = json.getAsJsonObject("sources").size();
        int errorCount = json.getAsJsonArray("errors").size();

        CompletableFuture<ExportResult> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                future.complete(new ExportResult(ExportWriter.write(json, settings), exported, errorCount));
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    private static JsonObject error(String sourceId, String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("source", sourceId);
        obj.addProperty("message", message);
        return obj;
    }

    private static void requireMainThread() {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Dynlair exports must be started from the main server thread");
    }
}
