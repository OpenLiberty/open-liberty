package com.ibm.ws.objectManager;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * Thrown when there are no more log records to read from the Log File.
 * 
 * @param Object
 *            throwing this exception.
 * @param Exception
 *            causing this one to be raised.
 */
public final class LogFileExhaustedException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 6570268559070389810L;

    protected LogFileExhaustedException(Object source,
                                        Exception underlyingException)
    {
        super(source,
              LogFileExhaustedException.class,
              underlyingException,
              new Object[] { source,
                            underlyingException });
    } // LogFileExhaustedException().
} // class LogFileExhaustyedException.
