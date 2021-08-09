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
 * This exception is thrown to indicate an attempt has been made to use
 * an invalid BeanId. <p>
 */

public class InvalidBeanIdException
                extends ContainerException
{
    private static final long serialVersionUID = -9118212346389086271L;

    /**
     * Create a new <code>InvalidBeanIdException</code> instance. <p>
     */
    public InvalidBeanIdException() {
        super();
    } // InvalidBeanIdException

    /**
     * Create a new <code>InvalidBeanIdException</code> instance
     * with the specified nested excpeption. <p>
     */
    // d356676.1
    public InvalidBeanIdException(Throwable exception)
    {
        super(exception);
    } // InvalidBeanIdException

} // InvalidBeanIdException
