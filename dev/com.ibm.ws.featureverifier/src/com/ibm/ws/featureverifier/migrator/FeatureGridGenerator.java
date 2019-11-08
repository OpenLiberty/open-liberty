/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.featureverifier.migrator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FeatureGridGenerator {

    public FeatureGridGenerator() {}

    public static TreeMap<String, TreeMap<String, String>> singletonBindings = new TreeMap<String, TreeMap<String, String>>();
    public static TreeSet<String> configIds = new TreeSet<String>();

    public static Map<String, Set<String>> featuresForConfigs = new HashMap<String, Set<String>>();

    private void processConfig(File serverXml, String configId, String releaseId) throws Exception {

        Document serverDoc = null;
        FileInputStream serverStream = null;
        try {
            serverStream = new FileInputStream(serverXml);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            serverDoc = db.parse(serverStream);
        } finally {
            try {
                if (serverStream != null)
                    serverStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Node ignoreElt = serverDoc.getElementsByTagName("featureManager").item(0);
        NodeList nl = ignoreElt.getChildNodes();
        int index = 0;
        for (index = 0; index < nl.getLength(); index++) {
            Node n = nl.item(index);
            String content = n.getTextContent();
            if (content.contains(" bound to ")) {
                String key = content.substring(0, content.indexOf(" bound to ")).trim();
                String value = content.substring(content.indexOf(" bound to ") + " bound to ".length()).trim();
                if (value.startsWith("##")) {
                    value.substring(2);
                }
                TreeMap<String, String> bindingsForKey;
                if (!singletonBindings.containsKey(key)) {
                    bindingsForKey = new TreeMap<String, String>();
                    singletonBindings.put(key, bindingsForKey);

                } else {
                    bindingsForKey = singletonBindings.get(key);
                }
                configIds.add(configId);
                bindingsForKey.put(configId, value);
            }
            if (n.getNodeName().equals("feature")) {
                Set<String> featuresForConfig = featuresForConfigs.get(configId);
                if (featuresForConfig == null) {
                    featuresForConfig = new TreeSet<String>();
                    featuresForConfigs.put(configId, featuresForConfig);
                }
                String feature = n.getTextContent();
                if (!feature.startsWith("protected.85") && !feature.equals("featureverifier-1.0")) {
                    featuresForConfig.add(n.getTextContent());
                }
            }
            if (content.contains(" will be supplied by")) {
                String supplied = content.substring(0, content.indexOf(" will be supplied by")).trim();
                Set<String> featuresForConfig = featuresForConfigs.get(configId);
                if (featuresForConfig == null) {
                    featuresForConfig = new TreeSet<String>();
                    featuresForConfigs.put(configId, featuresForConfig);
                }
                featuresForConfig.add(supplied);
            }
        }
    }

    public void processConfigsDir(String featureVerifyDir) throws Exception {
        File dir = new File(featureVerifyDir);
        if (dir == null || dir.listFiles() == null)
            System.out.println("Directory " + featureVerifyDir + " does not exist or is empty");

        String releaseId = "";
        for (File configDir : dir.listFiles()) {
            if (configDir.getName().contains("config")) {
                File serverXml = new File(configDir, "server.xml");
                String configDirName = configDir.getName();
                releaseId = configDirName.substring(0, configDirName.indexOf("config"));
                String configId = configDirName.substring(configDirName.indexOf("config") + 6, configDirName.length());

                System.out.println(" " + configId + " " + releaseId + " " + serverXml.getAbsolutePath());

                processConfig(serverXml, configId, releaseId);
            }
        }

        System.out.println("1. " + releaseId + " Feature Verifier Configs");

        System.out.println("<table>");
        System.out.println("<tr>");
        System.out.println("<td>SingletonID</td>");
        for (String configId : configIds) {
            System.out.println("<td>[Config" + configId + ">#HConfig" + configId + "]</td>");
        }
        System.out.println("</tr>");
        for (Entry<String, TreeMap<String, String>> t : singletonBindings.entrySet()) {
            String singletonKey = t.getKey();
            boolean isPrivate = false;
            if (singletonKey.startsWith("##")) {
                singletonKey = singletonKey.substring(2);
                isPrivate = true;
            }
            System.out.println("<tr>");
            System.out.println("<td>" + (isPrivate ? "~~" : "") + singletonKey + (isPrivate ? "~~" : "") + "</td>");
            for (String configId : configIds) {

                Map<String, String> bindings = t.getValue();
                String binding = bindings.get(configId);
                if (binding == null) {
                    System.out.println("<td></td>");
                } else {
                    boolean preferred = binding.startsWith("##");
                    if (preferred) {
                        binding = binding.substring(2);
                    }
                    if (isPrivate) {
                        System.out.println("<td bgcolor=" + getColorForOutput(binding) + ">~~" + binding.substring(binding.lastIndexOf('-') + 1) + "~~</td>");
                    } else {
                        System.out.println("<td bgcolor=" + getColorForOutput(binding) + ">*" + binding.substring(binding.lastIndexOf('-') + 1) + "*</td>");
                    }
                }
            }
            System.out.println("</tr>");
        }
        System.out.println("</table>");
        for (String configId : configIds) {
            System.out.println("1.1.1 Config" + configId);
            Set<String> features = featuresForConfigs.get(configId);
            for (String feature : features) {
                System.out.println("* " + feature);
            }
        }
    }

    String pastels[] = { "#f49ac2", "#cfcfc4", "#aec6cf", "#ffb347", "#779ecb", "#77dd77", "#cb99c9", "#dea5a4", "#ffd1dc", "#03c03c", "#fdfd96", "#ff6961", "#b39eb5", "#ffdfdf",
                        "#a3eeba" };
    int pastelIndex = 0;
    Map<String, String> allocatedColors = new HashMap<String, String>();

    private String getColorForOutput(String name) {
        //assume -x.x versioning.. 
        String version = name.substring(name.lastIndexOf("-"));
        String color;
        if (!allocatedColors.containsKey(version)) {
            color = pastels[pastelIndex];
            pastelIndex++;
            if (pastelIndex >= pastels.length) {
                pastelIndex = 0;
            }
            allocatedColors.put(version, color);
        } else {
            color = allocatedColors.get(version);
        }
        return color;
    }

    private String shortenNameForOutput(String name) {
        if (name.startsWith("com.ibm.websphere.appserver.javax")) {
            return "wj" + name.substring("com.ibm.websphere.appserver.javax".length());
        } else if (name.startsWith("com.ibm.websphere.appserver")) {
            return "wa" + name.substring("com.ibm.websphere.appserver".length());
        } else if (name.startsWith("com.ibm.ws.appserver")) {
            return "wsa" + name.substring("com.ibm.ws.appserver".length());
        } else if (name.startsWith("com.ibm.ws")) {
            return "ws" + name.substring("com.ibm.ws".length());
        }
        return name;
    }

    public static void main(String args[]) throws Exception {
        FeatureGridGenerator ip = new FeatureGridGenerator();
        //ip.processDir("c:\\liberty\\xopen2\\build.featureverify");

        ip.processConfigsDir("c:\\users\\ibm_admin\\workspace8558/build.featureverify/build/tmp/setGeneration/servers/setGenerate/configs");
    }

}
