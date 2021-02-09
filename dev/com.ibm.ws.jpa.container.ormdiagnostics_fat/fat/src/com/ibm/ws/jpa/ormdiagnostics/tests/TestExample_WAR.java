/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.ormdiagnostics.tests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.jpa.ormdiagnostics.FATSuite;
import com.ibm.ws.jpa.ormdiagnostics.ORMIntrospectorHelper;
import com.ibm.ws.jpa.ormdiagnostics.ORMIntrospectorHelper.JPAClass;
import com.ibm.ws.ormdiag.example.war.ExampleServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

@RunWith(FATRunner.class)
public class TestExample_WAR extends JPAFATServletClient {
    private final static String CONTEXT_ROOT = "exampleWAR";
    private final static String RESOURCE_ROOT = "test-applications/example/";
    private final static String appName = "exampleWAR";

    private static long timestart = 0;

    @Server("JPAORMServer")
    @TestServlets({
                    @TestServlet(servlet = ExampleServlet.class, path = CONTEXT_ROOT + "/" + "ExampleServlet")
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, FATSuite.JAXB_PERMS);
        bannerStart(TestExample_WAR.class);
        timestart = System.currentTimeMillis();

        int appStartTimeout = server.getAppStartTimeout();
        if (appStartTimeout < (120 * 1000)) {
            server.setAppStartTimeout(120 * 1000);
        }

        int configUpdateTimeout = server.getConfigUpdateTimeout();
        if (configUpdateTimeout < (120 * 1000)) {
            server.setConfigUpdateTimeout(120 * 1000);
        }

        server.startServer();

        setupTestApplication();
    }

    private static void setupTestApplication() throws Exception {
        //Create a JPA bundle
        JavaArchive jpaJar = ShrinkWrap.create(JavaArchive.class, appName + "_JPA.jar");
        jpaJar.addPackages(true, "com.ibm.ws.ormdiag.example.jpa");
        ShrinkHelper.addDirectory(jpaJar, RESOURCE_ROOT + "resources/jpa");

        //Create an EJB bundle
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, appName + "_EJB.jar");
        ejbJar.addPackages(true, "com.ibm.ws.ormdiag.example.ejb");
        ShrinkHelper.addDirectory(ejbJar, RESOURCE_ROOT + "resources/ejb");

