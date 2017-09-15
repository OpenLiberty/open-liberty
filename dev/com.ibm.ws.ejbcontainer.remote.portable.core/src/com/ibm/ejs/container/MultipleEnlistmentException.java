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
 * This exception is thrown whenever an attempt is made to
 * enlist a <code>BeanO</code> in a transaction it is already
 * enlisted with. <p>
 * 
 * During normal operation, this exception will not be thrown. It
 * indicates that an internal error occurred. <p>
 */

public class MultipleEnlistmentException
                extends ContainerInternalError
{
    private static final long serialVersionUID = 7592848777683128945L;

    /**
     * Create a new <code>MultipleEnlistmentException</code>
     * instance. <p>
     */

    public MultipleEnlistmentException() {
        super();
    } // MultipleEnlistmentException

} // MultipleEnlistmentException
