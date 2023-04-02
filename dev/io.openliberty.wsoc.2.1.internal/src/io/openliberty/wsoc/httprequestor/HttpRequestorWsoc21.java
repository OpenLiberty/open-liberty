/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.wsoc.httprequestor;

import com.ibm.ws.wsoc.outbound.HttpRequestorWsoc10;
import com.ibm.ws.wsoc.outbound.WsocAddress;
import com.ibm.ws.wsoc.outbound.Wsoc21Address;
import com.ibm.ws.wsoc.outbound.ClientTransportAccess;

import com.ibm.ws.wsoc.outbound.WsocOutboundChain;

import com.ibm.ws.wsoc.WebSocketVersionServiceManager;

import jakarta.websocket.ClientEndpointConfig;
import com.ibm.ws.wsoc.ParametersOfInterest;

import com.ibm.wsspi.channelfw.OutboundVirtualConnection;

public class HttpRequestorWsoc21 extends HttpRequestorWsoc10 {

    public HttpRequestorWsoc21(WsocAddress endpointAddress, ClientEndpointConfig config, ParametersOfInterest things) {
        super(endpointAddress, config, things);
    }

    @Override
    public void connect() throws Exception {

        access = new ClientTransportAccess();

        ((Wsoc21Address) endpointAddress).setSSLContext(config.getSSLContext());

        vc = (OutboundVirtualConnection) WsocOutboundChain.getVCFactory(endpointAddress);;
        access.setVirtualConnection(vc);
        vc.connect(endpointAddress);

    }
    
}
