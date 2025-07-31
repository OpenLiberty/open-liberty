package com.ibm.ws.wlp.repository.esa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;
import com.ibm.ws.wlp.cs.MD5Utils;
import com.ibm.ws.wlp.repository.DownloadXmlGenerator;
import com.ibm.ws.wlp.repository.xml.DownloadItem;
import com.ibm.ws.wlp.repository.xml.DownloadXml;

/**
 * This ant {@link Task} will create a set of ESAs based on the feature manifests included as a nested fileset element.
 */
public class GenerateEsas extends DownloadXmlGenerator {

    /** A string of a new line character. */
    private static final String NEW_LINE = System.getProperty("line.separator", "\r\n");
    /** The regex pattern for a backslash which can be used for replacement to normalise paths */
    private static final String REGEX_BACKSLASH = Pattern.quote("\\");;

    /** boolean to control if this should be installed to core and if it can apply to any version */
    private boolean productFeature = true;
    /** The version to set in the applies to header */
    private String version;
    /** The location (dir) of licenses */
    private String licenseLocation;
    /** The location (dir) of the html version of the licenses */
    private String htmlLicenseLocation;
    /** A license type like ILAN or ILAR or null if we're shipping under the new feature terms */
    private String licenseType;
    /** Where to output the ESA(s) to */
    private String outputDirLocation;
    /** Which additions to set in the applies to, can be null or empty to indicate it shouldn't be include */
    private String editions = null;
    /** A set of file sets for the features. The root dir should be the wlp install and the file set should only include the feature MF files */
    private final Set<FileSet> features = new HashSet<FileSet>();
    /** The location on the DHE server relative to the root of the WLP project that the downloadable files will be */
    private String downloadLocation;
    /** The location on the DHE server relative to the root of the WLP project that the downloadable licenses will be */
    private String downloadLicenseLocation;
    /** This is the installType (Archive or IM) that this ESA applies to */
    private String appliesToInstallType = null;
    /** Whether to generate knowledge centre links */
    private boolean generateKnowledgeCentreLinks = true;
    private String featureTermsUrl = null;

    boolean specialFeatureTermsApply() {
        return (licenseType == null);
    }

