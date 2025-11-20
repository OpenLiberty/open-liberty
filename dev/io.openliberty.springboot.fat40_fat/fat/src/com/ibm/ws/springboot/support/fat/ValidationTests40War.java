/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class ValidationTests40War extends ValidationAbstractTests {
    @Test
    public void testMonday() throws Exception {
        HttpUtils.findStringInUrl(server, "testName/name-for-day?dayOfWeek=1", "Monday");
    }

    @Test
    public void testSunday() throws Exception {
        HttpUtils.findStringInUrl(server, "testName/name-for-day?dayOfWeek=7", "Sunday");
    }

    // getHttpResponseAsString always adds LS to the returned value
    private final static String LS = System.getProperty("line.separator");

    @Test
    public void testZero() throws Exception {
        String response = HttpUtils.getHttpResponseAsString(server, "testName/name-for-day?dayOfWeek=0");
        assertEquals("Wrong response", "must be greater than or equal to 1" + LS, response);
    }

    @Test
    public void testEight() throws Exception {
        String response = HttpUtils.getHttpResponseAsString(server, "testName/name-for-day?dayOfWeek=8");
        assertEquals("Wrong response", "must be less than or equal to 7" + LS, response);
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("servlet-6.1", "componenttest-2.0", "validation-3.1"));
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.WEB_APP_TAG;
    }
}
