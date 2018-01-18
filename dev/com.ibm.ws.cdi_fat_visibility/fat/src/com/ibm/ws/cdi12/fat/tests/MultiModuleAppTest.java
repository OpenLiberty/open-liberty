/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import java.io.File;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

/**
 * All CDI tests with all applicable server features enabled.
 */
public class MultiModuleAppTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12MultiModuleServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BuildShrinkWrap
    public static Archive[] buildShrinkWrap() {

        JavaArchive multiModuleAppLib3 = ShrinkWrap.create(JavaArchive.class,"multiModuleAppLib3.jar")
                        .addClass("com.ibm.ws.cdi12.test.lib3.BasicBean3A")
                        .addClass("com.ibm.ws.cdi12.test.lib3.BasicBean3")
                        .addClass("com.ibm.ws.cdi12.test.lib3.CustomNormalScoped");

        JavaArchive multiModuleAppLib2 = ShrinkWrap.create(JavaArchive.class,"multiModuleAppLib2.jar")
                        .addClass("com.ibm.ws.cdi12.test.lib2.BasicBean2")
                        .add(new FileAsset(new File("test-applications/multiModuleAppLib2.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

        WebArchive multiModuleAppWeb1 = ShrinkWrap.create(WebArchive.class, "multiModuleAppWeb1.war")
                        .addClass("com.ibm.ws.cdi12.test.web1.Web1Servlet")
                        .add(new FileAsset(new File("test-applications/multiModuleAppWeb1.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        JavaArchive multiModuleAppLib1 = ShrinkWrap.create(JavaArchive.class,"multiModuleAppLib1.jar")
                        .addClass("com.ibm.ws.cdi12.test.lib1.BasicBean1")
                        .addClass("com.ibm.ws.cdi12.test.lib1.BasicBean1A")
                        .add(new FileAsset(new File("test-applications/multiModuleAppLib1.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");


        WebArchive multiModuleAppWeb2 = ShrinkWrap.create(WebArchive.class, "multiModuleAppWeb2.war")
                        .addClass("com.ibm.ws.cdi12.test.web2.Web2Servlet")
                        .add(new FileAsset(new File("test-applications/multiModuleAppWeb2.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsManifestResource(new File("test-applications/multiModuleAppWeb2.war/resources/META-INF/MANIFEST.MF"))
                        .addAsLibrary(multiModuleAppLib2)
                        .addAsLibrary(multiModuleAppLib3);

        EnterpriseArchive multiModuleAppOne = ShrinkWrap.create(EnterpriseArchive.class,"multiModuleApp1.ear")
                        .add(new FileAsset(new File("test-applications/multiModuleApp1.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsLibrary(multiModuleAppLib1)
                        .addAsModule(multiModuleAppWeb1)
                        .addAsModule(multiModuleAppWeb2);

        EnterpriseArchive multiModuleAppTwo = ShrinkWrap.create(EnterpriseArchive.class,"multiModuleApp2.ear")
                        .add(new FileAsset(new File("test-applications/multiModuleApp2.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsLibrary(multiModuleAppLib1)
                        .addAsLibrary(multiModuleAppLib3)
                        .addAsModule(multiModuleAppWeb1)
                        .addAsModule(multiModuleAppWeb2);

        Archive[] archives = new Archive[2];
        archives[0] = multiModuleAppOne;
        archives[1] = multiModuleAppTwo;
        return archives;
    }

    @Test
    public void testMultiModuleApps() throws Exception {
        //part of multiModuleApp1
        this.verifyResponse(
                            "/multiModuleAppWeb1/",
                            "Test Sucessful!");
        //part of multiModuleApp1
        this.verifyResponse(
                            "/multiModuleAppWeb2/",
                            "Test Sucessful!");
        //part of multiModuleApp2
        this.verifyResponse(
                            "/multiModuleAppWeb3/",
                            "Test Sucessful!");
        //part of multiModuleApp2
        this.verifyResponse(
                            "/multiModuleAppWeb4/",
                            "Test Sucessful!");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SHARED_SERVER != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            SHARED_SERVER.getLibertyServer().stopServer("SRVE9967W");
        }
    }
}
