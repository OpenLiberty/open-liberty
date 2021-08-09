/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.fat.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE7_FULL;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.ClassExclusionTestServlet;
import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.excludedpackage.ExcludedPackageBean;
import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.excludedpackagetree.subpackage.ExcludedPackageTreeBean;
import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.exludedbycombopackagetree.subpackage.ExcludedByComboBean;
import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.fallbackbeans.FallbackForExcludedBean;
import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.interfaces.IVetoedBean;
import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.packageexcludedbyproperty.ExcludedByPropertyBean;
import com.ibm.ws.cdi.beansxml.fat.apps.classexclusion.packageprotectedbyclass.ProtectedByClassBean;
import com.ibm.ws.cdi.beansxml.fat.apps.vetoedAlternative.AppScopedBean;
import com.ibm.ws.cdi.beansxml.fat.apps.vetoedAlternative.VetoedAlternativeBean;
import com.ibm.ws.cdi.beansxml.fat.apps.vetoedAlternative.VetoedAlternativeTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * These tests verify that you can exclude classes from Bean discovery through beans.xml and the @Vetoed annotaiton as per
 * http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html#bean_discovery
 * http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html#what_classes_are_beans
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ClassExclusionTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12ClassExclusionTestServer";

    public static final String CLASS_EXCLUSION_APP_NAME = "classExclusion";
    public static final String VETO_ALTERNATIVE_APP_NAME = "TestVetoedAlternative";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE7_FULL); //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = ClassExclusionTestServlet.class, contextRoot = CLASS_EXCLUSION_APP_NAME),
                    @TestServlet(servlet = VetoedAlternativeTestServlet.class, contextRoot = VETO_ALTERNATIVE_APP_NAME) }) //FULL
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive classExclusion = ShrinkWrap.create(WebArchive.class, "classExclusion.war");
        classExclusion.addPackage(ClassExclusionTestServlet.class.getPackage());
        classExclusion.addPackage(IVetoedBean.class.getPackage());
        classExclusion.addPackage(FallbackForExcludedBean.class.getPackage());
        classExclusion.addClass(ExcludedByPropertyBean.class);
        classExclusion.addClass(ProtectedByClassBean.class);
        classExclusion.addClass(ExcludedPackageBean.class);
        classExclusion.addClass(ExcludedPackageTreeBean.class);
        classExclusion.addClass(ExcludedByComboBean.class);

        CDIArchiveHelper.addBeansXML(classExclusion, ClassExclusionTestServlet.class);

        EnterpriseArchive classExclusionEar = ShrinkWrap.create(EnterpriseArchive.class, "classExclusion.ear");
        classExclusionEar.addAsManifestResource(ClassExclusionTestServlet.class.getPackage(), "permissions.xml", "permissions.xml");
        classExclusionEar.addAsModule(classExclusion);

        JavaArchive testVetoedAlternativeJar = ShrinkWrap.create(JavaArchive.class, "TestVetoedAlternative.jar");
        testVetoedAlternativeJar.addClass(AppScopedBean.class);
        testVetoedAlternativeJar.addClass(VetoedAlternativeBean.class);
        CDIArchiveHelper.addBeansXML(testVetoedAlternativeJar, AppScopedBean.class);

        WebArchive testVetoedAlternativeWar = ShrinkWrap.create(WebArchive.class, "TestVetoedAlternative.war");
        testVetoedAlternativeWar.setManifest(VetoedAlternativeTestServlet.class.getPackage(), "MANIFEST.MF");
        testVetoedAlternativeWar.addClass(VetoedAlternativeTestServlet.class);
        CDIArchiveHelper.addEmptyBeansXML(testVetoedAlternativeWar);

        EnterpriseArchive testVetoedAlternativeEar = ShrinkWrap.create(EnterpriseArchive.class, "TestVetoedAlternative.ear");
        testVetoedAlternativeEar.setApplicationXML(VetoedAlternativeTestServlet.class.getPackage(), "application.xml");
        testVetoedAlternativeEar.addAsModule(testVetoedAlternativeJar);
        testVetoedAlternativeEar.addAsModule(testVetoedAlternativeWar);

        ShrinkHelper.exportDropinAppToServer(server, classExclusionEar, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, testVetoedAlternativeEar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
