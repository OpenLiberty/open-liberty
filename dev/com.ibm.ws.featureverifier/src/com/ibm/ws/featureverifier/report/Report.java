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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.apache.aries.util.VersionRange;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Version;

import aQute.bnd.osgi.Processor;

/**
 * Kept as a single file to make it easier to share..
 *
 * If we decide to check this thing in, then we could
 * split out the various subclasses etc.
 *
 * "Much that once tWAS is lost, for none now remain who remember it."
 */
public class Report {

    public final static String fileSep = Pattern.quote(File.separator);

    private final FileUtils fileUtils;

    private final static boolean useCache = false;

    // Helper method for get the file content
    static List<String> fileToLines(File filename) {
        List<String> lines = new LinkedList<String>();
        String line = "";
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(filename));
            while ((line = in.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return lines;
    }

    private static class VersionedBundle implements Comparable<VersionedBundle> {
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                     + ((symbolicName == null) ? 0 : symbolicName.hashCode());
            result = prime * result
                     + ((vrString == null) ? 0 : vrString.hashCode());
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
            VersionedBundle other = (VersionedBundle) obj;
            if (symbolicName == null) {
                if (other.symbolicName != null)
                    return false;
            } else if (!symbolicName.equals(other.symbolicName))
                return false;
            if (vrString == null) {
                if (other.vrString != null)
                    return false;
            } else if (!vrString.equals(other.vrString))
                return false;
            return true;
        }

        final String vrString;
        final VersionRange vr;
        final String symbolicName;

        public VersionedBundle(String symbolicName, String versionRange) {
            vrString = versionRange;
            vr = new VersionRange(versionRange);
            this.symbolicName = symbolicName;
        }

        @Override
        public int compareTo(VersionedBundle o) {
            if (o == null)
                return -1;

            if (symbolicName.equals(o.symbolicName)) {
                return vrString.compareTo(o.vrString);
            } else {
                return symbolicName.compareTo(o.symbolicName);
            }
        }
    }

    private static class InvalidFeatureException extends Exception {

        /**  */
        private static final long serialVersionUID = -1586275845855237346L;
    }

    private static class FeatureFile {
        private static final String FEATURE_KIND = "kind";
        private static final String BETA_FEATURE = "beta";

        private boolean isBeta = false;

        public FeatureFile(File baseDir, String fileName) {
            File f = new File(baseDir, fileName);
            if (!f.exists()) {
                System.out.println("Could not find feature file " + f.getAbsolutePath());
                isBeta = false;
                return;
            }

            BufferedReader reader = null;
            try {
                InputStream is = new FileInputStream(f);
                reader = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(FEATURE_KIND)) {
                        String value = line.substring(FEATURE_KIND.length() + 1);
                        value = value.trim();
                        if (value.equalsIgnoreCase(BETA_FEATURE)) {
                            isBeta = true;
                            return;
                        }
                    }
                }

            } catch (IOException ex) {
                ex.printStackTrace();
                isBeta = false;
            } finally {
                if (reader != null)
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }

