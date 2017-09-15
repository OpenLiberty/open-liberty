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
import java.util.List;

import javax.servlet.jsp.tagext.TagFileInfo;
import javax.servlet.jsp.tagext.TagInfo;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.taglib.TagFileTagInfo;
import com.ibm.ws.jsp.translator.JspTranslationException;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.JspVisitorResult;
import com.ibm.ws.jsp.translator.visitor.configuration.JspVisitorUsage;
import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.webcontainer.WCCustomProperties;

public class ValidateTagFileVisitor extends ValidateVisitor {  

    private static final String[] invokeAttrNames = { "fragment", "var", "varReader", "scope" };
    private static final String[] doBodyAttrNames = { "var", "varReader", "scope" };

    private static final String[] tagDirectiveAttrNames =
        { "display-name", "body-content", "dynamic-attributes", "small-icon", "large-icon", "description", "example", "language", "import", "pageEncoding", "isELIgnored", "trimDirectiveWhitespaces", "deferredSyntaxAllowedAsLiteral" };  //jsp2.1work jsp2.1ELwork
    private static final String[] attributeDirectiveAttrNames = { "name", "required", "fragment", "rtexprvalue", "type", "description" };
    private static final String[] attributeDirectiveAttrNames21 = { "name", "required", "fragment", "rtexprvalue", "type", "description","deferredValue","deferredValueType","deferredMethod","deferredMethodSignature"};
    private static final String[] variableDirectiveAttrNames = { "name-given", "name-from-attribute", "alias", "variable-class", "declare", "scope", "description" };

    private static final String[] invokeRequiredAttrs = { "fragment" };
    // no required attributes for tag directives
    private static final String[] attributeDirectiveRequiredAttrs = { "name" };
    //	no required attributes for variable directives

    private HashMap userDefinedDirectives = new HashMap();

    public ValidateTagFileVisitor(JspVisitorUsage visitorUsage, 
                                  JspConfiguration jspConfiguration, 
                                  JspCoreContext context, 
                                  HashMap resultMap, 
                                  JspVisitorInputMap inputMap)
        throws JspCoreException {
        super(visitorUsage, jspConfiguration, context, resultMap, inputMap);
        result = new ValidateTagFileResult(visitorUsage.getJspVisitorDefinition().getId());
        
        //PM08060
        if(WCCustomProperties.ENABLE_DEFAULT_IS_EL_IGNORED_IN_TAG){
        	result.setIsELIgnored(false);
        	jspConfiguration.setElIgnored(false);
        }
        //PM08060
    }

    public JspVisitorResult getResult() throws JspCoreException {
        return (result);
    }

