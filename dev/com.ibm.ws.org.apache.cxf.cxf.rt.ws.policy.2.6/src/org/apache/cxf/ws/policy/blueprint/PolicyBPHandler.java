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

package org.apache.cxf.ws.policy.blueprint;

import java.net.URL;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;

public class PolicyBPHandler implements NamespaceHandler {

    public URL getSchemaLocation(String s) {
        //Say yes to various schemas.
        String location = null;
        if ("http://cxf.apache.org/policy".equals(s)) {
            location = "schemas/policy.xsd";
        } else if ("http://www.w3.org/ns/ws-policy".equals(s)) {
            location = "schemas/ws-policy-200702.xsd";
        } else if ("http://www.w3.org/2006/07/ws-policy".equals(s)) {
            location = "schemas/ws-policy-200607.xsd";
        } else if ("http://schemas.xmlsoap.org/ws/2004/09/policy".equals(s)) {
            location = "schemas/ws-policy-200409.xsd";
        } else if ("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd".equals(s)) {
            location = "schemas/oasis-200401-wss-wssecurity-secext-1.0.xsd";
        } else if ("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd".equals(s)) {
            location = "schemas/oasis-200401-wss-wssecurity-utility-1.0.xsd";
        } else if ("http://www.w3.org/2000/09/xmldsig#".equals(s)) {
            location = "schemas/xmldsig-core-schema.xsd";
        } else if ("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702".equals(s)) {
            location = "schemas/ws-securitypolicy-1.2.xsd";
        }
        if (location != null) {
            return getClass().getClassLoader().getResource(location);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public Set<Class> getManagedClasses() {
        return null;
    }

    public Metadata parse(Element element, ParserContext context) {

        String s = element.getLocalName();
        if ("policies".equals(s)) {
            return new PolicyFeatureBPDefinitionParser().parse(element, context);
        } else if ("engine".equals(s)) {
            return new PolicyEngineBPDefinitionParser().parse(element, context);
        } else if ("externalAttachment".equals(s)) {
            return new ExternalAttachmentProviderBPDefinitionParser().parse(element, context);
        }

        return null;
    }

    public ComponentMetadata decorate(Node node, ComponentMetadata componentMetadata, ParserContext context) {
        return null;
    }
}
