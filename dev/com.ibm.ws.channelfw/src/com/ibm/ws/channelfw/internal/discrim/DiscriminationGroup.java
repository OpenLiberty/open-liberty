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
package com.ibm.ws.channelfw.internal.discrim;

import java.util.List;

import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.exception.DiscriminationProcessException;

/**
 * An internal extension to the DiscriminationProcess given to users of the
 * channel framework. This object stores all information about connecting
 * channels
 * above.
 */
public interface DiscriminationGroup extends DiscriminationProcess, Comparable<DiscriminationGroup> {
    /**
     * Adds a discriminator to the group
     * 
     * @param d
     *            The discriminator to add
     * @param weight
     *            for discriminator
     * @throws DiscriminationProcessException
     */
    void addDiscriminator(Discriminator d, int weight) throws DiscriminationProcessException;

    /**
     * Removes a discriminator from the group
     * 
     * @param d
     *            The discriminator to remove
     * @throws DiscriminationProcessException
     */
    void removeDiscriminator(Discriminator d) throws DiscriminationProcessException;

    /**
     * Method getDiscriminators.
     * 
     * @return ArrayList
     */
    List<Discriminator> getDiscriminators();

    /**
     * Method getDiscriminationAlgorithm.
     * 
     * @return DiscriminationAlgorithm
     */
    DiscriminationAlgorithm getDiscriminationAlgorithm();

    /**
     * Method setDiscriminationAlgorithm.
     * 
     * @param da
     */
    void setDiscriminationAlgorithm(DiscriminationAlgorithm da);

    /**
     * Method start
     * 
     * Prepare this process to be run. This allows no more changes
     * to the DiscriminationGroup.
     */
    void start();

    /**
     * Method getChannelName
     * 
     * returns the channel name for this process.
     * 
     * @return String
     */
    String getChannelName();

    /**
     * Method getDiscriminatorNodes
     * 
     * gets the node chain for this discriminator. Internal use only.
     * 
     * @return Object
     */
    Object getDiscriminatorNodes();
}
