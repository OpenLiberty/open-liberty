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
package com.ibm.ws.app.manager.wab.installer.fat;

import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

/**
 *
 */
@RunWith(FATRunner.class)
public class WabAdditionalTests extends AbstractWABTests {

    private static final String SERVLET = "/WebDescriptorServlet";
    private static final String OUTPUT_WEB_XML_SERVLET = "service: test.wab1.WebXmlMapped";

    @BeforeClass
    public static void startServer() throws Exception {
        server.startServer(WabAdditionalTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testWebXmlDescrptor() throws Exception {
        checkWAB(PRODUCT1 + SERVLET, OUTPUT_WEB_XML_SERVLET);
    }

    @Test
    public void testGetStaticContent() throws Exception {
        checkWAB(PRODUCT1 + "/hello.html", "This is static content.");
    }

    @Test
    public void testOSGIProtectionFilter() throws Exception {
        try {
            checkWAB(PRODUCT1 + "/OSGI-INF/internal.txt", "internal stuff");
            fail("Expected 404 on get of internal resource");
        } catch (AssertionError e) {
            if (!e.getMessage().matches(".*Expected response 200 .*received 404.*")) {
                throw e;
            }
        }
    }
}
