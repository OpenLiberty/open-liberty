/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.activator;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.ContainerAS;

/**
 * A <code>SessionKey</code> wraps a (ContainerAS, BeanId) pair with
 * appropriate hashCode(), equals methods so the pair can be used as
 * key into the BeanO cache. <p>
 */
class SessionKey
{
    /**
     * Activity Session this cache key is associated with.
     */
    ContainerAS as;

    /**
     * BeanId this cache key associated with.
     */
    BeanId id;

    /**
     * Create new <code>ActivationKey</code> instance associated with
     * given transaction and bean id.
     */
    SessionKey(ContainerAS as, BeanId id)
    {
        this.as = as;
        this.id = id;
    } // ActivationKey

    /**
     * Return hash value appropriate for this (as, beanId) pair.
     */
    public final int hashCode()
    {
        return (as != null ? as.hashCode() : 0) + id.hashCode();
    } // hashCode

    /**
     * Returns true iff the supplied object wraps the same (as, beanId)
     * pair.
     * 
     * This override of the default Object.equals is required, even though
     * there are type specific overloads, in case the caller does not have
     * (or know) the parameter as the specific type. <p>
     */
    public final boolean equals(Object o)
    {
        if (o instanceof SessionKey)
        {
            SessionKey key = (SessionKey) o;
            return ((as != null ? as.equals(key.as)
                            : key.as == null) && id.equals(key.id));
        }

        return false;
    } // equals

    /**
     * Returns true iff the supplied object wraps the same (as, beanId)
     * pair.
     * 
     * This type specific version is provided for performance, and avoids
     * any instanceof or casting. <p>
     */
    // d195605
    public final boolean equals(SessionKey key)
    {
        if (key != null) // d273615
        {
            return ((as != null ? as.equals(key.as)
                            : key.as == null) && id.equals(key.id));
        }
        return false; // d273615
    } // equals

    public String toString()
    {
        return "ActivationKey(" + as + ", " + id + ")";
    } // toString

} // ActivationKey
