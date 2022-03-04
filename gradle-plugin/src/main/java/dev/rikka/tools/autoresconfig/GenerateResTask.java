package dev.rikka.tools.autoresconfig;

import javax.inject.Inject;
import java.io.File;
import java.io.PrintStream;
import java.util.Collection;
import java.util.stream.Collectors;

public class GenerateResTask extends GenerateTask {

    private final AutoResConfigExtension extension;

    @Inject
    public GenerateResTask(AutoResConfigExtension extension, File file, Collection<String> locales, Collection<String> displayLocales) {
        super(new File(file, "arrays.xml"), locales, displayLocales);

        this.extension = extension;
    }

    @Override
    public void onWrite(PrintStream os) {
        String content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resource>\n" +
                "<string-array name=\"%s\">\n" +
                "%s%s\n" +
                "</string-array>\n" +
                "<string-array name=\"%s\">\n" +
                "%s%s\n" +
                "</string-array>\n" +
                "</resource>\n";

        var prefix = extension.getGeneratedResPrefix().getOrElse("");
        var localesString = locales.stream().map(s -> "<item>" + s + "</item>").collect(Collectors.joining("\n"));
        var displayLocalesString = displayLocales.stream().map(s -> "<item>" + s + "</item>").collect(Collectors.joining("\n"));
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
