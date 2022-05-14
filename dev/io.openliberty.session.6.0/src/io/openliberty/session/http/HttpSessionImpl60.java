/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.session.http;

import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Level;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionBindingListener;

import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.session.ISession;


/**
 * This class provides the adapted version of the ISession.
 * It simply wrappers the session and proxies any of its method calls to
 * the underlying ISession object.
 *
 * session for servlet 6.0
 */
public class HttpSessionImpl60 implements HttpSession {

    
    private static final String methodClassName = "HttpSessionImpl60";
    
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
     * Adding a message for when IllegalStateException is thrown
     */
    private static final String iseMessage = "The method is called on an invalidated session: ";

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
    protected HttpSessionImpl60 (ISession session) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            if (!_loggedVersion) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "", "HttpSession implementation for servlet 6.0");
                _loggedVersion = true;
            }
        }
        _iSession = session;
        
        //System.out.println("PMDINH, HttpSession60Impl, servletVersion -> " +  com.ibm.ws.session.SessionContextRegistry.getInstance().getServerSMC().getServletVersion() );
    }

    /**
     * Method getCreationTime
     * <p>
     * 
     * @see javax.servlet.http.HttpSession#getCreationTime()
     */
    public long getCreationTime() {
        if (!_iSession.isValid())
            throw new IllegalStateException(iseMessage+_iSession.getId());
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
            throw new IllegalStateException(iseMessage+_iSession.getId());
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
     * Method getAttribute
     * <p>
     * 
     * @see javax.servlet.http.HttpSession#getAttribute(java.lang.String)
     */
    public Object getAttribute(String attributeName) {
        synchronized (_iSession) {
            if (!_iSession.isValid())
                throw new IllegalStateException(iseMessage+_iSession.getId());
            return _iSession.getAttribute(attributeName);
        }
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
                throw new IllegalStateException(iseMessage+_iSession.getId());
            return _iSession.getAttributeNames();
        }
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
                throw new IllegalStateException(iseMessage+_iSession.getId());
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
     * Method removeAttribute
     * <p>
     * 
     * @see javax.servlet.http.HttpSession#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String attributeName) {
        synchronized (_iSession) {
            if (!_iSession.isValid())
                throw new IllegalStateException(iseMessage+_iSession.getId());
            Object object = _iSession.removeAttribute(attributeName);
        }
    }

    /**
     * Method invalidate
     * <p>
     * 
     * @see javax.servlet.http.HttpSession#invalidate()
     */
    public void invalidate() {
        if (!_iSession.isValid())
            throw new IllegalStateException(iseMessage+_iSession.getId());
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
            throw new IllegalStateException(iseMessage+_iSession.getId());
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
        sb.append("# HttpSessionImpl60 # \n { ").append("\n _iSession=").append(_iSession).append("\n } \n");
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
