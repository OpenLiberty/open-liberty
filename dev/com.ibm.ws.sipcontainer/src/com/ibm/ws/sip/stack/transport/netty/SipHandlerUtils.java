/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.netty;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;
import com.ibm.ws.sip.stack.transport.sip.netty.SIPConnectionFactoryImplWs;

import jain.protocol.ip.sip.ListeningPoint;

public class SipHandlerUtils {

    private static final TraceComponent tc = Tr.register(SipHandlerUtils.class);

    /**
     * 
     * @param addr
     * @param transport
     * @return the listening point matching the given address and transport
     */
    protected static ListeningPoint getListeningPoint(SocketAddress addr, String transport) {
        ListeningPoint lp = null, defaultLp = null;

        if (!(addr instanceof InetSocketAddress)) {
            return null;
        }

        InetSocketAddress isa = (InetSocketAddress) addr;

        for (ListeningPoint lPoint : SIPConnectionFactoryImplWs.instance().getInboundChannels().keySet()) {
            if (lPoint.getPort() != isa.getPort()) {
                continue;
            }
            if (!lPoint.getTransport().equalsIgnoreCase(transport)) {
                continue;
            }
            String addHost = SIPStackUtil.getHostAddress(isa.getHostName());
            String lpHost = SIPStackUtil.getHostAddress(lPoint.getHost());

            if (SIPStackUtil.isSameHost(addHost, lpHost)) {
                lp = lPoint;
                break;
            } else {
                defaultLp = lPoint;
            }
        }

        if (lp == null) {
            lp = defaultLp;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getListeningPoint", "a listening point was not found for: " + addr + ","
                        + transport + ". returning the default value: " + lp);
            }
        }
        return lp;
    }
}
