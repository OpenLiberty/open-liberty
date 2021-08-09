/*******************************************************************************
 * Copyright (c) 1998, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.websphere.csi.CSITransactionRolledbackException;
import com.ibm.websphere.csi.ExceptionType;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * The ExceptionMappingStrategy defines the methods used by EJSDeployedSupport
 * class for mapping exceptions thrown by Remote and Local EJB References.
 * It also provides the method implementations common to all strategies.
 * Currently there are concrete exception strategies, LocalExceptionStrategy,
 * and RemoteExceptionStrategy.
 **/
public abstract class ExceptionMappingStrategy {
    //d121558
    private static final TraceComponent tc = Tr.register(ExceptionMappingStrategy.class, "EJBContainer", "com.ibm.ejs.container.container");

    /**
     * Provides a polymorphic behavior for processing exceptions depending on
     * type of wrapper (local or remote) this strategy was constructed by.
     */
    public abstract Throwable setUncheckedException(EJSDeployedSupport s, Throwable ex); // d395666 

    /**
     * Provides a polymorphic behavior for mapping CSITransactionRolledbackException
     * exceptions depending on type of wrapper (local or remote) this strategy was
     * constructed by. This method is specifically designed for use during EJSContainer
     * postInvoke processing.
     */
    public abstract Exception
                    mapCSITransactionRolledBackException(EJSDeployedSupport s, CSITransactionRolledbackException ex)
                                    throws com.ibm.websphere.csi.CSIException;

    /**
     * Finds the root cause of the exception. This routine will continue to
     * look through chained exceptions until it cannot find another chained
     * exception.
     * 
     * Used primarily for chaining the root cause of
     * TransactionRolledbackException.
     * 
     * @param throwable the <code>Throwable</code> thrown by bean method <p>
     **/
    public Throwable findRootCause(Throwable throwable) // d109641.1
    {
        return ExceptionUtil.findRootCause(throwable); // 150727
    }

    public final void setCheckedException(EJSDeployedSupport s, Exception ex) { // d135756

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "setting checked exception", ex);
        }
        s.exType = ExceptionType.CHECKED_EXCEPTION;
        s.ivException = ex;
        s.rootEx = findRootCause(ex); // d109641.1

        // d395666 start        
        // Check whether rollback setting was provided via application-exception DD
        // or ApplicationException annotation. 
        Boolean applicationExceptionRollback = s.getApplicationExceptionRollback(ex);

        // Mark transaction rollback only if rollback setting was provided
        // and it is set to true.
        if (applicationExceptionRollback == Boolean.TRUE)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            {
                Tr.event(tc, "ApplicationException with rollback set to true, setting rollback only", ex);
            }
            s.currentTx.setRollbackOnly();
        } // d395666 end

    } // setCheckedException
}
