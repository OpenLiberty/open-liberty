/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import java.io.File;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.jmx.mbeans.ApplicationMBean;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * These tests verify that scope lookup and destruction callbacks are supported as per
 * http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html#builtin_contexts
 */

@Mode(TestMode.FULL)
@SkipForRepeat({ SkipForRepeat.EE9_FEATURES }) // Skipped temporarily to test PassivationBeanTests for sessionDatabase-1.0 feature
public class BeanLifecycleTest extends LoggingTest {

    @ClassRule
    // Create the server.
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12BeanLifecycleTestServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BuildShrinkWrap
    public static Archive<?>[] buildShrinkWrap() {

        WebArchive scopeActivationDestructionTests = ShrinkWrap.create(WebArchive.class, "scopeActivationDestructionTests.war");
        scopeActivationDestructionTests.addClass("cdi12.scopedclasses.SessionScopedBean");
        scopeActivationDestructionTests.addClass("cdi12.scopedclasses.ConversationScopedBean");
        scopeActivationDestructionTests.addClass("cdi12.scopedclasses.RequestScopedBean");
        scopeActivationDestructionTests.addClass("cdi12.resources.GlobalState");
        scopeActivationDestructionTests.addClass("cdi12.resources.Move");
        scopeActivationDestructionTests.addClass("cdi12.resources.EndSessionServlet");
        scopeActivationDestructionTests.addClass("cdi12.resources.State");
        scopeActivationDestructionTests.addClass("cdi12.resources.BeanLifecycleServlet");
        scopeActivationDestructionTests.addClass("cdi12.resources.StateMachine");
        scopeActivationDestructionTests.add(new FileAsset(new File("test-applications/scopeActivationDestructionTests.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");
        scopeActivationDestructionTests.add(new FileAsset(new File("test-applications/scopeActivationDestructionTests.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        WebArchive scopeActivationDestructionSecondApp = ShrinkWrap.create(WebArchive.class, "scopeActivationDestructionSecondApp.war");
        scopeActivationDestructionSecondApp.addClass("cd12.secondapp.scopedclasses.SecondServlet");
        scopeActivationDestructionSecondApp.addClass("cd12.secondapp.scopedclasses.ApplicationScopedBean");
        scopeActivationDestructionSecondApp.add(new FileAsset(new File("test-applications/scopeActivationDestructionSecondApp.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");
        scopeActivationDestructionSecondApp.add(new FileAsset(new File("test-applications/scopeActivationDestructionSecondApp.war/resources/WEB-INF/beans.xml")),
                                                "/WEB-INF/beans.xml");

        EnterpriseArchive scopeActivationDestructionSecondAppEar = ShrinkWrap.create(EnterpriseArchive.class, "scopeActivationDestructionSecondApp.ear");
        scopeActivationDestructionSecondAppEar.add(new FileAsset(new File("test-applications/scopeActivationDestructionSecondApp.ear/resources/META-INF/permissions.xml")),
                                                   "/META-INF/permissions.xml");
        scopeActivationDestructionSecondAppEar.add(new FileAsset(new File("test-applications/scopeActivationDestructionSecondApp.ear/resources/META-INF/application.xml")),
                                                   "/META-INF/application.xml");
        scopeActivationDestructionSecondAppEar.addAsModule(scopeActivationDestructionSecondApp);
        EnterpriseArchive scopeActivationDestructionTestsEar = ShrinkWrap.create(EnterpriseArchive.class, "scopeActivationDestructionTests.ear");
        scopeActivationDestructionTestsEar.add(new FileAsset(new File("test-applications/scopeActivationDestructionTests.ear/resources/META-INF/permissions.xml")),
                                               "/META-INF/permissions.xml");
        scopeActivationDestructionTestsEar.add(new FileAsset(new File("test-applications/scopeActivationDestructionTests.ear/resources/META-INF/application.xml")),
                                               "/META-INF/application.xml");
        scopeActivationDestructionTestsEar.addAsModule(scopeActivationDestructionTests);

        Archive<?>[] archives = new Archive<?>[2];
        archives[0] = scopeActivationDestructionSecondAppEar;
        archives[1] = scopeActivationDestructionTestsEar;
        return archives;
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
