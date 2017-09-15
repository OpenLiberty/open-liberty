/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.udpchannel;

import java.io.IOException;

import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * A callback object whose methods are called by the UDPChannel
 * upon the completion (or error) of a writeAsynch request.
 */
public interface UDPWriteCompletedCallback {
    /**
     * Called when the request has completeted successfully.
     * 
     * @param vc associated with this request.
     * @param wsc associated with this request.
     */
    void complete(VirtualConnection vc, UDPWriteRequestContext wsc);

    /**
     * Called back if an exception occurrs while processing the request.
     * The implementer of this interface can then decide how to handle this
     * exception.
     * 
     * @param vc associated with this request.
     * @param wsc associated with this request.
     * @param ioe The exception.
     */
    void error(VirtualConnection vc, UDPWriteRequestContext wsc, IOException ioe);

}
