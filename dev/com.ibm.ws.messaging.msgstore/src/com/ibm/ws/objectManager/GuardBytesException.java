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
 * Thrown when an invalid guard byte is found.
 */
public final class GuardBytesException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 8186135736870686061L;

    /**
     * StateErrorException.
     * 
     * @param Object which is throwing this StateErrorException.
     * @param Object protected by the guard bytes.
     */
    protected GuardBytesException(Object source, Object target)
    {
        super(source,
              GuardBytesException.class
              , new Object[] { source, target });
    } // GuardBytesException(). 
} // class GuardBytesException.
