/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.faces40.internal.fips;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.common.crypto.CryptoUtils;

// The FIPSInitializer activates FIPS 140-3 compliant algorithms (HmacSHA256, AES, and SHA256DRBG) for faces-4.0 and newer.
public class FIPSInitializer implements ServletContainerInitializer {

    // Log instance for this class
    protected static final Logger log = Logger.getLogger("io.openliberty.faces40.internal.fips");
    private static final String CLASS_NAME = "FIPSInitializer";

    private static String MAC_ALGORITHM = "org.apache.myfaces.MAC_ALGORITHM";
    private static String SESSION_ALGORITHM = "org.apache.myfaces.ALGORITHM";
    private static String VIEWSTATE_ID_ALGORITHM = "org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_ALGORITHM";
     // Note 'ALGORITM' is misspelled, but that's how it's documented.
    private static String CSRF_SESSION_ALGORITHM = "org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_ALGORITM";

    @Override
    public void onStartup(Set<Class<?>> clazzes, ServletContext servletContext) throws ServletException {

        if (CryptoUtils.isFips140_3EnabledWithBetaGuard()) {
            /*
             * Note: org.apache.myfaces.MAC_ALGORITHM and org.apache.myfaces.ALGORITHM have used HmacSHA256 and AES 
             * since 3.0.0 and 4.0.0 (see MYFACES-4376). No need to set them here. 
             */

            if (servletContext.getInitParameter(VIEWSTATE_ID_ALGORITHM) == null) {
                servletContext.setInitParameter(VIEWSTATE_ID_ALGORITHM, "SHA256DRBG");
                log(VIEWSTATE_ID_ALGORITHM + " not found. Setting to SHA256DRBG.");
            }
            if (servletContext.getInitParameter(CSRF_SESSION_ALGORITHM) == null) {
                servletContext.setInitParameter(CSRF_SESSION_ALGORITHM, "SHA256DRBG");
                log(CSRF_SESSION_ALGORITHM + " not found. Setting to SHA256DRBG.");
            }
        } else {
            log("FIPS not enabled. Skipping FIPSInitializer setup");
        }
    }

    private void log(String message) {
        if (log.isLoggable(Level.FINE)) {
            log.logp(Level.FINE, CLASS_NAME, "onStartup", message);
        }
    }
}
