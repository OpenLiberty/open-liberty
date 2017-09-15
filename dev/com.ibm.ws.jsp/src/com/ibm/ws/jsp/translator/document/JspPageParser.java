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
package com.ibm.ws.jsp.translator.document;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.XMLConstants; //PI43036
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.CDATASection;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap; //248567
import org.w3c.dom.Node;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.translator.JspTranslationException;
import com.ibm.ws.jsp.translator.visitor.xml.ParserFactory;
import com.ibm.ws.util.WSUtil;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.jsp.resource.JspInputSource;
import com.ibm.wsspi.webcontainer.WCCustomProperties;

public class JspPageParser {
    private static final int EXPRESSION_ELEMENT = 1;
    private static final int DECLARATION_ELEMENT = 2;
    private static final int SCRIPTLET_ELEMENT = 3;
    private static final int DIRECTIVE_ELEMENT = 4;
    private static final int COMMENT_ELEMENT = 5;
    private static final String WEB_INF_TAGS = "/WEB-INF/tags";

    protected Reader jspReader = null;
    protected StringBuffer templateText = new StringBuffer();
    protected int currentLineNum = 1;
    protected int currentColumnNum = 0;
    protected int textCurrLineNum = 1;
    protected int textCurrColNum = 1;

    protected int jspSyntaxLineNum = 1;
    protected int jspSyntaxColNum = 1;
    protected int actionLineCount = 1;

    protected JspInputSource inputSource = null;
    protected String resolvedRelativeURL = null;
    protected String encodedRelativeURL = null;
    protected Stack elements = new Stack();
    protected HashMap<String,String>  tagPrefixes = new HashMap<String,String> ();
    protected HashMap<String,String> nonCustomTagPrefixMap = new HashMap<String,String>();  // defect 396002
    protected Document jspDocument = null;
    protected Element jspRootElement = null;
    protected JspCoreContext context = null;
    protected JspConfiguration jspConfiguration = null;
    protected JspOptions jspOptions = null;  //396002
    protected Stack directoryStack = null;
    protected Stack dependencyStack = null;
    protected List dependencyList = null;
    protected Map cdataJspIdMap = null;
    protected Map implicitTagLibMap = null;
    protected boolean pageEncodingSpecified = false;

    public JspPageParser(
        Reader jspReader,
        JspInputSource inputSource,
        String resolvedRelativeURL,
        JspCoreContext context,
        Stack directoryStack,
        JspConfiguration jspConfiguration,
        JspOptions jspOptions,     //396002
        Stack dependencyStack,
        List dependencyList,
        Map cdataJspIdMap,
        Map implicitTagLibMap)
        throws JspCoreException {
        this.jspReader = jspReader;
        this.inputSource = inputSource;

        this.resolvedRelativeURL = resolvedRelativeURL;
        try {
            encodedRelativeURL = URLEncoder.encode(resolvedRelativeURL, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new JspCoreException(e);
        }
        this.context = context;
        this.directoryStack = directoryStack;
        this.jspConfiguration = jspConfiguration;
        this.jspOptions = jspOptions;   //396002
        this.dependencyStack = dependencyStack;
        this.dependencyList = dependencyList;
        this.cdataJspIdMap = cdataJspIdMap;
        this.implicitTagLibMap = implicitTagLibMap;

        try {
            jspDocument = ParserFactory.newDocument (false, false);
            jspRootElement = jspDocument.createElementNS(Constants.JSP_NAMESPACE, "jsp:root");
            jspRootElement.setAttributeNS(Constants.JSP_NAMESPACE, "jsp:id", encodedRelativeURL + "[0,1,1]");
            jspDocument.appendChild(jspRootElement);
            elements.push(jspRootElement);
        }
        catch (ParserConfigurationException e) {
            throw new JspCoreException(e);
        }

        if (this.implicitTagLibMap != null && this.implicitTagLibMap.size() > 0) {
            tagPrefixes.putAll(implicitTagLibMap);
        }
    }

    public Document parse() throws JspCoreException {
        try {
            if (jspConfiguration.getPreludeList().size() > 0) {
                insertImplictIncludes(jspConfiguration.getPreludeList());
            }

            int character = 0;

            while ((character = readCharacter()) != -1) {
                switch (character) {
                    case '<' :
                        {
                            jspSyntaxLineNum = currentLineNum;
                            jspSyntaxColNum = currentColumnNum;
                            processJspSyntax();
                            break;
                        }

                    default :
                        {
                            templateText.append((char) character);
                            break;
                        }
                }
            }
            processTemplateText();

            jspRootElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:jsp", Constants.JSP_NAMESPACE); //PI43036
            jspRootElement.setAttribute("version", "2.0");
            Iterator itr = tagPrefixes.keySet().iterator();
            while (itr.hasNext()) {
                String prefix = (String) itr.next();
                String uri = tagPrefixes.get(prefix);
                try {
                    jspRootElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + prefix, uri); //PI43036
                }
                catch (DOMException e) {
                    throw new JspCoreException("jsp.error.unable.to.create.xml.attr", new Object[] { "xmlns:" + prefix, uri }, e);
                }
            }

            if (jspConfiguration.getCodaList().size() > 0) {
                insertImplictIncludes(jspConfiguration.getCodaList());
            }

            if (elements.size() > 1) {
                Element unmatchedElement = (Element) elements.peek();
                throw new JspCoreException("jsp.error.unmatched.tag", new Object[] { unmatchedElement.getNodeName()});
            }
        }
        catch (IOException e) {
            throw new JspCoreException(e);
        }
        return (jspDocument);
    }

    protected void processJspSyntax() throws IOException, JspCoreException {
        char c = (char) readNextCharacter();

        if (c == '!')
        {
            readCharacter();
            int[] chars = readAhead(2);

            if (chars[0] == '-' && chars[1] == '-') {
                processHtmlCommment();
                textCurrLineNum = currentLineNum;
                textCurrColNum = currentColumnNum + 1;
            }
            // Begin: PK03308: The processCDATA() method is no longer used.
            //                 Instead we pass this section unchanged
            /*
            else if (chars[0] == '[' && chars[1] == 'C' && chars[2] == 'D' && chars[3] == 'A' && chars[4] == 'T' && chars[5] == 'A' && chars[6] == '[') {
                skip(7);
                processCDATA();
            }
            */
            // End: PK03308
            else {
                templateText.append("<!");
            }
        }
        else if (c == '%') {
            readCharacter();
            processElement();
            textCurrLineNum = currentLineNum;
            textCurrColNum = currentColumnNum + 1;
        }
        else {
            processAction();
        }
    }

