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
package com.ibm.ws.microprofile.appConfig.ordForDefaults.test;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class OrdinalsForDefaultsTestServlet extends FATServlet {

    /**
     * Tests that default properties files can tolerate having the same
     * property defined in more that on micro-profile.xxx file and behaviour
     * is as expected.
     *
     * @throws Exception
     */
    @Test
    public void defaultsMixedOrdinals() throws Exception {
        System.setProperty("onlyInSysProps", "onlyInSysProps.sysPropsValue");
        System.setProperty("sysPropsOverriddenInOrd320", "OrdsysPropsOverriddenInOrd330.OrdsysPropsValue");
        System.setProperty("sysPropsOverriddenInOrd330", "OrdsysPropsOverriddenInOrd330.OrdsysPropsValue");
        System.setProperty("OrdsysPropsOverriddenInOrd320AndOrd330", "OrdsysPropsOverriddenInOrd330.OrdsysPropsValue");

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();
        Config config = builder.build();
        try {
            TestUtils.assertContains(config, "onlyInFile", "onlyInFile.fileValue");
            TestUtils.assertContains(config, "fileOverriddenInEnv", "fileOverriddenInEnv.envValue");
            TestUtils.assertContains(config, "fileOverriddenInSysProps", "fileOverriddenInSysProps.sysPropsValue");
            TestUtils.assertContains(config, "fileOverriddenInEnvAndSysProps", "fileOverriddenInEnvAndSysProps.sysPropsValue");

            TestUtils.assertContains(config, "onlyInEnv", "onlyInEnv.envValue");
            TestUtils.assertContains(config, "envOverriddenInSysProps", "envOverriddenInSysProps.sysPropsValue");
            TestUtils.assertContains(config, "envNotOverriddenByFile", "envNotOverriddenByFile.envValue");

            TestUtils.assertContains(config, "onlyInSysProps", "onlyInSysProps.sysPropsValue");
        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }
    }

    @Test
    public void defaultsOrdinalFromSource() throws Exception {
        System.setProperty("config_ordinal", "330");
        System.setProperty("Ord320OverriddenInOrd330", "Ord320OverriddenInOrd330.Ord330Value");

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();
        Config config = builder.build();
        try {
            TestUtils.assertContains(config, "onlyInOrd310", "onlyInOrd310.Ord310Value");
            TestUtils.assertContains(config, "Ord320OverriddenInOrd330", "Ord320OverriddenInOrd330.Ord330Value");

            TestUtils.assertContains(config, "onlyInOrd320", "onlyInOrd320.Ord320Value");
            TestUtils.assertContains(config, "Ord320NotOverriddenByOrd310", "Ord320NotOverriddenByOrd310.Ord320Value");

            TestUtils.assertContains(config, "onlyInOrd330", "onlyInOrd330.Ord330Value");

        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }
    }
}