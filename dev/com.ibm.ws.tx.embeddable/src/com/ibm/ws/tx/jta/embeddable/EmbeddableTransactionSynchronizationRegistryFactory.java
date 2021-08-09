/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.tx.jta.embeddable;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import javax.transaction.TransactionSynchronizationRegistry;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.embeddable.impl.EmbeddableTransactionSynchronizationRegistryImpl;

public class EmbeddableTransactionSynchronizationRegistryFactory extends com.ibm.tx.jta.TransactionSynchronizationRegistryFactory implements ObjectFactory {
    private static final TraceComponent tc = Tr.register(EmbeddableTransactionSynchronizationRegistryFactory.class, TranConstants.TRACE_GROUP, null);

    @Override
    public synchronized Object getObjectInstance(Object referenceObject, Name name, Context context, Hashtable<?, ?> env) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getObjectInstance", new Object[] { referenceObject, name, context, env, this });

        if (_instance == null) {
            _instance = new EmbeddableTransactionSynchronizationRegistryImpl();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getObjectInstance", _instance);
        return _instance;
    }

    public static synchronized TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getTransactionSynchronizationRegistry");

        if (_instance == null) {
            _instance = new EmbeddableTransactionSynchronizationRegistryImpl();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getTransactionSynchronizationRegistry", _instance);
        return _instance;
    }
}