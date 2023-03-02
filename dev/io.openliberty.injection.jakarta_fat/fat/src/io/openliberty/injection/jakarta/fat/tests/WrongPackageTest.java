/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * This test case verifies the behavior when the incorrect package version (javax vs jakarta)
 * of Jakarta EE Common Annotation APIs are used.
 */
@RunWith(FATRunner.class)
public class WrongPackageTest extends FATServletClient {
    public static final String WRONG_PACKAGE_PATH = "WrongPackageWeb/WrongPackageServlet";

    private static final String CWWKM0483I_EJB_MOD_RESOURCE = "CWWKM0483I:.*javax.annotation.Resource.*WrongPackageEJB.*jakarta.annotation.Resource.*io.openliberty.injection.jakarta.ejb.JakartaSingletonBean, io.openliberty.injection.jakarta.ejb.JakartaSingletonResourcesBean, io.openliberty.injection.jakarta.ejb.JakartaStatelessBean";
    private static final String CWWKM0483I_EJB_MOD_PREDESTROY = "CWWKM0483I:.*javax.annotation.PreDestroy.*WrongPackageEJB.*jakarta.annotation.PreDestroy.*io.openliberty.injection.jakarta.ejb.JakartaStatefulPreDestroyBean";
    private static final String CWWKM0483I_WEB_MOD_RESOURCE = "CWWKM0483I:.*javax.annotation.Resource.*WrongPackageWeb.*jakarta.annotation.Resource.*io.openliberty.injection.jakarta.web.JakartaSingletonWarBean, io.openliberty.injection.jakarta.web.JakartaStatelessWarBean, io.openliberty.injection.jakarta.web.WrongPackageServlet";
    private static final String CWWKM0483I_WEB_MOD_POSTCONSTRUCT = "CWWKM0483I:.*javax.annotation.PostConstruct.*WrongPackageWeb.*jakarta.annotation.PostConstruct.*io.openliberty.injection.jakarta.web.JakartaStatelessWarPostConstructBean";

    @Server("InjectionJakartaEE9Server")
    public static LibertyServer ee9server;

    @Server("InjectionJakartaEE10Server")
    public static LibertyServer ee10server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        JavaArchive WrongPackageEJB = ShrinkHelper.buildJavaArchive("WrongPackageEJB.jar", "io.openliberty.injection.jakarta.ejb.");
        WebArchive WrongPackageWeb = ShrinkHelper.buildDefaultApp("WrongPackageWeb.war", "io.openliberty.injection.jakarta.web.");
        EnterpriseArchive WrongPackageApp = ShrinkWrap.create(EnterpriseArchive.class, "WrongPackageApp.ear");
        WrongPackageApp.addAsModule(WrongPackageEJB).addAsModule(WrongPackageWeb);

        ShrinkHelper.exportDropinAppToServer(ee9server, WrongPackageApp, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(ee10server, WrongPackageApp, DeployOptions.SERVER_ONLY);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (ee9server != null && ee9server.isStarted()) {
            ee9server.stopServer();
        }
        if (ee10server != null && ee10server.isStarted()) {
            ee10server.stopServer();
        }
    }

    /**
     * This test verifies an informational message is logged when the javax.annotation.Resource,
     * javax.annotation.Resources, javax.annotation.PostConstruct, and javax.annotation.PreDestory
     * annotations are used with Jakarta EE 9 features and the annotation is ignored. There should
     * be one message per annotation type per module. Also, the jakarta.annotation.Resource
     * annotation works as expected.
     */
    @Test
    public void testWrongPackageCommonAnnotations_EE9() throws Exception {
        try {
            ee9server.startServer();
            assertEquals("Expected CWWKM0483I message not found for @Resource in EJB module : " + CWWKM0483I_EJB_MOD_RESOURCE, 1,
                         ee9server.findStringsInLogsUsingMark(CWWKM0483I_EJB_MOD_RESOURCE, ee9server.getDefaultLogFile()).size());
            assertEquals("Expected CWWKM0483I message not found for @PreDestroy in EJB module : " + CWWKM0483I_EJB_MOD_PREDESTROY, 1,
                         ee9server.findStringsInLogsUsingMark(CWWKM0483I_EJB_MOD_PREDESTROY, ee9server.getDefaultLogFile()).size());
            assertEquals("Expected CWWKM0483I message not found for @Resource in WEB module : " + CWWKM0483I_WEB_MOD_RESOURCE, 1,
                         ee9server.findStringsInLogsUsingMark(CWWKM0483I_WEB_MOD_RESOURCE, ee9server.getDefaultLogFile()).size());
            assertEquals("Expected CWWKM0483I message not found for @PostConstruct in WEB module : " + CWWKM0483I_WEB_MOD_POSTCONSTRUCT, 1,
                         ee9server.findStringsInLogsUsingMark(CWWKM0483I_WEB_MOD_POSTCONSTRUCT, ee9server.getDefaultLogFile()).size());
            FATServletClient.runTest(ee9server, WRONG_PACKAGE_PATH, "testWrongPackageCommonAnnotations");
        } finally {
            if (ee9server != null && ee9server.isStarted()) {
                ee9server.stopServer();
            }
        }
    }

    /**
     * This test verifies an informational message is logged when the javax.annotation.Resource,
     * javax.annotation.Resources, javax.annotation.PostConstruct, and javax.annotation.PreDestory
     * annotations are used with Jakarta EE 10 features and the annotation is ignored. There should
     * be one message per annotation type per module. Also, the jakarta.annotation.Resource
     * annotation works as expected.
     */
    @Test
    @MinimumJavaLevel(javaLevel = 11)
    public void testWrongPackageCommonAnnotations_EE10() throws Exception {
        try {
            ee10server.startServer();
            assertEquals("Expected CWWKM0483I message not found for @Resource in EJB module : " + CWWKM0483I_EJB_MOD_RESOURCE, 1,
                         ee10server.findStringsInLogsUsingMark(CWWKM0483I_EJB_MOD_RESOURCE, ee10server.getDefaultLogFile()).size());
            assertEquals("Expected CWWKM0483I message not found for @PreDestroy in EJB module : " + CWWKM0483I_EJB_MOD_PREDESTROY, 1,
                         ee10server.findStringsInLogsUsingMark(CWWKM0483I_EJB_MOD_PREDESTROY, ee10server.getDefaultLogFile()).size());
            assertEquals("Expected CWWKM0483I message not found for @Resource in WEB module : " + CWWKM0483I_WEB_MOD_RESOURCE, 1,
                         ee10server.findStringsInLogsUsingMark(CWWKM0483I_WEB_MOD_RESOURCE, ee10server.getDefaultLogFile()).size());
            assertEquals("Expected CWWKM0483I message not found for @PostConstruct in WEB module : " + CWWKM0483I_WEB_MOD_POSTCONSTRUCT, 1,
                         ee10server.findStringsInLogsUsingMark(CWWKM0483I_WEB_MOD_POSTCONSTRUCT, ee10server.getDefaultLogFile()).size());
            FATServletClient.runTest(ee10server, WRONG_PACKAGE_PATH, "testWrongPackageCommonAnnotations");
        } finally {
            if (ee10server != null && ee10server.isStarted()) {
                ee10server.stopServer();
            }
        }
    }

}