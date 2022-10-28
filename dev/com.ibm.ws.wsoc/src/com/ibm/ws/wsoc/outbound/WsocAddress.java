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

import java.net.URI;

import com.ibm.wsspi.http.channel.outbound.HttpAddress;

public interface WsocAddress extends HttpAddress {
    
    String getChainKey();

    void validateURI();
        
    boolean isSecure();
        
    URI getURI();
        
    String getPath();
}
