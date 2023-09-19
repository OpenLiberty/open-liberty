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
package com.ibm.ws.cdi.jee.jsp;

import static org.junit.Assert.assertNotNull;

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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class SimpleJSPTest {
    public static final String APP_NAME = "simpleJSPApp";
    public static final String SERVER_NAME = "cdi12BasicServer";

    @ClassRule //This test currently causes J2S errors when running with EE11
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE9, EERepeatActions.EE7);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive simpleJSPApp = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        simpleJSPApp.addClass(com.ibm.ws.cdi.jee.jsp.war.SimpleJspBean.class);
        Package pkg = com.ibm.ws.cdi.jee.jsp.war.SimpleJspBean.class.getPackage();
        ShrinkWrapUtils.addAsRootResource(simpleJSPApp, pkg, "WEB-INF/web.xml");
        ShrinkWrapUtils.addAsRootResource(simpleJSPApp, pkg, "index.jsp");
        CDIArchiveHelper.addBeansXML(simpleJSPApp, DiscoveryMode.ALL);

        ShrinkHelper.exportDropinAppToServer(server, simpleJSPApp, DeployOptions.SERVER_ONLY);
        server.startServer();
        assertNotNull(APP_NAME + " started or updated message", server.waitForStringInLogUsingMark("CWWKZ000[13]I.*" + APP_NAME));
    }

    @Test
    public void testSimpleJSP() throws Exception {
        HttpUtils.findStringInUrl(server, "/" + APP_NAME + "/", "Test Sucessful!");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
