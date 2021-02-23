/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.v1.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Version;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class gives a number of AdminCenter manifest utility methods.
 */
public class ManifestUtils {

    private static final TraceComponent tc = Tr.register(ManifestUtils.class);

    public static final String SUBSYSTEM_SYMBOLIC_NAME = "Subsystem-SymbolicName";
    public static final String SUBSYSTEM_NAME = "Subsystem-Name";
    public static final String SUBSYSTEM_CATEGORY = "Subsystem-Category";
    public static final String SUBSYSTEM_VERSION = "Subsystem-Version";
    public static final String SUBSYSTEM_ICON = "Subsystem-Icon";
    public static final String SUBSYSTEM_DESCRIPTION = "Subsystem-Description";
    public static final String SUBSYSTEM_CONTENT = "Subsystem-Content";

    public static final String SUBSYSTEM_ENDPOINT_CONTENT = "Subsystem-Endpoint-Content";
    public static final String SUBSYSTEM_ENDPOINT_ICONS = "Subsystem-Endpoint-Icons";
    public static final String SUBSYSTEM_ENDPOINT_NAMES = "Subsystem-Endpoint-Names";
    public static final String SUBSYSTEM_ENDPOINT_SHORTNAMES = "Subsystem-Endpoint-ShortNames";
    public static final String SUBSYSTEM_ENDPOINT_URLS = "Subsystem-Endpoint-Urls";

    public static final String IBM_SHORT_NAME = "IBM-ShortName";

    public static final String BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";
    public static final String BUNDLE_VERSION_NAME = "Bundle-Version";
    public static final String WEBCONTEXT_PATH_NAME = "Web-ContextPath";

    /**
     * This method returns the tool features found in the supplied location. The installedFeatures variable supplies a list of features, that the tool feature must be in, so
     * this can be used to filter the list.
     * 
     * @param featureDir - The directory to find the features in.
     * @param installedFeatures - A list of Strings that contain feature names. When this is variable is set, we only return the tool features that
     *            that are contained in this list. This can be useful so we only returned installed tool features.
     *            If this argument is null, we return all Tool features contained in the directory.
     * @param installedFeaturePrefix - A prefix that may be needed to be added to feature names to match entries in the installed feature list.
     * @return - A Map whose key is a String containing the symbolicname and version, and the value is another map the contained the manifest
     *         headers.
     */
    public static File[] findAllFeatureManifests(final File featureDir) {
        // Get an Array of all manifest files in the specified directory. 
        File[] manifestFiles = AccessController.doPrivileged(new PrivilegedAction<File[]>() {
            @Override
            public File[] run()
            {
                File[] manifestFiles = featureDir.listFiles(new FileFilter() {

                    @Override
                    public boolean accept(File pathname) {
                        boolean result = false;
                        // Store any files ending in .mf.
                        if (pathname.getName().toLowerCase().endsWith(".mf"))
                            result = true;

                        return result;
                    }

                });
                return manifestFiles;
            }
        });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Found Manifests: " + Arrays.toString(manifestFiles));
        return manifestFiles;
    }

    /**
     * This method returns the tool features found in the supplied location. The installedFeatures variable supplies a list of features, that the tool feature must be in, so
     * this can be used to filter the list.
     * 
     * @param manifestFiles - An Array of File objects that contain the feature manifests.
     * @param installedFeatures - A list of Strings that contain feature names. When this is variable is set, we only return the tool features that
     *            that are contained in this list. This can be useful so we only returned installed tool features.
     *            If this argument is null, we return all Tool features contained in the directory.
     * @param installedFeaturePrefix - A prefix that may be needed to be added to feature names to match entries in the installed feature list.
     * @return - A Map whose key is a String containing the manifest file path, and the value is another map the contained the manifest
     *         headers.
     */
    public static Map<String, Map<String, String>> findToolFeatureManifests(File[] manifestFiles) {
        Map<String, Map<String, String>> featureManifests = new HashMap<String, Map<String, String>>();

        // If we have manifest files, we need to process them. 
        if (manifestFiles != null) {
            for (final File manifestFile : manifestFiles) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Reading Manifest file: " + manifestFile);
                Manifest featureManifest = null;
                try {
                    // Extract the headers from the manifest.
                    featureManifest = AccessController.doPrivileged(new PrivilegedExceptionAction<Manifest>() {
                        @Override
                        public Manifest run() throws FileNotFoundException, IOException
                        {
                            return ManifestProcessor.parseManifest(new FileInputStream(manifestFile));
                        }
                    });
                } catch (PrivilegedActionException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "An exception occurred while reading the feature manifest " +
                                     manifestFile.getAbsolutePath(), e.toString());
                    }
                }
                // Ensure that we have a manifest to process.
                if (featureManifest != null) {
                    // Get the headers into a map.
                    Map<String, String> headers = ManifestProcessor.readManifestIntoMap(featureManifest);
                    // Find the Subsystem Category header which indicates whether we are a tool.
                    String symbolicCategory = headers.get(SUBSYSTEM_CATEGORY);

                    if (symbolicCategory != null) {
                        for (String category : symbolicCategory.split(",")) {
                            if ("admincenter".equals(category.toLowerCase())) {
                                featureManifests.put(manifestFile.getAbsolutePath(), headers);

                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    Tr.debug(tc, "Adding Manifest file " + manifestFile.getAbsolutePath() + " to list of tools.");
                            } else {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    Tr.debug(tc, "Manifest file " + manifestFile.getAbsolutePath() + " is not a tool manifest");
                            }
                        }
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Unable to read Manifest file: " + manifestFile.getAbsolutePath());
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Manifest files to process is null.");
        }

        return featureManifests;
    }

