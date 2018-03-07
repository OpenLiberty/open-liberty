/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.store.common;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

//import javax.ejb.EJBHome;
//import javax.ejb.EJBObject;
//import javax.rmi.PortableRemoteObject;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingListener;
import javax.transaction.UserTransaction;

//import com.ibm.ejs.oa.EJSORB; //reg LIDB3294-10
import com.ibm.ws.serialization.SerializationService;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.store.common.internal.J2EEObjectWrapper;
import com.ibm.ws.session.store.common.internal.LoggingUtil;
import com.ibm.ws.session.store.memory.MemorySession;
import com.ibm.ws.session.store.memory.MemoryStore;
//import com.ibm.ws.tx.UtxFactory;
//import com.ibm.ws.util.EJBSerializer; //reg LIDB3294-10
import com.ibm.wsspi.session.IStore;
import com.ibm.wsspi.session.IStoreCallback;

/*
 * BackedSession
 * Abstract class that extends MemorySession and is
 * extended by DatabaseSession and MTMSession
 * Extenders must implement abstract methods
 *    getSwappableData
 *    getSwappableListeners
 */
public abstract class BackedSession extends MemorySession {

    static protected final int initialCacheId = 1;

    // The non-swappable data
    private Hashtable mNonswappableData = null;
    //For logging.
    private static final String methodClassName = "BackedSession";
    //For logging the CMVC file version once.
    private static boolean _loggedVersion = false;

    BackedHashMap _sessions;

    public boolean needToInsert = false;
    // booleans to keep track of writes to the session
    public boolean listenCntHit;
    public boolean userWriteHit;
    public boolean maxInactWriteHit;

    // temporary store of changes to applications specific data (properties)
    // we need hashtables here because we have to store updates on a per thread
    // basis
    public Hashtable appDataChanges;
    public Hashtable appDataRemovals;

    public StringBuffer update;

    public short listenerFlag;

    public boolean cacheLastAccessedTime;

    private long mLastWriteTime;

    int deferWriteUntilNextTick;

    boolean removingSessionFromCache;
    //    public boolean duplicateIdDetected;
    private long lastWriteLastaccessTime;

    public static final short HTTP_SESSION_NO_LISTENER = 0;
    public static final short HTTP_SESSION_BINDING_LISTENER = 1;
    public static final short HTTP_SESSION_ACTIVATION_LISTENER = 2;
    public static final short HTTP_SESSION_BINDING_AND_ACTIVATION_LISTENER = 3;

    private final static String USER_TRANSACTION_J2EE = "COM_IBM_WS_J2EE_USER_TRANSACTION";

    protected boolean appDataTablesPerThread;
    protected boolean sessionAttributeListener = false;

    //    private EJBSerializer ejbSerializer = null;
    static final short EJB_LOCAL_OBJECT = 1;
    static final short EJB_LOCAL_HOME = 2;
    private static final long serialVersionUID = -6456396236183348328L;

    private final static int FLUSH = 0;
    private final static int SET_MAX_INACTIVE_INTERVAL = 1;
    private final static int SET_CREATION_TIME = 2;
    private final static int GET_ATTRIBUTE = 3;
    private final static int SET_ATTRIBUTE = 4;
    private final static int REMOVE_ATTRIBUTE = 5;
    private final static int GET_LAST_WRITE_TIME = 6;
    private final static int SET_LAST_WRITE_TIME = 7;
    private final static int REFILL_ATTR_NAMES = 8;
    private final static int CONVERT_OBJECT = 9;
    private final static int PUT_VALUE_GUTS = 10;
    private final static int GET_VALUE_GUTS = 11;
    private final static int REMOVE_VALUE_GUTS = 12;
    private final static int GET_LISTENER_FLAG = 13;
    private final static int SYNC = 14;
    private final static int INVALIDATE = 15;
    private final static String methodNames[] = { "flush", "setMaxInactiveInterval", "setCreationTime", "getAttribute", "setAttribute",
                                                 "removeAttribute", "getLastWriteTime", "setLastWriteTime", "refillAttrNames", "convertObject",
                                                 "putValueGuts", "getValueGuts", "removeValueGuts", "getListenerFlag", "sync", "invalidate" };

    /*
     * Default constructor - must subsequently call initSession
     */
    public BackedSession() {
        commonInit();
    }

