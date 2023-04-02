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

import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.junit.Test;

/**
 *
 */
public class JAXRSOutInterceptorTest {

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

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).withZone(ZoneId.of("GMT"));

        assertEquals(expected, dateFormatter.format(Instant.ofEpochMilli(now)));
    }

}
