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
import java.util.Date;

import com.ibm.ws.repository.resources.IfixResource;

/**
 * Represents an IFix Resource which can be uploaded to a repository.
 * <p>
 * This interface allows write access to fields which are specific to ifixes.
 */
public interface IfixResourceWritable extends IfixResource, RepositoryResourceWritable, WebDisplayable, ApplicableToProductWritable {

    /**
     * Sets the list of APAR IDs which are provided in the ifix resource
     *
     * @param provides the list of APAR IDs
     */
    public void setProvideFix(Collection<String> provides);

    /**
     * Sets the modified date of the most recently modified file to be replaced by the update
     *
     * @param date the date
     */
    public void setDate(Date date);

}