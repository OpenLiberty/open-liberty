/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import com.ibm.ws.security.authentication.AuthenticationData;

/**
 *
 */
public class AuthenticationDataMatcher extends TypeSafeMatcher<AuthenticationData> {

    private final AuthenticationData authData;
    private String error = "NO ERROR YET";

    public AuthenticationDataMatcher(AuthenticationData authData) {
        this.authData = authData;
    }

    private String setError(String descrpitor, Object expected, Object actual) {
        return "expected AuthenticationData " + descrpitor + " of \"" + expected + "\", but got \"" + actual + "\"";
    }

    /**
     * @param authData2
     * @param username
     * @return
     */
    private boolean passwordsAreTheSame(AuthenticationData authData) {
        String expectedPassword = null;
        if (this.authData.get(AuthenticationData.PASSWORD) != null) {
            expectedPassword = String.valueOf((char[]) this.authData.get(AuthenticationData.PASSWORD));
        }
        String actualPassword = null;
        if (authData.get(AuthenticationData.PASSWORD) != null) {
            actualPassword = String.valueOf((char[]) authData.get(AuthenticationData.PASSWORD));
        }

        return stringsAreTheSame(expectedPassword, actualPassword);
    }

    /**
     * @param expectedPassword
     * @param actualPassword
     * @return
     */
    private boolean stringsAreTheSame(String expectedPassword, String actualPassword) {
        if (actualPassword == expectedPassword) {
            return true;
        }
        if ((actualPassword != null) && (actualPassword.equals(expectedPassword))) {
            return true;
        }
        if ((expectedPassword != null) && (expectedPassword.equals(actualPassword))) {
            return true;
        }
        return false;
    }

    @Override
    public boolean matchesSafely(AuthenticationData authData) {
        boolean matches = true;

        if (!stringsAreTheSame((String) this.authData.get(AuthenticationData.USERNAME), (String) authData.get(AuthenticationData.USERNAME))) {
            matches = false;
            error = setError("name", this.authData.get(AuthenticationData.USERNAME), authData.get(AuthenticationData.USERNAME));
        }
        if (!passwordsAreTheSame(authData)) {
            matches = false;
            error = setError("name", this.authData.get(AuthenticationData.PASSWORD), authData.get(AuthenticationData.PASSWORD));
        }

        return matches;
    }

    public void describeTo(Description description) {
        description.appendText(error);
    }
}
