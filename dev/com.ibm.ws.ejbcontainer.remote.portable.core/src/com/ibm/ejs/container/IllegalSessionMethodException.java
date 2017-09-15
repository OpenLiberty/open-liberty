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
 * This exception is thrown when an attempt is made to invoke
 * getPrimaryKey() or remove(Object primaryKey) on a session bean. <p>
 */

public class IllegalSessionMethodException
                extends ContainerException
{
    private static final long serialVersionUID = 3020565125536306536L;

    /**
     * Create a new <code>IllegalSessionMethodException</code> instance. <p>
     */

    public IllegalSessionMethodException() {
        super();
    } // IllegalSessionMethodException

} // IllegalSessionMethodException
