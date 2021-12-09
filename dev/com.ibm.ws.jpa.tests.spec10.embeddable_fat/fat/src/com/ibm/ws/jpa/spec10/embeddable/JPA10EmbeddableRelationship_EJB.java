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

package com.ibm.ws.jpa.spec10.embeddable;

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
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.jpa.embeddable.relationship.ejb.TestEmbeddableRelationship_EJB_SFEx_Servlet;
import com.ibm.ws.jpa.embeddable.relationship.ejb.TestEmbeddableRelationship_EJB_SF_Servlet;
import com.ibm.ws.jpa.embeddable.relationship.ejb.TestEmbeddableRelationship_EJB_SL_Servlet;

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
public class JPA10EmbeddableRelationship_EJB extends JPAFATServletClient {
    private final static String CONTEXT_ROOT = "embeddableRelationshipEjb";
    private final static String RESOURCE_ROOT = "test-applications/embeddable/relationship/";
    private final static String appFolder = "ejb";
    private final static String appName = "embeddableRelationshipEjb";
    private final static String appNameEar = appName + ".ear";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA10_EMBEDDABLE_RELATIONSHIP_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_EMBEDDABLE_RELATIONSHIP_CREATE_${dbvendor}.ddl");
    }

    @Server("JPA10Server")
    @TestServlets({
                    @TestServlet(servlet = TestEmbeddableRelationship_EJB_SL_Servlet.class, path = CONTEXT_ROOT + "/" + "TestEmbeddableRelationship_EJB_SL_Servlet"),
                    @TestServlet(servlet = TestEmbeddableRelationship_EJB_SF_Servlet.class, path = CONTEXT_ROOT + "/" + "TestEmbeddableRelationship_EJB_SF_Servlet"),
                    @TestServlet(servlet = TestEmbeddableRelationship_EJB_SFEx_Servlet.class, path = CONTEXT_ROOT + "/" + "TestEmbeddableRelationship_EJB_SFEx_Servlet")
    })
    public static LibertyServer server;

    public static final JdbcDatabaseContainer<?> testContainer = FATSuite.testContainer;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, FATSuite.JAXB_PERMS);
        bannerStart(JPA10EmbeddableRelationship_EJB.class);
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

        System.out.println(JPA10EmbeddableRelationship_EJB.class.getName() + " Setting up database tables...");

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
        JavaArchive ejbApp = ShrinkWrap.create(JavaArchive.class, appName + ".jar");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.embeddable.relationship.ejblocal");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.embeddable.relationship.model");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.embeddable.relationship.testlogic");
        ShrinkHelper.addDirectory(ejbApp, RESOURCE_ROOT + appFolder + "/" + appName + ".jar");

        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.embeddable.relationship.ejb");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + appFolder + "/" + appName + ".war");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, appNameEar);
        app.addAsModule(ejbApp);
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
//        ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
//        ClassloaderElement loader = new ClassloaderElement();
//        loader.setApiTypeVisibility("+third-party");
////        loader.getCommonLibraryRefs().add("HibernateLib");
//        cel.add(loader);

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
                              "WTRN0074E: Exception caught from before_completion synchronization operation", // RuntimeException test, expected
                              "CWWJP0055E" // TODO: OpenJPA throws a MetaDataException while parsing the XML nested embeddables
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
            bannerEnd(JPA10EmbeddableRelationship_EJB.class, timestart);
        }
    }
}
