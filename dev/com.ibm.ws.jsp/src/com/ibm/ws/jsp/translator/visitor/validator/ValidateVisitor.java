/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.visitor.validator;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELException;
import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagFileInfo;
import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagLibraryValidator;
import javax.servlet.jsp.tagext.TagVariableInfo;
import javax.servlet.jsp.tagext.ValidationMessage;
import javax.servlet.jsp.tagext.VariableInfo;

import org.apache.el.lang.ELArithmetic;
import org.apache.el.lang.ELSupport;
import org.apache.jasper.compiler.ELNode;
import org.apache.jasper.compiler.ELParser;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.bean.BeanRepository;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.inputsource.JspInputSourceContainerImpl;
import com.ibm.ws.jsp.taglib.TagLibraryCache;
import com.ibm.ws.jsp.taglib.TagLibraryInfoImpl;
import com.ibm.ws.jsp.translator.JspTranslationException;
import com.ibm.ws.jsp.translator.utils.JspId;
import com.ibm.ws.jsp.translator.utils.JspTranslatorUtil;
import com.ibm.ws.jsp.translator.utils.TagFileId;
import com.ibm.ws.jsp.translator.visitor.JspVisitor;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.JspVisitorResult;
import com.ibm.ws.jsp.translator.visitor.configuration.JspVisitorUsage;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;
import com.ibm.wsspi.jsp.context.JspCoreContext;

public abstract class ValidateVisitor extends JspVisitor { 

	static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.translator.visitor.validator.ValidateVisitor";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}
    public static final String[] ATTR_STANDARD_ACTIONS = {
        Constants.JSP_USEBEAN_TYPE,
        Constants.JSP_SETPROPERTY_TYPE,
        Constants.JSP_GETPROPERTY_TYPE,
        Constants.JSP_INCLUDE_TYPE,
        Constants.JSP_FORWARD_TYPE,
        Constants.JSP_PARAM_TYPE,
        Constants.JSP_PLUGIN_TYPE,
        Constants.JSP_INVOKE_TYPE,
        Constants.JSP_DOBODY_TYPE,
        Constants.JSP_ELEMENT_TYPE,
        Constants.JSP_OUTPUT_TYPE
    }; 

    public static final String[] BODY_STANDARD_ACTIONS = {
        Constants.JSP_USEBEAN_TYPE,
        Constants.JSP_SETPROPERTY_TYPE,
        Constants.JSP_GETPROPERTY_TYPE,
        Constants.JSP_INCLUDE_TYPE,
        Constants.JSP_FORWARD_TYPE,
        Constants.JSP_PARAM_TYPE,
        Constants.JSP_PARAMS_TYPE,
        Constants.JSP_PLUGIN_TYPE,
        Constants.JSP_ELEMENT_TYPE,
        Constants.JSP_OUTPUT_TYPE,
        Constants.JSP_FALLBACK_TYPE
    };
     
    // valid attribute names for each standard action
    private static final String[] includeAttrNames = { "page", "flush" };
    private static final String[] forwardAttrNames = { "page" };
    private static final String[] useBeanAttrNames = { "id", "scope", "class", "beanName", "type" };
    private static final String[] setPropertyAttrNames = { "name", "property", "param", "value" };
    private static final String[] getPropertyAttrNames = { "name", "property" };
    private static final String[] paramAttrNames = { "name", "value" };
    private static final String[] pluginAttrNames =
        { "type", "code", "codebase", "align", "archive", "height", "hspace", "jreversion", "name", "vspace", "title", "width", "nspluginurl", "iepluginurl", "mayscript" };
    private static final String[] attributeAttrNames = { "name", "trim" };
    private static final String[] attributeAttrNames21MR2 = { "name", "trim", "omit" };
    private static final String[] elementAttrNames = { "name" };
    private static final String[] outputAttrNames = { "omit-xml-declaration", "doctype-root-element", "doctype-system", "doctype-public" };

    // valid runtime attribute names for each standard action    
    private static final String[] includeRuntimeAttrNames = { "page" };
    private static final String[] forwardRuntimeAttrNames = { "page" };
    private static final String[] setPropertyRuntimeAttrNames = { "value" };
    private static final String[] useBeanRuntimeAttrNames = { "beanName" };
    private static final String[] pluginRuntimeAttrNames = { "height", "width" };
    private static final String[] paramRuntimeAttrNames = { "value" };

    // valid required attribute names for each standard action    
    private static final String[] includeRequiredAttrs = { "page" };
    private static final String[] forwardRequiredAttrs = { "page" };
    private static final String[] useBeanRequiredAttrsClass = { "class", "id" };
    private static final String[] useBeanRequiredAttrsType = { "type", "id" };
    private static final String[] setPropertyRequiredAttrs = { "name", "property" };
    private static final String[] getPropertyRequiredAttrs = { "name", "property" };
    private static final String[] paramAttrsRequiredAttrs = { "name", "value" };
    private static final String[] pluginAttrsRequiredAttrs = { "type", "code", "codebase" };
    private static final String[] attributeRequiredAttrs = { "name" };
    private static final String[] elementRequiredAttrs = { "name" };

    protected ValidateResult result = null;
    protected TagLibraryCache tagLibraryCache = null;
    protected Stack tagCountStack = new Stack();
    private Vector dupVector = new Vector();		//PK29373
    private HashMap<String,Integer> scriptVars = new HashMap<String,Integer>();
    private HashMap<String,String> prefixToUriMap = new HashMap<String,String>();
    protected String jspUri = null;
    protected TagCountResult tagCountResult = null;
    protected boolean dupFlag = false;		//PK29373
    protected JspOptions jspOptions = null; //PK53233
    protected boolean evalQuotedAndEscapedExpression = false; //PK53233

    protected ValidateVisitor(JspVisitorUsage visitorUsage, 
                              JspConfiguration jspConfiguration, 
                              JspCoreContext context, 
                              HashMap resultMap, 
                              JspVisitorInputMap inputMap)
        throws JspCoreException {
        super(visitorUsage, jspConfiguration, context, resultMap, inputMap);
        tagLibraryCache = (TagLibraryCache) inputMap.get("TagLibraryCache");
        jspUri = (String)inputMap.get("JspUri");
        tagCountResult = (TagCountResult)resultMap.get("TagCount");
        //PK53233 start
        jspOptions = (JspOptions)inputMap.get("JspOptions");
        evalQuotedAndEscapedExpression = jspOptions.isEvalQuotedAndEscapedExpression();
        if(logger.isLoggable(Level.FINER)){
            logger.logp(Level.FINER, CLASS_NAME, "ValidateVisitor","evalQuotedAndEscapedExpression =[" + evalQuotedAndEscapedExpression +"]");
        }
        //PK53233 start
    }

    public JspVisitorResult getResult() throws JspCoreException {
        List staticIncludeDependencies = (List) inputMap.get("StaticIncludeDependencyList");
        result.getDependencyList().addAll(staticIncludeDependencies);

        List tagFileDependencies = (List) inputMap.get("TagFileDependencies");

        for (Iterator itr = tagFileDependencies.iterator(); itr.hasNext();) {
            TagFileId tagFileId = (TagFileId) itr.next();
            TagLibraryInfoImpl tli = tagLibraryCache.getTagLibraryInfo(tagFileId.getUri(), tagFileId.getPrefix(), jspUri);
            if (tli.isContainer()) {
                //TODO: figure out how to tell if it's in a jar
                TagFileInfo tfi = tli.getTagFile(tagFileId.getTagName());
                result.getDependencyList().add(tfi.getPath());
            } else {
                if (tli.getInputSource().getAbsoluteURL().getProtocol().equals("file")) {
                    /* Only add tagfile dependencies that are not in jar file */
                    TagFileInfo tfi = tli.getTagFile(tagFileId.getTagName());
                    result.getDependencyList().add(tfi.getPath());
                }
            }
        }

        /* Make sure BeanRepository is created even if it isn't required */
        getBeanRepository();
        return (result);
    }

    private BeanRepository getBeanRepository() {
        if (result.getBeanRepository() == null)
            result.setBeanRepository(new BeanRepository(context.getJspClassloaderContext().getClassLoader()));
        return (result.getBeanRepository());
    }

    protected void visitJspParamsStart(Element jspElement) throws JspCoreException {
        Node parent = jspElement.getParentNode();
        if (parent.getNamespaceURI().equals(Constants.JSP_NAMESPACE)) {
            if (parent.getLocalName().equals(Constants.JSP_PLUGIN_TYPE) == false &&
                parent.getLocalName().equals(Constants.JSP_BODY_TYPE) == false) 
                throw new JspTranslationException(jspElement, "jsp.params.invalid.parent");
        }
        else
            throw new JspTranslationException(jspElement, "jsp.params.invalid.parent");

    }

    protected void visitJspFallbackStart(Element jspElement) throws JspCoreException {
        Node parent = jspElement.getParentNode();
        if (parent.getNamespaceURI().equals(Constants.JSP_NAMESPACE)) {
            if (parent.getLocalName().equals(Constants.JSP_PLUGIN_TYPE) == false &&
                parent.getLocalName().equals(Constants.JSP_BODY_TYPE) == false) 
                throw new JspTranslationException(jspElement, "jsp.fallback.invalid.parent");
        }
        else
            throw new JspTranslationException(jspElement, "jsp.fallback.invalid.parent");
    }

    protected void visitJspParamStart(Element jspElement) throws JspCoreException {
        List specifiedStandardActionAttrs = getAttributeList(jspElement);
        validateRequiredAttributes(jspElement, specifiedStandardActionAttrs, paramAttrsRequiredAttrs);

        validateAttributes(specifiedStandardActionAttrs, jspElement, paramAttrNames);
        ELValidator.validateRuntimeExpressions(jspElement, paramRuntimeAttrNames, java.lang.Object.class, jspConfiguration, result, context, prefixToUriMap);
        validateEmptyBody(jspElement);
        // expectedType Object. leave for runtime to fail if wrong type passed in.
        Node parent = jspElement.getParentNode();
        String parentName = jspElement.getParentNode().getLocalName();
        if (parent.getNamespaceURI().equals(Constants.JSP_NAMESPACE)) {
            if (parentName.equals(Constants.JSP_INCLUDE_TYPE)
                || parentName.equals(Constants.JSP_FORWARD_TYPE)
                || parentName.equals(Constants.JSP_PARAMS_TYPE)
                || parentName.equals(Constants.JSP_BODY_TYPE)) {

                String name = getAttributeValue(jspElement, "name");
                if (name.equals(""))
                    throw new JspTranslationException(jspElement, "jsp.param.name.empty");
            }
            else
                throw new JspTranslationException(jspElement, "jsp.param.invalid.parent");
        }
        else {
            throw new JspTranslationException(jspElement, "jsp.param.invalid.parent");
        }
    }

