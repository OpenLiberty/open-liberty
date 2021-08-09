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

/**
 * Represents a Config Snippet Resource in a repository.
 * <p>
 * This interface allows read access to fields which are specific to config snippets.
 */
public interface ConfigSnippetResource extends RepositoryResource, ApplicableToProduct {

    /**
     * Gets the list of required features for this config snippet
     *
     * @return The list of required features for this config snippet, or null if no features are required
     */
    public Collection<String> getRequireFeature();

}
