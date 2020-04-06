/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class DataSourceJaasTest extends FATServletClient {

    //App names
    private static final String basicfat = "basicfat";
    private static final String dsdfat = "dsdfat";

    @ClassRule
    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    //Server used for DataSourceJaasTest.java
    @Server("com.ibm.ws.jdbc.jaas.fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Delete the Derby database that might be left over from last run
        Machine machine = server.getMachine();
        String installRoot = server.getInstallRoot();
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/jdbcfat");
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/derbyfat");

        //Get driver type
        DatabaseContainerType type = DatabaseContainerType.valueOf(testContainer);
        server.addEnvVar("DB_DRIVER", type.getDriverName());
        server.addEnvVar("ANON_DRIVER", type.getAnonymousDriverName());
        server.addEnvVar("DB_USER", testContainer.getUsername());
        server.addEnvVar("DB_PASSWORD", testContainer.getPassword());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        //**** jdbcJassServer apps ****
        ShrinkHelper.defaultApp(server, dsdfat, dsdfat);

        // Default app - jdbcapp.ear [basicfat.war, application.xml]
        WebArchive basicfatWAR = ShrinkHelper.buildDefaultApp(basicfat, basicfat);
        EnterpriseArchive jdbcappEAR = ShrinkWrap.create(EnterpriseArchive.class, "jdbcapp.ear");
        jdbcappEAR.addAsModule(basicfatWAR);
        ShrinkHelper.addDirectory(jdbcappEAR, "test-applications/jdbcapp/resources");
        ShrinkHelper.exportAppToServer(server, jdbcappEAR);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("DSRA9543W", //Expected since we're using a GSS Credential for authentication with Derby.
                          "CWWKE0701E"); //TODO investigate why this warning is being logged
    }

    @Test
    public void testDataSourceMappingConfigAlias() throws Exception {
        runTest(server, basicfat, testName);
    }

    @Test
    public void testDataSourceCustomLoginConfiguration() throws Exception {
        runTest(server, basicfat, testName);
    }

    @Test
    public void testJAASLoginWithGSSCredential() throws Exception {
        runTest(server, basicfat, testName);
    }
}