    protected void visitCustomTagStart(Element jspElement) throws JspCoreException {
        getAttributeList(jspElement);
        String uri = jspElement.getNamespaceURI();
        String prefix = jspElement.getPrefix();
        String tagName = jspElement.getLocalName();
        ValidateTagInfo vti;
        
        boolean isTagFile = false;

        if (uri.startsWith("urn:jsptld:")) {
            uri = uri.substring(uri.indexOf("urn:jsptld:") + 11);
        }
        else if (uri.startsWith("urn:jsptagdir:")) {
            uri = uri.substring(uri.indexOf("urn:jsptagdir:") + 14);
        }
        
        vti = getTagInfo (jspElement, uri, prefix, tagName);
        
        if (vti == null) {
             return;
        }
        
        TagInfo ti = vti.getTagInfo();
        isTagFile = vti.isTagFile();
        TagLibraryInfoImpl tli = vti.getTLI();

        // defect 420617 - TagInfo needs to have TagLibraryInfo
        ti.setTagLibrary(tli);

        TagAttributeInfo[] attributes = ti.getAttributes();

        validateCustomTagAttributes(jspElement, attributes);

        // build Hashtable of attributes associate with this custom tag.
        CustomTagInstance cti = validateCustomTagAttributeValues(jspElement, attributes, ti.hasDynamicAttributes(), tli);
        Hashtable attribs = cti.getAttributes();
        // Will be false if any of the custom tag attributes contains a runtime expression
        boolean isScriptless = cti.isScriptless();

        StringBuffer varNameSuffix = new StringBuffer();

        // defect 236125 begin: create more unique key for varname suffix
        varNameSuffix.append(uri);
        // end defect 236125 begin: create more unique key for varname suffix

        
        // verify all required attributes exist
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i].isRequired() && (attribs.containsKey(attributes[i].getName()) == false)) {
                throw new JspTranslationException(jspElement, "jsp.error.missing.attribute", new Object[] { attributes[i].getName()});
            }
            else if (attributes[i].isRequired() == false && attribs.containsKey(attributes[i].getName())) {
				varNameSuffix.append("_"+attributes[i].getName());
            }
        }
        
        if (JspTranslatorUtil.hasBody(jspElement) || JspTranslatorUtil.hasJspBody(jspElement)) {
            varNameSuffix.append("_jspxhasbody");
        }

        // defect 236125 begin: create more unique key for varname suffix
        String tmpValueToBeHashed = varNameSuffix.toString();
        varNameSuffix = new StringBuffer ( "_" + tmpValueToBeHashed.hashCode());
        // defect 236125 end: create more unique key for varname suffix

        // verify all attributes inside of custom tag are valid attribute names.
        Enumeration e = attribs.keys();
        while (e.hasMoreElements()) {
            boolean found = false;
            String attr = (String) e.nextElement();
            for (int i = 0; i < attributes.length; i++) {
                if (attributes[i].getName().equals(attr)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                if (ti.hasDynamicAttributes() == false)
                    throw new JspTranslationException(jspElement, "jsp.error.bad.attribute", new Object[] { attr });
            }
        }

        TagData tagData = new TagData(attribs);
        if (!ti.isValid(tagData))
            throw new JspTranslationException(jspElement, "jsp.error.invalid.attributes");


        TagExtraInfo tei = ti.getTagExtraInfo();
        if (tei != null) {
            // defect 420617 - tei needs TagInfo
            tei.setTagInfo(ti);
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
    			logger.logp(Level.FINER, CLASS_NAME, "visitCustomTagStart","tei.getTagInfo().getTagLibrary().getTagLibraryInfos() =[" + tei.getTagInfo().getTagLibrary().getTagLibraryInfos() +"]");
    		}

	        if (tei.getVariableInfo(tagData) != null
	            && tei.getVariableInfo(tagData).length > 0
	            && ti.getTagVariableInfos().length > 0) {
	            throw new JspTranslationException(jspElement, "jsp.error.non_null_tei_and_var_subelems");
	        }
        }
        ValidationMessage[] validationMessages = ti.validate(tagData);

        if (validationMessages != null && validationMessages.length > 0) {
            StringBuffer errMsg = new StringBuffer();
            for (int i = 0; i < validationMessages.length; i++) {
                if (validationMessages[i] != null) {
                    errMsg.append("<p>");
                    errMsg.append(validationMessages[i].getId());
                    errMsg.append(": ");
                    errMsg.append(validationMessages[i].getMessage());
                    errMsg.append("</p>");
                }
            }
            throw new JspTranslationException(jspElement, "jsp.error.tei.invalid.attributes", new Object[] { uri, tagName, errMsg.toString()});
        }
        
        if (isScriptless) {
            // Scan through children looking for any scripting elements or standard actions that 
            // contain runtime expression attributes 
            isScriptless = areChildrenScriptless(jspElement);
        }
        
        TagVariableInfo[] tagVarInfos = ti.getTagVariableInfos();
        VariableInfo[] varInfos = ti.getVariableInfo(tagData);

        if (varInfos == null)
            varInfos = new VariableInfo[0];

        boolean hasScriptingVars = varInfos.length > 0 || tagVarInfos.length > 0;

        Integer tagCount = (Integer)tagCountResult.getCountMap().get(jspElement); //PK41783
        //PM43415 start
        if (tagCount == null) {
                tagCount = 0;
        }
        //PM43415 end
        Vector atBeginVars = setScriptingVars(tagVarInfos, varInfos, tagData, VariableInfo.AT_BEGIN, tagCount.intValue());
        Vector nestedVars = setScriptingVars(tagVarInfos, varInfos, tagData, VariableInfo.NESTED, tagCount.intValue());

        result.addCollectTagData(jspElement, isScriptless, hasScriptingVars, atBeginVars, null, nestedVars, tagData, varNameSuffix.toString());

        if (tagLibraryCache.getTagClassInfo(ti) == null) {
            if (isTagFile == false) {
                Class tagClass = null;
                try {
                    tagClass = context.getJspClassloaderContext().getClassLoader().loadClass(ti.getTagClassName());
                    tagLibraryCache.addTagClassInfo(ti, tagClass);
                }
                catch (Exception ex) {
                    throw new JspTranslationException(jspElement, "jsp.error.unable.loadclass", new Object[] { ti.getTagClassName()});
                }
            }
        }
        tagCountStack.push(tagCountResult.getCountMap().get(jspElement)); //PK41783
        
    }

    protected void visitCustomTagEnd(Element jspElement) throws JspCoreException {
        ValidateResult.CollectedTagData collectedTagData = result.getCollectedTagData(jspElement);
        String uri = jspElement.getNamespaceURI();
        String prefix = jspElement.getPrefix();
        String tagName = jspElement.getLocalName();

        if (uri.startsWith("urn:jsptld:")) {
            uri = uri.substring(uri.indexOf("urn:jsptld:") + 11);
        }
        else if (uri.startsWith("urn:jsptagdir:")) {
            uri = uri.substring(uri.indexOf("urn:jsptagdir:") + 14);
        }
        TagLibraryInfoImpl tli = (TagLibraryInfoImpl) result.getTagLibMap().get(uri);
        if (tli != null) {
            tagCountStack.pop();
            TagInfo ti = tli.getTag(tagName);
            if (ti == null) {
                TagFileInfo tfi = tli.getTagFile(tagName);
                ti = tfi.getTagInfo();
            }
            TagVariableInfo[] tagVarInfos = ti.getTagVariableInfos();
            VariableInfo[] varInfos = ti.getVariableInfo(collectedTagData.getTagData());
            if (varInfos == null)
                varInfos = new VariableInfo[0];
            Integer tagCount = (Integer)tagCountResult.getCountMap().get(jspElement); //PK41783
            Vector atEndVars = setScriptingVars(tagVarInfos, varInfos, collectedTagData.getTagData(), VariableInfo.AT_END, (tagCount == null ? 0 : tagCount.intValue())); //PM43415
            //PK29373
            if(dupFlag) {
                Vector dupVector = getDuplicateVars();
                collectedTagData.setAtEndDuplicateVars(dupVector);
                dupFlag = false;
            }//PK29373
            collectedTagData.setAtEndScriptingVars(atEndVars);
        }
    }

    protected void visitJspRootStart(Element jspElement) throws JspCoreException {
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "visitJspRootStart","enter visitJspRootStart() =[" + jspElement.getNodeValue() +"]");
		}
        if (jspElement.getParentNode().getNodeType() != Node.DOCUMENT_NODE) {
            throw new JspTranslationException(jspElement, "jsp.error.root.must.be.top");
        }
        NamedNodeMap attributes = jspElement.getAttributes();
        if (attributes != null) {
            if (jspElement.hasAttribute("version") == false) {
                throw new JspTranslationException(jspElement, "jsp.error.root.invalid.version", new Object[] { "none found"});
            }
            
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                if (attribute.getNodeName().startsWith("xmlns:")) {
                    String prefix = attribute.getNodeName();
                    prefix = prefix.substring(prefix.indexOf(":") + 1);
                    String uri = attribute.getNodeValue();
                    if (uri.startsWith("urn:jsptld:")) {
                        uri = uri.substring(uri.indexOf("urn:jsptld:") + 11);
                    }
                    else if (uri.startsWith("urn:jsptagdir:")) {
                        uri = uri.substring(uri.indexOf("urn:jsptagdir:") + 14);
                    }
                    /* Start Defect 202915 */
                    if (uri.equals(Constants.JSP_NAMESPACE) == false && uri.equals(Constants.XSI_NAMESPACE) == false) {
                    /* End Defect 202915 */
                        TagLibraryInfoImpl tli = tagLibraryCache.getTagLibraryInfo(uri, prefix, jspUri);
                        if (tli != null) {
                            if (tli.getInputSource() instanceof JspInputSourceContainerImpl) {
                                if (tagLibraryCache.getImplicitTagLibPrefixMap().containsValue(uri) == false) {
                                    result.getDependencyList().add(tli.getTldFilePath());
                                }
                            } else {
                                if (tli.getTldFilePath() != null && 
                                    tli.getInputSource().getAbsoluteURL().getProtocol().equals("file") && 
                                    tagLibraryCache.getImplicitTagLibPrefixMap().containsValue(uri) == false) {
                                    result.getDependencyList().add(tli.getTldFilePath());
                                }
                            }
                            if (result.getTagLibMap().containsKey(uri) == false) {
                                result.getTagLibMap().put(uri, tli);
                                tli.setTagLibMap(result.getTagLibMap()); //jsp2.1work
                        		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                        			logger.logp(Level.FINER, CLASS_NAME, "visitJspRootStart","getTagLibraryInfos() =[" + tli.getTagLibraryInfos() +"]");
                        		}
                                prefixToUriMap.put(prefix, uri);
                                validateTagLib(jspElement.getOwnerDocument(), uri, prefix, tli);
                            }
                        }
                        else {
                            if (jspConfiguration.isXml() == false)
                            	throw new JspTranslationException(jspElement, "jsp.error.tld.not.found", new Object[] { uri, prefix });
                        }
                    }
                }
                else if (attribute.getNodeName().equals("version")) {
                    if (attribute.getNodeValue().equals("1.2") == false 
                    	&& attribute.getNodeValue().equals("2.0") == false 
                		&& attribute.getNodeValue().equals("2.1") == false) {
                        throw new JspTranslationException(jspElement, "jsp.error.root.invalid.version", new Object[] { attribute.getNodeValue()});
                    }
                }
            }
        }
        else {
            throw new JspTranslationException(jspElement, "jsp.root.has.no.attributes");
        }
    }

    protected void visitJspUseBeanStart(Element jspElement) throws JspCoreException {

        List specifiedStandardActionAttrs = getAttributeList(jspElement);
        try {
            validateRequiredAttributes(jspElement, specifiedStandardActionAttrs, useBeanRequiredAttrsClass);
        }
        catch (JspCoreException jce) { // try alternate useBean set of required attributes
            validateRequiredAttributes(jspElement, specifiedStandardActionAttrs, useBeanRequiredAttrsType);
        }

        validateAttributes(specifiedStandardActionAttrs, jspElement, useBeanAttrNames);
        ELValidator.validateRuntimeExpressions(jspElement, useBeanRuntimeAttrNames, java.lang.String.class, jspConfiguration, result, context, prefixToUriMap);

        String nameAttrValue = getAttributeValue(jspElement, "id");
        String scopeAttrValue = getAttributeValue(jspElement, "scope");
        String classAttrValue = getAttributeValue(jspElement, "class");
        String typeAttrValue = getAttributeValue(jspElement, "type");
        String beanNameAttrValue = getAttributeValue(jspElement, "beanName");

        // validation of useBean tag
        if (nameAttrValue.equals(""))
            throw new JspTranslationException(jspElement, "jsp.error.usebean.missing.attribute");

        if (classAttrValue.equals("") && typeAttrValue.equals(""))
            throw new JspTranslationException(jspElement, "jsp.error.usebean.missing.type");

        if (getBeanRepository().checkVariable(nameAttrValue) == true)
            throw new JspTranslationException(jspElement, "jsp.error.usebean.duplicate", new Object[] { nameAttrValue });

        // determine class name for bean
        if (classAttrValue.equals("") == false && beanNameAttrValue.equals("") == false)
            throw new JspTranslationException(jspElement, "jsp.error.usebean.not.both", new Object[] { classAttrValue, beanNameAttrValue });

        if (classAttrValue.equals("") == false && typeAttrValue.equals("") == false) {
            try {
                Class classAttrClass = Class.forName(classAttrValue, true, context.getJspClassloaderContext().getClassLoader());
                Class typeAttrClass = Class.forName(typeAttrValue, true, context.getJspClassloaderContext().getClassLoader());
                if (typeAttrClass.isAssignableFrom(classAttrClass) == false) {
                    throw new JspTranslationException(
                        jspElement,
                        "jsp.error.usebean.class.must.be.assignable.to.type",
                        new Object[] { classAttrValue, typeAttrValue, jspElement.getTagName()});

                }
            }
            catch (ClassNotFoundException cnfe) {
                throw new JspTranslationException(
                    jspElement,
                    "jsp.error.usebean.cannot.locate.class.to.validate.assignable",
                    new Object[] { classAttrValue, typeAttrValue, jspElement.getTagName()},
                    cnfe);
            }
        }

        String clsname = null;
        if (classAttrValue != null && classAttrValue.equals("") == false)
            clsname = classAttrValue;
        else
            clsname = typeAttrValue;

        // determine scope for bean.
        if (scopeAttrValue.equals("") || scopeAttrValue.equals("page")) {
            getBeanRepository().addPageBean(nameAttrValue, clsname);
        }
        else if (scopeAttrValue.equals("request")) {
            getBeanRepository().addRequestBean(nameAttrValue, clsname);
        }
        else if (scopeAttrValue.equals("session")) {
            getBeanRepository().addSessionBean(nameAttrValue, clsname);
        }
        else if (scopeAttrValue.equals("application")) {
            getBeanRepository().addApplicationBean(nameAttrValue, clsname);
        }
        else {
            throw new JspTranslationException(jspElement, "jsp.error.usebean.invalid.scope", new Object[] { scopeAttrValue });
        }
    }

    protected void visitJspExpressionStart(Element jspElement) throws JspCoreException {
        Node n = jspElement.getFirstChild();
        if (n.getNodeType() == Node.CDATA_SECTION_NODE) {
            if (jspConfiguration.scriptingInvalid()) {
                throw new JspTranslationException(jspElement, "jsp.error.expressions.disabled.for.translation.unit");
            }
        }
        else {
            throw new JspTranslationException(jspElement, "jsp.error.expression.contains.no.cdata");
        }
    }

    protected void visitJspScriptletStart(Element jspElement) throws JspCoreException {
        Node n = jspElement.getFirstChild();
        if (n.getNodeType() == Node.CDATA_SECTION_NODE) {
            if (jspConfiguration.scriptingInvalid()) {
                throw new JspTranslationException(jspElement, "jsp.error.scriptlets.disabled.for.translation.unit");
            }
        }
        else {
            throw new JspTranslationException(jspElement, "jsp.error.scriptlet.contains.no.cdata");
        }
    }

    protected void visitJspDeclarationStart(Element jspElement) throws JspCoreException {
        Node n = jspElement.getFirstChild();
        if (n.getNodeType() == Node.CDATA_SECTION_NODE) {
            if (jspConfiguration.scriptingInvalid()) {
                throw new JspTranslationException(jspElement, "jsp.error.declarations.disabled.for.translation.unit");
            }
        }
        else {
            throw new JspTranslationException(jspElement, "jsp.error.declaration.contains.no.cdata");
        }
    }

    protected void visitJspTextStart(Element jspElement) throws JspCoreException {
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "visitJspTextStart","jspElement =[" + jspElement +"]");
		}
        for (int i = 0; i < jspElement.getChildNodes().getLength(); i++) {
            Node n = jspElement.getChildNodes().item(i);
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
    			logger.logp(Level.FINER, CLASS_NAME, "visitJspTextStart","Node n =[" + n +"]");
    			logger.logp(Level.FINER, CLASS_NAME, "visitJspTextStart","Node n TYPE =[" + n.getNodeType() +"]");
    		}
            if (n.getNodeType() == Node.CDATA_SECTION_NODE) {
                CDATASection cdata = (CDATASection) n;
                visitJspELTextStart(jspElement, cdata.getData(), java.lang.String.class);
            }
            else if (n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {}
            else {
                throw new JspTranslationException(jspElement, "jsp.error.jsptext.has.child.elements");
            }
        }
    }

    protected void visitJspIncludeStart(Element jspElement) throws JspCoreException {
        List specifiedStandardActionAttrs = getAttributeList(jspElement);
        validateRequiredAttributes(jspElement, specifiedStandardActionAttrs, includeRequiredAttrs);

        String flushString = getAttributeValue(jspElement, "flush");
        if (flushString.equals("") == false) {
            if (flushString.equalsIgnoreCase("false") == false && flushString.equalsIgnoreCase("true") == false) {
                throw new JspTranslationException(jspElement, "jsp.error.include.flush.invalid.value", new Object[] { flushString });
            }
        }

        validateAttributes(specifiedStandardActionAttrs, jspElement, includeAttrNames);
        ELValidator.validateRuntimeExpressions(jspElement, includeRuntimeAttrNames, java.lang.String.class, jspConfiguration, result, context, prefixToUriMap);

    }

    protected void visitJspGetPropertyStart(Element jspElement) throws JspCoreException {
        List specifiedStandardActionAttrs = getAttributeList(jspElement);
        validateRequiredAttributes(jspElement, specifiedStandardActionAttrs, getPropertyRequiredAttrs);

        validateAttributes(specifiedStandardActionAttrs, jspElement, getPropertyAttrNames);
    }

    protected void visitJspForwardStart(Element jspElement) throws JspCoreException {
        List specifiedStandardActionAttrs = getAttributeList(jspElement);
        validateRequiredAttributes(jspElement, specifiedStandardActionAttrs, forwardRequiredAttrs);

        validateAttributes(specifiedStandardActionAttrs, jspElement, forwardAttrNames);
        ELValidator.validateRuntimeExpressions(jspElement, forwardRuntimeAttrNames, java.lang.String.class, jspConfiguration, result, context, prefixToUriMap);
    }

    protected void visitJspPluginStart(Element jspElement) throws JspCoreException {
        List specifiedStandardActionAttrs = getAttributeList(jspElement);
        validateRequiredAttributes(jspElement, specifiedStandardActionAttrs, pluginAttrsRequiredAttrs);

        validateAttributes(specifiedStandardActionAttrs, jspElement, pluginAttrNames);
        ELValidator.validateRuntimeExpressions(jspElement, pluginRuntimeAttrNames, java.lang.String.class, jspConfiguration, result, context, prefixToUriMap);
        
        String typeValue = getAttributeValue(jspElement, "type");
        if (typeValue.equals("applet") == false && typeValue.equals("bean") == false) {
            throw new JspTranslationException(jspElement, "jsp.error.plugin.invalid.type", new Object[] { typeValue });
        }
    }

    protected void visitJspSetPropertyStart(Element jspElement) throws JspCoreException {
        List specifiedStandardActionAttrs = getAttributeList(jspElement);
        validateRequiredAttributes(jspElement, specifiedStandardActionAttrs, setPropertyRequiredAttrs);

        validateAttributes(specifiedStandardActionAttrs, jspElement, setPropertyAttrNames);
        ELValidator.validateRuntimeExpressions(jspElement, setPropertyRuntimeAttrNames, java.lang.Object.class, jspConfiguration, result, context, prefixToUriMap);
    }

    protected void visitJspAttributeStart(Element jspElement) throws JspCoreException {
        Node parent = jspElement.getParentNode();
        if (parent.getNodeType() == Node.ELEMENT_NODE) {
            Element parentElement = (Element)parent;
            String name = jspElement.getAttribute("name");
            String prefix = null;
            
            if (name.indexOf(':') != -1) {
                prefix = name.substring(0, name.indexOf(':'));
                name = name.substring(name.indexOf(':') + 1);
            }
            
            String uri = (String)prefixToUriMap.get(parentElement.getPrefix());
            if (uri == null) {
                String actionName = parentElement.getLocalName();
                boolean validActionFound = false;
                for (int i = 0; i < ATTR_STANDARD_ACTIONS.length; i++) {
                    if (actionName.equals(ATTR_STANDARD_ACTIONS[i])) {
                        validActionFound = true;
                        break;    
                    }
                }
                if (validActionFound == false) {
                    throw new JspTranslationException(jspElement, "jsp.error.jsp.attribute.invalid.parent");
                }
                if (prefix != null && parentElement.getLocalName().equals(Constants.JSP_ELEMENT_TYPE) == false) {
                    if (parentElement.getPrefix().equals(prefix) == false) {
                        throw new JspTranslationException(jspElement, "jsp.error.jsp.attribute.prefix.mismatch", new Object[] {parentElement.getPrefix(), prefix});
                    }
                }
            }
            else {
                if (prefix != null) {
                    String tagName = parentElement.getLocalName();
                    TagLibraryInfoImpl tli = (TagLibraryInfoImpl) result.getTagLibMap().get(uri);
                    TagInfo ti = tli.getTag(tagName);
                    if (ti.hasDynamicAttributes() == false && parentElement.getPrefix().equals(prefix) == false) {
                        throw new JspTranslationException(jspElement, "jsp.error.jsp.attribute.prefix.mismatch", new Object[] {parentElement.getPrefix(), prefix});
                    }
                }
            }
        }
        else {
            throw new JspTranslationException(jspElement, "jsp.error.jsp.attribute.invalid.parent");
        }
        
        List specifiedStandardActionAttrs = getAttributeList(jspElement);
        validateRequiredAttributes(jspElement, specifiedStandardActionAttrs, attributeRequiredAttrs);
        if( Float.valueOf(jspConfiguration.getJspVersion()) >= JspConfiguration.twoPointOne){
            validateAttributes(specifiedStandardActionAttrs, jspElement, attributeAttrNames21MR2);
        } else {
            validateAttributes(specifiedStandardActionAttrs, jspElement, attributeAttrNames);
        }

        NodeList childNodes = jspElement.getChildNodes();
        int numberChilds = childNodes.getLength();
        for (int i = 0; i < numberChilds; i++) {
            Node currentNode = childNodes.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) currentNode;
                if (childElement.getNamespaceURI() != null
                    && childElement.getNamespaceURI().equals(Constants.JSP_NAMESPACE)
                    && childElement.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                    throw new JspTranslationException(jspElement, "jsp.error.jsp.attribute.defined.value.jsp.attribute");
                }
            }
        }

    }

    protected void visitJspElementStart(Element jspElement) throws JspCoreException {
        List specifiedStandardActionAttrs = getAttributeList(jspElement, true);

        validateRequiredAttributes(jspElement, specifiedStandardActionAttrs, elementRequiredAttrs);

        // special case.. do not validate child elements retrieved from List object.
        validateAttributes(jspElement, elementAttrNames);
    }

    protected void visitJspOutputStart(Element jspElement) throws JspCoreException {
        if (!jspConfiguration.isXml()) {
            throw new JspTranslationException(jspElement, "jsp.error.jspoutput.xml.only");
        }
        validateAttributes(jspElement, outputAttrNames);
        if (jspElement.hasChildNodes()) {
            throw new JspTranslationException(jspElement, "jsp.error.output.has.body");
        }
        Attr docTypeDeclElement = jspElement.getAttributeNode("omit-xml-declaration");
        Attr docTypeRootElement = jspElement.getAttributeNode("doctype-root-element");
        Attr docTypePublicElement = jspElement.getAttributeNode("doctype-public");
        Attr docTypeSystemElement = jspElement.getAttributeNode("doctype-system");
        
        if (docTypeSystemElement == null) {
            if (docTypeRootElement != null) {
                throw new JspTranslationException(jspElement, "jsp.error.no.doctype-system.attr");
            }
            if (docTypePublicElement != null) {
                throw new JspTranslationException(jspElement, "jsp.error.no.doctype-system.attr");
            }
        }
        else {
            if (docTypeRootElement == null) {
                throw new JspTranslationException(jspElement, "jsp.error.no.doctype-root-element.attr");
            }
        }
        
        // defect 393421 Check for multiple occurrences with different values
        boolean allowJspOutputElementMismatch=JspOptions.ALLOWJSPOUTPUTELEMENTMISMATCH;
        JspOptions jspOptions = (JspOptions)inputMap.get("JspOptions");
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "visitJspOutputStart","jspOptions =[" + jspOptions +"]");
		}
        if (jspOptions != null) {
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
    			logger.logp(Level.FINER, CLASS_NAME, "visitJspOutputStart","jspOptions.isAllowJspOutputElementMismatch():  ["+jspOptions.isAllowJspOutputElementMismatch()+"]");
    		}
    		allowJspOutputElementMismatch=jspOptions.isAllowJspOutputElementMismatch();
        }
        if (!allowJspOutputElementMismatch) {
	        String omitXmlDecl = null;
	        if (docTypeDeclElement!=null) 
	        	omitXmlDecl = docTypeDeclElement.getValue();
	        String doctypeRoot = null;
	        if (docTypeRootElement!=null) 
	        	doctypeRoot = docTypeRootElement.getValue();
	        String doctypePublic = null;
	        if (docTypePublicElement !=null) 
	        	doctypePublic = docTypePublicElement.getValue();
	        String doctypeSystem = null;
	        if (docTypeSystemElement!=null) 
	        	doctypeSystem = docTypeSystemElement.getValue();
	
	        String omitXmlDeclOld = result.getOmitXmlDecl();
	        String doctypeRootOld = result.getDoctypeRoot();
	        String doctypePublicOld = result.getDoctypePublic();
	        String doctypeSystemOld = result.getDoctypeSystem();
			if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
				logger.logp(Level.FINER, CLASS_NAME, "visitJspOutputStart","omitXmlDecl =[" + omitXmlDecl +"] omitXmlDeclOld =[" + omitXmlDeclOld +"]  ");
				logger.logp(Level.FINER, CLASS_NAME, "visitJspOutputStart","doctypeRoot =[" + doctypeRoot +"] doctypeRootOld =[" + doctypeRootOld +"]  ");
				logger.logp(Level.FINER, CLASS_NAME, "visitJspOutputStart","doctypePublic =[" + doctypePublic +"] doctypePublicOld =[" + doctypePublicOld +"]  ");
				logger.logp(Level.FINER, CLASS_NAME, "visitJspOutputStart","doctypeSystem =[" + doctypeSystem +"] doctypeSystemOld =[" + doctypeSystemOld +"]  ");
			}
	
	        if (omitXmlDecl != null && omitXmlDeclOld != null
	                && !omitXmlDecl.equals(omitXmlDeclOld)) {
	        	throw new JspTranslationException(jspElement, "jsp.error.jspoutput.conflict", new Object[] { "omit-xml-declaration", omitXmlDeclOld, omitXmlDecl });
	        }
	
	        if (doctypeRoot != null && doctypeRootOld != null
	                && !doctypeRoot.equals(doctypeRootOld)) {
	        	throw new JspTranslationException(jspElement, "jsp.error.jspoutput.conflict", new Object[] { "doctype-root-element", doctypeRootOld, doctypeRoot });
	        }
	
	        if (doctypePublic != null && doctypePublicOld != null
	                && !doctypePublic.equals(doctypePublicOld)) {
	        	throw new JspTranslationException(jspElement, "jsp.error.jspoutput.conflict", new Object[] { "doctype-public", doctypePublicOld, doctypePublic });
	        }
	
	        if (doctypeSystem != null && doctypeSystemOld != null
	                && !doctypeSystem.equals(doctypeSystemOld)) {
	        	throw new JspTranslationException(jspElement, "jsp.error.jspoutput.conflict", new Object[] { "doctype-system", doctypeSystemOld, doctypeSystem });
	        }
	
	        if (omitXmlDecl != null) {
	            result.setOmitXmlDecl(omitXmlDecl);
	        }
	        if (doctypeRoot != null) {
	        	result.setDoctypeRoot(doctypeRoot);
	        }
	        if (doctypeSystem != null) {
	        	result.setDoctypeSystem(doctypeSystem);
	        }
	        if (doctypePublic != null) {
	        	result.setDoctypePublic(doctypePublic);
	        }
        }
        // END defect 393421 Check for multiple occurrences with different values        
        
    }

    protected void visitCDataTag(CDATASection cdata) throws JspCoreException {
        visitJspELTextStart((Element) cdata.getParentNode(), cdata.getData(), java.lang.String.class);
    }

    protected void visitJspBodyStart(Element jspElement) throws JspCoreException {
        Node parent = jspElement.getParentNode();
        if (parent.getNodeType() == Node.ELEMENT_NODE) {
            Element parentElement = (Element)parent;
            String uri = (String)prefixToUriMap.get(parentElement.getPrefix());
            if (uri == null) {
                String actionName = parentElement.getLocalName();
                boolean validActionFound = false;
                for (int i = 0; i < BODY_STANDARD_ACTIONS.length; i++) {
                    if (actionName.equals(BODY_STANDARD_ACTIONS[i])) {
                        validActionFound = true;
                        break;    
                    }
                }
                if (validActionFound == false) {
                    if (actionName.equals(Constants.JSP_ROOT_TYPE)) {
                        if (jspConfiguration.isXml() == false) {
                            throw new JspTranslationException(jspElement, "jsp.error.jsp.body.invalid.parent");
                        }
                    }
                    else {
                        throw new JspTranslationException(jspElement, "jsp.error.jsp.body.invalid.parent");
                    }
                }
            }
            else {
                String tagName = parentElement.getLocalName();
                TagLibraryInfoImpl tli = (TagLibraryInfoImpl) result.getTagLibMap().get(uri);
                TagInfo ti = tli.getTag(tagName);
                if(ti != null) { //PK44873
                    if (ti.getBodyContent().equalsIgnoreCase(TagInfo.BODY_CONTENT_EMPTY)) {
                        throw new JspTranslationException(jspElement, "jsp.error.jsp.body.parent.nobody");
                    }
                }
            }
        }
        else {
            throw new JspTranslationException(jspElement, "jsp.error.jsp.body.invalid.parent");
        }
    }

    protected void visitIncludeDirectiveStart(Element jspElement) throws JspCoreException {}
    protected void visitTagDirectiveStart(Element jspElement) throws JspCoreException {}
    protected void visitAttributeDirectiveStart(Element jspElement) throws JspCoreException {}
    protected void visitVariableDirectiveStart(Element jspElement) throws JspCoreException {}
    protected void visitUninterpretedTagStart(Element jspElement) throws JspCoreException {
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "visitUninterpretedTagStart","jspElement =[" + jspElement +"]");
		}
        // 245645.1 Start
        String uri = jspElement.getNamespaceURI();
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "visitUninterpretedTagStart","uri =[" + uri +"]");
		}
        if (uri != null) {
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
    			logger.logp(Level.FINER, CLASS_NAME, "visitUninterpretedTagStart","uri =[" + uri +"]");
    		}
            if (uri.startsWith("urn:jsptld:")) {
                uri = uri.substring(uri.indexOf("urn:jsptld:") + 11);
            }
            else if (uri.startsWith("urn:jsptagdir:")) {
                uri = uri.substring(uri.indexOf("urn:jsptagdir:") + 14);
            }
            TagLibraryInfoImpl tli = (TagLibraryInfoImpl) result.getTagLibMap().get(uri);
    
            if (tli == null) {
                tli = tagLibraryCache.getTagLibraryInfo(uri, "", jspUri);
                if (tli != null) {
                    jspElement.setPrefix("");
                    visitCustomTagStart(jspElement);
                }
            }
            else {
                jspElement.setPrefix("");
                visitCustomTagStart(jspElement);
            }            
        }
        // 245645.1 End
        // EL Function support
        NamedNodeMap nodeAttrs = jspElement.getAttributes();
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "visitUninterpretedTagStart","nodeAttrs = ["+nodeAttrs+"]");
			logger.logp(Level.FINER, CLASS_NAME, "visitUninterpretedTagStart","nodeAttrs.getLength() = ["+nodeAttrs.getLength()+"]");
		}
        for (int i = 0; i < nodeAttrs.getLength(); i++) {
            Attr attr = (Attr)nodeAttrs.item(i);
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
    			logger.logp(Level.FINER, CLASS_NAME, "visitUninterpretedTagStart","nodeAttrs attr = ["+attr+"]");
    		}
            visitJspELTextStart(jspElement, attr.getValue(), java.lang.String.class);
        }
    }

    protected void visitJspParamEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspGetPropertyEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspRootEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspFallbackEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspUseBeanEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspForwardEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspPluginEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspSetPropertyEnd(Element jspElement) throws JspCoreException {}
    protected void visitIncludeDirectiveEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspExpressionEnd(Element jspElement) throws JspCoreException {}
    protected void visitPageDirectiveEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspIncludeEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspScriptletEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspDeclarationEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspTextEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspParamsEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspAttributeEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspElementEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspBodyEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspInvokeEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspDoBodyEnd(Element jspElement) throws JspCoreException {}
    protected void visitTagDirectiveEnd(Element jspElement) throws JspCoreException {}
    protected void visitAttributeDirectiveEnd(Element jspElement) throws JspCoreException {}
    protected void visitVariableDirectiveEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspOutputEnd(Element jspElement) throws JspCoreException {}
    protected void visitUninterpretedTagEnd(Element jspElement) throws JspCoreException {
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "visitUninterpretedTagEnd","jspElement =[" + jspElement +"]");
		}
        // 245645.1 Start
        String uri = jspElement.getNamespaceURI();
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "visitUninterpretedTagEnd","uri =[" + uri +"]");
		}
        if (uri != null) {
            if (uri.startsWith("urn:jsptld:")) {
                uri = uri.substring(uri.indexOf("urn:jsptld:") + 11);
            }
            else if (uri.startsWith("urn:jsptagdir:")) {
                uri = uri.substring(uri.indexOf("urn:jsptagdir:") + 14);
            }
            TagLibraryInfoImpl tli = (TagLibraryInfoImpl) result.getTagLibMap().get(uri);
    
            if (tli != null) {
                jspElement.setPrefix("");
                visitCustomTagEnd(jspElement);
            }
        }
        // 245645.1 End
    }
        

    private Vector setScriptingVars(TagVariableInfo[] tagVarInfos, VariableInfo[] varInfos, TagData tagData, int scope, int tagCount) throws JspCoreException {

        if (tagVarInfos.length == 0 && varInfos.length == 0) {
            return (null);
        }

        Vector vec = new Vector();

        Integer ownRange = null;
        if (scope == VariableInfo.AT_BEGIN || scope == VariableInfo.AT_END) {
            if (tagCountStack.size() > 0)
                ownRange = (Integer) tagCountStack.peek();
            else
                ownRange = new Integer(Integer.MAX_VALUE);
        }
        else {
            ownRange = new Integer(tagCount);
        }

        if (varInfos.length > 0) {
            for (int i = 0; i < varInfos.length; i++) {
                if (varInfos[i].getScope() != scope || !varInfos[i].getDeclare()) {
                    continue;
                }
                String varName = varInfos[i].getVarName();

                Integer currentRange = (Integer) scriptVars.get(varName);
                //PK29373
                //PM23597 Don't setDuplicateVars if the variable is of the NESTED scope.
                //The dupVar vector is only used at the tag end and it called - setAtEndScriptingVars.  
                //Since nested variables do not get persisted past the end of the tag, we are ok to check if the scope is nested.  
                //The dupVars was used to handle the case when the variables had to be visible after the tag was completed. 
                if(currentRange != null && scope != VariableInfo.NESTED) {
                    dupFlag = true;
                    setDuplicateVars(varInfos[i]);
                } //PK29373

                if (currentRange == null || ownRange.compareTo(currentRange) > 0) {
                    scriptVars.put(varName, ownRange);
                    vec.add(varInfos[i]);
                }
            }
        }
        else {
            for (int i = 0; i < tagVarInfos.length; i++) {
                if (tagVarInfos[i].getScope() != scope || !tagVarInfos[i].getDeclare()) {
                    continue;
                }
                String varName = tagVarInfos[i].getNameGiven();
                if (varName == null) {
                    varName = tagData.getAttributeString(tagVarInfos[i].getNameFromAttribute());
                    if (varName == null) {
                        throw new JspTranslationException("jsp.error.scripting.variable.missing_name", new Object[] { tagVarInfos[i].getNameFromAttribute()});
                    }
                }

                Integer currentRange = (Integer) scriptVars.get(varName);
                if (currentRange == null || ownRange.compareTo(currentRange) > 0) {
                    scriptVars.put(varName, ownRange);
                    vec.add(tagVarInfos[i]);
                }
            }
        }

        return (vec);
    }

    //PK29373
	private void setDuplicateVars(VariableInfo varInfo) {
        // PM23092
        boolean alreadyDup = false;
        VariableInfo var = null;
        String varInfoVarName = varInfo.getVarName();
        for (int x = 0; x < dupVector.size(); x++) {
            var = (VariableInfo) dupVector.get(x);
            if (var.getVarName().equals(varInfoVarName)) {
                alreadyDup = true;
                break;
            }
        }
        if (!alreadyDup) {
            dupVector.addElement((Object)varInfo);
        }
        // PM23092
	}

	private Vector getDuplicateVars()
	{
		return dupVector;
	}
	//PK29373

    protected void validateTagLib(Document jspDocument, String uri, String prefix, TagLibraryInfoImpl tli) throws JspCoreException {
        TagLibraryValidator tlv = tli.getTagLibraryValidator();
        if (tlv != null) {
            ClassLoader originalClassLoader = ThreadContextHelper.getContextClassLoader();
            ThreadContextHelper.setClassLoader(context.getJspClassloaderContext().getClassLoader());
            try {
                ValidationMessage[] validationMessages = tlv.validate(prefix, uri, new PageDataImpl(jspDocument, tagLibraryCache));
                ThreadContextHelper.setClassLoader(originalClassLoader);
                if (validationMessages != null && validationMessages.length > 0) {
                    StringBuffer errMsg = new StringBuffer();
                    for (int i = 0; i < validationMessages.length; i++) {
                        if (validationMessages[i] != null) {
                            errMsg.append("<p>");
                            if (validationMessages[i].getId() != null) {
                                JspId jspId = new JspId(validationMessages[i].getId());
                                errMsg.append("File ["+jspId.getFilePath() + "] ");
                                errMsg.append("Line [" + jspId.getStartSourceLineNum() + "] ");
                                errMsg.append("Column [" + jspId.getStartSourceColNum() + "] ");
                            }
                            errMsg.append(validationMessages[i].getMessage());
                            errMsg.append("</p>");
                        }
                    }
                    throw new JspTranslationException("jsp.error.tlv.invalid.page", new Object[] { uri, errMsg.toString()});
                }
            }
            catch (Throwable e) {
                throw new JspCoreException(e);
            }
        }
    }

    protected void validateAttributes(List attributeList, Element jspElement, String[] validAttrs) throws JspCoreException {
        String attr;
        for (int i = 0; i < attributeList.size(); i++) {
            attr = (String) attributeList.get(i);
            if (attr.equals("jsp:id"))
                continue;
            if (attr.startsWith("xmlns")) // 245645.1
                continue; // 245645.1
            boolean valid = false;
            for (int j = 0; j < validAttrs.length; j++) {
                if (attr.equals(validAttrs[j])) {
                    valid = true;
                    break;
                }
            }
            if (valid == false) {
                throw new JspTranslationException(jspElement, "jsp.error.unknown.attribute", new Object[] { attr, jspElement.getTagName()});
            }
        }
    }

    protected void validateAttributes(Element jspElement, String[] validAttrs) throws JspCoreException {
        NamedNodeMap attrs = jspElement.getAttributes();

        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            String uri = attr.getNamespaceURI();
            //String localName = attr.getLocalName();
            //String prefix = attr.getPrefix();
            String fullName = attr.getNodeName();
            
            if (uri != null) {
                if (attr.getLocalName().equals("id") && uri.equals(Constants.JSP_NAMESPACE)) {
                    continue; 
                }
            }
            
            if (fullName.startsWith("xmlns"))
                continue;
            boolean valid = false;
            for (int j = 0; j < validAttrs.length; j++) {
                if (attr.getNodeName().equals(validAttrs[j])) {
                    valid = true;
                    break;
                }
            }
            if (valid == false) {
                throw new JspTranslationException(jspElement, "jsp.error.unknown.attribute", new Object[] { attr.getNodeName(), jspElement.getTagName()});
            }
        }
    }

    protected void validateEmptyBody(Element jspElement) throws JspCoreException {
        NodeList childNodes = jspElement.getChildNodes();
        if (childNodes != null) {
            CDATASection cdata = null;
            Element childElement = null;
            String value = null;
            for (int j = 0; j < childNodes.getLength(); j++) {
                Node n = childNodes.item(j);
                // template text not allowed
                if (n.getNodeType() == Node.CDATA_SECTION_NODE) {
                    cdata = (CDATASection) n;
                    value = cdata.getData();
                    if (value.trim().length() > 0) {
                        throw new JspTranslationException(
                            jspElement,
                            "jsp.error.jspbody.emptybody.only",
                            new Object[] { jspElement.getNodeName(), "[" + n.getNodeName() + "] : " + value });
                    }
                }
                else if (n instanceof Element) {
                    childElement = (Element) n;
                    // only the jsp:attribute tag is allowed
                    if (!(childElement.getNamespaceURI().equals(Constants.JSP_NAMESPACE) && childElement.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE))) {
                        throw new JspTranslationException(
                            jspElement,
                            "jsp.error.jspbody.emptybody.only",
                            new Object[] { jspElement.getNodeName(), childElement.getNamespaceURI() + " : " + childElement.getLocalName()});
                    }
                }
            }
        }
    }

    protected void visitJspELTextStart(Element jspElement, String cdata, Class expectedType) throws JspCoreException {
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "visitJspELTextStart","cdata =[" + cdata +"]");
			logger.logp(Level.FINER, CLASS_NAME, "visitJspELTextStart","cdata.indexOf('${') =[" + cdata.indexOf("${") +"]");
			logger.logp(Level.FINER, CLASS_NAME, "visitJspELTextStart","cdata.indexOf('#{') =[" + cdata.indexOf("#{") +"]");
		}
        if (cdata.indexOf("${") == -1 && cdata.indexOf("#{") == -1) { // quick check to see if EL may exist
            return;
        }

		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "visitJspELTextStart","calling ELValidator.getELExpressions");
		}
        String[] expressions = ELValidator.getELExpressions(cdata, jspConfiguration, evalQuotedAndEscapedExpression);
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "visitJspELTextStart","expressions returned: ["+expressions+"]");
		}

        if (expressions != null) {
            for (int i = 0; i < expressions.length; i++) {
        		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
        			logger.logp(Level.FINER, CLASS_NAME, "visitJspELTextStart","about to validate expression: ["+expressions[i]+"]");
        		}
                ELValidator.validateELExpression(jspElement, expressions[i], expectedType, jspConfiguration, result, context, prefixToUriMap);
            }
        }
    }

    protected String getAttributeValue(Element element, String attributeName) {
        /* this needs to be moved to a util class to be shared with generator code(copied from PageTranslationTimeGenerator)*/
        String attributeValue = element.getAttribute(attributeName);

        if (attributeValue.equals("")) {
            NodeList nl = element.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) n;
                    if (e.getNamespaceURI() != null && e.getNamespaceURI().equals(Constants.JSP_NAMESPACE) && e.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                        String name = e.getAttribute("name");
                        if (name.indexOf(':') != -1) {
                            name = name.substring(name.indexOf(':') + 1);
                        }
                        if (name.equals(attributeName)) {
                            Node attrChildNode = e.getFirstChild();

                            CDATASection cdata = null;
                            if (attrChildNode.getNodeType() == Node.CDATA_SECTION_NODE) {
                                cdata = (CDATASection) attrChildNode;
                            }
                            else if (
                                attrChildNode instanceof Element
                                    && attrChildNode.getNamespaceURI().equals(Constants.JSP_NAMESPACE)
                                    && attrChildNode.getLocalName().equals(Constants.JSP_TEXT_TYPE)) {
                                Element jspElement = (Element) attrChildNode;
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
        if (attributeValue == null) {
            attributeValue = "";
        }
        return attributeValue;
    }

    protected static List getAttributeList(Element jspElement) throws JspCoreException {
        return getAttributeList(jspElement, false);
    }

    protected static List getAttributeList(Element jspElement, boolean ignoreDuplicates) throws JspCoreException {
        ArrayList<String> attributeList = new ArrayList<String>(11);
        NamedNodeMap nnmTop = jspElement.getAttributes();
        for (int i = 0; i < nnmTop.getLength(); i++) {
            Node parentNode = nnmTop.item(i);
            String nodeName = parentNode.getNodeName();
            if (nodeName.equals("jsp:id") == false) {
                if (attributeList.contains(nodeName)) {

                    if (ignoreDuplicates == false) {
                        throw new JspTranslationException(jspElement, "jsp.error.multiple.attribute.definitions", new Object[] { nodeName, jspElement.getTagName()});
                    }
                }
                else {
                    attributeList.add(nodeName);
                }
            }
        }
        NodeList nl = jspElement.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) n;
                if (e.getNamespaceURI() != null && e.getNamespaceURI().equals(Constants.JSP_NAMESPACE) && e.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                    NamedNodeMap nnmChild = e.getAttributes();
                    String nodeName = e.getAttribute("name");
                    if (nodeName.indexOf(':') != -1) {
                        nodeName = nodeName.substring(nodeName.indexOf(':') + 1);
                    }
                    if (attributeList.contains(nodeName)) {
                        if (ignoreDuplicates == false) {
                            throw new JspTranslationException(jspElement, "jsp.error.multiple.attribute.definitions", new Object[] { nodeName, jspElement.getTagName()});
                        }
                    }
                    else {
                        attributeList.add(nodeName);
                    }
                }
            }
        }
        return attributeList;
    }

    protected CustomTagInstance validateCustomTagAttributeValues(Element jspElement, TagAttributeInfo[] attributes, boolean hasDynamicAttributes, TagLibraryInfoImpl tli) throws JspCoreException {
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","Entry");
		}

        Hashtable attributeMap = new Hashtable(11);
        boolean isScriptless = true;
        boolean checkDeferred = !jspConfiguration.isDeferredSyntaxAllowedAsLiteral();
        if (tli.getRequiredVersion()!=null) { 
            checkDeferred = checkDeferred && !(tli.getRequiredVersion().equals("2.0") || tli.getRequiredVersion().equals("1.2")); // defect 409810
        }

        NamedNodeMap nnmTop = jspElement.getAttributes();
        for (int i = 0; i < nnmTop.getLength(); i++) {
            Node parentNode = nnmTop.item(i);
            String nodeName = parentNode.getNodeName();
            String nodeValue = parentNode.getNodeValue();

            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
            	logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","nodeName =[" + nodeName + "]");
            	logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","nodeValue =[" + nodeValue+ "]");
            }

            if (nodeName.equals("jsp:id") == false && nodeName.startsWith("xmlns") == false) {
        		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
        			logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","NOT jsp:id and NOT xmlns");
        		}
                TagAttributeInfo tai = getTagAttributeInfo(attributes, nodeName);
        		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
        			logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","attributes ["+attributes+"] nodeName ["+nodeName+"]  nodeValue ["+nodeValue+"]");
        			logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","tai ["+tai+"]");
        		}
                boolean dynamicDeferredMethod = false;
                String dynamicDeferredMethodSignature = null;
                if (tai == null) {
                    if (hasDynamicAttributes == false) {
                        throw new JspTranslationException(jspElement, "jsp.error.unable.locate.tagname", new Object[] { nodeName });
                    }
                }
                //512063 removed else: This should be called for dynamicAttributes too since these can also be deferred
                //else {
                isScriptless = validateCustomTagAttribute(jspElement, attributeMap, isScriptless, checkDeferred, nodeName, tai, nodeValue, dynamicDeferredMethod, dynamicDeferredMethodSignature);            		
                //}
            }
        }
        NodeList nl = jspElement.getChildNodes();
        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
        	logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","nl.getLength() =[" + nl.getLength()+"]");
        }

        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
            	logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","n.getNodeName() =[" + n.getNodeName()+"]");
            	logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","n.getNodeType() =[" + n.getNodeType()+"]");
            }

            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) n;
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                	logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","e.getNamespaceURI() =[" + e.getNamespaceURI()+"]");
                	logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","e.getLocalName() =[" + e.getLocalName()+"]");
                }

                if (e.getNamespaceURI() != null && e.getNamespaceURI().equals(Constants.JSP_NAMESPACE) && e.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                    String nodeName = e.getAttribute("name");
                    if (nodeName.indexOf(":") != -1) {
                        nodeName = nodeName.substring(nodeName.indexOf(':')+1);
                    }
                    boolean trim = true;
                    if (e.getAttribute("trim").equals("") == false) {
                        trim = Boolean.valueOf(e.getAttribute("trim")).booleanValue();
                    }

                    NodeList childAttributeNodeList = e.getChildNodes();
                    TagAttributeInfo tai = getTagAttributeInfo(attributes, nodeName);
                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                        logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","attributes ["+attributes+"] nodeName ["+nodeName+"]");
                        logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","tai ["+tai+"]");
                    }

                    boolean isFragment = false;
                    
                    boolean dynamicDeferredMethod = false;
                    String dynamicDeferredMethodSignature = null;
                    if (tai == null) {
                        if (hasDynamicAttributes == false) {
                            throw new JspTranslationException(jspElement, "jsp.error.unable.locate.tagname", new Object[] { nodeName });
                        } 
                        /*519690 - I'm not sure how to support dynamic - DeferredAttributes, but this isn't it.
                        else {
                            //hasDynamicAttributes ... need to set properties
                            //deferredMethod, deferredMethodSignature, and deferredValue should already have been set
                            Attr deferredMethod = e.getAttributeNode("deferredMethod"); // jsp2.1ELwork
                            if (deferredMethod!=null) {
                                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                                    logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","deferredMethod value = " + deferredMethod.getValue());
                                    logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","tai ["+tai+"]");
                                }
                                dynamicDeferredMethod = new Boolean(deferredMethod.getValue());
                                if (dynamicDeferredMethod) {
                                    Attr deferredMethodSignature = e.getAttributeNode("deferredMethodSignature"); // jsp2.1ELwork
                                    if (dynamicDeferredMethodSignature == null) {
                                        //512063: not sure if this deferredMethodSignature is correct.
                                        dynamicDeferredMethodSignature = "void "+nodeName+"();";
                                        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                                            logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","deferredMethodSignature is null ... setting to " + dynamicDeferredMethodSignature);
                                        }
                                    } else {
                                        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                                            logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","deferredMethodSignature value = " + deferredMethodSignature.getValue());
                                        }
                                        dynamicDeferredMethodSignature = deferredMethodSignature.getValue();
                                    }
                                }
                            }
                        }*/
                    }
                    else {
                        isFragment = tai.isFragment();    
                    }

                    if (e.hasChildNodes()) {
                        int numChildren = childAttributeNodeList.getLength();
                        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                        	logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","numChildren =[" + numChildren+"]");
                        }
                        if (numChildren == 1) {
                            Node childAttrNode = e.getFirstChild();
                            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                            	logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","childAttrNode.getNodeName(): " + childAttrNode.getNodeName());
                            	logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","childAttrNode.getNodeType(): " + childAttrNode.getNodeType());
                            }
                            
                            //This section is for an attribute specified like this:
                            //<jsp:attribute name="test">just text in here</jsp:attribute>
                            // or an el statement like this: <jsp:attribute name="test">${hello}</jsp:attribute>
                            if (childAttrNode.getNodeType() == Node.CDATA_SECTION_NODE) {
                                CDATASection cdata = (CDATASection) childAttrNode;
                                String nodeValue = cdata.getData();
                                if (trim == true) {
                                    nodeValue = nodeValue.trim();
                                }
                                isScriptless = validateCustomTagAttribute(jspElement, attributeMap, isScriptless, checkDeferred, nodeName, tai, nodeValue, dynamicDeferredMethod, dynamicDeferredMethodSignature);           		
                            }                            
                            // PK62934 start
                            //This section is for an attribute specified like this:
                            //<jsp:attribute name="test"><%=request.getAttribute("abc")%></jsp:attribute>
                            // The expression is only one child
                            else if (childAttrNode.getNodeType() == Node.ELEMENT_NODE) {
                            	if (!areChildrenScriptless(e)) {
                            
                            		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                            			logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","Element children are not scriptless.");
                            		}
                            		if (tai!=null && tai.canBeRequestTime() == false) {
                            			throw new JspTranslationException(jspElement, "jsp.error.attribute.cannot.be.request.time", new Object[] { nodeName, childAttrNode.getNodeValue()});
                            		}
                            		else {
                            			isScriptless = false;
                            			attributeMap.put(nodeName, TagData.REQUEST_TIME_VALUE);
                            		}
                            	}
                            	else {
                            		attributeMap.put(nodeName, TagData.REQUEST_TIME_VALUE);
                                }
                            }
                            //PK62934 end

                        }
                        else {
                            int childrenWithValues = 0;
                            boolean previousCData = false; //PK62934
                            
                            for (int child = 0; child < numChildren; child++) {
                                Node currNode = childAttributeNodeList.item(child);
                                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                                	logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","currNode.getNodeName(): " + currNode.getNodeName());
                                    logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","currNode.getNodeType(): " + currNode.getNodeType());
                                    logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","currNode.getNodeValue(): " + currNode.getNodeValue());
                                }
                                if (currNode.getNodeType() == Node.CDATA_SECTION_NODE) {
                                    CDATASection cdata = (CDATASection) currNode;
                                    String nodeValue = cdata.getData();
                                    if (trim == true) {
                                        nodeValue = nodeValue.trim();
                                    }
                                    if (nodeValue.length() > 0) {
                                    	//PK62934 - only count cdata once...expressions will break up cdata and mess up the counter.
                                    	if (!previousCData) {
                                    		childrenWithValues++;
                                    	}
                                    	previousCData = true; //PK62934
                                    }
                                }
                                // PK62934 start
                                else if (currNode.getNodeType() == Node.ELEMENT_NODE) {
                                	Element e1 = (Element) currNode;
                                	
                                    if (!areChildrenScriptless(e)) {
                                    	if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                                			logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues 2","2. Element children are not scriptless.");
                                		}
                                    	if (tai!=null && tai.canBeRequestTime() == false) {
                                            throw new JspTranslationException(jspElement, "jsp.error.attribute.cannot.be.request.time", new Object[] { nodeName, currNode.getNodeValue()});
                                        }
                                        else {
                                        	isScriptless = false;
                                        }
                                    }
                                }
                                // PK62934 end

                                else {
                                    childrenWithValues++;
                                }

                            }
                            //PK62934 changing isFragment check to true instead of false.
                            if (isFragment == true && childrenWithValues > 1) {
                            	if(logger.isLoggable(Level.FINER)){
                            		logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","childrenWithValues =["+childrenWithValues+"]");                        		
                            	}
                                throw new JspTranslationException(jspElement, "jsp.error.attribute.cannot.be.request.time.fragment", new Object[] { nodeName });
                            }
                            else {
                                attributeMap.put(nodeName, TagData.REQUEST_TIME_VALUE);
                            }
                        }
                    }
                    else {
                        attributeMap.put(nodeName, "");
                    }
                }
            }
        }
        return new CustomTagInstance(attributeMap, isScriptless);
    }

	private boolean validateCustomTagAttribute(Element jspElement, Hashtable attributeMap, boolean isScriptless, boolean checkDeferred, String nodeName, TagAttributeInfo tai, String nodeValue, boolean dynamicDeferredMethod, String dynamicDeferredMethodSignature) throws JspTranslationException, JspCoreException {
		boolean isExpr = JspTranslatorUtil.isExpression(nodeValue);
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","about to call JspTranslatorUtil.isELInterpreterInput(nodeValue, jspConfiguration)");
		}
		boolean isElExpr = JspTranslatorUtil.isELInterpreterInput(nodeValue, jspConfiguration, checkDeferred);
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","isExpr =["+isExpr+"] isElExpr= ["+isElExpr+"]");
			if (tai!=null) {
			    logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","tai.canBeRequestTime() =["+tai.canBeRequestTime()+"] tai.isDeferredMethod()= ["+tai.isDeferredMethod()+"] tai.isDeferredValue()= ["+tai.isDeferredValue()+"]");
            } else {
                logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","tai is null, must be a dynamic attribute");
            }
		}
		
        boolean elExpression = false;
        boolean deferred = false;
        boolean deferredValueIsLiteral = jspConfiguration.isDeferredSyntaxAllowedAsLiteral();

        //if the JSP version is either 1.2 or 2.0 or if isDeferredSyntaxAllowedAsLiteral is set, checkDeferred is false.
        //if the JSP version is at the WebApp2.3 spec, elIgnored is true
        ELNode.Nodes el = null;
        if (!isExpr) { // !expression of type <%= %>
            //check for EL only if we're NOT to ignore it
            if (!jspConfiguration.elIgnored() && !jspConfiguration.elIgnoredSetTrueInPage() && !jspConfiguration.elIgnoredSetTrueInPropGrp()) {
                el = ELParser.parse(nodeValue);
                Iterator<ELNode> nodes = el.iterator();
                while (nodes.hasNext()) {
                    ELNode node = nodes.next();
                    if (node instanceof ELNode.Root) {
                        if (((ELNode.Root) node).getType() == '$') {
                            elExpression = true;
                        } 
                        else if (checkDeferred && ((ELNode.Root) node).getType() == '#') {
                            //checkDeferred only set when 2.1 and !isDeferredSyntaxAllowedAsLiteral
                            elExpression = true;
                            deferred = true;
                        }
                    }
                }
            }
        }
        
        boolean expression = isExpr || elExpression;		
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","nodeValue ["+nodeValue+"]");
			logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","deferred ["+deferred+"]");
			logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","deferredValueIsLiteral ["+deferredValueIsLiteral+"]");
			logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","checkDeferred ["+checkDeferred+"]");
			logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","expression ["+expression+"]");
			logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","isExpr ["+isExpr+"]");
			logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","jspConfiguration.elIgnored() ["+jspConfiguration.elIgnored()+"]");
		}
        boolean thisIsADynamicAttribute=false;
        if (tai==null) {
            thisIsADynamicAttribute=true; 
        }

        //PK79754 - start
        //add check for checkDeferred
        if (thisIsADynamicAttribute || 
            (tai.canBeRequestTime() || ((checkDeferred) && (tai.isDeferredMethod() || tai.isDeferredValue())))) { // jsp2.1ELwork
        //PK79754 - end
			if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
				logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","in tai check nodeValue ["+nodeValue+"]");
				logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","in tai check deferred ["+deferred+"]");
				logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","in tai check deferredValueIsLiteral ["+deferredValueIsLiteral+"]");
				logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","in tai check checkDeferred ["+checkDeferred+"]");
				logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","in tai check expression ["+expression+"]");
			}
            String expectedType = null;
            //512063: Setting default values in case the tai is null (for dynamic attributes)
            boolean taiIsDeferredMethod = dynamicDeferredMethod;//passed in for dynamicAttribute (will be overwritten if tai is defined)
            boolean taiIsDeferredValue = deferred;//value for dynamicAttribute (will be overwritten if tai is defined)
            String taiGetMethodSignature = dynamicDeferredMethodSignature; //default value for method signature for dynamicAttribute (will be overwritten if tai is defined)
            String taiGetExpectedTypeName = "java.lang.Object"; //default value for dynamicAttribute (will be overwritten if tai is defined)
            String taiGetName = nodeName;
            boolean taiCanBeRequestTime = true; //default value for dynamicAttribute (will be overwritten if tai is defined)
            //512063: Setting values if the tai is not null.
            if (tai!=null) {
                taiIsDeferredMethod = tai.isDeferredMethod();
                taiIsDeferredValue = tai.isDeferredValue();
                taiGetMethodSignature = tai.getMethodSignature();
                taiGetExpectedTypeName = tai.getExpectedTypeName();
                taiGetName = tai.getName();
                taiCanBeRequestTime = tai.canBeRequestTime();
            }

            if (!expression) {                            
                //removed with 514246
                //    if (deferredValueIsLiteral && !jspConfiguration.isDeferredSyntaxAllowedAsLiteral()) {
                //        throw new JspTranslationException(jspElement, "jsp.error.attribute.cannot.be.request.time", new Object[] { nodeName, nodeValue });
                //    }

                if (taiIsDeferredMethod) { //only do this for 2.1 apps ... jsf handles this for older apps
                    // The String litteral must be castable to what is declared as type
                    // for the attribute
                    String m = taiGetMethodSignature;
                    if (m != null) {
                        int rti = m.trim().indexOf(' ');
                        if (rti > 0) {
                            expectedType = m.substring(0, rti).trim();
                        }
                    } else {
                        expectedType = "java.lang.Object";
                    }
                }
                if (taiIsDeferredValue) { //only do this for 2.1 apps ... jsf handles this for older apps
                    // The String litteral must be castable to what is declared as type
                    // for the attribute
                    expectedType = taiGetExpectedTypeName;
                }
                if (expectedType != null) {
                    Class expectedClass = String.class;
                    try {
                        expectedClass = JspTranslatorUtil.toClass(expectedType, context.getJspClassloaderContext().getClassLoader());
                    } catch (ClassNotFoundException cnfe) {
                        throw new JspTranslationException(jspElement, "jsp.error.unknown_attribute_type", new Object[] { taiGetName, expectedType });
                    }
                    // Check casting
                    try {
                        ELCheckType(nodeValue, expectedClass);
                    } catch (Exception excep) {
                        throw new JspTranslationException(jspElement, "jsp.error.coerce_to_type", new Object[] { taiGetName, expectedType, nodeValue });
                    }
                }
		        attributeMap.put(nodeName, nodeValue);
		    } else {
                if (isExpr) {
		            isScriptless = false;
		        }
				if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
					logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","isScriptless =["+isScriptless+"]");
				}
		        
		        if (deferred && !taiIsDeferredMethod && !taiIsDeferredValue) {
		            // No deferred expressions allowed for this attribute
		            throw new JspTranslationException(jspElement, "jsp.error.attribute.cannot.be.request.time", new Object[] { nodeName, nodeValue });
		            //err.jspError(n, "jsp.error.attribute.custom.non_rt_with_expr",
		            //        tai.getName());
		        }
		        if (!deferred && !taiCanBeRequestTime) {
		            // Must be rtexprvalue
		            throw new JspTranslationException(jspElement, "jsp.error.attribute.cannot.be.request.time", new Object[] { nodeName, nodeValue });
		            //err.jspError(n, "jsp.error.attribute.custom.non_rt_with_expr",
		            //        tai.getName());
		        }
		        if (elExpression) {                                
		            expectedType = taiGetExpectedTypeName; // !!!!!!!!!!!!!!!!
		            if (expectedType == null)
		                expectedType = "java.lang.Object";
		    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
		    			logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttribute","about to call validateELExpression, expectedType =["+expectedType+"]");
		    		}
		            ELValidator.validateELExpression(jspElement, nodeValue, expectedType, jspConfiguration, context, result, prefixToUriMap);
		        }
		        attributeMap.put(nodeName, TagData.REQUEST_TIME_VALUE);
		    }
		} else {
		    // Attribute does not accept any expressions.
		    // Make sure its value does not contain any.
		    if (expression) {
		        throw new JspTranslationException(jspElement, "jsp.error.attribute.cannot.be.request.time", new Object[] { nodeName, nodeValue });
		    }
            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
        		logger.logp(Level.FINER, CLASS_NAME, "validateCustomTagAttributeValues","Adding to attribute map nodeName =[" + nodeName + "] nodeValue =[" + nodeValue +"]");
            }
		    attributeMap.put(nodeName, nodeValue);            			
		}
		return isScriptless;
	}
	
	//method used to replace ELSupport.checkType
	private final static void ELCheckType(final Object obj, final Class<?> type) throws ELException {
	    if (String.class.equals(type)) {
	        ELSupport.coerceToString(obj);
        }
        if (ELArithmetic.isNumberType(type)) {
            ELSupport.coerceToNumber(obj, type);
        }
        if (Character.class.equals(type) || Character.TYPE == type) {
            ELSupport.coerceToCharacter(obj);
        }
        if (Boolean.class.equals(type) || Boolean.TYPE == type) {
            ELSupport.coerceToBoolean(obj);
        }
        if (type.isEnum()) {
            ELSupport.coerceToEnum(obj, type);
        }
	}

    protected void validateRequiredAttributes(Element jspElement, List specifiedActionAttrs, String[] requiredAttrs) throws JspCoreException {
        for (int i = 0; i < requiredAttrs.length; i++) {
            if (specifiedActionAttrs.contains(requiredAttrs[i]) == false) {
                throw new JspTranslationException(jspElement, "jsp.error.missing.required.attribute", new Object[] { requiredAttrs[i], jspElement.getTagName()});

            }
        }
    }

    protected void validateCustomTagAttributes(Element jspElement, TagAttributeInfo[] attributes) throws JspCoreException {
        NodeList nl = jspElement.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) n;
                if (e.getNamespaceURI() != null && e.getNamespaceURI().equals(Constants.JSP_NAMESPACE) && e.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                    NamedNodeMap nnmChild = e.getAttributes();
                    String nodeName = e.getAttribute("name");
                    if (nodeName.indexOf(":") != -1) {
                        nodeName = nodeName.substring(nodeName.indexOf(':')+1);
                    }
                    for (int attrIndex = 0; attrIndex < attributes.length; attrIndex++) {
                        TagAttributeInfo tai = attributes[attrIndex];
                        String taiName = tai.getName();
                        if (taiName != null && taiName.equals(nodeName) && tai.isFragment()) {
                            NodeList attrNodeList = e.getChildNodes();
                            for (int attrNodesIndex = 0; attrNodesIndex < attrNodeList.getLength(); attrNodesIndex++) {
                                Node attrNode = attrNodeList.item(attrNodesIndex);
                                if (attrNode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element attrNodeElement = (Element) attrNode;
                                    if (attrNodeElement.getNamespaceURI() != null && attrNodeElement.getNamespaceURI().equals(Constants.JSP_NAMESPACE)) {
                                        if (attrNodeElement.getLocalName().equals(Constants.JSP_SCRIPTLET_TYPE)
                                            || attrNodeElement.getLocalName().equals(Constants.JSP_EXPRESSION_TYPE)
                                            || attrNodeElement.getLocalName().equals(Constants.JSP_DECLARATION_TYPE)) {
                                            throw new JspTranslationException(
                                                jspElement,
                                                "jsp.error.attribute.fragment.cannot.be.scriplet",
                                                new Object[] { attrNodeElement.getNodeName()});
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected TagAttributeInfo getTagAttributeInfo(TagAttributeInfo[] attributes, String nodeName) {
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i].getName().equals(nodeName))
                return attributes[i];
        }
        return null;
    }
    
    protected boolean areChildrenScriptless(Element jspElement) {
        NodeList nl = jspElement.getChildNodes();
        
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element)n;
                if (e.getNamespaceURI() != null && e.getNamespaceURI().equals(Constants.JSP_NAMESPACE)) {
                    if (e.getLocalName().equals(Constants.JSP_SCRIPTLET_TYPE) ||
                        e.getLocalName().equals(Constants.JSP_EXPRESSION_TYPE) ||
                        e.getLocalName().equals(Constants.JSP_DECLARATION_TYPE)) {
                        return false;       
                    }
                    else {
                        for (int j = 0; j < ATTR_STANDARD_ACTIONS.length; j++) {
                            if (e.getLocalName().equals(ATTR_STANDARD_ACTIONS[j])) {
                                NamedNodeMap attrs = e.getAttributes();
                                if (attrs != null) {
                                    for (int k = 0; k < attrs.getLength(); k++) {
                                        if (JspTranslatorUtil.isExpression(attrs.item(k).getNodeValue(), true)) {
                                            return false;       
                                        } 
                                    }
                                }
                                if (!areChildrenScriptless(e))
                                    return false;
                            }
                        }
                    }
                } else { //end of if (e.getNamespaceURI... 
                    //PK47368
                    if (!areChildrenScriptless(e))
                        return false;
                    //PK47368
                }
            }
        }
        return true;
    }
    
    private ValidateTagInfo getTagInfo (Element jspElement, String uri, String prefix, String tagName) throws JspCoreException {
         TagLibraryInfoImpl tli = (TagLibraryInfoImpl) result.getTagLibMap().get(uri);
         boolean isTagFile = false;
         
         if (tli == null) {
             tli = tagLibraryCache.getTagLibraryInfo(uri, prefix, jspUri);
             if (tli != null) {
                 if (tli.getTldFilePath() != null &&
                     (tli.isContainer() || (!tli.isContainer() && tli.getInputSource().getAbsoluteURL().getProtocol().equals("file"))) && //TODO: figure out how to tell if it's in a jar, if its in a Container. 
                     tagLibraryCache.getImplicitTagLibPrefixMap().containsValue(uri) == false) {
                     result.getDependencyList().add(tli.getTldFilePath());
                 }
                 result.getTagLibMap().put(uri, tli);
                 tli.setTagLibMap(result.getTagLibMap()); //jsp2.1work
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                     logger.logp(Level.FINER, CLASS_NAME, "getTagInfo","getTagLibraryInfos() =[" + tli.getTagLibraryInfos() +"]");
                }
                 prefixToUriMap.put(prefix, uri);
                 validateTagLib(jspElement.getOwnerDocument(), uri, prefix, tli);
             }
             else {
                 if (jspConfiguration.isXml() == false) {
                      if (!uri.equalsIgnoreCase (Constants.JSP_NAMESPACE)) {
                           throw new JspTranslationException(jspElement, "jsp.error.tld.not.found", new Object[] { uri, prefix });
                      }
                      
                      else {
                           return null;
                      }
                 }
                 else
                     return null;
             }
         }

         TagInfo ti = tli.getTag(tagName);
         if (ti == null) {
             TagFileInfo tfi = tli.getTagFile(tagName);
             if (tfi == null) {
                 throw new JspTranslationException(jspElement, "jsp.error.tagfile.not.found", new Object[] { tagName });
             }
             if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                 logger.logp(Level.FINER, CLASS_NAME, "getTagInfo", "prefix : " + prefix + "; tagName : " + tagName  + " = Tag file path : " + tfi.getPath());
             }
             ti = tfi.getTagInfo();
             tagLibraryCache.addTagFileClassInfo(tfi);
             isTagFile = true;
         }
         return new ValidateTagInfo (ti, tli, isTagFile);
    }
    
    protected boolean shouldSkipChildrenForThisVisitor () {
         return true;
    }
    
    protected boolean isElementTagDependent (Element jspElement) throws JspCoreException {
         String uri = jspElement.getNamespaceURI();
         String prefix = jspElement.getPrefix();
         String tagName = jspElement.getLocalName();
         ValidateTagInfo vti = getTagInfo (jspElement, uri, prefix, tagName);
         
         if (vti == null) {
              return false;
         }
         
         String bodyContent = vti.getTagInfo().getBodyContent();
         
         if (bodyContent == null) {
              return false;
         }
         
         return bodyContent.equalsIgnoreCase (TagInfo.BODY_CONTENT_TAG_DEPENDENT);
    }
    
    protected class ValidateTagInfo {
         private TagInfo tagInfo;
         private TagLibraryInfoImpl tli;
         private boolean tagFile;
         
         protected ValidateTagInfo (TagInfo tagInfo, TagLibraryInfoImpl tli, boolean tagFile) {
              this.tagInfo = tagInfo;
              this.tli = tli;
              this.tagFile = tagFile;
         }
         
         protected TagInfo getTagInfo () {
              return this.tagInfo;
         }
         
         protected TagLibraryInfoImpl getTLI () {
              return this.tli;
         }
         
         protected boolean isTagFile () {
              return this.tagFile;
         }
    }
    
    protected class CustomTagInstance {
        private Hashtable attributes;
        private boolean isScriptless = true;

        protected CustomTagInstance(Hashtable attrs, boolean isScriptless) {
            this.attributes = attrs;
            this.isScriptless = isScriptless;
        }

        protected Hashtable getAttributes() {
            return this.attributes;
        }
        protected boolean isScriptless() {
            return this.isScriptless;
        }
    }
}
