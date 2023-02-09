/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.apache.myfaces40.fat.tests;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Programmatic Facelet Tests for Faces 4.0.
 *
 * https://github.com/jakartaee/faces/issues/1581
 *
 */
@RunWith(FATRunner.class)
public class ProgrammaticFaceletTests {

    private static final Logger LOG = Logger.getLogger(SimpleTest.class.getName());
    private static final String PROGRAMMATIC_FACELET_TEST_APP_NAME = "ProgrammaticFaceletTests";

    @Server("faces40_programmaticFaceletServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, PROGRAMMATIC_FACELET_TEST_APP_NAME + ".war",
                                      "io.openliberty.org.apache.faces40.fat.programmaticfacelettests");

        server.startServer(SimpleTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Basic test of the use of the @View annotation.
     *
     * @throws Exception
     */
    @Test
    public void testBasicView() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/" + PROGRAMMATIC_FACELET_TEST_APP_NAME + "/BasicView.xhtml", "Hello World!");
    }

    /**
     * Check the order of precedence between the same view defined in HTML and Java.
     * Java currently overrides what is defined in HTML.
     *
     * @throws Exception
     */
    @Mode(FULL)
    @Test
    public void testHtmlAndJavaDefinedView() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/" + PROGRAMMATIC_FACELET_TEST_APP_NAME + "/HtmlAndJava.xhtml", "Hello from Java!");
    }

    /**
     * Test the use of the @View annotation on a field which defines a Facelet.
     */
    @Test
    @Mode(FULL)
    public void testFieldDefinedView() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/" + PROGRAMMATIC_FACELET_TEST_APP_NAME + "/FieldView.xhtml", "@View on Field.");
    }

    /**
     * Test the use of the @View annotation on a method which returns a Facelet.
     */
    @Mode(FULL)
    @Test
    public void testMethodDefinedView() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/" + PROGRAMMATIC_FACELET_TEST_APP_NAME + "/MethodView.xhtml", "@View on Method.");
    }

    /**
     * Test the application of metadata in an @View defined view by passing a parameter on the URL.
     */
    @Test
    @Mode(FULL)
    public void testApplyMetadataInJavaDefinedView() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/" + PROGRAMMATIC_FACELET_TEST_APP_NAME + "/paramView.xhtml?name=Liberty", "Hello Liberty!");
    }
}
