/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.session.store.memory;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.session.IStore;
import com.ibm.wsspi.session.IStoreCallback;

/*
 * ISession implementation for in-memory sessions.  
 * This class is extended by persistent implementations.
 */
public class MemorySession implements ISession {

    private static final long serialVersionUID = 5445113386085090398L;
    protected IStore _store;
    protected IStoreCallback _storeCallback;
    protected String _sessionId;
    protected boolean _isValid = true;
    protected boolean _isNew = true;
    protected long _currentAccessTime;
    protected long _lastAccessedTime;
    protected long _creationTime;
    protected int _version;
    protected String _userName;
    protected int _maxInactiveInterval;
    protected int _refCount;

    protected String _appName; // passed around with persistent sessions and spit out during toString()

    protected Hashtable _attributes = new Hashtable(5);
    protected Hashtable _attributeNames = new Hashtable(5);
    private Object _adaptation = null;
    private Object _appAdaptation = null;
    private boolean isOverflow = false;
    protected boolean invalInProgress = false;
    protected SessionGCount _gcCount = null;
    protected SessionManagerConfig _smc;

    //The following strings are used for Logging
    private static final String methodClassName = "MemorySession";
    private static final String newValueString = "New Value=";
    private static final String oldValueString = "; Old Value=";
    protected String appNameAndIdString = "";

    private static final int INVALIDATE = 0;
    private static final int GET_ATTRIBUTE = 1;
    private static final int SET_ATTRIBUTE = 2;
    private static final int REMOVE_ATTRIBUTE = 3;
    private static final int GET_LISTENER_ATTRIBUTE_NAMES = 4;
    private static final int UPDATE_LAST_ACCESS_TIME = 5;
    private static final int INTERNAL_INVALIDATE = 6;
    private static final int GET_SWAPPABLE_DATA = 7;

    private boolean _removeAttrOnInvalidate = false;
    private static final String methodNames[] = { "invalidate", "getAttribute", "setAttribute", "removeAttribute", "getListenerAttributeNames",
                                                 "updateLastAccessTime", "internalInvalidate", "getSwappableData" };

    /*
     * The default constructor required for doing a readExternal & Clone
     */
    public MemorySession() {
        //only used for persistent extensions       
    }

    /*
     * The main constructor
     */
    public MemorySession(IStore store, String Id, IStoreCallback storeCallback) {        
        this(store,Id,storeCallback,false);
    }
    
