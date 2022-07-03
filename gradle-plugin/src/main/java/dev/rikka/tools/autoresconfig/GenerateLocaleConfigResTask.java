package dev.rikka.tools.autoresconfig;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.stream.Collectors;

public class GenerateLocaleConfigResTask extends GenerateTask {

    private final File file;

    @Inject
    public GenerateLocaleConfigResTask(File dir, Collection<String> locales, Collection<String> displayLocales) {
        super(dir, locales, displayLocales);

        this.file = new File(dir, "xml/locales_config.xml");
    }

    @Override
    public void generate() throws IOException {
        super.generate();

        createFile(file);

        var os = new PrintStream(file);
        write(os);
        os.flush();
        os.close();
    }

    public void write(PrintStream os) {
        String content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<locale-config xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                "%s\n" +
                "</locale-config>\n";

        var localesString = locales.stream().map(s -> "<locale android:name=\"" + s + "\"/>").collect(Collectors.joining("\n"));

        os.printf(content, localesString);
    }
}
