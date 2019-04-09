/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * This test uses the "usr" user feature space as the alternative product. Each product and the user
 * features have their own isolation space. The test comprises 8 bundles and 4 features. The bundles
 * are checked in as binaries because the test-bundles infrastructure did not provide any method of
 * dependency ordering the build of the bundles and the contents are trivial (marker interfaces and activators
 * that print a single line message to system out).
 * 
 */
public class SPIIsolationTest {

    private final static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.isolation");

    private final long timeout = 30000;

    @BeforeClass
    public static void setup() throws Exception {

        // BVT/UT build prep and FAT fight with each other
        // Make sure that a BVT step did not put these where they don't belong.
        server.uninstallFeature("isolation.Product.Test.Feature.Negative-1.0");
        server.uninstallFeature("isolation.Product.Test.Feature-1.0");
        server.uninstallFeature("isolation.KernelSPI.Test.Feature-1.0");
        server.uninstallBundle("test.feature.isolation.U4_1.0.0");
        server.uninstallBundle("test.feature.isolation.U5_1.0.0");
        server.uninstallBundle("test.feature.isolation.U6_1.0.0");
        server.uninstallBundle("test.feature.isolation.U7_1.0.0");
        server.uninstallBundle("test.feature.isolation.K1_1.0.0");

        //copy the isolation test runtime feature into the server features location
        server.copyFileToLibertyInstallRoot("lib/features", "isolation.Runtime.Test.Feature-1.0.mf");
        //copy the isolation test runtime bundles into the server lib location
        server.copyFileToLibertyInstallRoot("lib", "bundles/test.feature.isolation.L1_1.0.0.jar");
        server.copyFileToLibertyInstallRoot("lib", "bundles/test.feature.isolation.L2_1.0.0.jar");
        server.copyFileToLibertyInstallRoot("lib", "bundles/test.feature.isolation.L3_1.0.0.jar");

        //copy the negative runtime feature and bundle
        server.copyFileToLibertyInstallRoot("lib/features", "isolation.Runtime.Test.Feature.Negative-1.0.mf");
        server.copyFileToLibertyInstallRoot("lib", "bundles/test.feature.isolation.L8_1.0.0.jar");

        //install the user (different product isolation space) feature and bundles
        server.installUserFeature("isolation.Product.Test.Feature-1.0");
        server.installUserBundle("test.feature.isolation.U4_1.0.0");
        server.installUserBundle("test.feature.isolation.U5_1.0.0");
        server.installUserBundle("test.feature.isolation.U6_1.0.0");

        //install the negative user feature and bundles
        server.installUserFeature("isolation.Product.Test.Feature.Negative-1.0");
        server.installUserBundle("test.feature.isolation.U7_1.0.0");

        //install the kernel SPI user feature and bundle
        server.installUserFeature("isolation.KernelSPI.Test.Feature-1.0");
        server.installUserBundle("test.feature.isolation.K1_1.0.0");

        server.setServerConfigurationFile("isolation.server.xml");

        //all features and bundles in place, start the server
        server.startServer("SPIIsolationTest.log");
    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer();

        //copy the isolation test runtime feature into the server features location
        server.deleteFileFromLibertyInstallRoot("lib/features/isolation.Runtime.Test.Feature-1.0.mf");
        //copy the isolation test runtime bundles into the server lib location
        server.deleteFileFromLibertyInstallRoot("lib/bundles/test.feature.isolation.L1_1.0.0.jar");
        server.deleteFileFromLibertyInstallRoot("lib/bundles/test.feature.isolation.L2_1.0.0.jar");
        server.deleteFileFromLibertyInstallRoot("lib/bundles/test.feature.isolation.L3_1.0.0.jar");

        //copy the negative runtime feature and bundle
        server.deleteFileFromLibertyInstallRoot("lib/features/isolation.Runtime.Test.Feature.Negative-1.0.mf");
        server.deleteFileFromLibertyInstallRoot("lib/bundles/test.feature.isolation.L8_1.0.0.jar");

        //install the user (different product isolation space) feature and bundles
        server.uninstallUserFeature("isolation.Product.Test.Feature-1.0");
        server.uninstallUserBundle("test.feature.isolation.U4_1.0.0");
        server.uninstallUserBundle("test.feature.isolation.U5_1.0.0");
        server.uninstallUserBundle("test.feature.isolation.U6_1.0.0");

        //install the negative user feature and bundles
        server.uninstallUserFeature("isolation.Product.Test.Feature.Negative-1.0");
        server.uninstallUserBundle("test.feature.isolation.U7_1.0.0");

        //install the kernel SPI user feature and bundle
        server.uninstallUserFeature("isolation.KernelSPI.Test.Feature-1.0");
        server.uninstallUserBundle("test.feature.isolation.K1_1.0.0");

    }

