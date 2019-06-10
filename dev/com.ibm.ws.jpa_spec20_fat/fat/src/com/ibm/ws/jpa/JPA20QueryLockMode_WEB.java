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

package com.ibm.ws.jpa;

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
import com.ibm.ws.jpa.fvt.jpa20.querylockmode.tests.web.TestQueryLockModeServlet;

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
public class JPA20QueryLockMode_WEB extends JPAFATServletClient {
    private final static String RESOURCE_ROOT = "test-applications/querylockmode/";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();
    private final static Set<String> populateSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA20_QUERYLOCKMODE_DROP_${dbvendor}.ddl");
        createSet.add("JPA20_QUERYLOCKMODE_CREATE_${dbvendor}.ddl");
        populateSet.add("JPA20_QUERYLOCKMODE_POPULATE_${dbvendor}.ddl");
    }

    @Server("JPA20Server")
    @TestServlets({
                    @TestServlet(servlet = TestQueryLockModeServlet.class, path = "QueryLockModeWeb" + "/" + "TestQueryLockModeServlet"),

    })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);
        bannerStart(JPA20QueryLockMode_WEB.class);
        timestart = System.currentTimeMillis();

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

//        ddlSet.clear();
//        for (String ddlName : populateSet) {
//            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
//        }
//        executeDDL(server1, ddlSet, false);

        setupTestApplication();
    }

    private static void setupTestApplication() throws Exception {
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, "QueryLockModeWeb.war");
        webApp.addPackages(true, "suite.r80.base.common.datamodel.entities");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.jpa20.querylockmode.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.jpa20.querylockmode.tests.web");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + "web/QueryLockModeWeb.war");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, "QueryLockModeWeb.ear");
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
        appRecord.setLocation("QueryLockModeWeb.ear");
        appRecord.setName("QueryLockModeWeb");

        server1.setMarkToEndOfLog();
        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add("QueryLockModeWeb");
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
//            server1.dumpServer("QueryLockModeWeb");
            server1.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                               "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            bannerEnd(JPA20QueryLockMode_WEB.class, timestart);
        }

        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().clear();
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        server1.deleteFileFromLibertyServerRoot("apps/QueryLockModeWeb.ear");
        server1.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
    }
}
