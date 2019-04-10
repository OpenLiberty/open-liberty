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
package com.ibm.ws.microprofile.reactive.streams.test.jaxrs;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.streams.operators.CompletionRunner;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@ApplicationScoped
public class StorageService extends AbstractAsyncPublisher<String> implements Subscriber<String> {

    @Inject
    private SimpleAsyncPublisher publisher;

    private final List<String> messages = new ArrayList<>();

    private Subscription subscription;

    private boolean init = false;

    public List<String> listMessages() {
        System.out.println(messages);
        return messages;
    }

    @Override
    public void subscribe(Subscriber<? super String> arg0) {
        super.subscribe(arg0);
        for (String msg : messages) {
            publish(msg);
        }
    }

    @PostConstruct
    public void init() {
        if (!init) {
            publisher.toString();
            PublisherBuilder<String> pBuilder = ReactiveStreams.fromPublisher(publisher);
            SubscriberBuilder<String, Void> sBuilder = ReactiveStreams.fromSubscriber(this);
            CompletionRunner<Void> runner = pBuilder.to(sBuilder);
            runner.run();
            init = true;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onComplete() {
        System.out.println("onComplete");
    }

    /** {@inheritDoc} */
    @Override
    public void onError(Throwable arg0) {
        System.out.println("onError: " + arg0);
        arg0.printStackTrace();
    }

    /** {@inheritDoc} */
    @Override
    public void onNext(String message) {
        System.out.println("MESSAGE: " + message);
        this.messages.add(message);
        publish(message);
        this.subscription.request(1);
    }

    /** {@inheritDoc} */
    @Override
    public void onSubscribe(Subscription arg0) {
        this.subscription = arg0;
        this.subscription.request(1);
    }
}
