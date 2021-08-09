/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
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

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.validator.ValidateResult;
import com.ibm.wsspi.jsp.context.JspCoreContext;

public class TextGenerator extends CodeGeneratorBase {
    protected static final int MAXSIZE = 32 * 1024;
    protected static final String STANDARD_PREFIX = "private final static char[] _jsp_string";
    protected static final String STANDARD_SUFFIX = "\".toCharArray();";
    protected boolean elIgnored = false;
    protected boolean deferredIgnored = false;
    protected boolean generate = true;
    protected boolean trim = false;
    protected String  attrName = null;
    protected Integer nextStringNum = null;
    
    public void init(JspCoreContext ctxt,
                     Element element, 
                     ValidateResult validatorResult,
                     JspVisitorInputMap inputMap,
                     ArrayList methodWriterList,
                     FragmentHelperClassWriter fragmentHelperClassWriter, 
                     HashMap persistentData,
                     JspConfiguration jspConfiguration,
                     JspOptions jspOptions) 
        throws JspCoreException {
        super.init(ctxt, element, validatorResult, inputMap, methodWriterList, fragmentHelperClassWriter, persistentData, jspConfiguration, jspOptions);            
        elIgnored = jspConfiguration.elIgnored();
        deferredIgnored = jspConfiguration.isDeferredSyntaxAllowedAsLiteral();
    }

    public void startGeneration(int section, JavaCodeWriter writer) throws JspCoreException {
        if (section == CodeGenerationPhase.CLASS_SECTION ||
            section == CodeGenerationPhase.METHOD_SECTION) {
            if (element.getFirstChild() instanceof CDATASection) {
                if (section == CodeGenerationPhase.CLASS_SECTION) {
                    nextStringNum = (Integer)persistentData.get("nextStringNum");
                    if (nextStringNum == null) {
                        nextStringNum = new Integer(0);
                    }
                }
                
                Node parent = element.getParentNode();
                if (parent.getNamespaceURI() != null && parent.getNamespaceURI().equals(Constants.JSP_NAMESPACE)) {
                /* If the any sibling nodes are jsp:attribute then don't print out output. jsp:body will handle this */
                    NodeList childNodes = parent.getChildNodes();
                    for (int i = 0; i < childNodes.getLength(); i++) {
                        Node child = childNodes.item(i);
                        if (child instanceof Element) {
                            Element childElement = (Element)child;
                            if (childElement.getNamespaceURI() != null && childElement.getNamespaceURI().equals(Constants.JSP_NAMESPACE) &&
                                childElement.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                                return;                                    
                            }
                        }
                    }
                }
                        
                if (parent.getNamespaceURI() != null && parent.getNamespaceURI().equals(Constants.JSP_NAMESPACE) &&
                    parent.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                    
                    /* If the parent is a jsp:attribute and is literal text then we */
                    /* need to change prefix and sufiix for code generated */
                
                    HashMap jspAttributes = (HashMap)persistentData.get("jspAttributes");
                    if (jspAttributes != null) {
                        Node parentParent = parent.getParentNode();
                        ArrayList jspAttributeList = (ArrayList)jspAttributes.get(parentParent);
                        if (jspAttributeList != null) {
                            for (Iterator itr = jspAttributeList.iterator(); itr.hasNext();) {
                                AttributeGenerator.JspAttribute jspAttribute = (AttributeGenerator.JspAttribute)itr.next();
                                if (jspAttribute.getJspAttrElement().equals(parent)) {
                                    if (jspAttribute.isLiteral()) {
                                        attrName = jspAttribute.getVarName();
                                    }
                                    trim = jspAttribute.trim();
                                }
                            }
                        }
                    }
                }
                
                CDATASection cdata = (CDATASection) element.getFirstChild();
                String data = cdata.getData();
                // 232157 Start
                if (trim)
                    data = data.trim();
                // 232157 End
                    
                char[] chars = data.toCharArray();
    
                int current = 0;
                int limit = chars.length;

                //227804
                if (section == CodeGenerationPhase.METHOD_SECTION) {
                	writeDebugStartBegin(writer);
                }
                //PI79800
                int newCurrentStringCount = generateChunk(chars, writer, section, nextStringNum.intValue());
                
                if (section == CodeGenerationPhase.CLASS_SECTION) {
                    persistentData.put("nextStringNum", new Integer(newCurrentStringCount));
                }
                else { //227804
                	writeDebugStartEnd(writer);
                }
            }
        }
    }

    public void endGeneration(int section, JavaCodeWriter writer)  throws JspCoreException {
    }
    
    protected int generateChunk(char[] chars, JavaCodeWriter writer, int section, int currentStringCount) {
        return generateChunk(chars, writer, 0, chars.length, section, currentStringCount);
    }
    
