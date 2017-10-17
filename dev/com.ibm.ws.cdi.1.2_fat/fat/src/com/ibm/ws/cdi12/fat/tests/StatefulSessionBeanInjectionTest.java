package com.ibm.ws.cdi12.fat.tests;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.annotation.ExpectedFFDC;

/**
 * All CDI tests with all applicable server features enabled.
 */
public class StatefulSessionBeanInjectionTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapServer SHARED_SERVER = new ShrinkWrapServer("cdi12StatefulSessionBeanServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    @ExpectedFFDC("javax.ejb.NoSuchEJBException")
    public void testStatefulEJBRemoveMethod() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        this.verifyResponse(browser,
                            "/statefulSessionBeanInjection/",
                            "Test Sucessful! - STATE1");

        this.verifyResponse(browser,
                            "/statefulSessionBeanInjection/",
                            "Test Sucessful! - STATE2");

        this.verifyResponse(browser,
                            "/statefulSessionBeanInjection/remove",
                            "EJB Removed!");

        this.verifyResponse(browser,
                            "/statefulSessionBeanInjection/",
                            "NoSuchEJBException");
        // TODO Note that we stop the server in the test so that the expected FFDC on shutdown
        // happens in the testcase.  It is questionable that this FFDC is produced here.
        // It makes for the appearance of some leak with removed EJBs in the weld session
        getSharedServer().getLibertyServer().stopServer();
    }
}
