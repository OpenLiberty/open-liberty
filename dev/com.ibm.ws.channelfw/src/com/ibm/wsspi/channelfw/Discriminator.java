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
 * The Discriminator is the primary way that an InboundChannel claims ownership
 * of a specific connection.
 * <p>
 * This interface contains the methods and constants which must be used to
 * perform a single step in the discrimination process. Each inbound
 * non-Connector Channel will need a Discriminator in case that it needs to
 * evaluate the data from the channel below to determine whether it owns the
 * connection. This is part of the Discrimination Process
 * 
 * @see com.ibm.wsspi.channelfw.DiscriminationProcess
 */
public interface Discriminator {
    /**
     * Enumeration value which may be returned by the discriminate method to
     * indicate that
     * this channel should own this connection. This value means that the
     * discriminator
     * (and hence its associated Channel) wishes to claim the data.
     */
    int YES = 1;

    /**
     * Enumeration value which may be returned by the discriminate method to
     * indicate that
     * this channel should not own this connection and cannot handle this
     * connection. This
     * value means that the discriminator (and hence its associated Channel)
     * cannot (or will not) process the data being presented to it. Further
     * more, the discriminator wishes to be removed from the discrimination
     * process, as it can serve no futher useful purpose.
     */
    int NO = 0;

    /**
     * Enumeration value which may be returned by the discriminate method to
     * indicate that
     * this channel needs more information before it is able to determine whether
     * it owns
     * this connection or not. This value means that the discriminator (and hence
     * its
     * associated Channel) cannot determine if it can accept the data or not.
     * Unless any
     * other discriminator claims the data first, this discriminator is requesting
     * that
     * more data is read.
     */
    int MAYBE = -1;

    /**
     * Determines whether the channel associated with this discriminator can
     * process data being received as an argument to this method.
     * ( @see Discriminator#getChannel() )
     * <p>
     * The code that implements this method need only look at the data and make a
     * decision, the task of correctly binding channels is handled elsewhere.
     * <p>
     * At the point of reaching a MAYBE or YES conclusion, it may be desirable to
     * store some state in the VirtualConnection for reuse. The data stored in the
     * MAYBE method will be able to be cleaned up via the cleanUpState method in
     * the case where another Discriminator said "YES" and now owns this
     * connection.
     * 
     * @see Discriminator#cleanUpState(VirtualConnection)
     *      <p>
     *      For more information on how the discriminate is used and from where it
     *      will be called, see the DiscriminationProcess.
     * @see com.ibm.wsspi.channelfw.DiscriminationProcess
     *      <p>
     *      Note, upon return from this method, the discrimination object must be
     *      in the same state as when it was passed in. Treat the object as read
     *      only.
     * 
     * @param vc
     *            The virtual connection the data has passed along before
     *            reaching this discrimination step.
     * @param discrimData
     *            The data to discriminate using. This type of this
     *            data will be channel (and hence discriminator)
     *            dependent.
     * @return int An enumerated value represting this discriminator's
     *         (and hence associated Channel's) ability to deal with
     *         the data being presented with it. Valid return values
     *         include YES, NO, and MAYBE.
     */
    int discriminate(VirtualConnection vc, Object discrimData);

    /**
     * If a call to the discriminate method returns MAYBE, and the discrimination
     * process later finds another disciminator that returns YES, this method will
     * be called to clean up any state that was reserved for a follow up call to
     * discriminate.
     * 
     * @param vc
     *            The virtual connection that this Discriminator would have stored
     *            data in had it done so during the discriminate method.
     */
    void cleanUpState(VirtualConnection vc);

    /**
     * Returns a class which all discriminatory data valid for this
     * discriminator must be castable to. This method is used to ensure
     * coherency before runtime. If the data will be an array such as
     * String[] or ByteBuffer[], then the class passed should be
     * the class of the object and the channel's API docuementation
     * should clearly state that the discrimination data is an array.
     * 
     * @return Class<?>
     */
    Class<?> getDiscriminatoryDataType();

    /**
     * Returns the channel that this discriminator serves.
     * 
     * @return Channel
     */
    Channel getChannel();

    /**
     * Gets this discriminators weight as currently configured. This should have
     * been fetched
     * from the latest ChannelData that was passed into the Channel.
     * 
     * @return int
     */
    int getWeight();

}
