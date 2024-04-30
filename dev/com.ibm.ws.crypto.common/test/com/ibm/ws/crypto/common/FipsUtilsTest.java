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

    private static final String FIPS_PROVIDER_PROPERTY = "com.ibm.jsse2.usefipsprovider";
    
    private String usefipsproviderOrig;

    @Before
    public void beforeTest() {
        usefipsproviderOrig = System.getProperty(FIPS_PROVIDER_PROPERTY);
    }

    @After
    public void afterTest() {
        if (usefipsproviderOrig == null) {
            System.clearProperty(FIPS_PROVIDER_PROPERTY);
        } else {
            System.setProperty(FIPS_PROVIDER_PROPERTY, usefipsproviderOrig);
        }
    }

    @Test
    public void testIsFIPSEnabledFalse() {
        System.setProperty("com.ibm.jsse2.usefipsprovider", "false");
        assertFalse("isFIPSEnabled() is true", FipsUtils.isFIPSEnabled());
    }

    @Test
    public void testIsFIPSEnabledFalsePropNotSet() {
        System.clearProperty("com.ibm.jsse2.usefipsprovider");
        assertFalse("isFIPSEnabled() is true", FipsUtils.isFIPSEnabled());
    }

    @Test
    public void testIsFIPSEnabledTrue() {
        System.setProperty("com.ibm.jsse2.usefipsprovider", "true");
        assertTrue("isFIPSEnabled() is false", FipsUtils.isFIPSEnabled());
    }
}
