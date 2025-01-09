/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.http.channel;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a common interface for managing HTTP or HTTPS chain lifecycles
 * in a predictable and controlled manner.
 */
public interface Chain {

    /**
     * Indicates whether this chain manages HTTPS.
     */
    boolean isHttps();

    /**
     * Moves the chain from a "disabled" state into an "enabled" state.
     */
    void enable();

    /**
     * Moves the chain from an "enabled" state into a "disabled" state.
     */
    void disable();

    /**
     * Applies new configuration to the chain. If the chain is enabled
     * and the new configuration differs in a way that requires a rebind
     * (port, host, etc.), it should stop and then attempt to start with
     * the new configuration.
     * 
     * Configuration will be pulled from the associated {@link HttpEndpointImpl}
     *
     * @param hostname the new resolved hostname
     */
    void update(String hostname);

    /**
     * Fully stops the chain, releasing resources. Does not necessarily
     * disable it; the chain can be updated later if still enabled.
     */
    void stop();

    /**
     * Retrieves the current state of the chain as an integer.
     *
     * @return integer representing the chain’s state
     */
    AtomicReference<ChainState> state();

    /**
     * Returns the active port the chain is bound to, or -1 if not bound.
     */
    default int activePort() {
        return -1;
    }

    /**
     * @return Returns the active host the chain resolved to.
     */
    String activeHost();

    /**
     * Returns the currently stored chain config if available, or null
     * if uninitialized.
     */
    default ChainConfiguration config() {
        return null;
    }
}
