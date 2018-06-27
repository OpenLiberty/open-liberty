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

public class ImplicitBeanArchivesDisabledTest extends LoggingTest {

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12DisableImplicitBeanArchiveServer");

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {

       JavaArchive implicitBeanArchiveDisabledJar = ShrinkWrap.create(JavaArchive.class,"implicitBeanArchiveDisabled.jar")
                        .addClass("com.ibm.ws.cdi.implicit.bean.disabled.MyPlane");

       WebArchive implicitBeanArchiveDisabled = ShrinkWrap.create(WebArchive.class, "implicitBeanArchiveDisabled.war")
                        .addClass("com.ibm.ws.cdi12.implicit.archive.disabled.MyCar")
                        .addClass("com.ibm.ws.cdi12.implicit.archive.disabled.MyCarServlet")
                        .add(new FileAsset(new File("test-applications/implicitBeanArchiveDisabled.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(implicitBeanArchiveDisabledJar);
      
       JavaArchive explicitBeanArchive = ShrinkWrap.create(JavaArchive.class,"explicitBeanArchive.jar")
                        .addClass("com.ibm.ws.cdi.explicit.bean.MyBike")
                        .add(new FileAsset(new File("test-applications/explicitBeanArchive.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

       return ShrinkWrap.create(EnterpriseArchive.class,"implicitBeanArchiveDisabled.ear")
                        .add(new FileAsset(new File("test-applications/implicitBeanArchiveDisabled.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsLibrary(explicitBeanArchive)
                        .addAsModule(implicitBeanArchiveDisabled);
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
    public void testDisableImplicitBeanArchives() throws Exception {
        //part of multiModuleApp1
        this.verifyResponse(
                            "/implicitBeanArchiveDisabled/",
                            "Car Bike No Plane!");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SHARED_SERVER != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            /*
             * Ignore following warning as it is expected: CWOWB1009W: Implicit bean archives are disabled.
             */
            SHARED_SERVER.getLibertyServer().stopServer("CWOWB1009W");
        }
    }

}
