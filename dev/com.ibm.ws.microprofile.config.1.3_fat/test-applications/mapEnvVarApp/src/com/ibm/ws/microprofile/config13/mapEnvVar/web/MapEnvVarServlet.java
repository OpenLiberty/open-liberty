/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.mapEnvVar.web;

import static org.junit.Assert.assertEquals;

import java.util.NoSuchElementException;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.Test;

import com.ibm.ws.microprofile.config13.test.utils.ConfigChecker;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import componenttest.rules.repeater.MicroProfileActions;

/**
 *
 */
@WebServlet("/MapEnvVarServlet")
public class MapEnvVarServlet extends FATServlet {

    @Inject
    private ConfigChecker configChecker;

    @Inject
    @ConfigProperty(name = "serverXMLEnvKey", defaultValue = "dummy")
    private String serverXMLEnvVariable;

    @Test
    public void mapEnvVarTest() throws Exception {

        // Non-Alphanumerics will be mapped to underscores. Lower case chars
        // may be mapped to upper case.
        configChecker.assertConfigPropertyEquals("MPCONFIG_FATTEST_ENV_VARIABLE", "mpconfigtestValue");
        configChecker.assertConfigPropertyEquals("MPCONFIG.FATTEST.ENV.VARIABLE", "mpconfigtestValue");
        configChecker.assertConfigPropertyEquals("mpconfig.fattest.env.variable", "mpconfigtestValue");
        configChecker.assertConfigPropertyEquals("MPCONFIG_FATTEST_ENV.variable", "mpconfigtestValue");
        // Same property name as above, value should now be cached, do we get the same value?
        configChecker.assertConfigPropertyEquals("MPCONFIG_FATTEST_ENV.variable", "mpconfigtestValue");

        // Leading non-alpha chars
        configChecker.assertConfigPropertyEquals("__MPCONFIG_FATTEST_ENV_VARIABLE", "__mpconfigtestValue");
        configChecker.assertConfigPropertyEquals("_.MPCONFIG_FATTEST_ENV_VARIABLE", "__mpconfigtestValue");
        configChecker.assertConfigPropertyEquals("$$MPCONFIG_FATTEST_ENV_VARIABLE", "__mpconfigtestValue");
        configChecker.assertConfigPropertyEquals("$$MPCONFIG/FATTEST/ENV/VARIABLE", "__mpconfigtestValue");
        configChecker.assertConfigPropertyEquals("$$MPCONFIG.fattest.ENV.variable", "__mpconfigtestValue");

        // Trailing non-alpha chars
        configChecker.assertConfigPropertyEquals("MPCONFIG_FATTEST_ENV_VARIABLE__", "mpconfigtestValue__");
        configChecker.assertConfigPropertyEquals("MPCONFIG_FATTEST_ENV_VARIABLE_.", "mpconfigtestValue__");
        configChecker.assertConfigPropertyEquals("MPCONFIG_FATTEST_ENV_VARIABLE$$", "mpconfigtestValue__");
        configChecker.assertConfigPropertyEquals("MPCONFIG/FATTEST/ENV/VARIABLE$$", "mpconfigtestValue__");
        configChecker.assertConfigPropertyEquals("MPCONFIG.fattest.ENV.variable$$", "mpconfigtestValue__");

        // Middle non-alpha chars
        configChecker.assertConfigPropertyEquals("MPCONFIG_FATTEST__ENV_VARIABLE", "mpconfig__testValue");
        configChecker.assertConfigPropertyEquals("MPCONFIG_FATTEST_.ENV_VARIABLE", "mpconfig__testValue");
        configChecker.assertConfigPropertyEquals("MPCONFIG_FATTEST$$ENV_VARIABLE", "mpconfig__testValue");
        configChecker.assertConfigPropertyEquals("MPCONFIG/FATTEST//ENV/VARIABLE", "mpconfig__testValue");
        configChecker.assertConfigPropertyEquals("MPCONFIG.fattest..ENV.variable", "mpconfig__testValue");

        // Non-existent env variables
        configChecker.assertConfigPropertyEquals("MPCONFIG_FATTEST_NO_SUCH_ENV_VARIABLE", "not there");
        configChecker.assertConfigPropertyEquals("MPCONFIG.FATTEST.NO.SUCH.ENV.VARIABLE", "not there");
        configChecker.assertConfigPropertyEquals("mpconfig.fattest.no.such.env.variable", "not there");
        configChecker.assertConfigPropertyEquals("MPCONFIG_FATTEST_NO_SUCH_ENV.variable", "not there");
        configChecker.assertConfigPropertyEquals("MPCONFIG_FATTEST_NO_SUCH_ENV.variable", "not there");

        // Env Variables with no lower case chars or underscores
        configChecker.assertConfigPropertyEquals("MPCONFIGFATTESTENVVARIABLE", "mpconfigtestValue");
        configChecker.assertConfigPropertyEquals("MPCONFIGFATTESTNOSUCHENVVARIABLE", "not there");

        configChecker.assertConfigPropertyEquals("mpconfig_lowcase_fattest_env_variable", "mpconfiglowcasetestValue");
        configChecker.assertConfigPropertyEquals("mpconfig_MIXEDcase_fattest_env_variable", "mpconfigmixedcasetestValue");
        configChecker.assertConfigPropertyEquals("mpconfig_MIXEDcase.fattest_env_variable", "mpconfigmixedcasetestValue");
        configChecker.assertConfigPropertyEquals("mpconfig/MIXEDcase.fattest_env/variable", "mpconfigmixedcasetestValue");
        configChecker.assertConfigPropertyEquals("mpconfig_lowcase_FATTEST_env_variable", "not there");
        configChecker.assertConfigPropertyEquals("mpconfig_lowcase_fattest_env_variable.", "not there");
    }