    @Override
    public void execute() throws BuildException {
        log("Generating ESAs with properties:\nversion=" + version + "\nlicenseLocation=" + licenseLocation + "\noutputDirLocation=" + outputDirLocation + "\nfileset size="
            + features.size() + "\nfeature manifest locations=" + features);

        // Read the license number from the LI_en unless the special terms apply 
        String subsystemLicense = null;
        if (specialFeatureTermsApply()) {
            subsystemLicense = "Subsystem-License: " + featureTermsUrl;
        } else {
            // Find out the license number but reading the LI file
            File licenseRoot = new File(this.licenseLocation);
            File englishLI = new File(licenseRoot, "LI_en");
            BufferedReader licenseReader = null;
            try {
                licenseReader = new BufferedReader(new InputStreamReader(new FileInputStream(englishLI), StandardCharsets.UTF_16));
                String line = null;
                while ((line = licenseReader.readLine()) != null) {
                    if (line.startsWith("L/N:")) {
                        Pattern regex = Pattern.compile("L/N:\\s*([\\w-]*)");
                        Matcher matcher = regex.matcher(line);
                        if (!matcher.matches()) {
                            throw new BuildException("Unable to find license number from license string: '" + line + "'");
                        }

                        // Follow the Bundle-License spec for the license ID (section 3.2.1.10 of the OSGi spec)
                        subsystemLicense = "Subsystem-License: http://www.ibm.com/licenses/" + matcher.group(1);
                        break;
                    }
                }
            } catch (IOException e1) {
                log("IOException when reading license file. Exception message: " + e1.getMessage());
                throw new BuildException("Unable to find english license information to determine license number in file " + englishLI.getAbsoluteFile(), e1);
            } finally {
                if (licenseReader != null) {
                    try {
                        licenseReader.close();
                    } catch (IOException e) {
                        throw new BuildException("Error closing license file", e);
                    }
                }
            }
        }
        log("Using license ID: '" + subsystemLicense + "'");
        if (subsystemLicense == null || subsystemLicense.isEmpty()) {
            throw new BuildException("Unable to find license ID");
        }

        File outputDir = new File(this.outputDirLocation);
        outputDir.mkdirs();

        /*
         * For the download site that will host the ESAs we need to have a download.xml file that will store information on what is available for download. This should only contain
         * public features for the extended content. In order to do this we see if we were supplied a location for the XML file and if we were we will write to it. Also setup the
         * marshaller for writing it at this point so we can add a schema validation to make sure we are doing it right, it is better to do this now whilst we have the JaxbContext.
         */
        DownloadXml downloadXml = this.parseDownloadXml();

        // Go through all of the feature filesets that were added. 
        // Their dir should point to the install root and the include files should be the feature manifests  
        for (FileSet featureManifests : this.features) {
            if (featureManifests != null && featureManifests.getDir().exists()) {
                DirectoryScanner directoryScanner = featureManifests.getDirectoryScanner(getProject());
                String[] manifestFilePaths = directoryScanner.getIncludedFiles();
                for (String manifestFilePath : manifestFilePaths) {
                    File manifestFile = new File(directoryScanner.getBasedir(), manifestFilePath);
                    ProvisioningFeatureDefinition feature = null;
                    try {
                        feature = new SubsystemFeatureDefinitionImpl(null, manifestFile);

                        // Validate the IBM-Install-Policy header is set to a valid value if this is an auto feature
                        if (feature.isAutoFeature()) {
                            String installPolicy = feature.getHeader("IBM-Install-Policy");
                            if (installPolicy == null || !(installPolicy.equals("when-satisfied") || installPolicy.equals("manual"))) {
                                throw new BuildException(
                                                         "The feature manifest "
                                                                         + manifestFilePath
                                                                         + " is an auto feature but does not have the install policy set to a valid value of \"when-satisfied\" or \"manual\", it's value is: "
                                                                         + installPolicy);
                            }
                        }

                        File licenseRoot = new File(this.licenseLocation);
                        DownloadItem newDownloadItem = buildEsa(feature, outputDir, featureManifests.getDir(), licenseRoot, subsystemLicense);
                        if (newDownloadItem != null && downloadXml != null) {
                            downloadXml.getDownloadItems().add(newDownloadItem);
                        }

                        // Having created the .esa, create the .zip of metadata that will accompany it
                        buildZip(feature, outputDir);

                    } catch (IOException e) {
                        String featureName = feature != null ? feature.getSymbolicName() : manifestFilePath;
                        log("Failed to build ESA for:\t" + featureName + ". Exception message: " + e.getMessage(), e, Project.MSG_ERR);

                        // If we couldn't build the ESA that is a serious problem so bomb out...
                        throw new BuildException(e);
                    }
                }
            }
        }

        // Finally write out all of the new download content
        if (downloadXml != null) {
            this.writeDownloadXml(downloadXml);
        }
    }

    void buildZip(ProvisioningFeatureDefinition feature, File outputDir)
                    throws IOException
    {
        File outputFile = new File(outputDir, feature.getSymbolicName() + ".esa.metadata.zip");
        if (outputFile.exists()) {
            /*
             * If it's already there it means that it has already been generated (possibly with different settings for things like the applies to). As we are changing how we build
             * ESAs to go through feature_imports.xml this is guaranteed to happen so just exit.
             */
            log(outputFile.getAbsolutePath() + " already exists so skipping ESA metadata generation", Project.MSG_INFO);
            return;
        }
        Properties assetInfoProps = new Properties();
        String name = feature.getHeader("Subsystem-Name", Locale.ENGLISH);
        if (name != null) {
            assetInfoProps.put("name", name);
        }
        String shortDescription = feature.getHeader("Subsystem-Description", Locale.ENGLISH);
        if (shortDescription != null) {
            assetInfoProps.put("shortDescription", shortDescription);
        }

        if (specialFeatureTermsApply()) {
            assetInfoProps.put("licenseType", "UNSPECIFIED");
        } else {
            assetInfoProps.put("licenseType", licenseType);
        }

        ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(outputFile));

