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

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.plugins.BaseClient;

public class ClientProviderXMLHandler extends DefaultHandler {
    protected File configFile = null;

    protected ArrayList<BaseClient> clients = null;
    protected BaseClient currentClient = null;

    public ClientProviderXMLHandler(File f) {
        clients = new ArrayList<BaseClient>();
        configFile = f;
    }

    public void parse() throws SAXException, IOException,
            ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(configFile, this);
    }

    public List<BaseClient> getClients() {
        return clients;
    }

    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        if (qName.equals(Constants.XML_TAG_CLIENT)) {
            String id = attributes.getValue(Constants.XML_ATTR_CLIENT_ID);
            String component = attributes
                    .getValue(Constants.XML_ATTR_CLIENT_COMPONENT);
            String secret = attributes
                    .getValue(Constants.XML_ATTR_CLIENT_SECRET);
            String displayName = attributes
                    .getValue(Constants.XML_ATTR_CLIENT_DISPLAYNAME);
            String redirect = attributes
                    .getValue(Constants.XML_ATTR_CLIENT_REDIRECT);
            boolean enabled = "true".equalsIgnoreCase(attributes
                    .getValue(Constants.XML_ATTR_CLIENT_ENABLED));

            // decode client secret
            secret = PasswordUtil.passwordDecode(secret);
            BaseClient newClient = new BaseClient(component, id, secret,
                    displayName, OidcOAuth20Util.initJsonArray(redirect), enabled);
            clients.add(newClient);
        }
    }

};