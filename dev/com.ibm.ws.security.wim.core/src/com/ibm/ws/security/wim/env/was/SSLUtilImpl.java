/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.env.was;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.naming.Context;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ConfigConstants;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.security.wim.env.ISSLUtil;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.exception.WIMSystemException;

@Trivial
public class SSLUtilImpl implements ISSLUtil {

    /**
     * Register the class to trace service.
     */
    private final static TraceComponent tc = Tr.register(SSLUtilImpl.class);

    @Override
    public Properties getSSLPropertiesOnThread() {
        return JSSEHelper.getInstance().getSSLPropertiesOnThread();
    }

    @Override
    public void resetSSLAlias() {
        JSSEHelper.getInstance().setSSLPropertiesOnThread(null);
    }

    @Override
    public void setSSLAlias(String sslAlias, Hashtable<?, ?> ldapEnv) throws WIMException {
        final String METHODNAME = "setSSLAlias";

        try {
            Map<String, Object> connectionInfo = null;
            Properties props;
            String provider = (String) ldapEnv.get(Context.PROVIDER_URL);

            if (provider != null) {
                // Get the first URL
                StringTokenizer providerTokens = new StringTokenizer(provider);
                provider = providerTokens.nextToken();

                // Create a URL to extract host-name and port
                provider = provider.replaceFirst("ldap", "http");
                URL providerURL = new URL(provider);

                // Set out bound connection info
                connectionInfo = new HashMap<String, Object>();
                connectionInfo.put(com.ibm.websphere.ssl.JSSEHelper.CONNECTION_INFO_DIRECTION, com.ibm.websphere.ssl.JSSEHelper.DIRECTION_OUTBOUND);
                connectionInfo.put(com.ibm.websphere.ssl.JSSEHelper.CONNECTION_INFO_REMOTE_HOST, providerURL.getHost());
                connectionInfo.put(com.ibm.websphere.ssl.JSSEHelper.CONNECTION_INFO_REMOTE_PORT, providerURL.getPort() == -1 ? "636" : Integer.toString(providerURL.getPort()));
            }

            if (connectionInfo != null)
                props = JSSEHelper.getInstance().getProperties(sslAlias, connectionInfo, null);
            else
                props = JSSEHelper.getInstance().getProperties(sslAlias);

            // Set properties to thread
            JSSEHelper.getInstance().setSSLPropertiesOnThread(props);

            if (tc.isDebugEnabled())
                Tr.debug(tc, METHODNAME + " Properties for SSL Alias '" + sslAlias + "':" + props);
        } catch (SSLException e) {
            throw new WIMSystemException(WIMMessageKey.INVALID_INIT_PROPERTY, Tr.formatMessage(
                                                                                               tc,
                                                                                               WIMMessageKey.INVALID_INIT_PROPERTY,
                                                                                               WIMMessageHelper.generateMsgParms(ConfigConstants.CONFIG_PROP_SSL_CONFIGURATION)));
        } catch (MalformedURLException e) {
            throw new WIMSystemException(WIMMessageKey.INVALID_INIT_PROPERTY, 
                    Tr.formatMessage(
                    tc,
                    WIMMessageKey.INVALID_INIT_PROPERTY,
                    WIMMessageHelper.generateMsgParms((String) ldapEnv.get(Context.PROVIDER_URL))
                    ));
		}
	}

    @Override
    public void setSSLPropertiesOnThread(Properties props) {
        JSSEHelper.getInstance().setSSLPropertiesOnThread(props);
    }
}
