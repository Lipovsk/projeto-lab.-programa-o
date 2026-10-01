package br.edu.unit.chemest.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;

/** Publish only complete files, keeping the previous destination on write failure. */
public final class SafeFileWriter {
    @FunctionalInterface
    public interface Content { void write(BufferedWriter writer) throws IOException; }

    private SafeFileWriter() {}

    public static void write(Path destination, boolean replace, Content content) throws IOException {
        Path target = destination.toAbsolutePath().normalize();
        if (Files.isDirectory(target)) throw new IOException("O destino deve ser um arquivo.");
        Path temporary = Files.createTempFile(target.getParent(), ".chemest-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary)) { content.write(writer); }
            if (replace) {
                try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException e) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
            } else {
                // No ATOMIC_MOVE here: its handling of an existing target is implementation-specific.
                Files.move(temporary, target);
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
