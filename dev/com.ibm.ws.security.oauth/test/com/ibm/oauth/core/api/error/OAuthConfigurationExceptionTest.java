/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.api.error;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Locale;

import org.junit.Test;

public class OAuthConfigurationExceptionTest {

    private static final String EXCEPTION_MSG = "Test message";
    private static final Locale TEST_LOCALE = new Locale("en-us");
    private static final String TEST_ENCODING = "utf-8";
    private static final String CONFIG_PROPERTY = "property1";
    private static final String CONFIG_VALUE = "value1";

    @Test
    public void testOAuthConfigurationException_NoMsgKey_Cause() {
        IOException ioe = new IOException(EXCEPTION_MSG);
        OAuthConfigurationException oce = new OAuthConfigurationException(CONFIG_PROPERTY, CONFIG_VALUE, ioe);

        assertOAuthConfigurationException(oce, "Error with configuration property: " + CONFIG_PROPERTY + " value: " + CONFIG_VALUE + " : " + EXCEPTION_MSG);
    }

    @Test
    public void testOAuthConfigurationException_NoMsgKey_NoCause() {
        OAuthConfigurationException oce = new OAuthConfigurationException(CONFIG_PROPERTY, CONFIG_VALUE, null);

        assertOAuthConfigurationException(oce, "Error with configuration property: " + CONFIG_PROPERTY + " value: " + CONFIG_VALUE);
    }

    @Test
    public void testOAuthConfigurationException_MsgKey_Cause1() {
        IOException ioe = new IOException(EXCEPTION_MSG);
        OAuthConfigurationException oce = new OAuthConfigurationException("security.oauth.error.invalidconfig.exception", CONFIG_PROPERTY, CONFIG_VALUE, ioe);

        assertOAuthConfigurationException(oce, "CWOAU0051E: The configuration value [" + CONFIG_VALUE + "] which is specified with the configuration parameter [" + CONFIG_PROPERTY
                                               + "] is not valid.");
    }

    @Test
    public void testOAuthConfigurationException_MsgKey_Cause2() {
        IOException ioe = new IOException(EXCEPTION_MSG);
        OAuthConfigurationException oce = new OAuthConfigurationException("security.oauth.error.classinstantiation.exception", CONFIG_PROPERTY, CONFIG_VALUE, ioe);

        assertOAuthConfigurationException(oce, "CWOAU0050E: The specified class [" + CONFIG_PROPERTY + "] with the configuration parameter [" + CONFIG_VALUE
                                               + "] cannot be instantiated. The root exception is : " + EXCEPTION_MSG);
    }

    @Test
    public void testOAuthConfigurationException_MsgKey_NoCause() {
        OAuthConfigurationException oce = new OAuthConfigurationException("security.oauth.error.mismatch.responsetype.exception", CONFIG_PROPERTY, CONFIG_VALUE, null);

        assertOAuthConfigurationException(oce, "CWOAU0053E: The value of the configuration parameter [" + CONFIG_PROPERTY + "] does not match the response_type parameter ["
                                               + CONFIG_VALUE + "] in the OAuth request.");
    }

    private void assertOAuthConfigurationException(OAuthConfigurationException oce, String expectedMessage) {
        assertEquals("The exception message must be formatted.", expectedMessage, oce.formatSelf(TEST_LOCALE, TEST_ENCODING));
    }
}
