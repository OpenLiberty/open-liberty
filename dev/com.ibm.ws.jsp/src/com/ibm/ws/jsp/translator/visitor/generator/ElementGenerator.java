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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.translator.utils.JspTranslatorUtil;

public class ElementGenerator extends CodeGeneratorBase {
    private MethodWriter attributesWriter = new MethodWriter();
    private MethodWriter bodyWriter = new MethodWriter();
    
    public void startGeneration(int section, JavaCodeWriter writer) throws JspCoreException {}

    public void endGeneration(int section, JavaCodeWriter writer)  throws JspCoreException {
        
        if (section == CodeGenerationPhase.METHOD_SECTION) {
            String nameValue = null;
            HashMap map = new HashMap();
            
            //PK65013 - start
            String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
            if (isTagFile && jspOptions.isModifyPageContextVariable()) {
            	pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
            }
            //PK65013 - end
            
            Node attr = element.getAttributeNode("name");
            nameValue = " + " + GeneratorUtils.attributeValue(attr.getNodeValue(), false, String.class, jspConfiguration, isTagFile, pageContextVar); //PK65013

            writeDebugStartBegin(writer);
            HashMap jspAttributes = (HashMap)persistentData.get("jspAttributes");
            if (jspAttributes != null) {
                ArrayList jspAttributeList = (ArrayList)jspAttributes.get(element);
                if (jspAttributeList != null) {
                    writer.printMultiLn(attributesWriter.toString());

                    for (Iterator itr = jspAttributeList.iterator(); itr.hasNext();) {
                        AttributeGenerator.JspAttribute jspAttribute = (AttributeGenerator.JspAttribute)itr.next();
                        String s = null;
                        String attrName = jspAttribute.getName();
                        if (jspAttribute.getPrefix() != null) {
                            attrName = jspAttribute.getPrefix() + ":" + attrName; 
                        }
                        s = " + \" " + attrName + "=\\\"\" + " + jspAttribute.getVarName() + " + \"\\\"\"";
                        map.put(jspAttribute, s);
                    }
                }
            }
            
            writer.print("out.write(\"<\"");
            writer.print(nameValue);
            writer.println(");");
            
            for (Iterator itr = map.keySet().iterator(); itr.hasNext();) {
            	AttributeGenerator.JspAttribute thisAttribute = (AttributeGenerator.JspAttribute)itr.next();
                //JSP2.1MR2 - F000743-2571.1 need to test if omit attribute is set and not print out attribute if it is - this could be a runtime expression
            	if (thisAttribute.isOmitSet()) {
            		writer.print("if (!");
            		String expression = thisAttribute.getOmit();
                    String stringResult;
                    if (JspTranslatorUtil.isExpression(expression) || JspTranslatorUtil.isELInterpreterInput(expression, jspConfiguration)) {
                        boolean _ENCODE=false; // same as above
                        stringResult=GeneratorUtils.attributeValue(expression, _ENCODE, Boolean.class,
                                jspConfiguration, isTagFile, pageContextVar);
                    } else {
                        stringResult=(Boolean.valueOf(expression)).toString();
                    }
                    writer.print(stringResult);
            		writer.println(") {");
            	}
            	String attrName = (String) thisAttribute.getName();
                writer.print("out.write(\"\"");
                writer.print((String) map.get(thisAttribute));
                writer.println(");");
            	if (thisAttribute.isOmitSet()) {
            		writer.println("}");
            	}
            }
            writer.print("out.write(\"\""); //start the out.write up again

            // Does the <jsp:element> have nested tags other than
            // <jsp:attribute>
            boolean hasBody = false;
            NodeList childNodes = element.getChildNodes();
            boolean hasJspAttributes = false;
            boolean hasJspBody = false;
            
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node child = childNodes.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElement = (Element)child;
                    if (childElement.getNamespaceURI().equals(Constants.JSP_NAMESPACE)) {
                        if (childElement.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                            hasJspAttributes = true;
                        }
                        else if (childElement.getLocalName().equals(Constants.JSP_BODY_TYPE)) {
                            hasJspBody = true;
                        }
                        else {
                            hasBody = true;
                        }
                    }
                    else {
                        hasBody = true;
                    }
                }
                else if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
                    hasBody = true;
                }
            }
            if (hasJspAttributes && hasJspBody == false)
                hasBody = false;
            if (hasBody || hasJspBody) {
                writer.print(" + \">\");");
                writer.println();
                writeDebugStartEnd(writer);

                writer.printMultiLn(bodyWriter.toString());

                writeDebugEndBegin(writer);
                writer.print("out.write(\"</\"");
                writer.print(nameValue);
                writer.print(" + \">\");");
                writer.println();
                writeDebugEndEnd(writer);
            }
            else {
                writer.print(" + \"/>\");");
                writer.println();
                writeDebugStartEnd(writer);
            }
        }
    }
    
    public JavaCodeWriter getWriterForChild(int section, Node childElement) throws JspCoreException {
        JavaCodeWriter writerForChild = null;
        if (section == CodeGenerationPhase.METHOD_SECTION) {
            if (childElement.getNodeType() == Node.ELEMENT_NODE) {
                if (childElement.getNamespaceURI().equals(Constants.JSP_NAMESPACE) && 
                    childElement.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                    writerForChild = attributesWriter;
                }
                else {
                    writerForChild = bodyWriter;
                }
            }
            else {
                writerForChild = bodyWriter;
            }
        }
        
        return writerForChild;
    }
}
