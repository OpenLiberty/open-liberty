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
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * All CDI tests with all applicable server features enabled.
 */
@RunWith(FATRunner.class)
public class StatefulSessionBeanInjectionTest extends FATServletClient {

    private static final Logger LOG = Logger.getLogger(StatefulSessionBeanInjectionTest.class.getName());

    public static final String SERVER_NAME = "cdi12StatefulSessionBeanServer";
    public static final String STATEFUL_SESSION_BEAN_APP_NAME = "statefulSessionBeanInjection";

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE7_FULL);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        JavaArchive statefulSessionBeanInjection = ShrinkWrap.create(JavaArchive.class, STATEFUL_SESSION_BEAN_APP_NAME + ".jar")
                                                             .addClass(com.ibm.ws.cdi.ejb.apps.statefulSessionBean.implicitEJB.InjectedEJBImpl.class)
                                                             .addClass(com.ibm.ws.cdi.ejb.apps.statefulSessionBean.implicitEJB.InjectedEJB.class)
                                                             .addClass(com.ibm.ws.cdi.ejb.apps.statefulSessionBean.implicitEJB.InjectedBean1.class)
                                                             .addClass(com.ibm.ws.cdi.ejb.apps.statefulSessionBean.implicitEJB.InjectedBean2.class)
                                                             .add(new FileAsset(new File("test-applications/" + STATEFUL_SESSION_BEAN_APP_NAME + ".jar/resources/META-INF/beans.xml")),
                                                                  "/META-INF/beans.xml");

        WebArchive statefulSessionBeanInjectionWar = ShrinkWrap.create(WebArchive.class, STATEFUL_SESSION_BEAN_APP_NAME + ".war")
                                                               .addClass(com.ibm.ws.cdi.ejb.apps.statefulSessionBean.web.RemoveServlet.class)
                                                               .addClass(com.ibm.ws.cdi.ejb.apps.statefulSessionBean.web.TestServlet.class)
                                                               .add(new FileAsset(new File("test-applications/" + STATEFUL_SESSION_BEAN_APP_NAME + ".war/resources/WEB-INF/beans.xml")),
                                                                    "/WEB-INF/beans.xml")
                                                               .addAsLibrary(statefulSessionBeanInjection);

        ShrinkHelper.exportDropinAppToServer(server, statefulSessionBeanInjectionWar, DeployOptions.SERVER_ONLY);
        server.startServer();

    }

    @Test
    @ExpectedFFDC("javax.ejb.NoSuchEJBException")
    public void testStatefulEJBRemoveMethod() throws Exception {
        WebBrowser wb = WebBrowserFactory.getInstance().createWebBrowser();

        verifyResponse(wb, server,
                       "/" + STATEFUL_SESSION_BEAN_APP_NAME + "/",
                       "Test Sucessful! - STATE1");

        verifyResponse(wb, server,
                       "/" + STATEFUL_SESSION_BEAN_APP_NAME + "/",
                       "Test Sucessful! - STATE2");

        verifyResponse(wb, server,
                       "/" + STATEFUL_SESSION_BEAN_APP_NAME + "/remove",
                       "EJB Removed!");

        verifyResponse(wb, server,
                       "/" + STATEFUL_SESSION_BEAN_APP_NAME + "/",
                       "NoSuchEJBException");
        // TODO Note that we stop the server in the test so that the expected FFDC on shutdown
        // happens in the testcase.  It is questionable that this FFDC is produced here.
        // It makes for the appearance of some leak with removed EJBs in the weld session
        server.stopServer();
    }

    private WebResponse verifyResponse(WebBrowser webBrowser, LibertyServer server, String resource, String expectedResponse) throws Exception {
        String url = this.createURL(server, resource);
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
