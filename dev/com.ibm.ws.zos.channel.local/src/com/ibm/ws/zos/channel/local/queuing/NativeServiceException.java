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
package com.ibm.ws.zos.channel.local.queuing;

import com.ibm.ws.zos.channel.local.queuing.internal.LocalCommServiceResults;

/**
 * Failures that occur on the native side of the local comm channel
 * are wrapped with this class.
 * 
 * The class contains a reference to NativeServiceResult, which contains
 * the return codes and reason codes for the native method that failed.
 */
public class NativeServiceException extends Exception {
    
	private static final long serialVersionUID = 4551852984982361272L;
	private NativeServiceResult nativeServiceResult;

    public NativeServiceException(NativeServiceResult nativeServiceResult) {
        super(nativeServiceResult.toString());
        this.nativeServiceResult = nativeServiceResult;
    }
    
    public NativeServiceException(LocalCommServiceResults localCommServiceResults) {
        super(localCommServiceResults.toString());
    }
    
    NativeServiceResult getServiceResult() {
    	return this.nativeServiceResult;
    }
}
