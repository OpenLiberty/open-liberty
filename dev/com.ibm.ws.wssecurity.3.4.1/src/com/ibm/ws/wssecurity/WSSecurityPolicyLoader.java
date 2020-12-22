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

package com.ibm.ws.wssecurity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.ws.policy.AssertionBuilderLoader;
import org.apache.cxf.ws.policy.AssertionBuilderRegistry;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyInterceptorProviderLoader;
import org.apache.cxf.ws.policy.PolicyInterceptorProviderRegistry;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertionBuilder;
import org.apache.cxf.ws.security.policy.custom.AlgorithmSuiteBuilder;
import org.apache.cxf.ws.security.policy.interceptors.HttpsTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.IssuedTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.KerberosTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.SecureConversationTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.SpnegoTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.WSSecurityInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.WSSecurityPolicyInterceptorProvider;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.builders.xml.XMLPrimitiveAssertionBuilder;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SP13Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.builders.AsymmetricBindingBuilder;
import org.apache.wss4j.policy.builders.BootstrapPolicyBuilder;
import org.apache.wss4j.policy.builders.ContentEncryptedElementsBuilder;
import org.apache.wss4j.policy.builders.EncryptedElementsBuilder;
import org.apache.wss4j.policy.builders.EncryptedPartsBuilder;
import org.apache.wss4j.policy.builders.HttpsTokenBuilder;
import org.apache.wss4j.policy.builders.InitiatorEncryptionTokenBuilder;
import org.apache.wss4j.policy.builders.InitiatorSignatureTokenBuilder;
import org.apache.wss4j.policy.builders.InitiatorTokenBuilder;
import org.apache.wss4j.policy.builders.IssuedTokenBuilder;
import org.apache.wss4j.policy.builders.KerberosTokenBuilder;
import org.apache.wss4j.policy.builders.KeyValueTokenBuilder;
import org.apache.wss4j.policy.builders.LayoutBuilder;
import org.apache.wss4j.policy.builders.ProtectionTokenBuilder;
import org.apache.wss4j.policy.builders.RecipientEncryptionTokenBuilder;
import org.apache.wss4j.policy.builders.RecipientSignatureTokenBuilder;
import org.apache.wss4j.policy.builders.RecipientTokenBuilder;
import org.apache.wss4j.policy.builders.RequiredElementsBuilder;
import org.apache.wss4j.policy.builders.RequiredPartsBuilder;
import org.apache.wss4j.policy.builders.SamlTokenBuilder;
import org.apache.wss4j.policy.builders.SecureConversationTokenBuilder;
import org.apache.wss4j.policy.builders.SecurityContextTokenBuilder;
import org.apache.wss4j.policy.builders.SignatureTokenBuilder;
import org.apache.wss4j.policy.builders.SignedElementsBuilder;
import org.apache.wss4j.policy.builders.SignedPartsBuilder;
import org.apache.wss4j.policy.builders.SpnegoContextTokenBuilder;
import org.apache.wss4j.policy.builders.SupportingTokensBuilder;
import org.apache.wss4j.policy.builders.SymmetricBindingBuilder;
import org.apache.wss4j.policy.builders.TransportBindingBuilder;
import org.apache.wss4j.policy.builders.TransportTokenBuilder;
import org.apache.wss4j.policy.builders.Trust10Builder;
import org.apache.wss4j.policy.builders.Trust13Builder;
import org.apache.wss4j.policy.builders.UsernameTokenBuilder;
import org.apache.wss4j.policy.builders.WSS10Builder;
import org.apache.wss4j.policy.builders.WSS11Builder;
import org.apache.wss4j.policy.builders.X509TokenBuilder;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.wssecurity.cxf.interceptor.UsernameTokenInterceptorProvider;
import com.ibm.ws.wssecurity.cxf.interceptor.WSSecurityLibertyCallerProvider;
import com.ibm.ws.wssecurity.cxf.interceptor.WSSecurityLibertyPluginProvider;
import com.ibm.ws.wssecurity.cxf.interceptor.WSSecuritySamlTokenInterceptorProvider;

@NoJSR250Annotations
public final class WSSecurityPolicyLoader implements PolicyInterceptorProviderLoader, AssertionBuilderLoader {

    private static final TraceComponent tc = Tr.register(WSSecurityPolicyLoader.class);

    Bus bus;

