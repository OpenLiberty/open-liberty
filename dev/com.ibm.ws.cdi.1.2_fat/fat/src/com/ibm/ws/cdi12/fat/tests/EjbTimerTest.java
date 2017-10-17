package com.ibm.ws.cdi12.fat.tests;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebBrowser;

/**
 * Asynchronous CDI tests with EJB Timers and Scheduled Tasks
 */
public class EjbTimerTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapServer SHARED_SERVER = new ShrinkWrapServer("cdi12EJB32Server");

    /**
     * Verifies that a Session Scoped counter works correctly when incremented via either a
     * EJB Timer (i.e asynchronously)
     *
     * @throws Exception
     *             if counter is wrong, or if an unexpected error occurs
     */
    @Test
    public void testCDIScopeViaEJBTimer() throws Exception {
        //the count values returned are from BEFORE the increment occurs
        //request count should always be 0 since it should be a new request each time

        WebBrowser browser = createWebBrowserForTestCase();

        //first couple of times is synchronous (no timer or task)
        SHARED_SERVER.verifyResponse(browser, "/ejbTimer/NoTimer", "session = 0 request = 0");
        SHARED_SERVER.verifyResponse(browser, "/ejbTimer/NoTimer", "session = 1 request = 0");

        //the next couple start a timer which will increment asynchronously after 1 second
        //only one timer can be active at a time so subsequent calls will block until the previous timers have finished
        SHARED_SERVER.verifyResponse(browser, "/ejbTimer/Timer", "session = 2 request = 0");
        SHARED_SERVER.verifyResponse(browser, "/ejbTimer/Timer", "session = 3 request = 0");
        SHARED_SERVER.verifyResponse(browser, "/ejbTimer/NoTimer", "session = 4 request = 0");

        //this time do the same as above but injecting a RequestScoped bean to make sure
        //we are using the Weld SessionBeanInterceptor to set up the Request scope.
        SHARED_SERVER.verifyResponse(browser, "/ejbTimer/timerTimeOut", "counter = 0");
        SHARED_SERVER.verifyResponse(browser, "/ejbTimer/timerTimeOut", "counter = 1");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapServer getSharedServer() {
        return SHARED_SERVER;
    }

}
