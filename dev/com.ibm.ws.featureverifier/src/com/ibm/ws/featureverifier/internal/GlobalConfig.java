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
package com.ibm.ws.featureverifier.internal;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.osgi.framework.Version;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.aries.buildtasks.semantic.versioning.model.FeatureInfo;
import com.ibm.aries.buildtasks.semantic.versioning.model.PkgInfo;

public class GlobalConfig {

    public static final boolean GenerateGlobalConfigMode = "true".equals(System.getenv("featureChecker.generateGlobalConfig"));
    public static final boolean ApiSpiReviewMode = "true".equals(System.getenv("featureChecker.apiSpiReview"));
    public static final boolean ignoreMissingFeatures = "true".equals(System.getenv("featureChecker.ignoreMissingFeatures"));

    private static class TrackedPattern {
        Pattern pattern;
        Boolean seen;
        String patternAsString;

        public TrackedPattern(Pattern p) {
            seen = Boolean.FALSE;
            pattern = p;
            patternAsString = p.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime
                     * result
                     + ((patternAsString == null) ? 0 : patternAsString
                                     .hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TrackedPattern other = (TrackedPattern) obj;
            if (patternAsString == null) {
                if (other.patternAsString != null)
                    return false;
            } else if (!patternAsString.equals(other.patternAsString))
                return false;
            return true;
        }
    }

    private static Map<String, Set<TrackedPattern>> messagesToIgnoreByFeature = new HashMap<String, Set<TrackedPattern>>();

    private static Set<String> featuresToRestrictUnusedReportingTo = new HashSet<String>();

    private static Set<String> featuresToIgnoreInRuntime = new HashSet<String>();
    private static Set<String> featuresToIgnoreInBaseline = new HashSet<String>();
    private static Set<String> packagesToGloballyIgnoreInRuntime = new HashSet<String>();
    private static Set<String> packagesToGloballyIgnoreInBaseline = new HashSet<String>();
    private static Map<String, Set<String>> packagesToIgnorePerFeatureInRuntime = new HashMap<String, Set<String>>();
    private static Map<String, Set<String>> packagesToIgnorePerFeatureInBaseline = new HashMap<String, Set<String>>();

    private static Boolean setGenerate = Boolean.FALSE;
    private static String setGenerateEditionName = null;
    private static String setGenerateConfigPrefix = "";
    private static String queryMode = null;
    private static String releaseMode = "RELEASE";
    private static boolean ignoreMissingResources = false;

    private static String editionNameToTestAllFeaturesForWithoutComparison = null;

    private static Set<String> featuresToIgnoreDuringSetGeneration = new HashSet<String>();

    private static Map<String, String> extraJarVersionData = new HashMap<String, String>();

    private static Set<String> activeTagNames = new HashSet<String>();
    private static Set<String> fqnClassesToIgnoreInBaseline = new HashSet<String>();

    public static String getExtraJarVersionData(String location) {
        return extraJarVersionData.get(location);
    }

    private static boolean hasPatternsToIgnore(String feature) {
        return messagesToIgnoreByFeature.containsKey(feature);
    }

    private static Set<TrackedPattern> getIgnorePatternsForFeature(String feature) {
        return messagesToIgnoreByFeature.get(feature);
    }

    /**
     * Whether things like features referencing jars which don't exist should cause an exception
     */
    public static boolean getIgnoreMissingResources() {
        return ignoreMissingResources;
    }

    /**
     * Sets whether things like features referencing jars which don't exist should cause an exception.
     */
    public static void setIgnoreMissingResources(boolean ignoreMissingResources) {
        GlobalConfig.ignoreMissingResources = ignoreMissingResources;
    }

