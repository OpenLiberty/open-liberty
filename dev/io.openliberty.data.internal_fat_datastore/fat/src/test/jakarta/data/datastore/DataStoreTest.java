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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        server.stopServer(EXPECTED_ERROR_MESSAGES);
    }

    @Test
    public void testDDLGeneration() throws Exception {
        // Build a map of expected DDL file names to expected list of SQL commands in that DDL
        // Note: not the full SQL statements, just the first part through the table names
        Map<String, List<String>> expectedSQLPerFile = new HashMap<String, List<String>>();
        expectedSQLPerFile.put("application[DataStoreTestApp].databaseStore[java.app.jdbc.DataSourceDef]_repository.ddl", //
                               Arrays.asList("CREATE TABLE DSDEntity", "CREATE TABLE DSDEntityEJB", "CREATE TABLE DSDEntityWar", "CREATE TABLE DSDEntityWar2"));
        expectedSQLPerFile.put("application[DataStoreTestApp].databaseStore[jdbc.ServerDataSource]_repository.ddl", //
                               Arrays.asList("CREATE TABLE ServerDSEntity"));
        expectedSQLPerFile.put("application[DataStoreTestApp].databaseStore[ServerDataSource]_repository.ddl", //
                               Arrays.asList("CREATE TABLE ServerDSEntity"));
        expectedSQLPerFile.put("application[DataStoreTestApp].module[DataStoreTestEJB.jar].databaseStore[java.module.env.jdbc.ServerDataSourceRef]_repository.ddl", //
                               Arrays.asList("CREATE TABLE ServerDSEntity"));
        expectedSQLPerFile.put("application[DataStoreTestApp].module[DataStoreTestEJB.jar].databaseStore[java.module.jdbc.DataSourceDef]_repository.ddl", //
                               Arrays.asList("CREATE TABLE EJBModuleDSDEntity"));
        expectedSQLPerFile.put("application[DataStoreTestApp].module[DataStoreTestWeb1.war].databaseStore[java.comp.DefaultDataSource]_repository.ddl", //
                               Arrays.asList("CREATE TABLE PersistenceUnitEntity", "CREATE TABLE DefDSEntity", "CREATE TABLE DefDSEntity2"));
        expectedSQLPerFile.put("application[DataStoreTestApp].module[DataStoreTestWeb1.war].databaseStore[java.module.env.jdbc.ServerDataSourceRef]_repository.ddl", //
                               Arrays.asList("CREATE TABLE ServerDSEntity"));
        expectedSQLPerFile.put("application[DataStoreTestApp].module[DataStoreTestWeb2.war].databaseStore[java.comp.DefaultDataSource]_repository.ddl", //
                               Arrays.asList("CREATE TABLE DefDSEntityWar2"));
        expectedSQLPerFile.put("application[DataStoreTestApp].module[DataStoreTestWeb2.war].databaseStore[java.module.env.jdbc.ServerDataSourceRef]_repository.ddl", //
                               Arrays.asList("CREATE TABLE ServerDSEntity"));
        expectedSQLPerFile.put("databaseStore[defaultDatabaseStore]_persistentExecutor.ddl", //
                               Arrays.asList("CREATE TABLE WLPPART", "CREATE TABLE WLPTASK", "CREATE TABLE WLPPROP", "ALTER TABLE WLPPART"));
        expectedSQLPerFile.put("databaseStore[java.global.env.jdbc.ServerDataSourceRef]_repository.ddl", //
                               Arrays.asList("CREATE TABLE GlobalLibEntity"));

        // Run the ddlgen command and obtain the list of generated files
        List<String> ddlFileNames = DDLGenScriptHelper.getDDLFiles(server);

        // Verify that all the expected DDL files were found
        for (String expectedFile : expectedSQLPerFile.keySet()) {
            assertTrue("Expected DDL file not found : " + expectedFile, ddlFileNames.remove(expectedFile));
        }

        // Verify that no extra DDL files were generated
        assertTrue("Unexpected DDL file(s) found " + ddlFileNames, ddlFileNames.isEmpty());

        // Verify that all of the expected tables are being created
        // Note: does not verify the entire SQL statement, just the table names
        expectedSQLPerFile.forEach((expectedFile, expectedSQL) -> {
            List<String> sqlFromDDL = DDLGenScriptHelper.readSQLFromDDLFile(server, expectedFile);
            for (String expected : expectedSQL) {
                boolean found = false;
                for (String sql : sqlFromDDL) {
                    if (sql.startsWith(expected)) {
                        found = true;
                        break;
                    }
                }
                assertTrue("Expected SQL not found in DDL File : " + expected + " : " + expectedFile, found);
            }
            assertTrue("Unexpected additional SQL found in file " + expectedFile + ", expected = " + expectedSQL + ", found = " + sqlFromDDL,
                       expectedSQL.size() == sqlFromDDL.size());
        });
    }
}
