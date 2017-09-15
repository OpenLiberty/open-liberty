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
package com.ibm.ws.jsp.translator.visitor.tagfilescan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagVariableInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.taglib.TagFileTagInfo;
import com.ibm.ws.jsp.taglib.TagLibraryInfoImpl;
import com.ibm.ws.jsp.translator.JspTranslationException;
import com.ibm.ws.jsp.translator.utils.JspId;
import com.ibm.ws.jsp.translator.utils.JspTranslatorUtil;
import com.ibm.ws.jsp.translator.utils.NameMangler;
import com.ibm.ws.jsp.translator.visitor.JspVisitor;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.JspVisitorResult;
import com.ibm.ws.jsp.translator.visitor.configuration.JspVisitorUsage;
import com.ibm.wsspi.jsp.context.JspCoreContext;

public class TagFileScanVisitor extends JspVisitor {  

	static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.translator.visitor.tagfilescan.TagFileScanVisitor";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}
    private TagLibraryInfoImpl tli = null;
    private String tagFilePath = null;

    private String name = null;
    private String tagclass = null;
    private TagExtraInfo tei = null;
    private String bodycontent = TagInfo.BODY_CONTENT_SCRIPTLESS;
    private String description = null;
    private String displayName = null;
    private String smallIcon = null;
    private String largeIcon = null;
    private String dynamicAttributes = null;
    private boolean bodyContentIsDefault = true;	//516829

    private ArrayList attributes = new ArrayList();
    private ArrayList variables = new ArrayList();
    private HashMap tagFileUniqueNameGivenVariableNames = new HashMap(19);
    private HashMap tagFileUniqueNameFromAttributeVariableNames = new HashMap(19);

    public TagFileScanVisitor(
        JspVisitorUsage visitorUsage,
        JspConfiguration jspConfiguration,
        JspCoreContext context,
        HashMap resultMap,
        JspVisitorInputMap inputMap)
        throws JspCoreException {
        super(visitorUsage, jspConfiguration, context, resultMap, inputMap);
        name = (String) inputMap.get("TagFileName");
        displayName = name;
        tli = (TagLibraryInfoImpl) inputMap.get("TagLibraryInfo");
        tagFilePath = (String) inputMap.get("TagFilePath");

        String className = tagFilePath.substring(tagFilePath.lastIndexOf('/') + 1);
        className = className.substring(0, className.indexOf(".tag"));
        className = NameMangler.mangleClassName(className);

        String dirName = tagFilePath.substring(0, tagFilePath.lastIndexOf('/'));
        if (dirName.startsWith("/WEB-INF/tags")) {
            dirName = dirName.substring(dirName.indexOf("/WEB-INF/tags") + 13);
        } else if (dirName.startsWith("/META-INF/tags")) {
            dirName = dirName.substring(dirName.indexOf("/META-INF/tags") + 14);
        }
        
        //PM70267 start
        if (dirName.indexOf("-") > -1) {
            dirName = NameMangler.handlePackageName(dirName);
            if (!dirName.startsWith(".")) {
                dirName = "." + dirName;
            }
        }
        //PM70267 end

        dirName = dirName.replace('/', '.');
        tagclass = Constants.TAGFILE_PACKAGE_NAME + "." + tli.getOriginatorId() + dirName + "." + className;
    }

