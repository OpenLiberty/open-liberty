/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.lifecycle.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebBrowserFactory;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * These tests verify that scope lookup and destruction callbacks are supported as per
 * http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html#builtin_contexts
 */

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class BeanLifecycleTest {

    private static final Class<?> c = BeanLifecycleTest.class;

    private static final String SERVER_NAME = "cdi12BeanLifecycleTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME,
                                                         EERepeatActions.EE10,
                                                         EERepeatActions.EE9,
                                                         EERepeatActions.EE7);

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

    @BeforeClass
    public static void runLifecycle() throws Exception {

        WebArchive beanLifecycleApp1 = ShrinkWrap.create(WebArchive.class, "beanLifecycle1.war")
                                                 .addPackages(true, "com.ibm.ws.cdi.lifecycle.apps.beanLifecycle1War")
                                                 .addAsWebInfResource("com/ibm/ws/cdi/lifecycle/apps/beanLifecycle1War/beans.xml", "beans.xml");

        WebArchive beanLifecycleApp2 = ShrinkWrap.create(WebArchive.class, "beanLifecycle2.war")
                                                 .addPackages(true, "com.ibm.ws.cdi.lifecycle.apps.beanLifecycle2War")
                                                 .addAsWebInfResource("com/ibm/ws/cdi/lifecycle/apps/beanLifecycle2War/beans.xml", "beans.xml")
                                                 .addAsManifestResource("com/ibm/ws/cdi/lifecycle/apps/beanLifecycle2War/permissions.xml", "permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, beanLifecycleApp1, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, beanLifecycleApp2, DeployOptions.SERVER_ONLY);

        server.startServer();

        System.out.println("MYTEST - BEFORE");

        WebBrowser wb = WebBrowserFactory.getInstance().createWebBrowser();
        String testServletUrl = HttpUtils.createURL(server, "/beanLifecycle1/BeanLifecycle").toString();
        String servletTwoUrl = HttpUtils.createURL(server, "/beanLifecycle2/SecondServlet").toString();
        String endSessionUrl = HttpUtils.createURL(server, "/beanLifecycle1/EndSession").toString();

        wb.request(servletTwoUrl).getResponseBody();

        responseOne = "Request One: " + wb.request(testServletUrl).getResponseBody();
        Log.info(c, "runLifecycle", responseOne);

        responseTwo = "Request Two: " + wb.request(testServletUrl).getResponseBody();
        Log.info(c, "runLifecycle", responseTwo);

        wb.request(endSessionUrl);
        wb.close();
        wb = WebBrowserFactory.getInstance().createWebBrowser();

        responseThree = "Request Three: " + wb.request(testServletUrl).getResponseBody();
        Log.info(c, "runLifecycle", responseThree);

        server.removeAndStopDropinsApplications("beanLifecycle2.war");

        responseFour = "Request Four: " + wb.request(testServletUrl).getResponseBody();
        Log.info(c, "runLifecycle", responseFour);

    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
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
