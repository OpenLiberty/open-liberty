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

import com.ibm.ws.objectManager.utils.Trace;
import com.ibm.ws.objectManager.utils.Tracing;

/**
 * A starter implementation of the Set interface.
 * This is a new Set because AbstractCollection extends ManagedObject.
 * 
 * @see Set
 * @see AbstractCollection
 * @see AbstractSetView
 */
public abstract class AbstractSet
                extends AbstractCollection
                implements Set
{
    private static final Class cclass = AbstractSet.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(AbstractSet.class,
                                                                     ObjectManagerConstants.MSG_GROUP_MAPS);

    /**
     * Default no argument constructor.
     */
    protected AbstractSet()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
        {
            trace.entry(this, cclass, "<init>");
            trace.exit(this, cclass, "<init>");
        }
    } // AbstractSet().
} // AbstractSet.

