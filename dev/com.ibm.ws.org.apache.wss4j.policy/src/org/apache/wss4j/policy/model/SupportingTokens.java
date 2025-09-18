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
package org.apache.wss4j.policy.model;

import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyContainingAssertion;
import org.apache.wss4j.policy.SPConstants;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SupportingTokens extends AbstractSecurityAssertion implements PolicyContainingAssertion {

    private QName supportingTokenType;
    private final List<AbstractToken> tokens = new ArrayList<>();
    private AlgorithmSuite algorithmSuite;
    private SignedParts signedParts;
    private SignedElements signedElements;
    private EncryptedParts encryptedParts;
    private EncryptedElements encryptedElements;
    private Policy nestedPolicy;

    public SupportingTokens(SPConstants.SPVersion version, QName supportingTokenType, Policy nestedPolicy) {
        super(version);
        this.supportingTokenType = supportingTokenType;
        this.nestedPolicy = nestedPolicy;

        parseNestedPolicy(nestedPolicy, this);
    }

    @Override
    public QName getName() {
        return supportingTokenType;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (!(object instanceof SupportingTokens)) {
            return false;
        }

        SupportingTokens that = (SupportingTokens)object;
        if (supportingTokenType != null && !supportingTokenType.equals(that.supportingTokenType)
            || supportingTokenType == null && that.supportingTokenType != null) {
            return false;
        }
        if (algorithmSuite != null && !algorithmSuite.equals(that.algorithmSuite)
            || algorithmSuite == null && that.algorithmSuite != null) {
            return false;
        }
        if (signedParts != null && !signedParts.equals(that.signedParts)
            || signedParts == null && that.signedParts != null) {
            return false;
        }
        if (signedElements != null && !signedElements.equals(that.signedElements)
            || signedElements == null && that.signedElements != null) {
            return false;
        }
        if (encryptedParts != null && !encryptedParts.equals(that.encryptedParts)
            || encryptedParts == null && that.encryptedParts != null) {
            return false;
        }
        if (encryptedElements != null && !encryptedElements.equals(that.encryptedElements)
            || encryptedElements == null && that.encryptedElements != null) {
            return false;
        }
        if (!tokens.equals(that.tokens)) {
            return false;
        }

        return super.equals(object);
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (supportingTokenType != null) {
            result = 31 * result + supportingTokenType.hashCode();
        }
        if (algorithmSuite != null) {
            result = 31 * result + algorithmSuite.hashCode();
        }
        if (signedParts != null) {
            result = 31 * result + signedParts.hashCode();
        }
        if (signedElements != null) {
            result = 31 * result + signedElements.hashCode();
        }
        if (encryptedParts != null) {
            result = 31 * result + encryptedParts.hashCode();
        }
        if (encryptedElements != null) {
            result = 31 * result + encryptedElements.hashCode();
        }
        result = 31 * result + tokens.hashCode();

        return 31 * result + super.hashCode();
    }

    @Override
    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        super.serialize(writer, getPolicy());
    }

    @Override
    public PolicyComponent normalize() {
        return super.normalize(getPolicy());
    }

    @Override
    protected AbstractSecurityAssertion cloneAssertion(Policy nestedPolicy) {
        return new SupportingTokens(getVersion(), getName(), nestedPolicy);
    }

    @Override
    public Policy getPolicy() {
        return nestedPolicy;
    }

    protected void parseNestedPolicy(Policy nestedPolicy, SupportingTokens supportingTokens) {
        Iterator<List<Assertion>> alternatives = nestedPolicy.getAlternatives();
        //we just process the first alternative
        //this means that if we have a compact policy only the first alternative is visible
        //in contrary to a normalized policy where just one alternative exists
        if (alternatives.hasNext()) {
            List<Assertion> assertions = alternatives.next();
            for (Assertion assertion : assertions) {
                String assertionName = assertion.getName().getLocalPart();
                String assertionNamespace = assertion.getName().getNamespaceURI();
                if (assertion instanceof AbstractToken) {
                    AbstractToken abstractToken = (AbstractToken) assertion;
                    supportingTokens.addToken(abstractToken);
                    abstractToken.setParentAssertion(supportingTokens);
                    continue;
                }

                QName algSuite = getVersion().getSPConstants().getAlgorithmSuite();
                if (algSuite.getLocalPart().equals(assertionName)
                    && algSuite.getNamespaceURI().equals(assertionNamespace)) {
                    if (supportingTokens.getAlgorithmSuite() != null) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    supportingTokens.setAlgorithmSuite((AlgorithmSuite) assertion);
                    continue;
                }

                QName signedParts = getVersion().getSPConstants().getSignedParts();
                if (signedParts.getLocalPart().equals(assertionName)
                    && signedParts.getNamespaceURI().equals(assertionNamespace)) {
                    if (supportingTokens.getSignedParts() != null) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    supportingTokens.setSignedParts((SignedParts) assertion);
                    continue;
                }

                QName signedElements = getVersion().getSPConstants().getSignedElements();
                if (signedElements.getLocalPart().equals(assertionName)
                    && signedElements.getNamespaceURI().equals(assertionNamespace)) {
                    if (supportingTokens.getSignedElements() != null) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    supportingTokens.setSignedElements((SignedElements) assertion);
                    continue;
                }

                QName encryptedParts = getVersion().getSPConstants().getEncryptedParts();
                if (encryptedParts.getLocalPart().equals(assertionName)
                    && encryptedParts.getNamespaceURI().equals(assertionNamespace)) {
                    if (supportingTokens.getEncryptedParts() != null) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    supportingTokens.setEncryptedParts((EncryptedParts) assertion);
                    continue;
                }

                QName encryptedElements = getVersion().getSPConstants().getEncryptedElements();
                if (encryptedElements.getLocalPart().equals(assertionName)
                    && encryptedElements.getNamespaceURI().equals(assertionNamespace)) {
                    if (supportingTokens.getEncryptedElements() != null) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    supportingTokens.setEncryptedElements((EncryptedElements) assertion);
                    continue;
                }
            }
        }
    }

    public List<AbstractToken> getTokens() {
        return tokens;
    }

    public void addToken(AbstractToken token) {
        this.tokens.add(token);
    }

    public AlgorithmSuite getAlgorithmSuite() {
        return algorithmSuite;
    }

    protected void setAlgorithmSuite(AlgorithmSuite algorithmSuite) {
        this.algorithmSuite = algorithmSuite;
    }

    public SignedParts getSignedParts() {
        return signedParts;
    }

    protected void setSignedParts(SignedParts signedParts) {
        this.signedParts = signedParts;
    }

    public SignedElements getSignedElements() {
        return signedElements;
    }

    protected void setSignedElements(SignedElements signedElements) {
        this.signedElements = signedElements;
    }

    public EncryptedParts getEncryptedParts() {
        return encryptedParts;
    }

    protected void setEncryptedParts(EncryptedParts encryptedParts) {
        this.encryptedParts = encryptedParts;
    }

    public EncryptedElements getEncryptedElements() {
        return encryptedElements;
    }

    protected void setEncryptedElements(EncryptedElements encryptedElements) {
        this.encryptedElements = encryptedElements;
    }

    /**
     * @return true if the supporting token should be encrypted
     */
    public boolean isEncryptedToken() {
        QName name = getName();
        if (name != null && name.getLocalPart().contains("Encrypted")) {
            return true;
        }
        return false;
    }

    /**
     * @return true if the supporting token is endorsing
     */
    public boolean isEndorsing() {
        QName name = getName();
        if (name != null && name.getLocalPart().contains("Endorsing")) {
            return true;
        }
        return false;
    }

}
