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

package org.apache.neethi;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Element;

import org.apache.neethi.builders.AssertionBuilder;

/**
 * PolicyBuilder provides set of methods to create a Policy object from an
 * InputStream, Element, XMLStreamReader, OMElement, etc.. It maintains an instance of
 * AssertionBuilderFactory that can return AssertionBuilders that can create a
 * Domain Assertion out of an element. These AssertionBuilders are used when
 * constructing a Policy object.
 */
public class PolicyBuilder {

    public static final String MAX_DEPTH_PROPERTY = "org.apache.neethi.parser.maxDepth";
    public static final String MAX_ELEMENTS_PROPERTY = "org.apache.neethi.parser.maxElements";
    public static final String MAX_ATTRIBUTES_PROPERTY = "org.apache.neethi.parser.maxAttributes";

    private static final int DEFAULT_MAX_DEPTH = 256;
    private static final int DEFAULT_MAX_ELEMENTS = 100000;
    private static final int DEFAULT_MAX_ATTRIBUTES = 10000;

    protected AssertionBuilderFactory factory;
    protected PolicyRegistry defaultPolicyRegistry;
    private final int maxDepth;
    private final int maxElements;
    private final int maxAttributes;
    
    public PolicyBuilder() {
        factory = new AssertionBuilderFactoryImpl(this);
        maxDepth = readConfiguredLimit(MAX_DEPTH_PROPERTY, DEFAULT_MAX_DEPTH);
        maxElements = readConfiguredLimit(MAX_ELEMENTS_PROPERTY, DEFAULT_MAX_ELEMENTS);
        maxAttributes = readConfiguredLimit(MAX_ATTRIBUTES_PROPERTY, DEFAULT_MAX_ATTRIBUTES);
    }
    
    public PolicyBuilder(AssertionBuilderFactory factory) {
        this.factory = factory;
        maxDepth = readConfiguredLimit(MAX_DEPTH_PROPERTY, DEFAULT_MAX_DEPTH);
        maxElements = readConfiguredLimit(MAX_ELEMENTS_PROPERTY, DEFAULT_MAX_ELEMENTS);
        maxAttributes = readConfiguredLimit(MAX_ATTRIBUTES_PROPERTY, DEFAULT_MAX_ATTRIBUTES);
    }
    
    
    /**
     * Registers an AssertionBuilder instances and associates it with a QName.
     * PolicyManager or other AssertionBuilders instances can use this
     * AssertionBuilder instance to process and build an Assertion from a
     * element with the specified QName.
     * 
     * @param qname
     *            the QName of the Assertion that the Builder can build
     * @param builder
     *            the AssertionBuilder that can build assertions that of 'qname'
     *            type
     */
    public void registerBuilder(QName qname, AssertionBuilder<?> builder) {
        factory.registerBuilder(qname, builder);
    }
    
    
    /**
     * The PolicyEngine can have a default PolicyRegistry that the Policy objects
     * that it creates are setup to use when normalize is called without the 
     * PolicyRegistry.   
     * @return the default PolicyRegistry
     */
    public PolicyRegistry getPolicyRegistry() {
        return defaultPolicyRegistry;
    }

    public void setPolicyRegistry(PolicyRegistry reg) {
        defaultPolicyRegistry = reg;
    }
    
    public AssertionBuilderFactory getAssertionBuilderFactory() {
        return factory;
    }