    protected void visitTagDirectiveStart(Element jspElement) throws JspCoreException {
        validateAttributes(jspElement, tagDirectiveAttrNames);

        ValidateTagFileResult tagFileResult = (ValidateTagFileResult) result;
        NamedNodeMap attributes = jspElement.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                String directiveName = attribute.getNodeName();
                String directiveValue = attribute.getNodeValue();

                Object oldDirectiveValue = addUserTagDirective(directiveName, directiveValue);
                if (oldDirectiveValue != null
                    && (directiveName.equals("import") == false) // import can appear more than once in translation unit.
                    && (directiveName.equals("jsp:id") == false)) {
                    if (oldDirectiveValue.equals(directiveValue) == false) {                            
                        throw new JspTranslationException(jspElement, "jsp.error.multiple.occurrences.tag.directive", new Object[] { directiveName, oldDirectiveValue, directiveValue });
                    }
                }

                boolean valid = false;
                if (directiveName.equals("language")) {
                    valid = true;
                    result.setLanguage(directiveValue);
                }
                else if (directiveName.equals("import")) {
                    valid = true;
                }
                else if (directiveName.equals("pageEncoding")) {
                    valid = true;
                    result.setPageEncoding(directiveValue);
                }
                else if (directiveName.equals("isELIgnored")) {
                    valid = true;
                    if (directiveValue.equalsIgnoreCase("true")) {
                        result.setIsELIgnored(true);
                        jspConfiguration.setElIgnored(true);
                        jspConfiguration.setElIgnoredSetTrueInPage(true);
                    }
                    else if (directiveValue.equalsIgnoreCase("false")) {
                        result.setIsELIgnored(false);
                        jspConfiguration.setElIgnored(false);
                    }
                    else
                        throw new JspTranslationException(jspElement, "jsp.error.page.invalid.iselignored");
                }
                else if (directiveName.equals("display-name")) {
                    valid = true;
                    tagFileResult.setDisplayName(directiveValue);
                }
                else if (directiveName.equals("body-content")) {
                    if (directiveValue.equals("scriptless") || directiveValue.equals("tagdependent") || directiveValue.equals("empty")) {
                        valid = true;
                        tagFileResult.setBodyContent(directiveValue);
                    }
                    else {
                        throw new JspTranslationException(jspElement, "jsp.body-content.directive.value.invalid", new Object[] { directiveValue });
                    }
                }
                else if (directiveName.equals("dynamic-attributes")) {
                    valid = true;
                    tagFileResult.setDynamicAttributes(directiveValue);
                }
                else if (directiveName.equals("small-icon")) {
                    valid = true;
                    tagFileResult.setSmallIcon(directiveValue);
                }
                else if (directiveName.equals("large-icon")) {
                    valid = true;
                    tagFileResult.setLargeIcon(directiveValue);
                }
                else if (directiveName.equals("description")) {
                    valid = true;
                    tagFileResult.setDescription(directiveValue);
                }
                else if (directiveName.equals("example")) {
                    valid = true;
                    tagFileResult.setExample(directiveValue);
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
	                        result.setTrimDirectiveWhitespaces(true);
	                        jspConfiguration.setTrimDirectiveWhitespaces(true);
	                    }
	                    else if (directiveValue.equalsIgnoreCase("false")) {
	                        result.setTrimDirectiveWhitespaces(false);
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
	                        result.setDeferredSyntaxAllowedAsLiteral(true);
	                        jspConfiguration.setDeferredSyntaxAllowedAsLiteral(true);
	                    }
	                    else if (directiveValue.equalsIgnoreCase("false")) {
	                        result.setDeferredSyntaxAllowedAsLiteral(false);
	                        jspConfiguration.setDeferredSyntaxAllowedAsLiteral(false);
	                    }
	                    else {
	                        throw new JspTranslationException(jspElement, "jsp.error.page.invalid.deferredsyntaxallowedasliteral");
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

    protected void visitPageDirectiveStart(Element jspElement) throws JspCoreException {
        throw new JspTranslationException(jspElement, "jsp.error.page.directive.only.in.jsps");
    }

    protected void visitJspInvokeStart(Element jspElement) throws JspCoreException {
        List specifiedStandardActionAttrs = getAttributeList(jspElement);
        validateRequiredAttributes(jspElement, specifiedStandardActionAttrs, invokeRequiredAttrs);
        validateAttributes(specifiedStandardActionAttrs, jspElement, invokeAttrNames);
        validateEmptyBody(jspElement);
        if (jspElement.hasAttribute("var") && jspElement.hasAttribute("varReader")) {
            throw new JspTranslationException(jspElement, "jsp.error.tagfile.invoke.var_varreader");
        }
        
        if (jspElement.hasAttribute("scope")) {
            if (jspElement.hasAttribute("var") == false && jspElement.hasAttribute("varReader") == false) {
                throw new JspTranslationException(jspElement, "jsp.error.tagfile.invoke.scope_var_varreader");
            }
        }
        
        Attr scopeAttr = jspElement.getAttributeNode("scope");
        
        if (scopeAttr != null) {
            boolean valid = false;
            
            if (scopeAttr.getValue().equals("page")) {
                valid = true;
            }
            else if (scopeAttr.getValue().equals("request")) {
                valid = true;
            }
            else if (scopeAttr.getValue().equals("session")) {
                valid = true;
            }
            else if (scopeAttr.getValue().equals("application")) {
                valid = true;
            }
            if (valid == false) {
                throw new JspTranslationException(jspElement, "jsp.error.tagfile.invoke.scope_invalid");
            }
        }
    }

    protected void visitJspDoBodyStart(Element jspElement) throws JspCoreException {
        List specifiedStandardActionAttrs = getAttributeList(jspElement);
        validateAttributes(specifiedStandardActionAttrs, jspElement, doBodyAttrNames);
        validateEmptyBody(jspElement);
        Attr varAttr = jspElement.getAttributeNode("var");
        Attr varReaderAttr = jspElement.getAttributeNode("varReader");
        if (varAttr != null && varReaderAttr != null) {
            throw new JspTranslationException(jspElement, "jsp.error.tagfile.dobody.var_varreader");
        }

        if (jspElement.hasAttribute("scope")) {
            if (jspElement.hasAttribute("var") == false && jspElement.hasAttribute("varReader") == false) {
                throw new JspTranslationException(jspElement, "jsp.error.tagfile.dobody.scope_var_varreader");
            }
        }
        
        Attr scopeAttr = jspElement.getAttributeNode("scope");
        
        if (scopeAttr != null) {
            boolean valid = false;
            
            if (scopeAttr.getValue().equals("page")) {
                valid = true;
            }
            else if (scopeAttr.getValue().equals("request")) {
                valid = true;
            }
            else if (scopeAttr.getValue().equals("session")) {
                valid = true;
            }
            else if (scopeAttr.getValue().equals("application")) {
                valid = true;
            }
            if (valid == false) {
                throw new JspTranslationException(jspElement, "jsp.error.tagfile.dobody.scope_invalid");
            }
        }
    }

    protected void visitAttributeDirectiveStart(Element jspElement) throws JspCoreException {
        List specifiedStandardActionAttrs = getAttributeList(jspElement);
        validateRequiredAttributes(jspElement, specifiedStandardActionAttrs, attributeDirectiveRequiredAttrs);
        
        //if JSP 2.1 or later include deferred value and method attributes as valid. 
        if( Float.valueOf(jspConfiguration.getJspVersion()) >= JspConfiguration.twoPointOne){
        	validateAttributes(specifiedStandardActionAttrs, jspElement, attributeDirectiveAttrNames21);
        }else{	//JSP 2.0 or 1.2 thus deferred value and method attributes are not valid
        	validateAttributes(specifiedStandardActionAttrs, jspElement, attributeDirectiveAttrNames);
        }
        
        Attr nameAttr = jspElement.getAttributeNode("name");
        if (nameAttr != null) {
            TagFileInfo tfi = (TagFileInfo) inputMap.get("TagFileInfo");
            TagInfo ti = tfi.getTagInfo();
            if (ti instanceof TagFileTagInfo) {
                TagFileTagInfo tfti = (TagFileTagInfo) ti;
                if (tfti.getDynamicAttributesMapName() != null && tfti.getDynamicAttributesMapName().equals(nameAttr.getValue())) {
                    throw new JspTranslationException(jspElement, "jsp.error.tagfile.tag_dynamic_attrs_equals_attr_name");
                }
            }
        }
    }

    protected void visitVariableDirectiveStart(Element jspElement) throws JspCoreException {
        validateAttributes(jspElement, variableDirectiveAttrNames);
        Attr nameGivenAttr = jspElement.getAttributeNode("name-given");
        if (nameGivenAttr != null) {
            TagFileInfo tfi = (TagFileInfo) inputMap.get("TagFileInfo");
            TagInfo ti = tfi.getTagInfo();
            if (ti instanceof TagFileTagInfo) {
                TagFileTagInfo tfti = (TagFileTagInfo) ti;
                if (tfti.getDynamicAttributesMapName() != null && tfti.getDynamicAttributesMapName().equals(nameGivenAttr.getValue())) {
                    throw new JspTranslationException(jspElement, "jsp.error.tagfile.tag_dynamic_attrs_equals_var_name_given");
                }
            }
        }
    }

    protected Object addUserTagDirective(String directiveName, String directiveValue) {
        return userDefinedDirectives.put(directiveName, directiveValue);
    }

}
