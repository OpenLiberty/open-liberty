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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.VersionRange;

import com.ibm.ws.featureverifier.internal.NewManifestProcessor;
import com.ibm.ws.featureverifier.internal.XmlErrorCollator.ReportType;

/**
 * 
 */
public class NewFeatureInfo extends FeatureInfo {

    /*
     * These are all of the feature Attribute.NAME's that will appear in the Manifests.
     * They have been updated for this NewFeatureInfo to match the new manifest style.
     */

    private static final String FEATURE_HEADER_NAME = "features";
    private static final String ZOS_FEATURE_HEADER_NAME = "zos_features";
    private static final String BUNDLE_HEADER_NAME = "bundles";
    private static final String JAR_HEADER_NAME = "jars";

    private static final String SINGLETON_DIRECTIVE_NAME = "singleton";
    private static final String THIRD_PARTY_API_TYPE_VALUE = "third-party";
    private static final String INTERNAL_API_TYPE_VALUE = "internal";
    private static final String API_TYPE_ATTRIBUTE_NAME = "type";

    private static final String VISIBILITY_DIRECTIVE_NAME = "visibility:";
    private static final String DEFAULT_FEATURE_VISIBILITY = "private";

    final Collection<ErrorInfo> issuesFoundParsingManifest = new ArrayList<ErrorInfo>();

    /**
     * Partial constructor. To be used when creating a FeatureInfo from a Manifest.
     * 
     * @param name
     * @param version
     * @param visibility
     * @param shortName
     * @param featureAttribs
     */
    public NewFeatureInfo(String name, String version, String visibility, String shortName, Map<String, String> featureAttribs) {
        super(name, version, visibility, shortName, featureAttribs);
    }

    /**
     * Constructor.
     * 
     * @param name
     * @param version
     * @param visibility
     * @param shortName
     * @param featureAttribs
     * @param singleton
     * @param autofeature
     */
    public NewFeatureInfo(String name, String version, String visibility, String shortName, Map<String, String> featureAttribs, boolean singleton, boolean autofeature) {
        super(name, version, visibility, shortName, featureAttribs, singleton, autofeature);
    }

