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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 *
 */
public class LoggedOutJwtSsoCookieCacheTest {

    // submit 10100 entries, first 100 should be flushed, last 1000 should be retrievable.
    @Test
    public void testCacheWhenMaxSizeReached() {
        for (int i = 1; i <= 10100; i++) {
            String entry = "String000000" + i;
            LoggedOutJwtSsoCookieCache.put(entry);
            System.out.println(entry);
        }
        for (int i = 1; i <= 100; i++) {
            String entry = "String000000" + i;
            assertFalse("entry " + entry + " should have been flushed from cache, but was not",
                        LoggedOutJwtSsoCookieCache.contains(entry));
        }

        for (int i = 101; i <= 10100; i++) {
            String entry = "String000000" + i;
            assertTrue("entry " + entry + " should have been in  cache, but was not", LoggedOutJwtSsoCookieCache.contains(entry));
        }
    }

    // check that map isn't growing uncontrollably
    @Test
    public void testMapSize() {
        for (int i = 1; i < 50000; i++) {
            String entry = "String000000" + i;
            LoggedOutJwtSsoCookieCache.put(entry);
        }
        int setSize = LoggedOutJwtSsoCookieCache.getSetSize();
        assertTrue("Set  should not be growing above max size but is " + setSize, setSize <= 10000);

    }

}
