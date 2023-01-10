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

package com.ibm.ws.jpa.jarfile;

import java.util.HashSet;
import java.util.Set;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.jpa.FATSuite;
import com.ibm.ws.jpa.fvt.jarfile.tests.jarfilesupport.web_classes.JarFileWebClassesTestServlet;
import com.ibm.ws.jpa.fvt.jarfile.tests.jarfilesupport.web_lib.JarFileWebLibTestServlet;
import com.ibm.ws.testtooling.vehicle.web.JPAFATServletClient;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

/*
 *     These test cases exercise the following JPA <jar-file> configuration options:

    JPA Specification, Section 8.2.1.6.3 (Jar Files):

    Example 1: (ignored because the JPA 2 spec declares root-level utility jars as unsupported.)

    Example 2:

    app.ear
       lib/earEntities.jar
       lib/earLibPUnit.jar (with META-INF/persistence.xml)

    persistence.xml contains:
        <jar-file>earEntities.jar</jar-file>

    Example 6:

    app.ear
       lib/earEntities.jar
       war2.war
          WEB-INF/classes/META-INF/persistence.xml

    persistence.xml contains:
       <jar-file>../../lib/earEntities.jar</jar-file>

    Example 7:

    app.ear
       lib/earEntities.jar
       war1.war
          WEB-INF/lib/warPUnit.jar (with META-INF/persistence.xml)

    persistence.xml contains:
       <jar-file>../../../lib/earEntities.jar</jar-file>
 */
@RunWith(FATRunner.class)
public class JPAJarFileLibSupport_Web extends JPAFATServletClient {
    private final static String RESOURCE_ROOT = "test-applications/jarfile/";
    private final static String applicationName = "JPAJarFileSupport"; // Name of EAR
    private final static String appNameEar = applicationName + ".ear";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    static {
        dropSet.add("JPA10_INJECTION_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_INJECTION_CREATE_${dbvendor}.ddl");
    }

    private static long timestart = 0;

    @Server("JPAServerWeb")
    @TestServlets({
                    @TestServlet(servlet = JarFileWebClassesTestServlet.class, path = "JPAJarFileSupportWeb" + "/" + "JarFileWebClassesTestServlet"),
                    @TestServlet(servlet = JarFileWebLibTestServlet.class, path = "JPAJarFileSupportWebLib" + "/" + "JarFileWebLibTestServlet"),

    })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        int appStartTimeout = server1.getAppStartTimeout();
        if (appStartTimeout < (120 * 1000)) {
            server1.setAppStartTimeout(120 * 1000);
        }

