/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.session.async.fat;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

@Mode(FULL)
@RunWith(FATRunner.class)
public class AsyncErrTest extends FATServletClient {
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.session.async.fat.AsyncErrServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.session.async.fat.AsyncErrServer"));

    protected void runTest(String servlet, String testName) throws Exception {
        FATServletClient.runTest(server, servlet, testName);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.ejbcontainer.session.async.fat.AsyncErrServer");

        // Use ShrinkHelper to build the ears
        JavaArchive AsyncAErrIntf = ShrinkHelper.buildJavaArchive("AsyncAErrIntf.jar", "com.ibm.ws.ejbcontainer.session.async.err.shared.");
        JavaArchive AsyncErr1Bean = ShrinkHelper.buildJavaArchive("AsyncErr1Bean.jar", "com.ibm.ws.ejbcontainer.session.async.err.error1.ejb.");
        EnterpriseArchive AsyncErr1BeanApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncErr1BeanApp.ear");
        AsyncErr1BeanApp.addAsModule(AsyncErr1Bean);
        JavaArchive AsyncErr2Bean = ShrinkHelper.buildJavaArchive("AsyncErr2Bean.jar", "com.ibm.ws.ejbcontainer.session.async.err.error2.ejb.");
        EnterpriseArchive AsyncErr2BeanApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncErr2BeanApp.ear");
        AsyncErr2BeanApp.addAsModule(AsyncErr2Bean);
        JavaArchive AsyncXMLErr1Bean = ShrinkHelper.buildJavaArchive("AsyncXMLErr1Bean.jar", "com.ibm.ws.ejbcontainer.session.async.err.xmlerr1.ejb.");
        EnterpriseArchive AsyncXMLErr1BeanApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncXMLErr1BeanApp.ear");
        AsyncXMLErr1BeanApp.addAsModule(AsyncXMLErr1Bean);
        JavaArchive AsyncXMLErr2Bean = ShrinkHelper.buildJavaArchive("AsyncXMLErr2Bean.jar", "com.ibm.ws.ejbcontainer.session.async.err.xmlerr2.ejb.");
        EnterpriseArchive AsyncXMLErr2BeanApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncXMLErr2BeanApp.ear");
        AsyncXMLErr2BeanApp.addAsModule(AsyncXMLErr2Bean);
        JavaArchive AsyncXMLErr3Bean = ShrinkHelper.buildJavaArchive("AsyncXMLErr3Bean.jar", "com.ibm.ws.ejbcontainer.session.async.err.xmlerr3.ejb.");
        EnterpriseArchive AsyncXMLErr3BeanApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncXMLErr3BeanApp.ear");
        AsyncXMLErr3BeanApp.addAsModule(AsyncXMLErr3Bean);
        WebArchive AsyncErrTestWar = ShrinkHelper.buildDefaultApp("AsyncErrTest.war", "com.ibm.ws.ejbcontainer.session.async.err.web.");
        EnterpriseArchive AsyncErrTest = ShrinkWrap.create(EnterpriseArchive.class, "AsyncErrTest.ear");
        AsyncErrTest.addAsModule(AsyncErrTestWar);

        ShrinkHelper.exportDropinAppToServer(server, AsyncErr1BeanApp);
        ShrinkHelper.exportAppToServer(server, AsyncErr2BeanApp);
        ShrinkHelper.exportDropinAppToServer(server, AsyncXMLErr1BeanApp);
        ShrinkHelper.exportDropinAppToServer(server, AsyncXMLErr2BeanApp);
        ShrinkHelper.exportDropinAppToServer(server, AsyncXMLErr3BeanApp);
        ShrinkHelper.exportDropinAppToServer(server, AsyncErrTest);
        ShrinkHelper.exportToServer(server, "lib/global", AsyncAErrIntf);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0185E", "CNTR0187E", "CNTR0203E", "CNTR0204E", "CNTR4002E", "CNTR4006E", "CNTR4007E", "CWWKZ0002E", "CWWKZ0106E");
        }
    }

    /**
     * Invalid tx_attribute - NEVER
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException" })
    public void testNever() throws Exception {
        server.setMarkToEndOfLog();
        runTest("AsyncErrTest/AsyncErrorServlet", "testNever");
        assertNotNull("Message was not logged: CNTR0187E", server.waitForStringInLogUsingMark("CNTR0187E"));
    }

    /**
     * Invalid bean type - Message Driven Bean
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.container.service.state.StateChangeException", "com.ibm.ejs.container.EJBConfigurationException" })
    public void testMDB() throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("AsyncErr2BeanApp_server.xml");
        server.waitForConfigUpdateInLogUsingMark(null, "");

        assertNotNull("Message was not logged: CNTR0185E", server.waitForStringInLogUsingMark("CNTR0185E"));
        assertNotNull("An exception did NOT occurred while starting the application AsyncErr2BeanApp. CWWKZ0002E message should have been found.",
                      server.waitForStringInLogUsingMark("CWWKZ0002E"));
    }

    /**
     * Verify that the container throws an exception, CNTR0203E, when the
     * method-name element of the async-methodType XML is not present.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException" })
    public void testNoMethNameXML() throws Exception {
        server.setMarkToEndOfLog();
        runTest("AsyncErrTest/AsyncXMLErrorServlet", "testNoMethNameXML");
        assertFalse("Message was not logged: CNTR0203E", server.findStringsInLogsAndTraceUsingMark("CNTR0203E").isEmpty());
    }

    /**
     * Verify that the container throws an exception, CNTR0203E, when the
     * method-name element of the async-methodType XML is an empty string.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException" })
    public void testEmptyMethNameXML() throws Exception {
        server.setMarkToEndOfLog();
        runTest("AsyncErrTest/AsyncXMLErrorServlet", "testEmptyMethNameXML");
        assertFalse("Message was not logged: CNTR0203E", server.findStringsInLogsAndTraceUsingMark("CNTR0203E").isEmpty());
    }

    /**
     * Verify that the container throws an exception, CNTR0204E, when
     * method-params are defined while using Style 1 XML (i.e. * for
     * method-name).
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException" })
    public void testStyle1XMLwithParams() throws Exception {
        server.setMarkToEndOfLog();
        runTest("AsyncErrTest/AsyncXMLErrorServlet", "testStyle1XMLwithParams");
        assertFalse("Message was not logged: CNTR0204E", server.findStringsInLogsAndTraceUsingMark("CNTR0204E").isEmpty());
    }
}