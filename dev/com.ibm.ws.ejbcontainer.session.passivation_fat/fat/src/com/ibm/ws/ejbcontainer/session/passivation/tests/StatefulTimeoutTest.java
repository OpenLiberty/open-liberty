/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.session.passivation.tests;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
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
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.web.StatefulTimeoutServlet;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class StatefulTimeoutTest extends AbstractTest {
    @Server("com.ibm.ws.ejbcontainer.session.passivation.fat.sfTimeout")
    @TestServlets({ @TestServlet(servlet = StatefulTimeoutServlet.class, contextRoot = "StatefulTimeoutWeb") })
    public static LibertyServer server;

    @Override
    public LibertyServer getServer() {
        return server;
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.session.passivation.fat.sfTimeout")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.session.passivation.fat.sfTimeout")).andWith(new JakartaEE9Action().forServers("com.ibm.ws.ejbcontainer.session.passivation.fat.sfTimeout"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the Ear

        JavaArchive StatefulTimeoutEJBJar = ShrinkHelper.buildJavaArchive("StatefulTimeoutEJB.jar", "com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.");
        WebArchive StatefulTimeoutWeb = ShrinkHelper.buildDefaultApp("StatefulTimeoutWeb.war", "com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.web.");

        EnterpriseArchive StatefulTimeoutTestApp = ShrinkWrap.create(EnterpriseArchive.class, "StatefulTimeoutTestApp.ear");
        StatefulTimeoutTestApp.addAsModule(StatefulTimeoutEJBJar).addAsModule(StatefulTimeoutWeb);

        ShrinkHelper.exportDropinAppToServer(server, StatefulTimeoutTestApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CNTR0311E", "CNTR0312E", "CNTR4006E", "CNTR0304W", "CNTR0306W", "CNTR0310W", "CNTR0309E", "CNTR0020E");
    }

    /**
     * Tests that the container logs the CNTR0306W warning when the
     * <code>@StatefulTimeout</code> annotation appears on an interface and
     * when EJBContainer=all tracing is enabled.
     */
    @Test
    @Mode(FULL)
    public void testAnnotationOnInterfaceLogsWarning() throws Exception {
        server.setMarkToEndOfLog(server.getMostRecentTraceFile());
        runTest("StatefulTimeoutWeb/StatefulTimeoutServlet");
        assertNotNull(server.waitForStringInTraceUsingMark("CNTR0306W"));
    }

    /**
     * Tests that the container correctly fails to create a bean with a negative
     * timeout value (less than -1) in its annotation. The container should fail
     * the JNDI lookup and log a CNTR0311E error.
     */
    @Test
    @Mode(FULL)
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException" })
    public void testNegativeTwoValueInAnnotationLogsError() throws Exception {
        server.setMarkToEndOfLog();
        runTest("StatefulTimeoutWeb/StatefulTimeoutServlet");
        assertNotNull(server.waitForStringInLogUsingMark("CNTR0311E"));
    }

    /**
     * Tests that the container correctly fails to create a bean with a negative
     * timeout value (less than -1) in the XML DD. The container should fail the
     * JNDI lookup and log a CNTR0311E error.
     */
    @Test
    @Mode(FULL)
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException" })
    public void testNegativeTwoValueInXMLLogsError() throws Exception {
        server.setMarkToEndOfLog();
        runTest("StatefulTimeoutWeb/StatefulTimeoutServlet");
        assertNotNull(server.waitForStringInLogUsingMark("CNTR0312E"));
    }

    /**
     * Tests that the container logs a CNTR0304W warning when a stateless bean
     * contains a <code>@StatefulTimeout</code> annotation (and EJBContainer
     * trace is enabled).
     */
    @Test
    @Mode(FULL)
    public void testLogWarningOnStatefulTimeoutOnStatelessBeanAnnotation() throws Exception {
        server.setMarkToEndOfLog();
        runTest("StatefulTimeoutWeb/StatefulTimeoutServlet");
        assertNotNull(server.waitForStringInLogUsingMark("CNTR0304W"));
    }

    /**
     * Tests that the container logs a CNTR0310W warning when a stateless bean
     * is configured with a stateful-timeout element in the XML DD (and
     * EJBContainer trace is enabled).
     */
    @Test
    @Mode(FULL)
    public void testLogWarningOnStatefulTimeoutOnStatelessBeanXML() throws Exception {
        server.setMarkToEndOfLog();
        runTest("StatefulTimeoutWeb/StatefulTimeoutServlet");
        assertNotNull(server.waitForStringInLogUsingMark("CNTR0310W"));
    }

    /**
     * Tests that the container fails to start a SFSB that is configured with
     * too high of a stateful-timeout value in the XML DD. A value of
     * 999999999999 days converts to a long integer that is greater than MAX
     * LONG.
     */
    @Test
    @Mode(FULL)
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException" })
    public void testXMLTimeoutSpecifiedOverflow() throws Exception {
        server.setMarkToEndOfLog();
        runTest("StatefulTimeoutWeb/StatefulTimeoutServlet");
        assertNotNull(server.waitForStringInLogUsingMark("CNTR0309E"));
    }
}