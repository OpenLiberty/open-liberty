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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf23.fat.CDITestBase;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This is one of four CDI test applications, with configuration loaded in the following manner:
 * CDIFacesInWebXML - web.xml param that points to two faces-config files.
 *
 * We're extending CDITestBase, which has common test code.
 */
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 8)
@RunWith(FATRunner.class)
public class CDIFacesInWebXMLTests extends CDITestBase {

    @Server("jsfCDIFacesInWebXMLServer")
    public static LibertyServer jsfCDIFacesInWebXMLServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsfCDIFacesInWebXMLServer, "CDIFacesInWebXML.war",
                                      "com.ibm.ws.jsf23.fat.cdi.common.beans",
                                      "com.ibm.ws.jsf23.fat.cdi.common.beans.factory",
                                      "com.ibm.ws.jsf23.fat.cdi.common.beans.injected",
                                      "com.ibm.ws.jsf23.fat.cdi.common.managed",
                                      "com.ibm.ws.jsf23.fat.cdi.common.managed.factories",
                                      "com.ibm.ws.jsf23.fat.cdi.common.managed.factories.client.window");

        // Start the server and use the class name so we can find logs easily.
        jsfCDIFacesInWebXMLServer.startServer(CDIFacesInWebXMLTests.class.getSimpleName() + ".log");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfCDIFacesInWebXMLServer != null && jsfCDIFacesInWebXMLServer.isStarted()) {
            jsfCDIFacesInWebXMLServer.stopServer();
        }
    }

    /**
     * Test to ensure that CDI 2.0 injection works for a custom Navigation Handler
     * Field and Method injection, but no Constructor injection.
     * Also tested are use of request and session scope and use of qualifiers.
     *
     * We have a single source location of test-application-common for all application classes. build-test.xml contains the logic that
     * compiles this source directory and copies half the managed classes into the app's jar file and half into the app's war files.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testNavigationHandlerInjection_CDIFacesInWebXML() throws Exception {
        testNavigationHandlerInjectionByApp("CDIFacesInWebXML", jsfCDIFacesInWebXMLServer);
    }

    /**
     * Test to ensure that CDI 2.0 injection works for a custom EL Resolver
     * Field and Method injection but no Constructor Injection.
     * Also tested are use of request scope and use of qualifiers.
     *
     * We have a single source location of test-application-common for all application classes. build-test.xml contains the logic that
     * compiles this source directory and copies half the managed classes into the app's jar file and half into the app's war files.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testELResolverInjection_CDIFacesInWebXML() throws Exception {
        testELResolverInjectionByApp("CDIFacesInWebXML", jsfCDIFacesInWebXMLServer);
    }

    /**
     * Test method and field injection for Custom resource handler. No intercepter or constructor injection on this.
     *
     * Would like to do something more than look for message in logs, a future improvement.
     *
     * We have a single source in test-application-common for all application classes. build-test.xml contains the logic that
     * compiles this source directory and splits the copying managec classes the class files into app's jar/war files.
     *
     * @throws Exception
     */
    @Test
    public void testCustomResourceHandlerInjections_CDIFacesInWebXML() throws Exception {
        testCustomResourceHandlerInjectionsByApp("CDIFacesInWebXML", jsfCDIFacesInWebXMLServer);

    }

    /**
     * Test method and field injection on custom state manager. No intercepter or constructor tests on this.
     *
     * Would like to do something more than look for message in logs, a future improvement.
     *
     * We have a single source in test-application-common for all application classes. build-test.xml contains the logic that
     * compiles this source directory and splits the copying managec classes the class files into app's jar/war files.
     *
     * @throws Exception
     */
    @Test
    public void testCustomStateManagerInjections_CDIFacesInWebXML() throws Exception {
        testCustomStateManagerInjectionsByApp("CDIFacesInWebXML", jsfCDIFacesInWebXMLServer);
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
     * We have a single source location of test-application-common for all application classes. build-test.xml contains the logic that
     * compiles this source directory and copies half the managed classes into the app's jar file and half into the app's war files.
     *
     * @throws Exception
     */
    @Test
    public void testFactoryAndOtherScopeInjections_CDIFacesInWebXML() throws Exception {
        testFactoryAndOtherAppScopedInjectionsByApp("CDIFacesInWebXML", jsfCDIFacesInWebXMLServer);
    }

}
