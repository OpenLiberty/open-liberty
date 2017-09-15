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
 * This exception is thrown whenever an error internal to the EJS container
 * has occurred. <p>
 * 
 * In general, this exception must not be masked and must at some point
 * cause a fatal error to occur. <p>
 * 
 */

public class ContainerInternalError
                extends ContainerException
{
    private static final long serialVersionUID = -1736348221294252132L;

    public ContainerInternalError(Throwable ex) {
        super("", ex);
    }

    public ContainerInternalError() {
        super("");
    }
} // ContainerInternalError
