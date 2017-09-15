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
 * This exception is thrown when an attempt is made to install a bean
 * into the container with an illegal use of the TX_BEAN_MANAGED
 * transaction attribute. <p>
 * 
 * There are two cases where the TX_BEAN_MANAGED transaction attribute are
 * illegal. It cannot be used with entity beans and it cannot be mixed
 * with any other transaction attribute, i.e. if one method on the bean
 * is marked with it then all methods on the bean must be marked with
 * it. <p>
 */

public class IllegalBMTxAttrException
                extends ContainerException
{
    private static final long serialVersionUID = -4603137228938112818L;

    /**
     * Create a new <code>IllegalBMTxAttrException</code>
     * instance. <p>
     */

    public IllegalBMTxAttrException() {
        super();
    } // IllegalBMTxAttrException

} // IllegalBMTxAttrException
