/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.jpaContainer.fat;

import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.dsprops.Properties_derby_embedded;
import com.ibm.ws.testtooling.vehicle.web.JPAFATServletClient;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class JPADataSourceModifyConfigTest_Web extends JPAFATServletClient {

    private final static String RESOURCE_ROOT = "test-applications/jpaDataSourceApp/";
    private final static String appFolder = "web";
    private final static String appName = "jpadatasourceWeb";
    private final static String appNameEar = appName + ".ear";
    private final static String MSG_APP_START = "CWWKZ0001I.*" + appName;
    private final static String MSG_APP_RESTART = "CWWKZ0003I.*" + appName;

    private final static String SERVLET_NAME = "TestDataSourceServlet";

    @Server("DataSourceServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        int appStartTimeout = server.getAppStartTimeout();
        if (appStartTimeout < (120 * 1000)) {
            server.setAppStartTimeout(120 * 1000);
        }

        int configUpdateTimeout = server.getConfigUpdateTimeout();
        if (configUpdateTimeout < (120 * 1000)) {
            server.setConfigUpdateTimeout(120 * 1000);
        }

        setupTestApplication();
    }

    private static void setupTestApplication() throws Exception {
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.datasource.model");
        webApp.addPackages(true, "com.ibm.ws.jpa.datasource.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.datasource.web");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + appFolder + "/" + appName + ".war");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, appNameEar);
        app.addAsModule(webApp);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + appFolder, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {
            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }
        });

        ShrinkHelper.exportToServer(server, "apps", app);

        Application appRecord = new Application();
        appRecord.setLocation(appNameEar);
        appRecord.setName(appName);

        ServerConfiguration sc = server.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server.updateServerConfiguration(sc);
    }

    @Test
    public void jpa_datasource_testModifyDataSource_AMJTA_Web() throws Exception {
        final String resourceType = "AMJTA";
        final String dataSource1 = "JPA_AMJTA_DS1";
        final String dataSource2 = "JPA_AMJTA_DS2";
        testModifyDataSource(resourceType, dataSource1, dataSource2);
    }

    @Test
    public void jpa_datasource_testModifyDataSource_AMRL_Web() throws Exception {
        final String resourceType = "AMRL";
        final String dataSource1 = "JPA_AMRL_DS1";
        final String dataSource2 = "JPA_AMRL_DS2";
        testModifyDataSource(resourceType, dataSource1, dataSource2);
    }

    @Test
    public void jpa_datasource_testModifyDataSource_CMTS_Web() throws Exception {
        final String resourceType = "CMTS";
        final String dataSource1 = "JPA_CMTS_DS1";
        final String dataSource2 = "JPA_CMTS_DS2";
        testModifyDataSource(resourceType, dataSource1, dataSource2);
    }

    /**
     * Test that modifying the data source, while the server is running, prompts the application to restart.
     * Validate that after the application restarts, the JPA provider switches to using the new data source.
     */
    private void testModifyDataSource(String resourceType, String dataSource1, String dataSource2) throws Exception {
        Assert.assertFalse("Server is already started!", server.isStarted());

        try {
            // Start the server
            server.startServer();
            assertNotNull(appName + " was expected to start, but did not", server.waitForStringInLogUsingMark(MSG_APP_START));
            server.setMarkToEndOfLog();

            // Save the configuration to the default
            server.saveServerConfiguration();

            // Using the default data source, insert values into the database
            runTest(server, appName + '/' + SERVLET_NAME, "insert_" + resourceType);

            try {
                // Using the default data source, validate that the value previously inserted exists
                runTest(server, appName + '/' + SERVLET_NAME, "exists_" + resourceType);

                // Modify the data source to point to a new data source; save the configuration
                ServerConfiguration sc = server.getServerConfiguration();
                com.ibm.websphere.simplicity.config.DataSource d1 = sc.getDataSources().getById(dataSource1);
                com.ibm.websphere.simplicity.config.DataSource d2 = sc.getDataSources().getById(dataSource2);

                Properties_derby_embedded newProps = (Properties_derby_embedded) d2.getProperties_derby_embedded().get(0).clone();
                d1.replaceDatasourceProperties(newProps);
                server.setMarkToEndOfLog();
                server.updateServerConfiguration(sc);
                assertNotNull(appName + " was expected to restart, but did not", server.waitForStringInLogUsingMark(MSG_APP_RESTART));

                // Using the new data source, validate that the value previously inserted no longer exists
                runTest(server, appName + '/' + SERVLET_NAME, "notExists_" + resourceType);
            } finally {
                // Restore the configuration to the default
                server.setMarkToEndOfLog();
                server.restoreServerConfiguration();
                assertNotNull(appName + " was expected to restart, but did not", server.waitForStringInLogUsingMark(MSG_APP_RESTART));

                // Using the default data source, cleanup the database
                runTest(server, appName + '/' + SERVLET_NAME, "remove_" + resourceType);
            }
        } finally {
            server.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                              "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        }
    }
}
