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
package com.ibm.aries.buildtasks.semantic.versioning.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.wiring.BundleWiring;

import com.ibm.ws.featureverifier.internal.GlobalConfig;
import com.ibm.ws.featureverifier.internal.XmlErrorCollator;
import com.ibm.ws.featureverifier.internal.XmlErrorCollator.ReportType;

/**
 * Class for holding Feature related info discovered from feature manifests.
 * (or loaded back from xml).
 */
public class FeatureInfo extends VersionedEntity {
    protected static final String SUBSYSTEM_SYMBOLIC_NAME_HEADER_NAME = "Subsystem-SymbolicName";
    protected static final String SUBSYSTEM_VERSION_HEADER_NAME = "Subsystem-Version";
    protected static final String IBM_SPI_PACKAGE_HEADER_NAME = "IBM-SPI-Package";
    protected static final String IBM_API_PACKAGE_HEADER_NAME = "IBM-API-Package";
    protected static final String AUTOFEATURE_HEADER_NAME = "IBM-Provision-Capability";

    private static final String OSGI_SUBSYSTEM_FEATURE_CONTENT_TYPE = "osgi.subsystem.feature";
    private static final String FILE_CONTENT_TYPE = "file";
    private static final String BOOT_JAR_CONTENT_TYPE = "boot.jar";
    private static final String JAR_CONTENT_TYPE = "jar";
    private static final String OSGI_BUNDLE_CONTENT_TYPE = "osgi.bundle";
    private static final String BUNDLE_CONTENT_TYPE = "bundle";
    private static final String DEFAULT_CONTENT_TYPE = BUNDLE_CONTENT_TYPE;

    private static final String TOLERATES_DIRECTIVE_NAME = "ibm.tolerates:";
    private static final String LOCATION_DIRECTIVE_NAME = "location:";
    private static final String VERSION_ATTRIBUTE_NAME = "version";

    private static final String SINGLETON_DIRECTIVE_NAME = "singleton:";
    private static final String THIRD_PARTY_API_TYPE_VALUE = "third-party";
    private static final String INTERNAL_API_TYPE_VALUE = "internal";
    private static final String API_TYPE_ATTRIBUTE_NAME = "type";

    private static final String VISIBILITY_DIRECTIVE_NAME = "visibility:";
    private static final String DEFAULT_FEATURE_VISIBILITY = "private";

    private static int JAVA_VERSION = 7;
    static {
        try {
            String version = System.getProperty("java.version");
            String[] versionElements = version.split("\\D"); // split on non-digits
            // Pre-JDK 9 the java.version is 1.MAJOR.MINOR
            // Post-JDK 9 the java.version is MAJOR.MINOR
            int i = Integer.valueOf(versionElements[0]) == 1 ? 1 : 0;
            JAVA_VERSION = Integer.valueOf(versionElements[i]);
        } catch (Exception e) {
            // Avoid blowing up static initializer block at all costs
            e.printStackTrace();
        }
    }

    final String shortName;
    final String visibility;

    public static class ErrorInfo {
        public XmlErrorCollator.ReportType type;
        public String shortText;
        public String summary;
        public String detail;
    }

    final Collection<ErrorInfo> issuesFoundParsingManifest = new ArrayList<ErrorInfo>();

    String autofeatureHeaderContent;

    //api/spi defined by this feature
    VersionedEntityMap<PkgInfo, Map<String, String>> LocalAPIInfo;
    VersionedEntityMap<PkgInfo, Map<String, String>> LocalSPIInfo;

    public static class ApiSpiJarKey implements Comparable<ApiSpiJarKey> {
        /**
         * @return the type
         */
        public ContentType getType() {
            return type;
        }

        /**
         * @return the filename
         */
        public String getFilename() {
            return filename;
        }

        /**
         * @return the symbolicName
         */
        public String getSymbolicName() {
            return symbolicName;
        }

        /**
         * @return the version
         */
        public String getVersion() {
            return version;
        }

