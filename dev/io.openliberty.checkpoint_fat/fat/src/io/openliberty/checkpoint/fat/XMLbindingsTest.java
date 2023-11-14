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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import xmlBinding.Endpoints;
import xmlBinding.ServerRoot;
import xmlBinding.XMLobject;

@RunWith(FATRunner.class)
@CheckpointTest
public class XMLbindingsTest {

    public static final String APP_NAME = "xmlBinding";
    public static final String SERVER_NAME = "xmlBindingServer";

    public static final String xmlResponse = "<xmLobject><id>5</id><name>XML</name></xmLobject>";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void copyAppToDropins() throws Exception {

        WebArchive webappWar = ShrinkWrap.create(WebArchive.class, "xmlApp.war")
                        .addClass(Endpoints.class)
                        .addClass(ServerRoot.class)
                        .addClass(XMLobject.class);
        ShrinkHelper.exportAppToServer(server, webappWar, DeployOptions.OVERWRITE);
    }

    @Test
    public void testXMLbinding() throws Exception {

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer();
        server.checkpointRestore();

        HttpUtils.findStringInUrl(server, "xmlApp/root/endpoint/properties", xmlResponse);
        server.stopServer(null);
    }

}
