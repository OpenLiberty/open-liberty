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
package com.ibm.ws.jsp.translator.visitor.validator;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.translator.JspTranslationException;
import com.ibm.ws.jsp.translator.utils.JspId;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.configuration.JspVisitorUsage;
import com.ibm.wsspi.jsp.context.JspCoreContext;

public class ValidateJspVisitor extends ValidateVisitor {  
    static private Logger logger;
    private static final String CLASS_NAME="com.ibm.ws.jsp.translator.visitor.validator.ValidateJspVisitor";
    static{
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }
    
    private HashMap userDefinedDirectives = new HashMap();
    private Boolean autoFlushValue = null;
    private String bufferValue = null;

    public ValidateJspVisitor(JspVisitorUsage visitorUsage, 
                              JspConfiguration jspConfiguration, 
                              JspCoreContext context, 
                              HashMap resultMap, 
                              JspVisitorInputMap inputMap)
        throws JspCoreException {
        super(visitorUsage, jspConfiguration, context, resultMap, inputMap);
        result = new ValidateJspResult(visitorUsage.getJspVisitorDefinition().getId());
        //JSP2.1MR2
        if (jspConfiguration.getBuffer()!=null) {
        	//((ValidateJspResult)result).setBufferSize(jspConfiguration.getBuffer());
        	setBufferValue((ValidateJspResult)result, null, jspConfiguration.getBuffer());
        }
        //JSP2.1MR2
        if (jspConfiguration.getDefaultContentType()!=null) {
        	((ValidateJspResult)result).setContentType(jspConfiguration.getDefaultContentType());
        }
        //JSP2.1MR2
        if (jspConfiguration.isErrorOnUndeclaredNamespace()) {
        	
        }
    }

    protected void visitJspUseBeanStart(Element jspElement) throws JspCoreException {
        super.visitJspUseBeanStart(jspElement);
        ValidateJspResult jspResult = (ValidateJspResult) result;
        NamedNodeMap attributes = jspElement.getAttributes();
        if (attributes != null) {
            Attr scopeAttr = jspElement.getAttributeNode("scope");
            if (scopeAttr != null && scopeAttr.getValue().equalsIgnoreCase("session")) {
                if (jspResult.isGenSessionVariable() != true)
                    throw new JspTranslationException(jspElement, "jsp.error.usebean.prohibited.as.session");
            }
        }
        else {
            throw new JspTranslationException(jspElement, "jsp.error.usebean.contains.no.attributes");
        }
    }

