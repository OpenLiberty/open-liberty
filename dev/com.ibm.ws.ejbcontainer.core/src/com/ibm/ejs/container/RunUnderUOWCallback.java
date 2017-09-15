/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCallback;
import com.ibm.ws.Transaction.UOWCoordinator;

/**
 * Provides a callback method for the Transaction Service to notify the
 * EJB Container of a context change between Units of Work associated
 * with a call to com.ibm.wsspi.uow.UOWManager.runUnderUOW(). <p>
 * 
 * This callback mechanism allows the EJB Container to compensate for user
 * initiated Units of Work (such as when the Spring framework begins a
 * transaction). Normally, this is handled during preInvoke processing in
 * the TranStrategy. However, for Lightweight Local EJBs, transaction
 * processing may be skipped. <p>
 * 
 * The callback method on this class will compensate by triggering the normal
 * transaction preInvoke processing for EJBs configured as Lightweight Local.
 * This callback approach allows the normal performance benefits of Lightweight
 * Local mode, only forcing the transaction processing when notified of the
 * use of 'runUnderUOW'. <p>
 * 
 * Note: This class is not intended to be used as a general purpose
 * Unit of Work callback implementation, but specifically for those
 * context changes associated with the use of 'runUnderUOW'.
 */
public class RunUnderUOWCallback implements UOWCallback
{
    private static final TraceComponent tc = Tr.register(RunUnderUOWCallback.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    /**
     * This method will be called for POST_BEGIN and POST_END for all
     * UOWs started with com.ibm.wsspi.uow.UOWManager.runUnderUOW(). <p>
     * 
     * When called, an indication will be stored in the current EJB method
     * context, recording the fact that a user initiated transaction has
     * been started. This indication will be reset when the user initiated
     * transaction completes and this method is called a second time. <p>
     * 
     * The same EJB method context must be in effect for both the POST_BEGIN
     * and POST_END calls for a given UOW, since the runUnderUOW method will
     * start the UOW when called, and complete the UOW on exit. <p>
     * 
     * An indication that a user initiated transaction started must be
     * recoreded regardless of whether or not the current EJB method context
     * is configured for Lightweight Local mode. A user initiated transaction
     * does not actually effect the current method context, but rather
     * any future EJB method calls that occur within the scope of the
     * user initiated transaction. <p>
     * 
     * @param typeOfChange one of the following values:
     *            <PRE>
     *            POST_BEGIN
     *            POST_END
     *            </PRE>
     * 
     * @param UOW the Unit of Work that will be affected by the begin/end i.e.
     *            <PRE>
     *            POST_BEGIN - The UOW that was just begun
     *            POST_END - NULL
     *            </PRE>
     * 
     * @exception IllegalStateException
     */
    public void contextChange(int typeOfChange, UOWCoordinator UOW)
                    throws IllegalStateException
    {
        EJSDeployedSupport s = EJSContainer.getMethodContext();

        // Note: runUnderUOW() may be called outside the scope of an EJB
        //       method ( i.e. no method context ), in which case this
        //       method call should just be ignored.

        if (s != null)
        {
            switch (typeOfChange)
            {
                case POST_BEGIN:
                    s.ivRunUnderUOW++; // d601655
                    break;

                case POST_END:
                    s.ivRunUnderUOW--; // d601655
                    break;

                default:
                    throw new IllegalStateException("Illegal type of change for runUnderUOW callback : " +
                                                    getTypeString(typeOfChange));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "contextChange : " + getTypeString(typeOfChange) +
                         " : " + (s == null ? null : "depth=" + s.ivRunUnderUOW + ", " + s)); // d601655
    }

    /**
     * Internal method that returns the String representation of the
     * typeOfChange for trace purposes.
     **/
    private String getTypeString(int typeOfChange)
    {
        switch (typeOfChange)
        {
            case PRE_BEGIN:
                return "PRE_BEGIN";

            case POST_BEGIN:
                return "POST_BEGIN";

            case PRE_END:
                return "PRE_END";

            case POST_END:
                return "POST_END";
        }

        return "UNKNOWN";
    }

}
