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
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

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

import componenttest.rules.ServerRules;
import componenttest.rules.TestRules;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test that wars can be implicit bean archives.
 */
public class ImplicitWarTest {
    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("cdi12ImplicitServer");

    @ClassRule
    public static final TestRule startAndStopServerRule = ServerRules.startAndStopAutomatically(server);

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

       WebArchive implicitWarApp = ShrinkWrap.create(WebArchive.class, "implicitWarApp.war")
                        .addClass("com.ibm.ws.cdi12.test.implicitWar.TestServlet")
                        .addClass("com.ibm.ws.cdi12.test.implicitWar.AnnotatedBean")
                        .addAsLibrary(utilLib);

       server.setMarkToEndOfLog(server.getDefaultLogFile());
       ShrinkHelper.exportDropinAppToServer(server, implicitWarApp);
       assertNotNull("implicitWarApp started or updated message", server.waitForStringInLogUsingMark("CWWKZ000[13]I.*implicitWarApp"));
    }

    @Rule
    public final TestRule runAll = TestRules.runAllUsingTestNames(server).usingApp("implicitWarApp").andServlet("");

    /**
     * Test that injection works in a war that has no beans.xml but does have a class with a bean-defining annotation.
     */
    @Test
    public void testNoBeansXml() {}

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }
}
