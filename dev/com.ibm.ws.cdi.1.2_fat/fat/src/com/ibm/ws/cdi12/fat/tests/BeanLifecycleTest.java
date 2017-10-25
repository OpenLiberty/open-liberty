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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.jmx.mbeans.ApplicationMBean;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * These tests verify that scope lookup and destruction callbacks are supported as per
 * http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html#builtin_contexts
 */

@Mode(TestMode.FULL)
public class BeanLifecycleTest extends LoggingTest {

    @ClassRule
    // Create the server.
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12BeanLifecycleTestServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    private static boolean setUp = false;

    /*
     * Response one - the first hit on the servlet - this occurs after sending the application started method from application two.
     * Response two - a second hit without any additonal poking.
     * Response three - hitting the servlet with a new browser after manually ending the session.
     * Response Four - hitting the servlet after stopping the second application.
     */
    private static String responseOne = null;
    private static String responseTwo = null;
    private static String responseThree = null;
    private static String responseFour = null;

    private void assertResponseContains(String response, String target) {
        Assert.assertTrue("Did not find \"" + target + "\" in \"" + response + "\"", response.contains(target));
    }

    @Before
    public void runThroughCircleOfLIfe() throws Exception {

        if (setUp || SHARED_SERVER.getLibertyServer().getValidateApps() == false) {
            return;
        }

        System.out.println("MYTEST - BEFORE");

        WebBrowser wb = createWebBrowserForTestCase();
        String testServletUrl = SHARED_SERVER.getServerUrl(true, "/PrideRock/BeanLifecycle");
        String servletTwoUrl = SHARED_SERVER.getServerUrl(true, "/scopeActivationDestructionSecondApp/SecondServlet");
        String endSessionUrl = SHARED_SERVER.getServerUrl(true, "/PrideRock/EndSession");

        wb.request(servletTwoUrl).getResponseBody();

        responseOne = "Request One: " + wb.request(testServletUrl).getResponseBody();

        responseTwo = "Request Two: " + wb.request(testServletUrl).getResponseBody();

        wb.request(endSessionUrl);
        wb.close();
        wb = createWebBrowserForTestCase();

        responseThree = "Request Three: " + wb.request(testServletUrl).getResponseBody();

        ApplicationMBean mBean = SHARED_SERVER.getApplicationMBean("scopeActivationDestructionSecondApp");
        mBean.stop();

        responseFour = "Request Four: " + wb.request(testServletUrl).getResponseBody();

        setUp = true;
    }

    @Test
    public void testConversationLifecycleStart() throws Exception {
        assertResponseContains(responseOne, "Conversation Scoped Bean: STARTEDUP");
    }

    @Test
    public void testConversationLifecycleStop() throws Exception {
        assertResponseContains(responseTwo, "Conversation Scoped Bean: STARTEDTHENSTOPPED");
    }

    @Test
    public void testRequestLifecycleStart() throws Exception {
        //Ideally this should test for STARTEDUP - However the second servlet also creates a request scope that starts and stops before the test framework can have a look.
        assertResponseContains(responseOne, "Request Scoped Bean: STARTEDTHENSTOPPED");
    }

    @Test
    public void testRequestLifecycleStop() throws Exception {
        assertResponseContains(responseTwo, "Request Scoped Bean: STARTEDTHENSTOPPED");
    }

    @Test
    public void testApplicaitonLifecycleStart() throws Exception {
        assertResponseContains(responseOne, "Applicaiton Scoped Bean: STARTEDUP");
    }

    //This test checks that nothing has changed between responseOne and responseThree
    @Test
    public void testApplicaitonLifecycleContinue() throws Exception {
        assertResponseContains(responseThree, "Applicaiton Scoped Bean: STARTEDUP");
    }

    @Test
    public void testApplicationLifecycleStop() throws Exception {
        assertResponseContains(responseFour, "Applicaiton Scoped Bean: STARTEDTHENSTOPPED");
    }

    @Test
    public void testSessionLifecycleStart() throws Exception {
        assertResponseContains(responseOne, "Session Scoped Bean: STARTEDUP");
    }

    //This test checks that nothing has changed between responseOne and responseTwo
    @Test
    public void testSessionLifecycleContinue() throws Exception {
        assertResponseContains(responseTwo, "Session Scoped Bean: STARTEDUP");
    }

    @Test
    public void testSessionLifecycleStop() throws Exception {
        assertResponseContains(responseThree, "Session Scoped Bean: STARTEDTHENSTOPPED");
    }

}
