/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.jaxrs21.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs21.fat.exception.ExceptionClientTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

/*
 * The purpose of this test is to ensure that exceptions thrown during resource method processing in an application
 * with a com.ibm.wsspi.webcontainer.collaborator.WebAppInvocationCollaborator user feature defined will be reflected
 * in the com.ibm.wsspi.webcontainer.servlet.ServletRequestExtended SPI.
 */
@RunWith(FATRunner.class)
public class ExceptionTest extends FATServletClient {

    private static final String appName = "exception";

    @Server("com.ibm.ws.jaxrs21.fat.exception")
    @TestServlet(servlet = ExceptionClientTestServlet.class, contextRoot = appName)
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jaxrs21.fat.exception");

    
    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultUserFeatureArchive(server, "TestWebAppInvocationCollaboratorBundle", "test.collaborator");
        if (JakartaEEAction.isEE9OrLaterActive()) {
            server.installUserFeature("ee9/" + "MyWebAppInvocationCollaboratorFeature");
        } else {
            server.installUserFeature("MyWebAppInvocationCollaboratorFeature");
        }
      
        ShrinkHelper.defaultDropinApp(server, appName, "com.ibm.ws.jaxrs21.fat.exception");
        

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer("Exception.log", true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
    /*
     *  Ensure that the postInvoke() method in the user feature has been invoked and that the 
     *  expected exception is obtained from the SPI.   
     */
    @After
    public void checkLogs() throws Exception {
        assertNotNull("WebAppInvocationCollaborator postinvoke not executed as expected",server.waitForStringInLogUsingMark("TestWebAppInvocationCollaborator postInvoke "));
        assertNotNull("WebAppInvocationCollaborator did not get expected exception", server.waitForStringInLogUsingMark("TestWebAppInvocationCollaborator CurrentException ="));
        //Move mark to end of log after each test in the servlet.
        server.setMarkToEndOfLog();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("SRVE0777E","SRVE0315E"); // ignore expected exceptions in server logs

        if (server != null) {
            server.stopServer("CWWKE1102W","SRVE0777E","SRVE0315E");  //ignore server quiesce timeouts due to slow test machines
            server.uninstallUserBundle("TestWebAppInvocationCollaboratorBundle");
            server.uninstallUserFeature("MyWebAppInvocationCollaboratorFeature");

        }
    }

    @Before
    public void beforeTest() {}

    @After
    public void afterTest() {}
}