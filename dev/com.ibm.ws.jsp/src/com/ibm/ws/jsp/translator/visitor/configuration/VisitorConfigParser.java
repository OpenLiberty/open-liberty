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
package com.ibm.ws.jsp.translator.visitor.configuration;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.translator.visitor.xml.ParserFactory;

public class VisitorConfigParser extends DefaultHandler {


    public static final String DTD_PUBLIC_ID = "http://www.ibm.com/xml/ns/JspVisitorConfiguration.xsd";
    public static final String DTD_RESOURCE_PATH = "/com/ibm/ws/jsp/translator/visitor/configuration/JspVisitorConfiguration.xsd";
    
    protected SAXParser saxParser = null;
    protected JspVisitorUsage visitorUsage = null;
    protected JspVisitorCollection visitorCollection = null;
    protected JspVisitorDefinition visitorDefinition = null;
    protected JspVisitorConfiguration visitorConfiguration = null;
    protected String className = null;
    protected String resultType = null;
    
    protected StringBuffer chars = null;
    protected ClassLoader cl = null;
    
    public VisitorConfigParser(ClassLoader cl) throws JspCoreException {
        this.cl = cl;
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
    
    public JspVisitorConfiguration parse(InputStream is) throws JspCoreException {
        visitorConfiguration = new JspVisitorConfiguration();
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
        
        return (visitorConfiguration);
    }
    
    public void startElement(String namespaceURI, 
                             String localName,
                             String elementName, 
                             Attributes attrs) 
        throws SAXException {
        chars = new StringBuffer();
        if (elementName.equals("jsp-visitor-collection")) {
            visitorCollection = new JspVisitorCollection();
            visitorCollection.setId(attrs.getValue("id"));
        }
        else if (elementName.equals("jsp-visitor-definition")) {
            visitorDefinition = new JspVisitorDefinition();
            visitorDefinition.setId(attrs.getValue("id"));
        }
        else if (elementName.equals("jsp-visitor")) {
            int order = 1;
            int visits = 1;
            try {
                order = Integer.valueOf(attrs.getValue("order")).intValue();
                visits = Integer.valueOf(attrs.getValue("visits")).intValue();
            }
            catch (NumberFormatException e) {}
            String id = attrs.getValue("id");
            visitorUsage = new JspVisitorUsage(order, visits, visitorConfiguration.getJspVisitorDefinition(id));
            visitorCollection.getJspVisitorUsageList().add(visitorUsage);
            visitorUsage = null;
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
        else if (elementName.equals("result-type")) {
            resultType = chars.toString().trim();
        }
        else if (elementName.equals("jsp-visitor-collection")) {
            visitorConfiguration.addJspVisitorCollection(visitorCollection);
            visitorCollection = null;
        }
        else if (elementName.equals("jsp-visitor-definition")) {
            try {
                Class visitorClass = Class.forName(className, true, cl);
                Class visitorResultClass = Class.forName(resultType, true, cl);
                visitorDefinition.setVisitorClass(visitorClass);
                visitorDefinition.setVisitorResultClass(visitorResultClass);
            }
            catch (ClassNotFoundException e) {
            	String message = JspCoreException.getMsg("jsp.error.failed.load.jsp-visitor-definition");
                throw new SAXException (message, e);
            }
            visitorConfiguration.addJspVisitorDefinition(visitorDefinition);
            visitorDefinition = null;
            className = null;
            resultType = null;
        }
        chars = null;
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
                throw new SAXException(JspCoreException.getMsg("jsp.error.internal.dtd.not.found"));
            }
            isrc = new InputSource(input);
        }
        return isrc;
    }
}
