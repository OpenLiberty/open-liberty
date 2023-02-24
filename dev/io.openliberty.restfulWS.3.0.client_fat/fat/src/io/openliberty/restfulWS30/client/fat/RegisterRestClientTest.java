/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
package io.openliberty.restfulWS30.client.fat;

import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import annotation.test.rest.RestApplication;

/**
 * Tests that @RegisterRestClient does not cause an UnsatisfiedResolutionException, see https://github.com/OpenLiberty/open-liberty/issues/21547
 */
@AllowedFFDC
@RunWith(FATRunner.class)
public class RegisterRestClientTest extends FATServletClient {

    public static final String APP_NAME = "RegisterRestClient";
    public static final String SERVER_NAME = "RegisterRestClientServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, RestApplication.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @Test
    public void registerRestClientTest() {
       assertTrue(true); //We're really testing for a lack of error messages in the logs.
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKE1102W","CWWKE1106W","CWWKE1107W");  //ignore server quiesce timeouts due to slow test machines
    }

}
