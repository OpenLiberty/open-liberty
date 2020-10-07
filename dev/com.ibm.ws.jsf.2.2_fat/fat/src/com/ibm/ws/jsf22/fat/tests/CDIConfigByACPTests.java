/*
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.tests;

import static componenttest.annotation.SkipForRepeat.EE8_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf22.fat.CDITestBase;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This is one of four CDI test applications, with configuration loaded in the following manner:
 * CDIConfigByACP - Application Configuration Populator loading of the class files
 *
 * We're extending CDITestBase, which has common test code which is found under test-applications/CDICommon.
 *
 * NOTE: These tests should not run with jsf-2.3 feature because constructor injection is not supported.
 * As a result, these tests were modified to run in the JSF 2.3 FAT bucket without constructor injection.
 */
@Mode(TestMode.FULL)
@SkipForRepeat({ EE8_FEATURES, EE9_FEATURES })
@RunWith(FATRunner.class)
public class CDIConfigByACPTests extends CDITestBase {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "TestJSFEL";

    protected static final Class<?> c = CDIConfigByACPTests.class;

    @Server("jsfCDIConfigByACPServer")
    public static LibertyServer jsfCDIConfigByACPServer;

    @BeforeClass
    public static void setup() throws Exception {

        // CDIConfigByACP uses CDICommon packages
        JavaArchive CDIConfigByACPJar = ShrinkHelper.buildJavaArchive("CDIConfigByACP.jar", "com.ibm.ws.jsf22.fat.cdiconfigbyacp.jar",
                                                                      "com.ibm.ws.jsf22.fat.cdicommon.*");

        WebArchive CDIConfigByACPWar = ShrinkHelper.buildDefaultApp("CDIConfigByACP.war", "com.ibm.ws.jsf22.fat.cdiconfigbyacp");

        CDIConfigByACPWar.addAsLibraries(CDIConfigByACPJar);

        ShrinkHelper.exportDropinAppToServer(jsfCDIConfigByACPServer, CDIConfigByACPWar);

        jsfCDIConfigByACPServer.startServer(JSF22AppConfigPopTests.class.getSimpleName() + ".log");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfCDIConfigByACPServer != null && jsfCDIConfigByACPServer.isStarted()) {
            jsfCDIConfigByACPServer.stopServer();
        }
    }

    /**
     * Test to ensure that CDI 1.2 injection works for an custom action listener
     * Field, Method and Constructor Injection, and Interceptors. Also
     * tested are use of request and session scope and use of qualifiers.
     *
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testActionListenerInjection_CDIConfigByACP() throws Exception {
        testActionListenerInjectionByApp("CDIConfigByACP", jsfCDIConfigByACPServer);
    }

    /**
     * Test to ensure that CDI 1.2 injection works for a custom Navigation Handler
     * Field, Method and Constructor Injection, and Interceptors. Also
     * tested are use of request and session scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testNavigationHandlerInjection_CDIConfigByACP() throws Exception {
        testNavigationHandlerInjectionByApp("CDIConfigByACP", jsfCDIConfigByACPServer);
    }

    /**
     * Test to ensure that CDI 1.2 injection works for a custom EL Resolver
     * Field, Method and Constructor Injection, and Interceptors. Also
     * tested are use of request scope and use of qualifiers.
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
     * Most factories use delegate constructor method, so they are limited to tested basic field and method injection. Tests also use app scope as
     * request/session are not available to these managed classes that I can tell.
     *
     * @throws Exception
     */
    @Test
    public void testFactoryAndOtherScopeInjections_CDIConfigByACP() throws Exception {
        testFactoryAndOtherAppScopedInjectionsByApp("CDIConfigByACP", jsfCDIConfigByACPServer);
    }
}
