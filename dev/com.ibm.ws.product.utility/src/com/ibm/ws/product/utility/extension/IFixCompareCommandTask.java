/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.extension;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.aries.util.VersionRange;
import org.osgi.framework.Version;

import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.product.utility.BaseCommandTask;
import com.ibm.ws.product.utility.CommandConsole;
import com.ibm.ws.product.utility.CommandConstants;
import com.ibm.ws.product.utility.ExecutionContext;
import com.ibm.ws.product.utility.extension.VersionUtils.VersionParsingException;
import com.ibm.ws.product.utility.extension.ifix.xml.Applicability;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.product.utility.extension.ifix.xml.Offering;
import com.ibm.ws.product.utility.extension.ifix.xml.Problem;
import com.ibm.ws.product.utility.extension.ifix.xml.Resolves;
import com.ibm.ws.product.utility.extension.ifix.xml.UpdatedFile;
import com.ibm.ws.product.utility.extension.ifix.xml.Updates;

/**
 * This command will allow the <code>compare</code> command to be run. It will try to load XML files with iFix information from the wlp/lib/fixes directory and compare it to a new
 * version of WLP.
 */
public class IFixCompareCommandTask extends BaseCommandTask {

    private static final String INSTALL_LOCATION_TO_COMPARE_TO_OPTION = CommandConstants.COMMAND_OPTION_PREFIX + "target";
    private static final String APAR_TO_COMPARE_OPTION = CommandConstants.COMMAND_OPTION_PREFIX + "apars";
    private static final String VERBOSE_OPTION = CommandConstants.COMMAND_OPTION_PREFIX + "verbose";

    public static final String IFIX_COMPARE_TASK_NAME = "compare";
    private static final String APAR_FIX_PACK_ZIP_LOCATION = "lib/versions";
    private static final String APAR_FIX_PACK_ZIP_LOCATION_IN_ARCHIVE = "wlp/"
                                                                        + APAR_FIX_PACK_ZIP_LOCATION;
    private static final String APAR_FIX_PACK_ZIP_FILENAME_PATTERN = "wlp_fp[0-9]+_aparIds.zip";
    private static final String APAR_CSV_ENTRY_NAME = "aparIds.csv";

