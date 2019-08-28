/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Timestamp;
import java.text.ParseException;

import org.junit.Test;

/**
 *
 */
public class DateUtilTest {

    @Test(expected = IllegalArgumentException.class)
    public void parseRFC2616NullTime() {
        DateUtil.parseTimeRFC2616(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseRFC822NullTime() throws ParseException {
        DateUtil.parseTimeRFC822(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseRFC850NullTime() throws ParseException {
        DateUtil.parseTimeRFC850(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseANSINullTime() throws ParseException {
        DateUtil.parseTimeANSIC(null);
    }

    @Test
    public void parseTimeRFC822() {
        Timestamp timestamp = DateUtil.parseTimeRFC2616("Sat, 29 Oct 1994 19:43:31 GMT");
        assertNotNull(timestamp);
        assertEquals(timestamp.toGMTString(), "29 Oct 1994 19:43:31 GMT");
    }

    @Test
    public void parseTimeRFC850() {
        Timestamp timestamp = DateUtil.parseTimeRFC2616("Sunday, 06-Nov-94 08:49:37 GMT");
        assertNotNull(timestamp);
        assertEquals(timestamp.toGMTString(), "6 Nov 1994 08:49:37 GMT");
    }

    @Test
    public void parseTimeANSI() {
        Timestamp timestamp = DateUtil.parseTimeRFC2616("Sun Nov  6 08:49:37 1994");
        assertNotNull(timestamp);
        assertEquals(timestamp.toGMTString(), "6 Nov 1994 08:49:37 GMT");
    }

    @Test
    public void parseInvalidTime() {
        Timestamp timestamp = DateUtil.parseTimeRFC2616("Sun Ced  30 08:49:37 1994");
        assertNull(timestamp);

    }
}
