/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dfi.inh.anoovrd.DFI_Inh_AnoOvrd_EIW_SF_EJBTestServlet;
import com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dfi.inh.anoovrd.DFI_Inh_AnoOvrd_EIW_SL_EJBTestServlet;
import com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dfi.inh.ddovrd.DFI_Inh_DDOvrd_EIW_SF_EJBTestServlet;
import com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dfi.inh.ddovrd.DFI_Inh_DDOvrd_EIW_SL_EJBTestServlet;
import com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dfi.noinh.DFI_NoInh_EIW_SF_EJBTestServlet;
import com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dfi.noinh.DFI_NoInh_EIW_SL_EJBTestServlet;
import com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dmi.inh.anoovrd.DMI_Inh_AnoOvrd_EIW_SF_EJBTestServlet;
import com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dmi.inh.anoovrd.DMI_Inh_AnoOvrd_EIW_SL_EJBTestServlet;
import com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dmi.inh.ddovrd.DMI_Inh_DDOvrd_EIW_SF_EJBTestServlet;
import com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dmi.inh.ddovrd.DMI_Inh_DDOvrd_EIW_SL_EJBTestServlet;
import com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dmi.noinh.DMI_NoInh_EIW_SF_EJBTestServlet;
import com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dmi.noinh.DMI_NoInh_EIW_SL_EJBTestServlet;
import com.ibm.ws.jpa.fvt.ejbinwar.tests.web.jndi.JNDI_EIW_SF_EJBTestServlet;
import com.ibm.ws.jpa.fvt.ejbinwar.tests.web.jndi.JNDI_EIW_SL_EJBTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

@RunWith(FATRunner.class)
public class EjbInWar_Test extends JPAFATServletClient {
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

    @Server("JPAServer")
    @TestServlets({
                    @TestServlet(servlet = JNDI_EIW_SL_EJBTestServlet.class, path = "ejbinwar_jndi" + "/" + "JNDI_EIW_SL_EJBTestServlet"),
                    @TestServlet(servlet = JNDI_EIW_SF_EJBTestServlet.class, path = "ejbinwar_jndi" + "/" + "JNDI_EIW_SF_EJBTestServlet"),

                    @TestServlet(servlet = DFI_NoInh_EIW_SL_EJBTestServlet.class, path = "ejbinwar_dfi" + "/" + "DFI_NoInh_EIW_SL_EJBTestServlet"),
                    @TestServlet(servlet = DFI_NoInh_EIW_SF_EJBTestServlet.class, path = "ejbinwar_dfi" + "/" + "DFI_NoInh_EIW_SF_EJBTestServlet"),
                    @TestServlet(servlet = DFI_Inh_AnoOvrd_EIW_SL_EJBTestServlet.class, path = "ejbinwar_dfi" + "/" + "DFI_Inh_AnoOvrd_EIW_SL_EJBTestServlet"),
                    @TestServlet(servlet = DFI_Inh_AnoOvrd_EIW_SF_EJBTestServlet.class, path = "ejbinwar_dfi" + "/" + "DFI_Inh_AnoOvrd_EIW_SF_EJBTestServlet"),
                    @TestServlet(servlet = DFI_Inh_DDOvrd_EIW_SL_EJBTestServlet.class, path = "ejbinwar_dfi" + "/" + "DFI_Inh_DDOvrd_EIW_SL_EJBTestServlet"),
                    @TestServlet(servlet = DFI_Inh_DDOvrd_EIW_SF_EJBTestServlet.class, path = "ejbinwar_dfi" + "/" + "DFI_Inh_DDOvrd_EIW_SF_EJBTestServlet"),

                    @TestServlet(servlet = DMI_NoInh_EIW_SL_EJBTestServlet.class, path = "ejbinwar_dmi" + "/" + "DMI_NoInh_EIW_SL_EJBTestServlet"),
                    @TestServlet(servlet = DMI_NoInh_EIW_SF_EJBTestServlet.class, path = "ejbinwar_dmi" + "/" + "DMI_NoInh_EIW_SF_EJBTestServlet"),
                    @TestServlet(servlet = DMI_Inh_AnoOvrd_EIW_SL_EJBTestServlet.class, path = "ejbinwar_dmi" + "/" + "DMI_Inh_AnoOvrd_EIW_SL_EJBTestServlet"),
                    @TestServlet(servlet = DMI_Inh_AnoOvrd_EIW_SF_EJBTestServlet.class, path = "ejbinwar_dmi" + "/" + "DMI_Inh_AnoOvrd_EIW_SF_EJBTestServlet"),
                    @TestServlet(servlet = DMI_Inh_DDOvrd_EIW_SL_EJBTestServlet.class, path = "ejbinwar_dmi" + "/" + "DMI_Inh_DDOvrd_EIW_SL_EJBTestServlet"),
                    @TestServlet(servlet = DMI_Inh_DDOvrd_EIW_SF_EJBTestServlet.class, path = "ejbinwar_dmi" + "/" + "DMI_Inh_DDOvrd_EIW_SF_EJBTestServlet"),

    })
    public static LibertyServer server1;

    public static final JdbcDatabaseContainer<?> testContainer = FATSuite.testContainer;

    @BeforeClass
    public static void setUp() throws Exception {
        setUp(server1, EjbInWar_Test.class);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        tearDown(server1, EjbInWar_Test.class);
    }

    private final static String RESOURCE_ROOT = "test-applications/injection_ejbinwar/";
    private final static String applicationName = "EJBInWar"; // Name of EAR

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

        final JavaArchive testApiJar = buildTestAPIJar();

        // Application Modules

        // Web Test WAR Module

        // JNDI_EIW.war
        WebArchive JNDI_EIW_webApp = ShrinkWrap.create(WebArchive.class, "JNDI_EIW.war");
        JNDI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar.ejb.jndi");
        JNDI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar.testlogic");
        JNDI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar.tests.web.jndi");
        JNDI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.ejb");
        JNDI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        ShrinkHelper.addDirectory(JNDI_EIW_webApp, RESOURCE_ROOT + "/JNDI_EIW.war");

        // DFI_EIW.war
        WebArchive DFI_EIW_webApp = ShrinkWrap.create(WebArchive.class, "DFI_EIW.war");
        DFI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar.ejb.dfi");
        DFI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar.testlogic");
        DFI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dfi.noinh");
        DFI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dfi.inh.anoovrd");
        DFI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dfi.inh.ddovrd");
        DFI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.ejb");
        DFI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        ShrinkHelper.addDirectory(DFI_EIW_webApp, RESOURCE_ROOT + "/DFI_EIW.war");

        // DMI_EIW.war
        WebArchive DMI_EIW_webApp = ShrinkWrap.create(WebArchive.class, "DMI_EIW.war");
        DMI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar.ejb.dmi");
        DMI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar.testlogic");
        DMI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dmi.noinh");
        DMI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dmi.inh.anoovrd");
        DMI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.ejbinwar.tests.web.dmi.inh.ddovrd");
        DMI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.ejb");
        DMI_EIW_webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        ShrinkHelper.addDirectory(DMI_EIW_webApp, RESOURCE_ROOT + "/DMI_EIW.war");

        // Assemble Application
        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, applicationName + ".ear");
        app.addAsModule(JNDI_EIW_webApp);
        app.addAsModule(DFI_EIW_webApp);
        app.addAsModule(DMI_EIW_webApp);
        app.addAsLibrary(jpapulibJar);
        app.addAsLibrary(jpacoreJar);
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
