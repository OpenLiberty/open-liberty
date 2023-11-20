/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import JavaMailTestingApp.web.JavamailFATServlet;
import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class MailSessionInjectionTest extends FATServletClient {

    private static final String APP_NAME = "JavaMailTestingApp";

    @Server("mailSessionInjectionServer")
    @TestServlet(servlet = JavamailFATServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive webApp = ShrinkHelper.buildDefaultApp(APP_NAME, "JavaMailTestingApp.*");
        WebArchive earApp = ShrinkWrap.create(WebArchive.class, "JavaMailApp.ear")
                        .addAsLibrary(webApp);
        ShrinkHelper.exportAppToServer(server, earApp);

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true, null);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
