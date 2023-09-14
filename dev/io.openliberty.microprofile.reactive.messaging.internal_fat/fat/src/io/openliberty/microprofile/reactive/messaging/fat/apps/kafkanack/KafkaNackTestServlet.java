/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.apps.kafkanack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/NackTest")
public class KafkaNackTestServlet extends FATServlet {

    @Inject
    private KafkaNackTestDeliveryBean deliveryBean;

    /**
     * Check that if a message sent to Kafka is not sent, it is nacked
     */
    @Test
    public void testUndeliveredMessageIsNacked() {
        CompletableFuture<Void> cf = deliveryBean.sendMessage("test");
        try {
            cf.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            // Expected
            // Assert something about the exception?
        } catch (Exception e) {
            throw new AssertionError("Undelivered message was not nacked", e);
        }
    }
}
