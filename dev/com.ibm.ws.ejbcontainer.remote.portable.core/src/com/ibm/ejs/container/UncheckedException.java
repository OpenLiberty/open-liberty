/*******************************************************************************
 * Copyright (c) 1998, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

/**
 * An unchecked exception is thrown whenever the invocation of a bean
 * method raises an exception that the bean's method signature did
 * not explicitly declare. <p>
 */

public class UncheckedException
                extends java.rmi.RemoteException
{
    private static final long serialVersionUID = 4008328554878328030L;

    /**
     * Create a new <code>UncheckedException</code> instance. <p>
     * 
     * @param s the <code>String</code> describing the unchecked
     *            exception that was raised <p>
     * 
     * @param ex the <code>Throwable</code> that is the unchecked
     *            exception <p>
     */

    public UncheckedException(String s, Throwable ex) {
        super(s, ex);
    } // UncheckedException

} // UncheckedException
