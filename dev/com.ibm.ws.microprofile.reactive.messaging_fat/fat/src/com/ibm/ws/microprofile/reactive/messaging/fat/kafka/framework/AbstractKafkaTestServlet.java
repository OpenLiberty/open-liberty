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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import componenttest.app.FATServlet;

/**
 * Abstract test servlet for Kafka tests
 * <p>
 * Provides a {@link #kafkaTestClient} and calls {@link KafkaTestClient#cleanUp()} after every test
 * <p>
 * Uses an MP Config property named {@value #KAFKA_BOOTSTRAP_PROPERTY} as the value of {@code bootstrap.servers} for all created Kafka clients.
 */
public class AbstractKafkaTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    public static final String KAFKA_BOOTSTRAP_PROPERTY = "fat.test.kafkaBootstrap";

    @Inject
    @ConfigProperty(name = KAFKA_BOOTSTRAP_PROPERTY)
    private String kafkaBootstrap;

    protected KafkaTestClient kafkaTestClient;

    @PostConstruct
    private void setup() {
        kafkaTestClient = new KafkaTestClient(getKafkaBootstrap());
    }

    @Override
    protected void before() throws Exception {
        super.before();
    }

    @Override
    protected void after() throws Exception {
        super.after();
        kafkaTestClient.cleanUp();
    }

    protected String getKafkaBootstrap() {
        return kafkaBootstrap;
    }
}