    /**
     * Verify that the bundle L2 started.
     * It uses package p1b from bundle L1 so verifies that
     * a non-SPI package is allowed between runtime features.
     * 
     * @throws Exception
     */
    @Test
    public void testPackageAllowedBetweenRuntimeFeatures() throws Exception {
        assertFalse(server.findStringsInLogs("Isolation test: P2").isEmpty());
    }

    /**
     * Verify that the bundle L3 started.
     * It uses package p4a from bundle U4 so verifies that
     * a "stack product" SPI package is in a runtime feature
     * 
     * @throws Exception
     */
    @Test
    public void testProductFeatureSPIAllowedInRuntime() throws Exception {
        assertFalse(server.findStringsInLogs("Isolation test: P3").isEmpty());
    }

    /**
     * Verify that the bundle U5 started.
     * It uses package p4b from bundle U4 so verifies that
     * a non-SPI package can be used within a "stack product"
     * 
     * @throws Exception
     */
    @Test
    public void testPackageAllowedBetweenSameProductFeatures() throws Exception {
        assertFalse(server.findStringsInLogs("Isolation test: P5").isEmpty());
    }

    /**
     * Verify that the bundle L8 did not start
     * It uses package p4b from bundle U4 so verifies that a
     * non-SPI package is restricted between a "stack product"
     * and a runtime feature
     */
    @Test
    public void testNonSPIPackageRefusedProductExportRuntimeImport() throws Exception {
        try {
            //enable the feature with bundle L8
            TestUtils.makeConfigUpdateSetMark(server, "isolationUtoL.server.xml");

            //expect L8 resolve failure
            assertNotNull(server.waitForStringInLogUsingMark("CWWKF0029E.*test.feature.isolation.L8", timeout));
            assertNotNull(server.waitForStringInLogUsingMark(".*Unresolved requirement: Import-Package: test.feature.isolation.p4b", timeout));
        } finally {
            //revert to the original xml
            TestUtils.makeConfigUpdateSetMark(server, "isolation.server.xml");
        }
    }

    /**
     * Verify that the bundle U7 did not start
     * It uses package p1b from bundle L1 so verifies that a
     * non-SPI package is restricted between the runtime and
     * a "stack product" feature
     */
    @Test
    public void testNonSPIPackageRefusedRuntimeExportProductImport() throws Exception {
        try {
            //enable the feature with bundle U7
            TestUtils.makeConfigUpdateSetMark(server, "isolationLtoU.server.xml");

            //expect U7 resolve failure
            assertNotNull(server.waitForStringInLogUsingMark("CWWKF0029E.*test.feature.isolation.U7", timeout));
            assertNotNull(server.waitForStringInLogUsingMark(".*Unresolved requirement: Import-Package: test.feature.isolation.p1b", timeout));
        } finally {
            //revert to the original xml
            TestUtils.makeConfigUpdateSetMark(server, "isolation.server.xml");
        }
    }

    /**
     * Verify that the bundle U6 started.
     * It uses package p1a from bundle L1 so verifies that
     * a "stack product" can use a runtime SPI package
     */
    @Test
    public void testSPIAllowedBetweenProducts() throws Exception {
        assertFalse(server.findStringsInLogs("Isolation test: P6").isEmpty());
    }

    /**
     * Checks whether some SPIs provided by the kernel are available to
     * a user feature.
     */
    @Test
    public void testKernelProvidedSPI() throws Exception {
        try {
            TestUtils.makeConfigUpdateSetMark(server, "isolationKtoU.server.xml");

            //wait for the message from the kernel bundle
            assertNotNull(server.waitForStringInLogUsingMark("Isolation test: K1", timeout));
        } finally {
            //revert to the original xml
            TestUtils.makeConfigUpdateSetMark(server, "isolation.server.xml");
        }
    }
}
