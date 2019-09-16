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

package com.ibm.ws.jaxws.policy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.policy.attachment.external.DomainExpression;
import org.apache.cxf.ws.policy.attachment.external.DomainExpressionBuilder;
import org.w3c.dom.Element;

public class URIDomainExpressionBuilder implements DomainExpressionBuilder {
    private static final Collection<QName> SUPPORTED_TYPES = Collections.unmodifiableList(
                    Arrays.asList(new QName[] {
                                               new QName("http://www.w3.org/ns/ws-policy", "URI"),
                                               new QName("http://schemas.xmlsoap.org/ws/2004/09/policy", "URI")
                    }));

    @Override
    public DomainExpression build(Element paramElement) {
        return new URIDomainExpression(paramElement.getTextContent());
    }

    @Override
    public Collection<QName> getDomainExpressionTypes() {
        return SUPPORTED_TYPES;
    }
}
