/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.resource;

/**
 * Factory for creating resource configuration.
 * <p>
 * This interface is not intended to be implemented by clients.
 */
public interface ResourceConfigFactory
{
    /**
     * Creates a new {@link ResourceConfig} object.
     *
     * @param type the non-null interface type of the requested resource.
     */
    ResourceConfig createResourceConfig(String type);
}
