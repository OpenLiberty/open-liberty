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
package com.ibm.ws.ejbcontainer.interceptor.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout.web.AdvancedAroundTimeoutAnnServlet;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout.web.AdvancedAroundTimeoutMixServlet;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout.web.AdvancedAroundTimeoutXmlServlet;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout.web.AroundInvokeAnnServlet;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout.web.AroundTimeoutExcServlet;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout.web.InheritedAroundTimeoutAnnServlet;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout.web.InheritedTimerCallbackAnnServlet;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout.web.InvocationContextAnnServlet;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout.web.MDBAroundTimeoutAnnServlet;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout.web.OverrideAroundTimeoutAnnServlet;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout.web.SLSBAroundTimeoutAnnServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class AroundTimeoutTest extends FATServletClient {
    @Server("com.ibm.ws.ejbcontainer.interceptor.fat.AroundTimeoutServer")
    @TestServlets({ @TestServlet(servlet = AdvancedAroundTimeoutAnnServlet.class, contextRoot = "AroundTimeoutWeb"),
                    @TestServlet(servlet = AdvancedAroundTimeoutMixServlet.class, contextRoot = "AroundTimeoutWeb"),
                    @TestServlet(servlet = AdvancedAroundTimeoutXmlServlet.class, contextRoot = "AroundTimeoutWeb"),
                    @TestServlet(servlet = AroundInvokeAnnServlet.class, contextRoot = "AroundTimeoutWeb"),
                    @TestServlet(servlet = AroundTimeoutExcServlet.class, contextRoot = "AroundTimeoutWeb"),
                    @TestServlet(servlet = InheritedAroundTimeoutAnnServlet.class, contextRoot = "AroundTimeoutWeb"),
                    @TestServlet(servlet = InheritedTimerCallbackAnnServlet.class, contextRoot = "AroundTimeoutWeb"),
                    @TestServlet(servlet = InvocationContextAnnServlet.class, contextRoot = "AroundTimeoutWeb"),
                    @TestServlet(servlet = MDBAroundTimeoutAnnServlet.class, contextRoot = "AroundTimeoutWeb"),
                    @TestServlet(servlet = OverrideAroundTimeoutAnnServlet.class, contextRoot = "AroundTimeoutWeb"),
                    @TestServlet(servlet = SLSBAroundTimeoutAnnServlet.class, contextRoot = "AroundTimeoutWeb"),
    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.interceptor.fat.AroundTimeoutServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.interceptor.fat.AroundTimeoutServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        JavaInfo javaInfo = JavaInfo.forServer(server);
        if (javaInfo.toString().contains("IBM")) {
            server.copyFileToLibertyServerRoot("jvm.options");
        }

        // Use ShrinkHelper to build the ears
        JavaArchive AroundTimeoutAnnEJB = ShrinkHelper.buildJavaArchive("AroundTimeoutAnnEJB.jar", "com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.");
        JavaArchive AroundTimeoutExcEJB = ShrinkHelper.buildJavaArchive("AroundTimeoutExcEJB.jar", "com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_exc.ejb.");
        JavaArchive AroundTimeoutMixEJB = ShrinkHelper.buildJavaArchive("AroundTimeoutMixEJB.jar", "com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_mix.ejb.");
        JavaArchive AroundTimeoutXmlEJB = ShrinkHelper.buildJavaArchive("AroundTimeoutXmlEJB.jar", "com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_xml.ejb.");
        WebArchive AroundTimeoutWeb = ShrinkHelper.buildDefaultApp("AroundTimeoutWeb.war", "com.ibm.ws.ejbcontainer.interceptor.aroundTimeout.web.");
        EnterpriseArchive AroundTimeoutTest = ShrinkWrap.create(EnterpriseArchive.class, "AroundTimeoutTest.ear");
        AroundTimeoutTest.addAsModule(AroundTimeoutAnnEJB).addAsModule(AroundTimeoutExcEJB).addAsModule(AroundTimeoutMixEJB).addAsModule(AroundTimeoutXmlEJB).addAsModule(AroundTimeoutWeb);

        ShrinkHelper.exportDropinAppToServer(server, AroundTimeoutTest);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}