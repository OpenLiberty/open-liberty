/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.internal.discovery;

import java.io.IOException;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.PrivilegedExceptionAction;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.jwt.config.ConsumerUtils;
import com.ibm.ws.security.jwt.utils.JwtUtils;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.ssl.SSLSupport;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;
import com.ibm.ws.security.common.http.HttpUtils;

/**
 *
 */
public class DiscoveryHandler {
    
    SSLSocketFactory sslSocketFactory;

    public DiscoveryHandler(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

//    @FFDCIgnore({ SSLException.class })
//    public boolean handleDiscoveryEndpoint(String discoveryUrl) {
//
//        String jsonString = null;
//        boolean valid = false;
//
//        if (!isValidDiscoveryUrl(discoveryUrl)) {
//            Tr.error(tc, "OIDC_CLIENT_DISCOVERY_SSL_ERROR", getId(), discoveryUrl);
//            return false;
//        }
//        try {
//            setNextDiscoveryTime(); //
//            SSLSocketFactory sslSocketFactory = getSSLSocketFactory(discoveryUrl, sslConfigurationName, sslSupportRef.getService());
//            HttpClient client = createHTTPClient(sslSocketFactory, discoveryUrl, hostNameVerificationEnabled);
//            jsonString = getHTTPRequestAsString(client, discoveryUrl);
//            if (jsonString != null) {
//                parseJsonResponse(jsonString);
//                if (this.discoveryjson != null) {
//                    valid = discoverEndpointUrls(this.discoveryjson);
//                }
//            }
//
//        } catch (SSLException e) {
//            if (tc.isDebugEnabled()) {
//                Tr.debug(tc, "Fail to get successful discovery response : ", e.getCause());
//            }
//
//        } catch (Exception e) {
//            // could be ignored
//            if (tc.isDebugEnabled()) {
//                Tr.debug(tc, "Fail to get successful discovery response : ", e.getCause());
//            }
//        }
//
//        if (!valid) {
//            Tr.error(tc, "OIDC_CLIENT_DISCOVERY_SSL_ERROR", getId(), discoveryUrl);
//        }
//        return valid;
//    }
}
