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
import org.apache.wss4j.policy.SPConstants;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.List;

public class SymmetricBinding extends AbstractSymmetricAsymmetricBinding {

    private EncryptionToken encryptionToken;
    private SignatureToken signatureToken;
    private ProtectionToken protectionToken;

    public SymmetricBinding(SPConstants.SPVersion version, Policy nestedPolicy) {
        super(version, nestedPolicy);

        parseNestedPolicy(nestedPolicy, this);
    }

    @Override
    public QName getName() {
        return getVersion().getSPConstants().getSymmetricBinding();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (!(object instanceof SymmetricBinding)) {
            return false;
        }

        SymmetricBinding that = (SymmetricBinding)object;
        if (encryptionToken != null && !encryptionToken.equals(that.encryptionToken)
            || encryptionToken == null && that.encryptionToken != null) {
            return false;
        }
        if (signatureToken != null && !signatureToken.equals(that.signatureToken)
            || signatureToken == null && that.signatureToken != null) {
            return false;
        }
        if (protectionToken != null && !protectionToken.equals(that.protectionToken)
            || protectionToken == null && that.protectionToken != null) {
            return false;
        }

        return super.equals(object);
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (encryptionToken != null) {
            result = 31 * result + encryptionToken.hashCode();
        }
        if (signatureToken != null) {
            result = 31 * result + signatureToken.hashCode();
        }
        if (protectionToken != null) {
            result = 31 * result + protectionToken.hashCode();
        }

        return 31 * result + super.hashCode();
    }

    @Override
    protected AbstractSecurityAssertion cloneAssertion(Policy nestedPolicy) {
        return new SymmetricBinding(getVersion(), nestedPolicy);
    }

    protected void parseNestedPolicy(Policy nestedPolicy, SymmetricBinding symmetricBinding) {
        Iterator<List<Assertion>> alternatives = nestedPolicy.getAlternatives();
        //we just process the first alternative
        //this means that if we have a compact policy only the first alternative is visible
        //in contrary to a normalized policy where just one alternative exists
        if (alternatives.hasNext()) {
            List<Assertion> assertions = alternatives.next();
            for (Assertion assertion : assertions) {
                String assertionName = assertion.getName().getLocalPart();
                String assertionNamespace = assertion.getName().getNamespaceURI();

                QName encryptionToken = getVersion().getSPConstants().getEncryptionToken();
                if (encryptionToken.getLocalPart().equals(assertionName)
                    && encryptionToken.getNamespaceURI().equals(assertionNamespace)) {
                    if (symmetricBinding.getEncryptionToken() != null
                        || symmetricBinding.getProtectionToken() != null) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    symmetricBinding.setEncryptionToken((EncryptionToken) assertion);
                    continue;
                }

                QName signatureToken = getVersion().getSPConstants().getSignatureToken();
                if (signatureToken.getLocalPart().equals(assertionName)
                    && signatureToken.getNamespaceURI().equals(assertionNamespace)) {
                    if (symmetricBinding.getSignatureToken() != null
                        || symmetricBinding.getProtectionToken() != null) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    symmetricBinding.setSignatureToken((SignatureToken) assertion);
                    continue;
                }

                QName protectionToken = getVersion().getSPConstants().getProtectionToken();
                if (protectionToken.getLocalPart().equals(assertionName)
                    && protectionToken.getNamespaceURI().equals(assertionNamespace)) {
                    if (symmetricBinding.getProtectionToken() != null
                            || symmetricBinding.getEncryptionToken() != null
                            || symmetricBinding.getSignatureToken() != null) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    symmetricBinding.setProtectionToken((ProtectionToken) assertion);
                    continue;
                }
            }
        }
    }

    public EncryptionToken getEncryptionToken() {
        return encryptionToken;
    }

    protected void setEncryptionToken(EncryptionToken encryptionToken) {
        this.encryptionToken = encryptionToken;
    }

    public SignatureToken getSignatureToken() {
        return signatureToken;
    }

    protected void setSignatureToken(SignatureToken signatureToken) {
        this.signatureToken = signatureToken;
    }

    public ProtectionToken getProtectionToken() {
        return protectionToken;
    }

    protected void setProtectionToken(ProtectionToken protectionToken) {
        this.protectionToken = protectionToken;
    }
}
