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
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

public class H2MuxTCPReadCallback implements TCPReadCompletedCallback {

    H2InboundLink connLink = null;

    public void setConnLinkCallback(H2InboundLink _link) {
        connLink = _link;
    }

    @Override
    public void complete(VirtualConnection vc, TCPReadRequestContext rrc) {

        try {
            if (connLink != null) {
                connLink.processRead(vc, rrc);
            }
        } catch (Exception e) {
            //CMM TODO
            //Something bad happened, do something about it
        }

    }

    @Override
    public void error(VirtualConnection vc, TCPReadRequestContext rrc, IOException arg2) {

    }

}
