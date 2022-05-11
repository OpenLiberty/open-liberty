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
package io.openliberty.webcontainer60.session.impl;

import jakarta.servlet.ServletContext;

import java.util.logging.Level;
 
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.session.SessionContext;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.ws.webcontainer.facade.IFacade;
import com.ibm.ws.webcontainer.session.IHttpSession;
import com.ibm.wsspi.session.ISession;

import io.openliberty.session.impl.SessionData60;

/*
 * Since Servlet 6.0
 */

public class WCHttpSessionImpl60 extends SessionData60 implements IHttpSession, IFacade
{
    public WCHttpSessionImpl60 (ISession session, SessionContext sessCtx, ServletContext servCtx)
    {
        super(session, sessCtx, servCtx);
        
        if (TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, "WCHttpSessionImpl60" + " Constructor");
        }
    }

    /*
     * For security to store away special hidden value in the session
     * 
     * @see
     * com.ibm.ws.webcontainer.session.IHttpSession#putSecurityInfo(java.lang.
     * Object)
     */
    public void putSecurityInfo(Object value)
    {
        putSessionValue(SECURITY_PROP_NAME, value, true);
        _hasSecurityInfo = true;
    }

    /*
     * For security to retrieve special hidden value in the session
     * 
     * @see com.ibm.ws.webcontainer.session.IHttpSession#getSecurityInfo()
     */
    public Object getSecurityInfo()
    {
        return getSessionValue(SECURITY_PROP_NAME, true);
    }

    /*
     * To get the facade given out to the application
     * 
     * @see com.ibm.ws.webcontainer.facade.IFacade#getFacade()
     */
    public Object getFacade()
    {
        return (Object) _httpSessionFacade;
    }

}
