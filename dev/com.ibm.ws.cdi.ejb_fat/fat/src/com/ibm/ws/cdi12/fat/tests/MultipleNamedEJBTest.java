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

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;

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
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import componenttest.topology.utils.FATServletClient;

/**
 * Tests for having one EJB implementation class with two different {@code ejb-name}s declared in {@code ejb-jar.xml}.
 */
public class MultipleNamedEJBTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12EJB32Server", MultipleNamedEJBTest.class);

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {
        return ShrinkWrap.create(WebArchive.class, "multipleEJBsSingleClass.war")
                        .addClass("com.ibm.ws.cdi12.test.multipleNamedEJBs.SimpleEJBImpl")
                        .addClass("com.ibm.ws.cdi12.test.multipleNamedEJBs.SimpleEJBLocalInterface2")
                        .addClass("com.ibm.ws.cdi12.test.multipleNamedEJBs.TestServlet")
                        .addClass("com.ibm.ws.cdi12.test.multipleNamedEJBs.SimpleManagedBean")
                        .addClass("com.ibm.ws.cdi12.test.multipleNamedEJBs.SimpleEJBLocalInterface1")
                        .add(new FileAsset(new File("test-applications/multipleEJBsSingleClass.war/resources/WEB-INF/ejb-jar.xml")), "/WEB-INF/ejb-jar.xml")
                        .add(new FileAsset(new File("test-applications/multipleEJBsSingleClass.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
    }

    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Rule
    public final TestName testName = new TestName();

    private final void runTest() throws Exception {
        FATServletClient.runTest(SHARED_SERVER.getLibertyServer(), "multipleEJBsSingleClass", testName);
    }

    /**
     * Test that the two injected EJBs with different names are actually different instances.
     */
    @Test
    public void testEjbsAreDifferentInstances() throws Exception {
        this.runTest();
    }

    /**
     * Test that EJB wrapper class names include the correct EJB bean names.
     */
    @Test
    public void testWrapperClassNamesIncludeBeanName() throws Exception {
        this.runTest();
    }

    /**
     * Test that the 'enterprise bean name' used internally matches the name declared at the injection point.
     */
    @Test
    public void testInternalEnterpriseBeanNames() throws Exception {
        this.runTest();
    }

    /**
     * Test that the two EJBs are actually using different instances of the implementation by storing different state in each of them.
     */
    @Test
    public void testStateIsStoredSeparately() throws Exception {
        this.runTest();
    }

}
