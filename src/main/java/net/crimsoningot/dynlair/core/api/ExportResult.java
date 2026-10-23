package net.crimsoningot.dynlair.core.api;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Outcome of a file export.
 *
 * @param file            the file that was written
 * @param sourcesExported number of sources that exported successfully
 * @param errors          number of entries in the output's {@code errors} array
 */
public record ExportResult(@NotNull Path file, int sourcesExported, int errors) {
}
