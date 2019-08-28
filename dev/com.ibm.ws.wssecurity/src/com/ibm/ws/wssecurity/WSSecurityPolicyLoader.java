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
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.ws.policy.AssertionBuilderLoader;
import org.apache.cxf.ws.policy.AssertionBuilderRegistry;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyInterceptorProviderLoader;
import org.apache.cxf.ws.policy.PolicyInterceptorProviderRegistry;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertionBuilder;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.builders.AlgorithmSuiteBuilder;
import org.apache.cxf.ws.security.policy.builders.AsymmetricBindingBuilder;
import org.apache.cxf.ws.security.policy.builders.ContentEncryptedElementsBuilder;
import org.apache.cxf.ws.security.policy.builders.EncryptedElementsBuilder;
import org.apache.cxf.ws.security.policy.builders.EncryptedPartsBuilder;
import org.apache.cxf.ws.security.policy.builders.HttpsTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.InitiatorEncryptionTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.InitiatorSignatureTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.InitiatorTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.IssuedTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.KerberosTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.KeyValueTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.LayoutBuilder;
import org.apache.cxf.ws.security.policy.builders.ProtectionTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.RecipientEncryptionTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.RecipientSignatureTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.RecipientTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.RequiredElementsBuilder;
import org.apache.cxf.ws.security.policy.builders.RequiredPartsBuilder;
import org.apache.cxf.ws.security.policy.builders.SamlTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.SecureConversationTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.SecurityContextTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.SignedElementsBuilder;
import org.apache.cxf.ws.security.policy.builders.SignedPartsBuilder;
import org.apache.cxf.ws.security.policy.builders.SpnegoContextTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.SupportingTokens12Builder;
import org.apache.cxf.ws.security.policy.builders.SupportingTokensBuilder;
import org.apache.cxf.ws.security.policy.builders.SymmetricBindingBuilder;
import org.apache.cxf.ws.security.policy.builders.TransportBindingBuilder;
import org.apache.cxf.ws.security.policy.builders.TransportTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.Trust10Builder;
import org.apache.cxf.ws.security.policy.builders.Trust13Builder;
import org.apache.cxf.ws.security.policy.builders.UsernameTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.WSS10Builder;
import org.apache.cxf.ws.security.policy.builders.WSS11Builder;
import org.apache.cxf.ws.security.policy.builders.X509TokenBuilder;
import org.apache.cxf.ws.security.policy.interceptors.HttpsTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.IssuedTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.KerberosTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.SecureConversationTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.SpnegoTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.WSSecurityInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.WSSecurityPolicyInterceptorProvider;

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
        /*
         * if (tc.isEntryEnabled()) {
         * Tr.entry(tc, this.getClass().getName());
         * };
         */

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
        reg.registerBuilder(new AsymmetricBindingBuilder(pbuild));
        reg.registerBuilder(new ContentEncryptedElementsBuilder());
        reg.registerBuilder(new EncryptedElementsBuilder());
        reg.registerBuilder(new EncryptedPartsBuilder());
        reg.registerBuilder(new HttpsTokenBuilder(pbuild));
        reg.registerBuilder(new InitiatorTokenBuilder(pbuild));
        reg.registerBuilder(new InitiatorSignatureTokenBuilder(pbuild));
        reg.registerBuilder(new InitiatorEncryptionTokenBuilder(pbuild));
        reg.registerBuilder(new IssuedTokenBuilder(pbuild));
        reg.registerBuilder(new LayoutBuilder());
        reg.registerBuilder(new ProtectionTokenBuilder(pbuild));
        reg.registerBuilder(new RecipientTokenBuilder(pbuild));
        reg.registerBuilder(new RecipientSignatureTokenBuilder(pbuild));
        reg.registerBuilder(new RecipientEncryptionTokenBuilder(pbuild));
        reg.registerBuilder(new RequiredElementsBuilder());
        reg.registerBuilder(new RequiredPartsBuilder());
        reg.registerBuilder(new SamlTokenBuilder(pbuild));
        reg.registerBuilder(new KerberosTokenBuilder(pbuild));
        reg.registerBuilder(new SecureConversationTokenBuilder(pbuild));
        reg.registerBuilder(new SecurityContextTokenBuilder());
        reg.registerBuilder(new SignedElementsBuilder());
        reg.registerBuilder(new SignedPartsBuilder());
        reg.registerBuilder(new SpnegoContextTokenBuilder(pbuild));
        reg.registerBuilder(new SupportingTokens12Builder(pbuild));
        reg.registerBuilder(new SupportingTokensBuilder(pbuild));
        reg.registerBuilder(new SymmetricBindingBuilder(pbuild));
        reg.registerBuilder(new TransportBindingBuilder(pbuild, bus));
        reg.registerBuilder(new TransportTokenBuilder(pbuild));
        reg.registerBuilder(new Trust10Builder());
        reg.registerBuilder(new Trust13Builder());
        reg.registerBuilder(new UsernameTokenBuilder(pbuild));
        reg.registerBuilder(new KeyValueTokenBuilder());
        reg.registerBuilder(new WSS10Builder());
        reg.registerBuilder(new WSS11Builder());
        reg.registerBuilder(new X509TokenBuilder(pbuild));

        //add generic assertions for these known things to prevent warnings
        List<QName> others = Arrays.asList(new QName[] {
                                                        SP12Constants.INCLUDE_TIMESTAMP, SP11Constants.INCLUDE_TIMESTAMP,
                                                        SP12Constants.ENCRYPT_SIGNATURE, SP11Constants.ENCRYPT_SIGNATURE,
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
                                                        new QName(SP12Constants.SP_NS, SP12Constants.ENCRYPT_BEFORE_SIGNING),
                                                        new QName(SP11Constants.SP_NS, SP11Constants.ENCRYPT_BEFORE_SIGNING),
                                                        new QName(SP12Constants.SP_NS, SP12Constants.SIGN_BEFORE_ENCRYPTING),
                                                        new QName(SP11Constants.SP_NS, SP11Constants.SIGN_BEFORE_ENCRYPTING),
                                                        SP12Constants.REQUIRE_KEY_IDENTIFIER_REFERENCE,
                                                        SP11Constants.REQUIRE_KEY_IDENTIFIER_REFERENCE,
        });
        reg.registerBuilder(new PrimitiveAssertionBuilder(others));
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
