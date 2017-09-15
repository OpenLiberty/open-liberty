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
 * This exception is thrown to indicate a container transaction error has
 * occurred of a Local Interface. This functions in parallel to the
 * InvalidTransactionException of the remote interface.
 **/
public class InvalidTransactionLocalException
                extends ContainerLocalException
{
    private static final long serialVersionUID = -7960288866356505362L;

    public InvalidTransactionLocalException()
    {}

    public InvalidTransactionLocalException(java.lang.String message)
    {
        super(message);
    }

    public InvalidTransactionLocalException(java.lang.String message,
                                            java.lang.Exception ex)
    {
        super(message, ex);
    }

} // InvalidTransactionLocalException
