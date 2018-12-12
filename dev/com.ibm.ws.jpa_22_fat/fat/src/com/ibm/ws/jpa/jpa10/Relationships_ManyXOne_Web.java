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

package com.ibm.ws.jpa.jpa10;

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
import com.ibm.ws.jpa.fvt.relationships.manyXone.tests.web.TestManyXOneBidirectionalServlet;
import com.ibm.ws.jpa.fvt.relationships.manyXone.tests.web.TestManyXOneCompoundPKServlet;
import com.ibm.ws.jpa.fvt.relationships.manyXone.tests.web.TestManyXOneUnidirectionalServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

@RunWith(FATRunner.class)
public class Relationships_ManyXOne_Web extends JPAFATServletClient {
    private final static String RESOURCE_ROOT = "test-applications/jpa10/relationships/manyXone/";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA10_MANYXONE_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_MANYXONE_CREATE_${dbvendor}.ddl");
    }

    @Server("JPA10Server")
    @TestServlets({
                    @TestServlet(servlet = TestManyXOneUnidirectionalServlet.class, path = "ManyXOne10Web" + "/" + "TestManyXOneUnidirectionalServlet"),
                    @TestServlet(servlet = TestManyXOneBidirectionalServlet.class, path = "ManyXOne10Web" + "/" + "TestManyXOneBidirectionalServlet"),
                    @TestServlet(servlet = TestManyXOneCompoundPKServlet.class, path = "ManyXOne10Web" + "/" + "TestManyXOneCompoundPKServlet"),

    })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);
        bannerStart(CallbackTest.class);
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

        setupTestApplication();
    }

    private static void setupTestApplication() throws Exception {
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, "manyXone.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXone.entities");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.annotation");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.xml");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.compoundpk");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.compoundpk.annotated");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.compoundpk.xml");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.nooptional.annotated");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.nooptional.xml");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.uni.annotation");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.uni.xml");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXone.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.relationships.manyXone.tests.web");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + "web/manyXone.war");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, "ManyXOne_Web.ear");
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
        server1.addInstalledAppForValidation("ManyXOne_Web");

        Application appRecord = new Application();
        appRecord.setLocation("ManyXOne_Web.ear");
        appRecord.setName("ManyXOne_Web");

        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add("ManyXOne_Web");
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server1.dumpServer("relationships_manyXone_web");
            server1.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                               "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            bannerEnd(Relationships_ManyXOne_Web.class, timestart);
        }

        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().clear();
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        server1.deleteFileFromLibertyServerRoot("apps/ManyXOne_Web.ear");
        server1.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
    }

}
