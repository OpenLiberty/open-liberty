/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.fat.tests;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

public class ObservesInitializedTest extends LoggingTest {
    @ClassRule
    public static ShrinkWrapServer SHARED_SERVER = new ShrinkWrapServer("cdi12BasicServer");

    @Override
    protected ShrinkWrapServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    @Mode(TestMode.FULL)
    public void testObservesInitializedInJars() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();

        SHARED_SERVER.verifyResponse(browser, "/ObservesInitializedInJars/TestServlet", new String[] { "web-inf jar saw initilization: true",
                                                                                                       "manifest jar saw initilization: true" });
    }

}
