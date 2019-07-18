/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.timers;

import static componenttest.annotation.SkipIfSysProp.DB_Informix;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.dsprops.Properties_informix;

import componenttest.annotation.SkipIfSysProp;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.PersistentTimersTestServlet;

/**
 * Tests for persistent scheduled executor with task execution disabled
 */
@RunWith(FATRunner.class)
@SkipIfSysProp(DB_Informix) // persistent executor is not support on Informix
public class PersistentExecutorTimersTest extends FATServletClient {

    private static final String APP_NAME = "timersapp";

    @TestServlet(servlet = PersistentTimersTestServlet.class, path = APP_NAME)
    public static final LibertyServer server = FATSuite.server;

    /**
     * Before running any tests, start the server
     */
    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "web", "ejb");

        ServerConfiguration config = server.getServerConfiguration();

        // Update the server configuration artifacts if the bootstrapping.properties file was
        // populated with relevant database information.
        config.updateDatabaseArtifacts();

        // Set the lock_mode_wait property if dealing with informix jdbc.
        ConfigElementList<DataSource> dataSources = config.getDataSources();
        DataSource ds = dataSources.getById("DefaultDataSource");
        ConfigElementList<Properties_informix> informixProps = ds.getProperties_informix();
        if (!informixProps.isEmpty()) {
            Properties_informix properties = informixProps.get(0);
            properties.setIfxIFX_LOCK_MODE_WAIT("30");
        }

        server.updateServerConfiguration(config);

        if (!informixProps.isEmpty() || !ds.getProperties_informix_jcc().isEmpty())
            return; // TODO enable once persistence service adds support for Informix

        server.startServer();

    }

    /**
     * After completing all tests, stop the server.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted())
            server.stopServer("CWWKZ0022W");
    }
}