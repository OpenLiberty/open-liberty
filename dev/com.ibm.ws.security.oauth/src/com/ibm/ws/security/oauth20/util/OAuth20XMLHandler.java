/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.security.oauth20.api.Constants;

public class OAuth20XMLHandler extends DefaultHandler {
    private static TraceComponent tc = Tr.register(OAuth20XMLHandler.class,
            "OAuth20Provider", "com.ibm.ws.security.oauth20.resources.ProviderMsgs");

    protected File configFile = null;

    protected ArrayList<OAuth20Parameter> parameters = null;
    protected OAuth20Parameter currentParam = null;
    boolean inValue = false;
    StringBuilder currentValue = null;

    public OAuth20XMLHandler(File f) {
        parameters = new ArrayList<OAuth20Parameter>();
        configFile = f;
    }

    public void parse() throws SAXException, IOException,
            ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(configFile, this);
    }

    public List<OAuth20Parameter> getParameters() {
        return parameters;
    }

    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        if (qName.equals(Constants.XML_TAG_PARAM)) {
            String paramName = attributes
                    .getValue(Constants.XML_ATTR_PARAM_NAME);
            String paramType = attributes
                    .getValue(Constants.XML_ATTR_PARAM_TYPE);
            String customizable = attributes
                    .getValue(Constants.XML_ATTR_PARAM_CUSTOMIZABLE);
            currentParam = new OAuth20Parameter(paramName, paramType, customizable);
            parameters.add(currentParam);
        } else if (qName.equals(Constants.XML_TAG_VALUE)) {
            inValue = true;
            currentValue = new StringBuilder();
        }
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (qName.equals(Constants.XML_TAG_VALUE)) {
            currentParam.addValue(currentValue.toString());
            inValue = false;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "read parameter", new Object[] { currentParam });
            }
        }
    }

    public void characters(char ch[], int start, int length)
            throws SAXException {
        if (inValue) {
            currentValue.append(ch, start, length);
        }
    }

};