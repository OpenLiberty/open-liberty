/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Change activity:
 *
 * Reason     Date        Origin       Description
 * ---------- ----------- --------     --------------------------------------------
 * 184390.1.1 26-Jan-04   schofiel     Revised Reliability Qualities of Service - MS - Tests for spill
 * 188050.4   06-Apr-04   pradine      SpecJAppServer2003 optimization
 * ============================================================================
 */
package com.ibm.ws.sib.msgstore.test.persistence.dispatcher;

import com.ibm.ws.sib.msgstore.persistence.BatchingContext;
import com.ibm.ws.sib.msgstore.persistence.BatchingContextFactory;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;

public class BatchingContextFactoryImpl implements BatchingContextFactory {
    private PersistableEventDispatchListener _listener;

    public BatchingContextFactoryImpl() {
        super();
    }

    public BatchingContextFactoryImpl(PersistableEventDispatchListener listener) {
        _listener = listener;
    }

    public BatchingContext createBatchingContext() {
        return new BatchingContextImpl(_listener);
    }

    public BatchingContext createBatchingContext(int capacity) {
        return new BatchingContextImpl(_listener);
    }

    public BatchingContext createBatchingContext(int capacity, boolean useEnlistedConnections) {
        return new BatchingContextImpl(_listener);
    }

    public BatchingContext getBatchingContext(PersistentTransaction transaction, int capacity, boolean useEnlistedConnections) {
        return new BatchingContextImpl(_listener);
    }

    public BatchingContext getBatchingContext(PersistentTransaction transaction, int capacity) {
        return new BatchingContextImpl(_listener);
    }

}
