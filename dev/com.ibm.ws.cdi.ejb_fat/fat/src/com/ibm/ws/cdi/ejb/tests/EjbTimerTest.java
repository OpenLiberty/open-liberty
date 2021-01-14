/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE7_FULL;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;

import java.io.File;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebBrowserFactory;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Asynchronous CDI tests with EJB Timers and Scheduled Tasks
 */
@RunWith(FATRunner.class)
public class EjbTimerTest extends FATServletClient {

    private static final Logger LOG = Logger.getLogger(EjbTimerTest.class.getName());

    public static final String SERVER_NAME = "cdi12EJB32Server";
    public static final String EJB_TIMER_APP_NAME = "ejbTimer";

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE7_FULL);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive ejbTimer = ShrinkWrap.create(WebArchive.class, EJB_TIMER_APP_NAME + ".war")
                                        .addClass(com.ibm.ws.cdi.ejb.apps.timer.IncrementCountersRunnableTask.class)
                                        .addClass(com.ibm.ws.cdi.ejb.apps.timer.SessionScopedCounter.class)
                                        .addClass(com.ibm.ws.cdi.ejb.apps.timer.TestEjbTimerTimeOutServlet.class)
                                        .addClass(com.ibm.ws.cdi.ejb.apps.timer.RequestScopedCounter.class)
                                        .addClass(com.ibm.ws.cdi.ejb.apps.timer.EjbSessionBean2.class)
                                        .addClass(com.ibm.ws.cdi.ejb.apps.timer.view.EjbSessionBeanLocal.class)
                                        .addClass(com.ibm.ws.cdi.ejb.apps.timer.view.SessionBeanLocal.class)
                                        .addClass(com.ibm.ws.cdi.ejb.apps.timer.view.EjbSessionBean2Local.class)
                                        .addClass(com.ibm.ws.cdi.ejb.apps.timer.ApplicationScopedCounter.class)
                                        .addClass(com.ibm.ws.cdi.ejb.apps.timer.SessionBean.class)
                                        .addClass(com.ibm.ws.cdi.ejb.apps.timer.TestEjbNoTimerServlet.class)
                                        .addClass(com.ibm.ws.cdi.ejb.apps.timer.RequestScopedBean.class)
                                        .addClass(com.ibm.ws.cdi.ejb.apps.timer.TestEjbTimerServlet.class)
                                        .addClass(com.ibm.ws.cdi.ejb.apps.timer.EjbSessionBean.class)
                                        .add(new FileAsset(new File("test-applications/" + EJB_TIMER_APP_NAME + ".war/resources/META-INF/permissions.xml")),
                                             "/META-INF/permissions.xml")
                                        .add(new FileAsset(new File("test-applications/" + EJB_TIMER_APP_NAME + ".war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        ShrinkHelper.exportDropinAppToServer(server, ejbTimer, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    /**
     * Verifies that a Session Scoped counter works correctly when incremented via either a
     * EJB Timer (i.e asynchronously)
     *
     * @throws Exception
     *             if counter is wrong, or if an unexpected error occurs
     */
    @Test
    public void testCDIScopeViaEJBTimer() throws Exception {
        WebBrowser wb = WebBrowserFactory.getInstance().createWebBrowser();
        //the count values returned are from BEFORE the increment occurs
        //request count should always be 0 since it should be a new request each time

        //first couple of times is synchronous (no timer or task)
        verifyResponse(wb, server, "/ejbTimer/NoTimer", "session = 0 request = 0");
        verifyResponse(wb, server, "/ejbTimer/NoTimer", "session = 1 request = 0");

        //the next couple start a timer which will increment asynchronously after 1 second
        //only one timer can be active at a time so subsequent calls will block until the previous timers have finished
        verifyResponse(wb, server, "/ejbTimer/Timer", "session = 2 request = 0");
        verifyResponse(wb, server, "/ejbTimer/Timer", "session = 3 request = 0");
        verifyResponse(wb, server, "/ejbTimer/NoTimer", "session = 4 request = 0");

        //this time do the same as above but injecting a RequestScoped bean to make sure
        //we are using the Weld SessionBeanInterceptor to set up the Request scope.
        verifyResponse(wb, server, "/ejbTimer/timerTimeOut", "counter = 0");
        verifyResponse(wb, server, "/ejbTimer/timerTimeOut", "counter = 1");
    }

    private static WebResponse verifyResponse(WebBrowser webBrowser, LibertyServer server, String resource, String expectedResponse) throws Exception {
        String url = createURL(server, resource);
        WebResponse response = webBrowser.request(url);
        LOG.info("Response from webBrowser: " + response.getResponseBody());
        response.verifyResponseBodyContains(expectedResponse);
        return response;
    }

    private static String createURL(LibertyServer server, String path) {
        if (!path.startsWith("/"))
            path = "/" + path;
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + path;
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
