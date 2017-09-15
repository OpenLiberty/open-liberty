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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.translator.utils.JspTranslatorUtil;

public class AttributeGenerator extends CodeGeneratorBase {
    private JspAttribute jspAttribute = null;
    private boolean trim = true;
    private String omit=null;
    private boolean omitSet=false;
    private boolean isLiteral = false;

	static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.translator.visitor.generator.AttributeGenerator";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}
    
    public void startGeneration(int section, JavaCodeWriter writer) throws JspCoreException {
        // 232157.1
        if (section == CodeGenerationPhase.CLASS_SECTION) {
            if (element.getAttribute("omit").equals("") == false) {
            	omitSet = true;
            	omit = element.getAttribute("omit");
            }
            if (element.getAttribute("trim").equals("") == false) {
                trim = Boolean.valueOf(element.getAttribute("trim")).booleanValue();
            }
            HashMap jspAttributes = (HashMap)persistentData.get("jspAttributes");
            if (jspAttributes == null) {
                jspAttributes = new HashMap();
                persistentData.put("jspAttributes", jspAttributes);
            }
            ArrayList jspAttributeList = (ArrayList)jspAttributes.get(element.getParentNode());
            if (jspAttributeList == null) {
                jspAttributeList = new ArrayList();
                jspAttributes.put(element.getParentNode(), jspAttributeList);
            }

            String name = element.getAttribute("name");
            String prefix = null;

            if (name.indexOf(':') != -1) {
                prefix = name.substring(0, name.indexOf(':'));
                name = name.substring(name.indexOf(':') + 1);
            }

            jspAttribute = new JspAttribute(name, prefix, null, element, isLiteral, trim, omit, omitSet);
            jspAttributeList.add(jspAttribute);
        }
        else if (section == CodeGenerationPhase.METHOD_SECTION) {
            String varName = null; // 232157.1
            if (writer instanceof FragmentHelperClassWriter.FragmentWriter) {
                //No need to do anything as the parent element is a Custom Tag and this attribute is flagged as a Fragment
            }
            else if (writer instanceof NamedAttributeWriter) {
                NamedAttributeWriter attributeWriter = (NamedAttributeWriter)writer;
                varName = attributeWriter.getVarName();
                jspAttribute.setVarName(varName);
                generateAttributeStart(attributeWriter, varName);
            }
            else {
                varName = GeneratorUtils.nextTemporaryVariableName(persistentData);
                jspAttribute.setVarName(varName);
                generateAttributeStart(writer, varName);
            }
            // 232157.1
            jspAttribute.setIsLiteral(isLiteral);
        }
    }

    public void endGeneration(int section, JavaCodeWriter writer)  throws JspCoreException {
        if (section == CodeGenerationPhase.METHOD_SECTION) {
            if (writer instanceof FragmentHelperClassWriter.FragmentWriter) {
                FragmentHelperClassWriter.FragmentWriter fragmentWriter = (FragmentHelperClassWriter.FragmentWriter)writer;
                if (persistentData.get("methodNesting") == null) {
                    persistentData.put("methodNesting", new Integer(0));
                }
                int methodNesting =  ((Integer)persistentData.get("methodNesting")).intValue();
                fragmentHelperClassWriter.closeFragment(fragmentWriter, methodNesting);
            }
            else if (writer instanceof NamedAttributeWriter) {
                NamedAttributeWriter attributeWriter = (NamedAttributeWriter)writer;
                generateAttributeEnd(attributeWriter, attributeWriter.getVarName());
            }
            else {
                generateAttributeEnd(writer, jspAttribute.getVarName());
            }
        }
    }
    
