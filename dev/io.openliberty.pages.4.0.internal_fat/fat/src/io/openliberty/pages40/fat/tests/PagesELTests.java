/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.pages40.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import io.openliberty.pages40.fat.JSPUtils;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

/**
 * Test Pages 4.0 EL Changes. 
 *  - Verifies Expression Language Resolvers and Order
 */
@RunWith(FATRunner.class)
public class PagesELTests {
    private static final Logger LOG = Logger.getLogger(PagesELTests.class.getName());
    private static final String TestEL_APP_NAME = "TestEL";

    @Server("pagesEL40Server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server,
                                      TestEL_APP_NAME + ".war",
                                      "com.ibm.ws.jsp23.fat.testel.servlets");

        server.startServer(PagesELTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test Pages 4.0 Resolution of Variables and their Properties
     *
     * Test copied and updated from 2.3 FAT: JSPTests#testJSP23ResolutionVariableProperties
     * @throws Exception
     *                       if something goes wrong
     */
    @Test
    public void testPages40ResolutionVariableProperties() throws Exception {
        String[] expectedInResponse = { "class org.apache.el.stream.StreamELResolverImpl",
                                        "class jakarta.el.StaticFieldELResolver",
                                        "class jakarta.el.MapELResolver",
                                        "class jakarta.el.ResourceBundleELResolver",
                                        "class jakarta.el.ListELResolver",
                                        "class jakarta.el.ArrayELResolver",
                                        "class jakarta.el.BeanELResolver",
                                        "The order and number of ELResolvers from the CompositeELResolver are correct!",
                                        "Testing StaticFieldELResolver with Boolean.TRUE (Expected: true): true",
                                        "Testing StaticFieldELResolver with Integer.parseInt (Expected: 86): 86",
                                        "Testing StreamELResolver with distinct method (Expected: [1, 4, 3, 2, 5]): [1, 4, 3, 2, 5]",
                                        "Testing StreamELResolver with filter method (Expected: [4, 3, 5, 3]): [4, 3, 5, 3]" };

        this.verifyStringsInResponse(TestEL_APP_NAME, "ResolutionVariablesPropertiesServlet", expectedInResponse);

    }

    private void verifyStringsInResponse(String contextRoot, String path, String[] expectedResponseStrings) throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(JSPUtils.createHttpUrlString(server, contextRoot, path));
        WebResponse response = wc.getResponse(request);
        LOG.info("Response : " + response.getText());

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());

        String responseText = response.getText();

        for (String expectedResponse : expectedResponseStrings) {
            assertTrue("The response did not contain: " + expectedResponse, responseText.contains(expectedResponse));
        }
    }
}
