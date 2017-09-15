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
package com.ibm.ws.jsp.taglib.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.translator.visitor.xml.ParserFactory;
import com.ibm.wsspi.jsp.taglib.config.GlobalTagLibConfig;
import com.ibm.wsspi.jsp.taglib.config.TldPathConfig;

public class TagLibCacheConfigParser extends DefaultHandler {
    public static final String DTD_PUBLIC_ID = "http://www.ibm.com/xml/ns/TagLibCacheConfig.xsd";
    public static final String DTD_RESOURCE_PATH = "/com/ibm/ws/jsp/tablib/config/TagLibCacheConfig.xsd";
    
    protected SAXParser saxParser = null;

    protected StringBuffer chars = null;
    
    protected String uri = null;
    protected String prefix = null;
    protected String location = null;
    protected String jarName = null;
    protected String type = null;
    protected String webinfFileName = null;
    protected String servletClassName = null;
    
    protected List implicitTagLibList = new ArrayList();
    protected List globalTagLibList = new ArrayList();
    
    protected ImplicitTagLibConfig implicitTagLibConfig = null;
    protected GlobalTagLibConfig globalTagLibConfig = null;
    protected TldPathConfig tldPathConfig = null;
    protected AvailabilityCondition availabilityCondition = null;
    
    
    public TagLibCacheConfigParser() throws JspCoreException {
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
    
    public void parse(InputStream is) throws JspCoreException {
    	reset();
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
    }
    
    public void startElement(String namespaceURI, 
                             String localName,
                             String elementName, 
                             Attributes attrs) 
        throws SAXException {
        chars = new StringBuffer();
        if (elementName.equals("global-taglib")) {
            globalTagLibConfig = new GlobalTagLibConfig();
        }
        else if (elementName.equals("tld-path")) {
            tldPathConfig = new TldPathConfig(attrs.getValue("path"), attrs.getValue("uri"), attrs.getValue("contains-listener-defs"));
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
        if (elementName.equals("uri")) {
            uri = chars.toString().trim();
        }
        else if (elementName.equals("prefix")) {
            prefix = chars.toString().trim();
        }
        else if (elementName.equals("location")) {
            location = chars.toString().trim();
        }
        else if (elementName.equals("jar-name")) {
            jarName = chars.toString().trim();
        }
        else if (elementName.equals("type")) {
            type = chars.toString().trim();
        }
        else if (elementName.equals("webinf-filename")) {
            webinfFileName = chars.toString().trim();
        }
        else if (elementName.equals("servlet-classname")) {
            servletClassName = chars.toString().trim();
        }
        else if (elementName.equals("implict-taglib")) {
            implicitTagLibConfig = new ImplicitTagLibConfig(uri, prefix, location); 
            implicitTagLibList.add(implicitTagLibConfig);
            implicitTagLibConfig = null;
        }
        else if (elementName.equals("global-taglib")) {
            globalTagLibConfig.setJarName(jarName);
            globalTagLibList.add(globalTagLibConfig);
            globalTagLibConfig = null;
        }
        else if (elementName.equals("tld-path")) {
            globalTagLibConfig.getTldPathList().add(tldPathConfig);
            tldPathConfig = null;
        }
        else if (elementName.equals("availability-condition")) {
            AvailabilityConditionType conditionType = null;
            String conditionValue = null;
            if (type.equalsIgnoreCase("WEBINF-FILE")) {
                conditionType = AvailabilityConditionType.webinfFileType;
                conditionValue = webinfFileName; 
            }
            else if (type.equalsIgnoreCase("SERVLET-CLASSNAME")) {
                conditionType = AvailabilityConditionType.servletClassNameType;
                conditionValue = servletClassName;
            }
            availabilityCondition = new AvailabilityCondition(conditionType, conditionValue);
            tldPathConfig.getAvailabilityConditionList().add(availabilityCondition);
            availabilityCondition = null;
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
    
    public List getGlobalTagLibList() {
        return globalTagLibList;
    }

    public List getImplicitTagLibList() {
        return implicitTagLibList;
    }
    private void reset() {    
    	uri = null;
    	prefix = null;
    	location = null;
    	jarName = null;
    	type = null;
    	webinfFileName = null;
    	servletClassName = null;

    	implicitTagLibList = new ArrayList();
    	globalTagLibList = new ArrayList();

    	implicitTagLibConfig = null;
    	globalTagLibConfig = null;
    	tldPathConfig = null;
    	availabilityCondition = null;
    }
}
