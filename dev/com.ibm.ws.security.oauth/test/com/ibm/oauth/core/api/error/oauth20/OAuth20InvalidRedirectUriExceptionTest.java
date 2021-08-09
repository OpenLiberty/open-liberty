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

public class OAuth20InvalidRedirectUriExceptionTest {

    private static final String REDIRECT_URI = "SomeInvalidURI";
    private static final String TIVOLI_EXCEPTION_MSG = "The redirect URI parameter was invalid: " + REDIRECT_URI;
    private static final String END_USER_MSG = "CWOAU0062E: The OAuth service provider could not redirect the request because the redirect URI was not valid. Contact your system administrator to resolve the problem.";
    private static final String REGULAR_MSG = "CWOAU0026E: The redirect URI parameter was invalid: " + REDIRECT_URI;
    private static final String LIBERTY_MSG_KEY = "security.oauth20.error.invalid.redirecturi";
    private static final Locale TEST_LOCALE = new Locale("en-us");
    private static final String TEST_ENCODING = "utf-8";

    @Test
    public void testException() {
        OAuth20InvalidRedirectUriException exception = new OAuth20InvalidRedirectUriException(LIBERTY_MSG_KEY, REDIRECT_URI, null);
        assertException(exception, REGULAR_MSG, END_USER_MSG);
    }

    @Test
    public void testException_TivoliConstructor() {
        OAuth20InvalidRedirectUriException exception = new OAuth20InvalidRedirectUriException(REDIRECT_URI, null);
        assertException(exception, TIVOLI_EXCEPTION_MSG, END_USER_MSG);
    }

    private void assertException(OAuth20InvalidRedirectUriException exception, String expectedMessage, String expectedFormattedMessage) {
        assertEquals("There must be an exception message.", expectedMessage, exception.getMessage());
        assertEquals("There must be a formatted message.", expectedFormattedMessage, exception.formatSelf(TEST_LOCALE, TEST_ENCODING));
        assertEquals("The redirection URI must be set.", REDIRECT_URI, exception.getRedirectURI());
    }

}
