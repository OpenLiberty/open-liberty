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
import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

/**
 * This class acts as a datastore for the Publisher, it is ApplicationScoped
 * in order to retain the data across calls and users.
 */
@ApplicationScoped
public class SessionScopedStateBean implements Iterable<String> {

    private ArrayList<String> messages;

    @PostConstruct
    void init() {
        messages = new ArrayList<>();
    }

    /**
     * @return the pb
     */
    public PublisherBuilder<String> getPublisherBuilder() {
        return ReactiveStreams.fromIterable(messages);
    }

    /**
     * @param message
     */
    public void add(String message) {
        messages.add(message);
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<String> iterator() {
        return messages.iterator();
    }

}
