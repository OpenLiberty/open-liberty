/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import com.ibm.ws.jpa.fvt.relationships.oneXone.tests.ejb.TestOneXOneBidirectional_EJB_SFEX_Servlet;
import com.ibm.ws.jpa.fvt.relationships.oneXone.tests.ejb.TestOneXOneBidirectional_EJB_SF_Servlet;
import com.ibm.ws.jpa.fvt.relationships.oneXone.tests.ejb.TestOneXOneBidirectional_EJB_SL_Servlet;
import com.ibm.ws.jpa.fvt.relationships.oneXone.tests.ejb.TestOneXOneCompoundPK_EJB_SFEX_Servlet;
import com.ibm.ws.jpa.fvt.relationships.oneXone.tests.ejb.TestOneXOneCompoundPK_EJB_SF_Servlet;
import com.ibm.ws.jpa.fvt.relationships.oneXone.tests.ejb.TestOneXOneCompoundPK_EJB_SL_Servlet;
import com.ibm.ws.jpa.fvt.relationships.oneXone.tests.ejb.TestOneXOnePKJoin_EJB_SFEX_Servlet;
import com.ibm.ws.jpa.fvt.relationships.oneXone.tests.ejb.TestOneXOnePKJoin_EJB_SF_Servlet;
import com.ibm.ws.jpa.fvt.relationships.oneXone.tests.ejb.TestOneXOnePKJoin_EJB_SL_Servlet;
import com.ibm.ws.jpa.fvt.relationships.oneXone.tests.ejb.TestOneXOneUnidirectional_EJB_SFEX_Servlet;
import com.ibm.ws.jpa.fvt.relationships.oneXone.tests.ejb.TestOneXOneUnidirectional_EJB_SF_Servlet;
import com.ibm.ws.jpa.fvt.relationships.oneXone.tests.ejb.TestOneXOneUnidirectional_EJB_SL_Servlet;

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
public class Relationships_OneXOne_EJB extends JPAFATServletClient {
    private final static String RESOURCE_ROOT = "test-applications/jpa10/relationships/oneXone/";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA10_ONEXONE_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_ONEXONE_CREATE_${dbvendor}.ddl");
    }

    @Server("JPA10Server")
    @TestServlets({
                    @TestServlet(servlet = TestOneXOneUnidirectional_EJB_SL_Servlet.class, path = "OneXOne10EJB" + "/" + "TestOneXOneUnidirectional_EJB_SL_Servlet"),
                    @TestServlet(servlet = TestOneXOneUnidirectional_EJB_SF_Servlet.class, path = "OneXOne10EJB" + "/" + "TestOneXOneUnidirectional_EJB_SF_Servlet"),
                    @TestServlet(servlet = TestOneXOneUnidirectional_EJB_SFEX_Servlet.class, path = "OneXOne10EJB" + "/" + "TestOneXOneUnidirectional_EJB_SFEX_Servlet"),

                    @TestServlet(servlet = TestOneXOneCompoundPK_EJB_SL_Servlet.class, path = "OneXOne10EJB" + "/" + "TestOneXOneCompoundPK_EJB_SL_Servlet"),
                    @TestServlet(servlet = TestOneXOneCompoundPK_EJB_SF_Servlet.class, path = "OneXOne10EJB" + "/" + "TestOneXOneCompoundPK_EJB_SF_Servlet"),
                    @TestServlet(servlet = TestOneXOneCompoundPK_EJB_SFEX_Servlet.class, path = "OneXOne10EJB" + "/" + "TestOneXOneCompoundPK_EJB_SFEX_Servlet"),

                    @TestServlet(servlet = TestOneXOnePKJoin_EJB_SL_Servlet.class, path = "OneXOne10EJB" + "/" + "TestOneXOnePKJoin_EJB_SL_Servlet"),
                    @TestServlet(servlet = TestOneXOnePKJoin_EJB_SF_Servlet.class, path = "OneXOne10EJB" + "/" + "TestOneXOnePKJoin_EJB_SF_Servlet"),
                    @TestServlet(servlet = TestOneXOnePKJoin_EJB_SFEX_Servlet.class, path = "OneXOne10EJB" + "/" + "TestOneXOnePKJoin_EJB_SFEX_Servlet"),

                    @TestServlet(servlet = TestOneXOneBidirectional_EJB_SL_Servlet.class, path = "OneXOne10EJB" + "/" + "TestOneXOneBidirectional_EJB_SL_Servlet"),
                    @TestServlet(servlet = TestOneXOneBidirectional_EJB_SF_Servlet.class, path = "OneXOne10EJB" + "/" + "TestOneXOneBidirectional_EJB_SF_Servlet"),
                    @TestServlet(servlet = TestOneXOneBidirectional_EJB_SFEX_Servlet.class, path = "OneXOne10EJB" + "/" + "TestOneXOneBidirectional_EJB_SFEX_Servlet"),

    })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);
        bannerStart(Relationships_OneXOne_EJB.class);
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
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, "oneXoneejb.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.tests.ejb");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + "ejb/oneXoneejb.war");

        JavaArchive ejbApp = ShrinkWrap.create(JavaArchive.class, "oneXone.jar");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.ejblocal");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.annotation");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.xml");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.annotation");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.xml");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.nooptional.annotation");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.nooptional.xml");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.pkjoincolumn.annotation");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.pkjoincolumn.xml");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.uni.annotation");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.uni.xml");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.testlogic");

        ShrinkHelper.addDirectory(ejbApp, RESOURCE_ROOT + "ejb/oneXone.jar");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, "OneXOne_EJB.ear");
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
        appRecord.setLocation("OneXOne_EJB.ear");
        appRecord.setName("OneXOne_EJB");

        server1.setMarkToEndOfLog();
        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add("OneXOne_EJB");
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
//            server1.dumpServer("relationships_oneXone_ejb");
            server1.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                               "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            bannerEnd(Relationships_OneXOne_EJB.class, timestart);
        }

        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().clear();
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        server1.deleteFileFromLibertyServerRoot("apps/OneXOne_EJB.ear");
        server1.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
    }

}
