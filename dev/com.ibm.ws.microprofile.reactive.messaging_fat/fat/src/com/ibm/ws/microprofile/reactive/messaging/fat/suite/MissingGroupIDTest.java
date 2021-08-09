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

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;
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
import com.ibm.ws.microprofile.reactive.messaging.kafka.KafkaConnectorConstants;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class MissingGroupIDTest {

    private static final String APP_NAME = "missingGroupIDTest";

    @Server("SimpleRxMessagingServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        PropertiesAsset config = new PropertiesAsset()
                        .include(simpleOutgoingChannel(PlaintextTests.kafkaContainer, BasicMessagingBean.CHANNEL_OUT))
                        .include(simpleIncomingChannel(PlaintextTests.kafkaContainer, BasicMessagingBean.CHANNEL_IN, "groupIDtoberemoved")
                                        .removeProperty(ConsumerConfig.GROUP_ID_CONFIG));

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
    public void testMissingGroupID() throws Exception {
        List<String> errors = server
                        .findStringsInLogs("CWMRX1005E.*" + ConnectorFactory.INCOMING_PREFIX + BasicMessagingBean.CHANNEL_IN + "." + KafkaConnectorConstants.GROUP_ID);
        assertTrue("Missing Group ID Message not found", errors.size() > 0);
    }

    @AfterClass
    public static void teardownTest() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWMRX1005E");
        }
    }

}
