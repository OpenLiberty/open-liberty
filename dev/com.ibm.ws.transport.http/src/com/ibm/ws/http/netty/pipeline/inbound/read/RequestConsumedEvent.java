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
 * Fired by {@link ReadFlowHandler} when the current request body has been
 * fully consumed (or purged) and the connection is ready for the next request.
 * <p>Singleton — always reference {@link #INSTANCE}.
 */
public final class RequestConsumedEvent {

    public static final RequestConsumedEvent INSTANCE = new RequestConsumedEvent();

    private RequestConsumedEvent() {}

    @Override
    public String toString() {
        return "REQUEST_CONSUMED";
    }
}
