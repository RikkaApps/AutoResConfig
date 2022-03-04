package dev.rikka.tools.autoresconfig;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;

public abstract class GenerateTask extends DefaultTask {

    protected final Collection<String> locales;
    protected final Collection<String> displayLocales;
    private final File file;

    @Inject
    public GenerateTask(File file, Collection<String> locales, Collection<String> displayLocales) {
        this.locales = locales;
        this.displayLocales = displayLocales;
        this.file = file;
    }

    @TaskAction
    public void writeToFile() throws IOException {
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

        var os = new PrintStream(file);
        onWrite(os);
        os.flush();
        os.close();
    }

    public abstract void onWrite(PrintStream os);
}

