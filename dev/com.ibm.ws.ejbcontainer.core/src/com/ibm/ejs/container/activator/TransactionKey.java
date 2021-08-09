/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation and others.
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
import com.ibm.ejs.container.ContainerTx;

/**
 * A <code>TransactionKey</code> wraps a (ContainerTx, BeanId) pair with
 * appropriate hashCode(), equals methods so the pair can be used as
 * key into the BeanO cache. <p>
 */

class TransactionKey
{
    /**
     * Transaction this cache key is associated with.
     */
    final ContainerTx tx;

    /**
     * BeanId this cache key associated with.
     */
    BeanId id;

    /**
     * Hash value. Cached for performance.
     */
    // d140003.28
    final int hashValue;

    /**
     * Create new <code>TransactionKey</code> instance associated with
     * given transaction and bean id.
     */
    TransactionKey(ContainerTx tx, BeanId id)
    {
        this.tx = tx;
        this.id = id;

        // When a TransactionKey is created, it will generally be used for
        // at least to cache lookups, so cache the hash code value.     d140003.28
        this.hashValue = (tx != null ? tx.hashCode() : 0) + id.hashCode();
    } // TransactionKey

    /**
     * Return hash value appropriate for this (tx, beanId) pair.
     */
    public final int hashCode()
    {
        return hashValue;
    } // hashCode

    /**
     * Returns true iff the supplied object wraps the same (tx, beanId)
     * pair.
     * 
     * This override of the default Object.equals is required, even though
     * there are type specific overloads, in case the caller does not have
     * (or know) the parameter as the specific type. <p>
     */
    public final boolean equals(Object o)
    {
        if (o instanceof TransactionKey)
        {
            TransactionKey key = (TransactionKey) o;

            // Note that the ContainerTx must be the exact same object,
            // whereas BeanId just be 'equals'.                          d140003.28
            if (tx == key.tx && (id == key.id || id.equals(key.id)))
            {
                // If they are equal, then set the id's to the same object
                // so subsequent comparisons are faster....                d154342.1
                id = key.id;
                return true;
            }
        }

        return false;
    } // equals

    /**
     * Returns true iff the supplied object wraps the same (tx, beanId)
     * pair.
     * 
     * This type specific version is provided for performance, and avoids
     * any instanceof or casting. <p>
     */
    // d195605
    public final boolean equals(TransactionKey key)
    {
        if (key != null) // d273615
        {
            // Note that the ContainerTx must be the exact same object,
            // whereas BeanId just be 'equals'.                          d140003.28
            if (tx == key.tx && (id == key.id || id.equals(key.id)))
            {
                // If they are equal, then set the id's to the same object
                // so subsequent comparisons are faster....                d154342.1
                id = key.id;
                return true;
            }
        }

        return false;
    } // equals

    public String toString()
    {
        return "TransactionKey(" + tx + ", " + id + ")";
    } // toString

} // TransactionKey
