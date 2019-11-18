/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.suite;

import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.ConnectorProperties.simpleOutgoingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.suite.KafkaUtils.kafkaClientLibs;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.reactive.messaging.fat.apps.kafka.BasicMessagingBean;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class BadConnectorIDTest {

    private static final String APP_NAME = "badConnectorIDTest";

    @Server("SimpleRxMessagingServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        PropertiesAsset config = new PropertiesAsset()
                        .include(simpleOutgoingChannel(PlaintextTests.kafkaContainer, "badConnectorID", BasicMessagingBean.CHANNEL_OUT, BasicMessagingBean.CHANNEL_OUT))
                        .include(simpleIncomingChannel(PlaintextTests.kafkaContainer, BasicMessagingBean.CHANNEL_IN, "test-consumer"));

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(BasicMessagingBean.class.getPackage())
                        .addPackage(KafkaTestConstants.class.getPackage())
                        .addAsResource(config, "META-INF/microprofile-config.properties")
                        .addAsManifestResource(KafkaUtils.kafkaPermissions(), "permissions.xml")
                        .addAsLibraries(kafkaClientLibs());

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        /*
         * The application will fail to start due to the brokenness of the RM Connector under test.
         *
         * Use the LibertyServer startServerAndValidate() method to allow the server to start
         * without the necessity to validate that the application has started successfully.
         *
         * The startServerAndValidate parameters are set, in order, as follows,
         *
         * DEFAULT_PRE_CLEAN=true, DEFAULT_CLEANSTART=true, validateApps=false
         */
        server.startServerAndValidate(true, true, false);
    }

    @Test
    public void testBadConnectorID() throws Exception {
        List<String> errors = server
                        .findStringsInLogs("java.lang.IllegalArgumentException: Unknown connector for " + BasicMessagingBean.CHANNEL_OUT);
        assertTrue(errors.size() > 0);
    }

    @AfterClass
    public static void teardownTest() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKZ0002E");
        }
    }

}
