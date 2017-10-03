/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
 * This tests the ordinal numbers coming directly from the contents of the default source Ord310s
 * The rules usually are:
 * <ol>
 * <li>System properties (ordinal=400)</li>
 * <li>Environment properties (ordinal=300)</li>
 * <li>/META-INF/microproOrd310-config.properties (ordinal=100)</li>
 * </ol>
 *
 * but here we change the rules to reverse the order...
 *
 * System.getProperties config_ordinal=310
 * server.env config_ordinal=320
 * micorprofile-config.properties config_ordinal=330
 *
 */
public class DefaultsOrdinalFromSource extends AbstractAppConfigTestApp {
    /** {@inheritDoc} */
    @Override
    public String runTest(HttpServletRequest request) {
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

        } catch (Throwable t) {
            TestUtils.fail(t);
        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }

        return "PASSED";
    }

}
