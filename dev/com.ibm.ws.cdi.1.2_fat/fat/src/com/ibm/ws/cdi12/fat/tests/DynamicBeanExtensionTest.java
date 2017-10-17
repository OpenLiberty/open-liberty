/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.fat.tests;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.custom.junit.runner.Mode;

/**
 * Scope tests for Dynamically Added Beans
 */
@Mode(FULL)
public class DynamicBeanExtensionTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapServer SHARED_SERVER = new ShrinkWrapServer("cdi12DynamicallyAddedBeansServer");

    @Override
    protected ShrinkWrapServer getSharedServer() {
        return SHARED_SERVER;
    }

    /**
     * Test that bean classes which are loaded by the Root ClassLoader can be injected correctly
     *
     * @throws Exception
     */
    @Test
    public void testDynamicallyAddedBeans() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        SHARED_SERVER.verifyResponse(browser, "/dynamicallyAddedBeans/", new String[] { "DynamicBean1 count: 1, 2", "DynamicBean2 count: 1, 2" });
        SHARED_SERVER.verifyResponse(browser, "/dynamicallyAddedBeans/", new String[] { "DynamicBean1 count: 3, 4", "DynamicBean2 count: 1, 2" });
        browser = createWebBrowserForTestCase();
        SHARED_SERVER.verifyResponse(browser, "/dynamicallyAddedBeans/", new String[] { "DynamicBean1 count: 1, 2", "DynamicBean2 count: 1, 2" });
    }

}
