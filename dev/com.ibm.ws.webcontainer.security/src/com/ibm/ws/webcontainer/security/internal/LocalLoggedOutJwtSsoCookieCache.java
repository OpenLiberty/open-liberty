/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.ibm.ws.ffdc.FFDCSelfIntrospectable;

/**
 * A class to "remember" which jwtsso cookies have been logged out using a local cache.
 * Allows for defense against cookie hijacking attacks,
 * reuse of a previously logged out cookie value can be detected and authentication refused.
 *
 * todo: could also use security/common/structures/cache.java, but that's heavier weight.
 */
public class LocalLoggedOutJwtSsoCookieCache implements FFDCSelfIntrospectable {
    private ArrayList<String> clist = null;
    private Set<String> cSet = null;
    private static int maxSize = 10000;
    private int lastPosition = 0;
    private boolean atCapacity = false;
    private Object lock = null;

    LocalLoggedOutJwtSsoCookieCache() {
        clist = new ArrayList<String>();
        cSet = new HashSet<String>();
        lock = new Object();
    }

    public void put(String tokenString) {
        if (tokenString == null)
            return;

        synchronized (lock) {
            String digest = tokenString;
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

    int getSetSize() { // For unit testing
        return cSet.size();
    }

    public boolean contains(String tokenString) {
        synchronized (lock) {
            return tokenString == null ? false : cSet.contains(tokenString);
        }
    }

    @Override
    public String[] introspectSelf() {
        /*
         * Don't dump out the list or set, as they contain sensitive info.
         */
        return new String[] { "clist.size() = " + clist.size(),
                              "cSet.size() = " + cSet.size(),
                              "maxSize = " + lastPosition,
                              "lastPosition = " + lastPosition,
                              "atCapacity = " + atCapacity,
                              "lock = " + lock };
    }
}
