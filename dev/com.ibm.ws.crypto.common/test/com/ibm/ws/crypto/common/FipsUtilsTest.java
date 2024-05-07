/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.crypto.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FipsUtilsTest {

    private static final String FIPS_MODE_PROPERTY = "com.ibm.fips.mode";
    
    private String fipsModeOrig;

    @Before
    public void beforeTest() {
        fipsModeOrig = System.getProperty(FIPS_MODE_PROPERTY);
    }

    @After
    public void afterTest() {
        if (fipsModeOrig == null) {
            System.clearProperty(FIPS_MODE_PROPERTY);
        } else {
            System.setProperty(FIPS_MODE_PROPERTY, fipsModeOrig);
        }
    }

    @Test
    public void testIsFIPSEnabledFalse() {
        System.setProperty(FIPS_MODE_PROPERTY, "140-2");
        assertFalse("isRunningFIPS140Dash3Mode() is true", FipsUtils.isRunningFIPS140Dash3Mode());
    }

    @Test
    public void testIsFIPSEnabledFalsePropNotSet() {
        System.clearProperty(FIPS_MODE_PROPERTY);
        assertFalse("isRunningFIPS140Dash3Mode() is true", FipsUtils.isRunningFIPS140Dash3Mode());
    }

    @Test
    public void testIsFIPSEnabledTrue() {
        System.setProperty(FIPS_MODE_PROPERTY, "140-3");
        assertTrue("isRunningFIPS140Dash3Mode() is false", FipsUtils.isRunningFIPS140Dash3Mode());
    }
}
