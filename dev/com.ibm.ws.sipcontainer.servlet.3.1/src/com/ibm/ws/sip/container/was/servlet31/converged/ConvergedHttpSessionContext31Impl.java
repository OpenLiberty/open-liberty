/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.was.servlet31.converged;

import javax.servlet.ServletContext;
import javax.servlet.sip.ConvergedHttpSession;

import com.ibm.ws.session.SessionApplicationParameters;
import com.ibm.ws.session.SessionData;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStoreService;
import com.ibm.ws.webcontainer31.session.impl.HttpSessionContext31Impl;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.sip.converge.ConvergedHttpSessionContextImpl;
import com.ibm.wsspi.sip.converge.ConvergedHttpSessionImpl;

public class ConvergedHttpSessionContext31Impl extends HttpSessionContext31Impl{
    
	/**
     * @param smc
     * @param sap
     * @param sessionStoreService
     */
    public ConvergedHttpSessionContext31Impl(SessionManagerConfig smc, SessionApplicationParameters sap, SessionStoreService sessionStoreService) {
        super(smc, sap, sessionStoreService);
    }

    /*
     * createSessionObject
     */
    public Object createSessionObject(ISession isess, ServletContext servCtx){
      return new ConvergedHttpSessionImpl(isess, this, servCtx);
    }
    
    /**
     * 
     * @param session
     * @param contextPath
     * @param relativePath
     * @param scheme
     * @return
     */
    public String getSipBaseUrlForEncoding(ConvergedHttpSession session, String contextPath, String relativePath, String scheme) {
        return ConvergedHttpSessionContextImpl.getSipBaseUrlForEncoding(_smc, contextPath, relativePath, scheme, this);
    }
}
