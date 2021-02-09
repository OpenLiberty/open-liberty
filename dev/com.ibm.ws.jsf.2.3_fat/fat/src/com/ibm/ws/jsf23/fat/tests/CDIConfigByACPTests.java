/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf23.fat.CDITestBase;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This is one of four CDI test applications, with configuration loaded in the following manner:
 * CDIConfigByACP - Application Configuration Populator loading of the class files
 *
 * We're extending CDITestBase, which has common test code.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CDIConfigByACPTests extends CDITestBase {

    @Server("jsfCDIConfigByACPServer")
    public static LibertyServer jsfCDIConfigByACPServer;

    @BeforeClass
    public static void setup() throws Exception {

        // Create the CDIConfigByACP jar that is used in CDIConfigByACP.war,
        JavaArchive cdiConfigByACPJar = ShrinkWrap.create(JavaArchive.class, "CDIConfigByACP.jar");
        cdiConfigByACPJar.addPackage("com.ibm.ws.jsf23.fat.cdi.common.beans");
        cdiConfigByACPJar.addPackage("com.ibm.ws.jsf23.fat.cdi.common.beans.factory");
        cdiConfigByACPJar.addPackage("com.ibm.ws.jsf23.fat.cdi.common.beans.injected");
        cdiConfigByACPJar.addPackage("com.ibm.ws.jsf23.fat.cdi.common.managed");
        cdiConfigByACPJar.addPackage("com.ibm.ws.jsf23.fat.cdi.common.managed.factories");
        cdiConfigByACPJar.addPackage("com.ibm.ws.jsf23.fat.cdi.common.managed.factories.client.window");
        cdiConfigByACPJar.addPackage("com.ibm.ws.jsf23.fat.cdi.jar.appconfigpop");
        ShrinkHelper.addDirectory(cdiConfigByACPJar, "test-applications/" + "CDIConfigByACP.jar" + "/resources");

        // Create the CDIConfigByACP.war application
        WebArchive cdiConfigByACPWar = ShrinkWrap.create(WebArchive.class, "CDIConfigByACP.war");
        cdiConfigByACPWar.addAsLibrary(cdiConfigByACPJar);
        cdiConfigByACPWar.addPackage("com.ibm.ws.jsf23.fat.cdi.appconfigpop");
        ShrinkHelper.addDirectory(cdiConfigByACPWar, "test-applications/" + "CDIConfigByACP.war" + "/resources");
        ShrinkHelper.exportToServer(jsfCDIConfigByACPServer, "dropins", cdiConfigByACPWar);

        // Start the server and use the class name so we can find logs easily.
        jsfCDIConfigByACPServer.startServer(CDIConfigByACPTests.class.getSimpleName() + ".log");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfCDIConfigByACPServer != null && jsfCDIConfigByACPServer.isStarted()) {
            jsfCDIConfigByACPServer.stopServer();
        }
    }

    /**
     * Test to ensure that CDI 2.0 injection works for a custom Navigation Handler
     * Field and Method injection, but no Constructor injection.
     * Also tested are use of request and session scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testNavigationHandlerInjection_CDIConfigByACP() throws Exception {
        testNavigationHandlerInjectionByApp("CDIConfigByACP", jsfCDIConfigByACPServer);
    }

    /**
     * Test to ensure that CDI 2.0 injection works for a custom EL Resolver
     * Field and Method injection, but no Constructor injection.
     * Also tested are use of request and session scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testELResolverInjection_CDIConfigByACP() throws Exception {
        testELResolverInjectionByApp("CDIConfigByACP", jsfCDIConfigByACPServer);
    }

    /**
     * Test method and field injection for Custom resource handler. No intercepter or constructor injection on this.
     *
     * Would like to do something more than look for message in logs, a future improvement.
     *
     * @throws Exception
     */
    @Test
    public void testCustomResourceHandlerInjections_CDIConfigByACP() throws Exception {
        testCustomResourceHandlerInjectionsByApp("CDIConfigByACP", jsfCDIConfigByACPServer);

    }

    /**
     * Test method and field injection on custom state manager. No intercepter or constructor tests on this.
     *
     * Would like to do something more than look for message in logs, a future improvement.
     *
     * @throws Exception
     */
    @Test
    public void testCustomStateManagerInjections_CDIConfigByACP() throws Exception {
        testCustomStateManagerInjectionsByApp("CDIConfigByACP", jsfCDIConfigByACPServer);
    }

    /**
     * Test that hits most of the managed factory classes, and system-event listener, and phase-listener. See faces-config.xml for details.
     * Most factories use delegate constructor method, so they are limited to tested basic field and method injection.
     *
     * Test Field and Method injection in system-event and phase listeners. No Constructor injection.
     *
     * Tests also use app scope as
     * request/session are not available to these managed classes that I can tell.
     *
     * @throws Exception
     */
    @Test
    public void testFactoryAndOtherScopeInjections_CDIConfigByACP() throws Exception {
        testFactoryAndOtherAppScopedInjectionsByApp("CDIConfigByACP", jsfCDIConfigByACPServer);
    }

}
