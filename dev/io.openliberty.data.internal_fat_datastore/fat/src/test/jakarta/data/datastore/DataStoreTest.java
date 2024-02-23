/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package test.jakarta.data.datastore;

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
import test.jakarta.data.datastore.web.DataStoreTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class DataStoreTest extends FATServletClient {

    @Server("io.openliberty.data.internal.fat.datastore")
    @TestServlet(servlet = DataStoreTestServlet.class, contextRoot = "DataStoreTestWeb")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive DataStoreTestWeb = ShrinkHelper.buildDefaultApp("DataStoreTestWeb", "test.jakarta.data.datastore.web");

        EnterpriseArchive DataStoreTestApp = ShrinkWrap.create(EnterpriseArchive.class, "DataStoreTestApp.ear");
        DataStoreTestApp.addAsModule(DataStoreTestWeb);
        //DataStoreTestApp.addAsModule(DataStoreTestEJB);
        //ShrinkHelper.addDirectory(DataStoreTestApp, "test-applications/DataStoreTestApp/resources");
        ShrinkHelper.exportAppToServer(server, DataStoreTestApp);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
