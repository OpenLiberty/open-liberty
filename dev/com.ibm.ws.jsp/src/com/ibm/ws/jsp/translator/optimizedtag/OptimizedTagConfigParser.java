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
package com.ibm.ws.jsp.translator.optimizedtag;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.translator.visitor.xml.ParserFactory;
import com.ibm.wsspi.jsp.context.JspCoreContext;

public class OptimizedTagConfigParser extends DefaultHandler {
    public static final String DTD_PUBLIC_ID = "http://www.ibm.com/xml/ns/OptimizedTag.xsd";
    public static final String DTD_RESOURCE_PATH = "/com/ibm/ws/jsp/translator/optimizedtag/OptimizedTag.xsd";
    
    protected SAXParser saxParser = null;
    protected JspCoreContext ctxt = null;

    protected StringBuffer chars = null;
    protected Map optimizedTagConfigMap = new HashMap();
    protected OptimizedTagConfig optimizedTagConfig = null;
    protected String className = null;
    protected String uri = null;
    protected String version = null;
    protected String shortName = null;
    
    public OptimizedTagConfigParser(JspCoreContext ctxt) throws JspCoreException {
        this.ctxt = ctxt;
        try {
            saxParser = ParserFactory.newSAXParser(false, true);
        }
        catch (ParserConfigurationException e) {
            throw new JspCoreException(e);
        }
        catch (SAXException e) {
            throw new JspCoreException(e);
        }
    }
    
    public Map parse(InputStream is) throws JspCoreException {
        optimizedTagConfigMap.clear();
        try {
 			ParserFactory.parseDocument(saxParser, is, this);
        }
        catch (SAXException e) {
            if (e.getCause() != null)
                throw new JspCoreException(e.getCause());
            else
                throw new JspCoreException(e);
                
        }
        catch (IOException e) {
            throw new JspCoreException(e);
        }
        finally {
            try {
                is.close();
            } catch (IOException e) {}
        }
        Map m = new HashMap();
        m.putAll(optimizedTagConfigMap);
        return (m);
    }
    
    public void startElement(String namespaceURI, 
                             String localName,
                             String elementName, 
                             Attributes attrs) 
        throws SAXException {
        chars = new StringBuffer();
        if (elementName.equals("optimized-tag")) {
            optimizedTagConfig = new OptimizedTagConfig();
        }
    }
    
    public void characters(char[] ch, int start, int length) throws SAXException {
        for (int i = 0; i < length; i++) {
            if (chars != null)
                chars.append(ch[start+i]);
        }
    }
    
    public void endElement(String namespaceURI,
                           String localName,
                           String elementName)
        throws SAXException {
        if (elementName.equals("class-name")) {
            className = chars.toString().trim();
        }
        else if (elementName.equals("short-name")) {
            shortName = chars.toString().trim();
        }
        else if (elementName.equals("taglib-uri")) {
            uri = chars.toString().trim();
        }
        else if (elementName.equals("tlib-version")) {
            version = chars.toString().trim();
        }
        else if (elementName.equals("optimized-tag")) {
            try {
                Class optClass = Class.forName(className, true, ctxt.getJspClassloaderContext().getClassLoader()); 
                optimizedTagConfig.setOptClass(optClass);
                optimizedTagConfig.setShortName(shortName);
                optimizedTagConfig.setTlibUri(uri);
                optimizedTagConfig.setTlibversion(version);
                optimizedTagConfigMap.put(uri+version+shortName, optimizedTagConfig);                
            }
            catch (ClassNotFoundException e) {
                throw new SAXException(e);
            }                           
        }
    }

    public InputSource resolveEntity(String publicId, String systemId)
        throws SAXException {
        InputSource isrc = null;
        String resourcePath = null;            
        if (publicId.equals(DTD_PUBLIC_ID)) {
            resourcePath = DTD_RESOURCE_PATH;
        }
        if (resourcePath != null) {
            InputStream input = this.getClass().getResourceAsStream(resourcePath);
            if (input == null) {
                throw new SAXException("jsp.error.internal.dtd.not.found");
            }
            isrc = new InputSource(input);
        }
        return isrc;
    }
}
