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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

import org.junit.Test;

import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;

public class ExceptionsTest {

    private static final String EXCEPTION_MSG = "Test message";
    private static final String ENCODED_MSG = "Test+message";
    private static final Locale TEST_LOCALE = new Locale("en-us");
    private static final String TEST_ENCODING = "utf-8";

    @Test
    public void testOAuth20InternalException_AuthorizationIOException() {
        IOException ioe = new IOException(EXCEPTION_MSG);
        String responseRedirectURI = "someplace";
        String[] objs = new String[] { responseRedirectURI, ioe.getMessage() };
        OAuth20InternalException oie = new OAuth20InternalException("security.oauth20.error.authorization.internal.ioexception", ioe, objs);

        assertOAuth20InternalException(oie, "CWOAU0043E: The OAuth authorization endpoint could not redirect the user agent to the redirection URI [" + responseRedirectURI
                                            + "] because there was an unexpected java.io.IOException with message: " + ENCODED_MSG);
    }

    @Test
    public void testOAuth20InternalException_AuthorizationMissingIssuer() {
        OAuth20InternalException oie = new OAuth20InternalException("security.oauth20.error.authorization.internal.missing.issuer",
                        new Throwable("Missing " + OAuth20Constants.ISSUER_IDENTIFIER));

        assertOAuth20InternalException(oie, "CWOAU0044E: The OAuth authorization endpoint could not process the OAuth request because there was no issuer identifier found.");
    }

    @Test
    public void testOAuth20InternalException_TokenIOException() {
        IOException ioe = new IOException(EXCEPTION_MSG);
        String clientId = "someplace";
        String[] objs = new String[] { clientId, ioe.getMessage() };
        OAuth20InternalException oie = new OAuth20InternalException("security.oauth20.error.token.internal.exception", ioe, objs);

        assertOAuth20InternalException(oie, "CWOAU0045E: The OAuth token endpoint could not write the HTTP response to the OAuth client " + clientId
                                            + " because there was an unexpected exception with message: " + ENCODED_MSG);
    }

    @Test
    public void testOAuth20InternalException_TokenException() {
        Exception e = new Exception(EXCEPTION_MSG);
        String clientId = "someplace";
        String[] objs = new String[] { clientId, e.getMessage() };
        OAuth20InternalException oie = new OAuth20InternalException("security.oauth20.error.token.internal.exception", e, objs);

        assertOAuth20InternalException(oie, "CWOAU0045E: The OAuth token endpoint could not write the HTTP response to the OAuth client " + clientId
                                            + " because there was an unexpected exception with message: " + ENCODED_MSG);
    }

    @Test
    public void testOAuth20InternalException_TokenMissingIssuer() {
        OAuth20InternalException oie = new OAuth20InternalException("security.oauth20.error.token.internal.missing.issuer",
                        new Throwable("Missing " + OAuth20Constants.ISSUER_IDENTIFIER));

        assertOAuth20InternalException(oie, "CWOAU0046E: The OAuth token endpoint could not process the OAuth request because there was no issuer identifier found.");
    }

    @Test
    public void testOAuth20InternalException_UnsupportedEncodingException() {
        UnsupportedEncodingException uee = new UnsupportedEncodingException(EXCEPTION_MSG);
        OAuth20InternalException oie = new OAuth20InternalException("security.oauth20.error.internal.unsupported.encoding.exception", uee);

        assertOAuth20InternalException(oie,
                                       "CWOAU0047E: The OAuth service provider could not decode an HTTP request query string parameter because there was an unexpected java.io.UnsupportedEncodingException.");
    }

    private void assertOAuth20InternalException(OAuth20InternalException oie, String expectedMessage) {
        assertEquals("The exception message must be formatted.",
                     expectedMessage,
                     oie.formatSelf(TEST_LOCALE, TEST_ENCODING));
    }
}
