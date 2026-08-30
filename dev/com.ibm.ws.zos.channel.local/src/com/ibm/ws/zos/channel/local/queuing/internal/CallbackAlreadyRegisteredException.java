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
 * Thrown by BlackQueueDemultiplexor.registerCallback if another callback is already
 * registered for the given requestType and lhdlPointer.
 * 
 */
public class CallbackAlreadyRegisteredException extends IOException {
    
    public CallbackAlreadyRegisteredException(NativeWorkRequestType requestType, 
                                              LocalCommClientConnHandle clientConnHandle, 
                                              BlackQueueReadyCallback currentCallback,
                                              BlackQueueReadyCallback rejectedCallback) {
        super("Callback already registered for requestType " + requestType + " and clientConnHandle " + clientConnHandle +
              ". Current callback: " + currentCallback.toString() + "; Rejected callback: " + rejectedCallback.toString());
    }

}
