/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import java.util.Locale;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TimestampUtilsTest {
    static Locale saveLocale;

    @BeforeClass
    public static void setUpBeforeClass() {
        saveLocale = Locale.getDefault();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        Locale.setDefault(saveLocale);
    }

    @Test
    public void getElapsedTimeAsStringFromMilliInterval_English() throws Exception {
        Locale.setDefault(new Locale("en"));
        String processedTime = TimestampUtils.getElapsedTimeAsStringFromMilliInterval(0);
        Assert.assertEquals("time interval converted incorrectly", "0.000", processedTime);

        processedTime = TimestampUtils.getElapsedTimeAsStringFromMilliInterval(1);
        Assert.assertEquals("time interval converted incorrectly", "0.001", processedTime);

        processedTime = TimestampUtils.getElapsedTimeAsStringFromMilliInterval(10);
        Assert.assertEquals("time interval converted incorrectly", "0.010", processedTime);

        processedTime = TimestampUtils.getElapsedTimeAsStringFromMilliInterval(100);
        Assert.assertEquals("time interval converted incorrectly", "0.100", processedTime);

        processedTime = TimestampUtils.getElapsedTimeAsStringFromMilliInterval(1010);
        Assert.assertEquals("time interval converted incorrectly", "1.010", processedTime);

        processedTime = TimestampUtils.getElapsedTimeAsStringFromMilliInterval(10000000);
        Assert.assertEquals("time interval converted incorrectly", "10000.000", processedTime);
    }

    @Test
    public void getElapsedTimeAsStringFromMilliInterval_Polish() throws Exception {
        Locale.setDefault(new Locale("pl"));
        String processedTime = TimestampUtils.getElapsedTimeAsStringFromMilliInterval(0);
        Assert.assertEquals("time interval converted incorrectly", "0,000", processedTime);

        processedTime = TimestampUtils.getElapsedTimeAsStringFromMilliInterval(1);
        Assert.assertEquals("time interval converted incorrectly", "0,001", processedTime);

        processedTime = TimestampUtils.getElapsedTimeAsStringFromMilliInterval(10);
        Assert.assertEquals("time interval converted incorrectly", "0,010", processedTime);

        processedTime = TimestampUtils.getElapsedTimeAsStringFromMilliInterval(100);
        Assert.assertEquals("time interval converted incorrectly", "0,100", processedTime);

        processedTime = TimestampUtils.getElapsedTimeAsStringFromMilliInterval(1010);
        Assert.assertEquals("time interval converted incorrectly", "1,010", processedTime);

        processedTime = TimestampUtils.getElapsedTimeAsStringFromMilliInterval(10000000);
        Assert.assertEquals("time interval converted incorrectly", "10000,000", processedTime);
    }
}
