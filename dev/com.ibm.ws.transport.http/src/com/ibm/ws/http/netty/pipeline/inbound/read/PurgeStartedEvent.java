/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.inbound.read;

/**
 * Pipeline user event fired by {@link ReadFlowHandler} when an async body
 * purge begins — i.e. the response has completed but the request body has
 * not yet been fully received from the wire.
 *
 * <p>{@link io.openliberty.http.netty.timeout.TimeoutHandler} listens for
 * this event and arms the {@code READ} phase so that each arriving body
 * fragment is governed by the read timeout, matching Channel Framework's
 * behaviour where every body buffer read during purge uses
 * {@code readTimeout} rather than {@code persistTimeout}.
 *
 * <p>Singleton — always reference {@link #INSTANCE}.
 */
public final class PurgeStartedEvent {

    /** Singleton — allocate once, reuse across all exchanges. */
    public static final PurgeStartedEvent INSTANCE = new PurgeStartedEvent();

    private PurgeStartedEvent() {}

    @Override
    public String toString() {
        return "PURGE_STARTED";
    }
}
