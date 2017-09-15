/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.tcpchannel;

import java.io.IOException;

import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * A callback object whose methods are called by the TCPChannel
 * upon the completion (or error) of a writeAsynch request.
 * 
 * @ibm-spi
 */
public interface TCPWriteCompletedCallback {
    /**
     * Called when the request has completeted successfully.
     * 
     * @param vc
     *            vitual connection associated with this request.
     * @param wsc
     *            the TCPWriteRequestContext associated with this request.
     */
    public void complete(VirtualConnection vc, TCPWriteRequestContext wsc);

    /**
     * Called back if an exception occurres while processing the request.
     * The implementer of this interface can then decide how to handle this
     * exception.
     * 
     * @param vc
     *            vitual connection associated with this request.
     * @param wsc
     *            the TCPWriteRequestContext associated with this request.
     * @param ioe
     *            the exception.
     */
    public void error(VirtualConnection vc, TCPWriteRequestContext wsc, IOException ioe);

}
