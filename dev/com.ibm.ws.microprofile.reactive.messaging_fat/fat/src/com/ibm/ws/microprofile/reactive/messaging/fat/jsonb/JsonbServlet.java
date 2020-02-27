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
package com.ibm.ws.microprofile.reactive.messaging.fat.jsonb;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.util.List;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/jsonbTest")
public class JsonbServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private JsonbDeliveryBean deliveryBean;

    @Inject
    private JsonbReceptionBean receptionBean;

    @Resource
    private ManagedExecutorService executor;

    @Test
    public void testJsonb() throws InterruptedException {
        TestData t = new TestData();
        t.a = "test";
        t.b = 42;
        executor.submit(() -> deliveryBean.sendMessage(t));

        List<Message<TestData>> receivedMessages = receptionBean.assertReceivedMessages(1, Duration.ofSeconds(10));
        TestData received = receivedMessages.get(0).getPayload();

        assertEquals(t, received);
    }
}
