/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.jakarta.validation.v40.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import val40.web.Validation40TestServlet;
import val40attr.web.ConstraintDescriptorTestServlet;
import val40init.web.ConstraintValidatorInitContextTestServlet;

@RunWith(FATRunner.class)
public class Validation40Test extends FATServletClient {

    public static final String APP_NAME      = "val40";
    public static final String APP_NAME_ATTR = "val40attr";
    public static final String APP_NAME_INIT = "val40init";

    @Server("validation.v40.fat")
    @TestServlets({
        @TestServlet(servlet = Validation40TestServlet.class,                   contextRoot = APP_NAME),
        @TestServlet(servlet = ConstraintDescriptorTestServlet.class,           contextRoot = APP_NAME_ATTR),
        @TestServlet(servlet = ConstraintValidatorInitContextTestServlet.class, contextRoot = APP_NAME_INIT),
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME,      "val40.web");
        ShrinkHelper.defaultApp(server, APP_NAME_ATTR, "val40attr.web");
        ShrinkHelper.defaultApp(server, APP_NAME_INIT, "val40init.web");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
