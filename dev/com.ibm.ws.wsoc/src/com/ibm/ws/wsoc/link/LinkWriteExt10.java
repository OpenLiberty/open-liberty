/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.wsoc.link;

import java.util.logging.Logger;

import javax.websocket.SendResult;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

public class LinkWriteExt10 extends LinkWrite {

        private static final TraceComponent tc = Tr.register(LinkWriteExt10.class);
        private static final Logger LOG = Logger.getLogger(LinkWriteExt10.class.getName());
    
        public void processWrite(TCPWriteRequestContext wsc) {

        // write completed successfully - call sendHandler if this was the result of websocket async write.
        // if a Send with a Future is being used, then we are using our future send handler here.

        if (wsocSendOutstanding == true) {

            wsocSendOutstanding = false;
            if (wsocSendHandler != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "calling onResult on SendHandler: " + wsocSendHandler);
                }
                wsocSendHandler.onResult(SendResultGood);
            }
        }

    }

    public void processError(TCPWriteRequestContext wsc, Throwable ioe) {
        // write completed with an error - call sendHandler if this was the result of websocket async write
        // if a Send with a Future is being used, then we are using our future send handler here.

        // cleanup up before calling onResult, since onResult, or an async user thread, may want to oddly write data right away
        // no cleanup if exception occurred before trying to write on the wire
        if (wsc != null) {
            messageWriter.frameCleanup();
        }

        if (wsocSendOutstanding == true) {

            wsocSendOutstanding = false;
            if (wsocSendHandler != null) {

                SendResult result = new SendResult(ioe);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "calling onResult on SendHandler: " + wsocSendHandler);
                }
                wsocSendHandler.onResult(result);
            }
        }

    }
}
