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

import com.ibm.ws.wsoc.outbound.HttpRequestorFactory;

import com.ibm.ws.wsoc.outbound.HttpRequestor;

import com.ibm.ws.wsoc.outbound.HttpRequestorFactory;
import com.ibm.ws.wsoc.outbound.WsocAddress;
import com.ibm.ws.wsoc.outbound.WsocOutboundChain;
import jakarta.websocket.ClientEndpointConfig;
import com.ibm.ws.wsoc.ParametersOfInterest;

public class HttpRequestorWsoc21FactoryImpl implements HttpRequestorFactory {
    
    public HttpRequestor getHttpRequestor(WsocAddress endpointAddress, ClientEndpointConfig config, ParametersOfInterest things){
    	if (WsocOutboundChain.isUsingNetty())
             return new NettyHttpRequestorWsoc21(endpointAddress, config, things);
        return new HttpRequestorWsoc21(endpointAddress, config, things);
    }
}
