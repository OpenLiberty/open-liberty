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
 * This exception is thrown to indicate a unknown container error has occurred.
 **/
public class UnknownLocalException extends ContainerLocalException
{
    private static final long serialVersionUID = -2906017836291024122L;

    public UnknownLocalException()
    {}

    public UnknownLocalException(java.lang.String message)
    {
        super(message);
    }

    public UnknownLocalException(java.lang.String message, java.lang.Exception ex)
    {
        super(message, ex);
    }
} // UnknownLocalException
