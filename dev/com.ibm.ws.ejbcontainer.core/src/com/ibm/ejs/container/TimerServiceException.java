/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import javax.ejb.EJBException;

import com.ibm.ejs.container.util.ExceptionUtil;

/**
 * TimerServiceException is a subclass of EJBException used to report
 * system-level failures that occur during Timer Service processing.
 **/
public class TimerServiceException extends EJBException
{
    private static final long serialVersionUID = 517416094460234231L;

    /**
     * Constructor that takes a message and root / cause exception.
     *
     * @param message informational text about the exception, which
     *            should indicate the identity of either the
     *            bean or timer experiencing a problem.
     * @param cause the root or cause exception that occurred that
     *            resulted in this TimerServiceException.
     **/
    public TimerServiceException(String message, Throwable cause)
    {
        super(message, ExceptionUtil.Exception(cause));
    }
    
    /**
     * Constructor that takes a message.
     *
     * @param message informational text about the exception, which
     *            should indicate the identity of either the
     *            bean or timer experiencing a problem.
     **/
    public TimerServiceException(String message)
    {
        super(message);
    }
}
