/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.visitor.generator;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;

public abstract class PageTranslationTimeGenerator extends CodeGeneratorBase {
    protected String[] translationTimeAttrs = null;
    
    public PageTranslationTimeGenerator(String[] translationTimeAttrs) {
        this.translationTimeAttrs = translationTimeAttrs;
    }
    
    public JavaCodeWriter getWriterForChild(int section, Node childNode) throws JspCoreException {
        JavaCodeWriter writerForChild = null;
        if (section == CodeGenerationPhase.METHOD_SECTION) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element)childNode;
                if (childElement.getNamespaceURI().equals(Constants.JSP_NAMESPACE) && 
                    childElement.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                    String jspAttributeName = childElement.getAttribute("name");
                    if (jspAttributeName.indexOf(':') != -1) {
                        jspAttributeName = jspAttributeName.substring(jspAttributeName.indexOf(':') + 1);
                    }
                    for (int i = 0; i < translationTimeAttrs.length; i++) {
                        if (jspAttributeName.equals(translationTimeAttrs[i])) {
                            /* if there is a match for thsi jsp:attribute then create a dummy writer */
                            /* Page Translation-time attributes don't generate any code. The Parent */
                            /* Generator will handle retrieving the value specified in the jsp:attribute */
                            writerForChild = new MethodWriter();
                            break;
                        }
                    }
                }
            }
        }
        
        return writerForChild;
    }
    
    protected String getAttributeValue(String attributeName) {
        Attr attr = element.getAttributeNode(attributeName);
        String attributeValue = null;
        
        if (attr == null) {
            NodeList nl = element.getChildNodes();
            for (int i =0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element)n;
                    if (e.getNamespaceURI().equals(Constants.JSP_NAMESPACE) && 
                        e.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                        String name = e.getAttribute("name");
                        if (name.indexOf(':') != -1) {
                            name = name.substring(name.indexOf(':') + 1);
                        }
                        if (name.equals(attributeName)) {
                            Node attrChildNode = e.getFirstChild();
                            
                            CDATASection cdata = null;
                            if (attrChildNode.getNodeType() == Node.CDATA_SECTION_NODE) {
                                cdata = (CDATASection)attrChildNode;
                            }
                            else if (attrChildNode instanceof Element && 
                                     attrChildNode.getNamespaceURI().equals(Constants.JSP_NAMESPACE) &&
                                     attrChildNode.getLocalName().equals(Constants.JSP_TEXT_TYPE)) {
                                Element jspElement = (Element)attrChildNode;                            
                                cdata = (CDATASection) jspElement.getFirstChild();
                            }
                
                            attributeValue = cdata.getData();
                            if (e.getAttribute("trim").equals("false") == false)
                                attributeValue = attributeValue.trim();
                        }
                    }
                }
            }   
        }
        else {
            attributeValue = attr.getValue();
        }
        if (attributeValue != null){
        	attributeValue = attributeValue.replaceAll("&gt;", ">");
        	attributeValue = attributeValue.replaceAll("&lt;", "<");
        	attributeValue = attributeValue.replaceAll("&amp;", "&");
        	attributeValue = attributeValue.replaceAll("<\\%", "<%");
        	attributeValue = attributeValue.replaceAll("%\\>", "%>");
        }
        return attributeValue;
    }
}