    private void generateAttributeStart(JavaCodeWriter writer, String varName) {
        //PK65013 start
        String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
        if (isTagFile) {
            if (jspOptions.isModifyPageContextVariable()) {
                pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
            }
        }
        //PK65013 end
        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
			logger.logp(Level.FINEST, CLASS_NAME, "generateAttributeStart","writer =[" + writer +"]");
			logger.logp(Level.FINEST, CLASS_NAME, "generateAttributeStart","varName =[" + varName +"]");
            logger.logp(Level.FINEST, CLASS_NAME, "generateAttributeStart","pageContextVar =[" + pageContextVar +"]");
		}
        writeDebugStartBegin(writer);
        if (element.hasChildNodes()) {
            Node attrChildNode = element.getFirstChild();
            if (element.getChildNodes().getLength() == 1) { 
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
                else {
                    /* throw exception or should this be handled by validator ? */
                }
                
                if(cdata != null) { //PK34107
	                String value = cdata.getData();
	        		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
	        			logger.logp(Level.FINEST, CLASS_NAME, "generateAttributeStart","about to call isELInterpreterInput value =[" + value +"]");
	        		}
	                if (JspTranslatorUtil.isELInterpreterInput(value, jspConfiguration) == false) {
	                    isLiteral = true;
	            		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
	            			logger.logp(Level.FINEST, CLASS_NAME, "generateAttributeStart","after call to isELInterpreterInput isLiteral =[" + isLiteral +"]");
	            		}
                        //PK68493 start
                        // example:  <jsp:attribute name="test">    </jsp:attribute>
                        if (trim && value != null && value.trim().equals("")) {
                            writer.println("String " + varName + " = \"\";");                       
                        }
                        //PK68493 end
	                }
	                else {
                        //PK65013 change pageContext variable to customizable one.
                        writer.println("out = "+pageContextVar+".pushBody();");
	            		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
	            			logger.logp(Level.FINEST, CLASS_NAME, "generateAttributeStart","after call to isELInterpreterInput wrote pushBody() line.");
	            		}
	                }                    
				}//PK34107
				else {
                    //PK65013 change pageContext variable to customizable one.
                    writer.println("out = "+pageContextVar+".pushBody();");
				} //PK34107
            }
            else {
                //PK65013 change pageContext variable to customizable one.
                writer.println("out = "+pageContextVar+".pushBody();");
            }
        }
        else {
            writer.println("String " + varName + " = \"\";");
        }
        writeDebugStartEnd(writer);
    }
    
    private void generateAttributeEnd(JavaCodeWriter writer, String varName) {
        //PK65013 start
        String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
        if (isTagFile) {
            if (jspOptions.isModifyPageContextVariable()) {
                pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
            }
        }
        //PK65013 end
        if (element.hasChildNodes()) {
            if (isLiteral == false) {
                writeDebugEndBegin(writer);                    
                writer.println("String " + varName + " = " + "((javax.servlet.jsp.tagext.BodyContent)" + "out).getString();");
                writer.println("out = "+pageContextVar+".popBody();"); //PK65013
                writeDebugEndEnd(writer);
            }
        }
    }
    
    public class JspAttribute {
        private String name = null;
        private String prefix = null;
        private String varName = null;
        private Element jspAttrElement = null;
        private boolean isLiteral = false;
        private boolean trim = true;
        private String omit = null;
        private boolean omitSet = false;
        
        public JspAttribute(String name, String prefix, String varName, Element jspAttrElement, boolean isLiteral, boolean trim, String omit, boolean omitSet) {
            this.name = name;
            this.prefix = prefix;
            this.varName = varName;
            this.jspAttrElement = jspAttrElement;
            this.isLiteral = isLiteral;
            this.trim = trim;
            this.omit = omit;
            this.omitSet = omitSet;
        }

        public String getName() {
            return name;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getVarName() {
            return varName;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public void setVarName(String varName) {
            this.varName = varName;
        }

        public Element getJspAttrElement() {
            return jspAttrElement;
        }

        public void setJspAttrElement(Element jspAttrElement) {
            this.jspAttrElement = jspAttrElement;
        }
        
        public boolean isLiteral() {
            return isLiteral;
        }

        public void setIsLiteral(boolean isLiteral) {
            this.isLiteral = isLiteral;
        }

        public boolean trim() {
            return trim;
        }

        public void setTrim(boolean trim) {
            this.trim = trim;
        }
        
        public String getOmit() {
            return omit;
        }
        
        public void setOmit(String omit) {
            this.omit = omit;
        }
        
        public boolean isOmitSet() {
            return omitSet;
        }

        public void setOmitSet(boolean omitSet) {
            this.omitSet = omitSet;
        }

    }
}
