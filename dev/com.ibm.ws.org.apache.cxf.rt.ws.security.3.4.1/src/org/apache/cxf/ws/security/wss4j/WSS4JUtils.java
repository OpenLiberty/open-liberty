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
package org.apache.cxf.ws.security.wss4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.crypto.SecretKey;
import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.cache.CXFEHCacheReplayCache;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.tokenstore.TokenStoreUtils;
import org.apache.wss4j.common.cache.MemoryReplayCache;
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.cache.WSS4JCacheUtil;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.JasyptPasswordEncryptor;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.Loader;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.xml.security.exceptions.XMLSecurityException;

/**
 * Some common functionality that can be shared between the WSS4JInInterceptor and the
 * UsernameTokenInterceptor.
 */
public final class WSS4JUtils {

    private static final Logger LOG = LogUtils.getL7dLogger(WSS4JUtils.class);

    private WSS4JUtils() {
        // complete
    }

    /**
     * Get the security token lifetime value (in milliseconds). The default is "300000" (5 minutes).
     * @return the security token lifetime value in milliseconds
     */
    public static long getSecurityTokenLifetime(Message message) {
        if (message != null) {
            String tokenLifetime =
                (String)message.getContextualProperty(SecurityConstants.SECURITY_TOKEN_LIFETIME);
            if (tokenLifetime != null) {
                return Long.parseLong(tokenLifetime);
            }
        }
        return 300000L;
    }

    /**
     * Get a ReplayCache instance. It first checks to see whether caching has been explicitly
     * enabled or disabled via the booleanKey argument. If it has been set to false then no
     * replay caching is done (for this booleanKey). If it has not been specified, then caching
     * is enabled only if we are not the initiator of the exchange. If it has been specified, then
     * caching is enabled.
     *
     * It tries to get an instance of ReplayCache via the instanceKey argument from a
     * contextual property, and failing that the message exchange. If it can't find any, then it
     * defaults to using an EH-Cache instance and stores that on the message exchange.
     */
    public static ReplayCache getReplayCache(
        SoapMessage message, String booleanKey, String instanceKey
    ) throws WSSecurityException {
        boolean specified = false;
        Object o = message.getContextualProperty(booleanKey);
        if (o != null) {
            if (!PropertyUtils.isTrue(o)) {
                return null;
            }
            specified = true;
        }

        if (!specified && MessageUtils.isRequestor(message)) {
            return null;
        }
        
        ReplayCache replayCache = (ReplayCache)message.getContextualProperty(instanceKey);
        Endpoint ep = message.getExchange().getEndpoint();
        if (replayCache == null && ep != null && ep.getEndpointInfo() != null) {
            EndpointInfo info = ep.getEndpointInfo();
            synchronized (info) {
                replayCache = (ReplayCache)info.getProperty(instanceKey);

                if (replayCache == null) {
                    String cacheKey = instanceKey;
                    if (info.getName() != null) {
                        int hashcode = info.getName().toString().hashCode();
                        if (hashcode < 0) {
                            cacheKey += hashcode;
                        } else {
                            cacheKey += "-" + hashcode;
                        }
                    }
                    if (WSS4JCacheUtil.isEhCacheInstalled()) {
                        Bus bus = message.getExchange().getBus();
                        Path diskstoreParent = null;
                        try {
                            diskstoreParent = Files.createTempDirectory("cxf");
                        } catch (IOException ex) {
                            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
                        }
                        replayCache = new CXFEHCacheReplayCache(cacheKey, bus, diskstoreParent);
                    } else {
                        replayCache = new MemoryReplayCache();
                    }

                    info.setProperty(instanceKey, replayCache);
                }
            }
        }
        return replayCache;
    }

