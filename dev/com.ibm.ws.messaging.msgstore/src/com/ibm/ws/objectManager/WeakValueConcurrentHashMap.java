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

public class WeakValueConcurrentHashMap extends ConcurrentHashMap
{
    private static final Class cclass = WeakValueConcurrentHashMap.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_MAPS);

    public WeakValueConcurrentHashMap(int subMapCount)
    {
        super(subMapCount);
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
        {
            trace.entry(this, cclass, "<init>", new Integer(subMapCount));
            trace.exit(this, cclass, "<init>");
        }
    }

    java.util.Map makeSubMap()
    {
        return new WeakValueHashMap();
    }
} // class WeakValueConcurrentHashMap.
