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

package com.ibm.ws.jpa.ormdiagnostics.tests;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.ws.jpa.ormdiagnostics.ORMApplicationBuilder;
import com.ibm.ws.jpa.ormdiagnostics.ORMIntrospectorHelper;
import com.ibm.ws.jpa.ormdiagnostics.ORMIntrospectorHelper.JPAClass;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.PrivHelper;
import jpasimple.web.TestJPASimpleServlet;

@RunWith(FATRunner.class)
public class TestBasicLibertyDump extends FATServletClient {
    private static final Logger LOG = Logger.getLogger(TestBasicLibertyDump.class.getName());

    public static final String APP_NAME = "jpasimple";
    public static final String SERVLET = "TestJPASimple";

    @Server("JPADiagTestServer")
    @TestServlets({
                    @TestServlet(servlet = TestJPASimpleServlet.class, path = APP_NAME + "/" + SERVLET),
    })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1,
                                        "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect\";",
                                        "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind\";");

        LOG.info("Setup : Creating WAR");
        WebArchive war = ORMApplicationBuilder.createWAR("jpasimple.war", "jpasimple.entity", "jpasimple.web");
        ORMApplicationBuilder.addArchivetoServer(server1, "dropins", war);

        LOG.info("Setup : Starting Server");
        server1.startServer();

        server1.addInstalledAppForValidation("jpasimple");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer("CWWJP9991W");
    }

    @Test
    public void testBasicDump() throws Exception {
        callTestServlet();

        final LocalFile lf = server1.dumpServer("jpa_testBasicDump");
        Assert.assertNotNull(lf);

        final String introspectorData = ORMIntrospectorHelper.extractJPAIntrospection(lf);
        Assert.assertNotNull(introspectorData);

        // Test for some easy and consistent strings that are expected to appear in the introspector text

        Assert.assertTrue(introspectorData.contains("JPA Runtime Internal State Information")); // Description
        Assert.assertTrue(introspectorData.contains("jpaRuntime = com.ibm.ws.jpa.container.v22.internal.JPA22Runtime"));
        Assert.assertTrue(introspectorData.contains("Provider Runtime Integration Service = com.ibm.ws.jpa.container.eclipselink.EclipseLinkJPAProvider"));

        ORMIntrospectorHelper.verifyApplications("jpasimple", 0, 2,
                                                 new String[] { "jpasimple.war!/WEB-INF/classes/" },
                                                 introspectorData);

        ORMIntrospectorHelper.verifyPersistenceUnit("JPAPU", introspectorData);

        List<JPAClass> classes = new Vector<JPAClass>();
        final String entity1 = "package jpasimple.entity;\n" +
                               "\n" +
                               "@javax.persistence.Entity\n" +
                               "public class SimpleTestEntity {\n" +
                               "  // Fields\n" +
                               "  @javax.persistence.Id\n" +
                               "  @javax.persistence.GeneratedValue\n" +
                               "  private long id;\n" +
                               "\n" +
                               "  @javax.persistence.Basic\n" +
                               "  private java.lang.String strData;\n" +
                               "\n" +
                               "  @javax.persistence.Version\n" +
                               "  private long version;\n" +
                               "\n" +
                               "  // Methods\n" +
                               "  public SimpleTestEntity();\n" +
                               "\n" +
                               "  public long getId();\n" +
                               "\n" +
                               "  public void setId(long);\n" +
                               "\n" +
                               "  public java.lang.String getStrData();\n" +
                               "\n" +
                               "  public void setStrData(java.lang.String);\n" +
                               "\n" +
                               "  public long getVersion();\n" +
                               "\n" +
                               "  public void setVersion(long);\n" +
                               "\n" +
                               "  public java.lang.String toString();\n" +
                               "\n" +
                               "}";
        classes.add(new JPAClass("jpasimple.entity.SimpleTestEntity", "jpasimple.war!/WEB-INF/classes/", entity1));

        ORMIntrospectorHelper.verifyPersistentClasses(classes, introspectorData);
    }

    private void callTestServlet() throws Exception {
        FATServletClient.runTest(server1, APP_NAME + "/TestJPASimple", "testJPAFunction");
    }
}