    protected void processTemplateText() {
        if (templateText.length() > 0) {
            int lineCount = 1;
            for (int i = 0; i < templateText.length(); i++) {
                if (templateText.charAt(i) == '\n')
                    lineCount++;
            }
            //if(text.trim().length() > 0)  //PK26679   //PK26679-rework
            //{  //PK26679-rework
            //CDATASection cdata = jspDocument.createCDATASection(templateText.toString().trim());   //PK26679-rework
            CDATASection cdata = jspDocument.createCDATASection(templateText.toString());   //383041 - rework of PK26679-AGAIN
        	// PK30879
            if (!elements.empty()) {
	            Element parentElement = (Element) elements.peek();
	            parentElement.appendChild(cdata);
            }
            if (templateText.toString().trim().length() > 0) {
            	String jspId = encodedRelativeURL + "[" + textCurrLineNum + "," + textCurrColNum + "," + lineCount + "]";
                cdataJspIdMap.put(new Integer(cdata.hashCode()), jspId);
            }  //PK26679
            templateText.delete(0, templateText.length());
        }
    }

    protected void processElement() throws IOException, JspCoreException {
        processTemplateText();

        int character = readNextCharacter();

        switch (character) {
            case '@' :
                {
                    readCharacter();
                    processDirective();
                    break;
                }

            case '-' :
                {
                    readCharacter();
                    processJspComment();
                    break;
                }

            case '=' :
                {
                    readCharacter();
                    processScriptingElement(EXPRESSION_ELEMENT);
                    break;
                }

            case '!' :
                {
                    readCharacter();
                    processScriptingElement(DECLARATION_ELEMENT);
                    break;
                }

            default :
                {
                    processScriptingElement(SCRIPTLET_ELEMENT);
                    break;
                }
        }
    }

    protected void processAction() throws IOException, JspCoreException {
        boolean endFound = false;

        StringBuffer actionText = new StringBuffer();
        actionText.append('<');

        StringBuffer actionPrefixBuffer = new StringBuffer();
        StringBuffer actionNameBuffer = new StringBuffer();

        boolean inName = false;
        int character = 0;
        actionLineCount = 1;
        boolean endOfFile=false;  // PK16574

        while (endFound == false) {
            // PK07606
            // begin 223492: jsp parser failed to parse javascript for loops containing scriptlets.
            int nextChar = readNextCharacter();
            if (nextChar == '<') {
                endFound = true;
                break;
            }
            // end 223492: jsp parser failed to parse javascript for loops containing scriptlets.
            character = readCharacter();
            // PK16574, comment-out:   actionText.append((char) character);

            switch (character) {
                case -1 :
                    {
                        // PK16574; don't throw exception yet....
                        endOfFile=true;
                        break;
                    }

                case ':' :
                    {
                        inName = true;

                        break;
                    }

                case '>' :
                    {
                        endFound = true;
                        break;
                    }

                case ' ' :
                case '\t' :
                case '\r' : //PK73789
                    {
                    	StringBuffer sb = new StringBuffer();  //PK63250
                    	sb.append((char)character); //PK63250
                    	if (readNextNonWhite(sb) != '>') { //PK43599  and PK63250
                        	if (inName) {
                        		endFound = true;
                        	}
                        	else if (actionPrefixBuffer.toString().trim().length() > 0) {
                        		endFound = true;
                        	}
                         //PK43599
                    	}
                        //PK63250 start
                        if (sb.length() > 0) {
                        	character = sb.charAt(sb.length()-1);//going to append this char later
                        	actionText.append(sb.deleteCharAt(sb.length()-1));//append all white characters
                        }
                        //PK63250 end
                        break;
                    }

                case '\n' :
                    {
                    	StringBuffer sb = new StringBuffer();  //PK63250
                    	sb.append((char)character);  //PK63250
                    	if (readNextNonWhite(sb) != '>') { //PK43599 and PK63250
                        	if (inName) {
                        	    endFound = true;
                        	}
                        	else if (actionPrefixBuffer.toString().trim().length() > 0) {
                        		endFound = true;                        	  
                        	}
                    	} //PK43599
                        //PK63250 start	
                    	if (sb.length() > 0) {
                        	character = sb.charAt(sb.length()-1);//going to append this char later
                            actionText.append(sb.deleteCharAt(sb.length()-1));//append all white characters
                        }
                    	//PK63250 end
                        
                        actionLineCount++;
                        break;
                    }

                default :
                    {
                        if (inName)
                            actionNameBuffer.append((char) character);
                        else
                            actionPrefixBuffer.append((char) character);
                        break;
                    }
            }
            
            // PK16574; get out now if EOF was reached
            if (endOfFile)
                break;

            // PK16574; add character to actionText
            actionText.append((char) character);
            
        }

        boolean isEndTag = false;
        boolean hasChildren = false;
        
        boolean doIncrement=false; // PK30879

        String actionPrefix = actionPrefixBuffer.toString().trim();
        String actionName = actionNameBuffer.toString().trim();

        if (actionPrefix.startsWith("/")) {
            actionPrefix = actionPrefix.substring(1);
            isEndTag = true;
        }

        if (actionName.endsWith("/")) {
            actionName = actionName.substring(0, actionName.length() - 1);
        }
        else {
            hasChildren = true;
        }

        if (inName && (actionPrefix.equals("jsp") || tagPrefixes.containsKey(actionPrefix))) {
            // PK16574; throw exception now, since we were definitely parsing a action:name construct.
            if (endOfFile){
                throw new JspCoreException(
                        "jsp.error.end.of.file.reached",
                        new Object[] {
                            "<" + actionPrefixBuffer.toString().trim() + " " + actionNameBuffer.toString().trim(),
                            "Line " + jspSyntaxLineNum + " Col " + jspSyntaxColNum });

            }
            else {
                processTemplateText();
                if (isEndTag) {
                    Element element = (Element) elements.pop();
                    String jspId = element.getAttributeNS(Constants.JSP_NAMESPACE, "id");
                    jspId = jspId + "[" + jspSyntaxLineNum + "," + jspSyntaxColNum + "]";
                    if (element.getPrefix().equals(actionPrefix) && element.getLocalName().equals(actionName)) {
                        element.setAttributeNS(Constants.JSP_NAMESPACE, "jsp:id", jspId);
                    }
                    else {
    					//PK30879 begin
                    	if (jspOptions.isAllowUnmatchedEndTag()) {
    		                //System.out.println("JspPageParser.processAction  not throwing exception, appending templateText: ["+actionText.toString()+"]");
                			templateText.append(actionText.toString());
    		                //System.out.println("JspPageParser.processAction  pushing element back on! : ["+element+"]");
                            elements.push(element);
                			doIncrement=true;
            			} else {
            				throw new JspCoreException(
    								"jsp.error.unmatched.end.tag",
    								new Object[] { element.getNodeName(), actionPrefix + ":" + actionName, "[" + currentLineNum + "," + currentColumnNum + "]" });                				
            			}
    					//PK30879 end
                    }
                }
                else {
                    String namespaceURI = Constants.JSP_NAMESPACE;
                    if (actionPrefix.equals("jsp") == false) {
                        namespaceURI = tagPrefixes.get(actionPrefix);
                        if (namespaceURI.startsWith("urn:jsptld:")) {
                            namespaceURI = namespaceURI.substring(namespaceURI.indexOf("urn:jsptld:") + 11);
                        }
                        else if (namespaceURI.startsWith("urn:jsptagdir:")) {
                            namespaceURI = namespaceURI.substring(namespaceURI.indexOf("urn:jsptagdir:") + 14);
                        }
                    }
                    Element actionElement = null;
                    try {
                        actionElement = jspDocument.createElementNS(namespaceURI, actionPrefix + ":" + actionName.trim());
                    }
                    catch (DOMException e) {
                        throw new JspCoreException("jsp.error.unable.to.create.xml.element", new Object[] { namespaceURI, actionPrefix + ":" + actionName.trim()}, e);
                    }
                    // PK30879
                    if (!elements.empty()) {
	                    Element parentElement = (Element) elements.peek();
	                    if (actionName.equals(Constants.JSP_INCLUDE_DIRECTIVE_TYPE) == false) {
	                        parentElement.appendChild(actionElement);
	                    }
                    }
                    if (character != '>') {
                        hasChildren = processActionAttributes(actionElement);
                    }

                    if (actionName.equals(Constants.JSP_INCLUDE_DIRECTIVE_TYPE)) {
                        if (actionElement.hasAttribute("file")) {
                            insertInclude(actionElement.getAttribute("file"));
                        }
                        else {
                            throw new JspCoreException("jsp.error.include.directive.attribute.invalid");
                        }
                    }
                    else {
                        if (hasChildren) {
                            elements.push(actionElement);
                        }
                    }
                    String jspId = actionElement.getAttributeNS(Constants.JSP_NAMESPACE, "id");
                    jspId = jspId + encodedRelativeURL + "[" + jspSyntaxLineNum + "," + jspSyntaxColNum + "," + actionLineCount + "]";
                    actionElement.setAttributeNS(Constants.JSP_NAMESPACE, "jsp:id", jspId);
                }
            	if (!doIncrement) {// PK30879
	                textCurrLineNum = currentLineNum;
	                textCurrColNum = currentColumnNum + 1;
	            	doIncrement=false;
            	}
            }
        }
        else {
            // begin defect 396002
        	if (inName && !tagPrefixes.containsKey(actionPrefix)) {
        		String msg=buildLineNumberMessage(" ");
            	nonCustomTagPrefixMap.put(actionPrefix,msg);
        	}
            // end defect 396002
            templateText.append(actionText.toString());
        }
    }

