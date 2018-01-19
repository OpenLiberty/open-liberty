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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

public class H2MuxTCPReadCallback implements TCPReadCompletedCallback {

    H2InboundLink connLink = null;

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(H2MuxTCPReadCallback.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    public void setConnLinkCallback(H2InboundLink _link) {
        connLink = _link;
    }

    @Override
    public void complete(VirtualConnection vc, TCPReadRequestContext rrc) {

        if (connLink != null) {
            connLink.setReadLinkStatusToNotReadingAndNotify();
            connLink.processRead(vc, rrc);
        }
    }

    @Override
    public void error(VirtualConnection vc, TCPReadRequestContext rrc, IOException exception) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "H2MuxTCPReadCallback error callback called with exception: " + exception);
        }

        if (connLink != null) {
            connLink.setReadLinkStatusToNotReadingAndNotify();
            connLink.closeConnectionLink(exception);
        }
    }

}
