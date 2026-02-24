package dev.rikka.tools.autoresconfig;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;

import java.io.IOException;
import java.io.PrintStream;
import java.util.stream.Collectors;

public abstract class GenerateJavaTask extends GenerateTask {
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
        var file = getOutputDir().file(String.format("%s.java",
                String.join("/", extension.getGeneratedClassFullName().get().split("\\.")))).get().getAsFile();
        createFile(file);

        var os = new PrintStream(file);
        write(os, extension);
        os.flush();
        os.close();
    }

    public void write(PrintStream os, AutoResConfigExtension extension) {
        String content = """
                package %s;
                
                public final class %s {
                    public static final String[] LOCALES = {%s%s};
                    public static final String[] DISPLAY_LOCALES = {%s%s};
                }
                """;

        var generatedClassFullName = extension.getGeneratedClassFullName().get();
        var index = generatedClassFullName.lastIndexOf('.');
        var packageName = generatedClassFullName.substring(0, index);
        var className = generatedClassFullName.substring(index + 1);

        var localesString = getLocales().get().stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(","));
        var displayLocalesString = getDisplayLocales().get().stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(","));
        var firstItem = extension.getGeneratedArrayFirstItem().getOrElse("").isEmpty() ? ""
                : ("\"" + extension.getGeneratedArrayFirstItem().get() + "\",");

        os.printf(content,
                packageName,
                className,
                firstItem,
                localesString,
                firstItem,
                displayLocalesString);
    }
}