    /*
     * Constructor
     */
    public BackedSession(BackedHashMap sessions, String id, IStoreCallback storeCallback) {
        super(sessions.getIStore(), id, storeCallback);

        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            if (!_loggedVersion) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "", "CMVC Version 1.15 8/30/10 10:39:20");
                _loggedVersion = true;
            }
        }
        commonInit();
        _sessions = sessions;
        appDataTablesPerThread = _sessions.getAppDataTablesPerThread();
    }

    /*
     * commonInit - common initialization for both constructors
     */
    private void commonInit() {
        listenerFlag = HTTP_SESSION_NO_LISTENER;
        deferWriteUntilNextTick = 0;
        mLastWriteTime = -1;
        removingSessionFromCache = false;
        _refCount = 0;
        _version = initialCacheId;
    }

    /*
     * initSession - used to initialize a session when the default (no-arg)
     * constructor is used.
     */
    public void initSession(IStore is) {
        _store = is;
        _storeCallback = is.getStoreCallback();
        _sessions = ((BackedStore) is).getSessions();
        _smc = ((MemoryStore) _store).getSessionManagerConfig();
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            appNameAndIdString = getAppNameAndID();
        }
        _sessions.setStoreCallback(_storeCallback);
    }

    /*
     * readExternal
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        //needs to be implemented in any extending class requiring this method
    }

    /*
     * writeExernal
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        //needs to be implemented in any extending class requiring this method
    }

    /*
     * getId
     */
    public String getId() {
        return _sessionId;
    }

    /*
     * setId
     */
    public void setId(String s) {
        _sessionId = s;
    }

    /*
     * Method flush
     *
     * @see com.ibm.wsspi.session.ISession#flush()
     */
    public synchronized void flush() {
        flush(false);
    }

    /*
     * Method flush(boolean)
     *
     * @see com.ibm.wsspi.session.ISession#flush(boolean)
     */
    public synchronized void flush(boolean b) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[FLUSH], "(" + b + ") session = " + this);
        }
        cacheLastAccessedTime = b;
        //we're going to call _sessions.put instead of storeSession to handle the case when it hasn't been inserted yet into the backend
        //if it hasn't been inserted we need to call insertSession, otherwise we just call storeSession.
        //_sessions.storeSession(this);
        _sessions.put(this.getId(), this);
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[FLUSH]);
        }
    }

    /*
     * Method setUserName(String)
     *
     * @see com.ibm.wsspi.session.ISession#setUserName(java.lang.String)
     */
    public synchronized void setUserName(String userName) {
        if (userName != null && !userName.equals(_userName)) {
            _userName = userName;
            userWriteHit = true;
        }
    }

    /*
     * Sets the _userName. Called internally when session is
     * read from persistent store. We don't want to flag
     * userWriteHit in this case.
     */
    public void internalSetUser(String name) {
        _userName = name;
    }

    /*
     * Method setMaxInactiveInterval
     *
     * @see com.ibm.wsspi.session.ISession#setMaxInactiveInterval(int)
     */
    public synchronized void setMaxInactiveInterval(int interval) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[SET_MAX_INACTIVE_INTERVAL], "interval= " + interval);
        }
        if (_maxInactiveInterval != interval) {
            _maxInactiveInterval = interval;
            maxInactWriteHit = true;
        }
    }

    /*
     * internalSetMaxInactive - called when reading in from back end
     */
    public void internalSetMaxInactive(int interval) {
        _maxInactiveInterval = interval;
    }

    /*
     * Method setCreationTime
     * <p>
     *
     * @see com.ibm.wsspi.session.ISession#setMaxInactiveInterval(int)
     */
    public void setCreationTime(long ct) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[SET_CREATION_TIME], "creationTime= " + ct);
        }
        _creationTime = ct;
    }

    /*
     * Method getAttribute
     *
     * @see com.ibm.wsspi.session.ISession#getAttribute(java.lang.Object)
     */
    public Object getAttribute(Object name) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[GET_ATTRIBUTE], "name= " + name);
        }

        Object value = getValueGuts(name.toString());

        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            if (!SessionManagerConfig.isHideSessionValues()) {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[GET_ATTRIBUTE], "value= " + value);
            } else {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[GET_ATTRIBUTE]);
            }
        }
        return value;
    }

    /*
     * Method setAttribute
     *
     * @see com.ibm.wsspi.session.ISession#setAttribute(java.lang.Object, java.lang.Object)
     */
    public Object setAttribute(Object name, Object value, Boolean newIsListener) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            if (!SessionManagerConfig.isHideSessionValues()) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[SET_ATTRIBUTE], "name= " + name + " value=" + value);
            } else {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[SET_ATTRIBUTE], "name= " + name);
            }
        }
        _attributes.put(name, value);
        Object oldValue = getValueGuts((String) name);
        putValueGuts((String) name, value);
        Boolean oldIsListener = (Boolean) _attributeNames.put(name, newIsListener);
        _storeCallback.sessionAttributeSet(this, name, oldValue, oldIsListener, value, newIsListener);

        return oldValue;
    }

    /*
     * Method removeAttribute
     *
     * @see com.ibm.wsspi.session.ISession#removeAttribute(java.lang.Object)
     */
    public Object removeAttribute(Object name) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[REMOVE_ATTRIBUTE], "name= " + name);
        }
        _attributes.remove(name);
        Object oldValue = removeValueGuts((String) name);
        Boolean oldIsBindingListener = (Boolean) _attributeNames.get(name);
        _attributeNames.remove(name);
        _storeCallback.sessionAttributeRemoved(this, name, oldValue, oldIsBindingListener);
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            if (!SessionManagerConfig.isHideSessionValues()) {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[REMOVE_ATTRIBUTE], "oldAttributeValue =  " + oldValue);
            } else {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[REMOVE_ATTRIBUTE]);
            }
        }
        return oldValue;
    }

    public long getLastWriteLastaccessTime() {
        return lastWriteLastaccessTime;
    }

    public void setLastWriteLastAccessTime(long val) {
        this.lastWriteLastaccessTime = val;
    }

    /*
     * last write time for this session
     */
    public long getLastWriteTime() {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_LAST_WRITE_TIME], "");
        }
        return mLastWriteTime;
    }

    /*
     * Set the last write time for this session
     */
    public void setLastWriteTime(long pLastWriteTime) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[SET_LAST_WRITE_TIME], "" + pLastWriteTime);
        }
        mLastWriteTime = pLastWriteTime;
    }

    /*
     * refillAttrNames
     * sets attribute names based on what is contained in swappable data
     */
    protected void refillAttrNames(Map<Object, Object> swappable) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[REFILL_ATTR_NAMES]);
        }
        if ((swappable != null) && (swappable.size() > 0)) {
            Hashtable tempAttributeNames = new Hashtable();
            Iterator attributeIterator = swappable.keySet().iterator();
            while (attributeIterator.hasNext()) {
                Object key = attributeIterator.next();
                Object val = swappable.get(key);
                if (val instanceof HttpSessionBindingListener) { //EventListener) {
                    tempAttributeNames.put(key, Boolean.TRUE);
                } else {
                    tempAttributeNames.put(key, Boolean.FALSE);
                }
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[REFILL_ATTR_NAMES],
                                                        "added " + key + " with listener value " + tempAttributeNames.get(key));
                }
            }
            _attributeNames = tempAttributeNames;
        }
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[REFILL_ATTR_NAMES]);
        }
    }

    /*
     * setSwappableData
     */
    public abstract void setSwappableData(Map<Object, Object> ht);

    protected abstract SerializationService getSerializationService();

    private Object replaceObjectForSerialization(Object object) {
        if (object instanceof UserTransaction) {
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, "replaceObjectForSerialization", "replacing UserTransaction");
            }

            // Use J2EEObjectWrapper for compatibility with 8.5.0.
            return new J2EEObjectWrapper(USER_TRANSACTION_J2EE);
        }

        return getSerializationService().replaceObjectForSerialization(object);
    }

    protected abstract UserTransaction getUserTransaction();

    private Object resolveObject(Object object) {
        // Use J2EEObjectWrapper for compatibility with 8.5.0.
        if (object instanceof J2EEObjectWrapper) {
            Object serObj = ((J2EEObjectWrapper)object).getSerializableObject();
            if (USER_TRANSACTION_J2EE.equals(serObj)) {
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, "replaceObjectForSerialization", "converting UserTransaction");
                }

                return this.getUserTransaction();
            }
        }

        return getSerializationService().resolveObject(object);
    }

    /*
     * putValueGuts
     */
    synchronized void putValueGuts(String pName, Object pValue) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[PUT_VALUE_GUTS], "pName = " + pName);
        }

        if (pName == null || pValue == null) {
            return;
        }

        // If in Servlet 2.2 mode and Distributable Web App and data in not serializable
        // then throw an exception
        boolean distributableWebApp = getIStore().isDistributable();
        boolean swappable = true;
        Object convObj = replaceObjectForSerialization(pValue);
        if (convObj == null) {
            swappable = false;
            convObj = pValue;
        }

        if (!swappable) { //105962 (tWAS 745384) update the trace message logic to be more specific
            if (distributableWebApp) {
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PUT_VALUE_GUTS], "IllegalArgumentException");
                }

                throw new IllegalArgumentException("Attribute with name " + pName + " is not java.io.Serializable. All attributes stored in session must be Serializable when web module is marked as distributable");
            } else if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PUT_VALUE_GUTS], "Attribute with name " + pName + " is not Serializable. This may cause a problem on a failover.");
            }
        }

        if (appDataChanges == null) {
            if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PUT_VALUE_GUTS], "init appDataChanges");
            }
            appDataChanges = new Hashtable();
        }

        if (swappable) {
            // Still want to update lists since whole session may not get written at first EOS
            if (appDataTablesPerThread) {
                Thread t = Thread.currentThread();
                Hashtable sht = (Hashtable) appDataChanges.get(t);
                if (sht == null) {
                    sht = new Hashtable();
                    appDataChanges.put(t, sht);
                }
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    if (!SessionManagerConfig.isHideSessionValues()) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PUT_VALUE_GUTS], "storing for " + getId() + " prop " + pName + " with value "
                                                                                                                      + pValue + " via thread " + t);
                    } else {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PUT_VALUE_GUTS], "storing for " + getId() + " prop " + pName + " via thread "
                                                                                                                      + t);
                    }
                }
                sht.put(pName, convObj);
            } else { // appDataTablesPerSession
                appDataChanges.put(pName, convObj);
            }

            boolean listenerFlagSetInMethod = false; //a check for instanceof was moved into the individual instancof blocks ... still only want to get getListenerFlag() once.

            if (pValue instanceof HttpSessionBindingListener) {
                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PUT_VALUE_GUTS], "Property is a binding listener: " + pName);
                }
                if (!isNew()) { // listenerFlag should be up-to-date for new session
                    //Read from the database only once
                    listenerFlag = getListenerFlag();
                    listenerFlagSetInMethod = true;
                }

                switch (listenerFlag) {
                    case HTTP_SESSION_NO_LISTENER:
                        listenerFlag = HTTP_SESSION_BINDING_LISTENER;
                        listenCntHit = true;
                        break;
                    case HTTP_SESSION_ACTIVATION_LISTENER:
                        listenerFlag = HTTP_SESSION_BINDING_AND_ACTIVATION_LISTENER;
                        listenCntHit = true;
                        break;
                    default:
                        break;
                }
            }

            if (pValue instanceof HttpSessionActivationListener) {

                if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[PUT_VALUE_GUTS], "Property is an Activation listener: " + pName);
                }
                if (!isNew() && !listenerFlagSetInMethod) { // listenerFlag should be up-to-date for new session
                    //Read from the database only once
                    listenerFlag = getListenerFlag();
                }

                switch (listenerFlag) {
                    case HTTP_SESSION_NO_LISTENER:

                        listenerFlag = HTTP_SESSION_ACTIVATION_LISTENER;
                        listenCntHit = true;
                        break;
                    case HTTP_SESSION_BINDING_LISTENER:
                        listenerFlag = HTTP_SESSION_BINDING_AND_ACTIVATION_LISTENER;
                        listenCntHit = true;
                        break;
                    default:
                        break;
                }
            }

            //if value is removed and then put back, remove it from removalList
            // long term, we may want to check all the threads
            // We need to remove it from the removals even if new since removals are updated when new

            if (appDataRemovals != null) {

                if (appDataTablesPerThread) {
                    Thread t = Thread.currentThread();
                    Hashtable sht2 = (Hashtable) appDataRemovals.get(t);
                    if (sht2 != null) {
                        sht2.remove(pName);

                        if (sht2.isEmpty()) {
                            appDataRemovals.remove(t);
                        }
                    }
                } else { // appDataTablesPerSession
                    appDataRemovals.remove(pName);
                }
            }
        } //end if (swappable) block

        if (swappable) {
            if (mNonswappableData != null) { // don't call getNonswappable if null
                                             // don't want to allocate unless needed
                getNonswappableData().remove(pName); // make sure it doesn't exist in nonswappable
            }
            getSwappableData().put(pName, convObj);
        } else {
            getSwappableData().remove(pName);// ensure it doesn't exist in swappable
                                             // this probably won't work for multi-row
            getNonswappableData().put(pName, convObj);
        }
    }

    /*
     * GetValueGuts
     */
    synchronized Object getValueGuts(String id) {
        //create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[GET_VALUE_GUTS]);
        }

        Object returnValue = (mNonswappableData == null) ? null : getNonswappableData().get(id);
        if (returnValue == null) {
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_VALUE_GUTS], "Value from non-swappable is null for id " + id);
            }
            returnValue = getSwappableData().get(id);
        }

        if (returnValue != null) {
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_VALUE_GUTS], "Retrieved cached object for " + id);
            }
            returnValue = resolveObject(returnValue);
        } else {
            //if this is a new session, don't access the database because
            //the session in memory is up-to-date..
            if (!isNew()) {

                Thread t = Thread.currentThread();
                //And don't go to database, if object is already removed from session
                if (appDataRemovals != null) {
                    if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_VALUE_GUTS], "appDataRemovals is not null");
                    }
                    if (appDataTablesPerThread) {
                        if (appDataRemovals.get(t) != null && ((Hashtable) appDataRemovals.get(t)).get(id) != null) {
                            return returnValue; // null
                        }

                    } else { //appDataTablesPerSession
                        if (appDataRemovals.get(id) != null) {
                            return returnValue; // null
                        }
                    }
                }
                if (_sessions != null) {
                    // we may get a value if running mulitrow db
                    returnValue = _sessions.loadOneValue(id, this);
                }
                if (returnValue != null) {
                    returnValue = resolveObject(returnValue);
                    if (returnValue instanceof HttpSessionBindingListener) {
                        _attributeNames.put(id, Boolean.TRUE);
                    } else {
                        _attributeNames.put(id, Boolean.FALSE);
                    }
                    getSwappableData().put(id, returnValue);
                }
            }
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                String s = (returnValue == null) ? "null" : "not null";
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_VALUE_GUTS], "Value from db is " + s + " for id " + id);
            }
        }
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[GET_VALUE_GUTS]);
        }
        return returnValue;
    }

    /*
     * removeValueGuts - To remove the values
     */
    synchronized Object removeValueGuts(String pName) {
        //create local variable - JIT performance improvement
        final boolean isTraceOn = com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[REMOVE_VALUE_GUTS], "session " + getId() + " prop id " + pName);
        }

        Object tmp = getValueGuts(pName);
        // if object does not acutally exist, just return
        if (tmp == null) {
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
                LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[REMOVE_VALUE_GUTS], "prop id " + pName + " not in " + getId());
            }
            return null;
        }

        if (appDataRemovals == null) {
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[REMOVE_VALUE_GUTS], "init appDataRemovals " + getId());
            }
            appDataRemovals = new Hashtable();
        }

        if (appDataTablesPerThread) {
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[REMOVE_VALUE_GUTS], "appDataTablesPerThread");
            }
            Thread t = Thread.currentThread();
            Hashtable sht = (Hashtable) appDataRemovals.get(t);
            if (sht == null) {
                sht = new Hashtable();
                appDataRemovals.put(t, sht);
            }
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[REMOVE_VALUE_GUTS], "remove value for " + getId() + " and prop " + pName
                                                                                                                 + " via thread " + t);
            }
            sht.put(pName, pName);
        } else { //appDataTablesPerSession
            if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[REMOVE_VALUE_GUTS], "appDataTablesPerSession");
            }
            appDataRemovals.put(pName, pName);
        }

        Object ret = (mNonswappableData == null) ? null : mNonswappableData.remove(pName);
        if (ret == null)
            ret = getSwappableData().remove(pName);

        if (isTraceOn && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[REMOVE_VALUE_GUTS]);
        }
        return ret;
    }

    /*
     * To get at NonSwappable data
     * Don't call this unless you are going to put a value.
     * We don't want to create a hashtable that won't be used.
     */
    Hashtable getNonswappableData() {
        if (mNonswappableData == null)
            mNonswappableData = new Hashtable(5); // cmd PQ81539 - limit to 5
        return mNonswappableData;
    }

    public void setOverflow() {
        //overflow concept doesn't apply to backed Sessions ... do nothing.
    }

    public boolean isOverflow() {
        return false;
    }

    /*
     * Get the listener mark
     */
    public synchronized short getListenerFlag() {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodNames[GET_LISTENER_FLAG], "" + listenerFlag);
        }
        return listenerFlag;
    }

    /*
     * To set the listener count
     * This is called when read from database
     */
    public void setListenerFlag(short flag) {
        this.listenerFlag = flag;
    }

    /*
     * setInsert
     * called only when a new session is created and needs to be newly inserted into the store
     */
    public void setInsert() {
        needToInsert = true;
    }

    /*
     * getSessions
     */
    public BackedHashMap getSessions() {
        return _sessions;
    }

    /*
     * setSessions
     */
    public void setSessions(BackedHashMap _sessions) {
        this._sessions = _sessions;
    }

    /*
     * sync
     */
    public synchronized void sync() {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[SYNC]);
        }
        flush();
    }

    /*
     * toString
     * String representation of the object
     */
    public synchronized String toString() {
        StringBuffer strbuf = new StringBuffer();

        strbuf
                        .append(super.toString())
                        .append("\n")
                        .append("app data changes : ");
        if (!SessionManagerConfig.isHideSessionValues()) {
            strbuf
                            .append(appDataChanges != null ? appDataChanges.toString() : null)
                            .append("\n")
                            .append("app data removals : ")
                            .append(appDataRemovals != null ? appDataRemovals.toString() : null)
                            .append("\n");
        } else {
            strbuf
                            .append(appDataChanges != null ? appDataChanges.keySet().toString() : null)
                            .append("\n")
                            .append("app data removals : ")
                            .append(appDataRemovals != null ? appDataRemovals.keySet().toString() : null)
                            .append("\n");
        }
        strbuf
                        .append("user write hit : ")
                        .append(userWriteHit)
                        .append("\n")
                        .append("max inact write hit : ")
                        .append(maxInactWriteHit)
                        .append("\n")
                        .append("listener count hit : ")
                        .append(listenCntHit)
                        .append("\n")
                        .append("update : ")
                        .append(update != null ? update.toString() : null)
                        .append("\n")
                        .append("listener flag : ")
                        .append(listenerFlag)
                        .append("\n")
                        .append("version : ")
                        .append(_version)
                        .append("\n")
                        .append("\n");

        return (strbuf.toString());
    }

    /*
     * @see com.ibm.wsspi.session.ISession#invalidate()
     * Ensure we load data if there are binding listeners.
     */
    /*
     * PM03375.1: Introduce boolean parameter to distinguish between application invoked invalidation and timeout.
     * If so then we need to call sessionCacheDiscard
     */
    public synchronized void invalidate(boolean appInvoked) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[INVALIDATE], appInvoked);
        }
        getSwappableListeners(BackedSession.HTTP_SESSION_BINDING_LISTENER);
        if (appInvoked && (!invalInProgress) && isValid())
            _storeCallback.sessionCacheDiscard(this);
        super.invalidate();
        //The session will be invalid, but do we need to worry about these in case we are already processing this session somewhere else?
        if (this.appDataChanges != null)
            this.appDataChanges.clear();
        if (this.appDataRemovals != null)
            this.appDataRemovals.clear();
        this.update = null;
        this.userWriteHit = false;
        this.maxInactWriteHit = false;
        this.listenCntHit = false;
        setSwappableData(null);
        this.mNonswappableData = null;
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[INVALIDATE]);
        }
    }

    // PM03375.1: Moved from MTMSession to BackedSession to make this
    // PM11279: Introduce new method to help distinguish between application invoked invalidation and timeout
    protected void callInvalidateFromInternalInvalidate() {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "callInvalidateFromInternalInvalidate", "calling this.invalidate( false )");
        }
        invalidate(false);
    }

    public abstract Map<Object, Object> getSwappableData();

    public abstract boolean getSwappableListeners(short listener);
}
