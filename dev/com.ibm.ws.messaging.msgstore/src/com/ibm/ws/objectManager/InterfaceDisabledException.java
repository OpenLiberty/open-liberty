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
 * Thrown when an attempt is made to use an interfaces that is disabled in this build.
 * 
 * @param Object insrtancse containing the disabled interface.
 * @param String the methodname that is disabled.
 */
public final class InterfaceDisabledException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -5181515899320029031L;

    protected InterfaceDisabledException(Object source,
                                         String methodName)
    {
        super(source,
              InterfaceDisabledException.class,
              new Object[] { source,
                            methodName });
    } // InterfaceDisabledException().
} // class InterfaceDisabledException.