    public static String parseAndStoreStreamingSecurityToken(
        org.apache.xml.security.stax.securityToken.SecurityToken securityToken,
        Message message
    ) throws XMLSecurityException, TokenStoreException {
        if (securityToken == null) {
            return null;
        }
        SecurityToken existingToken = TokenStoreUtils.getTokenStore(message).getToken(securityToken.getId());
        if (existingToken == null || existingToken.isExpired()) {
            Instant created = Instant.now();
            Instant expires = created.plusSeconds(WSS4JUtils.getSecurityTokenLifetime(message) / 1000L);

            SecurityToken cachedTok =
                new SecurityToken(securityToken.getId(), created, expires);
            cachedTok.setSHA1(securityToken.getSha1Identifier());

            if (securityToken.getTokenType() != null) {
                if (securityToken.getTokenType() == WSSecurityTokenConstants.EncryptedKeyToken) {
                    cachedTok.setTokenType(WSSConstants.NS_WSS_ENC_KEY_VALUE_TYPE);
                } else if (securityToken.getTokenType() == WSSecurityTokenConstants.KERBEROS_TOKEN) {
                    cachedTok.setTokenType(WSSConstants.NS_GSS_KERBEROS5_AP_REQ);
                } else if (securityToken.getTokenType() == WSSecurityTokenConstants.SAML_11_TOKEN) {
                    cachedTok.setTokenType(WSSConstants.NS_SAML11_TOKEN_PROFILE_TYPE);
                } else if (securityToken.getTokenType() == WSSecurityTokenConstants.SAML_20_TOKEN) {
                    cachedTok.setTokenType(WSSConstants.NS_SAML20_TOKEN_PROFILE_TYPE);
                } else if (securityToken.getTokenType() == WSSecurityTokenConstants.SECURE_CONVERSATION_TOKEN
                    || securityToken.getTokenType() == WSSecurityTokenConstants.SECURITY_CONTEXT_TOKEN) {
                    cachedTok.setTokenType(WSSConstants.NS_WSC_05_02);
                }
            }

            for (Map.Entry<String, Key> entry : securityToken.getSecretKey().entrySet()) {
                if (entry.getValue() != null) {
                    cachedTok.setKey(entry.getValue());
                    if (entry.getValue() instanceof SecretKey) {
                        cachedTok.setSecret(entry.getValue().getEncoded());
                    }
                    break;
                }
            }

            TokenStoreUtils.getTokenStore(message).add(cachedTok);

            return cachedTok.getId();
        }
        return existingToken.getId();

    }

    /**
     * Create a SoapFault from a WSSecurityException, following the SOAP Message Security
     * 1.1 specification, chapter 12 "Error Handling".
     *
     * When the Soap version is 1.1 then set the Fault/Code/Value from the fault code
     * specified in the WSSecurityException (if it exists).
     *
     * Otherwise set the Fault/Code/Value to env:Sender and the Fault/Code/Subcode/Value
     * as the fault code from the WSSecurityException.
     */
    public static SoapFault createSoapFault(
        SoapMessage message, SoapVersion version, WSSecurityException e
    ) {
        SoapFault fault;

        String errorMessage = null;
        javax.xml.namespace.QName faultCode = null;

        boolean returnSecurityError =
            MessageUtils.getContextualBoolean(message, SecurityConstants.RETURN_SECURITY_ERROR, false);
        if (returnSecurityError || MessageUtils.isRequestor(message)) {
            errorMessage = e.getMessage();
            faultCode = e.getFaultCode();
        } else {
            errorMessage = e.getSafeExceptionMessage();
            faultCode = e.getSafeFaultCode();
        }

        if (version.getVersion() == 1.1 && faultCode != null) {
            fault = new SoapFault(errorMessage, e, faultCode);
        } else {
            fault = new SoapFault(errorMessage, e, version.getSender());
            if (version.getVersion() != 1.1 && faultCode != null) {
                fault.setSubCode(faultCode);
            }
        }
        return fault;
    }

    public static Properties getProps(Object o, URL propsURL) {
        Properties properties = null;
        if (o instanceof Properties) {
            properties = (Properties)o;
        } else if (propsURL != null) {
            try {
                properties = new Properties();
                try (InputStream ins = propsURL.openStream()) {
                    properties.load(ins);
                }
            } catch (IOException e) {
                properties = null;
            }
        }

        return properties;
    }

