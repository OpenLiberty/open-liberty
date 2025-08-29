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
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.*;

public class UsernameToken extends AbstractToken {

    public enum PasswordType {
        NoPassword,
        HashPassword;

        private static final Map<String, PasswordType> LOOKUP = new HashMap<>();

        static {
            for (PasswordType u : EnumSet.allOf(PasswordType.class)) {
                LOOKUP.put(u.name(), u);
            }
        }

        public static PasswordType lookUp(String name) {
            return LOOKUP.get(name);
        }
    }

    public enum UsernameTokenType {
        WssUsernameToken10,
        WssUsernameToken11;

        private static final Map<String, UsernameTokenType> LOOKUP = new HashMap<>();

        static {
            for (UsernameTokenType u : EnumSet.allOf(UsernameTokenType.class)) {
                LOOKUP.put(u.name(), u);
            }
        }

        public static UsernameTokenType lookUp(String name) {
            return LOOKUP.get(name);
        }
    }

    private PasswordType passwordType;
    private boolean created;
    private boolean nonce;
    private UsernameTokenType usernameTokenType;

    public UsernameToken(SPConstants.SPVersion version, SPConstants.IncludeTokenType includeTokenType,
                         Element issuer, String issuerName, Element claims, Policy nestedPolicy) {
        super(version, includeTokenType, issuer, issuerName, claims, nestedPolicy);

        parseNestedPolicy(nestedPolicy, this);
    }

    @Override
    public QName getName() {
        return getVersion().getSPConstants().getUsernameToken();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof UsernameToken)) {
            return false;
        }

        UsernameToken that = (UsernameToken)object;
        if (passwordType != that.passwordType || usernameTokenType != that.usernameTokenType) {
            return false;
        }
        if (created != that.created || nonce != that.nonce) {
            return false;
        }

        return super.equals(object);
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (passwordType != null) {
            result = 31 * result + passwordType.hashCode();
        }
        if (usernameTokenType != null) {
            result = 31 * result + usernameTokenType.hashCode();
        }
        result = 31 * result + Boolean.hashCode(created);
        result = 31 * result + Boolean.hashCode(nonce);

        return 31 * result + super.hashCode();
    }

    @Override
    protected AbstractSecurityAssertion cloneAssertion(Policy nestedPolicy) {
        return new UsernameToken(getVersion(), getIncludeTokenType(), getIssuer(),
                                 getIssuerName(), getClaims(), nestedPolicy);
    }

    protected void parseNestedPolicy(Policy nestedPolicy, UsernameToken usernameToken) {
        Iterator<List<Assertion>> alternatives = nestedPolicy.getAlternatives();
        //we just process the first alternative
        //this means that if we have a compact policy only the first alternative is visible
        //in contrary to a normalized policy where just one alternative exists
        if (alternatives.hasNext()) {
            List<Assertion> assertions = alternatives.next();
            for (Assertion assertion : assertions) {
                String assertionName = assertion.getName().getLocalPart();
                String assertionNamespace = assertion.getName().getNamespaceURI();
                PasswordType passwordType = PasswordType.lookUp(assertionName);
                if (passwordType != null) {
                    if (usernameToken.getPasswordType() != null) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    usernameToken.setPasswordType(passwordType);
                    continue;
                }
                if (getVersion().getSPConstants().getCreated().getLocalPart().equals(assertionName)
                        && getVersion().getSPConstants().getCreated().getNamespaceURI().equals(assertionNamespace)) {
                    if (usernameToken.isCreated()) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    usernameToken.setCreated(true);
                    continue;
                }
                if (getVersion().getSPConstants().getNonce().getLocalPart().equals(assertionName)
                        && getVersion().getSPConstants().getNonce().getNamespaceURI().equals(assertionNamespace)) {
                    if (usernameToken.isNonce()) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    usernameToken.setNonce(true);
                    continue;
                }
                DerivedKeys derivedKeys = DerivedKeys.lookUp(assertionName);
                if (derivedKeys != null) {
                    if (usernameToken.getDerivedKeys() != null) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    usernameToken.setDerivedKeys(derivedKeys);
                    continue;
                }
                UsernameTokenType usernameTokenType = UsernameTokenType.lookUp(assertionName);
                if (usernameTokenType != null) {
                    if (usernameToken.getUsernameTokenType() != null) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    usernameToken.setUsernameTokenType(usernameTokenType);
                    continue;
                }
            }
        }
    }

    public PasswordType getPasswordType() {
        return passwordType;
    }

    protected void setPasswordType(PasswordType passwordType) {
        this.passwordType = passwordType;
    }

    public boolean isCreated() {
        return created;
    }

    protected void setCreated(boolean created) {
        this.created = created;
    }

    public boolean isNonce() {
        return nonce;
    }

    protected void setNonce(boolean nonce) {
        this.nonce = nonce;
    }

    public UsernameTokenType getUsernameTokenType() {
        return usernameTokenType;
    }

    protected void setUsernameTokenType(UsernameTokenType usernameTokenType) {
        this.usernameTokenType = usernameTokenType;
    }
}
