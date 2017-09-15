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
package com.ibm.ws.jsp.taglib;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.configuration.JspConfigurationManager;
import com.ibm.wsspi.jsp.context.JspCoreContext;


public class ImplicitTldParser extends TldParser {

    static protected Logger logger;
    static protected Level logLevel = Level.FINEST;
    private static final String CLASS_NAME="com.ibm.ws.jsp.taglib.ImplicitTldParser";
    static {
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }

    private static final String TLIB_VERSION = "1.0";
    private static final String JSP_VERSION = "2.0";
    String tlibversion = TLIB_VERSION;
    String jspversion = JSP_VERSION;

    public ImplicitTldParser(JspCoreContext ctxt,
                     JspConfigurationManager configManager,
                     boolean validateTLDs) throws JspCoreException {
        super(ctxt, configManager, validateTLDs);
    }


    public void startElement(String namespaceURI,
                             String localName,
                             String elementName,
                             Attributes attrs)
        throws SAXException {
        chars = new StringBuffer();
        if (elementName.equals("taglib")) {
            currentElement = TldParser.TAGLIB_ELEMENT;
            tli.setTlibversion(tlibversion);
            tli.setRequiredVersion(jspversion);
            String ver=attrs.getValue("version");
            if (ver!=null) {
                try {
                    double version = Double.parseDouble(ver);
                    if (version < 2.0) {
                        String message = JspCoreException.getMsg("jsp.error.invalid.implicit.version", new Object[]{this.tldLocation, ver.trim()});
                        logger.logp(Level.FINE, CLASS_NAME, "startElement", message);
                        throw new SAXException(message);
                    }
                } catch (NumberFormatException e) {
                    String message = JspCoreException.getMsg("jsp.error.invalid.implicit.version", new Object[]{this.tldLocation, ver.trim()});
                    logger.logp(Level.FINE, CLASS_NAME, "startElement", message);
                    throw new SAXException(message);
                }
                tli.setRequiredVersion(ver.trim());
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
            logger.logp(logLevel, CLASS_NAME, "startElement", "currentElement= ["+elementTypes[currentElement-1]+"]");
        }
    }

    public void endElement(String namespaceURI,
                           String localName,
                           String elementName)
        throws SAXException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
            logger.logp(logLevel, CLASS_NAME, "endElement", "namespaceURI= ["+namespaceURI+"] localName= ["+localName+"] elementName=["+elementName+"]");
        }
        if (elementName.equals("tlibversion") || elementName.equals("tlib-version")) {
        	tlibversion=chars.toString().trim();
            tli.setTlibversion(tlibversion);
        }
        else if (elementName.equals("jspversion") || elementName.equals("jsp-version")) {
            jspversion =chars.toString().trim();
            try {
                double version = Double.parseDouble(jspversion);
                if (version < 2.0) {
                    String message = JspCoreException.getMsg("jsp.error.invalid.implicit.version", new Object[]{this.tldLocation, jspversion});
                    logger.logp(Level.FINE, CLASS_NAME, "startElement", message);
                    throw new SAXException(message);
                }
            } catch (NumberFormatException e) {
                String message = JspCoreException.getMsg("jsp.error.invalid.implicit.version", new Object[]{this.tldLocation, jspversion});
                logger.logp(Level.FINE, CLASS_NAME, "startElement", message);
                throw new SAXException(message);
            }
            tli.setRequiredVersion(jspversion);
        }
        else if (elementName.equals("shortname") || elementName.equals("short-name") || elementName.equals("taglib")) {
            // ignore
        }
        else {
            // All other elements are invalid
            String message = JspCoreException.getMsg("jsp.error.invalid.implicit", new Object[]{this.tldLocation, elementName});
            logger.logp(Level.FINE, CLASS_NAME, "startElement", message);
            throw new SAXException(message);
        }

        chars = null;
    }
}
