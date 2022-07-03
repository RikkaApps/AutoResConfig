package dev.rikka.tools.autoresconfig;

import org.gradle.api.provider.Property;

public abstract class AutoResConfigExtension {

    public abstract Property<Boolean> getGenerateClass();

    public abstract Property<String> getGeneratedClassFullName();

    public abstract Property<Boolean> getGenerateRes();

    public abstract Property<Boolean> getGenerateLocaleConfig();

    public abstract Property<String> getGeneratedResPrefix();

    public abstract Property<String> getGeneratedArrayFirstItem();

    public AutoResConfigExtension() {
        getGenerateClass().set(true);
        getGeneratedClassFullName().set("rikka.autoresconfig.AutoResConfigLocales");
        getGenerateRes().set(true);
        getGeneratedArrayFirstItem().set("SYSTEM");
        getGenerateLocaleConfig().set(true);
    }

    @Override
    public String toString() {
        return "AutoResConfigExtension{" +
                "generateClass=" + getGenerateClass().get() +
                ", generatedClassFullName=" + getGeneratedClassFullName().get() +
                ", generateRes=" + getGenerateRes().get() +
                ", generatedResPrefix=" + getGeneratedResPrefix().getOrElse("(null)") +
                ", generatedArrayFirstItem=" + getGeneratedArrayFirstItem().get() +
                ", generateLocaleConfig=" + getGenerateLocaleConfig().get() +
                "}";
    }
}
