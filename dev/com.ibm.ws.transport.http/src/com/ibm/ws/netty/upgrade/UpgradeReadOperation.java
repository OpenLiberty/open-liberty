/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.netty.upgrade;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

import io.netty.util.concurrent.ScheduledFuture;

/**
 * State owned by one upgraded asynchronous read.
 */
final class UpgradeReadOperation {
    final TCPReadCompletedCallback callback;
    final VirtualConnection virtualConnection;
    final TCPReadRequestContext readContext;
    final WsByteBuffer[] buffers;
    final int minimumBytes;

    private volatile ScheduledFuture<?> timeout;

    UpgradeReadOperation(TCPReadCompletedCallback callback,
                         VirtualConnection virtualConnection,
                         TCPReadRequestContext readContext,
                         long minimumBytes) {
        this.callback = callback;
        this.virtualConnection = virtualConnection;
        this.readContext = readContext;
        WsByteBuffer[] currentBuffers = readContext == null ? null : readContext.getBuffers();
        this.buffers = currentBuffers == null ? null : currentBuffers.clone();
        this.minimumBytes = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, minimumBytes));
    }

    void setTimeout(ScheduledFuture<?> timeout) {
        this.timeout = timeout;
    }

    void cancelTimeout() {
        ScheduledFuture<?> currentTimeout = timeout;
        timeout = null;
        if (currentTimeout != null) {
            currentTimeout.cancel(false);
        }
    }

    void restoreReadContext() {
        if (readContext != null) {
            readContext.setBuffers(buffers);
        }
    }
}
