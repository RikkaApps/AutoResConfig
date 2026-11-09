package dev.rikka.tools.autoresconfig;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;

import java.io.IOException;
import java.io.PrintStream;
import java.util.stream.Collectors;

@CacheableTask
public abstract class GenerateResTask extends GenerateTask {
    @Input
    public abstract ListProperty<String> getLocales();

    @Input
    public abstract ListProperty<String> getDisplayLocales();

    @Input
    public abstract Property<AutoResConfigExtension> getExtension();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Override
    public void generate() throws IOException {
        super.generate();
        var extension = getExtension().get();
        var file = getOutputDir().file("values/arrays.xml").get().getAsFile();
        createFile(file);

        var os = new PrintStream(file);
        write(os, extension);
        os.flush();
        os.close();
    }

    public void write(PrintStream os, AutoResConfigExtension extension) {
        String content = """
                <?xml version="1.0" encoding="utf-8"?>
                <resources>
                <string-array name="%s">
                %s%s
                </string-array>
                <string-array name="%s">
                %s%s
                </string-array>
                </resources>
                """;

        var prefix = extension.getGeneratedResPrefix().getOrElse("");
        var localesString = getLocales().get().stream().map(s -> "<item>" + s + "</item>").collect(Collectors.joining("\n"));
        var displayLocalesString = getDisplayLocales().get().stream().map(s -> "<item>" + s + "</item>").collect(Collectors.joining("\n"));
        var localesResName = prefix.isEmpty() ? "locales" : prefix + "_locales";
        var displayLocalesResName = prefix.isEmpty() ? "display_locales" : prefix + "_display_locales";
        var firstItem = extension.getGeneratedArrayFirstItem().getOrElse("").isEmpty() ? ""
                : ("<item>" + extension.getGeneratedArrayFirstItem().get() + "</item>\n");

        os.printf(content,
                localesResName,
                firstItem,
                localesString,
                displayLocalesResName,
                firstItem,
                displayLocalesString);
    }
}
