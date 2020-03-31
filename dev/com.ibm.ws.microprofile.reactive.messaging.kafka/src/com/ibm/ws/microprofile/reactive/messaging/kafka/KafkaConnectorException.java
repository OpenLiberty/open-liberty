/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka;

/**
 * General exception for problems which originate in the Kafka connector
 */
public class KafkaConnectorException extends Exception {

    private static final long serialVersionUID = 1L;

    public KafkaConnectorException(String message, Throwable cause) {
        super(message, cause);
    }

    public KafkaConnectorException(String message) {
        super(message);
    }

    public KafkaConnectorException(Throwable cause) {
        super(cause);
    }

}
