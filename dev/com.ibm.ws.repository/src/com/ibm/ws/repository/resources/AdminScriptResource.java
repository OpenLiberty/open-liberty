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
 * Represents an Admin Script Resource in a repository.
 * <p>
 * This interface allows read access to fields which are specific to admin scripts.
 */
public interface AdminScriptResource extends RepositoryResource, ApplicableToProduct {

    /**
     * Get the language that the script is written in
     *
     * @return the language the script is written in, or null if it has not been set
     */
    public String getScriptLanguage();

    /**
     * Gets the list of required features for this admin script
     *
     * @return The list of required features for this admin script, or null if no features are required
     */
    public Collection<String> getRequireFeature();

}
