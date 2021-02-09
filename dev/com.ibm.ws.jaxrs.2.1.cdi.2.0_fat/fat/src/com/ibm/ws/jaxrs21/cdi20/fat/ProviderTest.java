/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.cdi20.fat;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jaxrs21.fat.provider.ProviderTestServlet;


@RunWith(FATRunner.class)
public class ProviderTest extends FATServletClient {

    private static final String appName = "provider";
    
    @Server("jaxrs21.fat.provider")
    @TestServlet(servlet = ProviderTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, appName, "jaxrs21.fat.provider");
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        //verify results of the test
        assertStatesExsited(5000, new String[] {
                                                "isReadable Hello",
                                                "readFrom Hello",
                                                "isWriteable Hello",
                                                "writeTo Hello",                                                
                                                "post1",
                                                "ApplicationInjectionProxy",                                               
                                                "WSJdbcDataSource"
        });       
        
        server.stopServer("CWWKW1002W");
    }
    
    private static void assertStatesExsited(long timeout, String... states) {
        String findStr = null;
        if (states != null && states.length != 0) {
            for (String state : states) {
                findStr = server.waitForStringInLog(state, timeout);
                assertTrue("Unable to find the output [" + state + "]  in the server log", findStr != null);
            }
        }
    }
}
