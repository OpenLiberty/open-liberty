/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal;

import java.io.IOException;

import javax.net.ssl.SSLEngineResult;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 * This class represents a callback that is used when an asynchronous read or write
 * is needed during the SSL handshake.
 */
public class SSLHandshakeIOCallback implements TCPReadCompletedCallback, TCPWriteCompletedCallback {

    /** Trace component for WAS. */
    private static final TraceComponent tc =
                    Tr.register(SSLHandshakeIOCallback.class,
                                SSLChannelConstants.SSL_TRACE_NAME,
                                SSLChannelConstants.SSL_BUNDLE);

    /** Connection using this callback instance */
    private SSLConnectionLink connLink;
    /** Network buffer used */
    private WsByteBuffer netBuffer;
    /** Decrypted network buffer */
    private WsByteBuffer decryptedNetBuffer;
    /** Encrypted user buffer */
    private WsByteBuffer encryptedAppBuffer;
    /** Result from the handshake attempts */
    private SSLEngineResult result;
    /** Callback use with the handshake */
    private SSLHandshakeCompletedCallback callback;

    /**
     * Constructor.
     * 
     * @param _connLink
     * @param _netBuffer
     * @param _decryptedNetBuffer
     * @param _encryptedAppBuffer
     * @param _result
     * @param _callback
     */
    public SSLHandshakeIOCallback(
                                  SSLConnectionLink _connLink,
                                  WsByteBuffer _netBuffer,
                                  WsByteBuffer _decryptedNetBuffer,
                                  WsByteBuffer _encryptedAppBuffer,
                                  SSLEngineResult _result,
                                  SSLHandshakeCompletedCallback _callback) {
        this.connLink = _connLink;
        this.netBuffer = _netBuffer;
        this.decryptedNetBuffer = _decryptedNetBuffer;
        this.encryptedAppBuffer = _encryptedAppBuffer;
        this.result = _result;
        this.callback = _callback;
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback#complete(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPReadRequestContext)
     */
    public void complete(VirtualConnection vc, TCPReadRequestContext rsc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "complete(read)");
        }
        try {
            // Continue handshake.
            SSLUtils.handleHandshake(connLink, netBuffer, decryptedNetBuffer,
                                     encryptedAppBuffer, result, callback, true);
        } catch (IOException ioe) {
            error(vc, rsc, ioe);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "complete(read)");
        }
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback#error(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPReadRequestContext, java.io.IOException)
     */
    public void error(VirtualConnection vc, TCPReadRequestContext rsc, IOException ioe) {
        // Alert the handshake completed callback. Buffers used in the handshake will be freed there.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Error occured during a read, exception:" + ioe);
        }
        this.callback.error(ioe);
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback#complete(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPWriteRequestContext)
     */
    public void complete(VirtualConnection vc, TCPWriteRequestContext wsc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "complete(write)");
        }
        // Clear the encrypted buffer before continuing hand shake.
        encryptedAppBuffer.clear();
        try {
            // Continue handshake.
            SSLUtils.handleHandshake(connLink, netBuffer, decryptedNetBuffer,
                                     encryptedAppBuffer, result, callback, true);
        } catch (IOException ioe) {
            error(vc, wsc, ioe);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "complete(write)");
        }
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback#error(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPWriteRequestContext,
     * java.io.IOException)
     */
    public void error(VirtualConnection vc, TCPWriteRequestContext wsc, IOException ioe) {
        // Alert the handshake completed callback. Buffers used in the handshake will be freed there.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Error occured during a write, exception:" + ioe);
        }
        this.callback.error(ioe);
    }

}
