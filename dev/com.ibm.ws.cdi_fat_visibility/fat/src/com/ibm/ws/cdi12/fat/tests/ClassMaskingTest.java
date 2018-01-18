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


import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * This test ensures that if a class in one module is masked by a class in another module, CDI doesn't break.
 * <p>
 * The test was introduced because CDI was assuming that all classes present in a module should be loaded by that modules classloader. However, they could be loaded from another
 * module which is on the application classpath.
 * <p>
 * We also test that beans in an App Client jar are not visible to other modules.
 */

public class ClassMaskingTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer server = new ShrinkWrapSharedServer("cdi12ClassMasking");

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {
       
        JavaArchive maskedClassEjb = ShrinkWrap.create(JavaArchive.class,"maskedClassEjb.jar")
                        .addClass("test.Type1")
                        .addClass("beans.SessionBean1")
                        .add(new FileAsset(new File("test-applications/maskedClassEjb.jar/file.txt")), "/file.txt");

        WebArchive maskedClassWeb = ShrinkWrap.create(WebArchive.class, "maskedClassWeb.war")
                        .addClass("test.TestBeanWarImpl")
                        .addClass("test.Type3")
                        .addClass("test.Type1")
                        .addClass("zservlet.TestServlet")
                        .add(new FileAsset(new File("test-applications/maskedClassWeb.war/file.txt")), "/file.txt");

        JavaArchive maskedClassLib = ShrinkWrap.create(JavaArchive.class,"maskedClassLib.jar")
                        .addClass("test.TestBean");

        JavaArchive maskedClassZAppClient = ShrinkWrap.create(JavaArchive.class,"maskedClassZAppClient.jar")
                        .addClass("test.TestBeanAppClientImpl")
                        .addClass("appclient.Main");

        return ShrinkWrap.create(EnterpriseArchive.class,"maskedClass.ear")
                        .addAsModule(maskedClassEjb)
                        .addAsModule(maskedClassWeb)
                        .addAsModule(maskedClassZAppClient)
                        .addAsLibrary(maskedClassLib);
    }


    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return server;
    }

    @Mode(TestMode.FULL)
    @Test
    public void testClassMasking() throws Exception {
        LibertyServer lServer = server.getLibertyServer();

        HttpUtils.findStringInUrl(lServer, "/maskedClassWeb/TestServlet",
                                  "Type1: from ejb",
                                  "Type3: This is Type3, a managed bean in the war",
                                  "TestBean: This is TestBean in the war");
    }
}
