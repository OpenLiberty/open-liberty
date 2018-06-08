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

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import componenttest.custom.junit.runner.Mode;

public class ImplicitBeanArchiveNoAnnotationsTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12EjbDefInXmlServer");

    @BuildShrinkWrap
    public static Archive[] buildShrinkWrap() {
       Archive[] apps = new Archive[3];

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


       apps[0] = ShrinkWrap.create(WebArchive.class, "archiveWithNoBeansXml.war")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.FirstManagedBeanInterface")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.EjbImpl")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.SimpleServlet")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.ManagedSimpleBean")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.OtherManagedSimpleBean")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.SecondManagedBeanInterface")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.ConstructorInjectionServlet")
                        .add(new FileAsset(new File("test-applications/archiveWithNoBeansXML.war/resources/WEB-INF/ejb-jar.xml")), "/WEB-INF/ejb-jar.xml");

       apps[1] = ShrinkWrap.create(EnterpriseArchive.class,"ejbArchiveWithNoAnnotations.ear")
                        .add(new FileAsset(new File("test-applications/ejbArchiveWithNoAnnotations.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(ejbArchiveWithNoAnnotations1)
                        .addAsModule(ejbArchiveWithNoAnnotations2);

       apps[2] = ShrinkWrap.create(EnterpriseArchive.class,"ejbJarInWarNoAnnotations.ear")
                        .add(new FileAsset(new File("test-applications/ejbJarInWarNoAnnotations.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(ejbJarInWarNoAnnotations);

       return apps;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testMultipleNamedEJBsInWar() throws Exception {
        this.verifyResponse("/archiveWithNoBeansXml/SimpleServlet", "PASSED");
    }

    @Mode
    @Test
    public void testConstructorInjection() throws Exception {
        this.verifyResponse("/archiveWithNoBeansXml/ConstructorInjectionServlet", "SUCCESSFUL");
    }

    @Test
    public void testMultipleNamesEjbsInEar() throws Exception {
        this.verifyResponse("/ejbArchiveWithNoAnnotations/ejbServlet", "PASSED");
    }

    @Test
    public void testEjbJarInWar() throws Exception {
        this.verifyResponse("/ejbJarInWarNoAnnotations/ejbServlet", "PASSED");
    }
}
