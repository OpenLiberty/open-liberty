package com.ibm.ws.objectManager;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * Thrown when an attempt is made to locate a log file which does not exist or could not be created.
 * 
 * @version @(#) 1/25/13
 * @author Andrew_Banks
 */
public final class NonExistentLogFileException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 8292128603593836549L;

    /**
     * @param objectManagerState that is unable to locate the log file.
     * @param exception caught by the ObjectManagerstate.
     * @param logFileName the log file name.
     */
    protected NonExistentLogFileException(ObjectManagerState objectManagerState,
                                          Exception exception,
                                          String logFileName)
    {
        super(objectManagerState,
              NonExistentLogFileException.class,
              exception,
              new Object[] { objectManagerState,
                            exception,
                            logFileName });
    } // NonExistentLogFileException().
} // class NonExistentLogFileException.