    protected boolean processActionAttributes(Element actionElement) throws IOException, JspCoreException {
        boolean hasChildren = true;

        StringBuffer attributeName = new StringBuffer();
        StringBuffer attributeValue = new StringBuffer();
        boolean inValue = false;
        boolean endFound = false;
        boolean usingSingleQuotes = false;
        boolean inEscape = false;
        boolean evalExpressionFollowingTwoBackslashes = WCCustomProperties.EVAL_EXPRESSION_FOLLOWING_TWO_BACKSLASHES; //PM81674

        StringBuffer attrNames = new StringBuffer();

        int prev = 0;
        int character = 0;
        while (endFound == false) {
            prev = character;
            character = readCharacter();
            switch (character) {
                case -1 :
                    {
                        throw new JspCoreException(
                            "jsp.error.end.of.file.reached",
                            new Object[] { actionElement.getNodeName(), "Line: " + jspSyntaxLineNum + " Col: " + jspSyntaxColNum });
                    }
                case '>' :
                    {
                        if (inValue == false) {
                            if (prev == '/')
                                hasChildren = false;
                            endFound = true;
                        }
                        else {
                            if (prev != '%')
                                attributeValue.append("&gt;");
                        }
                        break;
                    }

                case '\"' :
                    {
                        if (inValue == true) {
                            //PI43036 start
                            String namespaceUri = null;
                            if (attributeName.toString().indexOf(':') != -1) {
                                String prefix = attributeName.toString().substring(0, attributeName.toString().indexOf(':'));
                                namespaceUri = (String) tagPrefixes.get(prefix);
                            }
                            //PI43036 end
                            if (usingSingleQuotes == false && inEscape == false) { //PQ98664
                                try {
                                    //PI30519 start
                                    if (jspOptions.isAllowMultipleAttributeValues()) {
                                        //PI43036 start
                                        if (namespaceUri == null)
                                            actionElement.setAttribute(attributeName.toString(), attributeValue.toString());
                                        else
                                            actionElement.setAttributeNS(namespaceUri, attributeName.toString(), attributeValue.toString());
                                        //PI43036 end
                                        List valueList = (ArrayList) actionElement.getUserData(attributeName.toString());
                                        if (valueList == null) {
                                                valueList = new ArrayList();
                                        }
                                        valueList.add(attributeValue.toString());
                                        actionElement.setUserData(attributeName.toString(), valueList, null);
                                        valueList = null;
                                    } else {
                                        //PI43036 start
                                        if (namespaceUri == null)
                                            actionElement.setAttribute(attributeName.toString(), attributeValue.toString());
                                        else
                                            actionElement.setAttributeNS(namespaceUri, attributeName.toString(), attributeValue.toString());
                                        //PI43036 end
                                    }
                                    //PI30519 end
                                    attrNames.append(attributeName.toString()+"~");
                                }
                                catch (DOMException e) {
                                    throw new JspCoreException("jsp.error.unable.to.create.xml.attr", new Object[] { attributeName.toString(), attributeValue.toString()}, e);
                                }
                                attributeName.delete(0, attributeName.length());
                                attributeValue.delete(0, attributeValue.length());
                                inValue = false;
                            }
                            else {
                                attributeValue.append('\"');
                            }
                            inEscape = false; //PQ98664
                        }
                        else {
                            inValue = true;
                            usingSingleQuotes = false;
                        }
                        break;
                    }

                case '\'' :
                    {
                        if (inValue == true) {
                            //PI43036 start
                            String namespaceUri = null;
                            if (attributeName.toString().indexOf(':') != -1) {
                                String prefix = attributeName.toString().substring(0, attributeName.toString().indexOf(':'));
                                namespaceUri = (String) tagPrefixes.get(prefix);
                            }
                            //PI43036 end
                            if (usingSingleQuotes && inEscape == false) { //PQ98664
                                try {
                                    //PI30519 start
                                    if (jspOptions.isAllowMultipleAttributeValues()) {
                                        //PI43036 start
                                        if (namespaceUri == null)
                                            actionElement.setAttribute(attributeName.toString(), attributeValue.toString());
                                        else
                                            actionElement.setAttributeNS(namespaceUri, attributeName.toString(), attributeValue.toString());
                                        //PI43036 end
                                        List valueList = (ArrayList) actionElement.getUserData(attributeName.toString());
                                        if (valueList == null) {
                                                valueList = new ArrayList();
                                        }
                                        valueList.add(attributeValue.toString());
                                        actionElement.setUserData(attributeName.toString(), valueList, null);
                                        valueList = null;
                                    } else {
                                        //PI43036 start
                                        if (namespaceUri == null)
                                            actionElement.setAttribute(attributeName.toString(), attributeValue.toString());
                                        else
                                            actionElement.setAttributeNS(namespaceUri, attributeName.toString(), attributeValue.toString());
                                        //PI43036 end
                                    }
                                    //PI30519 end
                                    attrNames.append(attributeName.toString()+"~");
                                }
                                catch (DOMException e) {
                                    throw new JspCoreException("jsp.error.unable.to.create.xml.attr", new Object[] { attributeName.toString(), attributeValue.toString()}, e);
                                }
                                attributeName.delete(0, attributeName.length());
                                attributeValue.delete(0, attributeValue.length());
                                inValue = false;
                            }
                            else {
                                attributeValue.append('\'');
                            }
                            inEscape = false; //PQ98664
                        }
                        else {
                            inValue = true;
                            usingSingleQuotes = true;
                        }
                        break;
                    }

                case '\\' :
                    {
                        if (inValue == true) {
                            //PQ98664 Start
                            if (inEscape) {
                                inEscape = false;
                                attributeValue.append((char) character);
                            }
                            else {
                                int nextChar = readNextCharacter();
                                if (nextChar == '\'' || nextChar == '\"' || nextChar == '\\') {
                                    inEscape = true;
                                    //PM81674 start
                                    if(evalExpressionFollowingTwoBackslashes){
                                        int chars[] = readAhead(2);
                                        if (chars[1] != '\'' && chars[1] != '\"') {
                                            //This means that we have an expression like "\\${10+10}".  The backslash should be appended as the EL expression will be evaluated.
                                            attributeValue.append((char) character);
                                        }
                                    }
                                    //PM81674 end
                                }
                                else {
                                    attributeValue.append((char) character);
                                }
                            }
                            //PQ98664 End
                        }
                        else {
                            attributeName.append((char) character);
                        }
                        break;
                    }

                case '&' :
                    {
                        if (inValue == true) {
                            attributeValue.append("&amp;");
                        }
                        break;
                    }

                case '<' :
                    {
                        if (inValue == true) {
                            int[] nextChars = readAhead(2);
                            if (nextChars[0] != '%' && nextChars[1] != '=') {
                                attributeValue.append("&lt;");
                            }
                        }
                        else {
                            throw new JspCoreException("jsp.error.invalid.jsp.syntax", new Object[] { attributeName.toString()});
                        }
                        break;
                    }
                    // begin 246266    JSP error that has page directive on multiple lines    WAS.jsp
                case '\n' :
                {
                    actionLineCount++;
                }
                case '\r' :
                    // end 246266    JSP error that has page directive on multiple lines    WAS.jsp
                case ' ' :
                case '\t' :
                    {
                        if (inValue == true)
                            attributeValue.append((char) character);
                        break;
                    }

                case '=' :
                    {
                        if (inValue == true)
                            attributeValue.append((char) character);
                        break;
                    }
                    // begin 246266    JSP error that has page directive on multiple lines    WAS.jsp
                /*
                case '\n' :
                {
                    actionLineCount++;
                    break;
                }

                case '\r' :
                {
                    break;
                }
                */
                    // end 246266    JSP error that has page directive on multiple lines    WAS.jsp
                default :
                    {
                        if (inValue == true)
                            attributeValue.append((char) character);
                        else
                            attributeName.append((char) character);
                        break;
                    }
            }
        }

        if (actionElement.getNamespaceURI().equals(Constants.JSP_NAMESPACE) == false && attrNames.length() > 0) {
            actionElement.setAttributeNS(Constants.JSP_NAMESPACE, "jsp:id", "{"+attrNames.toString()+"}");
        }

        return hasChildren;
    }

