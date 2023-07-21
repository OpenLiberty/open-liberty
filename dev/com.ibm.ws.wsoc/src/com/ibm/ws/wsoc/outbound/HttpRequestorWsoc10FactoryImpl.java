/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.wsoc.outbound;

import javax.websocket.ClientEndpointConfig;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.wsoc.ParametersOfInterest;
import com.ibm.ws.wsoc.WebSocketVersionServiceManager;

public class HttpRequestorWsoc10FactoryImpl implements HttpRequestorFactory {
    private static final TraceComponent tc = Tr.register(HttpRequestorWsoc10FactoryImpl.class);

    public HttpRequestor getHttpRequestor(WsocAddress endpointAddress, ClientEndpointConfig config, ParametersOfInterest things) {
        if (!WebSocketVersionServiceManager.useNetty()) {

            return new HttpRequestorWsoc10(endpointAddress, config, things);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Using netty");
            }
            return new NettyHttpRequestorWsoc10(endpointAddress, config, things);
        }
    }
}
