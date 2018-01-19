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

/**
 * Interface to indicate that this resource is something that can hold an applies to field indicating which products it is applicable to.
 */
public interface ApplicableToProduct {

    /**
     * Gets the appliesTo field associated with the resource
     *
     * @return The appliesTo field associated with the resource, or null if it has not been set
     */
    public String getAppliesTo();

}