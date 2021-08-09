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
package com.ibm.oauth.core.api.error.oauth20;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

public class OAuth20InvalidClientExceptionTest {

    private static final String CLIENT_ID = "TestClient";
    private static final String TIVOLI_EXCEPTION_MSG = "The client could not be found: " + CLIENT_ID;
    private static final String END_USER_MSG = "CWOAU0061E: The OAuth service provider could not find the client because the client name is not valid. Contact your system administrator to resolve the problem.";
    private static final String REGULAR_MSG = "CWOAU0023E: The OAuth service provider could not find the client " + CLIENT_ID + ".";
    private static final String LIBERTY_MSG_KEY = "security.oauth20.error.invalid.client";
    private static final Locale TEST_LOCALE = new Locale("en-us");
    private static final String TEST_ENCODING = "utf-8";

    @Test
    public void testException_AuthorizationRequest() {
        OAuth20InvalidClientException ice = new OAuth20InvalidClientException(LIBERTY_MSG_KEY, CLIENT_ID, false);
        assertException(ice, REGULAR_MSG, END_USER_MSG);
    }

    @Test
    public void testException_TokenRequest() {
        OAuth20InvalidClientException ice = new OAuth20InvalidClientException(LIBERTY_MSG_KEY, CLIENT_ID, true);
        assertException(ice, REGULAR_MSG, REGULAR_MSG);
    }

    @Test
    public void testException_TivoliConstructor_AuthorizationRequest() {
        OAuth20InvalidClientException ice = new OAuth20InvalidClientException(CLIENT_ID, false);
        assertException(ice, TIVOLI_EXCEPTION_MSG, END_USER_MSG);
    }

    @Test
    public void testException_TivoliConstructor_TokenRequest() {
        OAuth20InvalidClientException ice = new OAuth20InvalidClientException(CLIENT_ID, true);
        assertException(ice, TIVOLI_EXCEPTION_MSG, TIVOLI_EXCEPTION_MSG);
    }

    private void assertException(OAuth20InvalidClientException exception, String expectedMessage, String expectedFormattedMessage) {
        assertEquals("There must be an exception message.", expectedMessage, exception.getMessage());
        assertEquals("There must be a formatted message.", expectedFormattedMessage, exception.formatSelf(TEST_LOCALE, TEST_ENCODING));
        assertEquals("The client id must be set.", CLIENT_ID, exception.getClientId());
    }

}
