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
package com.ibm.ws.microprofile.appConfig.ordForDefaults.test;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

/**
 *
 */
public class DefaultsMixedOrdinals extends AbstractAppConfigTestApp {
    /** {@inheritDoc} */
    @Override
    public String runTest(HttpServletRequest request) {
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
        } catch (Throwable t) {
            TestUtils.fail(t);
        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }

        return "PASSED";
    }
}
