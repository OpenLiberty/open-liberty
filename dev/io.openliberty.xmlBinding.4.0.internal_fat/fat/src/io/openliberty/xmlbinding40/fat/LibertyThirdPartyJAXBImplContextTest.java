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
package io.openliberty.xmlbinding40.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jaxbimpl.thirdparty.web.ThirdPartyJAXBImplContextTestServlet;

/**
 * This test is intended to use the JAXBContext object to marshall and unmarshall various Java types on the Liberty runtime
 * using a user provided JAXB Implementation.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class LibertyThirdPartyJAXBImplContextTest extends FATServletClient {

    private static final String APP_NAME = "thirdPartyJaxbImplContextApp";

    @Server("jaxb_thirdpartyimpl_fat")
    @TestServlet(servlet = ThirdPartyJAXBImplContextTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "jaxbimpl.thirdparty.web", "jaxb.web.utils", "jaxb.web.dataobjects");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}