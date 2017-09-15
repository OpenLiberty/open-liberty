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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants; //PI43036
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.translator.visitor.xml.ParserFactory;
import com.ibm.ws.util.WSUtil;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.jsp.resource.JspInputSource;

public class JspDocumentParser extends DefaultHandler implements LexicalHandler {
    private static final String LEXICAL_HANDLER_PROPERTY = "http://xml.org/sax/properties/lexical-handler";
    private static final String DTD_FOUND_MESSAGE = "dtd has been found";

	private static Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.translator.document.JspDocumentParser";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}       

    protected Locator locator;
    protected int textLineNum = 0;
    protected int textColNum = 0;
    protected int lastLineNum = 1;
    protected int lastColNum = 1;
    protected Document document = null;
    protected JspCoreContext ctxt = null;
    protected SAXParser saxParser = null;
    protected Stack elementStack = new Stack();

    protected Stack directoryStack = null;
    protected Stack dependencyStack = null;
    protected List dependencyList = null;
    protected Map cdataJspIdMap = null;
    protected Map implicitTagLibMap = null;
    protected HashMap tagPrefixes = new HashMap(); // PK24144.2

    protected JspConfiguration jspConfiguration = null;
    protected JspOptions jspOptions = null;  //396002
    protected JspInputSource inputSource = null;
    protected String resolvedRelativeURL = null;
    protected String encodedRelativeURL = null;
    protected String encoding = null;
    protected Stack charsBuffers = new Stack();
    protected boolean inDTD = false;
    protected List preRootCommentList = new ArrayList();
    protected boolean pageEncodingSpecified = false;
    protected String jspPrefix = "jsp"; //245645.1
    protected boolean isValidating = false;
    //  jsp2.1work
    protected boolean isBomPresent = false;
    protected boolean isEncodingSpecifiedInProlog = false;
    protected String sourceEnc = null;
    //  jsp2.1work

    public JspDocumentParser(JspInputSource inputSource,
        String resolvedRelativeURL,
        JspCoreContext ctxt,
        JspConfiguration jspConfiguration,
        JspOptions jspOptions,  // 396002
        Stack directoryStack,
        Stack dependencyStack,
        List dependencyList,
        Map cdataJspIdMap,
        Map implicitTagLibMap,
        boolean isBomPresent,  
        boolean isEncodingSpecifiedInProlog,
        String sourceEnc)
        
        throws JspCoreException {
        this.inputSource = inputSource;            
        this.ctxt = ctxt;
        this.directoryStack = directoryStack;
        this.dependencyStack = dependencyStack;
        this.dependencyList = dependencyList;
        this.cdataJspIdMap = cdataJspIdMap;
        this.implicitTagLibMap = implicitTagLibMap;
        this.jspConfiguration = jspConfiguration;
        this.jspOptions = jspOptions;  //396002
        this.resolvedRelativeURL = resolvedRelativeURL;
        this.isBomPresent = isBomPresent;
        this.isEncodingSpecifiedInProlog = isEncodingSpecifiedInProlog;
        this.sourceEnc = sourceEnc;
        try {
            encodedRelativeURL = URLEncoder.encode(resolvedRelativeURL, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new JspCoreException(e);
        }
/*
        Set keys=implicitTagLibMap.keySet();
        if (!keys.isEmpty()) {
            Iterator iter=keys.iterator();
            while (iter.hasNext()) {
                String prefix = (String) iter.next();
                String uri = (String) implicitTagLibMap.get(prefix);
                System.out.println("JspDocumentParser...implicitTagLibMap  prefix ["+prefix+"]  uri ["+uri+"]");
            }
        }
*/
        // PK24144.2 begin
        if (this.implicitTagLibMap != null && this.implicitTagLibMap.size() > 0) {
            tagPrefixes.putAll(implicitTagLibMap);
        }
        // PK24144.2 end
    }

    public Document parse(InputSource is) throws JspCoreException {
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "parse(InputSource is)","is =[" + is +"]");
		}

