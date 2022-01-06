/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;
import com.ibm.ws.cdi.extension.apps.appExtension.InSameWarBean;
import com.ibm.ws.cdi.extension.apps.appExtension.jar.InLibJarBean;
import com.ibm.ws.cdi.extension.apps.appExtension.jar.PlainExtension;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class AppExtensionTest extends FATServletClient {

    public static final String APP_NAME = "applicationExtension";
    public static final String SERVER_NAME = "cdi12AppExtensionServer";

    @Server(SERVER_NAME)
    @TestServlet(servlet = com.ibm.ws.cdi.extension.apps.appExtension.AppExtensionServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        JavaArchive applicationExtensionJar = ShrinkWrap.create(JavaArchive.class, APP_NAME + ".jar");
        applicationExtensionJar.addClass(PlainExtension.class);
        applicationExtensionJar.addClass(InLibJarBean.class);

        CDIArchiveHelper.addCDIExtensionFile(applicationExtensionJar, PlainExtension.class.getPackage());
        CDIArchiveHelper.addBeansXML(applicationExtensionJar, DiscoveryMode.ANNOTATED);

        WebArchive applicationExtension = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        applicationExtension.addClass(com.ibm.ws.cdi.extension.apps.appExtension.AppExtensionServlet.class);
        applicationExtension.addClass(InSameWarBean.class);
        applicationExtension.addAsLibrary(applicationExtensionJar);

        ShrinkHelper.exportDropinAppToServer(server, applicationExtension, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @Test
    public void testAppExtensionLoaded() throws Exception {
        Assert.assertFalse("Test for before bean discovery event",
                           server.findStringsInLogs("PlainExtension: beginning the scanning process").isEmpty());
        Assert.assertFalse("Test for processing annotation type event",
                           server.findStringsInLogs("PlainExtension: scanning type->").isEmpty());
        Assert.assertFalse("Test for after bean discovery event",
                           server.findStringsInLogs("PlainExtension: finished the scanning process").isEmpty());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }

}