    /**
     * A method used to create a FeatureInfo object using the data from a Manifest file.
     * 
     * @param manifest The manifest file to read properties from.
     * @param installDir The installation directory.
     * @param context
     * @param devJars
     * @return The new FeatureInfo object
     * @throws IOException
     */
    public static FeatureInfo createFromManifest(File manifestFile, String installDir, BundleContext context, Map<VersionedEntity, String> devJars) throws IOException {
        String manifestFileName = manifestFile.getName();
        String symbolicName = null;
        String visibility = DEFAULT_FEATURE_VISIBILITY;
        Map<String, String> symbolicNameAttributes = new HashMap<String, String>();

        List<ErrorInfo> errors = new ArrayList<ErrorInfo>();

        Manifest manifestObject = null;
        Map<String, String> manifestMap = null;

        /*
         * Parse the input manifest file into a Manifest object. Then run the object through the
         * NewManifestProcessor.readManifestIntoMap() to get a Map of the properties.
         */
        manifestObject = NewManifestProcessor.parseManifest(manifestFile);
        NewManifestProcessor.replaceVariables(manifestObject);
        manifestMap = NewManifestProcessor.readManifestIntoMap(manifestObject);

        /*
         * Scan the attributes for duplicate entries. If multiple entries for the same attribute
         * are found, instead of allowing for multiple entries the Map is changed like so:
         * {A -> B, A -> C} turns into {A -> [B, C]}
         */
        Attributes attributes = manifestObject.getMainAttributes();
        Map<String, List<String>> duplicateCheckMap = new HashMap<String, List<String>>();
        for (Entry<Object, Object> attribute : attributes.entrySet()) {
            String key = attribute.getKey().toString();
            String value = attribute.getValue().toString();

            if (!duplicateCheckMap.containsKey(key))
                duplicateCheckMap.put(key, new ArrayList<String>());

            duplicateCheckMap.get(key).add(value);
        }

        /*
         * <PROPERTY 1> symbolicName
         * <PROPERTY 2> visibility
         * Pull out the symbolicName and visibility via the ManifestHeaderProcessor
         */
        List<String> duplicateHeaders = duplicateCheckMap.get(SUBSYSTEM_SYMBOLIC_NAME_HEADER_NAME);
        if (duplicateHeaders != null && !duplicateHeaders.isEmpty()) {
            boolean duplicatesExist = (duplicateHeaders.size() > 1);

            String rawSymbolicName = duplicateHeaders.iterator().next();
            ManifestHeaderProcessor.NameValuePair parsedSymbolicName;

            try {
                // Parse the symbolic name from the rawSymbolicName
                parsedSymbolicName = ManifestHeaderProcessor.parseBundleSymbolicName(rawSymbolicName);
                symbolicName = parsedSymbolicName.getName();

                // If the parsedSymbolicName has a visibility attribute, save it.
                if (parsedSymbolicName.getAttributes() != null && parsedSymbolicName.getAttributes().get(VISIBILITY_DIRECTIVE_NAME) != null)
                    visibility = parsedSymbolicName.getAttributes().get(VISIBILITY_DIRECTIVE_NAME);

                // Save all of the parsedSymbolicNames attributes
                symbolicNameAttributes = parsedSymbolicName.getAttributes();
            } catch (Exception exception) {
                ErrorInfo errorInfo = new ErrorInfo();

                errorInfo.type = ReportType.ERROR;
                errorInfo.shortText = "[BAD_SYMBOLIC_NAME " + rawSymbolicName + "]";
                errorInfo.summary = "SymbolicName did not parse correctly from " + manifestFileName;
                errorInfo.detail = "When attempting to load feature manifest for feature from manifest " + manifestFileName
                                   + " the content of the SubSystem-SymbolicName header was unable to be parsed successfully. The header had content " + rawSymbolicName;
                errors.add(errorInfo);
                symbolicName = "BadSymbolicNameFor" + manifestFile.getName();
            }

            if (duplicatesExist)
                symbolicName = "DuplicateSymbolicNameFor" + symbolicName;
        } else {
            ErrorInfo errorInfo = new ErrorInfo();

            errorInfo.type = ReportType.ERROR;
            errorInfo.shortText = "[MISSING_SYMBOLIC_NAME " + manifestFileName + "]";
            errorInfo.summary = "SymbolicName missing from manifest " + manifestFileName;
            errorInfo.detail = "When attempting to load feature manifest for feature manifest file " + manifestFileName + " no SubSystem-SymbolicName header was encountered.";
            errors.add(errorInfo);
        }

        /*
         * Look through every property from the Manifest. If there was more than one entry for it
         * in the Manifest file, create an error to log it. Then clear and remove the duplicateCheckMap.
         */
        for (Entry<String, List<String>> duplicateCheckEntry : duplicateCheckMap.entrySet()) {
            if (duplicateCheckEntry.getValue().size() > 1) {
                ErrorInfo errorInfo = new ErrorInfo();

                errorInfo.type = ReportType.ERROR;
                errorInfo.shortText = "[DUPLICATE_MANIFEST_ENTRY " + duplicateCheckEntry.getKey() + "]";
                errorInfo.summary = "Duplicate Manifest Header " + duplicateCheckEntry.getKey() + " in feature manifest file " + manifestFileName;
                errorInfo.detail = "When attempting to load feature manifest from manifest file " + manifestFileName + " duplicate headers were found with the header key "
                                   + duplicateCheckEntry.getKey() + " with values of " + duplicateCheckEntry.getValue();
                errors.add(errorInfo);
            }
        }
        duplicateCheckMap.clear();
        duplicateCheckMap = null;

        /*
         * <PROPERTY 3>
         * Pull out the subsystem version string
         */
        String versionString = manifestMap.get(SUBSYSTEM_VERSION_HEADER_NAME);

        /*
         * <PROPERTY 4>
         * Pull out the ibm shortname string.
         */
        String ibmShortname = manifestMap.get("IBM-ShortName");

        // Create the featureInfo object that will be returned.
        NewFeatureInfo featureInfo = new NewFeatureInfo(symbolicName, versionString, visibility, ibmShortname, symbolicNameAttributes);
        featureInfo.source = manifestFile.getAbsolutePath();

        // Add all the errors found so far to the FeatureInfo and reset the errors tracked
        featureInfo.issuesFoundParsingManifest.addAll(errors);
        errors = null;

        // Read in the API and SPI information
        String apiString = manifestMap.get(IBM_API_PACKAGE_HEADER_NAME);
        String spiString = manifestMap.get(IBM_SPI_PACKAGE_HEADER_NAME);

        processPackageString(apiString, featureInfo.LocalAPIInfo);
        processPackageString(spiString, featureInfo.LocalSPIInfo);

        if (manifestMap.containsKey("IBM-Test-Feature"))
            featureInfo.isTestFeature = true;

        /*
         * <PROPERTY 5>
         * If this is an autofeature, pull out the header content. Also, validate that it does not create
         * a public api/spi.
         */
        if (manifestMap.containsKey(AUTOFEATURE_HEADER_NAME)) {
            featureInfo.autofeatureHeaderContent = manifestMap.get(AUTOFEATURE_HEADER_NAME);
            featureInfo.isAutoFeature = true;

            Set<Entry<PkgInfo, Set<Map<String, String>>>> apiAndspiInfoSet = new HashSet<Entry<PkgInfo, Set<Map<String, String>>>>();
            apiAndspiInfoSet.addAll(featureInfo.LocalAPIInfo.entrySet());
            apiAndspiInfoSet.addAll(featureInfo.LocalSPIInfo.entrySet());

            // Loop through API/SPI entries looking for non-internal, non-third-party APIs
            for (Entry<PkgInfo, Set<Map<String, String>>> entry : apiAndspiInfoSet) {
                for (Map<String, String> attributeMap : entry.getValue()) {
                    String type = attributeMap.get(API_TYPE_ATTRIBUTE_NAME);

                    if (type != null) {
                        if (type.contains(INTERNAL_API_TYPE_VALUE) || type.contains(THIRD_PARTY_API_TYPE_VALUE)) {
                            // NOOP
                        } else {
                            System.out.println("WARNING: Auto Feature " + symbolicName + " is attempting to declar api/spi with type " + type
                                               + " which will make user API/SPI shape nonconsistent for affected features");
                        }
                    }
                }
            }
        }

        /*
         * <PROPERTY 6>
         * Extract out the feature dependencies. In this style of manifest that's going to be
         * everything under -features, and everything under -zos.features
         */
        featureInfo.contentFeatures = new HashMap<String, String>();

        // Add any features that are dependencies
        if (manifestMap.containsKey(FEATURE_HEADER_NAME)) {
            // Grab the list of all of the features
            String featuresToAdd = manifestMap.get(FEATURE_HEADER_NAME);

            // Split the feature list on the commas to get an arrray of each feature
            Set<String> listOfFeaturesToAdd = ManifestHeaderProcessor.parseImportString(featuresToAdd).keySet();

            // Add each feature to the FeatureInfo. They should be mapped to Versions, but that isn't in the manifest
            for (String feature : listOfFeaturesToAdd) {
                featureInfo.contentFeatures.put(feature.trim(), null);
            }
        }

        // Add any z/OS features that are dependencies
        if (manifestMap.containsKey(ZOS_FEATURE_HEADER_NAME)) {
            // Grab the list of all the features
            String featuresToAdd = manifestMap.get(ZOS_FEATURE_HEADER_NAME);

            // Split the feature list on the commas to get an array of each feature
            Set<String> listOfFeaturesToAdd = ManifestHeaderProcessor.parseImportString(featuresToAdd).keySet();

            // Add each feature to the FeatureInfo. They should be mapped to Versions,  but that isn't in the manifest
            for (String feature : listOfFeaturesToAdd) {
                featureInfo.contentFeatures.put(feature.trim(), null);
            }
        }

        /*
         * <PROPERTY 7>
         * Extract out the bundle dependencies. In this style of manifest that's going to be
         * everything under -bundle.
         */
        featureInfo.contentBundles = new HashMap<String, Set<VersionRange>>();

        if (manifestMap.containsKey(BUNDLE_HEADER_NAME)) {
            // Grab the list of all the bundles
            String bundlesToAdd = manifestMap.get(BUNDLE_HEADER_NAME);

            // Split the bundle list using the ManifestHeaderProcessor (it will handle directives,
            // which is nice since directives may include commas, which mess up a simple split.
            // The left hand side of the map will be the names
            Set<String> listOfBundlesToAdd = ManifestHeaderProcessor.parseImportString(bundlesToAdd).keySet();

            // Add each bundle to the FeatureInfo. They should be mapped to Sets of allowable version, but that isn't in the manifest
            for (String bundle : listOfBundlesToAdd) {
                featureInfo.contentBundles.put(bundle.trim(), null);
            }
        }

        /*
         * <PROPERTY 8>
         * Extract out the jar dependencies. In this style of manifest that's going to be
         * everything under -jars. These are going into multiple places. They get mapped into
         * FeatureInfo.apiSpiJarNameToContainedResources, but they also get put into the devJars list
         * For consistency with the superclass, they also go into the content bundles.
         */
        featureInfo.apiSpiJarNameToContainedResources = new TreeMap<ApiSpiJarKey, Map<String, Boolean>>();

        if (manifestMap.containsKey(JAR_HEADER_NAME)) {
            // Grab the list of all the jars
            String jarsToAdd = manifestMap.get(JAR_HEADER_NAME);

            /*
             * It would be very nice if we could just split on the commas and be done, but
             * that doesn't work because there are commas in the version string. Because of this
             * we need to find another way to do the split. The current tactic is to find the commas
             * not in the version and turn them into another character that we know won't be used. Then split
             * on that new character
             */
            jarsToAdd = jarsToAdd.replaceAll("([^0-9]),", "$1#");

            // Split the jar list on the hash to get an array of info
            String[] listOfJarsToAdd = jarsToAdd.split("#");

            // Loop over the list of jars, pulling out the relevant information
            for (String jar : listOfJarsToAdd) {
                String[] jarInfoArray = jar.split(";");

                // These are the potential elements of the information infoArray
                String jarName = null;
                String jarVersion = null;
                String jarLocation = null;
                int numberOfElements = jarInfoArray.length;

                // The first element is, in all cases, the name
                jarName = jarInfoArray[0].trim();

                // If there is a second element, process it
                if (2 <= numberOfElements) {
                    // Split the property into its name and value
                    String[] rawElementData = jarInfoArray[1].split("=", 2);

                    if (rawElementData[0].contains("version")) {
                        // This is the version information
                        // Remove quotes from the data if they have it
                        if (-1 != rawElementData[1].indexOf('\"')) {
                            rawElementData[1] = rawElementData[1].substring(1, rawElementData.length - 1);
                        }

                        // Assign the version information
                        jarVersion = rawElementData[1].trim();
                    } else if (rawElementData[0].contains("location")) {
                        // This is the location information
                        // Remove quotes from the data if they have it
                        if (-1 != rawElementData[1].indexOf('\"')) {
                            rawElementData[1] = rawElementData[1].substring(1, rawElementData.length - 1);
                        }

                        // Assign the version information
                        jarLocation = rawElementData[1].trim();
                    }
                }

                // If there is a third element, process it
                if (3 == numberOfElements) {
                    // Split the property into its name and value
                    String[] rawElementData = jarInfoArray[2].split("=", 2);

                    if (rawElementData[0].contains("version")) {
                        // This is the version information, but I don't think we can get here. I don't think manifests can be formatted like this.
                        // Remove quotes from the data if they have it
                        if (-1 != rawElementData[1].indexOf('\"')) {
                            rawElementData[1] = rawElementData[1].substring(1, rawElementData.length - 1);
                        }

                        // Assign the version information
                        jarVersion = rawElementData[1].trim();
                    } else if (rawElementData[0].contains("location")) {
                        // This is the location information
                        // Remove quotes from the data if they have it
                        if (-1 != rawElementData[1].indexOf('\"')) {
                            rawElementData[1] = rawElementData[1].substring(1, rawElementData.length - 1);
                        }

                        // Assign the version information
                        jarLocation = rawElementData[1].trim();
                    }
                }

                ApiSpiJarKey apiSpiJarKey = new ApiSpiJarKey(ApiSpiJarKey.ContentType.JAR, manifestFile.getName(), jarName, jarVersion, jarLocation);

                // Add the mapping
                featureInfo.apiSpiJarNameToContainedResources.put(apiSpiJarKey, NewFeatureInfo.collectResourcesInJar(featureInfo.getName(), jarLocation, installDir));

                featureInfo.contentBundles.put(jarName, null);
            }
        }

        /*
         * <PROPERTY 9>
         * Determine if this is a singleton feature.
         */
        if (manifestMap.containsKey(SINGLETON_DIRECTIVE_NAME))
            featureInfo.singleton = (manifestMap.get(SINGLETON_DIRECTIVE_NAME).equals("true")) ? true : false;

        // Return the FeatureInfo object
        return featureInfo;
    }
}
