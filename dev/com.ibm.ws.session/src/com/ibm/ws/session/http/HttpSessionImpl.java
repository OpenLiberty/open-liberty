/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.http;

import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Level;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;

import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.session.ISession;

/**
 * This class provides the adapted version of the ISession.
 * It simply wrappers the session and proxies any of its method calls to
 * the underlying ISession object.
 * 
 * @author dettlaff
 */
public class HttpSessionImpl implements HttpSession {

    // ----------------------------------------
    // Private Members
    // ----------------------------------------
    /*
     * For logging.
     */
    private static final String methodClassName = "HttpSessionImpl";
    /*
     * For logging the CMVC file version once.
     */
    private static boolean _loggedVersion = false;

    // TODO Do isValid checks all over the place and throw IllegalStateExceptions

    /*
     * A reference to the wrapper iManagedSession object
     */
    private ISession _iSession;

    /*
     * A reference to the ServletContext object returns for all http sessions
     * created in this SessionManager.
     */
    private ServletContext _servletContext;

    /*
     * A reference to the HttpSessionContext singleton that will
     * be returned for all http sessions.
     * 
     * @deprecated
     */
    private static final HttpSessionContext _httpSessionContext = new HttpSessionContextImpl();

    // ----------------------------------------
    // Constructor
    // ----------------------------------------
    /**
     * Class Constructor
     * <p>
     * Note: This method receives an instance of IManagedSession as an argument, but it also needs to be an instance of ISession. If not, this implementation will be broken.
     * 
     * @param session
     */
    protected HttpSessionImpl(ISession session) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            if (!_loggedVersion) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "", "CMVC Version 1.6 10/16/08 11:56:10");
                _loggedVersion = true;
            }
        }
        _iSession = session;
    }

    // ----------------------------------------
    // Public Methods
    // ----------------------------------------
    /**
     * Method getCreationTime
     * <p>
     * 
     * @see javax.servlet.http.HttpSession#getCreationTime()
     */
    public long getCreationTime() {
        if (!_iSession.isValid())
            throw new IllegalStateException();
        return _iSession.getCreationTime();
    }

    /**
     * Method getId
     * <p>
     * 
     * @see javax.servlet.http.HttpSession#getId()
     */
    public String getId() {
        return _iSession.getId();
    }

    /**
     * Method getLastAccessedTime
     * <p>
     * 
     * @see javax.servlet.http.HttpSession#getLastAccessedTime()
     */
    public long getLastAccessedTime() {
        if (!_iSession.isValid())
            throw new IllegalStateException();
        return _iSession.getLastAccessedTime();
    }

    /**
     * Method setServletContext
     * <p>
     * 
     * @param context
     * @return ServletContext
     * @see javax.servlet.http.HttpSession#getServletContext()
     */
    public ServletContext setServletContext(ServletContext context) {
        return _servletContext = context;
    }

    /**
     * Method getServletContext
     * <p>
     * 
     * @see javax.servlet.http.HttpSession#getServletContext()
     */
    public ServletContext getServletContext() {
        return _servletContext;
    }

    /**
     * Method setMaxInactiveInterval
     * <p>
     * 
     * @see javax.servlet.http.HttpSession#setMaxInactiveInterval(int)
     */
    public void setMaxInactiveInterval(int maxInactiveInterval) {
        _iSession.setMaxInactiveInterval(maxInactiveInterval);

    }

    /**
     * Method getMaxInactiveInterval
     * <p>
     * 
     * @see javax.servlet.http.HttpSession#getMaxInactiveInterval()
     */
    public int getMaxInactiveInterval() {
        return _iSession.getMaxInactiveInterval();
    }

    /**
     * Method getSessionContext
     * <p>
     * 
     * @deprecated
     * @see javax.servlet.http.HttpSession#getSessionContext()
     */
    public HttpSessionContext getSessionContext() {
        return _httpSessionContext;
    }

    /**
     * Method getAttribute
     * <p>
     * 
     * @see javax.servlet.http.HttpSession#getAttribute(java.lang.String)
     */
    public Object getAttribute(String attributeName) {
        synchronized (_iSession) {
            if (!_iSession.isValid())
                throw new IllegalStateException("The following session is not valid! "+_iSession.getId());
            return _iSession.getAttribute(attributeName);
        }
    }

    /**
     * Method getValue
     * <p>
     * 
     * @deprecated
     * @see javax.servlet.http.HttpSession#getValue(java.lang.String)
     */
    public Object getValue(String attributeName) {
        return this.getAttribute(attributeName);
    }

    /**
     * Method getAttributeNames
     * <p>
     * 
     * @see javax.servlet.http.HttpSession#getAttributeNames()
     */
    public Enumeration getAttributeNames() {
        synchronized (_iSession) {
            if (!_iSession.isValid())
                throw new IllegalStateException();
            return _iSession.getAttributeNames();
        }
    }

    /**
     * Method getValueNames
     * <p>
     * 
     * @deprecated
     * @see javax.servlet.http.HttpSession#getValueNames()
     */
    public String[] getValueNames() {
        if (!_iSession.isValid())
            throw new IllegalStateException();
        Enumeration enumeration = this.getAttributeNames();
        Vector valueNames = new Vector();
        String name = null;
        while (enumeration.hasMoreElements()) {
            name = (String) enumeration.nextElement();
            valueNames.add(name);
        }
        String[] names = new String[valueNames.size()];
        return (String[]) valueNames.toArray(names);
    }

    /**
     * Method setAttribute
     * <p>
     * 
     * @see javax.servlet.http.HttpSession#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(String attributeName, Object value) {
        synchronized (_iSession) {
            if (!_iSession.isValid())
                throw new IllegalStateException();
            if (null != value) {
                if (!(value instanceof HttpSessionBindingListener)) {
                    _iSession.setAttribute(attributeName, value, Boolean.FALSE);
                } else {
                    _iSession.setAttribute(attributeName, value, Boolean.TRUE);
                }
            }
        }
    }

    /**
     * Method putValue
     * <p>
     * 
     * @deprecated
     * @see javax.servlet.http.HttpSession#putValue(java.lang.String, java.lang.Object)
     */
    public void putValue(String attributeName, Object value) {
        this.setAttribute(attributeName, value);

    }

    /**
     * Method removeAttribute
     * <p>
     * 
     * @see javax.servlet.http.HttpSession#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String attributeName) {
        synchronized (_iSession) {
            if (!_iSession.isValid())
                throw new IllegalStateException();
            Object object = _iSession.removeAttribute(attributeName);
        }
    }

    /**
     * Method removeValue
     * <p>
     * 
     * @deprecated
     * @see javax.servlet.http.HttpSession#removeValue(java.lang.String)
     */
    public void removeValue(String attributeName) {
        this.removeAttribute(attributeName);
    }

    /**
     * Method invalidate
     * <p>
     * 
     * @see javax.servlet.http.HttpSession#invalidate()
     */
    public void invalidate() {
        if (!_iSession.isValid())
            throw new IllegalStateException();
        _iSession.invalidate();
    }

    /**
     * Method isNew
     * <p>
     * 
     * @see javax.servlet.http.HttpSession#isNew()
     */
    public boolean isNew() {
        if (!_iSession.isValid())
            throw new IllegalStateException();
        return _iSession.isNew();

    }

    /**
     * Method toString
     * <p>
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("# HttpSessionImpl # \n { ").append("\n _iSession=").append(_iSession).append("\n _httpSessionContext=").append(_httpSessionContext).append("\n } \n");
        return sb.toString();
    }

    /**
     * Method getISession
     * <p>
     * 
     * @return ISession
     */
    public ISession getISession() {
        return _iSession;
    }

}