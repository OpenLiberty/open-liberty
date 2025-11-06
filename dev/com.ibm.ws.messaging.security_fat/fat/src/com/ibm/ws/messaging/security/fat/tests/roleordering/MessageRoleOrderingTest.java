/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.messaging.security.fat.tests.roleordering;

import org.jboss.shrinkwrap.api.ShrinkWrap;
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
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class MessageRoleOrderingTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12MessageRoleOrderingServer";
    public static final String APP_NAME = "MessageRoleOrderApp";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE11, EERepeatActions.EE9, EERepeatActions.EE8, EERepeatActions.EE7);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = MessagingServlet.class, contextRoot = APP_NAME),
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive cdiCurrentWar = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                             .addClass(MessagingServlet.class.getName());

        ShrinkHelper.exportDropinAppToServer(server, cdiCurrentWar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWSIJ0056E"); //TODO investigate why this is needed
    }
}

