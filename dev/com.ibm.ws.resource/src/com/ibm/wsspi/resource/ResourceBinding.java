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

import java.util.Collection;

/**
 * Information about a resource binding.
 * <p>
 * This interface is not intended to be implemented by clients.
 */
public interface ResourceBinding
{
    /**
     * Returns the reference name, or null if this event is not for a reference.
     */
    String getReferenceName();

    /**
     * Returns the type of the resource name (for example, {@code javax.sql.DataSource}).
     */
    String getTypeName();

    /**
     * Returns the current binding name for the resource.
     */
    String getBindingName();

    /**
     * Returns a property value. The list of property names varies depending on
     * the type and declaration of the resource.
     *
     * @param name the property name
     * @return the property value, or null if not found
     * @see #getPropertyNames
     */
    Object getProperty(String name);

    /**
     * Returns an immutable collection of property names that will return a
     * non-null value from {@link #getProperty}.
     *
     * @return a non-null immutable collection of property names
     */
    Collection<String> getPropertyNames();

    /**
     * Sets the binding name for the resource.
     */
    void setBindingName(String name);
}
