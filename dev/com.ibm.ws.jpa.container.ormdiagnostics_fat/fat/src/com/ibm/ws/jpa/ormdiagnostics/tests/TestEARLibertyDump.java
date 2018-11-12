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

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
import servlet.GuestBookServlet;

@RunWith(FATRunner.class)
public class TestEARLibertyDump extends FATServletClient {
    private static final Logger LOG = Logger.getLogger(TestEARLibertyDump.class.getName());

    public static final String APP_NAME = "GuestBookAPP";
    public static final String SERVLET = "GuestBookServlet";

    @Server("GuestBookServer")
    @TestServlets({
                    @TestServlet(servlet = GuestBookServlet.class, path = APP_NAME + "/" + SERVLET),
    })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1,
                                        "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect\";",
                                        "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind\";");

        LOG.info("Setup : Creating JARs");
        List<JavaArchive> jars = Arrays.asList(ORMApplicationBuilder.createJAR("GuestBookEJB.jar", "ejb"),
                                               ORMApplicationBuilder.createJAR("GuestBookJPA.jar", "jpa"));
        LOG.info("Setup : Creating LIB");
        List<JavaArchive> libs = Arrays.asList(ORMApplicationBuilder.createJAR("GuestBookJPA.jar", "jpa"));
        LOG.info("Setup : Creating WAR");
        List<WebArchive> wars = Arrays.asList(ORMApplicationBuilder.createWAR("GuestBookAPP.war", jars, "servlet"));

        LOG.info("Setup : Creating EAR");
        EnterpriseArchive ear = ORMApplicationBuilder.createEAR("GuestBookEAR.ear", wars, libs);
        ORMApplicationBuilder.addArchivetoServer(server1, "dropins", ear);

        LOG.info("Setup : Starting Server");
        server1.startServer();

        server1.addInstalledAppForValidation("GuestBookEAR");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer("CWWJP9991W");
    }

    @Test
    public void testBasicDump() throws Exception {

        final LocalFile lf = server1.dumpServer("jpa_testBasicDump");
        Assert.assertNotNull(lf);

        final String introspectorData = ORMIntrospectorHelper.extractJPAIntrospection(lf);
        Assert.assertNotNull(introspectorData);

        // Test for some easy and consistent strings that are expected to appear in the introspector text

        Assert.assertTrue(introspectorData.contains("JPA Runtime Internal State Information")); // Description
        Assert.assertTrue(introspectorData.contains("jpaRuntime = com.ibm.ws.jpa.container.v21.internal.JPA21Runtime"));
        Assert.assertTrue(introspectorData.contains("Provider Runtime Integration Service = com.ibm.ws.jpa.container.eclipselink.EclipseLinkJPAProvider"));

        ORMIntrospectorHelper.verifyApplications("GuestBookEAR", 0, 4,
                                                 new String[] { "GuestBookJPA.jar",
                                                                "GuestBookAPP.war/WEB-INF/lib/GuestBookJPA.jar" },
                                                 introspectorData);

        ORMIntrospectorHelper.verifyPersistenceUnit("SimpleApplicationPU", introspectorData);

        List<JPAClass> classes = new Vector<JPAClass>();
        final String entity1 = "package jpa;\n" +
                               "\n" +
                               "@javax.persistence.Entity\n" +
                               "@javax.persistence.IdClass(value=<<Object <jpa.GuestId> id=0 >>)\n" +
                               "@javax.persistence.NamedQueries(value={   @javax.persistence.NamedQuery(name=\"findAllGuests\", query=\"SELECT g FROM GuestEntity g\")\n" +
                               ",   @javax.persistence.NamedQuery(name=\"findByFirstName\", query=\"SELECT g FROM GuestEntity g WHERE g.firstName = :firstname\")\n" +
                               ",   @javax.persistence.NamedQuery(name=\"findByLastName\", query=\"SELECT g FROM GuestEntity g WHERE g.lastName = :lastname\")\n" +
                               "} )\n" +
                               "public class GuestEntity {\n" +
                               "  // Fields\n" +
                               "  @javax.persistence.Id\n" +
                               "  private java.lang.String firstName;\n" +
                               "\n" +
                               "  @javax.persistence.Id\n" +
                               "  private java.lang.String lastName;\n" +
                               "\n" +
                               "  @javax.persistence.Basic\n" +
                               "  private java.time.LocalDateTime localDateTime;\n" +
                               "\n" +
                               "  // Methods\n" +
                               "  public GuestEntity();\n" +
                               "\n" +
                               "  public java.lang.String getFirstName();\n" +
                               "\n" +
                               "  public void setFirstName(java.lang.String);\n" +
                               "\n" +
                               "  public java.lang.String getLastName();\n" +
                               "\n" +
                               "  public void setLastName(java.lang.String);\n" +
                               "\n" +
                               "  public java.time.LocalDateTime getLocalDateTime();\n" +
                               "\n" +
                               "  public void setLocalDateTime(java.time.LocalDateTime);\n" +
                               "\n" +
                               "}";
        classes.add(new JPAClass("jpa.GuestEntity", "lib/GuestBookJPA.jar", entity1));
        classes.add(new JPAClass("jpa.GuestEntity", "GuestBookAPP.war/WEB-INF/lib/GuestBookJPA.jar", entity1));

        final String entity2 = "package jpa;\n" +
                               "\n" +
                               "public class GuestId implements java.io.Serializable {\n" +
                               "  // Fields\n" +
                               "  final private static long serialVersionUID;\n" +
                               "\n" +
                               "  private java.lang.String firstName;\n" +
                               "\n" +
                               "  private java.lang.String lastName;\n" +
                               "\n" +
                               "  // Methods\n" +
                               "  public GuestId();\n" +
                               "\n" +
                               "  public java.lang.String getFirstName();\n" +
                               "\n" +
                               "  public void setFirstName(java.lang.String);\n" +
                               "\n" +
                               "  public java.lang.String getLastName();\n" +
                               "\n" +
                               "  public void setLastName(java.lang.String);\n" +
                               "\n" +
                               "  public int hashCode();\n" +
                               "\n" +
                               "  public boolean equals(java.lang.Object);\n" +
                               "\n" +
                               "}";
        classes.add(new JPAClass("jpa.GuestId", "GuestBookJPA.jar!/", entity2));
        classes.add(new JPAClass("jpa.GuestId", "GuestBookAPP.war/WEB-INF/lib/GuestBookJPA.jar!/", entity2));

        ORMIntrospectorHelper.verifyPersistentClasses(classes, introspectorData);
    }
}
