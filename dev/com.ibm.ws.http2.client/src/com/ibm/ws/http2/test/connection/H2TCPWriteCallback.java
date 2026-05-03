/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http2.test.connection;

import java.io.IOException;
import java.io.EOFException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 *
 */
public class H2TCPWriteCallback implements TCPWriteCompletedCallback {

    H2Connection h2connection = null;

    private static final String CLASS_NAME = H2TCPWriteCallback.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public H2TCPWriteCallback(H2Connection connection) {
        h2connection = connection;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.tcp.channel.TCPWriteCompletedCallback#complete(com.ibm.wsspi.channel.framework.VirtualConnection, com.ibm.wsspi.tcp.channel.TCPWriteRequestContext)
     */
    @Override
    public void complete(VirtualConnection arg0, TCPWriteRequestContext arg1) {
        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "complete", "H2TCPWriteCallback complete");
        h2connection.syncWrite();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.tcp.channel.TCPWriteCompletedCallback#error(com.ibm.wsspi.channel.framework.VirtualConnection, com.ibm.wsspi.tcp.channel.TCPWriteRequestContext,
     * java.io.IOException)
     */
    @Override
    public void error(VirtualConnection arg0, TCPWriteRequestContext arg1, IOException arg2) {
        if (arg2 instanceof EOFException) {
            if (LOGGER.isLoggable(Level.FINEST))
                LOGGER.logp(Level.FINEST, CLASS_NAME, "error", "H2TCPWriteCallback.error: Ignoring EOFException in callback from connection " + arg1.getSocket() + " -> " + arg2);
                return;
        }
        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "error", "H2TCPWriteCallback.error: Received error callback from connection " + arg1.getSocket() + " -> " + arg2);
        if (!h2connection.isClosedCalled()) {
            if (LOGGER.isLoggable(Level.FINEST))
                LOGGER.logp(Level.FINEST, CLASS_NAME, "error", "H2TCPWriteCallback.error: Calling close with encountered exception");
            h2connection.getReportedExceptions().add(arg2);
            h2connection.close();
        }
    }

}
