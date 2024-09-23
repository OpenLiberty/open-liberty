/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.mail.fat;

import static org.junit.Assert.assertNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import fvtweb.web.JavamailFATServlet;

@RunWith(FATRunner.class)
public class MailSessionInjectionTest extends FATServletClient {

    static final String APP_NAME = "fvtweb";

    @Server("com.ibm.ws.javamail.fat")
    @TestServlet(servlet = JavamailFATServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive fvtweb = ShrinkHelper.buildDefaultApp(APP_NAME, "fvtweb.*");
        WebArchive fvtear = ShrinkWrap.create(WebArchive.class, "fvtapp.ear")
                        .addAsLibrary(fvtweb);
        ShrinkHelper.exportAppToServer(server, fvtear);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {

        String resourceWarning = server.waitForStringInLog("expected resource not found:");

        // If this assert fails its because the default mail cap files needed to set default encoding/decoding via
        // the activation framework are missing are not visible to the mail-2.x spec. Check the bnd file configuration
        // to ensure bundle has proper resources included
        assertNull("FAIL: One of the Jakarta Mail resources in /META-INF/ directory is not availible to the application",
                   resourceWarning);

        server.stopServer("J2CA0086W.*State:STATE_TRAN_WRAPPER_INUSE", // EXPECTED: One test intentionally leaves an open connection
                          "CWWKG0007W"); // let Nathan handle this : The system could not delete C:\Users\IBM_ADMIN\Documents\workspace\build.image/wlp/usr/servers\com.ibm.ws.jca.fat\workarea\org.eclipse.osgi\9\data\configs\com.ibm.ws.jca.jmsConnectionFactory.properties_83!-723947066
    }
}
