/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.ibm.ws.xml.ParserFactory;
import com.ibm.wsspi.kernel.service.location.WsResource;

public class PluginParser implements ContentHandler {
    private HashMap<String, Set<String>> uriGroups = new HashMap<String, Set<String>>(3);
    private HashMap<String, Set<String>> vhostUris = new HashMap<String, Set<String>>(3);

    private HashSet<String> curUris;

    public void parse(WsResource pluginCfgXml) throws IOException {
        if (pluginCfgXml != null && pluginCfgXml.exists()) {
            XMLReader xmlReader;
            InputStream iStream = new BufferedInputStream(pluginCfgXml.get());
            try {
                xmlReader = ParserFactory.createXMLReader();
                xmlReader.setContentHandler(this);
                InputSource is = new InputSource(iStream);
                is.setSystemId(pluginCfgXml.asFile().getAbsolutePath());
                xmlReader.parse(is);
            } catch (SAXException e) {
                throw new IOException(e);
            } catch (ParserConfigurationException e) {
                throw new IOException(e);
            } catch (FactoryConfigurationError e) {
                throw new IOException(e);
            } finally {
                if (iStream != null) {
                    iStream.close();
                }
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {}

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws SAXException {

    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("UriGroup")) {
            curUris = null;
        }

    }

    /** {@inheritDoc} */
    @Override
    public void endPrefixMapping(String prefix) throws SAXException {

    }

    /** {@inheritDoc} */
    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {

    }

    /** {@inheritDoc} */
    @Override
    public void processingInstruction(String target, String data) throws SAXException {

    }

    /** {@inheritDoc} */
    @Override
    public void setDocumentLocator(Locator locator) {

    }

    /** {@inheritDoc} */
    @Override
    public void skippedEntity(String name) throws SAXException {

    }

    /** {@inheritDoc} */
    @Override
    public void startDocument() throws SAXException {

    }

    /** {@inheritDoc} */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (qName.equals("Uri")) {
            curUris.add(atts.getValue("Name"));
        } else if (qName.equals("UriGroup")) {
            String uriGroup = atts.getValue("Name");
            curUris = new HashSet<String>(5);
            uriGroups.put(uriGroup, curUris);
        } else if (qName.equals("Route")) {
            String uriGroup = atts.getValue("UriGroup");
            String vhostGroup = atts.getValue("VirtualHostGroup");
            vhostUris.put(vhostGroup, uriGroups.get(uriGroup));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {

    }

    /**
     * @return the vhostUris
     */
    public HashMap<String, Set<String>> getVhostUris() {
        return vhostUris;
    }
}
