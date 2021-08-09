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
 * Factory for creating resource reference configuration and lists.
 * <p>
 * This interface is not intended to be implemented by clients.
 */
public interface ResourceRefConfigFactory
{
    /**
     * Creates a new {@link ResourceRefConfig} object.
     *
     * @param type the non-null interface type of the requested resource.
     */
    ResourceRefConfig createResourceRefConfig(String type);

    /**
     * Creates a new {@link ResourceRefConfigList} object.
     */
    ResourceRefConfigList createResourceRefConfigList();
}
