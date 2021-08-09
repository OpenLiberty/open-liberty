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
package com.ibm.ws.security.csiv2.util;

import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.csiv2.config.ssl.SSLConfig;

/**
 * Class to obtain security OSGi services from non-OSGi code.
 */
public class SecurityServices {

    private static SSLConfig sslConfig;
    private static UnauthenticatedSubjectService unauthenticatedSubjectService;

    /**
     * @param sslSupport
     */
    public static synchronized void setupSSLConfig(SSLConfig sslConfig) {
        SecurityServices.sslConfig = sslConfig;
    }
    
    /**
     * @param unauthenticatedSubjectService
     */
    public static synchronized void setUnauthenticatedSubjectService(UnauthenticatedSubjectService unauthenticatedSubjectService) {
        SecurityServices.unauthenticatedSubjectService = unauthenticatedSubjectService;
    }

    public static synchronized SSLConfig getSSLConfig() {
        return sslConfig;
    }

    public static synchronized UnauthenticatedSubjectService getUnauthenticatedSubjectService() {
        return unauthenticatedSubjectService;
    }

    /**
     * Nulls out all allocated services.
     */
    public static synchronized void clean() {
        sslConfig = null;
        unauthenticatedSubjectService = null;
    }

}
