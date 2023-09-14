/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.internal.interfaces;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.reactive.messaging.Message;

/**
 * Provides access to features of Message which aren't available on the 1.0 API
 * <p>
 * Meant to allow common code to take advantage of new features when running with a new version of the API
 */
public interface MessageAccess {

    /**
     * Create a {@link Message} with the given ack and nack functions
     *
     * @param <T> the payload type
     * @param payload the payload
     * @param ackFunction the ack function
     * @param nackFunction the nack function
     * @return the newly created message
     */
    <T> Message<T> create(T payload, Supplier<CompletionStage<Void>> ackFunction, Function<Throwable, CompletionStage<Void>> nackFunction);

    /**
     * Nack a {@link Message}
     *
     * @param message the message to nack
     * @param exception the exception causing the nack
     */
    void nack(Message<?> message, Throwable exception);
}
