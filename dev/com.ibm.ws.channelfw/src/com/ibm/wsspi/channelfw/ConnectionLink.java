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
 * The ConnectionLink is the primary interface for inter Channel communication
 * on a (Virtual) Connection specific basis.
 * <p>
 * This object gives access to the interface this channel stated it would
 * expose, as well as lets the ChannelFramework link it to the Upper and Lower
 * Channel's ConnectionLink on a per VirtualConnection basis.
 * <p>
 * Some of the method's in the ConnectionLink should only be called by Channel's
 * on one specific side. The higher (side of the channel closer to the
 * Application) channel can call getChannelAccessor, connect, close. Whereas,
 * the lower (Device side) channel can call ready and destroy.
 * <p>
 * <h4>Close and destroy</h4>
 * Close and destroy have very specific flows. Close should first be called by
 * the Application channel (the highest level channel). When the Connector
 * Channel gets a close, it can then call destroy back up the stack. Once
 * destroy is called, then the channel may clean up resources. To avoid
 * NullPointerExceptions, no resources should be cleaned up until the destroy
 * method.
 * <p>
 * If an Application channel can get into a situation where it is waiting for an
 * outstanding asynchronous callback and wants to close the stack, it will have
 * to use the virtual connnection's special close synchronized close behavior.
 * <p>
 * To summarize the basic close/destroy methodology, closes propogate down from
 * the application channel to the connector channel which is then resposible to
 * call destroy back up the chain.
 */
public interface ConnectionLink extends ConnectionReadyCallback {
    /**
     * Returns an instance of the interface to this channel that may
     * or may not be connection specific dependent on the implementation.
     * <p>
     * The Object returned here must be castable to the interface returned by the
     * Channel and ChannelFactory.
     * 
     * @return Object
     */
    Object getChannelAccessor();

    /**
     * Returns the virtual connection associated with the instance.
     * 
     * @return VirtualConnection
     */
    VirtualConnection getVirtualConnection();

    /**
     * Connect a callback to the application side of this link.
     * <p>
     * The callback is the primary interface to the channel above. It encompasses
     * the only interface a channel is allowed to call on the above channel
     * through the framework's interfaces, ready and destroy.
     * 
     * @param next
     *            The ConnectionReadyCallback from the Channel above.
     *            On the inbound side, this will be set during the
     *            DiscriminationProcess and
     *            from the outbound side this will be set up at initial connection
     *            setup.
     */
    void setApplicationCallback(ConnectionReadyCallback next);

    /**
     * Get the ConnectionLink from the channel below your (device side) on the
     * stack.
     * <p>
     * The ChannelFramework will have set the value to be returned via the
     * setDeviceLink method.
     * 
     * @return ConnectionLink
     */
    ConnectionLink getDeviceLink();

    /**
     * Get the callback on the application side.
     * <p>
     * The ChannelFramework will have set this value to be returned via the
     * setApplicationCallback method.
     * 
     * @return ConnectionReadyCallback
     */
    ConnectionReadyCallback getApplicationCallback();

    /**
     * Indicate to the channels device side that the application side channel
     * is complete with this connection. Items should not be pooled until after
     * a destroy call is made back.
     * <p>
     * Some channel's behavior may be documented that a close without an Exception
     * allows a ready() to be called back to it. This is dependent on the behavior
     * of the device channel. By default, a close with an Exception should mean
     * that the channel below will continue to propogate the close down to the
     * connector channel. Check with the device channel's documented behavior for
     * exceptions to the default behavior of this.
     * 
     * @param vc
     *            This is the VirtualConnection that this ConnectionLink
     *            should already have a reference to. This can be used to validate
     *            that
     *            this is the correct vc for logging purposes.
     * @param e
     *            This is a reference to the exception, if one exists, that is
     *            causing this connection to be closed.
     */
    void close(VirtualConnection vc, Exception e);

    /**
     * Connect a channel link object to the device side of this link.
     * <p>
     * On the inbound case, the ChannelFramework will set this during the
     * DiscriminationProcess of the device side channel.
     * <p>
     * On the outbound case, this will be set during initial connection creation.
     * 
     * @param next
     *            This is the link to the channel below.
     */
    void setDeviceLink(ConnectionLink next);

}
