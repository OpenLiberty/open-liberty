/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.utility.http;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.ibm.ws.jbatch.utility.utils.StringUtils;

public class HttpUtils {
    
    
    /**
     * 
     * Header parameters are in the form {parmName}={parmValue}.
     * 
     * This method assumes there are no spaces, commas, or semi-colons in the
     * parmName or parmValue.
     * 
     */
    public static String parseHeaderParameter(String headerValue, String parmName) {
        
        if (StringUtils.isEmpty(headerValue)) {
            return null;
        }
        
        for (String segment : headerValue.split(";|\\s+|,")) {
            if (segment.startsWith(parmName + "=")) {
                return segment.substring( (parmName + "=").length() ) ;
            }
        }
        
        return null;
    }
    
    /**
     * @return a TrustManager configured to trust ALL certificates.
     */
    public static TrustManager getTrustAllCertificates() {
        return new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        };
    }
    
    /**
     * @return a HostnameVerifier configured to trust all hostnames.
     */
    public static HostnameVerifier getTrustAllHostnames() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
    }
    
    /**
     * Set the default SSL context factor to trust all SSL certs and hostnames.
     */
    public static void setDefaultTrustAllCertificates() {


        try {
            SSLContext sc =  SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[] { getTrustAllCertificates() } , new SecureRandom());

        	//
        	// WARNING: BE VERY CAREFUL AND DON'T REUSE (COPY/PASTE) THIS CODE LIGHTLY.  
            // IT EFFECTIVELY DISABLES SSL.  IT'S OK IN THIS CONTEXT FROM THE COMMAND-LINE
            // WITH THE PARTICULAR PARAMETER THAT WAS SET.  PASTING INTO THE
        	// SERVER WOULD BE VERY BAD!
        	// 
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            
            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(getTrustAllHostnames());
            
            // Set as default for SimpleHttpClient.getConnection
            SSLContext.setDefault(sc);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

}
