/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.concurrent.internal.compat;

import java.util.concurrent.Flow.Processor;
import java.util.concurrent.Flow.Subscriber;

import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

import io.openliberty.concurrent.internal.compat.impl.ContextualProcessor;
import io.openliberty.concurrent.internal.compat.impl.ContextualSubscriber;

/**
 * Superclass for ContextServiceImpl when using Concurrency 3.1 or higher.
 */
public abstract class ContextServiceBase {
    /**
     * Implemented in ContextServiceImpl
     */
    protected abstract ThreadContextDescriptor captureThreadContext();

    public <T> Subscriber<T> contextualSubscriber(Subscriber<T> subscriber) {
        if (subscriber instanceof ContextualProcessor)
            throw new IllegalArgumentException(ContextualProcessor.class.getSimpleName());
        if (subscriber instanceof ContextualSubscriber)
            throw new IllegalArgumentException(ContextualSubscriber.class.getSimpleName());

        ThreadContextDescriptor contextDescriptor = captureThreadContext();
        return new ContextualSubscriber<T>(contextDescriptor, subscriber);
    }

    public <T, R> Processor<T, R> contextualProcessor(Processor<T, R> processor) {
        if (processor instanceof ContextualProcessor)
            throw new IllegalArgumentException(ContextualProcessor.class.getSimpleName());

        ThreadContextDescriptor contextDescriptor = captureThreadContext();
        return new ContextualProcessor<T, R>(contextDescriptor, processor);
    }
}