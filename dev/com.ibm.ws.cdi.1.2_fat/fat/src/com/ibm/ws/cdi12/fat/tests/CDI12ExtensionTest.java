/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.fat.tests;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Test the runtime extension to function correctly
 */
public class CDI12ExtensionTest extends LoggingTest {

    // @ClassRule
    // Create the server.
    public static ShrinkWrapServer EXTENSION_SERVER = new ShrinkWrapServer("cdi12RuntimeExtensionServer");
    public static String INSTALL_USERBUNDLE = "cdi.helloworld.extension_1.0.0";
    public static String INSTALL_USERFEATURE = "cdi.helloworld.extension";

    public static String EXPOSE_INTERNAL_CDI_EXTENSION_API_FEATURE = "cdi12.internals-1.0";
    private static LibertyServer server;

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapServer getSharedServer() {
        return EXTENSION_SERVER;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        /**
         * Install the user feature and the bundle
         */
        server = EXTENSION_SERVER.getLibertyServer();
        System.out.println("Intall the user feature bundle... cdi.helloworld.extension_1.0.0");
        server.installUserBundle(INSTALL_USERBUNDLE);
        server.installUserFeature(INSTALL_USERFEATURE);
        server.installSystemFeature(EXPOSE_INTERNAL_CDI_EXTENSION_API_FEATURE);
        server.startServer(true);
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application helloWorldExension started");
    }

    @Test
    public void testHelloWorldExtensionServlet() throws Exception {

        HttpUtils.findStringInUrl(server, "/helloWorldExtension/hello", "Hello World CDI 1.2!");

        Assert.assertFalse("Test for Requested scope destroyed",
                           server.findStringsInLogs("Stop Event request scope is happening").isEmpty());
    }

    @Test
    public void testExtensionLoaded() throws Exception {
        Assert.assertFalse("Test for extension loadded",
                           server.findStringsInLogs("Hello World! We are starting the container").isEmpty());
        Assert.assertFalse("Test for extension loadded",
                           server.findStringsInLogs("Hello World! scanning class").isEmpty());
        Assert.assertFalse("Test for extension loadded",
                           server.findStringsInLogs("Hello World! scanning class javax.validation.ValidatorFactory").isEmpty());
        Assert.assertFalse("Test for extension loadded",
                           server.findStringsInLogs("Hello World! scanning class javax.validation.Validator").isEmpty());
        Assert.assertFalse("Test for extension loadded",
                           server.findStringsInLogs("Hello World! We are almost finished with the CDI container boot now...").isEmpty());
    }

    @Test
    public void testCDINotEnabled() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();

        verifyResponse(browser, "/multipleWar3", "MyEjb myWar1Bean");
        verifyResponse(browser, "/multipleWarNoBeans", "ContextNotActiveException");
    }

    /**
     * Post test processing.
     *
     * @throws Exception
     */
    @AfterClass
    public static void cleanup() throws Exception {
        final String METHOD_NAME = "cleanup";
        Log.info(CDI12ExtensionTest.class, METHOD_NAME, "Stopping the server.");
        if (server.isStarted()) {
            server.stopServer();
        }
        Log.info(CDI12ExtensionTest.class, METHOD_NAME, "Removing cdi extension test user feature files.");
        server.uninstallSystemFeature(EXPOSE_INTERNAL_CDI_EXTENSION_API_FEATURE);
        server.uninstallUserBundle(INSTALL_USERBUNDLE);
        server.uninstallUserFeature(INSTALL_USERFEATURE);
    }
}
