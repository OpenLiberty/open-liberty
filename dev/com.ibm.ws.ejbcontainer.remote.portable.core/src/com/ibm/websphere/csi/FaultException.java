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
 * <code>EJBCache</code> throws a <code>FaultException</code> when something
 * goes wrong during <code>findAndFault()</code>. It is a wrapper for the
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

public class FaultException extends Exception
{
    private static final long serialVersionUID = 7156106138967365539L;

    /**
     * Constructs a <code>FaultException</code> object, identifying the
     * exception and object which caused an error during a fault-in
     * <p>
     * 
     * @param e The exception which was originally thrown
     * @param object The object which was created during the fault-in, or
     *            null if no object was constructed
     * 
     */
    public FaultException(Exception e, Object object)
    {
        super(object == null ? null : object.toString(), e); // d193690
        orgException = e;
        this.object = object;
    }

    // orgException is no longer used, but still exists for serialization
    // compatibility.... and the name of the instance variable 'object' must
    // also be maintained for serialization compatibility.               LI3706-7
    public final Exception orgException;
    public final Object object;

}
