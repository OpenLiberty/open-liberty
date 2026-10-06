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
 * Fired by {@link ReadFlowHandler} when an async body purge begins — the
 * response has completed but the request body has not yet been fully received.
 * <p>Singleton — always reference {@link #INSTANCE}.
 */
public final class PurgeStartedEvent {

    public static final PurgeStartedEvent INSTANCE = new PurgeStartedEvent();

    private PurgeStartedEvent() {}

    @Override
    public String toString() {
        return "PURGE_STARTED";
    }
}
