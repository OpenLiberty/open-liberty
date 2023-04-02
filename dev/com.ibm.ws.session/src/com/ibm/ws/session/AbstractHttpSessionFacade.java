/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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

import com.ibm.websphere.servlet.session.IBMApplicationSession;
import com.ibm.websphere.servlet.session.IBMSession;
import com.ibm.wsspi.servlet.session.IBMSessionExt;

/*
 * Since Servlet 6.0 - Common APIs 
 */

public abstract class AbstractHttpSessionFacade implements IBMSession, IBMSessionExt { 

    protected transient AbstractSessionData _session = null;
    
    protected AbstractHttpSessionFacade(AbstractSessionData sessionData) {
        _session = sessionData;
    }

    /**
     * @see IBMSession#getUserName()
     */
    @Override
    public final String getUserName() {
        return _session.getUserName();
    }

    /**
     * @see IBMSession#sync()
     */
    @Override
    public final void sync() {
        _session.sync();
    }

    /**
     * @see IBMSession#isOverflow()
     */
    @Override
    public final boolean isOverflow() {
        return _session.isOverflow();
    }

    /**
     * @see IBMSessionExt#invalidateAll()
     */
    @Override
    public final void invalidateAll() {
        _session.invalidateAll();
    }

    /**
     * @see IBMSessionExt#invalidateAll(boolean)
     */
    @Override
    public final void invalidateAll(boolean remote) {
        _session.invalidateAll(remote);
    }

    /**
     * @see HttpSession#getCreationTime()
     */
    @Override
    public final long getCreationTime() {
        return _session.getCreationTime();
    }

    /**
     * @see HttpSession#getId()
     */
    @Override
    public final String getId() {
        return _session.getId();
    }

    /**
     * @see HttpSession#getLastAccessedTime()
     */
    @Override
    public final long getLastAccessedTime() {
        return _session.getLastAccessedTime();
    }

    /**
     * @see HttpSession#getServletContext()
     */
    @Override
    public final ServletContext getServletContext() {
        return _session.getServletContext();
    }

    /**
     * @see HttpSession#setMaxInactiveInterval(int)
     */
    @Override
    public final void setMaxInactiveInterval(int arg0) {
        _session.setMaxInactiveInterval(arg0);
    }

    /**
     * @see HttpSession#getMaxInactiveInterval()
     */
    @Override
    public final int getMaxInactiveInterval() {
        return _session.getMaxInactiveInterval();
    }

    /**
     * @see HttpSession#getAttribute(String)
     */
    @Override
    public final Object getAttribute(String arg0) {
        return _session.getAttribute(arg0);
    }

    /**
     * @see HttpSession#getAttributeNames()
     */
    @Override
    public final Enumeration<String> getAttributeNames() {
        return _session.getAttributeNames();
    }

    /**
     * @see HttpSession#setAttribute(String, Object)
     */
    @Override
    public final void setAttribute(String arg0, Object arg1) {
        _session.setAttribute(arg0, arg1);
    }


    /**
     * @see HttpSession#removeAttribute(String)
     */
    @Override
    public final void removeAttribute(String arg0) {
        _session.removeAttribute(arg0);
    }


    /**
     * @see HttpSession#invalidate()
     */
    @Override
    public final void invalidate() {
        _session.invalidate();
    }

    /**
     * @see HttpSession#isNew()
     */
    @Override
    public final boolean isNew() {
        return _session.isNew();
    }

    /**
     * toString
     */
    @Override
    public String toString() {
        return _session.toString();
    }

    // -------------------------------------
    // java.io.Externalizable methods
    // -------------------------------------
    @Override
    public void writeExternal(ObjectOutput pOut) throws IOException {
        _session.writeExternal(pOut);
    }

    @Override
    public void readExternal(ObjectInput pIn) throws IOException, ClassNotFoundException {
        _session.readExternal(pIn);
    }

    // Added to IBMSession interface
    @Override
    public final IBMApplicationSession getIBMApplicationSession() {
        return _session.getIBMApplicationSession();
    }

    @Override
    public final IBMApplicationSession getIBMApplicationSession(boolean create) {
        return _session.getIBMApplicationSession(create);
    }

    // Internal for SIP to set application session -- added to IBMSessionExt
    // interface without javadoc
    public void setIBMApplicationSession(IBMApplicationSession IBMAppSess) {
        _session.setIBMApplicationSession(IBMAppSess);
    }
}
