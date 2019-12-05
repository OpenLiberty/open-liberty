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
import com.ibm.ws.jpa.fvt.relationships.oneXmany.tests.web.TestOneXManyCollectionTypeServlet;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.tests.web.TestOneXManyCompoundPKServlet;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.tests.web.TestOneXManyUnidirectionalServlet;

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
public class Relationships_OneXMany_Web extends JPAFATServletClient {
    private final static String RESOURCE_ROOT = "test-applications/jpa10/relationships/oneXmany/";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA10_ONEXMANY_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_ONEXMANY_CREATE_${dbvendor}.ddl");
    }

    @Server("JPA10Server")
    @TestServlets({
                    @TestServlet(servlet = TestOneXManyUnidirectionalServlet.class, path = "OneXMany10Web" + "/" + "TestOneXManyUnidirectionalServlet"),
                    @TestServlet(servlet = TestOneXManyCollectionTypeServlet.class, path = "OneXMany10Web" + "/" + "TestOneXManyCollectionTypeServlet"),
                    @TestServlet(servlet = TestOneXManyCompoundPKServlet.class, path = "OneXMany10Web" + "/" + "TestOneXManyCompoundPKServlet"),

    })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);
        bannerStart(Relationships_OneXMany_Web.class);
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
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, "oneXmany.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.annotated");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.xml");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.containertype.annotated");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.containertype.xml");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.annotation");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.xml");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXmany.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.oneXmany.tests.web");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + "web/oneXmany.war");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, "OneXMany_Web.ear");
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
        appRecord.setLocation("OneXMany_Web.ear");
        appRecord.setName("OneXMany_Web");

        server1.setMarkToEndOfLog();
        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add("OneXMany_Web");
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
//            server1.dumpServer("relationships_oneXmany_web");
            server1.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                               "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            bannerEnd(Relationships_OneXMany_Web.class, timestart);
        }

        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().clear();
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        server1.deleteFileFromLibertyServerRoot("apps/OneXMany_Web.ear");
        server1.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
    }

}
