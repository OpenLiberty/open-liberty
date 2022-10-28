/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc.outbound;

import com.ibm.ws.wsoc.outbound.WsocAddress;
import javax.websocket.ClientEndpointConfig;
import com.ibm.ws.wsoc.ParametersOfInterest;

public interface HttpRequestorFactory {
    
    HttpRequestor getHttpRequestor(WsocAddress endpointAddress, ClientEndpointConfig config, ParametersOfInterest things);
}