    /**
     * Creates a Policy object from an InputStream.
     * 
     * @param inputStream
     *            the InputStream of the Policy
     * @return a Policy object of the Policy that is fed as a InputStream
     */
    public Policy getPolicy(InputStream inputStream) {
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            XMLStreamReader reader = xif.createXMLStreamReader(inputStream);
            return getPolicy(reader);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Could not load policy.", ex); 
        }
    }

    public Policy getPolicy(Element el) {
        ParseBudgetContext context = new ParseBudgetContext(maxDepth, maxElements, maxAttributes);
        return getPolicyOperator(el, context, 1);
    }
    
    
    public Policy getPolicy(XMLStreamReader reader) {
        ParseBudgetContext context = new ParseBudgetContext(maxDepth, maxElements, maxAttributes);
        return getPolicyOperator(reader, context, 1);
    }

    /**
     * Creates a Policy object from an element.
     * 
     * @param element
     *            the Policy element
     * @return a Policy object of the Policy element
     */
    public Policy getPolicy(Object element) {
        ParseBudgetContext context = new ParseBudgetContext(maxDepth, maxElements, maxAttributes);
        return getPolicyOperator(element, context, 1);
    }

    /**
     * Creates a PolicyReference object.
     * 
     * @param inputStream
     *            the InputStream of the PolicyReference
     * @return a PolicyReference object of the PolicyReference
     */
    public PolicyReference getPolicyReference(InputStream inputStream) {
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            XMLStreamReader reader = xif.createXMLStreamReader(inputStream);
            return getPolicyReference(reader);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Could not load policy reference.", ex); 
        }
    }

    /**
     * Creates a PolicyReference object from an element.
     * 
     * @param element
     *            the PolicyReference element
     * @return a PolicyReference object of the PolicyReference element
     */
    public PolicyReference getPolicyReference(Object element) {
        ParseBudgetContext context = new ParseBudgetContext(maxDepth, maxElements, maxAttributes);
        return getPolicyReference(element, context, 1);
    }

    private PolicyReference getPolicyReference(Object element, ParseBudgetContext context, int depth) {
        context.checkDepth(depth);
        context.incrementElementCount();

        QName qn = factory.getConverterRegistry().findQName(element);

        if (!Constants.isPolicyRef(qn)) {
            throw new RuntimeException(
                    "Specified element is not a <wsp:PolicyReference .. />  element");
        }

        PolicyReference reference = new PolicyReference(this);

        Map<QName, String> attributes = factory.getConverterRegistry().getAttributes(element);
        context.incrementAttributeCount(attributes.size());

        // setting the URI value
        reference.setURI(attributes.get(new QName("URI")));
        return reference;
    }

    private Policy getPolicyOperator(Object element, ParseBudgetContext context, int depth) {
        context.checkDepth(depth);
        context.incrementElementCount();

        QName qn = factory.getConverterRegistry().findQName(element);
        
        if (Constants.isPolicyElement(qn)) {
            String ns = qn.getNamespaceURI();
            return (Policy) processOperationElement(element, new Policy(defaultPolicyRegistry, ns), context, depth);
        }
        throw new IllegalArgumentException(qn + " is not a <wsp:Policy> element."); 
    }

    private ExactlyOne getExactlyOneOperator(Object element, ParseBudgetContext context, int depth) {
        context.checkDepth(depth);
        context.incrementElementCount();
        return (ExactlyOne) processOperationElement(element, new ExactlyOne(), context, depth);
    }

    private All getAllOperator(Object element, ParseBudgetContext context, int depth) {
        context.checkDepth(depth);
        context.incrementElementCount();
        return (All) processOperationElement(element, new All(), context, depth);
    }

    private PolicyOperator processOperationElement(Object operationElement,
                                                   PolicyOperator operator,
                                                   ParseBudgetContext context,
                                                   int depth) {

        if (Constants.TYPE_POLICY == operator.getType()) {
            Policy policyOperator = (Policy) operator;

            Map<QName, String> attributes = factory.getConverterRegistry().getAttributes(operationElement);
            context.incrementAttributeCount(attributes.size());
            
            for (Map.Entry<QName, String> ent : attributes.entrySet()) {
                policyOperator.addAttribute(ent.getKey(), ent.getValue());
            }
        }

        for (Iterator<?> iterator = factory.getConverterRegistry().getChildElements(operationElement); 
            iterator.hasNext();) {
            
            Object childElement = iterator.next();
            QName qn = factory.getConverterRegistry().findQName(childElement);
            
            if (childElement == null || qn == null 
                || qn.getNamespaceURI() == null) {
                
                notifyUnknownPolicyElement(childElement);
                
            } else if (Constants.isInPolicyNS(qn)) {
                if (Constants.ELEM_POLICY.equals(qn.getLocalPart())) {
                    operator.addPolicyComponent(getPolicyOperator(childElement, context, depth + 1));
                } else if (Constants.ELEM_EXACTLYONE.equals(qn.getLocalPart())) {
                    operator.addPolicyComponent(getExactlyOneOperator(childElement, context, depth + 1));
                } else if (Constants.ELEM_ALL.equals(qn.getLocalPart())) {
                    operator.addPolicyComponent(getAllOperator(childElement, context, depth + 1));
                } else if (Constants.ELEM_POLICY_REF.equals(qn.getLocalPart())) {
                    operator.addPolicyComponent(getPolicyReference(childElement, context, depth + 1));
                } else {
                    operator.addPolicyComponent(factory.build(childElement));
                }
            } else {
                operator.addPolicyComponent(factory.build(childElement));
            }
        }
        return operator;
    } 

    private static int readConfiguredLimit(String key, int defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.trim().length() == 0) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static final class ParseBudgetContext {
        private final int maxDepth;
        private final int maxElements;
        private final int maxAttributes;
        private int elementCount;
        private int attributeCount;

        ParseBudgetContext(int maxDepth, int maxElements, int maxAttributes) {
            this.maxDepth = maxDepth;
            this.maxElements = maxElements;
            this.maxAttributes = maxAttributes;
        }

        void checkDepth(int depth) {
            if (depth > maxDepth) {
                throw new RuntimeException(
                    "Policy parsing exceeded the maximum policy nesting depth ("
                    + maxDepth + ").");
            }
        }

        void incrementElementCount() {
            elementCount++;
            if (elementCount > maxElements) {
                throw new RuntimeException(
                    "Policy parsing exceeded the maximum number of elements ("
                    + maxElements + ").");
            }
        }

        void incrementAttributeCount(int delta) {
            if (delta <= 0) {
                return;
            }
            attributeCount += delta;
            if (attributeCount > maxAttributes) {
                throw new RuntimeException(
                    "Policy parsing exceeded the maximum number of attributes ("
                    + maxAttributes + ").");
            }
        }
    }
    
    protected void notifyUnknownPolicyElement(Object childElement) {
        //NO-Op - subclass could log or throw exception or something
    }
}
