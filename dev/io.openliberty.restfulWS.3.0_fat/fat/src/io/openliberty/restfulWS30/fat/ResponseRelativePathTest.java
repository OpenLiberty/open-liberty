/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.restfulWS30.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.restfulWS30.fat.responserelativepath.ResponseRelativePathClientTestServlet;

/*
 * The purpose of this test is to ensure that the following change to the RestfulWS specification was implemented correctly:
 * "Response.created(URI) now resolves relative URIs into an absolute URI against the base URI, not against the
 * request URI anymore."
 */
@RunWith(FATRunner.class)
public class ResponseRelativePathTest extends FATServletClient {

    public static final String APP_NAME = "responserelativepath";
    public static final String SERVER_NAME = APP_NAME;
    
    @Server(SERVER_NAME)
    @TestServlet(servlet = ResponseRelativePathClientTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Build the application
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, ResponseRelativePathClientTestServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);
        
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer("CWWKE1102W");  //ignore server quiesce timeouts due to slow test machines
        }
    }
    
}
