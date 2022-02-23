/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.image.util;

import java.io.FileReader;
import java.io.Reader;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;

public class XMLUtils {
    public static final String CLASS_NAME = XMLUtils.class.getSimpleName();

    public static void log(String message) {
        System.out.println(message);
    }

    //
    
    public static Validator getSchemaValidator(String schemaPath) throws Exception {
        SchemaFactory sfactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema;
        try ( Reader schemaReader = new FileReader(schemaPath) ) {
            Source schemaSource = new StreamSource(schemaReader);
            schema = sfactory.newSchema(schemaSource);
        }
        return schema.newValidator();
    }
    
    public static Document parse(String configPath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        DocumentBuilder parser = factory.newDocumentBuilder();
        return parser.parse(configPath);
    }    
    
    //
    
    public static void validate(String configPath, String schemaPath) throws Exception {
        Document configDoc = XMLUtils.parse(configPath); 

        Validator validator = XMLUtils.getSchemaValidator(schemaPath);
        validator.validate( new DOMSource(configDoc) );
    }
}