        boolean isBeta() {
            return isBeta;
        }
    }

    private static class FeatureInfo implements Comparable<FeatureInfo> {

        final File baseDir;
        final String featureSourceData;
        final String symbolicName;
        final boolean singleton;
        final String visibility;
        final boolean bndBased;
        final Set<String> metatypes;
        final Set<VersionedBundle> bundles;

        private NameValuePair getDetailsFromManifest(File manifest, Set<VersionedBundle> bundles, Map<String, Set<String>> features) {
            NameValuePair name = null;
            try {
                InputStream is = new FileInputStream(manifest);
                try {
                    Manifest m = ManifestProcessor.parseManifest(is);
                    String nameStr = m.getMainAttributes().getValue("Subsystem-SymbolicName");
                    name = ManifestHeaderProcessor.parseBundleSymbolicName(nameStr);

                    String contentString = m.getMainAttributes().getValue("Subsystem-Content");
                    Map<String, Map<String, String>> content = ManifestHeaderProcessor.parseImportString(contentString);
                    for (Entry<String, Map<String, String>> contentItem : content.entrySet()) {
                        if (!contentItem.getValue().containsKey("type") || contentItem.getValue().get("type").equals("osgi.bundle")) {
                            String version = contentItem.getValue().get("version");
                            if (version == null || version.trim().equals("")) {
                                version = "0.0.0";
                            }
                            VersionedBundle vr = new VersionedBundle(contentItem.getKey(), version);
                            bundles.add(vr);
                        } else if (contentItem.getValue().get("type").equals("osgi.subsystem.feature")) {
                            String tolerates = contentItem.getValue().get("ibm.tolerates:");
                            String preferred = contentItem.getKey();
                            Set<String> tolerated = new TreeSet<String>();
                            if (tolerates != null) {
                                String parts[] = tolerates.split(",");
                                String base = preferred.substring(0, preferred.lastIndexOf('-'));
                                for (String part : parts) {
                                    tolerated.add(base + "-" + part);
                                }
                            }
                            features.put(preferred, Collections.unmodifiableSet(tolerated));
                        }
                    }
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            } catch (IOException io) {
                System.err.println("Manifest " + manifest.getAbsolutePath());
                io.printStackTrace();
            }
            return name;
        }

        public boolean matchesBundle(String name, Version v) {
            for (VersionedBundle vb : bundles) {
                if (vb.symbolicName.equals(name)) {
                    if (vb.vr.matches(v)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public FeatureInfo(File baseDir, String source, Set<String> metatypes) throws InvalidFeatureException {
            this.baseDir = baseDir;
            Set<VersionedBundle> bundles = new TreeSet<VersionedBundle>();
            Map<String, Set<String>> features = new TreeMap<String, Set<String>>();
            String src = null;
            if (source.endsWith(".feature")) {
                String parts[] = source.split(Report.fileSep);
                File project = new File(baseDir, parts[0]);
                File build = new File(project, "build");
                File manifest = new File(build, "subsystem.mf");
                if (manifest.exists() && manifest.isFile()) {
                    bndBased = true;
                    src = parts[0] + File.separator + "build" + File.separator + "subsystem.mf";
                    NameValuePair parsedName = getDetailsFromManifest(manifest, bundles, features);
                    symbolicName = parsedName.getName();
                    String visibilityStr = null;
                    String singletonStr = null;
                    if (parsedName.getAttributes() != null) {
                        visibilityStr = parsedName.getAttributes().get("visibility:");
                        singletonStr = parsedName.getAttributes().get("singleton:");
                    }
                    visibility = visibilityStr == null ? "private" : visibilityStr;
                    singleton = singletonStr == null ? false : Boolean.valueOf(singletonStr);
                } else {
                    // manifest will not be produced for kind=beta. Check if that's the case here
                    FeatureFile feature = new FeatureFile(baseDir, source);
                    if (!feature.isBeta()) {
                        throw new InvalidFeatureException();
                    } else {
                        throw new InvalidFeatureException();
                    }
                }
            } else {
                src = source;
                bndBased = false;
                NameValuePair parsedName = getDetailsFromManifest(new File(baseDir, source), bundles, features);
                symbolicName = parsedName.getName();
                String visibilityStr = null;
                String singletonStr = null;
                if (parsedName.getAttributes() != null) {
                    visibilityStr = parsedName.getAttributes().get("visibility:");
                    singletonStr = parsedName.getAttributes().get("singleton:");
                }
                visibility = visibilityStr == null ? "private" : visibilityStr;
                singleton = singletonStr == null ? false : Boolean.valueOf(singletonStr);
            }

            if (symbolicName == null) {
                throw new IllegalStateException(source);
            }
            this.featureSourceData = src;
            this.metatypes = Collections.unmodifiableSet(metatypes);

            this.bundles = Collections.unmodifiableSet(bundles);

        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                     + ((symbolicName == null) ? 0 : symbolicName.hashCode());
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
            FeatureInfo other = (FeatureInfo) obj;
            if (symbolicName == null) {
                if (other.symbolicName != null)
                    return false;
            } else if (!symbolicName.equals(other.symbolicName))
                return false;
            return true;
        }

        @Override
        public int compareTo(FeatureInfo o) {
            if (o == null)
                return -1;
            else
                return symbolicName.compareTo(o.symbolicName);
        }

    }

    private void findFiles(File base, File currentDir, Map<Pattern, Set<String>> results) {
        int count = 0;
        int max = 0;
        int percent = 0;
        if (base.equals(currentDir)) {
            max = count = currentDir.listFiles().length;
            if (useCache) {
                //see if we can reload from the cache..
                File cache = new File(base, ".fcache");
                if (cache.exists() && cache.isFile()) {
                    System.out.print("Restructuring dilithium crystals using " + currentDir.getPath() + " [");
                    Properties p = new Properties();
                    try {
                        FileInputStream fis = new FileInputStream(cache);
                        try {
                            p.load(fis);
                            max = count = p.size();
                            Map<Pattern, Set<String>> cacheResults = new HashMap<Pattern, Set<String>>();
                            int pkey = 0;
                            while (p.containsKey("Pattern-" + pkey)) {
                                String patternString = p.getProperty("Pattern-" + pkey);
                                String countString = p.getProperty("Pattern-" + pkey + ".count");
                                String prefixString = p.getProperty("Pattern-" + pkey + ".prefix");
                                Pattern pattern = null;
                                for (Pattern resultPattern : results.keySet()) {
                                    if (resultPattern.toString().equals(patternString)) {
                                        pattern = resultPattern;
                                        break;
                                    }
                                }
                                if (pattern == null) {
                                    System.out.println("Unknown pattern " + patternString);
                                    pattern = Pattern.compile(patternString);
                                }
                                Set<String> values = new TreeSet<String>();
                                int pmax = Integer.parseInt(countString);
                                for (int i = 0; i < pmax; i++) {
                                    values.add(p.getProperty(prefixString + "." + i));
                                    if (i == 0)
                                        count -= 4;
                                    else
                                        count--;
                                    int newPercent = (int) ((50.0 / max) * count);
                                    if (newPercent != percent) {
                                        System.out.print(".");
                                        System.out.flush();
                                        percent = newPercent;
                                    }
                                }
                                cacheResults.put(pattern, values);
                                pkey++;
                            }
                            //now all of property file is loaded without io error etc.. transfer results to real map & return
                            results.putAll(cacheResults);
                            System.out.println("]");
                            return;
                        } catch (IOException io) {
                        } finally {
                            if (fis != null) {
                                try {
                                    fis.close();
                                } catch (IOException e) {
                                }
                            }
                        }
                    } catch (FileNotFoundException e) {
                    }
                    System.out.println("]");
                } else {
                    System.out.println("Warp Engines are offline.");
                }
            }
            //still here? then the cache either didn't exist.. or was bad.
            System.out.print("Engaging sector search using maximum sensor sweep at " + currentDir.getPath() + " [");
        }

        for (File f : currentDir.listFiles()) {
            if (count > 0) {
                count--;
                int newPercent = (int) ((50.0 / max) * count);
                if (newPercent != percent) {
                    System.out.print(".");
                    System.out.flush();
                    percent = newPercent;
                }
            }

            if (f.isDirectory()) {
                //skip .dirs =)
                if (!f.getName().startsWith(".")) {
                    findFiles(base, f, results);
                }
            } else {
                Path basePath = Paths.get(base.toURI());
                Path filePath = Paths.get(f.toURI());
                Path relativePath = basePath.relativize(filePath);
                String fPathAsString = relativePath.toString();

                for (Entry<Pattern, Set<String>> e : results.entrySet()) {
                    if (e.getKey().matcher(fPathAsString).matches()) {
                        e.getValue().add(fPathAsString);
                        System.out.println("Matched file " + filePath + " using matcher " + e.getKey().toString());
                    }
                }
            }
        }

        if (base.equals(currentDir)) {
            System.out.println("]");
            if (useCache) {
                System.out.print("Realigning main deflector dish [");
                //update the cache, because we didn't load from it.
                Properties p = new Properties();
                char prefix = 'a';
                int pcount = 0;
                count = max = results.size();
                for (Entry<Pattern, Set<String>> result : results.entrySet()) {
                    count--;
                    int newPercent = (int) ((50.0 / max) * count);
                    if (newPercent != percent) {
                        System.out.print(".");
                        System.out.flush();
                        percent = newPercent;
                    }
                    Pattern pattern = result.getKey();
                    Set<String> values = result.getValue();
                    p.setProperty("Pattern-" + pcount, pattern.toString());
                    p.setProperty("Pattern-" + pcount + ".count", "" + values.size());
                    p.setProperty("Pattern-" + pcount + ".prefix", "" + prefix);
                    int vcount = 0;
                    for (String value : values) {
                        p.setProperty(prefix + "." + vcount, value);
                        vcount++;
                    }
                    pcount++;
                    prefix++;
                }
                File cache = new File(base, ".fcache");
                try {
                    FileOutputStream fos = new FileOutputStream(cache);
                    try {
                        p.store(fos, "file cache for api/spi review");
                    } catch (IOException io) {
                    } finally {
                        try {
                            fos.close();
                        } catch (IOException io) {
                        }
                    }
                } catch (FileNotFoundException e) {
                }
                System.out.println("]");
            }
        }
    }

    private void populateMaps(File baseDir, Set<String> featurePaths, Set<String> metatypePaths, Map<String, FeatureInfo> nameToFeatureMap,
                              Map<String, FeatureInfo> sourceToFeatureMap) {
        int count = featurePaths.size();;
        int max = count;
        int percent = 0;
        System.out.print("Populating feature maps [");

        // Create the list of all features
        Set<FeatureInfo> allFeatures = new TreeSet<FeatureInfo>();
        allFeatures.addAll(nameToFeatureMap.values());
        Set<String> metatypePathsToProcess = new TreeSet<String>(metatypePaths);
        for (String featurePath : featurePaths) {

            count--;
            int newPercent = (int) ((50.0 / max) * count);
            if (newPercent != percent) {
                System.out.print(".");
                System.out.flush();
                percent = newPercent;
            }

            Set<String> metaTypesForFeature = new TreeSet<String>();
            //1st dir of featurePath is the project dir..
            String[] parts = featurePath.split(fileSep);
            Set<String> toRemove = new HashSet<String>();
            for (String metatypePath : metatypePathsToProcess) {
                String[] mparts = metatypePath.split(fileSep);
                if (parts[0].equals(mparts[0])) {
                    toRemove.add(metatypePath);
                    metaTypesForFeature.add(metatypePath);
                    //System.out.println(" recognized "+metatypePath+" as part of "+featurePath);
                }
            }
            metatypePathsToProcess.removeAll(toRemove);

            try {
                FeatureInfo fi = new FeatureInfo(baseDir, featurePath, metaTypesForFeature);
                allFeatures.add(fi);
            } catch (InvalidFeatureException ex) {
                // OK
            }
        }
        System.out.println("]");

        if (metatypePathsToProcess.size() > 0) {
            //these ones we need to match by their bundle owners.. tricksy.
            for (String path : metatypePathsToProcess) {
                String[] parts = path.split(fileSep);
                File project = new File(baseDir, parts[0]);

                if (project.listFiles() != null) {
                    for (File f : project.listFiles()) {
                        if (f.getName().equals("bnd.bnd")) {

                            try (Processor p = new Processor()) {
                                p.addProperties(f);

                                String name = p.getProperty("Bundle-SymbolicName");
                                String version = p.getProperty("Bundle-Version");
                                if (version == null) {
                                    System.out.println("WARNING: version is null for bnd.bnd file: " + f.getAbsolutePath());
                                } else {
                                    version = version.replace("${liberty.bundle.micro.version}", System.getProperty("micro.version"));
                                    version = version.replace(".${buildLabel}", "");
                                    version = version.replace(".${if;${driver;gradle};${version.qualifier};eclipse}", "");
                                    if (version.contains("${")) {
                                        throw new IllegalStateException("The bundle version can not be parsed: " + version);
                                    }
                                    Version v = Version.parseVersion(version);
                                    Set<FeatureInfo> rebuilt = new HashSet<FeatureInfo>();
                                    for (FeatureInfo fi : allFeatures) {
                                        if (fi.matchesBundle(name, v)) {
                                            Set<String> newMetas = new TreeSet<String>(fi.metatypes);
                                            newMetas.add(path);
                                            try {
                                                rebuilt.add(new FeatureInfo(fi.baseDir, fi.featureSourceData, newMetas));
                                            } catch (InvalidFeatureException ex) {
                                                // OK
                                            }
                                        }
                                    }
                                    allFeatures.removeAll(rebuilt);
                                    allFeatures.addAll(rebuilt);
                                }
                            } catch (Exception io) {
                                io.printStackTrace();
                            }

                        }
                    }
                } else {
                    if (new File(project, "build.xml").exists())
                        System.out.println("Unable to match a jar for " + path);
                }
            }
        }

        for (FeatureInfo fi : allFeatures) {
            if (!excludedFeatures.contains(fi.symbolicName)) {
                nameToFeatureMap.put(fi.symbolicName, fi);
                sourceToFeatureMap.put(fi.featureSourceData, fi);
            }
        }
    }

    /**
     * These are all auto generated based on information in other features. The changes will show up in the report under the
     * other features, so no need to review these.
     */
    private final String[] editionFeatures = { "com.ibm.websphere.appserver.baseBundle",
                                               "com.ibm.websphere.appserver.libertyCoreBundle",
                                               "com.ibm.websphere.appserver.ndControllerBundle",
                                               "com.ibm.websphere.appserver.ndMemberBundle",
                                               "com.ibm.websphere.appserver.zosBundle",
                                               "com.ibm.websphere.appserver.zosCoreBundle" };

    private final List<String> excludedFeatures = new ArrayList<String>(Arrays.asList(editionFeatures));

    private void buildIndexes(File repoDir, Map<String, FeatureInfo> nameToFeatureMap, Map<String, FeatureInfo> sourceToFeatureMap) {
        Map<Pattern, Set<String>> patternsToSeek = new HashMap<Pattern, Set<String>>();
        Set<String> featureSources = new TreeSet<String>();

        Pattern oldManifests = Pattern.compile("(?!.*(test|bvt|fat|build).*).*" + fileSep + "features" + fileSep + "(?!.*(test|bvt|fat|build).*).*.mf");
        patternsToSeek.put(oldManifests, new TreeSet<String>());

        Pattern manifests = Pattern.compile("build.image" + fileSep + "wlp" + fileSep + "lib" + fileSep + "features" + fileSep
                                            + "(?!.*(protected\\.|test|bvt|fat|build).*).*.mf");
        patternsToSeek.put(manifests, new TreeSet<String>());

        // Gather BND style features
        Pattern bnds = Pattern.compile("(?!.*(test|bvt|fat|build).*).*\\.feature");
        patternsToSeek.put(bnds, new TreeSet<String>());

        Pattern metatypes = Pattern.compile("(?!.*(test|bvt|fat|build).*).*" + fileSep + "metatype" + fileSep + "(?!.*(test|bvt|fat|build).*).*.xml");
        patternsToSeek.put(metatypes, new TreeSet<String>());

        findFiles(repoDir, repoDir, patternsToSeek);

        featureSources.addAll(patternsToSeek.get(bnds));
        featureSources.addAll(patternsToSeek.get(oldManifests));
        featureSources.addAll(patternsToSeek.get(manifests));

        populateMaps(repoDir, featureSources, patternsToSeek.get(metatypes), nameToFeatureMap, sourceToFeatureMap);
    }

    private String getFileLog(FeatureInfo fi) {
        String logpath = "other.html";
        String path = fi.featureSourceData;
        String parts[] = path.split(fileSep);
        String project = parts[0];
        if ("build.image".equals(project)) {
            // bnd style feature from build.image
            project = parts[4];
        }
        try {
            if (project.matches("com.ibm.ws.jms.*|com.ibm.ws.messaging.*|javax.jms.*")) {
                logpath = "messaging.html";
            } else if (project.matches("com.ibm.ws.transport.iiop.*|com.ibm.ws.security.csiv2.*|org.apache.yoko.*")) {
                logpath = "orb.html";
            } else if (project.matches("com.ibm.ws.wsecurity.*|javax.jaspic.*")) {
                logpath = "security.html";
            } else if (project.matches("com.ibm.ws.xlsp.*|com.ibm.ws.jaxws.*")) {
                logpath = "jaxws.html";
            } else if (project.matches("openwebbeans-.*")) {
                logpath = "cdi.html";
            } else if (project.startsWith("com.ibm.ws.")) {
                logpath = project.substring("com.ibm.ws.".length());
                if (logpath.indexOf('.') != -1) {
                    logpath = logpath.substring(0, logpath.indexOf('.'));
                }
                logpath += ".html";
            } else if (project.startsWith("com.ibm.websphere.appserver.")) {
                // BND based feature
                logpath = project.substring("com.ibm.websphere.appserver.".length());
                // Strip off version info
                if (logpath.indexOf('-') != -1) {
                    logpath = logpath.substring(0, logpath.lastIndexOf('-'));
                }

                if (logpath.startsWith("adminCenter")) {
                    logpath = "adminCenter";
                }

                logpath += ".html";
            } else if (project.startsWith("javax.j2ee.")) {
                logpath = project.substring("javax.j2ee.".length());
                if (logpath.indexOf('.') != -1) {
                    logpath = logpath.substring(0, logpath.indexOf('.'));
                }
                logpath += ".html";
            } else if (project.startsWith("javax.")) {
                logpath = project.substring("javax.".length());
                if (logpath.indexOf('.') != -1) {
                    logpath = logpath.substring(0, logpath.indexOf('.'));
                }
                logpath += ".html";
            } else {
                logpath = "other.html";
            }
        } catch (StringIndexOutOfBoundsException e) {
            System.out.println("error '" + project + "'");
            throw e;
        }
        return logpath;
    }

    private void reportRemovedFeatures(Map<String, FeatureInfo> baseNameToFeatureMap, Map<String, FeatureInfo> newNameToFeatureMap,
                                       Map<String, File> logFilesUsed) {
        System.out.println("Looking for Quarks:");
        for (Entry<String, FeatureInfo> feature : baseNameToFeatureMap.entrySet()) {
            if (!newNameToFeatureMap.containsKey(feature.getKey())) {
                System.out.println(" Detected missing feature " + feature.getKey() + " previously at " + feature.getValue().featureSourceData);

                fileUtils.copyOldFile(feature.getValue().featureSourceData);

                String logFileName = getFileLog(feature.getValue());
                PrintWriter output = fileUtils.getLog(logFileName, logFilesUsed);

                output.println("<div class=\"feature\">");
                output.println("  <div class=\"oldfilename\">" + feature.getValue().featureSourceData + "</div>");
                output.println("  <div class=\"symbolicName\">" + feature.getValue().symbolicName + "</div>");
                output.println("  <div class=\"visibility\">" + feature.getValue().visibility + "</div>");
                output.println("  <div class=\"singleton\">" + feature.getValue().singleton + "</div>");
                output.println("  <div class=\"bndBased\">" + feature.getValue().bndBased + "</div>");
                output.println("  <div class=\"AMD\">DELETED</div>");
                output.println("</div>");

                MetatypeDiff mtd = new MetatypeDiff(fileUtils, feature.getValue().baseDir, feature.getValue().metatypes, null, null);

                mtd.reportChanges(output);

                output.flush();
            }
        }

    }

    private void reportAddedFeatures(Map<String, FeatureInfo> baseNameToFeatureMap, Map<String, FeatureInfo> newNameToFeatureMap, FeatureCheckerOutput fco,
                                     Map<String, File> logFilesUsed) {
        System.out.println("Looking for Singularities:");
        for (Entry<String, FeatureInfo> feature : newNameToFeatureMap.entrySet()) {
            if (!baseNameToFeatureMap.containsKey(feature.getKey())) {
                System.out.println(" Detected added feature " + feature.getKey() + " present at " + feature.getValue().featureSourceData);

                fileUtils.copyNewFile(feature.getValue().featureSourceData);

                String logFileName = getFileLog(feature.getValue());
                PrintWriter output = fileUtils.getLog(logFileName, logFilesUsed);

                output.println("<div class=\"feature\">");
                output.println("  <div class=\"filename\">" + feature.getValue().featureSourceData + "</div>");
                output.println("  <div class=\"symbolicName\">" + feature.getValue().symbolicName + "</div>");
                output.println("  <div class=\"visibility\">" + feature.getValue().visibility + "</div>");
                output.println("  <div class=\"singleton\">" + feature.getValue().singleton + "</div>");
                output.println("  <div class=\"bndBased\">" + feature.getValue().bndBased + "</div>");
                output.println("  <div class=\"AMD\">ADDED</div>");

                //new features should have an entry in the junit xmls ..
                if (fco.hasProblemsForFeature(feature.getKey())) {
                    output.println("  <div class=\"checkerdetail\">");

                    System.out.println(" - Feature checker detailed information : ");
                    for (String s : fco.getNewProblems(feature.getKey())) {
                        output.println("    <div class=\"detailinfo\">");
                        System.out.println(s);
                        //we have to escape s.
                        String escaped = s.replaceAll("<", "&lt;");
                        escaped = escaped.replaceAll(">", "&gt;");
                        output.println(escaped);
                        output.println("    </div>");
                    }
                    output.println("  </div>");
                }
                output.println("</div>");

                MetatypeDiff mtd = new MetatypeDiff(fileUtils, null, null, feature.getValue().baseDir, feature.getValue().metatypes);

                mtd.reportChanges(output);

                output.flush();
            }
        }
    }

    //not a great routine, it doesn't check for locks etc..
    //but it's not too important.
    private void cleanupDir(File dir, String pattern) {
        if (dir.listFiles() != null) {
            for (File old : dir.listFiles()) {
                if (pattern == null || old.getName().endsWith(pattern)) {
                    if (old.isDirectory()) {
                        cleanupDir(old, pattern);
                    }
                    old.delete();
                }
            }
        }
    }

    public String getCurrentDate() {
        DateFormat df = new SimpleDateFormat(ReportConstants.DATE_FORMAT);
        return df.format(new Date());
    }

    public String getBaselineDate() {
        try {
            File timestamp = new File(fileUtils.getBaseDir(), ReportConstants.TIMESTAMP_FILE);
            if (!timestamp.exists()) {
                return "Unknown";
            }

            try (BufferedReader br = new BufferedReader(new FileReader(timestamp))) {
                String time = br.readLine();
                DateFormat df = new SimpleDateFormat(ReportConstants.DATE_FORMAT);

                return df.format(new Date(Long.valueOf(time)));
            }
        } catch (IOException ex) {
            return "Unknown";
        }
    }

    public String getPreviousReviewDates() {
        StringBuffer dates = new StringBuffer();
        File[] reviewed = fileUtils.getBaseDir().listFiles(new ReviewFileFilter());
        for (File f : reviewed) {
            String name = f.getName();
            name = name.substring(8, name.length() - 5);
            DateFormat dateTime = new SimpleDateFormat(ReportConstants.DATE_TIME_FORMAT);
            DateFormat date = new SimpleDateFormat(ReportConstants.DATE_FORMAT);
            try {
                dates.append(date.format(dateTime.parse(name)));
                dates.append(", ");
            } catch (ParseException ex) {
                System.out.println("ERROR: Couldn't parse date: " + name);
            }
        }
        if (dates.length() > 0) {
            dates.deleteCharAt(dates.length() - 2);
        }
        return dates.toString();
    }

    private void closeLogs(Map<String, File> logFilesUsed) {
        for (File log : logFilesUsed.values()) {
            try {
                FileWriter fw = new FileWriter(log, true);
                PrintWriter output = new PrintWriter(fw);
                output.println("</body></html>");
                output.close();
            } catch (IOException e) {
                System.err.println("Unable to write to " + log.getAbsolutePath());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        Map<String, File> dummy = new HashMap<String, File>();
        PrintWriter index = fileUtils.getLog("index.html", dummy);
        index.println("<h2>API/SPI, Metatype, and Feature Changes</h2>");
        index.println("<h3>Baseline Date: " + getBaselineDate() + "</h3>");
        index.println("<h3>Report Date: " + getCurrentDate() + "</h3>");
        index.println("<h3>Previous Reviews: " + getPreviousReviewDates() + "</h3>");
        index.println("<table class=\"table table-striped\">");
        List<File> logs = new ArrayList<File>(logFilesUsed.values());
        Collections.sort(logs);
        for (File log : logs) {

            index.println("<tr><td><a href=\"" + log.getName() + "\">" + log.getName() + "</a></td></tr>");
        }
        index.println("</table>");
        index.println("</body></html>");
        index.close();

    }

    private void reportDifferencesForFeatures(Map<String, FeatureInfo> baseNameToFeatureMap, Map<String, FeatureInfo> newNameToFeatureMap,
                                              FeatureCheckerOutput fco, Map<String, File> logFilesUsed) {
        System.out.println("Looking for Wormholes:");
        for (Entry<String, FeatureInfo> feature : newNameToFeatureMap.entrySet()) {
            feature.getKey();
            if (baseNameToFeatureMap.containsKey(feature.getKey())) {
                FeatureInfo newF = feature.getValue();
                FeatureInfo oldF = baseNameToFeatureMap.get(feature.getKey());
                ManifestDiff md = new ManifestDiff(oldF.baseDir, oldF.featureSourceData, newF.baseDir, newF.featureSourceData);
                MetatypeDiff mtd = new MetatypeDiff(fileUtils, oldF.baseDir, oldF.metatypes, newF.baseDir, newF.metatypes);

                String logFileName = null;
                PrintWriter output = null;

                if (md.hasChanges() | mtd.hasChanges() | fco.hasProblemsForFeature(feature.getKey())) {
                    System.out.println("Feature " + newF.symbolicName + " has changes.");

                    fileUtils.copyOldFile(oldF.featureSourceData);
                    fileUtils.copyNewFile(newF.featureSourceData);

                    logFileName = getFileLog(newF);
                    output = fileUtils.getLog(logFileName, logFilesUsed);

                    output.println("<div class=\"feature\">");
                    output.println("  <div class=\"filename\">" + newF.featureSourceData + "</div>");
                    output.println("  <div class=\"oldfilename\">" + oldF.featureSourceData + "</div>");
                    output.println("  <div class=\"symbolicName\">" + newF.symbolicName + "</div>");
                    output.println("  <div class=\"visibility\">" + newF.visibility + "</div>");
                    output.println("  <div class=\"singleton\">" + newF.singleton + "</div>");
                    output.println("  <div class=\"bndBased\">" + newF.bndBased + "</div>");
                    output.println("  <div class=\"AMD\">MODIFIED</div>");
                }

                //manifest changes...

                if (md.hasChanges()) {

                    output.println("  <div class=\"manifest\">");

                    if (!md.addedHeaders.isEmpty()) {
                        output.println("    <div class=\"addedheaders\">");
                        System.out.println(" - Added headers : ");
                        for (Map.Entry<String, String> header : md.addedHeaders.entrySet()) {
                            output.println("      <div class=\"header\">");

                            System.out.println("  " + header.getKey() + " = " + header.getValue());
                            output.println("        <div class=\"headerKey\">" + header.getKey() + "</div>");
                            output.println("        <div class=\"headerValue\">" + header.getValue() + "</div>");

                            output.println("      </div>");
                        }
                        output.println("    </div>");
                    }
                    if (!md.deletedHeaders.isEmpty()) {
                        output.println("    <div class=\"removedheaders\">");

                        System.out.println(" - Removed headers : ");
                        for (Map.Entry<String, String> header : md.deletedHeaders.entrySet()) {
                            output.println("      <div class=\"header\">");
                            System.out.println("  " + header.getKey() + " = " + header.getValue());
                            output.println("        <div class=\"headerKey\">" + header.getKey() + "</div>");
                            output.println("        <div class=\"headerValue\">" + header.getValue() + "</div>");
                            output.println("      </div>");
                        }

                        output.println("</div>");
                    }
                    if (!md.changedHeaders.isEmpty()) {
                        output.println("    <div class=\"alteredheaders\">");
                        System.out.println(" - Altered headers : ");
                        for (Entry<String, Map<ManifestDiff.Why, Set<ManifestDiff.Change>>> header : md.changedHeaders.entrySet()) {
                            output.println("      <div class=\"header\">");
                            System.out.println("  " + header.getKey());
                            output.println("        <div class=\"headerKey\">" + header.getKey() + "</div>");
                            Set<ManifestDiff.Change> added = header.getValue().get(ManifestDiff.Why.ADDED);
                            Set<ManifestDiff.Change> removed = header.getValue().get(ManifestDiff.Why.REMOVED);
                            Set<ManifestDiff.Change> changed = header.getValue().get(ManifestDiff.Why.CHANGED);
                            if (added != null || removed != null || changed != null) {
                                output.println("          <div class=\"reason\">");
                                if (added != null) {
                                    for (ManifestDiff.Change s : added) {
                                        output.println("            <div class=\"headerValue.ADDED\">" + s.newValue + "</div>");
                                    }
                                }
                                if (removed != null) {
                                    for (ManifestDiff.Change s : removed) {
                                        output.println("            <div class=\"headerValue.REMOVED\">" + s.oldValue + "</div>");
                                    }
                                }
                                if (changed != null) {
                                    for (ManifestDiff.Change s : changed) {
                                        output.println("            <div class=\"headerValue.CHANGED.pair\">");
                                        output.println("              <div class=\"headerValue.CHANGED.old\">" + s.oldValue + "</div>");
                                        output.println("              <div class=\"headerValue.CHANGED.new\">" + s.newValue + "</div>");
                                        output.println("            </div>");
                                    }
                                }
                                output.println("          </div>");
                            }

                            output.println("      </div>");
                        }
                        output.println("    </div>");
                    }

                    output.println("  </div>");
                }

                mtd.reportChanges(output);

                // and now.. scan the junit.xmls to see if anything has been reported there..
                if (fco.hasProblemsForFeature(feature.getKey())) {

                    output.println("  <div class=\"checkerdetail\">");

                    System.out.println(" - Feature checker detailed change information : ");
                    for (String s : fco.getNewProblems(feature.getKey())) {
                        output.println("    <div class=\"detailinfo\">");
                        System.out.println(s);
                        //we have to escape s.
                        String escaped = s.replaceAll("<", "&lt;");
                        escaped = escaped.replaceAll(">", "&gt;");
                        output.println(escaped);
                        output.println("    </div>");
                    }

                    output.println("  </div>");
                }

                if (md.hasChanges() | mtd.hasChanges() | fco.hasProblemsForFeature(feature.getKey())) {
                    System.out.println("");
                    output.println("</div>");
                    output.close();
                }
            }
        }
    }

    private void prepareReport(Map<String, FeatureInfo> baseNameToFeatureMap, Map<String, FeatureInfo> newNameToFeatureMap, FeatureCheckerOutput fco) {
        Map<String, File> logFilesUsed = new HashMap<String, File>();

        //tidy up the old html..
        cleanupDir(fileUtils.getOutDir(), ".html");
        //tidy up the old comparison dir..
        File compare = new File(fileUtils.getOutDir(), "compare");
        File oldDir = new File(compare, "old");
        File newDir = new File(compare, "new");
        cleanupDir(oldDir, null);
        cleanupDir(newDir, null);

        reportRemovedFeatures(baseNameToFeatureMap, newNameToFeatureMap, logFilesUsed);
        reportAddedFeatures(baseNameToFeatureMap, newNameToFeatureMap, fco, logFilesUsed);
        reportDifferencesForFeatures(baseNameToFeatureMap, newNameToFeatureMap, fco, logFilesUsed);

        closeLogs(logFilesUsed);
    }

//    private void dumpClassPathForLogs() {
//        ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
//        //Get the URLs
//        URL[] urls = ((URLClassLoader) sysClassLoader).getURLs();
//        System.out.println("Federation Starship Roster:");
//        for (int i = 0; i < urls.length; i++) {
//            System.out.println(" USS " + urls[i].getFile());
//        }
//    }

    public Report(String args[]) throws IOException {
        fileUtils = new FileUtils(args);
        //      dumpClassPathForLogs();

        // Build baseline indices
        Map<String, FeatureInfo> baseNameToFeatureMap = new TreeMap<String, FeatureInfo>();
        Map<String, FeatureInfo> baseSourceToFeatureMap = new TreeMap<String, FeatureInfo>();

        buildIndexes(fileUtils.getBaseDir(), baseNameToFeatureMap, baseSourceToFeatureMap);

        // Build open Liberty indices
        Map<String, FeatureInfo> newNameToFeatureMap = new TreeMap<String, FeatureInfo>();
        Map<String, FeatureInfo> newSourceToFeatureMap = new TreeMap<String, FeatureInfo>();

        // Add commercial liberty stuff to open Liberty indices -- this will overwrite open features with their CL versions
        buildIndexes(fileUtils.getCommercialLibertyBuildDir(), newNameToFeatureMap, newSourceToFeatureMap);

        buildIndexes(fileUtils.getOpenLibertyBuildDir(), newNameToFeatureMap, newSourceToFeatureMap);

        FeatureCheckerOutput fco = new FeatureCheckerOutput(fileUtils.getBaseDir(), fileUtils.getCommercialLibertyBuildDir(), fileUtils.getOutDir());

        prepareReport(baseNameToFeatureMap, newNameToFeatureMap, fco);
    }

    public static void main(String[] args) throws IOException {
        new Report(args);
    }

}