        //Create a WAR bundle with JPA & EJB libraries
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appName + "_WAR.war");
        webApp.addPackages(true, "com.ibm.ws.ormdiag.example.war");
        webApp.addAsLibrary(jpaJar);
        webApp.addAsLibrary(ejbJar);
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + "resources/war");

        ShrinkHelper.exportAppToServer(server, webApp);

        Application appRecord = new Application();
        appRecord.setLocation(appName + "_WAR.war");
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
            server.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                              "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            try {
                ServerConfiguration sc = server.getServerConfiguration();
                sc.getApplications().clear();
                server.updateServerConfiguration(sc);
                server.saveServerConfiguration();

                server.deleteFileFromLibertyServerRoot("apps/" + appName + "_WAR.war");
                server.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(TestExample_WAR.class, timestart);
        }
    }

    @Test
    public void testBasicDump() throws Exception {
        ServerConfiguration sc = server.getServerConfiguration();
        Set<String> fList = sc.getFeatureManager().getFeatures();

        if (fList.contains("jpa-2.0")) {
            // Auto pass, doesn't support ee7 default datasource
            return;
        }

        final LocalFile lf = server.dumpServer("jpa_testBasicDump");
        Assert.assertNotNull(lf);

        final String introspectorData = ORMIntrospectorHelper.extractJPAIntrospection(lf);
        Assert.assertNotNull(introspectorData);

        // Test for some easy and consistent strings that are expected to appear in the introspector text

        Assert.assertTrue(introspectorData.contains("JPA Runtime Internal State Information")); // Description

        Assert.assertTrue(introspectorData.contains("JPA Runtime Internal State Information")); // Description
        if (fList.contains("jpa-2.1") || fList.contains("jpaContainer-2.1")) {
            Assert.assertTrue(introspectorData.contains("jpaRuntime = com.ibm.ws.jpa.container.v21.internal.JPA21Runtime"));
        }
        if (fList.contains("jpa-2.2") || fList.contains("jpaContainer-2.2")) {
            Assert.assertTrue(introspectorData.contains("jpaRuntime = com.ibm.ws.jpa.container.v22.internal.JPA22Runtime"));
        }

        Assert.assertTrue(introspectorData.contains("Provider Runtime Integration Service = com.ibm.ws.jpa.container.eclipselink.EclipseLinkJPAProvider"));

        ORMIntrospectorHelper.verifyApplications(appName, 0, 1,
                                                 new String[] { "wsjpa:wsjar:file:.../" + appName + "_JPA.jar!/" },
                                                 introspectorData);

        List<String> expectedArchives = new ArrayList<String>();
        expectedArchives.add(appName + " (WAR)");
        expectedArchives.add(appName + "/WEB-INF/lib/" + appName + "_EJB.jar");
        expectedArchives.add(appName + "/WEB-INF/lib/" + appName + "_JPA.jar");
        ORMIntrospectorHelper.verifyApplicationArchives(expectedArchives, introspectorData);

        ORMIntrospectorHelper.verifyPersistenceUnit("EXAMPLE_JTA", introspectorData);

        List<JPAClass> classes = new Vector<JPAClass>();
        final String entity1 = "package com.ibm.ws.ormdiag.example.jpa;\n" +
                               "\n" +
                               "@javax.persistence.Entity\n" +
                               "@javax.persistence.NamedQueries(value={   @javax.persistence.NamedQuery(name=\"findAllEntities\", query=\"SELECT e FROM ExampleEntity e\")\n" +
                               "} )\n" +
                               "public class ExampleEntity {\n" +
                               "  // Fields\n" +
                               "  @javax.persistence.Id\n" +
                               "  private int id;\n" +
                               "\n" +
                               "  private java.lang.String str1;\n" +
                               "\n" +
                               "  private java.lang.String str2;\n" +
                               "\n" +
                               "  private java.lang.String str3;\n" +
                               "\n" +
                               "  private char char1;\n" +
                               "\n" +
                               "  private char char2;\n" +
                               "\n" +
                               "  private char char3;\n" +
                               "\n" +
                               "  private int int1;\n" +
                               "\n" +
                               "  private int int2;\n" +
                               "\n" +
                               "  private int int3;\n" +
                               "\n" +
                               "  private long long1;\n" +
                               "\n" +
                               "  private long long2;\n" +
                               "\n" +
                               "  private long long3;\n" +
                               "\n" +
                               "  private short short1;\n" +
                               "\n" +
                               "  private short short2;\n" +
                               "\n" +
                               "  private short short3;\n" +
                               "\n" +
                               "  private float float1;\n" +
                               "\n" +
                               "  private float float2;\n" +
                               "\n" +
                               "  private float float3;\n" +
                               "\n" +
                               "  private double double1;\n" +
                               "\n" +
                               "  private double double2;\n" +
                               "\n" +
                               "  private double double3;\n" +
                               "\n" +
                               "  private boolean boolean1;\n" +
                               "\n" +
                               "  private boolean boolean2;\n" +
                               "\n" +
                               "  private boolean boolean3;\n" +
                               "\n" +
                               "  private byte byte1;\n" +
                               "\n" +
                               "  private byte byte2;\n" +
                               "\n" +
                               "  private byte byte3;\n" +
                               "\n" +
                               "  // Methods\n" +
                               "  public ExampleEntity();\n" +
                               "\n" +
                               "  public int getId();\n" +
                               "\n" +
                               "  public void setId(int);\n" +
                               "\n" +
                               "  public java.lang.String getStr1();\n" +
                               "\n" +
                               "  public void setStr1(java.lang.String);\n" +
                               "\n" +
                               "  public java.lang.String getStr2();\n" +
                               "\n" +
                               "  public void setStr2(java.lang.String);\n" +
                               "\n" +
                               "  public java.lang.String getStr3();\n" +
                               "\n" +
                               "  public void setStr3(java.lang.String);\n" +
                               "\n" +
                               "  public char getChar1();\n" +
                               "\n" +
                               "  public void setChar1(char);\n" +
                               "\n" +
                               "  public char getChar2();\n" +
                               "\n" +
                               "  public void setChar2(char);\n" +
                               "\n" +
                               "  public char getChar3();\n" +
                               "\n" +
                               "  public void setChar3(char);\n" +
                               "\n" +
                               "  public int getInt1();\n" +
                               "\n" +
                               "  public void setInt1(int);\n" +
                               "\n" +
                               "  public int getInt2();\n" +
                               "\n" +
                               "  public void setInt2(int);\n" +
                               "\n" +
                               "  public int getInt3();\n" +
                               "\n" +
                               "  public void setInt3(int);\n" +
                               "\n" +
                               "  public long getLong1();\n" +
                               "\n" +
                               "  public void setLong1(long);\n" +
                               "\n" +
                               "  public long getLong2();\n" +
                               "\n" +
                               "  public void setLong2(long);\n" +
                               "\n" +
                               "  public long getLong3();\n" +
                               "\n" +
                               "  public void setLong3(long);\n" +
                               "\n" +
                               "  public short getShort1();\n" +
                               "\n" +
                               "  public void setShort1(short);\n" +
                               "\n" +
                               "  public short getShort2();\n" +
                               "\n" +
                               "  public void setShort2(short);\n" +
                               "\n" +
                               "  public short getShort3();\n" +
                               "\n" +
                               "  public void setShort3(short);\n" +
                               "\n" +
                               "  public float getFloat1();\n" +
                               "\n" +
                               "  public void setFloat1(float);\n" +
                               "\n" +
                               "  public float getFloat2();\n" +
                               "\n" +
                               "  public void setFloat2(float);\n" +
                               "\n" +
                               "  public float getFloat3();\n" +
                               "\n" +
                               "  public void setFloat3(float);\n" +
                               "\n" +
                               "  public double getDouble1();\n" +
                               "\n" +
                               "  public void setDouble1(double);\n" +
                               "\n" +
                               "  public double getDouble2();\n" +
                               "\n" +
                               "  public void setDouble2(double);\n" +
                               "\n" +
                               "  public double getDouble3();\n" +
                               "\n" +
                               "  public void setDouble3(double);\n" +
                               "\n" +
                               "  public boolean isBoolean1();\n" +
                               "\n" +
                               "  public void setBoolean1(boolean);\n" +
                               "\n" +
                               "  public boolean isBoolean2();\n" +
                               "\n" +
                               "  public void setBoolean2(boolean);\n" +
                               "\n" +
                               "  public boolean isBoolean3();\n" +
                               "\n" +
                               "  public void setBoolean3(boolean);\n" +
                               "\n" +
                               "  public byte getByte1();\n" +
                               "\n" +
                               "  public void setByte1(byte);\n" +
                               "\n" +
                               "  public byte getByte2();\n" +
                               "\n" +
                               "  public void setByte2(byte);\n" +
                               "\n" +
                               "  public byte getByte3();\n" +
                               "\n" +
                               "  public void setByte3(byte);\n" +
                               "\n" +
                               "}";
        classes.add(new JPAClass("com.ibm.ws.ormdiag.example.jpa.ExampleEntity", "wsjpa:wsjar:file:.../" + appName + "_JPA.jar!/", entity1));

        ORMIntrospectorHelper.verifyPersistentClasses(classes, introspectorData);
    }
}
