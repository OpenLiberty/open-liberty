/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.ws.security.policy.interceptors;

import java.net.HttpURLConnection;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.transport.http.MessageTrustDecider;
import org.apache.cxf.transport.http.URLConnectionInfo;
import org.apache.cxf.transport.http.UntrustedURLConnectionIOException;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.HttpsToken;

/**
 * 
 */
public class HttpsTokenInterceptorProvider extends AbstractPolicyInterceptorProvider {
    
    private static final long serialVersionUID = -13951002554477036L;

    public HttpsTokenInterceptorProvider() {
        super(Arrays.asList(SP11Constants.HTTPS_TOKEN, SP12Constants.HTTPS_TOKEN));
        this.getOutInterceptors().add(new HttpsTokenOutInterceptor());
        this.getOutFaultInterceptors().add(new HttpsTokenOutInterceptor());
        this.getInInterceptors().add(new HttpsTokenInInterceptor());
        this.getInFaultInterceptors().add(new HttpsTokenInInterceptor());
    }
    
    private static Map<String, List<String>> getSetProtocolHeaders(Message message) {
        Map<String, List<String>> headers =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));        
        if (null == headers) {
            headers = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
            message.put(Message.PROTOCOL_HEADERS, headers);
        }
        return headers;
    }

    static class HttpsTokenOutInterceptor extends AbstractPhaseInterceptor<Message> {
        public HttpsTokenOutInterceptor() {
            super(Phase.PRE_STREAM);
        }
        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais = aim.get(SP12Constants.HTTPS_TOKEN);
                if (ais == null) {
                    return;
                }
                if (isRequestor(message)) {
                    assertHttps(ais, message);
                } else {
                    //server side should be checked on the way in
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }                    
                }
            }
        }
        private void assertHttps(Collection<AssertionInfo> ais, Message message) {
            for (AssertionInfo ai : ais) {
                HttpsToken token = (HttpsToken)ai.getAssertion();
                
                HttpURLConnection connection = 
                    (HttpURLConnection) message.get("http.connection");
                
                ai.setAsserted(true);
                Map<String, List<String>> headers = getSetProtocolHeaders(message);
                
                if (connection instanceof HttpsURLConnection) {
                    if (token.isRequireClientCertificate()) {
                        final MessageTrustDecider orig = message.get(MessageTrustDecider.class);
                        MessageTrustDecider trust = new MessageTrustDecider() {
                            public void establishTrust(String conduitName,
                                                       URLConnectionInfo connectionInfo,
                                                       Message message)
                                throws UntrustedURLConnectionIOException {
                                if (orig != null) {
                                    orig.establishTrust(conduitName, connectionInfo, message);
                                }
                                HttpsURLConnectionInfo info = (HttpsURLConnectionInfo)connectionInfo;
                                if (info.getLocalCertificates() == null 
                                    || info.getLocalCertificates().length == 0) {
                                    throw new UntrustedURLConnectionIOException(
                                        "RequireClientCertificate is set, "
                                        + "but no local certificates were negotiated.  Is"
                                        + " the server set to ask for client authorization?");
                                }
                            }
                        };
                        message.put(MessageTrustDecider.class, trust);
                    }
                    if (token.isHttpBasicAuthentication()) {
                        List<String> auth = headers.get("Authorization");
                        if (auth == null || auth.size() == 0 
                            || !auth.get(0).startsWith("Basic")) {
                            ai.setNotAsserted("HttpBasicAuthentication is set, but not being used");
                        }
                    }
                    if (token.isHttpDigestAuthentication()) {
                        List<String> auth = headers.get("Authorization");
                        if (auth == null || auth.size() == 0 
                            || !auth.get(0).startsWith("Digest")) {
                            ai.setNotAsserted("HttpDigestAuthentication is set, but not being used");
                        }                        
                    }
                } else {
                    ai.setNotAsserted("HttpURLConnection is not a HttpsURLConnection");
                }
                if (!ai.isAsserted()) {
                    throw new PolicyException(ai);
                }
            }            
        }

    }
    
    static class HttpsTokenInInterceptor extends AbstractPhaseInterceptor<Message> {
        public HttpsTokenInInterceptor() {
            super(Phase.PRE_STREAM);
        }

        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais = aim.get(SP12Constants.HTTPS_TOKEN);
                if (ais == null) {
                    return;
                }
                if (!isRequestor(message)) {
                    assertHttps(ais, message);
                    // Store the TLS principal on the message context
                    SecurityContext sc = message.get(SecurityContext.class);
                    if (sc == null || sc.getUserPrincipal() == null) {
                        TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);      
                        if (tlsInfo != null && tlsInfo.getPeerCertificates() != null 
                                && tlsInfo.getPeerCertificates().length > 0
                                && (tlsInfo.getPeerCertificates()[0] instanceof X509Certificate)
                        ) {
                            X509Certificate cert = (X509Certificate)tlsInfo.getPeerCertificates()[0];
                            message.put(
                                SecurityContext.class, createSecurityContext(cert.getSubjectX500Principal())
                            );
                        } 
                    }
                    
                } else {
                    //client side should be checked on the way out
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }                    
                }
            }
        }
        
        private void assertHttps(Collection<AssertionInfo> ais, Message message) {
            for (AssertionInfo ai : ais) {
                boolean asserted = true;
                HttpsToken token = (HttpsToken)ai.getAssertion();
                
                Map<String, List<String>> headers = getSetProtocolHeaders(message);                
                if (token.isHttpBasicAuthentication()) {
                    List<String> auth = headers.get("Authorization");
                    if (auth == null || auth.size() == 0 
                        || !auth.get(0).startsWith("Basic")) {
                        asserted = false;
                    }
                }
                if (token.isHttpDigestAuthentication()) {
                    List<String> auth = headers.get("Authorization");
                    if (auth == null || auth.size() == 0 
                        || !auth.get(0).startsWith("Digest")) {
                        asserted = false;
                    }                        
                }

                TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);                
                if (tlsInfo != null) {
                    if (token.isRequireClientCertificate()
                        && (tlsInfo.getPeerCertificates() == null 
                            || tlsInfo.getPeerCertificates().length == 0)) {
                        asserted = false;
                    }
                } else {
                    asserted = false;
                }                
                
                ai.setAsserted(asserted);
            }
        }
        
        private SecurityContext createSecurityContext(final Principal p) {
            return new SecurityContext() {
                public Principal getUserPrincipal() {
                    return p;
                }
                public boolean isUserInRole(String role) {
                    return false;
                }
            };
        }
    }
}
