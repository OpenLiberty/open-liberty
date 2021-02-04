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
 * ConnectionReadyCallback to be notified when new connections are ready on the
 * inbound or outbound side.
 * <p>
 * All channels will not implement this and instead implement the ConnectionLink
 * which extends this interface. This is the only piece of the ConnectionLink
 * interface that the device side or lower channel in the stack should ever call
 * on the channel above.
 * <p>
 * The ConnectionReadyCallback is also used for users of outbound channel chains
 * who are not actually implementing channel and wish to use asynchronous
 * connects. The asynchronous connects through the OutboundVirtualConnection
 * interface require a ConnectionReadyCallback to be registered.
 */
public interface ConnectionReadyCallback {

    /**
     * The ready method is the primary event notification that a new connection is
     * ready (on the inbound side)
     * or that the asynchronous connect has completed (on the outbound side).
     * <p>
     * Some channels may also use this to indicate there is more work to be done
     * when a close without an Exception is called. This is a non-default behavior
     * of certain channels, and it is an attribute of the specific channel's
     * behavior below.
     * 
     * @param vc
     *            This is the virtualConnection that is ready.
     */
    void ready(VirtualConnection vc);

    /**
     * Destroy this Callback and the resources associated with it.
     * <p>
     * This is a life cycle method. Called when the virtual connection that this
     * callback belongs to is destroyed. Allows callbacks to return themselves to
     * a pool. Channels are expected to continue propogating the destroy calls to
     * the channels above.
     * <p>
     * If this is called with an Exception then an abnormal state has occured. The
     * top level application may want to know this and normally this Exception
     * should continue to be propogated up the chain.
     * <p>
     * Also used for callbacks when asynchronous connects fail on the outbound
     * side.
     * <p>
     * If a user of the outbound VirtualConnection uses this class during a
     * connectAsynch, a destroy should be seen after the connection is closed.
     * <p>
     * Aside from the outbound asynchronous connects, this destroy call should not
     * be made unless it is coming from the channel above and should be after a
     * destroy.
     * 
     * @param e
     *            This is the exceptional case in which this destroy is being
     *            called.
     *            This parameter may be null if this was not an exceptional state.
     */
    void destroy(Exception e);

}
