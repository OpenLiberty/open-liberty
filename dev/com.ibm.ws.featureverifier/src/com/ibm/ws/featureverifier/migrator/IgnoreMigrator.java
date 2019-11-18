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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class IgnoreMigrator {

    public IgnoreMigrator() {}

    enum Section {
        DEFECT, API_SPI_TYPE_REF, MISSING_3RD_PARTY, APPROVED, LOAD_RULES
    };

    static Map<Section, List<String>> headers = new HashMap<Section, List<String>>();
    static {
        headers.put(Section.DEFECT,
                    Arrays.asList("",
                                  "<!--=============================================================================================================================================-->",
                                  "<!-- Ignores that either should have, or already have defects... these are ones that we want to fix, or still don't understand.                  -->",
                                  "<!--=============================================================================================================================================-->",
                                  ""
                                    ));
        headers.put(Section.API_SPI_TYPE_REF,
                    Arrays.asList("",
                                  "<!--=============================================================================================================================================-->",
                                  "<!-- Api/Spi type reference issues. In their own section because they tend to be messier to fix, defects where appropriate                       -->",
                                  "<!--=============================================================================================================================================-->",
                                  ""));
        headers.put(Section.MISSING_3RD_PARTY,
                    Arrays.asList("",
                                  "<!--=============================================================================================================================================-->",
                                  "<!-- Internal API missing from dev, not an error, but tracked here to know how widespread the classloader contamination is by internal packages             -->",
                                  "<!--=============================================================================================================================================-->",
                                  ""
                                    ));
        headers.put(Section.APPROVED,
                    Arrays.asList("",
                                  "<!--=============================================================================================================================================-->",
                                  "<!-- Ignores that are confirmed as acceptable. Ignores in this section should have a comment referring to the work item number that added them   -->",
                                  "<!-- this is to assist with porting the ignore.xmls and maintaining a link to why we consider each of the ignores in this section to be acceptable.             -->",
                                  "<!--=============================================================================================================================================-->",
                                  ""
                                    ));
        headers.put(Section.LOAD_RULES,
                    Arrays.asList("",
                                  "<!--=============================================================================================================================================-->",
                                  "<!-- Ignores that only make sense in load rule builds.                                                                                           -->",
                                  "<!--=============================================================================================================================================-->",
                                  ""
                                    ));
    }

//    private static Map<Section, Collection<IgnoreBlock>> ignores = new HashMap<Section, Collection<IgnoreBlock>>();
//    static {
//        ignores.put(Section.DEFECT, new LinkedList<IgnoreBlock>());
//        ignores.put(Section.API_SPI_TYPE_REF, new LinkedList<IgnoreBlock>());
//        ignores.put(Section.MISSING_3RD_PARTY, new LinkedList<IgnoreBlock>());
//        ignores.put(Section.APPROVED, new LinkedList<IgnoreBlock>());
//    }

    private static HashSet<Ignore> ignores = new LinkedHashSet<Ignore>();

    private void processConfig(File ignoreXml) throws Exception {
        System.out.println("processing config " + ignoreXml.getAbsolutePath());
        Document ignoreDoc = null;
        FileInputStream ignoreStream = null;
        try {
            ignoreStream = new FileInputStream(ignoreXml);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            ignoreDoc = db.parse(ignoreStream);
        } finally {
            try {
                if (ignoreStream != null)
                    ignoreStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        List<String> commentsForCurrentFeature = new LinkedList<String>();
        Node ignoreElt = ignoreDoc.getElementsByTagName(IgnoreConstants.IGNORE_LIST).item(0);
        NodeList nl = ignoreElt.getChildNodes();
        int index = 0;
        Section current = Section.DEFECT;

        for (index = 0; index < nl.getLength(); index++) {
            Node n = nl.item(index);
            String content = n.getTextContent();
            if (n.getNodeType() == Node.COMMENT_NODE) {

                System.out.println("In a comment node with text " + n.getTextContent());

                //section divider?
                if (content.contains("=========")) {
                    index++;
                    n = nl.item(index);
                    while (n.getNodeType() != Node.COMMENT_NODE) {
                        index++;
                        n = nl.item(index);
                    }
                    System.out.println("Moved past section identifier.. 1st message in section is " + n.getTextContent());
                    content = n.getTextContent();
                    if (content.contains("Api/Spi type reference issues")) {
                        current = Section.API_SPI_TYPE_REF;
                        index++;
                        n = nl.item(index);
                        while (n.getNodeType() != Node.COMMENT_NODE) {
                            index++;
                            n = nl.item(index);
                        }
                        System.out.println("Recognised apispi type section: skipped to comment node with " + n.getTextContent());
                    } else if (content.contains("Internal API missing from dev")) {
                        current = Section.MISSING_3RD_PARTY;
                        index++;
                        n = nl.item(index);
                        while (n.getNodeType() != Node.COMMENT_NODE) {
                            index++;
                            n = nl.item(index);
                        }
                        System.out.println("Recognised 3rdparty section: skipped to comment node with " + n.getTextContent());
                    } else if (content.contains("Ignores that are confirmed as acceptable.")) {
                        current = Section.APPROVED;
                        while (n.getNodeType() != Node.COMMENT_NODE) {
                            index++;
                            n = nl.item(index);
                        }
                        while (!n.getTextContent().contains("=========")) {
                            index++;
                            n = nl.item(index);
                            while (n.getNodeType() != Node.COMMENT_NODE) {
                                index++;
                                n = nl.item(index);
                            }
                        }
                        System.out.println("Recognised approved section: skipped to comment node with " + n.getTextContent());
                    } else if (content.contains("Ignores that either should have, or already have defects")) {
                        current = Section.DEFECT;
                        while (n.getNodeType() != Node.COMMENT_NODE) {
                            index++;
                            n = nl.item(index);
                        }
                        while (!n.getTextContent().contains("=========")) {
                            index++;
                            n = nl.item(index);
                            while (n.getNodeType() != Node.COMMENT_NODE) {
                                index++;
                                n = nl.item(index);
                            }
                        }
                        System.out.println("Recognized defect section: skipped to comment node with " + n.getTextContent());
                    } else if (content.contains("Ignores that only make sense in load rule")) {
                        current = Section.LOAD_RULES;
                        while (n.getNodeType() != Node.COMMENT_NODE) {
                            index++;
                            n = nl.item(index);
                        }
                        while (!n.getTextContent().contains("=========")) {
                            index++;
                            n = nl.item(index);
                            while (n.getNodeType() != Node.COMMENT_NODE) {
                                index++;
                                n = nl.item(index);
                            }
                        }
                        System.out.println("Recognized load rule section: skipped to comment node with " + n.getTextContent());
                    } else if (content.contains("Ignore file for config")) {
                        while (n.getNodeType() != Node.COMMENT_NODE) {
                            index++;
                            n = nl.item(index);
                        }
                        while (!n.getTextContent().contains("=========")) {
                            index++;
                            n = nl.item(index);
                            while (n.getNodeType() != Node.COMMENT_NODE) {
                                index++;
                                n = nl.item(index);
                            }
                        }
                        System.out.println("Recognized header section: skipped to comment node with " + n.getTextContent());
                    } else {

                        //unknown section header.. skip until we find the next ==== block        			
                        index++;
                        n = nl.item(index);
                        while (n.getNodeType() != Node.COMMENT_NODE) {
                            index++;
                            n = nl.item(index);
                        }
                        System.out.println("skipped to comment node with " + n.getTextContent());
                        while (!n.getTextContent().contains("=========")) {
                            index++;
                            n = nl.item(index);
                            while (n.getNodeType() != Node.COMMENT_NODE) {
                                index++;
                                n = nl.item(index);
                            }
                            System.out.println("Unnown section: skipped to comment node with " + n.getTextContent());
                        }
                    }
                } else {
                    System.out.println("Added comment to list for block");
                    commentsForCurrentFeature.add(content);
                }
            } else {
                if (n.getNodeName().equals(IgnoreConstants.MESSAGE_RULE)) {
                    String forFeature = n.getAttributes().getNamedItem(IgnoreConstants.FEATURE_NAME).getTextContent();
                    System.out.println(current.name() + " for feature " + forFeature);

                    MissingOptions missingOptions = null;
                    Node missing = n.getAttributes().getNamedItem(IgnoreConstants.IF_MISSING);
                    Node included = n.getAttributes().getNamedItem(IgnoreConstants.WHEN_INCLUDED);
                    if (missing != null || included != null) {
                        String ifMissing = missing == null ? null : missing.getTextContent();
                        String whenIncluded = included == null ? null : included.getTextContent();
                        missingOptions = new MissingOptions(ifMissing, whenIncluded);
                    }

                    LoadRuleOptions loadRuleOptions = null;
                    Node only = n.getAttributes().getNamedItem(IgnoreConstants.ONLY_FOR_LOAD_RULES);
                    Node unused = n.getAttributes().getNamedItem(IgnoreConstants.ALLOW_UNUSED_FOR_LOAD_RULES);
                    if (only != null || unused != null) {
                        String onlyForLoadRules = only == null ? null : only.getTextContent();
                        String allowUnusedForLoadRules = unused == null ? null : unused.getTextContent();
                        loadRuleOptions = new LoadRuleOptions(onlyForLoadRules, allowUnusedForLoadRules);
                    }

                    for (String comment : commentsForCurrentFeature) {
                        Comment c = new Comment(current, forFeature, comment, missingOptions);
                        ignores.add(c);
                    }
                    commentsForCurrentFeature.clear();
                    MessageIgnore ignore = new MessageIgnore(current, forFeature, n.getTextContent(), missingOptions, loadRuleOptions);
                    ignores.add(ignore);

                } else if (n.getNodeName().equals(IgnoreConstants.PACKAGE_RULE)) {
                    Node feature = n.getAttributes().getNamedItem(IgnoreConstants.FOR_FEATURE);
                    String forFeature = feature == null ? null : feature.getTextContent();
                    boolean baseline = Boolean.valueOf(n.getAttributes().getNamedItem(IgnoreConstants.BASELINE_PACKAGE).getTextContent());
                    boolean runtime = Boolean.valueOf(n.getAttributes().getNamedItem(IgnoreConstants.RUNTIME_PACKAGE).getTextContent());
                    System.out.println(current.name() + " for feature " + forFeature);

                    for (String comment : commentsForCurrentFeature) {
                        Comment c = new Comment(current, forFeature, comment, null);
                        ignores.add(c);
                    }
                    commentsForCurrentFeature.clear();
                    PackageIgnore ignore = new PackageIgnore(current, forFeature, n.getTextContent(), baseline, runtime);
                    ignores.add(ignore);

                }
            }
        }

    }

    private Set<String> expandFeature(String feature, Map<String, String> singletonBindings, Map<String, FeatureInfo> nameToFeatureMap) {
        Set<String> results = new HashSet<String>();
        results.add(feature);

        FeatureInfo f = nameToFeatureMap.get(feature);
        for (Entry<String, Set<String>> nested : f.features.entrySet()) {
            FeatureInfo nestedInfo = nameToFeatureMap.get(nested.getKey());
            if (nestedInfo.singleton) {
                String singletonKey = nestedInfo.symbolicName.substring(0, nestedInfo.symbolicName.lastIndexOf("-"));
                if (nestedInfo.visibility.equals("private")) {
                    singletonKey = "##" + singletonKey;
                }
                if (singletonBindings.containsKey(singletonKey)) {
                    String choice = singletonBindings.get(singletonKey);
                    if (choice.startsWith("##")) {
                        choice = choice.substring(2);
                    }
                    nestedInfo = nameToFeatureMap.get(choice);
                }
            }
            results.addAll(expandFeature(nestedInfo.symbolicName, singletonBindings, nameToFeatureMap));
        }

        return results;
    }

    public void processDir(String featureVerifyDir) throws Exception {
        File featureVerifyDirectory = new File(featureVerifyDir);
        File oldServersDirectory = new File(featureVerifyDirectory, "servers");
        for (File configDir : oldServersDirectory.listFiles()) {
            if (configDir.getName().contains("config")) {
                File ignoreXml = new File(configDir, "ignore.xml");
                processConfig(ignoreXml);
            }
        }

        Map<String, FeatureInfo> nameToFeatureMap = new HashMap<String, FeatureInfo>();
        //ok.. now we need to load up all the features so we can reason over them.. 
        File allFeaturesDir = new File(new File(new File(new File(featureVerifyDirectory.getParentFile(), "build.image"), "wlp"), "lib"), "features");
        for (File feature : allFeaturesDir.listFiles()) {
            if (feature.getName().endsWith(".mf")) {
                FeatureInfo f = new FeatureInfo(allFeaturesDir, feature.getName());
                nameToFeatureMap.put(f.symbolicName, f);
            }
        }

        //now scan the map, any features we have ignores for that are not in the map, just became kernel-common features
        //where we'll add the ignores for every config. 
        Set<String> commonFeatures = new HashSet<String>();
        for (Ignore i : ignores) {
            if (!nameToFeatureMap.containsKey(i.feature)) {
                commonFeatures.add(i.feature);
            }
        }

        //ok.. now we need to process the new configs.. 
        File generatedConfigsDir = new File(new File(new File(new File(new File(new File(featureVerifyDirectory, "build"), "tmp"), "setGeneration"), "servers"), "setGenerate"), "configs");

        //for each new config, read in the server.xml to discover which features (and singleton bindings) are present.
        for (File configDir : generatedConfigsDir.listFiles()) {
            if (configDir.getName().equals("protected"))
                continue;

            //       File outputDir = new File(serversDir, configDir.getName());
            File outputFile = new File(configDir, "ignore.xml");
            FileOutputStream outStream = new FileOutputStream(outputFile);
            PrintWriter out = new PrintWriter(outStream);

            File serverXml = new File(configDir, "server.xml");
            ConfigInfo ci = ConfigInfo.fromFile(serverXml);

            //then use that info to expand each feature into its aggregate feature set
            //and aggregate all the aggregate feature sets
            Set<String> aggregate = new HashSet<String>();
            for (String feature : ci.features) {
                aggregate.addAll(expandFeature(feature, ci.singletonBindings, nameToFeatureMap));
            }

            aggregate.addAll(commonFeatures);

            // Now check if any auto features are enabled by the aggregate set
            for (Map.Entry<String, FeatureInfo> entry : nameToFeatureMap.entrySet()) {
                FeatureInfo feature = entry.getValue();
                if (feature.isAutoFeature()) {
                    Set<Set<String>> features = feature.getEnablingFeatureNames();
                    for (Set<String> featureSet : features) {
                        boolean allKnown = true;
                        for (String featureToTest : featureSet) {
                            if (!aggregate.contains(featureToTest)) {
                                allKnown = false;
                            }
                        }
                        if (allKnown) {
                            aggregate.addAll(expandFeature(feature.symbolicName, ci.singletonBindings, nameToFeatureMap));
                        }
                    }
                }
            }
            out.println("<ignoreList>");
            out.println("<!--=============================================================================================================================================-->");
            out.println("<!--  Ignore file for config " + configDir.getName() + "                                                                                     -->");
            out.println("<!--  Based on autoported ignore information from previous release                                                                       -->");
            out.println("<!--=============================================================================================================================================-->");
            //now per section.. output any ignores for features, or common
            for (Section s : Section.values()) {
                List<String> header = headers.get(s);
                for (String h : header) {
                    out.println(h);
                }

                for (Ignore i : ignores) {
                    if ((i.getSection() == s) && i.appliesTo(aggregate)) {
                        out.println(i.toString());
                    }
                }

            }

            out.println("</ignoreList>");
            out.close();
        }
    }

    public static void main(String args[]) throws Exception {
        IgnoreMigrator ip = new IgnoreMigrator();
        if (args.length < 1) {
            usage();
            System.exit(-1);
        }

        ip.processDir(args[0]);
    }

    /**
     * 
     */
    private static void usage() {
        System.out.println("Usage: com.ibm.ws.featureverifier.migrator.IgnoreMigrator <build.featureVerify directory>");

    }

}
