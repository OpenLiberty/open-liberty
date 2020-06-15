/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;

import com.ibm.ws.fat.util.SharedServer;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.ibm.websphere.simplicity.log.Log;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class WebServerSetup {
    private static final Class<?> c = WebServerSetup.class;
    public static Boolean webserverInFront = false;

    private int targetPort = 8010;
    private int targetSecurePort = 8020;
    private String webserverHost = "localhost";
    private String targetHost = "localhost";
    private int webserverPort = 80;
    private int webserverSecurePort = 443;
    private SharedServer ss = null;

    public WebServerSetup(SharedServer ss) {
        this.ss = ss;
    }

    public void setUp() throws Exception {

        targetPort = ss.getTargetPort();
        targetHost = ss.getHost();
        targetSecurePort = ss.getTargetSecurePort();
        webserverHost = WebServerControl.getHostname();

        String port = WebServerControl.getPort();
        if (port != null) {
            webserverPort = Integer.valueOf(WebServerControl.getPort());
        }
        webserverSecurePort = Integer.valueOf(WebServerControl.getSecurePort());

        webserverInFront = WebServerControl.isWebserverInFront();
        if (webserverInFront) {
            Log.info(c, "setUp", "NonSecure WebServer: " + webserverHost + ":" + webserverPort + " targetNonSecure: " + targetHost + ":" + targetPort);
            Log.info(c, "setUp", "Secure WebServer: " + webserverHost + ":" + webserverSecurePort + " targetSecure: " + targetHost + ":" + targetSecurePort);
        }
        else {
            Log.info(c, "setUp", "targetNonSecure: " + targetHost + ":" + targetPort);
            Log.info(c, "setUp", "targetSecure: " + targetHost + ":" + targetSecurePort);
        }

        if (webserverInFront) {
            File cfgFile = new File("publish/plugin-cfg.xml");
            File updateFile = new File("publish/updated/plugin-cfg.xml");

            WebServerControl.copyLocalPluginConfig(cfgFile, updateFile);

            //edit plugin-cfg.xml
            updatePluginConfigurationFile(updateFile);

            //deploy
            WebServerControl.deployPluginConfigurationFile(updateFile);

            //start
            WebServerControl.startWebServer();
        }
    }

    public void tearDown() throws Exception {

        if (webserverInFront)
            //stop
            WebServerControl.stopWebServer(ss.getServerName());

    }

    private void updatePluginConfigurationFile(File cfgFile) throws Exception {
        Log.info(c, "updatePluginConfigurationFile", "cfgFile:" + cfgFile.exists() + ":" + cfgFile.getAbsolutePath());
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        Document doc;
        Transformer t = TransformerFactory.newInstance().newTransformer();

        BufferedReader s = new BufferedReader(new FileReader(cfgFile));
        InputSource input = new InputSource(s);
        doc = (Document) xpath.evaluate("/", input, XPathConstants.NODE);

        /*
         * Hostname
         */
        //TODO: we need a better way to determine Machine for Liberty server
        //      for now just assume Liberty server is co-located with junit execution
        //      when running with a non-local webserver create the rtc.host property
        //      with a routable address and use this so webserver can route to it.
        updateXPathAttribute(doc, xpath, "//Server/Transport[@Protocol='http']", "Hostname", targetHost);
        updateXPathAttribute(doc, xpath, "//Server/Transport[@Protocol='http']", "Port", String.valueOf(targetPort)); //MSN NEW
        updateXPathAttribute(doc, xpath, "//Server/Transport[@Protocol='https']", "Hostname", targetHost);
        updateXPathAttribute(doc, xpath, "//Server/Transport[@Protocol='https']", "Port", String.valueOf(targetSecurePort)); // MSN NEW

        /*
         * PluginInstallRoot
         */
        updateXPathAttribute(doc, xpath, "//Config/Log", "Name", WebServerControl.getLogPath());
        updateXPathAttribute(doc, xpath, "//Config/Property[@Name='PluginInstallRoot']", "Value", WebServerControl.getPlgDir());

        /*
         * VirtualHost
         */
        updateXPathAttribute(doc, xpath, "//Config/VirtualHostGroup/VirtualHost[@Name='*:80']", "Name", "*:" + webserverPort);

        Source source = new DOMSource(doc);
        OutputStream out = new FileOutputStream(cfgFile);
        StreamResult result = new StreamResult(out);
        t.transform(source, result);

    }

    /**
     * @param doc
     * @param xpath
     * @param xpathValue
     * @param attributeName
     * @param updatedAttributeValue
     * @throws XPathExpressionException
     */
    private void updateXPathAttribute(Document doc, XPath xpath, String xpathValue, String attributeName, String updatedAttributeValue) throws XPathExpressionException {

        NodeList nodeList = (NodeList) xpath.evaluate(xpathValue, doc, XPathConstants.NODESET);

        String value = nodeList.item(0).getAttributes().getNamedItem(attributeName).getNodeValue();
        Log.info(c, "updatePluginConfigurationFile", ":before:" + value);

        nodeList.item(0).getAttributes().getNamedItem(attributeName).setNodeValue(updatedAttributeValue);
        value = nodeList.item(0).getAttributes().getNamedItem(attributeName).getNodeValue();
        Log.info(c, "updatePluginConfigurationFile", ":after:" + value);
    }

}
