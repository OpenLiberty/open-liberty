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
 * Thrown when an unexpected condition was found, indicating an internal Logic error.
 * 
 * @param Object
 *            discovering the invalid condition and throwing the exception.
 * @param String
 *            describing the variable containing the invalid value.
 * @param String
 *            describing the invalid value.
 */
public final class InvalidConditionException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 6077051984304754182L;

    protected InvalidConditionException(Object source,
                                        String variableName,
                                        String value)
    {
        super(source,
              InvalidConditionException.class,
              new Object[] { source,
                            variableName,
                            value });
    } // InvalidConditionException().
} // class InvalidConditionException.
