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
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.wss4j.policy.SP13Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.SPUtils;
import org.apache.wss4j.policy.model.Header;
import org.apache.wss4j.policy.model.RequiredParts;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

public class RequiredPartsBuilder implements AssertionBuilder<Element> {

    @Override
    public Assertion build(Element element, AssertionBuilderFactory factory) throws IllegalArgumentException {

        final SPConstants.SPVersion spVersion = SPConstants.SPVersion.getSPVersion(element.getNamespaceURI());

        final List<Header> headers = getHeaders(element, false, spVersion);
        RequiredParts requiredParts = new RequiredParts(spVersion, headers);
        requiredParts.setOptional(SPUtils.isOptional(element));
        requiredParts.setIgnorable(SPUtils.isIgnorable(element));
        return requiredParts;
    }

    protected List<Header> getHeaders(Element element, boolean ignoreNameElement, SPConstants.SPVersion spVersion) {
        List<Header> headers = new ArrayList<>();
        Element child = SPUtils.getFirstChildElement(element);
        while (child != null) {
            if (SPConstants.HEADER.equals(child.getLocalName())
                    && spVersion.getNamespace().equals(child.getNamespaceURI())) {
                String headerName = child.getAttributeNS(null, SPConstants.NAME);
                if (headerName.length() == 0) {
                    if (ignoreNameElement) {
                        headerName = null;
                    } else {
                        throw new IllegalArgumentException("sp:" + element.getLocalName()
                            + "/sp:" + child.getLocalName() + " must have a Name attribute");
                    }
                }
                String headerNamespace = child.getAttributeNS(null, SPConstants.NAMESPACE);
                if (headerNamespace == null || headerNamespace.length() == 0) {
                    throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                }
                headers.add(new Header(headerName, headerNamespace));
            }
            child = SPUtils.getNextSiblingElement(child);
        }
        return headers;
    }

    @Override
    public QName[] getKnownElements() {
        return new QName[]{SP13Constants.REQUIRED_PARTS};
    }
}
