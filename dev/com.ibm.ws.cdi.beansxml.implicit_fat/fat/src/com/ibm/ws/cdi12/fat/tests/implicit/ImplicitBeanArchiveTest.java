/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests.implicit;
 
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
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
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test that library jars inside a war can be implicit bean archives.
 */
@RunWith(FATRunner.class)
public class ImplicitBeanArchiveTest extends FATServletClient {

    @Server("cdi12ImplicitServer")
    @TestServlets({
                    @TestServlet(servlet = com.ibm.ws.cdi12.test.implicit.servlet.Web1Servlet.class, contextRoot = "/") 
    })
    public static LibertyServer server;

    @BeforeClass
    public static void buildShrinkWrap() throws Exception {

       JavaArchive archiveWithBeansXML = ShrinkWrap.create(JavaArchive.class,"archiveWithBeansXML.jar")
                        .addClass("com.ibm.ws.cdi12.test.beansXML.UnannotatedBeanInAllModeBeanArchive")
                        .add(new FileAsset(new File("test-applications/archiveWithBeansXML.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

       JavaArchive archiveWithNoScanBeansXML = ShrinkWrap.create(JavaArchive.class,"archiveWithNoScanBeansXML.jar")
                        .addClass("com.ibm.ws.cdi12.test.implicit.noscan.RequestScopedButNoScan")
                        .add(new FileAsset(new File("test-applications/archiveWithNoScanBeansXML.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

       JavaArchive archiveWithNoImplicitBeans = ShrinkWrap.create(JavaArchive.class,"archiveWithNoImplicitBeans.jar")
                        .addClass("com.ibm.ws.cdi12.test.implicit.nobeans.ClassWithInjectButNotABean");

       JavaArchive archiveWithImplicitBeans = ShrinkWrap.create(JavaArchive.class,"archiveWithImplicitBeans.jar")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.StereotypedBean")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.MyExtendedScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.MyStereotype")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.SessionScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.ConversationScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.UnannotatedBeanInImplicitBeanArchive")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.MyExtendedNormalScoped")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.RequestScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.ApplicationScopedBean");

       JavaArchive archiveWithAnnotatedModeBeansXML = ShrinkWrap.create(JavaArchive.class,"archiveWithAnnotatedModeBeansXML.jar")
                        .addClass("com.ibm.ws.cdi12.test.annotatedBeansXML.DependentScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.annotatedBeansXML.UnannotatedClassInAnnotatedModeBeanArchive")
                        .add(new FileAsset(new File("test-applications/archiveWithAnnotatedModeBeansXML.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

       WebArchive implicitBeanArchive = ShrinkWrap.create(WebArchive.class, "implicitBeanArchive.war")
                        .addClass("com.ibm.ws.cdi12.test.implicit.servlet.Web1Servlet")
                        .add(new FileAsset(new File("test-applications/implicitBeanArchive.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(archiveWithBeansXML)
                        .addAsLibrary(archiveWithImplicitBeans)
                        .addAsLibrary(archiveWithNoImplicitBeans)
                        .addAsLibrary(archiveWithNoScanBeansXML)
                        .addAsLibrary(archiveWithAnnotatedModeBeansXML);

       ShrinkHelper.exportDropinAppToServer(server, implicitBeanArchive);
       server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
