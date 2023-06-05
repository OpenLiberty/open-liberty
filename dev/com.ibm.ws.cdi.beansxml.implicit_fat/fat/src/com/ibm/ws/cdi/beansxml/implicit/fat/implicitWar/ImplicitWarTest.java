/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.beansxml.implicit.fat.implicitWar;

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
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitWar.war.AnnotatedBean;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitWar.war.ImplicitWarServlet;
import com.ibm.ws.cdi.beansxml.implicit.fat.utils.SimpleAbstract;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test that wars can be implicit bean archives.
 */
@RunWith(FATRunner.class)
public class ImplicitWarTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12ImplicitServer";

    private static final String IMPLICIT_WAR_APP_NAME = "implicitWarApp";

    @Server("cdi12ImplicitServer")
    @TestServlets({
                    @TestServlet(servlet = ImplicitWarServlet.class, contextRoot = IMPLICIT_WAR_APP_NAME)//LITE
    })
    public static LibertyServer server;

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE9, EERepeatActions.EE7);

    @BeforeClass
    public static void buildShrinkWrap() throws Exception {

        JavaArchive utilLib = ShrinkWrap.create(JavaArchive.class, "utilLib.jar");
        utilLib.addClass(SimpleAbstract.class);
        CDIArchiveHelper.addBeansXML(utilLib, SimpleAbstract.class.getPackage(), "empty-beans.xml");

        WebArchive implicitWarApp = ShrinkWrap.create(WebArchive.class, IMPLICIT_WAR_APP_NAME + ".war");
        implicitWarApp.addClass(ImplicitWarServlet.class);
        implicitWarApp.addClass(AnnotatedBean.class);
        implicitWarApp.addAsLibrary(utilLib);

        ShrinkHelper.exportDropinAppToServer(server, implicitWarApp, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }
}
