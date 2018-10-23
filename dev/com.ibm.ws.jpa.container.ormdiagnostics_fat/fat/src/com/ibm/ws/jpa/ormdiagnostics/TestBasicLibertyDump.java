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

package com.ibm.ws.jpa.ormdiagnostics;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

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

        final String resPath = "test-applications/" + APP_NAME + "/resources/";

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        app.addPackage("jpasimple.web");
        app.addPackage("jpasimple.entity");
        app.merge(ShrinkWrap.create(GenericArchive.class).as(ExplodedImporter.class).importDirectory(resPath).as(GenericArchive.class),
                  "/",
                  Filters.includeAll());
        ShrinkHelper.exportDropinAppToServer(server1, app);

        server1.startServer();
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

        final String introspectorData = extractJPAIntrospection(lf);
        Assert.assertNotNull(introspectorData);

        // Test for some easy and consistent strings that are expected to appear in the introspector text 

        Assert.assertTrue(introspectorData.contains("JPA Runtime Internal State Information")); // Description
        Assert.assertTrue(introspectorData.contains("jpaRuntime = com.ibm.ws.jpa.container.v22.internal.JPA22Runtime"));
        Assert.assertTrue(introspectorData.contains("Provider Runtime Integration Service = com.ibm.ws.jpa.container.eclipselink.EclipseLinkJPAProvider"));

        final String targetString1 = "################################################################################\n" +
                                     "Application \"jpasimple\":\n" +
                                     "   Total ORM Files: 0\n" +
                                     "   Total JPA Involved Classes: 2";
        Assert.assertTrue(introspectorData.contains(targetString1));

        final String targetString2 = "<persistence-unit name=\"JPAPU\">";
        Assert.assertTrue(introspectorData.contains(targetString2));

        final String targetString3 = "package jpasimple.entity;\n" +
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
        Assert.assertTrue(introspectorData.contains(targetString3));
    }

    private void callTestServlet() throws Exception {
        FATServletClient.runTest(server1, APP_NAME + "/TestJPASimple", "testJPAFunction");

    }

    private String extractJPAIntrospection(final LocalFile dumpLocalFile) throws Exception {
        final File dumpFile = new File(dumpLocalFile.getAbsolutePath());
        try (ZipFile zip = new ZipFile(dumpFile)) {
            final Enumeration<? extends ZipEntry> entryEnum = zip.entries();
            while (entryEnum.hasMoreElements()) {
                final ZipEntry zipEntry = entryEnum.nextElement();
//                Log.info(TestBasicLibertyDump.class, "extractJPAIntrospection", "ZipEntry: " + zipEntry);

                if (zipEntry.getName().endsWith("JPARuntimeInspector.txt")) {
                    // Found JPARuntimeIntrospector.txt
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    final byte[] buffer = new byte[4096];
                    try (BufferedInputStream bis = new BufferedInputStream(zip.getInputStream(zipEntry))) {
                        int bytesRead = 0;
                        while ((bytesRead = bis.read(buffer)) != -1) {
                            baos.write(buffer, 0, bytesRead);
                        }
                    }

                    Log.info(TestBasicLibertyDump.class, "extractJPAIntrospection", "Returning Data (" + baos.size() + " bytes)");
                    return baos.toString();
                }
            }
        }

        Log.info(TestBasicLibertyDump.class, "extractJPAIntrospection", "Failed to find JPARuntimeInspector.txt");
        return null;
    }
}
