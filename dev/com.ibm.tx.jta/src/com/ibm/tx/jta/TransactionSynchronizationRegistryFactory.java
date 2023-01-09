/*******************************************************************************
 * Copyright (c) 2017,2021 IBM Corporation and others.
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
package com.ibm.tx.jta;

import javax.transaction.TransactionSynchronizationRegistry;

import com.ibm.tx.TranConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

public class TransactionSynchronizationRegistryFactory {
    private static final TraceComponent tc = Tr.register(TransactionSynchronizationRegistryFactory.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    protected static TransactionSynchronizationRegistry _instance;

    public static synchronized TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getTransactionSynchronizationRegistry");

        if (_instance == null) {
            try {
                final Class clazz = Class.forName("com.ibm.tx.jta.impl.TransactionSynchronizationRegistryImpl");

                _instance = (TransactionSynchronizationRegistry) clazz.newInstance();
            } catch (Exception e) {
                FFDCFilter.processException(e, "com.ibm.tx.jta.TransactionSynchronizationRegistryFactory.getTransactionSynchronizationRegistry", "27");
                if (tc.isEntryEnabled())
                    Tr.entry(tc, "getTransactionSynchronizationRegistry", e);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getTransactionSynchronizationRegistry", _instance);
        return _instance;
    }
}