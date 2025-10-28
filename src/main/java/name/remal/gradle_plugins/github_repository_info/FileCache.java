package name.remal.gradle_plugins.github_repository_info;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.move;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.write;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Objects.requireNonNull;
import static name.remal.gradle_plugins.toolkit.PathUtils.normalizePath;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import lombok.SneakyThrows;

class FileCache {

    private final Path file;
    private final Path lockFile;

    public FileCache(Path file) {
        file = normalizePath(file);

        this.file = file;
        this.lockFile = file.resolveSibling(file.getFileName() + ".lock");
    }


    @SneakyThrows
    public byte[] getOrCreateContent(CacheFileContentCreator creator) {
        // 1. Fast Path: Try to read the file.
        try {
            return readAllBytes(file);
        } catch (NoSuchFileException ignored) {
            // File doesn't exist. Fall through to the creation logic.
        }

        // 2. Slow Path: File doesn't exist. We must acquire a lock to create it.
        var cacheDir = requireNonNull(file.getParent());
        createDirectories(cacheDir);
        try (
            var lockChannel = FileChannel.open(lockFile, CREATE, WRITE);
            var lock = lockChannel.lock()
        ) {
            if (!lock.isValid()) {
                throw new IllegalStateException("Invalid lock on file: " + lockFile);
            }

            // 3. Double-Check: Did another process create the file while we were waiting for the lock?
            try {
                return readAllBytes(file);
            } catch (NoSuchFileException ignored) {
                // File still doesn't exist. It is our job to create it.
            }

            // 4. Compute, Write to Temp, and Atomic Move
            var data = creator.execute();
            Path tempFile = null;
            try {
                // Write to a temp file in the same directory
                tempFile = createTempFile(cacheDir, file.getFileName() + "-", ".tmp");
                write(tempFile, data);

                // Atomically move the complete file into place
                move(tempFile, file, ATOMIC_MOVE);

                // We can return the data we just computed
                return data;

            } catch (IOException e) {
                // Clean up the temp file if anything went wrong
                if (tempFile != null) {
                    deleteIfExists(tempFile);
                }
                throw new IOException("Failed to create cache file", e);
            }
        }
    }

    public interface CacheFileContentCreator {
        byte[] execute() throws Throwable;
    }

}
