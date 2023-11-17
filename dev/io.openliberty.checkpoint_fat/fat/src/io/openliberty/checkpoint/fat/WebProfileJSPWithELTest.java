/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat;

import static org.junit.Assert.assertNotNull;

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

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class WebProfileJSPWithELTest {

    public static final String APP_NAME = "JSPWithEL";
    public static final String SERVER_NAME = "webProfileJSPWithEL";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void exportWebAppAndServerSetup() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp(APP_NAME, "jsp.with.el");
        war = (WebArchive) ShrinkHelper.addDirectory(war, "test-applications/" + APP_NAME + "/resources");
        CDIArchiveHelper.addBeansXML(war, DiscoveryMode.ALL);
        ShrinkHelper.exportAppToServer(server, war, DeployOptions.OVERWRITE);

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true, (s) -> {
            assertNotNull("No Initializing application context message",
                          server.waitForStringInLogUsingMark(": Initializing application context", 0));
        });
        server.startServer();
    }

    @Test
    public void testJSPWithELRequestScopeAccess() throws Exception {
        for (int i = 1; i <= 10; i++) {
            HttpUtils.findStringInUrl(server, APP_NAME + "/accessRequestScope.jsp", "rsb.getTestData - RSB - TEST - " + i);
        }
    }

    @Test
    public void testJSPWithELAppScopeAccess() throws Exception {
        for (int i = 1; i <= 10; i++) {
            HttpUtils.findStringInUrl(server, APP_NAME + "/accessAppScope.jsp", "asb.getTestData - ASB - TEST - 1");
        }
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.stopServer();
    }

}
