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

import com.ibm.wsspi.channelfw.exception.DiscriminationProcessException;

/**
 * An interface to the discrimination process.
 * <p>
 * This discrimination process is the means by which a channel link will find
 * its app side callback on an inbound connection.
 * <p>
 * The DiscriminationProcess is set within the Channel by the channel framework.
 * During changes in chain configurations, the DiscriminationProcess will be
 * used to add in new channels and remove old channels. However, this can be a
 * synchronization nightmare if the rules of this interface are not followed.
 * <p>
 * DiscriminationProcesses are stateless. All of their state (used for
 * optimization) are kept within the VirtualConnection objects.
 * <p>
 * Users should pick up a DiscriminationProcess from their channel every time
 * they are about to call it.
 * <p>
 * All data should be passed into discriminate every time.
 * <h4>Usage Example</h4>
 * 
 * <pre>
 * 
 * // fetch myChannel's discrimination process
 * DiscriminationProcess dp = getChannel().getDiscriminationProcess();
 * 
 * // track the state of the discrimination process
 * // set it to initially fail
 * int state = DiscriminationProcess.FAILURE;
 * while (true)
 * {
 * try
 * {
 * state = dp.discriminate(getVirtualConnection(), connLink.getData(), this);
 * }
 * catch (Exception e)
 * {
 * // Follow the failure logic
 * log(&quot;DiscriminationProblem&quot;, e);
 * }
 * if (DiscriminationProcess.AGAIN == state)
 * {
 * connLink.read();
 * continue;
 * }
 * 
 * if (DiscriminationProcess.FAILURE == state)
 * {
 * // close the connection because nobody wants it
 * newSocket.close();
 * connLink.destroy();
 * vc.destroy();
 * }
 * break;
 * }
 * if (DiscriminationProcess.SUCCESS == state)
 * {
 * getApplicationCallback().ready(getVirtualConnection());
 * }
 * </pre>
 */
public interface DiscriminationProcess {

    /**
     * SUCCESS indicates that we have found a discriminating match.
     */
    static int SUCCESS = 1;

    /**
     * FAILURE means that no discriminator can be found and the connection
     * should be closed.
     */
    static int FAILURE = 0;

    /**
     * AGAIN means you need to ask again with more data.
     */
    static int AGAIN = 2;

    /**
     * This is where the magic happens. Users of this class should keep
     * calling this method until it returns a value of either success or
     * failure. If it returns a value of again, the caller should go and
     * get some more data before invoking it again.
     * <p>
     * If a value of success is returned, the ready method must be called on the
     * application side connection link in order to pass control of the virtual
     * connection onto the next channel.
     * <p>
     * If a value of "failure" is returned, it is the responsibility of the
     * current channel to clean things up. The virtual connection will have been
     * left untouched.
     * <p>
     * This method can be invoked multiple times after it has already returned a
     * value of success or failure and it will keep returning the same value (and
     * doing nothing).
     * <p>
     * The users of this should obtain a reference to the discrimination process
     * at the beginning of discrimination and use that process each time it is
     * called. The DiscriminationProcess that is set within the Channels may
     * change and will cause problems if two different DiscriminationProcesses are
     * called....the second being after the first returned again.
     * <p>
     * When calling this the user needs to pass all data (including the first data
     * passed)
     * 
     * @param vc
     * @param discrimData
     * @param currentChannel
     *            link
     * @return int
     * @throws DiscriminationProcessException
     */
    int discriminate(VirtualConnection vc, Object discrimData, ConnectionLink currentChannel) throws DiscriminationProcessException;

    /**
     * This is a way in which to bypass the normal discriminate when through some
     * configuration
     * or other map, the channel above which this channel wants to connect to is
     * known.
     * <p>
     * Pass in the current channels link and the name of the channel to connect
     * to, and this will connect the channel links.
     * <p>
     * this returns a status SUCCESS or FAILURE whether or not the channel could
     * be found.
     * 
     * @param vc
     * @param channelName
     * @param currentChannel
     * @throws DiscriminationProcessException
     * @return int
     */
    int discriminate(VirtualConnection vc, ConnectionLink currentChannel, String channelName) throws DiscriminationProcessException;

}
