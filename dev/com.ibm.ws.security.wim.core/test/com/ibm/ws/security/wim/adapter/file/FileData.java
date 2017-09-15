/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.file;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.Group;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.PersonAccount;

public class FileData {
    //if this is a constant findbugs complains.
    public String tempFile;

    Document doc = null;

    DocumentBuilderFactory DOMFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = null;

    XPathFactory xPathFactory = XPathFactory.newInstance();

    public FileData() {
        try {
            docBuilder = DOMFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public synchronized void load() throws Exception {
        String data = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
                      "<sdo:datagraph xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
                      "xmlns:sdo=\"commonj.sdo\" xmlns:wim=\"http://www.ibm.com/websphere/wim\">\r\n" +
                      "<wim:Root>\r\n" +
                      "<wim:entities xsi:type=\"wim:PersonAccount\">\r\n" +
                      "<wim:identifier externalId=\"c9e46e40-42bb-4365-b9d7-5228abc222b4\" externalName=\"uid=admin,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"c9e46e40-42bb-4365-b9d7-5228abc222b4\" uniqueName=\"uid=admin,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "<wim:parent>\r\n" +
                      "<wim:identifier uniqueName=\"o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:parent>\r\n" +
                      "<wim:createTimestamp>2012-03-09T09:52:47Z</wim:createTimestamp>\r\n" +
                      "<wim:password>U0hBLTE6eXA5a2JzemFkOHZ0OmQ4KzFHWmhwRUJpN3pVU05MTHhNeXlydS8vTT0NCg==</wim:password>\r\n" +
                      "<wim:uid>admin</wim:uid>\r\n" +
                      "<wim:cn>admin</wim:cn>\r\n" +
                      "<wim:sn>admin</wim:sn>\r\n" +
                      "</wim:entities>\r\n" +
                      "<wim:entities xsi:type=\"wim:PersonAccount\">\r\n" +
                      "<wim:identifier externalId=\"2b147182-5ff7-4306-9e3c-502912f2f9cd\" externalName=\"uid=samples,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"2b147182-5ff7-4306-9e3c-502912f2f9cd\" uniqueName=\"uid=samples,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "<wim:parent>\r\n" +
                      "<wim:identifier uniqueName=\"o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:parent>\r\n" +
                      "<wim:createTimestamp>2012-03-09T09:52:56.656Z</wim:createTimestamp>\r\n" +
                      "<wim:modifyTimestamp>2012-08-01T12:34:06.656+05:30</wim:modifyTimestamp>\r\n" +
                      "<wim:password>U0hBLTE6emViYWtkaWh2MXJoOmNDM2V6dWlCNG02ZWxWS2dRM216VlBsc1VoVT0NCg==</wim:password>\r\n" +
                      "<wim:uid>samples</wim:uid>\r\n" +
                      "<wim:cn>samples</wim:cn>\r\n" +
                      "<wim:sn>samples</wim:sn>\r\n" +
                      "<wim:mail></wim:mail>\r\n" +
                      "</wim:entities>\r\n" +
                      "<wim:entities xsi:type=\"wim:Group\">\r\n" +
                      "<wim:identifier externalId=\"43e0355c-17a7-427d-8fee-a6827a1fa831\" externalName=\"cn=sampadmn,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"43e0355c-17a7-427d-8fee-a6827a1fa831\" uniqueName=\"cn=sampadmn,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "<wim:parent>\r\n" +
                      "<wim:identifier uniqueName=\"o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:parent>\r\n" +
                      "<wim:createTimestamp>2012-03-09T09:52:58.156Z</wim:createTimestamp>\r\n" +
                      "<wim:modifyTimestamp>2012-08-01T12:33:53.531+05:30</wim:modifyTimestamp>\r\n" +
                      "<wim:cn>sampadmn</wim:cn>\r\n" +
                      "<wim:members>\r\n" +
                      "<wim:identifier externalId=\"2b147182-5ff7-4306-9e3c-502912f2f9cd\" externalName=\"uid=samples,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"2b147182-5ff7-4306-9e3c-502912f2f9cd\" uniqueName=\"uid=samples,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:members>\r\n" +
                      "</wim:entities>\r\n" +
                      "<wim:entities xsi:type=\"wim:Group\">\r\n" +
                      "<wim:identifier externalId=\"928da06c-1ed7-41fe-adbd-d188b2e97375\" externalName=\"cn=group1,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"928da06c-1ed7-41fe-adbd-d188b2e97375\" uniqueName=\"cn=group1,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "<wim:parent>\r\n" +
                      "<wim:identifier uniqueName=\"o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:parent>\r\n" +
                      "<wim:createTimestamp>2012-08-01T12:34:37.156+05:30</wim:createTimestamp>\r\n" +
                      "<wim:modifyTimestamp>2012-08-01T13:22:47.156+05:30</wim:modifyTimestamp>\r\n" +
                      "<wim:cn>group1</wim:cn>\r\n" +
                      "<wim:members>\r\n" +
                      "<wim:identifier externalId=\"34b84db1-0ab0-4a05-88b5-c28e409a502b\" externalName=\"cn=nestedGroup1,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"34b84db1-0ab0-4a05-88b5-c28e409a502b\" uniqueName=\"cn=nestedGroup1,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:members>\r\n" +
                      "</wim:entities>\r\n" +
                      "<wim:entities xsi:type=\"wim:Group\">\r\n" +
                      "<wim:identifier externalId=\"34b84db1-0ab0-4a05-88b5-c28e409a502b\" externalName=\"cn=nestedGroup1,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"34b84db1-0ab0-4a05-88b5-c28e409a502b\" uniqueName=\"cn=nestedGroup1,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "<wim:parent>\r\n" +
                      "<wim:identifier uniqueName=\"o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:parent>\r\n" +
                      "<wim:createTimestamp>2012-08-01T12:34:44.812+05:30</wim:createTimestamp>\r\n" +
                      "<wim:modifyTimestamp>2012-08-01T13:23:01.218+05:30</wim:modifyTimestamp>\r\n" +
                      "<wim:cn>nestedGroup1</wim:cn>\r\n" +
                      "<wim:members>\r\n" +
                      "<wim:identifier externalId=\"73660e2a-5eb5-4189-bd3b-d37ae6976c9e\" externalName=\"uid=user1,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"73660e2a-5eb5-4189-bd3b-d37ae6976c9e\" uniqueName=\"uid=user1,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:members>\r\n" +
                      "</wim:entities>\r\n" +
                      "<wim:entities xsi:type=\"wim:PersonAccount\">\r\n" +
                      "<wim:identifier externalId=\"73660e2a-5eb5-4189-bd3b-d37ae6976c9e\" externalName=\"uid=user1,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"73660e2a-5eb5-4189-bd3b-d37ae6976c9e\" uniqueName=\"uid=user1,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "<wim:parent>\r\n" +
                      "<wim:identifier uniqueName=\"o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:parent>\r\n" +
                      "<wim:createTimestamp>2012-08-01T13:22:25.984+05:30</wim:createTimestamp>\r\n" +
                      "<wim:password>U0hBLTE6eXV6Nmp4YzJqNHo5OkYwbURoN3NtckxNSFlwaWZIUHFaZndjNHZxST0NCg==</wim:password>\r\n" +
                      "<wim:uid>user1</wim:uid>\r\n" +
                      "<wim:cn>user1</wim:cn>\r\n" +
                      "<wim:sn>user1</wim:sn>\r\n" +
                      "</wim:entities>\r\n" +
                      "<wim:entities xsi:type=\"wim:PersonAccount\">\r\n" +
                      "<wim:identifier externalId=\"c9e46e40-42bb-4365-b9d7-5228abc222b4\" externalName=\"uid=vmmuser1,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"c9e46e40-42bb-4365-b9d7-5228abc222b4\" uniqueName=\"uid=vmmuser1,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "<wim:parent>\r\n" +
                      "<wim:identifier uniqueName=\"o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:parent>\r\n" +
                      "<wim:createTimestamp>2012-03-09T09:52:47Z</wim:createTimestamp>\r\n" +
                      "<wim:password>U0hBLTE6N2xvcnA1eGd1eWVkOmFDWURQYVFFNWhLNEs1d3pPYk9CMnNENDZ0OD0NCg==</wim:password>\r\n" +
                      "<wim:uid>vmmuser1</wim:uid>\r\n" +
                      "<wim:cn>vmmuser1</wim:cn>\r\n" +
                      "<wim:sn>snvmmuser1</wim:sn>\r\n" +
                      "<wim:displayName>vmmuser1 display</wim:displayName>\r\n" +
                      "</wim:entities>\r\n" +
                      "<wim:entities xsi:type=\"wim:PersonAccount\">\r\n" +
                      "<wim:identifier externalId=\"c9e46e40-42bb-4365-b9d7-5228abc222b4\" externalName=\"uid=vmmuser2,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"c9e46e40-42bb-4365-b9d7-5228abc222b4\" uniqueName=\"uid=vmmuser2,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "<wim:parent>\r\n" +
                      "<wim:identifier uniqueName=\"o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:parent>\r\n" +
                      "<wim:createTimestamp>2012-03-09T09:52:47Z</wim:createTimestamp>\r\n" +
                      "<wim:password>U0hBLTE6N2xvcnA1eGd1eWVkOmFDWURQYVFFNWhLNEs1d3pPYk9CMnNENDZ0OD0NCg==</wim:password>\r\n" +
                      "<wim:uid>vmmuser2</wim:uid>\r\n" +
                      "<wim:cn>vmmuser2</wim:cn>\r\n" +
                      "<wim:sn>vmmuser2</wim:sn>\r\n" +
                      "</wim:entities>\r\n" +
                      "<wim:entities xsi:type=\"wim:PersonAccount\">\r\n" +
                      "<wim:identifier externalId=\"c9e46e40-42bb-4365-b9d7-5228abc222b4\" externalName=\"uid=vmmuser3,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"c9e46e40-42bb-4365-b9d7-5228abc222b4\" uniqueName=\"uid=vmmuser3,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "<wim:parent>\r\n" +
                      "<wim:identifier uniqueName=\"o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:parent>\r\n" +
                      "<wim:createTimestamp>2012-03-09T09:52:47Z</wim:createTimestamp>\r\n" +
                      "<wim:password>U0hBLTE6N2xvcnA1eGd1eWVkOmFDWURQYVFFNWhLNEs1d3pPYk9CMnNENDZ0OD0NCg==</wim:password>\r\n" +
                      "<wim:uid>vmmuser3</wim:uid>\r\n" +
                      "<wim:cn>vmmuser3</wim:cn>\r\n" +
                      "<wim:sn>vmmuser3</wim:sn>\r\n" +
                      "</wim:entities>\r\n" +
                      "<wim:entities xsi:type=\"wim:Group\">\r\n" +
                      "<wim:identifier externalId=\"928da06c-1ed7-41fe-adbd-d188b2e97375\" externalName=\"cn=vmmgroup1,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"928da06c-1ed7-41fe-adbd-d188b2e97375\" uniqueName=\"cn=vmmgroup1,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "<wim:parent>\r\n" +
                      "<wim:identifier uniqueName=\"o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:parent>\r\n" +
                      "<wim:createTimestamp>2012-08-01T12:34:37.156+05:30</wim:createTimestamp>\r\n" +
                      "<wim:modifyTimestamp>2012-08-01T13:22:47.156+05:30</wim:modifyTimestamp>\r\n" +
                      "<wim:cn>vmmgroup1</wim:cn>\r\n" +
                      "<wim:members>\r\n" +
                      "<wim:identifier externalId=\"c9e46e40-42bb-4365-b9d7-5228abc222b4\" externalName=\"uid=vmmuser1,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"c9e46e40-42bb-4365-b9d7-5228abc222b4\" uniqueName=\"uid=vmmuser1,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:members>\r\n" +
                      "</wim:entities>\r\n" +
                      "<wim:entities xsi:type=\"wim:Group\">\r\n" +
                      "<wim:identifier externalId=\"928da06c-1ed7-41fe-adbd-d188b2e97375\" externalName=\"cn=vmmgroup2,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"928da06c-1ed7-41fe-adbd-d188b2e97375\" uniqueName=\"cn=vmmgroup2,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "<wim:parent>\r\n" +
                      "<wim:identifier uniqueName=\"o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:parent>\r\n" +
                      "<wim:createTimestamp>2012-08-01T12:34:37.156+05:30</wim:createTimestamp>\r\n" +
                      "<wim:modifyTimestamp>2012-08-01T13:22:47.156+05:30</wim:modifyTimestamp>\r\n" +
                      "<wim:cn>vmmgroup2</wim:cn>\r\n" +
                      "<wim:members>\r\n" +
                      "<wim:identifier externalId=\"c9e46e40-42bb-4365-b9d7-5228abc222b4\" externalName=\"uid=vmmuser1,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"c9e46e40-42bb-4365-b9d7-5228abc222b4\" uniqueName=\"uid=vmmuser1,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "<wim:identifier externalId=\"c9e46e40-42bb-4365-b9d7-5228abc222b4\" externalName=\"uid=vmmuser2,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"c9e46e40-42bb-4365-b9d7-5228abc222b4\" uniqueName=\"uid=vmmuser2,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:members>\r\n" +
                      "</wim:entities>\r\n" +
                      "<wim:entities xsi:type=\"wim:Group\">\r\n" +
                      "<wim:identifier externalId=\"928da06c-1ed7-41fe-adbd-d188b2e97375\" externalName=\"cn=vmmgroup3,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"928da06c-1ed7-41fe-adbd-d188b2e97375\" uniqueName=\"cn=vmmgroup3,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "<wim:parent>\r\n" +
                      "<wim:identifier uniqueName=\"o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:parent>\r\n" +
                      "<wim:createTimestamp>2012-08-01T12:34:37.156+05:30</wim:createTimestamp>\r\n" +
                      "<wim:modifyTimestamp>2012-08-01T13:22:47.156+05:30</wim:modifyTimestamp>\r\n" +
                      "<wim:cn>vmmgroup3</wim:cn>\r\n" +
                      "<wim:members>\r\n" +
                      "<wim:identifier externalId=\"c9e46e40-42bb-4365-b9d7-5228abc222b4\" externalName=\"uid=vmmuser3,o=defaultWIMFileBasedRealm\"\r\n" +
                      "uniqueId=\"c9e46e40-42bb-4365-b9d7-5228abc222b4\" uniqueName=\"uid=vmmuser3,o=defaultWIMFileBasedRealm\"/>\r\n" +
                      "</wim:members>\r\n" +
                      "</wim:entities>\r\n" +
                      "</wim:Root>\r\n" +
                      "</sdo:datagraph>\r\n";

        InputSource in = new InputSource(new StringReader(data));
        doc = docBuilder.parse(in);
    }

    public synchronized void saveEntities() throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        tempFile = "/temp/file.xml";
        StreamResult result = new StreamResult(new File(tempFile));
        transformer.transform(source, result);
    }

    public Entity get(String entityType, String searchStr) throws Exception {
        if (doc == null)
            load();

        XPath xpath = xPathFactory.newXPath();
        xpath.setNamespaceContext(new WIMNamespaceContext());
        String xpathString = "//*[@type='wim:" + entityType + "' and " + searchStr + "]";
        // System.out.println(xpathString);
        XPathExpression expr = xpath.compile(xpathString);

        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        // System.out.println("Found = " + nodes.getLength());
        Entity entity = null;
        for (int i = 0; i < nodes.getLength(); i++) {
            // System.out.println(nodes.item(i).getChildNodes().item(11).getFirstChild().getNodeValue());
            entity = convertToDataObject(entityType, nodes.item(i));
        }

        return entity;
    }

    private Entity convertToDataObject(String entityType, Node item) {
        Entity entity = null;
        if (entityType.equalsIgnoreCase("PersonAccount")) {
            entity = new PersonAccount();
            NodeList properties = item.getChildNodes();
            for (int i = 0; i < properties.getLength(); i++) {
                String propName = properties.item(i).getNodeName();
                if (propName.startsWith("wim:"))
                    propName = propName.substring(4);
                String dataType = entity.getDataType(propName);
                if (dataType != null && properties.item(i).getFirstChild() != null
                    && !("parent".equalsIgnoreCase(propName))
                    && !("createTimestamp".equalsIgnoreCase(propName))
                    && !("modifyTimestamp".equalsIgnoreCase(propName))
                    && !("password".equalsIgnoreCase(propName))) {
                    entity.set(propName, properties.item(i).getFirstChild().getNodeValue());
                }
                if (propName.equalsIgnoreCase("identifier")) {
                    IdentifierType id = new IdentifierType();
                    Node idNode = properties.item(i);
                    NamedNodeMap attr = idNode.getAttributes();
                    id.setUniqueName(attr.getNamedItem("uniqueName").getNodeValue());
                    id.setUniqueId(attr.getNamedItem("uniqueId").getNodeValue());
                    id.setExternalName(attr.getNamedItem("externalName").getNodeValue());
                    id.setExternalId(attr.getNamedItem("externalId").getNodeValue());
                    entity.setIdentifier(id);
                }
            }
        } else if (entityType.equalsIgnoreCase("Group")) {
            entity = new Group();
            NodeList properties = item.getChildNodes();
            for (int i = 0; i < properties.getLength(); i++) {
                String propName = properties.item(i).getNodeName();
                if (propName.startsWith("wim:"))
                    propName = propName.substring(4);
                String dataType = entity.getDataType(propName);
                if (dataType != null && !dataType.equalsIgnoreCase("Entity")
                    && properties.item(i).getFirstChild() != null
                    && !("parent".equalsIgnoreCase(propName))
                    && !("createTimestamp".equalsIgnoreCase(propName))
                    && !("modifyTimestamp".equalsIgnoreCase(propName)))
                    entity.set(propName, properties.item(i).getFirstChild().getNodeValue());
                if (propName.equalsIgnoreCase("identifier")) {
                    IdentifierType id = new IdentifierType();
                    Node idNode = properties.item(i);
                    NamedNodeMap attr = idNode.getAttributes();
                    id.setUniqueName(attr.getNamedItem("uniqueName").getNodeValue());
                    id.setUniqueId(attr.getNamedItem("uniqueId").getNodeValue());
                    id.setExternalName(attr.getNamedItem("externalName").getNodeValue());
                    id.setExternalId(attr.getNamedItem("externalId").getNodeValue());
                    entity.setIdentifier(id);
                }
                if (propName.equalsIgnoreCase("members")) {
                    NodeList memberIds = properties.item(i).getChildNodes();
                    for (int mem = 0; mem < memberIds.getLength(); mem++) {
                        Node idNode = memberIds.item(mem);
                        if (idNode.getNodeName().equalsIgnoreCase("wim:identifier")) {
                            Entity member = new Entity();
                            IdentifierType id = new IdentifierType();
                            member.setIdentifier(id);
                            NamedNodeMap attr = idNode.getAttributes();
                            id.setUniqueName(attr.getNamedItem("uniqueName").getNodeValue());
                            id.setUniqueId(attr.getNamedItem("uniqueId").getNodeValue());
                            id.setExternalName(attr.getNamedItem("externalName").getNodeValue());
                            id.setExternalId(attr.getNamedItem("externalId").getNodeValue());
                            ((Group) entity).getMembers().add(member);
                        }
                    }
                }
            }
        }

        return entity;
    }

    public List<Entity> search(String entityType, String searchStr, boolean searchAll, boolean returnSubType) throws Exception {
        if (doc == null)
            load();

        XPath xpath = xPathFactory.newXPath();
        xpath.setNamespaceContext(new WIMNamespaceContext());
        if (returnSubType) {
            // append entity types
        }
        String xpathString = "//*[@type='wim:" + entityType + "' and " + searchStr + "]";
        // System.out.println(xpathString);
        XPathExpression expr = xpath.compile(xpathString);

        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        // System.out.println("Found = " + nodes.getLength());
        ArrayList<Entity> results = new ArrayList<Entity>();

        for (int i = 0; i < nodes.getLength(); i++) {
            // System.out.println(nodes.item(i).getChildNodes().item(11).getFirstChild().getNodeValue());
            Entity entity = convertToDataObject(entityType, nodes.item(i));
            results.add(entity);

            if (!searchAll)
                return results;
        }

        return results;
    }

    public static void main(String args[]) throws Exception {
        FileData fileData = new FileData();
        fileData.search("Group", "members/identifier/@uniqueName='uid=user1,o=defaultWIMFileBasedRealm'", false, true);
//    	fileData.get("Group", "cn='group1'");
//    	fileData.search("PersonAccount", "cn='a*'", false, true);
//    	fileData.saveEntities();
    }
}