    protected void processHtmlCommment() throws IOException, JspCoreException {
        Element element = null;
        processTemplateText();

        boolean endFound = false;
        StringBuffer htmlCommentText = new StringBuffer();
        htmlCommentText.append("<!");

        int lineCount = 1;
        int character = 0;

        while (endFound == false) {
            character = readCharacter();

            switch (character) {
                case -1 :
                    {
                        throw new JspCoreException(
                            "jsp.error.end.of.file.reached",
                            new Object[] { "html comment " + htmlCommentText.toString(), "Line: " + jspSyntaxLineNum + " Col: " + jspSyntaxColNum });
                    }

                case '>' :
                    {
                        if (htmlCommentText.charAt(htmlCommentText.length() - 1) == '-' && htmlCommentText.charAt(htmlCommentText.length() - 2) == '-')
                            endFound = true;
                        htmlCommentText.append((char) character);
                        break;
                    }

                case '<' :
                    {
                        endFound = true;
                        break;
                    }

                case '\n' :
                    {
                        lineCount++;
                        htmlCommentText.append((char) character);
                        break;
                    }

                default :
                    {
                        htmlCommentText.append((char) character);
                        break;
                    }
            }
        }

        CDATASection cdata = jspDocument.createCDATASection(htmlCommentText.toString());
        if (htmlCommentText.toString().trim().length() > 0) {
            String jspId = encodedRelativeURL + "[" + jspSyntaxLineNum + "," + jspSyntaxColNum + "," + lineCount + "]";
            cdataJspIdMap.put(new Integer(cdata.hashCode()), jspId);
        }
        Element parentElement = (Element) elements.peek();
        parentElement.appendChild(cdata);
        if ((char) character == '<') {
            processJspSyntax();
        }
    }

