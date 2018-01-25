/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.json.fat;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Enable JSON logging in messages.log with environment variables in server.env
 */
@RunWith(FATRunner.class)
public class MessagesLogEnvTest extends JSONEventsTest {

    protected static final Class<?> c = MessagesLogEnvTest.class;

    @Server("com.ibm.ws.logging.json.MessagesLogEnvServer")
    public static LibertyServer server;

    @Override
    public LibertyServer getServer() {
        return server;
    }

    @Override
    public RemoteFile getLogFile() throws Exception {
        return server.getDefaultLogFile();
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.logs");
        try {
            server.installSystemBundle("com.ibm.ws.logging.json_fat.TrWriterBundle");
            server.installSystemFeature("TrWriter-1.0");
        } catch (Exception e) {
            Log.error(c, "setUpClass", e);
        }
        server.startServer();
        Assert.assertNotNull("Test app LogstashApp does not appear to have started.", server.waitForStringInLog("CWWKT0016I:.*LogstashApp"));
    }

    @AfterClass
    public static void tearDownClass() {
        if ((server != null) && (server.isStarted())) {
            try {
                server.stopServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
