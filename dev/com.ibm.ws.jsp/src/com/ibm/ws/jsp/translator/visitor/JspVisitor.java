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
package com.ibm.ws.jsp.translator.visitor;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.translator.JspTranslationException;
import com.ibm.ws.jsp.translator.visitor.configuration.JspVisitorUsage;
import com.ibm.wsspi.jsp.context.JspCoreContext;

public abstract class JspVisitor {
    protected JspVisitorUsage visitorUsage = null;
    protected JspConfiguration jspConfiguration = null;
    protected JspCoreContext context = null;
    protected HashMap resultMap = null;
    protected JspVisitorInputMap inputMap = null;
    protected int visitCount = 0;

	static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.translator.visitor.JspVisitor";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}

    public JspVisitor(JspVisitorUsage visitorUsage,
                      JspConfiguration jspConfiguration, 
                      JspCoreContext context, 
                      HashMap resultMap,
                      JspVisitorInputMap inputMap) 
        throws JspCoreException {
        this.visitorUsage = visitorUsage;
        this.jspConfiguration = jspConfiguration;
        this.context = context;
        this.resultMap = resultMap;
        this.inputMap = inputMap;
    }

    protected abstract void visitJspRootStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspRootEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspTextStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspTextEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitIncludeDirectiveStart(Element jspElement) throws JspCoreException;
    protected abstract void visitIncludeDirectiveEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitPageDirectiveStart(Element jspElement) throws JspCoreException;
    protected abstract void visitPageDirectiveEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitTagDirectiveStart(Element jspElement) throws JspCoreException;
    protected abstract void visitTagDirectiveEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitAttributeDirectiveStart(Element jspElement) throws JspCoreException;
    protected abstract void visitAttributeDirectiveEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitVariableDirectiveStart(Element jspElement) throws JspCoreException;
    protected abstract void visitVariableDirectiveEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspDeclarationStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspDeclarationEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspExpressionStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspExpressionEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspScriptletStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspScriptletEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspParamStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspParamEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspParamsStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspParamsEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspFallbackStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspFallbackEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspIncludeStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspIncludeEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspForwardStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspForwardEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspUseBeanStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspUseBeanEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspGetPropertyStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspGetPropertyEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspSetPropertyStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspSetPropertyEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspPluginStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspPluginEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitCustomTagStart(Element jspElement) throws JspCoreException;
    protected abstract void visitCustomTagEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspAttributeStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspAttributeEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspElementStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspElementEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspBodyStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspBodyEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspInvokeStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspInvokeEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspDoBodyStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspDoBodyEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitJspOutputStart(Element jspElement) throws JspCoreException;
    protected abstract void visitJspOutputEnd(Element jspElement) throws JspCoreException;
    protected abstract void visitUninterpretedTagStart(Element jspElement) throws JspCoreException;
    protected abstract void visitUninterpretedTagEnd(Element jspElement) throws JspCoreException;
    
    protected abstract void visitCDataTag(CDATASection cdata) throws JspCoreException;

    public abstract JspVisitorResult getResult() throws JspCoreException;
    
    // Common method to determine if a particular node defines a tagdependent tag.
    
    protected boolean isElementTagDependent (Element jspElement) throws JspCoreException {
         return false;
    }
    
    protected boolean isNodeTagDependent (Node node) throws JspCoreException {
         if ((node == null) || (node.getNodeType() != Node.ELEMENT_NODE)) {
              return false;
         }
         
         return isElementTagDependent ((Element) node);
    }
    
    // Common method to help us limit the scope of child processing skips to only the ValidateVisitor
    // and the GenerateVisitor.
    
    protected boolean shouldSkipChildrenForThisVisitor () {
         return false;
    }
    
    // This method is used to handle the situation where a tagdependent tag is encountered.
    // We need to avoid having the normal JspVisitor code visit the children and instead have
    // visitCustomTagStart() echo out the children verbatim (or in the case of validation, not
    // validate the children).
    
