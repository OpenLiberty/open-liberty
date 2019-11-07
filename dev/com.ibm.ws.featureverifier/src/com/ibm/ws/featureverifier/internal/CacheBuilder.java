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
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import com.ibm.aries.buildtasks.semantic.versioning.model.VersionedEntity;

/**
 *
 */
public class CacheBuilder {

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

    private void collectJarsUnderPath(File devDir, Set<File> jars) {
        for (File f : devDir.listFiles()) {
            if (f.isDirectory()) {
                collectJarsUnderPath(f, jars);
            } else {
                if (f.getName().toLowerCase().endsWith(".jar")) {
                    jars.add(f);
                }
            }
        }
    }

    private Map<VersionedEntity, String> collectDevJars(File devDir, File installDir) throws IOException {
        Set<File> jars = new HashSet<File>();
        collectJarsUnderPath(devDir, jars);
        Map<VersionedEntity, String> results = new HashMap<VersionedEntity, String>();
        for (File j : jars) {
            JarFile jf = new JarFile(j);
            try {
                Manifest m = jf.getManifest();

                if (m != null) {
                    //eg.
                    //Bundle-SymbolicName: com.ibm.ws.org.osgi.core.4.2.0
                    //Bundle-Version: 1.0.1.201410061245
                    String symbname = m.getMainAttributes().getValue("Bundle-SymbolicName");
                    if (symbname != null) {
                        NameValuePair name = ManifestHeaderProcessor.parseBundleSymbolicName(symbname);
                        String version = m.getMainAttributes().getValue("Bundle-Version");
                        VersionedEntity ve = new VersionedEntity(name.getName(), version);
                        String path = j.getCanonicalPath().substring(installDir.getCanonicalPath().length() + 1);

                        if (File.separatorChar != '/') {
                            //windows system.. 
                            path = path.replace('\\', '/');
                        }

                        results.put(ve, path);
                    }
                }
            } finally {
                jf.close();
            }
        }
        return results;
    }

