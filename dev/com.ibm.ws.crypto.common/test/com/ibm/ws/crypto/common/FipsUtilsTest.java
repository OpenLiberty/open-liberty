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
        FipsUtils.unitTest = true;
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
        FipsUtils.FIPSLevel = FipsUtils.getFipsLevel();
        assertFalse("isFips140_3Enabled() is true", FipsUtils.isFips140_3Enabled());
    }

    @Test
    public void testIsFIPSEnabledFalsePropNotSet() {
        System.clearProperty(FIPS_MODE_PROPERTY);
        FipsUtils.FIPSLevel = FipsUtils.getFipsLevel();
        assertFalse("isFips140_3Enabled() is true", FipsUtils.isFips140_3Enabled());
    }

    @Test
    public void testIsFIPSEnabledTrue() {
        System.setProperty(FIPS_MODE_PROPERTY, "140-3");
        FipsUtils.FIPSLevel = FipsUtils.getFipsLevel();
        assertTrue("isFips140_3Enabled() is false", FipsUtils.isFips140_3Enabled());
    }
}
