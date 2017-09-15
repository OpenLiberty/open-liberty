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
 * Thrown when you request that the log file size be reduced and it cannot contain the
 * existing log data.
 * 
 * @param LogOutput throwing this exception.
 * @param long the current size of the log file.
 * @param long the requested size of the log.
 * @param long the available space remaining in the log file.
 * @param float the ocupancy of the log if the new size were to be applied.
 * @param float the maximum occupancy before the ObjectManager triggers a checkpoint.
 */
public final class LogFileSizeTooSmallException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -8054778811799273018L;

    protected LogFileSizeTooSmallException(LogOutput logOutput
                                           , long currentSize
                                           , long requestedSize
                                           , long availableSize
                                           , float newOcupany
                                           , float logFullPostCheckpointThreshold)
    {
        super(logOutput,
              LogFileSizeTooSmallException.class,
              new Object[] { new Long(currentSize)
                            , new Long(requestedSize)
                            , new Long(availableSize)
                            , new Float(newOcupany)
                            , new Float(logFullPostCheckpointThreshold) });

    } // LogFileSizeTooSmallException().
} // class LogFileSizeTooSmallException.