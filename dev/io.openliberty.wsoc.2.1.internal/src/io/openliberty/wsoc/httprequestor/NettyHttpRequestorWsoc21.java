/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.wsoc.httprequestor;

import com.ibm.ws.wsoc.ParametersOfInterest;
import com.ibm.ws.wsoc.outbound.NettyHttpRequestorWsoc10;
import com.ibm.ws.wsoc.outbound.Wsoc21Address;
import com.ibm.ws.wsoc.outbound.WsocAddress;

import jakarta.websocket.ClientEndpointConfig;

/**
 *
 */
public class NettyHttpRequestorWsoc21 extends NettyHttpRequestorWsoc10 {

    public NettyHttpRequestorWsoc21(WsocAddress endpointAddress, ClientEndpointConfig config, ParametersOfInterest things) {
        super(endpointAddress, config, things);
    }

    @Override
    public void connect() throws Exception {
        ((Wsoc21Address) endpointAddress).setSSLContext(config.getSSLContext());
        super.connect();
    }

}