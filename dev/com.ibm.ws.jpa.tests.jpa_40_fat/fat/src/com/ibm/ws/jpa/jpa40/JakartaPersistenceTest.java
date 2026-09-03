/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/

package com.ibm.ws.jpa.jpa40;

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
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.jpa.FATSuite;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;
import io.openliberty.jpa.persistence.tests.web.JakartaPersistenceServlet;

/**
 * Smoke-tests Jakarta Persistence 4.0 against:
 *   - EclipseLink (default, LITE) via {@code persistence-4.0}
 *   - Hibernate 8 (FULL) via {@code persistenceContainer-4.0} + user-supplied library
 *
 * The repeat phase is injected into the server at runtime via
 * {@code <include location="${shared.config.dir}/${env.repeat_phase}"/>}
 * so the base server.xml stays feature-neutral.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 21)
@MaximumJavaLevel(javaLevel = 25)  // Hibernate ByteBuddy limitation
public class JakartaPersistenceTest {

    public static final String APP_NAME  = "jakartapersistence";
    public static final String SERVLET   = "JakartaPersistence40";
    public static final String SPECLEVEL = "4.0";

    @Server("JakartaPersistenceServer")
    @TestServlet(servlet = JakartaPersistenceServlet.class,
                 path = APP_NAME + "_" + SPECLEVEL + "/" + SERVLET)
    public static LibertyServer server;

    public static final JdbcDatabaseContainer<?> testContainer = FATSuite.testContainer;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, PrivHelper.JAXB_PERMISSION);

        server.addEnvVar("repeat_phase", AbstractFATSuite.repeatPhase);
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        int appStartTimeout = server.getAppStartTimeout();
        if (appStartTimeout < (180 * 1000)) {
            server.setAppStartTimeout(180 * 1000);
        }

        createApplication(SPECLEVEL);
        server.startServer();
    }

    private static void createApplication(String specLevel) throws Exception {
        final String resPath = "test-applications/" + APP_NAME + "/resources/jpa-" + specLevel + "/web/";

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + "_" + specLevel + ".war");
        app.addPackage("io.openliberty.jpa.persistence.tests.models");
        app.addPackage("io.openliberty.jpa.persistence.tests.web");
        app.merge(ShrinkWrap.create(GenericArchive.class)
                            .as(ExplodedImporter.class)
                            .importDirectory(resPath)
                            .as(GenericArchive.class),
                  "/",
                  Filters.includeAll());
        ShrinkHelper.exportAppToServer(server, app);

        Application appRecord = new Application();
        appRecord.setLocation(APP_NAME + "_" + specLevel + ".war");
        appRecord.setName(APP_NAME + "_" + specLevel);

        // When running the Hibernate repeat phase, wire HibernateLib as a
        // commonLibraryRef so Hibernate 8 JARs are visible to the app classloader.
        if (AbstractFATSuite.repeatPhase != null
                && AbstractFATSuite.repeatPhase.contains("hibernate")) {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.getCommonLibraryRefs().add("HibernateLib");
            cel.add(loader);
        }

        ServerConfiguration sc = server.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server.updateServerConfiguration(sc);
        server.saveServerConfiguration();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(
            "CWWJP9991W",                                                // EclipseLink drop-and-create
            "WTRN0074E: Exception caught from before_completion synchronization operation", // expected tx test
            "Missing PostgreSQL10JsonPlatform");                          // postgres without plugin
    }
}
