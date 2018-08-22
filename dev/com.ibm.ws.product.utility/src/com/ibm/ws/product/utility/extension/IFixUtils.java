/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.extension;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.aries.util.manifest.BundleManifest;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.w3c.dom.Document;

import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;
import com.ibm.ws.product.utility.CommandConsole;
import com.ibm.ws.product.utility.CommandUtils;
import com.ibm.ws.product.utility.extension.ifix.xml.BundleFile;
import com.ibm.ws.product.utility.extension.ifix.xml.Bundles;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.product.utility.extension.ifix.xml.LibertyProfileMetadataFile;
import com.ibm.ws.product.utility.extension.ifix.xml.Problem;
import com.ibm.ws.product.utility.extension.ifix.xml.UpdatedFile;

/**
 *
 */
public class IFixUtils {
    private static DocumentBuilder docBuilder;

    /**
     * This method will find which iFixes have been installed and return a list with all of the IDs of APARs that have been fixed by the iFixes.
     *
     * @param wlpInstallationDirectory The installation directory of the current install
     * @param console The console for printing messages to
     * @return The list of APAR IDs contained in the installed iFixes or an empty list if none were found.
     */
    public static Set<IFixInfo> getInstalledIFixes(File wlpInstallationDirectory, CommandConsole console) {
        Set<IFixInfo> iFixInfos = new HashSet<IFixInfo>();
        // First create a parser for reading the iFix XML
        try {
            docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception e) {
            // If we can't create a DocumentBuilder then we won't be able to read any files so print a message and return the empty list
            console.printlnErrorMessage(getMessage("ifixutils.unable.to.create.parser", e.getMessage()));
            return iFixInfos;
        }

        // Find the iFix XML files
        File[] iFixFiles = findIFixXmlFiles(wlpInstallationDirectory);

        // Read in each file and look for the problem information inside it, the problem info is contained in a problem sub-element (contained within a "resolves" within a "fix" element)
        for (File file : iFixFiles) {
            try {
                Document doc = docBuilder.parse(file);
                IFixInfo iFixInfo = IFixInfo.fromDocument(doc);
                iFixInfos.add(iFixInfo);
            } catch (Exception e) {
                // There was an error reading this one file but we can continue to read the next files so print a message but then continue
                console.printlnErrorMessage(getMessage("ifixutils.unable.to.read.file", file.getAbsolutePath(), e.getMessage()));
            }
        }
        return iFixInfos;
    }

    /**
     * This method will find which iFixes have been installed and return a list with all of the IDs of APARs that have been fixed by the iFixes.
     *
     * @param wlpInstallationDirectory The installation directory of the current install
     * @param console The console for printing messages to
     * @return The Set of LibertyProfileMetadataFile objects from all the *.lpmf files in the supplied installation dir.
     */
    public static Set<LibertyProfileMetadataFile> getInstalledLibertyProfileMetadataFiles(File wlpInstallationDirectory, CommandConsole console) {
        Set<LibertyProfileMetadataFile> lpmfInfos = new HashSet<LibertyProfileMetadataFile>();
        // First create a parser for reading the Liberty Profile Metadata XML
        try {
            docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception e) {
            // If we can't create an unmarshaller then we won't be able to read any files so print a message and return the empty list
            console.printlnErrorMessage(getMessage("ifixutils.unable.to.create.parser", e.getMessage()));
            return lpmfInfos;
        }

        // Find the lpmf XML files
        File[] lpmfFiles = findLPMFXmlFiles(wlpInstallationDirectory);

        // Read in each file and parse the liberty profile metadata into in memory objects.
        for (File file : lpmfFiles) {
            try {
                Document doc = docBuilder.parse(file);
                LibertyProfileMetadataFile lpmfInfo = LibertyProfileMetadataFile.fromDocument(doc);
                lpmfInfos.add(lpmfInfo);
            } catch (Exception e) {
                // There was an error reading this one file but we can continue to read the next files so print a message but then continue
                console.printlnErrorMessage(getMessage("ifixutils.unable.to.read.file", file.getAbsolutePath(), e.getMessage()));
            }
        }
        return lpmfInfos;
    }

