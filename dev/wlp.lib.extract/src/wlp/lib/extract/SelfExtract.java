/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wlp.lib.extract;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

import wlp.lib.extract.SelfExtractor.ExternalDependencies;
import wlp.lib.extract.SelfExtractor.ExternalDependency;

/**
 * This is the self-extracting jar's Main-Class (see the bnd file used to generate the
 * product jar from our build).
 * <p>
 * This class:
 * <li>discovers the product licenses
 * <li>determines the product and license names for those licenses
 * <li>presents users with information about the license terms (and provides a utility for viewing the license)
 * <li>prompts for license acceptance
 * <li>unpacks the jar file
 * <li>restores permissions for scripts on non-windows platforms.
 * <p>
 * This is, unfortunately, no true replacement for zip or pax. It lives in this relatively obscurely named
 * package to blend in with the scenery of the typical liberty profile image: wlp/lib/lotsOf.jar. This
 * package and it's classes are not extracted from this jar.
 * <p>
 * There is a --help option, should anyone think to invoke it. Options with single or double leading -, and are
 * case-insensitive.
 */
public class SelfExtract {
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(SelfExtract.class.getName() + "Messages");
    private static BufferedReader in;

    protected static boolean acceptLicense = false;
    protected static boolean downloadDependencies = false;
    protected static boolean verbose = false;
    protected static String targetString = null;
    protected static boolean extractDirPredefined = false;

    public static final class VerboseExtractProgress implements ExtractProgress {
        @Override
        public void extractedFile(String f) {
            System.out.println("\t" + f);
        }

        @Override
        public void downloadingFile(URL sourceUrl, File targetFile) {
            out("downloadingFileNotice", new Object[] { sourceUrl.toString(), targetFile.getAbsolutePath() });
        }

        @Override
        public void dataDownloaded(int numBytes) {}

        @Override
        public void setFilesToExtract(int count) {}

        @Override
        public void commandRun(List args) {
            StringBuffer cmdString = new StringBuffer();
            for (int i = 0; i < args.size(); i++) {
                cmdString.append(args.get(i)).append(' ');
            }

            cmdString.setLength(cmdString.length() - 1);
            System.out.println(cmdString);
        }

        @Override
        public void commandsToRun(int count) {}

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void skippedFile() {}
    }

    /**
     * Main -- process arguments, display prompts, etc.
     *
     * @param args
     */
    public static void main(String[] args) {
        System.exit(doMain(args));

    }

    static SelfExtractor extractor = null;
    private static ReturnCode createExtractor_rc = null;
    private static boolean extractorCreated = false;

    // ok to call this before doMain from subclass main
    protected static ReturnCode createExtractor() {
        if (!extractorCreated) {
            createExtractor_rc = SelfExtractor.buildInstance();
            extractor = SelfExtractor.getInstance();
            extractorCreated = true;
        }
        return createExtractor_rc;
    }

