/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package suite.r80.base.jca16.ann;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class AnnUtils {
    @SuppressWarnings("resource")
    // the scanner is closed, the tooling thinks that it isn't because of the useDelimiter call...
    //TODO Parsing the entire trace is an inefficient way to find this metadata, and fixing this will improve test performance.
    public String getMetatype(LibertyServer server, String rar) throws Exception {

        String rarName = "factoryPid=\"com.ibm.ws.jca.resourceAdapter.properties."
                         + rar;
        String traceFile = server.getLogsRoot() + "trace.log";
        File file = new File(traceFile);
        StringBuffer trace_log = new StringBuffer();
        Scanner s = null;
        try {
            s = new Scanner(file).useDelimiter("\n");
            boolean capture = false;
            while (s.hasNext()) {
                String line = s.next();
                if (line.contains("<metatype"))
                    capture = true;
                if (capture) {
                    trace_log.append(line);
                }
                if (line.contains(":MetaData>")) {

                    String metatype = trace_log.toString().trim();
                    if (metatype.contains(rarName + "\"")) {
                        System.out.println("**************** METATYPE is: *****************" + metatype);
                        return metatype;
                    }
                    trace_log = new StringBuffer();
                    capture = false;
                }
            }
        } finally {
            if (s != null) {
                s.close();
                s = null;
            }
        }

        return null;
    }

    public Boolean getMessageListener(String metatype, String rarDisplayName,
                                      String MLType, String ASClass) throws Exception {
        if (metatype == null)
            return false;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

        String pid = "com.ibm.ws.jca.activationSpec.properties."
                     + rarDisplayName + "." + MLType;
        String expression = "//OCD[@id='" + pid + "']/AD";
        System.out.println("expression to compile is " + expression);
        XPathExpression expr = xpath.compile(expression);

        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() == 0) {
            System.out.println("Message Listener with MLType " + MLType
                               + " was not found");
            return false;
        }
        for (int i = 0; i < nodes.getLength(); i++) {

            if (nodes.item(i).hasAttributes()) {
                NamedNodeMap nm = nodes.item(i).getAttributes();
                if (((nm.getNamedItem("id").getNodeValue().toString()).equals("activationspec-class"))
                    && ((nm.getNamedItem("default").getNodeValue().toString()).equals(ASClass))) {
                    return true;
                }

            }

        }

        return false;
    }

    public int getMessageListeners(String metatype, String ASClass) throws Exception {
        if (metatype == null)
            return 0;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

        String expression = "//OCD/AD[@default='" + ASClass + "']";

        XPathExpression expr = xpath.compile(expression);

        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;

        return nodes.getLength();

    }

    /**
     * @param rarDisplayName
     * @param adminObjectClass
     * @param string
     * @return
     */
    public Boolean getAdministeredObject(String metatype,
                                         String rarDisplayName, String adminObjectClass,
                                         String adminObjectType) throws Exception {
        if (metatype == null)
            return false;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

        String pid = "com.ibm.ws.jca.adminObject.properties." + rarDisplayName
                     + "." + adminObjectType + "-" + adminObjectClass;
        String expression = "//OCD[@id='" + pid + "']/AD";
        System.out.println("expression to compile is " + expression);
        XPathExpression expr = xpath.compile(expression);

        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() == 0) {
            System.out.println("AdminObject with Type " + adminObjectType
                               + " and class " + adminObjectClass + " was not found");
            return false;
        }
        for (int i = 0; i < nodes.getLength(); i++) {

            if (nodes.item(i).hasAttributes()) {
                NamedNodeMap nm = nodes.item(i).getAttributes();
                if (((nm.getNamedItem("id").getNodeValue().toString()).equals("adminobject-class"))
                    && ((nm.getNamedItem("default").getNodeValue().toString()).equals(adminObjectClass))) {
                    return true;
                }

            }

        }

        return false;
    }

    /**
     * @param rarDisplayName
     * @param adminObjectClass
     * @return
     */
    public int getAdministeredObjects(String metatype, String adminObjectClass) throws Exception {
        if (metatype == null)
            return 0;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

        String expression = "//OCD/AD[@default='" + adminObjectClass + "']";

        XPathExpression expr = xpath.compile(expression);

        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;

        return nodes.getLength();
    }

    /**
     * @param activationSpec
     * @param metatype
     * @return
     */
    public Boolean getConfigPropertyFromML(String metatype,
                                           String rarDisplayName, String MLClass, String ASClass,
                                           String Property) throws Exception {
        if (metatype == null)
            return false;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

        String pid = "com.ibm.ws.jca.activationSpec.properties."
                     + rarDisplayName + "." + MLClass;
        String expression = "//OCD[@id='" + pid + "']/AD";
        System.out.println("expression to compile is " + expression);
        XPathExpression expr = xpath.compile(expression);

        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() == 0) {
            System.out.println("Message Listener with MLClass " + MLClass
                               + " was not found");
            return false;
        }
        for (int i = 0; i < nodes.getLength(); i++) {

            if (nodes.item(i).hasAttributes()) {
                NamedNodeMap nm = nodes.item(i).getAttributes();
                if ((nm.getNamedItem("id").getNodeValue().toString()).equals(Property)) {
                    System.out.println("Config Property " + Property
                                       + " was found");
                    return true;
                }

            }

        }
        System.out.println("Config Property " + Property + " was not found");
        return false;
    }

    public String getConfigPropertyAttributeFromML(String metatype,
                                                   String rarDisplayName, String MLClass, String ASClass,
                                                   String Property, String attribute) throws Exception {
        if (metatype == null)
            return null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

        String pid = "com.ibm.ws.jca.activationSpec.properties."
                     + rarDisplayName + "." + MLClass;
        String expression = "//OCD[@id='" + pid + "']/AD";
        System.out.println("expression to compile is " + expression);
        XPathExpression expr = xpath.compile(expression);

        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() == 0) {
            return null;
        }
        for (int i = 0; i < nodes.getLength(); i++) {

            if (nodes.item(i).hasAttributes()) {
                NamedNodeMap nm = nodes.item(i).getAttributes();
                if ((nm.getNamedItem("id").getNodeValue().toString()).equals(Property)) {
                    System.out.println("Attribute " + attribute + " for Config Property " + Property
                                       + " was found");
                    if (nm.getNamedItem(attribute) != null)
                        return nm.getNamedItem(attribute).getNodeValue();
                    else
                        return null;
                }

            }

        }
        System.out.println("Attribute " + attribute + " for Config Property " + Property + " was not found");
        return null;
    }

    public Boolean getConfigPropertyFromRA(String metatype,
                                           String rarDisplayName, String Property, String Value) throws Exception {
        if (metatype == null)
            return false;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

        String pid = "com.ibm.ws.jca.resourceAdapter.properties."
                     + rarDisplayName;
        String expression = "//OCD[@id='" + pid + "']/AD";
        System.out.println("expression to compile is " + expression);
        XPathExpression expr = xpath.compile(expression);

        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() == 0) {
            System.out.println("Resource Adapter with rarDisplayName "
                               + rarDisplayName + " was not found.");
            return false;
        }
        for (int i = 0; i < nodes.getLength(); i++) {

            if (nodes.item(i).hasAttributes()) {
                NamedNodeMap nm = nodes.item(i).getAttributes();
                if (((nm.getNamedItem("id").getNodeValue().toString()).equals(Property))
                    && (nm.getNamedItem("default") != null) && ((nm.getNamedItem("default").getNodeValue().toString()).equals(Value))) {
                    System.out.println("Config Property " + Property
                                       + " was found");
                    return true;
                }

            }

        }
        System.out.println("Config Property " + Property + " was not found");
        return false;
    }

    public Boolean getConfigPropertyFromRA(String metatype,
                                           String rarDisplayName, String Property) throws Exception {
        if (metatype == null)
            return false;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

        String pid = "com.ibm.ws.jca.resourceAdapter.properties."
                     + rarDisplayName;
        String expression = "//OCD[@id='" + pid + "']/AD";
        System.out.println("expression to compile is " + expression);
        XPathExpression expr = xpath.compile(expression);

        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() == 0) {
            System.out.println("Resource Adapter with rarDisplayName "
                               + rarDisplayName + " was not found.");
            return false;
        }
        for (int i = 0; i < nodes.getLength(); i++) {

            if (nodes.item(i).hasAttributes()) {
                NamedNodeMap nm = nodes.item(i).getAttributes();
                if ((nm.getNamedItem("id").getNodeValue().toString()).equals(Property)) {
                    System.out.println("Config Property " + Property
                                       + " was found");
                    return true;
                }

            }

        }
        System.out.println("Config Property " + Property + " was not found");
        return false;
    }

    public String getConfigPropertyAttributeFromRA(String metatype,
                                                   String rarDisplayName, String Property, String attributeName) throws Exception {
        if (metatype == null)
            return null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

        String pid = "com.ibm.ws.jca.resourceAdapter.properties."
                     + rarDisplayName;
        String expression = "//OCD[@id='" + pid + "']/AD";
        System.out.println("expression to compile is " + expression);
        XPathExpression expr = xpath.compile(expression);

        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() == 0) {
            return null;
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).hasAttributes()) {
                NamedNodeMap nm = nodes.item(i).getAttributes();
                if (((nm.getNamedItem("id").getNodeValue().toString()).equals(Property))) {
                    System.out.println("Attribute " + attributeName + " of Config Property " + Property
                                       + " was found");
                    if (nm.getNamedItem(attributeName) != null)
                        return nm.getNamedItem(attributeName).getNodeValue();
                    else
                        return null;
                }
            }
        }
        System.out.println("Attribute " + attributeName + " of Config Property " + Property + " was not found");
        return null;
    }

    public Boolean getConfigPropertyFromAO(String metatype,
                                           String rarDisplayName, String AOClass, String AOInterface,
                                           String Property) throws Exception {
        if (metatype == null)
            return false;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));
        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();
        String pid = "com.ibm.ws.jca.adminObject.properties." + rarDisplayName
                     + "." + AOInterface + '-' + AOClass;
        String expression = "//OCD[@id='" + pid + "']/AD";
        System.out.println("expression to compile is " + expression);
        XPathExpression expr = xpath.compile(expression);
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() == 0) {
            System.out.println("AdminObject with Type " + AOInterface
                               + " and class " + AOClass + " was not found");
            return false;
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).hasAttributes()) {
                NamedNodeMap nm = nodes.item(i).getAttributes();
                if ((nm.getNamedItem("id").getNodeValue().toString()).equals(Property)) {
                    System.out.println("Config Property " + Property
                                       + " was found");
                    return true;
                }
            }
        }
        System.out.println("Config Property " + Property + " was not found");
        return false;
    }

    public String getConfigPropertyAttributeFromAO(String metatype,
                                                   String rarDisplayName, String AOClass, String AOInterface,
                                                   String Property, String attribute) throws Exception {
        if (metatype == null)
            return null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));
        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();
        String pid = "com.ibm.ws.jca.adminObject.properties." + rarDisplayName
                     + "." + AOInterface + '-' + AOClass;
        String expression = "//OCD[@id='" + pid + "']/AD";
        System.out.println("expression to compile is " + expression);
        XPathExpression expr = xpath.compile(expression);

        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() == 0) {
            return null;
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).hasAttributes()) {
                NamedNodeMap nm = nodes.item(i).getAttributes();
                if ((nm.getNamedItem("id").getNodeValue().toString()).equals(Property)) {
                    System.out.println("Attribute " + attribute + " of Config Property " + Property
                                       + " was found");
                    if (nm.getNamedItem(attribute) != null)
                        return nm.getNamedItem(attribute).getNodeValue();
                    else
                        return null;
                }
            }
        }
        System.out.println("Attribute " + attribute + " of Config Property " + Property + " was not found");
        return null;
    }

    /**
     * @param metatype
     * @param rarDisplayName
     * @param mCF_CLASS
     * @param property
     * @param value
     * @return
     */
    public boolean getConfigPropertyFromCD(String metatype,
                                           String rarDisplayName, String CF_CLASS_1, String Property) throws Exception {
        if (metatype == null)
            return false;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));
        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();
        String pid = "com.ibm.ws.jca.connectionFactory.properties."
                     + rarDisplayName + "." + CF_CLASS_1;
        String expression = "//OCD[@id='" + pid + "']/AD";
        System.out.println("expression to compile is " + expression);
        XPathExpression expr = xpath.compile(expression);
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() == 0) {
            System.out.println("connectionFactory with MCF_CLASS " + CF_CLASS_1
                               + " was not found.");
            return false;
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).hasAttributes()) {
                NamedNodeMap nm = nodes.item(i).getAttributes();
                if ((nm.getNamedItem("id").getNodeValue().toString()).equals(Property)) {
                    System.out.println("Config Property " + Property
                                       + " was found");
                    return true;
                }
            }
        }
        System.out.println("Config Property " + Property + " was not found");
        return false;
    }

    public String getConfigPropertyAttributeFromCD(String metatype,
                                                   String rarDisplayName, String CF_CLASS_1, String Property, String attribute) throws Exception {
        if (metatype == null)
            return null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));
        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();
        String pid = "com.ibm.ws.jca.connectionFactory.properties."
                     + rarDisplayName + "." + CF_CLASS_1;
        String expression = "//OCD[@id='" + pid + "']/AD";
        System.out.println("expression to compile is " + expression);
        XPathExpression expr = xpath.compile(expression);
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() == 0) {
            return null;
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).hasAttributes()) {
                NamedNodeMap nm = nodes.item(i).getAttributes();
                if ((nm.getNamedItem("id").getNodeValue().toString()).equals(Property)) {
                    System.out.println("Attribute " + attribute + " for Config Property " + Property
                                       + " was found");
                    if (nm.getNamedItem(attribute) != null)
                        return nm.getNamedItem(attribute).getNodeValue();
                    else
                        return null;
                }
            }
        }
        System.out.println("Attribute " + attribute + " for Config Property " + Property + " was not found");
        return null;
    }

    public Boolean getConnector(String metatype,
                                String rarDisplayName, String RAClass) throws Exception {
        if (metatype == null)
            return false;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));
        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();
        String pid = "com.ibm.ws.jca.resourceAdapter.properties." + rarDisplayName;
        String expression = "//OCD[@id='" + pid + "']/AD";
        System.out.println("expression to compile is " + expression);
        XPathExpression expr = xpath.compile(expression);
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() == 0) {
            System.out.println("Resource Adapter with name : " + rarDisplayName
                               + " was not found");
            return false;
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).hasAttributes()) {
                NamedNodeMap nm = nodes.item(i).getAttributes();
                if (((nm.getNamedItem("id").getNodeValue().toString()).equals("resourceadapter-class"))
                    && ((nm.getNamedItem("default").getNodeValue().toString()).equals(RAClass))) {
                    return true;
                }
            }
        }
        return false;
    }

    public Boolean getReqCtx(String metatype, String rarDisplayName, String RequiredWorkContext) throws Exception {
        if (metatype == null)
            return false;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));
        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();
        String pid = "com.ibm.ws.jca.resourceAdapter.properties." + rarDisplayName;
        String expression = "//OCD[@id='" + pid + "']/AD";
        System.out.println("expression to compile is " + expression);
        XPathExpression expr = xpath.compile(expression);
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).hasAttributes()) {
                NamedNodeMap nm = nodes.item(i).getAttributes();
                if (((nm.getNamedItem("id").getNodeValue().toString()).contains("requiredContextProvider.target"))
                    && ((nm.getNamedItem("default").getNodeValue().toString()).contains(RequiredWorkContext))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param rarDisplayName
     * @return
     */
    public int getConnectionDefinitions(String metatype, String connDef) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

        String expression = "//OCD/AD[@default='" + connDef + "']";

        XPathExpression expr = xpath.compile(expression);

        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;

        return nodes.getLength();
    }

    /**
     * @param metatype
     * @return
     */
    public boolean getMCF(String metatype, String rarDisplayName, String name) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

        String pid = "com.ibm.ws.jca.connectionFactory.properties." + rarDisplayName
                     + "." + "javax.resource.cci.ConnectionFactory";
        String expression = "//OCD[@id='" + pid + "']/AD";
        System.out.println("expression to compile is " + expression);
        XPathExpression expr = xpath.compile(expression);

        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() == 0) {
            System.out.println("Connection Factory definition was not found");
            return false;
        }
        for (int i = 0; i < nodes.getLength(); i++) {

            if (nodes.item(i).hasAttributes()) {
                NamedNodeMap nm = nodes.item(i).getAttributes();
                if (((nm.getNamedItem("id").getNodeValue().toString()).equals("managedconnectionfactory-class"))
                    && ((nm.getNamedItem("default").getNodeValue().toString()).equals(name))) {
                    return true;
                }

            }

        }

        return false;

    }

    public String getAttributeValueFromRA(String metatype,
                                          String rarDisplayName, String attribute) throws Exception {
        if (metatype == null)
            return null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(metatype.getBytes("utf-8"))));

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

        String pid = "com.ibm.ws.jca.resourceAdapter.properties."
                     + rarDisplayName;
        String expression = "//OCD[@id='" + pid + "']";
        System.out.println("expression to compile is " + expression);
        XPathExpression expr = xpath.compile(expression);

        Object result = expr.evaluate(doc, XPathConstants.NODE);
        Node node = (Node) result;
        if (node == null) {
            System.out.println("Resource Adapter with rarDisplayName "
                               + rarDisplayName + " was not found.");
            return null;
        }
        if (node.hasAttributes()) {
            NamedNodeMap nm = node.getAttributes();
            return nm.getNamedItem(attribute).getNodeValue().toString();

        }
        System.out.println("Attribute " + attribute + " was not found on resource adapter.");
        return null;
    }

}