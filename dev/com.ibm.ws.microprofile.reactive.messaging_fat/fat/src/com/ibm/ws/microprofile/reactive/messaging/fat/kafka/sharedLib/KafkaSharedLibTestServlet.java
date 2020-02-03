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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.sharedLib;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;

@WebServlet("/KafkaSharedLibTestServlet")
@ApplicationScoped
public class KafkaSharedLibTestServlet extends AbstractKafkaTestServlet {

    private static final long serialVersionUID = 1L;

    @Test
    public void testSharedLibrary() {
        KafkaReader<String, String> reader = kafkaTestClient.readerFor(SharedLibMessagingBean.CHANNEL_OUT);
        KafkaWriter<String, String> writer = kafkaTestClient.writerFor(SharedLibMessagingBean.CHANNEL_IN);

        writer.sendMessage("123");
        writer.sendMessage("xyz");

        List<String> msgs = reader.assertReadMessages(2, KafkaTestConstants.DEFAULT_KAFKA_TIMEOUT);

        assertThat(msgs, contains("321", "zyx"));
    }

}
