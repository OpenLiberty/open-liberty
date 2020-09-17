/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.defaultSources.tests;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfig20EE8;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class DefaultSourcesTestServlet extends FATServlet {

    /**
     * Test that a simple config loads from the default locations
     * no provided files so config should just be process environment
     * variables, System.properties and WAS files.
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetConfig() throws Exception {
        Config config = ConfigProvider.getConfig();
        TestUtils.assertContains(config, "defaultSources.jar.meta-inf.config.properties", "jarPropertiesDefaultValue");
        TestUtils.assertContains(config, "defaultSources.war.meta-inf.config.properties", "warPropertiesDefaultValue");
    }

    @Test
    public void defaultsGetBuilderWithDefaults() throws Exception {
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        Config config = builder.addDefaultSources().build();

        TestUtils.assertContains(config, "defaultSources.jar.meta-inf.config.properties", "jarPropertiesDefaultValue");
        TestUtils.assertContains(config, "defaultSources.war.meta-inf.config.properties", "warPropertiesDefaultValue");
        //TODO add sys and env
    }

    /**
     * Tests that we can get a builder that will not include the default sources
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetEmptyBuilderNoDefaults() throws Exception {
        System.setProperty("MY_VALUE", "aValue");
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        Config config = builder.build();

        TestUtils.assertNotContains(config, "MY_VALUE");
    }

    /**
     * Tests that a config source can be loaded from within a jar
     */
    @Test
    public void defaultsGetConfigPathJar() throws Exception {
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();
        Config config = builder.build();

        TestUtils.assertContains(config, "defaultSources.jar.meta-inf.config.properties", "jarPropertiesDefaultValue");
    }

    /**
     * Tests that a config source can be loaded from within a war
     */
    @Test
    public void defaultsGetConfigPathWar() throws Exception {
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();
        Config config = builder.build();

        TestUtils.assertContains(config, "defaultSources.war.meta-inf.config.properties", "warPropertiesDefaultValue");
        TestUtils.assertNotContains(config, "warVisibility.war.meta-inf.config.properties");
    }

    /**
     * Tests that a config source can be loaded from all
     * valid places within a ear
     */
    @Test
    public void defaultsGetConfigPathEar() throws Exception {
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();
        Config config = builder.build();

        TestUtils.assertContains(config, "defaultSources.earLib.meta-inf.config.properties", "earlibPropertiesDefaultValue");
    }

    /**
     * Test that the microprofile-config.properties files are sourced ok
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetConfigProperties() throws Exception {
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();
        Config config = builder.build();

        TestUtils.assertContains(config, "defaultSources.jar.meta-inf.config.properties", "jarPropertiesDefaultValue");
        TestUtils.assertContains(config, "defaultSources.war.meta-inf.config.properties", "warPropertiesDefaultValue");
    }

    /**
     * Test that the WAS server level *.xml, *.properties and *.env files are sourced
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetConfigWasSpecific() throws Exception {
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();
        Config config = builder.build();

        TestUtils.assertContains(config, "bootstrap.properties.appConfig", "bootstrap.properties.defaultValue");
        TestUtils.assertContains(config, "server_env_appConfig", "server.env.defaultValue");
        TestUtils.assertContains(config, "jvm_options_appConfig", "jvm.options.defaultValue");
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetConfigPathSysProps() throws Exception {
        System.getProperties().setProperty("defaultSources.sysProps", "sysPropsValue");
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();
        Config config = builder.build();

        TestUtils.assertContains(config, "defaultSources.sysProps", "sysPropsValue");
    }

    /**
     *
     * @throws Exception
     */
    @Test
    @SkipForRepeat(RepeatConfig20EE8.ID) //temporarily disabled for MP Config 2.0
    public void defaultsGetConfigPathProcEnv() throws Exception {
        Map<String, String> env = new HashMap<>(System.getenv());
        Properties props = System.getProperties();

        //Properties override environment variables.
        for (Map.Entry<?, ?> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            env.put(key, value);
        }

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();
        Config config = builder.build();

        TestUtils.assertContains(config, env);
    }
}