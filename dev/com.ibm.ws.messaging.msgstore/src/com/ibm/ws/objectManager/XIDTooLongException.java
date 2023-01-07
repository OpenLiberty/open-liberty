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
 * Thrown when an attempt is made to set an XID which is longer than java.lang.Short.MAX_VALUE. program.
 * 
 * @param Object throwing the exception.
 * @param int the length of the XID.
 */
public final class XIDTooLongException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 1776711044329464846L;

    protected XIDTooLongException(Object source,
                                  int length)
    {
        super(source,
              XIDTooLongException.class,
              new Object[] { new Integer(length) });
    } // XIDTooLongException().
} // class XIDTooLongException.
