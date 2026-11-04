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

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.zos.channel.local.queuing.BlackQueueReadyCallback;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequest;

/**
 * BlackQueue callback for handling FFDC work requests.
 * 
 * Logs an FFDC record containing the data from the work request.
 */
public class FFDCCallback implements BlackQueueReadyCallback {
    
    /**
     * Log an FFDC record for the work request.
     */
    @Override
    public void blackQueueReady(NativeWorkRequest nativeWorkRequest) {

        FFDCFilter.processException(buildException(nativeWorkRequest), 
                                    "FFDCCallback", 
                                    String.valueOf(nativeWorkRequest.getTP()),
                                    nativeWorkRequest);

    }
    
    /**
     * @return a dummy exception for the FFDC record.
     */
    protected Exception buildException(NativeWorkRequest nativeWorkRequest) {
        return new Exception("Native error at TP(x" + Integer.toHexString(nativeWorkRequest.getTP()) + "): " 
                             + CodepageUtils.ebcdicBytesToString(nativeWorkRequest.getFFDCRawData())
                             + " (Note: the stack trace for this exception is irrelevant)");
    }

    /**
     * This method will never be called.
     */
    @Override
    public void cancel(Exception e) { }

}
