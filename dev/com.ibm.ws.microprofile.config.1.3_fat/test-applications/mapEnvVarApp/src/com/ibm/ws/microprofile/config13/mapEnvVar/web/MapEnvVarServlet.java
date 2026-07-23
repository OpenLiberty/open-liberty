/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.mapEnvVar.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.NoSuchElementException;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import componenttest.rules.repeater.MicroProfileActions;

/**
 *
 */
@WebServlet("/MapEnvVarServlet")
public class MapEnvVarServlet extends FATServlet {

    private Config config = null;

    @Inject
    @ConfigProperty(name = "serverXMLEnvKey", defaultValue = "dummy")
    private String serverXMLEnvVariable;

    private void buildConfig() {
        if (config == null) {
            ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
            builder.addDefaultSources();
            config = builder.build();
        }
    }

    @Test
    public void mapEnvVarTest() throws Exception {

        buildConfig();

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

    /**
     * Test that environment variables set in the shell can be referenced in server.xml
     * and accessed through MicroProfile Config.
     *
     * This test is skipped for MP33 (Config 1.4) because variable expansion for ${...} syntax
     * was not supported until MicroProfile Config 2.0. See:
     * https://openliberty.io/blog/2021/03/31/microprofile-config-2.0.html
     */
    @Test
    @SkipForRepeat(MicroProfileActions.MP33_ID)
    public void testShellEnvVarInServerXML() throws Exception {
        buildConfig();

        // Test that the environment variable set in the shell is accessible
        getAndCheckEnvVarValue("serverXMLEnvKey", "correctEnvValue"); //Without the `env.` prefix the mpConfig should see the raw string "${mpEnvKey}" from server.xml. Since it has ${} mpConfig will attempt to translate this itself. This is done in io.smallrye.config.EnvConfigSource.getValue()

        //MP Config will find serverXMLEnvKeyWithPrefix in server.xml, its server.xml value is a placeholder value: ${env.mpEnvKey}
        //under the old deprecated rules, this would map to an environment variable "mpEnvKey". However mpConfig does not use those old rules
        //as a result mpConfig is looking for an environment variable "env.mpEnvKey". This is deliberately not set.
        //So mpConfig cannot expand the value.
        try {
            //Expected value on MicroProfile_60 and below
            getAndCheckEnvVarValue("serverXMLEnvKeyWithPrefix", "not there");
        } catch (NoSuchElementException e) {
            //Expected value on MP70

            //Even though we are asking for an Optional. on MP70 MPConfig throws an exception when it finds a placeholder and cannot expand it.
        }
    }

    @Test
    @SkipForRepeat(MicroProfileActions.MP33_ID)
    public void testBuiltInDollarCurleyBraceVarInServerXML() throws Exception {
        buildConfig();

        //The value for this property is not found by the server.xml config source but by
        //https://github.com/smallrye/smallrye-config/blob/3.3.0/implementation/src/main/java/io/smallrye/config/SysPropConfigSource.java

        // Test that the environment variable set in the shell is accessible
        getAndCheckEnvVarValueContains("server.config.dir", "wlp/usr/server"); //With the `env.` prefix this should be translated by OSGiConfigUtils.getVariablesFromServerXML();
    }

    @Test
    @SkipForRepeat(MicroProfileActions.MP33_ID)
    public void testShellEnvVarInServerXMLViaInject() throws Exception {
        // Test that the environment variable set in the shell is accessible via injected bean
        String injectedValue = serverXMLEnvVariable;
        System.out.println("NYTRACE: Injected value from ConfigBean: " + injectedValue);
        assertEquals("Incorrect value found from injected bean", "correctEnvValue", injectedValue);
    }

    @Test
    @SkipForRepeat(MicroProfileActions.MP33_ID)
    public void testShellEnvVarViaJNDI() throws Exception {
        // for comparison purposes, test that getting an variable with an env prefix via JNDI has the same result

        //It turns out JDNI does correctly map ${env.x} to env variable x. But its also fine doing x -> x
        Object value = new InitialContext().lookup("serverXMLEnvKeyJNDI");
        String stringValue = (String) value;
        assertEquals("Incorrect value found from JNDI", "correctEnvValue", stringValue);

        value = new InitialContext().lookup("serverXMLEnvKeyWithPrefixJNDI");
        stringValue = (String) value;
        assertEquals("Incorrect value found from JNDI (with prefix)", "correctEnvValue", stringValue);
    }

    // Test that the server.xml variable that references the shell env var is accessible
    private void getAndCheckEnvVarValue(String key, String expectedValue) {
        String value = config.getOptionalValue(key, String.class).orElse("not there");
        System.out.println("NYTRACE: Seek: " + key + ", Expected: " + expectedValue + ", Found: " + value);
        assertEquals("Incorrect value found", expectedValue, value);
    }

    // Test that the server.xml variable that references the shell env var is accessible
    private void getAndCheckEnvVarValueContains(String key, String expectedValue) {
        String value = config.getOptionalValue(key, String.class).orElse("not there");
        System.out.println("NYTRACE: Seek: " + key + ", Expected: " + expectedValue + ", Found: " + value);
        assertTrue("Incorrect value found", value.contains(expectedValue));
    }

}
