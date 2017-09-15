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
package com.ibm.ws.httpsvc.session.internal;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Implementation of an HTTP servlet spec session.
 */
@SuppressWarnings("deprecation")
public class SessionImpl implements HttpSession {

    /** Debug variable */
    private static final TraceComponent tc = Tr.register(SessionImpl.class);

    private static final long NO_TIMEOUT = -1L;

    private String myID = null;
    private boolean valid = true;
    private boolean isNew = true;
    private ServletContext myContext = null;
    private final Map<String, Object> attributes = new HashMap<String, Object>();
    private long creationTime = 0L;
    private long lastAccesss = 0L;
    private int maxInactiveTimeSetting = -1;
    private long maxInactiveTime = NO_TIMEOUT;

    /**
     * Constructor.
     * 
     * @param id
     * @param context
     */
    public SessionImpl(String id, ServletContext context) {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        this.myID = id;
        this.myContext = context;
        this.creationTime = System.currentTimeMillis();
        this.lastAccesss = this.creationTime;
        this.isNew = true;
        this.valid = true;
        if (bTrace && tc.isEventEnabled()) {
            Tr.event(tc, "Created new session; " + this);
        }
        // List<HttpSessionListener> list = this.myConfig.getSessionListeners();
        // if (null != list) {
        // HttpSessionEvent event = new HttpSessionEvent(this);
        // for (HttpSessionListener listener : list) {
        // if (bTrace && tc.isDebugEnabled()) {
        // Tr.debug(tc, "Notifying listener of new session; " + listener);
        // }
        // listener.sessionCreated(event);
        // }
        // }
    }