    /*
     * The main constructor
     */
    public MemorySession(IStore store, String Id, IStoreCallback storeCallback, boolean removeAttrOnInvalidate) {        
        _store = store;
        _appName = store.getId();
        _sessionId = Id;
        _creationTime = System.currentTimeMillis();
        _lastAccessedTime = 0;
        _currentAccessTime = _creationTime;
        _storeCallback = storeCallback;
        _smc = ((MemoryStore) _store).getSessionManagerConfig();
        if (_smc.isTrackGCCount()) {
            _gcCount = new SessionGCount();
        }
        //if (_smc.isUsingMemory()) {
        //    _attributes = new Hashtable(5);
        //}
        _removeAttrOnInvalidate = removeAttrOnInvalidate;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            appNameAndIdString = getAppNameAndID() + " , _removeAttrOnInvalidate -->" + _removeAttrOnInvalidate;
        }
    }

    /*
     * To keep track of GarbageCollection
     * InnerClass which increments a PMI counter when it is garbage collected ...
     * one object is created per memory session
     * 
     * This comment was added a long time ago, we just kept the function in place - Aug 22, 2006
     * For some reason, when finalize method is overridden in session data
     * GC collection is getting delayed. So we are using this helper object
     * to count the gc count. To turn this gc, specify SessionManager property
     * trackSessionGC=true
     */
    class SessionGCount extends Object {
        @Override
        protected void finalize() throws Throwable {
            try {
                if (_store != null && _store.getSessionStatistics() != null)
                    _store.getSessionStatistics().incSessionGarbageCollected(System.currentTimeMillis());
            } catch (Throwable t) {
                com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.session.store.memory.MemorySession.finalize", "96", this);
            }
            super.finalize();
        }
    }

    /*
     * @see com.ibm.wsspi.session.ISession#getId()
     */
    @Override
    public String getId() {
        return _sessionId;
    }

    /*
     * @see com.ibm.wsspi.session.ISession#isValid()
     */
    @Override
    public boolean isValid() {
        if (!_isValid && com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            //only log this if it is invalid
            String s = _isValid + appNameAndIdString;
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "isValid", s);
        }
        return _isValid;
    }

    /*
     * @see com.ibm.wsspi.session.ISession#isNew()
     */
    @Override
    public boolean isNew() {
        return _isNew;
    }

    /*
     * @see com.ibm.wsspi.session.ISession#setIsNew(boolean)
     */
    @Override
    public void setIsNew(boolean flag) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer(newValueString).append(flag).append(oldValueString).append(_isNew).append(appNameAndIdString);
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "setIsNew", sb.toString());
        }
        _isNew = flag;
    }

    /*
     * @see com.ibm.wsspi.session.ISession#setIsValid(boolean)
     */
    @Override
    public synchronized void setIsValid(boolean flag) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer(newValueString).append(flag).append(oldValueString).append(_isValid).append(appNameAndIdString);
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "setIsValid", sb.toString());
        }
        _isValid = flag;

    }

    /*
     * @see com.ibm.wsspi.session.ISession#invalidate()
     */
    @Override
    public synchronized void invalidate() {

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[INVALIDATE], appNameAndIdString);
        }
        if (invalInProgress) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[INVALIDATE], "Invalidation in Progress");
            }
            return;
        }
        if (!_isValid) {
            throw new IllegalStateException();
        }
        invalInProgress = true;
        
        // PM03375: Remove duplicate call to sessionCacheDiscard for persistence case
        if (_smc.isUsingMemory()) {
            _storeCallback.sessionCacheDiscard(this);
        }
        _storeCallback.sessionInvalidated(this);
        _store.removeSession(_sessionId);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[INVALIDATE], "_removeAttrOnInvalidate = " + _removeAttrOnInvalidate );
            if(_attributes != null) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[INVALIDATE], "session _attributes.size() = " + _attributes.size() );
            }
        }        
        
        //CTS defect expects removal of session attributes
        if(_attributes != null && _removeAttrOnInvalidate) {
            
           Enumeration<String> attrKey =  _attributes.keys();
           while(attrKey.hasMoreElements()){
               
               String key = attrKey.nextElement();
               this.removeAttribute(key);
           }                    
           if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
               LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[INVALIDATE], "session attributes removed");
           }
        }

        setIsValid(false);

        invalInProgress = false;
        _attributes = null;
        _attributeNames.clear();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[INVALIDATE]);
        }
    }

    /*
     * @see com.ibm.wsspi.session.ISession#flush()
     */
    @Override
    public void flush() {
        // in-memory - do nothing
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "flush", "");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#flush(boolean)
     */
    @Override
    public void flush(boolean metadataOnly) {
        // in-memory - do nothing
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "flush", "" + metadataOnly);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#setVersion(int)
     */
    @Override
    public void setVersion(int version) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer(newValueString).append(version).append(oldValueString).append(_version).append(appNameAndIdString);
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "setVersion", sb.toString());
        }
        _version = version;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#getVersion()
     */
    @Override
    public int getVersion() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            String s = _version + appNameAndIdString;
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "getVersion()", s);
        }
        return _version;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#getAdaptation()
     */
    @Override
    public final Object getAdaptation() {
        // synchronized(_adaptation) {
        return _adaptation;
        // }
    }

    @Override
    public final Object getAdaptation(int i) {
        if (i == ISession.ADAPT_TO_APPLICATION_SESSION) {
            return _appAdaptation;
        } else {
            return _adaptation;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#setAdaptation(java.lang.Object)
     */
    @Override
    public void setAdaptation(Object adaptation) {
        // synchronized(_adaptation) {
        _adaptation = adaptation;
        // }
    }

    @Override
    public void setAdaptation(Object adaptation, int i) {
        if (i == ISession.ADAPT_TO_APPLICATION_SESSION) {
            _appAdaptation = adaptation;
        } else {
            _adaptation = adaptation;
        }
    }

    /*
     * @see com.ibm.wsspi.session.ISession#getContextId()
     * not implemented, not used
     */
    @Override
    public String getContextId() {
        return null;
    }

    /*
     * @see com.ibm.wsspi.session.ISession#setContextId(java.lang.String)
     * not used, not implemented
     */
    @Override
    public void setContextId(String contextID) {

    }

    /*
     * @see com.ibm.wsspi.session.ISession#getUserName()
     */
    @Override
    public String getUserName() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            String s = _userName + appNameAndIdString;
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "getUserName", s);
        }
        return _userName;
    }

    /*
     * @see com.ibm.wsspi.session.ISession#setUserName(java.lang.String)
     */
    @Override
    public void setUserName(String userName) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer(newValueString).append(userName).append(oldValueString).append(_userName).append(appNameAndIdString);
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "setUserName", sb.toString());
        }
        _userName = userName;
    }

    /*
     * @see com.ibm.wsspi.session.ISession#getCreationTime()
     */
    @Override
    public long getCreationTime() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            String s = _creationTime + appNameAndIdString;
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "getCreationTime", s);
        }
        return _creationTime;
    }

    /*
     * @see com.ibm.wsspi.session.ISession#getLastAccessedTime()
     */
    @Override
    public synchronized long getLastAccessedTime() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            String s = _lastAccessedTime + appNameAndIdString;
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "getLastAccessedTime", s);
        }
        return _lastAccessedTime;
    }

    /*
     * @see com.ibm.wsspi.session.ISession#getMaxInactiveInterval()
     */
    @Override
    public int getMaxInactiveInterval() {
        return _maxInactiveInterval;
    }

    /*
     * @see com.ibm.wsspi.session.ISession#setMaxInactiveInterval(int)
     */
    @Override
    public void setMaxInactiveInterval(int maxInactiveInterval) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer(newValueString).append(maxInactiveInterval).append(oldValueString).append(_maxInactiveInterval).append(appNameAndIdString);
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "setMaxInactiveInterval", sb.toString());
        }
        if (maxInactiveInterval <= 0) {
            _maxInactiveInterval = -1;
        } else {
            _maxInactiveInterval = maxInactiveInterval;
        }
    }

    public void setMaxInactiveIntervalForInvalidateAll(int maxInactiveInterval) {
        if (maxInactiveInterval != 0) {
            setMaxInactiveInterval(maxInactiveInterval);
        } else {
            //truly setting it to 0 for InvalidateAll
            _maxInactiveInterval = 0;
        }
    }

    /*
     * @see com.ibm.wsspi.session.ISession#getExpiryTime()
     */
    @Override
    public long getExpiryTime() {
        return 0; // not used
    }

    /*
     * @see com.ibm.wsspi.session.ISession#setExpiryTime()
     */
    @Override
    public void setExpiryTime(long l) {
        // not used
    }

    /*
     * @see com.ibm.wsspi.session.ISession#getAttribute(java.lang.Object)
     */
    @Override
    public synchronized Object getAttribute(Object name) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            String s = name + appNameAndIdString;
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[GET_ATTRIBUTE], s);
        }
        Object o = _attributes.get(name);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            if (!SessionManagerConfig.isHideSessionValues()) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[GET_ATTRIBUTE], o);
            } else {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[GET_ATTRIBUTE]);
            }
        }
        return o;
    }

    /*
     * @see com.ibm.wsspi.session.ISession#setAttribute(java.lang.Object, java.lang.Object, java.lang.Boolean)
     */
    @Override
    public synchronized Object setAttribute(Object name, Object value, Boolean newIsListener) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            StringBuffer sb = new StringBuffer("{").append(name).append(",");
            if (!SessionManagerConfig.isHideSessionValues()) {
                sb.append(value).append(",");
            }
            sb.append(newIsListener).append("} ").append(appNameAndIdString);
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[SET_ATTRIBUTE], sb.toString());
        }
        Object oldValue = _attributes.put(name, value);
        Boolean oldIsListener = (Boolean) _attributeNames.put(name, newIsListener);
        _storeCallback.sessionAttributeSet(this, name, oldValue, oldIsListener, value, newIsListener);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            if (!SessionManagerConfig.isHideSessionValues()) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SET_ATTRIBUTE], oldValue);
            } else {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[SET_ATTRIBUTE]);
            }
        }
        return oldValue;
    }

    /*
     * @see com.ibm.wsspi.session.ISession#removeAttribute(java.lang.Object)
     */
    @Override
    public synchronized Object removeAttribute(Object name) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            String s = name + appNameAndIdString;
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[REMOVE_ATTRIBUTE], s);
        }
        Object oldValue = _attributes.remove(name);
        Boolean oldIsBindingListener = (Boolean) _attributeNames.get(name);
        _attributeNames.remove(name);
        _storeCallback.sessionAttributeRemoved(this, name, oldValue, oldIsBindingListener);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            if (!SessionManagerConfig.isHideSessionValues()) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[REMOVE_ATTRIBUTE], oldValue);
            } else {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[REMOVE_ATTRIBUTE]);
            }
        }
        return oldValue;
    }

    /*
     * @see com.ibm.wsspi.session.ISession#getAttributes()
     * not implemented because its never called
     */
    @Override
    public synchronized Map getAttributes() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#getAttributeNames()
     */
    @Override
    public synchronized Enumeration getAttributeNames() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            String s = _attributeNames + appNameAndIdString;
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "getAttributeNames", s);
        }
        return _attributeNames.keys();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#getListenerAttributeNames()
     */
    @Override
    public synchronized ArrayList getListenerAttributeNames() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[GET_LISTENER_ATTRIBUTE_NAMES], appNameAndIdString);
        }
        ArrayList arrayList = new ArrayList();
        synchronized (_attributeNames) {
            String attributeName = null;
            Iterator iterator = _attributeNames.keySet().iterator();
            while (iterator.hasNext()) {
                attributeName = (String) iterator.next();
                if (_attributeNames.get(attributeName).equals(Boolean.TRUE)) {
                    arrayList.add(attributeName);
                }
            }
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[GET_LISTENER_ATTRIBUTE_NAMES], arrayList);
        }
        return arrayList;
    }

    /*
     * @see com.ibm.wsspi.session.ISession#hasAttributeListeners()
     * not implemented, never called
     */
    @Override
    public boolean hasAttributeListeners() {
        return false;
    }

    /*
     * @see com.ibm.wsspi.session.ISession#getAttributeListenerCount()
     * not implemented, never called
     */
    @Override
    public int getAttributeListenerCount() {
        return 0;
    }

    /*
     * @see com.ibm.wsspi.session.ISession#setAttributeListenerCount(int)
     * not implemented, never called
     */
    @Override
    public synchronized void setAttributeListenerCount(int attributeListenerCount) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#incrementRefCount()
     */
    @Override
    public synchronized int incrementRefCount() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            int temp = _refCount + 1;
            StringBuffer sb = new StringBuffer("to ").append(temp).append(" from ").append(_refCount).append(appNameAndIdString);
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "incrementRefCount", sb.toString());
        }
        _refCount++;
        return _refCount;
    }

    //only to be used to reset the refCount for ApplicationSessions
    @Override
    public synchronized void setRefCount(int i) {
        _refCount = i;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#getRefCount()
     */
    @Override
    public int getRefCount() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            String s = _refCount + appNameAndIdString;
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "getRefCount", s);
        }
        return _refCount;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#decrementRefCount()
     */
    @Override
    public synchronized int decrementRefCount() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            int temp = _refCount - 1;
            StringBuffer sb = new StringBuffer("to ").append(temp).append(" from ").append(_refCount).append(appNameAndIdString);
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "decrementRefCount", sb.toString());
        }
        _refCount--;
        return _refCount;
    }

    /*
     * @see com.ibm.wsspi.session.ISession#setSharingAcrossWebapps(boolean)
     * not used, not implemented
     */
    @Override
    public void setSharingAcrossWebapps(boolean flag) {

    }

    /*
     * @see com.ibm.wsspi.session.ISession#getLockContext()
     */
    @Override
    public Object getLockContext() {
        //Not used by WAS implementation
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#setLockContext(Object)
     */
    @Override
    public void setLockContext(Object lockContext) {
        //Not used by WAS implementation
        return;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#updateLastAccessTime()
     */
    @Override
    public void updateLastAccessTime() {
        //do nothing in our implementations
    }

    /**
     * Method updateLastAccessTime
     * <p>
     * This method updates the lastAccessTime and the currentAccessTime
     */
    @Override
    public synchronized void updateLastAccessTime(long accessTime) {
        if (_lastAccessedTime == 0) { // newly created session object
            _lastAccessedTime = accessTime;
        } else {
            _lastAccessedTime = _currentAccessTime;
        }
        _currentAccessTime = accessTime;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[UPDATE_LAST_ACCESS_TIME], "" + accessTime);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#getCurrentAccessTime()
     */
    @Override
    public long getCurrentAccessTime() {
        return _currentAccessTime;
    }

    /*
     * This returns the toString representation of the Session
     * If HideSessionValues is set, we will only show the attribute names, otherwise we will show the key/value pairs
     */
    @Override
    public synchronized String toString() {
        StringBuffer sb = new StringBuffer();
        //sb.append("# MemorySession # \n { ").
        sb.append("# ").append(this.getClass().getName()).append(" # \n { ")
                        .append("\n _sessionId=").append(_sessionId)
                        .append("\n")
                        .append("hashCode : ")
                        .append(hashCode())
                        .append("\n")
                        .append("create time : ")
                        .append(new Date(_creationTime).toString())
                        .append("\n")
                        .append("last access : ")
                        .append(new Date(_lastAccessedTime).toString())
                        .append("\n")
                        .append("max inactive interval : ")
                        .append(_maxInactiveInterval)
                        .append("\n")
                        .append("user name : ")
                        .append(_userName)
                        .append("\n")
                        .append("valid session : ")
                        .append(_isValid)
                        .append("\n")
                        .append("new session : ")
                        .append(_isNew)
                        .append("\n")
                        .append("overflowed : ")
                        .append(isOverflow)
                        .append("\n")
                        .append("app name : ")
                        .append(_appName)
                        .append("\n");
        if (_store != null) {
            if (SessionManagerConfig.isHideSessionValues()) {
                sb.append("\nAttribute Names=").append(_attributeNames.keySet()); //does this get all the attributes? swappable & non-swappable?
            } else if (_attributes != null) {
                sb.append("\nAttributes=").append(_attributes); //does this get all the attributes? swappable & non-swappable?
            }
            // cmd don't do this - causes db bug sb.append("\n The Serializable Attribute Names=").append(getSwappableData().keySet());
        }
        sb.append("\n _refCount=").append(_refCount)
                        .append("\n } \n");
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#setOverflow()
     */
    public void setOverflow() {
        isOverflow = true;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "setOverflow", appNameAndIdString);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#isOverflow()
     */
    public boolean isOverflow() {
        return isOverflow;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        // in-memory - do nothing
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        // in-memory - do nothing        
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#getStoreCallback()
     */
    public IStoreCallback getStoreCallback() {
        return _storeCallback;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#setStoreCallback()
     */
    public void setStoreCallback(IStoreCallback callback) {
        _storeCallback = callback;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISession#getIStore()
     */
    public IStore getIStore() {
        return _store;
    }

    /*
     * Mutator for the _store
     * The store is also set via constructor or BackedSession.initSession
     */
    public void setIStore(IStore store) {
        _store = store;
    }

    /*
     * To invalidate the session internally by sessionmanager
     */
    public void internalInvalidate(boolean timeoutOccurred) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[INTERNAL_INVALIDATE], appNameAndIdString);
        }
        //if it's not valid, just return
        if (_isValid) {
            //sets the Thread's context (classLoader) so that we can handle app specific listeners
            try {
                _store.setThreadContext();
                // PM11279: Call callInvalidateFromInternalInvalidate, which is overridden in MTMSesion
                callInvalidateFromInternalInvalidate();
                if (timeoutOccurred) {
                    _storeCallback.sessionInvalidatedByTimeout(this);
                }
            } finally {
                _store.unsetThreadContext();
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[INTERNAL_INVALIDATE]);
        }
    }

    /*
     * To get at the data that can by Serialized and stored externally
     * called from mBean
     */
    public Map<Object, Object> getSwappableData() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[GET_SWAPPABLE_DATA], appNameAndIdString);
        }
        Hashtable swapData = new Hashtable();
        if (_attributes != null) {
            for (Enumeration e = _attributes.keys(); e.hasMoreElements();) {
                Object mykey = e.nextElement();
                if (_attributes.get(mykey) instanceof Serializable) {
                    swapData.put(mykey, _attributes.get(mykey));
                }
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[GET_SWAPPABLE_DATA], swapData);
        }
        return swapData;
    }

    /*
     * To get at whether this session is in the process of being invalidated. We only want to invalidate once and
     * there could be a possibility of multiple Threads trying to invalidate the same session.
     */
    public boolean isInvalInProgress() {
        return invalInProgress;
    }

    /*
     * To get the application Name and Session ID for logging purposes. This will remain the same throughout
     * this session's lifetime, so we just need to get this string once per session.
     * For a backedSession, this needs to be called during initSession as well.
     */
    public String getAppNameAndID() {
        StringBuffer sb = new StringBuffer(" AppName=");
        sb.append(_appName);
        sb.append("; Id=").append(_sessionId);
        return sb.toString();
    }

    /*
     * Used to set the appName that this session belongs to. This is the equivalent to the storeId. This should
     * only be called when we are using persistent stores and need to recreate a session (either cloned or read from external)
     */
    protected void setAppName(String storeId) {
        _appName = storeId;
    }

    //needs to be public so BackedHashMap and DatabaseHashMap and MTMHashMap can have access
    //used to retrieve the appName for this session
    public String getAppName() {
        return _appName;
    }

    /**
     * Check if attribute is swappable as defined by J2EE
     */
    protected boolean isSwappableData(Object obj) {
        if (obj != null && (obj instanceof Serializable || obj instanceof Externalizable)) {
            return true;
        }
        return false;
    }

    // PM11279: overridden in BackedSesion
    protected void callInvalidateFromInternalInvalidate() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "callInvalidateFromInternalInvalidate", "calling invalidate");
        }
        this.invalidate();
    }

    //XD methods - don't need to implement
    public Object getAdaptation(Integer protocol) {
        return null;
    }

    public Object getCorrelator() {
        return null;
    }

    public void setAdaptation(Object adaptation, Integer protocol) {}
    //end of XD methods
    

    /* (non-Javadoc)
     * for the request.changeSessionId method call
     * @see com.ibm.wsspi.session.ISession#setId()
     */
    @Override
    public void setId(String id) {
        _sessionId = id;
    }

}