        try {
            File tempProps = File.createTempFile("assetInfo", ".properties");
            OutputStream os = new FileOutputStream(tempProps);
            try {
                assetInfoProps.store(os, null);
            } finally {
                os.close();
            }

            outputStream.putNextEntry(new ZipEntry("assetInfo.properties"));
            copy(outputStream, tempProps);
            outputStream.closeEntry();

            File tempDescription = File.createTempFile("description", ".html");
            Writer writer = new OutputStreamWriter(new FileOutputStream(tempDescription), StandardCharsets.UTF_8);
            try {
                writer.write(getLongDescription(feature));
            } finally {
                writer.close();
            }

            outputStream.putNextEntry(new ZipEntry("description.html"));
            copy(outputStream, tempDescription);
            outputStream.closeEntry();

            File featureTerms = new File(htmlLicenseLocation);
            if (featureTerms.isDirectory()) {
                for (File f : featureTerms.listFiles()) {
                    String path = "lafiles/" + f.getName();
                    outputStream.putNextEntry(new ZipEntry(path));
                    copy(outputStream, f);
                    outputStream.closeEntry();
                }
            }

        } finally {
            outputStream.close();
        }
    }

    /**
     * Generate a boilerplate long description: how to install it, how to include it in server.xml
     */
    private String getLongDescription(ProvisioningFeatureDefinition feature) {
        EsaDescriptionHtmlGenerator generator = new EsaDescriptionHtmlGenerator();
        return generator.generateDescriptionHtml(feature, generateKnowledgeCentreLinks, licenseType);
    }

    /**
     * This method will build an ESA for a single feature.
     * 
     * @param feature The feature to build an ESA for
     * @param outputDir The directory to save the ESA to, it will be named after the feature symbolic name
     * @param installRoot The root on the WLP install to get files for
     * @param licenseRoot The directory containing the license files to include
     * @param subsystemLicense The license string to put into the manifest header
     * @return
     * @throws IOException If something goes wrong copying the files
     */
    private DownloadItem buildEsa(ProvisioningFeatureDefinition feature, File outputDir, File installRoot, File licenseRoot, String subsystemLicense) throws IOException {
        log("Building ESA for feature " + feature.getSymbolicName(), Project.MSG_VERBOSE);
        File outputFile = new File(outputDir, feature.getSymbolicName() + ".esa");
        if (outputFile.exists()) {
            /*
             * If it's already there it means that it has already been generated (possibly with different settings for things like the applies to). As we are changing how we build
             * ESAs to go through feature_imports.xml this is guaranteed to happen so just exit.
             */
            log(outputFile.getAbsolutePath() + " already exists so skipping ESA generation", Project.MSG_INFO);
            return null;
        }
        int lengthOfInstallRoot = installRoot.getAbsolutePath().length();
        Set<String> absPathsForLibertyContent = new HashSet<String>();
        Set<String> absPathsForLibertyBundles = new HashSet<String>();
        Set<String> absPathsForLibertyLocalizations = new HashSet<String>();
        Set<String> absPathsForLibertyIcons = new HashSet<String>();
        getFilesForFeature(installRoot, absPathsForLibertyContent, absPathsForLibertyBundles, absPathsForLibertyLocalizations, absPathsForLibertyIcons, feature,
                           new ContentBasedLocalBundleRepository(installRoot, null, false));
        log("Files to include are: " + absPathsForLibertyContent, Project.MSG_VERBOSE);
        log("Bundles to include are: " + absPathsForLibertyBundles, Project.MSG_VERBOSE);
        log("Localizations to include are: " + absPathsForLibertyLocalizations, Project.MSG_VERBOSE);
        log("Creating ESA file: " + outputFile.getAbsolutePath(), Project.MSG_VERBOSE);
        ZipOutputStream outputStream = null;
        String appliesTo = null;
        FileOutputStream tempManifestOutputStream = null;

        // Initialize the checksums if we have one
        Properties checksumInput = null;
        Properties checksumOutput = null;
        boolean hasChecksum = false;
        File checksumFile = feature.getFeatureChecksumFile();
        log("Reading file from: " + checksumFile.getAbsolutePath(), Project.MSG_VERBOSE);
        if (checksumFile != null && checksumFile.exists()) {
            hasChecksum = true;
            checksumInput = new Properties();
            FileInputStream checksumInputStream = null;
            try {
                checksumInputStream = new FileInputStream(checksumFile);
                checksumInput.load(checksumInputStream);
            } finally {
                if (checksumInputStream != null) {
                    checksumInputStream.close();
                }
            }
            checksumOutput = new Properties();
        }
        try {
            outputStream = new ZipOutputStream(new FileOutputStream(outputFile));
            outputStream.putNextEntry(new ZipEntry("OSGI-INF/SUBSYSTEM.MF"));
            File manifestFile = feature.getFeatureDefinitionFile();

            /*
             * We need to modify the manifest to include things like the applies to and license information. This will mess up the generated checksums though so write it to a temp
             * file so we can then generate the checksum for it
             */
            File tempManifestFile = File.createTempFile(feature.getSymbolicName(), ".mf");
            tempManifestOutputStream = new FileOutputStream(tempManifestFile);

            // Extract the current manifest contents so that we can add ESA specific headers
            StringBuilder manifestBuilder = extract(manifestFile);

            /*
             * The SUBSYSTEM.MF does not have line length limits (see section 134.2 of the Enterprise OSGi spec) etc so happily add the extra headers we need without worrying about
             * length
             */
            appliesTo = feature.getHeader("IBM-AppliesTo");
            if (appliesTo == null) {
                appliesTo = getAppliesTo();
                manifestBuilder.append(NEW_LINE);
                manifestBuilder.append("IBM-AppliesTo: " + appliesTo);
            }
            if (feature.getHeader("Subsystem-License") == null) {
                manifestBuilder.append(NEW_LINE);
                manifestBuilder.append(subsystemLicense);
            }
            if (feature.getHeader("IBM-License-Agreement") == null) {
                manifestBuilder.append(NEW_LINE);
                manifestBuilder.append("IBM-License-Agreement: wlp/lafiles/LA");
            }
            if (!specialFeatureTermsApply() && feature.getHeader("IBM-License-Information") == null) {
                manifestBuilder.append(NEW_LINE);
                manifestBuilder.append("IBM-License-Information: wlp/lafiles/LI");
            }
            if (this.productFeature && feature.getHeader("IBM-InstallTo") == null) {
                manifestBuilder.append(NEW_LINE);
                manifestBuilder.append("IBM-InstallTo: core");
            }
            // Default the vendor to IBM if it is not set
            String vendor = feature.getHeader("Subsystem-Vendor");
            if (vendor == null) {
                manifestBuilder.append(NEW_LINE);
                manifestBuilder.append("Subsystem-Vendor: IBM");
            }

            // If we have localization files we need a header for it
            String subsystemLocalization = feature.getHeader("Subsystem-Localization");
            if (!absPathsForLibertyLocalizations.isEmpty() && (subsystemLocalization == null || subsystemLocalization.isEmpty())) {
                // Use default
                log("The feature " + feature.getSymbolicName() + " has localization files but no subsystem localization header so adding default", Project.MSG_ERR);
                subsystemLocalization = "OSGI-INF/l10n/subsystem";
                manifestBuilder.append(NEW_LINE);
                manifestBuilder.append("Subsystem-Localization: " + subsystemLocalization);
            }

            // After all ESA headers have been added, write the new manifest to the temporary file
            tempManifestOutputStream.write(manifestBuilder.toString().getBytes());

            // Finished copying to the temp file, close it and copy to the zip
            tempManifestOutputStream.close();
            copy(outputStream, tempManifestFile);
            outputStream.closeEntry();

            // Also add the correct checksum entry
            if (hasChecksum) {
                // FIPS 140-3: Algorithm assessment complete; no changes required.
                // MD5 is used for file comparison / checksums which isn't a cryptographic use case
                // We can't update the hashing algorithm used here because Ifix/ESA's only provide MD5 checksums
                String manifestMD5String = MD5Utils.getFileMD5String(tempManifestFile);
                checksumOutput.put("OSGI-INF/SUBSYSTEM.MF", manifestMD5String);
            }

            // All done with the temp manifest so delete it
            if (!tempManifestFile.delete()) {
                tempManifestFile.deleteOnExit();
            }

            // Handle normal files, the path matches that in the liberty install
            for (String filePath : absPathsForLibertyContent) {
                File file = new File(filePath);
                if (!file.exists()) {
                    // This really should not happen but at the time of writing it did for at least one file (see defect 97157).
                    // As we don't have a validator for invalid manifests this is the only place to catch these errors so fail the
                    // whole build. All of the errors spotted so far are due to the JavaDoc not being generated so help out the
                    // build monitor and say this may be a good place to start looking...
                    throw new BuildException(
                                             "The file "
                                                             + filePath
                                                             + " for feature "
                                                             + feature.getSymbolicName()
                                                             + " is missing.\nAt the time of writting this was always caused by missing JavaDoc bundles where their generation failed due to not having a dependency on the OSGi spec JARs.  If the file reported is a JavaDoc file see 97157 for a possible fix.");
                }
                String pathInWlp = filePath.substring(lengthOfInstallRoot).replaceAll(REGEX_BACKSLASH, "/");
                String entryPath = "wlp" + pathInWlp;
                outputStream.putNextEntry(new ZipEntry(entryPath));
                copy(outputStream, file);
                outputStream.closeEntry();

                // Write out checksum to the path in ESA
                if (hasChecksum) {
                    // Remove first "/" in path in wlp
                    String checksumEntry = checksumInput.getProperty(pathInWlp.substring(1));
                    if (checksumEntry != null && !checksumEntry.isEmpty()) {
                        checksumOutput.put(entryPath, checksumEntry);
                    } else {
                        log("Unable to find checksum entry for file " + pathInWlp, Project.MSG_WARN);
                    }
                }
            }

            // Bundles all go in the root of the ESAs
            for (String filePath : absPathsForLibertyBundles) {
                File file = new File(filePath);
                if (!file.exists()) {
                    /*
                     * This really really should not happen as we resolve bundles to an existing file. Not seen it so far but as above we don't ever validate for this so fail the
                     * build as a useful validation step to make sure that our manifests are valid
                     */
                    throw new BuildException("The bundle at path " + filePath + " for feature " + feature.getSymbolicName() + " is missing");
                }
                String entryPath = file.getName();
                outputStream.putNextEntry(new ZipEntry(entryPath));
                copy(outputStream, file);
                outputStream.closeEntry();

                // Write out checksum to the path in ESA
                if (hasChecksum) {
                    String pathInWlp = filePath.substring(lengthOfInstallRoot + 1).replaceAll(REGEX_BACKSLASH, "/");
                    String checksumEntry = checksumInput.getProperty(pathInWlp);
                    if (checksumEntry != null && !checksumEntry.isEmpty()) {
                        checksumOutput.put(entryPath, checksumEntry);
                    } else {
                        log("Unable to find checksum entry for bundle " + pathInWlp, Project.MSG_WARN);
                    }
                }
            }

            // Localization files go somewhere determined by a header
            for (String filePath : absPathsForLibertyLocalizations) {
                // Strip the feature name off the front of the file name so we are left with, for example, _fr.properties
                File file = new File(filePath);
                String featureName = feature.getSymbolicName();
                String fileName = file.getName();
                String fileEnding = fileName.startsWith(featureName) ? fileName.substring(featureName.length()) : fileName;

                // subsystemLocalization is always set as we fall back to a default above
                String entryPath = subsystemLocalization + fileEnding;
                outputStream.putNextEntry(new ZipEntry(entryPath));
                copy(outputStream, file);
                outputStream.closeEntry();

                // No checksums for localization files
            }

            // Always copy in the license files
            for (File licenseFile : licenseRoot.listFiles()) {
                String entryPath = "wlp/lafiles/" + licenseFile.getName();
                outputStream.putNextEntry(new ZipEntry(entryPath));
                copy(outputStream, licenseFile);
                outputStream.closeEntry();

                // No checksums for license files
            }

            // Add in all of the icons
            for (String filePath : absPathsForLibertyIcons) {
                // Strip the feature name off the front of the file name so we are left with, for example, OSGI-INF/toolIcon.png
                File file = new File(filePath);
                String symbolicName = feature.getSymbolicName();
                String fileRef = filePath.substring(filePath.lastIndexOf(symbolicName) + symbolicName.length() + 1);
                // If we don't do this any path separators will go into the manifest as backslashes on windows
                outputStream.putNextEntry(new ZipEntry(fileRef.replace("\\", "/")));
                copy(outputStream, file);
                outputStream.closeEntry();
            }

            // If there is a checksum file then include it now
            if (hasChecksum) {
                String entryPath = "OSGI-INF/checksums.cs";
                outputStream.putNextEntry(new ZipEntry(entryPath));
                checksumOutput.store(outputStream, null);
                outputStream.closeEntry();
            }
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
            if (tempManifestOutputStream != null) {
                tempManifestOutputStream.close();
            }
        }
        // Finally create the download item for the XML file, only need to do this for public features
        return getDownloadItem(feature, outputFile, appliesTo);
    }

    /**
     * @param feature
     * @param outputFile
     * @param appliesTo
     * @return
     */
    private DownloadItem getDownloadItem(ProvisioningFeatureDefinition feature, File outputFile, String appliesTo)
    {
        if (feature.getVisibility() != Visibility.PUBLIC) {
            return null;
        }
        DownloadItem downloadItem = new DownloadItem();

        // The name must be set so fallback onto the symbolic name if it isn't set
        String symbolicName = feature.getSymbolicName();

        // Download XML is always English
        String subsystemName = feature.getHeader("Subsystem-Name", Locale.ENGLISH);
        String name = (subsystemName != null && !subsystemName.isEmpty()) ? subsystemName : symbolicName;
        downloadItem.setName(name);
        downloadItem.setLicenses(this.downloadLicenseLocation);
        downloadItem.setFile(this.downloadLocation + outputFile.getName());
        downloadItem.setType("feature");
        downloadItem.setAppliesTo(appliesTo);
        downloadItem.setDescription(feature.getHeader("Subsystem-Description", Locale.ENGLISH));
        downloadItem.setDownloadSize(outputFile.length());
        downloadItem.setProvideFeature(symbolicName);
        return downloadItem;
    }

    /**
     * @return
     */
    private String getAppliesTo() {
        // Make a list of properties to add then add ";" to separate them as necessary
        List<String> properties = new ArrayList<String>();
        String appliesTo;
        if (this.editions != null && !this.editions.isEmpty()) {
            properties.add("productEdition=\"" + this.editions + "\"");
        }
        if (this.productFeature) {
            // usr features must use spi so can apply to any version
            properties.add("productVersion=" + this.version);
        }
        if (this.appliesToInstallType != null && !this.appliesToInstallType.isEmpty()) {
            properties.add("productInstallType=" + this.appliesToInstallType);
        }
        StringBuilder appliesToBuilder = new StringBuilder("com.ibm.websphere.appserver");
        for (String property : properties) {
            appliesToBuilder.append("; ");
            appliesToBuilder.append(property);
        }
        appliesTo = appliesToBuilder.toString();
        return appliesTo;
    }

    // Note that this method adds a lot of duplication with the ServerContentHelper.processServerContentRequest method used by the minify command
    // but this was decided to be preferable to re-using that code for the following reasons:
    // 1. Re-use would of required us to make the method public for the sake of the build even though it is private in the runtime, this seemed
    //    like it was a poor reason to make a public method
    // 2. The method in ServerContentHelper is not perfect for us here.  For instance when packaging an ESA we do special things to the path for
    //    the bundle resources and localizations, if we used ServerContentHelper we'd have to do a lot of post-processing to work out what we
    //    were given (and this is not trivial for features where there is a bundle and type=jar for the same file!)
    // 3. The method in ServerContentHelper does some stuff we don't need here, for instance it processes iFixes and platform specific JARs which
    //    as we have a complete build we don't need
    private void getFilesForFeature(File installRoot,
                                    final Set<String> absPathsForLibertyContent,
                                    final Set<String> absPathsForLibertyBundles,
                                    final Set<String> absPathsForLibertyLocalizations,
                                    final Set<String> absPathsForLibertyIcons,
                                    final ProvisioningFeatureDefinition fd,
                                    final ContentBasedLocalBundleRepository br) {
        Collection<FeatureResource> frs = fd.getConstituents(null);

        for (FeatureResource fr : frs) {
            switch (fr.getType()) {
                case FEATURE_TYPE: {
                    // No resource for this feature
                    break;
                }

                case BUNDLE_TYPE:
                case BOOT_JAR_TYPE: {
                    // Add to the list of bundle files
                    addJarResource(absPathsForLibertyBundles, fd, br, fr);
                    break;
                }

                case JAR_TYPE: {
                    // Add to the list of normal files
                    addJarResource(absPathsForLibertyContent, fd, br, fr);
                    break;
                }

                case FILE_TYPE: {
                    // file uses loc as a relative path from install root.
                    String locString = fr.getLocation();
                    if (locString != null) {
                        addFileResource(installRoot, absPathsForLibertyContent,
                                        locString);
                    } else {
                        // a file type without a loc is bad, means misuse of the
                        // type.
                        throw new BuildException("No location on file type for resource "
                                                 + fr.getSymbolicName() + " in feature "
                                                 + fd.getFeatureName());
                    }
                    break;
                }

                case UNKNOWN: {
                    // if its not jar,bundle,feature, or file.. then something
                    // is wrong.
                    log("Unknown feature resource for " + fr.getSymbolicName()
                        + " in feature " + fd.getFeatureName()
                        + ". The type is: " + fr.getRawType(),
                        Project.MSG_ERR);
                    // we assume that other types will use the location field as
                    // something useful.
                    String locString = fr.getLocation();
                    if (locString != null) {
                        addFileResource(installRoot, absPathsForLibertyContent,
                                        locString);
                    }
                }
            }
        }
        // add in (all) the NLS files for this featuredef.. if any..
        for (File nls : fd.getLocalizationFiles()) {
            if (nls.exists()) {
                absPathsForLibertyLocalizations.add(nls.getAbsolutePath());
            }
        }

        for (String icon : fd.getIcons()) {
            File iconFile = new File(fd.getFeatureDefinitionFile().getParentFile(), "icons/" + fd.getSymbolicName() + "/" + icon);
            if (iconFile.exists()) {
                absPathsForLibertyIcons.add(iconFile.getAbsolutePath());
            } else {
                throw new BuildException("Icon file " + iconFile.getAbsolutePath() + " doesn't exist");
            }
        }
    }

    /**
     * Adds a file resource to the set of file paths.
     * 
     * @param installRoot The install root where we are getting files from
     * @param content The content to add the file paths to
     * @param locString The location string from the feature resource
     */
    public void addFileResource(File installRoot, final Set<String> content,
                                String locString) {
        String[] locs;
        if (locString.contains(",")) {
            locs = locString.split(",");
        } else {
            locs = new String[] { locString };
        }

        for (String loc : locs) {
            File test = new File(loc);
            if (!test.isAbsolute()) {
                test = new File(installRoot, loc);
            }
            loc = test.getAbsolutePath();
            content.add(loc);
        }
    }

    /**
     * This method will find the appropriate JAR(s) to the supplied set of file paths.
     * 
     * @param content The file paths to add the JAR(s) to
     * @param fd The feature definition containing this JAR
     * @param br The bundle repository to be queried to find the JAR
     * @param fr The feature resource defining the JAR
     */
    public void addJarResource(final Set<String> content,
                               final ProvisioningFeatureDefinition fd,
                               final ContentBasedLocalBundleRepository br, FeatureResource fr) {
        // type = bundle, need to query the bundle repository for
        // this feature def, to find which files are being used 
        // (could be in a different location or an iFix).
        // type = jar, basically same as type=bundle here
        // type = boot.jar, basically same as type=bundle here
        File b = br.selectBundle(fr.getLocation(),
                                 fr.getSymbolicName(), fr.getVersionRange());

        // bb is the bundle if no iFix were installed
        File bb = br.selectBaseBundle(fr.getLocation(),
                                      fr.getSymbolicName(), fr.getVersionRange());
        // b & bb will be the same bundle if the base was the
        // selected..
        if (b != null && bb != null) {
            // ESAs don't (and can't) install iFix content so just include the base bundle
            content.add(bb.getAbsolutePath());
        } else {
            /*
             * As we have a built version we should have everything
             * so if it is missing then that is bad
             */
            log("Missing JAR for resource " + fr.getSymbolicName() + " in feature " + fd.getSymbolicName(), Project.MSG_ERR);
        }
    }

    /**
     * Copies the file to the output stream
     * 
     * @param out The stream to write to
     * @param in The file to write
     * @throws IOException If something goes wrong
     */
    private void copy(OutputStream out, File in) throws IOException {
        FileInputStream inStream = null;
        try {
            inStream = new FileInputStream(in);

            byte[] buffer = new byte[4096];
            int len;
            while ((len = inStream.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        } finally {
            if (inStream != null) {
                inStream.close();
            }
        }
    }

    /**
     * Extracts the file into a string builder, trimming any white space/new lines that
     * may exist at the beginning or end of the file.
     * 
     * @param in The file to extract into string form
     * @return the StringBuilder version of the file after trimming
     * @throws IOException
     */
    private StringBuilder extract(File in) throws IOException {
        BufferedReader reader = null;
        String line = null;
        StringBuilder builder = new StringBuilder();

        try {
            reader = new BufferedReader(new FileReader(in));

            while ((line = reader.readLine()) != null) {
                // Forcibly exclude IBM-License-Information header if we're using the specialTerms 
                if (!line.startsWith("IBM-License-Information") || !specialFeatureTermsApply()) {
                    builder.append(line);
                    builder.append(NEW_LINE);
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        String trimmedFile = builder.toString().trim();
        return new StringBuilder(trimmedFile);
    }

    public void setEditions(String editions) {
        this.editions = editions;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setLicenseLocation(String licenseLocation) {
        this.licenseLocation = licenseLocation;
    }

    public void setOutputDirLocation(String outputDirLocation) {
        this.outputDirLocation = outputDirLocation;
    }

    /**
     * Factory method ANT uses to create a new nested file set element for the features.
     * 
     * @return The new file set
     */
    public FileSet createFileset() {
        FileSet newFeatures = new FileSet();
        features.add(newFeatures);
        return newFeatures;
    }

    /**
     * @return the downloadLocation
     */
    public String getDownloadLocation() {
        return downloadLocation;
    }

    /**
     * @param downloadLocation the downloadLocation to set
     */
    public void setDownloadLocation(String downloadLocation) {
        this.downloadLocation = downloadLocation;
    }

    /**
     * @return the downloadLicenseLocation
     */
    public String getDownloadLicenseLocation() {
        return downloadLicenseLocation;
    }

    /**
     * @param downloadLicenseLocation the downloadLicenseLocation to set
     */
    public void setDownloadLicenseLocation(String downloadLicenseLocation) {
        this.downloadLicenseLocation = downloadLicenseLocation;
    }

    /**
     * @param installToCore the installToCore to set
     */
    public void setProductFeature(boolean productFeature) {
        this.productFeature = productFeature;
    }

    /**
     * @param knowledgeCentreLinks Whether to set the knowledge centre links
     */
    public void setGenerateKnowledgeCentreLinks(boolean generateKnowledgeCentreLinks) {
        this.generateKnowledgeCentreLinks = generateKnowledgeCentreLinks;
    }

    /**
     * @param appliesToInstallType the appliesToInstallType to set
     */
    public void setAppliesToInstallType(String appliesToInstallType) {
        this.appliesToInstallType = appliesToInstallType;
    }

    /**
     * @return the licenseType, or null if not set
     */
    public String getLicenseType() {
        return licenseType;
    }

    /**
     * @param specialFeatureTerms the licenseType to set, use a blank string to indicate specialFeatureTerms
     */
    public void setLicenseType(String licenseType) {
        if (licenseType.isEmpty()) {
            this.licenseType = null;
        } else {
            this.licenseType = licenseType;
        }
    }

    /**
     * Sets the license URL to be used if there is not a license type
     * 
     * @param url
     */
    public void setFeatureTermsUrl(String featureTermsUrl)
    {
        this.featureTermsUrl = featureTermsUrl;
    }

    /**
     * @return the htmlLicenseLocation
     */
    public String getHtmlLicenseLocation() {
        return htmlLicenseLocation;
    }

    /**
     * @param htmlLicenseLocation the htmlLicenseLocation to set
     */
    public void setHtmlLicenseLocation(String htmlLicenseLocation) {
        this.htmlLicenseLocation = htmlLicenseLocation;
    }
}
