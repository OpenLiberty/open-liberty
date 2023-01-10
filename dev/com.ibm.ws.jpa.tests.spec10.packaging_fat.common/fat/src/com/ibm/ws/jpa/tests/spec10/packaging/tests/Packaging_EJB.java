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
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.tests.ejb.TestAnnotationOverrideEJBSFEXServlet;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.tests.ejb.TestAnnotationOverrideEJBSFServlet;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.tests.ejb.TestAnnotationOverrideEJBSLServlet;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.tests.ejb.TestMappingFileDefaultsEJBSFEXServlet;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.tests.ejb.TestMappingFileDefaultsEJBSFServlet;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.tests.ejb.TestMappingFileDefaultsEJBSLServlet;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.tests.ejb.TestMetadataCompleteEJBSFEXServlet;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.tests.ejb.TestMetadataCompleteEJBSFServlet;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.tests.ejb.TestMetadataCompleteEJBSLServlet;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.tests.ejb.TestXMLMetadataCompleteEJBSFEXServlet;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.tests.ejb.TestXMLMetadataCompleteEJBSFServlet;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.tests.ejb.TestXMLMetadataCompleteEJBSLServlet;
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
@Mode(TestMode.FULL)
public class Packaging_EJB extends JPAFATServletClient {
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
    private final static String appFolder = "Packaging10EJB";
    private final static String appName = "Packaging10EJB";
    private final static String appNameEar = appName + ".ear";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA10_PACKAGING_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_PACKAGING_CREATE_${dbvendor}.ddl");
    }

    @Server("JPA10EJBServer")
    @TestServlets({
                    @TestServlet(servlet = TestAnnotationOverrideEJBSLServlet.class, path = "Packaging10EJB" + "/" + "TestAnnotationOverrideEJBSLServlet"),
                    @TestServlet(servlet = TestAnnotationOverrideEJBSFServlet.class, path = "Packaging10EJB" + "/" + "TestAnnotationOverrideEJBSFServlet"),
                    @TestServlet(servlet = TestAnnotationOverrideEJBSFEXServlet.class, path = "Packaging10EJB" + "/" + "TestAnnotationOverrideEJBSFEXServlet"),

                    @TestServlet(servlet = TestMappingFileDefaultsEJBSLServlet.class, path = "Packaging10EJB" + "/" + "TestMappingFileDefaultsEJBSLServlet"),
                    @TestServlet(servlet = TestMappingFileDefaultsEJBSFServlet.class, path = "Packaging10EJB" + "/" + "TestMappingFileDefaultsEJBSFServlet"),
                    @TestServlet(servlet = TestMappingFileDefaultsEJBSFEXServlet.class, path = "Packaging10EJB" + "/" + "TestMappingFileDefaultsEJBSFEXServlet"),

                    @TestServlet(servlet = TestMetadataCompleteEJBSLServlet.class, path = "Packaging10EJB" + "/" + "TestMetadataCompleteEJBSLServlet"),
                    @TestServlet(servlet = TestMetadataCompleteEJBSFServlet.class, path = "Packaging10EJB" + "/" + "TestMetadataCompleteEJBSFServlet"),
                    @TestServlet(servlet = TestMetadataCompleteEJBSFEXServlet.class, path = "Packaging10EJB" + "/" + "TestMetadataCompleteEJBSFEXServlet"),

                    @TestServlet(servlet = TestXMLMetadataCompleteEJBSLServlet.class, path = "Packaging10EJB" + "/" + "TestXMLMetadataCompleteEJBSLServlet"),
                    @TestServlet(servlet = TestXMLMetadataCompleteEJBSFServlet.class, path = "Packaging10EJB" + "/" + "TestXMLMetadataCompleteEJBSFServlet"),
                    @TestServlet(servlet = TestXMLMetadataCompleteEJBSFEXServlet.class, path = "Packaging10EJB" + "/" + "TestXMLMetadataCompleteEJBSFEXServlet"),
    })
    public static LibertyServer server;

    public static final JdbcDatabaseContainer<?> testContainer = AbstractFATSuite.testContainer;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, AbstractFATSuite.JAXB_PERMS);
        bannerStart(Packaging_EJB.class);
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

        System.out.println(Packaging_EJB.class.getName() + " Setting up database tables...");

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
        final String earFileName = "Packaging10EJB.ear";

        // EJBTestExecutor.war
        WebArchive ejbExecutorWar = ShrinkWrap.create(WebArchive.class, "EJBTestExecutor.war");
        ejbExecutorWar.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.tests.ejb");
        ejbExecutorWar.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.tests.ejb");
        ejbExecutorWar.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.tests.ejb");
        ejbExecutorWar.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.tests.ejb");
        ShrinkHelper.addDirectory(ejbExecutorWar, RESOURCE_ROOT + "/" + earFileName + "/EJBTestExecutor.war");

        // JPAAnoOverrideEJB.jar
        JavaArchive annoOverrideEjb = ShrinkWrap.create(JavaArchive.class, "JPAAnoOverrideEJB.jar");
        annoOverrideEjb.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.entities");
        annoOverrideEjb.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.testlogic");
        annoOverrideEjb.addClass("com.ibm.ws.jpa.fvt.packaging.ejblocal.AnoOverrideSFEJBLocal");
        annoOverrideEjb.addClass("com.ibm.ws.jpa.fvt.packaging.ejblocal.AnoOverrideSFEXEJBLocal");
        annoOverrideEjb.addClass("com.ibm.ws.jpa.fvt.packaging.ejblocal.AnoOverrideSLEJBLocal");
        ShrinkHelper.addDirectory(annoOverrideEjb, RESOURCE_ROOT + "/" + earFileName + "/JPAAnoOverrideEJB.jar");

        // JPAMappingFileDefaultsEJB.jar
        JavaArchive mappingFileDefaultsEjb = ShrinkWrap.create(JavaArchive.class, "JPAMappingFileDefaultsEJB.jar");
        mappingFileDefaultsEjb.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities");
        mappingFileDefaultsEjb.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.testlogic");
        mappingFileDefaultsEjb.addClass("com.ibm.ws.jpa.fvt.packaging.ejblocal.MappingFileDefaultsSFEJBLocal");
        mappingFileDefaultsEjb.addClass("com.ibm.ws.jpa.fvt.packaging.ejblocal.MappingFileDefaultsSFEXEJBLocal");
        mappingFileDefaultsEjb.addClass("com.ibm.ws.jpa.fvt.packaging.ejblocal.MappingFileDefaultsSLEJBLocal");
        ShrinkHelper.addDirectory(mappingFileDefaultsEjb, RESOURCE_ROOT + "/" + earFileName + "/JPAMappingFileDefaultsEJB.jar");

        // JPAMetadataCompleteEJB.jar
        JavaArchive metadataCompleteEjb = ShrinkWrap.create(JavaArchive.class, "JPAMetadataCompleteEJB.jar");
        metadataCompleteEjb.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.entities");
        metadataCompleteEjb.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.testlogic");
        metadataCompleteEjb.addClass("com.ibm.ws.jpa.fvt.packaging.ejblocal.MetadataCompleteSFEJBLocal");
        metadataCompleteEjb.addClass("com.ibm.ws.jpa.fvt.packaging.ejblocal.MetadataCompleteSFEXEJBLocal");
        metadataCompleteEjb.addClass("com.ibm.ws.jpa.fvt.packaging.ejblocal.MetadataCompleteSLEJBLocal");
        ShrinkHelper.addDirectory(metadataCompleteEjb, RESOURCE_ROOT + "/" + earFileName + "/JPAMetadataCompleteEJB.jar");

        // JPAXMLMetadataCompleteEJB.jar
        JavaArchive xmlMetadataCompleteEjb = ShrinkWrap.create(JavaArchive.class, "JPAXMLMetadataCompleteEJB.jar");
        xmlMetadataCompleteEjb.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.entities");
        xmlMetadataCompleteEjb.addPackages(true, "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.testlogic");
        metadataCompleteEjb.addClass("com.ibm.ws.jpa.fvt.packaging.ejblocal.XMLMetadataCompleteSFEJBLocal");
        metadataCompleteEjb.addClass("com.ibm.ws.jpa.fvt.packaging.ejblocal.XMLMetadataCompleteSFEXEJBLocal");
        metadataCompleteEjb.addClass("com.ibm.ws.jpa.fvt.packaging.ejblocal.XMLMetadataCompleteSLEJBLocal");
        ShrinkHelper.addDirectory(xmlMetadataCompleteEjb, RESOURCE_ROOT + "/" + earFileName + "/JPAXMLMetadataCompleteEJB.jar");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, appNameEar);
        app.addAsModule(annoOverrideEjb);
        app.addAsModule(mappingFileDefaultsEjb);
        app.addAsModule(metadataCompleteEjb);
        app.addAsModule(xmlMetadataCompleteEjb);
        app.addAsModule(ejbExecutorWar);
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
            bannerEnd(Packaging_EJB.class, timestart);
        }
    }
}
