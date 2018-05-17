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

import java.io.UnsupportedEncodingException;
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
    private static int maxSize = 10000;
    private static int lastPosition = 0;
    private static boolean atCapacity = false;
    private static boolean initialized = false;
    private static MessageDigest md = null;
    private static Object lock = null;

    private LoggedOutJwtSsoCookieCache() {}

    private static void init() {
        clist = new ArrayList<String>();
        cSet = new HashSet<String>();
        lock = new Object();
        try {
            md = MessageDigest.getInstance("SHA-1"); // nice short string
        } catch (Exception e) {
        }
        initialized = true;
    }

    // store as digests to save space
    private static String toDigest(String input) {
        md.reset();
        try {
            md.update(input.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
        }
        String result = com.ibm.ws.common.internal.encoder.Base64Coder.base64EncodeToString(md.digest());
        //String result = Base64.getEncoder().encodeToString(md.digest());  // java8 only
        return result;
    }

    public static void put(String tokenString) {
        if (tokenString == null)
            return;
        if (!initialized)
            init();
        synchronized (lock) {
            String digest = toDigest(tokenString);
            if (atCapacity) {
                cSet.remove(clist.get(lastPosition));
                clist.set(lastPosition, digest); // overwrite eldest entry
            } else {
                clist.add(digest);
            }
            cSet.add(digest);
            lastPosition++;
            // use rotating list so we can remove eldest entries once capacity is reached.
            if (lastPosition >= maxSize) {
                lastPosition = 0;
                atCapacity = true;
            }
        }
    }

    static int getSetSize() { // unit testing
        return cSet.size();
    }

    public static boolean contains(String tokenString) {
        if (!initialized)
            return false;
        synchronized (lock) {
            return tokenString == null ? false : cSet.contains(toDigest(tokenString));
        }
    }

}
