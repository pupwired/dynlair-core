package net.crimsoningot.dynlair.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Writes export documents to disk. Safe to call from any thread. */
final class ExportWriter {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ExportWriter() {
    }

    static Path write(JsonObject json, OutputSettings settings) throws IOException {
        Files.createDirectories(settings.directory());

        String name = settings.timestamped()
                ? "dynlair-" + STAMP.format(LocalDateTime.now()) + ".json"
                : "dynlair.json";
        Path target = settings.directory().resolve(name);
        Path temp = settings.directory().resolve(name + ".tmp");

        GsonBuilder builder = new GsonBuilder().disableHtmlEscaping();
        if (settings.pretty()) builder.setPrettyPrinting();
        Gson gson = builder.create();

        // Write to a temp file first so readers never see a half-written export.
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            gson.toJson(json, writer);
        }
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }
}
