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

package org.apache.cxf.ws.policy.builder.primitive;

import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.neethi.Assertion;
import org.apache.neethi.builders.xml.XMLPrimitiveAssertionBuilder;

/**
 * 
 */
public class PrimitiveAssertion 
    extends org.apache.neethi.builders.PrimitiveAssertion {

    public PrimitiveAssertion() {
        super();
    }
    
    public PrimitiveAssertion(QName n) {
        super(n, false);
    }
    
    public PrimitiveAssertion(QName n, boolean o) {
        super(n, o);
    }
    public PrimitiveAssertion(QName n, boolean o, boolean i) {
        super(n, o, i);
    }
    public PrimitiveAssertion(QName n, boolean o, boolean i, Map<QName, String> atts) {
        super(n, o, i, atts);
    }
    
    public PrimitiveAssertion(Element element) {
        super(new QName(element.getNamespaceURI(), element.getLocalName()),
              XMLPrimitiveAssertionBuilder.isOptional(element),
              XMLPrimitiveAssertionBuilder.isIgnorable(element));
    }
    
    @Override
    protected Assertion clone(boolean opt) {
        if (opt == this.optional) {
            return this;
        }
        return new PrimitiveAssertion(name, opt, ignorable);
    }

}
