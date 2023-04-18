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
package com.ibm.ws.cdi.beansxml.implicit.fat.implicitWarLibJars;

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
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitWarLibJars.war.ImplicitWarLibJarsServlet;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitWarLibJars.war.discoveryModeAll.InExplicitBeanArchive;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitWarLibJars.war.discoveryModeAnnotated.AnnotatedModeBean;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitWarLibJars.war.noBeansXml.NoBeansXmlBean;
import com.ibm.ws.cdi.beansxml.implicit.fat.utils.ChainableList;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test that library jars inside a war can be implicit bean archives.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class ImplicitWarLibJarsTest extends FATServletClient {

    private static final String APP_NAME_IMPLICIT_BEAN_DISCOVERY = "implicitBeanDiscovery";

    public static final String SERVER_NAME = "cdi12ImplicitServer";

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE9, EERepeatActions.EE7);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = ImplicitWarLibJarsServlet.class, contextRoot = APP_NAME_IMPLICIT_BEAN_DISCOVERY), //FULL
    })
    public static LibertyServer server;

    @BeforeClass
    public static void buildShrinkWrap() throws Exception {

        JavaArchive implicitBeanAnnotatedMode = ShrinkWrap.create(JavaArchive.class, "implicitBeanAnnotatedMode.jar");
        implicitBeanAnnotatedMode.addClass(AnnotatedModeBean.class);
        CDIArchiveHelper.addBeansXML(implicitBeanAnnotatedMode, AnnotatedModeBean.class.getPackage(), "annotated-beans.xml");

        JavaArchive implicitBeanNoBeansXml = ShrinkWrap.create(JavaArchive.class, "implicitBeanNoBeansXml.jar");
        implicitBeanNoBeansXml.addClass(NoBeansXmlBean.class);

        JavaArchive implicitBeanExplicitArchive = ShrinkWrap.create(JavaArchive.class, "implicitBeanExplicitArchive.jar");
        implicitBeanExplicitArchive.addClass(InExplicitBeanArchive.class);
        CDIArchiveHelper.addBeansXML(implicitBeanExplicitArchive, InExplicitBeanArchive.class.getPackage(), "all-beans.xml");

        JavaArchive utilLib = ShrinkWrap.create(JavaArchive.class, "utilLib.jar");
        utilLib.addPackage(ChainableList.class.getPackage());
        CDIArchiveHelper.addBeansXML(utilLib, ChainableList.class.getPackage(), "empty-beans.xml");

        WebArchive implicitBeanDiscovery = ShrinkWrap.create(WebArchive.class, APP_NAME_IMPLICIT_BEAN_DISCOVERY + ".war");
        implicitBeanDiscovery.addClass(ImplicitWarLibJarsServlet.class);
        CDIArchiveHelper.addBeansXML(implicitBeanDiscovery, ImplicitWarLibJarsServlet.class.getPackage(), "all-beans.xml");
        implicitBeanDiscovery.addAsLibrary(implicitBeanAnnotatedMode);
        implicitBeanDiscovery.addAsLibrary(implicitBeanNoBeansXml);
        implicitBeanDiscovery.addAsLibrary(implicitBeanExplicitArchive);
        implicitBeanDiscovery.addAsLibrary(utilLib);

        ShrinkHelper.exportDropinAppToServer(server, implicitBeanDiscovery, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