    public static PasswordEncryptor getPasswordEncryptor(Message message) {
        if (message == null) {
            return null;
        }
        PasswordEncryptor passwordEncryptor =
            (PasswordEncryptor)message.getContextualProperty(
                SecurityConstants.PASSWORD_ENCRYPTOR_INSTANCE
            );
        if (passwordEncryptor != null) {
            return passwordEncryptor;
        }

        Object o = SecurityUtils.getSecurityPropertyValue(SecurityConstants.CALLBACK_HANDLER, message);
        try {
            CallbackHandler callbackHandler = SecurityUtils.getCallbackHandler(o);
            if (callbackHandler != null) {
                return new JasyptPasswordEncryptor(callbackHandler);
            }
        } catch (Exception ex) {
            return null;
        }

        return null;
    }

    public static Crypto loadCryptoFromPropertiesFile(
        Message message,
        String propFilename,
        ClassLoader classLoader,
        PasswordEncryptor passwordEncryptor
    ) throws WSSecurityException {
        try {
            URL url = SecurityUtils.loadResource(message, propFilename);
            if (url != null) {
                Properties props = new Properties();
                try (InputStream in = url.openStream()) {
                    props.load(in);
                }
                return CryptoFactory.getInstance(props, classLoader, passwordEncryptor);
            }
        } catch (Exception e) {
            //ignore
        }
        return CryptoFactory.getInstance(propFilename, classLoader);
    }

    public static Crypto getEncryptionCrypto(
        Object e,
        SoapMessage message,
        PasswordEncryptor passwordEncryptor
    ) throws WSSecurityException {
        Crypto encrCrypto = null;
        if (e instanceof Crypto) {
            encrCrypto = (Crypto)e;
        } else if (e != null) {
            URL propsURL = SecurityUtils.loadResource(message, e);
            Properties props = WSS4JUtils.getProps(e, propsURL);
            if (props == null) {
                LOG.fine("Cannot find Crypto Encryption properties: " + e);
                Exception ex = new Exception("Cannot find Crypto Encryption properties: " + e);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
            }

            encrCrypto = CryptoFactory.getInstance(props, Loader.getClassLoader(CryptoFactory.class),
                                                   passwordEncryptor);

            EndpointInfo info = message.getExchange().getEndpoint().getEndpointInfo();
            synchronized (info) {
                info.setProperty(SecurityConstants.ENCRYPT_CRYPTO, encrCrypto);
            }
        }
        return encrCrypto;
    }

    public static Crypto getSignatureCrypto(
        Object s,
        SoapMessage message,
        PasswordEncryptor passwordEncryptor
    ) throws WSSecurityException {
        Crypto signCrypto = null;
        if (s instanceof Crypto) {
            signCrypto = (Crypto)s;
        } else if (s != null) {
            URL propsURL = SecurityUtils.loadResource(message, s);
            Properties props = WSS4JUtils.getProps(s, propsURL);
            if (props == null) {
                //Liberty code change start
                //LOG.fine("Cannot find Crypto Signature properties: " + s);
                //Liberty code change end
                Exception ex = new Exception("Cannot find Crypto Signature properties: " + s);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
            }
            signCrypto = CryptoFactory.getInstance(props, Loader.getClassLoader(CryptoFactory.class),
                                                   passwordEncryptor);

            EndpointInfo info = message.getExchange().getEndpoint().getEndpointInfo();
            synchronized (info) {
                info.setProperty(SecurityConstants.SIGNATURE_CRYPTO, signCrypto);
            }
        }
        return signCrypto;
    }

    /**
     * Get the certificate that was used to sign the request
     */
    public static X509Certificate getReqSigCert(List<WSHandlerResult> results) {
        if (results == null || results.isEmpty()) {
            return null;
        }

        for (WSHandlerResult rResult : results) {
            List<WSSecurityEngineResult> signedResults =
                rResult.getActionResults().get(WSConstants.SIGN);

            if (signedResults != null && !signedResults.isEmpty()) {
                for (WSSecurityEngineResult signedResult : signedResults) {
                    if (signedResult.containsKey(WSSecurityEngineResult.TAG_X509_CERTIFICATE)) {
                        return (X509Certificate)signedResult.get(
                            WSSecurityEngineResult.TAG_X509_CERTIFICATE);
                    }
                }
            }
        }

        return null;
    }
}
