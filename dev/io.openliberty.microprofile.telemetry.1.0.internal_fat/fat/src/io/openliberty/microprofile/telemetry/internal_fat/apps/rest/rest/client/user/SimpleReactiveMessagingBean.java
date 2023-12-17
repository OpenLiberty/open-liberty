/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.rest.rest.client.user;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class SimpleReactiveMessagingBean {

    @Inject
    @RestClient
    RestClientInterface restClient;

    private final List<String> results = new ArrayList<>();

    @Outgoing("source")
    public PublisherBuilder<String> source() {
        return ReactiveStreams.of("Length 8", "Length 9!", "Length 10!", "Length 11!!", "Length 12!!!");
    }

    @Incoming("source")
    @Outgoing("uppercased")
    @WithSpan
    public String toUpperCase(String payload) {
        Span span = Span.current();
        Response restResponse = restClient.postStringReturnUpper(payload);
        MessageAndRestClientTestServlet.recordSpan(span);
        return restResponse.readEntity(String.class);
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

    public List<String> getResults() {
        return results;
    }

}
