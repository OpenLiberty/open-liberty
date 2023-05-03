/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxrs.utils.test;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import org.apache.cxf.jaxrs.interceptor.CachedTimeAccessor;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.junit.Test;

import junit.framework.Assert;

public class JAXRSOutInterceptorTest {

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM uuuu HH:mm:ss zzz", Locale.US).withZone(ZoneId.of("GMT"));

    /**
     * This test validated that the HttpUtils.getHttpDateFormat previously used in CachedTime
     * doesn't change its format.  We now use a DateTimeFormatter to avoid the overhead of
     * SimpleDateFormat and we want the DateTimeFormatter settings to match any changes done
     * in HttpUtils.getHttpDateFormat. So if this test fails, we need to update the DateTimeFormatter
     * config in CachedTime.
     */
    @Test
    public void testDateFormat() {
        SimpleDateFormat sdf = HttpUtils.getHttpDateFormat();
        long now = System.currentTimeMillis();
        String expected = sdf.format(new Date(now));

        assertEquals(expected, dateFormatter.format(Instant.ofEpochMilli(now)));
    }

    /**
     * Validate that CachedTime works correctly to return a cached formatted String if the seconds is the same as the previous
     * time, i.e. the CachedTime function is working and not formatting the time over and over.  This is single threaded.  In a
     * multi-threaded scenario there is a window where a cached String may not be returned if two threads come in at the same time
     * and both try to update the cached String.
     */
    @Test
    public void testCachedTime() {
        long start = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        String previousCachedString = CachedTimeAccessor.getTimeString(now);
        long previousNow = now;
        while (now - start < 5000) {
            String cachedString = CachedTimeAccessor.getTimeString(now);
            String newString = dateFormatter.format(Instant.ofEpochMilli(now));
            Assert.assertEquals("Strings are different than expected ", newString, cachedString);
            if ((now - (now % 1000)) == (previousNow - (previousNow % 1000))) {
                Assert.assertSame(previousCachedString, cachedString);
            }
            previousCachedString = cachedString;
            previousNow = now;
            now = System.currentTimeMillis();
        }

    }

}
