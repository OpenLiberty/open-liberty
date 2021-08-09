/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cloudant.internal;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Properties;

import javax.net.ssl.SSLSocketFactory;

public class SSLHelper 
{
    /**
     * Get an ssl socket factory, if configured
     */
    public static SSLSocketFactory getSSLSocketFactory(Object sslConfig) throws Exception {
        SSLSocketFactory sslSF = null;
        if(sslConfig != null) {
            String sslAlias = ((com.ibm.wsspi.ssl.SSLConfiguration) sslConfig).getAlias();
            final com.ibm.websphere.ssl.JSSEHelper helper = com.ibm.websphere.ssl.JSSEHelper.getInstance();
            final Properties sslProps = helper.getProperties(sslAlias);
            
            sslSF = AccessController.doPrivileged(new PrivilegedExceptionAction<SSLSocketFactory>() {
                @Override
                public SSLSocketFactory run() throws Exception {
                    return helper.getSSLSocketFactory(Collections.<String, Object>emptyMap(), sslProps);
                }
            });
        }
        return sslSF;
    }
}
