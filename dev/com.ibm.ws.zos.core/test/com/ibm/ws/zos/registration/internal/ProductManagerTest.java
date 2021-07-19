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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.zos.jni.NativeMethodManager;

import test.common.SharedOutputManager;

/**
 *
 */
public class ProductManagerTest {

    private static SharedOutputManager outputMgr;

    final Mockery context = new JUnit4Mockery();

    final NativeMethodManager nativeMethodManager = context.mock(NativeMethodManager.class);

    public class TestProductRegistrationImpl extends ProductRegistrationImpl {
        public int deregistrationCallCount = 0;

        @Override
        public void initialize(NativeMethodManager n) {

        }

        @Override
        public void registerProduct(Product p) {

        }

        @Override
        public void deregisterProduct(Product p) {
            deregistrationCallCount++;
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
     * Test method for {@link com.ibm.ws.zos.registration.internal.ProductManager#ProductManager(com.ibm.ws.zos.jni.NativeMethodManager, org.osgi.framework.BundleContext)}.
     */
    @Test
    public void testProductManager() {
        addNewPMInstanceExpectations();
        new ProductManager(nativeMethodManager);
    }

    /**
     * Test method for {@link com.ibm.ws.zos.registration.internal.ProductManager#start()}.
     * getProductFiles returns null
     */
    @Test
    public void testStart1() {
        final BundleContext bundleContext = context.mock(BundleContext.class);
        addStartExpectations(bundleContext);

        final class TestProductManager extends ProductManager {

            public boolean getProductFilesCalled = false;

            public boolean getProductFilesCalled() {
                return getProductFilesCalled;
            }

            public boolean buildProductsCalled = false;

            public boolean buildProductsCalled() {
                return buildProductsCalled;
            }

            public TestProductManager(NativeMethodManager n) {
                super(n);
                // replace productRegistrationImpl with our own stubbed implementation
                productRegistrationImpl = new TestProductRegistrationImpl();
            }

            @Override
            protected String[] getProductFiles(String x) {
                getProductFilesCalled = true;
                return null;
            }

            @Override
            protected void buildProducts(List<String> s) {
                buildProductsCalled = true;
            }

        }
        TestProductManager testProductManager = new TestProductManager(nativeMethodManager);

        testProductManager.start(bundleContext);
        assertEquals(true, testProductManager.getProductFilesCalled());
        assertEquals(false, testProductManager.buildProductsCalled());
    }

    /**
     * Test method for {@link com.ibm.ws.zos.registration.internal.ProductManager#start()}.
     * getProductFiles returns zero files
     */
    @Test
    public void testStart2() {
        final BundleContext bundleContext = context.mock(BundleContext.class);
        addStartExpectations(bundleContext);

        final class TestProductManager extends ProductManager {

            public boolean getProductFilesCalled = false;

            public boolean getProductFilesCalled() {
                return getProductFilesCalled;
            }

            public boolean buildProductsCalled = false;

            public boolean buildProductsCalled() {
                return buildProductsCalled;
            }

            public TestProductManager(NativeMethodManager n) {
                super(n);
                // replace productRegistrationImpl with our own stubbed implementation
                productRegistrationImpl = new TestProductRegistrationImpl();
            }

            @Override
            protected String[] getProductFiles(String x) {
                getProductFilesCalled = true;
                return new String[0];
            }

            @Override
            protected void buildProducts(List<String> s) {
                buildProductsCalled = true;
            }

        }
        TestProductManager testProductManager = new TestProductManager(nativeMethodManager);

        testProductManager.start(bundleContext);
        assertEquals(true, testProductManager.getProductFilesCalled());
        assertEquals(false, testProductManager.buildProductsCalled());
    }

    /**
     * Test method for {@link com.ibm.ws.zos.registration.internal.ProductManager#start()}.
     * getProductFiles returns files
     */
    @Test
    public void testStart3() {
        final BundleContext bundleContext = context.mock(BundleContext.class);
        addStartExpectations(bundleContext);

        final class TestProductManager extends ProductManager {

            public boolean getProductFilesCalled = false;

            public boolean getProductFilesCalled() {
                return getProductFilesCalled;
            }

            public boolean buildProductsCalled = false;

            public boolean buildProductsCalled() {
                return buildProductsCalled;
            }

            public TestProductManager(NativeMethodManager n) {
                super(n);
                // replace productRegistrationImpl with our own stubbed implementation
                productRegistrationImpl = new TestProductRegistrationImpl();
            }

            @Override
            protected String[] getProductFiles(String x) {
                getProductFilesCalled = true;
                String[] y = { "A" };
                return y;
            }

            @Override
            protected void buildProducts(List<String> s) {
                buildProductsCalled = true;
            }

        }
        TestProductManager testProductManager = new TestProductManager(nativeMethodManager);
        testProductManager.start(bundleContext);
        assertEquals(true, testProductManager.getProductFilesCalled());
        assertEquals(true, testProductManager.buildProductsCalled());
    }

    /**
     * Test ProductManager.start() where there are no products to process and the server
     * is NOT authorized to deregister products.
     */
    @Test
    public void testStart4() {
        // Reset the stream to make sure we have a single message in the logs.
        outputMgr.restoreStreams();
        outputMgr.captureStreams();

        // Mock up the bundle context and add startup expectations.
        final BundleContext bundleContext = context.mock(BundleContext.class);
        addStartExpectations(bundleContext);

        // Wrap the ProductManager to set some data.
        final class TestProductManager extends ProductManager {

            public TestProductManager(NativeMethodManager n) {
                super(n);
                // replace productRegistrationImpl with our own stubbed implementation
                productRegistrationImpl = new TestProductRegistrationImpl();
            }

            @Override
            protected boolean isServerAuthorizedToDeregister(BundleContext bundleContext) {
                return false;
            }

        }

        // Calss start on the product manager. There shouldn't be any products to process.
        TestProductManager testProductManager = new TestProductManager(nativeMethodManager);
        testProductManager.start(bundleContext);

        // Make sure a default product was created.
        Product defaultProduct = testProductManager.baseProductMap.get(Product.DEFAULT_BASE_PRODUCTID);
        assertTrue(defaultProduct != null);
        assertTrue((defaultProduct.owner()).equals(Product.DEFAULT_BASE_PROD_OWNER));
        assertTrue((defaultProduct.name()).equals(Product.DEFAULT_BASE_PROD_NAME));
        assertTrue((defaultProduct.version()).equals(Product.DEFAULT_BASE_PROD_VERSION));
        assertTrue((defaultProduct.pid()).equals(Product.DEFAULT_BASE_PID));

        // isServerAuthorizedToDeregister() will return false. Make sure message:
        // CWWKB0113I (PRODUCT_REGISTRATION_SUMMARY_NOT_AUTHORIZED) is printed.
        assertEquals(true, outputMgr.checkForMessages("CWWKB0113I: The number of successfully registered products with z/OS is 0"));
    }

    /**
     * Test ProductManager.start() where there are products to process and the server
     * IS authorized to deregister products.
     */
    @Test
    public void testStart5() {
        // Reset the stream to make sure we have a single message in the logs.
        outputMgr.restoreStreams();
        outputMgr.captureStreams();

        // Mock up the bundle context and add startup expectations.
        final BundleContext bundleContext = context.mock(BundleContext.class);
        addStartExpectations(bundleContext);

        // Wrap the ProductManager to set some data.
        final class TestProductManager extends ProductManager {

            public TestProductManager(NativeMethodManager n) {
                super(n);
                // replace productRegistrationImpl with our own stubbed implementation
                productRegistrationImpl = new TestProductRegistrationImpl();
                baseProductMap.put("base", new Product(new Properties()));
                productMap.put("x", new Product(new Properties()));
                productMap.put("y", new Product(new Properties()));
                productMap.put("z", new Product(new Properties()));
            }

            @Override
            protected boolean isServerAuthorizedToDeregister(BundleContext bundleContext) {
                return true;
            }

            @Override
            protected void registerBaseProducts() {
                for (Product p : baseProductMap.values()) {
                    p.setRegistered(true);
                }
                super.registerBaseProducts();
            }

            @Override
            protected void registerProducts() {
                for (Product p : productMap.values()) {
                    p.setRegistered(true);
                }
                super.registerProducts();
            }
        }
        // There shouldn't be any products to process.
        TestProductManager testProductManager = new TestProductManager(nativeMethodManager);
        testProductManager.start(bundleContext);

        // Make sure a default product was created.
        assertTrue(testProductManager.baseProductMap.size() == 1);
        assertTrue(testProductManager.productMap.size() == 3);

        // isServerAuthorizedToDeregister() will return true. Make sure message:
        // CWWKB0113I (PRODUCT_REGISTRATION_SUMMARY_AUTHORIZED) is printed and shows 4 registered products (3 stack and 1 base).
        assertEquals(true, outputMgr.checkForMessages("CWWKB0112I: The number of successfully registered products with z/OS is 4."));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.registration.internal.ProductManager#stop(org.osgi.framework.BundleContext)}.
     * Where product deregistration does not take place because the server is not authorized to do so.
     */
    @Test
    public void testUnauthorizedDeregistrationDuringStop() {
        final BundleContext mockBundleContext = context.mock(BundleContext.class);

        context.checking(new Expectations() {
            {
                oneOf(nativeMethodManager).registerNatives(with(ProductRegistrationImpl.class));

                try {
                    oneOf(mockBundleContext).getServiceReferences(with(any(String.class)), with(any(String.class)));
                } catch (InvalidSyntaxException ise) {
                    // Nothing.
                }
            }
        });

        final class TestProductManager extends ProductManager {
            public TestProductManager(NativeMethodManager n) {
                super(n);
                // replace productRegistrationImpl with our own stubbed implementation
                productRegistrationImpl = new TestProductRegistrationImpl();
            }

            @Override
            protected boolean isServerAuthorizedToDeregister(BundleContext bundleContext) {
                return false;
            }

            protected int getDeregCount() {
                return ((TestProductRegistrationImpl) productRegistrationImpl).deregistrationCallCount;
            }
        }

        TestProductManager tpm = new TestProductManager(nativeMethodManager);
        tpm.stop(mockBundleContext);
        assertTrue(tpm.getDeregCount() == 0);
    }

    /**
     * Test method for {@link com.ibm.ws.zos.registration.internal.ProductManager#stop(org.osgi.framework.BundleContext)}.
     * Where product deregistration is attempted because the server is authorized to do so.
     */
    @Test
    public void testAuthorizedDeregistrationDuringStop() {
        final BundleContext mockBundleContext = context.mock(BundleContext.class);
        context.checking(new Expectations() {
            {
                oneOf(nativeMethodManager).registerNatives(with(ProductRegistrationImpl.class));

                try {
                    oneOf(mockBundleContext).getServiceReferences(with(any(String.class)), with(any(String.class)));
                } catch (InvalidSyntaxException ise) {
                    // Nothing.
                }
            }
        });

        final class TestProductManager extends ProductManager {
            public TestProductManager(NativeMethodManager n) {
                super(n);
                // replace productRegistrationImpl with our own stubbed implementation
                productRegistrationImpl = new TestProductRegistrationImpl();
                // Put something as baseServer.
                baseProductMap.put("base", new Product(new Properties()));
                // stuff something in the product map
                Properties prop = new Properties();
                Product p = new Product(prop);
                productMap.put("x", p);
                productMap.put("y", p);
                productMap.put("z", p);
            }

            @Override
            protected boolean isServerAuthorizedToDeregister(BundleContext bundleContext) {
                return true;
            }

            protected int getDeregCount() {
                return ((TestProductRegistrationImpl) productRegistrationImpl).deregistrationCallCount;
            }
        }

        TestProductManager tpm = new TestProductManager(nativeMethodManager);
        tpm.stop(mockBundleContext);
        assertTrue(tpm.getDeregCount() == 4);
    }

    /**
     * Test method for {@link com.ibm.ws.zos.registration.internal.ProductManager#registerBaseProducts()}.
     * Drives the path with a base product.. no base product is covered by start() tests above
     */
    @Test
    public void testRegisterBase() {
        addNewPMInstanceExpectations();

        final class TestProductManager extends ProductManager {
            public TestProductManager(NativeMethodManager n) {
                super(n);
                // replace productRegistrationImpl with our own stubbed implementation
                productRegistrationImpl = new TestProductRegistrationImpl();
            }
        }
        TestProductManager testProductManager = new TestProductManager(nativeMethodManager);
        Properties prop = new Properties();
        Product p = new Product(prop);
        testProductManager.baseProductMap.put("base", p);
        testProductManager.registerBaseProducts();

    }

    /**
     * Test method for {@link com.ibm.ws.zos.registration.internal.ProductManager#registerProducts()}.
     * Tests the path where we have a product... no products is covered under start()
     */
    @Test
    public void testRegisterProducts() {
        final BundleContext bundleContext = context.mock(BundleContext.class);
        context.checking(new Expectations() {
            {
                oneOf(nativeMethodManager).registerNatives(with(ProductRegistrationImpl.class));

                try {
                    oneOf(bundleContext).getServiceReferences(with(any(String.class)), with(any(String.class)));
                } catch (InvalidSyntaxException ise) {
                    // Nothing.
                }
            }
        });

        final class TestProductManager extends ProductManager {
            public TestProductManager(NativeMethodManager n) {
                super(n);
                // replace productRegistrationImpl with our own stubbed implementation
                productRegistrationImpl = new TestProductRegistrationImpl();
                // stuff something in the product map
                Properties prop = new Properties();
                Product p = new Product(prop);
                productMap.put("x", p);
            }
        }
        TestProductManager testProductManager = new TestProductManager(nativeMethodManager);
        testProductManager.registerProducts();
    }

    /**
     * Test method for {@link com.ibm.ws.zos.registration.internal.ProductManager#getProductFiles(java.lang.String)}.
     */
    @Test
    public void testGetProductFiles() {
        addNewPMInstanceExpectations();
        ProductManager productManager = new ProductManager(nativeMethodManager);

        // Read some made up directory so we probably won't get anything back
        // If we use a real directory, I can't be sure there won't be properties files
        // So the assert is for a null because the directory doesn't exist (I hope!)
        String[] x = productManager.getProductFiles("/bogus/dir");

        assertTrue(x == null ? true : false);
    }

    /**
     * Test method for {@link com.ibm.ws.zos.registration.internal.ProductManager#buildProducts(java.lang.String, java.lang.String[])}.
     */
    @Test
    public void testBuildProducts() {
        addNewPMInstanceExpectations();

        final class TestProductManager extends ProductManager {

            public boolean processFileCalled = false;

            public boolean getProcessFileCalled() {
                return processFileCalled;
            }

            public boolean resolveBaseProductsCalled = false;

            public boolean getResolveBaseProductsCalled() {
                return resolveBaseProductsCalled;
            }

            public TestProductManager(NativeMethodManager n) {
                super(n);
                // replace productRegistrationImpl with our own stubbed implementation
                productRegistrationImpl = new TestProductRegistrationImpl();
            }

            @Override
            protected Product processFile(String x) {
                processFileCalled = true;
                Properties prop = new Properties();
                prop.setProperty(Product.REPLACES, "another product");
                Product p = new Product(prop);
                return p;
            }

            @Override
            protected void resolveBaseProducts(Product currentBaseProduct, HashMap<String, ArrayList<Product>> replacesMap) {
                resolveBaseProductsCalled = true;
            }

        }
        TestProductManager testProductManager = new TestProductManager(nativeMethodManager);

        String[] strings = { "string1", "string2" };
        testProductManager.buildProducts(Arrays.asList(strings));
        assertEquals(true, testProductManager.getProcessFileCalled());
        assertEquals(true, testProductManager.getResolveBaseProductsCalled());

    }

    /**
     * Test method for {@link com.ibm.ws.zos.registration.internal.ProductManager#processFile(java.lang.String)}.
     */
    @Test
    public void testProcessFile() {
        addNewPMInstanceExpectations();

        final class TestProductManager extends ProductManager {

            public boolean readPropertiesCalled = false;

            public boolean getReadPropertiesCalled() {
                return readPropertiesCalled;
            }

            public TestProductManager(NativeMethodManager n) {
                super(n);
                // replace productRegistrationImpl with our own stubbed implementation
                productRegistrationImpl = new TestProductRegistrationImpl();
            }

            @Override
            protected Properties readProperties(String x) {
                readPropertiesCalled = true;
                return new Properties();
            }

        }
        TestProductManager testProductManager = new TestProductManager(nativeMethodManager);

        testProductManager.processFile("test");
        assertEquals(true, testProductManager.getReadPropertiesCalled());

    }

    /**
     * Test method for {@link com.ibm.ws.zos.registration.internal.ProductManager#readProperties(java.lang.String)}.
     * Just covers the file-not-found case
     */
    @Test
    public void testReadProperties() {
        addNewPMInstanceExpectations();

        final class TestProductManager extends ProductManager {

            public boolean getPropertiesFileCalled = false;

            public boolean getPropertiesFileCalled() {
                return getPropertiesFileCalled;
            }

            public TestProductManager(NativeMethodManager n) {
                super(n);
                // replace productRegistrationImpl with our own stubbed implementation
                productRegistrationImpl = new TestProductRegistrationImpl();
            }

            @Override
            protected FileInputStream getPropertiesFile(String x) {
                getPropertiesFileCalled = true;
                return null;
            }

        }
        TestProductManager testProductManager = new TestProductManager(nativeMethodManager);

        testProductManager.readProperties("test");
        assertEquals(true, testProductManager.getPropertiesFileCalled());

    }

    /**
     * Test method for {@link com.ibm.ws.zos.registration.internal.ProductManager#readProperties(java.lang.String)}.
     * Just covers the file-not-found case
     */
    @Test
    public void testReadProperties2() {
        addNewPMInstanceExpectations();
        ProductManager productManager = new ProductManager(nativeMethodManager);
        String filename = new String("testfile.properties");
        try {

            FileOutputStream fos = new FileOutputStream(filename);
            fos.write("some.property=somevalue".getBytes());
            fos.close();
        } catch (FileNotFoundException fnfe) {
            System.out.println("problem creating file " + filename + " exception " + fnfe);
        } catch (IOException ioe) {
            System.out.println("problem creating file " + filename + " exception " + ioe);
        }

        Properties prop = productManager.readProperties(filename);

        // Tidy up...
        File f = new File(filename);
        f.delete();

        assertEquals(prop.getProperty("some.property"), "somevalue");
    }

    /**
     * Test method for {@link com.ibm.ws.zos.registration.internal.ProductManager#resolveBaseProduct(java.util.HashMap)}.
     */
    @Test
    public void testResolveBaseProduct() {
        addNewPMInstanceExpectations();
        ProductManager productManager = new ProductManager(nativeMethodManager);
        HashMap<String, ArrayList<Product>> replacesMap = new HashMap<String, ArrayList<Product>>();
        Properties prop = new Properties();
        prop.setProperty(Product.NAME, "prod1");
        prop.setProperty(Product.PRODUCTID, Product.BASE_PRODUCTID);
        Product prod1 = new Product(prop);

        prop.setProperty(Product.NAME, "prod2");
        prop.setProperty(Product.PRODUCTID, "some product");
        Product prod2 = new Product(prop);

        productManager.productMap.put(Product.BASE_PRODUCTID, prod1); // prod1 is base product
        productManager.productMap.put(prod2.productID(), prod2); // prod2 is just in the map

        ArrayList<Product> replacementProducts = new ArrayList<Product>();
        replacementProducts.add(prod2);
        replacesMap.put(Product.BASE_PRODUCTID, replacementProducts); // prod2 replaces prod1

        productManager.resolveBaseProducts(prod1, replacesMap);

        assertTrue(productManager.baseProductMap.containsKey(prod2.productID));

    }

    /**
     * Test method for {@link com.ibm.ws.zos.registration.internal.ProductManager#resolveStackProduct(java.util.HashMap)}.
     */
    @Test
    public void testResolveStackProduct() {
        addNewPMInstanceExpectations();
        ProductManager productManager = new ProductManager(nativeMethodManager);
        HashMap<String, ArrayList<Product>> replacesMap = new HashMap<String, ArrayList<Product>>();
        Properties prop = new Properties();
        prop.setProperty(Product.NAME, "stackProd1");
        prop.setProperty(Product.PRODUCTID, "stack.prod.1");
        Product prod1 = new Product(prop);

        prop.setProperty(Product.NAME, "stackProd2");
        prop.setProperty(Product.PRODUCTID, "stack.prod.2");
        Product prod2 = new Product(prop);

        productManager.productMap.put("stack.prod.1", prod1); // prod1 is stack product
        productManager.productMap.put("stack.prod.2", prod2); // prod2 is stack product replacing prod1

        ArrayList<Product> replacementProducts = new ArrayList<Product>();
        replacementProducts.add(prod2);
        replacesMap.put("stack.prod.1", replacementProducts); // prod2 replaces prod1

        productManager.resolveStackProducts(replacesMap);

        // Make sure the product map contains prod2 and NOT prod1
        assertTrue(productManager.productMap.containsKey(prod2.productID));
        assertTrue(!(productManager.productMap.containsKey(prod1.productID)));

    }

    /**
     * Test method for {@link com.ibm.ws.zos.registration.internal.ProductManager#resolveBaseProduct(java.util.HashMap)}.
     * Tests scenario with multiple products replacing the base
     */
    @Test
    public void testResolveMultipleBaseProducts() {
        addNewPMInstanceExpectations();
        ProductManager productManager = new ProductManager(nativeMethodManager);
        HashMap<String, ArrayList<Product>> replacesMap = new HashMap<String, ArrayList<Product>>();

        Properties prop = new Properties();
        prop.setProperty(Product.NAME, "prod1");
        prop.setProperty(Product.PRODUCTID, Product.BASE_PRODUCTID);
        Product prod1 = new Product(prop);

        prop.setProperty(Product.NAME, "prod2");
        prop.setProperty(Product.PRODUCTID, "product2");
        Product prod2 = new Product(prop);

        prop.setProperty(Product.NAME, "prod3");
        prop.setProperty(Product.PRODUCTID, "product3");
        Product prod3 = new Product(prop);

        prop.setProperty(Product.NAME, "prod4");
        prop.setProperty(Product.PRODUCTID, "product4");
        Product prod4 = new Product(prop);

        prop.setProperty(Product.NAME, "prod5");
        prop.setProperty(Product.PRODUCTID, "product5");
        Product prod5 = new Product(prop);

        productManager.productMap.put(Product.BASE_PRODUCTID, prod1); // prod1 is base product
        productManager.productMap.put(prod2.productID(), prod2); // prod2 is just in the map
        productManager.productMap.put(prod3.productID(), prod3); // prod3 is just in the map
        productManager.productMap.put(prod4.productID(), prod4); // prod4 is just in the map
        productManager.productMap.put(prod5.productID(), prod5); // prod5 is just in the map

        // Product 2 and 3 replace product 1
        ArrayList<Product> productsThatReplaceProduct1 = new ArrayList<Product>();
        productsThatReplaceProduct1.add(prod2);
        productsThatReplaceProduct1.add(prod3);
        replacesMap.put(Product.BASE_PRODUCTID, productsThatReplaceProduct1); // prod2 and prod3 replace prod1

        // Product 4 replaces product 2
        ArrayList<Product> productsThatReplaceProduct2 = new ArrayList<Product>();
        productsThatReplaceProduct2.add(prod4);
        replacesMap.put("product2", productsThatReplaceProduct2); // prod4 replaces prod2

        // Product 5 replaces some product 6 that doesnt exist. This just tests that product 5 stays in the product map to be registered as a stack product
        ArrayList<Product> productsThatReplaceProduct6 = new ArrayList<Product>();
        productsThatReplaceProduct6.add(prod5);
        replacesMap.put("product6", productsThatReplaceProduct6);

        productManager.resolveBaseProducts(prod1, replacesMap);

        // The base product map should only contain product 3 AND product 4.
        assertTrue(productManager.baseProductMap.size() == 2);
        assertTrue(productManager.baseProductMap.containsKey(prod3.productID));
        assertTrue(productManager.baseProductMap.containsKey(prod4.productID));

        // The product map should only contain product 5 (1, 2, 3 and 4 should have been removed).
        System.out.println(productManager.productMap.size());
        assertTrue(productManager.productMap.size() == 1);
        assertTrue(productManager.productMap.containsKey(prod5.productID));

    }

    /**
     * New product manager instance expectations.
     */
    private void addNewPMInstanceExpectations() {
        final Sequence newPMSequence = context.sequence("newPMSequence");
        context.checking(new Expectations() {
            {
                oneOf(nativeMethodManager).registerNatives(with(ProductRegistrationImpl.class));
                inSequence(newPMSequence);
            }
        });
    }

    /**
     * New product manager start expectations.
     *
     * @param bundleContext The bundle context.
     */
    private void addStartExpectations(final BundleContext bundleContext) {
        final Sequence startSequence = context.sequence("startSequence");

        addNewPMInstanceExpectations();

        context.checking(new Expectations() {
            {
                oneOf(bundleContext).getProperty(BootstrapConstants.LOC_INTERNAL_LIB_DIR);
                will(returnValue(""));
                inSequence(startSequence);

                try {
                    oneOf(bundleContext).getServiceReferences(with(any(String.class)), with(any(String.class)));
                    inSequence(startSequence);
                } catch (InvalidSyntaxException ise) {
                    // Nothing.
                }
            }
        });
    }
}