    /**
     * This method returns the installed feature name. This is worked out by:
     * 
     * Public features : Return the IBM Short name, or Subsystem SymbolicName if short name is not supplied.
     * Protected features: Return the Subsystem SymbolicName.
     * Private features : Return the Subsystem SymbolicName.
     * 
     * @param symbolicNameHeader - A String containing the SubsystemSymbolicName manifest header.
     * @param shortName - A String containing the IBM-Shortname manifest header.
     * @param featurePrefix - A Product Extension prefix. This can be "" if no prefix is set. Null will set to "".
     * @return
     */
    public static String getInstalledFeatureName(String symbolicNameHeader, String shortName, String featurePrefix) {

        String installedFeatureName = null;
        // Get the Subsystem symbolicName and visibility. This is found on the Subsystem-SymbolicName header, if set.
        NameValuePair nvpSymbolicName = ManifestHeaderProcessor.parseBundleSymbolicName(symbolicNameHeader);
        String symbolicName = nvpSymbolicName.getName();
        Map<String, String> symbolicNameAttrs = nvpSymbolicName.getAttributes();
        String visibility = symbolicNameAttrs.get("visibility:");
        if (visibility == null) {
            visibility = "private";
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Visibility for " + symbolicName + " is " + visibility);

        // If the featurePrefix is set to null, set it to empty string.
        if (featurePrefix == null)
            featurePrefix = "";

        if ("public".equals(visibility.toLowerCase())) {
            if (shortName != null) {
                installedFeatureName = featurePrefix + shortName;
            } else {
                installedFeatureName = featurePrefix + symbolicName;
            }
        } else {
            installedFeatureName = featurePrefix + symbolicName;
        }

        return installedFeatureName;
    }

    /**
     * This method processes the WAB bundle Manifest.
     * 
     * @param bundle - The File that is the bundle to process.
     * @return - A SortedBundle that contains the SymbolicName and version, plus the Web-ContextPath.
     */
    public static SortedBundle processBundleManifest(final File bundle) {
        SortedBundle sortedBundle = null;

        if (bundle != null) {
            Map<String, String> manifestAttrs = null;
            try {
                manifestAttrs = AccessController.doPrivileged(new PrivilegedExceptionAction<Map<String, String>>() {
                    @Override
                    public Map<String, String> run() throws ZipException, IOException
                    {
                        ZipFile zipFile = null;
                        InputStream zis = null;
                        Map<String, String> attrs = null;
                        try {
                            // Open the bundle as a zipfile. Read the manifest and access the manifest headers.
                            zipFile = new ZipFile(bundle);
                            ZipEntry zipEntry = zipFile.getEntry(JarFile.MANIFEST_NAME);
                            zis = zipFile.getInputStream(zipEntry);
                            attrs = ManifestProcessor.readManifestIntoMap(ManifestProcessor.parseManifest(zis));
                        } finally {
                            if (zis != null)
                                zis.close();
                            if (zipFile != null)
                                zipFile.close();
                        }

                        return attrs;
                    }
                });
            } catch (PrivilegedActionException e) {
                // Issue FFDC to say that we can't install the feature.
            }

            // If we have any manifest headers, then process this manifest.
            if (manifestAttrs != null) {

                String symbolicName = manifestAttrs.get(BUNDLE_SYMBOLIC_NAME);
                String versionString = manifestAttrs.get(BUNDLE_VERSION_NAME);
                // Ensure we have a Symbolicname and version. If we do, then get the Web-ContextPath if there is one, 
                // and store in a SortedBundle.
                if (symbolicName != null && versionString != null) {
                    symbolicName = ManifestHeaderProcessor.parseBundleSymbolicName(symbolicName).getName();
                    Version version = new Version(versionString);
                    String webContextPath = manifestAttrs.get(WEBCONTEXT_PATH_NAME);
                    // See if this is a AdminCenter endpoint bundle. If we have the header, and it is set to true then
                    // set the boolean to indicate that this is a valid endpoint.
                    String uiEndpointFeatureName = manifestAttrs.get("IBM-AdminCenter-PrimaryEndpoint");
                    sortedBundle = new SortedBundle(symbolicName, version, webContextPath, uiEndpointFeatureName);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Creating Sorted Bundle: " + sortedBundle);
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Problem reading Manifest Attrs for " + bundle.getAbsolutePath());
            }
        }
        return sortedBundle;
    }
}
