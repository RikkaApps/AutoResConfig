package dev.rikka.tools.autoresconfig;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;

public abstract class GenerateTask extends DefaultTask {

    protected final Collection<String> locales;
    protected final Collection<String> displayLocales;
    private final File dir;

    @Inject
    public GenerateTask(File dir, Collection<String> locales, Collection<String> displayLocales) {
        this.locales = locales;
        this.displayLocales = displayLocales;
        this.dir = dir;
    }

    @TaskAction
    public void generate() throws IOException {
        try {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
        }
    }

    public final void createFile(File file) throws IOException {
        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    throw new IOException("Failed to create " + file.getParentFile());
                }
            }
            if (!file.createNewFile()) {
                throw new IOException("Failed to create " + file);
            }
        }
    }
}

