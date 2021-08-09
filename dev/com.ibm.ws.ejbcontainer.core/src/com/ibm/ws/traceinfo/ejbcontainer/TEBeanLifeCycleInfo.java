/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.traceinfo.ejbcontainer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Processor to write out and read in a EJB bean life cycle state and ejb callback
 * method invocation records.
 */
public class TEBeanLifeCycleInfo implements TEInfoConstants
{
    private static final TraceComponent tc = Tr.register(TEBeanLifeCycleInfo.class,
                                                         "TEExplorer",
                                                         "com.ibm.ws.traceinfo.ejbcontainer");

    /**
     * This is called by the EJB container server code to write a
     * ejb method callback entry record to the trace log, if enabled.
     */
    public static void traceEJBCallEntry(String methodDesc)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            StringBuffer sbuf = new StringBuffer();

            sbuf
                            .append(BeanLifeCycle_EJBCallEntry_Type_Str).append(DataDelimiter)
                            .append(BeanLifeCycle_EJBCallEntry_Type).append(DataDelimiter)
                            .append(methodDesc);

            Tr.debug(tc, sbuf.toString());
        }
    }

    /**
     * This is called by the EJB container server code to write a
     * ejb method callback exit record to the trace log, if enabled.
     */
    public static void traceEJBCallExit(String methodDesc)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            StringBuffer sbuf = new StringBuffer();

            sbuf
                            .append(BeanLifeCycle_EJBCallExit_Type_Str).append(DataDelimiter)
                            .append(BeanLifeCycle_EJBCallExit_Type).append(DataDelimiter)
                            .append(methodDesc);

            Tr.debug(tc, sbuf.toString());
        }
    }

    /**
     * This is called by the EJB container server code to write a
     * ejb bean state record to the trace log, if enabled.
     */
    public static void traceBeanState(int oldState, String oldString, int newState, String newString) // d167264
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            StringBuffer sbuf = new StringBuffer();

            sbuf
                            .append(BeanLifeCycle_State_Type_Str).append(DataDelimiter)
                            .append(BeanLifeCycle_State_Type).append(DataDelimiter)
                            .append(oldState).append(DataDelimiter) // d167264
                            .append(oldString).append(DataDelimiter) // d167264
                            .append(newState).append(DataDelimiter) // d167264
                            .append(newString).append(DataDelimiter) // d167264
            ;

            Tr.debug(tc, sbuf.toString());
        }
    }

    // PQ74774 Begins
    /**
     * Returns true if trace for this class is enabled. This is used to guard the
     * caller to avoid unncessary processing before the trace is depositied.
     */
    public static boolean isTraceEnabled()
    {
        return (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled());
    }
    // PQ74774 Ends
}
