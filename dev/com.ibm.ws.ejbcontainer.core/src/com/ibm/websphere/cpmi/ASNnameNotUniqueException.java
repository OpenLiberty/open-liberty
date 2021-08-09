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
package com.ibm.websphere.cpmi;

public class ASNnameNotUniqueException extends CPMIException
{
    private static final long serialVersionUID = -5462731871331978475L;

    /**
     * Create a new CPMIException with an empty description string.
     */
    public ASNnameNotUniqueException()
    {
        super();
    }

    /**
     * Create a new CPMIException with the associated string description. <p.
     * 
     * @param s the <code>String</code> describing the exception <p>
     */
    public ASNnameNotUniqueException(String s)
    {
        super(s);
    }

    /**
     * Create a new CPMIException with the associated string description and
     * nested exception. <p>
     * 
     * @param s the <code>String</code> describing the exception <p>
     * 
     * @param ex the nested <code>Throwable</code>
     */
    public ASNnameNotUniqueException(String s, Throwable ex)
    {
        super(s, ex);
    }
}
