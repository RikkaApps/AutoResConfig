package dev.rikka.tools.autoresconfig;

import com.android.build.api.dsl.ApplicationExtension;
import com.android.build.api.variant.AndroidComponentsExtension;
import com.android.build.api.variant.ApplicationAndroidComponentsExtension;
import com.android.build.api.variant.ApplicationVariant;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@SuppressWarnings("unused")
public class AutoResConfigPlugin implements Plugin<Project> {

    private static final Map<String, String> displayLocaleMap = new HashMap<>();

    static {
        displayLocaleMap.put("zh-CN", "zh-Hans");
        displayLocaleMap.put("zh-TW", "zh-Hant");
    }

    private final Logger logger = Logging.getLogger(AutoResConfigPlugin.class);
    private final Map<String, Set<String>> modifierCache = new HashMap<>();

    private void collectModifiers(File dir, Collection<String> output) throws IOException {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        logger.debug("AutoResConfig: Collect modifiers from {}", dir);

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
                    .toList());
        }
    }

    private Collection<String> collectModifiers(ApplicationVariant variant) {
        var set = new HashSet<String>();
        set.add("en");

        Set<Directory> resDirs = new HashSet<>();
        var res = variant.getSources().getRes();
        if (res != null) {
            res.getStatic().get().forEach(resDirs::addAll);
        }

        for (var dir : resDirs) {
            Set<String> modifiers = modifierCache.get(dir.getAsFile().getAbsolutePath());
            if (modifiers != null) {
                set.addAll(modifiers);
            }
        }

        var list = new ArrayList<>(set);
        list.sort(String.CASE_INSENSITIVE_ORDER);
        return list;
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
        var appExtension = project.getExtensions().findByType(ApplicationExtension.class);
        if (appExtension == null) throw new GradleException("Android application extension not found");

        var extension = project.getExtensions().create(
                "autoResConfig", AutoResConfigExtension.class);

        // Build a file path -> locale modifiers cache
        modifierCache.clear();

        appExtension.getSourceSets().all(sourceSet -> {
            logger.info("AutoResConfig: Collect locale modifiers for {}", sourceSet.getName());

            for (var path : sourceSet.getRes().getDirectories()) {
                var dir = project.getLayout().getProjectDirectory().dir(path).getAsFile();
                var absolutePath = dir.getAbsolutePath();

                var set = modifierCache.get(absolutePath);
                if (set != null) continue;

                set = new HashSet<>();
                set.add("en");

                modifierCache.put(absolutePath, set);

                try {
                    collectModifiers(dir, set);
                } catch (IOException e) {
                    logger.error("AutoResConfig: Failed to collect modifiers from {}", dir, e);
                }
            }
        });

        var androidComponents = project.getExtensions().findByType(AndroidComponentsExtension.class);
        if (androidComponents instanceof ApplicationAndroidComponentsExtension componentsExtension) {
            componentsExtension.onVariants(androidComponents.selector().all(), variant -> {
                var variantName = variant.getName();
                var variantNameCapitalized = Util.capitalize(variantName);

                logger.info("AutoResConfig: Variant {}", variantName);
                logger.info("AutoResConfig: {}", extension);

                var modifiers = collectModifiers(variant);

                variant.getAndroidResources()
                        .getLocaleFilters()
                        .addAll(modifiers);

                var locales = convertModifiersToLocales(modifiers);
                var displayLocales = convertLocalesToDisplayLocales(locales);
                logger.info("AutoResConfig: Locales {}", locales);
                logger.info("AutoResConfig: Display locales {}", displayLocales);

                if (extension.getGenerateClass().get()) {
                    var javaSourceDir = project.getLayout().getBuildDirectory()
                            .dir(String.format("generated/auto_res_config/%s/java", variantName));

                    var taskName = String.format("generate%sAutoResConfigSource", variantNameCapitalized);

                    var task = project.getTasks().register(taskName,
                            GenerateJavaTask.class, t -> {
                                t.getExtension().set(extension);
                                t.getOutputDir().set(javaSourceDir);
                                t.getLocales().set(locales);
                                t.getDisplayLocales().set(displayLocales);
                            });

                    var java = variant.getSources().getJava();
                    if (java != null) {
                        java.addGeneratedSourceDirectory(task, GenerateJavaTask::getOutputDir);
                    }

                    logger.info("AutoResConfig: register {} {}", taskName, javaSourceDir);
                }

                if (extension.getGenerateRes().get()) {
                    var resDir = project.getLayout().getBuildDirectory()
                            .dir(String.format("generated/auto_res_config/%s/res", variantName));

                    var taskName = String.format("generate%sAutoResConfigRes", variantNameCapitalized);

                    var task = project.getTasks().register(taskName, GenerateResTask.class, t -> {
                        t.getExtension().set(extension);
                        t.getOutputDir().set(resDir);
                        t.getLocales().set(locales);
                        t.getDisplayLocales().set(displayLocales);
                    });

                    var res = variant.getSources().getRes();
                    if (res != null) {
                        res.addGeneratedSourceDirectory(task, GenerateResTask::getOutputDir);
                    }

                    logger.info("AutoResConfig: register {} {}", taskName, resDir);
                }

                if (extension.getGenerateLocaleConfig().get()) {
                    var resDir = project.getLayout().getBuildDirectory()
                            .dir(String.format("generated/auto_res_config/%s/res", variantName));

                    var taskName = String.format("generate%sAutoResConfigLocaleConfigRes", variantNameCapitalized);

                    var task = project.getTasks().register(taskName, GenerateLocaleConfigResTask.class, t -> {
                        t.getOutputDir().set(resDir);
                        t.getLocales().set(locales);
                        t.getDisplayLocales().set(displayLocales);
                    });

                    var res = variant.getSources().getRes();
                    if (res != null) {
                        res.addGeneratedSourceDirectory(task, GenerateLocaleConfigResTask::getOutputDir);
                    }

                    logger.info("AutoResConfig: register {} {}", taskName, resDir);
                }
            });
        }

    }
}