    protected void processDirective() throws IOException, JspCoreException {
        boolean endFound = false;

        Element element = null;
        int lineCount = 1;

        StringBuffer directive = new StringBuffer();
        StringBuffer name = new StringBuffer();
        StringBuffer value = new StringBuffer();
        String prefix = "";
        String uri = "";
        String tagdir = "";

        boolean inValue = false;
        boolean inDirective = true;
        boolean inSingleQuotes = false; //PK86095
        
        int prev = 0;
        int character = 0;

        while (endFound == false) {
            prev = character;
            character = readCharacter();

            switch (character) {
                case -1 :
                    {
                        throw new JspCoreException(
                            "jsp.error.end.of.file.reached",
                            new Object[] { "directive " + directive.toString(), "Line: " + jspSyntaxLineNum + " Col: " + jspSyntaxColNum });
                    }

                case '<' :
                    {
                        if (inValue)
                            value.append("&lt;");
                        else if (inDirective)
                            throw new JspCoreException("jsp.error.invalid.jsp.syntax", new Object[] { directive.toString()});
                        else
                            throw new JspCoreException("jsp.error.invalid.jsp.syntax", new Object[] { name.toString()});
                        break;
                    }

                case '>' :
                    {
                        if (prev == '%')
                            endFound = true;
                        else {
                            if (inValue)
                                value.append("&gt;");
                            else if (!inDirective)
                                name.append("&gt;");
                        }
                        break;
                    }
                    // begin 246266    JSP error that has page directive on multiple lines    WAS.jsp
                /*case '\n' :
                    {
                        lineCount++;
                        break;
                    }
                    */
                    // end 246266    JSP error that has page directive on multiple lines    WAS.jsp
                case '\r' :
                    {
                        break;
                    }

                case '=' :
                    {
                        if (inValue) {
                            value.append((char) character);
                        }
                        break;
                    }
                    // begin 246266    JSP error that has page directive on multiple lines    WAS.jsp
                case '\n' :
                {
                    lineCount++;
                }
                // end 246266    JSP error that has page directive on multiple lines    WAS.jsp
                case ' ' :
                case '\t' :
                    {
                        if (inDirective && directive.toString().trim().length() > 0) {
                            if (directive.toString().equals("taglib") == false && directive.toString().equals("include") == false) {

                                try {
                                    element = jspDocument.createElementNS(Constants.JSP_NAMESPACE, "jsp:directive." + directive.toString());
                                }
                                catch (DOMException e) {
                                    throw new JspCoreException(
                                        "jsp.error.unable.to.create.xml.element",
                                        new Object[] { Constants.JSP_NAMESPACE, "jsp:directive." + directive.toString()},
                                        e);
                                }
                                setElementAttribute(
                                    element,
                                    Constants.JSP_NAMESPACE,
                                    "jsp:id",
                                encodedRelativeURL + "[" + jspSyntaxLineNum + "," + jspSyntaxColNum + "," + lineCount + "]");
                            }
                            inDirective = false;
                        }
                        else if (inValue) {
                            value.append((char) character);
                        }
                        break;
                    }

                case '\'' :
                case '\"' :
                    {
                    	if (inValue) {
                    		if((inSingleQuotes && character == '\'')  ||	
                    				(!inSingleQuotes && character == '\"')){ //PK86095 - this is not a single quote inside of a double quote or vice-versa so continue as normal
                    			if (directive.toString().equals("taglib")) {
                    				if (name.toString().equals("prefix")) {
                    					prefix = value.toString();
                    				}
                    				else if (name.toString().equals("uri")) {
                    					if (uri != null && uri.trim().equals("") == false) {
                    						if (uri.startsWith("urn:jsptagdir:")) {
                    							throw new JspTranslationException("jsp.error.invalid.taglib.directive.tagdir.uri", new Object[] { value.toString(), uri });
                    						}
                    						else {
                    							throw new JspTranslationException("jsp.error.invalid.taglib.directive.duplicate.uri", new Object[] { value.toString(), uri });
                    						}
                    					}
                    					uri = value.toString();
                    				}
                    				else if (name.toString().equals("tagdir")) {
                    					if (uri != null && uri.trim().equals("") == false) {
                    						if (uri.startsWith("urn:jsptagdir:")) {
                    							throw new JspTranslationException("jsp.error.invalid.taglib.directive.duplicate.tagdir", new Object[] { value.toString(), uri });
                    						}
                    						else {
                    							throw new JspTranslationException("jsp.error.invalid.taglib.directive.tagdir.uri", new Object[] { uri, value.toString()});
                    						}
                    					}

                    					if (value.toString()!=null){
                    						if(!value.toString().startsWith(WEB_INF_TAGS)) {
                    							throw new JspTranslationException("jsp.error.invalid.tagdir", new Object[] {tagdir});
                    						}
                    						else {
                    							tagdir= value.toString();                                    		
                    						}
                    					}                                    
                    					uri = "urn:jsptagdir:" + value.toString();
                    				}else{
                    					throw new JspTranslationException("jsp.error.invalid.taglib.directive.attribute", new Object[] {name.toString(), value.toString()});
                    				}

                    			}
                    			else if (directive.toString().equals("page")) {
                    				if (name.toString().equals("import")) {
                    					if (element.getAttributeNode("import") != null) {
                    						String currentImport = element.getAttribute("import");
                    						setElementAttribute(element, null, "import", currentImport + "," + value.toString());
                    					}
                    					else {
                    						setElementAttribute(element, null, "import", value.toString());
                    					}
                    				}
                    				else {	//516829 - duplicate attrs are ok as long as they have the same value.  added that check.
                    					if (element.getAttributeNode(name.toString()) != null && !element.getAttributeNode(name.toString()).getValue().equals(value.toString()))
                    						throw new JspCoreException("jsp.error.page.directive.dup.value", new Object[] { name.toString()});
                    					setElementAttribute(element, null, name.toString(), value.toString());
                    				}
                    			}
                    			else if (directive.toString().equals("include")) {
                    				if (name.toString().equals("file")) {
                    					String includeURI = value.toString();
                    					if (includeURI == null || includeURI.toString().equals("")) {
                    						throw new JspCoreException("jsp.error.static.include.value.missing");
                    					}
                    					insertInclude(includeURI);
                    				}
                    				else {
                    					throw new JspCoreException("jsp.error.include.directive.attribute.invalid", new Object[] { name.toString()});
                    				}
                    			}
                    			else if (directive.toString().equals("tag")) {
                    				if (name.toString().equals("import")) {
                    					if (element.getAttributeNode("import") != null) {
                    						String currentImport = element.getAttribute("import");
                    						setElementAttribute(element, null, "import", currentImport + "," + value.toString());
                    					}
                    					else {
                    						setElementAttribute(element, null, "import", value.toString());
                    					}
                    				}
                    				else {	//516829 - duplicate attrs are ok as long as they have the same value.  added that check.
                    					if (element.getAttributeNode(name.toString()) != null && !element.getAttributeNode(name.toString()).getValue().equals(value.toString()) )
                    						throw new JspCoreException("jsp.error.page.directive.dup.value", new Object[] { name.toString()});
                    					setElementAttribute(element, null, name.toString(), value.toString());
                    				}
                    			}
                    			else if (directive.toString().equals("attribute")) {
                    				setElementAttribute(element, null, name.toString(), value.toString());
                    			}
                    			else if (directive.toString().equals("variable")) {
                    				setElementAttribute(element, null, name.toString(), value.toString());
                    			}
                    			name.delete(0, name.length());
                    			value.delete(0, value.length());
                    			inValue = false;
                    			inSingleQuotes = false;
                    		}else{
                    			//PK86095 - this is a single quotes inside of a double quote or vice-versa, just append it
                    			value.append((char) character);
                    		}
                    	}
                    	else {
                    		inValue = true;
                    		if(character == '\''){	//PK86095
                            	inSingleQuotes = true;
                            }
                    	}
                    	break;
                    }

                default :
                    {
                        if (inValue)
                            value.append((char) character);
                        else if (inDirective)
                            directive.append((char) character);
                        else
                            name.append((char) character);
                        break;
                    }
            }
        }

        if (prefix.equals("") == false && uri.equals("") == false) {
        	String tmpUri=tagPrefixes.get(prefix);
        	// defect 396002 begin
        	if (!jspOptions.isAllowTaglibPrefixRedefinition()) {
	        	if (tmpUri !=null && !tmpUri.equals(uri)) {
	        	    //need additional check for startsWith "urn:jsptagdir:"
	        	    //uri is not NULL because of above check
	        	    boolean tagDir=uri.startsWith("urn:jsptagdir:");
	        	    if (!tagDir || (tagDir &&!tmpUri.equals(uri.substring(14)))) {
	        	        throw new JspCoreException("jsp.error.prefix.redefined",new Object[] { prefix, uri, tmpUri});
	        	    }
	        	}
        	}
        	if (!jspOptions.isAllowTaglibPrefixUseBeforeDefinition()) {
	        	String msg=nonCustomTagPrefixMap.get(prefix);
	        	if (msg!=null) {
	                throw new JspCoreException("jsp.error.prefix.use_before_dcl",new Object[] { prefix, msg});      		
	        	}
        	}
        	// defect 396002 end
            tagPrefixes.put(prefix, uri);
        } else if (directive.toString().equals("taglib") && prefix.equals("")){
            throw new JspTranslationException ("jsp.error.invalid.taglib.directive.missing.required.prefix");
        }

        if (element != null) {
            if (element.getLocalName().equals(Constants.JSP_PAGE_DIRECTIVE_TYPE)) {
                if (element.hasAttribute("pageEncoding")) {
                    if (pageEncodingSpecified == false) {
                        pageEncodingSpecified = true;
                        if (jspConfiguration.getPageEncoding() != null) {
                            if (jspConfiguration.getPageEncoding().equalsIgnoreCase(element.getAttribute("pageEncoding")) == false) { //PI37485
                                throw new JspCoreException(
                                    "jsp.error.encoding.mismatch.config.pageencoding",
                                    new Object[] { jspConfiguration.getPageEncoding(), element.getAttribute("pageEncoding")});
                            }
                        }
                        else {
                            jspConfiguration.setPageEncoding(element.getAttribute("pageEncoding"));
                        }
                    }
                    else {
                        throw new JspCoreException(JspCoreException.getMsg("jsp.error.page.pageencoding.dup", new Object[] { resolvedRelativeURL}));
                    }
                }
            }

            Element parentElement = (Element) elements.peek();
            parentElement.appendChild(element);
        }
    }

