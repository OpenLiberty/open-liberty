package com.ibm.ws.objectManager.utils;

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
 * @author Andrew_Banks
 * 
 *         Wrapper for Exception.
 */
public abstract class UtilsException
                extends Exception
{
    /**
     * @param message NLS message.
     */
    public UtilsException(String message)
    {
        super(message);
    }

    /**
     * @param message NLS message.
     * @param cause undelying exception causing this one.
     */
    public UtilsException(String message,
                          Throwable cause) {
        super(message,
              cause);
    }

    /**
     * @param cause undelying exception causing this one.
     */
    public UtilsException(Throwable cause)
    {
        super(cause);
    }

} // class UtilsException.
