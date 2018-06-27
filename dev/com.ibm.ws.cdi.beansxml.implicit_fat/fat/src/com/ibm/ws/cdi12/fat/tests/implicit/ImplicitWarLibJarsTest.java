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

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.rules.ServerRules;
import componenttest.rules.TestRules;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Test that library jars inside a war can be implicit bean archives.
 */
@Mode(TestMode.FULL)
public class ImplicitWarLibJarsTest extends LoggingTest {

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("cdi12ImplicitServer");

    @ClassRule
    public static final TestRule startAndStopServerRule = ServerRules.startAndStopAutomatically(server);

    @Rule
    public final TestRule runAll = TestRules.runAllUsingTestNames(server).usingApp("implicitBeanDiscovery").andServlet("");

    @BeforeClass
    public static void buildShrinkWrap() throws Exception {

       JavaArchive implicitBeanAnnotatedMode = ShrinkWrap.create(JavaArchive.class,"implicitBeanAnnotatedMode.jar")
                        .addClass("com.ibm.ws.cdi12.test.implicitBean.AnnotatedModeBean")
                        .add(new FileAsset(new File("test-applications/implicitBeanAnnotatedMode.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

       JavaArchive implicitBeanNoBeansXml = ShrinkWrap.create(JavaArchive.class,"implicitBeanNoBeansXml.jar")
                        .addClass("com.ibm.ws.cdi12.test.implicitBean.NoBeansXmlBean");

       JavaArchive implicitBeanExplicitArchive = ShrinkWrap.create(JavaArchive.class,"implicitBeanExplicitArchive.jar")
                        .addClass("com.ibm.ws.cdi12.test.implicitBean.InExplicitBeanArchive")
                        .add(new FileAsset(new File("test-applications/implicitBeanExplicitArchive.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

       JavaArchive utilLib = ShrinkWrap.create(JavaArchive.class,"utilLib.jar")
                        .addClass("com.ibm.ws.cdi12.test.utils.ChainableListImpl")
                        .addClass("com.ibm.ws.cdi12.test.utils.Intercepted")
                        .addClass("com.ibm.ws.cdi12.test.utils.ChainableList")
                        .addClass("com.ibm.ws.cdi12.test.utils.Utils")
                        .addClass("com.ibm.ws.cdi12.test.utils.SimpleAbstract")
                        .addClass("com.ibm.ws.cdi12.test.utils.ForwardingList")
                        .add(new FileAsset(new File("test-applications/utilLib.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

       WebArchive implicitEJBInWar = ShrinkWrap.create(WebArchive.class, "implicitEJBInWar.war")
                        .addClass("com.ibm.ws.cdi12.test.implicit.ejb.Web1Servlet")
                        .addClass("com.ibm.ws.cdi12.test.implicit.ejb.SimpleEJB")
                        .addAsLibrary(utilLib);

       WebArchive implicitBeanDiscovery = ShrinkWrap.create(WebArchive.class, "implicitBeanDiscovery.war")
                        .addClass("com.ibm.ws.cdi12.test.implicitBean.TestServlet")
                        .add(new FileAsset(new File("test-applications/implicitBeanDiscovery.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(implicitBeanAnnotatedMode)
                        .addAsLibrary(implicitBeanNoBeansXml)
                        .addAsLibrary(implicitBeanExplicitArchive)
                        .addAsLibrary(utilLib);

       server.setMarkToEndOfLog(server.getDefaultLogFile());
       ShrinkHelper.exportDropinAppToServer(server, implicitEJBInWar);
       ShrinkHelper.exportDropinAppToServer(server, implicitBeanDiscovery);
       assertNotNull("implicitBeanDiscovery started or updated message", server.waitForStringInLogUsingMark("CWWKZ000[13]I.*implicitEJBInWar"));
       assertNotNull("implicitBeanDiscovery started or updated message", server.waitForStringInLogUsingMark("CWWKZ000[13]I.*implicitBeanDiscovery"));
    }

    /**
     * Test that we can inject beans from an explicit bean archive (sanity check).
     */
    @Test
    public void testExplicitBeanArchive() {}

    /**
     * Test that we can inject annotated beans from an archive with {@code bean-discovery-mode=annotated}.
     */
    @Test
    public void testAnnotatedBeanDiscoveryMode() {}

    /**
     * Test that we can inject annotated beans from an archive with no beans.xml.
     */
    @Test
    public void testNoBeansXml() {}

    /** {@inheritDoc} */
    @Override
    protected SharedServer getSharedServer() {
        // TODO Auto-generated method stub
        return null;
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
