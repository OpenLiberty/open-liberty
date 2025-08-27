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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.SoapActionInInterceptor;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.EffectivePolicy;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.policy.custom.DefaultAlgorithmSuiteLoader;
import org.apache.wss4j.common.WSSPolicyException;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractBinding;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.stax.OperationPolicy;
import org.apache.wss4j.policy.stax.enforcer.PolicyEnforcer;
import org.apache.wss4j.policy.stax.enforcer.PolicyInputProcessor;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.impl.securityToken.HttpsSecurityTokenImpl;
import org.apache.wss4j.stax.securityEvent.HttpsTokenSecurityEvent;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventListener;

/**
 *
 */
public class PolicyBasedWSS4JStaxInInterceptor extends WSS4JStaxInInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(PolicyBasedWSS4JStaxInInterceptor.class);

    public void handleMessage(SoapMessage msg) throws Fault {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        boolean enableStax =
            MessageUtils.getContextualBoolean(msg, SecurityConstants.ENABLE_STREAMING_SECURITY);
        if (aim != null && enableStax) {
            super.handleMessage(msg);
        }
    }

    @Override
    protected WSSSecurityProperties createSecurityProperties() {
        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setSkipDocumentEvents(true);
        return securityProperties;
    }

    private void checkAsymmetricBinding(
        AssertionInfoMap aim, SoapMessage message, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        AssertionInfo ais = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.ASYMMETRIC_BINDING);
        if (ais == null) {
            return;
        }

        Object s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_CRYPTO, message);
        if (s == null) {
            s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PROPERTIES, message);
        }
        Object e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_CRYPTO, message);
        if (e == null) {
            e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_PROPERTIES, message);
        }

        Crypto encrCrypto = getEncryptionCrypto(e, message, securityProperties);
        final Crypto signCrypto;
        if (e != null && e.equals(s)) {
            signCrypto = encrCrypto;
        } else {
            signCrypto = getSignatureCrypto(s, message, securityProperties);
        }

        if (signCrypto != null) {
            securityProperties.setDecryptionCrypto(signCrypto);
        }

        if (encrCrypto != null) {
            securityProperties.setSignatureVerificationCrypto(encrCrypto);
        } else if (signCrypto != null) {
            securityProperties.setSignatureVerificationCrypto(signCrypto);
        }
    }

    private void checkTransportBinding(
        AssertionInfoMap aim, SoapMessage message, WSSSecurityProperties securityProperties
    ) throws XMLSecurityException {
        boolean transportPolicyInEffect =
            PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.TRANSPORT_BINDING) != null;
        if (!transportPolicyInEffect
            && !(PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.SYMMETRIC_BINDING) == null
                && PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.ASYMMETRIC_BINDING) == null)) {
            return;
        }

        // Add a HttpsSecurityEvent so the policy verification code knows TLS is in use
        if (isRequestor(message)) {
            HttpsTokenSecurityEvent httpsTokenSecurityEvent = new HttpsTokenSecurityEvent();
            httpsTokenSecurityEvent.setAuthenticationType(
                HttpsTokenSecurityEvent.AuthenticationType.HttpsNoAuthentication
            );
            HttpsSecurityTokenImpl httpsSecurityToken = new HttpsSecurityTokenImpl();
            try {
                httpsSecurityToken.addTokenUsage(WSSecurityTokenConstants.TOKENUSAGE_MAIN_SIGNATURE);
            } catch (XMLSecurityException e) {
                LOG.fine(e.getMessage());
            }
            httpsTokenSecurityEvent.setSecurityToken(httpsSecurityToken);

            List<SecurityEvent> securityEvents = getSecurityEventList(message);
            securityEvents.add(httpsTokenSecurityEvent);
        }

        Object s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_CRYPTO, message);
        if (s == null) {
            s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PROPERTIES, message);
        }
        Object e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_CRYPTO, message);
        if (e == null) {
            e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_PROPERTIES, message);
        }

        Crypto encrCrypto = getEncryptionCrypto(e, message, securityProperties);
        final Crypto signCrypto;
        if (e != null && e.equals(s)) {
            signCrypto = encrCrypto;
        } else {
            signCrypto = getSignatureCrypto(s, message, securityProperties);
        }

        if (signCrypto != null) {
            securityProperties.setDecryptionCrypto(signCrypto);
        }

        if (encrCrypto != null) {
            securityProperties.setSignatureVerificationCrypto(encrCrypto);
        } else if (signCrypto != null) {
            securityProperties.setSignatureVerificationCrypto(signCrypto);
        }
    }

    private List<SecurityEvent> getSecurityEventList(Message message) {
        @SuppressWarnings("unchecked")
        List<SecurityEvent> securityEvents =
            (List<SecurityEvent>) message.getExchange().get(SecurityEvent.class.getName() + ".out");
        if (securityEvents == null) {
            securityEvents = new ArrayList<>();
            message.getExchange().put(SecurityEvent.class.getName() + ".out", securityEvents);
        }

        return securityEvents;
    }

    private void checkSymmetricBinding(
        AssertionInfoMap aim, SoapMessage message, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        AssertionInfo ais = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
        if (ais == null) {
            return;
        }

        Object s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_CRYPTO, message);
        if (s == null) {
            s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PROPERTIES, message);
        }
        Object e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_CRYPTO, message);
        if (e == null) {
            e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_PROPERTIES, message);
        }

        Crypto encrCrypto = getEncryptionCrypto(e, message, securityProperties);
        final Crypto signCrypto;
        if (e != null && e.equals(s)) {
            signCrypto = encrCrypto;
        } else {
            signCrypto = getSignatureCrypto(s, message, securityProperties);
        }

        if (isRequestor(message)) {
            Crypto crypto = encrCrypto;
            if (crypto == null) {
                crypto = signCrypto;
            }
            if (crypto != null) {
                securityProperties.setSignatureCrypto(crypto);
            }

            crypto = signCrypto;
            if (crypto == null) {
                crypto = encrCrypto;
            }
            if (crypto != null) {
                securityProperties.setDecryptionCrypto(crypto);
            }
        } else {
            Crypto crypto = signCrypto;
            if (crypto == null) {
                crypto = encrCrypto;
            }
            if (crypto != null) {
                securityProperties.setSignatureVerificationCrypto(crypto);
            }

            crypto = encrCrypto;
            if (crypto == null) {
                crypto = signCrypto;
            }
            if (crypto != null) {
                securityProperties.setDecryptionCrypto(crypto);
            }
        }
    }

    @Override
    protected void configureProperties(
        SoapMessage msg, WSSSecurityProperties securityProperties
    ) throws XMLSecurityException {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        checkAsymmetricBinding(aim, msg, securityProperties);
        checkSymmetricBinding(aim, msg, securityProperties);
        checkTransportBinding(aim, msg, securityProperties);

        AssertionInfo assertionInfo = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.ASYMMETRIC_BINDING);
        AbstractBinding abstractBinding = null;
        if (assertionInfo == null) {
            assertionInfo = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
            if (assertionInfo == null) {
                assertionInfo = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.TRANSPORT_BINDING);
            }
        }
        if (assertionInfo != null) {
            abstractBinding = (AbstractBinding)assertionInfo.getAssertion();
        }
        AlgorithmSuite.AlgorithmSuiteType originalAlgSuite = null;
        if (abstractBinding != null) {
            originalAlgSuite = abstractBinding.getAlgorithmSuite().getAlgorithmSuiteType();
        }

        AlgorithmSuite.AlgorithmSuiteType customAlgSuite = DefaultAlgorithmSuiteLoader
                .customize(originalAlgSuite, msg);

        // Allow for setting non-standard signature algorithms
        String asymSignatureAlgorithm =
                msg.getContextualProperty(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM) != null
                        ? (String)msg.getContextualProperty(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM)
                        : (customAlgSuite != null ? customAlgSuite.getAsymmetricSignature() : null);
        String symSignatureAlgorithm =
                msg.getContextualProperty(SecurityConstants.SYMMETRIC_SIGNATURE_ALGORITHM) != null
                        ? (String)msg.getContextualProperty(SecurityConstants.SYMMETRIC_SIGNATURE_ALGORITHM)
                        : (customAlgSuite != null ? customAlgSuite.getSymmetricSignature() : null);
        if (asymSignatureAlgorithm != null || symSignatureAlgorithm != null) {
            Collection<AssertionInfo> algorithmSuites =
                aim.get(SP12Constants.ALGORITHM_SUITE);
            if (algorithmSuites != null && !algorithmSuites.isEmpty()) {
                for (AssertionInfo algorithmSuite : algorithmSuites) {
                    AlgorithmSuite algSuite = (AlgorithmSuite)algorithmSuite.getAssertion();
                    //customize Alg suite (if possible)
                    DefaultAlgorithmSuiteLoader.customize(algSuite.getAlgorithmSuiteType(), msg);

                    if (asymSignatureAlgorithm != null) {
                        algSuite.getAlgorithmSuiteType().setAsymmetricSignature(asymSignatureAlgorithm);
                    }
                    if (symSignatureAlgorithm != null) {
                        algSuite.getAlgorithmSuiteType().setSymmetricSignature(symSignatureAlgorithm);
                    }
                }
            }
        }

        super.configureProperties(msg, securityProperties);
    }

    /**
     * Is a Nonce Cache required, i.e. are we expecting a UsernameToken
     */
    @Override
    protected boolean isNonceCacheRequired(SoapMessage msg, WSSSecurityProperties securityProperties) {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        if (aim != null) {
            AssertionInfo ais = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.USERNAME_TOKEN);
            if (ais != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Is a Timestamp cache required, i.e. are we expecting a Timestamp
     */
    @Override
    protected boolean isTimestampCacheRequired(SoapMessage msg, WSSSecurityProperties securityProperties) {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        if (aim != null) {
            AssertionInfo ais = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.INCLUDE_TIMESTAMP);
            if (ais != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Is a SAML Cache required, i.e. are we expecting a SAML Token
     */
    @Override
    protected boolean isSamlCacheRequired(SoapMessage msg, WSSSecurityProperties securityProperties) {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        if (aim != null) {
            AssertionInfo ais = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.SAML_TOKEN);
            if (ais != null) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected List<SecurityEventListener> configureSecurityEventListeners(
        SoapMessage msg, WSSSecurityProperties securityProperties
    ) throws WSSPolicyException {
        final List<SecurityEventListener> securityEventListeners = new ArrayList<>(2);
        securityEventListeners.addAll(super.configureSecurityEventListeners(msg, securityProperties));

        Endpoint endoint = msg.getExchange().getEndpoint();

        PolicyEnforcer policyEnforcer = createPolicyEnforcer(endoint.getEndpointInfo(), msg);
        securityProperties.addInputProcessor(new PolicyInputProcessor(policyEnforcer, securityProperties));
        securityEventListeners.add(policyEnforcer);

        return securityEventListeners;
    }

    private PolicyEnforcer createPolicyEnforcer(
        EndpointInfo endpointInfo, SoapMessage msg
    ) throws WSSPolicyException {
        EffectivePolicy dispatchPolicy = null;
        List<OperationPolicy> operationPolicies = new ArrayList<>();
        Collection<BindingOperationInfo> bindingOperationInfos = endpointInfo.getBinding().getOperations();
        for (Iterator<BindingOperationInfo> bindingOperationInfoIterator =
                     bindingOperationInfos.iterator(); bindingOperationInfoIterator.hasNext();) {
            BindingOperationInfo bindingOperationInfo = bindingOperationInfoIterator.next();
            QName operationName = bindingOperationInfo.getName();

            // todo: I'm not sure what the effectivePolicy exactly contains,
            // a) only the operation policy,
            // or b) all policies for the service,
            // or c) all policies which applies for the current operation.
            // c) is that what we need for stax.
            EffectivePolicy policy =
                (EffectivePolicy)bindingOperationInfo.getProperty("policy-engine-info-serve-request");
            //PolicyEngineImpl.POLICY_INFO_REQUEST_SERVER);
            if (MessageUtils.isRequestor(msg)) {
                policy =
                    (EffectivePolicy)bindingOperationInfo.getProperty("policy-engine-info-client-response");
                // Save the Dispatch Policy as it may be used on another BindingOperationInfo
                if (policy != null
                    && "http://cxf.apache.org/jaxws/dispatch".equals(operationName.getNamespaceURI())) {
                    dispatchPolicy = policy;
                }
                if (bindingOperationInfo.getOutput() != null) {
                    MessageInfo messageInfo = bindingOperationInfo.getOutput().getMessageInfo();
                    operationName = messageInfo.getName();
                    if (messageInfo.getMessagePartsNumber() > 0) {
                        QName cn = messageInfo.getFirstMessagePart().getConcreteName();
                        if (cn != null) {
                            operationName = cn;
                        }
                    }
                }
            } else {
                if (bindingOperationInfo.getInput() != null) {
                    MessageInfo messageInfo = bindingOperationInfo.getInput().getMessageInfo();
                    operationName = messageInfo.getName();
                    if (messageInfo.getMessagePartsNumber() > 0) {
                        QName cn = messageInfo.getFirstMessagePart().getConcreteName();
                        if (cn != null) {
                            operationName = cn;
                        }
                    }
                }
            }

            SoapOperationInfo soapOperationInfo = bindingOperationInfo.getExtensor(SoapOperationInfo.class);
            if (soapOperationInfo != null && policy == null && dispatchPolicy != null) {
                policy = dispatchPolicy;
            }

            if (policy != null && soapOperationInfo != null) {
                String soapNS;
                BindingInfo bindingInfo = bindingOperationInfo.getBinding();
                if (bindingInfo instanceof SoapBindingInfo) {
                    soapNS = ((SoapBindingInfo)bindingInfo).getSoapVersion().getNamespace();
                } else {
                    //no idea what todo here...
                    //most probably throw an exception:
                    throw new IllegalArgumentException("BindingInfo is not an instance of SoapBindingInfo");
                }

                OperationPolicy operationPolicy = new OperationPolicy(operationName);
                operationPolicy.setPolicy(policy.getPolicy());
                operationPolicy.setOperationAction(soapOperationInfo.getAction());
                operationPolicy.setSoapMessageVersionNamespace(soapNS);

                operationPolicies.add(operationPolicy);
            }
        }

        String soapAction = SoapActionInInterceptor.getSoapAction(msg);
        if (soapAction == null) {
            soapAction = "";
        }
        String actor = (String)msg.getContextualProperty(SecurityConstants.ACTOR);
        final Collection<org.apache.cxf.message.Attachment> attachments = msg.getAttachments();
        int attachmentCount = 0;
        if (attachments != null && !attachments.isEmpty()) {
            attachmentCount = attachments.size();
        }
        return new PolicyEnforcer(operationPolicies, soapAction, isRequestor(msg),
                                  actor, attachmentCount,
                                  new WSS4JPolicyAsserter(msg.get(AssertionInfoMap.class)),
                                  WSSConstants.NS_SOAP12.equals(msg.getVersion().getNamespace()));
    }

}
