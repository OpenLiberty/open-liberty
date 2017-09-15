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

import java.util.Properties;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

public class DefaultConfigProviderSourcesTest {

    @Test
    public void testUsersPropertiesSource() {
        Properties props = new Properties();
        props.setProperty("testKey", "testValue");
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(new PropertiesTestSource(props));
        Config config = builder.build();
        String value = config.getOptionalValue("testKey", String.class).orElse("not there");
        assertEquals("testValue", value);
    }

    @Test
    public void testPropertiesSource() {
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
    public void testMultipleSourcesNoOverrides() {
        Config config = null;
        try {
            ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder().addDefaultSources();
            JsonTestSource jsonSource = new JsonTestSource();
            XmlTestSource xmlSource = new XmlTestSource();
            builder.withSources(jsonSource, xmlSource);
            config = builder.build();
            testFound(config, "Styracosaurus", "spikey");
            testFound(config, "Diplodicus", "long");
            testFound(config, "Tyranosaurus", "scary");
            testFound(config, "Velociraptor", "fast");
            testFound(config, "Dimetrodon", "cool");
        } finally {
            if (config != null) {
                ConfigProviderResolver.instance().releaseConfig(config);
            }
        }
    }

    private void testFound(Config config, String propertyName, String expected) {
        String configuredValue = config.getOptionalValue(propertyName, String.class).orElse("missing");
        assertEquals(expected, configuredValue);
    }
}
