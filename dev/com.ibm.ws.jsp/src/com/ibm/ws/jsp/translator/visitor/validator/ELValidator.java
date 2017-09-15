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

// Created for feature LIDB4147-9 "Integrate Unified Expression Language"  2006/08/14  Scott  Johnson

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;

import org.apache.jasper.compiler.ELNode;
import org.apache.jasper.compiler.ELParser;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.taglib.TagLibraryInfoImpl;
import com.ibm.ws.jsp.translator.JspTranslationException;
import com.ibm.ws.jsp.translator.utils.JspTranslatorUtil;
import com.ibm.ws.jsp.webcontainerext.JSPExtensionFactory;
import com.ibm.wsspi.jsp.context.JspCoreContext;

public class ELValidator { 

	static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.translator.visitor.validator.ELValidator";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}
    protected static void validateELExpression(Element jspElement, String expression, Class expectedType, JspConfiguration jspConfiguration, ValidateResult result, JspCoreContext context, HashMap prefixToUriMap) throws JspCoreException {
        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
            logger.logp(Level.FINER, CLASS_NAME, "validateELExpression 1","expression= ["+expression+"]");
            logger.logp(Level.FINER, CLASS_NAME, "validateELExpression 1","jspConfiguration.elIgnored() =[" + jspConfiguration.elIgnored() +"]");
        }
        if (JspTranslatorUtil.isELInterpreterInput(expression, jspConfiguration)) {
            try {
                String uri;
            	int index = expression.indexOf("#{");
            	// see if #{ is allowed as literal
                if (index!=-1) {
                    uri = jspElement.getNamespaceURI();
                    if (!jspConfiguration.isDeferredSyntaxAllowedAsLiteral()&& uri == null) {
                        //throw new JspTranslationException(jspElement, "jsp.error.el.template.deferred", new Object[] { expression });
                        throw new JspTranslationException(jspElement, "jsp.error.el.template.deferred", new Object[] { "#{" });                    	
                    }
                    else if (!jspConfiguration.isDeferredSyntaxAllowedAsLiteral()&& uri != null) {
                        String prefix;
                        String tagName;
	                    prefix = jspElement.getPrefix();
	                    tagName = jspElement.getLocalName();
	                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
	                        logger.logp(Level.FINER, CLASS_NAME, "validateELExpression ","uri= ["+uri+"]");
	                        logger.logp(Level.FINER, CLASS_NAME, "validateELExpression ","prefix =[" + prefix +"]");
	                        logger.logp(Level.FINER, CLASS_NAME, "validateELExpression ","tagName =[" + tagName +"]");
	                    }
	                    if (uri.startsWith("urn:jsptld:")) {
	                        uri = uri.substring(uri.indexOf("urn:jsptld:") + 11);
	                    }
	                    else if (uri.startsWith("urn:jsptagdir:")) {
	                        uri = uri.substring(uri.indexOf("urn:jsptagdir:") + 14);
	                    }
	                    TagLibraryInfoImpl tli = (TagLibraryInfoImpl) result.getTagLibMap().get(uri);
	                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
	                        logger.logp(Level.FINER, CLASS_NAME, "validateELExpression ","uri 2= ["+uri+"]");
	                        logger.logp(Level.FINER, CLASS_NAME, "validateELExpression ","tli =[" + tli +"]");
	                        if (tli!=null) {
	                            logger.logp(Level.FINER, CLASS_NAME, "validateELExpression ","tli.getRequiredVersion() =[" + tli.getRequiredVersion() +"]");
	                        	
	                        }
	                    }
	                	
                        if (tli == null) { 
                            //514120 removed case when tli version is at 2.1 because this is valid
                            throw new JspTranslationException(jspElement, "jsp.error.el.template.deferred", new Object[] { "#{" });                    	
                        }
                        //PM16142 start - remove else statement as valid deferred expressions with functions are not being fully parsed / validated
                        //else {
                        	//return;
                        //}
                        //PM16142 end
                    }
                }               
            	
