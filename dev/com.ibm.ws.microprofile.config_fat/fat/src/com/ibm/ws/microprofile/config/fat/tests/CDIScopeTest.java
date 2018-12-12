/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.fat.tests;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfig14EE8;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class CDIScopeTest extends FATServletClient {

    @ClassRule
    public static RepeatTests r = RepeatTests //selected combinations
                    .with(new RepeatConfig14EE8("CDIScopeServer"));

    @Server("CDIScopeServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = SharedShrinkWrapApps.cdiConfigServerApps();

        ShrinkHelper.exportDropinAppToServer(server, war);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testConfigScope() throws Exception {
        //set a system property
        HttpUtils.findStringInReadyUrl(server, "/cdiConfig/system?key=SYS_PROP&value=value1", "SYS_PROP=value1");
        //check it
        HttpUtils.findStringInReadyUrl(server, "/cdiConfig/system?key=SYS_PROP", "SYS_PROP=value1");
        //change it
        HttpUtils.findStringInReadyUrl(server, "/cdiConfig/system?key=SYS_PROP&value=value2", "SYS_PROP=value2");
        //check it again ... it shouldn't have changed because the injected property should be Session scoped
        HttpUtils.findStringInReadyUrl(server, "/cdiConfig/system?key=SYS_PROP", "SYS_PROP=value1");
    }
}
