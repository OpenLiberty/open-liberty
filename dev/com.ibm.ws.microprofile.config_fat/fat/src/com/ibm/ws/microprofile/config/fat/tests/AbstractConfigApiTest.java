/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.microprofile.config.fat.tests;

import org.junit.rules.TestName;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebBrowser;

/**
 *
 */
public abstract class AbstractConfigApiTest extends LoggingTest {

    public static final String PASSED = "PASSED";
    public static final String FAIL = "FAIL";

    private final String contextRoot;

    protected AbstractConfigApiTest(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public void test(TestName tn) throws Exception {
        test(tn.getMethodName());
    }

    public final void test(String test) throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        String testClass = upperCaseInitialLetter(test);
        getSharedServer().verifyResponse(browser, contextRoot + "?test=" + testClass, "PASSED");
    }

    public static String upperCaseInitialLetter(String string) {
        return string != null && string.length() >= 1 ? string.substring(0, 1).toUpperCase() + string.substring(1) : "";
    }
}
