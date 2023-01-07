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
 * Thrown when no CheckpointEndLogRecord was read from the log during a warm start.
 * 
 * @param Object throwing the exception.
 * @param String the name of the log file that was read.
 */
public final class CheckpointEndNotFoundException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 323526130366347326L;

    protected CheckpointEndNotFoundException(Object source,
                                             String logFileName)
    {
        super(source,
              CheckpointEndNotFoundException.class,
              new Object[] { logFileName });

    } // CheckpointEndNotFoundException().
} // class CheckpointEndNotFoundException.
