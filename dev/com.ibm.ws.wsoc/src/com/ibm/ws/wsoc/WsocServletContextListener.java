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
package com.ibm.ws.wsoc;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 *
 */
public class WsocServletContextListener implements ServletContextListener {

    EndpointManager endpointManager = null;
    private static final TraceComponent tc = Tr.register(WsocServletContextListener.class);

    public void initialize(EndpointManager em) {
        endpointManager = em;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        if (endpointManager != null) {
            if (!FrameworkState.isStopping()) {
                endpointManager.closeAllOpenSessions();
                endpointManager.clear();
            }
            else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Server is being shutdown, no need to stop stop clients");
                }
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent arg0) {

    }

}
