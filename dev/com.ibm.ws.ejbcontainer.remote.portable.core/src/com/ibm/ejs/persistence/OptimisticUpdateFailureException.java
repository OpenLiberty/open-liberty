/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.persistence;

public class OptimisticUpdateFailureException extends EJSPersistenceException
{
    private static final long serialVersionUID = -4880895633333380831L;

    /**
     * Create a new EJSPersistenceException with an empty description string.
     */
    public OptimisticUpdateFailureException()
    {
        // intentionally left blank.
    }

    /**
     * Create a new OptimisticUpdateFailureException with the associated
     * string description.
     * 
     * @param s the <code>String</code> describing the exception.
     */
    public OptimisticUpdateFailureException(String s)
    {
        super(s);
    }

    /**
     * Create a new OptimisticUpdateFailureException with the associated string
     * description and nested exception.
     * 
     * @param s the <code>String</code> describing the exception.
     */
    public OptimisticUpdateFailureException(String s, Throwable ex)
    {
        super(s, ex);
    }

    /**
     * Create a new OptimisticUpdateFailureException with a nested exception.
     * 
     * @param s the <code>String</code> describing the exception.
     */
    public OptimisticUpdateFailureException(Throwable ex)
    {
        super(ex);
    }

    /**
     * Create a new OptimisticUpdateFailureException with the associated error code.
     * 
     * @param s the <code>String</code> describing the exception.
     */
    public OptimisticUpdateFailureException(int error)
    {
        super(error);
    }

} // OptimisticUpdateFailureException
