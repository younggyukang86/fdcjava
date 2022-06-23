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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component("config")
public class ConfigProperties {

    private static final String SERVER_PROPERTIES = "config.properties";

    private ReloadingFileBasedConfigurationBuilder<PropertiesConfiguration> builder;
    private Environment env;

    @Autowired
    public ConfigProperties(final Environment env) {
        Assert.notNull(env, "env can't be null");
        this.env = env;
    }

    @PostConstruct
    void init() {
        File file = new File(getClass().getClassLoader().getResource(SERVER_PROPERTIES).getFile());

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