    protected boolean shouldSkipChildren (Element jspElement) throws JspCoreException {
         if (!shouldSkipChildrenForThisVisitor()) {
              return false;
         }
         
         String uri = jspElement.getNamespaceURI();
         String name = jspElement.getLocalName();
         NodeList children;
         
         // First, see if this is a <jsp:body> tag.
         
         if (uri.equalsIgnoreCase (Constants.JSP_NAMESPACE) && name.equalsIgnoreCase (Constants.JSP_BODY_TYPE)) {
              // It is, so we need to skip processing children if the parent tag is
              // tagdependent.
              
              return isNodeTagDependent (jspElement.getParentNode());
         }
         
         // Otherwise, this is a custom tag.  We have three cases:
         
         if (!isElementTagDependent (jspElement)) {
              // Case 1: not a tagdependent tag, so return.
              
              return false;
         }
         
         // Now, we know this is a tagdependent tag.  Look at the children.
         
         children = jspElement.getChildNodes();
         
         if (children.getLength() == 0) {
              // Case 2: there are no children, so there's no harm in allowing processing to continue.
              
              return false;
         }
         
         for (int i = 0; i < children.getLength(); ++i) {
              Node node = children.item (i);
              
              if (node.getNodeType() == Node.ELEMENT_NODE) {
                   Element element = (Element) node;
                   String elemURI = element.getNamespaceURI();
                   
                   if (elemURI.equalsIgnoreCase (Constants.JSP_NAMESPACE)) {
                        String elemName = element.getLocalName();
                        
                        if (elemName.equalsIgnoreCase (Constants.JSP_ATTRIBUTE_TYPE) ||
                             elemName.equalsIgnoreCase (Constants.JSP_BODY_TYPE)) {
                             // Case 3: the first child is <jsp:attribute> or <jsp:body>, so we
                             // have to process children.
                             
                             return false;
                        }
                        
                        else {
                             // Default case: the first child is not <jsp:attribute> or <jsp:body>,
                             // so we have an implicit body defined.  Skip processing.
                             
                             return true;
                        }
                   }
              }
         }
         
         // Not likely we'd make it here...
              
         return false;
    }
    