        int configUpdateTimeout = server1.getConfigUpdateTimeout();
        if (configUpdateTimeout < (120 * 1000)) {
            server1.setConfigUpdateTimeout(120 * 1000);
        }

        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);
        bannerStart(JPAJarFileLibSupport_Web.class);
        timestart = System.currentTimeMillis();

        server1.addEnvVar("repeat_phase", FATSuite.repeatPhase);

        server1.startServer();

        setupDatabaseApplication(server1, RESOURCE_ROOT + "ddl/");

        final Set<String> ddlSet = new HashSet<String>();

        ddlSet.clear();
        for (String ddlName : dropSet) {
            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
        }
        executeDDL(server1, ddlSet, true);

        ddlSet.clear();
        for (String ddlName : createSet) {
            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
        }
        executeDDL(server1, ddlSet, false);

        setupTestApplication(server1);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server1.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                               "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            try {
                ServerConfiguration sc = server1.getServerConfiguration();
                sc.getApplications().clear();
                server1.updateServerConfiguration(sc);
                server1.saveServerConfiguration();

                server1.deleteFileFromLibertyServerRoot("apps/" + appNameEar);
                server1.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(JPAJarFileLibSupport_Web.class, timestart);
        }
    }

    private static void setupTestApplication(LibertyServer server1) throws Exception {
        // Library Jars

        final JavaArchive testApiJar = buildTestAPIJar();

        // V10Employee.jar
        final JavaArchive v10EmployeeJar = ShrinkWrap.create(JavaArchive.class, "V10Employee.jar");
        v10EmployeeJar.addPackage("com.ibm.ws.jpa.commonentities.jpa10.employee");

        // JPAPU.jar
        final JavaArchive jpaPuJar = ShrinkWrap.create(JavaArchive.class, "JPAPU.jar");
        jpaPuJar.addPackage("com.ibm.ws.jpa.commonentities.jpa10.simple");
        ShrinkHelper.addDirectory(jpaPuJar, RESOURCE_ROOT + "/JPAJarFileSupport.ear/lib/JPAPU.jar", new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {
            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }
        });

        // JPAJarFileSupportWebApp.war
        WebArchive JPAJarFileSupportWebApps_webApp = ShrinkWrap.create(WebArchive.class, "JPAJarFileSupportWebApp.war");
        JPAJarFileSupportWebApps_webApp.addPackage("com.ibm.ws.jpa.commonentities.jpa10.simple");
        JPAJarFileSupportWebApps_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.jarfile.testlogic");
        JPAJarFileSupportWebApps_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.jarfile.tests.jarfilesupport.web_classes");
        ShrinkHelper.addDirectory(JPAJarFileSupportWebApps_webApp, RESOURCE_ROOT + "/JPAJarFileSupport.ear/JPAJarFileSupportWebApp.war");

        // JPAJarFileSupportWebAppLib.war
        WebArchive JPAJarFileSupportWebApps_webApplib = ShrinkWrap.create(WebArchive.class, "JPAJarFileSupportWebAppLib.war");
        JPAJarFileSupportWebApps_webApplib.addPackages(true, "com.ibm.ws.jpa.fvt.jarfile.testlogic");
        JPAJarFileSupportWebApps_webApplib.addPackages(true, "com.ibm.ws.jpa.fvt.jarfile.tests.jarfilesupport.web_lib");

        final JavaArchive webLibJPAjar = ShrinkWrap.create(JavaArchive.class, "WebLibJPA.jar");
        webLibJPAjar.addPackage("com.ibm.ws.jpa.commonentities.jpa10.simple");
        ShrinkHelper.addDirectory(webLibJPAjar, RESOURCE_ROOT + "/JPAJarFileSupport.ear/JPAJarFileSupportWebAppLib.war/WEB-INF/lib/WebLibJPA.jar",
                                  new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {
                                      @Override
                                      public boolean include(ArchivePath arg0) {
                                          if (arg0.get().startsWith("/META-INF/")) {
                                              return true;
                                          }
                                          return false;
                                      }
                                  });

        JPAJarFileSupportWebApps_webApplib.addAsLibrary(webLibJPAjar);
        ShrinkHelper.addDirectory(JPAJarFileSupportWebApps_webApplib, RESOURCE_ROOT + "/JPAJarFileSupport.ear/JPAJarFileSupportWebAppLib.war",
                                  new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {
                                      @Override
                                      public boolean include(ArchivePath arg0) {
                                          if (arg0.get().startsWith("/WEB-INF/") && !arg0.get().startsWith("/WEB-INF/lib/")) {
                                              return true;
                                          }
                                          return false;
                                      }
                                  });

        // Assemble Application
        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, applicationName + ".ear");
        app.addAsModule(JPAJarFileSupportWebApps_webApp);
        app.addAsModule(JPAJarFileSupportWebApps_webApplib);
        app.addAsLibrary(v10EmployeeJar);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + "/JPAJarFileSupport.ear", new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }

        });

        ShrinkHelper.exportToServer(server1, "apps", app);

        Application appRecord = new Application();
        appRecord.setLocation(appNameEar);
        appRecord.setName(applicationName);

        if (FATSuite.repeatPhase != null && FATSuite.repeatPhase.contains("hibernate")) {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.getCommonLibraryRefs().add("HibernateLib");
            cel.add(loader);
        }

        server1.setMarkToEndOfLog();
        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add(applicationName);
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");

    }

}
