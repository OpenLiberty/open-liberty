/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.utility.utils;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * This is a helper class for matching stdout.println statements.
 */
public class StringStartsWithMatcher implements Matcher<String> {
    private final String prefix;

    public StringStartsWithMatcher(String s) {
        prefix = s;
    }

    @Override
    public void describeTo(Description arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void _dont_implement_Matcher___instead_extend_BaseMatcher_() {
        // TODO Auto-generated method stub
    }

    /**
     * Check if the result returned by the security task starts with
     * the prefix specified at object creation. If TLS was specified,
     * also accept SSL. The only options for protocols are TLS and SSLv3.
     */
    @Override
    public boolean matches(Object s) {
        if (prefix.equals("TLS")) {
            return s.toString().startsWith("SSL") || s.toString().startsWith("TLS");
        }
        return s.toString().startsWith(prefix);
    }

}
