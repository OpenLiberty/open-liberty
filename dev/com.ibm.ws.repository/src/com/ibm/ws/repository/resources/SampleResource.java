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
package com.ibm.ws.repository.resources;

import java.util.Collection;

import com.ibm.ws.repository.common.enums.ResourceType;

/**
 * Represents a Sample Resource in a repository.
 * <p>
 * This interface allows read access to fields which are specific to samples.
 * <p>
 * Samples represented by this interface can either be of type {@link ResourceType#PRODUCTSAMPLE} or {@link ResourceType#OPENSOURCE}.
 */
public interface SampleResource extends RepositoryResource, ApplicableToProduct {

    /**
     * Gets the list of required features for this sample
     *
     * @return The list of required features for this sample, or null if it has not been set
     */
    public Collection<String> getRequireFeature();

    /**
     * Gets the short name for this sample
     * <p>
     * The short name should be the name of the server included in the sample.
     *
     * @return The short name for this sample, or null if it has not been set
     */
    public String getShortName();

}
