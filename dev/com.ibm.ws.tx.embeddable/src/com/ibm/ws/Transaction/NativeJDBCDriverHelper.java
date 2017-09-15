/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.Transaction;

import java.lang.reflect.Method;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * This is a utility class used to resolve incompatibilities with native
 * type 2 JDBC drivers.
 */
public class NativeJDBCDriverHelper
{
    /**
     * TraceComponent for this class.
     */
    static private final TraceComponent tc = Tr.register(
                                                         NativeJDBCDriverHelper.class,
                                                         TranConstants.TRACE_GROUP,
                                                         TranConstants.NLS_FILE);

    /**
     * DB2 t2zos JDBC driver utilities class name
     */
    static private final String EXTERNAL_OPS =
                    "com.ibm.db2.jcc.t2zos.ExternalOps";

    /**
     * Boolean indicating classload of DB2 JDBC driver was attempted.
     */
    static private boolean _loadAttempted;

    /**
     * Method to call for a DB2 thread switch
     */
    static private Method _dissociateCurrentAttachmentFromTCB;

    /**
     * Lock
     */
    static private Object _lock = new Object();

    /**
     * The DB2 Type 2 JDBC driver for z/OS attaches the JDBC connection to the
     * thread where it is running until another thread requires the connection.
     * An IRB is scheduled to move the connection to the new thread. In a
     * WebSphere for z/OS environment, the original thread may be in a WLM
     * SELECT WORK block, which prevents IRBs from running. The new thread
     * may block indefinitely. This method alerts the DB2 for z/OS JDBC
     * driver that the current thread is about to go into a WLM SELECT WORK
     * block and the connection should be removed from the thread.
     * 
     * This method is meant to be a happy medium between enabling and
     * disabling DB2DISABLETAF. This performs essentially the same function,
     * except that WebSphere attempts to tell DB2 when to disassociate the
     * affinity, instead of having DB2 disassociate after each JDBC call.
     */
    public static void threadSwitch()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "threadSwitch");

//      Not in Liberty
//        /*-----------------------------------------------------------------*/
//        /* We can't tell whether the user actually uses the DB2 Type 2 JCC */
//        /* JDBC driver on z/OS.  Only look for it once.                    */
//        /*-----------------------------------------------------------------*/
//        synchronized(_lock)
//        {
//            if (_loadAttempted == false)
//            {
//                /*---------------------------------------------------------*/
//                /* Only try to load the class if we're on z/OS.            */
//                /*---------------------------------------------------------*/
//                if (TxProperties.NATIVE_CONTEXTS_USED)
//                {
//                    /*-----------------------------------------------------*/
//                    /* If we can find the JDBC driver class, dump all the  */
//                    /* methods for debugging purposes.                     */
//                    /*-----------------------------------------------------*/
//                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
//                    {
//                        try
//                        {
//                            final Class externalOps = ExtClassLoader.
//                                getInstance().loadClass(EXTERNAL_OPS);
//                            final Method[] methods = externalOps.
//                                getDeclaredMethods();
//
//                            Tr.debug(tc, "ExternalOps methods", methods);
//                        }
//                        catch (Throwable t)
//                        {
//                            Tr.debug(tc, "Debug check failed", t);
//                        }
//                    }
//
//                    /*-----------------------------------------------------*/
//                    /* Try to reflect the method that allows us to notify  */
//                    /* DB2 of the thread switch.                           */
//                    /*-----------------------------------------------------*/
//                    try
//                    {
//                        final Class externalOps = ExtClassLoader.
//                            getInstance().loadClass(EXTERNAL_OPS);
//                        _dissociateCurrentAttachmentFromTCB =
//                            externalOps.getMethod(
//                            "dissociateCurrentAttachmentFromTCB", 
//                            new Class[0]);
//                    }
//                    catch (Throwable t)
//                    {
//                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
//                        {
//                            Tr.event(tc, "Could not load DB2 " +
//                                     "driver method", t);
//                        }
//                    }
//                }
//
//                /*---------------------------------------------------------*/
//                /* Assuming we were able to load the thread switch method, */
//                /* try to run the method.  The JDBC driver will throw an   */
//                /* exception if the environment is invalid.                */
//                /*---------------------------------------------------------*/
//                if (_dissociateCurrentAttachmentFromTCB != null)
//                {
//                    try
//                    {
//                        _dissociateCurrentAttachmentFromTCB.
//                            invoke(null, (Object[])null);
//                    }
//                    catch (Throwable t)
//                    {
//                        _dissociateCurrentAttachmentFromTCB = null;
//
//                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
//                        {
//                            Tr.event(tc, "Could not drive DB2 driver " + 
//                                     "method", t);
//                        }
//                    }
//                }
//
//                /*---------------------------------------------------------*/
//                /* Indicate that we've already tried to load the class so  */
//                /* that subsequent requests don't do it.                   */
//                /*---------------------------------------------------------*/
//                _loadAttempted = true;
//            }
//        }
//
//        /*-----------------------------------------------------------------*/
//        /* If we were able to load the thread switch method, drive it now. */
//        /* We can be reasonably confident that it will work since we tried */
//        /* to drive it when the method was loaded.                         */
//        /*-----------------------------------------------------------------*/
//        if (_dissociateCurrentAttachmentFromTCB != null)
//        {
//            try
//            {
//                _dissociateCurrentAttachmentFromTCB.invoke(null, (Object[])null);
//            }
//            catch (Throwable t)
//            {
//                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
//                {
//                    Tr.event(tc, "Could not drive DB2 driver method", t);
//                }
//            }
//        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "threadSwitch");
    }
}