/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.queuing.internal;

import java.io.IOException;

import com.ibm.ws.zos.channel.local.LocalCommClientConnHandle;
import com.ibm.ws.zos.channel.local.queuing.BlackQueueReadyCallback;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequestType;


/**
 * An attempt to register a callback with the BlackQueueDemultiplexor for 
 * a given connection failed because the connection is closed or is in the
 * process of closing. 
 * 
 */
public class ConnectionClosedException extends IOException {

    /**
     * @param requestType
     * @param lhdlPointer
     * @param callback
     */
    public ConnectionClosedException(NativeWorkRequestType requestType, 
                                     LocalCommClientConnHandle clientConnHandle,
                                     BlackQueueReadyCallback callback) {
        super("Failed to register callback. The connection is closed or closing. For requestType " + requestType + " and clientConnHandle " + clientConnHandle + ".  Rejected callback: " + callback.toString());
    }
}
