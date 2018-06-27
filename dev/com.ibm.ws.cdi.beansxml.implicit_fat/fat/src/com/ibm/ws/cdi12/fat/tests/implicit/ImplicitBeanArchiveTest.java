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

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.rules.ServerRules;
import componenttest.rules.TestRules;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test that library jars inside a war can be implicit bean archives.
 */
public class ImplicitBeanArchiveTest extends LoggingTest {

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("cdi12ImplicitServer");

    @ClassRule
    public static final TestRule startAndStopServerRule = ServerRules.startAndStopAutomatically(server);

    @BeforeClass
    public static void buildShrinkWrap() throws Exception {

       JavaArchive archiveWithBeansXML = ShrinkWrap.create(JavaArchive.class,"archiveWithBeansXML.jar")
                        .addClass("com.ibm.ws.cdi12.test.beansXML.UnannotatedBeanInAllModeBeanArchive")
                        .add(new FileAsset(new File("test-applications/archiveWithBeansXML.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

       JavaArchive archiveWithNoScanBeansXML = ShrinkWrap.create(JavaArchive.class,"archiveWithNoScanBeansXML.jar")
                        .addClass("com.ibm.ws.cdi12.test.implicit.noscan.RequestScopedButNoScan")
                        .add(new FileAsset(new File("test-applications/archiveWithNoScanBeansXML.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

       JavaArchive archiveWithNoImplicitBeans = ShrinkWrap.create(JavaArchive.class,"archiveWithNoImplicitBeans.jar")
                        .addClass("com.ibm.ws.cdi12.test.implicit.nobeans.ClassWithInjectButNotABean");

       JavaArchive archiveWithImplicitBeans = ShrinkWrap.create(JavaArchive.class,"archiveWithImplicitBeans.jar")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.StereotypedBean")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.MyExtendedScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.MyStereotype")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.SessionScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.ConversationScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.UnannotatedBeanInImplicitBeanArchive")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.MyExtendedNormalScoped")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.RequestScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.ApplicationScopedBean");

       JavaArchive archiveWithAnnotatedModeBeansXML = ShrinkWrap.create(JavaArchive.class,"archiveWithAnnotatedModeBeansXML.jar")
                        .addClass("com.ibm.ws.cdi12.test.annotatedBeansXML.DependentScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.annotatedBeansXML.UnannotatedClassInAnnotatedModeBeanArchive")
                        .add(new FileAsset(new File("test-applications/archiveWithAnnotatedModeBeansXML.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

       WebArchive implicitBeanArchive = ShrinkWrap.create(WebArchive.class, "implicitBeanArchive.war")
                        .addClass("com.ibm.ws.cdi12.test.implicit.servlet.Web1Servlet")
                        .add(new FileAsset(new File("test-applications/implicitBeanArchive.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(archiveWithBeansXML)
                        .addAsLibrary(archiveWithImplicitBeans)
                        .addAsLibrary(archiveWithNoImplicitBeans)
                        .addAsLibrary(archiveWithNoScanBeansXML)
                        .addAsLibrary(archiveWithAnnotatedModeBeansXML);

       server.setMarkToEndOfLog(server.getDefaultLogFile());
       ShrinkHelper.exportDropinAppToServer(server, implicitBeanArchive);
       assertNotNull("implicitBeanArchive started or updated message", server.waitForStringInLogUsingMark("CWWKZ000[13]I.*implicitBeanArchive"));
    }

    @Rule
    public final TestRule runAll = TestRules.runAllUsingTestNames(server).usingApp("implicitBeanArchive").andServlet("");

    @Test
    public void testUnannotatedBeanInAllModeBeanArchive() {
        //this one has a beans.xml with mode set to "all" so should be ok
    }

    @Test
    public void testApplicationScopedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
    }

    @Test
    public void testConversationScopedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
    }

    @Test
    public void testNormalScopedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
    }

    @Test
    public void testStereotypedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
    }

    @Test
    public void testRequestScopedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
    }

    @Test
    public void testSessionScopedBeanInImplicitArchive() {
        //this one is an implicit bean so should be ok
    }

    @Test
    public void testUnannotatedBeanInImplicitArchive() {
        //this one is NOT an implicit bean and has no beans.xml so it should be null
    }

    @Test
    public void testDependentScopedBeanInAnnotatedModeArchive() {
        //this one is an implicit bean in an "annotated" mode archive so should be ok
    }

    @Test
    public void testUnannotatedBeanInAnnotatedModeArchive() {
        //this one is NOT an implicit bean in an "annotated" mode archive so should be null
    }

    @Test
    public void testRequestScopedBeanInNoneModeArchive() {
        //this one is an implicit bean in an "none" mode archive so should be null
    }

    @Test
    public void testClassWithInjectButNotInABeanArchive() {
        //this one is not an implicit bean and has no beans.xml so it should be null
    }

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
