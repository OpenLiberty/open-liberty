/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jpa.jpa32;

import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jpa.FATSuite;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;
import io.openliberty.jpa.data.tests.web.JakartaDataRecreateServlet;

/**
 * Recreate issues found during development of Jakarta Data with EclipseLink
 * using pure Jakarta Persistence.
 *
 * These tests should be contributed back to EclipseLink as these issues are resovled.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class JakartaDataRecreateTest {
    public static final String APP_NAME = "jakartadata";
    public static final String SERVLET = "JakartaDataRecreate";
    public static final String SPECLEVEL = "3.2";

    @Server("JakartaDataRecreateServer")
    @TestServlet(servlet = JakartaDataRecreateServlet.class, path = APP_NAME + "_" + SPECLEVEL + "/" + SERVLET)
    public static LibertyServer server;

    public static final JdbcDatabaseContainer<?> testContainer = FATSuite.testContainer;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, PrivHelper.JAXB_PERMISSION);

        //Get driver name
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        createApplication(SPECLEVEL);
        server.startServer();
    }

    private static void createApplication(String specLevel) throws Exception {
        final String resPath = "test-applications/" + APP_NAME + "/resources/jpa-" + specLevel + "/web/";

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + "_" + specLevel + ".war");
        app.addPackage("io.openliberty.jpa.data.tests.models");
        app.addPackage("io.openliberty.jpa.data.tests.web");
        app.merge(ShrinkWrap.create(GenericArchive.class).as(ExplodedImporter.class).importDirectory(resPath).as(GenericArchive.class),
                  "/",
                  Filters.includeAll());
        ShrinkHelper.exportDropinAppToServer(server, app);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWJP9991W",
                          "Missing PostgreSQL10JsonPlatform"); // Generated with postgres db, since we don't include the postgres plugin);
    }

}