    protected void processJspComment() throws IOException, JspCoreException {
        boolean endFound = false;
        StringBuffer jspCommentText = new StringBuffer();

        while (endFound == false) {
            int character = readCharacter();

            switch (character) {
                case -1 :
                    {
                        throw new JspCoreException(
                            "jsp.error.end.of.file.reached",
                            new Object[] { "jspComment " + jspCommentText.toString(), "Line: " + jspSyntaxLineNum + " Col: " + jspSyntaxColNum });
                    }

                case '>' :
                    {
                        if (jspCommentText.length() >= 3 //PI73022
                            && jspCommentText.charAt(jspCommentText.length() - 1) == '%'
                            && jspCommentText.charAt(jspCommentText.length() - 2) == '-'
                            && jspCommentText.charAt(jspCommentText.length() - 3) == '-') {
                            endFound = true;
                        }
                        else {
                            jspCommentText.append((char) character);
                        }
                        break;
                    }

                default :
                    {
                        jspCommentText.append((char) character);
                        break;
                    }
            }
        }
    }

    protected void processScriptingElement(int type) throws IOException, JspCoreException {
        Element element = null;
        int lineCount = 1;

        StringBuffer value = new StringBuffer();
        boolean endFound = false;
        boolean inComment = false;   //PK18890
        boolean inSingleQuoteValue = false;
        boolean inDoubleQuoteValue = false;
        int character = 0;
        int previousChar = 0;

        while (endFound == false) {
            previousChar = character;
            character = readCharacter();

            switch (character) {
                case -1 :
                    {
                        throw new JspCoreException(
                            "jsp.error.end.of.file.reached",
                            new Object[] { "scripting element " + value.toString(), "Line: " + jspSyntaxLineNum + " Col: " + jspSyntaxColNum });
                    }

                case '%' :
                    {
                        int nextChar = readNextCharacter();
                        if (nextChar != '>') {
                            value.append((char) character);
                        }
                        break;
                    }

                case '<' :
                    {
					 // Begin :PK11883
                     // int[] chars = readAhead(8);
                       int[] chars = readAhead(2);
                       /* if (chars[0] == '!' && chars[1] == '[' && chars[2] == 'C' && chars[3] == 'D' && chars[4] == 'A' && chars[5] == 'T' && chars[6] == 'A' && chars[7] == '[') {
                            skip(8);
                        }
                        else {*/
                            if (inSingleQuoteValue == false && inDoubleQuoteValue == false && inComment == false) {  //PK18890
                                if (chars[0] == '%') {
                                    throw new JspCoreException("jsp.error.invalid.jsp.syntax", new Object[] { value.toString()});
                                }
                            }
                            value.append("&lt;");
                        //}  End :PK11883
                        break;
                    }
				// Begin :PK11883
               /* case ']' :
                    {
                        int chars[] = readAhead(2);
                        if (chars[0] == ']' && chars[1] == '>') {
                            skip(2);
                        }
                        else {
                            value.append((char) character);
                        }
                        break;
                    }*/
				// End :PK11883
                case '&' :
                    {
                        value.append("&amp;");
                        break;
                    }

                case '>' :
                    {
                        if (previousChar == '%') {
                            endFound = true;
                        }
                        else {
                            value.append("&gt;");
                        }
                        break;
                    }

                case '\'' :
                    {
                        if (inSingleQuoteValue) {
                            inSingleQuoteValue = false;
                        }
                        else {
                            inSingleQuoteValue = true;
                        }
                        value.append((char) character);
                        break;
                    }

                case '\"' :
                    {
                        if (inDoubleQuoteValue) {
                            inDoubleQuoteValue = false;
                        }
                        else {
                            inDoubleQuoteValue = true;
                        }
                        value.append((char) character);
                        break;
                    }

                case '\n' :
                    {
                        lineCount++;
                        value.append((char) character);
                        break;
                    }

                //PK18890 begin
				case '/' :
				    {
						int nextChar = readNextCharacter();
						if(inComment && (previousChar == '*') && (nextChar == -1))
						{
							inComment = false;
						}
						if(nextChar == '*' || nextChar == '/')
						{
							inComment = true;
						}

						value.append((char) character);
						break;
					}
				//PK18890 end

                default :
                    {
                        value.append((char) character);
                        break;
                    }
            }
        }

        switch (type) {
            case SCRIPTLET_ELEMENT :
                {
                    element = jspDocument.createElementNS(Constants.JSP_NAMESPACE, "jsp:scriptlet");
                    break;
                }

            case EXPRESSION_ELEMENT :
                {
                    element = jspDocument.createElementNS(Constants.JSP_NAMESPACE, "jsp:expression");
                    break;
                }

            case DECLARATION_ELEMENT :
                {
                    element = jspDocument.createElementNS(Constants.JSP_NAMESPACE, "jsp:declaration");
                    break;
                }
            default:
                break;
        }

        CDATASection cdata = jspDocument.createCDATASection(value.toString());
        element.appendChild(cdata);

        Element parentElement = (Element) elements.peek();
        parentElement.appendChild(element);
        element.setAttributeNS(Constants.JSP_NAMESPACE, "jsp:id", encodedRelativeURL + "[" + jspSyntaxLineNum + "," + jspSyntaxColNum + "," + lineCount + "]");
    }

