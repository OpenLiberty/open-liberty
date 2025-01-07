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

/**
 * Enumerates possible states for {@link Chain} implementations.
 */
public enum ChainState {
    /**
     * The chain has just been constructed or initialized but not yet set up
     * in the framework.
     */
    UNINITIALIZED(0),

    /**
     * The chain has been destroyed or removed entirely.
     */
    DESTROYED(1),

    /**
     * The chain is initialized but not yet started.
     */
    INITIALIZED(2),

    /**
     * The chain is fully stopped, not actively listening.
     */
    STOPPED(3),

    /**
     * The chain is quiesced but not fully stopped (some frameworks
     * differentiate "quiescing" from "stopped".
     */
    QUIESCED(4),

    /**
     * The chain is fully started and actively listening.
     */
    STARTED(5),

    /**
     * The chain is in the process of starting, typically after an update call.
     */
    STARTING(6),

    /**
     * The chain is in the process of stopping.
     */
    STOPPING(7);

    private final int value;

    ChainState(int val) {
        this.value = val;
    }

    /**
     * @return An integer code associated with the state.
     */
    public int value() {
        return value;
    }

    /**
     * @return Returns the enum instance corresponding to the given integer code,
     * or {@code null} if none matches.
     */
    public static ChainState fromValue(int val) {
        for (ChainState st : values()) {
            if (st.value == val) {
                return st;
            }
        }
        return null;
    }

}
