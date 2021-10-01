/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs2x.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

/**
 * An unmapped app is when a JAX-RS application module contains (or can reference) an
 * Application subclass, but that Application subclass is not referenced in the web.xml
 * and it does not contain an {@code @ApplicationPath} annotation. Thus the Application
 * subclass cannot be mapped to an HTTP path.
 * If this behavior is detected, ideally a warning should be printed, but ultimately the
 * Application subclass should be ignored.
 */
@RunWith(FATRunner.class)
public class UnmappedAppTest {

    @Server("jaxrs2x.unmappedApp.UnmappedAppTest")
    public static LibertyServer server;

    private static final String warName = "unmappedApp";

    private static String webXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<web-app id=\"UnmappedApp\" version=\"3.0\" \n"
                    + "     xmlns=\"http://java.sun.com/xml/ns/javaee\"\n"
                    + "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                    + "     xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee\n"
                    + "     http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\">\n"
                    + "\n"
                    + "  <servlet id=\"Servlet_1\">\n"
                    + "    <servlet-name>%s.ws.rs.core.Application</servlet-name>\n"
                    + "  </servlet>\n"
                    + "  <servlet-mapping id=\"ServletMapping_1\">\n"
                    + "    <servlet-name>%s.ws.rs.core.Application</servlet-name>\n"
                    + "    <url-pattern>/app/*</url-pattern>\n"
                    + "  </servlet-mapping>\n"
                    + "</web-app>";
    @BeforeClass
    public static void setup() throws Exception {

        WebArchive war = ShrinkHelper.buildDefaultAppFromPathNoResources(warName, null, "jaxrs2x.unmappedApp");
        String pkgPrefix = JakartaEE9Action.isActive() ? "jakarta" : "javax";
        war.addAsWebInfResource(new StringAsset(String.format(webXml, pkgPrefix, pkgPrefix)), "web.xml");
        ShrinkHelper.exportDropinAppToServer(server, war);

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testCanAccessResourceInAppWithUnmappedApplication() throws Exception {
        String url = TestUtils.getBaseTestUri(warName, "app", "resource");
        String resp = TestUtils.accessWithURLConn(url, "GET", null, 200, null);
        assertNotNull(resp);
        assertEquals("hello", resp.trim());

        if (JakartaEE9Action.isActive()) {
            assertFalse(server.findStringsInLogs("CWWKW1302W").isEmpty());
        }
    }
}