/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.smallrye.reactive.messaging.providers.connectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.AnnotatedType;

/**
 * This is a dummy version SmallRye's WorkerPoolRegistry which does nothing ... we do not expect it to be called
 * https://github.com/smallrye/smallrye-reactive-messaging/blob/4.10.1/smallrye-reactive-messaging-provider/src/main/java/io/smallrye/reactive/messaging/providers/connectors/WorkerPoolRegistry.java
 *
 * Properties are using during the MediatorManager.start() process
 *
 */
@ApplicationScoped
public class WorkerPoolRegistry {
    public static final String WORKER_CONFIG_PREFIX = "smallrye.messaging.worker";
    public static final String WORKER_CONCURRENCY = "max-concurrency";

    public <T> void analyzeWorker(AnnotatedType<T> annotatedType) {}
}