    public WSSecurityPolicyLoader(Bus b) {
        
          if (tc.isEntryEnabled()) {
             Tr.entry(tc, "WSSecurityPolicyLoader : ", this.getClass().getName());
          }
         

        bus = b;

        boolean bOk = true;
        try {
            registerBuilders();
            registerProviders();
        } catch (Throwable t) {
            //probably wss4j isn't found or something. We'll ignore this
            //as the policy framework will then not find the providers
            //and error out at that point.  If nothing uses ws-securitypolicy
            //no warnings/errors will display
            bOk = false;
            Tr.error(tc, "error.policy.notloaded", t);
        }
        if (bOk) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The WS-Security Policy Loader is invoked successfully.");
            }
        }
    }

    public void registerBuilders() {
        AssertionBuilderRegistry reg = bus.getExtension(AssertionBuilderRegistry.class);
        if (reg == null) {
            return;
        }
        PolicyBuilder pbuild = bus.getExtension(PolicyBuilder.class);
        reg.registerBuilder(new AlgorithmSuiteBuilder(bus));
        reg.registerBuilder(new AsymmetricBindingBuilder());
        reg.registerBuilder(new ContentEncryptedElementsBuilder());
        reg.registerBuilder(new EncryptedElementsBuilder());
        reg.registerBuilder(new EncryptedPartsBuilder());
        reg.registerBuilder(new HttpsTokenBuilder());
        reg.registerBuilder(new InitiatorTokenBuilder());
        reg.registerBuilder(new InitiatorSignatureTokenBuilder());
        
        reg.registerBuilder(new InitiatorEncryptionTokenBuilder());
        reg.registerBuilder(new IssuedTokenBuilder());
        reg.registerBuilder(new LayoutBuilder());
        reg.registerBuilder(new ProtectionTokenBuilder());
        reg.registerBuilder(new RecipientTokenBuilder());
        
        
        reg.registerBuilder(new RecipientSignatureTokenBuilder());
        reg.registerBuilder(new RecipientEncryptionTokenBuilder());
        reg.registerBuilder(new RequiredElementsBuilder());
        reg.registerBuilder(new RequiredPartsBuilder());
        
        reg.registerBuilder(new SamlTokenBuilder());
        reg.registerBuilder(new KerberosTokenBuilder());
        reg.registerBuilder(new SecureConversationTokenBuilder());
        reg.registerBuilder(new BootstrapPolicyBuilder());
        reg.registerBuilder(new SecurityContextTokenBuilder());
        
        
        reg.registerBuilder(new SignedElementsBuilder());
        reg.registerBuilder(new SignedPartsBuilder());
        reg.registerBuilder(new SignatureTokenBuilder());
        reg.registerBuilder(new SpnegoContextTokenBuilder());
        
        reg.registerBuilder(new SupportingTokensBuilder()); 
        
        reg.registerBuilder(new SymmetricBindingBuilder()); 
        reg.registerBuilder(new TransportBindingBuilder()); 
        reg.registerBuilder(new TransportTokenBuilder()); 
        reg.registerBuilder(new Trust10Builder());
        reg.registerBuilder(new Trust13Builder());
        reg.registerBuilder(new UsernameTokenBuilder()); 
        reg.registerBuilder(new KeyValueTokenBuilder()); 
        reg.registerBuilder(new WSS10Builder());
        reg.registerBuilder(new WSS11Builder());
        reg.registerBuilder(new X509TokenBuilder()); 

        //add generic assertions for these known things to prevent warnings
        List<QName> others = Arrays.asList(new QName[] {
                                                        SP12Constants.INCLUDE_TIMESTAMP, SP11Constants.INCLUDE_TIMESTAMP,
                                                        SP12Constants.ENCRYPT_SIGNATURE, SP11Constants.ENCRYPT_SIGNATURE,
                                                        SP12Constants.PROTECT_TOKENS, SP11Constants.PROTECT_TOKENS,
                                                        SP12Constants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY,
                                                        SP11Constants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY,
                                                        SP12Constants.WSS_X509_V1_TOKEN_10,
                                                        SP12Constants.WSS_X509_V1_TOKEN_11,
                                                        SP12Constants.WSS_X509_V3_TOKEN_10,
                                                        SP12Constants.WSS_X509_V3_TOKEN_11,
                                                        SP11Constants.WSS_X509_V1_TOKEN_10,
                                                        SP11Constants.WSS_X509_V1_TOKEN_11,
                                                        SP11Constants.WSS_X509_V3_TOKEN_10,
                                                        SP11Constants.WSS_X509_V3_TOKEN_11,
                                                        SP12Constants.WSS_X509_PKCS7_TOKEN_11,
                                                        SP12Constants.WSS_X509_PKI_PATH_V1_TOKEN_11,
                                                        SP11Constants.WSS_X509_PKCS7_TOKEN_11,
                                                        SP11Constants.WSS_X509_PKI_PATH_V1_TOKEN_11,
                                                        SP12Constants.REQUIRE_THUMBPRINT_REFERENCE,
                                                        SP11Constants.REQUIRE_THUMBPRINT_REFERENCE,
                                                        SP12Constants.REQUIRE_DERIVED_KEYS,
                                                        SP11Constants.REQUIRE_DERIVED_KEYS,
                                                        SP12Constants.REQUIRE_INTERNAL_REFERENCE,
                                                        SP11Constants.REQUIRE_INTERNAL_REFERENCE,
                                                        SP12Constants.REQUIRE_ISSUER_SERIAL_REFERENCE,
                                                        SP11Constants.REQUIRE_ISSUER_SERIAL_REFERENCE,
                                                        SP12Constants.REQUIRE_EMBEDDED_TOKEN_REFERENCE,
                                                        SP11Constants.REQUIRE_EMBEDDED_TOKEN_REFERENCE,
                                                        //new QName(SP12Constants.SP_NS, SP12Constants.ENCRYPT_BEFORE_SIGNING),
                                                        SP12Constants.ENCRYPT_BEFORE_SIGNING,
                                                        //new QName(SP11Constants.SP_NS, SP11Constants.ENCRYPT_BEFORE_SIGNING), 
                                                        SP11Constants.ENCRYPT_BEFORE_SIGNING,
                                                        //new QName(SP12Constants.SP_NS, SP12Constants.SIGN_BEFORE_ENCRYPTING), 
                                                        SP12Constants.SIGN_BEFORE_ENCRYPTING,
                                                        //new QName(SP11Constants.SP_NS, SP11Constants.SIGN_BEFORE_ENCRYPTING), 
                                                        SP11Constants.SIGN_BEFORE_ENCRYPTING,
                                                        SP12Constants.REQUIRE_KEY_IDENTIFIER_REFERENCE,
                                                        SP11Constants.REQUIRE_KEY_IDENTIFIER_REFERENCE,
                                                        SP12Constants.PROTECT_TOKENS,
                                                        SP11Constants.PROTECT_TOKENS,
                                                        SP12Constants.RSA_KEY_VALUE,
                                                        
                                                        // Layout
                                                        SP11Constants.LAX, SP11Constants.LAXTSFIRST, SP11Constants.LAXTSLAST, SP11Constants.STRICT,
                                                        SP12Constants.LAX, SP12Constants.LAXTSFIRST, SP12Constants.LAXTSLAST, SP12Constants.STRICT,

                                                        // UsernameToken
                                                        SP11Constants.WSS_USERNAME_TOKEN10, SP12Constants.WSS_USERNAME_TOKEN10,
                                                        SP11Constants.WSS_USERNAME_TOKEN11, SP12Constants.WSS_USERNAME_TOKEN11,
                                                        SP12Constants.HASH_PASSWORD, SP12Constants.NO_PASSWORD,
                                                        SP13Constants.CREATED, SP13Constants.NONCE,

                                                        SP12Constants.REQUIRE_INTERNAL_REFERENCE, SP11Constants.REQUIRE_INTERNAL_REFERENCE,
                                                        SP12Constants.REQUIRE_EXTERNAL_REFERNCE, SP11Constants.REQUIRE_EXTERNAL_REFERNCE,

                                                        // Kerberos
                                                        new QName(SP11Constants.SP_NS, "WssKerberosV5ApReqToken11"),
                                                        new QName(SP12Constants.SP_NS, "WssKerberosV5ApReqToken11"),
                                                        new QName(SP11Constants.SP_NS, "WssGssKerberosV5ApReqToken11"),
                                                        new QName(SP12Constants.SP_NS, "WssGssKerberosV5ApReqToken11"),

                                                        // Spnego
                                                        SP12Constants.MUST_NOT_SEND_AMEND,
                                                        SP12Constants.MUST_NOT_SEND_CANCEL,
                                                        SP12Constants.MUST_NOT_SEND_RENEW,

                                                        // Backwards compatibility thing
                                                        new QName("http://schemas.microsoft.com/ws/2005/07/securitypolicy", SPConstants.MUST_NOT_SEND_CANCEL),

                                                        // SCT
                                                        SP12Constants.REQUIRE_EXTERNAL_URI_REFERENCE,
                                                        SP12Constants.SC13_SECURITY_CONTEXT_TOKEN,
                                                        SP11Constants.SC10_SECURITY_CONTEXT_TOKEN,

                                                        // WSS10
                                                        SP12Constants.MUST_SUPPORT_REF_KEY_IDENTIFIER, SP11Constants.MUST_SUPPORT_REF_KEY_IDENTIFIER,
                                                        SP12Constants.MUST_SUPPORT_REF_ISSUER_SERIAL, SP11Constants.MUST_SUPPORT_REF_ISSUER_SERIAL,
                                                        SP12Constants.MUST_SUPPORT_REF_EXTERNAL_URI, SP12Constants.MUST_SUPPORT_REF_EXTERNAL_URI,
                                                        SP12Constants.MUST_SUPPORT_REF_EMBEDDED_TOKEN, SP11Constants.MUST_SUPPORT_REF_EMBEDDED_TOKEN,

                                                        // WSS11
                                                        SP12Constants.MUST_SUPPORT_REF_THUMBPRINT, SP11Constants.MUST_SUPPORT_REF_THUMBPRINT,
                                                        SP12Constants.MUST_SUPPORT_REF_ENCRYPTED_KEY, SP11Constants.MUST_SUPPORT_REF_ENCRYPTED_KEY,
                                                        SP12Constants.REQUIRE_SIGNATURE_CONFIRMATION, SP11Constants.REQUIRE_SIGNATURE_CONFIRMATION,

                                                        // SAML
                                                        new QName(SP11Constants.SP_NS, "WssSamlV11Token10"),
                                                        new QName(SP12Constants.SP_NS, "WssSamlV11Token10"),
                                                        new QName(SP11Constants.SP_NS, "WssSamlV11Token11"),
                                                        new QName(SP12Constants.SP_NS, "WssSamlV11Token11"),
                                                        new QName(SP11Constants.SP_NS, "WssSamlV20Token11"),
                                                        new QName(SP12Constants.SP_NS, "WssSamlV20Token11"),

                                                        // HTTPs
                                                        SP12Constants.HTTP_BASIC_AUTHENTICATION,
                                                        SP12Constants.HTTP_DIGEST_AUTHENTICATION,
                                                        SP12Constants.REQUIRE_CLIENT_CERTIFICATE,

                                                        // Trust13
                                                        SP12Constants.MUST_SUPPORT_CLIENT_CHALLENGE, SP11Constants.MUST_SUPPORT_CLIENT_CHALLENGE,
                                                        SP12Constants.MUST_SUPPORT_SERVER_CHALLENGE, SP11Constants.MUST_SUPPORT_SERVER_CHALLENGE,
                                                        SP12Constants.REQUIRE_CLIENT_ENTROPY, SP11Constants.REQUIRE_CLIENT_ENTROPY,
                                                        SP12Constants.REQUIRE_SERVER_ENTROPY, SP11Constants.REQUIRE_SERVER_ENTROPY,
                                                        SP12Constants.MUST_SUPPORT_ISSUED_TOKENS, SP11Constants.MUST_SUPPORT_ISSUED_TOKENS,
                                                        SP12Constants.REQUIRE_REQUEST_SECURITY_TOKEN_COLLECTION,
                                                        SP12Constants.REQUIRE_APPLIES_TO,
                                                        SP13Constants.SCOPE_POLICY_15,
                                                        SP13Constants.MUST_SUPPORT_INTERACTIVE_CHALLENGE,

                                                        // AlgorithmSuite misc
                                                        new QName(SP11Constants.SP_NS, SPConstants.INCLUSIVE_C14N),
                                                        new QName(SP12Constants.SP_NS, SPConstants.INCLUSIVE_C14N),
        });
        //reg.registerBuilder(new PrimitiveAssertionBuilder(others));
        
        final Map<QName, Assertion> assertions = new HashMap<>();
        for (QName q : others) {
            assertions.put(q, new PrimitiveAssertion(q));
        }
        for (String s : AlgorithmSuite.getSupportedAlgorithmSuiteNames()) {
            QName q = new QName(SP11Constants.SP_NS, s);
            assertions.put(q, new PrimitiveAssertion(q));
            q = new QName(SP12Constants.SP_NS, s);
            assertions.put(q, new PrimitiveAssertion(q));
        }
        reg.registerBuilder(new PrimitiveAssertionBuilder(assertions.keySet()) {
            public Assertion build(Element element, AssertionBuilderFactory fact) {
                if (XMLPrimitiveAssertionBuilder.isOptional(element)
                    || XMLPrimitiveAssertionBuilder.isIgnorable(element)) {
                    return super.build(element, fact);
                }
                QName q = new QName(element.getNamespaceURI(), element.getLocalName());
                return assertions.get(q);
            }
        });
    }

    public void registerProviders() {
        //interceptor providers for all of the above
        PolicyInterceptorProviderRegistry reg = bus.getExtension(PolicyInterceptorProviderRegistry.class);
        if (reg == null) {
            return;
        }
        reg.register(new WSSecurityPolicyInterceptorProvider());
        reg.register(new WSSecurityInterceptorProvider());
        reg.register(new HttpsTokenInterceptorProvider());
        reg.register(new KerberosTokenInterceptorProvider());
        reg.register(new IssuedTokenInterceptorProvider());
        //reg.register(new UsernameTokenBindingInterceptorProvider(bus));
        reg.register(new UsernameTokenInterceptorProvider(bus));
        reg.register(new WSSecuritySamlTokenInterceptorProvider());
        reg.register(new SecureConversationTokenInterceptorProvider());
        reg.register(new SpnegoTokenInterceptorProvider());
        reg.register(new WSSecurityLibertyPluginProvider());
        reg.register(new WSSecurityLibertyCallerProvider()); //Caller support

    }

}
