/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.ola.jca;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationService;

/**
 * OSGI service tracker for the SecurityService.
 * 
 * The WOLA JCA code uses the SecurityService to perform authentication of the
 * JAAS alias or username/password provided to the WOLA JCA connection.
 * 
 */
public class SecurityServiceTracker extends ServiceTracker<SecurityService, SecurityService> {

    /**
     * Static instance for simplicity.
     */
    static SecurityServiceTracker staticInstance;
    
    /**
     * CTOR.
     */
    public SecurityServiceTracker() {
        super( getBundleContext(), SecurityService.class, null);
    }
    
    /**
     * @return The BundleContext for the SecurityService class (in the com.ibm.ws.security bundle).
     */
    private static BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(SecurityService.class).getBundleContext();
    }
    
    /**
     * Open the tracker.
     * 
     * @return this
     */
    protected SecurityServiceTracker openMe() {
        open();
        return this;
    }
    
    /**
     * @return the static singleton instance of this class.
     */
    protected static SecurityServiceTracker getInstance() {
        if (staticInstance == null) {
            staticInstance = new SecurityServiceTracker().openMe();
        }
        return staticInstance;
    }
    
    /**
     * @return the authenticationService hung off the security service tracked by this tracker.
     */
    protected static AuthenticationService getAuthenticationService() {
        SecurityService securityService = getInstance().getService();
        return (securityService != null) ? securityService.getAuthenticationService() : null;
    }
}
