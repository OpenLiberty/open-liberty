package com.ibm.ws.objectManager.utils;

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
 * This class holds a reference to the trace object we use to check
 * if any tracing is enabled at all. Hopefully through the use of
 * static methods and a final reference to the Trace object used
 * it will allow the JIT to inline the checks (and remove any corresponding
 * tracing checks if tracing is disabled). Also the use of a single trace
 * object for all checks should stop excessive paging of Trace objects
 * in and out of memory purely for the purposes of checking to see if
 * trace is enabled.
 */
public final class Tracing
{
    // Statically initialise the final instance of trace that we will
    // use to check whether any tracing is enabled.
    public static final Trace trace = createTraceObject();

    private static Trace createTraceObject()
    {
        // Create an NLS object
        NLS anyTraceNLS = (NLS) Utils.getImpl("com.ibm.ws.objectManager.utils.NLSImpl",
                                              new Class[] { String.class },
                                              new Object[] { UtilsConstants.MSG_BUNDLE });

        // Create a TraceFactory
        TraceFactory factory = (TraceFactory) Utils.getImpl("com.ibm.ws.objectManager.utils.TraceFactoryImpl",
                                                            new Class[] { NLS.class },
                                                            new Object[] { anyTraceNLS });

        // Create a Trace object
        return factory.getTrace(TraceFactory.class, "");
    }

    public static boolean isAnyTracingEnabled()
    {
        return trace.isAnyTracingEnabled();
    }
}
