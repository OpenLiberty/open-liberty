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
 * Thrown when a request is made for a set of statistics that is unknown.
 */
public final class StatisticsNameNotFoundException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -356341809168269499L;

    protected StatisticsNameNotFoundException(Object source,
                                              String name)
    {
        super(source,
              StatisticsNameNotFoundException.class,
              new Object[] { name });
    } // StatisticsNameNotFoundException().
} // class StatisticsNameNotFoundException.
