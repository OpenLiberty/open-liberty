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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.translator.visitor.xml.ParserFactory;
import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.jsp.resource.JspInputSource;

public class JspDocumentConverter {
    private JspInputSource inputSource = null;
    private String resovledRelativeURL = null;
    private String encodedRelativeURL = null;
    private JspCoreContext ctxt = null;
    private JspConfiguration jspConfiguration = null;
    private JspOptions jspOptions = null;  //396002
    private Stack directoryStack = null;
    private Stack dependencyStack = null;
    private List dependencyList = null;
    private Map cdataJspIdMap = null;
    private Map implicitTagLibMap = null;
    private String jspPrefix = "jsp";
    
    public JspDocumentConverter(JspInputSource inputSource,
                                String resovledRelativeURL,
                                JspCoreContext ctxt,
                                Stack directoryStack,
                                JspConfiguration jspConfiguration,
                                JspOptions jspOptions,  // 396002
                                Stack dependencyStack,
                                List dependencyList,
                                Map cdataJspIdMap,
                                Map implicitTagLibMap) {
        this.inputSource = inputSource;
        this.resovledRelativeURL = resovledRelativeURL;
        try {
            encodedRelativeURL = URLEncoder.encode(resovledRelativeURL, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            FFDCFilter.processException(e, "com.ibm.ws.jsp.translator.document.JspDocumentConverter", "66", new Object[] {resovledRelativeURL});
        }
        this.ctxt = ctxt;
        this.jspConfiguration = jspConfiguration;
        this.jspOptions = jspOptions;  //396002
        this.directoryStack = directoryStack;
        this.dependencyStack = dependencyStack;
        this.dependencyList = dependencyList;
        this.cdataJspIdMap = cdataJspIdMap;
        this.implicitTagLibMap = implicitTagLibMap;
    }
    
    public Document convert() throws JspCoreException {
        Document convertedDocument = null;
        try {
            convertedDocument = ParserFactory.newDocument(false, false);
        }
        catch (ParserConfigurationException e) {
            throw new JspCoreException(e);
        }
        
        convertDocument(convertedDocument, convertedDocument, inputSource.getDocument().getChildNodes());
        return convertedDocument;
    }
    
    private void convertDocument(Document convertedDocument, Node node, NodeList childNodes) throws JspCoreException {
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNamespaceURI() != null && childNode.getNamespaceURI().equals(Constants.JSP_NAMESPACE)) {
                jspPrefix = childNode.getPrefix();
            }
            
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element)childNode;
                if (element.getLocalName() != null &&
                    element.getLocalName().equals(Constants.JSP_INCLUDE_DIRECTIVE_TYPE) &&
                    element.getNamespaceURI().equals(Constants.JSP_NAMESPACE)) {
                    insertInclude(convertedDocument, element.getParentNode(), element.getAttribute("file"));   
                }
                else {
                    Node newNode = convertedDocument.importNode(childNode, false);
                    StringBuffer attrNames = new StringBuffer();
                    NamedNodeMap attrs = element.getAttributes();
                    for (int j = 0; j < attrs.getLength(); j++) {
                        Attr attr = (Attr)attrs.item(j);
                        if (attr.getNodeName().startsWith("xmlns") == false) {
                            if (attr.getPrefix() != null) {
                                attrNames.append(attr.getPrefix()+":"+attr.getLocalName()+"~");
                            }
                            else {
                                attrNames.append(attr.getLocalName()+"~");
                            }
                        }
                    }
                    String jspId = null;
                    if (attrs.getLength() > 0) {
                        jspId = "{"+attrNames.toString()+"}"+encodedRelativeURL + "[0,0,0]";
                    }
                    else {                         
                        jspId = encodedRelativeURL + "[0,0,0]";
                    }
                        
                    ((Element)newNode).setAttributeNS(Constants.JSP_NAMESPACE, jspPrefix + ":id", jspId);
                    node.appendChild(newNode);
                    convertDocument(convertedDocument, newNode, childNode.getChildNodes());           
                }
            }
            else if (childNode.getNodeType() == Node.CDATA_SECTION_NODE) {
                boolean keepWhitespace = false;
                boolean nonWhiteSpaceFound = false;
                if (node.getNamespaceURI() != null && 
                    node.getNamespaceURI().equals(Constants.JSP_NAMESPACE) &&
                    node.getLocalName().equals(Constants.JSP_TEXT_TYPE)) {
                   keepWhitespace = true;
                }
                CDATASection cdata = (CDATASection)childNode;
                for (int j = 0; j < cdata.getLength(); j++) {
                    char ch = cdata.getData().charAt(j);
                    if (ch != '\n' && ch != ' ' && ch != '\r' && ch != '\t') {
                        nonWhiteSpaceFound = true;
                    }
                }
                if (nonWhiteSpaceFound) {
                    Node newNode = convertedDocument.importNode(childNode, false);
                    node.appendChild(newNode);
                }
                else if (keepWhitespace) {
                    Node newNode = convertedDocument.importNode(childNode, false);
                    node.appendChild(newNode);
                }
            }
            else if (childNode.getNodeType() == Node.TEXT_NODE) {
                boolean keepWhitespace = false;
                boolean nonWhiteSpaceFound = false;
                if (node.getNamespaceURI() != null && 
                    node.getNamespaceURI().equals(Constants.JSP_NAMESPACE) &&
                    node.getLocalName().equals(Constants.JSP_TEXT_TYPE)) {
                   keepWhitespace = true;
                }
                Text text = (Text)childNode;
                for (int j = 0; j < text.getLength(); j++) {
                    char ch = text.getData().charAt(j);
                    if (ch != '\n' && ch != ' ' && ch != '\r' && ch != '\t') {
                        nonWhiteSpaceFound = true;
                    }
                }
                if (nonWhiteSpaceFound) {
                    Node newNode = convertedDocument.createCDATASection(text.getData());
                    node.appendChild(newNode);
                }
                else if (keepWhitespace) {
                    Node newNode = convertedDocument.createCDATASection(text.getData());
                    node.appendChild(newNode);
                }
            }
            else {
                Node newNode = convertedDocument.importNode(childNode, false);
                node.appendChild(newNode);
                convertDocument(convertedDocument, newNode, childNode.getChildNodes());           
            }
        }
    }
    
    private void insertInclude(Document document, Node parentNode, String includePath) throws JspCoreException {
        String fullPath = ctxt.getRealPath(includePath);
        if (dependencyStack.contains(fullPath))
            throw new JspCoreException("jsp.error.static.include.circular.dependency", new Object[] { fullPath });
        dependencyStack.push(fullPath);
        if (inputSource.getAbsoluteURL().getProtocol().equals("file")) {
            /* Only add static include dependencies if they are not in a jar */
            String depIncludePath = includePath;
            if (depIncludePath.startsWith("/") == false) {
                int pos = resovledRelativeURL.lastIndexOf("/");
                if (pos > 0) {
                    depIncludePath = resovledRelativeURL.substring(0, pos + 1) + depIncludePath;
                }
                else {
                    depIncludePath = "/" + depIncludePath;
                }
            }
            dependencyList.add(depIncludePath);
        }
        JspConfiguration includeConfiguration = jspConfiguration.getConfigManager().getConfigurationForStaticInclude(includePath, jspConfiguration);
        JspInputSource includePathInputSource = ctxt.getJspInputSourceFactory().copyJspInputSource(inputSource, includePath);
        Jsp2Dom jsp2Dom = new Jsp2Dom(includePathInputSource, 
                                      ctxt, 
                                      directoryStack, 
                                      includeConfiguration, 
                                      jspOptions,  //396002
                                      dependencyStack, 
                                      dependencyList, 
                                      cdataJspIdMap, 
                                      implicitTagLibMap, 
                                      true);
        Document includeDocument = jsp2Dom.getJspDocument();
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
    
    public void printElements(Element element, int level) {
        for (int i = 0; i < level; i++) {
            System.out.print("\t");
        }
        System.out.println("Element - " + element.getNodeName());

        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = (Attr) attrs.item(i);
            for (int j = 0; j < level; j++) {
                System.out.print("\t");
            }
            System.out.println("Attr - " + attr.getName() + " : " + attr.getValue() + " : " + attr.getNamespaceURI());
        }

        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n instanceof Element) {
                Element childElement = (Element) n;
                System.out.println();
                printElements(childElement, level + 1);
            }
            else if (n instanceof CDATASection) {
                System.out.println();
                CDATASection cdata = (CDATASection) n;
                for (int j = 0; j < level + 1; j++) {
                    System.out.print("\t");
                }
                String s = cdata.getData();
                s = s.replaceAll("\r", "");
                s = s.replaceAll("\n", "{cr}");
                System.out.println("CDATA - [" + s + "]");
            }
            else if (n instanceof Text) {
                System.out.println();
                Text text = (Text)n;
                for (int j = 0; j < level + 1; j++) {
                    System.out.print("\t");
                }
                String s = text.getData();
                s = s.replaceAll("\r", "");
                s = s.replaceAll("\n", "{cr}");
                System.out.println("Text - [" + s + "]");
                
            }
        }
    }

}
