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
 * Thrown when an unexpected exception is thrown and caught by the ObjectManager.
 * 
 * @param Object where the exception was caught generating this exception.
 * @param Exception that was thrown.
 */
public final class UnexpectedExceptionException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 544590594159839300L;

    protected UnexpectedExceptionException(Object source,
                                           Exception exception)
    {
        super(source,
              UnexpectedExceptionException.class,
              exception,
              exception);
    } // End of Constructor.
} // End of class UnexpectedExceptionException.
