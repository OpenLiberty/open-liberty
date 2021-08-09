package com.ibm.json.xml;


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
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.StringTokenizer;
import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.util.logging.*;

import com.ibm.json.xml.internal.JSONSAXHandler;


/**
 * This class is a static helper for various ways of converting an XML document/InputStream into a JSON stream or String.
 * 
 * For example, the XML document:<br>
 * <xmp>
 *   <getValuesReturn return="true">
 *     <attribute attrValue="value"/>
 *     <String>First item</String>
 *     <String>Second item</String>
 *     <String>Third item</String>
 *     <TextTag>Text!</TextTag>
 *     <EmptyTag/>
 *     <TagWithAttrs attr1="value1" attr2="value2" attr3="value3"/>
 *     <TagWithAttrsAndText attr1="value1" attr2="value2" attr3="value3">Text!</TagWithAttrsAndText>
 *   </getValuesReturn>
 * </xmp>
 * <br>
 * in JSON (in non-compact form) becomes<br>
 * <xmp>
 * {
 *    "getValuesReturn" : {
 *       "return" : "true",
 *       "TextTag" : "Text!",
 *       "String" : [
 *          "First item",
 *          "Second item",
 *          "Third item"
 *       ],
 *       "TagWithAttrsAndText" : {
 *          "content" : "Text!",
 *          "attr3" : "value3",
 *          "attr2" : "value2",
 *          "attr1" : "value1"
 *       }
 *       ,
 *       "EmptyTag" : true,
 *       "attribute" : {
 *          "attrValue" : "value"
 *       }
 *       ,
 *       "TagWithAttrs" : {
 *          "attr3" : "value3",
 *          "attr2" : "value2",
 *          "attr1" : "value1"
 *       }
 *    }
 * } 
 * </xmp>
 */
