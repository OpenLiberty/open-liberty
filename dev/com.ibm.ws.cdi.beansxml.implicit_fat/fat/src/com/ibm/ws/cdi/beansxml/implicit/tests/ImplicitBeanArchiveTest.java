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
package com.ibm.ws.cdi.beansxml.implicit.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE7_FULL;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatTests;
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
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE7_FULL);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = com.ibm.ws.cdi.beansxml.implicit.apps.servlets.ImplicitBeanArchiveServlet.class, contextRoot = IMPLICIT_BEAN_ARCHIVE_APP_NAME) //LITE
    })
    public static LibertyServer server;

    @BeforeClass
    public static void buildShrinkWrap() throws Exception {

        JavaArchive archiveWithBeansXML = ShrinkWrap.create(JavaArchive.class, "archiveWithBeansXML.jar")
                                                    .addClass(com.ibm.ws.cdi.beansxml.implicit.apps.unannotated.UnannotatedBeanInAllModeBeanArchive.class)
                                                    .add(new FileAsset(new File("test-applications/archiveWithBeansXML.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

        JavaArchive archiveWithNoScanBeansXML = ShrinkWrap.create(JavaArchive.class, "archiveWithNoScanBeansXML.jar")
                                                          .addClass(com.ibm.ws.cdi.beansxml.implicit.apps.beans.RequestScopedButNoScan.class)
                                                          .add(new FileAsset(new File("test-applications/archiveWithNoScanBeansXML.jar/resources/META-INF/beans.xml")),
                                                               "/META-INF/beans.xml");

        JavaArchive archiveWithNoImplicitBeans = ShrinkWrap.create(JavaArchive.class, "archiveWithNoImplicitBeans.jar")
                                                           .addClass(com.ibm.ws.cdi.beansxml.implicit.apps.unannotated.ClassWithInjectButNotABean.class);

        JavaArchive archiveWithImplicitBeans = ShrinkWrap.create(JavaArchive.class, "archiveWithImplicitBeans.jar")
                                                         .addClass(com.ibm.ws.cdi.beansxml.implicit.apps.beans.StereotypedBean.class)
                                                         .addClass(com.ibm.ws.cdi.beansxml.implicit.apps.beans.MyExtendedScopedBean.class)
                                                         .addClass(com.ibm.ws.cdi.beansxml.implicit.apps.beans.MyStereotype.class)
                                                         .addClass(com.ibm.ws.cdi.beansxml.implicit.apps.beans.SessionScopedBean.class)
                                                         .addClass(com.ibm.ws.cdi.beansxml.implicit.apps.beans.ConversationScopedBean.class)
                                                         .addClass(com.ibm.ws.cdi.beansxml.implicit.apps.unannotated.UnannotatedBeanInImplicitBeanArchive.class)
                                                         .addClass(com.ibm.ws.cdi.beansxml.implicit.apps.beans.MyExtendedNormalScoped.class)
                                                         .addClass(com.ibm.ws.cdi.beansxml.implicit.apps.beans.RequestScopedBean.class)
                                                         .addClass(com.ibm.ws.cdi.beansxml.implicit.apps.beans.ApplicationScopedBean.class);

        JavaArchive archiveWithAnnotatedModeBeansXML = ShrinkWrap.create(JavaArchive.class, "archiveWithAnnotatedModeBeansXML.jar")
                                                                 .addClass(com.ibm.ws.cdi.beansxml.implicit.apps.beans.DependentScopedBean.class)
                                                                 .addClass(com.ibm.ws.cdi.beansxml.implicit.apps.unannotated.UnannotatedClassInAnnotatedModeBeanArchive.class)
                                                                 .add(new FileAsset(new File("test-applications/archiveWithAnnotatedModeBeansXML.jar/resources/META-INF/beans.xml")),
                                                                      "/META-INF/beans.xml");

        WebArchive implicitBeanArchive = ShrinkWrap.create(WebArchive.class, IMPLICIT_BEAN_ARCHIVE_APP_NAME + ".war")
                                                   .addClass(com.ibm.ws.cdi.beansxml.implicit.apps.servlets.ImplicitBeanArchiveServlet.class)
                                                   .add(new FileAsset(new File("test-applications/" + IMPLICIT_BEAN_ARCHIVE_APP_NAME + ".war/resources/WEB-INF/beans.xml")),
                                                        "/WEB-INF/beans.xml")
                                                   .addAsLibrary(archiveWithBeansXML)
                                                   .addAsLibrary(archiveWithImplicitBeans)
                                                   .addAsLibrary(archiveWithNoImplicitBeans)
                                                   .addAsLibrary(archiveWithNoScanBeansXML)
                                                   .addAsLibrary(archiveWithAnnotatedModeBeansXML);

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
