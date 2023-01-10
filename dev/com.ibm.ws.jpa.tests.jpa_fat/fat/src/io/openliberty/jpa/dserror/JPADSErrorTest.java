/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package io.openliberty.jpa.dserror;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jpa.fat.dserror.web.DSErrorServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class JPADSErrorTest {
    public static final String APP_NAME = "dserror";
    public static final String appPath = APP_NAME + "/DSErrorServlet";

    @Server("com.ibm.ws.jpa.fat.dserror")
    @TestServlets({
                    @TestServlet(servlet = DSErrorServlet.class, path = appPath)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")//
                        .addPackage("com.ibm.ws.jpa.fat.dserror.web");
        ShrinkHelper.addDirectory(app, "test-applications/" + APP_NAME + "/resources/");
        ShrinkHelper.exportToServer(server, "apps", app);

        server.addInstalledAppForValidation(APP_NAME);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CWWJP0015E", "CWNEN0035E", "CWWJP0029E");
    }
}