    public static boolean isIgnored(String feature, String message) {
        if (GlobalConfig.hasPatternsToIgnore(feature)) {
            Set<TrackedPattern> excluded = GlobalConfig.getIgnorePatternsForFeature(feature);
            for (TrackedPattern p : excluded) {
                Matcher m = p.pattern.matcher(message);
                if (m.matches()) {
                    p.seen = Boolean.TRUE;
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isClassIgnoredInBaseline(String fnqToCheck) {
        return fqnClassesToIgnoreInBaseline.contains(fnqToCheck);
    }

    static void addFeatureToIgnore(FeatureInfo f) {
        addFeatureToIgnore(f.getName());
        if (f.getShortName() != null) {
            addFeatureToIgnore(f.getShortName());
        }
    }

    static void addFeatureToIgnore(String f) {
        featuresToIgnoreInRuntime.add(f);
        featuresToIgnoreInBaseline.add(f);
    }

    static void restrictUnusedReportingToFeatureSet(Collection<FeatureInfo> fi) {
        for (FeatureInfo f : fi) {
            featuresToRestrictUnusedReportingTo.add(f.getName());
            if (f.getShortName() != null) {
                featuresToRestrictUnusedReportingTo.add(f.getShortName());
            }
        }
    }

    public static boolean ignoreUnusedsForThisFeature(String featureName) {
        if (!ignoreMissingFeatures) {
            //if ignore missing features is false, then we must report all unused.. 
            //it's only true when running load rules, when unuseds become harder to deal with.
            return false;
        } else {
            //basic verification uses a set of features to restrict reporting to
            //and non-basic adds the features to ignore to the ignore set.
            if (featuresToRestrictUnusedReportingTo.isEmpty()) {
                return featuresToIgnoreInRuntime.contains(featureName);
            } else {
                return !featuresToRestrictUnusedReportingTo.contains(featureName);
            }
        }
    }

    public static Map<String, Set<Pattern>> getUnusedMessageIgnores() {
        Map<String, Set<Pattern>> results = new HashMap<String, Set<Pattern>>();
        for (Map.Entry<String, Set<TrackedPattern>> feature : messagesToIgnoreByFeature.entrySet()) {
            Set<Pattern> notseen = new HashSet<Pattern>();
            for (TrackedPattern tp : feature.getValue()) {
                if (!tp.seen) {
                    notseen.add(tp.pattern);
                }
            }
            if (!notseen.isEmpty()) {
                results.put(feature.getKey(), notseen);
            }
        }
        return results;
    }

    public static boolean isPackageToBeIgnoredInBaseline(PkgInfo pkgInfo, String fromBundle, String forFeature) {
        String pkgVersion = pkgInfo.getVersion();
        if (pkgVersion != null) {
            Version v = Version.parseVersion(pkgVersion);
            pkgVersion = v.getMajor() + "." + v.getMinor();
        } else {
            pkgVersion = "null";
        }
        String pkg = pkgInfo.getName() + "@" + pkgVersion;

        //try global first
        if (packagesToGloballyIgnoreInBaseline.contains(pkg))
            return true;

        //if we have a bundle arg, test globally with that next.. 
        if (fromBundle != null && packagesToGloballyIgnoreInBaseline.contains(pkg + "##" + fromBundle))
            return true;

        //if we have a feature arg, test global for that feature.. 
        if (packagesToIgnorePerFeatureInBaseline.containsKey(forFeature)
            && packagesToIgnorePerFeatureInBaseline.get(forFeature).contains(pkg))
            return true;

        //if we have a feature arg, test with bundle arg.. 
        if (packagesToIgnorePerFeatureInBaseline.containsKey(forFeature)
            && packagesToIgnorePerFeatureInBaseline.get(forFeature).contains(pkg + "##" + fromBundle))
            return true;

        return false;
    }

    public static boolean isPackageToBeIgnoredInRuntime(PkgInfo pkgInfo, String fromBundle, String forFeature) {
        String pkgVersion = pkgInfo.getVersion();
        if (pkgVersion != null) {
            Version v = Version.parseVersion(pkgVersion);
            pkgVersion = v.getMajor() + "." + v.getMinor();
        } else {
            pkgVersion = "null";
        }
        String pkg = pkgInfo.getName() + "@" + pkgVersion;

        //try global first
        if (packagesToGloballyIgnoreInRuntime.contains(pkg))
            return true;

        //if we have a bundle arg, test globally with that next.. 
        if (fromBundle != null && packagesToGloballyIgnoreInRuntime.contains(pkg + "##" + fromBundle))
            return true;

        //if we have a feature arg, test global for that feature.. 
        if (packagesToIgnorePerFeatureInRuntime.containsKey(forFeature)
            && packagesToIgnorePerFeatureInRuntime.get(forFeature).contains(pkg))
            return true;

        //if we have a feature arg, test with bundle arg.. 
        if (packagesToIgnorePerFeatureInRuntime.containsKey(forFeature)
            && packagesToIgnorePerFeatureInRuntime.get(forFeature).contains(pkg + "##" + fromBundle))
            return true;

        return false;
    }

    public static boolean isFeatureToBeIgnoredInBaseline(String feature) {
        if (featuresToIgnoreInBaseline.contains(feature))
            return true;

        return false;
    }

    public static boolean isFeatureToBeIgnoredInRuntime(String feature) {
        if (featuresToIgnoreInRuntime.contains(feature))
            return true;

        return false;
    }

    public static boolean getSetGenerate() {
        return setGenerate;
    }

    public static String getQueryMode() {
        return queryMode;
    }

    public static String getReleaseMode() {
        return releaseMode;
    }

    public static String getSetGenerateEditionName() {
        return setGenerateEditionName;
    }

    public static String getSetGenerateConfigPrefix() {
        return setGenerateConfigPrefix;
    }

    public static Set<String> featuresToIgnoreDuringSetGeneration() {
        return featuresToIgnoreDuringSetGeneration;
    }

    public static String getEditionNameToTestAllFeaturesForWithoutComparison() {
        return editionNameToTestAllFeaturesForWithoutComparison;
    }

    /**
     * Load the ignore data.. if any..
     * 
     * @param configFile
     */
    public static void setConfigFile(File configFile) throws Exception {
        //no file is ok.
        if (configFile == null || !configFile.exists()) {
            return;
        }

        //if file exists, it'd better parse ok, else we'll just exception out of here.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document d = db.parse(configFile);

        NodeList modes = d.getElementsByTagName("mode");
        if (modes != null) {
            Node mode = modes.item(0);
            if (mode != null) {
                NamedNodeMap fattrs = mode.getAttributes();
                if (fattrs.getNamedItem("setGenerate") != null) {
                    String setgen = fattrs.getNamedItem("setGenerate").getTextContent();
                    setGenerate = Boolean.valueOf(setgen);
                }
                if (fattrs.getNamedItem("useEdition") != null) {
                    String editionName = fattrs.getNamedItem("useEdition").getTextContent();
                    setGenerateEditionName = editionName;
                }
                if (fattrs.getNamedItem("configPrefix") != null) {
                    String prefix = fattrs.getNamedItem("configPrefix").getTextContent();
                    setGenerateConfigPrefix = prefix;
                }
                if (fattrs.getNamedItem("releaseMode") != null) {
                    String prefix = fattrs.getNamedItem("releaseMode").getTextContent();
                    releaseMode = prefix;
                }
                if (fattrs.getNamedItem("editionNameToTestAllFeaturesForWithoutComparison") != null) {
                    String editionNameForNonCompareTests = fattrs.getNamedItem("editionNameToTestAllFeaturesForWithoutComparison").getTextContent();
                    editionNameToTestAllFeaturesForWithoutComparison = editionNameForNonCompareTests;
                }
                if (fattrs.getNamedItem("queryMode") != null) {
                    String prefix = fattrs.getNamedItem("queryMode").getTextContent();
                    queryMode = prefix;
                }
            }
        }

        NodeList ignoreDuringSetGeneration = d.getElementsByTagName("ignoreDuringSetGeneration");
        if (ignoreDuringSetGeneration != null) {
            Node node = ignoreDuringSetGeneration.item(0);
            if (node != null) {
                NodeList children = node.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node ignoreFeature = children.item(i);
                    if (ignoreFeature.getNodeName().equals("feature")) {
                        String featureName = ignoreFeature.getTextContent();
                        featuresToIgnoreDuringSetGeneration.add(featureName);
                    }
                }
            }
        }
        Map<String, String> osNamePatternsMap = new HashMap<String, String>();
        Map<String, String> vendorNamePatternsMap = new HashMap<String, String>();
        NodeList tagNamePatterns = d.getElementsByTagName("tagName");
        if (tagNamePatterns != null) {
            for (int i = 0; i < tagNamePatterns.getLength(); i++) {
                Node node = tagNamePatterns.item(i);
                if (node != null) {
                    NamedNodeMap fattrs = node.getAttributes();
                    String name = null;
                    String patt = null;
                    if (fattrs.getNamedItem("name") != null) {
                        name = fattrs.getNamedItem("name").getTextContent();
                    }
                    if (fattrs.getNamedItem("osNamePattern") != null && fattrs.getNamedItem("vendorNamePattern") != null) {
                        throw new IllegalStateException("Cannot specify both osNamePattern and vendorNamePattern on same tag name");
                    }
                    boolean isOs = false;
                    if (fattrs.getNamedItem("osNamePattern") != null) {
                        patt = fattrs.getNamedItem("osNamePattern").getTextContent();
                        isOs = true;
                    }
                    if (fattrs.getNamedItem("vendorNamePattern") != null) {
                        patt = fattrs.getNamedItem("vendorNamePattern").getTextContent();
                    }
                    if (name == null || patt == null) {
                        throw new IllegalStateException("Bad osName element, expected <osName name=\"fred\" pattern=\".*IBM.*\"/>");
                    }
                    if (isOs)
                        osNamePatternsMap.put(name, patt);
                    else
                        vendorNamePatternsMap.put(name, patt);
                }
            }
        }
        String osName = System.getProperty("os.name", "unknown");

        for (Map.Entry<String, String> osNamePattern : osNamePatternsMap.entrySet()) {
            if (osName.matches(osNamePattern.getValue())) {
                activeTagNames.add(osNamePattern.getKey());
            }
        }

        String vendorName = System.getProperty("java.vendor", "unknown");

        for (Map.Entry<String, String> vendorNamePattern : vendorNamePatternsMap.entrySet()) {
            if (vendorName.matches(vendorNamePattern.getValue())) {
                activeTagNames.add(vendorNamePattern.getKey());
            }
        }

        NodeList classesToIgnoreInBaseline = d.getElementsByTagName("ignoreClassInBaseline");
        if (classesToIgnoreInBaseline != null) {
            for (int i = 0; i < classesToIgnoreInBaseline.getLength(); i++) {
                Node node = classesToIgnoreInBaseline.item(i);
                if (node != null) {
                    String className = node.getTextContent();
                    fqnClassesToIgnoreInBaseline.add(className);
                }
            }
        }

        Node ignoreList = d.getElementsByTagName("ignoreList").item(0);
        NodeList ignoreListContent = ignoreList.getChildNodes();
        for (int i = 0; i < ignoreListContent.getLength(); i++) {
            Node ignorableEntry = ignoreListContent.item(i);
            if (ignorableEntry.getNodeName().equals("message")) {
                NamedNodeMap fattrs = ignorableEntry.getAttributes();
                String featureName = fattrs.getNamedItem("feature").getTextContent();

                if (fattrs.getNamedItem("onlyForTag") != null) {
                    String restrictToOsList = fattrs.getNamedItem("onlyForTag").getTextContent();
                    String oslist[] = restrictToOsList.split(",");
                    boolean match = false;
                    for (String os : oslist) {
                        if (activeTagNames.contains(os)) {
                            match = true;
                        }
                    }
                    if (!match) {
                        //skip this ignore.. it's not supposed to be active for this os.
                        continue;
                    }
                }

                if (fattrs.getNamedItem("onlyForLoadRules") != null) {
                    String onlyText = fattrs.getNamedItem("onlyForLoadRules").getTextContent();
                    Boolean onlyForLoadRules = Boolean.valueOf(onlyText);
                    if (onlyForLoadRules && !GlobalConfig.ignoreMissingFeatures) {
                        //skip this rule, it's only needed for load rules, and we're not processing them at the mo.
                        continue;
                    }
                }

                boolean presetAsSeen = false;
                if (GlobalConfig.ignoreMissingFeatures && fattrs.getNamedItem("allowUnusedWithLoadRules") != null) {
                    String ignoreText = fattrs.getNamedItem("allowUnusedWithLoadRules").getTextContent();
                    Boolean ignore = Boolean.valueOf(ignoreText);
                    presetAsSeen = ignore;
                }

                String pattern = ignorableEntry.getTextContent();
                Pattern p = Pattern.compile(pattern);

                if (!messagesToIgnoreByFeature.containsKey(featureName)) {
                    messagesToIgnoreByFeature.put(featureName, new HashSet<TrackedPattern>());
                }
                TrackedPattern tp = new TrackedPattern(p);
                if (presetAsSeen) {
                    tp.seen = Boolean.TRUE;
                }
                messagesToIgnoreByFeature.get(featureName).add(tp);
            } else if (ignorableEntry.getNodeName().equals("feature")) {
                NamedNodeMap fattrs = ignorableEntry.getAttributes();
                String baseline = fattrs.getNamedItem("baseline") == null ? null : fattrs.getNamedItem("baseline").getTextContent();
                String runtime = fattrs.getNamedItem("runtime") == null ? null : fattrs.getNamedItem("runtime").getTextContent();

                String featureName = ignorableEntry.getTextContent();

                if (baseline != null) {
                    featuresToIgnoreInBaseline.add(featureName);
                }
                if (runtime != null) {
                    featuresToIgnoreInRuntime.add(featureName);
                }

            } else if (ignorableEntry.getNodeName().equals("package")) {
                NamedNodeMap fattrs = ignorableEntry.getAttributes();
                String baseline = fattrs.getNamedItem("baseline") == null ? null : fattrs.getNamedItem("baseline").getTextContent();
                String runtime = fattrs.getNamedItem("runtime") == null ? null : fattrs.getNamedItem("runtime").getTextContent();
                String fromBundle = fattrs.getNamedItem("fromBundle") == null ? null : fattrs.getNamedItem("fromBundle").getTextContent();
                String forFeatures = fattrs.getNamedItem("forFeatures") == null ? null : fattrs.getNamedItem("forFeatures").getTextContent();

                String packageName = ignorableEntry.getTextContent();

                if (fromBundle != null) {
                    packageName += "##" + fromBundle;
                }

                boolean globalIgnore = forFeatures == null;

                if (baseline != null) {
                    if (globalIgnore)
                        packagesToGloballyIgnoreInBaseline.add(packageName);
                    else {
                        String features[] = forFeatures.split(",");
                        for (String feature : features) {
                            if (!packagesToIgnorePerFeatureInBaseline.containsKey(feature)) {
                                packagesToIgnorePerFeatureInBaseline.put(feature, new HashSet<String>());
                            }
                            packagesToIgnorePerFeatureInBaseline.get(feature).add(packageName);
                        }
                    }
                }
                if (runtime != null) {
                    if (globalIgnore)
                        packagesToGloballyIgnoreInRuntime.add(packageName);
                    else {
                        String features[] = forFeatures.split(",");
                        for (String feature : features) {
                            if (!packagesToIgnorePerFeatureInRuntime.containsKey(feature)) {
                                packagesToIgnorePerFeatureInRuntime.put(feature, new HashSet<String>());
                            }
                            packagesToIgnorePerFeatureInRuntime.get(feature).add(packageName);
                        }
                    }
                }

            }
        }

        //lastly.. process the extra jar version data file.. if it's present. 
        File configDir = configFile.getParentFile();
        File baselineJarVersionData = new File(configDir, "extraJarVersionData.txt");
        if (baselineJarVersionData.exists()) {
            FileReader fr = new FileReader(baselineJarVersionData);
            BufferedReader br = new BufferedReader(fr);
            try {
                String line = br.readLine();
                while (line != null) {
                    String nextLine = br.readLine();
                    extraJarVersionData.put(line, nextLine);
                    System.out.println("Loaded extra " + line + "   ::   " + nextLine);
                    line = br.readLine();
                }
            } finally {
                br.close();
            }
        } else {
            System.out.println("No extraVersionData found");
        }
    }

    /**
     * Create the ignore data specific to this server's installed features and master config.
     *
     * @param configFile
     * @param featureSet
     */
    public static void generateConfigFile(File configFile, File masterConfigFile, Set<String> featureSet) throws Exception {
        if (configFile == null || masterConfigFile == null || !masterConfigFile.exists()) {
            return;
        }

        // parse master config file for tagName, message, and package nodes
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document d = db.parse(masterConfigFile);

        Set<String> alwaysIncludedXml = new HashSet<String>();
        Map<String, SortedSet<String>> ignoreListFeaturesToXml = new HashMap<String, SortedSet<String>>();

        // list of tagName elements
        NodeList tagNamePatterns = d.getElementsByTagName("tagName");
        if (tagNamePatterns != null) {
            for (int i = 0; i < tagNamePatterns.getLength(); i++) {
                Node node = tagNamePatterns.item(i);
                if (node != null) {
                    alwaysIncludedXml.add(GlobalConfig.stringifyXMLNode(node));
                }
            }
        }

        Node ignoreList = d.getElementsByTagName("ignoreList").item(0);
        NodeList ignoreListContent = ignoreList.getChildNodes();
        for (int i = 0; i < ignoreListContent.getLength(); i++) {
            Node ignorableEntry = ignoreListContent.item(i);

            if (ignorableEntry != null) {
                if (ignorableEntry.getNodeName().equals("message")) {
                    // map feature names to set of ignore messages
                    String xmlString = GlobalConfig.stringifyXMLNode(ignorableEntry);
                    String feature = ignorableEntry.getAttributes().getNamedItem("feature") == null ? null : ignorableEntry.getAttributes().getNamedItem("feature").getTextContent();
                    if (!ignoreListFeaturesToXml.containsKey(feature)) {
                        ignoreListFeaturesToXml.put(feature, new TreeSet<String>());
                    }
                    ignoreListFeaturesToXml.get(feature).add(xmlString);
                } else if (ignorableEntry.getNodeName().equals("package")) {
                    // include in mapping feature names to package elements
                    String xmlString = GlobalConfig.stringifyXMLNode(ignorableEntry);
                    alwaysIncludedXml.add(xmlString);
                }
            }
        }

        //generate ignore file for server as a set of lines of xml
        SortedSet<String> xmlLines = new TreeSet<String>();
        xmlLines.addAll(alwaysIncludedXml);

        //since featureSet contains shortnames, we need to thoroughly search feature names with ignore messages
        //to see whether the shortnames are contained as a substring
        Set<String> featureSetSymbolicNames = new HashSet<String>();
        List<String> possibleShortnameFeatures = new LinkedList<String>();
        Set<String> ignoredFeatures = ignoreListFeaturesToXml.keySet();

        for (String feature : ignoredFeatures) {
            if (featureSet.contains(feature)) {
                xmlLines.addAll(ignoreListFeaturesToXml.get(feature));
            } else {
                possibleShortnameFeatures.add(feature);
            }
        }

        for (String feature : featureSet) {
            if (!ignoredFeatures.contains(feature)) {
                boolean found = false;
                int index = 0;
                while (!found && index < possibleShortnameFeatures.size()) {
                    String symbolicName = possibleShortnameFeatures.get(index);
                    if (symbolicName.endsWith(feature)) {
                        xmlLines.addAll(ignoreListFeaturesToXml.get(symbolicName));
                        found = true;
                    }
                    index++;
                }
            }
        }

        String text = "<ignoreList>\n";
        for (String line : xmlLines) {
            text += "    " + line + "\n";
        }
        text += "</ignoreList>";

        PrintWriter writer = new PrintWriter(configFile.getAbsolutePath(), "UTF-8");
        writer.print(text);
        writer.close();
    }

    public static String stringifyXMLNode(Node node) {
        NamedNodeMap fattrs = node.getAttributes();
        String xmlString = "<" + node.getNodeName();
        for (int j=0; j<fattrs.getLength(); j++) {
            Node attribute = fattrs.item(j);
            if (attribute != null) {
                String key = attribute.getNodeName();
                String value = attribute.getNodeValue();
                xmlString = xmlString + " " + key + "=\"" + value + "\"";
            }
        }
        if (node.getTextContent() != null) {
            xmlString += ">" + node.getTextContent() + "</" + node.getNodeName() + ">";
        } else {
            xmlString += "/>";
        }
        return xmlString;
    }
}
