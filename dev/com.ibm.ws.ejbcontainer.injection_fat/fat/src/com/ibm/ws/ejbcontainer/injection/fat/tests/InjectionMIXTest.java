/*******************************************************************************
 * Copyright (c) 2006, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.fat.tests;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.ServerConfigurationFactory;
import com.ibm.ws.ejbcontainer.injection.fat.tests.repeataction.RepeatWithCDI;
import com.ibm.ws.ejbcontainer.injection.fat.tests.repeataction.RepeatWithEE10CDI;
import com.ibm.ws.ejbcontainer.injection.fat.tests.repeataction.RepeatWithEE11CDI;
import com.ibm.ws.ejbcontainer.injection.fat.tests.repeataction.RepeatWithEE9CDI;
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
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class InjectionMIXTest {

    @Server("ejbcontainer.injection.ra.fat.MsgEndpointServer")
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

    /*@formatter:off*/
    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES()
                                                    .fullFATOnly()
                                                    .forServers("ejbcontainer.injection.ra.fat.MsgEndpointServer"))
                                    .andWith(FeatureReplacementAction.EE8_FEATURES()
                                                    .forServers("ejbcontainer.injection.ra.fat.MsgEndpointServer"))
                                    .andWith(RepeatWithCDI.WithRepeatWithCDI()
                                                    .forServers("ejbcontainer.injection.ra.fat.MsgEndpointServer"))
                                    .andWith(FeatureReplacementAction.EE9_FEATURES()
                                                    .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11)
                                                    .forServers("ejbcontainer.injection.ra.fat.MsgEndpointServer"))
                                    .andWith(RepeatWithEE9CDI.EE9CDI_FEATURES()
                                                    .fullFATOnly()
                                                    .forServers("ejbcontainer.injection.ra.fat.MsgEndpointServer"))
                                    .andWith(FeatureReplacementAction.EE10_FEATURES()
                                                    .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17)
                                                    .forServers("ejbcontainer.injection.ra.fat.MsgEndpointServer"))
                                    .andWith(RepeatWithEE10CDI.EE10CDI_FEATURES()
                                                    .fullFATOnly()
                                                    .forServers("ejbcontainer.injection.ra.fat.MsgEndpointServer"))
                                    .andWith(FeatureReplacementAction.EE11_FEATURES()
                                                    .forServers("ejbcontainer.injection.ra.fat.MsgEndpointServer"))
                                    .andWith(RepeatWithEE11CDI.EE11CDI_FEATURES()
                                                    .fullFATOnly()
                                                    .forServers("ejbcontainer.injection.ra.fat.MsgEndpointServer"));
    /*@formatter:on*/

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the ear
        JavaArchive EJB3INJSMBeanJar = ShrinkHelper.buildJavaArchive("EJB3INJSMBean.jar", "com.ibm.ws.ejbcontainer.injection.mix.ejb.");
        EJB3INJSMBeanJar = (JavaArchive) ShrinkHelper.addDirectory(EJB3INJSMBeanJar, "test-applications/EJB3INJSMBean.jar/resources");

        JavaArchive EJB3INJINTMBeanJar = ShrinkHelper.buildJavaArchive("EJB3INJINTMBean.jar", "com.ibm.ws.ejbcontainer.injection.mix.ejbint.");
        EJB3INJINTMBeanJar = (JavaArchive) ShrinkHelper.addDirectory(EJB3INJINTMBeanJar, "test-applications/EJB3INJINTMBean.jar/resources");

        WebArchive EJB3INJSMWeb = ShrinkHelper.buildDefaultApp("EJB3INJSMWeb.war", "com.ibm.ws.ejbcontainer.injection.mix.web.");

        EnterpriseArchive EJB3INJSMTestApp = ShrinkWrap.create(EnterpriseArchive.class, "EJB3INJSMTestApp.ear");
        EJB3INJSMTestApp = (EnterpriseArchive) ShrinkHelper.addDirectory(EJB3INJSMTestApp, "test-applications/EJB3INJSMTestApp.ear/resources");
        EJB3INJSMTestApp.addAsModule(EJB3INJSMBeanJar).addAsModule(EJB3INJINTMBeanJar).addAsModule(EJB3INJSMWeb);

        ShrinkHelper.exportAppToServer(server, EJB3INJSMTestApp, DeployOptions.SERVER_ONLY);
        server.addInstalledAppForValidation("EJB3INJSMTestApp");

        //#################### AdapterForEJB.jar  (RAR Implementation)
        JavaArchive AdapterForEJBJar = ShrinkHelper.buildJavaArchive("AdapterForEJB.jar", "com.ibm.ws.ejbcontainer.fat.rar.*");
        ShrinkHelper.exportToServer(server, "ralib", AdapterForEJBJar, DeployOptions.SERVER_ONLY);

        //#################### AdapterForEJB.rar
        ResourceAdapterArchive AdapterForEJBRar = ShrinkWrap.create(ResourceAdapterArchive.class, "AdapterForEJB.rar");
        ShrinkHelper.addDirectory(AdapterForEJBRar, "test-resourceadapters/AdapterForEJB.rar/resources");
        ShrinkHelper.exportToServer(server, "connectors", AdapterForEJBRar, DeployOptions.SERVER_ONLY);

        server.startServer();

        if (RepeatTestFilter.getRepeatActionsAsString().endsWith("CDIENABLED")) {
            // verify the appSecurity-2.0 feature is ready
            assertNotNull("Security service did not report it was ready", server.waitForStringInLogUsingMark("CWWKS0008I"));
            assertNotNull("LTPA configuration did not report it was ready", server.waitForStringInLogUsingMark("CWWKS4105I"));
            assertNotNull("ORB did not report it was ready", server.waitForStringInLogUsingMark("CWWKI0001I"));
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        try {
            server.stopServer("J2CA8501E", "CNTR0168W", "CNTR0338W", "CWNEN0013W", "CNTR0020E", "CWWKS9582E");
        } finally {
            // Remove the appSecurity feature that was added by the CDI repeat actions
            if (RepeatWithCDI.isActive() || RepeatWithEE9CDI.isActive() || RepeatWithEE10CDI.isActive() || RepeatWithEE11CDI.isActive()) {
                File publishConfigFile = new File("publish/servers/ejbcontainer.injection.ra.fat.MsgEndpointServer/server.xml");
                ServerConfiguration config = ServerConfigurationFactory.fromFile(publishConfigFile);
                Set<String> features = config.getFeatureManager().getFeatures();
                for (String feature : features) {
                    if (feature.toLowerCase().startsWith("appsecurity-")) {
                        features.remove(feature);
                        ServerConfigurationFactory.toFile(publishConfigFile, config);
                        break;
                    }
                }
            }
        }
    }
}
