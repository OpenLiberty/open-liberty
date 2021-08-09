/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.tests.spec10.injection.mdb;

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
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dfi.anoovrd.DFIPkgYesInhAnoOvrdMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dfi.anoovrd.DFIPriYesInhAnoOvrdMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dfi.anoovrd.DFIProYesInhAnoOvrdMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dfi.anoovrd.DFIPubYesInhAnoOvrdMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dfi.ddovrd.DFIPkgYesInhDDOvrdMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dfi.ddovrd.DFIPriYesInhDDOvrdMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dfi.ddovrd.DFIProYesInhDDOvrdMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dfi.ddovrd.DFIPubYesInhDDOvrdMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dfi.noovrd.DFIPkgNoInhMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dfi.noovrd.DFIPriNoInhMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dfi.noovrd.DFIProNoInhMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dfi.noovrd.DFIPubNoInhMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dmi.anoovrd.DMIPkgYesInhAnoOvrdMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dmi.anoovrd.DMIPriYesInhAnoOvrdMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dmi.anoovrd.DMIProYesInhAnoOvrdMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dmi.anoovrd.DMIPubYesInhAnoOvrdMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dmi.ddovrd.DMIPkgYesInhDDOvrdMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dmi.ddovrd.DMIPriYesInhDDOvrdMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dmi.ddovrd.DMIProYesInhDDOvrdMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dmi.ddovrd.DMIPubYesInhDDOvrdMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dmi.noovrd.DMIPkgNoInhMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dmi.noovrd.DMIPriNoInhMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dmi.noovrd.DMIProNoInhMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.dmi.noovrd.DMIPubNoInhMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.jndi.AnnotatedJNDIMDBServlet;
import com.ibm.ws.jpa.fvt.injection.tests.mdb.jndi.DDJNDIMDBServlet;

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
public class JPA10Injection_MDB extends JPAFATServletClient {

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

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA10_INJECTION_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_INJECTION_CREATE_${dbvendor}.ddl");
    }

    @Server("JPAServerJMS")
    @TestServlets({
                    // JNDI Injection
                    @TestServlet(servlet = AnnotatedJNDIMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "AnnotatedJNDIMDBServlet"),
                    @TestServlet(servlet = DDJNDIMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DDJNDIMDBServlet"),

                    // Field Injection
                    @TestServlet(servlet = DFIPkgNoInhMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DFIPkgNoInhMDBServlet"),
                    @TestServlet(servlet = DFIPriNoInhMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DFIPriNoInhMDBServlet"),
                    @TestServlet(servlet = DFIProNoInhMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DFIProNoInhMDBServlet"),
                    @TestServlet(servlet = DFIPubNoInhMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DFIPubNoInhMDBServlet"),

                    @TestServlet(servlet = DFIPkgYesInhAnoOvrdMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DFIPkgYesInhAnoOvrdMDBServlet"),
                    @TestServlet(servlet = DFIPriYesInhAnoOvrdMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DFIPriYesInhAnoOvrdMDBServlet"),
                    @TestServlet(servlet = DFIProYesInhAnoOvrdMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DFIProYesInhAnoOvrdMDBServlet"),
                    @TestServlet(servlet = DFIPubYesInhAnoOvrdMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DFIPubYesInhAnoOvrdMDBServlet"),

                    @TestServlet(servlet = DFIPkgYesInhDDOvrdMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DFIPkgYesInhDDOvrdMDBServlet"),
                    @TestServlet(servlet = DFIPriYesInhDDOvrdMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DFIPriYesInhDDOvrdMDBServlet"),
                    @TestServlet(servlet = DFIProYesInhDDOvrdMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DFIProYesInhDDOvrdMDBServlet"),
                    @TestServlet(servlet = DFIPubYesInhDDOvrdMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DFIPubYesInhDDOvrdMDBServlet"),

                    // Method Injection
                    @TestServlet(servlet = DMIPkgNoInhMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DMIPkgNoInhMDBServlet"),
                    @TestServlet(servlet = DMIPriNoInhMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DMIPriNoInhMDBServlet"),
                    @TestServlet(servlet = DMIProNoInhMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DMIProNoInhMDBServlet"),
                    @TestServlet(servlet = DMIPubNoInhMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DMIPubNoInhMDBServlet"),

                    @TestServlet(servlet = DMIPkgYesInhAnoOvrdMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DMIPkgYesInhAnoOvrdMDBServlet"),
                    @TestServlet(servlet = DMIPriYesInhAnoOvrdMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DMIPriYesInhAnoOvrdMDBServlet"),
                    @TestServlet(servlet = DMIProYesInhAnoOvrdMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DMIProYesInhAnoOvrdMDBServlet"),
                    @TestServlet(servlet = DMIPubYesInhAnoOvrdMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DMIPubYesInhAnoOvrdMDBServlet"),

                    @TestServlet(servlet = DMIPkgYesInhDDOvrdMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DMIPkgYesInhDDOvrdMDBServlet"),
                    @TestServlet(servlet = DMIPriYesInhDDOvrdMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DMIPriYesInhDDOvrdMDBServlet"),
                    @TestServlet(servlet = DMIProYesInhDDOvrdMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DMIProYesInhDDOvrdMDBServlet"),
                    @TestServlet(servlet = DMIPubYesInhDDOvrdMDBServlet.class, path = "JPA10InjectionMDB" + "/" + "DMIPubYesInhDDOvrdMDBServlet"),

    //
    })
    public static LibertyServer server1;

    public static final JdbcDatabaseContainer<?> testContainer = FATSuite.testContainer;

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
        bannerStart(JPA10Injection_MDB.class);
        timestart = System.currentTimeMillis();

        server1.addEnvVar("repeat_phase", RepeaterInfo.repeatPhase);

        //Get driver name
        server1.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server1, testContainer);

        server1.startServer();

        setupDatabaseApplication(server1, "test-applications/injection_mdb/" + "ddl/");

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

        setupTestApplication_MDB();
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

                server1.deleteFileFromLibertyServerRoot("apps/" + "InjectionMDB" + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(JPA10Injection_MDB.class, timestart);
        }
    }

    private static void setupTestApplication_MDB() throws Exception {
        final String RESOURCE_ROOT = "test-applications/injection_mdb/";
        final String applicationName = "InjectionMDB"; // Name of EAR

        final String appModuleName = "injectionMDB";
        final String appFileRootPath = RESOURCE_ROOT + "/" + appModuleName + ".ear/";
        final String libsPath = RESOURCE_ROOT + "libs/";

        // Library Jars

        // jpapulib.jar
        final JavaArchive jpapulibJar = ShrinkWrap.create(JavaArchive.class, "jpapulib.jar");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earlib");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earroot");
        ShrinkHelper.addDirectory(jpapulibJar, libsPath + "jpapulib.jar");

        // jpacore.jar
        final JavaArchive jpacoreJar = ShrinkWrap.create(JavaArchive.class, "jpacore.jar");
        jpacoreJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.core");

        // jpaejbA.jar
        final JavaArchive jpaejbAJar = ShrinkWrap.create(JavaArchive.class, "jpaejbA.jar");
        jpaejbAJar.addClass(com.ibm.ws.jpa.fvt.injection.entities.ejb.EJBEntityA.class);

        // jpaejbA.jar
        final JavaArchive jpaejbBJar = ShrinkWrap.create(JavaArchive.class, "jpaejbB.jar");
        jpaejbBJar.addClass(com.ibm.ws.jpa.fvt.injection.entities.ejb.EJBEntityB.class);

        // jpawar.jar
        final JavaArchive jpawarJar = ShrinkWrap.create(JavaArchive.class, "jpawar.jar");
        jpawarJar.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");

        final JavaArchive testApiJar = buildTestAPIJar();

        // EJB Jar Module
        JavaArchive ejbApp = ShrinkWrap.create(JavaArchive.class, appModuleName + ".jar");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.mdb");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.mdb.dfi");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.mdb.dmi");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.mdb.jndi");
        ShrinkHelper.addDirectory(ejbApp, appFileRootPath + appModuleName + ".jar");

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appModuleName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.mdb.jndi");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.mdb.dfi");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.mdb.dmi");
        ShrinkHelper.addDirectory(webApp, appFileRootPath + appModuleName + ".war");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, applicationName + ".ear");
        app.addAsModule(webApp);
        app.addAsModule(ejbApp);
        app.addAsLibrary(jpapulibJar);
        app.addAsLibrary(jpacoreJar);
        app.addAsLibrary(jpaejbAJar);
        app.addAsLibrary(jpaejbBJar);
        app.addAsLibrary(jpawarJar);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, appFileRootPath, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

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

        if (RepeaterInfo.repeatPhase != null && RepeaterInfo.repeatPhase.contains("hibernate")) {
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
