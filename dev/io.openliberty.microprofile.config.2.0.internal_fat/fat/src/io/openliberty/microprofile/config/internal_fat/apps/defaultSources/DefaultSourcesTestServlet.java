/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat.apps.defaultSources;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.Assert;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class DefaultSourcesTestServlet extends FATServlet {

    @Inject
    Config config;

    /**
     * At time of writing, there are 7 default sources in this application. However, technically the exact number is
     * implementation specific. For example, the DefaultValuesConfigSource is added by SmallRye but never used.
     * This test is here as a sanity check because we discovered that the PropertiesConfigSource was being added twice.
     *
     * server.xml.appproperties.config.source(600)
     * server.xml.variables.config.source(500)
     * SysPropConfigSource
     * EnvConfigSource
     * PropertiesConfigSource[source=wsjar:file:/Users/tevans/Liberty/openLibertyGit/open-liberty/dev/build.image/wlp/usr/servers/Config20Server/dropins/defaultSourcesApp.war!/WEB-INF/classes/META-INF/microprofile-config.properties]
     * server.xml.default.variables.config.source(1)
     * DefaultValuesConfigSource
     */
    @Test
    public void testNumberOfSources() {
        Iterable<ConfigSource> sources = config.getConfigSources();
        int count = 0;
        for (ConfigSource src : sources) {
            count++;
            System.out.println("ConfigSource: " + src);
        }
        assertEquals("Wrong number of ConfigSources found", 7, count);
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void defaultSystemConfigProperties() throws Exception {
        Map<String, String> systemValues = new HashMap<>(System.getenv());
        Properties props = System.getProperties();

        // Properties override environment variables.
        for (Map.Entry<?, ?> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            systemValues.put(key, value);
        }

        // From mpConfig > 1.4, Property Expressions may evaluate to unexpected values for this test.
        // To avoid this, all values including a "$" or "\" are removed.
        systemValues.values().removeIf(v -> v.contains("$") || v.contains("\\"));

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();
        Config config = builder.build();

        // Bit of a naughty cast but we know it's a set and that's easier to work on
        Set<String> configPropertyNames = (Set<String>) config.getPropertyNames();

        Assert.assertFalse("There should be at least 1 system value.", systemValues.isEmpty());

        // Check expected Config property values are present
        for (Map.Entry<String, String> systemValue : systemValues.entrySet()) {

            String expectedKey = systemValue.getKey();

            Assert.assertTrue("The system value for " + expectedKey + " should be a Config property name",
                              configPropertyNames.contains(expectedKey));

            if (!systemValue.getValue().isEmpty()) {

                String expectedValue = systemValue.getValue();
                String actualValue = config.getValue(expectedKey, expectedKey.getClass());

                Assert.assertTrue("The system value for " + expectedKey + " should be " + expectedValue + ", not " + actualValue,
                                  expectedValue.equals(actualValue));
            }
        }
    }
}