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
 * CDIFacesInWebXML - web.xml param that points to two faces-config files.
 *
 * We're extending CDITestBase, which has common test code which is found under test-applications/CDICommon.
 *
 * NOTE: These tests should not run with jsf-2.3 feature because constructor injection is not supported.
 * As a result, these tests were modified to run in the JSF 2.3 FAT bucket without constructor injection.
 */
@Mode(TestMode.FULL)
@SkipForRepeat({ EE8_FEATURES, EE9_FEATURES })
@RunWith(FATRunner.class)
public class CDIFacesInWebXMLTests extends CDITestBase {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "TestJSFEL";

    protected static final Class<?> c = CDIFacesInWebXMLTests.class;

    @Server("jsfCDIFacesInWebXMLServer")
    public static LibertyServer jsfCDIFacesInWebXMLServer;

    @BeforeClass
    public static void setup() throws Exception {

        // CDIFacesInWebXML uses CDICommon packages
        WebArchive war = ShrinkHelper.buildDefaultApp("CDIFacesInWebXML.war", "com.ibm.ws.jsf22.fat.cdicommon.*");

        ShrinkHelper.exportDropinAppToServer(jsfCDIFacesInWebXMLServer, war);

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
     * Test to ensure that CDI 1.2 injection works for an custom action listener
     * Field, Method and Constructor Injection, and Interceptors. Also
     * tested are use of request and session scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testActionListenerInjection_CDIFacesInWebXML() throws Exception {
        testActionListenerInjectionByApp("CDIFacesInWebXML", jsfCDIFacesInWebXMLServer);
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
    public void testNavigationHandlerInjection_CDIFacesInWebXML() throws Exception {
        testNavigationHandlerInjectionByApp("CDIFacesInWebXML", jsfCDIFacesInWebXMLServer);
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
    public void testELResolverInjection_CDIFacesInWebXML() throws Exception {
        testELResolverInjectionByApp("CDIFacesInWebXML", jsfCDIFacesInWebXMLServer);
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
        testCustomResourceHandlerInjectionsByApp("CDIFacesInWebXML", jsfCDIFacesInWebXMLServer);

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
        testCustomStateManagerInjectionsByApp("CDIFacesInWebXML", jsfCDIFacesInWebXMLServer);
    }

    /**
     * Test that hits most of the managed factory classes, and system-event listener, and phase-listener. See faces-config.xml for details.
     * Most factories use delegate constructor method, so they are limited to tested basic field and method injection. Tests also use app scope as
     * request/session are not available to these managed classes that I can tell.
     *
     * @throws Exception
     */
    @Test
    public void testFactoryAndOtherScopeInjections_CDIFacesInWebXML() throws Exception {
        testFactoryAndOtherAppScopedInjectionsByApp("CDIFacesInWebXML", jsfCDIFacesInWebXMLServer);
    }
}
