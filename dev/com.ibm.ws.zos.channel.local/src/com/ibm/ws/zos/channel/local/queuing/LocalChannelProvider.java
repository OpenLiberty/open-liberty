/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.queuing;

import java.util.Map;

import com.ibm.ws.zos.channel.local.LocalCommClientConnHandle;
import com.ibm.ws.zos.channel.local.internal.LocalCommConnLink;
import com.ibm.ws.zos.channel.local.queuing.internal.NativeRequestHandler;
import com.ibm.wsspi.channelfw.exception.ChannelException;

/**
 * Represents the native half of the LocalComm channel.  LocalCommChannel.java interacts
 * with the native localcomm impl via this service. 
 * 
 */
public interface LocalChannelProvider {

	/**
     * Start the native half of the localcomm channel. 
     * 
     * @throws ChannelException
     *             Requested action cannot be taken
     */
    void start() throws ChannelException;

    /**
     * Stop the native half of the localcomm channel. 
     * <p>
     * Stop not only inhibits the creation of new virtual connections, but also
     * closes any existing connections.
     * <p>
     * Although users of this SPI have the ability to throw ChannelExceptions,
     * they are expected to still have stopped their individual channel when a
     * time of 0 is given.
     * <p>
     * ChannelExceptions should only be thrown in cases which the Channel hit a
     * significant obstacle in trying to stop.
     * <p>
     * Stop 0 means stop immediately, stop with a time means prepare to stop.
     * 
     * @param millisec
     *            time until stop will be asserted. If zero,
     *            assertion is immediate.
     * @throws ChannelException
     */
    void stop(long millisec) throws ChannelException;

    /**
     * The BlackQueueDemultiplexor routes local comm native black queue requests to the
     * appropriate callback handler, registered for the given type and connection handle 
     * associated with the black queue work request.
     * 
     * @return The BlackQueueDemultiplexor
     */
    BlackQueueDemultiplexor getBlackQueueDemultiplexor();
    
    /**
     * The NativeRequestHandler contains methods for performing native operations on
     * the LocalCommChannel, e.g. reading/writing data on the connection. 
     * 
     * @return The NativeRequestHandler
     */
    NativeRequestHandler getNativeRequestHandler();

    /**
     * @return A mapping of native conn handles to LocalCommConnLink objects.
     */
    Map<LocalCommClientConnHandle, LocalCommConnLink> getConnHandleToConnLinkMap();
    
    /**
     * Tells is if the current thread is a black queue listener thread.
     */
    boolean isBlackQueueListenerThread();
}