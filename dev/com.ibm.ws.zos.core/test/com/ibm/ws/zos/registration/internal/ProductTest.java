/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.registration.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

/**
 * Tests the Product class
 */
public class ProductTest {

    private static SharedOutputManager outputMgr;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() {
        // Restore stdout and stderr to normal behavior
        outputMgr.restoreStreams();
    }

    /**
     * Test constructor
     */
    @Test
    public void testProductCtor() {
        Properties prop = new Properties();
        Product p = new Product(prop);
        assertEquals(Product.UNKNOWN, p.productID());
        assertEquals(Product.UNKNOWN, p.owner());
        assertEquals(Product.UNKNOWN, p.name());
        assertEquals(Product.UNKNOWN, p.version());
        assertEquals(Product.UNKNOWN, p.pid());
        assertEquals(Product.UNKNOWN, p.qualifier());
        assertEquals(null, p.replaces());
        assertEquals("false", p.gssp());
    }

    /**
     * New one up, set the attributes, get 'em back..basic stuff
     */
    @Test
    public void testProduct() {
        Properties prop = new Properties();
        prop.setProperty(Product.PRODUCTID, "PRODID");
        prop.setProperty(Product.OWNER, "OWNER");
        prop.setProperty(Product.NAME, "NAME");
        prop.setProperty(Product.VERSION, "8.5.0.0");
        prop.setProperty(Product.PID, "1234");
        prop.setProperty(Product.QUALIFIER, "qual");
        prop.setProperty(Product.REPLACES, "test");
        prop.setProperty(Product.GSSP, "true");
        Product p = new Product(prop);
        assertEquals("PRODID", p.productID());
        assertEquals("OWNER", p.owner());
        assertEquals("NAME", p.name());
        assertEquals("8.5", p.versionRelease());
        assertEquals("8.5.0.0", p.version());
        assertEquals("1234", p.pid());
        assertEquals("qual", p.qualifier());
        assertEquals("test", p.replaces());
        assertEquals("true", p.gssp());
    }

    /**
     * Now pound on the version getter to make sure it parses correctly
     */
    @Test
    public void testVersion() {
        Properties prop = new Properties();
        // Cover the easy stuff... common case (8.5.0.0) is covered in mainline test above
        prop.setProperty(Product.VERSION, "8");
        Product p = new Product(prop);
        assertEquals("8", p.versionRelease());

        prop.setProperty(Product.VERSION, "8.5");
        p = new Product(prop);
        assertEquals("8.5", p.versionRelease());

        prop.setProperty(Product.VERSION, "8.5.0");
        p = new Product(prop);
        assertEquals("8.5", p.versionRelease());

        // Now the weird stuff
        prop.setProperty(Product.VERSION, "12345678");
        p = new Product(prop);
        assertEquals("12345678", p.versionRelease());

        prop.setProperty(Product.VERSION, "........");
        p = new Product(prop);
        assertEquals("........", p.versionRelease());

        prop.setProperty(Product.VERSION, "");
        p = new Product(prop);
        assertEquals("", p.versionRelease());

        prop.setProperty(Product.VERSION, ".1.2.3");
        p = new Product(prop);
        assertEquals(".1", p.versionRelease());

        prop.setProperty(Product.VERSION, "..123");
        p = new Product(prop);
        assertEquals(".", p.versionRelease());

        prop.setProperty(Product.VERSION, "10.123.4.5");
        p = new Product(prop);
        assertEquals("10.123", p.versionRelease());

    }

    /**
     * Test that validateAndTranslate truncates properly.
     */
    @Test
    public void testValidateAndTranslate() throws Exception {

        Product prod = new Product(new Properties());
        assertArrayEquals("TestName".getBytes("Cp1047"), prod.validateAndTranslate("TestName", 8, "Parm1"));
        assertArrayEquals("TestName".getBytes("Cp1047"), prod.validateAndTranslate("TestNameTruncated", 8, "Parm1"));
    }

    /**
     * Tests Product byte array getters. These getters call validateAndTranslate to get an EBCDIC
     * representation of the values being retrieved.
     *
     * @throws Exception
     */
    @Test
    public void testEbcdicByteArrayGetters() throws Exception {
        Product prod = new Product("productID", "theOwnerOfThisProduct", "theNameOfThisProduct", "productVersion", "PID", "productQualifier", "productReplacement", "false");
        assertArrayEquals("theOwnerOfThisPr".getBytes("Cp1047"), prod.getOwnerBytes());
        assertArrayEquals("theNameOfThisPro".getBytes("Cp1047"), prod.getNameBytes());
        assertArrayEquals("PID".getBytes("Cp1047"), prod.getPidBytes());
        assertArrayEquals("productQ".getBytes("Cp1047"), prod.getQualifierBytes());

        // Note that version is a special one. GSSP is set to false so the version is passed in.
        assertArrayEquals("productV".getBytes("Cp1047"), prod.getVersionBytes());
    }

    /**
     * Tests Product byte array getters. These getters call validateAndTranslate to get an EBCDIC
     * representation of the values being retrieved.
     *
     * @throws Exception
     */
    @Test
    public void testEbcdicByteArrayGettersGSSP() throws Exception {
        Product prod = new Product("productID", "theOwnerOfThisProduct", "theNameOfThisProduct", "productVersion", "PID", "productQualifier", "productReplacement", "true");
        assertArrayEquals("theOwnerOfThisPr".getBytes("Cp1047"), prod.getOwnerBytes());
        assertArrayEquals("theNameOfThisPro".getBytes("Cp1047"), prod.getNameBytes());
        assertArrayEquals("PID".getBytes("Cp1047"), prod.getPidBytes());
        assertArrayEquals("productQ".getBytes("Cp1047"), prod.getQualifierBytes());

        // Note that version is a special one. GSSP is set to true so NOTUSAGE is passed in.
        assertArrayEquals("NOTUSAGE".getBytes("Cp1047"), prod.getVersionBytes());
    }

    /**
     * Tests for any issues with toString (i.e. NPEs).
     *
     * @throws Exception
     */
    @Test
    public void testToString() throws Exception {
        Product prod = new Product(new Properties());
        assertArrayEquals("UNKNOWN".getBytes("Cp1047"), prod.getOwnerBytes());
        assertArrayEquals("UNKNOWN".getBytes("Cp1047"), prod.getNameBytes());
        String output = prod.toString();
        assertTrue(output != null);
        assertTrue(output.length() != 0);
    }
}
