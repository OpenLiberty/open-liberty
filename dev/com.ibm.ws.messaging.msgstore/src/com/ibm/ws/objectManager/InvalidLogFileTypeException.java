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
 * Thrown when the object manager is asked to instantiate with a log file
 * of an unknown type.
 */
public final class InvalidLogFileTypeException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 189513734879231033L;

    /**
     * An invalid log file type was passed to the ObjectManager constructor.
     * 
     * @param Object which is throwing this Exception.
     * @param Exception which was caught.
     */
    protected InvalidLogFileTypeException(Object source,
                                          int logFileType)
    {
        super(source,
              InvalidLogFileTypeException.class,
              new Integer(logFileType));

    } // InvalidLogFileTypeException().

} // End of class InvalidLogFileTypeException.
