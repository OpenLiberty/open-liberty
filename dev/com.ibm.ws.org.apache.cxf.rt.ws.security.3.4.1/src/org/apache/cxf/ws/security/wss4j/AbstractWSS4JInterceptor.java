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

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandler;
//No Liberty Change, but needed to recompile due to Liberty change in MessageImpl.
public abstract class AbstractWSS4JInterceptor extends WSHandler implements SoapInterceptor,
    PhaseInterceptor<SoapMessage> {

    private static final Set<QName> HEADERS = new HashSet<>();

    static {
        HEADERS.add(new QName(WSS4JConstants.WSSE_NS, "Security"));
        HEADERS.add(new QName(WSS4JConstants.ENC_NS, "EncryptedData"));
        HEADERS.add(new QName(WSS4JConstants.WSSE11_NS, "EncryptedHeader"));
    }

    private Map<String, Object> properties = new ConcurrentHashMap<>();
    private final Set<String> before = new HashSet<>();
    private final Set<String> after = new HashSet<>();
    private String phase;
    private String id;

    public AbstractWSS4JInterceptor() {
        super();
        id = getClass().getName();
    }

    public Set<URI> getRoles() {
        return null;
    }

    public void handleFault(SoapMessage message) {
    }

    public void postHandleMessage(SoapMessage message) throws Fault {
    }
    public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
        return null;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public Object getOption(String key) {
        return properties.get(key);
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public String getPassword(Object msgContext) {
        return (String)((Message)msgContext).getContextualProperty("password");
    }

    public Object getProperty(Object msgContext, String key) {
        if (msgContext == null) {
            return null;
        }

        Object obj = SecurityUtils.getSecurityPropertyValue(key, (Message)msgContext);
        if (obj == null) {
            obj = getOption(key);
        }
        return obj;
    }

    public void setPassword(Object msgContext, String password) {
        ((Message)msgContext).put("password", password);
    }

    public void setProperty(Object msgContext, String key, Object value) {
        ((Message)msgContext).put(key, value);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<QName> getUnderstoodHeaders() {
        return HEADERS;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Set<String> getAfter() {
        return after;
    }

    public Set<String> getBefore() {
        return before;
    }

    protected boolean isRequestor(SoapMessage message) {
        return MessageUtils.isRequestor(message);
    }

    protected void translateProperties(SoapMessage msg) {
        String bspCompliant = (String)msg.getContextualProperty(SecurityConstants.IS_BSP_COMPLIANT);
        if (bspCompliant != null) {
            msg.put(ConfigurationConstants.IS_BSP_COMPLIANT, bspCompliant);
        }
        String futureTTL =
            (String)msg.getContextualProperty(SecurityConstants.TIMESTAMP_FUTURE_TTL);
        if (futureTTL != null) {
            msg.put(ConfigurationConstants.TTL_FUTURE_TIMESTAMP, futureTTL);
        }
        String ttl =
                (String)msg.getContextualProperty(SecurityConstants.TIMESTAMP_TTL);
        if (ttl != null) {
            msg.put(ConfigurationConstants.TTL_TIMESTAMP, ttl);
        }

        String utFutureTTL =
            (String)msg.getContextualProperty(SecurityConstants.USERNAMETOKEN_FUTURE_TTL);
        if (utFutureTTL != null) {
            msg.put(ConfigurationConstants.TTL_FUTURE_USERNAMETOKEN, utFutureTTL);
        }
        String utTTL =
            (String)msg.getContextualProperty(SecurityConstants.USERNAMETOKEN_TTL);
        if (utTTL != null) {
            msg.put(ConfigurationConstants.TTL_USERNAMETOKEN, utTTL);
        }

        String certConstraints =
            (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SUBJECT_CERT_CONSTRAINTS, msg);
        if (certConstraints != null) {
            msg.put(ConfigurationConstants.SIG_SUBJECT_CERT_CONSTRAINTS, certConstraints);
        }

        String certConstraintsSeparator =
            (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.CERT_CONSTRAINTS_SEPARATOR, msg);
        if (certConstraintsSeparator != null && !certConstraintsSeparator.isEmpty()) {
            msg.put(ConfigurationConstants.SIG_CERT_CONSTRAINTS_SEPARATOR, certConstraintsSeparator);
        }

        // Now set SAML SenderVouches + Holder Of Key requirements
        String valSAMLSubjectConf =
            (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION,
                                                           msg);
        boolean validateSAMLSubjectConf = true;
        if (valSAMLSubjectConf != null) {
            validateSAMLSubjectConf = Boolean.parseBoolean(valSAMLSubjectConf);
        }
        msg.put(
            ConfigurationConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION,
            Boolean.toString(validateSAMLSubjectConf)
        );

        PasswordEncryptor passwordEncryptor =
            (PasswordEncryptor)msg.getContextualProperty(SecurityConstants.PASSWORD_ENCRYPTOR_INSTANCE);
        if (passwordEncryptor != null) {
            msg.put(ConfigurationConstants.PASSWORD_ENCRYPTOR_INSTANCE, passwordEncryptor);
        }
    }

    @Override
    protected Crypto loadCryptoFromPropertiesFile(
        String propFilename,
        RequestData reqData
    ) throws WSSecurityException {
        Message message = (Message)reqData.getMsgContext();
        ClassLoader classLoader = this.getClassLoader(reqData.getMsgContext());
        PasswordEncryptor passwordEncryptor = getPasswordEncryptor(reqData);
        return
            WSS4JUtils.loadCryptoFromPropertiesFile(
                message, propFilename, classLoader, passwordEncryptor
            );
    }

}
