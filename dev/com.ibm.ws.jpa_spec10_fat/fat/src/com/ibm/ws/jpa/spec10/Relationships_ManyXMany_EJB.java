/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.spec10;

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
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.jpa.FATSuite;
import com.ibm.ws.jpa.JPAFATServletClient;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.tests.ejb.TestManyXManyBidirectional_EJB_SFEX_Servlet;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.tests.ejb.TestManyXManyBidirectional_EJB_SF_Servlet;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.tests.ejb.TestManyXManyBidirectional_EJB_SL_Servlet;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.tests.ejb.TestManyXManyCollectionType_EJB_SFEX_Servlet;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.tests.ejb.TestManyXManyCollectionType_EJB_SF_Servlet;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.tests.ejb.TestManyXManyCollectionType_EJB_SL_Servlet;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.tests.ejb.TestManyXManyCompoundPK_EJB_SFEX_Servlet;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.tests.ejb.TestManyXManyCompoundPK_EJB_SF_Servlet;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.tests.ejb.TestManyXManyCompoundPK_EJB_SL_Servlet;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.tests.ejb.TestManyXManyUnidirectional_EJB_SFEX_Servlet;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.tests.ejb.TestManyXManyUnidirectional_EJB_SF_Servlet;
import com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.tests.ejb.TestManyXManyUnidirectional_EJB_SL_Servlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Relationships_ManyXMany_EJB extends JPAFATServletClient {
    private final static String RESOURCE_ROOT = "test-applications/jpa10/relationships/manyXmany/";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA10_MANYXMANY_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_MANYXMANY_CREATE_${dbvendor}.ddl");
    }

    @Server("JPA10Server")
    @TestServlets({
                    @TestServlet(servlet = TestManyXManyUnidirectional_EJB_SL_Servlet.class, path = "ManyXMany10EJB" + "/" + "TestManyXManyUnidirectional_EJB_SL_Servlet"),
                    @TestServlet(servlet = TestManyXManyUnidirectional_EJB_SF_Servlet.class, path = "ManyXMany10EJB" + "/" + "TestManyXManyUnidirectional_EJB_SF_Servlet"),
                    @TestServlet(servlet = TestManyXManyUnidirectional_EJB_SFEX_Servlet.class, path = "ManyXMany10EJB" + "/" + "TestManyXManyUnidirectional_EJB_SFEX_Servlet"),
                    @TestServlet(servlet = TestManyXManyBidirectional_EJB_SL_Servlet.class, path = "ManyXMany10EJB" + "/" + "TestManyXManyBidirectional_EJB_SL_Servlet"),
                    @TestServlet(servlet = TestManyXManyBidirectional_EJB_SF_Servlet.class, path = "ManyXMany10EJB" + "/" + "TestManyXManyBidirectional_EJB_SF_Servlet"),
                    @TestServlet(servlet = TestManyXManyBidirectional_EJB_SFEX_Servlet.class, path = "ManyXMany10EJB" + "/" + "TestManyXManyBidirectional_EJB_SFEX_Servlet"),
                    @TestServlet(servlet = TestManyXManyCollectionType_EJB_SL_Servlet.class, path = "ManyXMany10EJB" + "/" + "TestManyXManyCollectionType_EJB_SL_Servlet"),
                    @TestServlet(servlet = TestManyXManyCollectionType_EJB_SF_Servlet.class, path = "ManyXMany10EJB" + "/" + "TestManyXManyCollectionType_EJB_SF_Servlet"),
                    @TestServlet(servlet = TestManyXManyCollectionType_EJB_SFEX_Servlet.class, path = "ManyXMany10EJB" + "/" + "TestManyXManyCollectionType_EJB_SFEX_Servlet"),
                    @TestServlet(servlet = TestManyXManyCompoundPK_EJB_SL_Servlet.class, path = "ManyXMany10EJB" + "/" + "TestManyXManyCompoundPK_EJB_SL_Servlet"),
                    @TestServlet(servlet = TestManyXManyCompoundPK_EJB_SF_Servlet.class, path = "ManyXMany10EJB" + "/" + "TestManyXManyCompoundPK_EJB_SF_Servlet"),
                    @TestServlet(servlet = TestManyXManyCompoundPK_EJB_SFEX_Servlet.class, path = "ManyXMany10EJB" + "/" + "TestManyXManyCompoundPK_EJB_SFEX_Servlet"),
    })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);
        bannerStart(Relationships_ManyXMany_EJB.class);
        timestart = System.currentTimeMillis();

        int appStartTimeout = server1.getAppStartTimeout();
        if (appStartTimeout < (120 * 1000)) {
            server1.setAppStartTimeout(120 * 1000);
        }

        int configUpdateTimeout = server1.getConfigUpdateTimeout();
        if (configUpdateTimeout < (120 * 1000)) {
            server1.setConfigUpdateTimeout(120 * 1000);
        }

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

        setupTestApplication();
    }

    private static void setupTestApplication() throws Exception {
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, "manyXmanyejb.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.tests.ejb");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + "ejb/manyXmanyejb.war");

        JavaArchive ejbApp = ShrinkWrap.create(JavaArchive.class, "manyXmany.jar");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXmany.ejblocal");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.annotation");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.xml");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.annotated");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.xml");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.containertype.annotated");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.containertype.xml");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.annotation");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.xml");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic");
        ShrinkHelper.addDirectory(ejbApp, RESOURCE_ROOT + "ejb/manyXmany.jar");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, "ManyXMany_EJB.ear");
        app.addAsModule(ejbApp);
        app.addAsModule(webApp);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + "ejb", new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

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
        appRecord.setLocation("ManyXMany_EJB.ear");
        appRecord.setName("ManyXMany_EJB");

        server1.setMarkToEndOfLog();
        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add("ManyXMany_EJB");
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
//            server1.dumpServer("relationships_manyXmany_ejb");
            server1.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                               "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            bannerEnd(Relationships_ManyXMany_EJB.class, timestart);
        }

        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().clear();
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        server1.deleteFileFromLibertyServerRoot("apps/ManyXMany_EJB.ear");
        server1.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
    }
}
