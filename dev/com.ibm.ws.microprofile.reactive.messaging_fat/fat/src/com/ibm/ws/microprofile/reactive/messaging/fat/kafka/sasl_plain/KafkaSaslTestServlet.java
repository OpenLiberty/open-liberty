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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.sasl_plain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.apps.kafka.BasicMessagingBean;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;

@WebServlet("/KafkaTlsTestServlet")
@ApplicationScoped
public class KafkaSaslTestServlet extends AbstractKafkaTestServlet {

    public static final String TRUSTSTORE_PASSWORD_PROPERTY = "kafka.truststore.password";
    public static final String TEST_USER_PROPERTY = "kafka.test.user";
    public static final String TEST_SECRET_PROPERTY = "kafka.test.secret";
    public static final String TRUSTSTORE_FILENAME = "kafkaSaslKey.jks";

    @Inject
    @ConfigProperty(name = TRUSTSTORE_PASSWORD_PROPERTY)
    private String trustStorePassword;

    @Inject
    @ConfigProperty(name = TEST_USER_PROPERTY)
    private String testUser;

    @Inject
    @ConfigProperty(name = TEST_SECRET_PROPERTY)
    private String testSecret;

    private static final long serialVersionUID = 1L;

    @Test
    public void testTls() {
        Map<String, Object> sslConfig = new HashMap<>();
        sslConfig.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, TRUSTSTORE_FILENAME);
        sslConfig.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, trustStorePassword);
        sslConfig.put("security.protocol", "SASL_SSL");
        sslConfig.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        sslConfig.put(SaslConfigs.SASL_JAAS_CONFIG,
                      "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + testUser + "\" password=\"" + testSecret + "\";");

        KafkaReader<String, String> reader = kafkaTestClient.readerFor(sslConfig, BasicMessagingBean.CHANNEL_OUT);
        KafkaWriter<String, String> writer = kafkaTestClient.writerFor(sslConfig, BasicMessagingBean.CHANNEL_IN);

        writer.sendMessage("abc");
        writer.sendMessage("xyz");

        List<String> msgs = reader.assertReadMessages(2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);

        assertThat(msgs, contains("cba", "zyx"));
    }

}
