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
 * Thrown when the physical size of the log file is found to be shorter than the
 * LogHeader claims it should be.
 * 
 * @param Object throwing the exception.
 * @param String the name of the log file.
 * @param long the expected size of the log file.
 * @param long the byte found to be beyond the physical end of the file.
 */
public final class PrematureEndOfLogFileException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -5814027892097475805L;

    protected PrematureEndOfLogFileException(Object source,
                                             String logFileName
                                             , long expectedSize
                                             , long byteAccessed)
    {
        super(source,
              PrematureEndOfLogFileException.class,
              new Object[] { logFileName,
                            new Long(expectedSize),
                            new Long(byteAccessed) });

    } // PrematureEndOfLogFileException.
} // class PrematureEndOfLogFileException.