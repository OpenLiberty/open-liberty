package com.ibm.tx.jta;
/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

public class XAResourceNotAvailableException extends Exception 
{
    protected static final long serialVersionUID = -3663835677857622787L;

    public Throwable detail;

    public XAResourceNotAvailableException(Throwable t)
    {
        super(t);        
        detail = t;
    }
    
    public XAResourceNotAvailableException()
    {
        super();
    }
}
