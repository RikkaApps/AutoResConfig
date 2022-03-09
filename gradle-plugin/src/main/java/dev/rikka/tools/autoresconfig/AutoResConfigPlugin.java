package dev.rikka.tools.autoresconfig;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;
import com.android.builder.core.AbstractProductFlavor;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.SourceTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class AutoResConfigPlugin implements Plugin<Project> {

    private static final Map<String, String> displayLocaleMap = new HashMap<>();

    static {
        displayLocaleMap.put("zh-CN", "zh-Hans");
        displayLocaleMap.put("zh-TW", "zh-Hant");
    }

    private final Logger logger = Logging.getLogger(AutoResConfigPlugin.class);

    private void collectModifiers(File dir, Collection<String> output) throws IOException {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        logger.debug("AutoResConfig: Collect modifiers from " + dir);

        try (var stream = Files.list(dir.toPath())) {
            output.addAll(stream
                    .filter(file -> {
                        try {
                            return Files.isDirectory(file)
                                    && file.toFile().getName().startsWith("values-")
                                    && Files.exists(file.resolve("strings.xml"))

                                    // not an empty xml
                                    // TODO replace with find <string>?
                                    && Files.size(file.resolve("strings.xml")) > 62;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .map(java.nio.file.Path::getFileName)
                    .map(path -> path.toFile().getName().substring("values-".length()))
                    .filter(s -> s.split("-").length <= 3)
                    .collect(Collectors.toList()));
        }
    }

    private Collection<String> collectModifiers(@SuppressWarnings("deprecation") ApplicationVariant variant) {
        var set = new HashSet<String>();
        set.add("en");

        Set<File> resDirs = new HashSet<>();
        variant.getSourceSets().forEach(sourceProvider -> resDirs.addAll(sourceProvider.getResDirectories()));

        for (File dir : resDirs) {
            try {
                collectModifiers(dir, set);
            } catch (Throwable e) {
                logger.error("AutoResConfig: Failed to collect modifiers from " + dir, e);
            }
        }

        var list = new ArrayList<>(set);
        list.sort(String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    private boolean updateResConfig(@SuppressWarnings("deprecation") ApplicationVariant variant, Collection<String> modifiers) {
        var mergedFlavor = variant.getMergedFlavor();

        //noinspection deprecation
        if (mergedFlavor instanceof AbstractProductFlavor) {
            //noinspection deprecation
            var flavor = (AbstractProductFlavor) mergedFlavor;
            flavor.addResourceConfigurations(modifiers);
            return true;
        } else {
            return false;
        }
    }

    private Collection<String> convertModifiersToLocales(Collection<String> modifiers) {
        var locales = new ArrayList<String>();

        String locale;
        for (String modifier : modifiers) {
            if (modifier.startsWith("b+")) {
                String[] names = modifier.substring("b+".length()).split("\\+", 2);
                if (names.length == 2) {
                    locale = names[0] + "-" + names[1];
                } else {
                    locale = names[0];
                }
            } else {
                String[] names = modifier.split("-", 2);
                if (names.length == 2) {
                    locale = names[0] + "-" + names[1].substring("r".length());
                } else {
                    locale = names[0];
                }
            }

            locales.add(locale);
        }

        locales.sort(String.CASE_INSENSITIVE_ORDER);
        return locales;
    }

    private Collection<String> convertLocalesToDisplayLocales(Collection<String> locales) {
        var displayLocales = new ArrayList<String>();

        for (String locale : locales) {
            String displayLocale = displayLocaleMap.get(locale);
            if (displayLocale == null) {
                displayLocale = locale;
            }
            displayLocales.add(displayLocale);
        }

        return displayLocales;
    }

    @Override
    public void apply(Project project) {
        var appExtension = project.getExtensions().findByType(AppExtension.class);
        if (appExtension == null) throw new GradleException("Android application extension not found");

        var extension = project.getExtensions().create(
                "autoResConfig", AutoResConfigExtension.class);

        appExtension.getApplicationVariants().all(variant -> {
            var variantName = variant.getName();
            var variantNameCapitalized = Util.capitalize(variantName);

            logger.info("AutoResConfig: Variant " + variantName);
            logger.info("AutoResConfig: " + extension);

            var modifiers = collectModifiers(variant);

            if (updateResConfig(variant, modifiers)) {
                logger.info("AutoResConfig: Update resConfig " + modifiers);
            } else {
                logger.error("AutoResConfig: Failed to update resConfig");
            }

            var locales = convertModifiersToLocales(modifiers);
            var displayLocales = convertLocalesToDisplayLocales(locales);
            logger.info("AutoResConfig: Locales " + locales);
            logger.info("AutoResConfig: Display locales " + displayLocales);

            if (extension.getGenerateClass().get()) {
                var javaSourceDir = new File(project.getBuildDir(),
                        String.format("generated/auto_res_config/%s/java", variantName));

                var taskName = String.format("generate%sAutoResConfigSource", variantNameCapitalized);

                var generateJavaTask = project.getTasks().register(taskName,
                        GenerateJavaTask.class, extension, javaSourceDir, locales, displayLocales);

                variant.registerJavaGeneratingTask(generateJavaTask, javaSourceDir);

                logger.info("AutoResConfig: register " + taskName + " " + javaSourceDir);

                var kotlinCompileTask = (SourceTask) project.getTasks().findByName("compile" + variantNameCapitalized + "Kotlin");
                if (kotlinCompileTask != null) {
                    kotlinCompileTask.dependsOn(generateJavaTask);
                    kotlinCompileTask.source(generateJavaTask);
                }
            }

            if (extension.getGenerateRes().get()) {
                var resDir = new File(project.getBuildDir(),
                        String.format("generated/auto_res_config/%s/res", variantName));

                var taskName = String.format("generate%sAutoResConfigRes", variantNameCapitalized);

                var generateResTask = project.getTasks().register(taskName,
                        GenerateResTask.class, extension, resDir, locales, displayLocales);

                variant.registerGeneratedResFolders(
                        project.files(resDir).builtBy(generateResTask));

                logger.info("AutoResConfig: register " + taskName + " " + resDir);
            }
        });

    }
}
