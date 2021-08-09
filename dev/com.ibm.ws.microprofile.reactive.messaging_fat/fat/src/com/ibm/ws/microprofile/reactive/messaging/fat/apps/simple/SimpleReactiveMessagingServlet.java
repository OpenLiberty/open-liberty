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
package com.ibm.ws.microprofile.reactive.messaging.fat.apps.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/")
public class SimpleReactiveMessagingServlet extends FATServlet {

    private static final long MAX_WAIT = 2000; //millis
    private static final long WAIT_INTERVAL = 50; //millis

    @Inject
    private SimpleReactiveMessagingBean reactiveBean;

    @Test
    public void simpleReactiveMesagingTest() throws InterruptedException {
        List<String> results = reactiveBean.getResults();
        long maxNanos = System.nanoTime() + millisToNanos(MAX_WAIT);
        synchronized (results) {
            while (results.size() < 3 && nanoTimeRemaining(maxNanos)) {
                results.wait(WAIT_INTERVAL);
            }
            assertEquals(3, results.size());
            assertFalse(results.contains("LENGTH 8"));
            assertFalse(results.contains("LENGTH 9!"));
            assertTrue(results.contains("LENGTH 10!"));
            assertTrue(results.contains("LENGTH 11!!"));
            assertTrue(results.contains("LENGTH 12!!!"));
        }
    }

    private static final long millisToNanos(long millis) {
        return millis * 1000 * 1000;
    }

    private static final boolean nanoTimeRemaining(long maxNanos) {
        return (maxNanos - System.nanoTime()) > 0;
    }

}
