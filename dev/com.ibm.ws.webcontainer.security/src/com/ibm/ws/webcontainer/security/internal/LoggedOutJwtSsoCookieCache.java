/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * A class to "remember" which jwtsso cookies have been logged out.
 * Allows for defense against cookie hijacking attacks,
 * reuse of a previously logged out cookie value can be detected and authentication refused.
 *
 * todo: could also use security/common/structures/cache.java, but that's heavier weight.
 */
public class LoggedOutJwtSsoCookieCache {
    private static ArrayList<String> clist = null;
    private static Set<String> cSet = null;
    private static int maxSize = 1000;
    private static int lastPosition = 0;
    private static boolean atCapacity = false;
    private static boolean initialized = false;
    private static MessageDigest md = null;

    private LoggedOutJwtSsoCookieCache() {}

    private static void init() {
        clist = new ArrayList<String>();
        cSet = new HashSet<String>();
        try {
            md = MessageDigest.getInstance("SHA-1"); // nice short string
        } catch (Exception e) {
        }
    }

    // store as digests to save space
    static String toDigest(String input) {
        md.update(input.getBytes());
        String result = new String(md.digest());
        md.reset();
        return result;
    }

    public static synchronized void put(String tokenString) {
        if (tokenString == null)
            return;
        if (!initialized)
            init();
        String digest = toDigest(tokenString);
        if (atCapacity) {
            cSet.remove(clist.get(lastPosition));
        }
        cSet.add(digest);

        // use rotating list so we can remove eldest entries once capacity is reached.
        clist.add(lastPosition++, digest);
        if (lastPosition >= maxSize) {
            lastPosition = 0;
            atCapacity = true;
        }

    }

    static int getSize() { // unit testing
        return cSet.size();
    }

    public static synchronized boolean contains(String tokenString) {
        if (!initialized)
            return false;
        return tokenString == null ? false : cSet.contains(toDigest(tokenString));

    }

}