    /** {@inheritDoc} */
    @Override
    public Set<String> getSupportedOptions() {
        return new HashSet<String>(Arrays.asList(INSTALL_LOCATION_TO_COMPARE_TO_OPTION,
                                                 APAR_TO_COMPARE_OPTION,
                                                 VERBOSE_OPTION));
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(ExecutionContext context) {
        CommandConsole console = context.getCommandConsole();

        /*
         * There are two uses of the compare command, they may have supplied the --apar option or the --to option, if neither is supplied then this is illegal use so put an error
         * out
         */
        boolean toSet = context.optionExists(INSTALL_LOCATION_TO_COMPARE_TO_OPTION);
        boolean aparSet = context.optionExists(APAR_TO_COMPARE_OPTION);
        if (!toSet && !aparSet) {
            console.printlnErrorMessage(getMessage("compare.no.option.set"));
            return;
        }

        // Both commands need the iFix information for the current install so grab it now so it's only loaded once
        File wlpInstallationDirectory = context.getAttribute(CommandConstants.WLP_INSTALLATION_LOCATION, File.class);
        InstalledIFixInformation installedIFixes = findInstalledIFixes(wlpInstallationDirectory, console, context.optionExists(VERBOSE_OPTION));
        if (toSet) {
            compareToInstallLocation(context, console, installedIFixes.validIFixes, wlpInstallationDirectory);
        }
        if (aparSet) {
            compareToAparList(context, console, installedIFixes.validIFixes, wlpInstallationDirectory);
        }
        // Finally list all of the iFixes that were not applicable
        if (!installedIFixes.iFixesWithMissingFiles.isEmpty()) {
            console.printlnInfoMessage("");
            console.printlnInfoMessage(getMessage("compare.invalid.ifixes.missing", installedIFixes.iFixesWithMissingFiles));
        }

        if (!installedIFixes.inapplicableIFixes.isEmpty()) {
            console.printlnInfoMessage("");
            console.printlnInfoMessage(getMessage("compare.invalid.ifixes.badversion", installedIFixes.inapplicableIFixes));
        }

        if (!installedIFixes.iFixesWithInvalidXml.isEmpty()) {
            console.printlnInfoMessage("");
            console.printlnErrorMessage(getMessage("compare.invalid.ifixes.bad.xml", installedIFixes.iFixesWithInvalidXml));
        }
    }

    /**
     * Compares the current install to a list of APARs in the console arguments
     *
     * @param context
     * @param console
     * @param wlpInstallationDirectory
     * @param installedIFixes
     */
    private void compareToAparList(ExecutionContext context, CommandConsole console, Map<String, Set<String>> installedIFixes, File wlpInstallationDirectory) {
        // console.printlnInfoMessage("WLP Directory [ " + wlpInstallationDirectory.getPath() + " ]");
        // console.printlnInfoMessage("Installed IFixes [ " + installedIFixes.keySet() + " ]");

        // First grab the apar list
        String aparListString = context.getOptionValue(APAR_TO_COMPARE_OPTION);

        List<String> aparList;

        // Service 83179: String.split yields a singleton list on an empty string!
        aparListString = aparListString.trim();
        if (aparListString.isEmpty()) {
            aparList = Collections.emptyList();
        } else {
            // Service 83179: Trim spaces and duplicates; delimit on space and tab, too.
            aparList = new ArrayList<String>();
            String[] aparListArray = aparListString.split(",| |\t");
            for (String apar : aparListArray) {
                apar = apar.trim();
                if (!apar.isEmpty() && !aparList.contains(apar)) {
                    aparList.add(apar);
                }
            }
        }
        // console.printlnInfoMessage("APAR List String [ " + aparListString + " ]");
        // console.printlnInfoMessage("APAR List [ " + Arrays.toString(aparList) + " ]");

        // We now know what APARs to look for so load which ones are installed from both iFixes and Fix Packs
        Set<String> aparsInstalled = null;
        try {
            aparsInstalled = findFixPackAparIds(wlpInstallationDirectory, console, context);
        } catch (IllegalArgumentException e) {
            // These are thrown by the methods when the inputs were invalid and should have the message set to something readable to the user
            console.printlnErrorMessage(e.getMessage());
            return;
        } catch (ZipException e) {
            console.printlnErrorMessage(getMessage("compare.error.reading.install", wlpInstallationDirectory, e.getMessage()));
            return;
        } catch (IOException e) {
            console.printlnErrorMessage(getMessage("compare.error.reading.install", wlpInstallationDirectory, e.getMessage()));
            return;
        }
        // console.printlnInfoMessage("Installed APARs [ " + aparsInstalled + " ]");

        aparsInstalled.addAll(installedIFixes.keySet());

        // Service 83179: Use a list: Keep the missing APARs in the order as originally specified.
        List<String> missingApars = new ArrayList<String>();
        for (String apar : aparList) {
            if (!aparsInstalled.contains(apar)) {
                missingApars.add(apar);
            }
        }
        // console.printlnInfoMessage("Missing APARS [ " + missingApars.size() + " ] [ " + missingApars + " ]");

        if (missingApars.isEmpty()) {
            // Output a message saying all APARs were present
            console.printlnInfoMessage(getMessage("compare.all.apars.present"));
        } else {
            // Output a message saying which APARs were missing
            console.printlnInfoMessage(getMessage("compare.missing.apars", missingApars.toString()));
        }
    }

    /**
     * Compares the current install to one supplied in the console arguments
     *
     * @param context
     * @param console
     * @param wlpInstallationDirectory
     * @param installedIFixes
     */
    private void compareToInstallLocation(ExecutionContext context, CommandConsole console, Map<String, Set<String>> aparToIFixMap, File wlpInstallationDirectory) {
        String installToCompareToLocation = context.getOptionValue(INSTALL_LOCATION_TO_COMPARE_TO_OPTION);
        File installToCompareToFile = new File(installToCompareToLocation);
        if (!installToCompareToFile.exists()) {
            console.printlnErrorMessage(getMessage("compare.to.option.does.not.exist", installToCompareToLocation));
            return;
        }
        Set<String> aparsForNewInstall = null;
        try {
            aparsForNewInstall = findFixPackAparIds(installToCompareToFile, console, context);
        } catch (IllegalArgumentException e) {
            // These are thrown by the methods when the inputs were invalid and should have the message set to something readable to the user
            console.printlnErrorMessage(e.getMessage());
            return;
        } catch (ZipException e) {
            console.printlnErrorMessage(getMessage("compare.error.reading.install", installToCompareToLocation, e.getMessage()));
            return;
        } catch (IOException e) {
            console.printlnErrorMessage(getMessage("compare.error.reading.install", installToCompareToLocation, e.getMessage()));
            return;
        }

        // Go through all of the APARs that are currently installed and make sure they are in the new version
        Map<String, Set<String>> missingApars = new HashMap<String, Set<String>>();
        for (Map.Entry<String, Set<String>> aparIFixInfo : aparToIFixMap.entrySet()) {
            String apar = aparIFixInfo.getKey();
            if (!aparsForNewInstall.contains(apar)) {
                missingApars.put(apar, aparIFixInfo.getValue());
            }
        }
        // Output the result, this will consist of a message saying if there were any missing iFixes then details on what was missing and what was included
        if (missingApars.isEmpty()) {
            console.printlnInfoMessage(getMessage("compare.all.ifixes.present", wlpInstallationDirectory.getAbsolutePath(), installToCompareToLocation));
        } else {
            console.printlnInfoMessage(getMessage("compare.ifixes.missing", wlpInstallationDirectory.getAbsolutePath(), installToCompareToLocation));
            console.printlnInfoMessage("");
            console.printlnInfoMessage(getMessage("compare.list.missing.ifixes", wlpInstallationDirectory.getAbsolutePath(), installToCompareToLocation));
            printAparIFixInfo(console, missingApars);
        }
        // Now print the included APARs by removing all of the missing ones
        for (String missingApar : missingApars.keySet()) {
            aparToIFixMap.remove(missingApar);
        }
        if (!aparToIFixMap.isEmpty()) {
            console.printlnInfoMessage("");
            console.printlnInfoMessage(getMessage("compare.list.included.ifixes", wlpInstallationDirectory.getAbsolutePath(), installToCompareToLocation));
            printAparIFixInfo(console, aparToIFixMap);
        }
    }

    /**
     * This will print the map of APARs to iFixes by giving a line to each APAR listing which iFixes it is in
     *
     * @param console The console to print to
     * @param aparToIFixMap The map to print
     */
    private void printAparIFixInfo(CommandConsole console, Map<String, Set<String>> aparToIFixMap) {
        for (Map.Entry<String, Set<String>> aparIFixInfo : aparToIFixMap.entrySet()) {
            console.printlnInfoMessage(getMessage("compare.ifix.apar.info", aparIFixInfo.getKey(), aparIFixInfo.getValue()));
        }
    }

    /**
     * <p>
     * This method will find which iFixes have been installed and return a list with all of the IDs of APARs that have been fixed by the iFixes.
     * </p>
     * <p>
     * The return object will contain information about specific iFixes that were either invalid or were not applicable to the current install so that they can be printed as
     * messages once any other processes is done.
     * </p>
     *
     * @param wlpInstallationDirectory The installation directory of the current install
     * @param console The console for printing messages to
     * @param verbose If error messages should be printed whilst processing iFixes
     * @return A data object containing information about all of the iFixes installed
     */
    private InstalledIFixInformation findInstalledIFixes(File wlpInstallationDirectory, CommandConsole console, boolean verbose) {
        InstalledIFixInformation installedIFixInformation = new InstalledIFixInformation();
        Set<IFixInfo> iFixInfos = IFixUtils.getInstalledIFixes(wlpInstallationDirectory, console);

        // We only want iFixes that are applicable to the current version of the install. First we need to know what the current version is so get this now.
        Version productVersion = null;
        try {
            productVersion = getProductVersion(wlpInstallationDirectory);
        } catch (VersionParsingException e) {
            /*
             * We won't be able to do any version checking without a version. Not the end of the world but should give a message to the user warning them some iFixes that we have
             * checked are not relevant
             */
            console.printlnErrorMessage(getMessage("compare.version.parsing.error", e.getMessage()));
        }

        // Go through the iFixes checking they are valid and adding them to the appropriate map
        for (IFixInfo iFixInfo : iFixInfos) {
            try {
                if (!isIFixFileListValid(iFixInfo, wlpInstallationDirectory)) {
                    installedIFixInformation.iFixesWithMissingFiles.add(iFixInfo.getId());
                    continue;
                }
            } catch (ParseException e) {
                /*
                 * If there was an error getting the parsing the dates from this file then print out an error message then continue as we have to assume that this iFix is valid if
                 * we can't tell otherwise
                 */
                installedIFixInformation.iFixesWithInvalidXml.add(iFixInfo.getId());
                if (verbose) {
                    console.printlnErrorMessage(getMessage("compare.ifix.file.parse.error", iFixInfo.getId()));
                }
            }

            try {
                if (!isIFixApplicable(productVersion, iFixInfo)) {
                    installedIFixInformation.inapplicableIFixes.add(iFixInfo.getId());
                    continue;
                }
            } catch (VersionParsingException e) {
                /*
                 * If there was an error getting the version from this file then print out an error message then continue as we have to assume that this iFix is valid if we can't
                 * tell otherwise
                 */
                installedIFixInformation.iFixesWithInvalidXml.add(iFixInfo.getId());
                if (verbose) {
                    console.printlnErrorMessage(e.getMessage());
                }
            }

            Resolves resolves = iFixInfo.getResolves();
            if (resolves == null) {
                installedIFixInformation.iFixesWithInvalidXml.add(iFixInfo.getId());
                if (verbose) {
                    console.printlnErrorMessage(getMessage("compare.no.apar", iFixInfo.getId()));
                }
                continue;
            }
            List<Problem> problems = resolves.getProblems();
            if (problems == null) {
                installedIFixInformation.iFixesWithInvalidXml.add(iFixInfo.getId());
                if (verbose) {
                    console.printlnErrorMessage(getMessage("compare.no.apar", iFixInfo.getId()));
                }
                continue;
            }
            for (Problem problem : problems) {
                String problemId = problem.getDisplayId();
                if (problemId != null && !problemId.isEmpty()) {
                    Set<String> iFixesForThisApar = installedIFixInformation.validIFixes.get(problemId);
                    if (iFixesForThisApar == null) {
                        iFixesForThisApar = new HashSet<String>();
                        installedIFixInformation.validIFixes.put(problemId, iFixesForThisApar);
                    }
                    iFixesForThisApar.add(iFixInfo.getId());
                }
            }
        }
        return installedIFixInformation;
    }

    /**
     * Gets the product version from the properties file for the "com.ibm.websphere.appserver" product
     *
     * @param wlpInstallationDirectory
     *
     * @return The product version as parsed by the OSGi {@link Version} class
     * @throws VersionParsingException
     */
    private Version getProductVersion(File wlpInstallationDirectory) throws VersionParsingException {
        // First get the properties
        Map<String, ProductInfo> productProperties = VersionUtils.getAllProductInfo(wlpInstallationDirectory);

        // Get the properties for WAS
        ProductInfo wasProperties = productProperties.get("com.ibm.websphere.appserver");
        if (wasProperties == null) {
            throw new VersionParsingException(getMessage("compare.no.was.properties.found"));
        }

        Version version = convertVersion(wasProperties);

        return version;
    }

    /**
     * @param wasProperties
     * @return
     * @throws VersionParsingException
     */
    private Version convertVersion(ProductInfo wasProperties) throws VersionParsingException {
        // Get the version for this product
        String versionString = wasProperties.getVersion();

        /*
         * Product versions are stored as d1.d2.d3.d4 all of which are ints which is therefore slightly different to OSGi spec and also the way that they are stored in the meta
         * data XML where they are 3 digits following the rule:
         *
         * d1.d2.d3*1000+d4
         *
         * convert now
         */
        Pattern versionPattern = Pattern.compile("([0-9]*\\.[0-9]*\\.)([0-9]*)\\.([0-9]*)");
        Matcher matcher = versionPattern.matcher(versionString);
        if (matcher.matches()) {
            // If it doesn't match then we can't convert so leave it as it is
            int d3 = Integer.parseInt(matcher.group(2));
            int d4 = Integer.parseInt(matcher.group(3));
            versionString = matcher.group(1) + Integer.toString(d3 * 1000 + d4);
        } else {
            throw new VersionParsingException(getMessage("compare.version.incompatible", versionString));
        }

        // The version is a "must have" property so don't need to do null check
        Version version;
        try {
            version = Version.parseVersion(versionString);
        } catch (IllegalArgumentException e) {
            // I'm pretty sure this is impossible as we matched the pattern.  Better not risk it though so re-throw the error
            throw new VersionParsingException(getMessage("compare.unable.to.parse.version", versionString, e.getMessage()));
        }
        return version;
    }

    /**
     * Returns <code>true</code> if the list of files in the IFixInfo are present (for non-platform JARs) or more recent than those listed (for platform JARs or static files).
     *
     * @param iFixInfo
     * @param wlpInstallationDirectory
     * @return
     * @throws ParseException
     */
    private boolean isIFixFileListValid(IFixInfo iFixInfo, File wlpInstallationDirectory) throws ParseException {
        // First get the list of files to check
        Updates updates = iFixInfo.getUpdates();
        if (updates == null) {
            return true;
        }
        Set<UpdatedFile> files = updates.getFiles();

        // For each file work out if it is a feature JAR or not. Can do this based on the name as it will be in the format:
        // <pathAndName>_1.0.0.v20120424-1111.jar
        Pattern featureJarPattern = Pattern.compile(".*_([0-9]*\\.){3}v[0-9]{8}-[0-9]{4}\\.jar");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (UpdatedFile file : files) {
            String fileId = file.getId();
            boolean isFeatureJar = featureJarPattern.matcher(fileId).matches();

            if (isFeatureJar) {
                // If it's a feature JAR we just need to see if it is present
                File jarFile = new File(wlpInstallationDirectory, fileId);
                if (!jarFile.exists()) {
                    return false;
                }
            } else {
                // If its not a feature JAR then it means you don't get a unique JAR per iFix so see if the file that is there is the one for this iFix or newer
                File fileOnDisk = new File(wlpInstallationDirectory, fileId);
                String fileDate = file.getDate();
                long metaDataDate = dateFormat.parse(fileDate).getTime();
                if (metaDataDate > fileOnDisk.lastModified()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns <code>true</code> if the IFixInfo applies the current installation version.
     *
     * @param productVersion
     *
     * @param iFixInfo
     * @return
     */
    private boolean isIFixApplicable(Version productVersion, IFixInfo iFixInfo) throws VersionParsingException {
        // If we don't know the product version just return true
        if (productVersion == null) {
            return true;
        }

        // Get the applicability for the iFix
        Applicability applicability = iFixInfo.getApplicability();
        if (applicability == null) {
            throw new VersionParsingException(getMessage("compare.unable.to.find.offering", iFixInfo.getId()));
        }
        List<Offering> offerings = applicability.getOfferings();
        if (offerings == null || offerings.isEmpty()) {
            throw new VersionParsingException(getMessage("compare.unable.to.find.offering", iFixInfo.getId()));
        }

        // All offerings do not have the same version range so we need to iterate to see if we match
        // an of the applicable ranges
        boolean matches = false;
        for (Offering offering : offerings) {
            String tolerance = offering.getTolerance();
            VersionRange toleranceRange = VersionRange.parseVersionRange(tolerance);

            // Make sure the product version is in the range of the tolerance
            if (toleranceRange.matches(productVersion)) {
                matches = true;
                break;
            }
        }
        return matches;
    }

    /**
     * This method will look in the wlp/lib/versions/aparIds.zip file for a file
     * named aparIds.csv which it will then read to obtain a set of APARs that
     * are included in this Fix Pack.
     *
     * @param installLocation
     *            The location of the installation file to get the APAR IDs
     *            from, it can be either an archive file (zip or jar) or an
     *            extracted installation. The file must exist
     * @param context
     * @param console
     * @return A set included all of the APAR IDs that are included in this fix
     *         pack. Will be empty if no APARs are found.
     * @throws IllegalArgumentException
     *             if the <code>installLocation</code> is neither a directory or
     *             archive or the aparId zip is not valid
     * @throws IOException
     *             If something goes wrong reading the zip file
     * @throws ZipException
     *             If something goes wrong reading the zip file
     */
    private Set<String> findFixPackAparIds(File installLocation, CommandConsole console, ExecutionContext context) throws IllegalArgumentException, ZipException, IOException {
        // First need to work out what type of installation we have, is it an
        // archive or extracted?
        Set<String> fixPackAparIds;
        if (installLocation.isDirectory()) {
            // Extracted
            fixPackAparIds = readAparCsvFromExtractedInstall(installLocation);
            InstalledIFixInformation installedIFixes = findInstalledIFixes(installLocation, console, context.optionExists(VERBOSE_OPTION));
            for (String apar : installedIFixes.validIFixes.keySet()) {
                fixPackAparIds.add(apar);
            }
        } else {
            String fileName = installLocation.getName();
            if (!FileUtils.matchesFileExtension(".jar", fileName)
                && !FileUtils.matchesFileExtension(".zip", fileName)) {
                // We have a file that isn't an archive, can't proceed so return
                // an error message
                throw new IllegalArgumentException(getMessage("compare.install.not.zip.or.dir", installLocation.getAbsolutePath()));
            }
            // Archive
            fixPackAparIds = readAparCsvFromArchiveInstall(installLocation, console);
        }

        return fixPackAparIds;
    }

    private Set<String> convertCsvToSet(String csv) {
        // A null line in the file indicates an empty file so return an empty
        // set as it means no APARs were found
        if (csv == null) {
            return Collections.emptySet();
        }

        // If we got this far then we must of found the CSV file so process it
        // to find the APAR IDs
        Set<String> fixPackAparIds = new HashSet<String>();
        String[] aparIds = csv.split(",");
        for (String id : aparIds) {
            fixPackAparIds.add(id);
        }
        return fixPackAparIds;
    }

    /**
     * This will read the archive file at the <code>installLocation</code> and
     * obtain the CSV file within it that lists the APAR ids.
     *
     * @param installLocation
     *            The location of the archive install file
     * @return The contents of the CSV file or <code>null</code> if the file
     *         doesn't exist or is empty
     * @throws ZipException
     *             If an error occurs opening the archive
     * @throws IOException
     *             If an error occurs opening the archive
     */
    private Set<String> readAparCsvFromArchiveInstall(File installLocation, CommandConsole console) throws ZipException, IOException {
        // Archive means the CSV will be in a zip within the zip, will need to iterate the content of the install ZIP to find files that match the right pattern
        ZipFile installZipFile = new ZipFile(installLocation);
        Enumeration<? extends ZipEntry> zipEntries = installZipFile.entries();
        Pattern pattern = Pattern.compile(APAR_FIX_PACK_ZIP_LOCATION_IN_ARCHIVE + "/" + APAR_FIX_PACK_ZIP_FILENAME_PATTERN);

        /*
         * Read each file that matches the pattern, note we need to iterate through the archive install location because there might not be a ZIP Entry for the folder containing
         * the apar IDs
         */
        Set<String> aparIds = new HashSet<String>();
        List<IFixInfo> ifixes = new ArrayList<IFixInfo>();
        ProductInfo info = null;
        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = zipEntries.nextElement();
            String entryName = entry.getName();
            if (pattern.matcher(entryName).matches()) {
                // Have a matching zip file so find the CSV file within it
                ZipInputStream aparZipFileInputStream = new ZipInputStream(installZipFile.getInputStream(entry));
                ZipEntry aparCsvEntry = null;
                do {
                    aparCsvEntry = aparZipFileInputStream.getNextEntry();
                } while (aparCsvEntry != null && !APAR_CSV_ENTRY_NAME.equals(aparCsvEntry.getName()));

                // Null is invalid, if the zip file exists then it should contain this file
                if (aparCsvEntry == null) {
                    throw new IllegalArgumentException(getMessage("compare.no.csv.entry", installLocation.getAbsolutePath(), entryName,
                                                                  APAR_CSV_ENTRY_NAME));
                }
                String csv = readLine(aparZipFileInputStream);
                aparIds.addAll(convertCsvToSet(csv));
            } else if (entryName.startsWith("wlp/lib/fixes/")) {
                IFixInfo ifix = IFixUtils.parseIFix(installZipFile.getInputStream(entry), entryName, console);
                if (ifix != null) {
                    ifixes.add(ifix);
                }
            } else if (entryName.startsWith("wlp/lib/versions/") && entryName.endsWith(".properties")) {
                try {
                    ProductInfo pi = ProductInfo.parseProductInfo(new InputStreamReader(installZipFile.getInputStream(entry), "UTF-8"), null);
                    if (pi != null && "com.ibm.websphere.appserver".equals(pi.getId())) {
                        info = pi;
                    }
                } catch (ProductInfoParseException e) {
                    // ignore this and move on, it probably wasn't a product info file.
                }
            }
        }
        installZipFile.close();

        Version v = null;
        if (info == null) {
            console.printErrorMessage(getMessage("compare.no.was.properties.found"));
        } else {
            try {
                v = convertVersion(info);
            } catch (VersionParsingException e) {
                console.printErrorMessage(e.getMessage());
            }
        }

        for (IFixInfo fix : ifixes) {
            try {
                if (v == null || isIFixApplicable(v, fix)) {
                    Resolves resolves = fix.getResolves();
                    if (resolves != null) {
                        List<Problem> problems = resolves.getProblems();
                        if (problems != null) {
                            for (Problem problem : resolves.getProblems()) {
                                aparIds.add(problem.getDisplayId());
                            }
                        } else {
                            console.printErrorMessage(getMessage("compare.no.apar", fix.getId()));
                        }
                    } else {
                        console.printErrorMessage(getMessage("compare.no.apar", fix.getId()));
                    }
                }
            } catch (VersionParsingException e) {
                console.printErrorMessage(e.getMessage());
            }
        }

        // Read the csv entry
        return aparIds;
    }

    /**
     * This will load the APAR archive within the extracted file at the
     * <code>installLocation</code> and obtain the CSV file within it that lists
     * the APAR ids.
     *
     * @param installLocation
     *            The location of the archive install file
     * @return The contents of the CSV file or <code>null</code> if the file
     *         doesn't exist or is empty
     * @throws ZipException
     *             If an error occurs opening the archive
     * @throws IOException
     *             If an error occurs opening the archive
     */
    private Set<String> readAparCsvFromExtractedInstall(File installLocation) throws ZipException, IOException {
        // Extracted, means we can open up the zip containing the APAR CSV file
        // and get the entry for it
        File fixesFolder = new File(installLocation, APAR_FIX_PACK_ZIP_LOCATION);
        final Pattern pattern = Pattern.compile(APAR_FIX_PACK_ZIP_FILENAME_PATTERN);
        // Find all of the FP folder using the regext
        File[] fixPackZips = fixesFolder.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File arg0, String filename) {
                return pattern.matcher(filename).matches();
            }
        });

        Set<String> aparIds = new HashSet<String>();
        for (File aparFile : fixPackZips) {
            ZipFile aparZipFile = null;
            try {
                aparZipFile = new ZipFile(aparFile);
                ZipEntry aparCsvEntry = aparZipFile.getEntry(APAR_CSV_ENTRY_NAME);

                // Null is invalid, if the zip file exists then it should contain
                // this file
                if (aparCsvEntry == null) {
                    throw new IllegalArgumentException(getMessage("compare.no.csv.entry", installLocation.getAbsolutePath(), aparFile.getName(), APAR_CSV_ENTRY_NAME));
                }

                // Read the CSV
                InputStream csvInputStream = aparZipFile.getInputStream(aparCsvEntry);
                String aparCsv = readLine(csvInputStream);
                aparIds.addAll(convertCsvToSet(aparCsv));
            } finally {
                if (aparZipFile != null) {
                    aparZipFile.close();
                }
            }
        }
        return aparIds;
    }

    /**
     * This will read a single line from the input stream as a string.
     *
     * @param stream
     *            The stream to read
     * @return The line that was read
     * @throws IOException
     *             If there was a problem reading the line
     */
    private String readLine(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        return reader.readLine();
    }

    /**
     * Contains information about the iFixes contained in a particular installation
     */
    private static class InstalledIFixInformation {

        /** A map of APARs to iFix(es) that contain them. These are all iFixes that are valid */
        public final Map<String, Set<String>> validIFixes = new HashMap<String, Set<String>>();

        /** A set of iFixes that have the XML meta data present but are missing some/all of the files they are supposed to contain */
        public final Set<String> iFixesWithMissingFiles = new HashSet<String>();

        /** A set of iFixes that have the XML meta data present but it cannot be read for various reasons */
        public final Set<String> iFixesWithInvalidXml = new HashSet<String>();

        /** A set of iFixes that have the XML meta data present but aren't applicable to the current installation */
        public final Set<String> inapplicableIFixes = new HashSet<String>();

    }

    /** {@inheritDoc} */
    @Override
    public String getTaskDescription() {
        return getOption("compare.desc");
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskHelp() {
        return super.getTaskHelp("compare.desc", "compare.usage.options", "compare.option-key.", "compare.option-desc.", "compare.option.addon");
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskName() {
        return IFIX_COMPARE_TASK_NAME;
    }

}
