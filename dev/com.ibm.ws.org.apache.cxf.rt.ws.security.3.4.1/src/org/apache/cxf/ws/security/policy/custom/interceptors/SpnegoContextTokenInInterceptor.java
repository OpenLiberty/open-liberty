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

import java.time.Instant;
import java.util.Collection;

import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.SoapActionInInterceptor;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.policy.interceptors.HttpsTokenInterceptorProvider.HttpsTokenInInterceptor;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JUtils;
import org.apache.neethi.All;
import org.apache.neethi.Assertion;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.spnego.SpnegoTokenContext;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.message.token.SecurityContextToken;
import org.apache.wss4j.policy.SPConstants;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.utils.XMLUtils;

class SpnegoContextTokenInInterceptor extends AbstractPhaseInterceptor<SoapMessage> {

    SpnegoContextTokenInInterceptor() {
        super(Phase.PRE_STREAM);
        addBefore(WSS4JStaxInInterceptor.class.getName());
        addBefore(HttpsTokenInInterceptor.class.getName());
    }

    public void handleMessage(SoapMessage message) throws Fault {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        // extract Assertion information
        if (aim != null) {
            Collection<AssertionInfo> ais =
                PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SPNEGO_CONTEXT_TOKEN);
            if (ais.isEmpty()) {
                return;
            }
            if (isRequestor(message)) {
                //client side should be checked on the way out
                for (AssertionInfo ai : ais) {
                    ai.setAsserted(true);
                }
                return;
            }
            String s = (String)message.get(SoapBindingConstants.SOAP_ACTION);
            if (s == null) {
                s = SoapActionInInterceptor.getSoapAction(message);
            }
            AddressingProperties inProps = (AddressingProperties)message
                .getContextualProperty(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND);
            if (inProps != null && s == null) {
                //MS/WCF doesn't put a soap action out for this, must check the headers
                s = inProps.getAction().getValue();
            }

            if (s != null
                && s.contains("/RST/Issue")
                && (s.startsWith(STSUtils.WST_NS_05_02)
                    || s.startsWith(STSUtils.WST_NS_05_12))) {

                Policy p = new Policy();
                ExactlyOne ea = new ExactlyOne();
                p.addPolicyComponent(ea);
                All all = new All();
                Assertion ass = NegotiationUtils.getAddressingPolicy(aim, false);
                all.addPolicyComponent(ass);
                ea.addPolicyComponent(all);

                //setup endpoint and forward to it.
                unmapSecurityProps(message);
                String ns = STSUtils.WST_NS_05_12;
                if (s.startsWith(STSUtils.WST_NS_05_02)) {
                    ns = STSUtils.WST_NS_05_02;
                }
                NegotiationUtils.recalcEffectivePolicy(message, ns, p, new SpnegoSTSInvoker(), false);
            } else {
                message.getInterceptorChain().add(SpnegoContextTokenFinderInterceptor.INSTANCE);
            }
        }
    }

    private void unmapSecurityProps(Message message) {
        Exchange ex = message.getExchange();
        for (String s : SecurityConstants.ALL_PROPERTIES) {
            Object v = message.getContextualProperty(s);
            if (v != null) {
                ex.put(s, v);
            }
        }
    }

    public class SpnegoSTSInvoker extends STSInvoker {

        void doIssue(
            Element requestEl,
            Exchange exchange,
            Element binaryExchange,
            W3CDOMStreamWriter writer,
            String prefix,
            String namespace
        ) throws Exception {

            SpnegoTokenContext spnegoToken =
                handleBinaryExchange(binaryExchange, exchange.getInMessage(), namespace);

            writer.writeStartElement(prefix, "RequestSecurityTokenResponseCollection", namespace);
            writer.writeStartElement(prefix, "RequestSecurityTokenResponse", namespace);

            String context = requestEl.getAttributeNS(null, "Context");
            if (context != null && !"".equals(context)) {
                writer.writeAttribute("Context", context);
            }

            // Find TokenType and KeySize
            int keySize = 256;
            String tokenType = null;
            Element el = DOMUtils.getFirstElement(requestEl);
            while (el != null) {
                String localName = el.getLocalName();
                if (namespace.equals(el.getNamespaceURI())) {
                    if ("KeySize".equals(localName)) {
                        keySize = Integer.parseInt(el.getTextContent());
                    } else if ("TokenType".equals(localName)) {
                        tokenType = el.getTextContent();
                    }
                }

                el = DOMUtils.getNextElement(el);
            }

            // Check received KeySize
            if (keySize < 128 || keySize > 512) {
                keySize = 256;
            }

            // TokenType
            writer.writeStartElement(prefix, "TokenType", namespace);
            writer.writeCharacters(tokenType);
            writer.writeEndElement();

            writer.writeStartElement(prefix, "RequestedSecurityToken", namespace);

            // SecurityContextToken
            SecurityContextToken sct =
                new SecurityContextToken(
                    NegotiationUtils.getWSCVersion(tokenType), writer.getDocument()
                );
            WSSConfig wssConfig = WSSConfig.getNewInstance();
            sct.setID(wssConfig.getIdAllocator().createId("sctId-", sct));

            // Lifetime
            Instant created = Instant.now();
            Instant expires =
                created.plusSeconds(WSS4JUtils.getSecurityTokenLifetime(exchange.getOutMessage()) / 1000L);

            SecurityToken token = new SecurityToken(sct.getIdentifier(), created, expires);
            token.setToken(sct.getElement());
            token.setTokenType(sct.getTokenType());

            SecurityContext sc = exchange.getInMessage().get(SecurityContext.class);
            if (sc != null) {
                token.setSecurityContext(sc);
            }

            writer.getCurrentNode().appendChild(sct.getElement());
            writer.writeEndElement();

            // References
            writer.writeStartElement(prefix, "RequestedAttachedReference", namespace);
            token.setAttachedReference(
                writeSecurityTokenReference(writer, "#" + sct.getID(), tokenType)
            );
            writer.writeEndElement();

            writer.writeStartElement(prefix, "RequestedUnattachedReference", namespace);
            token.setUnattachedReference(
                writeSecurityTokenReference(writer, sct.getIdentifier(), tokenType)
            );
            writer.writeEndElement();

            writeLifetime(writer, created, expires, prefix, namespace);

            // KeySize
            writer.writeStartElement(prefix, "KeySize", namespace);
            writer.writeCharacters(Integer.toString(keySize));
            writer.writeEndElement();

            byte[] secret = XMLSecurityConstants.generateBytes(keySize / 8);
            byte[] key = spnegoToken.wrapKey(secret);

            writeProofToken(writer, prefix, namespace, key);

            writer.writeEndElement();

            /*
            // Second RequestSecurityTokenResponse containing the Authenticator
            // TODO
            writer.writeStartElement(prefix, "RequestSecurityTokenResponse", namespace);
            if (context != null && !"".equals(context)) {
                writer.writeAttribute("Context", context);
            }
            writeAuthenticator(writer, prefix, namespace, secret);
            writer.writeEndElement();
            */

            writer.writeEndElement();

            spnegoToken.clear();

            token.setSecret(secret);
            ((TokenStore)exchange.getEndpoint().getEndpointInfo()
                    .getProperty(TokenStore.class.getName())).add(token);
        }

        void doRenew(
                Element requestEl,
                Exchange exchange,
                SecurityToken securityToken,
                Element binaryExchange,
                W3CDOMStreamWriter writer,
                String prefix,
                String namespace
        ) {
            //Not implemented
        }

        private SpnegoTokenContext handleBinaryExchange(
            Element binaryExchange,
            Message message,
            String namespace
        ) throws Exception {
            if (binaryExchange == null) {
                throw new Exception("No BinaryExchange element received");
            }
            String encoding = binaryExchange.getAttributeNS(null, "EncodingType");
            if (!WSS4JConstants.BASE64_ENCODING.equals(encoding)) {
                throw new Exception("Unknown encoding type: " + encoding);
            }

            String valueType = binaryExchange.getAttributeNS(null, "ValueType");
            if (!(namespace + "/spnego").equals(valueType)) {
                throw new Exception("Unknown value type: " + valueType);
            }

            String content = DOMUtils.getContent(binaryExchange);
            byte[] decodedContent = XMLUtils.decode(content);

            String jaasContext =
                (String)message.getContextualProperty(SecurityConstants.KERBEROS_JAAS_CONTEXT_NAME);
            String kerberosSpn =
                (String)message.getContextualProperty(SecurityConstants.KERBEROS_SPN);
            CallbackHandler callbackHandler =
                SecurityUtils.getCallbackHandler(
                    SecurityUtils.getSecurityPropertyValue(SecurityConstants.CALLBACK_HANDLER, message)
                );

            SpnegoTokenContext spnegoToken = new SpnegoTokenContext();
            spnegoToken.validateServiceTicket(
                jaasContext, callbackHandler, kerberosSpn, decodedContent
            );
            return spnegoToken;
        }

        private void writeProofToken(
            W3CDOMStreamWriter writer,
            String prefix,
            String namespace,
            byte[] key
        ) throws Exception {
            // RequestedProofToken
            writer.writeStartElement(prefix, "RequestedProofToken", namespace);

            // EncryptedKey
            writer.writeStartElement(WSS4JConstants.ENC_PREFIX, "EncryptedKey", WSS4JConstants.ENC_NS);
            writer.writeStartElement(WSS4JConstants.ENC_PREFIX, "EncryptionMethod", WSS4JConstants.ENC_NS);
            writer.writeAttribute("Algorithm", namespace + "/spnego#GSS_Wrap");
            writer.writeEndElement();
            writer.writeStartElement(WSS4JConstants.ENC_PREFIX, "CipherData", WSS4JConstants.ENC_NS);
            writer.writeStartElement(WSS4JConstants.ENC_PREFIX, "CipherValue", WSS4JConstants.ENC_NS);

            writer.writeCharacters(XMLUtils.encodeToString(key));

            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();

            writer.writeEndElement();
        }

        /*
        private void writeAuthenticator(
            W3CDOMStreamWriter writer,
            String prefix,
            String namespace,
            byte[] secret
        ) throws Exception {
            // Authenticator
            writer.writeStartElement(prefix, "Authenticator", namespace);

            // CombinedHash
            writer.writeStartElement(prefix, "CombinedHash", namespace);

            P_SHA1 psha1 = new P_SHA1();
            byte[] seed = "AUTH-HASH".getBytes();
            byte[] digest = psha1.createKey(secret, seed, 0, 32);
            writer.writeCharacters(Base64.encode(digest));

            writer.writeEndElement();

            writer.writeEndElement();
        }
        */

    }


    static final class SpnegoContextTokenFinderInterceptor
        extends AbstractPhaseInterceptor<SoapMessage> {

        static final SpnegoContextTokenFinderInterceptor INSTANCE
            = new SpnegoContextTokenFinderInterceptor();

        private SpnegoContextTokenFinderInterceptor() {
            super(Phase.PRE_PROTOCOL);
            addAfter(WSS4JInInterceptor.class.getName());
        }

        public void handleMessage(SoapMessage message) throws Fault {
            try {
                boolean foundSCT = NegotiationUtils.parseSCTResult(message);

                AssertionInfoMap aim = message.get(AssertionInfoMap.class);
                // extract Assertion information
                if (aim != null) {
                    Collection<AssertionInfo> ais =
                            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SPNEGO_CONTEXT_TOKEN);
                    if (ais.isEmpty()) {
                        return;
                    }
                    for (AssertionInfo inf : ais) {
                        if (foundSCT) {
                            inf.setAsserted(true);
                        } else {
                            inf.setNotAsserted("No SecurityContextToken token found in message.");
                        }
                    }
                }
            } catch (TokenStoreException ex) {
                throw new Fault(ex);
            }
        }
    }



}
