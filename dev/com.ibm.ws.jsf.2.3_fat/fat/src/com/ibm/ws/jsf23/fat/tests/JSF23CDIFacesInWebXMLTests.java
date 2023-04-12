/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
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
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.topology.impl.LibertyServer;

/**
 * This is one of four CDI test applications, with configuration loaded in the following manner:
 * CDIFacesInWebXML - web.xml param that points to two faces-config files.
 *
 * We're extending CDITestBase, which has common test code.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF23CDIFacesInWebXMLTests extends CDITestBase {

    protected static final Class<?> c = JSF23CDIFacesInWebXMLTests.class;
    private static final String APP_NAME = "CDIFacesInWebXML.war";

    @Server("jsf23CDIFacesInWebXMLServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        boolean isEE10 = JakartaEE10Action.isActive();

        // Include @Named beans if Faces 4.0 is being tested. Include @ManagedBean beans otherwise.
        // Include correct resources directory, since the faces-config.xml points to different CustomNavigationHandler classes.
        if (isEE10) {
            WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME);
            app.addPackages(false, "com.ibm.ws.jsf23.fat.cdi.common.beans.faces40",
                            "com.ibm.ws.jsf23.fat.cdi.common.beans.factory",
                            "com.ibm.ws.jsf23.fat.cdi.common.beans.injected",
                            "com.ibm.ws.jsf23.fat.cdi.common.managed",
                            "com.ibm.ws.jsf23.fat.cdi.common.managed.faces40",
                            "com.ibm.ws.jsf23.fat.cdi.common.managed.factories",
                            "com.ibm.ws.jsf23.fat.cdi.common.managed.factories.client.window");
            ShrinkHelper.addDirectory(app, "test-applications/" + APP_NAME + "/resources");
            ShrinkHelper.addDirectory(app, "test-applications/" + APP_NAME + "/resourcesFaces40");
            ShrinkHelper.exportDropinAppToServer(server, app);
        } else {
            WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME);
            app.addPackages(false, "com.ibm.ws.jsf23.fat.cdi.common.beans.jsf23",
                            "com.ibm.ws.jsf23.fat.cdi.common.beans.factory",
                            "com.ibm.ws.jsf23.fat.cdi.common.beans.injected",
                            "com.ibm.ws.jsf23.fat.cdi.common.managed",
                            "com.ibm.ws.jsf23.fat.cdi.common.managed.jsf23",
                            "com.ibm.ws.jsf23.fat.cdi.common.managed.factories",
                            "com.ibm.ws.jsf23.fat.cdi.common.managed.factories.client.window");
            ShrinkHelper.addDirectory(app, "test-applications/" + APP_NAME + "/resources");
            ShrinkHelper.addDirectory(app, "test-applications/" + APP_NAME + "/resourcesJSF23");
            ShrinkHelper.exportDropinAppToServer(server, app);
        }

        // Start the server and use the class name so we can find logs easily.
        server.startServer(c.getSimpleName() + ".log");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
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
    public void testNavigationHandlerInjection_CDIFacesInWebXML() throws Exception {
        testNavigationHandlerInjectionByApp("CDIFacesInWebXML", server);
    }

    /**
     * Test to ensure that CDI 2.0 injection works for a custom EL Resolver
     * Field and Method injection but no Constructor Injection.
     * Also tested are use of request scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testELResolverInjection_CDIFacesInWebXML() throws Exception {
        testELResolverInjectionByApp("CDIFacesInWebXML", server);
    }

    /**
     * Test method and field injection for Custom resource handler. No intercepter or constructor injection on this.
     *
     * Would like to do something more than look for message in logs, a future improvement.
     *
     * @throws Exception
     */
    @Test
    public void testCustomResourceHandlerInjections_CDIFacesInWebXML() throws Exception {
        testCustomResourceHandlerInjectionsByApp("CDIFacesInWebXML", server);

    }

    /**
     * Test method and field injection on custom state manager. No intercepter or constructor tests on this.
     *
     * Would like to do something more than look for message in logs, a future improvement.
     *
     * @throws Exception
     */
    @Test
    public void testCustomStateManagerInjections_CDIFacesInWebXML() throws Exception {
        testCustomStateManagerInjectionsByApp("CDIFacesInWebXML", server);
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
    public void testFactoryAndOtherScopeInjections_CDIFacesInWebXML() throws Exception {
        testFactoryAndOtherAppScopedInjectionsByApp("CDIFacesInWebXML", server);
    }

}
