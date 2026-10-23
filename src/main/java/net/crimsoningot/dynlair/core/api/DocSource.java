package net.crimsoningot.dynlair.core.api;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

/**
 * A provider of documentation data, e.g. one per supported plugin.
 * Hook plugins implement this and register it with {@link DynlairApi#registerSource(DocSource)}.
 */
public interface DocSource {

    /**
     * Unique identifier, used as the key in the exported JSON and in commands.
     * Must match {@code [a-z0-9][a-z0-9_-]*}.
     */
    @NotNull String getId();

    /** Human-readable name shown in listings. */
    @NotNull String getDisplayName();

    /**
     * Version of this source's own data format. Bump it when the shape of
     * {@link #export()} changes in a way consumers must care about.
     */
    default int getSchemaVersion() {
        return 1;
    }

    /**
     * Produce this source's documentation data.
     * <p>
     * Called on the server's main thread, so it is safe to touch Bukkit and
     * other plugin APIs. Keep it reasonably fast. Throwing an exception does
     * not abort the whole export; it is recorded in the output's
     * {@code errors} array and other sources still export.
     */
    @NotNull JsonObject export() throws Exception;
}
