/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common;

import java.time.Duration;

/**
 *
 */
public class KafkaTestConstants {
    //timeout where operation is usually expected to pass quickly
    public static final Duration DEFAULT_KAFKA_TIMEOUT = Duration.ofSeconds(30);

    //timeout where operation is expected to fail
    public static final Duration EXPECTED_FAILURE_KAFKA_TIMEOUT = Duration.ofSeconds(2);

    //kafka environment setup or shutdown timeout
    public static final Duration KAFKA_ENVIRONMENT_TIMEOUT = Duration.ofSeconds(5);

}