    private void processCDATA() throws IOException, JspCoreException {
        boolean endFound = false;

        StringBuffer cdataText = new StringBuffer();
        int character = 0;

        while (endFound == false) {
            character = readCharacter();
            switch (character) {
                case -1 :
                    {
                        throw new JspCoreException("jsp.error.end.of.file.reached", new Object[] { "cdata", "Line: " + jspSyntaxLineNum + " Col: " + jspSyntaxColNum });
                    }

                case ']' :
                    {
                        int chars[] = readAhead(2);
                        if (chars[0] == ']' && chars[1] == '>') {
                            endFound = true;
                            skip(2);
                        }
                        else {
                            cdataText.append((char) character);
                        }
                        break;
                    }

                default :
                    {
                        cdataText.append((char) character);
                        break;
                    }
            }
        }

        templateText.append(cdataText.toString());

    }

    private void setElementAttribute(Element element, String namespace, String name, String value) throws JspCoreException {
        try {
            if (namespace != null)
                element.setAttributeNS(namespace, name, value);
            else {
                //PK16731 begin
                if(name.toString().equals("pageEncoding"))
                {
                    value = com.ibm.wsspi.webcontainer.util.EncodingUtils.getJvmConverter(value.toString());
                }
                //PK16731 end
                element.setAttribute(name, value);
            }
        }
        catch (DOMException e) {
            throw new JspCoreException("jsp.error.unable.to.create.xml.attr", new Object[] { name, value }, e);
        }
    }

    private void insertImplictIncludes(ArrayList implicitIncludeList) throws JspCoreException {
        for (Iterator itr = implicitIncludeList.iterator(); itr.hasNext();) {
            String includePath = (String) itr.next();
            insertInclude(includePath);
        }
    }

