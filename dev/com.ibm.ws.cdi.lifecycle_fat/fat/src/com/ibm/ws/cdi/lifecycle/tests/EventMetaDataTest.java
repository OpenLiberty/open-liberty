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
package com.ibm.ws.cdi.lifecycle.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.lifecycle.apps.eventMetaDataWar.MetaDataServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * These tests verify that inspecting event meta data works correctly as per http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html#event_metadata
 */

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class EventMetaDataTest extends FATServletClient {

    private static final String SERVER_NAME = "cdi12EventMetadataServer";

    @Server(SERVER_NAME)
    @TestServlet(contextRoot = "eventMetaData", servlet = MetaDataServlet.class)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME,
                                                         EERepeatActions.EE10,
                                                         EERepeatActions.EE9,
                                                         EERepeatActions.EE7);

    @BeforeClass
    public static void setup() throws Exception {

        WebArchive eventMetaData = ShrinkWrap.create(WebArchive.class, "eventMetaData.war")
                                             .addPackages(true, MetaDataServlet.class.getPackage())
                                             .addAsManifestResource(MetaDataServlet.class.getResource("permissions.xml"), "permissions.xml")
                                             .addAsWebInfResource(MetaDataServlet.class.getResource("beans.xml"), "beans.xml");

        ShrinkHelper.exportDropinAppToServer(server, eventMetaData, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }
}