/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.archaius.impl.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

public class DefaultBuilderSourcesTest {

    @Test
    public void testUsersPropertiesSource() {
        Properties props = new Properties();
        props.setProperty("testKey", "testValue");
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder().addDefaultSources();
        builder.withSources(new PropertiesTestSource(props));
        Config config = builder.build();
        String value = config.getOptionalValue("testKey", String.class).orElse("not there");
        assertEquals("testValue", value);
    }

    @Test
    public void testUsersEnvSource() {
        Map.Entry<String, String> entry = System.getenv().entrySet().iterator().next();
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder().addDefaultSources();
        builder.withSources(new EnvTestSource());
        Config config = builder.build();
        String actual = config.getOptionalValue(entry.getKey(), String.class).orElse("not there");
        assertEquals(entry.getValue(), actual);
    }

    @Test
    public void testUsersSystemsSource() {

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder().addDefaultSources();
        builder.withSources(new SystemPropertiesTestSource());
        Config config = builder.build();
        String slash = config.getOptionalValue("file.separator", String.class).orElse("noSlash");
        assertEquals(slash, File.separator);
    }

    @Test
    public void testDefaultPropertiesBuilderSource() {
        Config config = null;
        try {
            config = ConfigProvider.getConfig();
            String dino = config.getOptionalValue("Dimetrodon", String.class).orElse("extinct");
            assertEquals("cool", dino);
        } finally {
            if (config != null) {
                ConfigProviderResolver.instance().releaseConfig(config);
            }
        }
    }

    @Test
    public void testMultipleUserAddedDefaultSourcesNoOverrides() {

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder().addDefaultSources();
        Properties props = new Properties();
        props.setProperty("testKey", "testValue");

        builder.withSources(new XmlTestSource(), new JsonTestSource(), new PropertiesTestSource());

        Config config = builder.build();

        testFound(config,
                  "Ankylosaurus", "knobbly",
                  "Styracosaurus", "spikey",
                  "Diplodicus", "long",
                  "Tyranosaurus", "scary",
                  "Velociraptor", "fast",
                  "Dimetrodon", "cool");

    }

    private void testFound(Config config, String... string) {
        for (int i = 0; i < (string.length - 1);) {
            String name = string[i++];
            String expectedValue = string[i++];

            String configuredValue = config.getOptionalValue(name, String.class).orElse("missing");
            assertEquals(configuredValue, expectedValue);
        }
    }
}
