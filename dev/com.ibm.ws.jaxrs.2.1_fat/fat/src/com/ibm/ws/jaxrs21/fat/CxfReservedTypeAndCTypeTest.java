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
package com.ibm.ws.jaxrs21.fat;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTestAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

import typeAndCType.ClientTestServlet;

/**
 * CXF allows clients to specify query parameters to override the Accept and Content-Type headers,
 * specifically {@code _type} and {@code_ctype}, respectively. This is documented at
 * <a href="http://cxf.apache.org/docs/jax-rs.html#JAX-RS-Debugging">cxf.apache.org</a>.
 * This test suite verifies that these query parameters work by default (e.g. {@code ?_type=json} or
 * {@code ?_type=application/json} will set the Accept header to {@code application/json} even if the
 * actual header value was set to something else.
 * 
 * This test should also verify that these query parameters do NOT modify the headers when the user
 * has enabled the {@code NoOpRequestPreprocessor} via the {@code jaxrs.cxf.use.noop.requestPreprocessor=true}
 * system property.
 * 
 */
@SkipForRepeat(JakartaEE9Action.ID) // only applies to CXF, not RESTEasy
@RunWith(FATRunner.class)
public class CxfReservedTypeAndCTypeTest extends FATServletClient {

    private static final String app = "typeAndCType";

    @Server("jaxrs21.fat.typeAndCType")
    @TestServlet(servlet = ClientTestServlet.class, contextRoot = app)
    public static LibertyServer server;
    
    @ClassRule
    public static RepeatTests repeat = RepeatTests.withoutModification().andWith(new RepeatTestAction() {

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void setup() throws Exception {
            server.setAdditionalSystemProperties(
                Collections.singletonMap("jaxrs.cxf.use.noop.requestPreprocessor", "true"));
        }

        @Override
        public String getID() {
            return "WithUpdatedConfig";
        }});
    

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, app, "typeAndCType");

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
        if (server != null) {
            server.stopServer("CWWKW1002W");
        }
    }
}