    /**
     * Test that environment variables set in the can be referenced in server.xml
     * and accessed through MicroProfile Config.
     *
     * This test is skipped for MP33 (Config 1.4) because variable expansion for ${...} syntax
     * was not supported until MicroProfile Config 2.0. See:
     * https://openliberty.io/blog/2021/03/31/microprofile-config-2.0.html
     */
    @Test
    @SkipForRepeat(MicroProfileActions.MP33_ID)
    public void testEnvVarInServerXML() throws Exception {

        // Test that the environment variable set in the is accessible
        configChecker.assertConfigPropertyEquals("serverXMLEnvKey", "correctEnvValue"); //Without the `env.` prefix the mpConfig should see the raw string "${mpEnvKey}" from server.xml. Since it has ${} mpConfig will attempt to translate this itself. This is done in io.smallrye.config.EnvConfigSource.getValue()

        //MP Config will find serverXMLEnvKeyWithPrefix in server.xml, its server.xml value is a placeholder value: ${env.mpEnvKey}
        //under the old deprecated rules, this would map to an environment variable "mpEnvKey". However mpConfig does not use those old rules
        //as a result mpConfig is looking for an environment variable "env.mpEnvKey". This is deliberately not set.
        //So mpConfig cannot expand the value.
        try {
            //Expected value on MP70
            configChecker.assertConfigPropertyNotPresent("serverXMLEnvKeyWithPrefix");
        } catch (NoSuchElementException e) {
            //Expected value on MicroProfile_60 and below

            //Even though we are asking for an Optional. on MP70 MPConfig throws an exception when it finds a placeholder and cannot expand it.
        }
    }

    @Test
    @SkipForRepeat(MicroProfileActions.MP33_ID)
    public void testEnvVarInServerXMLViaInject() throws Exception {
        // Test that the environment variable set in the is accessible via injected bean
        String injectedValue = serverXMLEnvVariable;
        System.out.println("NYTRACE: Injected value from ConfigBean: " + injectedValue);
        assertEquals("Incorrect value found from injected bean", "correctEnvValue", injectedValue);
    }

    @Test
    @SkipForRepeat(MicroProfileActions.MP33_ID)
    public void testEnvVarViaJNDI() throws Exception {
        // for comparison purposes, test that getting an variable with an env prefix via JNDI has the same result

        //It turns out JDNI does correctly map ${env.x} to env variable x. But its also fine doing x -> x
        Object value = new InitialContext().lookup("serverXMLEnvKeyJNDI");
        String stringValue = (String) value;
        assertEquals("Incorrect value found from JNDI", "correctEnvValue", stringValue);

        value = new InitialContext().lookup("serverXMLEnvKeyWithPrefixJNDI");
        stringValue = (String) value;
        assertEquals("Incorrect value found from JNDI (with prefix)", "correctEnvValue", stringValue);
    }
}
