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
package test.jakarta.data.experimental;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
import test.jakarta.data.experimental.web.DataExperimentalServlet;

/**
 * This test is a place for experimental function that did not make it into
 * the current version of the Jakarta Data specification, but which might be
 * added in a future version.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 21)
public class DataExperimentalTest extends FATServletClient {

    @Server("io.openliberty.data.internal.fat.exp")
    @TestServlet(servlet = DataExperimentalServlet.class, contextRoot = "DataExperimentalWeb")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive DataExperimentalWeb = ShrinkHelper.buildDefaultApp("DataExperimentalWeb", "test.jakarta.data.experimental.web");

        EnterpriseArchive DataExperimentalApp = ShrinkWrap.create(EnterpriseArchive.class, "DataExperimentalApp.ear");
        DataExperimentalApp.addAsModule(DataExperimentalWeb);
        ShrinkHelper.exportAppToServer(server, DataExperimentalApp);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
