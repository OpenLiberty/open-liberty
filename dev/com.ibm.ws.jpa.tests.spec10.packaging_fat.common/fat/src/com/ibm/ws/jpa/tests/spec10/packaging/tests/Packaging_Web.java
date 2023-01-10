/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.jpa.tests.spec10.packaging.tests;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.tests.web.TestAnnotationOverrideServlet;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.tests.web.TestMappingFileDefaultsServlet;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.tests.web.TestMetadataCompleteServlet;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.tests.web.TestXMLMetadataCompleteServlet;
import com.ibm.ws.testtooling.vehicle.web.JPAFATServletClient;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class Packaging_Web extends JPAFATServletClient {
    @Rule
    public org.junit.rules.TestRule skipDBRule = new org.junit.rules.Verifier() {

        @Override
        public Statement apply(Statement arg0, Description arg1) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    String database = getDbVendor().name();
                    boolean shouldSkip = (database != null
                                          && !Pattern.compile("derby", Pattern.CASE_INSENSITIVE).matcher(database).find());
                    System.out.println("Checking if skip test");
                    if (shouldSkip) {
                        throw new AssumptionViolatedException("Database is not Derby. Skipping test!");
                    } else {
                        System.out.println("Not Skipping");
                        arg0.evaluate();
                    }
                }
            };
        }
    };

    private final static String RESOURCE_ROOT = "test-applications/packaging/";
    private final static String appFolder = "Packaging10Web";
    private final static String appName = "Packaging10Web";
    private final static String appNameEar = appName + ".ear";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA10_PACKAGING_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_PACKAGING_CREATE_${dbvendor}.ddl");
    }

    @Server("JPA10WebServer")
    @TestServlets({
                    @TestServlet(servlet = TestAnnotationOverrideServlet.class, path = "AnoOverride" + "/" + "TestAnnotationOverrideServlet"),
                    @TestServlet(servlet = TestMappingFileDefaultsServlet.class, path = "MappingFileDefaults" + "/" + "TestMappingFileDefaultsServlet"),
                    @TestServlet(servlet = TestMetadataCompleteServlet.class, path = "MetadataComplete" + "/" + "TestMetadataCompleteServlet"),
                    @TestServlet(servlet = TestXMLMetadataCompleteServlet.class, path = "XMLMetadataComplete" + "/" + "TestXMLMetadataCompleteServlet"),

    //
    })
    public static LibertyServer server;

    public static final JdbcDatabaseContainer<?> testContainer = AbstractFATSuite.testContainer;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, AbstractFATSuite.JAXB_PERMS);
        bannerStart(Packaging_Web.class);
        timestart = System.currentTimeMillis();

        int appStartTimeout = server.getAppStartTimeout();
        if (appStartTimeout < (120 * 1000)) {
            server.setAppStartTimeout(120 * 1000);
        }

        int configUpdateTimeout = server.getConfigUpdateTimeout();
        if (configUpdateTimeout < (120 * 1000)) {
            server.setConfigUpdateTimeout(120 * 1000);
        }

        //Get driver name
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        server.startServer();

        setupDatabaseApplication(server, RESOURCE_ROOT + "ddl/");

        final Set<String> ddlSet = new HashSet<String>();

        System.out.println(Packaging_Web.class.getName() + " Setting up database tables...");

        ddlSet.clear();
        for (String ddlName : dropSet) {
            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
        }
        executeDDL(server, ddlSet, true);

        ddlSet.clear();
        for (String ddlName : createSet) {
            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
        }
        executeDDL(server, ddlSet, false);

        setupTestApplication();

    }

    private static void setupTestApplication() throws Exception {
        final String earFileName = "Packaging10Web.ear";

        // AnoOverride.war
        WebArchive annoOverrideWar = ShrinkWrap.create(WebArchive.class, "AnoOverride.war");
        annoOverrideWar.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.entities");
        annoOverrideWar.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.testlogic");
        annoOverrideWar.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.tests.web");
        ShrinkHelper.addDirectory(annoOverrideWar, RESOURCE_ROOT + "/" + earFileName + "/AnoOverride.war");

        // MappingFileDefaults.war
        WebArchive mappingFileDefaultsWar = ShrinkWrap.create(WebArchive.class, "MappingFileDefaults.war");
        mappingFileDefaultsWar.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities");
        mappingFileDefaultsWar.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.testlogic");
        mappingFileDefaultsWar.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.tests.web");
        ShrinkHelper.addDirectory(mappingFileDefaultsWar, RESOURCE_ROOT + "/" + earFileName + "/MappingFileDefaults.war");

        // MetadataComplete.war
        WebArchive metadataCompleteWar = ShrinkWrap.create(WebArchive.class, "MetadataComplete.war");
        metadataCompleteWar.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.entities");
        metadataCompleteWar.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.testlogic");
        metadataCompleteWar.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.tests.web");
        ShrinkHelper.addDirectory(metadataCompleteWar, RESOURCE_ROOT + "/" + earFileName + "/MetadataComplete.war");

        // XMLMetadataComplete.war
        WebArchive xmlMetadataCompleteWar = ShrinkWrap.create(WebArchive.class, "XMLMetadataComplete.war");
        xmlMetadataCompleteWar.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.entities");
        xmlMetadataCompleteWar.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.testlogic");
        xmlMetadataCompleteWar.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.tests.web");
        ShrinkHelper.addDirectory(xmlMetadataCompleteWar, RESOURCE_ROOT + "/" + earFileName + "/XMLMetadataComplete.war");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, appNameEar);
        app.addAsModule(annoOverrideWar);
        app.addAsModule(mappingFileDefaultsWar);
        app.addAsModule(metadataCompleteWar);
        app.addAsModule(xmlMetadataCompleteWar);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + "/" + earFileName, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

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

        server.setMarkToEndOfLog();
        ServerConfiguration sc = server.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server.updateServerConfiguration(sc);
        server.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            // Clean up database
            try {
                final Set<String> ddlSet = new HashSet<String>();
                for (String ddlName : dropSet) {
                    ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
                }
                executeDDL(server, ddlSet, true);
            } catch (Throwable t) {
                t.printStackTrace();
            }

            server.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                              "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            try {
                ServerConfiguration sc = server.getServerConfiguration();
                sc.getApplications().clear();
                server.updateServerConfiguration(sc);
                server.saveServerConfiguration();

                server.deleteFileFromLibertyServerRoot("apps/" + appNameEar);
                server.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(Packaging_Web.class, timestart);
        }
    }
}
