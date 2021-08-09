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
 * Thrown when an unknown object is read from a log file
 */
// TODO Delete this file. Now use CLassNotFound exception instead
public final class LogFileClassNotFoundException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -3946808296497648020L;

    protected LogFileClassNotFoundException(Object source)
    {
        super(source,
              LogFileClassNotFoundException.class // No matching error message.
        );
    } // LogFileClassNotFoundException().
} // class LogFileClassNotFoundException.
