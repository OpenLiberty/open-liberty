/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.ejbcontainer.injection.mix.web.AdvSFEnvInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.mix.web.AdvSFRemoteEnvInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.mix.web.AdvSLEnvInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.mix.web.AdvSLRemoteEnvInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.mix.web.BasicSFEnvInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.mix.web.BasicSFRemoteEnvInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.mix.web.BasicSLEnvInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.mix.web.BasicSLRemoteEnvInjectionServlet;
import com.ibm.ws.ejbcontainer.injection.mix.web.BindingOfRefTypesInInterceptorsServlet;
import com.ibm.ws.ejbcontainer.injection.mix.web.SuperEnvInjectionServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class InjectionMIXTest {

    @Server("com.ibm.ws.ejbcontainer.injection.fat.mdbserver")
    @TestServlets({ @TestServlet(servlet = AdvSFEnvInjectionServlet.class, contextRoot = "EJB3INJSMWeb"),
                    @TestServlet(servlet = AdvSFRemoteEnvInjectionServlet.class, contextRoot = "EJB3INJSMWeb"),
                    @TestServlet(servlet = AdvSLEnvInjectionServlet.class, contextRoot = "EJB3INJSMWeb"),
                    @TestServlet(servlet = AdvSLRemoteEnvInjectionServlet.class, contextRoot = "EJB3INJSMWeb"),
                    @TestServlet(servlet = BasicSFEnvInjectionServlet.class, contextRoot = "EJB3INJSMWeb"),
                    @TestServlet(servlet = BasicSFRemoteEnvInjectionServlet.class, contextRoot = "EJB3INJSMWeb"),
                    @TestServlet(servlet = BasicSLEnvInjectionServlet.class, contextRoot = "EJB3INJSMWeb"),
                    @TestServlet(servlet = BasicSLRemoteEnvInjectionServlet.class, contextRoot = "EJB3INJSMWeb"),
                    @TestServlet(servlet = BindingOfRefTypesInInterceptorsServlet.class, contextRoot = "EJB3INJSMWeb"),
                    @TestServlet(servlet = SuperEnvInjectionServlet.class, contextRoot = "EJB3INJSMWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.injection.fat.mdbserver")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.injection.fat.mdbserver"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the ear

        JavaArchive EJB3INJEJBMBeanJar = ShrinkHelper.buildJavaArchive("EJB3INJEJBMBean.jar", "com.ibm.ws.ejbcontainer.injection.mix.ejb.");
        EJB3INJEJBMBeanJar = (JavaArchive) ShrinkHelper.addDirectory(EJB3INJEJBMBeanJar, "test-applications/EJB3INJEJBMBean.jar/resources");

        JavaArchive EJB3INJINTMBeanJar = ShrinkHelper.buildJavaArchive("EJB3INJINTMBean.jar", "com.ibm.ws.ejbcontainer.injection.mix.ejbint.");
        EJB3INJINTMBeanJar = (JavaArchive) ShrinkHelper.addDirectory(EJB3INJINTMBeanJar, "test-applications/EJB3INJINTMBean.jar/resources");

        WebArchive EJB3INJSMWeb = ShrinkHelper.buildDefaultApp("EJB3INJSMWeb.war", "com.ibm.ws.ejbcontainer.injection.mix.web.");

        EnterpriseArchive EJB3INJSMTestApp = ShrinkWrap.create(EnterpriseArchive.class, "EJB3INJSMTestApp.ear");
        EJB3INJSMTestApp = (EnterpriseArchive) ShrinkHelper.addDirectory(EJB3INJSMTestApp, "test-applications/EJB3INJSMTestApp.ear/resources");
        EJB3INJSMTestApp.addAsModule(EJB3INJEJBMBeanJar).addAsModule(EJB3INJINTMBeanJar).addAsModule(EJB3INJSMWeb);

        ShrinkHelper.exportDropinAppToServer(server, EJB3INJSMTestApp);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
        //server.stopServer("CNTR0168W", "CNTR0020E", "CWNEN0061E", "CWNEN0009E", "CNTR4006E", "CNTR0338W");
    }
}
