/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.store.db;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;

import javax.transaction.UserTransaction;

import com.ibm.ws.serialization.SerializationService;
import com.ibm.ws.session.store.common.BackedSession;
import com.ibm.wsspi.session.IStoreCallback;

public class DatabaseSession extends BackedSession {

    private static final long serialVersionUID = 3831254703056795406L;
    private static final String methodClassName = "DatabaseSession";
    private boolean populatedAppData = false;
    protected boolean usingMultirow = false;

    // The swappable data
    Hashtable mSwappableData;

    private static final int GET_SWAPPABLE_DATA = 0;
    private static final int GET_SWAPPABLE_LISTENERS = 1;
    private static final int GET_SINGLE_ROW_APP_DATA = 2;
    private static final int GET_MULTI_ROW_APP_DATA = 3;

    private static final String methodNames[] = { "getSwappableData", "getSwappableListeners", "getSingleRowAppData", "getMultiRowAppData" };

    /*
     * Constructor
     *
     * @param sessions
     *
     * @param id
     *
     * @param storeCallback
     */
    public DatabaseSession(DatabaseHashMap sessions, String id, IStoreCallback storeCallback) {
        super(sessions, id, storeCallback);
        usingMultirow = _smc.isUsingMultirow();
    }

    public DatabaseSession() {
        super();
    }

    protected DatabaseStoreService getDatabaseStoreService() {
        return ((DatabaseHashMap) getSessions()).getDatabaseStoreService();
    }

    protected SerializationService getSerializationService() {
        return this.getDatabaseStoreService().getSerializationService();
    }

    protected UserTransaction getUserTransaction() {
        return this.getDatabaseStoreService().getUserTransaction();
    }

    /*
     * To get at the swappable data
     */
    public Hashtable getSwappableData() {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[GET_SWAPPABLE_DATA]);
        }

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
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[GET_SWAPPABLE_DATA]);
        }
        return mSwappableData;
    }

    /*
     * Get the swappable listeners
     * Called to load session attributes if the session contains Activation or Binding listeners
     * Note, we always load ALL attributes here since we can't tell which are listeners until they
     * are loaded.
     */
    public boolean getSwappableListeners(short requestedListener) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[GET_SWAPPABLE_LISTENERS]);
        }
        short thisListenerFlag = getListenerFlag();
        boolean rc = false;
        // check session's listenrCnt to see if it has any of the type we want
        // input listener is either BINDING or ACTIVATION, so if the session has both, its a match
        if ((thisListenerFlag == requestedListener) || (thisListenerFlag == HTTP_SESSION_BINDING_AND_ACTIVATION_LISTENER)) {
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_SWAPPABLE_LISTENERS], "loading db data because we have listener match for "
                                                                                                                       + requestedListener);
            }
            rc = true;
            if (!populatedAppData) {
                // PM00465: Set thread context class loader to CompoundClassLoader
                try {
                    getSessions().getIStore().setThreadContext();
                    if (usingMultirow) {
                        getMultiRowAppData();
                    } else {
                        getSingleRowAppData();
                    }
                } finally {
                    // PM00465: Unset thread context class loader to prior classloader
                    getSessions().getIStore().unsetThreadContext();
                }
            }
        }
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[GET_SWAPPABLE_LISTENERS]);
        }
        return rc;
    }

    /*
     * Method getAttributeNames
     * <p>
     *
     * @see com.ibm.wsspi.session.ISession#getAttributeNames()
     * Ensures db data is read in and attribute names are populated
     */
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

    /*
     * getSingleRowAppData
     * populates the swappableData with all session attributes when running single-row schema
     * This method is always called after db retrieval so we can simply call setSwappableData
     * with the entire hashtable.
     */
    private void getSingleRowAppData() {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[GET_SINGLE_ROW_APP_DATA]);
        }
        populatedAppData = true;
        Hashtable swappable = (Hashtable) ((DatabaseHashMap) getSessions()).getValue(getId(), this);
        setSwappableData(swappable);
        synchronized (_attributeNames) {
            refillAttrNames(swappable);
        }
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[GET_SINGLE_ROW_APP_DATA]);
        }
    }

    /*
     * getMultiRowAppData
     * This method may or may not be called after db retrieval depending on whether we
     * need to call listeners or get all attribute names. Therefore, we add to the
     * existing swappable data rather than just calling setSwappable data.
     */
    private void getMultiRowAppData() {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[GET_MULTI_ROW_APP_DATA]);
        }
        populatedAppData = true;
        Hashtable swappable = getSwappableData();
        Hashtable props = (Hashtable) ((DatabaseHashMap) getSessions()).getAllValues(this);
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
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[GET_MULTI_ROW_APP_DATA]);
        }
    }

    /*
     * PM03375.1: Pass boolean true to invalidate to distinguish application invoked invalidation and timed out session
     */
    public synchronized void invalidate() {
        invalidate(true);
    }

    public boolean getPopulatedAppData() { //PM90293
        return populatedAppData;
    }

    /*
     * setSwappableData
     */
    @Override
    public void setSwappableData(Hashtable ht) {
        mSwappableData = ht;
    }
}