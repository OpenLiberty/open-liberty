/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests.implicit;
 
import java.io.File;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class ImplicitBeanArchiveNoAnnotationsTest {

    @Server("cdi12EjbDefInXmlServer")
    public static LibertyServer server;

    @BeforeClass
    public static void buildShrinkWrap() throws Exception {

       WebArchive ejbArchiveWithNoAnnotations1 = ShrinkWrap.create(WebArchive.class, "ejbArchiveWithNoAnnotations.war")
                        .addClass("com.ibm.ws.cdi12.test.EjbServlet");

       JavaArchive ejbArchiveWithNoAnnotations2 = ShrinkWrap.create(JavaArchive.class,"ejbArchiveWithNoAnnotations.jar")
                        .addClass("com.ibm.ws.cdi12.test.SimpleEjbBean")
                        .addClass("com.ibm.ws.cdi12.test.SimpleEjbBean2")
                        .add(new FileAsset(new File("test-applications/ejbArchiveWithNoAnnotations.jar/resources/META-INF/ejb-jar.xml")), "/META-INF/ejb-jar.xml");

       JavaArchive ejbJarInWarNoAnnotationsJar = ShrinkWrap.create(JavaArchive.class,"ejbJarInWarNoAnnotations.jar")
                        .addClass("com.ibm.ws.cdi12.test.ejbJarInWarNoAnnotations.SimpleEjbBean")
                        .addClass("com.ibm.ws.cdi12.test.ejbJarInWarNoAnnotations.SimpleEjbBean2");

       WebArchive ejbJarInWarNoAnnotations = ShrinkWrap.create(WebArchive.class, "ejbJarInWarNoAnnotations.war")
                        .addClass("com.ibm.ws.cdi12.test.ejbJarInWarNoAnnotations.EjbServlet")
                        .add(new FileAsset(new File("test-applications/ejbJarInWarNoAnnotations.war/resources/WEB-INF/ejb-jar.xml")), "/WEB-INF/ejb-jar.xml")
                        .addAsLibrary(ejbJarInWarNoAnnotationsJar);


       WebArchive archiveWithNoBeansXml = ShrinkWrap.create(WebArchive.class, "archiveWithNoBeansXml.war")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.FirstManagedBeanInterface")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.EjbImpl")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.SimpleServlet")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.ManagedSimpleBean")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.OtherManagedSimpleBean")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.SecondManagedBeanInterface")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.ConstructorInjectionServlet")
                        .add(new FileAsset(new File("test-applications/archiveWithNoBeansXML.war/resources/WEB-INF/ejb-jar.xml")), "/WEB-INF/ejb-jar.xml");

       EnterpriseArchive ejbArchiveWithNoAnnotations = ShrinkWrap.create(EnterpriseArchive.class,"ejbArchiveWithNoAnnotations.ear")
                        .add(new FileAsset(new File("test-applications/ejbArchiveWithNoAnnotations.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(ejbArchiveWithNoAnnotations1)
                        .addAsModule(ejbArchiveWithNoAnnotations2);

       EnterpriseArchive ejbJarInWarNoAnnotationsEar = ShrinkWrap.create(EnterpriseArchive.class,"ejbJarInWarNoAnnotations.ear")
                        .add(new FileAsset(new File("test-applications/ejbJarInWarNoAnnotations.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(ejbJarInWarNoAnnotations);

       ShrinkHelper.exportDropinAppToServer(server, archiveWithNoBeansXml);
       ShrinkHelper.exportDropinAppToServer(server, ejbArchiveWithNoAnnotations);
       ShrinkHelper.exportDropinAppToServer(server, ejbJarInWarNoAnnotationsEar);
       server.startServer();
    }

    @Test
    public void testMultipleNamedEJBsInWar() throws Exception {
        HttpUtils.findStringInUrl(server, "/archiveWithNoBeansXml/SimpleServlet", "PASSED");
    }

    @Mode
    @Test
    public void testConstructorInjection() throws Exception {
        HttpUtils.findStringInUrl(server, "/archiveWithNoBeansXml/ConstructorInjectionServlet", "SUCCESSFUL");
    }

    @Test
    public void testMultipleNamesEjbsInEar() throws Exception {
        HttpUtils.findStringInUrl(server, "/ejbArchiveWithNoAnnotations/ejbServlet", "PASSED");
    }

    @Test
    public void testEjbJarInWar() throws Exception {
        HttpUtils.findStringInUrl(server, "/ejbJarInWarNoAnnotations/ejbServlet", "PASSED");
    }
}