public class XMLToJSONTransformer
{
    /**
     * Logger init.
     */
    private static String  className              = "com.ibm.json.xml.transform.XMLToJSONTransformer";
    private static Logger logger                  = Logger.getLogger(className,null);

    
    /**
     * Method to do the transform from an XML input stream to a JSON stream.
     * Neither input nor output streams are closed.  Closure is left up to the caller.  Same as calling transform(inStream, outStream, false);  (Default is compact form)
     *
     * @param XMLStream The XML stream to convert to JSON
     * @param JSONStream The stream to write out JSON to.  The contents written to this stream are always in UTF-8 format.
     * 
     * @throws SAXException Thrown is a parse error occurs.
     * @throws IOException Thrown if an IO error occurs.
     */
    public static void transform(InputStream XMLStream, OutputStream JSONStream)
    throws SAXException, IOException
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.entering(className, "transform(InputStream, OutputStream)");
        }
        transform(XMLStream,JSONStream,false);    

        if (logger.isLoggable(Level.FINER))
        {
            logger.entering(className, "transform(InputStream, OutputStream)");
        }
    }

    /**
     * Method to do the transform from an XML input stream to a JSON stream.
     * Neither input nor output streams are closed.  Closure is left up to the caller.
     *
     * @param XMLStream The XML stream to convert to JSON
     * @param JSONStream The stream to write out JSON to.  The contents written to this stream are always in UTF-8 format.
     * @param verbose Flag to denote whether or not to render the JSON text in verbose (indented easy to read), or compact (not so easy to read, but smaller), format.
     *
     * @throws SAXException Thrown if a parse error occurs.
     * @throws IOException Thrown if an IO error occurs.
     */
    public static void transform(InputStream XMLStream, OutputStream JSONStream, boolean verbose)
    throws SAXException, IOException
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.entering(className, "transform(InputStream, OutputStream)");
        }

        if (XMLStream == null)
        {
            throw new NullPointerException("XMLStream cannot be null");
        }
        else if (JSONStream == null)
        {
            throw new NullPointerException("JSONStream cannot be null");
        }
        else
        {

            if (logger.isLoggable(Level.FINEST))
            {
                logger.logp(Level.FINEST, className, "transform", "Fetching a SAX parser for use with JSONSAXHandler");
            }

            try
            {
                /**
                 * Get a parser.
                 */
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                SAXParser sParser = factory.newSAXParser();
                XMLReader parser = sParser.getXMLReader();
                JSONSAXHandler jsonHandler = new JSONSAXHandler(JSONStream, verbose);
                parser.setContentHandler(jsonHandler);
                parser.setErrorHandler(jsonHandler);
                InputSource source = new InputSource(new BufferedInputStream(XMLStream));

                if (logger.isLoggable(Level.FINEST))
                {
                    logger.logp(Level.FINEST, className, "transform", "Parsing the XML content to JSON");
                }

                /** 
                 * Parse it.
                 */
                source.setEncoding("UTF-8");
                parser.parse(source);                 
                jsonHandler.flushBuffer();
            }
            catch (javax.xml.parsers.ParserConfigurationException pce)
            {
                throw new SAXException("Could not get a parser: " + pce.toString());
            }
        }

        if (logger.isLoggable(Level.FINER))
        {
            logger.exiting(className, "transform(InputStream, OutputStream)");
        }
    }

    /**
     * Method to take an input stream to an XML document and return a String of the JSON format.  
     * Note that the xmlStream is not closed when read is complete.  This is left up to the caller, who may wish to do more with it.  
     * This is the same as transform(xmlStream,false)
     *
     * @param xmlStream The InputStream to an XML document to transform to JSON.
     * @return A string of the JSON representation of the XML file
     * 
     * @throws SAXException Thrown if an error occurs during parse.
     * @throws IOException Thrown if an IOError occurs.
     */
    public static String transform(InputStream xmlStream)
    throws SAXException, IOException
    {
        return transform(xmlStream,false);
    }


    /**
     * Method to take an input stream to an XML document and return a String of the JSON format.  Note that the xmlStream is not closed when read is complete.  This is left up to the caller, who may wish to do more with it.
     * @param xmlStream The InputStream to an XML document to transform to JSON.
     * @param verbose Boolean flag denoting whther or not to write the JSON in verbose (formatted), or compact form (no whitespace)
     * @return A string of the JSON representation of the XML file
     * 
     * @throws SAXException Thrown if an error occurs during parse.
     * @throws IOException Thrown if an IOError occurs.
     */
    public static String transform(InputStream xmlStream, boolean verbose)
    throws SAXException, IOException
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.exiting(className, "transform(InputStream, boolean)");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String result              = null;

        try
        {
            transform(xmlStream,baos,verbose);
            result = baos.toString("UTF-8");
            baos.close();
        }
        catch (UnsupportedEncodingException uec)
        {
            IOException iox = new IOException(uec.toString());
            iox.initCause(uec);
            throw iox;
        }

        if (logger.isLoggable(Level.FINER))
        {
            logger.exiting(className, "transform(InputStream, boolean)");
        }

        return result;
    }


    /**
     * Method to take an XML file and return a String of the JSON format.  
     * 
     * @param xmlFile The XML file to transform to JSON.
     * @param verbose Boolean flag denoting whther or not to write the JSON in verbose (formatted), or compact form (no whitespace)
     * @return A string of the JSON representation of the XML file
     * 
     * @throws SAXException Thrown if an error occurs during parse.
     * @throws IOException Thrown if an IOError occurs.
     */
    public static String transform(File xmlFile, boolean verbose)
    throws SAXException, IOException
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.exiting(className, "transform(InputStream, boolean)");
        }

        FileInputStream fis        = new FileInputStream(xmlFile);
        String result              = null;

        result = transform(fis,verbose);
        fis.close();

        if (logger.isLoggable(Level.FINER))
        {
            logger.exiting(className, "transform(InputStream, boolean)");
        }

        return result;
    }

    /**
     * Method to take an XML file and return a String of the JSON format.  
     * This is the same as transform(xmlStream,false)
     *
     * @param xmlFile The XML file to convert to JSON.
     * @return A string of the JSON representation of the XML file
     * 
     * @throws SAXException Thrown if an error occurs during parse.
     * @throws IOException Thrown if an IOError occurs.
     */
    public static String transform(File xmlFile)
    throws SAXException, IOException
    {
        return transform(xmlFile,false);
    }
}
