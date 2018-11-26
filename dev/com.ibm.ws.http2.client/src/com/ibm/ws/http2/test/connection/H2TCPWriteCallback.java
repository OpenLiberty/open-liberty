/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http2.test.connection;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 *
 */
public class H2TCPWriteCallback implements TCPWriteCompletedCallback {

    H2Connection h2connetion = null;

    private static final String CLASS_NAME = H2TCPReadCallback.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public H2TCPWriteCallback(H2Connection connection) {
        h2connetion = connection;
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
        h2connetion.syncWrite();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.tcp.channel.TCPWriteCompletedCallback#error(com.ibm.wsspi.channel.framework.VirtualConnection, com.ibm.wsspi.tcp.channel.TCPWriteRequestContext,
     * java.io.IOException)
     */
    @Override
    public void error(VirtualConnection arg0, TCPWriteRequestContext arg1, IOException arg2) {
        // TODO Auto-generated method stub
        if (LOGGER.isLoggable(Level.INFO))
            LOGGER.logp(Level.INFO, CLASS_NAME, "error", "H2TCPWriteCallback error: " + arg2);

    }

}
