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
package com.ibm.ws.featureverifier.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 */
public class FeatureCheckerOutput {

    private static final String REVIEWED_CLOSE = "</reviewed>\n";

    private static final String REVIEWED_OPEN = "<reviewed>\n";

    /**
     * Contains all feature checker errors (measured against the release baseline)
     */
    private final Map<String, Set<String>> nameToJunitIssuesMap = new TreeMap<String, Set<String>>();

    /**
     * Contains feature checker errors that have not been reviewed previously
     */
    private final Map<String, Set<String>> currentIssues = new TreeMap<String, Set<String>>();

    /**
     * Contains feature checker errors that have been reviewed previously
     */
    private final Map<String, Set<String>> reviewedIssues = new TreeMap<String, Set<String>>();

    /**
     * @param newBuildDir
     * @param outDir
     * @param outDir2
     */
    public FeatureCheckerOutput(File baseDir, File newBuildDir, File outDir) {
        // Load all feature checker errors from junit.xml files
        loadErrors(newBuildDir);

        // Load lists of errors that have already been reviewed
        try {
            loadPreviouslyReviewed(baseDir);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        // Build an error map that excludes previously reviewed errors
        processCurrentErrors();

        try {
            saveCurrentErrors(outDir);
        } catch (IOException ex) {
            System.err.println("Failed to save current issues");
            ex.printStackTrace();
        }
    }

    /**
     * 
     */
    private void processCurrentErrors() {
        currentIssues.putAll(nameToJunitIssuesMap);
        for (Map.Entry<String, Set<String>> entry : reviewedIssues.entrySet()) {
            String featureName = entry.getKey();
            Set<String> all = currentIssues.get(featureName);
            if (all != null) {
                all.removeAll(entry.getValue());
                if (all.isEmpty()) {
                    currentIssues.remove(featureName);
                }
            }
        }

    }

    /**
     * @param outDir
     * @throws IOException
     * 
     */
    private void saveCurrentErrors(File outDir) throws IOException {
        StringBuffer fileName = new StringBuffer(ReportConstants.REVIEWED_PREFIX);
        DateFormat df = new SimpleDateFormat(ReportConstants.DATE_TIME_FORMAT);
        fileName.append(df.format(new Date()));
        fileName.append(ReportConstants.XML_SUFFIX);

        File outFile = new File(outDir, fileName.toString());
        FileWriter fw = new FileWriter(outFile);
        PrintWriter output = new PrintWriter(fw);

        output.write(REVIEWED_OPEN);
        for (Map.Entry<String, Set<String>> featureEntry : currentIssues.entrySet()) {
            FeatureXml feature = new FeatureXml(featureEntry.getKey(), featureEntry.getValue());
            feature.write(output);

        }
        output.write(REVIEWED_CLOSE);

        fw.close();
    }

    private class FeatureXml {
        private final String name;
        private final Set<String> issues;

        FeatureXml(String featureName, Set<String> issues) {
            this.name = featureName;
            this.issues = issues;
        }

        /**
         * @param output
         */
        public void write(PrintWriter output) {
            output.write("<feature name=\"" + name + "\">\n");
            for (String issue : issues) {
                output.write("<issue>");
                output.write(issue);
                output.write("</issue>\n");
            }
            output.write("</feature>\n");
        }

    }

    /**
     * @param baseDir
     * @throws IOException
     * @throws SAXException
     * @throws FileNotFoundException
     * @throws ParserConfigurationException
     * 
     */
    private void loadPreviouslyReviewed(File baseDir) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
        File[] reviewedFiles = baseDir.listFiles(new ReviewFileFilter());

        for (File f : reviewedFiles) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document d = db.parse(new FileInputStream(f));
            NodeList features = d.getElementsByTagName("feature");
            for (int i = 0; i < features.getLength(); i++) {
                Node feature = features.item(i);
                String featureName = feature.getAttributes().getNamedItem("name").getNodeValue();
                NodeList issueList = feature.getChildNodes();
                Set<String> issues = reviewedIssues.get(featureName);
                if (issues == null) {
                    issues = new HashSet<String>();
                    reviewedIssues.put(featureName, issues);
                }
                for (int j = 0; j < issueList.getLength(); j++) {
                    issues.add(issueList.item(j).getTextContent());
                }

            }
        }
    }

    /**
     * @param key
     * @return
     */
    public boolean hasProblemsForFeature(String key) {
        return currentIssues.containsKey(key);
    }

    /**
     * @param key
     * @return
     */
    public Set<String> getNewProblems(String key) {
        return currentIssues.get(key);
    }

    public void loadErrors(File baseDir) {
        //"build\tmp\apiCheckerBase\servers";
        File fvProject = new File(baseDir, "build.featureverify");
        File fvProjectServerDir = new File(fvProject, "build" + File.separator + "tmp" + File.separator + "apiCheckerBase" + File.separator + "servers");
        if (!fvProjectServerDir.exists()) {
            System.out.println("Unable to merge Junit reports, as they were not found, ensure you have run the feature checker locally.");
        } else {
            System.out.print("Loading feature checker errors [");
            for (File server : fvProjectServerDir.listFiles()) {
                System.out.print(".");
                if (server.getName().startsWith(".")) {
                    continue;
                }
                File junitXml = new File(server, "junit.xml");
                if (junitXml.exists() && junitXml.isFile()) {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db;
                    try {
                        db = dbf.newDocumentBuilder();
                        Document doc = db.parse(junitXml);

                        NodeList nl = doc.getElementsByTagName("failure");

                        int nodecount = nl.getLength();
                        for (int i = 0; i < nodecount; i++) {
                            Node n = nl.item(i);
                            String feature = n.getAttributes().getNamedItem("message").getTextContent();
                            String text = n.getTextContent();
                            if (!nameToJunitIssuesMap.containsKey(feature)) {
                                nameToJunitIssuesMap.put(feature, new TreeSet<String>());
                            }
                            if (text.endsWith("For More Information, visit http://was.pok.ibm.com/xwiki/bin/view/Build/FeatureChecker")) {
                                text = text.substring(0, text.length() - "For More Information, visit http://was.pok.ibm.com/xwiki/bin/view/Build/FeatureChecker".length());
                            }

                            nameToJunitIssuesMap.get(feature).add(text);
                        }
                    } catch (ParserConfigurationException e) {
                        System.err.println("junitXml : " + junitXml.getAbsolutePath());
                        e.printStackTrace();
                    } catch (SAXException e) {
                        System.err.println("junitXml : " + junitXml.getAbsolutePath());
                        e.printStackTrace();
                    } catch (IOException e) {
                        System.err.println("junitXml : " + junitXml.getAbsolutePath());
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("]");
        }
    }

}
