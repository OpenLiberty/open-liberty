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
package com.ibm.ws.cdi12.fat.tests;

import java.io.File;

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

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * These tests use a runtime feature to destroy a contextual object.
 * The test passes if that object exists before destroy is called
 * and is null afterwards
 */

@Mode(TestMode.FULL)
public class AlterableContextTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12AlterableContextServer");

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {

          JavaArchive alterableContextExtension = ShrinkWrap.create(JavaArchive.class,"alterableContextExtension.jar")
                        .addClass("com.ibm.ws.cdi12.alterablecontext.test.extension.DirtySingleton")
                        .addClass("com.ibm.ws.cdi12.alterablecontext.test.extension.AlterableContextBean")
                        .addClass("com.ibm.ws.cdi12.alterablecontext.test.extension.AlterableContextExtension")
                        .add(new FileAsset(new File("test-applications/alterableContextExtension.jar/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension")
                        .add(new FileAsset(new File("test-applications/alterableContextExtension.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
         
          WebArchive alterableContextApp = ShrinkWrap.create(WebArchive.class, "alterableContextApp.war")
                        .addClass("com.ibm.ws.cdi12.alterablecontext.test.AlterableContextTestServlet")
                        .add(new FileAsset(new File("test-applications/alterableContextApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(alterableContextExtension);

          return ShrinkWrap.create(EnterpriseArchive.class,"alterableContextsApp.ear")
                        .add(new FileAsset(new File("test-applications/alterableContextsApp.ear/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml")
                        .add(new FileAsset(new File("test-applications/alterableContextsApp.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(alterableContextApp);
    }

    /** {@inheritDoc} */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        // TODO Auto-generated method stub
        return SHARED_SERVER;
    }

    @Test
    public void testBeanWasFound() throws Exception {
        this.verifyResponse("/alterableContextApp/", "I got this from my alterablecontext: com.ibm.ws.cdi12.alterablecontext.test.extension.AlterableContextBean");
    }

    @Test
    public void testBeanWasDestroyed() throws Exception {
        this.verifyResponse("/alterableContextApp/", "Now the command returns: null");
    }

}
