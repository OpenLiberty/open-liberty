/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry.basic.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import org.junit.Test;

/**
 *
 */
public class BasicPasswordTest {

    /**
     * Test method for constructor with null
     */
    @Test
    public void testConstructorNull() {
        try {
            BasicPassword bp = new BasicPassword(null, true);
            assertNotNull("Even a given parameter is null with 2nd parameter is set as true, an object needs to be constructed properly", bp);
            bp = new BasicPassword(null, false);
            assertNotNull("Even a given parameter is null with 2nd parameter is set as false, an object needs to be constructed properly", bp);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test method for constructor with empty string
     */
    @Test
    public void testConstructorEmpty() {
        try {
            BasicPassword bp = new BasicPassword("", true);
            assertNotNull("Even a given parameter is an empty string, an object needs to be constructed properly", bp);
            bp = new BasicPassword("", false);
            assertNotNull("Even a given parameter is an empty string, an object needs to be constructed properly", bp);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test method for constructor with valid value, no 2nd parameter.
     */
    @Test
    public void testConstructorOneParameter() {
        String str = "password";
        BasicPassword bp = new BasicPassword(str);
        assertNotNull(bp);
        assertTrue(Arrays.equals(str.toCharArray(), bp.getPassword().getChars()));
        assertNull("if a constructor which takes one pamameter is used, getHashedPassword should return null", bp.getHashedPassword());
        assertFalse("If a constructor which takes one parameter is used, isHashed is set as false", bp.isHashed());
    }

    /**
     * Test method for constructor with valid values, 2nd parameter.
     */
    @Test
    public void testConstructorTwoParametersPlain() {
        String str = "password";
        BasicPassword bp = new BasicPassword(str, false);
        assertNotNull(bp);
        assertTrue(Arrays.equals(str.toCharArray(), bp.getPassword().getChars()));
        assertNull("getHashedPassword should return null", bp.getHashedPassword());
        assertFalse("isHashed is set as false", bp.isHashed());
    }

    /**
     * Test method for constructor with valid values, 2nd parameter.
     */
    @Test
    public void testConstructorTwoParametersHashed() {
        String str = "hashedpassword";
        BasicPassword bp = new BasicPassword(str, true);
        assertNotNull(bp);
        assertEquals(str, bp.getHashedPassword());
        assertNull("getPassword should return null", bp.getPassword());
        assertTrue("isHashed is set as false", bp.isHashed());
    }

}
