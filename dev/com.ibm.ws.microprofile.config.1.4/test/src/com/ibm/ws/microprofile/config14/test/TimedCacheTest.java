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
package com.ibm.ws.microprofile.config14.test;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.Test;

import com.ibm.ws.microprofile.config14.impl.TimedCache;

public class TimedCacheTest {

    @Test
    public void testCaching() throws InterruptedException {
        TimedCache<String, String> cache = new TimedCache<>(null, 500, TimeUnit.MILLISECONDS);
        AtomicReference<String> suffix = new AtomicReference<>("bar");
        Function<String, String> lookupFunction = k -> k + suffix.get();

        assertEquals("foobar", cache.get("foo", lookupFunction));

        suffix.set("baz");

        assertEquals("foobar", cache.get("foo", lookupFunction));
    }

    @Test
    public void testCacheExpiry() throws InterruptedException {
        TimedCache<String, String> cache = new TimedCache<>(null, 500, TimeUnit.MILLISECONDS);
        AtomicReference<String> suffix = new AtomicReference<>("bar");
        Function<String, String> lookupFunction = k -> k + suffix.get();

        assertEquals("foobar", cache.get("foo", lookupFunction));

        suffix.set("baz");

        Thread.sleep(1000); // Allow cache to expire
        assertEquals("foobaz", cache.get("foo", lookupFunction));
    }

    @Test
    public void testCachingDisabled() {
        TimedCache<String, String> cache = new TimedCache<>(null, 0, TimeUnit.MILLISECONDS);
        AtomicReference<String> suffix = new AtomicReference<>("bar");
        Function<String, String> lookupFunction = k -> k + suffix.get();

        assertEquals("foobar", cache.get("foo", lookupFunction));

        suffix.set("baz");

        assertEquals("foobaz", cache.get("foo", lookupFunction));
    }

    @Test
    public void testCachingNegative() {
        // Negative delay should be equivalent to zero, i.e. no caching
        TimedCache<String, String> cache = new TimedCache<>(null, -3000, TimeUnit.MILLISECONDS);
        AtomicReference<String> suffix = new AtomicReference<>("bar");
        Function<String, String> lookupFunction = k -> k + suffix.get();

        assertEquals("foobar", cache.get("foo", lookupFunction));

        suffix.set("baz");

        assertEquals("foobaz", cache.get("foo", lookupFunction));
    }

}
