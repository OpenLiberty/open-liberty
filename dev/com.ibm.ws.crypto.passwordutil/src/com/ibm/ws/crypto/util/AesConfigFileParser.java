/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.crypto.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.wsspi.security.crypto.PasswordEncryptException;

/**
 * A class for parsing a secure key file (xml or property file format) and retrieving values
 * for wlp.aes.encryption.key and wlp.password.encryption.key
 * This is used by the command line tasks to parse xml in order to encode passwords.
 */
public class AesConfigFileParser {
    private static final String ATTR_VALUE = "value";
    private static final String ATTR_NAME = "name";
    private static final String TAG_VARIABLE = "variable";

    /**
     *
     * @param aesConfigFilePath the path of the XML or property file to be parsed.
     * @return a Map containing keysPasswordUtil.PROPERTY_AES_KEY or PasswordUtil.PROPERTY_CRYPTO_KEY
     *         if they are found within the parsed file.
     * @throws PasswordEncryptException            if parsing the file fails for both XML and Properties parsers.
     *
     * @throws UnsupportedConfigurationException   if the file contains both AESKeyManager.NAME_WLP_BASE64_AES_ENCRYPTION_KEY and
     *                                                 AESKeyManager.NAME_WLP_BASE64_AES_ENCRYPTION_KEY
     * @throws UnsupportedCryptoAlgorithmException if the file does not contain either AESKeyManager.NAME_WLP_BASE64_AES_ENCRYPTION_KEY or
     *                                                 AESKeyManager.NAME_WLP_BASE64_AES_ENCRYPTION_KEY
     */
    public static Map<String, String> parseAesEncryptionFile(String aesConfigFilePath) throws PasswordEncryptException, UnsupportedConfigurationException, UnsupportedCryptoAlgorithmException {
        Map<String, String> props = new HashMap<>();

        String base64Key = null;
        String passKey = null;

        try {
            Map<String, String> encryptionVars = extractVariables(aesConfigFilePath);
            base64Key = encryptionVars.get(AESKeyManager.NAME_WLP_BASE64_AES_ENCRYPTION_KEY);
            passKey = encryptionVars.get(AESKeyManager.NAME_WLP_PASSWORD_ENCRYPTION_KEY);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new PasswordEncryptException(e);
        }

        if ((base64Key == null && passKey == null)) {
            throw new UnsupportedCryptoAlgorithmException();
        } else if (base64Key != null && passKey != null) {
            throw new UnsupportedConfigurationException();
        }

        if (base64Key != null) {
            props.put(PasswordUtil.PROPERTY_AES_KEY, base64Key);
        } else {
            props.put(PasswordUtil.PROPERTY_CRYPTO_KEY, passKey);
        }
        return props;

    }

    /**
     *
     * @param aesConfigFilePath the xml/property file's path
     * @return a map containing all 'variables' defined in the file specified in aesConfigFilePath
     * @throws FileNotFoundException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    private static Map<String, String> extractVariables(String aesConfigFilePath) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        Map<String, String> variables;
        if (isXmlFile(aesConfigFilePath)) {
            variables = getXmlVariables(aesConfigFilePath);
        } else {
            variables = getPropertyVariables(aesConfigFilePath);
        }
        return variables;

    }

    /**
     * @param aesConfigFilePath a path to a java property file
     * @return a Map containing all variables in the property file
     * @throws IOException
     */
    private static Map<String, String> getPropertyVariables(String aesConfigFilePath) throws IOException {
        Map<String, String> variables = new HashMap<>();
        Properties props = new Properties();
        try (FileInputStream fs = new FileInputStream(aesConfigFilePath)) {
            props.load(fs);
            for (String name : props.stringPropertyNames()) {
                variables.put(name, props.getProperty(name));
            }
        }
        return variables;
    }

    /**
     * @param aesConfigFilePath a path to an XML file
     * @return A map of all 'variables' in the xml file pointed to by aesConfigFilePath
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private static Map<String, String> getXmlVariables(String aesConfigFilePath) throws SAXException, IOException, ParserConfigurationException {
        Map<String, String> variables = new HashMap<>();
        DocumentBuilder docBuilder = getQuietDocBuilder();
        Document doc = docBuilder.parse(new File(aesConfigFilePath));
        Element element = doc.getDocumentElement();

        NodeList varList = element.getElementsByTagName(TAG_VARIABLE);
        for (int j = 0; j < varList.getLength(); j++) {
            Node vl = varList.item(j);
            if (vl.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element vlElement = (Element) vl;
            String varName = vlElement.getAttribute(ATTR_NAME);
            String varVal;
            if (vlElement.getAttribute(ATTR_VALUE).isEmpty()) {
                varVal = null;
            } else {
                varVal = vlElement.getAttribute(ATTR_VALUE);
            }
            variables.put(varName, varVal);
        }
        return variables;
    }

    /**
     * According to linting rule java:S2755 we must disable XXE processing. Also, the java
     * DocumentBuilder writes to stderr /stdout by default. This method returns a document builder
     * that doesn't write to console and disables xxe.
     *
     * @return a java {@link DocumentBuilder}
     * @throws ParserConfigurationException
     */
    private static DocumentBuilder getQuietDocBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        // Disable access to external entities in XML
        dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbFactory.setXIncludeAware(false);
        dbFactory.setExpandEntityReferences(false);
        DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
        docBuilder.setErrorHandler(new ErrorHandler() {

            @Override
            public void warning(SAXParseException exception) throws SAXException {
                //intentionally empty, do nothing for warnings
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                throw exception;
            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                //intentionally empty, do nothing for recoverable errors
            }
        });
        return docBuilder;
    }

    /**
     * @param aesConfigFilePath the path to the secure key file.
     * @return true if the file found in path aesConfigFilePath is in XML format.
     * @throws FileNotFoundException        if the file did not exist
     * @throws ParserConfigurationException if the parser configuration fails
     */
    private static boolean isXmlFile(String aesConfigFilePath) throws FileNotFoundException, ParserConfigurationException {
        boolean isXML;
        try {
            DocumentBuilder docBuilder = getQuietDocBuilder();
            docBuilder.parse(new File(aesConfigFilePath));
            isXML = true;
        } catch (FileNotFoundException fnfe) {
            throw fnfe;
        } catch (IOException | SAXException e) {
            isXML = false;
        } catch (ParserConfigurationException e) {
            throw e;
        }
        return isXML;
    }
}