    public void visit(Document jspDocument, int visitCount) throws JspCoreException {
        this.visitCount = visitCount;

        NodeList jspNodes = jspDocument.getChildNodes();
        for (int i = 0; i < jspNodes.getLength(); i++) {
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
    			logger.logp(Level.FINEST, CLASS_NAME, "visit","jspNodes.item(i) =[" + jspNodes.item(i) +"]");
    		}
            Node jspNode = jspNodes.item(i);
            processJspElement(jspNode);
        }
    }

    private void processJspElement(Node jspNode)
        throws JspCoreException {

        if (jspNode.getNodeType() == Node.ELEMENT_NODE) {
            Element jspElement = (Element)jspNode;
            String namespaceURI = jspElement.getNamespaceURI();            
            String jspElementType = jspElement.getLocalName();
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
    			logger.logp(Level.FINEST, CLASS_NAME, "processJspElement","jspElement =[" + jspElement +"]  namespaceURI= ["+namespaceURI+"] jspElementType= [ "+jspElementType+"]");
    		}
            
            if (namespaceURI != null && namespaceURI.equals(Constants.JSP_NAMESPACE)) {
                if (jspElementType.equals(Constants.JSP_ROOT_TYPE)) {
                    visitJspRootStart(jspElement);
                    processChildren(jspElement);
                    visitJspRootEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_TEXT_TYPE)) {
                    visitJspTextStart(jspElement);
                    processChildren(jspElement);
                    visitJspTextEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_INCLUDE_DIRECTIVE_TYPE)) {
                    visitIncludeDirectiveStart(jspElement);
                    processChildren(jspElement);
                    visitIncludeDirectiveEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_PAGE_DIRECTIVE_TYPE)) {
                    visitPageDirectiveStart(jspElement);
                    processChildren(jspElement);
                    visitPageDirectiveEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_TAG_DIRECTIVE_TYPE)) {
                    visitTagDirectiveStart(jspElement);
                    processChildren(jspElement);
                    visitTagDirectiveEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_ATTRIBUTE_DIRECTIVE_TYPE)) {
                    visitAttributeDirectiveStart(jspElement);
                    processChildren(jspElement);
                    visitAttributeDirectiveEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_VARIABLE_DIRECTIVE_TYPE)) {
                    visitVariableDirectiveStart(jspElement);
                    processChildren(jspElement);
                    visitVariableDirectiveEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_DECLARATION_TYPE)) {
                    visitJspDeclarationStart(jspElement);
                    processChildren(jspElement);
                    visitJspDeclarationEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_EXPRESSION_TYPE)) {
                    visitJspExpressionStart(jspElement);
                    processChildren(jspElement);
                    visitJspExpressionEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_SCRIPTLET_TYPE)) {
                    visitJspScriptletStart(jspElement);
                    processChildren(jspElement);
                    visitJspScriptletEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_PARAM_TYPE)) {
                    visitJspParamStart(jspElement);
                    processChildren(jspElement);
                    visitJspParamEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_PARAMS_TYPE)) {
                    visitJspParamsStart(jspElement);
                    processChildren(jspElement);
                    visitJspParamsEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_FALLBACK_TYPE)) {
                    visitJspFallbackStart(jspElement);
                    processChildren(jspElement);
                    visitJspFallbackEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_INCLUDE_TYPE)) {
                    visitJspIncludeStart(jspElement);
                    processChildren(jspElement);
                    visitJspIncludeEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_FORWARD_TYPE)) {
                    visitJspForwardStart(jspElement);
                    processChildren(jspElement);
                    visitJspForwardEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_USEBEAN_TYPE)) {
                    visitJspUseBeanStart(jspElement);
                    processChildren(jspElement);
                    visitJspUseBeanEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_GETPROPERTY_TYPE)) {
                    visitJspGetPropertyStart(jspElement);
                    processChildren(jspElement);
                    visitJspGetPropertyEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_SETPROPERTY_TYPE)) {
                    visitJspSetPropertyStart(jspElement);
                    processChildren(jspElement);
                    visitJspSetPropertyEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_PLUGIN_TYPE)) {
                    visitJspPluginStart(jspElement);
                    processChildren(jspElement);
                    visitJspPluginEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                    visitJspAttributeStart(jspElement);
                    processChildren(jspElement);
                    visitJspAttributeEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_ELEMENT_TYPE)) {
                    visitJspElementStart(jspElement);
                    processChildren(jspElement);
                    visitJspElementEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_BODY_TYPE)) {
                    visitJspBodyStart(jspElement);
                    
                    if (!shouldSkipChildren (jspElement)) {
                         processChildren(jspElement);
                    }
                    
                    visitJspBodyEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_INVOKE_TYPE)) {
                    visitJspInvokeStart(jspElement);
                    processChildren(jspElement);
                    visitJspInvokeEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_DOBODY_TYPE)) {
                    visitJspDoBodyStart(jspElement);
                    processChildren(jspElement);
                    visitJspDoBodyEnd(jspElement);
                }
                else if (jspElementType.equals(Constants.JSP_OUTPUT_TYPE)) {
                    visitJspOutputStart(jspElement);
                    processChildren(jspElement);
                    visitJspOutputEnd(jspElement);
                }
                else {
                    throw new JspTranslationException(jspElement, "jsp.error.element.unknown", new Object[] { jspElement.getTagName() });
                } 
            }
            else if (jspElement.getTagName().indexOf(':') != -1) {
                visitCustomTagStart(jspElement);
                
                if (!shouldSkipChildren (jspElement)) {
                     processChildren(jspElement);
                }
                
                visitCustomTagEnd(jspElement);
            }
            else {
        		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
        			logger.logp(Level.FINEST, CLASS_NAME, "processJspElement","About to call visitUninterpretedTagStart(jspElement), jspElement =[" + jspElement +"]");
        		}
                visitUninterpretedTagStart(jspElement);
        		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
        			logger.logp(Level.FINEST, CLASS_NAME, "processJspElement","About to call processChildren(jspElement), jspElement =[" + jspElement +"]");
        		}
                processChildren(jspElement);
        		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
        			logger.logp(Level.FINEST, CLASS_NAME, "processJspElement","About to call visitUninterpretedTagEnd(jspElement), jspElement =[" + jspElement +"]");
        		}
                visitUninterpretedTagEnd(jspElement);
            }
        }
        else if (jspNode.getNodeType() == Node.CDATA_SECTION_NODE) {
            CDATASection cdata = (CDATASection)jspNode;
            Node parentNode = jspNode.getParentNode();
            
            if (parentNode.getNamespaceURI() != null && parentNode.getNamespaceURI().equals(Constants.JSP_NAMESPACE)) {
                 if (parentNode.getLocalName().equals(Constants.JSP_TEXT_TYPE) ||
                     parentNode.getLocalName().equals(Constants.JSP_EXPRESSION_TYPE) ||  
                     parentNode.getLocalName().equals(Constants.JSP_DECLARATION_TYPE) ||  
                     parentNode.getLocalName().equals(Constants.JSP_SCRIPTLET_TYPE)) {
                      
                     if (isNodeTagDependent (parentNode)) {
                          visitCDataTag(cdata);
                     }
                 }
                 
                 else {
                      visitCDataTag (cdata);
                 }
            }
            else {
                visitCDataTag(cdata);
            }
        }
    }

    private void processChildren(Node jspNode)
        throws JspCoreException {
        NodeList childJspNodes = jspNode.getChildNodes();
        for (int i = 0; i < childJspNodes.getLength(); i++) {
            Node n = childJspNodes.item(i);
            processJspElement(n);
        }
    }
}

