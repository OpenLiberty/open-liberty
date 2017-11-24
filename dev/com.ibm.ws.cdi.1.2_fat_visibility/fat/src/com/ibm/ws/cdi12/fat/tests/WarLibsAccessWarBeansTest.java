/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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

public class WarLibsAccessWarBeansTest extends LoggingTest {

    @ClassRule
    // Create the server.
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12WarLibsAccessWarServer");

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
    public static Archive buildShrinkWrap() {

        JavaArchive warLibAccessBeansInWarLibJar = ShrinkWrap.create(JavaArchive.class,"warLibAccessBeansInWarJar.jar")
                        .addClass("com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar.TestInjectionClass")
                        .addClass("com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar.WarBeanInterface")
                        .add(new FileAsset(new File("test-applications/warLibAccessBeansInWarJar.jar/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        JavaArchive warLibAccessBeansInWarJar = ShrinkWrap.create(JavaArchive.class,"warLibAccessBeansInWar2.jar")
                        .addClass("com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar2.TestInjectionClass2")
                        .addClass("com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar2.WarBeanInterface2");

        WebArchive warLibAccessBeansInWar = ShrinkWrap.create(WebArchive.class, "warLibAccessBeansInWar.war")
                        .addAsManifestResource(new File("test-applications/warLibAccessBeansInWar.war/resources/META-INF/MANIFEST.MF"))
                        .addClass("com.ibm.ws.cdi12.test.warLibAccessBeansInWar.TestServlet")
                        .addClass("com.ibm.ws.cdi12.test.warLibAccessBeansInWar.WarBean")
                        .add(new FileAsset(new File("test-applications/warLibAccessBeansInWar.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(warLibAccessBeansInWarLibJar);

        return ShrinkWrap.create(EnterpriseArchive.class,"warLibAccessBeansInWar.ear")
                        .add(new FileAsset(new File("test-applications/warLibAccessBeansInWar.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(warLibAccessBeansInWar)
                        .addAsModule(warLibAccessBeansInWarJar);     
    }

    @Test
    public void testWarLibsCanAccessBeansInWar() throws Exception {
        this.verifyResponse("/warLibAccessBeansInWar/TestServlet", "TestInjectionClass: WarBean TestInjectionClass2: WarBean");
    }

}
