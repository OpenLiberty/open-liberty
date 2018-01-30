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

import org.junit.AfterClass;
import org.junit.BeforeClass;
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

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;


public class SimpleJSFWithSharedLibTest extends LoggingTest {

    public static LibertyServer server;

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive sharedLibrary = ShrinkWrap.create(JavaArchive.class,"sharedLibrary.jar")
                        .addClass("com.ibm.ws.cdi12.test.shared.NonInjectedHello")
                        .addClass("com.ibm.ws.cdi12.test.shared.InjectedHello");

        WebArchive simpleJSFWithSharedLib = ShrinkWrap.create(WebArchive.class, "simpleJSFWithSharedLib.war")
                        .addClass("com.ibm.ws.cdi12.test.jsf.sharelib.SimpleJsfBean")
                        .add(new FileAsset(new File("test-applications/simpleJSFWithSharedLib.war/resources/WEB-INF/faces-config.xml")), "/WEB-INF/faces-config.xml")
                        .add(new FileAsset(new File("test-applications/simpleJSFWithSharedLib.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/simpleJSFWithSharedLib.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/simpleJSFWithSharedLib.war/resources/testBasicJsf.xhtml")), "/testBasicJsf.xhtml");

        server = LibertyServerFactory.getLibertyServer("cdi12JSFWithSharedLibServer");
        ShrinkHelper.exportToServer(server, "/InjectionSharedLibrary", sharedLibrary);
        ShrinkHelper.exportToServer(server, "/apps", simpleJSFWithSharedLib);
        server.startServer();
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application simpleJSFWithSharedLib started");
    }

    @Test
    public void testSimpleJSFWithSharedLib() throws Exception {
        HttpUtils.findStringInUrl(server, "/simpleJSFWithSharedLib/faces/testBasicJsf.xhtml",
                                  "SimpleJsfBean injected with: Hello from an InjectedHello, I am here: SimpleJsfBean");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }
}
