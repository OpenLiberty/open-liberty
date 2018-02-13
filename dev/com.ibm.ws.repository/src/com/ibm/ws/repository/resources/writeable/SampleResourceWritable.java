/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resources.writeable;

import java.util.Collection;

import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.resources.SampleResource;

/**
 * Represents a Sample Resource which can be uploaded to a repository.
 * <p>
 * This interface allows write access to fields which are specific to samples.
 * <p>
 * Samples represented by this interface can either be of type {@link ResourceType#PRODUCTSAMPLE} or {@link ResourceType#OPENSOURCE}.
 */
public interface SampleResourceWritable extends SampleResource, RepositoryResourceWritable, ApplicableToProductWritable {

    /**
     * Sets the list of required features for this sample
     *
     * @param requireFeature the list of symbolic names of required features
     */
    public void setRequireFeature(Collection<String> requireFeature);

    /**
     * Sets the short name for this sample
     * <p>
     * The short name should be the name of the server included in the sample.
     *
     * @param shortName The short name for this sample
     */
    public void setShortName(String shortName);

    /**
     * Sets the type of the sample
     * <p>
     * Samples can either be of type {@link ResourceType#PRODUCTSAMPLE} or {@link ResourceType#OPENSOURCE}.
     *
     * @param type the type of the sample
     */
    public void setType(ResourceType type);

}