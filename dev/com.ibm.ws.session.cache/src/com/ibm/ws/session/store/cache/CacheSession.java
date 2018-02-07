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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;

import javax.transaction.UserTransaction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.ws.session.store.common.BackedSession;
import com.ibm.wsspi.session.IStoreCallback;

/**
 * TODO most methods are not implemented
 */
public class CacheSession extends BackedSession {
    private static final TraceComponent tc = Tr.register(CacheSession.class);

    // The swappable data
    private Hashtable<?, ?> mSwappableData;

    private boolean populatedAppData;
    private boolean usingMultirow;

    public CacheSession(CacheHashMap sessions, String id, IStoreCallback storeCallback) {
        super(sessions, id, storeCallback);
        usingMultirow = _smc.isUsingMultirow();
    }

    /**
     * Ensures db data is read in and attribute names are populated
     * 
     * @see com.ibm.wsspi.session.ISession#getAttributeNames()
     */
    @Override
    @SuppressWarnings("rawtypes")
    public synchronized Enumeration getAttributeNames() {
        if (!populatedAppData) {
            if (usingMultirow) {
                getMultiRowAppData();
            } else {
                getSingleRowAppData();
            }
        }
        return super.getAttributeNames();
    }

    /**
     * Copied from DatabaseSession:
     * This method may or may not be called after retrieval depending on whether we
     * need to call listeners or get all attribute names. Therefore, we add to the
     * existing swappable data rather than just calling setSwappable data.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void getMultiRowAppData() {
        populatedAppData = true;
        Hashtable swappable = getSwappableData();
        Hashtable props = (Hashtable) ((CacheHashMapMR) getSessions()).getAllValues(this);
        if (props != null) {
            Enumeration kys = props.keys();
            while (kys.hasMoreElements()) {
                Object key = kys.nextElement();
                swappable.put(key, props.get(key));
            }
            synchronized (_attributeNames) {
                refillAttrNames(swappable);
            }
        }
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
            if (!isNew() && !usingMultirow && !populatedAppData) {
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
     * Copied from DatabaseSession.getSwappableListeners.
     * Get the swappable listeners
     * Called to load session attributes if the session contains Activation or Binding listeners
     * Note, we always load ALL attributes here since we can't tell which are listeners until they
     * are loaded.
     * 
     * @see com.ibm.ws.session.store.common.BackedSession#getSwappableListeners(short)
     */
    @Override
    public boolean getSwappableListeners(short requestedListener) {
        short thisListenerFlag = getListenerFlag();
        boolean rc = false;
        // check session's listenrCnt to see if it has any of the type we want
        // input listener is either BINDING or ACTIVATION, so if the session has both, its a match
        if (thisListenerFlag == requestedListener || thisListenerFlag == HTTP_SESSION_BINDING_AND_ACTIVATION_LISTENER) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "loading data because we have listener match for " + requestedListener);

            rc = true;
            if (!populatedAppData) {
                try {
                    getSessions().getIStore().setThreadContext();
                    if (usingMultirow) {
                        getMultiRowAppData();
                    } else {
                        getSingleRowAppData();
                    }
                } finally {
                    getSessions().getIStore().unsetThreadContext();
                }
            }
        }
        return rc;
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
