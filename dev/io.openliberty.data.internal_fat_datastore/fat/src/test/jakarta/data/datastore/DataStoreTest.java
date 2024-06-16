/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
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

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.data.datastore.web.DataStoreTestServlet;
import test.jakarta.data.datastore.web2.DataStoreSecondServlet;
import test.jakarta.data.datastore.webapp.DataStoreWebAppServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class DataStoreTest extends FATServletClient {

    @Server("io.openliberty.data.internal.fat.datastore")
    @TestServlets({
                    @TestServlet(servlet = DataStoreTestServlet.class, contextRoot = "DataStoreTestWeb1"),
                    @TestServlet(servlet = DataStoreSecondServlet.class, contextRoot = "DataStoreTestWeb2"),
                    @TestServlet(servlet = DataStoreWebAppServlet.class, contextRoot = "DataStoreWebApp")
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive DataRepoGlobalLib = ShrinkWrap.create(JavaArchive.class,
                                                          "DataRepoGlobalLib.jar")
                        .addPackage("test.jakarta.data.datastore.global.lib");
        ShrinkHelper.exportToServer(server, "lib/global", DataRepoGlobalLib);

        WebArchive DataStoreWebApp = ShrinkWrap.create(WebArchive.class,
                                                       "DataStoreWebApp.war")
                        .addPackage("test.jakarta.data.datastore.webapp")
                        .addAsWebInfResource(new File("test-applications/DataStoreWebApp/resources/WEB-INF/ibm-web-bnd.xml"));
        ShrinkHelper.exportAppToServer(server, DataStoreWebApp);

        JavaArchive DataStoreTestLib = ShrinkWrap.create(JavaArchive.class,
                                                         "DataStoreTestLib.jar")
                        .addPackage("test.jakarta.data.datastore.lib");

        WebArchive DataStoreTestWeb1 = ShrinkWrap.create(WebArchive.class,
                                                         "DataStoreTestWeb1.war")
                        .addPackage("test.jakarta.data.datastore.web")
                        .addAsWebInfResource(new File("test-applications/DataStoreTestApp/resources/DataStoreTestWeb1/WEB-INF/ibm-web-bnd.xml"))
                        .addAsResource(new File("test-applications/DataStoreTestApp/resources/DataStoreTestWeb1/WEB-INF/classes/META-INF/persistence.xml"),
                                       "META-INF/persistence.xml");

        WebArchive DataStoreTestWeb2 = ShrinkWrap.create(WebArchive.class,
                                                         "DataStoreTestWeb2.war")
                        .addPackage("test.jakarta.data.datastore.web2")
                        .addAsWebInfResource(new File("test-applications/DataStoreTestApp/resources/DataStoreTestWeb2/WEB-INF/ibm-web-bnd.xml"));

        JavaArchive DataStoreTestEJB = ShrinkWrap.create(JavaArchive.class,
                                                         "DataStoreTestEJB.jar")
                        .addPackage("test.jakarta.data.datastore.ejb")
                        .addAsManifestResource(new File("test-applications/DataStoreTestApp/resources/DataStoreTestEJB/META-INF/ejb-jar.xml"))
                        .addAsManifestResource(new File("test-applications/DataStoreTestApp/resources/DataStoreTestEJB/META-INF/ibm-ejb-jar-bnd.xml"));

        EnterpriseArchive DataStoreTestApp = ShrinkWrap.create(EnterpriseArchive.class,
                                                               "DataStoreTestApp.ear")
                        .addAsLibrary(DataStoreTestLib)
                        .addAsModule(DataStoreTestWeb1)
                        .addAsModule(DataStoreTestWeb2)
                        .addAsModule(DataStoreTestEJB);
        //ShrinkHelper.addDirectory(DataStoreTestApp, "test-applications/DataStoreTestApp/resources");
        ShrinkHelper.exportAppToServer(server, DataStoreTestApp);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
