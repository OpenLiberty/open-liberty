/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.resource;

import java.util.Map;

/**
 * Builder for resource factories. Implementations should be registered in the
 * service registry with the {@link ResourceFactory#CREATES_OBJECT_CLASS} property.
 */
public interface ResourceFactoryBuilder
{
    /**
     * Creates a resource factory that creates handles of the type specified
     * by the {@link ResourceFactory#CREATES_OBJECT_CLASS} property.
     *
     * @param props the resource-specific type information
     * @return the resource factory
     * @throws Exception a resource-specific exception
     */
    public ResourceFactory createResourceFactory(Map<String, Object> props)
                    throws Exception;

    public boolean removeExistingConfigurations(String filter) throws Exception;
}