        /**
         * @return the location
         */
        public String getLocation() {
            return location;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((symbolicName == null) ? 0 : symbolicName.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            result = prime * result + ((version == null) ? 0 : version.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ApiSpiJarKey other = (ApiSpiJarKey) obj;
            if (symbolicName == null) {
                if (other.symbolicName != null)
                    return false;
            } else if (!symbolicName.equals(other.symbolicName))
                return false;
            if (type != other.type)
                return false;
            if (version == null) {
                if (other.version != null)
                    return false;
            } else if (!version.equals(other.version))
                return false;
            return true;
        }

        public static enum ContentType {
            JAR, BUNDLE
        };

        private final ContentType type;
        private final String filename;
        private final String symbolicName;
        private final String version;
        private final String location;

        public ApiSpiJarKey(ContentType type, String filename, String symbolicName, String version, String location) {
            this.type = type;
            this.filename = filename;
            this.symbolicName = symbolicName;
            this.version = version;
            this.location = location;
        }

        @Override
        public String toString() {
            return "Type:" + type.name() + " SymbolicName:" + symbolicName + " Version:" + version
                   + (type.equals(ContentType.JAR) ? (" Filename: " + filename + " Location:" + location) : "");
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(ApiSpiJarKey o) {
            if (o != null)
                return this.toString().compareTo(o.toString());
            else
                return -1;
        }
    }

    //classes/xsds contained in each jar declared under dev for this feature.
    //jarlocation->(resourcename->seeninruntime)
    protected Map<ApiSpiJarKey, Map<String, Boolean>> apiSpiJarNameToContainedResources = null;

    public void setResources(Map<ApiSpiJarKey, Map<String, Boolean>> resources) {
        if (apiSpiJarNameToContainedResources != null) {
            throw new RuntimeException("ERROR: attempt to set map into " + this.getName() + " hash " + this.hashCode() + " when map already set");
        } else {
            apiSpiJarNameToContainedResources = Collections.unmodifiableMap(resources);
        }
    }

    //api/spi defined by this feature and all nested features
    //pkginfo->
    VersionedEntityMap<PkgInfo, Map<String, String>> AggregateAPIInfo;
    VersionedEntityMap<PkgInfo, Map<String, String>> AggregateSPIInfo;
    //per api/spi package, the features that made it so.
    VersionedEntityMap<PkgInfo, String> AggregateAPIDeclarers;
    VersionedEntityMap<PkgInfo, String> AggregateSPIDeclarers;

    final Map<String, String> featureAttributes = new HashMap<String, String>();

    Map<String, Set<VersionRange>> contentBundles;
    //map of feature name to tolerates value, if any
    Map<String, String> contentFeatures;

    Set<FeatureInfo> aggregateFeatureInfo = null;

    String source;

    boolean aggregateIsComplete = false;

    boolean singleton = false;

    public boolean getSingleton() {
        return singleton;
    }

    boolean isAutoFeature = false;

    public boolean getAutoFeature() {
        return isAutoFeature;
    }

    boolean isTestFeature = false;

    public boolean getTestFeature() {
        return isTestFeature;
    }

    /**
     *
     * @param resourceName
     * @return
     */
    public List<ApiSpiJarKey> isResourceKnownToAssociatedDevJar(String resourceName, boolean debug) {
        if (debug) {
            System.out.println("Feature " + getName() + " asked about resource " + resourceName + " and has known api/spi jars of " + apiSpiJarNameToContainedResources.keySet());
            System.out.println(this);
        }
        List<ApiSpiJarKey> matchedResources = new LinkedList<ApiSpiJarKey>();
        for (Map.Entry<ApiSpiJarKey, Map<String, Boolean>> devJar : apiSpiJarNameToContainedResources.entrySet()) {
            if (debug) {
                System.out.println(" - API/SPI jar " + devJar.getKey().toString() + " has entryset " + devJar.getValue().keySet());
            }
            if (devJar.getValue().keySet().contains(resourceName)) {
                matchedResources.add(devJar.getKey());
            }
        }
        return matchedResources;
    }

    public void markResourceAsSeenAtRuntime(String location, String resourceName) {
        Map<String, Boolean> seenInRuntime = apiSpiJarNameToContainedResources.get(location);
        seenInRuntime.put(location, Boolean.TRUE);
    }

    private static Map<String, Boolean> collectResourcesInDevJar(String forFeature, String symbolicName, String versionRange, String locationString, String installDir,
                                                                 Map<VersionedEntity, String> devJars) throws IOException {
        VersionRange vr = new VersionRange(versionRange);
        for (Map.Entry<VersionedEntity, String> ve : devJars.entrySet()) {
            if (ve.getKey().getName().equals(symbolicName)) {
                if (vr.includes(new Version(ve.getKey().getVersion()))) {
                    System.out.println("Matched dev jar request (for feature " + forFeature + ") for " + symbolicName + "@" + versionRange + " to " + ve.getValue());
                    boolean correctLocation = false;
                    for (String location : locationString.split(",")) {
                        System.out.println("Testing " + ve.getValue() + " against " + location);
                        if (ve.getValue().startsWith(location)) {
                            System.out.println("Testing " + ve.getValue().substring(location.length()) + " for / separators.");
                            if (!ve.getValue().substring(location.length()).contains("/")) {
                                correctLocation = true;
                                break;
                            }
                        }
                    }
                    if (correctLocation) {
                        return collectResourcesInJar(forFeature, ve.getValue(), installDir);
                    }
                } else {
                    System.out.println("Matched symb name " + symbolicName + " but VersionRange " + vr.toString() + " did not include " + ve.getKey().getVersion());
                }
            }
        }
        System.out.println("NO MATCH for dev jar request for feature " + forFeature + ") for " + symbolicName + "@" + versionRange);
        Map<String, Boolean> resources = new TreeMap<String, Boolean>();
        return resources;
    }

    private static Map<String, Boolean> collectResourcesInBundle(String forFeature, String symbolicName, String versionRange, String locationString, BundleContext context) {
        Map<String, Boolean> resources = new TreeMap<String, Boolean>();
        boolean found = false;
        VersionRange vr = new VersionRange(versionRange);
        System.out.println("looking for " + symbolicName + " with version " + versionRange);
        if (context != null) {
            for (Bundle b : context.getBundles()) {

                if (b.getSymbolicName().equals(symbolicName) && vr.includes(b.getVersion())) {
                    System.out.println("- Matched ranged jar to loaded bundle " + b.getSymbolicName() + " loc: " + b.getLocation());

                    BundleWiring bw = b.adapt(BundleWiring.class);
                    if (bw == null) {
                        XmlErrorCollator.addProcessingIssue(ReportType.ERROR, "Unable to gather dev jar paths from bundle " + b.getSymbolicName()
                                                                              + " because it could not be adapted to wiring");
                        continue;
                    }
                    //this should be..
                    //Collection<String> paths = bw.listResources("/", null, BundleWiring.LISTRESOURCES_RECURSE|BundleWiring.LISTRESOURCES_LOCAL);
                    //but the baseline was generated without the local flag, so for 8554/8555 we must leave it as is.. can fix when we gen for 8556
                    Collection<String> paths = bw.listResources("/", null, BundleWiring.LISTRESOURCES_RECURSE | BundleWiring.LISTRESOURCES_LOCAL);
                    resources.put("##MANIFEST.MF##NAME##" + b.getSymbolicName() + "##VERSION##" + b.getVersion() + "##FILE##" + b.getLocation() + "##TYPE##BUNDLE", Boolean.TRUE);
                    if (paths != null) {
                        //System.err.println(" - given "+paths.size()+" paths to query. ");
                        for (String p : paths) {
                            String className = p.replace('/', '.');
                            if (className.startsWith("."))
                                className = className.substring(1);
                            if (className.endsWith(".class") || className.endsWith(".xsd")) {
                                resources.put(className, Boolean.FALSE);
                            }
                        }
                    } else {
                        System.err.println("Unable to scan bundle " + b.getSymbolicName() + "@" + b.getVersion() + " for content, null paths returned");
                        XmlErrorCollator.addProcessingIssue(ReportType.ERROR, "Unable to scan bundle " + b.getSymbolicName() + "@" + b.getVersion()
                                                                              + " for content, null paths returned");
                    }
                }
            }
        }

        if (!found) {
            System.out.println("ERROR: expected Bundle " + symbolicName + "@" + versionRange + " (" + locationString + ") to have been loaded for feature " + forFeature
                               + " but did not find it from context");
            resources = null;

        }
        return resources;
    }

    protected static Map<String, Boolean> collectResourcesInJar(String forFeature, String location, String installDirLocation) throws IOException {
        Map<String, Boolean> resources = new TreeMap<String, Boolean>();
        if (installDirLocation != null) {
            File installDir = new File(installDirLocation);
            File jar = new File(installDir, location);
            ZipFile zf = new ZipFile(jar);
            boolean foundManifest = false;
            try {
                Enumeration<? extends ZipEntry> entries = zf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry ze = entries.nextElement();
                    String zipEntryName = ze.getName();
                    if (!ze.isDirectory() && (zipEntryName.endsWith(".class") || zipEntryName.endsWith(".xsd"))) {
                        String className = zipEntryName.replace('/', '.');
                        if (className.startsWith("."))
                            className = className.substring(1);
                        resources.put(className, Boolean.FALSE);
                        System.out.println(" + " + className);
                    } else {
                        if (!ze.isDirectory() && (zipEntryName.equals("META-INF/MANIFEST.MF") || zipEntryName.equals("/META-INF/MANIFEST.MF"))) {
                            System.out.println("Found manifest for " + location);
                            Manifest m = ManifestProcessor.parseManifest(zf.getInputStream(ze));
                            Map<String, String> manifestMap = ManifestProcessor.readManifestIntoMap(m);
                            System.out.println("Read into map");
                            NameValuePair symbNameParsed = ManifestHeaderProcessor.parseBundleSymbolicName(manifestMap.get(Constants.BUNDLE_SYMBOLICNAME));
                            String symbName = symbNameParsed.getName();
                            String version = manifestMap.get(Constants.BUNDLE_VERSION);
                            System.out.println("Name: " + symbName + " Version: " + version);
                            //a bit of a cheat.. we'll remember the symbname/version as a special resource
                            resources.put("##MANIFEST.MF##NAME##" + symbName + "##VERSION##" + version + "##FILE##" + location + "##TYPE##JAR", Boolean.TRUE);
                            System.out.println("done gathering name");
                            foundManifest = true;
                        }
                    }
                }
                //System.err.println(" - found "+resources.size()+" paths in jar. ");
            } finally {
                if (!foundManifest) {
                    System.out.println("**ERROR: did not find manifest for " + location);
                }
                zf.close();
            }
        }
        return resources;

    }

    public static FeatureInfo createFromManifest(File manifest, String installDir, BundleContext context, Map<VersionedEntity, String> devJars) throws IOException {
        String symbolicName;
        String visibility = DEFAULT_FEATURE_VISIBILITY;
        Map<String, String> symbNameAttributes = new HashMap<String, String>();

        List<ErrorInfo> errors = new ArrayList<ErrorInfo>();

        Manifest m = ManifestProcessor.parseManifest(new FileInputStream(manifest));
        Map<String, String> manifestMap = ManifestProcessor.readManifestIntoMap(m);

        //scan the manifest for duplicate headers.
        Attributes attribs = m.getMainAttributes();
        Map<String, List<String>> dupeCheckMap = new HashMap<String, List<String>>();
        for (Entry<Object, Object> attrib : attribs.entrySet()) {
            String key = String.valueOf(attrib.getKey());
            String val = String.valueOf(attrib.getValue());
            if (!dupeCheckMap.containsKey(key)) {
                dupeCheckMap.put(key, new ArrayList<String>());
            }
            dupeCheckMap.get(key).add(val);
        }
        //lets see if the manifest at least had a name we can report errors against!!
        List<String> dupeHeaders = dupeCheckMap.get(SUBSYSTEM_SYMBOLIC_NAME_HEADER_NAME);
        if (dupeHeaders != null && dupeHeaders.size() > 0) {
            boolean dupe = dupeHeaders.size() > 1;
            String symbNameRaw = dupeHeaders.iterator().next();
            ManifestHeaderProcessor.NameValuePair symbNameParsed;
            try {
                symbNameParsed = ManifestHeaderProcessor.parseBundleSymbolicName(symbNameRaw);
                symbolicName = symbNameParsed.getName();

                //grab the visibility.. no vis == private
                if (symbNameParsed.getAttributes() != null && symbNameParsed.getAttributes().get(VISIBILITY_DIRECTIVE_NAME) != null) {
                    visibility = symbNameParsed.getAttributes().get(VISIBILITY_DIRECTIVE_NAME);
                }

                symbNameAttributes = symbNameParsed.getAttributes();
            } catch (Exception e) {
                ErrorInfo ei = new ErrorInfo();
                ei.type = ReportType.ERROR;
                ei.shortText = "[BAD_SYMBOLIC_NAME " + symbNameRaw + "]";
                ei.summary = "SymbolicName did not parse correctly from " + manifest.getName();
                ei.detail = "When attempting to load feature manifest for feature from manifest " + manifest.getName()
                            + " the content of the SubSystem-SymbolicName header was unable to be parsed successfully. The header had content " + symbNameRaw;
                errors.add(ei);
                symbolicName = "BadSymbolicNameFor" + manifest.getName();
            }
            if (dupe) {
                //change the feature name to highlight the dupe issue in the error report
                //after all, the feature has 2 names, so otherwise we could report against the 'wrong' one
                //the dupe issue will be reported below.
                symbolicName = "DuplicateSymbolicNameFor" + symbolicName;
            }
        } else {
            ErrorInfo ei = new ErrorInfo();
            ei.type = ReportType.ERROR;
            ei.shortText = "[MISSING_SYMBOLIC_NAME " + manifest.getName() + "]";
            ei.summary = "SymbolicName missing from manifest " + manifest.getName();
            ei.detail = "When attempting to load feature manifest for feature manifest file " + manifest.getName() + " no SubSystem-SymbolicName header was encountered.";
            errors.add(ei);
            symbolicName = "MissingSymbolicNameFor" + manifest.getName();
        }
        for (Entry<String, List<String>> dupeCheckEntry : dupeCheckMap.entrySet()) {
            if (dupeCheckEntry.getValue().size() > 1) {
                ErrorInfo ei = new ErrorInfo();
                ei.type = ReportType.ERROR;
                ei.shortText = "[DUPLICATE_MANIFEST_ENTRY " + dupeCheckEntry.getKey() + "]";
                ei.summary = "Duplicate Manifest Header " + dupeCheckEntry.getKey() + " in feature manifest file " + manifest.getName();
                ei.detail = "When attempting to load feature manifest from manifest file " + manifest.getName() + " duplicate headers were found with the header key "
                            + dupeCheckEntry.getKey() + " with values of " + dupeCheckEntry.getValue();
                errors.add(ei);
            }
        }
        dupeCheckMap.clear();
        dupeCheckMap = null;

        //version for subsystem.. not sure if we use it in liberty, as we encode version onto symbname
        String versionString = manifestMap.get(SUBSYSTEM_VERSION_HEADER_NAME);

        FeatureInfo fi = new FeatureInfo(symbolicName, versionString, visibility, manifestMap.get("IBM-ShortName"), symbNameAttributes);
        fi.source = manifest.getAbsolutePath();

        fi.issuesFoundParsingManifest.addAll(errors);
        errors = null;

        //read in the api info.. package->type for api/spi
        String apiString = manifestMap.get(IBM_API_PACKAGE_HEADER_NAME);
        String spiString = manifestMap.get(IBM_SPI_PACKAGE_HEADER_NAME);

        processPackageString(apiString, fi.LocalAPIInfo);
        processPackageString(spiString, fi.LocalSPIInfo);

        if (manifestMap.containsKey("IBM-Test-Feature:")) {
            fi.isTestFeature = true;
        }

        //validate if this is an autofeature that it does not create public api/spi
        if (manifestMap.containsKey(AUTOFEATURE_HEADER_NAME)) {
            fi.autofeatureHeaderContent = manifestMap.get(AUTOFEATURE_HEADER_NAME);
            fi.isAutoFeature = true;
            for (Map.Entry<PkgInfo, Set<Map<String, String>>> entry : fi.LocalAPIInfo.entrySet()) {
                for (Map<String, String> attribMap : entry.getValue()) {
                    String type = attribMap.get(API_TYPE_ATTRIBUTE_NAME);
                    if (type != null) {
                        if (type.contains(INTERNAL_API_TYPE_VALUE) || type.contains(THIRD_PARTY_API_TYPE_VALUE)) {
                            //not a problem, we don't enforce api for these.
                        } else {
                            System.out.println("WARNING: Auto Feature " + symbolicName + " is attempting to declare api with type " + type
                                               + " which will make user API/SPI shape nonconsistent for affected features");
                        }
                    }
                }
            }
            for (Map.Entry<PkgInfo, Set<Map<String, String>>> entry : fi.LocalSPIInfo.entrySet()) {
                for (Map<String, String> attribMap : entry.getValue()) {
                    String type = attribMap.get(API_TYPE_ATTRIBUTE_NAME);
                    if (type != null) {
                        if (type.contains(INTERNAL_API_TYPE_VALUE) || type.contains(THIRD_PARTY_API_TYPE_VALUE)) {
                            //not a problem, we don't enforce spi for these.
                        } else {
                            System.out.println("WARNING: Auto Feature " + symbolicName + " is attempting to declare spi with type " + type
                                               + " which will make user API/SPI shape nonconsistent for affected features");
                        }
                    }
                }
            }
        }

        //read in the content for the subsystem, we only care about bundles, and sub features.
        //System.out.println("init'ing content map for "+fi.getName());
        fi.contentFeatures = new HashMap<String, String>();
        fi.contentBundles = new HashMap<String, Set<VersionRange>>();
        String contentString = manifestMap.get("Subsystem-Content");
        if (fi.apiSpiJarNameToContainedResources == null) {
            fi.apiSpiJarNameToContainedResources = new TreeMap<ApiSpiJarKey, Map<String, Boolean>>();
            System.out.println("Configuring resources for fi " + fi.getName() + " fi hash " + fi.hashCode() + " from " + manifest.getAbsolutePath());
        } else {
            System.out.println("ERROR: attempt made to overwrite api/spi map for " + fi.getName());
            throw new RuntimeException("ERROR: attempt made to overwrite api/spi map for " + fi.getName() + " fi hash " + fi.hashCode());
        }

        processContentString(fi, contentString, fi.contentFeatures, fi.contentBundles, fi.apiSpiJarNameToContainedResources, installDir, context, devJars);
        fi.apiSpiJarNameToContainedResources = Collections.unmodifiableMap(fi.apiSpiJarNameToContainedResources);

        //System.out.println(fi.contentFeatures);
        if (symbNameAttributes != null && symbNameAttributes.containsKey(SINGLETON_DIRECTIVE_NAME)) {
            fi.singleton = symbNameAttributes.get(SINGLETON_DIRECTIVE_NAME).equalsIgnoreCase("false") ? false : true;
        }

        return fi;
    }

    //partial constructor, used when creating from manifest
    public FeatureInfo(String name, String version, String visibility, String shortName, Map<String, String> featureAttribs) {
        this(name, version, visibility, shortName, featureAttribs, false, false);
    }

    //full constructor.. used by create debug features..
    public FeatureInfo(String name, String version, String visibility, String shortName, Map<String, String> featureAttribs, boolean singleton, boolean autofeature) {
        super(name, version);
        this.visibility = visibility;
        this.shortName = shortName;
        if (featureAttribs != null) {
            featureAttributes.putAll(featureAttribs);
        }
        this.singleton = singleton;
        this.isAutoFeature = autofeature;
        LocalAPIInfo = new VersionedEntityMap<PkgInfo, Map<String, String>>();
        LocalSPIInfo = new VersionedEntityMap<PkgInfo, Map<String, String>>();
        AggregateAPIInfo = new VersionedEntityMap<PkgInfo, Map<String, String>>();
        AggregateSPIInfo = new VersionedEntityMap<PkgInfo, Map<String, String>>();
        AggregateAPIDeclarers = new VersionedEntityMap<PkgInfo, String>();
        AggregateSPIDeclarers = new VersionedEntityMap<PkgInfo, String>();
    }

    public void addLocalApi(PkgInfo p, Map<String, String> attribs) {
        if (!hasRequiredJava(attribs))
            return;
        LocalAPIInfo.merge(p, attribs);
    }

    public void addLocalSpi(PkgInfo p, Map<String, String> attribs) {
        if (!hasRequiredJava(attribs))
            return;
        LocalSPIInfo.merge(p, attribs);
    }

    public void addApi(PkgInfo p, Map<String, String> attribs, String declaringFeature) {
        if (!hasRequiredJava(attribs))
            return;
        AggregateAPIInfo.merge(p, attribs);
        AggregateAPIDeclarers.merge(p, declaringFeature);
    }

    public void addSpi(PkgInfo p, Map<String, String> attribs, String declaringFeature) {
        if (!hasRequiredJava(attribs))
            return;
        AggregateSPIInfo.merge(p, attribs);
        AggregateSPIDeclarers.merge(p, declaringFeature);
    }

    private boolean hasRequiredJava(Map<String, String> attribs) {
        String attr = attribs.get("require-java:");
        int requireJava = 7;
        if (attr != null) {
            attr = attr.substring(1, attr.indexOf(']'));
            requireJava = Integer.parseInt(attr);
        }
        return JAVA_VERSION >= requireJava;
    }

    public void setAggregateReady() {
        if (aggregateFeatureInfo == null) {
            System.out.println("ERROR: attempt to set aggregate ready with no aggregateFeatureInfo.. !!");
            throw new IllegalStateException(" attempt to set aggregate ready with no aggregateFeatureInfo..");
        }

        aggregateIsComplete = true;
    }

    public Map<PkgInfo, Set<Map<String, String>>> getLocalAPI() {
        return LocalAPIInfo;
    }

    public Map<PkgInfo, Set<Map<String, String>>> getLocalSPI() {
        return LocalSPIInfo;
    }

    public Collection<ErrorInfo> getIssuesFoundParsingManifest() {
        return issuesFoundParsingManifest;
    }

    public String getAutoFeatureHeaderContent() {
        return autofeatureHeaderContent;
    }

    public Map<PkgInfo, Set<Map<String, String>>> getAggregateAPI() {
        if (!aggregateIsComplete)
            throw new RuntimeException("Aggregate is not ready yet!");
        return AggregateAPIInfo;
    }

    public Map<PkgInfo, Set<Map<String, String>>> getAggregateSPI() {
        if (!aggregateIsComplete)
            throw new RuntimeException("Aggregate is not ready yet!");
        return AggregateSPIInfo;
    }

    public Set<FeatureInfo> getAggregateFeatureSet() {
        if (!aggregateIsComplete)
            throw new RuntimeException("Aggregate is not ready yet!");
        return aggregateFeatureInfo;
    }

    public Map<PkgInfo, Set<String>> getAggregateAPIDeclarers() {
        return AggregateAPIDeclarers;
    }

    public Map<PkgInfo, Set<String>> getAggregateSPIDeclarers() {
        return AggregateSPIDeclarers;
    }

    public Map<ApiSpiJarKey, Map<String, Boolean>> getDevJarResources() {
        return apiSpiJarNameToContainedResources;
    }

    public String getShortName() {
        return shortName;
    }

    public String getSource() {
        return source;
    }

    public String getVisibility() {
        return visibility;
    }

    public Map<String, String> getContentFeatures() {
        return contentFeatures;
    }

    public Map<String, Set<VersionRange>> getContentBundles() {
        return contentBundles;
    }

    private static void addContentBundle(String symbName, String versionString, Map<String, Set<VersionRange>> bundles) {
        if (!bundles.containsKey(symbName)) {
            bundles.put(symbName, new HashSet<VersionRange>());
        }
        if (versionString != null) {
            VersionRange vr = new VersionRange(versionString);
            bundles.get(symbName).add(vr);
        } else {
            bundles.get(symbName).add(null);
        }
    }

    private static void processContentString(FeatureInfo fi,
                                             String content,
                                             Map<String, String> features,
                                             Map<String, Set<VersionRange>> bundles,
                                             Map<ApiSpiJarKey, Map<String, Boolean>> apispijarcontent,
                                             String installDirLocation,
                                             BundleContext context,
                                             Map<VersionedEntity, String> devJars) throws IOException {
        //can reuse the import string parser ;p
        Map<String, Map<String, String>> contentMap = ManifestHeaderProcessor.parseImportString(content);

        for (Map.Entry<String, Map<String, String>> contentEntry : contentMap.entrySet()) {
            String type = DEFAULT_CONTENT_TYPE;
            if (contentEntry.getValue().containsKey(API_TYPE_ATTRIBUTE_NAME)) {
                type = contentEntry.getValue().get(API_TYPE_ATTRIBUTE_NAME);
            }
            if (OSGI_SUBSYSTEM_FEATURE_CONTENT_TYPE.equals(type)) {
                String tolerates = contentEntry.getValue().get(TOLERATES_DIRECTIVE_NAME);
                features.put(contentEntry.getKey(), tolerates);
            } else if (DEFAULT_CONTENT_TYPE.equals(type) || OSGI_BUNDLE_CONTENT_TYPE.equals(type)) {
                String versionString = contentEntry.getValue().get(VERSION_ATTRIBUTE_NAME);

                if (versionString == null) {
                    ErrorInfo ei = new ErrorInfo();
                    ei.type = ReportType.ERROR;
                    ei.shortText = "[MISSING_CONTENT_VERSION " + fi.getName() + "]";
                    ei.summary = "Missing version information for " + contentEntry.getKey() + " in feature " + fi.getName();
                    ei.detail = "When attempting to load feature manifest for feature "
                                + fi.getName()
                                + " an entry was encountered in the SubSystem-Content header that had no version range for a content type of bundle, this is an error. The Content had name "
                                + contentEntry.getKey();

                    fi.issuesFoundParsingManifest.add(ei);
                }

                addContentBundle(contentEntry.getKey(), versionString, bundles);

                //bundle jars can be api spi too..
                //Subsystem-Content: com.ibm.ws.javaee.servlet.3.0; version="[1,1.0.100)"; location:="dev/api/spec/,lib/"

                StringBuffer sb = new StringBuffer();
                sb.append("[");
                for (Map.Entry<String, String> attrib : contentEntry.getValue().entrySet()) {
                    sb.append(attrib.getKey() + "=" + attrib.getValue() + ",");
                }
                sb.append("]");
                String locationString = contentEntry.getValue().get(LOCATION_DIRECTIVE_NAME);
                if (locationString != null) {
                    if (locationString.contains("dev/")) {
                        System.out.println("** API/SPI VERSIONED BUNDLE: " + contentEntry.getKey() + " " + type + " " + sb.toString() + " location " + locationString);
                        //versioned content always uses compound key
                        String keyStr = contentEntry.getKey();
                        //if the bundle is loaded, we can read its content that way.. if not, then we need to read it as a devjar..
                        //when we run in query mode, we are forced to load jars by location, because we need to query unloaded features.
                        if (GlobalConfig.getQueryMode() != null) {
                            System.out.println("collecting resources in bundle as devjar");
                            //treat this one as an api jar..
                            Map<String, Boolean> resources = FeatureInfo.collectResourcesInDevJar(fi.getName(), contentEntry.getKey(), versionString,
                                                                                                  locationString, installDirLocation, devJars);
                            ApiSpiJarKey key = extractKeyFromResources(resources, keyStr, locationString);
                            apispijarcontent.put(key, resources);
                        } else {
                            System.out.println("collecting resources in bundle");
                            //treat this one as a bundle..
                            Map<String, Boolean> resources = FeatureInfo.collectResourcesInBundle(fi.getName(), contentEntry.getKey(), versionString, locationString, context);
                            if (resources == null) {
                                //retry as dev jar..
                                System.out.println("retrying as devjar");
                                resources = FeatureInfo.collectResourcesInDevJar(fi.getName(), contentEntry.getKey(), versionString,
                                                                                 locationString, installDirLocation, devJars);

                            }
                            ApiSpiJarKey key = extractKeyFromResources(resources, keyStr, locationString);
                            if (key != null && resources != null) {
                                apispijarcontent.put(key, resources);
                            }
                        }
                    }
                }

            } else if (JAR_CONTENT_TYPE.equals(type) || BOOT_JAR_CONTENT_TYPE.equals(type)) {
                StringBuffer sb = new StringBuffer();
                sb.append("[");
                for (Map.Entry<String, String> attrib : contentEntry.getValue().entrySet()) {
                    sb.append(attrib.getKey() + "=" + attrib.getValue() + ",");
                }
                String versionString = contentEntry.getValue().get(VERSION_ATTRIBUTE_NAME);
                if (versionString != null) {
                    addContentBundle(contentEntry.getKey(), versionString, bundles);
                    //version jars can be api spi too..
                    //eg com.ibm.ws.javaee.connector.1.6; version="[1,1.0.100)"; type="jar"; location:="dev/api/spec/,lib/",
                    String locationString = contentEntry.getValue().get(LOCATION_DIRECTIVE_NAME);
                    if (locationString != null) {
                        if (locationString.contains("dev/")) {
                            //versioned content always uses compound key..
                            String keyStr = contentEntry.getKey();
                            System.out.println("** API/SPI VERSIONED JAR: " + contentEntry.getKey() + " " + type + " " + sb.toString() + " location " + locationString);
                            //treat this one as an api jar..
                            System.out.println("collecting resources in versioned devjar");
                            Map<String, Boolean> resources = FeatureInfo.collectResourcesInDevJar(fi.getName(), contentEntry.getKey(), versionString,
                                                                                                  locationString, installDirLocation, devJars);
                            ApiSpiJarKey key = extractKeyFromResources(resources, keyStr, locationString);
                            if (key != null && resources != null) {
                                apispijarcontent.put(key, resources);
                            }
                        }
                    } else {
                        System.err.println("** versioned jar content with no location.. " + contentEntry.getKey() + " " + type + " " + sb.toString());
                    }
                } else {
                    String locationString = contentEntry.getValue().get(LOCATION_DIRECTIVE_NAME);
                    if (locationString != null) {
                        if (locationString.contains("dev/")) {
                            String keyStr = contentEntry.getKey();
                            //unversioned content only uses filename as key..
                            if (locationString.endsWith(".jar")) {
                                keyStr = "";
                            }
                            System.out.println("** API/SPI JAR: " + contentEntry.getKey() + " " + type + " " + sb.toString());
                            System.out.println("collecting resources in devjar");
                            //remember the api/spi jar content for later..
                            Map<String, Boolean> resources = FeatureInfo.collectResourcesInJar(fi.getName(), locationString, installDirLocation);
                            ApiSpiJarKey key = extractKeyFromResources(resources, keyStr, locationString);
                            if (key != null && resources != null) {
                                apispijarcontent.put(key, resources);
                            }
                        } else {
                            //System.err.println("** LOCATION JAR: "+contentEntry.getKey()+" "+type+" "+sb.toString());
                        }
                    } else
                        System.err.println("WARNING: Unknown jar type: " + contentEntry.getKey() + " " + type + " " + sb.toString());
                }

            } else if (!FILE_CONTENT_TYPE.equals(type)) {
                System.err.println("WARNING: Unknown content type " + contentEntry.getKey() + " " + type);
            }
        }
    }

    /**
     * @param resources
     * @return
     */
    public static ApiSpiJarKey extractKeyFromResources(Map<String, Boolean> resources, String name, String location) {
        ApiSpiJarKey result = null;
        for (String entry : resources.keySet()) {
            //the manifest info for the jar is serialized as an entry.. the simplest way to handle all content types, and serialization.
            if (entry.startsWith("##MANIFEST.MF")) {
                //##MANIFEST.MF##NAME##com.ibm.websphere.appserver.spi.fish##VERSION##1.0.8.201504##FILE##fish.jar##TYPE##JAR
                String parts[] = entry.split("##");
                //verify format..
                if (!(parts[0].length() == 0 && parts[1].equals("MANIFEST.MF") && parts[2].equals("NAME") && parts[4].equals("VERSION") && parts[6].equals("FILE")
                      && parts[8].equals("TYPE"))) {
                    throw new IllegalStateException("ERROR: Bad format for manifest data entry for " + name + " at " + location + " " + entry);
                }
                String symbolicName = parts[3];
                String version = parts[5];
                String filename = parts[7];
                String type = parts[9];

                result = new ApiSpiJarKey(com.ibm.aries.buildtasks.semantic.versioning.model.FeatureInfo.ApiSpiJarKey.ContentType.valueOf(type), filename, symbolicName, version, location);
                break;
            }
        }
        if (result == null) {
            //try patch file.. we didn't always include this info in baselines, and had to go back to add it.
            System.out.println("INFO: Baseline had no extended manifest info for api/spi jar " + name + " with location " + location);

            String jarVersionData = GlobalConfig.getExtraJarVersionData(name + location);
            if (jarVersionData != null) {

                String parts[] = jarVersionData.split("##");
                //verify format..
                if (!(parts.length >= 10 && parts[0].length() == 0 && parts[1].equals("MANIFEST.MF") && parts[2].equals("NAME") && parts[4].equals("VERSION")
                      && parts[6].equals("FILE") && parts[8].equals("TYPE"))) {
                    throw new IllegalStateException("ERROR: Bad format for manifest data entry from extraJarVersionData " + name + " at " + location);
                }
                String symbolicName = parts[3];
                String version = parts[5];
                String filename = parts[7];
                String type = parts[9];

                result = new ApiSpiJarKey(com.ibm.aries.buildtasks.semantic.versioning.model.FeatureInfo.ApiSpiJarKey.ContentType.valueOf(type), filename, symbolicName, version, location);
            } else {
                System.out.println("ERROR: Extended info had no info for jar.");
            }
        }
        if (result == null) {
            if (GlobalConfig.getIgnoreMissingResources()) {
                // This may happen when processing dodgy manifests, so try and struggle on
                System.out.println("ERROR: [ignored] Unable to form new key info for jar " + name + " at location " + location);
            } else {
                throw new IllegalArgumentException("ERROR: Unable to form new key info for jar " + name + " at location " + location);
            }
        }
        return result;
    }

    protected static void processPackageString(String pkg, VersionedEntityMap<PkgInfo, Map<String, String>> info) {
        List<NameValuePair> parsedImports = ManifestHeaderProcessor.parseExportString(pkg);
        for (NameValuePair parsedImport : parsedImports) {
            String name = parsedImport.getName();
            String version = parsedImport.getAttributes().get(VERSION_ATTRIBUTE_NAME); //ok to be null.
            PkgInfo p = new PkgInfo(name, version, null);
            info.merge(p, parsedImport.getAttributes());
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Feature:" + getName() + " (" + shortName + ")  " + getVersion() + "\n");
        sb.append("  From: " + source + "\n");
        sb.append("  Visibility:" + visibility + "\n");
        if (contentBundles != null) {
            sb.append("  Bundles:\n");
            for (Map.Entry<String, Set<VersionRange>> bundle : contentBundles.entrySet()) {
                sb.append("    - " + bundle.getKey() + " " + bundle.getValue() + "\n");
            }
        }
        if (contentFeatures != null) {
            sb.append("  Features:\n");
            for (Map.Entry<String, String> subFeature : contentFeatures.entrySet()) {
                sb.append("    - " + subFeature.getKey() + " " + (subFeature.getValue() == null ? "" : " tolerates:=" + subFeature.getValue()) + "\n");
            }
        }
        sb.append("  API:\n");
        for (Map.Entry<PkgInfo, Set<Map<String, String>>> entry : AggregateAPIInfo.entrySet()) {
            sb.append("    - " + entry.getKey());
            for (Map<String, String> m : entry.getValue()) {
                sb.append("[");
                for (Map.Entry<String, String> attrib : m.entrySet()) {
                    sb.append(" " + attrib.getKey() + "=" + attrib.getValue());
                }
                sb.append("]");
            }
            sb.append("\n");
        }
        sb.append("  SPI:\n");
        for (Map.Entry<PkgInfo, Set<Map<String, String>>> entry : AggregateSPIInfo.entrySet()) {
            sb.append("    - " + entry.getKey());
            for (Map<String, String> m : entry.getValue()) {
                sb.append("[");
                for (Map.Entry<String, String> attrib : m.entrySet()) {
                    sb.append(" " + attrib.getKey() + "=" + attrib.getValue());
                }
                sb.append("]");
            }
            sb.append("\n");
        }
        for (Map.Entry<ApiSpiJarKey, Map<String, Boolean>> e : apiSpiJarNameToContainedResources.entrySet()) {
            sb.append("    - DEVJAR: " + e.getKey());
            sb.append("\n     - [");
            for (Map.Entry<String, Boolean> m : e.getValue().entrySet()) {
                sb.append(m.getKey());
                sb.append(", ");
            }
            sb.append("]");
            sb.append("\n");
        }
        return sb.toString();
    }

//		/**
//		 * Builds up a set of FeatureInfo starting from a given FeatureInfo, using a map of featureName to featureInfo to resolve nested features.
//		 * @param f The feature to start from
//		 * @param set The set to populate with features from (and including) f.
//		 * @param featureNameToFeatureInfo map used to resolve included features by name.
//		 */
//		private void OLDaggregateFeatureForXmlSerialize(FeatureInfo f, Set<FeatureInfo> set, Map<String,FeatureInfo> featureNameToFeatureInfo){
//			if(!set.contains(f)){
//				set.add(f);
//				for(String childFeatureName : f.getContentFeatures().keySet()){
//					FeatureInfo childFeature = featureNameToFeatureInfo.get(childFeatureName);
//					if(childFeature==null){
//						System.err.println("ERROR: Unable to locate child feature "+childFeatureName+" for feature "+f.getName());
//					}else{
//						if(!set.contains(childFeature)){
//							aggregateFeatureUsingNarrowedSingletonChoices(childFeature, set, featureNameToFeatureInfo);
//						}
//					}
//				}
//			}
//		}

    /**
     * Builds up a set of FeatureInfo starting from a given FeatureInfo, using a map of featureName to featureInfo to resolve nested features.<p>
     *
     * @param f The feature to start from
     * @param set The set to populate with features from (and including) f.
     * @param featureNameToFeatureInfo map used to resolve included features by name.
     */
    private static void aggregateFeatureUsingNarrowedSingletonChoices(FeatureInfo f, Set<FeatureInfo> set, Map<String, FeatureInfo> featureNameToFeatureInfo) {
        if (!set.contains(f)) {
            set.add(f);
            //problem.. if there are tolerates that are not singletons..
            //solution.. tolerates will only ever be singletons (re tom.w)
            for (Map.Entry<String, String> childFeatureName : f.getContentFeatures().entrySet()) {
                List<FeatureInfo> matches = new ArrayList<FeatureInfo>();

                String name = childFeatureName.getKey();
                String setId = name.substring(0, name.lastIndexOf('-'));

                FeatureInfo childFeature = featureNameToFeatureInfo.get(childFeatureName.getKey());

                if (childFeature != null)
                    matches.add(childFeature);

                //if this content allows tolerates, only one option should be present..
                String tolerates = childFeatureName.getValue();
                if (tolerates != null) {
                    String[] versions = tolerates.split(",");
                    for (String version : versions) {
                        FeatureInfo tolerated = featureNameToFeatureInfo.get(setId + "-" + version);
                        if (tolerated != null)
                            matches.add(tolerated);
                    }
                }

                if (matches.size() != 1) {
                    if (matches.size() > 1) {
                        String matchNames = "";
                        for (FeatureInfo m : matches) {
                            matchNames += " " + m.getName();
                        }
                        System.err.println("ERROR: multiple matches [" + matchNames + "] present in runtime for singleton choice " + name
                                           + ((tolerates != null) ? " tolerating " + tolerates : "(no tolerates)"));
                    }
                } else {
                    childFeature = matches.get(0);
                }

                if (childFeature == null) {
                    System.err.println("ERROR: Unable to locate child feature " + childFeatureName + " for feature " + f.getName());
                } else {
                    if (!set.contains(childFeature)) {
                        aggregateFeatureUsingNarrowedSingletonChoices(childFeature, set, featureNameToFeatureInfo);
                    }
                }
            }
        }
    }

    public void collectAggregateInformation(Map<String, FeatureInfo> featureNameToFeatureInfo) {
        if (aggregateIsComplete)
            return;
        else {
            //although FeatureInfo doesn't implement hashcode/equals, we
            //only have one instance of FeatureInfo per feature, so can use
            //instance equality for this map.
            Set<FeatureInfo> aggregate = new HashSet<FeatureInfo>();
            aggregateFeatureUsingNarrowedSingletonChoices(this, aggregate, featureNameToFeatureInfo);
            if (aggregate.isEmpty()) {
                System.out.println("error: aggregate came back without even itself in the set!!");
            }

            //add in all kernel features as if this feature included them directly.
            //since effectively at runtime that's one way to think about it.
            //TODO: define kernel somewhere sane!!
            String[] kernelFeatures = {
                                        "com.ibm.websphere.appserver.kernel-1.0",
                                        "com.ibm.websphere.appserver.kernelCore-1.0",
                                        "com.ibm.websphere.appserver.os400.extensions-1.0",
                                        "com.ibm.websphere.appserver.zos.extensions-1.0",
                                        "com.ibm.websphere.appserver.zos.no.console.extensions-1.0",
                                        "com.ibm.websphere.appserver.logging-1.0",
                                        "com.ibm.websphere.appserver.binary.logging-1.0" };
            for (String kfeat : kernelFeatures) {
                if (featureNameToFeatureInfo.containsKey(kfeat)) {
                    aggregate.add(featureNameToFeatureInfo.get(kfeat));
                }
            }

            this.aggregateFeatureInfo = aggregate;

            Set<String> featureNames = new TreeSet<String>();
            Map<PkgInfo, Map<String, Set<String>>> combinedApi = new TreeMap<PkgInfo, Map<String, Set<String>>>();
            Map<PkgInfo, Map<String, Set<String>>> combinedSpi = new TreeMap<PkgInfo, Map<String, Set<String>>>();
            Map<PkgInfo, Set<FeatureInfo>> apiDeclarerMap = new TreeMap<PkgInfo, Set<FeatureInfo>>();
            Map<PkgInfo, Set<FeatureInfo>> spiDeclarerMap = new TreeMap<PkgInfo, Set<FeatureInfo>>();
            for (FeatureInfo member : aggregate) {
                featureNames.add(member.getName());
                String apispi = "api";
                for (Map.Entry<PkgInfo, Set<Map<String, String>>> e : member.getLocalAPI().entrySet()) {
                    if (!combinedApi.containsKey(e.getKey())) {
                        combinedApi.put(e.getKey(), new TreeMap<String, Set<String>>());
                    }
                    if (!apiDeclarerMap.containsKey(e.getKey())) {
                        apiDeclarerMap.put(e.getKey(), new TreeSet<FeatureInfo>());
                    }
                    apiDeclarerMap.get(e.getKey()).add(member);
                    //now process the attributes for the package..
                    Map<String, Set<String>> map = combinedApi.get(e.getKey());
                    for (Map<String, String> pmap : e.getValue()) {
                        for (Map.Entry<String, String> s : pmap.entrySet()) {
                            if (!map.containsKey(s.getKey())) {
                                map.put(s.getKey(), new TreeSet<String>());
                            } else {
                                //we met this attribute key before from another feature content line
                                //nested feature, or dupe feature content line..
                                //we're only really interested when the values for attributes clash
                                if (!map.get(s.getKey()).contains(s.getValue())) {
                                    System.out.println("WARNING: Feature " + member.getName() + " " + member.getVersion() + " (as part of " + getName() + ") redeclares package "
                                                       + e.getKey() + " (" + apispi + ") attribute " + s.getKey() + " to be " + s.getValue() + " (existing value known as "
                                                       + map.get(s.getKey()) + ")");
                                }
                            }
                            map.get(s.getKey()).add(s.getValue());
                        }
                    }
                }
                apispi = "spi";
                for (Map.Entry<PkgInfo, Set<Map<String, String>>> e : member.getLocalSPI().entrySet()) {
                    if (!combinedSpi.containsKey(e.getKey())) {
                        combinedSpi.put(e.getKey(), new TreeMap<String, Set<String>>());
                    }
                    if (!spiDeclarerMap.containsKey(e.getKey())) {
                        spiDeclarerMap.put(e.getKey(), new TreeSet<FeatureInfo>());
                    }
                    spiDeclarerMap.get(e.getKey()).add(member);
                    //now process the attributes for the package..
                    Map<String, Set<String>> map = combinedSpi.get(e.getKey());
                    for (Map<String, String> pmap : e.getValue()) {
                        for (Map.Entry<String, String> s : pmap.entrySet()) {
                            if (!map.containsKey(s.getKey())) {
                                map.put(s.getKey(), new HashSet<String>());
                            } else {
                                //we met this attribute key before from another feature content line
                                //nested feature, or dupe feature content line..
                                //we're only really interested when the values for attributes clash
                                if (!map.get(s.getKey()).contains(s.getValue())) {
                                    System.out.println("WARNING: Feature " + member.getName() + " " + member.getVersion() + " (as part of " + getName() + ") redeclares package "
                                                       + e.getKey() + " (" + apispi + ") attribute " + s.getKey() + " to be " + s.getValue() + " (existing value known as "
                                                       + map.get(s.getKey()) + ")");
                                }
                            }
                            map.get(s.getKey()).add(s.getValue());
                        }
                    }
                }
            }
            for (Map.Entry<PkgInfo, Map<String, Set<String>>> e : combinedApi.entrySet()) {
                Map<String, String> flat = new TreeMap<String, String>();
                for (Map.Entry<String, Set<String>> s : e.getValue().entrySet()) {
                    flat.put(s.getKey(), s.getValue().toString());
                }
                for (FeatureInfo declaringFeature : apiDeclarerMap.get(e.getKey())) {
                    addApi(e.getKey(), flat, declaringFeature.getName());
                }
            }
            for (Map.Entry<PkgInfo, Map<String, Set<String>>> e : combinedSpi.entrySet()) {
                Map<String, String> flat = new TreeMap<String, String>();
                for (Map.Entry<String, Set<String>> s : e.getValue().entrySet()) {
                    flat.put(s.getKey(), s.getValue().toString());
                }
                for (FeatureInfo declaringFeature : spiDeclarerMap.get(e.getKey())) {
                    addSpi(e.getKey(), flat, declaringFeature.getName());
                }
            }
            setAggregateReady();
        }
    }

    /**
     * Serialize the known features as xml.
     *
     * Basic checks are performed while evaluating the set, to report if features declare the same
     * package at differing visibility levels.
     *
     * @param pw
     * @param featureNameToFeatureInfo
     */
    public void toXML(PrintWriter pw, Map<String, FeatureInfo> featureNameToFeatureInfo) {
        //we only process the public ones..
        //if("public".equals(getVisibility()) || "protected".equals(getVisibility())){
        if (!aggregateIsComplete) {
            collectAggregateInformation(featureNameToFeatureInfo);
        }

        pw.println("  <feature name=\"" + getName() + "\" version=\"" + getVersion() + "\" visibility=\"" + getVisibility() + "\" singleton=\"" + getSingleton()
                   + "\" autoFeature=\"" + getAutoFeature() + "\">");
        pw.println("    <api>");
        Map<PkgInfo, Set<Map<String, String>>> combinedApi = getAggregateAPI();
        Map<PkgInfo, Set<Map<String, String>>> combinedSpi = getAggregateSPI();
        Map<PkgInfo, Set<String>> apiDeclarerMap = getAggregateAPIDeclarers();
        Map<PkgInfo, Set<String>> spiDeclarerMap = getAggregateSPIDeclarers();
        for (Map.Entry<PkgInfo, Set<Map<String, String>>> e : combinedApi.entrySet()) {
            String version = e.getKey().getVersion() == null ? "" : "version=\"" + e.getKey().getVersion() + "\"";
            pw.print("      <pkg symbname=\"" + e.getKey().getName() + "\" " + version + " ");
            pw.print(" declarers=\"");
            String prefix = "";
            for (String declaringFeature : apiDeclarerMap.get(e.getKey())) {
                pw.print(prefix + declaringFeature);
                prefix = ",";
            }
            pw.print("\" ");
            //dupe attribs possible here if attribs are redeclared by different features
            //(which they are..)
            Set<Map<String, String>> attribs = e.getValue();
            Map<String, Set<String>> collatedAttribs = new HashMap<String, Set<String>>();
            for (Map<String, String> attrib : attribs) {
                for (Map.Entry<String, String> a : attrib.entrySet()) {
                    //avoid duplicate version attrib =)
                    if (!(version != null && "version".equals(a.getKey()))) {
                        if (!collatedAttribs.containsKey(a.getKey())) {
                            collatedAttribs.put(a.getKey(), new HashSet<String>());
                        }
                        String val = a.getValue();
                        if (a.getValue().startsWith("[") && a.getValue().endsWith("]")) {
                            val = val.substring(1, val.length() - 1);
                            String vals[] = val.split(",");
                            collatedAttribs.get(a.getKey()).addAll(Arrays.asList(vals));
                        } else {
                            collatedAttribs.get(a.getKey()).add(val);
                        }
                    }
                }
            }
            for (Map.Entry<String, Set<String>> a : collatedAttribs.entrySet()) {
                pw.print(" " + a.getKey() + "=\"" + a.getValue() + "\"");
            }
            pw.println(" />");
        }
        pw.println("    </api>");
        pw.println("    <spi>");
        for (Map.Entry<PkgInfo, Set<Map<String, String>>> e : combinedSpi.entrySet()) {
            String version = e.getKey().getVersion() == null ? "" : "version=\"" + e.getKey().getVersion() + "\"";
            pw.print("      <pkg symbname=\"" + e.getKey().getName() + "\" " + version + " ");
            pw.print(" declarers=\"");
            String prefix = "";
            for (String declaringFeature : spiDeclarerMap.get(e.getKey())) {
                pw.print(prefix + declaringFeature);
                prefix = ",";
            }
            pw.println("\" />");
        }
        pw.println("    </spi>");

        pw.println("    <jarContent hashcode=\"" + apiSpiJarNameToContainedResources.hashCode() + "\" entryCount=\"" + apiSpiJarNameToContainedResources.size() + "\">");
        for (Map.Entry<ApiSpiJarKey, Map<String, Boolean>> jar : apiSpiJarNameToContainedResources.entrySet()) {
            pw.println("       <jar location=\"" + jar.getKey() + "\">");
            for (String location : jar.getValue().keySet()) {
                pw.println("          <entry>" + location + "</entry>");
            }
            pw.println("       </jar>");
        }
        pw.println("    </jarContent>");

        pw.println("    <featureContent>");
        for (FeatureInfo f : aggregateFeatureInfo) {
            String shortString = f.getShortName() == null ? "" : "shortName=\"" + f.getShortName() + "\"";
            pw.println("       <feature name=\"" + f.getName() + "\" " + shortString + " />");
        }
        pw.println("    </featureContent>");

        pw.println("</feature>");
    }
    //}

}
