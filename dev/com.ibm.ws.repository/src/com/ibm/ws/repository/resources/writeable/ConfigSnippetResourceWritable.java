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

import com.ibm.ws.repository.resources.ConfigSnippetResource;

/**
 * Represents a Config Snippet Resource in a repository.
 * <p>
 * This interface allows read access to fields which are specific to config snippets.
 */
public interface ConfigSnippetResourceWritable extends ConfigSnippetResource, RepositoryResourceWritable, ApplicableToProductWritable {

    /**
     * Sets the list of required features for this config snippet
     *
     * @param requireFeature the list of symbolic names of required features
     */
    public void setRequireFeature(Collection<String> requireFeature);

}