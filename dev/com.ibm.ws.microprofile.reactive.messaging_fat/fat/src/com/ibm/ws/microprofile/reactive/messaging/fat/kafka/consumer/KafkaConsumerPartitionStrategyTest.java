/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.consumer;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.PlaintextTests;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.ReactiveMessagingActions;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleOutgoingChannel;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

@RunWith(FATRunner.class)
public class KafkaConsumerPartitionStrategyTest {
    public static final String APP_NAME = "KafkaMultipleConsumerApp";
    public static final String SERVER_NAME = "SimpleRxMessagingServer";
    public static KafkaTestClient kafkaTestClient;

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeat(SERVER_NAME,
                                                                  ReactiveMessagingActions.MP61_RM30,
                                                                  ReactiveMessagingActions.MP20_RM10);

    @Before
    public void setup() throws Exception {
        ConnectorProperties inputChannel1 = simpleIncomingChannel(PlaintextTests.connectionProperties(), KafkaMultipleChannelMessageBean.CHANNEL_IN_1, APP_NAME);
        ConnectorProperties outputChannel1 = simpleOutgoingChannel(PlaintextTests.connectionProperties(), KafkaMultipleChannelMessageBean.CHANNEL_OUT_1);

        ConnectorProperties inputChannel2 = simpleIncomingChannel(PlaintextTests.connectionProperties(), KafkaMultipleChannelMessageBean.CHANNEL_IN_2, APP_NAME);
        ConnectorProperties outputChannel2 = simpleOutgoingChannel(PlaintextTests.connectionProperties(), KafkaMultipleChannelMessageBean.CHANNEL_OUT_2);

        ConnectorProperties inputChannel3 = simpleIncomingChannel(PlaintextTests.connectionProperties(), KafkaMultipleChannelMessageBean.CHANNEL_IN_3, APP_NAME);
        ConnectorProperties outputChannel3 = simpleOutgoingChannel(PlaintextTests.connectionProperties(), KafkaMultipleChannelMessageBean.CHANNEL_OUT_3);

        ConnectorProperties inputChannel4 = simpleIncomingChannel(PlaintextTests.connectionProperties(), KafkaMultipleChannelMessageBean.CHANNEL_IN_4, APP_NAME);
        ConnectorProperties outputChannel4 = simpleOutgoingChannel(PlaintextTests.connectionProperties(), KafkaMultipleChannelMessageBean.CHANNEL_OUT_4);

        ConnectorProperties inputChannel5 = simpleIncomingChannel(PlaintextTests.connectionProperties(), KafkaMultipleChannelMessageBean.CHANNEL_IN_5, APP_NAME);
        ConnectorProperties outputChannel5 = simpleOutgoingChannel(PlaintextTests.connectionProperties(), KafkaMultipleChannelMessageBean.CHANNEL_OUT_5);

        ConnectorProperties inputChannel6 = simpleIncomingChannel(PlaintextTests.connectionProperties(), KafkaMultipleChannelMessageBean.CHANNEL_IN_6, APP_NAME);
        ConnectorProperties outputChannel6 = simpleOutgoingChannel(PlaintextTests.connectionProperties(), KafkaMultipleChannelMessageBean.CHANNEL_OUT_6);

        ConnectorProperties inputChannel7 = simpleIncomingChannel(PlaintextTests.connectionProperties(), KafkaMultipleChannelMessageBean.CHANNEL_IN_7, APP_NAME);
        ConnectorProperties outputChannel7 = simpleOutgoingChannel(PlaintextTests.connectionProperties(), KafkaMultipleChannelMessageBean.CHANNEL_OUT_7);

        PropertiesAsset config = new PropertiesAsset()
                .include(inputChannel1)
                .include(outputChannel1)
                .include(inputChannel2)
                .include(outputChannel2)
                .include(inputChannel3)
                .include(outputChannel3)
                .include(inputChannel4)
                .include(outputChannel4)
                .include(inputChannel5)
                .include(outputChannel5)
                .include(inputChannel6)
                .include(outputChannel6)
                .include(inputChannel7)
                .include(outputChannel7);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                .addPackage(KafkaMultipleChannelMessageBean.class.getPackage())
                .addAsResource(config, "META-INF/microprofile-config.properties");

        KafkaUtils.addKafkaTestFramework(war, PlaintextTests.connectionProperties());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.setJvmOptions(Arrays.asList("-Dcom.ibm.ws.beta.edition=true"));

        server.startServer();

        kafkaTestClient = new KafkaTestClient(PlaintextTests.connectionProperties());
    }

    @Test
    public void MultipleConnectorSingleGroupIdTest() throws Exception{
        for(int i=0;i<1000;i++){
            int num = ThreadLocalRandom.current().nextInt(0,6);
            switch(num){
                case 0:
                    writeMessages(KafkaMultipleChannelMessageBean.CHANNEL_IN_1, KafkaMultipleChannelMessageBean.CHANNEL_OUT_1);
                    break;
                case 1:
                    writeMessages(KafkaMultipleChannelMessageBean.CHANNEL_IN_2, KafkaMultipleChannelMessageBean.CHANNEL_OUT_2);
                    break;
                case 2:
                    writeMessages(KafkaMultipleChannelMessageBean.CHANNEL_IN_3, KafkaMultipleChannelMessageBean.CHANNEL_OUT_3);
                    break;
                case 3:
                    writeMessages(KafkaMultipleChannelMessageBean.CHANNEL_IN_4, KafkaMultipleChannelMessageBean.CHANNEL_OUT_4);
                    break;
                case 4:
                    writeMessages(KafkaMultipleChannelMessageBean.CHANNEL_IN_5, KafkaMultipleChannelMessageBean.CHANNEL_OUT_5);
                    break;
                case 5:
                    writeMessages(KafkaMultipleChannelMessageBean.CHANNEL_IN_6, KafkaMultipleChannelMessageBean.CHANNEL_OUT_6);
                    break;
                case 6:
                    writeMessages(KafkaMultipleChannelMessageBean.CHANNEL_IN_7, KafkaMultipleChannelMessageBean.CHANNEL_OUT_7);
                    break;
            }
        }
    }

    public void writeMessages(String channelIn, String channelOut) throws Exception{
        try(KafkaWriter<String, String> testWriter = kafkaTestClient.writerFor(channelIn);
            KafkaReader<String, String> testReader = kafkaTestClient.readerFor(channelOut)){
            testWriter.sendMessage("abc-" + channelOut);
            testWriter.sendMessage("def-" + channelOut);
            testWriter.sendMessage("ghi-" + channelOut);
            testWriter.sendMessage("jkl-" + channelOut);
            testWriter.sendMessage("mno-" + channelOut);

            List<String> messages = testReader.assertReadMessages(5, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
            assertThat(messages, contains(channelIn + "-abc-" + channelOut, channelIn + "-def-" + channelOut , channelIn + "-ghi-" + channelOut, channelIn + "-jkl-" + channelOut, channelIn + "-mno-" + channelOut));
        }
    }

    @After
    public void teardown() throws Exception {
        try {
            KafkaUtils.kafkaStopServer(server);
        } finally {
            KafkaUtils.deleteKafkaTopics(PlaintextTests.getAdminClient());
        }
    }
}
