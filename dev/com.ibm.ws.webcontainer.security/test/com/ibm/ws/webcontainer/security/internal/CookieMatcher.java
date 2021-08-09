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
package com.ibm.ws.webcontainer.security.internal;

import javax.servlet.http.Cookie;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class CookieMatcher extends TypeSafeMatcher<Cookie> {
    private final Cookie cookie;
    private String error = "NO ERROR YET";

    public CookieMatcher(Cookie c) {
        this.cookie = c;
    }

    private String setError(String descrpitor, int expected, int actual) {
        return setError(descrpitor, String.valueOf(expected), String.valueOf(actual));
    }

    private String setError(String descrpitor, String expected, String actual) {
        return "expected cookie " + descrpitor + " of \"" + expected + "\", but got \"" + actual + "\"";
    }

    @Override
    public boolean matchesSafely(Cookie c) {
        boolean matches = true;
        if (!cookie.getName().equals(c.getName())) {
            matches = false;
            error = setError("name", cookie.getName(), c.getName());
        }
        if (!cookie.getValue().equals(c.getValue())) {
            matches = false;
            error = setError("value", cookie.getValue(), c.getValue());
        }
        if (!(cookie.getMaxAge() == c.getMaxAge())) {
            matches = false;
            error = setError("max age", cookie.getMaxAge(), c.getMaxAge());
        }
        if (cookie.getPath() != null && !cookie.getPath().equals(c.getPath())) {
            matches = false;
            error = setError("path", cookie.getPath(), c.getPath());
        }
        return matches;
    }

    public void describeTo(Description description) {
        description.appendText(error);
    }
}
