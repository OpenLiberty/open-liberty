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
package com.ibm.ws.security.saf;

/**
 * Methods for parsing a "safSecurityName".
 *
 * A safSecurityName is a concatenation of:
 * (a) the userSecurityName (userId), and
 * (b) the SAFCredentialToken key
 */
public class SAFSecurityName {

    public static final String DELIM = "::";

    public static String create(String userId, String safCredTokenKey) {
        if (userId.indexOf(DELIM) > 0) {
            // The userId already contains a safCredTokenKey. Blow up.
            throw new IllegalArgumentException("Can't create SAFSecurityName with safCredTokenKey " + safCredTokenKey +
                                               " because userId " + userId + " already contains a safCredTokenKey");
        } else if (safCredTokenKey == null) {
            return userId;
        } else {
            return userId + DELIM + safCredTokenKey;
        }
    }

    public static String parseUserId(String safSecurityName) {
        int idx = safSecurityName.indexOf(DELIM); // Don't use String.split() - way too expensive.
        if (idx > 0) {
            return safSecurityName.substring(0, idx);
        } else {
            return safSecurityName;
        }
    }

    public static String parseKey(String safSecurityName) {
        int idx = safSecurityName.indexOf(DELIM); // Don't use String.split() - way too expensive.
        if (idx > 0) {
            return safSecurityName.substring(idx + DELIM.length());
        } else {
            return null;
        }
    }

    public static boolean containsKey(String safSecurityName) {
        return safSecurityName.contains(DELIM);
    }
}
