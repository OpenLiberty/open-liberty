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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * A stateful error collector.
 *
 * Different levels can set which feature, nested feature, is being processed, so that errors reported are aggregated correctly
 */
public class XmlErrorCollator {

    public static class Issue implements Comparable<Issue> {
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                     + ((forFeature == null) ? 0 : forFeature.hashCode());
            result = prime * result
                     + ((message == null) ? 0 : message.hashCode());
            result = prime * result
                     + ((shortText == null) ? 0 : shortText.hashCode());
            result = prime * result
                     + ((summary == null) ? 0 : summary.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
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
            Issue other = (Issue) obj;
            if (forFeature == null) {
                if (other.forFeature != null)
                    return false;
            } else if (!forFeature.equals(other.forFeature))
                return false;
            if (message == null) {
                if (other.message != null)
                    return false;
            } else if (!message.equals(other.message))
                return false;
            if (shortText == null) {
                if (other.shortText != null)
                    return false;
            } else if (!shortText.equals(other.shortText))
                return false;
            if (summary == null) {
                if (other.summary != null)
                    return false;
            } else if (!summary.equals(other.summary))
                return false;
            if (type == null) {
                if (other.type != null)
                    return false;
            } else if (!type.equals(other.type))
                return false;
            return true;
        }

        String forFeature;
        String summary;
        String shortText;
        String type;
        String message;

        private final static String FOOTER = "\nFor More Information, visit http://was.pok.ibm.com/xwiki/bin/view/Build/FeatureChecker";

        public void writeXml(PrintWriter pw) {

            pw.println("    <testcase assertions=\"1\" classname=\"" + escape(forFeature) + "\" name=\"" + escape(summary) + "\" status=\"fail\" time=\"1\">\n" +
                       "      <failure message=\"" + escape(forFeature) + "\" type=\"" + type + "\">" + escape(shortText) + "\n" + escape(message) + FOOTER + "</failure>\n" +
                       "    </testcase>");
        }

        @Override
        public int compareTo(Issue o) {
            return message.compareTo(o.message);
        }

        @Override
        public String toString() {
            return "{for:" + forFeature + " shortText:" + shortText + " message:" + message + " summary:" + summary + " type:" + type.toString() + "}";
        }
    }

    //feature->nestedfeature->errorclass->(setof)errortext
    private static Map<String, Map<String, Map<ReportType, Set<Issue>>>> currentIssues = new TreeMap<String, Map<String, Map<ReportType, Set<Issue>>>>();

    private static List<String> processingIssues = new LinkedList<String>();

    public enum ReportType {
        ERROR, WARNING, INFO
    };

    private static String currentFeature;
    private static Set<String> currentFeatures;
    private static String nestedFeature;
    private static Set<String> nestedFeatures;

    private static boolean needToPublish = false;

    public static boolean getNeedToPublish() {
        return needToPublish;
    }

    public static void setCurrentFeature(String feature) {
        currentFeature = feature;
        currentFeatures = null;
    }

    public static void setCurrentFeature(Set<String> feature) {
        currentFeatures = feature;
        currentFeature = null;
    }

    public static void setNestedFeature(String feature) {
        nestedFeature = feature;
        nestedFeatures = null;
    }

    public static void setNestedFeature(Set<String> feature) {
        nestedFeatures = feature;
        nestedFeature = null;
    }

    public static void addProcessingIssue(ReportType type, String issue) {
        processingIssues.add(type.toString() + " " + issue);
    }