    public static int doMain(String[] args) {

        ReturnCode rc = createExtractor();

        if (rc == ReturnCode.OK) {
            SelfExtract.acceptLicense = false;
            /*
             * THIS IS A TEMPORARY WORKAROUND - IN NORMAL OPERATION, acceptLicense MUST BE
             * ALWAYS INITIALISED TO FALSE.
             *
             * The tooling currently cannot display external dependency downloads to users -
             * the intended workaround is to put the external dependencies into a pseudo
             * license instead. We already handle this nicely here, so to avoid displaying
             * deps twice, we will NOT support licenses for samples. At GA, no samples will
             * have a real license, so that is okay.
             */
            if (extractor.isUserSample()) {
                SelfExtract.acceptLicense = true;
            }

            SelfExtract.downloadDependencies = false;
            //Default to disabled for command line only
            extractor.setDoExternalDepsDownload(false);
            SelfExtract.verbose = false;
            SelfExtract.targetString = null;
            boolean archiveHasLicense = extractor.hasLicense();

            //
            // Parse arguments
            //  this will call us back via the setters to update the flags/target location.
            //
            extractor.parseArguments(args, archiveHasLicense);

            if (archiveHasLicense) {
                extractor.handleLicenseAcceptance(extractor, acceptLicense);
            }

            //
            // Get extract location, only used if the extractor didn't supply us with one!
            //
            File outputDirFromUser;
            if (SelfExtract.targetString == null) {
                File workingDir = new File(System.getProperty("user.dir"));
                //Is the working dir a valid destination for this archive, or does it contain one? (must be called wlp)
                File defaultPath = findValidWlpInstallPath(workingDir, extractor);
                //Prompt the user to pick a destination, defaulting our best guess as selected above
                out(extractor.getExtractInstructionMessageKey());
                out("extractDefault", defaultPath.getAbsolutePath());
                System.out.println();
                SelfExtract.targetString = getResponse(format("extractPrompt") + " ");
                SelfExtract.targetString = SelfExtract.targetString.trim();
                if ("".equals(SelfExtract.targetString)) {
                    //User wants the default
                    outputDirFromUser = defaultPath;
                } else {
                    outputDirFromUser = new File(SelfExtract.targetString);
                }
            } else {
                outputDirFromUser = new File(targetString.trim());
            }

            String pathName = outputDirFromUser.getPath();
            if (pathName.startsWith("~")) {
                String pathNameToReturn = pathName.substring(1);
                String home = System.getenv("HOME");
                if (home == null) {
                    err("invalidInstall", pathName);
                    System.exit(ReturnCode.BAD_INPUT);
                }
                outputDirFromUser = new File(home, pathNameToReturn);
            }

            outputDirFromUser = outputDirFromUser.getAbsoluteFile();

            File outputDir = findValidWlpInstallPath(outputDirFromUser, extractor);
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                err("extractDirectoryError", outputDir.getAbsolutePath());
                System.exit(ReturnCode.BAD_OUTPUT);
            }

            // Note for product addons, getRoot returns "".
            File wlpOutputDir = new File(outputDir, extractor.getRoot());
            rc = extractor.validate(wlpOutputDir);
            if (rc != ReturnCode.OK && rc.getMessageKey() != null && rc.getMessageKey().equalsIgnoreCase("missingRequiredFeatures")) {
                rc = extractor.installMissingRequiredFeatures(wlpOutputDir, rc);
            }
            if (rc == ReturnCode.OK) {
                rc = checkUserWantsExternalDeps(extractor, SelfExtract.downloadDependencies);
                if (rc == ReturnCode.OK) {
                    // We only get here if we could create a new/fresh/clean wlp directory, and parsed external deps file if present
                    rc = extractor.extract(wlpOutputDir, verbose ? new VerboseExtractProgress() : null);
                }
            }
        }

        int code = rc.getCode();
        if (code > 0) {
            System.err.println(rc.getErrorMessage());
        } else {
            out(extractor.getExtractSuccessMessageKey());
        }

