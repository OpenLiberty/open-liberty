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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.policy.custom.DefaultAlgorithmSuiteLoader;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.AlgorithmSuite;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractBinding;
import org.apache.wss4j.policy.model.AbstractSecurityAssertion;
import org.apache.wss4j.policy.model.AlgorithmSuite.AlgorithmSuiteType;
import org.apache.wss4j.policy.model.SamlToken;
import org.apache.wss4j.policy.model.SupportingTokens;

/**
 * Translate any AlgorithmSuite policy that may be operative into a WSS4J AlgorithmSuite object
 * to enforce what algorithms are allowed in a request.
 */
public final class AlgorithmSuiteTranslater {

    private final Map<String, Object> customAlgSuiteProperties;

    public AlgorithmSuiteTranslater() {
        this.customAlgSuiteProperties = Collections.emptyMap();
    }

    public AlgorithmSuiteTranslater(Map<String, Object> customAlgorithmSuiteProperties) {
        this.customAlgSuiteProperties = customAlgorithmSuiteProperties;
    }

    public void translateAlgorithmSuites(AssertionInfoMap aim, RequestData data) throws WSSecurityException {
        if (aim == null) {
            return;
        }

        List<org.apache.wss4j.policy.model.AlgorithmSuite> algorithmSuites =
            getAlgorithmSuites(getBindings(aim));
        if (!algorithmSuites.isEmpty()) {
            // Translate into WSS4J's AlgorithmSuite class
            AlgorithmSuite algorithmSuite = translateAlgorithmSuites(algorithmSuites);
            data.setAlgorithmSuite(algorithmSuite);
        }

        // Now look for an AlgorithmSuite for a SAML Assertion
        Collection<AssertionInfo> ais =
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SAML_TOKEN);
        if (!ais.isEmpty()) {
            List<org.apache.wss4j.policy.model.AlgorithmSuite> samlAlgorithmSuites = new ArrayList<>();
            for (AssertionInfo ai : ais) {
                SamlToken samlToken = (SamlToken)ai.getAssertion();
                AbstractSecurityAssertion parentAssertion = samlToken.getParentAssertion();
                if (parentAssertion instanceof SupportingTokens
                    && ((SupportingTokens)parentAssertion).getAlgorithmSuite() != null) {
                    samlAlgorithmSuites.add(((SupportingTokens)parentAssertion).getAlgorithmSuite());
                }
            }

            if (!samlAlgorithmSuites.isEmpty()) {
                data.setSamlAlgorithmSuite(translateAlgorithmSuites(samlAlgorithmSuites));
            }
        }
    }

    /**
     * Translate a list of CXF AlgorithmSuite objects into a single WSS4J AlgorithmSuite object
     */
    private AlgorithmSuite translateAlgorithmSuites(
        List<org.apache.wss4j.policy.model.AlgorithmSuite> algorithmSuites
    ) {
        AlgorithmSuite algorithmSuite = null;

        for (org.apache.wss4j.policy.model.AlgorithmSuite cxfAlgorithmSuite : algorithmSuites) {
            if (cxfAlgorithmSuite == null) {
                continue;
            }

            // Translate into WSS4J's AlgorithmSuite class
            if (algorithmSuite == null) {
                algorithmSuite = new AlgorithmSuite();
            }
            AlgorithmSuiteType customAlgSuite =
                    DefaultAlgorithmSuiteLoader.customize(cxfAlgorithmSuite.getAlgorithmSuiteType(),
                            customAlgSuiteProperties);

            if (customAlgSuite != null) {
            // Set asymmetric key lengths
                if (algorithmSuite.getMaximumAsymmetricKeyLength()
                    < customAlgSuite.getMaximumAsymmetricKeyLength()) {
                    algorithmSuite.setMaximumAsymmetricKeyLength(
                        customAlgSuite.getMaximumAsymmetricKeyLength());
                }
                if (algorithmSuite.getMinimumAsymmetricKeyLength()
                    > customAlgSuite.getMinimumAsymmetricKeyLength()) {
                    algorithmSuite.setMinimumAsymmetricKeyLength(
                        customAlgSuite.getMinimumAsymmetricKeyLength());
                }

                // Set symmetric key lengths
                if (algorithmSuite.getMaximumSymmetricKeyLength()
                    < customAlgSuite.getMaximumSymmetricKeyLength()) {
                    algorithmSuite.setMaximumSymmetricKeyLength(
                        customAlgSuite.getMaximumSymmetricKeyLength());
                }
                if (algorithmSuite.getMinimumSymmetricKeyLength()
                    > customAlgSuite.getMinimumSymmetricKeyLength()) {
                    algorithmSuite.setMinimumSymmetricKeyLength(
                        customAlgSuite.getMinimumSymmetricKeyLength());
                }

                algorithmSuite.addEncryptionMethod(customAlgSuite.getEncryption());
                algorithmSuite.addKeyWrapAlgorithm(customAlgSuite.getSymmetricKeyWrap());
                algorithmSuite.addKeyWrapAlgorithm(customAlgSuite.getAsymmetricKeyWrap());
                algorithmSuite.addDigestAlgorithm(customAlgSuite.getDigest());

                algorithmSuite.addSignatureMethod(customAlgSuite.getAsymmetricSignature());
                algorithmSuite.addSignatureMethod(customAlgSuite.getSymmetricSignature());
            }

            algorithmSuite.addC14nAlgorithm(cxfAlgorithmSuite.getC14n().getValue());

            algorithmSuite.addTransformAlgorithm(cxfAlgorithmSuite.getC14n().getValue());
            algorithmSuite.addTransformAlgorithm(SPConstants.STRT10);
            algorithmSuite.addTransformAlgorithm(WSS4JConstants.C14N_EXCL_OMIT_COMMENTS);
            algorithmSuite.addTransformAlgorithm(WSS4JConstants.NS_XMLDSIG_ENVELOPED_SIGNATURE);
            algorithmSuite.addTransformAlgorithm(WSS4JConstants.SWA_ATTACHMENT_CONTENT_SIG_TRANS);
            algorithmSuite.addTransformAlgorithm(WSS4JConstants.SWA_ATTACHMENT_COMPLETE_SIG_TRANS);

            algorithmSuite.addDerivedKeyAlgorithm(SPConstants.P_SHA1);
            algorithmSuite.addDerivedKeyAlgorithm(SPConstants.P_SHA1_L128);
        }

        return algorithmSuite;
    }

    /**
     * Get all of the WS-SecurityPolicy Bindings that are in operation
     */
    private List<AbstractBinding> getBindings(AssertionInfoMap aim) {
        List<AbstractBinding> bindings = new ArrayList<>();

        Collection<AssertionInfo> ais =
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.TRANSPORT_BINDING);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                bindings.add((AbstractBinding)ai.getAssertion());
            }
        }
        ais = PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.ASYMMETRIC_BINDING);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                bindings.add((AbstractBinding)ai.getAssertion());
            }
        }
        ais = PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                bindings.add((AbstractBinding)ai.getAssertion());
            }
        }

        return bindings;
    }

    /**
     * Get all of the CXF AlgorithmSuites from the bindings
     */
    private List<org.apache.wss4j.policy.model.AlgorithmSuite> getAlgorithmSuites(
        List<AbstractBinding> bindings
    ) {
        List<org.apache.wss4j.policy.model.AlgorithmSuite> algorithmSuites = new ArrayList<>();
        for (AbstractBinding binding : bindings) {
            if (binding.getAlgorithmSuite() != null) {
                algorithmSuites.add(binding.getAlgorithmSuite());
            }
        }
        return algorithmSuites;
    }
}
