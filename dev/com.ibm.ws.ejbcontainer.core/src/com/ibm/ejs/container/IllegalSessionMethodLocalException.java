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
package com.ibm.ejs.container;

/**
 * This exception is thrown when an attempt is made to invoke
 * getPrimaryKey() or remove(Object primaryKey) on a session bean.
 **/
public class IllegalSessionMethodLocalException
                extends ContainerLocalException
{
    private static final long serialVersionUID = 2467827007726460881L;

    /**
     * Create a new <code>IllegalSessionMethodLocalException</code> instance.
     */
    public IllegalSessionMethodLocalException()
    {
        super();
    }

} // IllegalSessionMethodLocalException
