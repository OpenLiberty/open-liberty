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
package org.apache.wss4j.policy.builders;

import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP13Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.SPUtils;
import org.apache.wss4j.policy.model.SymmetricBinding;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

public class SymmetricBindingBuilder implements AssertionBuilder<Element> {

    @Override
    public Assertion build(Element element, AssertionBuilderFactory factory) throws IllegalArgumentException {

        final SPConstants.SPVersion spVersion = SPConstants.SPVersion.getSPVersion(element.getNamespaceURI());
        final Element nestedPolicyElement = SPUtils.getFirstPolicyChildElement(element);
        if (nestedPolicyElement == null) {
            throw new IllegalArgumentException("sp:SymmetricBinding must have an inner wsp:Policy element");
        }
        final Policy nestedPolicy = factory.getPolicyEngine().getPolicy(nestedPolicyElement);
        SymmetricBinding symmetricBinding = new SymmetricBinding(
                spVersion,
                nestedPolicy
        );
        symmetricBinding.setOptional(SPUtils.isOptional(element));
        symmetricBinding.setIgnorable(SPUtils.isIgnorable(element));
        return symmetricBinding;
    }

    @Override
    public QName[] getKnownElements() {
        return new QName[]{SP13Constants.SYMMETRIC_BINDING, SP11Constants.SYMMETRIC_BINDING};
    }
}
