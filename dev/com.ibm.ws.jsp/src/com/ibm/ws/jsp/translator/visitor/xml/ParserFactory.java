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

package com.ibm.ws.jsp.translator.visitor.xml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;

/**
 * The purpose of this class is to force the usage of the Parser provided by the Runtime and not
 * any one that an application may provide.
 */
public class ParserFactory {
    private static DocumentBuilderFactory documentBuilderFactory = null;
    private static DocumentBuilder documentBuilder = null;
    private static SAXParserFactory saxParserFactory = null;
    
	public static synchronized SAXParser newSAXParser(boolean setNamespaceAware, boolean setValidating) throws ParserConfigurationException, SAXException {
		ClassLoader oldLoader = ThreadContextHelper.getContextClassLoader();
        ThreadContextHelper.setClassLoader(ParserFactory.class.getClassLoader());
		try {
            if (saxParserFactory != null) {
                if (saxParserFactory.isNamespaceAware() == setNamespaceAware &&
                    saxParserFactory.isValidating() == setValidating) {
                    return saxParserFactory.newSAXParser();                    
                }
            }
            saxParserFactory =  SAXParserFactory.newInstance();
            saxParserFactory.setValidating(setValidating);
            saxParserFactory.setNamespaceAware(setNamespaceAware);
            return saxParserFactory.newSAXParser();                    
		}
        finally {
            ThreadContextHelper.setClassLoader(oldLoader);
		}
	}
	
	public static synchronized Document newDocument(boolean setNamespaceAware, boolean setValidating) throws ParserConfigurationException{
		ClassLoader oldLoader = ThreadContextHelper.getContextClassLoader();
        ThreadContextHelper.setClassLoader(ParserFactory.class.getClassLoader());
		try {
            if (documentBuilderFactory != null) {
                if (documentBuilderFactory.isNamespaceAware() == setNamespaceAware &&
                    documentBuilderFactory.isValidating() == setValidating) {
                    return documentBuilder.newDocument();                    
                }
            }
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(setNamespaceAware);
            documentBuilderFactory.setValidating(setValidating);
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.newDocument();                    
		} 
        finally {
            ThreadContextHelper.setClassLoader(oldLoader);
		}
	}
    
	public static void parseDocument(SAXParser parser, InputSource is, DefaultHandler dh) throws IOException, SAXException {
		ClassLoader oldLoader = ThreadContextHelper.getContextClassLoader();
        ThreadContextHelper.setClassLoader(ParserFactory.class.getClassLoader());
		
		try {
			parser.parse(is, dh);
		}
		finally {
            ThreadContextHelper.setClassLoader(oldLoader);
		}
	}

	public static void parseDocument(SAXParser parser, InputStream is, DefaultHandler dh) throws IOException, SAXException {
		ClassLoader oldLoader = ThreadContextHelper.getContextClassLoader();
        ThreadContextHelper.setClassLoader(ParserFactory.class.getClassLoader());
		try {
			parser.parse(is, dh);
		}
		finally {
            ThreadContextHelper.setClassLoader(oldLoader);
		}
	}

    
    
}