    public JspVisitorResult getResult() throws JspCoreException {
        // defect 301032 begin
    	// Check that var.name-from-attributes has valid values.
        Iterator iter = tagFileUniqueNameFromAttributeVariableNames.keySet().iterator();
        while (iter.hasNext()) {
            String nameFrom = (String) iter.next();
            Element nameEntry = (Element) tagFileUniqueNameGivenVariableNames.get(nameFrom);
            Element nameFromEntry = (Element) tagFileUniqueNameFromAttributeVariableNames.get(nameFrom);
            if (nameEntry == null) {
                throw new JspTranslationException(
                                nameFromEntry,
                                "jsp.error.tagfile.nameFrom.noAttribute",
                                new Object[] { nameFrom});
            } else {
                Attr typeAttr = nameEntry.getAttributeNode("type");
                String type = "java.lang.String";
                if (typeAttr!=null) 
                	type = typeAttr.getValue();
                Attr requiredAttr = nameEntry.getAttributeNode("required");
                boolean required = false;
                if (requiredAttr!=null)
                	required = JspTranslatorUtil.booleanValue(requiredAttr.getValue());
                Attr rtexprvalueAttr = nameEntry.getAttributeNode("rtexprvalue");
                boolean rtexprvalue = true;
                if (rtexprvalueAttr!=null)
                	rtexprvalue = JspTranslatorUtil.booleanValue(rtexprvalueAttr.getValue());
                if (! "java.lang.String".equals(type)
                        || ! required
                        || rtexprvalue){
                    String jspIdString = nameEntry.getAttributeNS(Constants.JSP_NAMESPACE, "id");
                    JspId jspId = null;
                    String lineNum="unknown";
                    if (jspIdString.equals("") == false) { 
                        jspId = new JspId(jspIdString);
                        lineNum = String.valueOf(jspId.getStartSourceLineNum());
                    }
                	
                    throw new JspTranslationException(
                            nameFromEntry,
                            "jsp.error.tagfile.nameFrom.badAttribute",
                            new Object[] { lineNum ,nameFrom});
                 }
            }
        }
        // defect 301032 end

        TagVariableInfo[] tagVariableInfos = new TagVariableInfo[variables.size()];
        tagVariableInfos = (TagVariableInfo[]) variables.toArray(tagVariableInfos);

        TagAttributeInfo[] tagAttributeInfo = new TagAttributeInfo[attributes.size()];
        tagAttributeInfo = (TagAttributeInfo[]) attributes.toArray(tagAttributeInfo);

        TagInfo ti =
            new TagFileTagInfo(
                name,
                tagclass,
                bodycontent,
                description,
                tli,
                tei,
                tagAttributeInfo,
                displayName,
                smallIcon,
                largeIcon,
                tagVariableInfos,
                dynamicAttributes);

        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
			logger.logp(Level.FINEST, CLASS_NAME, "getResult","About to call setTagInfo(ti), ti =[\n" + ti +"]");
		}

