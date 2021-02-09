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
package org.apache.cxf.ws.security.policy.model;

import org.w3c.dom.Element;

import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.SPConstants.IncludeTokenType;

public abstract class Token extends AbstractSecurityAssertion {

    /**
     * Inclusion property of a TokenAssertion
     */
    private IncludeTokenType inclusion = IncludeTokenType.INCLUDE_TOKEN_ALWAYS;

    /**
     * Whether to derive keys or not
     */
    private boolean derivedKeys;

    private boolean impliedDerivedKeys;

    private boolean explicitDerivedKeys;
    
    private String issuerName;
    
    /**
     * A reference to the DOM wsp:Policy child Element
     */
    private Element policy;
    
    /**
     * A reference to the DOM wst:Claims child Element
     */
    private Element claims;
    
    /**
     * A Reference to a parent SupportingToken assertion
     */
    private SupportingToken supportingToken;

    public Token(SPConstants version) {
        super(version);
    }
    
    /**
     * @return Returns the inclusion.
     */
    public IncludeTokenType getInclusion() {
        return inclusion;
    }

    /**
     * @param inclusion The inclusion to set.
     */
    public void setInclusion(IncludeTokenType inclusion) {
        if (IncludeTokenType.INCLUDE_TOKEN_ALWAYS == inclusion
            || IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT == inclusion
            || IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_INITIATOR == inclusion
            || IncludeTokenType.INCLUDE_TOKEN_NEVER == inclusion 
            || IncludeTokenType.INCLUDE_TOKEN_ONCE == inclusion) {
            this.inclusion = inclusion;
        } else {
            // TODO replace this with a proper (WSSPolicyException) exception
            throw new RuntimeException("Incorrect inclusion value: " + inclusion);
        }
    }

    /**
     * @return Returns the derivedKeys.
     */
    public boolean isDerivedKeys() {
        return derivedKeys;
    }

    /**
     * @param derivedKeys The derivedKeys to set.
     */
    public void setDerivedKeys(boolean derivedKeys) {
        this.derivedKeys = derivedKeys;
    }

    public boolean isExplicitDerivedKeys() {
        return explicitDerivedKeys;
    }

    public void setExplicitDerivedKeys(boolean explicitDerivedKeys) {
        this.explicitDerivedKeys = explicitDerivedKeys;
    }

    public boolean isImpliedDerivedKeys() {
        return impliedDerivedKeys;
    }

    public void setImpliedDerivedKeys(boolean impliedDerivedKeys) {
        this.impliedDerivedKeys = impliedDerivedKeys;
    }

    public String getIssuerName() {
        return issuerName;
    }
    
    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }
    
    public SupportingToken getSupportingToken() {
        return supportingToken;
    }

    public void setSupportingToken(SupportingToken supportingToken) {
        this.supportingToken = supportingToken;
    }

    public Element getPolicy() {
        return policy;
    }

    public void setPolicy(Element policy) {
        this.policy = policy;
    }

    public Element getClaims() {
        return claims;
    }

    public void setClaims(Element claims) {
        this.claims = claims;
    }
}