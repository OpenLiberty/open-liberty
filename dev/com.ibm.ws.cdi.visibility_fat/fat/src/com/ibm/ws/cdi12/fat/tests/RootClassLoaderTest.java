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
package com.ibm.ws.cdi12.fat.tests;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

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
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.custom.junit.runner.Mode;

/**
 * Scope tests for EJBs
 */
@Mode(FULL)
public class RootClassLoaderTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12BasicServer");

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {

       JavaArchive rootClassLoaderExtension = ShrinkWrap.create(JavaArchive.class,"rootClassLoaderExtension.jar")
                        .addClass("com.ibm.ws.cdi12.test.rootClassLoader.extension.RandomBean")
                        .addClass("com.ibm.ws.cdi12.test.rootClassLoader.extension.OSName")
                        .addClass("com.ibm.ws.cdi12.test.rootClassLoader.extension.OSNameBean")
                        .addClass("com.ibm.ws.cdi12.test.rootClassLoader.extension.DefaultLiteral")
                        .addClass("com.ibm.ws.cdi12.test.rootClassLoader.extension.MyExtension")
                        .addClass("com.ibm.ws.cdi12.test.rootClassLoader.extension.TimerBean")
                        .addClass("com.ibm.ws.cdi12.test.rootClassLoader.extension.OSNameLiteral")
                        .add(new FileAsset(new File("test-applications/rootClassLoaderExtension.jar/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension");

       return ShrinkWrap.create(WebArchive.class, "rootClassLoaderApp.war")
                        .add(new FileAsset(new File("test-applications/rootClassLoaderApp.war/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml")
                        .addClass("com.ibm.ws.cdi12.test.rootClassLoader.web.RootClassLoaderServlet")
                        .addAsLibrary(rootClassLoaderExtension);
    }

    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    /**
     * Test that bean classes which are loaded by the Root ClassLoader can be injected correctly
     *
     * @throws Exception
     */
    @Test
    public void testRootClassLoader() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();

        SHARED_SERVER.verifyResponse(browser, "/rootClassLoaderApp/", "done");
    }

}
