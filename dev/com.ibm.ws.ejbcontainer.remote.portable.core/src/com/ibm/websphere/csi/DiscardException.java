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
package com.ibm.websphere.csi;

/**
 * <code>EJBCache</code> throws a <code>DiscardException</code> when something
 * goes wrong during <code>removeAndDiscard()</code> or
 * <code>evictObject</code>. It is a wrapper for the
 * original exception which was thrown and any object which may have been
 * [partially] constructed and initialized.
 * 
 * Typically you'll want to clean up and destroy the object as appropriate
 * and rethrow the original exception.
 * <p>
 * 
 * @see Cache
 * @see Cache#pin
 * @see Cache#unpin
 * 
 */

public class DiscardException extends Exception
{
    private static final long serialVersionUID = -7817264273424200849L;

    /**
     * Constructs a <code>DiscardException</code> object, identifying the
     * exception and object which caused an error during a fault-in
     * <p>
     * 
     * @param e The exception which was originally thrown
     * @param key The key on which the exception was thrown
     * 
     */
    public DiscardException(Exception e, Object key)
    {
        orgException = e;
        this.object = key;
    }

    public final Exception orgException;
    public final Object object;

}
