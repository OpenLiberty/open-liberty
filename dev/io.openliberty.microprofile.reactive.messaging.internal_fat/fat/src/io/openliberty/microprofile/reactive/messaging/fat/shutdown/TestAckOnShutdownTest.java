/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.shutdown;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaClientLibs;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractReceptionBean;
import com.ibm.ws.microprofile.reactive.messaging.fat.repeats.ReactiveMessagingActions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.reactive.messaging.fat.apps.shutdown.TestAckOnShutdownServlet;
import io.openliberty.microprofile.reactive.messaging.fat.suite.KafkaTests;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class TestAckOnShutdownTest extends FATServletClient {

    public static final String APP_NAME = "TestAckOnShutdown";
    public static final String SERVER_NAME = "TestAckOnShutdownServer";
    public static final String TOPIC_NAME = APP_NAME;

    @Server(SERVER_NAME)
    @TestServlet(contextRoot = APP_NAME, servlet = TestAckOnShutdownServlet.class)
    public static LibertyServer server;

    @ClassRule
    public static final RepeatTests r = ReactiveMessagingActions.reactive30Repeats(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        ConnectorProperties inputConfig = simpleIncomingChannel(KafkaTests.connectionProperties(),
                                                                APP_NAME,
                                                                APP_NAME);

        PropertiesAsset appConfig = new PropertiesAsset()
                        .include(inputConfig);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addPackage(TestAckOnShutdownServlet.class.getPackage())
                        .addPackage(AbstractReceptionBean.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportAppToServer(server, war, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

}
