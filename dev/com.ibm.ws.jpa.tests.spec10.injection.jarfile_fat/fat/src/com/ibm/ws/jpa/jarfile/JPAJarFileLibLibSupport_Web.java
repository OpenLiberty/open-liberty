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
import com.ibm.ws.jpa.fvt.jarfile.tests.jarfilesupport.webclasseslib2.JarFileWebClassesLibTestServlet;
import com.ibm.ws.jpa.fvt.jarfile.tests.jarfilesupport.webliblib2.JarFileWebLibLib2TestServlet;
import com.ibm.ws.testtooling.vehicle.web.JPAFATServletClient;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

/**
 * These test cases exercise the following JPA <jar-file> configuration options:
 *
 * JPA Specification, Section 8.2.1.6.3 (Jar Files):
 *
 * Example 4:
 *
 * app.ear
 * -- war1.war
 * ---- WEB-INF/lib/warEntities.jar
 * ---- WEB-INF/lib/warPUnit.jar (with META-INF/persistence.xml)
 *
 * persistence.xml contains:
 * -- <jar-file>warEntities.jar</jar-file>
 *
 * Example 5:
 *
 * app.ear
 * -- war2.war
 * ---- WEB-INF/lib/warEntities.jar
 * ---- WEB-INF/classes/META-INF/persistence.xml
 *
 * persistence.xml contains:
 * -- <jar-file>lib/warEntities.jar</jar-file>
 *
 **/

@RunWith(FATRunner.class)
public class JPAJarFileLibLibSupport_Web extends JPAFATServletClient {
    private final static String RESOURCE_ROOT = "test-applications/jarfile/";
    private final static String applicationName = "JPAJarFileWebLibSupport"; // Name of EAR
    private final static String appNameEar = applicationName + ".ear";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    static {
        dropSet.add("JPA10_INJECTION_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_INJECTION_CREATE_${dbvendor}.ddl");
    }

    private static long timestart = 0;

    @Server("JPAServerWebLib")
    @TestServlets({
                    @TestServlet(servlet = JarFileWebClassesLibTestServlet.class, path = "JPAJarFileSupportWebClassesLib" + "/" + "JarFileWebClassesLibTestServlet"),
                    @TestServlet(servlet = JarFileWebLibLib2TestServlet.class, path = "JPAJarFileSupportWebLibLib" + "/" + "JarFileWebLibLib2TestServlet"),

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
        bannerStart(JPAJarFileLibLibSupport_Web.class);
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
            bannerEnd(JPAJarFileLibLibSupport_Web.class, timestart);
        }
    }

    private static void setupTestApplication(LibertyServer server1) throws Exception {
        // Library Jars

        final JavaArchive testApiJar = buildTestAPIJar();

        // V10Employee.jar
        final JavaArchive v10EmployeeJar = ShrinkWrap.create(JavaArchive.class, "V10Employee.jar");
        v10EmployeeJar.addPackage("com.ibm.ws.jpa.commonentities.jpa10.employee");

        // warPUnit.jar
        final JavaArchive warPuJar = ShrinkWrap.create(JavaArchive.class, "warPUnit.jar");
        warPuJar.addPackage("com.ibm.ws.jpa.commonentities.jpa10.simple");
        ShrinkHelper.addDirectory(warPuJar, RESOURCE_ROOT + "/JPAJarFileWebLibSupport.ear/JPAJarFileSupportWebLibLib.war/WEB-INF/lib/warPUnit.jar",
                                  new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {
                                      @Override
                                      public boolean include(ArchivePath arg0) {
                                          if (arg0.get().startsWith("/META-INF/")) {
                                              return true;
                                          }
                                          return false;
                                      }
                                  });

        // JPAJarFileSupportWebLibLib.war
        WebArchive JPAJarFileSupportWebApps_webApp = ShrinkWrap.create(WebArchive.class, "JPAJarFileSupportWebLibLib.war");
        JPAJarFileSupportWebApps_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.jarfile.testlogic");
        JPAJarFileSupportWebApps_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.jarfile.tests.jarfilesupport.webliblib2");
        JPAJarFileSupportWebApps_webApp.addAsLibrary(v10EmployeeJar);
        JPAJarFileSupportWebApps_webApp.addAsLibrary(warPuJar);
        ShrinkHelper.addDirectory(JPAJarFileSupportWebApps_webApp, RESOURCE_ROOT + "/JPAJarFileWebLibSupport.ear/JPAJarFileSupportWebLibLib.war",
                                  new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {
                                      @Override
                                      public boolean include(ArchivePath arg0) {
                                          if (arg0.get().startsWith("/WEB-INF/lib")) {
                                              return false;
                                          }
                                          return true;
                                      }
                                  });

        // JPAJarFileSupportWebClassesLib.war
        WebArchive JPAJarFileSupportWebClassesLib = ShrinkWrap.create(WebArchive.class, "JPAJarFileSupportWebClassesLib.war");
        JPAJarFileSupportWebClassesLib.addPackage("com.ibm.ws.jpa.commonentities.jpa10.simple");
        JPAJarFileSupportWebClassesLib.addPackages(true, "com.ibm.ws.jpa.fvt.jarfile.testlogic");
        JPAJarFileSupportWebClassesLib.addPackages(true, "com.ibm.ws.jpa.fvt.jarfile.tests.jarfilesupport.webclasseslib2");
        JPAJarFileSupportWebClassesLib.addAsLibrary(v10EmployeeJar);
        ShrinkHelper.addDirectory(JPAJarFileSupportWebClassesLib, RESOURCE_ROOT + "/JPAJarFileWebLibSupport.ear/JPAJarFileSupportWebClassesLib.war");

        // Assemble Application
        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, applicationName + ".ear");
        app.addAsModule(JPAJarFileSupportWebApps_webApp);
        app.addAsModule(JPAJarFileSupportWebClassesLib);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + "/JPAJarFileWebLibSupport.ear", new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

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
