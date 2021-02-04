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
 * Channel extended interface for Inbound (server side) channels.
 * <p>
 * This interface adds the necessary methods to perform Discrimination on the
 * data to allow for channel sharing on the inbound side.
 * 
 * @see com.ibm.wsspi.channelfw.DiscriminationProcess
 */
public interface InboundChannel extends Channel {
    /**
     * Returns the discriminator which needs to be matched in order to
     * route a request to this channel.
     * 
     * @return Discriminator
     */
    Discriminator getDiscriminator();

    /**
     * Fetch the instance of the DiscriminationProcess currently assigned
     * to this channel chain. This is called by the channel
     * framework to map a discrimination group and algorithm with this
     * channel. It is the responsiblity of the channel implementor
     * to maintain a DiscriminationProcess in the channel implementation
     * which can be get and set by the framework.
     * <p>
     * It also may be used by individual channel implementors to get their
     * discriminationProcess to call.
     * <p>
     * Synchronization is not needed while the most up to date process is not
     * guaranteed.
     * 
     * @return DiscriminationProcess
     */
    DiscriminationProcess getDiscriminationProcess();

    /**
     * The ChannelFramework uses this to set a particular instance of
     * the DiscriminationProcess to a specific channel. This is called by the
     * channel
     * framework to assign the appropriate algorithm for doing
     * discrimination for inbound chains.
     * <p>
     * Synchronization is not needed while the most up to date process is not
     * guaranteed.
     * 
     * @param dp
     *            The DiscriminationProcess that should be used from
     *            the point of this method being called forward.
     */
    void setDiscriminationProcess(DiscriminationProcess dp);

    /**
     * Returns a class representing the type of discriminatory data which
     * all the upstream channels must be able to discriminate on.
     * <p>
     * In some cases, the channel below may use an array of these objects. This
     * should be clear via its documented behavior.
     * 
     * @return Class<?>
     */
    Class<?> getDiscriminatoryType();
}
