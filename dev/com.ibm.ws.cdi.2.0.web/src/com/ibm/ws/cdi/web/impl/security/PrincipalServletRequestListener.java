/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.web.impl.security;

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

/**
 * Class sets/removes HttpServletRequest to enable access to
 * HttpServletRequest.getUserPrincipal()
 */
public class PrincipalServletRequestListener implements ServletRequestListener {

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        WebSecurityContextStore.getCurrentInstance().removeHttpServletRequest();
    }

    @Override
    public void requestInitialized(ServletRequestEvent event) {
        ServletRequest request = event.getServletRequest();
        if (request instanceof HttpServletRequest) {
            WebSecurityContextStore.getCurrentInstance().storeHttpServletRequest((HttpServletRequest) request);
        }
    }

}
