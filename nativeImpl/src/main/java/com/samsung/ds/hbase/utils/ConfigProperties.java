package com.samsung.ds.hbase.utils;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.ConfigurationBuilderEvent;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.builder.fluent.PropertiesBuilderParameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.*;
import org.apache.commons.configuration2.reloading.PeriodicReloadingTrigger;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ConfigProperties {

    private static final String PROPERTIES_NAME = "config.properties";

    private static ConfigProperties instance = null;

    private ReloadingFileBasedConfigurationBuilder<PropertiesConfiguration> builder;

    private ConfigProperties() {
        File file = null;
        try {
            CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();
            File jarPath = new File(codeSource.getLocation().toURI().getPath());
            String propertiesPath = jarPath.getParentFile().getPath();

            if (Files.isRegularFile(Paths.get(propertiesPath + File.separator + PROPERTIES_NAME))) {
                //file = new File("file:" + PROPERTIES_NAME);
                file = new File(propertiesPath + File.separator + PROPERTIES_NAME);
            } else {
                file = new File(getClass().getClassLoader().getResource(PROPERTIES_NAME).getFile());
            }
        } catch (Exception e) {
            file = new File(getClass().getClassLoader().getResource(PROPERTIES_NAME).getFile());
        }

        List<FileLocationStrategy> subs = Arrays.asList(
                new ProvidedURLLocationStrategy(),
                new FileSystemLocationStrategy(),
                new ClasspathLocationStrategy());
        FileLocationStrategy strategy = new CombinedLocationStrategy(subs);

        PropertiesBuilderParameters propertiesBuilderParameters = new Parameters().properties()
                .setEncoding("UTF-8")
                .setFile(file)
                .setLocationStrategy(strategy)
                .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                .setReloadingRefreshDelay(2000L)
                .setThrowExceptionOnMissing(true);

        builder = new ReloadingFileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                .configure(propertiesBuilderParameters);

        builder.addEventListener(ConfigurationBuilderEvent.CONFIGURATION_REQUEST, Event -> {

        });

        PeriodicReloadingTrigger configReloadingTrigger = new PeriodicReloadingTrigger(
                builder.getReloadingController(), null, 1, TimeUnit.SECONDS);

        configReloadingTrigger.start();
    }

    public synchronized static ConfigProperties getInstance() {
        if(instance == null) {
            instance = new ConfigProperties();
        }

        return instance;
    }

    public Configuration getCompositeConfiguration() {
        try {
            return builder.getConfiguration();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getKeys() {
        Iterator<String> keys = getCompositeConfiguration().getKeys();
        List<String> keyList = new ArrayList<String>();
        while(keys.hasNext()) {
            keyList.add(keys.next());
        }
        return keyList;
    }

    public String getValue(String key) {
        String data = "";
        try {
            data = getCompositeConfiguration().getString(key, "");
        } catch (Exception e) {
            data = "";
        }
        return data;
    }

    public Integer getIntValue(String key) {
        Integer data = null;
        try {
            data = getCompositeConfiguration().getInteger(key, null);
        } catch (Exception e) {
            data = null;
        }
        return data;
    }

    public String[] getArrayValue(String key) {
        String[] data = new String[0];
        try {
            data = getCompositeConfiguration().getStringArray(key);
        } catch (Exception e) {
            data = new String[0];
        }
        return data;
    }
}