    public static void addReport(String resourceWithIssue,
                                 String bundleForResource,
                                 ReportType type,
                                 String summary,
                                 String shortText,
                                 String reason) {
        if (currentFeature == null && currentFeatures == null) {
            throw new RuntimeException("Error attempt to add xml report when feature has not been set.");
        }

        //if nested isnt set, use current as nested.
        if (nestedFeature == null && (nestedFeatures == null || nestedFeatures != null && nestedFeatures.size() == 0)) {
            if (currentFeature != null) {
                //we have a single currentFeature.
                //use it as the nested
                nestedFeature = currentFeature;
            } else {
                //we have a set of currentFeatures.. set the nesteds to null.
                nestedFeatures = null;
                nestedFeature = null;
            }
        }

        Set<String> currentWorkingSet = new HashSet<String>();
        if (currentFeature != null) {
            currentWorkingSet.add(currentFeature);
        } else {
            currentWorkingSet.addAll(currentFeatures);
        }

        for (String cf : currentWorkingSet) {

            if (!currentIssues.containsKey(cf)) {
                currentIssues.put(cf, new TreeMap<String, Map<ReportType, Set<Issue>>>());
            }
            Map<String, Map<ReportType, Set<Issue>>> nestedFeatureIssueMap = currentIssues.get(cf);

            Set<String> nestedFeaturesWorkingSet = XmlErrorCollator.nestedFeatures;
            if (nestedFeaturesWorkingSet == null) {
                nestedFeaturesWorkingSet = new HashSet<String>();
                if (nestedFeature != null) {
                    nestedFeaturesWorkingSet.add(nestedFeature);
                } else {
                    nestedFeaturesWorkingSet.add(cf);
                }
            }

            for (String nested : nestedFeaturesWorkingSet) {
                if (!nestedFeatureIssueMap.containsKey(nested)) {
                    nestedFeatureIssueMap.put(nested, new HashMap<ReportType, Set<Issue>>());
                }
                Map<ReportType, Set<Issue>> issuesByTypeForNestedFeature = nestedFeatureIssueMap.get(nested);

                if (!issuesByTypeForNestedFeature.containsKey(type)) {
                    issuesByTypeForNestedFeature.put(type, new TreeSet<Issue>());
                }

                Issue i = new Issue();
                i.forFeature = nested;
                i.summary = summary;
                i.type = type.toString();
                i.shortText = shortText;
                i.message = reason;

                issuesByTypeForNestedFeature.get(type).add(i);
            }
        }
    }

    private static boolean isIgnored(String feature, String message) {
        return GlobalConfig.isIgnored(feature, message);
    }

    public static void dumpRecordsToSysout() {
        for (Map.Entry<String, Map<String, Map<ReportType, Set<Issue>>>> feature : currentIssues.entrySet()) {
            System.out.println("Issues for Feature: " + feature.getKey());
            Map<ReportType, Set<Issue>> localIssues = feature.getValue().get(feature.getKey());
            if (localIssues != null) {
                for (Map.Entry<ReportType, Set<Issue>> errors : localIssues.entrySet()) {
                    for (Issue error : errors.getValue()) {
                        String message = errors.getKey().toString() + "  " + error.message;
                        if (!isIgnored(feature.getKey(), message) && !isIgnored(feature.getKey(), error.shortText)) {
                            System.out.println("   " + message);
                        }
                    }
                }
            }

            for (Map.Entry<String, Map<ReportType, Set<Issue>>> nestedFeature : feature.getValue().entrySet()) {
                if (!nestedFeature.getKey().equals(feature.getKey())) {
                    System.out.println(" Nested feature " + nestedFeature.getKey() + " has issues.");
//					System.out.println(" Issues with nested feature: "+nestedFeature.getKey());
//					for(Map.Entry<ReportType,Set<String>> errors : nestedFeature.getValue().entrySet()){
//						for(String error: errors.getValue()){
//							String message = errors.getKey().toString()+"  "+error;
//							if(!isIgnored(nestedFeature.getKey(), message)){
//								System.out.println("   "+message);
//							}
//						}
//					}
                }
            }
        }
        System.out.println("Processing Issues: ");
        for (String s : processingIssues) {
            System.out.println(" " + s);
        }
    }

    public static class TestSuite {
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                     + ((forFeature == null) ? 0 : forFeature.hashCode());
            result = prime * result
                     + ((issues == null) ? 0 : issues.hashCode());
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
            TestSuite other = (TestSuite) obj;
            if (forFeature == null) {
                if (other.forFeature != null)
                    return false;
            } else if (!forFeature.equals(other.forFeature))
                return false;
            if (issues == null) {
                if (other.issues != null)
                    return false;
            } else if (!issues.equals(other.issues))
                return false;
            return true;
        }

        String forFeature;
        Collection<Issue> issues = new ArrayList<Issue>();

        TestSuite(String feature) {
            forFeature = feature;
        }

        void add(Issue i) {
            issues.add(i);
        }

