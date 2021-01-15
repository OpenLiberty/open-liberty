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
 
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

/**
 * Test that wars can be implicit bean archives.
 */
@RunWith(FATRunner.class)
public class ImplicitWarTest extends FATServletClient {

    private static final String APP_NAME = "implicitWarApp";

    @Server("cdi12ImplicitServer")
    @TestServlets({
                    @TestServlet(servlet = com.ibm.ws.cdi12.test.implicitWar.TestServlet.class, contextRoot = APP_NAME) 
    })
    public static LibertyServer server;

    @BeforeClass
    public static void buildShrinkWrap() throws Exception {

       JavaArchive utilLib = ShrinkWrap.create(JavaArchive.class,"utilLib.jar")
                        .addClass("com.ibm.ws.cdi12.test.utils.ChainableListImpl")
                        .addClass("com.ibm.ws.cdi12.test.utils.Intercepted")
                        .addClass("com.ibm.ws.cdi12.test.utils.ChainableList")
                        .addClass("com.ibm.ws.cdi12.test.utils.Utils")
                        .addClass("com.ibm.ws.cdi12.test.utils.SimpleAbstract")
                        .addClass("com.ibm.ws.cdi12.test.utils.ForwardingList")
                        .add(new FileAsset(new File("test-applications/utilLib.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

       WebArchive implicitWarApp = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addClass("com.ibm.ws.cdi12.test.implicitWar.TestServlet")
                        .addClass("com.ibm.ws.cdi12.test.implicitWar.AnnotatedBean")
                        .addAsLibrary(utilLib);

       ShrinkHelper.exportDropinAppToServer(server, implicitWarApp);
       server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }
}
