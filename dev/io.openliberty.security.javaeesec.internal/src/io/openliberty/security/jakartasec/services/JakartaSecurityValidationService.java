/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec.services;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Implementation for Jakarta Security 1.0, 2.0 and 3.0, providing 
 * functional services for components whose implementation or 
 * conditional evaluations that may be different for JS 1.0-3.0 than 4.0+.
 * 
 * This whole bundle is only loaded for JS 1.0-3.0 (appSecurity 1.0-5.0).
 * For JS 4.0+ (appSecurity-6.0), another bundle is loaded with the 
 * same package name and class, but different implementations.
 */

public class JakartaSecurityValidationService {

    private static final TraceComponent tc = Tr.register(JakartaSecurityValidationService.class);
    
    // static flag to ensure the debug message is logged only once
    private static volatile boolean debugMessageLogged = false;

    /**
     * Logs the implementation version message once across all 
     * methods which is useful debugging information.
     */
    private static void logImplementationOnce() {
        if (!debugMessageLogged && tc.isDebugEnabled()) {
        	Tr.debug(tc, "Using Jakarta Security 1.0/2.0/3.0 implementation.");
        	debugMessageLogged = true;
        }
    }
    
    /**
     * Are we operating in a JS 4.0 environment or higher - avoiding
     * costly operations with OSGi or class loading which may
     * yield the same information.
     *
     * @return true if in a JS 4.0 environment or higher, false else.
     */
    public static boolean isJakartaSecurity40OrHigher() {
    	logImplementationOnce();
    	return false;
    }
}
