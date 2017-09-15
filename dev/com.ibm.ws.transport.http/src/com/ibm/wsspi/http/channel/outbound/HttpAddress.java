/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.channel.outbound;

import com.ibm.wsspi.tcpchannel.TCPConnectRequestContext;

/**
 * Address object to pass in to connect to a remote host.
 * 
 * @ibm-private-in-use
 */
public interface HttpAddress extends TCPConnectRequestContext {

    /**
     * Hostname to pass into the Host header of the request.
     * 
     * @return String
     */
    String getHostname();

    /**
     * Query whether the target in the address is a forward proxy. If this
     * is true, then the request message will send out the full URL (scheme
     * plus hostname plus URI, etc), otherwise the request will only send out
     * the URI ([GET /index.html HTTP/1.1] for example).
     * 
     * @return boolean
     */
    boolean isForwardProxy();

}
