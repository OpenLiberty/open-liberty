/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
 * This exception is thrown when EJB container detects that the user
 * configuration of an EJB is incorrect or invalid. The configuration error
 * can be either incorrect use of either annotation and/or xml.
 */
public class EJBConfigurationException extends Exception
{
    private static final long serialVersionUID = 3204992112732695704L;

    public EJBConfigurationException()
    {
        // intentionally left blank.
    }

    public EJBConfigurationException(String detailMessage)
    {
        super(detailMessage);
    }

    public EJBConfigurationException(Throwable throwable)
    {
        super(throwable);
    }

    public EJBConfigurationException(String detailMessage, Throwable throwable)
    {
        super(detailMessage, throwable);
    }

}
