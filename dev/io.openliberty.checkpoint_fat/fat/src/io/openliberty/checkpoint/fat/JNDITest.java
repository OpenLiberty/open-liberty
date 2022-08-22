/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import jndiApp.JNDIservlet;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class JNDITest {

    public static final String APP_NAME = "jndiApp";

    @Server("jndiTest")
    public static LibertyServer server;

    @BeforeClass
    public static void copyAppToDropins() throws Exception {

        WebArchive webappWar = ShrinkWrap.create(WebArchive.class, "JNDIApplication.war").addClass(JNDIservlet.class);
        ShrinkHelper.exportAppToServer(server, webappWar, DeployOptions.OVERWRITE);
    }

    @Test
    public void testJNDIlookup() throws Exception {

        server.startServer();
        HttpUtils.findStringInUrl(server, "jndiApp/servlet", "default value");
        server.stopServer();

        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, null);
        server.startServer();

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("jndiEntry").setValue("alternate value");
        server.updateServerConfiguration(config);

        server.checkpointRestore();

        HttpUtils.findStringInUrl(server, "jndiApp/servlet", "alternate value");

    }

}
