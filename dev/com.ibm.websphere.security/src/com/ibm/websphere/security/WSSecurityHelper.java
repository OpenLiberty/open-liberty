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
package com.ibm.websphere.security;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.intfc.WSSecurityService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Provides methods to perform security functions
 * 
 * @author International Business Machines Corp.
 * @version WAS 8.5
 * @since WAS 5.1.1
 * @ibm-api
 */
public class WSSecurityHelper {

    private final static AtomicServiceReference<WSSecurityService> securityServiceRef =
                    new AtomicServiceReference<WSSecurityService>(WSSecurityService.KEY_WS_SECURITY_SERVICE);

    protected void activate(ComponentContext cc) {
        securityServiceRef.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        securityServiceRef.deactivate(cc);
    }

    protected void setWsSecurityService(ServiceReference<WSSecurityService> reference) {
        securityServiceRef.setReference(reference);
    }

    protected void unsetWsSecurityService(ServiceReference<WSSecurityService> reference) {
        securityServiceRef.unsetReference(reference);
    }

    /**
     * <p>
     * The returning value of this method is the same as isGlobalSecurityEnabled method.
     * This method is for the compatibility with the traditional WebSphere Application Server.
     * </p>
     * @ return boolean true if the security service is activated, false otherwise.
     * 
     * @ibm-api
     **/
    public static boolean isServerSecurityEnabled() {
        WSSecurityService ss = securityServiceRef.getService();
        boolean enabled = false;
        if (ss != null)
            enabled = ss.isSecurityEnabled();
        return enabled;
    }

    /**
     * <p>
     * This method returns whether the security service is activated.
     * </p>
     * @ return boolean true if the security service is activated, false otherwise.
     * 
     * @ibm-api
     **/
    public static boolean isGlobalSecurityEnabled() {
        WSSecurityService ss = securityServiceRef.getService();
        boolean enabled = false;
        if (ss != null)
            enabled = ss.isSecurityEnabled();
        return enabled;
    }
}
