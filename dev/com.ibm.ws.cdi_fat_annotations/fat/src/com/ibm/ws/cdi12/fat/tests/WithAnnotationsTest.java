/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.io.File;

import com.ibm.websphere.simplicity.ShrinkHelper;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

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
import componenttest.rules.ServerRules;
import componenttest.rules.TestRules;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests for <code>@WithAnnotations</code> used in portable extensions for observing type discovery of beans with certain annotations.
 */
@Mode(TestMode.FULL)
public class WithAnnotationsTest extends LoggingTest {
    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("cdi12WithAnnotationsServer");
    private static boolean hasSetUp = false;

    @BeforeClass
    public static void setUp() throws Exception {
 
        if (hasSetUp) {
            return;
        }

        JavaArchive utilLib = ShrinkWrap.create(JavaArchive.class,"utilLib.jar")
                        .addClass("com.ibm.ws.cdi12.test.utils.ChainableListImpl")
                        .addClass("com.ibm.ws.cdi12.test.utils.Intercepted")
                        .addClass("com.ibm.ws.cdi12.test.utils.ChainableList")
                        .addClass("com.ibm.ws.cdi12.test.utils.Utils")
                        .addClass("com.ibm.ws.cdi12.test.utils.SimpleAbstract")
                        .addClass("com.ibm.ws.cdi12.test.utils.ForwardingList")
                        .add(new FileAsset(new File("test-applications/utilLib.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

        WebArchive withAnnotationsApp = ShrinkWrap.create(WebArchive.class, "withAnnotationsApp.war")
                        .addClass("com.ibm.ws.cdi12.test.withAnnotations.WithAnnotationsServlet")
                        .addClass("com.ibm.ws.cdi12.test.withAnnotations.WithAnnotationsExtension")
                        .addClass("com.ibm.ws.cdi12.test.withAnnotations.NonAnnotatedBean")
                        .addClass("com.ibm.ws.cdi12.test.withAnnotations.RequestScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.withAnnotations.ApplicationScopedBean")
                        .add(new FileAsset(new File("test-applications/withAnnotationsApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/withAnnotationsApp.war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension")
                        .addAsLibrary(utilLib);

        ShrinkHelper.exportDropinAppToServer(server, withAnnotationsApp);
        hasSetUp = true;
        server.waitForStringInLogUsingMark("CWWKZ0001I");

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return null;
    }

    @ClassRule
    public static final TestRule startAndStopServerRule = ServerRules.startAndStopAutomatically(server);

    @Rule
    public final TestRule runAll = TestRules.runAllUsingTestNames(server).usingApp("withAnnotationsApp").andServlet("testServlet");

    /**
     * Test that receiving a <code>@ProcessAnnotatedType</code> works without <code>@WithAnnotations</code>.
     */
    @Test
    public void testBasicProcessAnnotatedTypeEvent() {}

    /**
     * Test that events aren't fired for classes with no annotations.
     */
    @Test
    public void testNoAnnotations() {}

    /**
     * Test that events aren't fired for classes with annotations which aren't specified.
     */
    @Test
    public void testNonSpecifiedAnnotation() {}

    /**
     * Test that events are fired for classes with the specified annotations.
     */
    @Test
    public void testWithAnnotations() {}

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
