/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.was;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

/**
 * @author yaronr
 *
 * Read configuration properties from a XML file
 * File structure is:
 *******************************************************
 * <?xml version="1.0" encoding="UTF-8"?>
 * <sip-container-configuration>
 * <property>
 *		<name> name1 </name>
 *		<value> value1 </value>
 * 		<description> value1 </description>
 *	</property>
 *	<property>
 *		<name> name2 </name>
 *		<value> value2 </value>
 *		<description> value1 </description>
 *	</property>
 * </sip-container-configuration>
 ****************************************************** 
 */
public class SipConfigurationFileReader implements EntityResolver
{
    /**
     * Identify property entry in the configuration file
     */
    private final static String PROPERTY_ENTRY = "property";

    /**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(SipConfigurationFileReader.class);

    /**
     * DOM Parser.
     */
    private DocumentBuilder m_documentBuilder;

    /**
     * Construct a new parser for parsing sip.xml configuration.
     */
    public SipConfigurationFileReader() throws ParserConfigurationException
    {
        if (c_logger.isTraceEntryExitEnabled())
        {
            c_logger.traceEntry(this, "SipConfigurationFileReader");
        }

        // Create a document builder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        m_documentBuilder = factory.newDocumentBuilder();
        m_documentBuilder.setEntityResolver(this);

        if (c_logger.isTraceEntryExitEnabled())
        {
            c_logger.traceExit(this, "SipConfigurationFileReader");
        }

    }

    /**
     * Parse the input configuration file, return all properties in this file
     * 
     * @param in configuration file as input stream 
     * @return @throws
     *         SAXException
     * @throws IOException
     */
    public Properties parse(InputStream in) throws SAXException, IOException
    {
        Properties configuration = new Properties();

        // parse the file to a document and get all properties
        Document doc = m_documentBuilder.parse(in);
        NodeList properties = doc.getElementsByTagName(PROPERTY_ENTRY);

        //	For each property in the document
        for (int i = 0; i < properties.getLength(); i++)
        {
            NodeList childs = properties.item(i).getChildNodes();
            String name = null;
            String value = null;

            // get the property name and value
            for (int j = 0; j < childs.getLength(); j++)
            {
                Node node = childs.item(j);
                if (node.getNodeName().equalsIgnoreCase("name"))
                {
                    name = getTextNode(node);
                }
                else if (node.getNodeName().equalsIgnoreCase("value"))
                {
                    value = getTextNode(node);
                }
            }

            if (c_logger.isTraceDebugEnabled())
            {
                c_logger.traceDebug(this, "parse", "New Property, name: "
                        + name + ", value: " + value);
            }
            // add the property to the configuration
            if (null != name && null != value)
            {
                configuration.put(name, value);
            }
        }
        return configuration;
    }

    /**
     * Helper function. Looks at the childs of the given node. Returns the first
     * child node's value which is of of TEXT type.
     * 
     * @param node
     * @return String
     */
    private String getTextNode(Node node)
    {
        String rValue = null;
        NodeList list = node.getChildNodes();
        Node child;
        for (int i = 0; i < list.getLength(); i++)
        {
            child = list.item(i);
            if (child.getNodeType() == Node.TEXT_NODE)
            {
                rValue = child.getNodeValue();
                break;
            }
        }
        return rValue;
    }

    /**
     * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String,
     *      java.lang.String)
     * @see http://java.sun.com/j2se/1.4.2/docs/api/org/xml/sax/EntityResolver.html
     */
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException
    {
        // Doesn't look like we have to implement it,
        // http://java.sun.com/j2se/1.4.2/docs/api/org/xml/sax/EntityResolver.html
        // I guess we'll never get here but anyhow, lets log something
        if (c_logger.isTraceDebugEnabled())
        {
            StringBuffer buffer = new StringBuffer("publicId: ");
            buffer.append(publicId);
            buffer.append(", systemId: ");
            buffer.append(systemId);
            c_logger.traceDebug(this, "resolveEntity", buffer.toString());

        }

        return null;
    }
}