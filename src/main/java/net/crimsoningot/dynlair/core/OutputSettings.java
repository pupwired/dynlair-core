package net.crimsoningot.dynlair.core;

import org.bukkit.configuration.file.FileConfiguration;

import java.nio.file.Path;

/** Snapshot of the {@code output.*} config, taken on the main thread. */
record OutputSettings(Path directory, boolean timestamped, boolean pretty) {

    static OutputSettings from(DynlairCorePlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        Path dir = plugin.getDataFolder().toPath()
                .resolve(config.getString("output.directory", "exports"))
                .normalize();
        return new OutputSettings(
                dir,
                config.getBoolean("output.timestamped-files", true),
                config.getBoolean("output.pretty-print", true));
    }
}
