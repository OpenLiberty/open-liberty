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
 * Thrown when a helper thread is requested to perform an action while th thread is not running.
 * This might happen because the thread is asked to perform an operation after it has been asked
 * to shut down.
 * 
 * @param source the Object throwing the exception.
 * @param threadName the name of the thread which is being asked to perform the request.
 * @param requestName describes the request being made.
 */
public final class ThreadNotRunningException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 3843616808311058994L;

    protected ThreadNotRunningException(Object source,
                                        String threadName,
                                        String requestName)
    {
        super(source,
              ThreadNotRunningException.class,
              new Object[] { threadName, requestName });
    } // ThreadNotRunningException(). 
} // class ThreadNotRunningException.
