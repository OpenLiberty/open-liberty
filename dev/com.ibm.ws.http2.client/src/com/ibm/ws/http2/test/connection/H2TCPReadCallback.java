/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

/**
 *
 */
public class H2TCPReadCallback implements TCPReadCompletedCallback {

    H2Connection h2connetion = null;

    private static final String CLASS_NAME = H2TCPReadCallback.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public H2TCPReadCallback(H2Connection connection) {
        h2connetion = connection;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.tcp.channel.TCPReadCompletedCallback#complete(com.ibm.wsspi.channel.framework.VirtualConnection, com.ibm.wsspi.tcp.channel.TCPReadRequestContext)
     */
    @Override
    public void complete(VirtualConnection arg0, TCPReadRequestContext tcpReadRequestContext) {
        if (h2connetion.isClosedCalled()) {
            if (LOGGER.isLoggable(Level.FINEST))
                LOGGER.logp(Level.FINEST, CLASS_NAME, "complete", "H2TCPReadCallback.complete: Recevied callback after connection was closed. Will ignore callback");
            return;
        }
        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "complete", "H2TCPReadCallback.complete: Calling processData from callback");
        h2connetion.processData();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.tcp.channel.TCPReadCompletedCallback#error(com.ibm.wsspi.channel.framework.VirtualConnection, com.ibm.wsspi.tcp.channel.TCPReadRequestContext,
     * java.io.IOException)
     */
    @Override
    public void error(VirtualConnection arg0, TCPReadRequestContext arg1, IOException arg2) {

    }

    void processFrame(WsByteBuffer buffer) {

    }

}