        public void writeXml(PrintWriter pw, String configName) {
            if (issues.isEmpty())
                return;

            pw.println("  <testsuite hostname=\"featureVerifier\" id=\"featureVerifier." + configName + "." + forFeature + "\" \n" +
                       "             name=\"Feature verification (" + configName + ") - " + forFeature + "\" package=\"featureverify." + forFeature + "\" time=\"1\" \n" +
                       "             skipped=\"0\" tests=\"" + issues.size() + "\" failures=\"" + issues.size() + "\" errors=\"0\">");

            for (Issue i : issues) {
                i.writeXml(pw);
            }
            pw.println("  </testsuite>");
        }
    }

    public static class TestSuites {
        String configName = "";
        Collection<TestSuite> suites = new ArrayList<TestSuite>();

        TestSuites(String configName) {
            this.configName = configName;
        }

        void add(TestSuite t) {
            suites.add(t);
        }

        boolean hasDataToWrite() {
            if (suites.isEmpty())
                return false;
            for (TestSuite s : suites) {
                if (!s.issues.isEmpty())
                    return true;
            }
            return false;
        }

        public void writeXml(PrintWriter pw) {
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                       "<testsuites>");
            for (TestSuite t : suites) {
                t.writeXml(pw, configName);
            }
            pw.println("</testsuites>");
        }
    }

    public static void newDumpRecordsToXML(File xml, File junit) throws IOException {
        String configName = junit.getParentFile().getName();
        TestSuites ts = new TestSuites(configName);
        PrintWriter pw = new PrintWriter(new FileWriter(xml));
        pw.println("<report>");
        pw.println(" <features>");

        //collate feature issues back by feature name..
        Map<String, Map<ReportType, Set<Issue>>> collated = new HashMap<String, Map<ReportType, Set<Issue>>>();
        for (Map.Entry<String, Map<String, Map<ReportType, Set<Issue>>>> feature : currentIssues.entrySet()) {
            String featureName = feature.getKey();
            Map<String, Map<ReportType, Set<Issue>>> featureIssues = feature.getValue();
            for (Map.Entry<String, Map<ReportType, Set<Issue>>> issueData : featureIssues.entrySet()) {
                String reportedAgainstFeature = issueData.getKey();
                Map<ReportType, Set<Issue>> reports = issueData.getValue();
                for (Map.Entry<ReportType, Set<Issue>> errors : reports.entrySet()) {
                    for (Issue error : errors.getValue()) {
                        String message = errors.getKey().toString() + "  " + error.message;
                        String ignoreKey = featureName;
                        if (!reportedAgainstFeature.equals(featureName)) {
                            ignoreKey = reportedAgainstFeature;
                        }
                        if (!isIgnored(ignoreKey, message) && !isIgnored(ignoreKey, error.shortText)) {
                            if (!GlobalConfig.ApiSpiReviewMode || error.shortText.contains("API_SPI_REVIEW")) {
                                if (!collated.containsKey(reportedAgainstFeature)) {
                                    collated.put(reportedAgainstFeature, new HashMap<ReportType, Set<Issue>>());
                                }
                                if (!collated.get(reportedAgainstFeature).containsKey(errors.getKey())) {
                                    collated.get(reportedAgainstFeature).put(errors.getKey(), new HashSet<Issue>());
                                }
                                //remember this error for output.
                                collated.get(reportedAgainstFeature).get(errors.getKey()).add(error);

                                //if this error wasn't for the feature we are processing, also add a 'nested' warning
                                if (!reportedAgainstFeature.equals(featureName)) {
//                                    Issue nested = new Issue();
//                                    nested.forFeature = reportedAgainstFeature;
//                                    nested.type = ReportType.INFO.toString();
//                                    nested.summary = "Problem with nested feature " + reportedAgainstFeature;
//                                    nested.shortText = "[NESTED_ISSUE " + reportedAgainstFeature + "]";
//                                    nested.message = "Feature " + reportedAgainstFeature + " has issues, this feature (" + featureName + ") is affected because it contains "
//                                                     + reportedAgainstFeature + " within it's heirarchy";
//                                    if (!collated.containsKey(featureName)) {
//                                        collated.put(featureName, new HashMap<ReportType, Set<Issue>>());
//                                    }
//                                    if (!collated.get(featureName).containsKey(ReportType.INFO)) {
//                                        collated.get(featureName).put(ReportType.INFO, new HashSet<Issue>());
//                                    }
//                                    collated.get(featureName).get(ReportType.INFO).add(nested);
                                }
                            }
                        }
                    }
                }
            }
        }

        //now process all remembered / not-ignored errors.. out to xml/junit.
        if (processingIssues.isEmpty()) {
            for (Entry<String, Map<ReportType, Set<Issue>>> feature : collated.entrySet()) {
                TestSuite t = new TestSuite(feature.getKey());
                ts.add(t);
                if (!(GlobalConfig.ignoreMissingFeatures && GlobalConfig.isFeatureToBeIgnoredInRuntime(feature.getKey()))) {
                    pw.println("   <feature name=\"" + feature.getKey() + "\">");
                    for (Map.Entry<ReportType, Set<Issue>> issue : feature.getValue().entrySet()) {
                        for (Issue error : issue.getValue()) {
                            String message = issue.getKey().toString() + "  " + error.message;
                            pw.println("    <issue level=\"" + issue.getKey().toString() + "\" short=\"" + error.shortText + "\">"
                                       + message + "</issue>");
                            //we don't report info issues via junit.. otherwise defects get raised for no reason.
                            if (issue.getKey() != ReportType.INFO) {
                                t.add(error);
                                needToPublish = true;
                            }
                        }
                    }
                    pw.println("   </feature>");
                }
            }
        }

        pw.println(" </features>");
        pw.println(" <processingIssues>");
        TestSuite t = new TestSuite("Internal Processing Issues for Feature Checker during config " + configName);
        ts.add(t);
        for (String s : processingIssues) {
            needToPublish = true;
            pw.println("  <issue>" + s + "</issue>");
            Issue i = new Issue();
            i.forFeature = "Internal Issue";
            i.summary = "Internal Issue";
            i.shortText = "[INTERNAL_ERROR]";
            i.type = ReportType.ERROR.toString();
            i.message = "An internal error was reported during processing for config " + configName
                        + " the other issues reported should be taken with a large pinch of salt until this issue is resolved :: " + s;
            t.add(i);
        }
        pw.println(" </processingIssues>");

        Map<String, Set<Pattern>> unused = GlobalConfig.getUnusedMessageIgnores();
        if (!unused.isEmpty() && !GlobalConfig.ApiSpiReviewMode && processingIssues.isEmpty()) {
            pw.println(" <unusedIgnores>");
            TestSuite z = new TestSuite("Unused ignores for config " + configName);
            ts.add(z);
            for (Map.Entry<String, Set<Pattern>> f : unused.entrySet()) {
                if (!GlobalConfig.ignoreUnusedsForThisFeature(f.getKey())) {
                    // Don't needToPublish unused ignores, becoming errors only seen running locally
                    // When devs change open-liberty api/spi, ignore fixes need to be delivered first. Delivering ignores without
                    // corresponding api/spi changes will result in unused ignores, so treating unused ignores local only errors
                    // keeps the build green between the time ignores and api/spi changes are delivered.
                    for (Pattern p : f.getValue()) {
                        pw.println("  <message feature=\"" + f.getKey() + "\">" + p.toString() + "</message>");
                        Issue i = new Issue();
                        i.forFeature = "Unused ignore for feature " + f.getKey();
                        i.summary = "Unused ignore";
                        i.shortText = "[UNUSED_IGNORE]";
                        i.type = ReportType.ERROR.toString();
                        i.message = "This ignore pattern was not used to suppress an error for this config. Has the issue been fixed? perhaps this ignore is no longer required? "
                        + p.toString();
                        z.add(i);
                    }
                }
            }
            pw.println(" </unusedIgnores>");
        }
        pw.println("</report>");
        pw.close();

        if (ts.hasDataToWrite()) {
            PrintWriter pw2 = new PrintWriter(new FileWriter(junit));
            ts.writeXml(pw2);
            pw2.close();
        }
    }

    private static String escape(String forFeature) {
        return StringEscapeUtils.escapeXml(forFeature);
    }
}
