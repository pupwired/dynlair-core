package net.crimsoningot.dynlair.core;

import net.crimsoningot.dynlair.core.api.DocSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class DynlairCommand implements TabExecutor {

    private final DynlairCorePlugin plugin;
    private final DynlairService service;

    DynlairCommand(DynlairCorePlugin plugin, DynlairService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) return false; // prints usage from plugin.yml

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "sources" -> listSources(sender);
            case "export" -> export(sender, Arrays.copyOfRange(args, 1, args.length));
            default -> {
                return false;
            }
        }
        return true;
    }

    private void listSources(CommandSender sender) {
        List<DocSource> sources = new ArrayList<>(service.getSources());
        if (sources.isEmpty()) {
            send(sender, "No documentation sources registered. Install a hook plugin.", NamedTextColor.YELLOW);
            return;
        }
        send(sender, "Documentation sources (" + sources.size() + "):", NamedTextColor.GRAY);
        for (DocSource source : sources)
            send(sender, " - " + source.getId() + " (" + source.getDisplayName() + ")", NamedTextColor.WHITE);
    }

    private void export(CommandSender sender, String[] requested) {
        List<String> ids = requested.length > 0
                ? Arrays.asList(requested)
                : service.getSources().stream().map(DocSource::getId).toList();

        if (ids.isEmpty()) {
            send(sender, "No documentation sources registered. Install a hook plugin.", NamedTextColor.YELLOW);
            return;
        }

        send(sender, "Exporting " + ids.size() + " source(s)...", NamedTextColor.GRAY);
        service.exportToFile(ids).whenComplete((result, error) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (error != null) {
                        plugin.getLogger().log(java.util.logging.Level.SEVERE, "Export failed", error);
                        send(sender, "Export failed: " + error.getMessage(), NamedTextColor.RED);
                        return;
                    }
                    send(sender, "Exported " + result.sourcesExported() + " source(s) to " + result.file(), NamedTextColor.GREEN);
                    if (result.errors() > 0)
                        send(sender, result.errors() + " error(s) recorded in the file's \"errors\" array; see console.", NamedTextColor.YELLOW);
                }));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1)
            return filter(List.of("export", "sources"), args[0]);

        if (args.length >= 2 && args[0].equalsIgnoreCase("export")) {
            List<String> used = Arrays.asList(args).subList(1, args.length - 1);
            List<String> candidates = service.getSources().stream()
                    .map(DocSource::getId)
                    .filter(id -> !used.contains(id))
                    .toList();
            return filter(candidates, args[args.length - 1]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.startsWith(lower)).toList();
    }

    private static void send(CommandSender sender, String message, NamedTextColor color) {
        sender.sendMessage(Component.text("[Dynlair] ", NamedTextColor.DARK_AQUA)
                .append(Component.text(message, color)));
    }
}
