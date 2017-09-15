/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import com.ibm.websphere.servlet.session.IBMApplicationSession;
import com.ibm.websphere.servlet.session.IBMSession;
import com.ibm.wsspi.servlet.session.IBMSessionExt;

public class HttpSessionFacade implements IBMSession, IBMSessionExt { // cmd 196151

    protected transient SessionData _session = null;
    private static final long serialVersionUID = 3108339284895967670L;

    public HttpSessionFacade(SessionData data) {
        this._session = data;
    }

    /**
     * @see IBMSession#getUserName()
     */
    public String getUserName() {
        return _session.getUserName();
    }

    /**
     * @see IBMSession#sync()
     */
    public void sync() {
        _session.sync();
    }

    /**
     * @see IBMSession#isOverflow()
     */
    public boolean isOverflow() {
        return _session.isOverflow();
    }

    /**
     * @see IBMSessionExt#invalidateAll()
     */
    public void invalidateAll() {
        _session.invalidateAll();
    }

    /**
     * @see IBMSessionExt#invalidateAll(boolean)
     */
    public void invalidateAll(boolean remote) {
        _session.invalidateAll(remote);
    }

    /**
     * @see HttpSession#getCreationTime()
     */
    public long getCreationTime() {
        return _session.getCreationTime();
    }

    /**
     * @see HttpSession#getId()
     */
    public String getId() {
        return _session.getId();
    }

    /**
     * @see HttpSession#getLastAccessedTime()
     */
    public long getLastAccessedTime() {
        return _session.getLastAccessedTime();
    }

    /**
     * @see HttpSession#getServletContext()
     */
    public ServletContext getServletContext() {
        return _session.getServletContext();
    }

    /**
     * @see HttpSession#setMaxInactiveInterval(int)
     */
    public void setMaxInactiveInterval(int arg0) {
        _session.setMaxInactiveInterval(arg0);
    }

    /**
     * @see HttpSession#getMaxInactiveInterval()
     */
    public int getMaxInactiveInterval() {
        return _session.getMaxInactiveInterval();
    }

    /**
     * @see HttpSession#getSessionContext()
     * @deprecated
     */
    public HttpSessionContext getSessionContext() {
        return _session.getSessionContext();
    }

    /**
     * @see HttpSession#getAttribute(String)
     */
    public Object getAttribute(String arg0) {
        return _session.getAttribute(arg0);
    }

    /**
     * @see HttpSession#getValue(String)
     * @deprecated
     */
    public Object getValue(String arg0) {
        return _session.getValue(arg0);
    }

    /**
     * @see HttpSession#getAttributeNames()
     */
    public Enumeration getAttributeNames() {
        return _session.getAttributeNames();
    }

    /**
     * @see HttpSession#getValueNames()
     * @deprecated
     */
    public String[] getValueNames() {
        return _session.getValueNames();
    }

    /**
     * @see HttpSession#setAttribute(String, Object)
     */
    public void setAttribute(String arg0, Object arg1) {
        _session.setAttribute(arg0, arg1);
    }

    /**
     * @see HttpSession#putValue(String, Object)
     * @deprecated
     */
    public void putValue(String arg0, Object arg1) {
        _session.putValue(arg0, arg1);
    }

    /**
     * @see HttpSession#removeAttribute(String)
     */
    public void removeAttribute(String arg0) {
        _session.removeAttribute(arg0);
    }

    /**
     * @see HttpSession#removeValue(String)
     * @deprecated
     */
    public void removeValue(String arg0) {
        _session.removeValue(arg0);
    }

    /**
     * @see HttpSession#invalidate()
     */
    public void invalidate() {
        _session.invalidate();
    }

    /**
     * @see HttpSession#isNew()
     */
    public boolean isNew() {
        return _session.isNew();
    }

    /**
     * toString
     */
    public String toString() {
        return _session.toString();
    }

    // -------------------------------------
    // java.io.Externalizable methods
    // -------------------------------------
    public void writeExternal(ObjectOutput pOut) throws IOException {
        _session.writeExternal(pOut);
    }

    public void readExternal(ObjectInput pIn) throws IOException, ClassNotFoundException {
        _session.readExternal(pIn);
    }

    // LIBERTY implemented in webcontainer class IHttpSessionFacadeImpl
    /*
     * public Object getSecurityInfo() {
     * return ((IHttpSessionImpl)_session).getSecurityInfo();
     * }
     */

    // LIBERTY implemented in webcontainer class IHttpSessionFacadeImpl
    /*
     * public void putSecurityInfo(Object pValue) {
     * ((IHttpSessionImpl)_session).putSecurityInfo(pValue);
     * }
     */

    // Added to IBMSession interface
    public IBMApplicationSession getIBMApplicationSession() {
        return _session.getIBMApplicationSession();
    }

    public IBMApplicationSession getIBMApplicationSession(boolean create) {
        return _session.getIBMApplicationSession(create);
    }

    // Internal for SIP to set application session -- added to IBMSessionExt
    // interface without javadoc
    public void setIBMApplicationSession(IBMApplicationSession IBMAppSess) {
        _session.setIBMApplicationSession(IBMAppSess);
    }

}
