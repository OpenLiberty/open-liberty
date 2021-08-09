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
 * This exception is thrown whenever an attempt to passivate a
 * <code>BeanO</code> instance fails. <p>
 * 
 */

public class BeanOPassivationFailureException
                extends ContainerException
{
    private static final long serialVersionUID = 8894821895189549685L;

    public BeanOPassivationFailureException() {
        super();
    }
}
