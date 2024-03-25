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

import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Provides access to the current {@code MessageAccess}
 */
@Component
public class MessageAccessProvider {

    /**
     * A default MessageAccess implementation which does nothing beyond what's available in the 1.0 API
     */
    private static final MessageAccess DEFAULT = new MessageAccess() {

        @Override
        public void nack(Message<?> message, Throwable exception) {
            // No-op
        }

        @Override
        public <T> Message<T> create(T payload, Supplier<CompletionStage<Void>> ackFunction, Function<Throwable, CompletionStage<Void>> nackFunction) {
            // Ignore nack function
            return Message.of(payload, ackFunction);
        }
    };

    private static volatile MessageAccess instance;

    /**
     * Returns the {@link MessageAccess} singleton
     *
     * @return the MessageAccess
     */
    public static MessageAccess getMessageAccess() {
        MessageAccess result = instance;
        if (instance == null) {
            result = DEFAULT;
        }
        return result;
    }

    @Reference(policy = DYNAMIC)
    protected void setMessageAccess(MessageAccess messageAccess) {
        instance = messageAccess;
    }

    protected void unsetMessageAccess(MessageAccess messageAccess) {
        if (instance == messageAccess) {
            instance = null;
        }
    }

}
