/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.internal.netty.protocol;

import com.ibm.ws.http.netty.NettyHttpConstants.ProtocolName;
import com.ibm.ws.http.netty.ProtocolState.ProtocolSource;

/** Carries a connection protocol change through the pipeline. */
public final class ProtocolChangedEvent {
    private final ProtocolName previous;
    private final ProtocolName current;
    private final ProtocolSource source;

    public ProtocolChangedEvent(ProtocolName previous, ProtocolName current, ProtocolSource source) {
        this.previous = previous;
        this.current = current;
        this.source = source;
    }

    public ProtocolName previous() {
        return previous;
    }

    public ProtocolName current() {
        return current;
    }

    public ProtocolSource source() {
        return source;
    }
}
