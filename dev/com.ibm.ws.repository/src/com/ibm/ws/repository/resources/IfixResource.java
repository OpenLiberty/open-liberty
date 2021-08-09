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
import java.util.Date;

/**
 * Represents an IFix Resource in a repository.
 * <p>
 * This interface allows read access to fields which are specific to ifixes.
 */
public interface IfixResource extends RepositoryResource, ApplicableToProduct {

    /**
     * Gets the list of APAR IDs which are provided in this ifix resource
     *
     * @return the list of APAR IDs, or null if no APAR IDs are provided
     */
    public Collection<String> getProvideFix();

    /**
     * Gets the modified date of the most recently modified file to be replaced by the update
     *
     * @return the date, or null if it has not been set
     */
    public Date getDate();

}
