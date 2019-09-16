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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.tls;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.apache.kafka.common.config.SslConfigs;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.apps.kafka.BasicMessagingBean;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.SimpleKafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.SimpleKafkaWriter;

@WebServlet("/KafkaTlsTestServlet")
@ApplicationScoped
public class KafkaTlsTestServlet extends AbstractKafkaTestServlet {

    public static final String TRUSTSTORE_PASSWORD_PROPERTY = "kafka.truststore.password";
    public static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Inject
    @ConfigProperty(name = TRUSTSTORE_PASSWORD_PROPERTY)
    private String trustStorePassword;

    private static final long serialVersionUID = 1L;

    @Test
    public void testTls() {
        Map<String, Object> sslConfig = new HashMap<>();
        sslConfig.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "kafkakey.jks");
        sslConfig.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, trustStorePassword);
        sslConfig.put("security.protocol", "SSL");

        SimpleKafkaReader<String> reader = kafkaTestClient.readerFor(sslConfig, BasicMessagingBean.CHANNEL_OUT);
        SimpleKafkaWriter<String> writer = kafkaTestClient.writerFor(sslConfig, BasicMessagingBean.CHANNEL_IN);

        writer.sendMessage("abc");
        writer.sendMessage("xyz");

        List<String> msgs = reader.waitForMessages(2, TIMEOUT);

        assertThat(msgs, contains("cba", "zyx"));
    }

}
