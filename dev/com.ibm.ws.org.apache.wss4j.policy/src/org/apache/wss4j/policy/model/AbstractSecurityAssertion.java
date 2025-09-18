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

import org.apache.neethi.*;
import org.apache.wss4j.policy.AssertionState;
import org.apache.wss4j.policy.SPConstants;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class AbstractSecurityAssertion implements Assertion {

    private boolean isOptional;
    private boolean isIgnorable;

    // if normalized is null, then this policy hasn't been normalized yet
    // if normalized == this, then this policy is already in normalized form
    // else, normalized contains the normalized version of this policy
    private volatile PolicyComponent normalized;

    private SPConstants.SPVersion version;

    protected AbstractSecurityAssertion(SPConstants.SPVersion version) {
        this.version = version;
    }

    @Override
    public boolean isOptional() {
        return isOptional;
    }

    public void setOptional(boolean isOptional) {
        this.isOptional = isOptional;
    }

    @Override
    public boolean isIgnorable() {
        return isIgnorable;
    }

    public void setIgnorable(boolean isIgnorable) {
        this.isIgnorable = isIgnorable;
    }

    @Override
    public short getType() {
        return org.apache.neethi.Constants.TYPE_ASSERTION;
    }

    @Override
    public boolean equal(PolicyComponent policyComponent) {
        return policyComponent == this;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof AbstractSecurityAssertion)) {
            return false;
        }

        AbstractSecurityAssertion that = (AbstractSecurityAssertion)object;
        if (isOptional != that.isOptional) {
            return false;
        }
        if (isIgnorable != that.isIgnorable) {
            return false;
        }

        if (version != null && !version.equals(that.version)
            || version == null && that.version != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (version != null) {
            result = 31 * result + version.hashCode();
        }
        result = 31 * result + Boolean.hashCode(isOptional);
        result = 31 * result + Boolean.hashCode(isIgnorable);

        return result;
    }

    @Override
    public PolicyComponent normalize() {
        if (normalized == null) {
            Policy policy = new Policy();
            ExactlyOne exactlyOne = new ExactlyOne();
            policy.addPolicyComponent(exactlyOne);

            if (isOptional()) {
                exactlyOne.addPolicyComponent(new All());
            }

            AbstractSecurityAssertion a = clone(null);
            a.normalized = a;
            a.setOptional(false);

            All all = new All();
            all.addPolicyComponent(a);
            exactlyOne.addPolicyComponent(all);

            normalized = policy;
        }
        return normalized;
    }

    public boolean isNormalized() {
        return normalized == this;
    }

    public PolicyComponent normalize(Policy nestedPolicy) {
        if (normalized == null) {
            Policy normalizedNestedPolicy = nestedPolicy.normalize(true);

            Policy policy = new Policy();
            ExactlyOne exactlyOne = new ExactlyOne();
            policy.addPolicyComponent(exactlyOne);

            if (isOptional()) {
                exactlyOne.addPolicyComponent(new All());
            }

            // for all alternatives in normalized nested policy
            Iterator<List<Assertion>> alternatives = normalizedNestedPolicy.getAlternatives();
            while (alternatives.hasNext()) {
                List<Assertion> alternative = alternatives.next();

                Policy ncp = new Policy(nestedPolicy.getPolicyRegistry(), nestedPolicy.getNamespace());
                ExactlyOne nceo = new ExactlyOne();
                ncp.addPolicyComponent(nceo);

                All nca = new All();
                nceo.addPolicyComponent(nca);
                nca.addPolicyComponents(alternative);

                AbstractSecurityAssertion a = clone(ncp);
                a.normalized = a;
                a.setOptional(false);

                All all = new All();
                all.addPolicyComponent(a);
                exactlyOne.addPolicyComponent(all);

            }
            normalized = policy;
        }
        return normalized;
    }

    public SPConstants.SPVersion getVersion() {
        return version;
    }

    public void serialize(XMLStreamWriter writer, Policy nestedPolicy) throws XMLStreamException {
        writer.writeStartElement(getName().getPrefix(), getName().getLocalPart(), getName().getNamespaceURI());
        writer.writeNamespace(getName().getPrefix(), getName().getNamespaceURI());
        if (isOptional()) {
            writer.writeAttribute(Constants.ATTR_WSP,
                                  writer.getNamespaceContext().getNamespaceURI(Constants.ATTR_WSP),
                                  Constants.ATTR_OPTIONAL, "true");
        }
        if (isIgnorable()) {
            writer.writeAttribute(Constants.ATTR_WSP,
                                  writer.getNamespaceContext().getNamespaceURI(Constants.ATTR_WSP),
                                  Constants.ATTR_IGNORABLE, "true");
        }
        nestedPolicy.serialize(writer);
        writer.writeEndElement();
    }

    protected abstract AbstractSecurityAssertion cloneAssertion(Policy nestedPolicy);

    public AbstractSecurityAssertion clone(Policy nestedPolicy) {
        AbstractSecurityAssertion assertion = cloneAssertion(nestedPolicy);
        assertion.setIgnorable(isIgnorable());
        assertion.setOptional(isOptional());
        return assertion;
    }

    public boolean isAsserted(Map<QName, List<AssertionState>> assertionStatesMap) {
        List<AssertionState> assertionStateList = assertionStatesMap.get(getName());
        if (assertionStateList != null) {
            for (AssertionState assertionState : assertionStateList) {
                if (assertionState.getAssertion() == this && !assertionState.isAsserted()) {
                    return false;
                }
            }
        }
        return true;
    }
}
