/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap.context;

import static com.ibm.websphere.ssl.JSSEHelper.CONNECTION_INFO_DIRECTION;
import static com.ibm.websphere.ssl.JSSEHelper.CONNECTION_INFO_REMOTE_HOST;
import static com.ibm.websphere.ssl.JSSEHelper.CONNECTION_INFO_REMOTE_PORT;
import static com.ibm.websphere.ssl.JSSEHelper.DIRECTION_OUTBOUND;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.naming.Context;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.ws.security.wim.adapter.ldap.LdapConstants;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.exception.WIMSystemException;

@Trivial
public class SSLUtilImpl {

    /**
     * Register the class to trace service.
     */
    private final static TraceComponent tc = Tr.register(SSLUtilImpl.class);

    public Properties getSSLPropertiesOnThread() {
        return AccessController.doPrivileged(new PrivilegedAction<Properties>() {
            @Override
            public Properties run() {
                return JSSEHelper.getInstance().getSSLPropertiesOnThread();
            }
        });
    }

    public void resetSSLAlias() {
        setSSLPropertiesOnThread(null);
    }

    public void setSSLAlias(final String sslAlias, Hashtable<?, ?> ldapEnv) throws WIMException {
        final String METHODNAME = "setSSLAlias";

        final Map<String, Object> connectionInfo = new HashMap<String, Object>();;
        String provider = (String) ldapEnv.get(Context.PROVIDER_URL);

        if (provider != null) {
            /*
             * Get the first URL
             */
            StringTokenizer providerTokens = new StringTokenizer(provider);
            provider = providerTokens.nextToken();

            /*
             * Create a URL to extract host-name and port
             */
            provider = provider.replaceFirst("ldap", "http");
            URL providerURL = null;
            try {
                providerURL = new URL(provider);
            } catch (MalformedURLException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.INVALID_INIT_PROPERTY, WIMMessageHelper.generateMsgParms(ldapEnv.get(Context.PROVIDER_URL)));
                throw new WIMSystemException(WIMMessageKey.INVALID_INIT_PROPERTY, msg);
            }

            /*
             * Set out bound connection info
             */
            connectionInfo.put(CONNECTION_INFO_DIRECTION, DIRECTION_OUTBOUND);
            connectionInfo.put(CONNECTION_INFO_REMOTE_HOST, providerURL.getHost());
            connectionInfo.put(CONNECTION_INFO_REMOTE_PORT, providerURL.getPort() == -1 ? "636" : Integer.toString(providerURL.getPort()));
        }

        /*
         * Get the SSL properties.
         */
        Properties props;
        try {
            props = AccessController.doPrivileged(new PrivilegedExceptionAction<Properties>() {
                @Override
                public Properties run() throws Exception {
                    if (!connectionInfo.isEmpty()) {
                        return JSSEHelper.getInstance().getProperties(sslAlias, connectionInfo, null);
                    } else {
                        return JSSEHelper.getInstance().getProperties(sslAlias);
                    }
                }
            });
        } catch (PrivilegedActionException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.INVALID_INIT_PROPERTY, WIMMessageHelper.generateMsgParms(LdapConstants.CONFIG_PROP_SSL_CONFIGURATION));
            throw new WIMSystemException(WIMMessageKey.INVALID_INIT_PROPERTY, msg, e);
        }

        /*
         * Set SSL properties to thread
         */
        setSSLPropertiesOnThread(props);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " Properties for SSL Alias '" + sslAlias + "':" + props);
        }
    }

    public void setSSLPropertiesOnThread(final Properties props) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                JSSEHelper.getInstance().setSSLPropertiesOnThread(props);
                return null;
            }
        });
    }
}