        TagFileScanResult result = new TagFileScanResult(visitorUsage.getJspVisitorDefinition().getId());
        result.setTagInfo(ti);
        return (result);
    }

    protected void visitTagDirectiveStart(Element jspElement) throws JspCoreException {
    	// jsp2.1ELwork
    	//516829 - if body-content was set to default, check for conflicts with a null value instead of the default
    	if(bodyContentIsDefault){
    		String newbodycontent = checkConflict(jspElement, null, "body-content");
    		if (newbodycontent!=null){
    			bodycontent = newbodycontent;
    			bodyContentIsDefault = false;
    		}
    	}else{	// if bodyContent is not set to default, check for conflicting values per usual
    		bodycontent = checkConflict(jspElement, bodycontent, "body-content");	
    	}
        
        // added back in with servlet version check to ensure we don't break old apps
        if (bodycontent != null 
        		&& jspConfiguration.getServletVersion().equals("2.5")
                && !bodycontent.equalsIgnoreCase(TagInfo.BODY_CONTENT_EMPTY)
                && !bodycontent.equalsIgnoreCase(TagInfo.BODY_CONTENT_TAG_DEPENDENT)
                && !bodycontent.equalsIgnoreCase(TagInfo.BODY_CONTENT_SCRIPTLESS)) {
            throw new JspTranslationException(
            		jspElement,
                    "jsp.error.tagdirective.badbodycontent",
                    new Object[] { bodycontent });
        }
        //end 516829
        
        //PK93886 - start
        // do not need to checkConflict because you can have a different display-name than the tag name.
        Attr displayNameAttr = jspElement.getAttributeNode("display-name");
        if (displayNameAttr != null) {
            displayName = displayNameAttr.getValue();
        }
        //PK93886 - end
        dynamicAttributes = checkConflict(jspElement, dynamicAttributes, "dynamic-attributes");
        smallIcon = checkConflict(jspElement, smallIcon, "small-icon");
        largeIcon = checkConflict(jspElement, largeIcon, "large-icon");
        description = checkConflict(jspElement, description, "description");
    }

	// new method for jsp2.1ELwork
    private String checkConflict(Element elem, String oldAttrValue, String attr)
			throws JspCoreException {

		String result = oldAttrValue;
		String attrValue = null; 
		if (elem.getAttributeNode(attr) != null ) {
			attrValue = elem.getAttributeNode(attr).getValue();
		}
		if (attrValue != null) {
			if (oldAttrValue != null && !oldAttrValue.equals(attrValue)) {
	            throw new JspTranslationException(
	                    elem,
	                    "jsp.error.tag.conflict.attr",
	                    new Object[] { attr, oldAttrValue, attrValue });
			}
			result = attrValue;
		}
		return result;
	}
    
    protected void visitAttributeDirectiveStart(Element jspElement) throws JspCoreException {
        Attr nameAttr = jspElement.getAttributeNode("name");
        Attr requiredAttr = jspElement.getAttributeNode("required");
        Attr fragmentAttr = jspElement.getAttributeNode("fragment");
        Attr rtexprvalueAttr = jspElement.getAttributeNode("rtexprvalue");
        Attr typeAttr = jspElement.getAttributeNode("type");
        Attr deferredValueAttr = jspElement.getAttributeNode("deferredValue");  // jsp2.1ELwork 
        Attr deferredValueTypeAttr = jspElement.getAttributeNode("deferredValueType"); // jsp2.1ELwork 
        Attr deferredMethodAttr = jspElement.getAttributeNode("deferredMethod"); // jsp2.1ELwork 
        Attr deferredMethodSignatureAttr = jspElement.getAttributeNode("deferredMethodSignature"); // jsp2.1ELwork 

        String name = "";
        boolean required = false;
        boolean fragment = false;
        boolean rtexprvalue = true;
        String type = null;

        // JSP 2.1 Table JSP.8-3
        // handle deferredValue and deferredValueType
        boolean deferredValue = false;
        boolean deferredValueSpecified = false;
        String deferredValueString = null; 
        if (deferredValueAttr != null ){
        	deferredValueString = deferredValueAttr.getValue();
        }
        if (deferredValueString != null) {
            deferredValueSpecified = true;
            deferredValue = JspTranslatorUtil.booleanValue(deferredValueString);
        }
        String deferredValueType = null;
        if (deferredValueTypeAttr != null) {
        	deferredValueType = deferredValueTypeAttr.getValue();
        }
        if (deferredValueType != null) {
            if (deferredValueSpecified && !deferredValue) {
	            throw new JspTranslationException(jspElement,
	                    "jsp.error.deferredvaluetypewithoutdeferredvalue");
            } else {
                deferredValue = true;
            }
        } else if (deferredValue) {
            deferredValueType = "java.lang.Object";
        } else {
            deferredValueType = "java.lang.String";
        }

        // JSP 2.1 Table JSP.8-3
        // handle deferredMethod and deferredMethodSignature
        boolean deferredMethod = false;
        boolean deferredMethodSpecified = false;
        String deferredMethodString = null;
        if (deferredMethodAttr != null) {
        	deferredMethodString = deferredMethodAttr.getValue();
        }
        if (deferredMethodString != null) {
            deferredMethodSpecified = true;
            deferredMethod = JspTranslatorUtil.booleanValue(deferredMethodString);
        }
        String deferredMethodSignature = null;
        if (deferredMethodSignatureAttr != null ){
        	deferredMethodSignature = deferredMethodSignatureAttr.getValue();
        }
        if (deferredMethodSignature != null) {
            if (deferredMethodSpecified && !deferredMethod) {
	            throw new JspTranslationException(jspElement, 
	            		"jsp.error.deferredmethodsignaturewithoutdeferredmethod");
            } else {
                deferredMethod = true;
            }
        } else if (deferredMethod) {
            deferredMethodSignature = "void methodname()";
        }

        if (deferredMethod && deferredValue) {
            throw new JspTranslationException(jspElement, "jsp.error.deferredmethodandvalue");
        }
        
        
        if (nameAttr != null)
            name = nameAttr.getValue();

        if (requiredAttr != null)
        	required = JspTranslatorUtil.booleanValue(requiredAttr.getValue());

        if (fragmentAttr != null)
        	fragment = JspTranslatorUtil.booleanValue(fragmentAttr.getValue());

        if (typeAttr != null){
        	type = typeAttr.getValue();
        }
        if (rtexprvalueAttr != null){
        	rtexprvalue = JspTranslatorUtil.booleanValue(rtexprvalueAttr.getValue());
        }
        if (fragment) {
            // type is fixed to "JspFragment" and a translation error
            // must occur if specified.
            if (type != null) {
				throw new JspTranslationException(jspElement,"jsp.error.illegal.fragment.and.type", new Object []{ typeAttr.getValue()});
            }
            // rtexprvalue is fixed to "true" and a translation error
            // must occur if specified.
            rtexprvalue = true;
            if (rtexprvalueAttr != null){
           		throw new JspTranslationException(jspElement,"jsp.error.illegal.fragment.and.rtexprvalue", new Object[]{rtexprvalueAttr.getValue()});
            }
            	
        } else {
            if (type == null) {
				type = "java.lang.String";
			} else {
				try {
					if (isPrimitive(type)) {
						throw new JspTranslationException(jspElement,
								"jsp.error.illegal.type.primitive",
								new Object[] { type });
					}
					Class typeAttrClass = JspTranslatorUtil.toClass(type, context.getJspClassloaderContext().getClassLoader()); //PI18025                                       
					
				} catch (ClassNotFoundException cnfe) {
					throw new JspTranslationException(
							jspElement,
							"jsp.error.tagfile.cannot.locate.class.to.validate.primitive",
							new Object[] { type }, cnfe);
				}            	
            }            	
            
            if (deferredValue) {
                type = ValueExpression.class.getName();
            } else if (deferredMethod) {
                type = MethodExpression.class.getName();
            }
        }

        if (("2.0".equals(tli.getRequiredVersion()) || "1.2".equals(tli.getRequiredVersion())) // defect 409810
                && (deferredMethodSpecified || deferredMethod
                        || deferredValueSpecified || deferredValue)) {
			throw new JspTranslationException(jspElement,"jsp.error.invalid.version", new Object []{ tagFilePath});
        }
        
        TagAttributeInfo tai = new TagAttributeInfo(name, required, type, rtexprvalue, fragment, null, deferredValue,
                deferredMethod, deferredValueType, deferredMethodSignature);
        attributes.add(tai);
        checkValidTagName(jspElement, tagFileUniqueNameGivenVariableNames, name);
    }
    
    private boolean isPrimitive(String classType){
    	classType = classType.trim();
 		boolean primitive = false;
 		String[] primitiveList = {"boolean", "byte", "char", "short", "int", "long", "float", "double"};
 		for (int i=0; i < primitiveList.length; i++){
 			if(classType.equals(primitiveList[i])){
 				return true;
 			}
    	} 		
 		return primitive;
    }

    protected void visitVariableDirectiveStart(Element jspElement) throws JspCoreException {
        Attr nameGivenAttr = jspElement.getAttributeNode("name-given");
        Attr nameFromAttributeAttr = jspElement.getAttributeNode("name-from-attribute");
        Attr aliasAttr = jspElement.getAttributeNode("alias");
        Attr variableClassAttr = jspElement.getAttributeNode("variable-class");
        Attr declareAttr = jspElement.getAttributeNode("declare");
        Attr scopeAttr = jspElement.getAttributeNode("scope");

        String nameGiven = null;
        String nameFromAttribute = null;

        String variableClass = "java.lang.String";
        boolean declare = true;
        int scope = VariableInfo.NESTED;

        if (nameGivenAttr != null) {
            nameGiven = nameGivenAttr.getValue();
        }
        if (nameFromAttributeAttr != null) {
            nameFromAttribute = nameFromAttributeAttr.getValue();
        }

        if (nameGivenAttr != null && nameFromAttributeAttr != null) {
            throw new JspTranslationException(
                jspElement,
                "variable.attribute.nameGiven.nameFromAttribute.not.both",
                new Object[] { jspElement.getTagName(), nameGiven, nameFromAttribute });
        } else if (nameGivenAttr == null && nameFromAttributeAttr == null) {
            throw new JspTranslationException(
                jspElement,
                "missing.required.variable.attribute.nameGiven.nameFromAttribute",
                new Object[] { jspElement.getTagName()});
        } else if (nameFromAttributeAttr != null && aliasAttr == null) {
            throw new JspTranslationException(
                jspElement,
                "required.attribute.alias.required.if.nameFromAttribute.specified",
                new Object[] { jspElement.getTagName(), nameFromAttribute });
        } else if (nameGivenAttr != null && aliasAttr != null) {
            throw new JspTranslationException(
                jspElement,
                "attribute.alias.not.permitted.if.namegiven.specified",
                new Object[] { jspElement.getTagName(), nameGiven, aliasAttr.getValue()});
        }

        if (variableClassAttr != null)
            variableClass = variableClassAttr.getValue();
        if (declareAttr != null)
        	declare = JspTranslatorUtil.booleanValue(declareAttr.getValue());


        if (scopeAttr != null)
            scope = convertScopeToInt(jspElement, scopeAttr.getValue());

        if (aliasAttr != null) {
            nameGiven = aliasAttr.getValue();
        }
        TagVariableInfo tvi = new TagVariableInfo(nameGiven, nameFromAttribute, variableClass, declare, scope);
        variables.add(tvi);

        if (nameGivenAttr != null) {// defect 301032
            checkValidTagName(jspElement, tagFileUniqueNameGivenVariableNames, nameGiven);
        } else { // name-from-attribute specified.
            checkValidTagName(jspElement, tagFileUniqueNameFromAttributeVariableNames, nameFromAttribute);
            checkValidTagName(jspElement, tagFileUniqueNameGivenVariableNames, aliasAttr.getValue());// defect 301032
        }
    }

    protected void checkValidTagName(Element jspElement, HashMap map, String name) throws JspCoreException {
        Object priorElementObj = map.put(name, jspElement);
        if (priorElementObj != null) {
            Element priorElement = (Element) priorElementObj;
            throw new JspTranslationException(
                jspElement,
                "multiple.occurences.attribute.tagfile.name",
                new Object[] { jspElement.getTagName(), priorElement.getTagName(), name });
        }
    }

    protected int convertScopeToInt(Element jspElement, String scope) throws JspCoreException {
        if (scope.equals("AT_BEGIN"))
            return VariableInfo.AT_BEGIN;
        else if (scope.equals("NESTED"))
            return VariableInfo.NESTED;
        else if (scope.equals("AT_END"))
            return VariableInfo.AT_END;
        else
            throw new JspTranslationException(
                jspElement,
                "invalid.value.for.variable.directive.attribute.scope",
                new Object[] { jspElement.getTagName(), scope });
    }

    protected void visitJspRootStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspRootEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspTextStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspTextEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitIncludeDirectiveStart(Element jspElement) throws JspCoreException {
    }
    protected void visitIncludeDirectiveEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitPageDirectiveStart(Element jspElement) throws JspCoreException {
    }
    protected void visitPageDirectiveEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitTagDirectiveEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitAttributeDirectiveEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitVariableDirectiveEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspDeclarationStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspDeclarationEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspExpressionStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspExpressionEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspScriptletStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspScriptletEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspParamStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspParamEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspParamsStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspParamsEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspFallbackStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspFallbackEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspIncludeStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspIncludeEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspForwardStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspForwardEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspUseBeanStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspUseBeanEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspGetPropertyStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspGetPropertyEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspSetPropertyStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspSetPropertyEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspPluginStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspPluginEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitCustomTagStart(Element jspElement) throws JspCoreException {
    }
    protected void visitCustomTagEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspAttributeStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspAttributeEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspElementStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspElementEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspBodyStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspBodyEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspInvokeStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspInvokeEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspDoBodyStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspDoBodyEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitJspOutputStart(Element jspElement) throws JspCoreException {
    }
    protected void visitJspOutputEnd(Element jspElement) throws JspCoreException {
    }
    protected void visitUninterpretedTagStart(Element jspElement) throws JspCoreException {
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
			logger.logp(Level.FINEST, CLASS_NAME, "visitUninterpretedTagStart","jspElement =[" + jspElement +"]");
		}
    }
    protected void visitUninterpretedTagEnd(Element jspElement) throws JspCoreException {
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
			logger.logp(Level.FINEST, CLASS_NAME, "visitUninterpretedTagEnd","jspElement =[" + jspElement +"]");
		}
    }
    
    protected void visitCDataTag(CDATASection cdata) throws JspCoreException {}
}
