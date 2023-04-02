/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package com.ibm.wsspi.channelfw;

/**
 * This interface is used to create inbound virtual connections.
 * <p>
 * These factories should be obtained from the ChannelFramework.
 */
public interface InboundVirtualConnectionFactory extends VirtualConnectionFactory {
    /**
     * Create a VirtualConnection.
     * 
     * @return VirtualConnection
     */
    VirtualConnection createConnection();
}
