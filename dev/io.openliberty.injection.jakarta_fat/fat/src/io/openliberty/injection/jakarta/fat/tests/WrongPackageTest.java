/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.injection.jakarta.fat.tests;

import static org.junit.Assert.assertEquals;

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

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * This test case verifies the behavior when the incorrect package version (javax vs jakarta)
 * of Jakarta EE Common Annotation APIs are used.
 */
@RunWith(FATRunner.class)
public class WrongPackageTest extends FATServletClient {
    public static final String WRONG_PACKAGE_PATH = "WrongPackageWeb/WrongPackageServlet";
    public static final String WRONG_PACKAGE_WEBAPP_PATH = "WrongPackageWebApp/WrongPackageWebAppServlet";

    private static final String CWWKM0483I_EJB_MOD_RESOURCE = "CWWKM0483I:.*javax.annotation.Resource.*WrongPackageEJB.*WrongPackageApp.*jakarta.annotation.Resource.*io.openliberty.injection.jakarta.ejb.JakartaSingletonBean, io.openliberty.injection.jakarta.ejb.JakartaSingletonResourcesBean, io.openliberty.injection.jakarta.ejb.JakartaStatelessBean";
    private static final String CWWKM0483I_EJB_MOD_PREDESTROY = "CWWKM0483I:.*javax.annotation.PreDestroy.*WrongPackageEJB.*WrongPackageApp.*jakarta.annotation.PreDestroy.*io.openliberty.injection.jakarta.ejb.JakartaStatefulPreDestroyBean";
    private static final String CWWKM0483I_WEB_MOD_RESOURCE = "CWWKM0483I:.*javax.annotation.Resource.*WrongPackageWeb.*WrongPackageApp.*jakarta.annotation.Resource.*io.openliberty.injection.jakarta.web.JakartaSingletonWarBean, io.openliberty.injection.jakarta.web.JakartaStatelessWarBean, io.openliberty.injection.jakarta.web.WrongPackageServlet";
    private static final String CWWKM0483I_WEB_MOD_POSTCONSTRUCT = "CWWKM0483I:.*javax.annotation.PostConstruct.*WrongPackageWeb.*WrongPackageApp.*jakarta.annotation.PostConstruct.*io.openliberty.injection.jakarta.web.JakartaStatelessWarPostConstructBean";
    private static final String CWWKM0483I_WEB_APP_RESOURCE = "CWWKM0483I:.*javax.annotation.Resource.*WrongPackageWebApp.*WrongPackageWebApp.*jakarta.annotation.Resource.*io.openliberty.injection.jakarta.webapp.WrongPackageWebAppServlet";

    @Server("InjectionJakartaServer")
    public static LibertyServer jakarta_server;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification() //
                    .andWith(FeatureReplacementAction.EE10_FEATURES().setSkipTransformation(true).forServers("InjectionJakartaServer")) //
                    .andWith(FeatureReplacementAction.EE11_FEATURES().setSkipTransformation(true).forServers("InjectionJakartaServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        JavaArchive WrongPackageEJB = ShrinkHelper.buildJavaArchive("WrongPackageEJB.jar", "io.openliberty.injection.jakarta.ejb.");
        WebArchive WrongPackageWeb = ShrinkHelper.buildDefaultApp("WrongPackageWeb.war", "io.openliberty.injection.jakarta.web.");
        EnterpriseArchive WrongPackageApp = ShrinkWrap.create(EnterpriseArchive.class, "WrongPackageApp.ear");
        WrongPackageApp.addAsModule(WrongPackageEJB).addAsModule(WrongPackageWeb);

        ShrinkHelper.exportDropinAppToServer(jakarta_server, WrongPackageApp, DeployOptions.SERVER_ONLY);

        WebArchive WrongPackageWebApp = ShrinkHelper.buildDefaultApp("WrongPackageWebApp.war", "io.openliberty.injection.jakarta.webapp.");

        ShrinkHelper.exportDropinAppToServer(jakarta_server, WrongPackageWebApp, DeployOptions.SERVER_ONLY);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (jakarta_server != null && jakarta_server.isStarted()) {
            jakarta_server.stopServer();
        }
    }

    /**
     * This test verifies an informational message is logged when the javax.annotation.Resource,
     * javax.annotation.Resources, javax.annotation.PostConstruct, and javax.annotation.PreDestory
     * annotations are used with Jakarta EE features and the annotation is ignored. There should
     * be one message per annotation type per module. Also, the jakarta.annotation.Resource
     * annotation works as expected.
     */
    @Test
    public void testWrongPackageCommonAnnotations() throws Exception {
        try {
            jakarta_server.startServer();
            assertEquals("Expected CWWKM0483I message not found for @Resource in EJB module : " + CWWKM0483I_EJB_MOD_RESOURCE, 1,
                         jakarta_server.findStringsInLogsUsingMark(CWWKM0483I_EJB_MOD_RESOURCE, jakarta_server.getDefaultLogFile()).size());
            assertEquals("Expected CWWKM0483I message not found for @PreDestroy in EJB module : " + CWWKM0483I_EJB_MOD_PREDESTROY, 1,
                         jakarta_server.findStringsInLogsUsingMark(CWWKM0483I_EJB_MOD_PREDESTROY, jakarta_server.getDefaultLogFile()).size());
            assertEquals("Expected CWWKM0483I message not found for @Resource in WEB module : " + CWWKM0483I_WEB_MOD_RESOURCE, 1,
                         jakarta_server.findStringsInLogsUsingMark(CWWKM0483I_WEB_MOD_RESOURCE, jakarta_server.getDefaultLogFile()).size());
            assertEquals("Expected CWWKM0483I message not found for @PostConstruct in WEB module : " + CWWKM0483I_WEB_MOD_POSTCONSTRUCT, 1,
                         jakarta_server.findStringsInLogsUsingMark(CWWKM0483I_WEB_MOD_POSTCONSTRUCT, jakarta_server.getDefaultLogFile()).size());
            jakarta_server.resetLogMarks(); // application start order undefined
            assertEquals("Expected CWWKM0483I message not found for @Resource in WEB application : " + CWWKM0483I_WEB_APP_RESOURCE, 1,
                         jakarta_server.findStringsInLogsUsingMark(CWWKM0483I_WEB_APP_RESOURCE, jakarta_server.getDefaultLogFile()).size());
            FATServletClient.runTest(jakarta_server, WRONG_PACKAGE_PATH, "testWrongPackageCommonAnnotations");
            FATServletClient.runTest(jakarta_server, WRONG_PACKAGE_WEBAPP_PATH, "testWrongPackageCommonAnnotations");
        } finally {
            if (jakarta_server != null && jakarta_server.isStarted()) {
                jakarta_server.stopServer();
            }
        }
    }

}