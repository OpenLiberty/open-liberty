/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.jee.jaxrs.resource;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.jee.ShrinkWrapUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JaxWithNoInjectTest {

    public static final String APP_NAME = "jaxrsResourceInjection";
    public static final String SERVER_NAME = "cdi12JaxNoInjectServer";

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE9, EERepeatActions.EE10, EERepeatActions.EE7);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive jaxrsResourceInjectionApp = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        Package pkg = com.ibm.ws.cdi.jee.jaxrs.resource.war.MyApplication.class.getPackage();
        jaxrsResourceInjectionApp.addPackage(pkg);
        ShrinkWrapUtils.addAsRootResource(jaxrsResourceInjectionApp, pkg, "WEB-INF/ibm-web-bnd.xml");
        ShrinkWrapUtils.addAsRootResource(jaxrsResourceInjectionApp, pkg, "WEB-INF/web.xml");

        ShrinkHelper.exportDropinAppToServer(server, jaxrsResourceInjectionApp, DeployOptions.SERVER_ONLY);
        server.startServer();
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application " + APP_NAME + " started");
    }

    @Test
    public void testResourceInjectionIntoJaxClassWithNoInjectAnnotation() throws Exception {
        HttpUtils.findStringInUrl(server, "/" + APP_NAME + "/starter/resource", "DS =com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
