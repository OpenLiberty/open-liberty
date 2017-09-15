/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.session.impl;

import java.util.logging.Level;

import javax.servlet.ServletContext;

import com.ibm.ws.session.MemoryStoreHelper;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

public class MemoryStoreHelperImpl implements MemoryStoreHelper {

    protected final ServletContext sc;

    public MemoryStoreHelperImpl(ServletContext sc) {
        this.sc = sc;
    }

    @Override
    public void setThreadContextDuringRunInvalidation() {
        setThreadContext(false);
    }
    
    @Override
    public void setThreadContext() {
        setThreadContext(true);
    }
    
    private void setThreadContext(boolean reportException) {
        if (this.sc != null) {
            try {
                ((IServletContext) this.sc).startEnvSetup(true);
            } catch (Exception e) {
                String method = "setThreadContext";
                if (reportException) {
                    com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, MemoryStoreHelperImpl.class.getName()+"."+method, "362", this);
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.SEVERE, MemoryStoreHelperImpl.class.getSimpleName(), method, "CommonMessage.exception", e);
                } else {
                    //throw the exception to the caller so that it can bail out
                    //this should only be called during runInvalidation and really only matters if the server is going down.
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, MemoryStoreHelperImpl.class.getSimpleName(), method, "CommonMessage.exception", e);
                    if (e instanceof RuntimeException) {
                        throw ((RuntimeException)e);
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
    
    @Override
    public void unsetThreadContext() {
        if (this.sc != null) {
            try {
                ((IServletContext) this.sc).finishEnvSetup(true);
            } catch (Exception e) {
                String method = "unsetThreadContext";
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, MemoryStoreHelperImpl.class.getName()+"."+method, "377", this);
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.SEVERE, MemoryStoreHelperImpl.class.getSimpleName(), method, "CommonMessage.exception", e);
            }
        }
    }
    
}