    protected int generateChunk(char[] chars, 
                                 JavaCodeWriter writer, 
                                 int from, 
                                 int to, 
                                 int section,
                                 int currentStringCount) {
        int newCurrentStringCount = currentStringCount;                                      
        StringBuffer sb = new StringBuffer();
        boolean inElExpression = false;
        int arraySize = chars.length;
        boolean inQuotes = false;//PK01920, flag to determine if we are in double or single quotes 
        boolean usingSingleQuotes = false;//PK01920, flag to determine if we are in single quotes
        boolean inEscape = false;//PK01920, flag to determine if we are in an escape
        boolean doNotEscapeWhitespaceCharsInExpression = jspOptions.isDoNotEscapeWhitespaceCharsInExpression(); //PM94792
        //PK65013 - start
        String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
        if (isTagFile && jspOptions.isModifyPageContextVariable()) {
            pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
        }
        //PK65013 - end
        for (int i = from; i < to; i++) {
            char ch = chars[i];
            switch (ch) {
            	
            case '\'' : {
                if (inElExpression && !inEscape) {//PK01920, if we are in expression language text
                	if (inQuotes && usingSingleQuotes) {
                		inQuotes = false;//PK01920, if we were previously in quotes we now are not
                		usingSingleQuotes = false;
                	}
                	else if(!inQuotes ){	//PK73104 - if already in quotes, treat single quotes as regular text
                			usingSingleQuotes = true;
                			inQuotes = true;
                	}
                }
                inEscape = false;//PK01920, have been through 1 + iterations after an escape could have been found, so setting to false
                sb.append((char) ch);//PK01920, append this character
                break;
            }
            
                case '"' : {
                	if (inElExpression) {//PK01920, if we are in expression language text
	                    if (inQuotes && !usingSingleQuotes && !inEscape) {
	                        inQuotes = false;
	                    }
	                    else {
	                        inQuotes = true;
	                        //usingSingleQuotes = false;  	//PK25893
	                    }
                	}
                    inEscape = false;
                    if (!elIgnored && inElExpression) {
                        sb.append("\"");//PK01920, if we are in the element language place an escaped double quote on the output array.  Why don't we do this for single quotes?
                    }
                    else {
                        sb.append("\\\"");//PK01920, if we are not in the element language, then we have to double escape the quote 
                    }
                    break;
                }
                
                case '\\' : {//PK01920, if we have a backslash
                    int nextChar = ' ';
                    if (i+1 < arraySize) {//PK01920, get the next character in the array
                        nextChar = chars[i+1];
                    }
                    int prevChar = ' ';//PK01920, get the previous character in the array
                    if (i-1 >= 0) {
                    	prevChar = chars[i-1];
                    }
                    
                    if (inEscape) {
                        inEscape = false;//PK01920, toggle inEscape
                    }
                    else {
                        if (nextChar == '\'' || nextChar == '\"' || nextChar == '\\' || nextChar == '$') {//PK01920, we are assuming these are the only characters that we need to escape.  Is this true for the element language as well?
                            inEscape = true;
                        }
                    }
                    
                    boolean skip = false;//PK01920, set up a flag to show whether we are in <% %> and need to skip
                    
                    if ((prevChar == '%' && nextChar == '>') ||
                    	(prevChar == '<' && nextChar == '%')) {//PK01920, i have problems with the second part of this... this would assume that we are in a <\% - i don't think we have to check for that here
                    	skip = true;//PK01920, skip this 
                    }
            		if (elIgnored == false && (nextChar == '$' || nextChar == '#')) {//PK01920, if we are in the e.l. then skip
                    	skip = true;
            		}
            		if (!skip) {//PK01920, else append the two excaped backslashes.  I think we could do this just as an if & else... it would be a complex if statement but i think skip is unecessary
                    	//PK76583 start
                    	if(	inElExpression && inQuotes ){	// if in EL Expression and next char is a single or double quote, append a single escaped backslash
                    		sb.append("\\");					// later processing of EL expression adds the additional escape chars back in (see GeneratorUtils.escape())
                    	}else{					//otherwise append a double escaped backslash
                    		sb.append("\\\\");
                    	}
                    	//PK76583 end
                    }
                    break;
           		}
                    
                case '\r' :{
                    if (inElExpression && doNotEscapeWhitespaceCharsInExpression) { //PM94792
                            sb.append((char) ch);
                            break;
                    }
                    else{
                            sb.append("\\r");
                            break;
                    }
                }  
                case '\n' :{
                        if (inElExpression && doNotEscapeWhitespaceCharsInExpression) {  //PM94792
                                sb.append((char) ch);
                                break;
                        }
                        else{
                                sb.append("\\n");
                                break;
                        }
                }  
                case '\t' : {
                        if (inElExpression && doNotEscapeWhitespaceCharsInExpression) {  //PM94792
                                sb.append((char) ch);
                                break;
                        }
                        else{
                                sb.append("\\t");
                                break;
                        }    
                
                }
                case '#' :
                    if (deferredIgnored) {
                        sb.append((char) ch);
                        break;
                    } //else we do the following in the '$' case
                case '$' :{
                    if (elIgnored == false) {
                        if ((i+1 < arraySize) && (chars[i+1] == '{')) {
                            if (!inQuotes && !inEscape) {//PK01920, if we are not within quotes or an escape then do not treat specially 
                                if (sb.length() > 0) {
                                    if (section == CodeGenerationPhase.CLASS_SECTION) {
                                        Integer existingStringNumber = lookForExistingString(sb.toString());
                                        if (existingStringNumber == null) {
                                            int stringId = ++newCurrentStringCount;
                                            addExisitingString(sb.toString(), stringId);
                                            sb.insert(0,STANDARD_PREFIX+stringId+" = \"");
                                            writer.print(sb.toString());
                                            writer.print(STANDARD_SUFFIX);
                                            writer.println();
                                        }
                                    }
                                    else if (section == CodeGenerationPhase.METHOD_SECTION) {
                                        Integer existingStringNumber = lookForExistingString(sb.toString());
                                        int stringId = 0;
                                        if (existingStringNumber != null) {
                                            stringId = existingStringNumber.intValue();    
                                        }
                                        else {
                                            stringId = ++newCurrentStringCount;
                                        }
                                        if (attrName != null) {
                                            writer.print("String " + attrName + " = String.valueOf(_jsp_string"+stringId+")");
                                            writer.print(";");
                                            writer.println();
                                        }
                                        else {
                                            writer.print("out.write(_jsp_string"+stringId+");");
                                            writer.println();
                                        }
                                    }
                                    sb = new StringBuffer();
                                }
                                inElExpression = true;
                            }
                        }
                        /*
                         * If we are here, it means that we processed the EL expression either as escaped text or as an actual EL expression
                         * and this means that inEscape should be false, because we want to start checking for new EL expressions (we just finished processing this $).
                         * 
                         * Example:
                         * 
                         * \$
                         * \${pageContext.request.contextPath}
                         * \${pageContext.request.contextPath}
                         * 
                         * Output:
                         * $
                         * /webModuleContextPath
                         * ${pageContext.request.contextPath}
                         * 
                         * Expected output:
                         * 
                         * $
                         * ${pageContext.request.contextPath}
                         * ${pageContext.request.contextPath}
                         */
                        inEscape = false; //PI67257
                    }
                    sb.append((char) ch);
                    break;                
                }
                
                case '}' : {
                    if (elIgnored == false && inElExpression) {
                        if (inQuotes) {//PK01920, i don't understand why we get to ignore the $ and { case here
                            sb.append((char) ch);
                        }
                        else {
                            if (section == CodeGenerationPhase.METHOD_SECTION) {
                                sb.append((char) ch);
                                writer.print("out.write(");
                                writer.print(GeneratorUtils.interpreterCall(isTagFile, 
                                                                             sb.toString(), 
                                                                             String.class,
                                                                             "_jspx_fnmap",
                                                                             false,
                                                                             pageContextVar)); //PK65013
                                writer.print(");");
                                writer.println();
                            }
                            sb = new StringBuffer();
                            inElExpression = false;
                        }
                    }
                    else {
                        sb.append((char) ch);
                    }
                    break;
                }
                
                default :
                    sb.append((char) ch);
            }
        }
        if (sb.length() > 0) {
            if (section == CodeGenerationPhase.CLASS_SECTION) {
            	Integer existingStringNumber = lookForExistingString(sb.toString());
                if (existingStringNumber == null) {
                    int stringId = ++newCurrentStringCount;
                    addExisitingString(sb.toString(), stringId);
                    sb.insert(0,STANDARD_PREFIX+stringId+" = \"");
                    writer.print(sb.toString());
                    writer.print(STANDARD_SUFFIX);
                    writer.println();
                }
            }
            else if (section == CodeGenerationPhase.METHOD_SECTION) {
                Integer existingStringNumber = lookForExistingString(sb.toString());
                int stringId = 0;
                if (existingStringNumber != null) {
                    stringId = existingStringNumber.intValue();    
                }
                else {
                    stringId = ++newCurrentStringCount;
                }
                
                if (attrName != null) {
                    writer.print("String " + attrName + " = String.valueOf(_jsp_string"+stringId+")");
                    writer.print(";");
                    writer.println();
                }
                else {
                    writer.print("out.write(_jsp_string"+stringId+");");
                    writer.println();
                }
            }
        }
        
        return newCurrentStringCount;
    }
    
    private Integer lookForExistingString(String text) {
        Integer existingStringId = null;
        HashMap existingStringMap = (HashMap)persistentData.get("existingStringMap");
        if (existingStringMap == null) {
            existingStringMap = new HashMap();
            persistentData.put("existingStringMap", existingStringMap);
        }
        existingStringId = (Integer)existingStringMap.get(text);
        return (existingStringId);
    }
    
    private void addExisitingString(String text, int stringId) {
        HashMap existingStringMap = (HashMap)persistentData.get("existingStringMap");
        if (existingStringMap == null) {
            existingStringMap = new HashMap();
            persistentData.put("existingStringMap", existingStringMap);
        }
        existingStringMap.put(text, new Integer(stringId));
    }
}