                String exprStr = expression;
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                    logger.logp(Level.FINER, CLASS_NAME, "validateELExpression 1","exprStr 1= ["+exprStr+"]");
                }
                exprStr = exprStr.replaceAll("&gt;", ">");
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                    logger.logp(Level.FINER, CLASS_NAME, "validateELExpression 1","exprStr 2= ["+exprStr+"]");
                }
                exprStr = exprStr.replaceAll("&lt;", "<");
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                    logger.logp(Level.FINER, CLASS_NAME, "validateELExpression 1","exprStr 3= ["+exprStr+"]");
                }
                exprStr = exprStr.replaceAll("&amp;", "&");
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                    logger.logp(Level.FINER, CLASS_NAME, "validateELExpression 1","exprStr 4= ["+exprStr+"]");
                    logger.logp(Level.FINER, CLASS_NAME, "validateELExpression 1","jspElement= ["+jspElement+"]");
                }
                ELValidator.validateElFunction(jspElement, exprStr, result, context.getJspClassloaderContext().getClassLoader(), jspConfiguration, prefixToUriMap);
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                    logger.logp(Level.FINER, CLASS_NAME, "validateELExpression 1","exprStr 5= ["+exprStr+"]");
                    logger.logp(Level.FINER, CLASS_NAME, "validateELExpression 1","jspElement= ["+jspElement+"]");
                }
                //expressionEvaluator.parseExpression(exprStr, expectedType, result.getValidateFunctionMapper());
                ELNode.Nodes el = ELParser.parse(exprStr);

                // validate/prepare expression
                prepareExpression(el, jspElement, exprStr, result, context.getJspClassloaderContext().getClassLoader(), jspConfiguration, prefixToUriMap);
            }
            catch (javax.el.ELException ele) {
                throw new JspTranslationException(jspElement, "failed.to.parse.el.expression", new Object[] { expression }, ele);
            }
        }
    }

    protected static void validateELExpression(Element jspElement, String expression, String expectedType, JspConfiguration jspConfiguration, JspCoreContext context, ValidateResult result, HashMap prefixToUriMap) throws JspCoreException {
        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
            logger.logp(Level.FINER, CLASS_NAME, "validateELExpression 2","expression= ["+expression+"]");
            logger.logp(Level.FINER, CLASS_NAME, "validateELExpression 2","expectedType= ["+expectedType+"]");
            logger.logp(Level.FINER, CLASS_NAME, "validateELExpression 2","jspConfiguration.elIgnored() =[" + jspConfiguration.elIgnored() +"]");
        }
        if (JspTranslatorUtil.isELInterpreterInput(expression, jspConfiguration)) {
            try {
                //Class clazz = Class.forName(expectedType, true, context.getClassLoader());
                Class clazz = JspTranslatorUtil.toClass(expectedType, context.getJspClassloaderContext().getClassLoader());
                validateELExpression(jspElement, expression, clazz, jspConfiguration, result, context, prefixToUriMap);
            }
            catch (ClassNotFoundException cnfe) {
                throw new JspTranslationException(jspElement, "failed.to.parse.el.expression", new Object[] { expression }, cnfe);
            }
        }
    }

    protected static String[] getELExpressions(String data, JspConfiguration jspConfiguration, boolean evalQuotedAndEscapedExpression) {
        String[] elExpressions = null;
        List elList = new ArrayList();

        boolean inElExpression = false;
        char[] chars = data.toCharArray();
        StringBuffer sb = null;

        boolean inQuotes = false;//PK53233, flag to determine if we are in double or single quotes 
        boolean usingSingleQuotes = false;//PK53233, flag to determine if we are in single quotes
        boolean inEscape = false;//PK53233, flag to determine if we are in an escape

        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            switch (ch) {
            //PK53233 start
            case '\'' : {
                if (inElExpression && evalQuotedAndEscapedExpression) {//, if we are in expression language text
                    if (inQuotes && usingSingleQuotes && !inEscape) {
                        inQuotes = false;//if we were previously in quotes we now are not
                        usingSingleQuotes = false;
                    }
                    else {
                        inQuotes = true;//if we weren't previously in quotes we now are
                        usingSingleQuotes = true;
                    }
                }
                    
                    if (inElExpression){
                        sb.append((char)ch);
                    }
                    inEscape = false;//have been through 1 + iterations after an escape could have been found, so setting to false
                break;
            }
            case '"' : {
                if (inElExpression) {
                        if (evalQuotedAndEscapedExpression) {//if we are in expression language text
                    if (inQuotes && !usingSingleQuotes && !inEscape) {
                        inQuotes = false;
                    }
                    else {
                        inQuotes = true;
                    }

                            if (inEscape) {
                                sb.append((char)'\\');
                            }
                            else {
                                sb.append((char)ch);//if we are in the element language place an escaped double quote on the output array.  Why don't we do this for single quotes?
                            }
                    } else {
                            sb.append((char)ch);
                        }
                    }
                    inEscape = false;
                    break;
            }
            case '\\' : {//if we have a backslash
                if (evalQuotedAndEscapedExpression) {
                    int nextChar = ' ';
                    if (i+1 < chars.length) {//get the next character in the array
                        nextChar = chars[i+1];
                    }
                    int prevChar = ' ';//get the previous character in the array
                    if (i-1 >= 0) {
                        prevChar = chars[i-1];
                    }
            
                    if (inEscape) {
                        inEscape = false;//toggle inEscape
                    }
                    else {
                        if (nextChar == '\'' || nextChar == '\"' || nextChar == '\\' || nextChar == '$') {//we are assuming these are the only characters that we need to escape.  Is this true for the element language as well?
                            inEscape = true;
                        }
                    }
            
                    if (inElExpression && !inEscape) {
                        //means the \ character itself was escaped
                        sb.append((char)'\\');
                        sb.append((char)'\\');
                    }
                } else {
                        if (inElExpression) {
                            sb.append((char)'\\');
                        }
                    }
                break;
            }
            //PK53233 end
            case '$' :
                case '#' :
                    {
                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                        logger.logp(Level.FINER, CLASS_NAME, "getELExpressions","case '$' or '#'");
                        logger.logp(Level.FINER, CLASS_NAME, "getELExpressions","jspConfiguration.elIgnored() =[" + jspConfiguration.elIgnored() +"]");
                    }
                        if (jspConfiguration.elIgnored() == false && (i + 1 < chars.length)) {
                            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                                logger.logp(Level.FINER, CLASS_NAME, "getELExpressions","i =[" + i+"]");
                                logger.logp(Level.FINER, CLASS_NAME, "getELExpressions","chars[i + 1] == '{' =[" + (chars[i + 1] == '{') +"]");
                            }
                            if (chars[i + 1] == '{') {
                                char prevChar = ' ';
                                if (i > 0)
                                    prevChar = chars[i - 1];
                                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                                    logger.logp(Level.FINER, CLASS_NAME, "getELExpressions","prevChar =[" + prevChar+"]");
                                }
                                if (evalQuotedAndEscapedExpression) {
                                    if (!inQuotes && !inEscape) {
                                        if (prevChar != '\\' && sb==null) { //PK53233 this character is not escaped and we're not already in an ElExpression.  (Can't have nested expressions.)
                                            sb = new StringBuffer();
                                            sb.append((char) ch);
                                            inElExpression = true;
                                        }
                                        //PK53233 start
                                        else {
                                            if (sb!=null) {
                                                sb.append((char)ch);
                                            }
                                        }
                                    } else {
                                        if (inElExpression)
                                            sb.append((char)ch);
                                    }
                                    //PK53233 end
                                }
                                else if (!evalQuotedAndEscapedExpression) {  //PK53233
                                    if (prevChar != '\'' && prevChar != '\\') {
                                        sb = new StringBuffer();
                                        sb.append((char) ch);
                                        inElExpression = true;
                                    }
                                }
                            }
                            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                                logger.logp(Level.FINER, CLASS_NAME, "getELExpressions","1 inElExpression =[" + inElExpression+"]");
                            }
                        }
                        inEscape = false;  //PK53233
                        break;
                    }

                case '}' :
                    {
                        //PK53233 start
                        if (evalQuotedAndEscapedExpression) {
                            if (!inQuotes) {
                                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                                    logger.logp(Level.FINER, CLASS_NAME, "getELExpressions","case '}'");
                                    logger.logp(Level.FINER, CLASS_NAME, "getELExpressions","jspConfiguration.elIgnored() =[" + jspConfiguration.elIgnored() +"]");
                                }
                                if (jspConfiguration.elIgnored() == false) {
                                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                                        logger.logp(Level.FINER, CLASS_NAME, "getELExpressions","2 inElExpression =[" + inElExpression+"]");
                                    }
                                    if (inElExpression) {
                                        sb.append((char) ch);
                                        elList.add(sb.toString());
                                        inElExpression = false;
                                        sb = null;
                                    }
                                    if(logger.isLoggable(Level.FINER)){
                                        logger.logp(Level.FINER, CLASS_NAME, "getELExpressions","3 inElExpression =[" + inElExpression+"]");
                                    }
                                }
                            } else {
                               //we were quoted and thus we're in an expression; sb is not null
                                sb.append((char)ch);
                            }
                        } else {
                        //PK53233 end
                            if (inElExpression) {
                                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                                    //PM85512 need to make sure we don't hit an ArrayIndexOutOfBoundsException
                                    logger.logp(Level.FINER, CLASS_NAME, "getELExpressions","i =[" + i + "]");
                                    if (i > 3) {
                                        logger.logp(Level.FINER, CLASS_NAME, "getELExpressions","chars[i - 1] =[" + chars[i - 1]+"]");
                                        logger.logp(Level.FINER, CLASS_NAME, "getELExpressions","chars[i - 2] =[" + chars[i - 2]+"]");
                                        logger.logp(Level.FINER, CLASS_NAME, "getELExpressions","chars[i - 3] =[" + chars[i - 3]+"]");
                                        logger.logp(Level.FINER, CLASS_NAME, "getELExpressions","chars[i - 4] =[" + chars[i - 4]+"]");
                                    }
                                }
                                if ((i > 3) && chars[i - 1] == '\'' && chars[i - 2] == '{' && (chars[i - 3] == '$' || chars[i - 3] == '#') && chars[i - 4] == '\'') {
                                    sb.append((char) ch);
                                }
                                else {
                                    sb.append((char) ch);
                                    elList.add(sb.toString());
                                    inElExpression = false;
                                    sb = null;
                                }
                            }
                        }
                        inEscape = false; //PK53233
                        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                            logger.logp(Level.FINER, CLASS_NAME, "getELExpressions","3 inElExpression =[" + inElExpression+"]");
                        }
                        break;
                    }

                default :
                    {
                        if (sb != null) {
                            sb.append((char) ch);
                        }
                        inEscape = false;  //PK53233
                        break;
                    }
            }
        }

        if (elList.size() > 0) {
            elExpressions = new String[elList.size()];
            elExpressions = (String[]) elList.toArray(elExpressions);
        }

        return elExpressions;
    }

    protected static void validateRuntimeExpressions(Element jspElement, String[] validRuntimeAttrs, Class expectedType, JspConfiguration jspConfiguration, ValidateResult result, JspCoreContext context, HashMap prefixToUriMap) throws JspCoreException {
        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
            logger.logp(Level.FINER, CLASS_NAME, "validateRuntimeExpressions","Entry");
            logger.logp(Level.FINER, CLASS_NAME, "validateRuntimeExpressions","jspElement =[" + jspElement+"]");
            logger.logp(Level.FINER, CLASS_NAME, "validateRuntimeExpressions","validRuntimeAttrs =[" + validRuntimeAttrs+"]");
            logger.logp(Level.FINER, CLASS_NAME, "validateRuntimeExpressions","expectedType =[" + expectedType+"]");
        }

        NamedNodeMap attrs = jspElement.getAttributes();

        // first check tag for possible runtime expressions
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            if (attr.getNodeName().equals("jsp:id"))
                continue;
            String value = attr.getNodeValue();
            boolean valid = false;
            for (int j = 0; j < validRuntimeAttrs.length; j++) {
                if (attr.getNodeName().equals(validRuntimeAttrs[j])) {

                    validateExpressionForScripts(jspElement, value, jspConfiguration);
                    valid = true;
                    validateELExpression(jspElement, value, expectedType, jspConfiguration, result, context, prefixToUriMap);
                    break;
                }
            }
            if (valid == false) {
                if (JspTranslatorUtil.isExpression(value)) {
                    throw new JspTranslationException(
                        jspElement,
                        "rt.expression.not.allowed.for.attribute",
                        new Object[] { jspElement.getTagName(), attr.getNodeName(), value });
                }

                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                    logger.logp(Level.FINER, CLASS_NAME, "validateRuntimeExpressions","About to check for isELInterpreterInput 1");
                }
                if (JspTranslatorUtil.isELInterpreterInput(value, jspConfiguration)) {
                    throw new JspTranslationException(
                        jspElement,
                        "el.expression.not.allowed.for.attribute",
                        new Object[] { jspElement.getTagName(), attr.getNodeName(), value });
                }
            }
        }

        // then check jsp:attribute tag for possible runtime expressions
        if (validRuntimeAttrs != null) {
            NodeList children = jspElement.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child instanceof Element) {
                    Element childElement = (Element) child;
                    if (childElement.getNamespaceURI() != null
                        && childElement.getNamespaceURI().equals(Constants.JSP_NAMESPACE)
                        && childElement.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                        String name = childElement.getAttribute("name");
                        if (name.indexOf(":") != -1) {
                            name = name.substring(name.indexOf(':')+1);
                        }
                        if (name.equals("") == false) {
                            boolean valid = false;
                            String value = childElement.getAttribute("value");
                            for (int j = 0; j < validRuntimeAttrs.length; j++) {
                                if (name.equals(validRuntimeAttrs[j])) {
                                    valid = true;
                                    validateExpressionForScripts(jspElement, value, jspConfiguration);
                                    validateELExpression(jspElement, value, expectedType, jspConfiguration, result, context, prefixToUriMap);
                                    break;
                                }
                            }
                            if (valid == false) {
                                if (JspTranslatorUtil.isExpression(value)) {
                                    throw new JspTranslationException(
                                        jspElement,
                                        "rt.expression.not.allowed.for.attribute",
                                        new Object[] { jspElement.getTagName(), child.getNodeName(), value });
                                }
                                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                                    logger.logp(Level.FINER, CLASS_NAME, "validateRuntimeExpressions","About to check for isELInterpreterInput 2");
                                }
                                if (JspTranslatorUtil.isELInterpreterInput(value, jspConfiguration)) {
                                    throw new JspTranslationException(
                                        jspElement,
                                        "el.expression.not.allowed.for.attribute",
                                        new Object[] { jspElement.getTagName(), child.getNodeName(), value });
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private static void validateElFunction(Element jspElement, String expression, ValidateResult result, ClassLoader loader, JspConfiguration jspConfiguration, HashMap prefixToUriMap ) throws JspCoreException {
        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
            logger.logp(Level.FINER, CLASS_NAME, "validateElFunction","jspElement= ["+jspElement+"]");
            logger.logp(Level.FINER, CLASS_NAME, "validateElFunction","expression= ["+expression+"]");
        }
        ELNode.Nodes el = ELParser.parse(expression);
        
        boolean deferred = false;
        Iterator<ELNode> nodes = el.iterator();
        while (nodes.hasNext()) {
            ELNode node = nodes.next();
            if (node instanceof ELNode.Root) {
                if (((ELNode.Root) node).getType() == '#') {
                    deferred = true;
                }
            }
        }

        if (el.containsEL() && !jspConfiguration.elIgnored()
                && ((!jspConfiguration.isDeferredSyntaxAllowedAsLiteral() && deferred)
                        || !deferred)) {
	        validateFunctions(el, result, jspElement, prefixToUriMap );
	
	        //result = new Node.JspAttribute(tai, qName, uri,
	        //        localName, value, false, el, dynamic);
	
	        JSPExtensionFactory.getElValidatorExtFactory().getELValidatorExt().validateElFunction(el,jspElement,expression, result, loader, jspConfiguration);                             	        
        }
    }

    private static void validateExpressionForScripts(Element jspElement, String value, JspConfiguration jspConfiguration) throws JspCoreException {
        if (jspConfiguration.scriptingInvalid() && JspTranslatorUtil.isExpression(value, true)) {
            throw new JspTranslationException(jspElement, "jsp.error.scripting.disabled.for.translation.unit");
        }
    }

    
    public static void validateEL(ExpressionFactory ef, ELContext ctx, ELNode.Nodes el, String value)
			throws ELException {
		if (el != null) {
			// determine exact type
			ef.createValueExpression(ctx, value, String.class);
		}
    }
    
    /**
     * Validate functions in EL expressions
     */
    private static void validateFunctions(ELNode.Nodes el, ValidateResult result, Element jspElement, HashMap prefixToUriMap )
            throws JspTranslationException {
        el.visit(new FVVisitor(jspElement, result, prefixToUriMap));
    }

    private static void prepareExpression(ELNode.Nodes el, Element jspElement, String expr, ValidateResult result, ClassLoader loader, JspConfiguration jspConfiguration, HashMap prefixToUriMap)
            throws JspTranslationException {
        validateFunctions(el, result, jspElement, prefixToUriMap);

        JSPExtensionFactory.getElValidatorExtFactory().getELValidatorExt().prepareExpression(el, expr, result, loader, jspConfiguration);            
    }
    
    public static FunctionMapper getFunctionMapper(ELNode.Nodes el, ClassLoader loader, ValidateResult result)
			throws JspTranslationException {
		ValidateFunctionMapper fmapper = result.getValidateFunctionMapper();
		el.visit(new MapperELVisitor(fmapper, loader));
		return fmapper;
	}
}
