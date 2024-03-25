/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.context.custom;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

import java.util.List;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/KafkaCustomContextTest")
public class KafkaCustomContextTestServlet extends FATServlet {

    @Inject
    private KafkaTestClient client;

    @Resource(lookup = "java:app/AppName")
    private String appName;

    @Test
    public void testDefaultContextService() throws Exception {
        try (KafkaWriter<String, String> testWriter = client.writerFor(KakfaCustomContextTestBean.DEFAULT_IN);
                        KafkaReader<String, String> testReader = client.readerFor(KakfaCustomContextTestBean.DEFAULT_OUT)) {

            // Check our injection of the app name has worked
            assertThat(appName, not(isEmptyOrNullString()));

            // Send two messages
            testWriter.sendMessage("abc");
            testWriter.sendMessage("def");

            // Check correct context was propagated
            List<String> received = testReader.assertReadMessages(2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
            assertThat(received, contains("abc-" + appName + "-true",
                                          "def-" + appName + "-true"));
        }
    }

    @Test
    public void testPropagateAll() throws Exception {
        try (KafkaWriter<String, String> testWriter = client.writerFor(KakfaCustomContextTestBean.PROPAGATE_ALL_IN);
                        KafkaReader<String, String> testReader = client.readerFor(KakfaCustomContextTestBean.PROPAGATE_ALL_OUT)) {

            // Check our injection of the app name has worked
            assertThat(appName, not(isEmptyOrNullString()));

            // Check our injection of the app name has worked
            assertThat(appName, not(isEmptyOrNullString()));

            // Send two messages
            testWriter.sendMessage("ghi");
            testWriter.sendMessage("jkl");

            // Check correct context was propagated
            List<String> received = testReader.assertReadMessages(2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
            assertThat(received, contains("ghi-" + appName + "-true",
                                          "jkl-" + appName + "-true"));
        }
    }

    @Test
    public void testPropagateNone() throws Exception {
        try (KafkaWriter<String, String> testWriter = client.writerFor(KakfaCustomContextTestBean.PROPAGATE_NONE_IN);
                        KafkaReader<String, String> testReader = client.readerFor(KakfaCustomContextTestBean.PROPAGATE_NONE_OUT)) {

            // Check our injection of the app name has worked
            assertThat(appName, not(isEmptyOrNullString()));

            // Send two messages
            testWriter.sendMessage("mno");
            testWriter.sendMessage("pqr");

            // Check correct context was propagated
            List<String> received = testReader.assertReadMessages(2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
            assertThat(received, contains("mno-noapp-false",
                                          "pqr-noapp-false"));
        }
    }

    @Test
    public void testPropagateAppMetadataOnly() throws Exception {
        try (KafkaWriter<String, String> testWriter = client.writerFor(KakfaCustomContextTestBean.PROPAGATE_APP_IN);
                        KafkaReader<String, String> testReader = client.readerFor(KakfaCustomContextTestBean.PROPAGATE_APP_OUT)) {

            // Check our injection of the app name has worked
            assertThat(appName, not(isEmptyOrNullString()));

            // Send two messages
            testWriter.sendMessage("stu");
            testWriter.sendMessage("vwx");

            // Check correct context was propagated
            List<String> received = testReader.assertReadMessages(2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);
            assertThat(received, contains("stu-" + appName + "-true",
                                          "vwx-" + appName + "-true"));
        }
    }

}
