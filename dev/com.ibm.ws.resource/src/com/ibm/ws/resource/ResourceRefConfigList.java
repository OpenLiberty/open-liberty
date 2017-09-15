/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.resource;

/**
 * A list/map of resource reference configuration.
 * <p>
 * This interface is not intended to be implemented by clients.
 */
public interface ResourceRefConfigList
{
    /**
     * @return the number of resource references in the list.
     */
    int size();

    /**
     * @param i the index
     * @return the resource reference at the specified index
     */
    ResourceRefConfig getResourceRefConfig(int i);

    /**
     * Finds resource reference configuration by name.
     *
     * @param name the name
     * @return the resource reference configuration, or null
     */
    ResourceRefConfig findByName(String name);

    /**
     * Finds resource reference configuration by name, or creates a new resource
     * reference configuration and adds it to the list.
     *
     * @param name the name
     * @return the resource reference configuration
     */
    ResourceRefConfig findOrAddByName(String name);
}
