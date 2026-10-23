package net.crimsoningot.dynlair.core.api;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Public entry point of DynlairCore. Reachable from Bukkit's services manager:
 * <pre>{@code
 * DynlairApi api = Bukkit.getServicesManager().load(DynlairApi.class);
 * }</pre>
 * The service is registered in DynlairCore's {@code onEnable()}, so hook plugins
 * should declare {@code depend: [DynlairCore]} and use it from their own
 * {@code onEnable()}.
 */
public interface DynlairApi {

    /** Version of the exported JSON envelope. */
    int SCHEMA_VERSION = 1;

    /**
     * @throws IllegalArgumentException if the ID is malformed or already registered
     */
    void registerSource(@NotNull DocSource source);

    /** @return true if a source with that ID was registered and has been removed */
    boolean unregisterSource(@NotNull String id);

    @NotNull Optional<DocSource> getSource(@NotNull String id);

    /** @return registered sources, sorted by ID */
    @NotNull Collection<DocSource> getSources();

    /**
     * Run the given sources and build the export document in memory.
     * Must be called from the main thread.
     * Unknown IDs and failing sources are reported in the {@code errors} array.
     */
    @NotNull JsonObject export(@NotNull Collection<String> sourceIds);

    /** Convenience for {@link #export(Collection)} with every registered source. */
    @NotNull JsonObject exportAll();

    /**
     * Run the given sources on the main thread (this method must be called from
     * the main thread), then write the result to disk asynchronously according
     * to the plugin's {@code output.*} config.
     */
    @NotNull CompletableFuture<ExportResult> exportToFile(@NotNull Collection<String> sourceIds);
}
