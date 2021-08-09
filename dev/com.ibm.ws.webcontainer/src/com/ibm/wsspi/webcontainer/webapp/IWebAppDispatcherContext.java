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
package com.ibm.wsspi.webcontainer.webapp;

import java.io.IOException;

import javax.servlet.DispatcherType;

import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.webcontainer.RequestProcessor;

public interface IWebAppDispatcherContext {
    public boolean isEnforceSecurity();

    public void sessionPreInvoke();

    public void sessionPostInvoke();

    public RequestProcessor getCurrentServletReference();

    public void pushException(Throwable th);

    public boolean isInclude();

    public boolean isForward();

    public WebApp getWebApp();

    public String getRelativeUri();

    public String getOriginalRelativeURI();
    
    public void sendError(int sc, String message, boolean ignoreCommittedException) throws IOException;
    
    public DispatcherType getDispatcherType();

}
