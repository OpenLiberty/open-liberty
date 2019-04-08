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
package com.ibm.ws.microprofile.reactive.streams.test.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.security.Principal;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.junit.Test;

import componenttest.app.FATServlet;

/**
 *
 */
@WebServlet("/ReactiveStreamsContextTest")
public class ReactiveStreamsContextTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    Principal principle;

    @Inject
    IntegerSubscriber integerSubscriber;

    /*
     * Another simple test that plumbs a list to Subscriber
     */
    @Test
    public void helloReactiveWorld() throws Throwable {

        assertNotNull("Servlet Principle is null", principle);
        System.out.println(principle.toString());

        PublisherBuilder<Integer> data = ReactiveStreams.of(1, 2, 3, 4, 5);
        ProcessorBuilder<Integer, Integer> filter = ReactiveStreams.<Integer> builder().dropWhile(t -> t < 3);

        data.via(filter).to(integerSubscriber).run();

        while (!integerSubscriber.isComplete()) {
            Thread.sleep(100);
        }

        Throwable error = integerSubscriber.getError();
        if (error != null) {
            throw error;
        }

        ArrayList<Integer> results = integerSubscriber.getResults();
        assertEquals(3, results.size());
        for (int i = 0; i < 3; i++) {
            int res = results.get(i);
            assertEquals(i + 3, res);
        }
    }

}
