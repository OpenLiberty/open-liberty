/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.implicit.fat.implicitBeanArchive;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitBeanArchive.war.ImplicitBeanArchiveServlet;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitBeanArchive.war.discoveryModeAnnotated.UnannotatedClassInAnnotatedModeBeanArchive;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitBeanArchive.war.discoveryModeNone.RequestScopedButNoScan;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitBeanArchive.war.emptyBeansXML.UnannotatedBeanInAllModeBeanArchive;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitBeanArchive.war.implicitBeans.UnannotatedBeanInImplicitBeanArchive;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitBeanArchive.war.noBeans.ClassWithInjectButNotABean;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test that library jars inside a war can be implicit bean archives.
 */
@RunWith(FATRunner.class)
public class ImplicitBeanArchiveTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12ImplicitServer";

    public static final String IMPLICIT_BEAN_ARCHIVE_APP_NAME = "implicitBeanArchive";

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE9, EERepeatActions.EE10, EERepeatActions.EE7);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = com.ibm.ws.cdi.beansxml.implicit.fat.implicitBeanArchive.war.ImplicitBeanArchiveServlet.class,
                                    contextRoot = IMPLICIT_BEAN_ARCHIVE_APP_NAME) //LITE
    })
    public static LibertyServer server;

    @BeforeClass
    public static void buildShrinkWrap() throws Exception {

        JavaArchive archiveWithBeansXML = ShrinkWrap.create(JavaArchive.class, "archiveWithBeansXML.jar");
        archiveWithBeansXML.addClass(UnannotatedBeanInAllModeBeanArchive.class);
        CDIArchiveHelper.addBeansXML(archiveWithBeansXML, UnannotatedBeanInAllModeBeanArchive.class.getPackage(), "all-beans.xml");

        JavaArchive archiveWithNoScanBeansXML = ShrinkWrap.create(JavaArchive.class, "archiveWithNoScanBeansXML.jar");
        archiveWithNoScanBeansXML.addClass(RequestScopedButNoScan.class);
        CDIArchiveHelper.addBeansXML(archiveWithNoScanBeansXML, RequestScopedButNoScan.class.getPackage(), "none-beans.xml");

        JavaArchive archiveWithNoImplicitBeans = ShrinkWrap.create(JavaArchive.class, "archiveWithNoImplicitBeans.jar");
        archiveWithNoImplicitBeans.addClass(ClassWithInjectButNotABean.class);

        JavaArchive archiveWithImplicitBeans = ShrinkWrap.create(JavaArchive.class, "archiveWithImplicitBeans.jar");
        archiveWithImplicitBeans.addPackage(UnannotatedBeanInImplicitBeanArchive.class.getPackage());

        JavaArchive archiveWithAnnotatedModeBeansXML = ShrinkWrap.create(JavaArchive.class, "archiveWithAnnotatedModeBeansXML.jar");
        archiveWithAnnotatedModeBeansXML.addPackage(UnannotatedClassInAnnotatedModeBeanArchive.class.getPackage());
        CDIArchiveHelper.addBeansXML(archiveWithAnnotatedModeBeansXML, UnannotatedClassInAnnotatedModeBeanArchive.class.getPackage(), "annotated-beans.xml");

        WebArchive implicitBeanArchive = ShrinkWrap.create(WebArchive.class, IMPLICIT_BEAN_ARCHIVE_APP_NAME + ".war");
        implicitBeanArchive.addClass(ImplicitBeanArchiveServlet.class);
        implicitBeanArchive.addAsLibrary(archiveWithBeansXML);
        implicitBeanArchive.addAsLibrary(archiveWithImplicitBeans);
        implicitBeanArchive.addAsLibrary(archiveWithNoImplicitBeans);
        implicitBeanArchive.addAsLibrary(archiveWithNoScanBeansXML);
        implicitBeanArchive.addAsLibrary(archiveWithAnnotatedModeBeansXML);

        ShrinkHelper.exportDropinAppToServer(server, implicitBeanArchive, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
