/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.apps.startup;

import java.util.concurrent.CompletionStage;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractReceptionBean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TestMessageOnStartupKafkaConsumer extends AbstractReceptionBean<String> {

    @Incoming("TestMessageOnStartup")
    @Override
    public CompletionStage<Void> receiveMessage(Message<String> message) {
        System.out.println("Message recievd " + message.getPayload());

        InitialContext ctx;
        try {
            ctx = new InitialContext();

            ExampleDependency exampleDependency = (ExampleDependency) ctx
                            .lookup("java:module/ExampleDependency");

            exampleDependency.toString();

        } catch (NamingException e) {
            throw new RuntimeException("Failed to lookup the EJB bean " + e);
        }

        return super.receiveMessage(message);
    }

}
