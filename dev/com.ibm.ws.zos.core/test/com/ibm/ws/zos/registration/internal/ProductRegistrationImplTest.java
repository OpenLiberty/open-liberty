/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.registration.internal;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.zos.jni.NativeMethodManager;

import test.common.SharedOutputManager;

public class ProductRegistrationImplTest {

    private static SharedOutputManager outputMgr;

    final Mockery context = new JUnit4Mockery();

    class TestProductRegistrationImpl extends ProductRegistrationImpl {

        @Override
        protected int ntv_registerProduct(byte[] jowner, byte[] jname, byte[] jversion, byte[] jid, byte[] jqualifier) {
            return 0;
        }

    }

    class TestProduct extends Product {

        public TestProduct(String prodId, String owner, String name, String version, String pid, String qualifier, String replaces, String gssp) {
            super(prodId, owner, name, version, pid, qualifier, replaces, gssp);
        }
    }

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

    @After
    public void tearDown() {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * Test RegisterProduct( )
     */
    @Test
    public void testRegisterProduct() {
        TestProductRegistrationImpl testProductRegistrationImpl = new TestProductRegistrationImpl();

        // Pass strings that are too long and also empty and null
        testProductRegistrationImpl.registerProduct(new Product(null, "", null, "8.5", "5655-W65123456", "WAS Z/OS", null, "false"));
        assertEquals(true, outputMgr.checkForMessages("CWWKB0108I:"));

    }

    /**
     * Test RegisterProduct( )
     */
    @Test
    public void testRegisterProduct2() {
        TestProductRegistrationImpl testProductRegistrationImpl = new TestProductRegistrationImpl() {

            @Override
            protected int ntv_registerProduct(byte[] jowner, byte[] jname, byte[] jversion, byte[] jid, byte[] jqualifier) {
                return 4;
            }

        };

        // Pass strings that are too long and also empty and null
        testProductRegistrationImpl.registerProduct(new Product(null, "", null, "8.5", "5655-W65123456", "WAS Z/OS", null, "false"));

        assertEquals(true, outputMgr.checkForMessages("CWWKB0108I:"));

    }

    /**
     * Test method for
     * {@link com.ibm.ws.zos.registration.internal.ProductRegistrationImpl#registerProduct(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}
     * .
     *
     * Test native code returning a '8'
     */
    @Test
    public void testRegisterProduct3() {

        final TestProductRegistrationImpl testProductRegistrationImpl = new TestProductRegistrationImpl() {

            @Override
            // called by start...don't do anything in this test
            protected int ntv_registerProduct(byte[] jowner, byte[] jname, byte[] jversion, byte[] jid, byte[] jqualifier) {
                return 8;
            }

        };

        // Pass strings that are too long and also empty and null
        testProductRegistrationImpl.registerProduct(new Product(null, "", null, "8.5", "5655-W65123456", "WAS Z/OS", null, "false"));
        assertEquals(true, outputMgr.checkForStandardErr("CWWKB0109E:"));

    }

    /**
     * Test method for
     * {@link com.ibm.ws.zos.registration.internal.ProductRegistrationImpl#registerProduct(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}
     * .
     *
     * Test a null return from validateAndTranslate
     */
    @Test
    public void testRegisterProduct4() {

        final TestProduct product = new TestProduct(null, "", null, "8.5", "5655-W65123456", "WAS Z/OS", null, "false") {
            @Override
            protected byte[] validateAndTranslate(String str, int requiredLength, String parm) {
                return null;
            }
        };

        new TestProductRegistrationImpl().registerProduct(product);

        assertEquals(true, outputMgr.checkForStandardErr("CWWKB0107E:"));

    }

    /**
     * Test method for RegisterProduct(Product p)
     *
     */
    @Test
    public void testRegisterProduct5() {

        final TestProductRegistrationImpl testProductRegistrationImpl = new TestProductRegistrationImpl() {

            @Override
            // called by start...don't do anything in this test
            protected int ntv_registerProduct(byte[] jowner, byte[] jname, byte[] jversion, byte[] jid, byte[] jqualifier) {
                return 0;
            }

        };

        // Just new up an empty Product..  all strings are UNKNOWN
        Properties prop = new Properties();
        Product prod = new Product(prop);
        testProductRegistrationImpl.registerProduct(prod);
        assertEquals(true, outputMgr.checkForMessages("CWWKB0108I:"));

    }

    /**
     * Test initialize method
     */
    @Test
    public void testInitialize() {
        final NativeMethodManager nativeMethodManager = context.mock(NativeMethodManager.class);
        context.checking(new Expectations() {
            {
                oneOf(nativeMethodManager).registerNatives(with(ProductRegistrationImpl.class));
            }
        });
        ProductRegistrationImpl productRegistrationImpl = new ProductRegistrationImpl();
        productRegistrationImpl.initialize(nativeMethodManager);

    }

    /**
     * Test ProductRegistrationImpl.deregisterProduct(Product) where the native code returns a negative number.
     * The test should issue deregistration error message: CWWKB0114E.
     */
    @Test
    public void testDeregisterProductFailure() {

        final TestProductRegistrationImpl testProductRegistrationImpl = new TestProductRegistrationImpl() {

            @Override
            // called by start...don't do anything in this test
            protected int ntv_deregisterProduct(byte[] jowner, byte[] jname, byte[] jversion, byte[] jid, byte[] jqualifier) {
                return -8;
            }
        };

        // Create a product and set the registered flag to true. Products that were not originally registered are
        // ignored during deregistration.
        Product product = new Product(null, "", null, "8.5", "5655-W65123456", "WAS Z/OS", null, "false");
        product.setRegistered(true);
        testProductRegistrationImpl.deregisterProduct(product);
        assertEquals(true, outputMgr.checkForStandardErr("CWWKB0114E:"));
    }

    /**
     * Test ProductRegistrationImpl.deregisterProduct(Product) where the native code returns 0.
     * The test should issue deregistration success message: CWWKB0111I.
     */
    @Test
    public void testDeregisterProductSuccess() {

        final TestProductRegistrationImpl testProductRegistrationImpl = new TestProductRegistrationImpl() {

            @Override
            // called by start...don't do anything in this test
            protected int ntv_deregisterProduct(byte[] jowner, byte[] jname, byte[] jversion, byte[] jid, byte[] jqualifier) {
                return 0;
            }
        };

        // Create a product and set the registered flag to true. Products that were not originally registered are
        // ignored during deregistration.
        Product product = new Product(null, "", null, "8.5", "5655-W65123456", "WAS Z/OS", null, "false");
        product.setRegistered(true);
        testProductRegistrationImpl.deregisterProduct(product);
        assertEquals(true, outputMgr.checkForMessages("CWWKB0111I:"));
    }

}
