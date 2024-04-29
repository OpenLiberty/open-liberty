/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi41.internal.fat.el;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.cdi41.internal.fat.el.app.ELTestServlet;

@RunWith(FATRunner.class)
public class ELTest {

    public static final String APP_NAME = "elTest";

    @TestServlet(contextRoot = APP_NAME, servlet = ELTestServlet.class)
    @Server("ELServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(ELTestServlet.class.getPackage());
        CDIArchiveHelper.addBeansXML(app);

        ShrinkHelper.exportDropinAppToServer(server, app, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }
}
