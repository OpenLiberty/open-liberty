/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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
package com.ibm.wsspi.udpchannel;

import java.net.InetSocketAddress;

/**
 * This is the address passed to UDPChannel when establishing an outbound
 * connection. This is ONLY to set the local interface/port to listen on.
 */
public interface UDPRequestContext {
    /**
     * Load address to bind this socket to. Can return null in which case
     * the operating system dependent behaviour of binding to the next free
     * local address is assumed.
     * 
     * @return InetSocketAddress
     */
    InetSocketAddress getLocalAddress();

}
