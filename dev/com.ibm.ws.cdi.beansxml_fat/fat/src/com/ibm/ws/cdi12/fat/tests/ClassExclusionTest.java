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

import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;

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

/**
 * These tests verify that you can exclude classes from Bean discovery through beans.xml and the @Vetoed annotaiton as per
 * http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html#bean_discovery
 * http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html#what_classes_are_beans
 */

@Mode(TestMode.FULL)
public class ClassExclusionTest extends LoggingTest {

    @ClassRule
    // Create the server.
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12ClassExclusionTestServer");

    @BuildShrinkWrap
    public static Archive[] buildShrinkWrap() {
       WebArchive classExclusion = ShrinkWrap.create(WebArchive.class, "classExclusion.war")
                        .addClass("cdi12.classexclusion.test.packageexcludedbyproperty.ExcludedByPropertyBean")
                        .addClass("cdi12.classexclusion.test.ExcludedBean")
                        .addClass("cdi12.classexclusion.test.packageprotectedbyclass.ProtectedByClassBean")
                        .addClass("cdi12.classexclusion.test.excludedpackage.ExcludedPackageBean")
                        .addClass("cdi12.classexclusion.test.IncludedBean")
                        .addClass("cdi12.classexclusion.test.excludedpackagetree.subpackage.ExcludedPackageTreeBean")
                        .addClass("cdi12.classexclusion.test.ProtectedByHalfComboBean")
                        .addClass("cdi12.classexclusion.test.TestServlet")
                        .addClass("cdi12.classexclusion.test.interfaces.IVetoedBean")
                        .addClass("cdi12.classexclusion.test.interfaces.IExcludedPackageBean")
                        .addClass("cdi12.classexclusion.test.interfaces.IProtectedByHalfComboBean")
                        .addClass("cdi12.classexclusion.test.interfaces.IProtectedByClassBean")
                        .addClass("cdi12.classexclusion.test.interfaces.IExcludedBean")
                        .addClass("cdi12.classexclusion.test.interfaces.IIncludedBean")
                        .addClass("cdi12.classexclusion.test.interfaces.IExcludedByPropertyBean")
                        .addClass("cdi12.classexclusion.test.interfaces.IExcludedPackageTreeBean")
                        .addClass("cdi12.classexclusion.test.interfaces.IExcludedByComboBean")
                        .addClass("cdi12.classexclusion.test.fallbackbeans.FallbackForExcludedBean")
                        .addClass("cdi12.classexclusion.test.fallbackbeans.FallbackForExcludedPackageTreeBean")
                        .addClass("cdi12.classexclusion.test.fallbackbeans.FallbackForVetoedBean")
                        .addClass("cdi12.classexclusion.test.fallbackbeans.FallbackForExcludedPackageBean")
                        .addClass("cdi12.classexclusion.test.fallbackbeans.FallbackForExcludedByPropertyBean")
                        .addClass("cdi12.classexclusion.test.fallbackbeans.FallbackForExcludedByComboBean")
                        .addClass("cdi12.classexclusion.test.VetoedBean")
                        .addClass("cdi12.classexclusion.test.exludedbycombopackagetree.subpackage.ExcludedByComboBean")
                        .add(new FileAsset(new File("test-applications/classExclusion.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/classExclusion.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

       EnterpriseArchive classExclusionEar = ShrinkWrap.create(EnterpriseArchive.class,"classExclusion.ear")
                        .add(new FileAsset(new File("test-applications/classExclusion.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .add(new FileAsset(new File("test-applications/classExclusion.ear/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml")
                        .addAsModule(classExclusion);

       JavaArchive TestVetoedAlternativeJar = ShrinkWrap.create(JavaArchive.class,"TestVetoedAlternative.jar")
                        .addClass("com.ibm.cdi.test.vetoed.alternative.AppScopedBean")
                        .addClass("com.ibm.cdi.test.vetoed.alternative.VetoedAlternativeBean")
                        .add(new FileAsset(new File("test-applications/TestVetoedAlternative.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

       WebArchive TestVetoedAlternativeWar = ShrinkWrap.create(WebArchive.class, "TestVetoedAlternative.war")
                        .addAsManifestResource(new File("test-applications/TestVetoedAlternative.war/resources/META-INF/MANIFEST.MF"))
                        .addClass("com.ibm.cdi.test.vetoed.alternative.WebServ")
                        .add(new FileAsset(new File("test-applications/TestVetoedAlternative.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/TestVetoedAlternative.war/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

       EnterpriseArchive TestVetoedAlternativeEar = ShrinkWrap.create(EnterpriseArchive.class,"TestVetoedAlternative.ear")
                        .add(new FileAsset(new File("test-applications/TestVetoedAlternative.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(TestVetoedAlternativeJar)
                        .addAsModule(TestVetoedAlternativeWar);

       Archive[] archives = new Archive[2];
       archives[0] = classExclusionEar;
       archives[1] = TestVetoedAlternativeEar;
       return archives;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testIncludedBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "IncludedBean was correctly injected");
    }

    @Test
    public void testExcludedBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "ExcludedBean was correctly rejected");
    }

    @Test
    public void testExcludedPackageBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "ExcludedPackageBean was correctly rejected");
    }

    @Test
    public void testExcludedPackageTreeBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "ExcludedPackageTreeBean was correctly rejected");
    }

    @Test
    public void testProtectedByClassBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "ProtectedByClassBean was correctly injected");
    }

    @Test
    public void testExcludedByPropertyBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "ExcludedByPropertyBean was correctly rejected");
    }

    @Test
    public void testExcludedByComboBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "ExcludedByComboBean was correctly rejected");
    }

    @Test
    public void testProtectedByHalfComboBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "ProtectedByHalfComboBean was correctly injected");
    }

    @Test
    public void testVetoedBean() throws Exception {
        this.verifyResponse("/classExclusionTest/test", "VetoedBean was correctly rejected");
    }

    @Test
    public void testVetoedAlternativeDoesntThrowException() throws Exception {
        this.verifyResponse("/TestVetoedAlternative/testservlet", "Hello");
    }

}
