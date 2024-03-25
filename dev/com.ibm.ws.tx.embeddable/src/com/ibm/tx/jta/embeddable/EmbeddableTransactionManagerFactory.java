/*******************************************************************************
 * Copyright (c) 2009, 2023 IBM Corporation and others.
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
package com.ibm.tx.jta.embeddable;

import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.embeddable.impl.EmbeddableTranManagerSet;
import com.ibm.tx.ltc.embeddable.impl.EmbeddableLocalTranCurrentSet;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;

/**
 * This class provides access for app server components to the TransactionManager.
 * An instance can be obtained through the static method getTransactionManager().
 * In addition to the javax.transaction.TransactionManager interface, the returned
 * instance also provides methods to enlist and delist resources with transactions,
 * as defined in the WebSphereTransactionManager interface.
 *
 * <p> This class is private to WAS.
 * Any use of this class outside the WAS Express/ND codebase
 * is not supported.
 *
 */
public class EmbeddableTransactionManagerFactory extends TransactionManagerFactory {
    private static final TraceComponent tc = Tr.register(EmbeddableTransactionManagerFactory.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    protected static LocalTransactionCurrent _localTranCurrent;

    //
    // This is a factory class that should not be instantiated - police use of the constructor.
    //
    protected EmbeddableTransactionManagerFactory() {
    }

    /**
     * This method returns the underlying implementation of the TransactionManager. Use of this object
     * replaces use of the old JTSXA instance and static methods.
     */
    @Trivial
    public static EmbeddableWebSphereTransactionManager getTransactionManager() {

        if (_tranManager == null) {
            _tranManager = EmbeddableTranManagerSet.instance();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getTransactionManager: {0}", _tranManager);
        return (EmbeddableWebSphereTransactionManager) _tranManager;
    }

    public static LocalTransactionCurrent getLocalTransactionCurrent() {

        if (_localTranCurrent == null) {
            _localTranCurrent = EmbeddableLocalTranCurrentSet.instance();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getLocalTransactionCurrent: {0}", _localTranCurrent);
        return _localTranCurrent;
    }

    public static UOWCurrent getUOWCurrent() {
        final UOWCurrent uowc = (UOWCurrent) getTransactionManager();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getUOWCurrent: {0}", uowc);
        return uowc;
    }

    public static void setTransactionManager(EmbeddableTranManagerSet tm) {
        _tranManager = tm;
    }

    /**
     * Must only be called in a client
     *
     * @return
     */
    public static ExtendedTransactionManager getClientTransactionManager() {

        if (_tranManager == null) {
            _tranManager = EmbeddableTranManagerSet.instance();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getClientTransactionManager: {0}", _tranManager);
        return _tranManager;
    }
}