    public static IFixInfo parseIFix(InputStream in, String path, CommandConsole console) {
        try {
            Document doc = docBuilder.parse(in);
            return IFixInfo.fromDocument(doc);
        } catch (Exception e) {
            // There was an error reading this one file but we can continue to read the next files so print a message but then continue
            console.printlnErrorMessage(getMessage("ifixutils.unable.to.read.file", path, e.getMessage()));
        }

        return null;
    }

    /**
     * This loads all of the files ending with ".xml" within the lib/fixes directory of the WLP install. If none exist then it returns an empty array.
     *
     * @param wlpInstallationDirectory The installation directory of the current install
     * @return The list of XML files or an empty array if none is found
     */
    private static File[] findIFixXmlFiles(File wlpInstallationDirectory) {
        File iFixDirectory = new File(wlpInstallationDirectory, "lib/fixes");
        if (!iFixDirectory.exists() || !iFixDirectory.isDirectory()) {
            return new File[0];
        }
        File[] iFixFiles = iFixDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String fileName) {
                return FileUtils.matchesFileExtension(".xml", fileName);
            }
        });
        return iFixFiles;
    }

    /**
     * This loads all of the files ending with ".lpmf" within the lib/fixes directory of the WLP install. If none exist then it returns an empty array.
     *
     * @param wlpInstallationDirectory The installation directory of the current install
     * @return The list of Liberty Profile Metadata files or an empty array if none is found
     */
    private static File[] findLPMFXmlFiles(File wlpInstallationDirectory) {
        File lpmfDirectory = new File(wlpInstallationDirectory, "lib/fixes");
        if (!lpmfDirectory.exists() || !lpmfDirectory.isDirectory()) {
            return new File[0];
        }
        File[] lpmfFiles = lpmfDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String fileName) {
                return FileUtils.matchesFileExtension(".lpmf", fileName);
            }
        });
        return lpmfFiles;
    }

    protected static String getMessage(String key, Object... args) {
        return CommandUtils.getMessage(key, args);
    }

    /**
     * This method checks to see whether any of the ifix jars or any ifix static content files are missing from the runtime. It ensures that
     * ifix jar files have their base bundles available, and all hashes of the new versions of the ifix files are checked against any they find
     * within the runtime.
     *
     * @param installDir - The install directory of the product extension (including core and usr).
     * @param features - The list of ProvisioningFeatureDefinition found in the install dir.
     * @param repo - This is the bundle repository that is mapped to the install location of the product extension.
     * @param console - The CommandConsole to write messages to.
     * @return - A Set if the ifix IDs that need to be reapplied.
     */
    public static Set<String> getIFixesThatMustBeReapplied(File installDir, Map<String, ProvisioningFeatureDefinition> features,
                                                           ContentBasedLocalBundleRepository repo, CommandConsole console) {

        Set<String> ifixesToReApply = new HashSet<String>();

        Map<File, Map<String, Version>> allBaseBundleJarContent = new HashMap<File, Map<String, Version>>();
        Set<File> allBundleJarContent = new HashSet<File>();
        Set<File> allStaticFileContent = new HashSet<File>();
        // Process all subsystem content of all manifests and store them in the set. We'll use that to process the ifix files.
        processSubsystemContent(installDir, features, repo, allBaseBundleJarContent, allBundleJarContent, allStaticFileContent, console);

        // Get a list of all the LibertyProfile metadata files that use to match up with the ifixes.
        Map<String, BundleFile> bundleFiles = processLPMFXmls(installDir, console);

        // Iterate over each file that is found in all the ifix xmls. If the same file is listed in multiple ifix xml, we will have
        // been given the new version that should be on the system, if it relevant to the runtime.
        for (Map.Entry<String, IFixInfo> ifixInfoEntry : processIFixXmls(installDir, bundleFiles, console).entrySet()) {

            // Get the relative file name of the ifix file e.g. lib/test_1.0.0.20130101.jar
            String updateFileName = ifixInfoEntry.getKey();
            // Get the IfixInfo object that contains the latest version of this file.
            IFixInfo ifixInfo = ifixInfoEntry.getValue();

            // Loop through all the updated files in the current ifixInfo and when we've found the required file to
            // process, do the rest of the processing.
            for (UpdatedFile updatedFile : ifixInfo.getUpdates().getFiles()) {
                if (updatedFile.getId().equals(updateFileName)) {

                    File updateFile = new File(installDir, updateFileName);
                    // Check to see if we're dealing with a static content file or not. If not see if it is a bundle. If not then ignore as we
                    // don't need it.
                    if (allStaticFileContent.contains(updateFile)) {
                        // Check that the file exists in the runtime.
                        if (updateFile.exists()) {
                            // Get the hash of the static file from the ifix xml file.
                            String ifixHash = updatedFile.getHash();
                            // Now calculate the new hash and compare the 2. If they are NOT the same the ifix needs to be re-applied.
                            try {
                                // Now calculate the new hash and compare each hash. If they are NOT the same the ifix needs to be re-applied.
                                if (!equalsHashes(updateFile, ifixHash))
                                    ifixesToReApply.add(ifixInfo.getId());

                            } catch (IOException ioe) {
                                console.printlnErrorMessage(getMessage("ifixutils.unable.to.read.file", updateFile.getAbsolutePath(), ioe.getMessage()));
                            }

                        } else {
                            // If the static file doesn't appear on disk, then we need to re-apply the ifix.
                            ifixesToReApply.add(ifixInfo.getId());
                        }
                        // Process jar files. If the ifix jar doesn't exist, then check to see whether we have the relevant
                        // features installed that would require the ifix to be re-applied.
                    } else {
                        // If we're not dealing with static files, then we should be dealing with bundles and or static jars.
                        // If we have the actual ifix jar in the runtime, we need to check that it is the correct file. Check that hash.
                        if (allBundleJarContent.contains(updateFile)) {
                            // Get the hash of the bundle from the ifix xml file.
                            String ifixHash = updatedFile.getHash();
                            try {
                                // Now calculate the new hash and compare both hashes. If they are NOT the same the ifix needs to be re-applied.
                                if (!equalsHashes(updateFile, ifixHash))
                                    ifixesToReApply.add(ifixInfo.getId());
                            } catch (IOException ioe) {
                                console.printlnErrorMessage(getMessage("ifixutils.unable.to.read.file", updateFile.getAbsolutePath(), ioe.getMessage()));
                            }
                        } else {
                            // If the ifix jar isn't in the runtime, we need to check that the base bundle it is an ifix for does. If this doesn't
                            // exist we can then ignore the ifix.
                            // Find the BundleFile associated with the ifix file.
                            BundleFile bundleFile = bundleFiles.get(updatedFile.getId());
                            // If we don't have one, then we're not a jar file we know about. We know ignore this file.
                            if (bundleFile != null) {
                                // Get the symbolic name and version of the ifix jar we're dealing with.
                                String bundleSymbolicName = bundleFile.getSymbolicName();
                                Version bundleVersion = new Version(bundleFile.getVersion());

                                // Now iterate over all the bundles we know about in the runtime, and see if we can find a matching base bundle.
                                // If we can we need to reapply the ifix, otherwise we can ignore.
                                boolean found = false;
                                for (Iterator<Map<String, Version>> iter = allBaseBundleJarContent.values().iterator(); iter.hasNext() && !found;) {
                                    for (Map.Entry<String, Version> bundleEntry : iter.next().entrySet()) {
                                        // If we have a matching symbolic name and the version matches (ignoring the qualifier) then we have a base bundle on the
                                        // system.
                                        String entrySymbolicName = bundleEntry.getKey();
                                        Version entryVersion = bundleEntry.getValue();
                                        if (bundleSymbolicName.equals(entrySymbolicName) &&
                                            bundleVersion.getMajor() == entryVersion.getMajor() &&
                                            bundleVersion.getMinor() == entryVersion.getMinor() &&
                                            bundleVersion.getMicro() == entryVersion.getMicro()) {
                                            found = true;
                                        }
                                    }
                                }
                                // If we have found a base bundle, report we need to reinstall the ifix.
                                if (found)
                                    ifixesToReApply.add(ifixInfo.getId());
                            }
                        }
                    }
                }
            }
        }
        return ifixesToReApply;
    }

    /**
     * This method calculates a hash of the required file and compares it against the supplied hash. It then returns
     * true if both hashes are equals.
     *
     * @param fileToHash - The file to calculate the hash for.
     * @param hashToCompare - The hash to compare.
     * @return - A boolean indicating whether the hashes are equal.
     */
    private static boolean equalsHashes(File fileToHash, String hashToCompare) throws IOException {

        boolean result = false;
        // Now calculate the new hash and compare the 2. If they are NOT the same the ifix needs to be re-applied.
        String fileHash = MD5Utils.getFileMD5String(fileToHash);
        if (fileHash.equals(hashToCompare))
            result = true;

        return result;
    }

    /**
     *
     * This method reads all of the Feature manifests supplied and finds the files that should be in the runtime. For bundles it also
     * stores the Base bundles so we can use that to see if we have the necessary base bundles available in the runtime for the ifix.
     *
     * @param installDir - The installation directory of the runtime to search.
     * @param features - The Map of features that are in the runtime.
     * @param repo - The corresponding bundle repository that we use to get the latest versions of the bundles running in the server.
     * @param allBaseBundleJarContent - A Map to store the results of all the Base Bundles we find in the features. The key is the file, and
     *            the value is a map containing the symbolic name and version of this bundle.
     * @param allBundleJarContent - A Set of files that contain the latest bundles, including ifix bundles, that running on the system. Static jars
     *            are stored in this Set.
     * @param allStaticFileContent - A Set of files that contain all the static content files in the system. This does not include static jars, which
     *            are contained in allBundleJarContent.
     * @param console
     */
    private static void processSubsystemContent(File installDir, Map<String, ProvisioningFeatureDefinition> features, ContentBasedLocalBundleRepository repo,
                                                Map<File, Map<String, Version>> allBaseBundleJarContent, Set<File> allBundleJarContent,
                                                Set<File> allStaticFileContent, CommandConsole console) {
        for (Map.Entry<String, ProvisioningFeatureDefinition> entry : features.entrySet()) {
            // List all directives on each header element.
            ProvisioningFeatureDefinition featureDef = entry.getValue();
            // Add the existing manifest file to the static content Set.
            allStaticFileContent.add(new File(featureDef.getFeatureDefinitionFile().getAbsolutePath()));

            // Get the SubsystemContent for the current feature.
            for (FeatureResource featureRes : featureDef.getConstituents(null)) {

                SubsystemContentType type = featureRes.getType();
                // If we've got a manifest reference in the SubsystemContent, ignore it as we add all manifest files to the static content.
                if (type != SubsystemContentType.FEATURE_TYPE) {
                    // Get the symbolic name/location and version range of the current SubsystemContent entry.
                    String symbolicName = featureRes.getSymbolicName();
                    String location = featureRes.getLocation();
                    VersionRange versionRange = featureRes.getVersionRange();

                    // Use the bundle repository to find the current version being loaded into the runtime.
                    File runtimeFile = repo.selectBundle(location, symbolicName, versionRange);
                    // If we have a file returned it is a bundle, otherwise it is a static file.
                    if (runtimeFile != null) {
                        // Add the bundle to the set.
                        allBundleJarContent.add(runtimeFile);
                        // Now find the corresponding base bundle.
                        File runtimeBaseBundle = repo.selectBaseBundle(location, symbolicName, versionRange);
                        // If we have a base bundle we get the symbolicName and version and store that in the map.
                        if (runtimeBaseBundle != null) {
                            InputStream zis = null;
                            ZipFile zipFile = null;
                            try {
                                zipFile = new ZipFile(runtimeBaseBundle);
                                ZipEntry zipEntry = zipFile.getEntry("META-INF/MANIFEST.MF");
                                zis = zipFile.getInputStream(zipEntry);

                                // Use the ManifestProcessor to get the raw manifest, and then load it into BundleManifest to strip directives off
                                // the symbolicname and version headers. We have to do it this way, because we got exceptions if we tried to load the
                                // zip input stream directly into the BundleManifest.
                                Manifest manifest = ManifestProcessor.parseManifest(zis);
                                BundleManifest bundleManifest = new BundleManifest(manifest);

                                Map<String, Version> bundleNameVersion = new HashMap<String, Version>();
                                bundleNameVersion.put(bundleManifest.getSymbolicName(), bundleManifest.getVersion());

                                allBaseBundleJarContent.put(runtimeBaseBundle, bundleNameVersion);
                            } catch (Exception e) {
                                console.printlnErrorMessage(getMessage("ifixutils.unable.to.read.file", runtimeFile.getAbsolutePath(), e.getMessage()));
                            } finally {
                                try {
                                    if (zis != null)
                                        zis.close();
                                    if (zipFile != null)
                                        zipFile.close();

                                } catch (IOException ioe) {
                                    console.printlnErrorMessage(getMessage("ifixutils.unable.to.read.file", runtimeFile.getAbsolutePath(), ioe.getMessage()));
                                }
                            }
                        }
                    } else {
                        // We need to have this section because non bundle/jar files won't be found by the above process. So all other static
                        // content files will turn up here.
                        if (location != null) {
                            allStaticFileContent.add(new File(installDir, location));
                        }
                    }
                }
            }
        }
    }

    /**
     * This method processes all of the ifix xml's and stores the latest version of each file found in a map. The
     * comparisons on each file are based on the date associated with the file. The latest date means the latest update.
     *
     * @param wlpInstallationDirectory - The Liberty install dir.
     * @param bundleFiles - A Map of all bundle file Ids and the BundleFiles from all lpmf xmls.
     * @param console - The CommandConsole to write messages to.
     * @return - A Map of all files in all ifix xmls, and the IfixInfos that contain the latest versions of the file.
     */
    private static Map<String, IFixInfo> processIFixXmls(File wlpInstallationDirectory, Map<String, BundleFile> bundleFiles, CommandConsole console) {

        // A map of update file names and the IfixInfo objects that contain the latest versions of these files.
        Map<String, IFixInfo> filteredIfixInfos = new HashMap<String, IFixInfo>();
        // A map of updated file names and the UpdateFile Object that represents the latest version of the file.
        // We use this map so that we don't have to find the file in the ifix info object each time.
        Map<String, UpdatedFile> latestIfixFiles = new HashMap<String, UpdatedFile>();
        // Get the full list of XML files
        Set<IFixInfo> ifixInfos = getInstalledIFixes(wlpInstallationDirectory, console);
        // Iterate over each one and process each file.
        for (IFixInfo chkinfo : ifixInfos) {
            // For each ifix, list all of the updated files
            for (UpdatedFile updateFile : chkinfo.getUpdates().getFiles()) {
                // See if we have an existing entry in the map of existing files. If not, add this one to the map, otherwise
                // check to see if this file has a more recent date than the one stored. If it does, then
                // replace the existing one with this new entry.
                String existingUpdateFileID;
                String updateId = updateFile.getId();
                // If we have a jar file, then we need to find the existing id which won't be the same as our current one, because the ifix qualifiers are
                // different for each ifix jar.
                if (bundleFiles.containsKey(updateId)) {
                    existingUpdateFileID = getExistingMatchingIfixID(bundleFiles.get(updateId), latestIfixFiles.keySet(), bundleFiles);
                } else {
                    // If we're not a jar but a static file, then use the updateID to find the existing values.
                    existingUpdateFileID = updateId;
                }

                // If we've got an existing entry and the date of the currently processed updateFile has a greater date,
                // then remove the old version and replace it with the new one.
                UpdatedFile existingUpdateFile = latestIfixFiles.get(existingUpdateFileID);
                if (existingUpdateFile != null) {
                    int dateComparison = updateFile.getDate().compareTo(existingUpdateFile.getDate());
                    boolean replaceInMap = false;
                    if (dateComparison > 0)
                        replaceInMap = true;
                    else if (dateComparison == 0) {
                        //the same file had the same date in two fixes
                        //check the number of problems being resolved
                        IFixInfo existingFixInfo = filteredIfixInfos.get(existingUpdateFileID);
                        List<Problem> existingProblems = existingFixInfo.getResolves().getProblems();
                        List<Problem> problems = chkinfo.getResolves().getProblems();
                        //since fixes are cumulative, whichever resolves more problems is newer
                        if (existingProblems.size() < problems.size())
                            replaceInMap = true;
                    }
                    if (replaceInMap) {
                        filteredIfixInfos.remove(existingUpdateFileID);
                        latestIfixFiles.remove(existingUpdateFileID);
                        filteredIfixInfos.put(updateId, chkinfo);
                        latestIfixFiles.put(updateId, updateFile);
                    }
                } else {
                    // If we don't have an existing entry, add this new entry.
                    filteredIfixInfos.put(updateId, chkinfo);
                    latestIfixFiles.put(updateId, updateFile);
                }
            }
        }

        return filteredIfixInfos;
    }

    static Map<String, IFixInfo> getLatestFixInfos(File wlpInstallationDirectory, CommandConsole console) {
        return processIFixXmls(wlpInstallationDirectory, processLPMFXmls(wlpInstallationDirectory, console), console);
    }

    /**
     * This method takes a ifix ID finds any existing IFix IDs that refer to the same base bundle. Because the ifix jars will have
     * different qualifiers, we need to be able to work out which ids are related.
     *
     * @param bundleFile - The bundleFile that maps to the current jar file we're processing.
     * @param existingIDs - A Set of Strings that represent the existing updateIds.
     * @param bundleFiles - All the known bundleFile objects.
     * @return - The existing key that maps to the same base bundle.
     */
    private static String getExistingMatchingIfixID(BundleFile bundleFile, Set<String> existingIDs, Map<String, BundleFile> bundleFiles) {
        String existingIfixKey = null;
        // Iterate over the known ids.
        for (String existingID : existingIDs) {
            // If we have a corresponding BundleFile for the existing ID, we need to check it matches the currently processed file.
            if (bundleFiles.containsKey(existingID)) {
                BundleFile existingBundleFile = bundleFiles.get(existingID);
                // If our symbolic name match the supplied BundleFile then move on to the version checking.
                if (bundleFile.getSymbolicName().equals(existingBundleFile.getSymbolicName())) {
                    //Get the versions of both the checking bundle and the existing bundle. Check that the version, excluding the qualifier match
                    // and if they do, we have a match.
                    Version existingBundleVersion = new Version(existingBundleFile.getVersion());
                    Version bundleVersion = new Version(bundleFile.getVersion());
                    if (bundleVersion.getMajor() == existingBundleVersion.getMajor() &&
                        bundleVersion.getMinor() == existingBundleVersion.getMinor() &&
                        bundleVersion.getMicro() == existingBundleVersion.getMicro())
                        existingIfixKey = existingID;
                }
            }
        }
        return existingIfixKey;
    }

    /**
     * This method processes all of the Liberty profile Metadata files stores the BundleFile objects containing the BundleSymbolic
     * name and version of each bundle against the id of the file that matches the corresponding entry in the ifix.xml.
     *
     * @param wlpInstallationDirectory - The Liberty install dir.
     * @param console - The CommandConsole to write messages to.
     * @return - A Map of all bundle file Ids and the BundleFiles from all lpmf xmls.
     */
    private static Map<String, BundleFile> processLPMFXmls(File wlpInstallationDirectory, CommandConsole console) {

        // A map of update file names and the IfixInfo objects that contain the latest versions of these files.
        Map<String, BundleFile> bundleFiles = new HashMap<String, BundleFile>();
        // Iterate over each Liberty Profile Metadata file and process each bundle.
        for (LibertyProfileMetadataFile chklpmf : getInstalledLibertyProfileMetadataFiles(wlpInstallationDirectory, console)) {
            // For each metadata file, list all of the bundle files
            Bundles bundles = chklpmf.getBundles();
            if (bundles != null) {
                List<BundleFile> bundleEntries = bundles.getBundleFiles();
                if (bundleEntries != null) {
                    for (BundleFile bundleFile : bundleEntries) {
                        // See if we have an existing entry in the map of existing files. If not, add this one to the map, otherwise
                        // check to see if this file has a more recent date than the one stored. If it does, then
                        // replace the existing one with this new entry.
                        bundleFiles.put(bundleFile.getId(), bundleFile);
                    }
                }
            }
        }

        return bundleFiles;
    }
}
