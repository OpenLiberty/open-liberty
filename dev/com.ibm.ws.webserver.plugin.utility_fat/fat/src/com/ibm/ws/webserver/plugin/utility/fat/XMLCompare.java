/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.webserver.plugin.utility.fat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLCompare {
    private static XMLCompare instance = null;
    private DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    public static XMLCompare getInstance() {
        if(instance == null ) {
            instance = new XMLCompare();
        }
        return instance;
    }

    /*
     * This is an xml compare that takes knowledge of the plugin cfg format to identify all endpoints and compare between expected and test documents
     */
    public List<XMLIssue>compareEndpoints(File expectedFile, File testFile ) throws ParserConfigurationException, SAXException, IOException {
        List<XMLIssue> issues = new ArrayList<XMLIssue>();

        DocumentBuilder db = dbf.newDocumentBuilder();

        // consume expected
        Document expectedDoc = db.parse(expectedFile);
        Element expectedTopElement = expectedDoc.getDocumentElement();
        XMLNode eNode = new XMLNode(expectedDoc, (Node)expectedTopElement, null);
        
        // consume test
        Document testDoc = db.parse(testFile);
        Element testTopElement = testDoc.getDocumentElement();
        XMLNode tNode = new XMLNode(testDoc, (Node)testTopElement, null);


        XMLNodeList eRoutes = eNode.getChildrenByName("Route");
        XMLNodeList tRoutes = tNode.getChildrenByName("Route");

        if(eRoutes.size() != tRoutes.size()) {
            issues.add(new XMLIssue("Route Count","Expected Route count of " + eRoutes.size() + " found a Route count of " + tRoutes.size()));
        }

        XMLNodeList eUriGroups = eNode.getChildrenByName("UriGroup");
        XMLNodeList tUriGroups = tNode.getChildrenByName("UriGroup");

        if(eUriGroups.size() != tUriGroups.size()) {
            issues.add(new XMLIssue("UriGroup Count","Expected UriGroup count of " + eUriGroups.size() + " found a URIGroup count of " + tUriGroups.size()));
        }

        XMLNodeList eServerClusters = eNode.getChildrenByName("ServerCluster");
        XMLNodeList tServerClusters = tNode.getChildrenByName("ServerCluster");

        if(eServerClusters.size() != tServerClusters.size()) {
            issues.add(new XMLIssue("ServerCluster Count","Expected ServerCluster count of " + eServerClusters.size() + " found a ServerCluster count of " + tServerClusters.size()));
        }

        XMLNodeList eVirtualHostGroups = eNode.getChildrenByName("VirtualHostGroup");
        XMLNodeList tVirtualHostGroups = tNode.getChildrenByName("VirtualHostGroup");

        if(eServerClusters.size() != tServerClusters.size()) {
            issues.add(new XMLIssue("VirtualHostGroup Count","Expected VirtualHostGroup count of " + eVirtualHostGroups.size() + " found a VirtualHostGroup count of " + tVirtualHostGroups.size()));
        }

        // Build expected key table
        Map<String,String> eEndpoints = createEndpointMap(eRoutes,eUriGroups,eVirtualHostGroups,eServerClusters,issues);
        Map<String,String> tEndpoints = createEndpointMap(tRoutes,tUriGroups,tVirtualHostGroups,tServerClusters,issues);

        for(String e:eEndpoints.keySet()) {
            if(tEndpoints.get(e) == null) {
                // Expected endpoint not found
                issues.add(new XMLIssue("Expected endpoint missing",e));
            }
        }

        for(String t:tEndpoints.keySet()) {
            if(eEndpoints.get(t) == null) {
                // Unexpected endpoint found
                issues.add(new XMLIssue("Found an unexpected endpoint",t));
            }
        }

        return issues;
    }

    private Map<String,String> createEndpointMap(XMLNodeList routes, XMLNodeList uriGroups, XMLNodeList virtualHostGroups, XMLNodeList serverClusters, List<XMLIssue> issues) {
        Map<String,String> endpoints = new TreeMap<String,String>();
        
        for(XMLNode route:routes.getList()) {
            String uriGroupStr = route.getAttribute("UriGroup");
            String virtualHostGroupStr = route.getAttribute("VirtualHostGroup");
            String serverClusterStr = route.getAttribute("ServerCluster");

            XMLNodeList uriGroupList = uriGroups.getNodesByAttribute("Name",uriGroupStr);
            XMLNodeList uris;
            if(uriGroupList.size() == 1) {
                uris = uriGroupList.get(0).getChildrenByName("Uri");
            }
            else {
                // Shouldn't be the case, report an issue
                issues.add(new XMLIssue("UriGroup Analysis","Unable to find only one UriGroup matching the name of " + uriGroupStr));
                uris = new XMLNodeList();
            }

            XMLNodeList virtualHostGroupList = virtualHostGroups.getNodesByAttribute("Name",virtualHostGroupStr);
            XMLNodeList virtualHosts;
            if(virtualHostGroupList.size() == 1) {
                virtualHosts = virtualHostGroupList.get(0).getChildrenByName("VirtualHost");
            }
            else {
                // Shouldn't be the case, report an issue
                issues.add(new XMLIssue("VirutalHostGroup Analysis","Unable to find only one VirtualHostGroup matching the name of " + virtualHostGroupStr));
                virtualHosts = new XMLNodeList();
            }

            XMLNodeList serverClusterList = serverClusters.getNodesByAttribute("Name",serverClusterStr);
            XMLNodeList servers;
            if(serverClusterList.size() == 1) {
                servers = serverClusterList.get(0).getChildrenByName("Server");
            }
            else {
                // Shouldn't be the case, report an issue
                issues.add(new XMLIssue("ServerCluster Analysis","Unable to find only one ServerCluster matching the name of " + serverClusterStr));
                servers = new XMLNodeList();
            }

            for(XMLNode server:servers.getList()) {
                for(XMLNode virtualHost:virtualHosts.getList()) {
                    for(XMLNode uri:uris.getList()) {
                        String endpoint = server.getAttribute("CloneID") + "-" + virtualHost.getAttribute("Name") + "-" + uri.getAttribute("Name");
                        endpoints.put(endpoint,endpoint);
                    }
                }
            }
        }

        return endpoints;
    }

    public static void main(String [] parms) throws ParserConfigurationException, SAXException, IOException  {
        List<XMLIssue> issues = XMLCompare.getInstance().compareEndpoints(new File("publish/files/extendedtests/sample1/expected.xml"), new File("publish/files/extendedtests/sample1/expected2.xml"));
        if(issues.size() == 0) {
            System.out.println("Success");
        }
        else {
            System.out.println("Compare failed.  Found " +issues.size() + " issues.");
            for(XMLIssue issue:issues) {
                System.out.println(issue.path);
                System.out.println("     " + issue.problem);
            }
        }
    }

    private class XMLNode{
        String name;
        Document doc;
        XMLNode parent;
        NamedNodeMap attributes;
        List<XMLNode>children = new ArrayList<XMLNode>();

        XMLNode(Document doc, Node node, XMLNode parent) {
            name = node.getNodeName();
            this.doc = doc;
            this.parent = parent;
            attributes = node.getAttributes();
            NodeList nl = node.getChildNodes();
            for( int i=0; i<nl.getLength(); i++) {
                Node n = nl.item(i);
                if(n.getNodeType() == 1) {
                    XMLNode xn = new XMLNode(doc, n, this);
                    children.add(xn);    
                }
            }
        }

        public XMLNodeList getChildrenByName(String name) {
            XMLNodeList retval = new XMLNodeList();

            for(XMLNode n:children) {
                if(n.name.equals(name)) {
                    retval.add(n);
                }
            }

            return retval;
        }

        public XMLNodeList getChildrenByNameAndAttribute(String name, String attribute, String attributeValue) {
            XMLNodeList retval = new XMLNodeList();

            for(XMLNode n:children) {
                if(n.name.equals(name)) {
                    if(n.getAttribute(attribute).equals(attributeValue)) {
                        retval.add(n);
                    }
                }
            }

            return retval;
        }

        public String getAttribute(String key) {
            Node attr = attributes.getNamedItem(key);
            if(attr == null)
                return "";
            return attr.getNodeValue();
        }
    }

    class XMLNodeList {
        List<XMLNode> nodes = new ArrayList<XMLNode>();

        public void add(XMLNode node) {
            nodes.add(node);
        }

        public int size() {
            return nodes.size();
        }

        public List<XMLNode> getList() {
            return nodes;
        }

        public XMLNode get(int index) {
            return nodes.get(index);
        }

        public XMLNodeList getNodesByAttribute(String key, String value) {
            XMLNodeList retval = new XMLNodeList();

            for(XMLNode node:nodes) {
                if(node.getAttribute(key).equals(value)) {
                    retval.add(node);
                }
            }

            return retval;
        }
    }
}

