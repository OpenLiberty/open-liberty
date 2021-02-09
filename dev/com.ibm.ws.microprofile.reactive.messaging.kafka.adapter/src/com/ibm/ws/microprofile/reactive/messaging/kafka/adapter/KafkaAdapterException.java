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
package com.ibm.ws.microprofile.reactive.messaging.kafka.adapter;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
public class KafkaAdapterException extends RuntimeException {

    /**  */
    private static final long serialVersionUID = 8401273064700931173L;

    /**
     * @param cause
     */
    public KafkaAdapterException(Exception cause) {
        super(buildMessage(cause), cause);
    }

    /**
     * @param message
     */
    public KafkaAdapterException(String message) {
        super(message);
    }

    /**
     * Build a useful message based on the cause chain from Kafka
     * <p>
     * In general, the kafka client gives a helpful chain of exceptions explaining what happened and when. However, for liberty log messages, we want to capture the exception cause
     * chain in a single message.
     *
     * @param cause the cause of this exception
     * @return the message for this exception
     */
    private static String buildMessage(Throwable cause) {
        // Usually the first exception is an InvocationTargetException which isn't helpful in the error message
        if (cause instanceof InvocationTargetException) {
            cause = cause.getCause();
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        while (cause != null) {
            if (!first) {
                sb.append(": ");
            }
            first = false;

            sb.append(cause.toString());
            cause = cause.getCause();
        }

        return sb.toString();
    }

}