    /**
     * Once a session has been invalidated, this will perform final cleanup
     * and notifying any listeners.
     */
    private void destroy() {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isEventEnabled()) {
            Tr.event(tc, "Session being destroyed; " + this);
        }
        // List<HttpSessionListener> list = this.myConfig.getSessionListeners();
        // if (null != list) {
        // HttpSessionEvent event = new HttpSessionEvent(this);
        // for (HttpSessionListener listener : list) {
        // if (bTrace && tc.isDebugEnabled()) {
        // Tr.debug(tc, "Notifying listener of destroy; " + listener);
        // }
        // listener.sessionDestroyed(event);
        // }
        // }
        for (String key : this.attributes.keySet()) {
            Object value = this.attributes.get(key);
            HttpSessionBindingEvent event = new HttpSessionBindingEvent(this, key);
            if (value instanceof HttpSessionBindingListener) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Notifying attribute of removal: " + value);
                }
                ((HttpSessionBindingListener) value).valueUnbound(event);
            }
        }
        this.attributes.clear();
        this.myContext = null;
    }

    /**
     * Query whether or not this session is still valid.
     * 
     * @return boolean, true means invalid, false means valid
     */
    public boolean isInvalid() {
        return !this.valid;
    }

    /**
     * Check for possible expiration of this existing session. This will
     * update the last access time if still valid, and changes the isNew
     * flag to false if not already (since we now know the client has
     * re-used the session information). This will return true if the
     * session is expired or false if still valid. The caller should
     * start the invalidation process if this returned true.
     * 
     * @param updateAccessTime
     * @return boolean
     */
    public boolean checkExpiration(boolean updateAccessTime) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "checkExpiration: " + this);
        }
        if (isInvalid()) {
            // we're marked invalid already
            return true;
        }
        long now = System.currentTimeMillis();
        if ((NO_TIMEOUT == this.maxInactiveTime)
            || (this.maxInactiveTime > (now - this.lastAccesss))) {
            // still valid session
            if (updateAccessTime) {
                this.isNew = false;
                this.lastAccesss = now;
            }
            return false;
        }
        return true;
    }

    /*
     * @see javax.servlet.http.HttpSession#getAttribute(java.lang.String)
     */
    @Override
    public Object getAttribute(String name) {
        if (isInvalid()) {
            throw new IllegalStateException("Session is invalid");
        }
        return this.attributes.get(name);
    }

    /*
     * @see javax.servlet.http.HttpSession#getAttributeNames()
     */
    @Override
    public Enumeration<String> getAttributeNames() {
        if (isInvalid()) {
            throw new IllegalStateException("Session is invalid");
        }
        return Collections.enumeration(this.attributes.keySet());
    }

    /*
     * @see javax.servlet.http.HttpSession#getCreationTime()
     */
    @Override
    public long getCreationTime() {
        if (isInvalid()) {
            throw new IllegalStateException("Session is invalid");
        }
        return this.creationTime;
    }

    /*
     * @see javax.servlet.http.HttpSession#getId()
     */
    @Override
    public String getId() {
        return this.myID;
    }

    /*
     * @see javax.servlet.http.HttpSession#getLastAccessedTime()
     */
    @Override
    public long getLastAccessedTime() {
        return this.lastAccesss;
    }

    /*
     * @see javax.servlet.http.HttpSession#getMaxInactiveInterval()
     */
    @Override
    public int getMaxInactiveInterval() {
        return this.maxInactiveTimeSetting;
    }

    /*
     * @see javax.servlet.http.HttpSession#getServletContext()
     */
    @Override
    public ServletContext getServletContext() {
        return this.myContext;
    }

    /*
     * @see javax.servlet.http.HttpSession#getSessionContext()
     */
    @Override
    public HttpSessionContext getSessionContext() {
        // this is deprecated and should return null now
        return null;
    }

    /*
     * @see javax.servlet.http.HttpSession#getValue(java.lang.String)
     */
    @Override
    public Object getValue(String name) {
        return getAttribute(name);
    }

    /*
     * @see javax.servlet.http.HttpSession#getValueNames()
     */
    @Override
    public String[] getValueNames() {
        if (isInvalid()) {
            throw new IllegalStateException("Session is invalid");
        }
        Set<String> keys = this.attributes.keySet();
        return keys.toArray(new String[keys.size()]);
    }

    /*
     * @see javax.servlet.http.HttpSession#invalidate()
     */
    @Override
    public void invalidate() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Invalidating session; " + this);
        }
        this.valid = false;
        destroy();
    }

    /*
     * @see javax.servlet.http.HttpSession#isNew()
     */
    @Override
    public boolean isNew() {
        return this.isNew;
    }

    /*
     * @see javax.servlet.http.HttpSession#putValue(java.lang.String, java.lang.Object)
     */
    @Override
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    /*
     * @see javax.servlet.http.HttpSession#removeAttribute(java.lang.String)
     */
    @Override
    public void removeAttribute(String name) {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeAttribute: " + name);
        }
        if (isInvalid()) {
            throw new IllegalStateException("Session is invalid");
        }
        Object attr = this.attributes.remove(name);
        if (null == attr) {
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attribute not found");
            }
            return;
        }
        HttpSessionBindingEvent event = null;
        if (attr instanceof HttpSessionBindingListener) {
            // this value wants to know when it's removed
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Notifying value; " + attr);
            }
            event = new HttpSessionBindingEvent(this, name, attr);
            ((HttpSessionBindingListener) attr).valueUnbound(event);
        }
        // List<HttpSessionAttributeListener> listeners = this.myConfig.getSessionAttributeListeners();
        // if (null != listeners) {
        // // notify any session-attribute listeners that were registered
        // // in the module configuration
        // if (null == event) {
        // event = new HttpSessionBindingEvent(this, name, attr);
        // }
        // for (HttpSessionAttributeListener listener : listeners) {
        // if (bTrace && tc.isDebugEnabled()) {
        // Tr.debug(tc, "Notifying listener: " + listener);
        // }
        // listener.attributeRemoved(event);
        // }
        // }
    }

    /*
     * @see javax.servlet.http.HttpSession#removeValue(java.lang.String)
     */
    @Override
    public void removeValue(String name) {
        removeAttribute(name);
    }

    /*
     * @see javax.servlet.http.HttpSession#setAttribute(java.lang.String, java.lang.Object)
     */
    @Override
    public void setAttribute(String name, Object value) {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "setAttribute: " + name + "=" + value);
        }
        if (null == value) {
            removeAttribute(name);
            return;
        }
        if (isInvalid()) {
            throw new IllegalStateException("Session is invalid");
        }
        HttpSessionBindingEvent replaceEvent = null;
        HttpSessionBindingEvent addEvent = null;
        Object current = this.attributes.remove(name);
        if (null != current) {
            replaceEvent = new HttpSessionBindingEvent(this, name, current);
            if (current instanceof HttpSessionBindingListener) {
                // inform the old value of it's removal
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Notifying old value: " + current);
                }
                ((HttpSessionBindingListener) current).valueUnbound(replaceEvent);
            }
        }
        this.attributes.put(name, value);
        if (value instanceof HttpSessionBindingListener) {
            // inform the new value of it's use
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Notifying new value: " + value);
            }
            addEvent = new HttpSessionBindingEvent(this, name, value);
            ((HttpSessionBindingListener) value).valueBound(addEvent);
        }
        // List<HttpSessionAttributeListener> list = this.myConfig.getSessionAttributeListeners();
        // if (null != list) {
        // // if we replaced a value, let the listeners now that. Otherwise,
        // // tell them of the new value being set
        // if (null != current) {
        // for (HttpSessionAttributeListener listener : list) {
        // if (bTrace && tc.isDebugEnabled()) {
        // Tr.debug(tc, "Notifying listener of replace: " + listener);
        // }
        // listener.attributeReplaced(replaceEvent);
        // }
        // } else {
        // if (null == addEvent) {
        // addEvent = new HttpSessionBindingEvent(this, name, value);
        // }
        // for (HttpSessionAttributeListener listener : list) {
        // if (bTrace && tc.isDebugEnabled()) {
        // Tr.debug(tc, "Notifying listener of set: " + listener);
        // }
        // listener.attributeAdded(addEvent);
        // }
        // }
        // }
    }

    /*
     * @see javax.servlet.http.HttpSession#setMaxInactiveInterval(int)
     */
    @Override
    public void setMaxInactiveInterval(int time) {
        this.maxInactiveTimeSetting = time;
        if (time < 0) {
            this.maxInactiveTime = NO_TIMEOUT;
        } else {
            this.maxInactiveTime = (time * 1000L);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "max-inactive time: " + this.maxInactiveTimeSetting);
        }
    }

    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append(getClass().getName());
        sb.append("; id=").append(this.myID);
        sb.append(" created=").append(this.creationTime);
        sb.append(" lastAccess=").append(this.lastAccesss);
        sb.append(" numAttrs=").append(this.attributes.keySet().size());
        return sb.toString();
    }
}