    private String[] processPossibleDevJar(String featureName, File installDir, Map.Entry<String, Map<String, String>> contentEntry, Map<VersionedEntity, String> devJars,
                                           String type) throws Exception {
        String result[] = null;
        String versionString = contentEntry.getValue().get(VERSION_ATTRIBUTE_NAME);
        String locationString = contentEntry.getValue().get(LOCATION_DIRECTIVE_NAME);
        if (locationString != null) {
            if (locationString.contains("dev/")) {
                System.out.println("** API/SPI VERSIONED " + type + ": " + contentEntry.getKey() + " location " + locationString);
                String locationkey = contentEntry.getKey() + locationString;

                
                String relativePath = null;
                File jarSelected = null;
                if (versionString != null) {
                    VersionRange vr = new VersionRange(versionString);
                    for (Map.Entry<VersionedEntity, String> ve : devJars.entrySet()) {
                        if (ve.getKey().getName().equals(contentEntry.getKey()) && vr.includes(new Version(ve.getKey().getVersion()))) {
                            jarSelected = new File(installDir, ve.getValue());
                            relativePath = ve.getValue();
                        }
                    }
                    if (jarSelected == null) {
                        //try again with absolute location.
                        jarSelected = new File(installDir, locationString);
                        relativePath = locationString;
                        if (!jarSelected.exists()) {
                            System.out.println("##Could not build info for " + featureName + " " + contentEntry.getKey() + " loc " + locationString + " ver " + versionString);
                            throw new IllegalStateException("");
                        }
                    }
                } else {
                    jarSelected = new File(installDir, locationString);
                    relativePath = locationString;
                    //absolute jar references without version attribs are keyed by just the locationString.. 
                    if (locationString.endsWith(".jar"))
                        locationkey = locationString;
                }
                ZipFile zf = new ZipFile(jarSelected);
                try {
                    String manifestString = null;
                    Enumeration<? extends ZipEntry> entries = zf.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry ze = entries.nextElement();
                        String zipEntryName = ze.getName();
                        if (!ze.isDirectory() && zipEntryName.equals("META-INF/MANIFEST.MF")) {
                            System.out.println("Found manifest for " + locationString);
                            Manifest m = ManifestProcessor.parseManifest(zf.getInputStream(ze));
                            Map<String, String> manifestMap = ManifestProcessor.readManifestIntoMap(m);
                            System.out.println("Read into map");
                            NameValuePair symbNameParsed = ManifestHeaderProcessor.parseBundleSymbolicName(manifestMap.get(Constants.BUNDLE_SYMBOLICNAME));
                            String symbName = symbNameParsed.getName();
                            String version = manifestMap.get(Constants.BUNDLE_VERSION);
                            System.out.println("Name: " + symbName + " Version: " + version);
                            if (File.separator != "/") {
                                relativePath.replaceAll("\\\\", "/");
                            }
                            //a bit of a cheat.. we'll remember the symbname/version as a special resource
                            manifestString = "##MANIFEST.MF##NAME##" + symbName + "##VERSION##" + version + "##FILE##" + relativePath + "##TYPE##" + type;
                            System.out.println("done gathering name");
                            break;
                        }
                    }
                    if (manifestString != null) {
                        return new String[] { locationkey, manifestString };
                    } else {
                        System.out.println("##Did not find manifest for  " + featureName + " " + contentEntry.getKey() + " " + locationString + " using jar " + relativePath);
                        throw new IllegalStateException();
                    }
                } finally {
                    zf.close();
                }

            }
        }
        return result;
    }

    private void go() throws Exception {

        String installDirString = "/home/ajo1/LIBERTY/8554IFIX/build.image/wlp";
        File installDir = new File(installDirString);

        //now hunt down all the manifests
        List<File> manifestsToProcess = new ArrayList<File>();
        File libDir = new File(installDir, "lib");
        File featureDir = new File(libDir, "features");
        File libmanifests[] = featureDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toUpperCase().endsWith(".MF");
            }
        });
        manifestsToProcess.addAll(Arrays.asList(libmanifests));
        File platformDir = new File(libDir, "platform");
        File platformmanifests[] = platformDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toUpperCase().endsWith(".MF");
            }
        });
        manifestsToProcess.addAll(Arrays.asList(platformmanifests));

        //now build a map of symbolic name/versions to jar paths under dev.. 
        //we'll need this for all those content lines that do type=jar with a version range.
        File devDir = new File(installDir, "dev");
        Map<VersionedEntity, String> devJars = collectDevJars(devDir, installDir);

        //hey.. apparently some dev jars are actually in lib.. this may be historical due
        //to the old apichecker, but thanks to that, we need to look there too.
        Map<VersionedEntity, String> libJars = collectDevJars(libDir, installDir);
        Map<VersionedEntity, String> allJars = new HashMap<VersionedEntity, String>();
        allJars.putAll(devJars);
        allJars.putAll(libJars);

        List<String[]> results = new ArrayList<String[]>();
        for (File manifest : manifestsToProcess) {
            Manifest m = ManifestProcessor.parseManifest(new FileInputStream(manifest));
            Map<String, String> manifestMap = ManifestProcessor.readManifestIntoMap(m);
            String contentString = manifestMap.get("Subsystem-Content");
            //can reuse the import string parser ;p
            Map<String, Map<String, String>> contentMap = ManifestHeaderProcessor.parseImportString(contentString);

            for (Map.Entry<String, Map<String, String>> contentEntry : contentMap.entrySet()) {
                String type = DEFAULT_CONTENT_TYPE;
                if (contentEntry.getValue().containsKey(API_TYPE_ATTRIBUTE_NAME)) {
                    type = contentEntry.getValue().get(API_TYPE_ATTRIBUTE_NAME);
                }
                if (DEFAULT_CONTENT_TYPE.equals(type) || OSGI_BUNDLE_CONTENT_TYPE.equals(type)) {
                    String[] res = processPossibleDevJar(manifest.getName(), installDir, contentEntry, allJars, "BUNDLE");
                    if (res != null)
                        results.add(res);
                } else if (JAR_CONTENT_TYPE.equals(type) || BOOT_JAR_CONTENT_TYPE.equals(type)) {
                    String[] res = processPossibleDevJar(manifest.getName(), installDir, contentEntry, allJars, "JAR");
                    if (res != null)
                        results.add(res);
                }
            }
        }

        System.out.println("#######");
        System.out.println("#######");
        for (String[] result : results) {
            System.out.println(result[0]);
            System.out.println(result[1]);
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        CacheBuilder cb = new CacheBuilder();
        cb.go();
    }

}
