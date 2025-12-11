/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.apps.simple;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

@ApplicationScoped
public class SimpleReactiveMessagingBean {

    private final List<String> results = new ArrayList<>();

    @Outgoing("source")
    public PublisherBuilder<String> source() {
        return ReactiveStreams.of("Length 8", "Length 9!", "Length 10!", "Length 11!!", "Length 12!!!");
    }

    @Incoming("source")
    @Outgoing("uppercased")
    public String toUpperCase(String payload) {
        return payload.toUpperCase();
    }

    @Incoming("uppercased")
    @Outgoing("filtered")
    public PublisherBuilder<String> filter(PublisherBuilder<String> input) {
        return input.filter(item -> item.length() > 9);
    }

    @Incoming("filtered")
    public void sink(String word) {
        System.out.println(">> " + word);
        synchronized (results) {
            results.add(word);
            results.notifyAll();
        }
    }

    /**
     *
     */
    public List<String> getResults() {
        return results;
    }

}
