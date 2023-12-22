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

/**
 * Creates {@link RMAsyncProvider} instances.
 * <p>
 * An instance of this interface should be obtained by looking up the singleton service in OSGi.
 */
public interface RMAsyncProviderFactory {

    /**
     * Creates an {@code RMAsyncProvider} which uses the named context service to capture thread context
     *
     * @param contextServiceName the context service name, or {@code null} to use the default context service
     * @param channelName the channel the async provider is used for. Used in error messages.
     * @return the async provider
     */
    RMAsyncProvider getAsyncProvider(String contextServiceName, String channelName);
}
