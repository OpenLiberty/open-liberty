/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.session.store.cache;

import java.util.Hashtable;

import javax.transaction.UserTransaction;

import com.ibm.ws.serialization.SerializationService;
import com.ibm.ws.session.store.common.BackedSession;
import com.ibm.wsspi.session.IStoreCallback;

/**
 * TODO most methods are not implemented
 */
public class CacheSession extends BackedSession {
    // The swappable data
    private Hashtable<?, ?> mSwappableData;

    private boolean populatedAppData;

    public CacheSession(CacheHashMap sessions, String id, IStoreCallback storeCallback) {
        super(sessions, id, storeCallback);
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedSession#getSerializationService()
     */
    @Override
    protected SerializationService getSerializationService() {
        return ((CacheHashMap) getSessions()).cacheStoreService.serializationService;
    }

    /**
     * getSingleRowAppData
     * populates the swappableData with all session attributes when running single-row schema
     * This method is always called after db retrieval so we can simply call setSwappableData
     * with the entire hashtable.
     */
    private void getSingleRowAppData() {
        // TODO copied from DatabaseSession.getSingleRowAppData
        populatedAppData = true;
        @SuppressWarnings("rawtypes")
        Hashtable swappable = (Hashtable) ((CacheHashMap) getSessions()).getValue(getId(), this);
        setSwappableData(swappable);
        synchronized (_attributeNames) {
            refillAttrNames(swappable);
        }
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedSession#getSwappableData()
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Hashtable getSwappableData() {
        // TODO copied from DatabaseSession.getSwappableData
        if (mSwappableData == null) {
            if (!isNew() && !populatedAppData) {
                getSingleRowAppData(); // populate mSwappableData for single row db only, NOT multirow
            }
            //mSwappableData could have been updated
            if (mSwappableData == null) {
                mSwappableData = new Hashtable();
                if (isNew()) {
                    //if this is a new session, then we have the updated app data
                    populatedAppData = true;
                }
            }
        }
        return mSwappableData;
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedSession#getSwappableListeners(short)
     */
    @Override
    public boolean getSwappableListeners(short listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedSession#getUserTransaction()
     */
    @Override
    protected UserTransaction getUserTransaction() {
        // This is used for retrieving a UserTransaction as cached data, not to enlist JCache operations in a transaction (which isn't possible).
        return ((CacheHashMap) getSessions()).cacheStoreService.userTransaction;
    }

    /**
     * @see com.ibm.ws.session.store.common.BackedSession#setSwappableData(java.util.Hashtable)
     */
    @Override
    public void setSwappableData(@SuppressWarnings("rawtypes") Hashtable ht) {
        mSwappableData = ht;
    }
}
