/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

public class ObservesInitializedTest extends LoggingTest {
    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12BasicServer");

    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {
       JavaArchive ObservesInitializedInJarsWebInfJar = ShrinkWrap.create(JavaArchive.class,"ObservesInitializedInJarsWebInfJar.jar")
                        .addClass("cdi12.observersinjars.webinf.WebInfAutostartObserver")
                        .addClass("cdi12.observersinjars.webinf.SomeClass");

       JavaArchive ObservesInitializedInJarsManifestJar = ShrinkWrap.create(JavaArchive.class,"ObservesInitializedInJarsManifestJar.jar")
                        .addClass("cdi12.observersinjars.manifestjar.ManifestAutostartObserver")
                        .addClass("cdi12.observersinjars.manifestjar.SomeClass");

       WebArchive ObservesInitializedInJars = ShrinkWrap.create(WebArchive.class, "ObservesInitializedInJars.war")
                        .addClass("cdi12.observersinjarsbeforebean.WarBeforeBeansObserver")
                        .addClass("cdi12.observersinjars.SomeClass")
                        .addClass("cdi12.observersinjars.TestServlet")
                        .addClass("cdi12.observersinjars.WarAutostartObserver")
                        .addAsManifestResource(new File("test-applications/ObservesInitializedInJars.war/resources/META-INF/MANIFEST.MF"))
                        .add(new FileAsset(new File("test-applications/ObservesInitializedInJars.war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension")
                        .addAsLibrary(ObservesInitializedInJarsWebInfJar);
       WebArchive ObservesInitializedInJarsSecondWar = ShrinkWrap.create(WebArchive.class, "ObservesInitializedInJarsSecondWar.war")
                        .addClass("cdi12.observersinjarssecondwar.WarBeforeBeansObserver")
                        .addClass("cdi12.observersinjarssecondwar.SomeClass")
                        .addClass("cdi12.observersinjarssecondwar.TestServlet")
                        .add(new FileAsset(new File("test-applications/ObservesInitializedInJarsSecondWar.war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension");

       return ShrinkWrap.create(EnterpriseArchive.class,"ObservesInitializedInJars.ear")
                        .add(new FileAsset(new File("test-applications/ObservesInitializedInJars.ear/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml")
                        .add(new FileAsset(new File("test-applications/ObservesInitializedInJars.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(ObservesInitializedInJars)
                        .addAsModule(ObservesInitializedInJarsSecondWar)
                        .addAsModule(ObservesInitializedInJarsManifestJar);
    }


    @Test
    @Mode(TestMode.FULL)
    public void testObservesInitializedInJars() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();

        SHARED_SERVER.verifyResponse(browser, "/ObservesInitializedInJars/TestServlet", new String[] { "web-inf jar saw initilization: true",
                                                                                                       "manifest jar saw initilization: true" });
    }

}
