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

/**
 * This is a dummy version SmallRye's WorkerPoolRegistry which does nothing ... we do not expect it to be called
 * https://github.com/smallrye/smallrye-reactive-messaging/blob/4.7.0/smallrye-reactive-messaging-provider/src/main/java/io/smallrye/reactive/messaging/providers/connectors/WorkerPoolRegistry.java
 */
@ApplicationScoped
public class WorkerPoolRegistry {

}
