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
 * Thrown when the log file is to full to write a logRecord or reserve space.
 * 
 * @param source the Object throwing the exception.
 * @param newSpaceAllocatedInLogFile the number of bytes needed in the log file including any logRecord and overhead.
 * @param reservedDelta the number of bytes requested to be reserved.
 * @param the abbount of space that is currently available in the log.
 */
public final class LogFileFullException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 7458663820401900531L;

    protected LogFileFullException(Object source,
                                   long newSpaceAllocatedInLogFile,
                                   long reservedDelta,
                                   long available)
    {
        super(source,
              LogFileFullException.class,
              new Object[] { new Long(newSpaceAllocatedInLogFile), new Long(reservedDelta), new Long(available) });
    } // LogFileFullException(). 
} // class LogFileFullException.
