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

import java.util.List;
import org.w3c.dom.Element;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableCollectionMetadata;
import org.apache.aries.blueprint.mutable.MutablePassThroughMetadata;
import org.apache.cxf.configuration.blueprint.AbstractBPBeanDefinitionParser;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.WSPolicyFeature;
import org.osgi.service.blueprint.reflect.Metadata;

public class PolicyFeatureBPDefinitionParser extends AbstractBPBeanDefinitionParser {

    public Metadata parse(Element element, ParserContext context) {

        MutableCollectionMetadata ps = context.createMetadata(MutableCollectionMetadata.class);
        ps.setCollectionClass(List.class);

        MutableCollectionMetadata prs = context.createMetadata(MutableCollectionMetadata.class);
        ps.setCollectionClass(List.class);

        MutableBeanMetadata cxfBean = context.createMetadata(MutableBeanMetadata.class);
        cxfBean.setRuntimeClass(WSPolicyFeature.class);

        Element elem = DOMUtils.getFirstElement(element);
        while (elem != null) {
            if ("Policy".equals(elem.getLocalName())) {
                ps.addValue(createElement(context, elem));
            } else if ("PolicyReference".equals(elem.getLocalName())) {
                prs.addValue(createElement(context, elem));
            }
            elem = DOMUtils.getNextElement(elem);
        }
        cxfBean.addProperty("policyElements", ps);
        cxfBean.addProperty("policyReferenceElements", prs);

        super.parseChildElements(element, context, cxfBean);

        return cxfBean;
    }

    @Override
    protected void mapElement(ParserContext ctx, MutableBeanMetadata bean, Element el, String name) {
        if ("alternativeSelector".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, name);
        }
    }

    public static Metadata createElement(ParserContext context, Element value) {
        MutablePassThroughMetadata anElement = context.createMetadata(MutablePassThroughMetadata.class);
        anElement.setObject(value);
        return anElement;
    }

}