        return code;
    }

    // TODO we should revisit this logic. The validate is doing more work than is required by this method.
    protected static File findValidWlpInstallPath(File searchDirectory, SelfExtractor extractor) {
        // Checks the supplied directory to see if it either IS a valid wlp install dir for this archive, or CONTAINS a valid wlp install dir
        // (For core, we'll usually break out right away, as any new directory is valid)
        if ((extractor.isProductAddon() || extractor.isUserSample()) && extractor.validate(searchDirectory) != ReturnCode.OK) {
            // If the install path wasn't valid, and we are installing a product add on it could be
            // that we have been given the same path as the runtime archive installer was given. In this
            // case liberty will actually be installed in the wlp subfolder so we need to check there too.
            File wlpSubdir = new File(searchDirectory, "wlp");
            ReturnCode rc = extractor.validate(wlpSubdir);
            // The validate call does two things, it checks to see if the provided dir is liberty install
            // and it also validates that the archive can be installed into it. If the wlp folder isn't a
            // valid liberty install then we need to return the searchDirectory. If it is a valid directory
            // but it isn't an applicable install we want to return wlpSubdir so a later call to validate
            // will result in the right error message.
            if (rc.getCode() == ReturnCode.BAD_OUTPUT && ("invalidInstall".equals(rc.getMessageKey()) || "LICENSE_invalidInstall".equals(rc.getMessageKey()))) {
                return searchDirectory;
            } else {
                return wlpSubdir;
            }
        }
        return searchDirectory;
    }

    public static ReturnCode checkUserWantsExternalDeps(SelfExtractor extractor, boolean downloadDependencies) {
        if (extractor.hasExternalDepsFile()) {
            //Get the list of dependencies
            ExternalDependencies extDependencies;
            try {
                extDependencies = extractor.getExternalDependencies();
            } catch (Exception e) {
                return new ReturnCode(ReturnCode.UNREADABLE, "readDepsError", e.getMessage());
            }
            String dependenciesDesc = extDependencies.getDescription();
            List allDependencies = extDependencies.getDependencies();
            boolean enableDependencyDownload = false;

            if (!downloadDependencies) {
                //Not enabled on command line, so ask user to download dependencies
                StringBuilder printableDepsList = new StringBuilder();
                printableDepsList.append(dependenciesDesc).append("\n");

                for (int i = 0; i < allDependencies.size(); i++) {
                    ExternalDependency dependency = (ExternalDependency) allDependencies.get(i);
                    printableDepsList.append(dependency.getSourceUrl()).append("\n");
                }
                printableDepsList.append("\n");
                out("externalDepsInstruction", printableDepsList.toString());
                //1 is yes, and default, 2 is no
                enableDependencyDownload = getResponse(format("externalDepsPrompt", new Object[] { "[1]", "[2]" }), "1", "2", "1");
            } else {
                enableDependencyDownload = true;
            }

            //Enabled either by user or via command line
            extractor.setDoExternalDepsDownload(enableDependencyDownload);
        }
        return ReturnCode.OK;
    }

    public static String format(String key) {
        return format(key, new Object[0]);
    }

    public static String format(String key, Object o) {
        return format(key, new Object[] { o });
    }

    public static String format(String key, Object[] args) {
        return MessageFormat.format(resourceBundle.getString(key), args);
    }

    public static void wordWrappedOut(String fullLine) {
        List lines = SelfExtractUtils.wordWrap(fullLine, null);
        for (int i = 0; i < lines.size(); i++) {
            System.out.println(lines.get(i));
        }
        System.out.flush();
        System.out.println();
    }

    public static void out(String key) {
        out(key, new Object[0]);
    }

    public static void out(String key, Object o) {
        out(key, new Object[] { o });
    }

    public static void out(String key, Object[] args) {
        System.out.println(format(key, args));
    }

    public static void err(String key) {
        err(key, new Object[0]);
    }

    public static void err(String key, Object o) {
        err(key, new Object[] { o });
    }

    public static void err(String key, Object[] args) {
        System.err.println(format(key, args));
    }

    public static BufferedReader in() {
        if (in == null) {
            in = new BufferedReader(new InputStreamReader(System.in));
        }
        return in;
    }

    public static String getResponse(String promptString) {
        String input = null;
        System.out.print(promptString);
        try {
            input = in().readLine();
        } catch (IOException ioe) {
            err("inputException", ioe.getMessage());
        }
        if (input == null) {
            // Bad input. quit.
            // Users need to press enter to progress, which means if the read
            // succeeded, you have a non-null input string.
            System.exit(ReturnCode.BAD_INPUT);
        }
        return input;
    }

    public static boolean getResponse(String promptString, String yes, String no) {
        return getResponse(promptString, yes, no, null);
    }

    public static boolean getResponse(String promptString, String yes, String no, String defaultValue) {
        promptString += " ";
        String input = null;
        do {
            input = getResponse(promptString);

            if (input.length() == 0) {
                if (defaultValue != null) {
                    input = defaultValue;
                } else {
                    if (yes.length() == 0)
                        return true;
                    if (no.length() == 0)
                        return false;
                }
            }
            if (input.length() == 1) {
                if (yes.indexOf(input.charAt(0)) > -1)
                    return true;
                if (no.indexOf(input.charAt(0)) > -1)
                    return false;
            }
        } while (true);
    }

    /**
     * @param b
     */
    public static void setAcceptLicense(boolean b) {
        SelfExtract.acceptLicense = b;
    }

    /**
     * @param b
     */
    public static void setVerbose(boolean b) {
        SelfExtract.verbose = b;
    }

    /**
     * @param b
     */
    public static void setDownloadDependencies(boolean b) {
        SelfExtract.downloadDependencies = b;
    }

    /**
     * @param string
     */
    public static void setTargetString(String string) {
        SelfExtract.targetString = string;
    }

    public static void showLicenseFile(InputStream licenseFile) {
        SelfExtractor.buildInstance();
        SelfExtractor extractor = SelfExtractor.getInstance();
        extractor.showLicenseFile(licenseFile);
    }

    public static void handleLicenseAcceptance(LicenseProvider licenseProvider, boolean acceptLicense) {
        SelfExtractor.buildInstance();
        SelfExtractor extractor = SelfExtractor.getInstance();
        extractor.handleLicenseAcceptance(licenseProvider, acceptLicense);
    }
}
