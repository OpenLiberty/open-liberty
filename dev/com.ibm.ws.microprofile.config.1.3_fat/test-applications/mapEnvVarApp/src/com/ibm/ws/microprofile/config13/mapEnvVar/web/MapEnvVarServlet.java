/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.mapEnvVar.web;

import static org.junit.Assert.assertEquals;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import componenttest.app.FATServlet;

/**
 *
 */
@WebServlet("/MapEnvVarServlet")
public class MapEnvVarServlet extends FATServlet {

    private Config config = null;

    @Test
    public void mapEnvVarTest() throws Exception {

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();
        config = builder.build();

        // Non-Alphanumerics will be mapped to underscores. Lower case chars
        // may be mapped to upper case.
        getAndCheckEnvVarValue("MPCONFIG_FATTEST_ENV_VARIABLE", "mpconfigtestValue");
        getAndCheckEnvVarValue("MPCONFIG.FATTEST.ENV.VARIABLE", "mpconfigtestValue");
        getAndCheckEnvVarValue("mpconfig.fattest.env.variable", "mpconfigtestValue");
        getAndCheckEnvVarValue("MPCONFIG_FATTEST_ENV.variable", "mpconfigtestValue");
        // Same property name as above, value should now be cached, do we get the same value?
        getAndCheckEnvVarValue("MPCONFIG_FATTEST_ENV.variable", "mpconfigtestValue");

        // Leading non-alpha chars
        getAndCheckEnvVarValue("__MPCONFIG_FATTEST_ENV_VARIABLE", "__mpconfigtestValue");
        getAndCheckEnvVarValue("_.MPCONFIG_FATTEST_ENV_VARIABLE", "__mpconfigtestValue");
        getAndCheckEnvVarValue("$$MPCONFIG_FATTEST_ENV_VARIABLE", "__mpconfigtestValue");
        getAndCheckEnvVarValue("$$MPCONFIG/FATTEST/ENV/VARIABLE", "__mpconfigtestValue");
        getAndCheckEnvVarValue("$$MPCONFIG.fattest.ENV.variable", "__mpconfigtestValue");

        // Trailing non-alpha chars
        getAndCheckEnvVarValue("MPCONFIG_FATTEST_ENV_VARIABLE__", "mpconfigtestValue__");
        getAndCheckEnvVarValue("MPCONFIG_FATTEST_ENV_VARIABLE_.", "mpconfigtestValue__");
        getAndCheckEnvVarValue("MPCONFIG_FATTEST_ENV_VARIABLE$$", "mpconfigtestValue__");
        getAndCheckEnvVarValue("MPCONFIG/FATTEST/ENV/VARIABLE$$", "mpconfigtestValue__");
        getAndCheckEnvVarValue("MPCONFIG.fattest.ENV.variable$$", "mpconfigtestValue__");

        // Middle non-alpha chars
        getAndCheckEnvVarValue("MPCONFIG_FATTEST__ENV_VARIABLE", "mpconfig__testValue");
        getAndCheckEnvVarValue("MPCONFIG_FATTEST_.ENV_VARIABLE", "mpconfig__testValue");
        getAndCheckEnvVarValue("MPCONFIG_FATTEST$$ENV_VARIABLE", "mpconfig__testValue");
        getAndCheckEnvVarValue("MPCONFIG/FATTEST//ENV/VARIABLE", "mpconfig__testValue");
        getAndCheckEnvVarValue("MPCONFIG.fattest..ENV.variable", "mpconfig__testValue");

        // Non-existent env variables
        getAndCheckEnvVarValue("MPCONFIG_FATTEST_NO_SUCH_ENV_VARIABLE", "not there");
        getAndCheckEnvVarValue("MPCONFIG.FATTEST.NO.SUCH.ENV.VARIABLE", "not there");
        getAndCheckEnvVarValue("mpconfig.fattest.no.such.env.variable", "not there");
        getAndCheckEnvVarValue("MPCONFIG_FATTEST_NO_SUCH_ENV.variable", "not there");
        getAndCheckEnvVarValue("MPCONFIG_FATTEST_NO_SUCH_ENV.variable", "not there");

        // Env Variables with no lower case chars or underscores
        getAndCheckEnvVarValue("MPCONFIGFATTESTENVVARIABLE", "mpconfigtestValue");
        getAndCheckEnvVarValue("MPCONFIGFATTESTNOSUCHENVVARIABLE", "not there");

        getAndCheckEnvVarValue("mpconfig_lowcase_fattest_env_variable", "mpconfiglowcasetestValue");
        getAndCheckEnvVarValue("mpconfig_MIXEDcase_fattest_env_variable", "mpconfigmixedcasetestValue");
        getAndCheckEnvVarValue("mpconfig_MIXEDcase.fattest_env_variable", "mpconfigmixedcasetestValue");
        getAndCheckEnvVarValue("mpconfig/MIXEDcase.fattest_env/variable", "mpconfigmixedcasetestValue");
        getAndCheckEnvVarValue("mpconfig_lowcase_FATTEST_env_variable", "not there");
        getAndCheckEnvVarValue("mpconfig_lowcase_fattest_env_variable.", "not there");
    }

    private void getAndCheckEnvVarValue(String key, String expectedValue) {
        String value = config.getOptionalValue(key, String.class).orElse("not there");
        System.out.println("NYTRACE: Seek: " + key + ", Expected: " + expectedValue + ", Found: " + value);
        assertEquals("Incorrect value found", expectedValue, value);
    }

}
