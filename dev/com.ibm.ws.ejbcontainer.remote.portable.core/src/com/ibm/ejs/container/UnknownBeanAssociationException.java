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
 * This exception is thrown whenever an attempt is made to get a bean
 * via an unknown bean association. <p>
 * 
 */

public class UnknownBeanAssociationException
                extends ContainerException
{
    private static final long serialVersionUID = 7877119018214074490L;

    /**
     * Create a new <code>UnknownBeanAssociationException</code>
     * instance. <p>
     */

    public UnknownBeanAssociationException(String s) {
        super(s);
    } // UnknownBeanAssociationException

} // UnknownBeanAssociationException
