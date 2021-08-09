package com.ibm.ws.jtaextensions;

/*******************************************************************************
 * Copyright (c) 2002, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tx.TranConstants;
import com.ibm.websphere.jtaextensions.ExtendedJTATransaction;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * 
 * This class is registered in the component name space
 * and provides user applications with the instance
 * of ExtendedJTATransaction. It can also be accessed
 * directly to provide the instance to other internal
 * classes.
 * 
 * <p> This class is private to WAS.
 * Any use of this class outside the WAS Express/ND codebase
 * is not supported.
 * 
 */
public final class ExtendedJTATransactionFactory implements ObjectFactory {
    private static final TraceComponent tc = Tr.register(
                                                         ExtendedJTATransactionFactory.class
                                                         , TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private final static String extendedJTATxImplKey = "com.ibm.ws.transaction.ExtendedJTATransaction"; // d143104A

    // singleton instance of ExtendedJTATransaction
    private static ExtendedJTATransaction instance;

    // Implementation of ObjectFactory method.
    // Return the ExtendedJTATransaction object. 
    @Override
    public Object getObjectInstance(Object refObj, Name name, Context nameCtx, Hashtable env) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getObjectInstance", "" + name);
        }

        ExtendedJTATransaction extJTATran = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "class name: " + (refObj instanceof Reference ? ((Reference) refObj).getClassName() : null));
        }

        extJTATran = refObj instanceof Reference && ((Reference) refObj).getFactoryClassName() == null ? createExtendedJTATransaction() : null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getObjectInstance: " + extJTATran);
        }

        return extJTATran;
    }

    // Returns the singleton instance of ExtendedJTATransaction
    // If the instance does not exist, it is created and then
    // returned.
    public static ExtendedJTATransaction createExtendedJTATransaction() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "createExtendedJTATransaction");
        }

        // If we haven't already loaded and
        // instantiated ExtendedJTATransactionImpl
        // do so now.
        if (instance == null) {
            try {
                // LIBERTY avoid use of implfactory
                instance = new ExtendedJTATransactionImpl();
            } catch (Exception e) {
                FFDCFilter.processException(e, "com.ibm.ws.jtaextensions.ExtendedJTATransactionFactory.createExtendedJTATransaction", "95");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ExtendedJTATransactionImpl load and instantiation failed");
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "createExtendedJTATransaction", instance);
        }

        return instance;
    }

    // Get the singleton instance of ExtendedJTATransaction
    public static ExtendedJTATransaction getExtendedJTATransaction() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getExtendedJTATransaction", instance);
        }
        return instance;
    }
}
