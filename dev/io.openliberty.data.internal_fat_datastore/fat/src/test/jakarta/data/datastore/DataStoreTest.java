/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
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
    /**
     * Error messages, typically for invalid repository methods, that are
     * intentionally caused by tests to cover error paths.
     * These are ignored when checking the messages.log file for errors.
     */
    private static final String[] EXPECTED_ERROR_MESSAGES = //
                    new String[] {
                                   "CWWKD1063E.*PersistenceUnitRepo"
                    };

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

        JavaArchive DataStoreTestAppLib = ShrinkWrap.create(JavaArchive.class,
                                                            "DataStoreTestAppLib.jar")
                        .addPackage("test.jakarta.data.datastore.lib");

        JavaArchive DataStoreTestAppWebLib = ShrinkWrap.create(JavaArchive.class,
                                                               "DataStoreTestAppWebLib.jar")
                        .addPackage("test.jakarta.data.datastore.web.lib");

        WebArchive DataStoreTestWeb1 = ShrinkWrap.create(WebArchive.class,
                                                         "DataStoreTestWeb1.war")
                        .addPackage("test.jakarta.data.datastore.web")
                        .addAsWebInfResource(new File("test-applications/DataStoreTestApp/resources/DataStoreTestWeb1/WEB-INF/ibm-web-bnd.xml"))
                        .addAsResource(new File("test-applications/DataStoreTestApp/resources/DataStoreTestWeb1/WEB-INF/classes/META-INF/persistence.xml"),
                                       "META-INF/persistence.xml")
                        .addAsLibrary(DataStoreTestAppWebLib);

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
                        .addAsLibrary(DataStoreTestAppLib)
                        .addAsModule(DataStoreTestWeb1)
                        .addAsModule(DataStoreTestWeb2)
                        .addAsModule(DataStoreTestEJB);
        //ShrinkHelper.addDirectory(DataStoreTestApp, "test-applications/DataStoreTestApp/resources");
        ShrinkHelper.exportAppToServer(server, DataStoreTestApp);

        JavaArchive DataStoreEJBApp = ShrinkWrap.create(JavaArchive.class,
                                                        "DataStoreEJBApp.jar")
                        .addPackage("test.jakarta.data.datastore.ejbapp");
        ShrinkHelper.exportAppToServer(server, DataStoreEJBApp);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(EXPECTED_ERROR_MESSAGES);
    }

    @Test
    public void testDDLGeneration() throws Exception {

        // Run the ddlgen command and obtain the list of generated files
        List<String> ddlGeneratedFileNames = DDLGenScriptHelper.getGeneratedDDLFiles(server);
        List<String> ddlExpectedFileNames = DDLGenScriptHelper.getExpectedDDLFiles();

        // Verify that all the generated DDL files were expected
        assertEquals("Incorrect number of generated DDL files",
                     ddlExpectedFileNames.size(),
                     ddlGeneratedFileNames.size());
        for (int i = 0; i < ddlExpectedFileNames.size(); i++) {

            // Verify that all the generated DDL files had the correct name
            assertEquals("Incorrect name of generated DDL file", ddlExpectedFileNames.get(i), ddlGeneratedFileNames.get(i));

            List<String> sqlLinesGeneratedDDL = DDLGenScriptHelper.readSQLFromGeneratedDDLFile(server, ddlGeneratedFileNames.get(i));
            List<String> sqlLinesExpectedDDL = DDLGenScriptHelper.readSQLFromExpectedDDLFile(ddlExpectedFileNames.get(i));

            // Verify that all the generated DDL files had the correct number of lines
            assertEquals("Incorrect number of lines in generated DDL file " + ddlGeneratedFileNames.get(i),
                         sqlLinesExpectedDDL.size(), sqlLinesGeneratedDDL.size());

            // Verify that all the gerenerated DDL files had the correct SQL statements in the correct order
            for (int j = 0; j < sqlLinesExpectedDDL.size(); j++) {
                assertEquals("Incorrect SQL statement on line " + j + " of generated DDL file " + ddlGeneratedFileNames.get(i),
                             sqlLinesExpectedDDL.get(j), sqlLinesGeneratedDDL.get(j));
            }
        }
    }
}
