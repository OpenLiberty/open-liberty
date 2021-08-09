package com.ibm.json.xml.internal;

/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import java.util.logging.*;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Properties;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;


/**
 * This class is a SAX entension to do conversion of XML to JSON.
 */
public class JSONSAXHandler extends DefaultHandler
{
    /**
     * Logger code
     */
    private static String  className              = "com.ibm.json.xml.transform.impl.JSONSAXHandler";
    private static Logger logger                  = Logger.getLogger(className,null);

    /**
     * The writer to stream the JSON text out to.
     */
    private OutputStreamWriter osWriter           = null;

    /**
     * The current JSON object being constructed from the current TAG being parsed.
     */
    private JSONObject current                    = null;

    /** 
     * The stack of the current JSON object position.
     */
    private Stack previousObjects                 = new Stack();

    /**
     * The toplevel containing JSON object.
     */
    private JSONObject head                       = null;

    /**
     * Whether or not to render the JSON text is a compact or indented format.
     */
    private boolean compact                       = false;

    /**
     * Constructor.
     * @param os The outputStream to write the resulting JSON to.  Same as JSONSAXHander(os,false);
     * @throws IOException Thrown if an error occurs during streaming out, or XML read.
     */
    public JSONSAXHandler(OutputStream os)
    throws IOException
    {
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "JSONHander(OutputStream) <constructor>");
        
        this.osWriter = new OutputStreamWriter(os,"UTF-8");
        this.compact  = true;
        
        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "JSONHander(OutputStream) <constructor>");
    }

    /**
     * Constructor.
     * @param os The outputStream to write the resulting JSON to
     * @param verbose Whenther or not to render the stream in a verbose (formatted), or compact form.
     * @throws IOException Thrown if an error occurs during streaming out, or XML read.
     */
    public JSONSAXHandler(OutputStream os, boolean verbose)
    throws IOException
    {
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "JSONHander(OutputStream, boolean) <constructor>");
        
        this.osWriter = new OutputStreamWriter(os,"UTF-8");
        this.compact  = !verbose;
        
        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "JSONHander(OutputStream, boolean) <constructor>");
    }


    /**
     * This function parses an IFix top level element and all its children.
     */
    public void startElement(String namespaceURI, String localName, String qName, Attributes attrs)
    throws SAXException
    {
        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "startElement(String,String,String,org.xml.sax.Attributes)");

        Properties props = new Properties();
        int attrLength = attrs.getLength();
        for (int i = 0; i < attrLength; i++)
        {
            props.put(attrs.getQName(i), attrs.getValue(i));
        }

        JSONObject obj = new JSONObject(localName, props);
        if (this.head == null)
        {
            this.head    = obj;
            this.current = head;
        }
        else
        {
            if (current != null)
            {
                this.previousObjects.push(current);
                this.current.addJSONObject(obj);
            }
            this.current  = obj;
        }

        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "startElement(String,String,String,org.xml.sax.Attributes)");
    }

    /**
     * Function ends a tag in this iFix parser.
     */
    public void endElement(String uri, String localName, String qName) 
    throws SAXException
    {
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "endElement(String,String,String)");

        if (!previousObjects.isEmpty())
        {
            this.current = (JSONObject)this.previousObjects.pop();
        }
        else
        {
            this.current = null;
        }

        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "endElement(String,String,String)");
    }

    public void characters(char[] ch,
                       int start,
                       int length)
                throws SAXException
    {
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "characters(char[], int, int)");

        String str = new String(ch,start,length);
        if (this.current.getTagText() != null) {
            str = this.current.getTagText() + str;
        }
        this.current.setTagText(str);

        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "characters(char[], int, int)");
    }
    
    public void startDocument()
    throws SAXException
    {
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "startDocument()");

        startJSON();

        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "startDocument()");
    }

    public void endDocument()
    throws SAXException
    {
        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "endDocument()");

        endJSON();

        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "endDocument()");
    }

    /**
     * Method to flush out anything remaining in the buffers.
     */
    public void flushBuffer()
    throws IOException
    {
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "flushBuffer()");

        if (this.osWriter != null)
        {
            this.osWriter.flush();
        }

        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "flushBuffer()");
    }

    /**
     * Internal method to start JSON generation.
     */
    private void startJSON()
    throws SAXException
    {
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "startJSON()");

        this.head    = new JSONObject("",null);
        this.current = head;

        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "startJSON()");
    }
    
    /**
     * Internal method to end the JSON generation and to write out the resultant JSON text 
     * and reset the internal state of the hander.
     */
    private void endJSON()
    throws SAXException
    {   
        if (logger.isLoggable(Level.FINER)) logger.entering(className, "endJSON()");

        try
        {
            this.head.writeObject(this.osWriter, 0, true, this.compact);
            this.head    = null;
            this.current = null;
            this.previousObjects.clear();
        }
        catch (Exception ex)
        {
            SAXException saxEx = new SAXException(ex);
            saxEx.initCause(ex);
            throw saxEx;
        }

        if (logger.isLoggable(Level.FINER)) logger.exiting(className, "endJSON()");
    }

}


