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

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.wsspi.channelfw.exception.ChannelException;

/**
 * The Channel interface is the core to the inbound or outbound channel
 * implementation. This interface provides for lifecycle methods,
 * ConnectionLinks for each Virtual Connection, and the ability to update
 * channel configuration data in the running system. This
 * Channel will be created through its associated ChannelFactory.
 */
public interface Channel {
    /**
     * This returns an instance of the connection specific representation of this
     * channel in the chain. We provide all context
     * needed and this should all be stored in the ConnectionLink implementation
     * for use later.
     * 
     * @param vc
     *            This is the VirtualConnection that this ConnectionLink will be
     *            bound to until destroyed.
     * @return ConnectionLink
     */
    ConnectionLink getConnectionLink(VirtualConnection vc);

    /**
     * Starts the channel. The channels in a chain are started from
     * the end of the chain closest to the user application code.
     * 
     * @throws ChannelException
     *             Requested action cannot be taken
     */
    void start() throws ChannelException;

    /**
     * Stop this channel.
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
     * Enable channel implementations to do initialization in a step
     * separate from starting.
     * <p>
     * It is desired that the Initialized state be the one that a stop call also
     * takes a user into.
     * 
     * @throws ChannelException
     *             if requested action cannot be taken
     */
    void init() throws ChannelException;

    /**
     * Cleans up all resources of a channel before it is left for
     * garbage collection. Note, however, that once this method completes,
     * the channel instance should be left in a state where init() can
     * be called and the channel reused. The ChannelFactory implementation may
     * require it.
     * <p>
     * Although users of this SPI have the ability to throw ChannelExceptions,
     * they are expected to still have destroyed their Channel.
     * <p>
     * ChannelExceptions should only be thrown in cases which the Channel hit a
     * significant obstacle in trying to destroy.
     * 
     * @throws ChannelException
     */
    void destroy() throws ChannelException;

    /**
     * Return this channel's internal name. The name can be found in the
     * ChannelData
     * as was passed into the ChannelFactory when the Channel was created.
     * 
     * @return String
     */
    String getName();

    /**
     * Returns interface into this channel. Return the interface class that this
     * channel presents to adjacent
     * channels on its application side.
     * <p>
     * This is the interface that other channels who want to build on top of this
     * chain will use.
     * <p>
     * If this is an Application channel and will not expose an interface, this
     * implementation should just return null here.
     * 
     * @return Class<?>
     */
    Class<?> getApplicationInterface();

    /**
     * Return the device interface class supported. This
     * interface represents the type of objects that can be passed from
     * an adjacent channel to this channel on its device side.
     * <p>
     * Each channel will be built on one interface but the ChannelFactory can be
     * built for multiple interfaces. This allows a channel implementation to use
     * multiple interfaces based on its configuration.
     * <p>
     * If this is a Connector channel and will not be built on another channel,
     * this implementation should just return null here.
     * 
     * @return Class<?>
     */
    Class<?> getDeviceInterface();

    /**
     * Update Channel Configuration Data.
     * <p>
     * This is the way the framework gives a channel new data object to update the
     * instance with. There is no need to indicate that part or portions of this
     * data is rejected in this version.
     * <p>
     * It will be the Channel's responsibility to synchronize this update and push
     * out the changes to their ConnectionLink's as appropriate.
     * 
     * @param cc
     */
    void update(ChannelData cc);

}
