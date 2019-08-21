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
import com.ibm.ws.jpa.fvt.relationships.oneXone.tests.web.TestOneXOneBidirectionalServlet;
import com.ibm.ws.jpa.fvt.relationships.oneXone.tests.web.TestOneXOneCompoundPKServlet;
import com.ibm.ws.jpa.fvt.relationships.oneXone.tests.web.TestOneXOnePKJoinServlet;
import com.ibm.ws.jpa.fvt.relationships.oneXone.tests.web.TestOneXOneUnidirectionalServlet;

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
public class Relationships_OneXOne_Web extends JPAFATServletClient {
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
                    @TestServlet(servlet = TestOneXOneUnidirectionalServlet.class, path = "OneXOne10Web" + "/" + "TestOneXOneUnidirectionalServlet"),
                    @TestServlet(servlet = TestOneXOnePKJoinServlet.class, path = "OneXOne10Web" + "/" + "TestOneXOnePKJoinServlet"),
                    @TestServlet(servlet = TestOneXOneCompoundPKServlet.class, path = "OneXOne10Web" + "/" + "TestOneXOneCompoundPKServlet"),
                    @TestServlet(servlet = TestOneXOneBidirectionalServlet.class, path = "OneXOne10Web" + "/" + "TestOneXOneBidirectionalServlet"),
    })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);
        bannerStart(Relationships_OneXOne_Web.class);
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
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, "oneXone.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.annotation");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.xml");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.annotation");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.xml");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.nooptional.annotation");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.nooptional.xml");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.pkjoincolumn.annotation");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.pkjoincolumn.xml");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.uni.annotation");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.uni.xml");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXone.tests.web");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + "web/oneXone.war");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, "OneXOne_Web.ear");
        app.addAsModule(webApp);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + "web", new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

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
        appRecord.setLocation("OneXOne_Web.ear");
        appRecord.setName("OneXOne_Web");

        server1.setMarkToEndOfLog();
        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add("OneXOne_Web");
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
//            server1.dumpServer("relationships_oneXone_web");
            server1.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                               "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            bannerEnd(Relationships_OneXOne_Web.class, timestart);
        }

        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().clear();
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        server1.deleteFileFromLibertyServerRoot("apps/OneXOne_Web.ear");
        server1.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
    }

}
