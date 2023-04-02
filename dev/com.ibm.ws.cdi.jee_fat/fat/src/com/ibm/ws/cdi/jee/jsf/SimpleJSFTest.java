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
package com.ibm.ws.cdi.jee.jsf;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;
import com.ibm.ws.cdi.jee.ShrinkWrapUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class SimpleJSFTest {

    public static final String APP_NAME = "simpleJSFApp";
    public static final String SERVER_NAME = "cdi12JSFServer";

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE9, EERepeatActions.EE10, EERepeatActions.EE7);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive simpleJSFApp = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        Package pkg = com.ibm.ws.cdi.jee.jsf.simple.SimpleJsfBean.class.getPackage();
        simpleJSFApp.addPackage(pkg);
        ShrinkWrapUtils.addAsRootResource(simpleJSFApp, pkg, "WEB-INF/faces-config.xml");
        ShrinkWrapUtils.addAsRootResource(simpleJSFApp, pkg, "WEB-INF/web.xml");
        ShrinkWrapUtils.addAsRootResource(simpleJSFApp, pkg, "testBasicJsf.xhtml");
        CDIArchiveHelper.addBeansXML(simpleJSFApp, DiscoveryMode.ALL);

        ShrinkHelper.exportDropinAppToServer(server, simpleJSFApp, DeployOptions.SERVER_ONLY);
        server.startServer();
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application " + APP_NAME + " started");
    }

    @Test
    public void testSimpleJSF() throws Exception {
        HttpUtils.findStringInUrl(server, "/" + APP_NAME + "/faces/testBasicJsf.xhtml", "Hello from SimpleJsfBean injected with: otherJsfBean");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
