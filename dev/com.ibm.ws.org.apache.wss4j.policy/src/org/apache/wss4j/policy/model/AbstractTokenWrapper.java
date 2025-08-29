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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractTokenWrapper extends AbstractSecurityAssertion implements PolicyContainingAssertion {

    private Policy nestedPolicy;
    private AbstractToken token;
    private AbstractSecurityAssertion parentAssertion;

    protected AbstractTokenWrapper(SPConstants.SPVersion version, Policy nestedPolicy) {
        super(version);
        this.nestedPolicy = nestedPolicy;

        parseNestedPolicy(nestedPolicy, this);
    }

    @Override
    public Policy getPolicy() {
        return nestedPolicy;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof AbstractTokenWrapper)) {
            return false;
        }

        AbstractTokenWrapper that = (AbstractTokenWrapper)object;
        if (token != null && !token.equals(that.token)
            || token == null && that.token != null) {
            return false;
        }

        return super.equals(object);
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (token != null) {
            result = 31 * result + token.hashCode();
        }

        return 31 * result + super.hashCode();
    }

    @Override
    public PolicyComponent normalize() {
        return super.normalize(getPolicy());
    }

    @Override
    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        super.serialize(writer, getPolicy());
    }

    protected void parseNestedPolicy(Policy nestedPolicy, AbstractTokenWrapper tokenWrapper) {
        Iterator<List<Assertion>> alternatives = nestedPolicy.getAlternatives();
        //we just process the first alternative
        //this means that if we have a compact policy only the first alternative is visible
        //in contrary to a normalized policy where just one alternative exists
        if (alternatives.hasNext()) {
            List<Assertion> assertions = alternatives.next();
            for (Assertion assertion : assertions) {
                if (assertion instanceof AbstractToken) {
                    if (tokenWrapper.getToken() != null) {
                        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                    }
                    final AbstractToken abstractToken = (AbstractToken) assertion;
                    tokenWrapper.setToken(abstractToken);
                    abstractToken.setParentAssertion(tokenWrapper);
                    continue;
                }
            }
        }
    }

    public AbstractToken getToken() {
        return token;
    }

    protected void setToken(AbstractToken token) {
        this.token = token;
    }

    public AbstractSecurityAssertion getParentAssertion() {
        return parentAssertion;
    }

    public void setParentAssertion(AbstractSecurityAssertion parentAssertion) {
        this.parentAssertion = parentAssertion;
    }
}
