/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
@SkipForRepeat({ SkipForRepeat.EE9_FEATURES }) // Skipped temporarily to test PassivationBeanTests for sessionDatabase-1.0 feature
public class ObservesInitializedTest extends LoggingTest {
    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12BasicServer");

    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BuildShrinkWrap
    public static Archive<?> buildShrinkWrap() {
        JavaArchive observesInitializedInJarsWebInfJar = ShrinkWrap.create(JavaArchive.class, "ObservesInitializedInJarsWebInfJar.jar");
        observesInitializedInJarsWebInfJar.addClass("cdi12.observersinjars.webinf.WebInfAutostartObserver");
        observesInitializedInJarsWebInfJar.addClass("cdi12.observersinjars.webinf.SomeClass");

        JavaArchive observesInitializedInJarsManifestJar = ShrinkWrap.create(JavaArchive.class, "ObservesInitializedInJarsManifestJar.jar");
        observesInitializedInJarsManifestJar.addClass("cdi12.observersinjars.manifestjar.ManifestAutostartObserver");
        observesInitializedInJarsManifestJar.addClass("cdi12.observersinjars.manifestjar.SomeClass");

        WebArchive observesInitializedInJars = ShrinkWrap.create(WebArchive.class, "ObservesInitializedInJars.war");
        observesInitializedInJars.addClass("cdi12.observersinjarsbeforebean.WarBeforeBeansObserver");
        observesInitializedInJars.addClass("cdi12.observersinjars.SomeClass");
        observesInitializedInJars.addClass("cdi12.observersinjars.TestServlet");
        observesInitializedInJars.addClass("cdi12.observersinjars.WarAutostartObserver");
        observesInitializedInJars.addAsManifestResource(new File("test-applications/ObservesInitializedInJars.war/resources/META-INF/MANIFEST.MF"));

        observesInitializedInJars.add(new FileAsset(new File("test-applications/ObservesInitializedInJars.war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")),
                                      "/META-INF/services/javax.enterprise.inject.spi.Extension");
        observesInitializedInJars.addAsLibrary(observesInitializedInJarsWebInfJar);
        WebArchive observesInitializedInJarsSecondWar = ShrinkWrap.create(WebArchive.class, "ObservesInitializedInJarsSecondWar.war");
        observesInitializedInJarsSecondWar.addClass("cdi12.observersinjarssecondwar.WarBeforeBeansObserver");
        observesInitializedInJarsSecondWar.addClass("cdi12.observersinjarssecondwar.SomeClass");
        observesInitializedInJarsSecondWar.addClass("cdi12.observersinjarssecondwar.TestServlet");

        observesInitializedInJarsSecondWar.add(new FileAsset(new File("test-applications/ObservesInitializedInJarsSecondWar.war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")),
                                               "/META-INF/services/javax.enterprise.inject.spi.Extension");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ObservesInitializedInJars.ear");
        ear.add(new FileAsset(new File("test-applications/ObservesInitializedInJars.ear/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml");
        ear.add(new FileAsset(new File("test-applications/ObservesInitializedInJars.ear/resources/META-INF/application.xml")), "/META-INF/application.xml");
        ear.addAsModule(observesInitializedInJars);
        ear.addAsModule(observesInitializedInJarsSecondWar);
        ear.addAsModule(observesInitializedInJarsManifestJar);
        return ear;
    }

    @Test
    @Mode(TestMode.FULL)
    public void testObservesInitializedInJars() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();

        SHARED_SERVER.verifyResponse(browser, "/ObservesInitializedInJars/TestServlet", new String[] { "web-inf jar saw initilization: true",
                                                                                                       "manifest jar saw initilization: true" });
    }

}
