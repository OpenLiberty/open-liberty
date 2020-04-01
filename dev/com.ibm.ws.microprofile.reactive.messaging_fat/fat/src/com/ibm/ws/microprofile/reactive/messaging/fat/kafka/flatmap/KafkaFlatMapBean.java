/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.flatmap;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment.Strategy;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

/**
 *
 */
@ApplicationScoped
public class KafkaFlatMapBean {

    @Incoming(KafkaFlatMapServlet.IN_TOPIC)
    @Outgoing(KafkaFlatMapServlet.OUT_TOPIC)
    @Acknowledgment(Strategy.MANUAL)
    public PublisherBuilder<Message<String>> filterIt(Message<String> in) {
        if (in.getPayload().length() % 2 == 0) {
            in.ack(); // All processing of this message is done, ack it now
            return ReactiveStreams.empty();
        } else {
            // This message will be sent on, create a new message which acknowledges the incoming message when it is acked
            return ReactiveStreams.of(Message.of(in.getPayload(), () -> in.ack()));
        }
    }

}
