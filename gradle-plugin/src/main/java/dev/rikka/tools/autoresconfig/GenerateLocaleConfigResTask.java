package dev.rikka.tools.autoresconfig;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;

import java.io.IOException;
import java.io.PrintStream;
import java.util.stream.Collectors;

public abstract class GenerateLocaleConfigResTask extends GenerateTask {

    @Input
    public abstract ListProperty<String> getLocales();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Override
    public void generate() throws IOException {
        super.generate();
        var file = getOutputDir().file("xml/locales_config.xml").get().getAsFile();
        createFile(file);

        var os = new PrintStream(file);
        write(os);
        os.flush();
        os.close();
    }

    public void write(PrintStream os) {
        String content = """
                <?xml version="1.0" encoding="utf-8"?>
                <locale-config xmlns:android="http://schemas.android.com/apk/res/android">
                %s
                </locale-config>
                """;

        var localesString = getLocales().get().stream().map(s -> "<locale android:name=\"" + s + "\"/>").collect(Collectors.joining("\n"));

        os.printf(content, localesString);
    }
}
