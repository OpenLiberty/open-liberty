/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.channelfw;

/**
 * Outbound (client side) specific interface for Channels.
 * <p>
 * This extension of Channel adds information about the Object types accepted to
 * connect and connectAsynch calls.
 */
public interface OutboundChannel extends Channel {

    /**
     * The framework uses this method for coherency checking of address types for
     * connect and connectAsynch.
     * This method will return the type of address object this channel plans to
     * pass down towards
     * the device side.
     * 
     * @return Class<?>
     */
    Class<?> getDeviceAddress();

    /**
     * The framework uses this method for coherency checking of address types for
     * connect and connectAsynch.
     * This method will return the type of address objects this channel plans have
     * passed to it
     * from the application side. A channel may accept more than one address
     * object type but
     * passes only one down to the channels below.
     * 
     * @return Class<?>[]
     */
    Class<?>[] getApplicationAddress();

}
