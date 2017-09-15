/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

import java.io.IOException;

import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

public class ItemForCompletion {

    public enum ItemState {
        NOTHING_NEEDED,
        READ_COMPLETE_READY,
        READ_ERROR_READY,
        WRITE_COMPLETE_READY,
        WRITE_ERROR_READY,
    }

    ItemState itemState = ItemState.NOTHING_NEEDED;
    VirtualConnection virtualConn = null;
    TCPReadRequestContext tcpReadContext = null;
    TCPWriteRequestContext tcpWriteContext = null;
    IOException errorException = null;

    public ItemForCompletion() {
        itemState = ItemState.NOTHING_NEEDED;
    }

    public void setReadComplete(VirtualConnection vc, TCPReadRequestContext rrc) {
        itemState = ItemState.READ_COMPLETE_READY;
        virtualConn = vc;
        tcpReadContext = rrc;
    }

    public void setReadError(VirtualConnection vc, TCPReadRequestContext rrc, IOException ioe) {
        itemState = ItemState.READ_ERROR_READY;
        virtualConn = vc;
        tcpReadContext = rrc;
        errorException = ioe;
    }

    public void setWriteComplete(VirtualConnection vc, TCPWriteRequestContext twc) {
        itemState = ItemState.WRITE_COMPLETE_READY;
        virtualConn = vc;
        tcpWriteContext = twc;
    }

    public void setWriteError(VirtualConnection vc, TCPWriteRequestContext twc, IOException ioe) {
        itemState = ItemState.WRITE_ERROR_READY;
        virtualConn = vc;
        tcpWriteContext = twc;
        errorException = ioe;
    }

    public void reset() {
        itemState = ItemState.NOTHING_NEEDED;
        virtualConn = null;
        tcpWriteContext = null;
        errorException = null;
        tcpReadContext = null;
    }

    public ItemState getItemState() {
        return itemState;
    }

    public VirtualConnection getVC() {
        return virtualConn;
    }

    public TCPReadRequestContext getTCPReadContext() {
        return tcpReadContext;
    }

    public TCPWriteRequestContext getTCPWriteContext() {
        return tcpWriteContext;
    }

    public IOException getIOException() {
        return errorException;
    }

}