    private void insertInclude(String includePath) throws JspCoreException {

        if(includePath.startsWith("/") == false){
            int lastLocation = this.resolvedRelativeURL.lastIndexOf("/");
            if (lastLocation > 0) {
                includePath = WSUtil.resolveURI(this.resolvedRelativeURL.substring(0, lastLocation+1) + includePath);
            }else{
                includePath = WSUtil.resolveURI("/"+ includePath);
            }
        }

////
        Container container = context.getServletContext().getModuleContainer();
        String fullPath = null;
        if (container!=null) {
            Entry e = container.getEntry(includePath);
            if (e!=null) {
                fullPath = e.getPath();
                try {
                    Container convertedContainer = e.adapt(Container.class); 
                    //PM22082
                    if (convertedContainer!=null && e.getSize()==0 && !WCCustomProperties.ALLOW_DIRECTORY_INCLUDE)
                        return;
                    //PM22082
                } catch (UnableToAdaptException ex) {
                    throw new IllegalStateException(ex);
                }
            } else {
                //included jsp didn't exist ... need to still add it to the dependency list so that an exception is thrown
                fullPath = includePath;
            }
        } else {
            fullPath = context.getRealPath(includePath);
            //PM22082
            if (new File(fullPath).isDirectory() && !WCCustomProperties.ALLOW_DIRECTORY_INCLUDE)
                return;
            //PM22082
        }
        
        



        if (dependencyStack.contains(fullPath))
            throw new JspCoreException("jsp.error.static.include.circular.dependency", new Object[] { fullPath });
        dependencyStack.push(fullPath);
        if (container!=null || inputSource.getAbsoluteURL().getProtocol().equals("file")) {
            /* Only add static include dependencies if they are not in a jar */
            dependencyList.add(includePath);
        }

        Map mergedTagLibMap = new HashMap(implicitTagLibMap);
        mergedTagLibMap.putAll(tagPrefixes);
        JspConfiguration includeConfiguration = jspConfiguration.getConfigManager().getConfigurationForStaticInclude(includePath, jspConfiguration);
        JspInputSource includePathInputSource = context.getJspInputSourceFactory().copyJspInputSource(inputSource, includePath);
        Jsp2Dom jsp2Dom = new Jsp2Dom(includePathInputSource,
                                      context,
                                      directoryStack,
                                      includeConfiguration,
                                      jspOptions,      //396002
                                      dependencyStack,
                                      dependencyList,
                                      cdataJspIdMap,
                                      mergedTagLibMap,
                                      false);
        Document includeDocument=null;

        includeDocument = jsp2Dom.getJspDocument();

        Node parentNode = (Node) elements.peek();
        if (includeDocument.getDocumentElement().getNamespaceURI().equals(Constants.JSP_NAMESPACE)
            && includeDocument.getDocumentElement().getLocalName().equals(Constants.JSP_ROOT_TYPE)) {
            for (int i = 0; i < includeDocument.getDocumentElement().getChildNodes().getLength(); i++) {
                Node nodeToBeCopied = includeDocument.getDocumentElement().getChildNodes().item(i);
                Node n = jspDocument.importNode(nodeToBeCopied, true);
                if (nodeToBeCopied.getNodeType() == Node.CDATA_SECTION_NODE) {
                    Integer nodeHashCode = new Integer(nodeToBeCopied.hashCode());
                    if (cdataJspIdMap.containsKey(nodeHashCode)) {
                        String jspId = (String) cdataJspIdMap.remove(nodeHashCode);
                        cdataJspIdMap.put(new Integer(n.hashCode()), jspId);
                    }
                }
                parentNode.appendChild(n);
            }
            //248567 Start
            NamedNodeMap attrs = includeDocument.getDocumentElement().getAttributes();

            for (int i = 0; i < attrs.getLength(); i++) {
                Node attr = attrs.item(i);
                if (attr.getNodeName().startsWith("xmlns:")) {
                    String prefix = attr.getNodeName();
                    prefix = prefix.substring(prefix.indexOf(":") + 1);
                    String uri = attr.getNodeValue();
                    if (uri.startsWith("urn:jsptld:")) {
                        uri = uri.substring(uri.indexOf("urn:jsptld:") + 11);
                    }
                    else if (uri.startsWith("urn:jsptagdir:")) {
                        uri = uri.substring(uri.indexOf("urn:jsptagdir:") + 14);
                    }
                    if (uri.equals(Constants.JSP_NAMESPACE) == false && uri.equals(Constants.XSI_NAMESPACE) == false) {
                        tagPrefixes.put(prefix, uri);
                    }
                }
            }
            //248567 End
        }
        else {
            for (int i = 0; i < includeDocument.getChildNodes().getLength(); i++) {
                Node n = jspDocument.importNode(includeDocument.getChildNodes().item(i), true);
                parentNode.appendChild(n);
            }
        }

        dependencyStack.pop();
    }

    private int readCharacter() throws IOException {
        int character = jspReader.read();

        if (character == '\n') {
            currentLineNum++;
            currentColumnNum = 0;
        }
        else {
            currentColumnNum++;
        }

        return character;
    }

    private int readNextCharacter() throws IOException {
        jspReader.mark(1);
        int character = jspReader.read();
        jspReader.reset();
        return character;
    }
    
    //PK43599
    //PK63250 added StringBuffer parameter
	private int readNextNonWhite(StringBuffer sb) throws IOException {
		int character = readNextCharacter();
		while (character==' ' || character=='\t' || character=='\n' || character=='\r') {
			character = readCharacter(); //moves it forward 1
			sb.append((char)character);//PK63250
            character = readNextCharacter();
        }
        return character;
    }//PK43599


    private int[] readAhead(int count) throws IOException {
        int[] chars = new int[count];
        jspReader.mark(count);
        for (int i = 0; i < count; i++)
            chars[i] = jspReader.read();
        jspReader.reset();
        return chars;
    }

    private String readAheadToString(int count) throws IOException {
        StringBuffer sb = new StringBuffer();
        jspReader.mark(count);
        for (int i = 0; i < count; i++)
            sb.append((char) jspReader.read());
        jspReader.reset();
        return sb.toString();
    }

    private void skip(int count) throws IOException {
        for (int i = 0; i < count; i++) {
            readCharacter();
        }
    }

    public String buildLineNumberMessage (String msg){
        msg = resolvedRelativeURL + "(" + currentLineNum + "," + currentColumnNum + ") " + msg;
        return msg;
    }

}


