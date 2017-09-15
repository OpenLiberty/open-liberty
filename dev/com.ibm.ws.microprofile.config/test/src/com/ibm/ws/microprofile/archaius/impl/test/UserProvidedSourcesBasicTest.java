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
import java.io.IOException;
import java.util.Properties;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

public class UserProvidedSourcesBasicTest {

    @Test
    public void testUsersPropertiesSource() {
        Properties props = new Properties();
        props.setProperty("testKey", "testValue");
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(new PropertiesTestSource(props));
        Config config = builder.build();
        String value = config.getValue("testKey", String.class);
        assertEquals("testValue", value);
    }

    @Test
    public void testUsersSystemsPropertiesSource() {

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.withSources(new SystemPropertiesTestSource());
        Config config = builder.build();
        String slash = config.getValue("file.separator", String.class);
        assertEquals(slash, File.separator);
    }

    @Test
    public void testMultipleUserSourcesNoOverrides() throws IOException {

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();

        Properties props = new Properties();
        builder.withSources(new EnvTestSource(), new SystemPropertiesTestSource(), new PropertiesTestSource(props), new XmlTestSource(), new JsonTestSource(),
                            new PropertiesTestSource());

        Config config = builder.build();

        String slash = config.getValue("file.separator", String.class);
        assertEquals(slash, File.separator);

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

            String configuredValue = config.getValue(name, String.class);
            assertEquals(configuredValue, expectedValue);
        }
    }
}
