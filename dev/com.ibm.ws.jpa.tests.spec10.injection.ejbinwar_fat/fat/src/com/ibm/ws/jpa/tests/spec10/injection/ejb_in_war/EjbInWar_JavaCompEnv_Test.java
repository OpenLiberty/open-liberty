/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.tests.spec10.injection.ejb_in_war;

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
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.jpa.fvt.ejbinwar_javacomp.web.rrinejb1.JPAEjbInWarJavaCompRRInEJB1TestServlet;
import com.ibm.ws.jpa.fvt.ejbinwar_javacomp.web.rrinejb2.JPAEjbInWarJavaCompRRInEJB2TestServlet;
import com.ibm.ws.jpa.fvt.ejbinwar_javacomp.web.rrinwar.JPAEjbInWarJavaCompTestServlet;
import com.ibm.ws.testtooling.vehicle.web.JPAFATServletClient;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

@RunWith(FATRunner.class)
public class EjbInWar_JavaCompEnv_Test extends JPAFATServletClient {
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

    @Server("JPAJavaCompEnvServer")
    @TestServlets({
                    @TestServlet(servlet = JPAEjbInWarJavaCompTestServlet.class, path = "resRefInWebDD" + "/" + "JPAEjbInWarJavaCompTestServlet"),
                    @TestServlet(servlet = JPAEjbInWarJavaCompRRInEJB1TestServlet.class, path = "resRefInEJB1" + "/" + "JPAEjbInWarJavaCompRRInEJB1TestServlet"),
                    @TestServlet(servlet = JPAEjbInWarJavaCompRRInEJB2TestServlet.class, path = "resRefInEJB2" + "/" + "JPAEjbInWarJavaCompRRInEJB2TestServlet"),

    })
    public static LibertyServer server1;

    public static final JdbcDatabaseContainer<?> testContainer = FATSuite.testContainer;

    @BeforeClass
    public static void setUp() throws Exception {
        setUp(server1, EjbInWar_JavaCompEnv_Test.class);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        tearDown(server1, EjbInWar_JavaCompEnv_Test.class);
    }

    private final static String RESOURCE_ROOT = "test-applications/injection_ejbinwar_javacomp/";
    private final static String applicationName = "EJBInWar_JavaComp"; // Name of EAR

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA10_INJECTION_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_INJECTION_CREATE_${dbvendor}.ddl");
    }

    public static void setUp(LibertyServer server1, Class tCls) throws Exception {
        int appStartTimeout = server1.getAppStartTimeout();
        if (appStartTimeout < (120 * 1000)) {
            server1.setAppStartTimeout(120 * 1000);
        }

        int configUpdateTimeout = server1.getConfigUpdateTimeout();
        if (configUpdateTimeout < (120 * 1000)) {
            server1.setConfigUpdateTimeout(120 * 1000);
        }

        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);
        bannerStart(tCls);
        timestart = System.currentTimeMillis();

        server1.addEnvVar("repeat_phase", FATSuite.repeatPhase);

        //Get driver name
        server1.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server1, testContainer);

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

    private static void setupTestApplication(LibertyServer server1) throws Exception {
        // Library Jars
        final JavaArchive testApiJar = buildTestAPIJar();

        // Web Test WAR Module

        // java_comp_test.war
        WebArchive resRefInWebDD_war = ShrinkWrap.create(WebArchive.class, "resRefInWebDD.war");
        resRefInWebDD_war.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar_javacomp.ejb");
        resRefInWebDD_war.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar_javacomp.testlogic");
        resRefInWebDD_war.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar_javacomp.web.rrinwar");

        resRefInWebDD_war.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.core");
        resRefInWebDD_war.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.ejb");
        resRefInWebDD_war.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        resRefInWebDD_war.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earlib");
        resRefInWebDD_war.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earroot");
        ShrinkHelper.addDirectory(resRefInWebDD_war, RESOURCE_ROOT + "/resRefInWebDD.war");

        // resRefInEJB1.war
        WebArchive resRefInEJB1DD_war = ShrinkWrap.create(WebArchive.class, "resRefInEJB1.war");
        resRefInEJB1DD_war.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar_javacomp.ejb");
        resRefInEJB1DD_war.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar_javacomp.testlogic");
        resRefInEJB1DD_war.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar_javacomp.web.rrinejb1");

        resRefInEJB1DD_war.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.core");
        resRefInEJB1DD_war.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.ejb");
        resRefInEJB1DD_war.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        resRefInEJB1DD_war.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earlib");
        resRefInEJB1DD_war.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earroot");
        ShrinkHelper.addDirectory(resRefInEJB1DD_war, RESOURCE_ROOT + "/resRefInEJB1.war");

        // resRefInEJB2.war
        WebArchive resRefInEJB2DD_war = ShrinkWrap.create(WebArchive.class, "resRefInEJB2.war");
        resRefInEJB2DD_war.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar_javacomp.ejb");
        resRefInEJB2DD_war.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar_javacomp.testlogic");
        resRefInEJB2DD_war.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar_javacomp.web.rrinejb2");

        resRefInEJB2DD_war.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.core");
        resRefInEJB2DD_war.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.ejb");
        resRefInEJB2DD_war.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        resRefInEJB2DD_war.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earlib");
        resRefInEJB2DD_war.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earroot");
        ShrinkHelper.addDirectory(resRefInEJB2DD_war, RESOURCE_ROOT + "/resRefInEJB2.war");

        // Assemble Application
        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, applicationName + ".ear");
        app.addAsModule(resRefInWebDD_war);
        app.addAsModule(resRefInEJB1DD_war);
        app.addAsModule(resRefInEJB2DD_war);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

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
        appRecord.setLocation(applicationName + ".ear");
        appRecord.setName(applicationName);

        if (FATSuite.repeatPhase != null && FATSuite.repeatPhase.contains("hibernate")) {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.getCommonLibraryRefs().add("HibernateLib");
            cel.add(loader);
        } else if (FATSuite.repeatPhase != null && FATSuite.repeatPhase.contains("openjpa")) {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.getCommonLibraryRefs().add("OpenJPALib");
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

    public static void tearDown(LibertyServer server1, Class tCls) throws Exception {
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

                server1.deleteFileFromLibertyServerRoot("apps/" + applicationName + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(tCls, timestart);
        }
    }
}
