package dev.rikka.tools.autoresconfig;

import static java.nio.file.Files.delete;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;

@CacheableTask
public abstract class GenerateTask extends DefaultTask {

    @Input
    public abstract ListProperty<String> getLocales();

    @Input
    public abstract ListProperty<String> getDisplayLocales();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void generate() throws IOException {
        getOutputDir().getAsFileTree().visit(
                new FileVisitor() {
                    @Override
                    public void visitDir(@NonNull FileVisitDetails dirDetails) {
                        try {
                            delete(dirDetails.getFile().toPath());
                        } catch (IOException ignored) {
                        }
                    }

                    @Override
                    public void visitFile(@NonNull FileVisitDetails fileDetails) {
                        try {
                            delete(fileDetails.getFile().toPath());
                        } catch (IOException ignored) {
                        }
                    }
                }
        );
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