    private void setBufferValue(ValidateJspResult jspResult, Element jspElement, String bufferValue) throws JspCoreException {
        if (bufferValue.equalsIgnoreCase("none"))
            jspResult.setBufferSize(0);
        else {
            /*
             *  May want to revisit the handling of buffersize
             *  JSP Specification section JSP.1.10.1 The page directive
             *  Table JSP1-8 Page Directive Attributes: explicitly 
             *  states that "The size can only be specified in kilobytes
             *  The suffix kb is mandatory or a translation error must occur"
             *  The current version (same as jasper) does not care if suffix
             *  kb exists.  Change not made to prevent possible regression 
             *  when customers move to current spec level.
            */
            String servletVersion = jspConfiguration.getServletVersion();
            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
                logger.logp(Level.FINEST, CLASS_NAME, "visitPageDirectiveStart","ServletVersion=" + servletVersion);
            }
            Integer integer = null;
            try {
                String num;
                //check the servlet version so as not to break backward compatibility 
                if (servletVersion.equals("2.3") || servletVersion.equals("2.4")) {
                    int ind = bufferValue.indexOf("k");
                    if (ind == -1)
                        num = bufferValue;
                    else
                        num = bufferValue.substring(0, ind);
                } else { //servletVersion must be 2.5
                    int ind = bufferValue.indexOf("kb");
                    if (ind == -1) {
                    	if (jspElement==null) {
                    		throw new JspTranslationException("jsp.error.invalid.value.for.buffer");
                    	} else {
                    		throw new JspTranslationException(jspElement, "jsp.error.invalid.value.for.buffer");
                    	}
                    }
                    else
                        num = bufferValue.substring(0, ind);
                }
                integer = new Integer(num);
                if (integer<0) {
                	if (jspElement==null) {
                		throw new JspTranslationException("jsp.error.page.invalid.buffer");
                	} else {
                		throw new JspTranslationException(jspElement, "jsp.error.page.invalid.buffer");
                	}
                }
            }
            catch (NumberFormatException n) {
            	if (jspElement==null) {
            		throw new JspTranslationException("jsp.error.page.invalid.buffer");
            	} else {
            		throw new JspTranslationException(jspElement, "jsp.error.page.invalid.buffer");
            	}
            }
            jspResult.setBufferSize(integer.intValue() * 1024);
        }
    }
    
    protected void visitPageDirectiveStart(Element jspElement) throws JspCoreException {
        ValidateJspResult jspResult = (ValidateJspResult) result;
        NamedNodeMap attributes = jspElement.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                String directiveName = attribute.getNodeName();
                String directiveValue = attribute.getNodeValue();

                Object oldDirectiveValue = addUserPageDirective(directiveName, directiveValue);
                if (oldDirectiveValue != null
                    && (directiveName.equals("import") == false) // import can appear more than once in translation unit.
                    && (directiveName.equals("pageEncoding") == false) // pageEncoding can appear more than once in translation unit (only once per page or fragment).
                    && (directiveName.equals("jsp:id") == false)) {
                    if (oldDirectiveValue.equals(directiveValue) == false) {                            
                        throw new JspTranslationException(jspElement, "jsp.error.multiple.occurrences.directive", new Object[] { directiveName, oldDirectiveValue, directiveValue });
                    }
                }

                boolean valid = false;
                if (directiveName.equals("language")) {
                    if (directiveValue != null && directiveValue.equals("java") == false) {
                        throw new JspTranslationException(jspElement, "jsp.error.invalid.value.for.language", new Object[] { directiveValue });
                    }
                    else {
                        valid = true;
                        jspResult.setLanguage(directiveValue);
                    }
                }
                else if (directiveName.equals("extends")) {
                    valid = true;
                    jspResult.setExtendsClass(directiveValue);
                }
                else if (directiveName.equals("import")) {
                    valid = true;
                }
                else if (directiveName.equals("session")) {
                    valid = true;
                    if (directiveValue.equalsIgnoreCase("true"))
                        jspResult.setGenSessionVariable(true);
                    else if (directiveValue.equalsIgnoreCase("false"))
                        jspResult.setGenSessionVariable(false);
                    else
                        throw new JspTranslationException(jspElement, "jsp.error.invalid.value.for.session");
                }
                else if (directiveName.equals("buffer")) {
                    valid = true;
                    setBufferValue(jspResult, jspElement, directiveValue);
                    bufferValue = directiveValue;
                    if (autoFlushValue != null) {
                        if (bufferValue.equalsIgnoreCase("none") && autoFlushValue.booleanValue() == false) {
                            throw new JspTranslationException(jspElement, "jsp.error.page.invalid.autoflush.buffer");
                        }
                    }
                }
                else if (directiveName.equals("autoFlush")) {
                    valid = true;
                    if (directiveValue.equalsIgnoreCase("true")) {
                        jspResult.setAutoFlush(true);
                        autoFlushValue = new Boolean(true);
                    }
                    else if (directiveValue.equalsIgnoreCase("false")) {
                        jspResult.setAutoFlush(false);
                        autoFlushValue = new Boolean(false);
                    }
                    else
                        throw new JspTranslationException(jspElement, "jsp.error.page.invalid.autoflush");
                    if (bufferValue != null) {
                        if (bufferValue.equalsIgnoreCase("none") && autoFlushValue.booleanValue() == false) {
                            throw new JspTranslationException(jspElement, "jsp.error.page.invalid.autoflush.buffer");
                        }
                    }
                }
                else if (directiveName.equals("isThreadSafe")) {
                    valid = true;
                    if (directiveValue.equalsIgnoreCase("true"))
                        jspResult.setSingleThreaded(false);
                    else if (directiveValue.equalsIgnoreCase("false"))
                        jspResult.setSingleThreaded(true);
                    else
                        throw new JspTranslationException(jspElement, "jsp.error.page.invalid.threadsafe");
                }
                else if (directiveName.equals("info")) {
                    valid = true;
                    jspResult.setInfo(directiveValue);
                }
                else if (directiveName.equals("errorPage")) {
                    valid = true;
                    jspResult.setError(directiveValue);
                }
                else if (directiveName.equals("isErrorPage")) {
                    valid = true;
                    if (directiveValue.equalsIgnoreCase("true"))
                        jspResult.setIsErrorPage(true);
                    else if (directiveValue.equalsIgnoreCase("false"))
                        jspResult.setIsErrorPage(false);
                    else
                        throw new JspTranslationException(jspElement, "jsp.error.page.invalid.iserrorpage");
                }
                else if (directiveName.equals("contentType")) {
                    valid = true;
                    jspResult.setContentType(directiveValue);
                }
                else if (directiveName.equals("pageEncoding")) {
                    valid = true;

                    JspId jspId = new JspId(jspElement.getAttribute("jsp:id"));

                    String configPageEncoding = jspConfiguration.getResponseEncoding();
                    if(configPageEncoding == null){
                    	configPageEncoding = jspConfiguration.getPageEncoding();
                    }
                    String convertedPageEncoding = com.ibm.wsspi.webcontainer.util.EncodingUtils.getJvmConverter(directiveValue);
                    if (configPageEncoding != null) {
                        if (convertedPageEncoding.toUpperCase().startsWith("UTF-16")) {
                            if (configPageEncoding.toUpperCase().startsWith("UTF-16") == false) {
                                throw new JspTranslationException(jspElement, "jsp.error.page.pageencoding.mismatch", new Object[] { convertedPageEncoding, configPageEncoding });
                            }else if(configPageEncoding.equalsIgnoreCase("UTF-16LE") ||configPageEncoding.equalsIgnoreCase("UTF-16BE")){
                            	convertedPageEncoding = configPageEncoding;
                            }
                        }
                        else if (convertedPageEncoding.equalsIgnoreCase(configPageEncoding) == false) {
                            throw new JspTranslationException(jspElement, "jsp.error.page.pageencoding.mismatch", new Object[] { convertedPageEncoding, configPageEncoding });
                        }
                    }
                    jspResult.setPageEncoding(convertedPageEncoding);
                }
                else if (directiveName.equals("isELIgnored")) {
                    valid = true;
                    if (directiveValue.equalsIgnoreCase("true")) {
                        jspResult.setIsELIgnored(true);
                        jspConfiguration.setElIgnored(true);
                        jspConfiguration.setElIgnoredSetTrueInPage(true);
                    }
                    else if (directiveValue.equalsIgnoreCase("false")) {
                        jspResult.setIsELIgnored(false);
                        jspConfiguration.setElIgnored(false);
                    }
                    else
                        throw new JspTranslationException(jspElement, "jsp.error.page.invalid.iselignored");
                }
                else if (directiveName.equals("jsp:id")) {
                    valid = true;
                }
                else if (directiveName.startsWith("xmlns")) {
                    valid = true;
                }
                // jsp2.1work
                else if (directiveName.equals("trimDirectiveWhitespaces")) {
                    valid = true;
                    if (jspConfiguration.getTrimDirectiveWhitespaces() == null) {
	                    if (directiveValue.equalsIgnoreCase("true")) {
	                        jspResult.setTrimDirectiveWhitespaces(true);
	                        jspConfiguration.setTrimDirectiveWhitespaces(true);
	                    }
	                    else if (directiveValue.equalsIgnoreCase("false")) {
	                        jspResult.setTrimDirectiveWhitespaces(false);
	                        jspConfiguration.setTrimDirectiveWhitespaces(false);
	                    }
	                    else {
	                        throw new JspTranslationException(jspElement, "jsp.error.page.invalid.trimdirectivewhitespaces");
	                    }
                    }
                    else if (!jspConfiguration.getTrimDirectiveWhitespaces().equals(directiveValue)) {
                    	 throw new JspTranslationException(jspElement, "jsp.error.page.conflict.trimdirectivewhitespaces", new Object[] {jspConfiguration.getTrimDirectiveWhitespaces(), directiveName });                    
                    }
                }
                // jsp2.1ELwork
                else if (directiveName.equals("deferredSyntaxAllowedAsLiteral")) {
                    valid = true;
                    if (jspConfiguration.getDeferredSyntaxAllowedAsLiteral() == null) {
	                    if (directiveValue.equalsIgnoreCase("true")) {
	                        jspResult.setDeferredSyntaxAllowedAsLiteral(true);
	                        jspConfiguration.setDeferredSyntaxAllowedAsLiteral(true);
	                    }
	                    else if (directiveValue.equalsIgnoreCase("false")) {
	                        jspResult.setDeferredSyntaxAllowedAsLiteral(false);
	                        jspConfiguration.setDeferredSyntaxAllowedAsLiteral(false);
	                    }
	                    else {
	                    	throw new JspTranslationException(jspElement, "jsp.error.page.invalid.deferredsyntaxallowedasliteral", new Object[] {jspConfiguration.getDeferredSyntaxAllowedAsLiteral(), directiveName });                    
	                    }
                    }
                    else if (!jspConfiguration.getDeferredSyntaxAllowedAsLiteral().equals(directiveValue)) {
	                        throw new JspTranslationException(jspElement, "jsp.error.page.conflict.deferredsyntaxallowedasliteral");
                    }
                }
                if (valid == false) {
                    throw new JspTranslationException(jspElement, "jsp.error.page.directive.unknown", new Object[] { directiveName });
                }
            }
        }
        else {
            throw new JspTranslationException(jspElement, "jsp.error.page.directive.contains.no.attributes");
        }
    }

    protected void visitJspInvokeStart(Element jspElement) throws JspCoreException {
        throw new JspTranslationException(jspElement, "jsp.error.invoke.only.in.tagfiles");
    }

    protected void visitJspDoBodyStart(Element jspElement) throws JspCoreException {
        throw new JspTranslationException(jspElement, "jsp.error.dobody.only.in.tagfiles");
    }

    protected void visitTagDirectiveStart(Element jspElement) throws JspCoreException {
        throw new JspTranslationException(jspElement, "jsp.error.tag.directive.only.in.tagfiles");
    }

    protected void visitAttributeDirectiveStart(Element jspElement) throws JspCoreException {
        throw new JspTranslationException(jspElement, "jsp.error.attribute.directive.only.in.tagfiles");
    }

    protected void visitVariableDirectiveStart(Element jspElement) throws JspCoreException {
        throw new JspTranslationException(jspElement, "jsp.error.variable.directive.only.in.tagfiles");
    }

    protected Object addUserPageDirective(String directiveName, String directiveValue) {
        return userDefinedDirectives.put(directiveName, directiveValue);
    }
}
