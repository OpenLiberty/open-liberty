/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
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
 * An extension of {@link com.ibm.wsspi.resource.ResourceFactory} that handles
 * additional lifecycle methods in support of {@link ResourceFactoryBuilder}.
 */
public interface ResourceFactory
                extends com.ibm.wsspi.resource.ResourceFactory
{
    /**
     * The optional service registry property that specifies the JNDI name
     * relative to java:comp. This should only be used for "java:comp/DefaultX".
     * This property will typically be specified in defaultInstances.xml.
     */
    String JAVA_COMP_DEFAULT_NAME = "javaCompDefaultName";

    /**
     * Creates a resource handle of the specified type that respects the
     * specified resource reference information. The {@link ResourceRefInfo#getType} must match the {@link #CREATES_OBJECT_CLASS} property.
     *
     * @param info the resource reference information, or null if unavailable
     * @return the resource handle
     * @throws Exception a resource-specific exception
     * @see ResourceRefConfigFactory#createResourceRefConfig
     */
    Object createResource(ResourceRefInfo info)
                    throws Exception;

    /**
     * Destroy this resource factory.
     *
     * @throws Exception if an error occurs.
     * @since F46613-58054
     */
    void destroy() throws Exception;

    /**
     * Modify this resource factory.
     *
     * @param props typed name/value pairs representing the configuration.
     * @throws Exception if an error occurs.
     * @throws UnsupportedOperationException if this resource factory does not support modifications.
     * @since F46613-58054
     */
    void modify(Map<String, Object> props) throws Exception;
}
