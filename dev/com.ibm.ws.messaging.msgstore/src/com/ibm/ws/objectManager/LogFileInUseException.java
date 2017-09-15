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
 * Thrown when an attempt is made to use a logFile that is already being used by another program.
 * 
 * @param String the name of the log file.
 */
public final class LogFileInUseException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -795499354868379899L;

    protected LogFileInUseException(Object source,
                                    String logFileName)
    {
        super(source,
              LogFileInUseException.class,
              new Object[] { logFileName });
    } // LogFileInUseException.
} // class LogFileInUseException.
