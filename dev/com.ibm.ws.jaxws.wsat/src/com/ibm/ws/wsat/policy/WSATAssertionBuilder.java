/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.policy;

import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.neethi.builders.PolicyContainingPrimitiveAssertion;
import org.apache.neethi.builders.xml.XMLPrimitiveAssertionBuilder;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.wsat.Constants;

/**
 *
 */
public class WSATAssertionBuilder implements AssertionBuilder<Element> {
    private static final TraceComponent tc = Tr.register(WSATAssertionBuilder.class, Constants.TRACE_GROUP, null);

    @Override
    public QName[] getKnownElements() {
        return new QName[] { Constants.AT_ASSERTION_QNAME };
    }

    @Override
    public Assertion build(Element elem, AssertionBuilderFactory factory)
                    throws IllegalArgumentException {

        String localName = elem.getLocalName();
        QName qn = new QName(elem.getNamespaceURI(), localName);

        if (Constants.AT_ASSERTION_QNAME.equals(qn)) {
            Assertion nap = new XMLPrimitiveAssertionBuilder() {
                @Override
                public Assertion newPrimitiveAssertion(Element element, Map<QName, String> mp) {
                    if (isIgnorable(element)) {
                        throw new RuntimeException("WS-AT does not accept Ignorable attribute is TRUE");
                    }
                    return new PrimitiveAssertion(Constants.AT_ASSERTION_QNAME,
                                    isOptional(element), isIgnorable(element), mp);
                }

                @Override
                public Assertion newPolicyContainingAssertion(Element element,
                                                              Map<QName, String> mp,
                                                              Policy policy) {
                    if (isIgnorable(element)) {
                        throw new RuntimeException("WS-AT does not accept Ignorable attribute is TRUE");
                    }
                    return new PolicyContainingPrimitiveAssertion(
                                    Constants.AT_ASSERTION_QNAME,
                                    isOptional(element), isIgnorable(element),
                                    mp,
                                    policy);
                }
            }.build(elem, factory);
            return nap;
        }
        return null;
    }

}
