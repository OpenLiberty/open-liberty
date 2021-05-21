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

import java.security.Provider;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.wss4j.policyhandlers.AsymmetricBindingHandler;
import org.apache.cxf.ws.security.wss4j.policyhandlers.SymmetricBindingHandler;
import org.apache.cxf.ws.security.wss4j.policyhandlers.TransportBindingHandler;
import org.apache.neethi.Policy;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.crypto.ThreadLocalSecurityProvider;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.policy.model.AbstractBinding;
import org.apache.wss4j.policy.model.AsymmetricBinding;
import org.apache.wss4j.policy.model.SymmetricBinding;
import org.apache.wss4j.policy.model.TransportBinding;
//No Liberty Change, but needed to recompile due to Liberty change in MessageImpl.
public class PolicyBasedWSS4JOutInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
    public static final String SECURITY_PROCESSED = PolicyBasedWSS4JOutInterceptor.class.getName() + ".DONE";
    public static final PolicyBasedWSS4JOutInterceptor INSTANCE = new PolicyBasedWSS4JOutInterceptor();

    private static final Logger LOG = LogUtils.getL7dLogger(PolicyBasedWSS4JOutInterceptor.class);


    private PolicyBasedWSS4JOutInterceptorInternal ending;
    private SAAJOutInterceptor saajOut = new SAAJOutInterceptor();

    public PolicyBasedWSS4JOutInterceptor() {
        super(Phase.PRE_PROTOCOL);
        getAfter().add(SAAJOutInterceptor.class.getName());
        ending = createEndingInterceptor();
    }


    public void handleMessage(SoapMessage mc) throws Fault {
        boolean enableStax =
            MessageUtils.getContextualBoolean(mc, SecurityConstants.ENABLE_STREAMING_SECURITY);
        if (!enableStax) {
            if (mc.getContent(SOAPMessage.class) == null) {
                saajOut.handleMessage(mc);
            }
            mc.put(SECURITY_PROCESSED, Boolean.TRUE);
            mc.getInterceptorChain().add(ending);
        }
    }
    public void handleFault(SoapMessage message) {
        saajOut.handleFault(message);
    }

    public final PolicyBasedWSS4JOutInterceptorInternal createEndingInterceptor() {
        return new PolicyBasedWSS4JOutInterceptorInternal();
    }

    public final class PolicyBasedWSS4JOutInterceptorInternal
        implements PhaseInterceptor<SoapMessage> {
        public PolicyBasedWSS4JOutInterceptorInternal() {
            super();
        }

        public void handleMessage(SoapMessage message) throws Fault {
            Object provider = message.getExchange().get(Provider.class);
            final boolean useCustomProvider = provider != null && ThreadLocalSecurityProvider.isInstalled();
            try {
                if (useCustomProvider) {
                    ThreadLocalSecurityProvider.setProvider((Provider)provider);
                }
                handleMessageInternal(message);
            } finally {
                if (useCustomProvider) {
                    ThreadLocalSecurityProvider.unsetProvider();
                }
            }
        }

        private void handleMessageInternal(SoapMessage message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            if (aim == null) {
                // no policies available
                return;
            }
            SOAPMessage saaj = message.getContent(SOAPMessage.class);

            boolean mustUnderstand =
                MessageUtils.getContextualBoolean(
                    message, SecurityConstants.MUST_UNDERSTAND, true
                );
            String actor = (String)message.getContextualProperty(SecurityConstants.ACTOR);

            // extract Assertion information
            AbstractBinding binding = PolicyUtils.getSecurityBinding(aim);

            if (binding == null && isRequestor(message)) {
                Policy policy = new Policy();
                binding = new TransportBinding(org.apache.wss4j.policy.SPConstants.SPVersion.SP11,
                                                 policy);
            }

            if (binding != null) {
                WSSecHeader secHeader = new WSSecHeader(actor, mustUnderstand, saaj.getSOAPPart());
                Element el = null;
                try {
                    el = secHeader.insertSecurityHeader();
                } catch (WSSecurityException e) {
                    throw new SoapFault(
                        new Message("SECURITY_FAILED", LOG), e, message.getVersion().getSender()
                    );
                }
                try {
                    //move to end
                    SAAJUtils.getHeader(saaj).removeChild(el);
                    SAAJUtils.getHeader(saaj).appendChild(el);
                } catch (SOAPException e) {
                    //ignore
                }

                WSSConfig config = (WSSConfig)message.getContextualProperty(WSSConfig.class.getName());
                if (config == null) {
                    config = WSSConfig.getNewInstance();
                }
                translateProperties(message);

                String asymSignatureAlgorithm =
                    (String)message.getContextualProperty(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM);
                if (asymSignatureAlgorithm != null && binding.getAlgorithmSuite() != null) {
                    binding.getAlgorithmSuite().getAlgorithmSuiteType().setAsymmetricSignature(asymSignatureAlgorithm);
                }

                String symSignatureAlgorithm =
                    (String)message.getContextualProperty(SecurityConstants.SYMMETRIC_SIGNATURE_ALGORITHM);
                if (symSignatureAlgorithm != null && binding.getAlgorithmSuite() != null) {
                    binding.getAlgorithmSuite().getAlgorithmSuiteType().setSymmetricSignature(symSignatureAlgorithm);
                }

                try {
                    if (binding instanceof TransportBinding) {
                        new TransportBindingHandler(config, (TransportBinding)binding, saaj,
                                                    secHeader, aim, message).handleBinding();
                    } else if (binding instanceof SymmetricBinding) {
                        new SymmetricBindingHandler(config, (SymmetricBinding)binding, saaj,
                                                    secHeader, aim, message).handleBinding();
                    } else {
                        new AsymmetricBindingHandler(config, (AsymmetricBinding)binding, saaj,
                                                     secHeader, aim, message).handleBinding();
                    }
                } catch (SOAPException | TokenStoreException e) {
                    throw new SoapFault(
                        new Message("SECURITY_FAILED", LOG), e, message.getVersion().getSender()
                    );
                }

                if (el.getFirstChild() == null) {
                    el.getParentNode().removeChild(el);
                }
            }

        }

        public Set<String> getAfter() {
            return Collections.emptySet();
        }

        public Set<String> getBefore() {
            return Collections.emptySet();
        }

        public String getId() {
            return PolicyBasedWSS4JOutInterceptorInternal.class.getName();
        }

        public String getPhase() {
            return Phase.POST_PROTOCOL;
        }

        public void handleFault(SoapMessage message) {
            //nothing
        }

        public Collection<PhaseInterceptor<? extends org.apache.cxf.message.Message>>
        getAdditionalInterceptors() {

            return null;
        }

        private void translateProperties(SoapMessage msg) {
            String bspCompliant = (String)msg.getContextualProperty(SecurityConstants.IS_BSP_COMPLIANT);
            if (bspCompliant != null) {
                msg.put(ConfigurationConstants.IS_BSP_COMPLIANT, bspCompliant);
            }
        }
    }
}
