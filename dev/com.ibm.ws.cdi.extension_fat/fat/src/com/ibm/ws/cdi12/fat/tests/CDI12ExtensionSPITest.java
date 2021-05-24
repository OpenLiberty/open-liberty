/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Test the runtime extension to function correctly
 */
@RunWith(FATRunner.class)
public class CDI12ExtensionSPITest {

    public static final String SERVER_NAME = "cdi12SPIExtensionServer";
    public static final String INSTALL_USERBUNDLE_JAVAX = "cdi.spi.extension";
    public static final String INSTALL_USERBUNDLE_JAKARTA = "cdi.spi.extension-jakarta";
    public static final String INSTALL_MISPLACED_USERBUNDLE_JAVAX = "cdi.spi.misplaced";
    public static final String INSTALL_MISPLACED_USERBUNDLE_JAKARTA = "cdi.spi.misplaced-jakarta";
    public static final String INSTALL_USERFEATURE_JAVAX = "cdi.spi.extension-1.0";
    public static final String INSTALL_USERFEATURE_JAKARTA = "cdi.spi.extension-3.0";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive ClassSPIExtension = ShrinkWrap.create(WebArchive.class, "SPIExtension.war").addPackage("com.ibm.ws.cdi.extension.spi.test.app");

        System.out.println("Intall the user feature bundle... cdi.spi.extension");
        if (RepeatTestFilter.isRepeatActionActive("EE9_FEATURES")) {
            server.installUserBundle(INSTALL_USERBUNDLE_JAKARTA);
            server.installUserBundle(INSTALL_MISPLACED_USERBUNDLE_JAKARTA);
            server.installUserFeature(INSTALL_USERFEATURE_JAKARTA);
        } else {
            server.installUserBundle(INSTALL_USERBUNDLE_JAVAX);
            server.installUserBundle(INSTALL_MISPLACED_USERBUNDLE_JAVAX);
            server.installUserFeature(INSTALL_USERFEATURE_JAVAX);
        }
        ShrinkHelper.exportDropinAppToServer(server, ClassSPIExtension);
        server.startServer(true);
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application SPIExtension started");
    }

    @Test
    public void testExtensionSPI() throws Exception {
        HttpUtils.findStringInUrl(server, "/SPIExtension/",
                       new String[] { "Injection from a producer registered in a CDI extension that was registered through the SPI"
                                      , "Injection of a normal scoped class that was registered via getBeanClasses"
                                      , "An Interceptor registered via getBeanClasses in the SPI intercepted a normal scoped class registered via getBeanClasses"
                                      , "Could not find unregistered bean"
                                      , "An Interceptor registered via getBeanClasses in the SPI intercepted a normal scoped class in the application WAR"
                                      , "A Bean with an annotation registered via getBeanDefiningAnnotationClasses was successfully injected into a different bean with an annotation registered via getBeanDefiningAnnotationClasses"  });
    }

    @Test
    public void testExtensionSPIInDifferentBundle() throws Exception {
        HttpUtils.findStringInUrl(server, "/SPIExtension/misplaced",
                       new String[] { "Injection of a normal scoped class that was registered via getBeanClasses"
                                      , "An Interceptor registered via getBeanClasses in the SPI intercepted a normal scoped class registered via getBeanClasses"
                                      , "Could not find bean registered via an extension when both the bean and the extension are in a different bundle to the SPI impl class"
                                      , "An Interceptor registered via getBeanClasses in the SPI intercepted a normal scoped class in the application WAR"
                                      , "A Bean with an annotation registered via getBeanDefiningAnnotationClasses was successfully injected into a different bean with an annotation registered via getBeanDefiningAnnotationClasses"  });
    }

    @Test
    public void testExtensionSPICrossWiredBundle() throws Exception {
        HttpUtils.findStringInUrl(server, "/SPIExtension/CrossWire",
                       new String[] { "A bean created by an annotation defined by the SPI in a different bundle, injected into a bean created by an annotation defined by the spi in the same bundle, intercepted by two interceptors defined by the SPI one from each bundle"
                                      , "WELL PLACED INTERCEPTOR"
                                      , "MISSPLACED INTERCEPTOR"});
    }

    @AfterClass
    public static void cleanup() throws Exception {
        final String METHOD_NAME = "cleanup";
        Log.info(CDI12ExtensionTest.class, METHOD_NAME, "Stopping the server.");
        if (server.isStarted()) {
            server.stopServer();
        }
        Log.info(CDI12ExtensionTest.class, METHOD_NAME, "Removing cdi extension test user feature files.");
        if (RepeatTestFilter.isRepeatActionActive("EE9_FEATURES")) {
            server.uninstallUserBundle(INSTALL_USERBUNDLE_JAKARTA);
            server.uninstallUserFeature(INSTALL_USERFEATURE_JAKARTA);
        } else {
            server.uninstallUserBundle(INSTALL_USERBUNDLE_JAVAX);
            server.uninstallUserFeature(INSTALL_USERFEATURE_JAVAX);
        }
    }
}
