/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.clientcontainer.fat;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class JaxbClientTest {

    private static final Class<?> c = JaxbClientTest.class;
    protected static LibertyClient client;

    @Server("WebServiceRefTestServer")
    public static LibertyServer server;

    @Rule
    public TestName name = new TestName();

    // Packages are same for all tests
    private List<String> packageNames = Arrays.asList("com.ibm.jaxb.test.bean", "com.jaxb.test.servlet", "com.jaxb.test.util");

    @BeforeClass
    public static void setUp() throws Exception {
        String thisMethod = "setUp";

        Log.info(c, thisMethod, "setup complete ...");
    }

    /**
     * Testing individual clients specified by their name and the msg that gets emitted
     * once the client starts successfully
     *
     * @throws Exception
     */
    @Test
    public void testHelloFromJAXB() throws Exception {
        CommonApi.createClient("HelloFromJAXB", "HelloFromJAXB", "jaxbHello", "HelloFromJAXB", "<Customer id=\"1\">", packageNames, "jaxbHello", "jaxbHello");
    }

    @Test
    public void testNotUsingBootstrapClassLoader() throws Exception {
        CommonApi.createClient("TestNotUsingBootstrapClassLoader", "TestNotUsingBootstrapClassLoader", "jaxbHello", "TestNotUsingBootstrapClassLoader", "<Customer id=\"1\">",
                               packageNames, "jaxbHello", "jaxbHello");
    }

    @Test
    public void testUsingIBMImpl() throws Exception {
        CommonApi.createClient("TestUsingIBMImpl", "TestUsingIBMImpl", "jaxbHello", "HelloFromTestUsingIBMImplJAXB", "<Customer id=\"1\">", packageNames, "jaxbHello", "jaxbHello");
    }

}
