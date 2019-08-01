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

import java.time.Duration;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractKafkaTestServlet;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.SimpleKafkaReader;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.SimpleKafkaWriter;

@WebServlet("/KafkaSharedLibTestServlet")
@ApplicationScoped
public class KafkaSharedLibTestServlet extends AbstractKafkaTestServlet {

    public static final Duration TIMEOUT = Duration.ofSeconds(30);

    private static final long serialVersionUID = 1L;

    @Test
    public void testSharedLibrary() {
        SimpleKafkaReader<String> reader = kafkaTestClient.readerFor(SharedLibMessagingBean.CHANNEL_OUT);
        SimpleKafkaWriter<String> writer = kafkaTestClient.writerFor(SharedLibMessagingBean.CHANNEL_IN);

        writer.sendMessage("123");
        writer.sendMessage("xyz");

        List<String> msgs = reader.waitForMessages(2, TIMEOUT);

        assertThat(msgs, contains("321", "zyx"));
    }

}