        try {
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
    			logger.logp(Level.FINER, CLASS_NAME, "parse(InputSource is)","getting new document...");
    		}
            document = ParserFactory.newDocument(false, false);
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
    			logger.logp(Level.FINER, CLASS_NAME, "parse(InputSource is)","got new document: ["+document+"]");
    		}
        }
        catch (ParserConfigurationException e) {
            throw new JspCoreException(e);
        }

        elementStack.push(document);

		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "parse(InputSource is)","about to call parse(false,is), is: ["+is+"]");
		}
        if (parse(false, is)) {
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
    			logger.logp(Level.FINER, CLASS_NAME, "parse(InputSource is)","about to call parse(true,is)), is: ["+is+"]");
    		}
    		InputStream is2 = null;
    		try {
        		is2 = getInputStream(this.inputSource);
	            InputSource inputSource = new InputSource(is2);    		
	            parse(true, inputSource);
    		} finally {
                if (is2 != null) {
                    try {
                        is2.close();
                    }
                    catch (IOException e) {}
                }    			
    		}
        }
        
        return document;
    }
    
    protected boolean parse(boolean validating, InputSource is) throws JspCoreException {
        isValidating = validating;
        boolean reparseWithValidation = false;
        
        ClassLoader oldLoader = ThreadContextHelper.getContextClassLoader();
        ThreadContextHelper.setClassLoader(JspDocumentParser.class.getClassLoader());
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "(boolean validating, InputSource is)"," validating: ["+validating+"] is: ["+is+"]");
		}
        try {
            SAXParserFactory saxFactory = SAXParserFactory.newInstance();
            saxFactory.setNamespaceAware(true);
            saxFactory.setValidating(validating);
            saxFactory.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
            saxParser = saxFactory.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setProperty(LEXICAL_HANDLER_PROPERTY, this);
            xmlReader.setErrorHandler(this);
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
    			logger.logp(Level.FINER, CLASS_NAME, "(boolean validating, InputSource is)"," about to call ParserFactory.parseDocument");
    		}
            ParserFactory.parseDocument(saxParser, is, this);
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
    			logger.logp(Level.FINER, CLASS_NAME, "(boolean validating, InputSource is)"," back from ParserFactory.parseDocument");
    		}
        }
        catch (ParserConfigurationException e) {
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
    			logger.logp(Level.FINER, CLASS_NAME, "(boolean validating, InputSource is)"," caught ParserConfigurationException e: ["+e.getMessage()+"]");
    		}
            throw new JspCoreException(e);
        }
        catch (SAXException e) {
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
    			logger.logp(Level.FINER, CLASS_NAME, "(boolean validating, InputSource is)"," caught SAXException e: ["+e.getMessage()+"]");
    		}
            if (e.getMessage().equals(DTD_FOUND_MESSAGE)) {
                reparseWithValidation = true;
            }
            else {
                if (e.getCause() != null)
                    throw new JspCoreException(buildLineNumberMessage(e.getCause().getLocalizedMessage()));
                else
                    throw new JspCoreException(buildLineNumberMessage(e.getLocalizedMessage()));
            }
        }
        catch (IOException e) {
            throw new JspCoreException(buildLineNumberMessage(e.getLocalizedMessage()));
        }
        finally {
            ThreadContextHelper.setClassLoader(oldLoader);
        }
        
        return reparseWithValidation;
    }
    
    private InputStream getInputStream(JspInputSource jspInputSource) throws JspCoreException{
        InputStream is = null;
        try {
            is = jspInputSource.getInputStream();
        }
        catch (IOException e) {
            String msg = JspCoreException.getMsg("jsp.error.failed.to.find.resource", new Object[] {jspInputSource.getRelativeURL()});
            throw new JspCoreException(msg, new FileNotFoundException (msg));
        }
        return is;
    }

    public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.logp(Level.FINER, CLASS_NAME, "startElement","uri =[" + uri +"]");
			logger.logp(Level.FINER, CLASS_NAME, "startElement","localName =[" + localName +"]");
			logger.logp(Level.FINER, CLASS_NAME, "startElement","qName =[" + qName +"]");
			logger.logp(Level.FINER, CLASS_NAME, "startElement","attrs =[" + attrs +"]");
			logger.logp(Level.FINER, CLASS_NAME, "startElement","encoding =[" + encoding +"]");
			logger.logp(Level.FINER, CLASS_NAME, "startElement","jspConfiguration.getPageEncoding() =[" + jspConfiguration.getPageEncoding() +"]");		
		}
        if (encoding == null) {
        	if (this.sourceEnc!=null) {
        		this.encoding=this.sourceEnc;
	            if (jspConfiguration.getPageEncoding() != null) {
	            	if (isEncodingSpecifiedInProlog || isBomPresent) {
		        		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
		        			logger.logp(Level.FINER, CLASS_NAME, "startElement","comparing encodings configuration: =[" + jspConfiguration.getPageEncoding() +"] returned encoding: ["+encoding+"]");
		        		}
		                if (compareEncoding(jspConfiguration.getPageEncoding(), encoding) == false) {
		                    throw new SAXException(
		                        JspCoreException.getMsg("jsp.error.encoding.mismatch.config.xml", new Object[] { jspConfiguration.getPageEncoding(), encoding }));
		                }
	            	}
	            }
        	}
        }

        if (localName.equals(Constants.JSP_INCLUDE_DIRECTIVE_TYPE) && uri.equals(Constants.JSP_NAMESPACE)) {
            try {
                String includePath = null;
                boolean seenIncludePath = false;
                // mimic JSP Page behavior and only check first attribute.
                if (attrs.getLength() > 0) {
                    String attrLocalName = attrs.getLocalName(0);
                    if (attrLocalName.equals("") == false) {
                        if (attrLocalName.equals("file") == false) {
                            throw new JspCoreException("jsp.error.include.directive.attribute.invalid", new Object[] { attrLocalName });
                        }
                        else {
                            includePath = attrs.getValue("file");
                            seenIncludePath = true;
                        }
                    }
                }
                if (seenIncludePath == false || includePath.equals("")) {
                    throw new JspCoreException("jsp.error.static.include.value.missing");
                }
                insertInclude(includePath);
            }
            catch (JspCoreException e) {
                throw new SAXException(e.getLocalizedMessage());
            }
        }
        else {
            Node parentNode = (Node) elementStack.peek();

            if (charsBuffers.size() > 0) {
                CharacterBuffer chars = (CharacterBuffer) charsBuffers.peek();
                createJspTextElement(parentNode, chars.charsBuffer);
                chars.charsBuffer.delete(0, chars.charsBuffer.length());
                chars.clearNonWhiteSpaceFound();
            }
            StringBuffer attrNames = new StringBuffer();
            Element jspElement = document.createElementNS(uri, qName);
            for (int i = 0; i < attrs.getLength(); i++) {
                if (attrs.getQName(i).startsWith("xmlns")) {
                    jspElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, attrs.getQName(i), attrs.getValue(i)); //PI43036
                    if (attrs.getValue(i).equals(Constants.JSP_NAMESPACE)) {
                        jspPrefix = attrs.getQName(i).substring(attrs.getQName(i).indexOf("xmlns:") + 6);

                    }/*else {       //PK24144-rework
						jspPrefix = attrs.getQName(i).substring(attrs.getQName(i).indexOf("xmlns:") +6);
                        if(!jspPrefix.equals(""))
                            implicitTagLibMap.put(jspPrefix,attrs.getValue(i));
	            	} //PK24144-rework
                    */
                    else {
                        // PK24144.2 begin
                        String thisPrefix=attrs.getQName(i).substring(attrs.getQName(i).indexOf("xmlns:") + 6);
                        if (!thisPrefix.equals("")) {
                            tagPrefixes.put(thisPrefix, attrs.getValue(i));
                        }
                        // PK24144.2 end
                    }
                }
                else {
                    jspElement.setAttributeNS(attrs.getURI(i), attrs.getQName(i), attrs.getValue(i));
                    if (attrs.getURI(i).equals("") == false) {
                        attrNames.append(attrs.getURI(i)+":"+attrs.getQName(i)+"~");
                    }
                    else {
                        attrNames.append(attrs.getQName(i)+"~");
                    }
                }
            }
            String jspId = null;
            if (uri.equals(Constants.JSP_NAMESPACE) == false && attrNames.length() > 0) {
                jspId = "{" + attrNames.toString() + "}" + encodedRelativeURL + "[" + lastLineNum + "," + lastColNum + "," + (locator.getLineNumber() - (lastLineNum - 1)) + "]";
            }
            else {
                jspId = encodedRelativeURL + "[" + lastLineNum + "," + lastColNum + "," + (locator.getLineNumber() - (lastLineNum - 1)) + "]";
            }
            
            jspElement.setAttributeNS(Constants.JSP_NAMESPACE, "jsp:id", jspId); //245645.1

            if (uri.equals(Constants.JSP_NAMESPACE) && localName.equals(Constants.JSP_PAGE_DIRECTIVE_TYPE)) {
                if (jspElement.hasAttribute("pageEncoding")) {
                    if (pageEncodingSpecified == false) {
                        pageEncodingSpecified = true;
                        if (jspConfiguration.getPageEncoding() != null) {
                            if (compareEncoding(jspConfiguration.getPageEncoding(), encoding) == false) {
                                throw new SAXException(
                                    JspCoreException.getMsg("jsp.error.encoding.mismatch.config.xml", new Object[] { jspConfiguration.getPageEncoding(), encoding }));
                            }
                            if (compareEncoding(jspConfiguration.getPageEncoding(), jspElement.getAttribute("pageEncoding")) == false) {
                                throw new SAXException(
                                    JspCoreException.getMsg(
                                        "jsp.error.encoding.mismatch.config.pageencoding",
                                        new Object[] { jspConfiguration.getPageEncoding(), jspElement.getAttribute("pageEncoding")}));
                            }
                        }

                        if (compareEncoding(jspElement.getAttribute("pageEncoding"), encoding) == false) {
                            throw new SAXException(
                                JspCoreException.getMsg("jsp.error.encoding.mismatch.pageencoding.xml", new Object[] { jspElement.getAttribute("pageEncoding"), encoding }));
                        }

                        if (jspConfiguration.getPageEncoding() == null) {
                            jspConfiguration.setPageEncoding(encoding);
                        }
                    }
                    else {
                        throw new SAXException(JspCoreException.getMsg("jsp.error.page.pageencoding.dup", new Object[] { resolvedRelativeURL}));
                    }
                }
            }
            parentNode.appendChild(jspElement);
            elementStack.push(jspElement);
            if (parentNode instanceof Document) {
                for (Iterator itr = preRootCommentList.iterator(); itr.hasNext();) {
                    Element commentElement = (Element) itr.next();
                    jspElement.appendChild(commentElement);
                }
                if (jspConfiguration.getPreludeList().size() > 0) {
                    try {
                        insertImplictIncludes(jspConfiguration.getPreludeList());
                    }
                    catch (JspCoreException e) {
                        throw new SAXException(e.getLocalizedMessage());
                    }
                }
            }
        }
        lastLineNum = locator.getLineNumber();
        lastColNum = locator.getColumnNumber();
        charsBuffers.push(new CharacterBuffer());
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        Node n = (Node) elementStack.peek();
        boolean keepWhitespace = false;
        if (n instanceof Element) {
            Element e = (Element) n;
            if (e.getNamespaceURI() != null && e.getNamespaceURI().equals(Constants.JSP_NAMESPACE)) {
                if (e.getLocalName().equals(Constants.JSP_TEXT_TYPE)) {
                    keepWhitespace = true;
                }
            }
        }
        StringBuffer characters = new StringBuffer();
        textLineNum = locator.getLineNumber();
        textColNum = locator.getColumnNumber();

        CharacterBuffer chars = (CharacterBuffer) charsBuffers.peek();

        for (int i = 0; i < length; i++) {
            characters.append(ch[start + i]);
            if (ch[start + i] != '\n' && ch[start + i] != ' ' && ch[start + i] != '\r' && ch[start + i] != '\t') {
                chars.setNonWhiteSpaceFound();
            }
        }

        if (chars.isNonWhiteSpaceFound() || keepWhitespace) {
            chars.charsBuffer.append(characters);
        }
        lastLineNum = locator.getLineNumber();
        lastColNum = locator.getColumnNumber();
    }
    
    public void endElement(String uri, String localName, String qName) throws SAXException {
        Node node = null;
        CharacterBuffer chars = (CharacterBuffer) charsBuffers.pop();
        if (uri.equals(Constants.JSP_NAMESPACE)) {
            if (localName.equals(Constants.JSP_DECLARATION_TYPE) || 
                localName.equals(Constants.JSP_EXPRESSION_TYPE) || 
                localName.equals(Constants.JSP_SCRIPTLET_TYPE)) {
                String jspText = chars.charsBuffer.toString();
                CDATASection cdata = document.createCDATASection(jspText);
                node = (Node) elementStack.pop();
                node.appendChild(cdata);
            }
            else if (localName.equals(Constants.JSP_INCLUDE_DIRECTIVE_TYPE)) {}
            else {
                node = getJspElement();
                createJspTextElement(node, chars.charsBuffer);
            }
        }
        else {
            node = getJspElement();
            createJspTextElement(node, chars.charsBuffer);
        }
        if (node != null && node.getParentNode() instanceof Document) {
            elementStack.push(node);    
            if (jspConfiguration.getCodaList().size() > 0) {
                try {
                    insertImplictIncludes(jspConfiguration.getCodaList());
                }
                catch (JspCoreException e) {
                    throw new SAXException(e.getLocalizedMessage());
                }
            }
            elementStack.pop();
        }                    
        lastLineNum = locator.getLineNumber();
        lastColNum = locator.getColumnNumber();
        chars = null;
    }

    public void createJspTextElement(Node node, StringBuffer chars) {
        String jspText = chars.toString();
        if (chars.length() > 0) {
            CDATASection cdata = document.createCDATASection(jspText);
            node.appendChild(cdata);
            String jspId = encodedRelativeURL + "[" + textLineNum + "," + textColNum + "," + getLineCount(jspText) + "]";
            cdataJspIdMap.put(new Integer(cdata.hashCode()), jspId);
        }
    }

    public Node getJspElement() {
        Node node = (Node) elementStack.pop();
        if (node.hasChildNodes()) {
            Element jspElement = (Element) node;
            String jspId = jspElement.getAttributeNS(Constants.JSP_NAMESPACE, "id");
            jspId = jspId + "[" + lastLineNum + "," + lastColNum + "]";
            jspElement.setAttributeNS(Constants.JSP_NAMESPACE, "jsp:id", jspId); // 245645.1
        }
        return node;
    }

    public void comment(char[] buf, int offset, int len) throws SAXException {
        if (inDTD == false) {
            /*                        
                        Node node = (Node)elementStack.peek();
                        CDATASection cdata = document.createCDATASection("<!--" + new String(buf, offset, len) + "-->");
                        Element jspTextElement = document.createElementNS(Constants.JSP_NAMESPACE, "jsp:text");
                        jspTextElement.appendChild(cdata);
                        if (node instanceof Document)
                            preRootCommentList.add(jspTextElement);
                        else
                            node.appendChild(jspTextElement);
             */
        }
        lastLineNum = locator.getLineNumber();
        lastColNum = locator.getColumnNumber();
    }

    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    public void startCDATA() throws SAXException {}

    public void endCDATA() throws SAXException {
        CharacterBuffer chars = (CharacterBuffer) charsBuffers.peek();
        chars.clearNonWhiteSpaceFound();
    }

    public void startEntity(String name) throws SAXException {}

    public void endEntity(String name) throws SAXException {}

    public void startDTD(String name, String publicId, String systemId) throws SAXException {
        inDTD = true;
        if (isValidating == false) {
            throw new SAXException(DTD_FOUND_MESSAGE);
        }
    }

    public void endDTD() throws SAXException {
        inDTD = false;
    }

    public void fatalError(SAXParseException e) throws SAXException {
        throw e;
    }

    public void error(SAXParseException e) throws SAXException {
        throw e;
    }

    private void insertInclude(String includePath) throws JspCoreException {

        String fullPath = null;
        
        if (includePath.startsWith("/") == false) {
            int lastLocation = this.resolvedRelativeURL.lastIndexOf("/");
            if (lastLocation > 0) {
                includePath = WSUtil.resolveURI(this.resolvedRelativeURL.substring(0, lastLocation+1) + includePath);
            }
            else {
                includePath = WSUtil.resolveURI("/"+ includePath);
            }
        }

        Container container = ctxt.getServletContext().getModuleContainer();
        if (container!=null) {
            Entry e = container.getEntry(includePath);
            if (e!=null) {
                fullPath = e.getPath();
                try {
                    Container convertedContainer = e.adapt(Container.class); 
                    if (convertedContainer!=null && e.getSize()==0 && !WCCustomProperties.ALLOW_DIRECTORY_INCLUDE)
                        return;
                } catch (UnableToAdaptException ex) {
                    throw new IllegalStateException(ex);
                }
            } else {
                return;
            }
        } else {
            fullPath = ctxt.getRealPath(includePath);
            //PM22082
            if (new File(fullPath).isDirectory() && !WCCustomProperties.ALLOW_DIRECTORY_INCLUDE)
                return;
            //PM22082
        }

        
        if (dependencyStack.contains(fullPath))
            throw new JspCoreException("jsp.error.static.include.circular.dependency", new Object[] { fullPath });
        dependencyStack.push(fullPath);
        URL absoluteURL = inputSource.getAbsoluteURL();
        if (absoluteURL == null || absoluteURL.getProtocol().equals("file")) {
            /* Only add static include dependencies if they are not in a jar */
            dependencyList.add(includePath);
        }
        JspConfiguration includeConfiguration = jspConfiguration.getConfigManager().getConfigurationForStaticInclude(includePath, jspConfiguration);
        JspInputSource includePathInputSource = ctxt.getJspInputSourceFactory().copyJspInputSource(inputSource, includePath);
        // PK24144.2 begin
        Map mergedTagLibMap = new HashMap(implicitTagLibMap);
        mergedTagLibMap.putAll(tagPrefixes);
        // PK24144.2 end
        Jsp2Dom jsp2Dom = new Jsp2Dom(includePathInputSource, 
                                      ctxt, 
                                      directoryStack, 
                                      includeConfiguration, 
                                      jspOptions,  //396002
                                      dependencyStack, 
                                      dependencyList, 
                                      cdataJspIdMap, 
                                      mergedTagLibMap, // PK24144.2
                                      true);
        Document includeDocument = jsp2Dom.getJspDocument();
        Node parentNode = (Node) elementStack.peek();
        if (includeDocument.getDocumentElement().getNamespaceURI() != null
            && includeDocument.getDocumentElement().getNamespaceURI().equals(Constants.JSP_NAMESPACE)
            && includeDocument.getDocumentElement().getLocalName().equals(Constants.JSP_ROOT_TYPE)) {
            for (int i = 0; i < includeDocument.getDocumentElement().getChildNodes().getLength(); i++) {
                Node nodeToBeCopied = includeDocument.getDocumentElement().getChildNodes().item(i);
                Node n = document.importNode(nodeToBeCopied, true);
                if (nodeToBeCopied.getNodeType() == Node.CDATA_SECTION_NODE) {
                    Integer nodeHashCode = new Integer(nodeToBeCopied.hashCode());
                    if (cdataJspIdMap.containsKey(nodeHashCode)) {
                        String jspId = (String) cdataJspIdMap.remove(nodeHashCode);
                        cdataJspIdMap.put(new Integer(n.hashCode()), jspId);
                    }
                }
                parentNode.appendChild(n);
            }
        }
        else {
            for (int i = 0; i < includeDocument.getChildNodes().getLength(); i++) {
                Node n = document.importNode(includeDocument.getChildNodes().item(i), true);
                parentNode.appendChild(n);
            }
        }
        dependencyStack.pop();
    }

    private int getLineCount(String s) {
        int lineCount = 1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n')
                lineCount++;
        }
        return (lineCount);
    }

    private boolean compareEncoding(String enc1, String enc2) {
        boolean equal = false;

        if (enc1.equalsIgnoreCase(enc2)) {
            equal = true;
        }
        else if (enc1.toUpperCase().startsWith("UTF-16") && enc2.toUpperCase().startsWith("UTF-16")) {
            equal = true;
        }

        return equal;
    }

    public String buildLineNumberMessage(String msg) {
        msg = resolvedRelativeURL + "(" + lastLineNum + "," + lastColNum + ") " + msg;
        return msg;
    }

    private void insertImplictIncludes(ArrayList implicitIncludeList) throws JspCoreException {
        for (Iterator itr = implicitIncludeList.iterator(); itr.hasNext();) {
            String includePath = (String) itr.next();
            insertInclude(includePath);
        }
    }
    
    private class CharacterBuffer {
        StringBuffer charsBuffer = new StringBuffer();
        private boolean nonWhiteSpaceFound = false;
            
        public boolean isNonWhiteSpaceFound() {
            return nonWhiteSpaceFound;
        }
        
        public void setNonWhiteSpaceFound() {
            nonWhiteSpaceFound = true;
        }
        
        public void clearNonWhiteSpaceFound() {
            nonWhiteSpaceFound = false;
        }
    }
}
