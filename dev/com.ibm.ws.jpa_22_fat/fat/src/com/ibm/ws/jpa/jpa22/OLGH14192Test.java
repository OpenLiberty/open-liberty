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

package com.ibm.ws.jpa.jpa22;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jpa.FATSuite;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.PrivHelper;
import olgh14192.OLGH14192TestServlet;

@RunWith(FATRunner.class)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
public class OLGH14192Test extends FATServletClient {
    public static final String APP_NAME = "OLGH14192";
    public static final String SERVLET = "OLGH14192TestServlet";

    @Server("OLGH14192Server")
    @TestServlet(servlet = OLGH14192TestServlet.class, path = APP_NAME + "/" + SERVLET)
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        app.addPackage("olgh14192");
        ShrinkHelper.exportAppToServer(server1, app);
        server1.addInstalledAppForValidation(APP_NAME);

        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer("CWWJP9991W");
    }
}
