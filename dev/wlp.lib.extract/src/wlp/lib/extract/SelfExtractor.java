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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import wlp.lib.extract.Content.Entry;

/**
 *
 */
public class SelfExtractor implements LicenseProvider {
    private static SelfExtractor instance;

    /**
     * True if running on Windows (.bat files, no chmod, etc.)
     */
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).indexOf("win") >= 0;

    // TODO remove this
    protected final ZipFile jarFile;
    protected final Content container;
    protected final String root;
    protected final List productMatches;
    private final String archiveContentType;
    private final String providedFeatures;
    protected final boolean productAddOn;
    private final boolean extractInstaller;
    private final LicenseProvider licenseProvider;
    private final String requiredFeatures;
    private final String serverName; // for --include=runnable type jars only
    private static final String EXTERNAL_DEPS_FILE = "externaldependencies.xml";
    private boolean doExternalDepsDownload = true;
    private ExternalDependencies externalDeps = null;
    private final boolean licensePresent;
    // For command line installs, we handle finding the user dir automatically. Tools will override with a specific one.
    private File userDirOverride = null;

    protected boolean allowNonEmptyInstallDirectory = false;
    private String productInstallType = null;

    public File getUserDirOverride() {
        return userDirOverride;
    }

    public void setUserDirOverride(File userDirOverride) {
        this.userDirOverride = userDirOverride;
    }

    public String getServerName() {
        return serverName;
    }

    public static class NullExtractProgress implements ExtractProgress {
        @Override
        public void extractedFile(String f) {
        }

        @Override
        public void downloadingFile(URL sourceUrl, File targetFile) {
        }

        @Override
        public void dataDownloaded(int numBytes) {
        }

        @Override
        public void setFilesToExtract(int count) {
        }

        @Override
        public void commandRun(List args) {
        }

        @Override
        public void commandsToRun(int count) {
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void skippedFile() {
        }
    }

    public static class ExternalDependencies {
        private String description = "";
        private final List dependencies = new ArrayList();
        //Size is lazily loaded by getSize()
        private int size = -1;

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDescription() {
            return this.description;
        }

        public void add(URL sourceUrl, String targetPath) {
            dependencies.add(new ExternalDependency(sourceUrl, targetPath));
        }

        public List getDependencies() {
            return dependencies;
        }

        /**
         * Sums up the total size of all external dependencies in the externaldependencies.xml by getting the content
         * length from the destination URL, and returns it as an int. Note: If any of the dependencies could not have
         * their content length determined (destination server would not provide it), their size is not accounted for -
         * so this is technically a 'best effort' or 'lower bound' on the total size.
         *
         * @return - the aggregate number of bytes for all dependency downloads.
         */
        public int getSize() {
            if (this.size < 0) {
                int total = 0;
                for (int i = 0; i < dependencies.size(); i++) {
                    URL thisDepURL = ((ExternalDependency) dependencies.get(i)).getSourceUrl();
                    int thisDepSize = SelfExtractUtils.tryGetContentLengthOfURL(thisDepURL);

                    if (thisDepSize != -1) {
                        total += thisDepSize;
                    }
                }
                this.size = total;
            }
            return this.size;
        }
    }

    public static class ExternalDependency {
        public ExternalDependency(URL sourceUrl, String targetPath) {
            this.sourceUrl = sourceUrl;
            this.targetPath = targetPath;
        }

        private final URL sourceUrl;
        private final String targetPath;

        public URL getSourceUrl() {
            return sourceUrl;
        }

        public String getTargetPath() {
            return targetPath;
        }

    }

    // TODO remove this
    protected SelfExtractor(JarFile jar, LicenseProvider licenseProvider, Attributes attributes) {
        this(jar, new Content.JarContent(jar), licenseProvider, attributes);
    }

    protected SelfExtractor(Content container, LicenseProvider licenseProvider, Attributes attributes) {
        this(null, container, licenseProvider, attributes);
    }

    private SelfExtractor(JarFile jar, Content container, LicenseProvider licenseProvider, Attributes attributes) {
        this.jarFile = jar;
        this.container = container;
        this.licensePresent = licenseProvider == null ? false : true;
        this.licenseProvider = licenseProvider;
        String rootDir = attributes.getValue("Archive-Root");
        root = rootDir != null ? rootDir : "";
        String appliesTo = attributes.getValue("Applies-To");
        extractInstaller = Boolean.valueOf(attributes.getValue("Extract-Installer")).booleanValue();
        // Find the header that contains the required features.
        requiredFeatures = attributes.getValue("Require-Feature");

        productMatches = parseAppliesTo(appliesTo);

        archiveContentType = attributes.getValue("Archive-Content-Type");

        if (!productMatches.isEmpty() && !isUserSample()) {
            productAddOn = true;
        } else {
            productAddOn = false;
        }

        providedFeatures = attributes.getValue("Provide-Feature");
        serverName = attributes.getValue("Server-Name");
    }

    /**
     * <p>This method will parse an applies to string to load a list of {@link ProductMatch} objects. If the applies to string is not a valid applies to string an empty list will
     * be returned.</p>
     *
     * @param appliesTo The string to construct the {@link ProductMatch} objects from. It is a comma separated list of items in the form:</p>
     *            <code>{product_id}; productVersion={product_version}; productInstallType={product_install_type}; productEdition={product_edition(s)}</code></p>
     *            Note that the {product_edition(s)} can be either a single edition or a comma separated list of editions enclosed in quotes. For example the following is
     *            a valid
     *            applies to string:</p>
     *            <code>com.ibm.websphere.appserver; productVersion=8.5.next.beta; productInstallType=Archive; productEdition="BASE,DEVELOPERS,EXPRESS,ND"</code>
     * @return A list of {@link ProductMatch} objects or an empty list if none are found
     */
    public static List parseAppliesTo(String appliesTo) {
        List matches = new ArrayList();

        if (appliesTo != null) {
            boolean quoted = false;
            int index = 0;
            ProductMatch match = new ProductMatch();
            for (int i = 0; i < appliesTo.length(); i++) {
                char c = appliesTo.charAt(i);
                if (c == '"') {
                    quoted = !quoted;
                }
                if (!quoted) {
                    if (c == ',') {
                        match.add(appliesTo.substring(index, i));
                        index = i + 1;
                        matches.add(match);
                        match = new ProductMatch();
                    } else if (c == ';') {
                        match.add(appliesTo.substring(index, i));
                        index = i + 1;
                    }
                }
            }

            match.add(appliesTo.substring(index));
            matches.add(match);
        }
        return matches;
    }

    public static final SelfExtractor getInstance() {
        return instance;
    }

    public static final ReturnCode buildInstance() {
        if (instance != null) {
            return ReturnCode.OK;
        }
        File self = SelfExtractUtils.getSelf();
        if (self == null) {
            return new ReturnCode(ReturnCode.NOT_FOUND, "licenseNotFound", new Object[] {});
        }
        Content container = null;
        String laPrefix = null;
        String liPrefix = null;
        Attributes mainAttributes = null;
        boolean hasLicense = true;
        try {
            container = Content.build(self);
            Manifest man = container.getManifest();
            mainAttributes = man.getMainAttributes();
            laPrefix = mainAttributes.getValue("License-Agreement");
            liPrefix = mainAttributes.getValue("License-Information");
            hasLicense = laPrefix != null && liPrefix != null;

        } catch (Exception e) {
            return new ReturnCode(ReturnCode.NOT_FOUND, "licenseNotFound", new Object[] {});
        }

        if (hasLicense) {
            ReturnCode buildLicenseProviderReturnCode = ContentLicenseProvider.buildInstance(container, laPrefix, liPrefix);
            if (buildLicenseProviderReturnCode != ReturnCode.OK) {
                return buildLicenseProviderReturnCode;
            }
        }

        instance = new SelfExtractor(container, hasLicense ? ContentLicenseProvider.getInstance() : null, mainAttributes);

        return ReturnCode.OK;
    }

    public String getExtractSuccessMessageKey() {
        return "extractSuccess";
    }

    public String getExtractInstructionMessageKey() {
        return "extractInstruction";
    }

    @Override
    public InputStream getLicenseAgreement() {
        return this.licenseProvider == null ? null : this.licenseProvider.getLicenseAgreement();
    }

    @Override
    public InputStream getLicenseInformation() {
        return this.licenseProvider == null ? null : this.licenseProvider.getLicenseInformation();
    }

    @Override
    public String getProgramName() {
        return this.licenseProvider == null ? null : this.licenseProvider.getProgramName();
    }

    @Override
    public String getLicenseName() {
        return this.licenseProvider == null ? null : this.licenseProvider.getLicenseName();
    }

    public boolean hasLicense() {
        return licensePresent;
    }

    public int getSize() {
        return container.size();
    }

    public int getTotalDepsSize() {
        try {
            return getExternalDependencies().getSize();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Returns the Archive-Root, or the empty String if this is a
     * product addon or a sample
     *
     * @return
     */
    public String getRoot() {
        return (productAddOn || isUserSample()) ? "" : root;
    }

    public ReturnCode validate(File outputDir) {
        boolean dirExists = outputDir.exists();
        if (productAddOn || isUserSample()) {
            ReturnCode result = validateProductMatches(outputDir, productMatches);
            if (result.getCode() == 0) {
                // Ensure that the server that we're extracting into has all the required core features.
                try {
                    Set missingFeatures = listMissingCoreFeatures(outputDir);

                    if (!missingFeatures.isEmpty()) {
                        result = new ReturnCode(ReturnCode.NOT_FOUND, "missingRequiredFeatures", new Object[] { container.getName(), missingFeatures, outputDir });
                    }
                } catch (SelfExtractorFileException sefe) {
                    result = new ReturnCode(ReturnCode.NOT_FOUND, "fileProcessingException", new Object[] { sefe.getFileName(), sefe.getCause() });
                }
            }

            return result;
        } else {
            if (dirExists && !!!allowNonEmptyInstallDirectory) {
                File[] files = outputDir.listFiles();
                if (files != null && files.length > 0) {
                    return new ReturnCode(ReturnCode.BAD_OUTPUT, "extractDirectoryExists", outputDir.getAbsolutePath());
                }

            }
        }

        return ReturnCode.OK;
    }

    private static class OutputStreamCopierInstallUtil implements Runnable {
        private final InputStream in;
        private final StringBuffer output;

        public OutputStreamCopierInstallUtil(InputStream in, StringBuffer output) {
            this.in = in;
            this.output = output;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                for (String line; (line = reader.readLine()) != null;) {
                    output.append(line);
                    output.append("\n");
                }
            } catch (IOException ex) {
                output.append(ex.getMessage());
            }
        }
    }

    private int installAssets(File installRoot, Set assets) {

        File java = new File(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        if (assets.size() == 0)
            return 0;
        File installJarFile = new File(installRoot, "bin/tools/ws-installUtility.jar");
        String[] runtimeCmd = new String[assets.size() + 5];
        runtimeCmd[0] = java.getAbsolutePath();
        runtimeCmd[1] = "-jar";
        runtimeCmd[2] = installJarFile.getAbsolutePath();
        runtimeCmd[3] = "install";
        runtimeCmd[4] = "--acceptLicense";
        int i = 5;
        Iterator iter = assets.iterator();
        while (iter.hasNext()) {
            runtimeCmd[i++] = ((String) iter.next()).trim();
        }
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec(runtimeCmd, null, installRoot);
        } catch (IOException e1) {
            return -1;
        }

        StringBuffer stdout = new StringBuffer();
        Thread stderrCopier = new Thread(new OutputStreamCopierInstallUtil(process.getErrorStream(), stdout));
        stderrCopier.start();
        new OutputStreamCopierInstallUtil(process.getInputStream(), stdout).run();
        try {
            stderrCopier.join();
            process.waitFor();
        } catch (InterruptedException e) {
            return -1;
        } finally {
        }
        if (process.exitValue() != 0)
            System.out.println(stdout.toString());
        else {

            try {
                Set missingFeatures = listMissingCoreFeatures(installRoot);

                // The missing features are not installed even thought installUtil return 0
                if (!missingFeatures.isEmpty()) {
                    System.out.println(stdout.toString());
                    return -1;
                }

            } catch (SelfExtractorFileException sefe) {
                return -1;
            }

        }
        return process.exitValue();
    }

    public ReturnCode installMissingRequiredFeatures(File outputDir, ReturnCode rc) {

        String version = System.getProperty("java.version");
        int pos = version.indexOf('.');
        pos = version.indexOf('.', pos + 1);
        if (Double.parseDouble(version.substring(0, pos)) < 1.8)
            return rc;
        String skipInstallRequiredAssets = System.getProperty("was.liberty.self.extractor.skip.install.required.assets");
        if (skipInstallRequiredAssets != null && skipInstallRequiredAssets.equalsIgnoreCase("true")) {
            return rc;
        }
        System.setProperty("was.liberty.self.extractor.skip.install.required.assets", "true");
        // Ensure that the server that we're extracting into has all the required core features.
        try {
            Set missingFeatures = listMissingCoreFeatures(outputDir);

            if (!missingFeatures.isEmpty()) {

                // try to install missing features.
                if (installAssets(outputDir, missingFeatures) == 0)
                    return ReturnCode.OK;

                return rc;
            }
        } catch (SelfExtractorFileException sefe) {
            return new ReturnCode(ReturnCode.NOT_FOUND, "fileProcessingException", new Object[] { sefe.getFileName(), sefe.getCause() });
        }
        return rc;
    }

    /**
     * This method will validate that the install location proposed is valid for the list of productMatches supplied. The {@link ProductMatch#matches(Properties)} method will be
     * invoked for all of the supplied <code>productMatches</code> objects and for all of the properties files found in the lib/versions directory for the current install. The
     * matches method must return either {@link ProductMatch#NOT_APPLICABLE} or {@link ProductMatch#MATCHED} for every properties file for this method to return
     * {@link ReturnCode#OK}.
     *
     * @param outputDir Where the product is being installed to
     * @param productMatches The list of {@link ProductMatch} objects that need to be satisfied to the current install
     * @return A {@link ReturnCode} indicating if this was successful or not
     */
    public static ReturnCode validateProductMatches(File outputDir, List productMatches) {
        boolean dirExists;
        dirExists = outputDir.exists();
        if (dirExists) {
            File f = new File(outputDir, "lib/versions");
            File[] files = f.listFiles();
            if (files == null || files.length == 0) {
                // output error
                return new ReturnCode(ReturnCode.BAD_OUTPUT, "invalidInstall", outputDir.getAbsolutePath());
            } else {
                propFiles: for (int i = 0; i < files.length; i++) {
                    if (!files[i].getAbsolutePath().endsWith(".properties"))
                        continue;
                    Properties props = new Properties();
                    InputStream is = null;
                    try {
                        is = new FileInputStream(files[i]);
                        props.load(is);
                    } catch (IOException e) {
                        continue;
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException e) {
                            }
                        }
                    }

                    Iterator matches = productMatches.iterator();
                    while (matches.hasNext()) {
                        ProductMatch match = (ProductMatch) matches.next();
                        int result = match.matches(props);
                        if (result == ProductMatch.NOT_APPLICABLE) {
                            continue;
                        } else if (result == ProductMatch.INVALID_VERSION || result == ProductMatch.INVALID_EDITION) {
                            List longIDs = new ArrayList();
                            Iterator matchesItr = match.getEditions().iterator();

                            while (matchesItr.hasNext()) {
                                String shortID = (String) matchesItr.next();
                                String editionName = InstallUtils.getEditionName(shortID);
                                longIDs.add(editionName);
                            }
                            String edition = InstallUtils.getEditionName(props.getProperty("com.ibm.websphere.productEdition"));

                            if (result == ProductMatch.INVALID_VERSION) {
                                return new ReturnCode(ReturnCode.BAD_OUTPUT, "invalidVersion", new Object[] { props.getProperty("com.ibm.websphere.productVersion"),
                                                                                                              match.getVersion(),
                                                                                                              edition,
                                                                                                              longIDs,
                                                                                                              props.getProperty("com.ibm.websphere.productLicenseType") });
                            } else if (result == ProductMatch.INVALID_EDITION) {
                                return new ReturnCode(ReturnCode.BAD_OUTPUT, "invalidEdition", new Object[] { edition,
                                                                                                              longIDs,
                                                                                                              props.getProperty("com.ibm.websphere.productVersion"),
                                                                                                              match.getVersion(),
                                                                                                              props.getProperty("com.ibm.websphere.productLicenseType") });
                            }
                        } else if (result == ProductMatch.INVALID_INSTALL_TYPE) {
                            return new ReturnCode(ReturnCode.BAD_OUTPUT, "invalidInstallType", new Object[] { props.getProperty("com.ibm.websphere.productInstallType"),
                                                                                                              match.getInstallType() });
                        } else if (result == ProductMatch.INVALID_LICENSE) {
                            return new ReturnCode(ReturnCode.BAD_OUTPUT, "invalidLicense", new Object[] { props.getProperty("com.ibm.websphere.productLicenseType"),
                                                                                                          match.getLicenseType() });
                        }
                        break propFiles;
                    }
                }
            }
        } else {
            // output error
            return new ReturnCode(ReturnCode.BAD_OUTPUT, "invalidInstall", outputDir.getAbsolutePath());
        }
        return ReturnCode.OK;
    }

    public ReturnCode extract(File wlpInstallDir, ExtractProgress ep) {
        List<File> createdDirectoriesAndFiles = new ArrayList<File>();

        File outputDir = null;
        if (isUserSample()) {
            //We're a sample, so we should go into the user directory.
            if (userDirOverride != null) {
                outputDir = userDirOverride;
            } else {
                outputDir = determineTargetUserDirectory(wlpInstallDir);
            }
            SelfExtract.out("targetUserDirectory", new Object[] { outputDir.getAbsolutePath() });
        } else {
            //Extract to the product install directory
            outputDir = wlpInstallDir;
        }

        boolean dirExists = outputDir.exists();

        if (!!!allowNonEmptyInstallDirectory && !!!dirExists && !!!SelfExtractUtils.trackedMkdirs(outputDir, createdDirectoriesAndFiles)) {
            SelfExtractUtils.rollbackExtract(createdDirectoriesAndFiles);
            return new ReturnCode(ReturnCode.BAD_OUTPUT, "extractDirectoryError", outputDir.getAbsolutePath());
        }

        if (ep == null) {
            ep = new NullExtractProgress();
        }

        // Do download first
        ReturnCode rc = downloadFile(outputDir, createdDirectoriesAndFiles, ep);
        if (!rc.equals(ReturnCode.OK)) {
            return rc;
        }

        //Do the extract
        byte[] buf = new byte[4096];
        List<String> extractedFiles = new ArrayList<String>();
        boolean continueInstall = true;

        String metaInfDir = "META-INF/";

        // Stores the valid root file paths to extract along with the common directories with the archive root.
        // Any directories that are common with the archive root will be handled as part of the root extract.
        HashMap<String, String> filePathsToExtract = new HashMap<String, String>();
        filePathsToExtract.put(root, root);
        filePathsToExtract.put(metaInfDir, "");
        List<String> extensionRootDirs = new ArrayList<String>();

        if (!isUserSample() && !productAddOn) {
            try {
                extensionRootDirs = getExtensionInstallDirs();
                String[] s = root.split("/");

                for (Iterator it = extensionRootDirs.iterator(); it.hasNext();) {
                    String installDir = (String) it.next();
                    // Ignore all extension directories that are under the archive root since
                    // they will be laid down as part of the root extract
                    if (!installDir.startsWith(root)) {
                        String rootDir = "";
                        // Determine directories that are common with the archive root
                        for (int i = 0; i < s.length; i++) {
                            String dirname = s[i] + "/";
                            if (installDir.startsWith(dirname)) {
                                rootDir = rootDir.concat(dirname);
                            }
                        }

                        filePathsToExtract.put(installDir, rootDir);
                    }
                }
            } catch (IOException ioe) {
                return new ReturnCode(ReturnCode.BAD_OUTPUT, "extractFileError", ioe.getMessage());
            }
        }

        SelfExtract.out("extractDirectory", new Object[] { outputDir.getAbsolutePath() });
        if (!container.isExtracted()) {
            for (Entry entry : container) {
                String name = entry.getName();
                String commonRootDir = getCommonRootDir(name, filePathsToExtract);
                if (null != commonRootDir) {
                    if (entry.isDirectory()) {
                        //if it's  META-INF dir or below, being created, then create it under wlp/lib/extract
                        //Do not extract the installer unless extractInstaller (Extract-Installer in manifest) is true.
                        if (!extractInstaller && (name.startsWith(metaInfDir) || name.startsWith("wlp/lib/extract/"))) {
                            ep.skippedFile();
                            continue;
                        }
                        File file;
                        if (name.startsWith(metaInfDir)) {
                            file = new File(outputDir, "lib/extract");
                            if (!file.exists() && !SelfExtractUtils.trackedMkdirs(file, createdDirectoriesAndFiles)) {
                                SelfExtractUtils.rollbackExtract(createdDirectoriesAndFiles);
                                return new ReturnCode(ReturnCode.BAD_OUTPUT, "extractDirectoryError", file.getAbsolutePath());
                            }
                            file = new File(new File(outputDir, "lib/extract"), name);
                        } else if (name.startsWith(root)) {
                            file = new File(outputDir, name.substring(commonRootDir.length()));
                        } else {
                            file = new File(outputDir.getParentFile(), name.substring(commonRootDir.length()));
                        }

                        if (!file.exists() && !SelfExtractUtils.trackedMkdirs(file, createdDirectoriesAndFiles)) {
                            SelfExtractUtils.rollbackExtract(createdDirectoriesAndFiles);
                            return new ReturnCode(ReturnCode.BAD_OUTPUT, "extractDirectoryError", file.getAbsolutePath());
                        }
                        ep.skippedFile();
                    } else {
                        //for a standard extract, move meta-inf under wlp/lib/extract.
                        //Do not extract the installer unless extractInstaller (Extract-Installer in manifest) is true.
                        if (!extractInstaller && (name.startsWith(metaInfDir) || name.startsWith("wlp/lib/extract/"))) {
                            ep.skippedFile();
                            continue;
                        }
                        File file;
                        if (name.startsWith(metaInfDir)) {
                            file = new File(outputDir, "lib/extract");
                            file = new File(file, name);
                        } else if (name.startsWith(root)) {
                            file = new File(outputDir, name.substring(commonRootDir.length()));
                        } else {
                            file = new File(outputDir.getParentFile(), name.substring(commonRootDir.length()));
                        }

                        if (file.exists()) {
                            if (productAddOn) {
                                continue; // If the file already exists just skip over it.
                            } else if (allowNonEmptyInstallDirectory) {
                                if (name.endsWith("wlp/lib/extract/META-INF/MANIFEST.MF")) {
                                    // Sometimes there will be 2 manifests, one at META-INF/ and one at wlp/lib/extract/META-INF/
                                    // In this case we want to take the one at META-INF/ and ignore the other one
                                    continue;
                                } else if (name.endsWith("META-INF/MANIFEST.MF")) {
                                    file.delete();
                                } else {
                                    SelfExtractUtils.rollbackExtract(createdDirectoriesAndFiles);
                                    return new ReturnCode(ReturnCode.BAD_OUTPUT, "extractFileExists", file.getAbsolutePath());
                                }
                            }
                        }

                        File parentFile = file.getParentFile();
                        if (!parentFile.exists() && !SelfExtractUtils.trackedMkdirs(parentFile, createdDirectoriesAndFiles)) {
                            SelfExtractUtils.rollbackExtract(createdDirectoriesAndFiles);
                            return new ReturnCode(ReturnCode.BAD_OUTPUT, "extractDirectoryError", parentFile.getAbsolutePath());
                        }

                        ep.extractedFile(name);
                        extractedFiles.add(name);
                        createdDirectoriesAndFiles.add(file);

                        OutputStream os = null;
                        InputStream is = null;

                        // override the productInstallType property
                        if (productInstallType != null &&
                            name.equalsIgnoreCase("wlp/lib/versions/WebSphereApplicationServer.properties")) {
                            Properties wasProps = new Properties();
                            try {
                                is = entry.getInputStream();
                                wasProps.load(is);
                                wasProps.put("com.ibm.websphere.productInstallType", productInstallType);
                                os = new FileOutputStream(file);
                                wasProps.store(os, null);
                            } catch (IOException ioe) {
                                SelfExtractUtils.rollbackExtract(createdDirectoriesAndFiles);
                                return new ReturnCode(ReturnCode.BAD_OUTPUT, "extractFileError", ioe.getMessage());
                            } finally {
                                SelfExtractUtils.tryToClose(is);
                                SelfExtractUtils.tryToClose(os);
                            }
                            continue;
                        }

                        try {
                            os = new BufferedOutputStream(new FileOutputStream(file));
                            is = entry.getInputStream();

                            for (int read; (read = is.read(buf)) != -1;) {
                                os.write(buf, 0, read);
                            }
                        } catch (IOException ioe) {
                            SelfExtractUtils.tryToClose(os);
                            SelfExtractUtils.tryToClose(is);
                            SelfExtractUtils.rollbackExtract(createdDirectoriesAndFiles);
                            return new ReturnCode(ReturnCode.BAD_OUTPUT, "extractFileError", ioe.getMessage());
                        } finally {
                            SelfExtractUtils.tryToClose(os);
                            SelfExtractUtils.tryToClose(is);
                        }
                    }
                } else {
                    ep.skippedFile();
                }

                continueInstall = !!!ep.isCanceled();
            }
        }

        if (continueInstall) {
            rc = setFilePermission(outputDir, extensionRootDirs, ep);

            if (ReturnCode.OK.getCode() != rc.getCode())
                return rc;
        }

        // We've extracted some files. If this is an Extended install, check to see if any iFixes should be
        // reapplied.
        if (productAddOn) {
            printNeededIFixes(outputDir, extractedFiles);
        }

        if (!!!continueInstall) {
            SelfExtractUtils.rollbackExtract(createdDirectoriesAndFiles);
        }

        return ReturnCode.OK;

    }

    private ReturnCode setFilePermission(File outputDir, List<String> extInstallDirs, ExtractProgress ep) {
        ReturnCode rc = fixScriptPermissions(ep, outputDir);
        if (rc != null)
            return rc;

        try {
            rc = SelfExtractUtils.processExecutableDirective(outputDir);
        } catch (Exception e) {
            return new ReturnCode(ReturnCode.BAD_OUTPUT, "extractFileError", e.getMessage());
        }

        if (ReturnCode.OK.getCode() != rc.getCode())
            return rc;

        File outputDirParent = outputDir.getParentFile();
        for (Iterator<String> it = extInstallDirs.iterator(); it.hasNext();) {
            String installDir = it.next();
            File extDir = new File(outputDirParent, installDir);

            try {
                rc = SelfExtractUtils.processExecutableDirective(extDir);
            } catch (Exception e) {
                return new ReturnCode(ReturnCode.BAD_OUTPUT, "extractFileError", e.getMessage());
            }

            if (ReturnCode.OK.getCode() != rc.getCode())
                return rc;
        }

        return (null == rc) ? ReturnCode.OK : rc;
    }

    /**
     * If URLConnection is an HTTP connection, check that the response code is HTTP_OK
     *
     * @param uc the URLConnection
     * @throws IOException if the URLConnection is an HttpURLConnection and the response code is not HTTP_OK (200) or HTTP_MOVED_TEMP (302)
     */
    private static void checkResponseCode(URLConnection uc) throws IOException {
        if (uc instanceof HttpURLConnection) {
            HttpURLConnection httpConnection = (HttpURLConnection) uc;
            int rc = httpConnection.getResponseCode();
            if (rc != HttpURLConnection.HTTP_OK && rc != HttpURLConnection.HTTP_MOVED_TEMP) {
                throw new IOException();
            }
        }
    }

    private InputStream getInputStreamCheckRedirects(URLConnection uc) throws IOException {
        boolean isRedirect;
        int redirects = 0;
        InputStream in = null;
        do {
            uc.setReadTimeout(30 * 1000);
            checkResponseCode(uc);

            if (uc instanceof HttpURLConnection) {
                ((HttpURLConnection) uc).setInstanceFollowRedirects(false);
            }

            in = uc.getInputStream();
            isRedirect = false;

            if (uc instanceof HttpURLConnection) {
                HttpURLConnection http = (HttpURLConnection) uc;
                int stat = http.getResponseCode();

                if (stat >= 300 && stat <= 307 && stat != 306 &&
                    stat != HttpURLConnection.HTTP_NOT_MODIFIED) {
                    URL base = http.getURL();
                    String loc = http.getHeaderField("Location");
                    URL target = null;

                    if (loc != null) {
                        target = new URL(base, loc);
                    }

                    http.disconnect();

                    if (target == null || !(target.getProtocol().equals("http")
                                            || target.getProtocol().equals("https"))
                        || redirects >= 5) {
                        throw new SecurityException("illegal URL redirect");
                    }

                    isRedirect = true;
                    uc = target.openConnection();
                    redirects++;
                }
            }
        } while (isRedirect);
        return in;
    }

    private ReturnCode downloadFile(File downloadDir, List<File> createdDirectoriesAndFiles, ExtractProgress ep) {
        if (doExternalDepsDownload && hasExternalDepsFile()) {
            List depList = null;
            try {
                depList = getExternalDependencies().getDependencies();
            } catch (Exception e) {
                return new ReturnCode(ReturnCode.UNREADABLE, "readDepsError", new Object[] {});
            }

            SelfExtract.out("downloadingBeginNotice", new Object[] { "--verbose" });

            byte[] buffer = new byte[4096];
            for (int i = 0; i < depList.size(); i++) {
                ExternalDependency thisDep = (ExternalDependency) depList.get(i);
                URL sourceUrl = thisDep.getSourceUrl();
                String targetPath = thisDep.getTargetPath();
                File usrDir = downloadDir;

                File targetFile = new File(usrDir, targetPath);
                File targetDir = targetFile.getParentFile();

                if (targetFile.exists()) {
                    if (productAddOn || isUserSample()) {
                        //The file already exists, so skip it
                        continue;
                    } else if (allowNonEmptyInstallDirectory) {
                        SelfExtractUtils.rollbackExtract(createdDirectoriesAndFiles);
                        return new ReturnCode(ReturnCode.BAD_OUTPUT, "extractFileExists", targetFile.getAbsolutePath());
                    }
                }

                if (!!!SelfExtractUtils.trackedMkdirs(targetDir, createdDirectoriesAndFiles) && !!!targetDir.exists()) {
                    SelfExtractUtils.rollbackExtract(createdDirectoriesAndFiles);
                    return new ReturnCode(ReturnCode.BAD_OUTPUT, "extractDirectoryError", targetDir.getAbsolutePath());
                }

                ep.downloadingFile(sourceUrl, targetFile);
                createdDirectoriesAndFiles.add(targetFile);
                InputStream input = null;
                OutputStream output = null;

                try {
                    URLConnection uc = sourceUrl.openConnection();
                    input = getInputStreamCheckRedirects(uc);
                    output = new FileOutputStream(targetFile);

                    int n = -1;
                    while ((n = input.read(buffer)) != -1) {
                        output.write(buffer, 0, n);
                        ep.dataDownloaded(n);
                    }
                } catch (IOException ioe) {
                    SelfExtractUtils.tryToClose(output);
                    SelfExtractUtils.tryToClose(input);
                    SelfExtractUtils.rollbackExtract(createdDirectoriesAndFiles);
                    return new ReturnCode(ReturnCode.BAD_OUTPUT, "downloadFileError", new String[] { sourceUrl.toString(), targetFile.toString() });
                } finally {
                    SelfExtractUtils.tryToClose(output);
                    SelfExtractUtils.tryToClose(input);
                }

            }
        }

        return ReturnCode.OK;
    }

    /**
     * Retrieves the directory in common between the specified path and the archive root directory.
     * If the file path cannot be found among the valid paths then null is returned.
     *
     * @param filePath Path to the file to check
     * @param validFilePaths A list of valid file paths and their common directories with the root
     * @return The directory in common between the specified path and the root directory
     */
    private String getCommonRootDir(String filePath, HashMap validFilePaths) {
        for (Iterator it = validFilePaths.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            String path = (String) ((entry).getKey());
            if (filePath.startsWith(path))
                return (String) entry.getValue();
        }

        return null;
    }

    /**
     * Retrieves all the extension products' install directories as indicated their properties file.
     *
     * @return List of extension products' install directories.
     * @throws IOException
     */
    private ArrayList<String> getExtensionInstallDirs() throws IOException {
        String extensiondir = root + "etc/extensions/";
        ArrayList<String> extensionDirs = new ArrayList<String>();

        for (Entry entry : container) {
            if (entry.getName().startsWith(extensiondir) && entry.getName().endsWith(".properties")) {
                Properties prop = new Properties();
                prop.load(entry.getInputStream());
                String installDir = (prop.getProperty("com.ibm.websphere.productInstall"));

                if (null != installDir && !installDir.equals("")) {
                    extensionDirs.add(installDir);
                }
            }
        }

        return extensionDirs;
    }

    /**
     * @param ep - The object that will invoke the chmod commands.
     * @param outputDir - The Liberty runtime install root.
     */
    public ReturnCode fixScriptPermissions(ExtractProgress ep, File outputDir) {
        return fixScriptPermissions(ep, outputDir, null);
    }

    /**
     * @param ep - The object that will invoke the chmod commands.
     * @param outputDir - The Liberty runtime install root.
     * @param filter - A zip file containing files that we want to do the chmod against.
     */
    public ReturnCode fixScriptPermissions(ExtractProgress ep, File outputDir, ZipFile filter) {
        return SelfExtractUtils.fixScriptPermissions(ep, outputDir, filter);
    }

    /**
     * Get the user directory, defaulting to <installDir>/usr if WLP_USER_DIR is not set in server.env.
     *
     * @param extractor
     * @return
     */
    public static File determineTargetUserDirectory(File wlpInstallDir) {
        File defaultUserDir = new File(wlpInstallDir, "usr");
        File serverEnvFile = new File(wlpInstallDir, "etc/server.env");
        if (serverEnvFile.exists()) {
            //server.env wins, so check it first
            Properties serverEnvProps = new Properties();
            FileInputStream serverEnvStream = null;
            try {
                serverEnvStream = new FileInputStream(serverEnvFile);
                serverEnvProps.load(serverEnvStream);
            } catch (Exception e) {
                //Do nothing at the moment, and fall back to default dir.
            } finally {
                SelfExtractUtils.tryToClose(serverEnvStream);
            }
            String customUserDir = serverEnvProps.getProperty("WLP_USER_DIR");
            if (customUserDir != null && !"".equals(customUserDir)) {
                return new File(customUserDir);
            }
        } else {
            //No server.env, environment variables take next precedence
            String envVarUserDir = System.getenv("WLP_USER_DIR");
            if (envVarUserDir != null && !"".equals(envVarUserDir)) {
                return new File(envVarUserDir);
            }
        }
        //No server.env setting, or environment variable, so take default
        return defaultUserDir;
    }

    public boolean isUserSample() {
        return archiveContentType != null && archiveContentType.equalsIgnoreCase("sample");
    }

    public boolean isProductAddon() {
        return productAddOn;
    }

    public String getArchiveContentType() {
        return archiveContentType;
    }

    public String getProvidedFeatures() {
        return providedFeatures;
    }

    public boolean hasExternalDepsFile() {
        return (container.getEntry(EXTERNAL_DEPS_FILE) != null);
    }

    public void setDoExternalDepsDownload(boolean value) {
        doExternalDepsDownload = value;
    }

    private void buildExternalDependencies() throws Exception {
        Entry depsEntry = null;
        ExternalDependencies newDeps = new ExternalDependencies();

        if ((depsEntry = container.getEntry(EXTERNAL_DEPS_FILE)) != null) {
            InputStream entryInputStream = null;
            try {
                entryInputStream = depsEntry.getInputStream();

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db;
                db = dbf.newDocumentBuilder();
                Document doc = db.parse(entryInputStream);
                Element rootEle = doc.getDocumentElement();

                newDeps.setDescription(rootEle.getAttribute("description"));

                NodeList dependencies = rootEle.getElementsByTagName("dependency");

                for (int i = 0; i < dependencies.getLength(); i++) {
                    Node node = dependencies.item(i);
                    Element ele = (Element) node;

                    URL sourceUrl = new URL(ele.getAttribute("url"));
                    String targetPath = ele.getAttribute("targetpath");
                    newDeps.add(sourceUrl, targetPath);
                }

            } finally {
                SelfExtractUtils.tryToClose(entryInputStream);
            }
        }
        externalDeps = newDeps;
    }

    public ExternalDependencies getExternalDependencies() throws Exception {
        if (externalDeps == null) {
            buildExternalDependencies();
        }
        return externalDeps;
    }

    /**
     * If necessary this will print a message saying that the installed files mean that an iFix needs to be re-installed.
     *
     * @param outputDir The directory where the files were extracted to (typically the "wlp" directory)
     * @param extractedFiles A list of Strings which are file paths within the directory
     */
    public static void printNeededIFixes(File outputDir, List extractedFiles) {
        try {
            // To get the ifix information we run the productInfo validate command which as well as
            // listing the state of the runtime, also displays any ifixes that need to be reapplied.
            Runtime runtime = Runtime.getRuntime();
            // Set up the command depending on the OS we're running on.
            final String productInfo = new File(outputDir, isWindows ? "bin/productInfo.bat" : "bin/productInfo").getAbsolutePath();
            final String[] runtimeCmd = { productInfo, "validate" };
            Process process = runtime.exec(runtimeCmd, null, new File(outputDir, "bin"));

            Thread stderrCopier = new Thread(new OutputStreamCopier(process.getErrorStream(), System.err));
            stderrCopier.start();
            new OutputStreamCopier(process.getInputStream(), System.out).run();

            try {
                stderrCopier.join();
                process.waitFor();
            } catch (InterruptedException e) {
                // Auto FFDC
            }
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }

    private static class OutputStreamCopier implements Runnable {
        private final InputStream in;
        private final PrintStream out;

        OutputStreamCopier(InputStream in, PrintStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            try {
                int count;
                byte[] buf = new byte[4096];
                while ((count = in.read(buf)) >= 0) {
                    out.write(buf, 0, count);
                }
            } catch (IOException ex) {
                out.println(ex.getMessage());
            }
        }
    }

    protected static FilenameFilter createManifestFilter() {
        return new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                boolean result = false;
                if (name.endsWith(".mf"))
                    result = true;

                return result;
            }
        };
    }

    /**
     * This method checks that all the core features defined in the the manifest header exist in the server runtime
     * we're extracting into, and returns any features that don't.
     * If the coreFeatures header is blank it means we're not an extended jar.
     *
     * @return A Set of Strings containing the features that are missing from the installation runtime.
     */
    protected Set listMissingCoreFeatures(File outputDir) throws SelfExtractorFileException {

        Set missingFeatures = new HashSet();

        // If we have a Require Feature manifest header, we need to check that the runtime we're extracting into contains the
        // required features. If the customer has minified their runtime to a smaller set of features, then we may
        // not be able to install this extract into the runtime.
        // If we don't have a list of features then this is probably because we're not an extended install, and we don't need to check anymore.
        if (requiredFeatures != null && !"".equals(requiredFeatures)) {

            // Break the required feature headers value into indiviual strings and load them into a set.
            StringTokenizer tokenizer = new StringTokenizer(requiredFeatures, ",");
            while (tokenizer.hasMoreElements()) {
                String nextFeature = tokenizer.nextToken();
                if (nextFeature.indexOf(";") >= 0)
                    nextFeature = nextFeature.substring(0, nextFeature.indexOf(";"));
                missingFeatures.add(nextFeature.trim());
            }

            // Create fileFilter to get just the manifest files.
            FilenameFilter manifestFilter = createManifestFilter();

            File featuresDir = new File(outputDir + "/lib/features");
            File[] manifestFiles = featuresDir.listFiles(manifestFilter);
            // Iterate over each manifest in the runtime we're extracting to, until we've read all manifests or until we've found all of the
            // required features.
            if (manifestFiles != null) {
                for (int i = 0; i < manifestFiles.length && !missingFeatures.isEmpty(); i++) {

                    FileInputStream fis = null;
                    File currentManifestFile = null;
                    try {
                        currentManifestFile = manifestFiles[i];
                        fis = new FileInputStream(currentManifestFile);
                        Manifest currentManifest = new Manifest(fis);

                        Attributes attrs = currentManifest.getMainAttributes();
                        String manifestSymbolicName = attrs.getValue("Subsystem-SymbolicName");
                        if (manifestSymbolicName.indexOf(";") >= 0)
                            manifestSymbolicName = manifestSymbolicName.substring(0, manifestSymbolicName.indexOf(";"));

                        // Remove the current manifest from the list of required features. We may need to remove the short name depending on what has
                        // been stored.
                        missingFeatures.remove(manifestSymbolicName.trim());
                    } catch (FileNotFoundException fnfe) {
                        throw new SelfExtractorFileException(currentManifestFile.getAbsolutePath(), fnfe);
                    } catch (IOException ioe) {
                        throw new SelfExtractorFileException(currentManifestFile.getAbsolutePath(), ioe);
                    } finally {
                        SelfExtractUtils.tryToClose(fis);
                    }
                }
            }
        }
        return missingFeatures;
    }

    protected static final class SelfExtractorFileException extends Exception {
        private final String fileName;

        public SelfExtractorFileException(String fileName, Throwable exception) {
            super(exception);
            this.fileName = fileName;
        }

        public String getFileName() {
            return this.fileName;
        }
    }

    public void parseArguments(String[] args, boolean archiveHasLicense) {
        //
        // Parse arguments
        //
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].trim().toLowerCase(Locale.ENGLISH);

            if (arg.startsWith("-")) {
                if (argIsOption(arg, "-viewlicenseagreement")) {
                    if (archiveHasLicense) {
                        showLicenseFile(getLicenseAgreement());
                    } else {
                        SelfExtract.out("archiveContainsNoLicense");
                    }
                    System.exit(0);
                } else if (argIsOption(arg, "-viewlicenseinfo") || argIsOption(arg, "-viewlicenseinformation")) {
                    if (archiveHasLicense) {
                        showLicenseFile(getLicenseInformation());
                    } else {
                        SelfExtract.out("archiveContainsNoLicense");
                    }
                    System.exit(0);
                } else if (argIsOption(arg, "-help")) {
                    displayCommandLineHelp(this);
                    System.exit(0);
                } else if (argIsOption(arg, "-acceptlicense")) {
                    SelfExtract.setAcceptLicense(true);
                } else if (argIsOption(arg, "-downloadDependencies")) {
                    SelfExtract.setDownloadDependencies(true);
                } else if (argIsOption(arg, "-verbose")) {
                    SelfExtract.setVerbose(true);

                } else {
                    System.out.println("\n" + SelfExtract.format("invalidOption", arg));
                    displayCommandLineHelp(this);
                    System.exit(0);
                }
            } else {
                SelfExtract.setTargetString(args[i]);
            }
        }
    }

    /**
     * Test if the argument is an option. Allow single or double leading -, be
     * case insensitive.
     *
     * @param arg
     *            User specified argument
     * @param option
     *            Option for test/comparison
     * @return true if the argument matches the option
     */
    protected static boolean argIsOption(String arg, String option) {
        return arg.equalsIgnoreCase(option) || arg.equalsIgnoreCase('-' + option);
    }

    /**
     * Display command line usage.
     */
    protected static void displayCommandLineHelp(SelfExtractor extractor) {
        // This method takes a SelfExtractor in case we want to tailor the help to the current archive
        // Get the name of the JAR file to display in the command syntax");
        String jarName = System.getProperty("sun.java.command", "wlp-liberty-developers-core.jar");
        String[] s = jarName.split(" ");
        jarName = s[0];

        System.out.println("\n" + SelfExtract.format("usage"));
        System.out.println("\njava -jar " + jarName + " [" + SelfExtract.format("options") + "] [" + SelfExtract.format("installLocation") + "]\n");

        System.out.println(SelfExtract.format("options"));
        System.out.println("    --acceptLicense");
        System.out.println("        " + SelfExtract.format("helpAcceptLicense"));
        System.out.println("    --verbose");
        System.out.println("        " + SelfExtract.format("helpVerbose"));
        System.out.println("    --viewLicenseAgreement");
        System.out.println("        " + SelfExtract.format("helpAgreement"));
        System.out.println("    --viewLicenseInfo");
        System.out.println("        " + SelfExtract.format("helpInformation"));
        if (extractor.isUserSample()) {
            System.out.println("    --downloadDependencies");
            System.out.println("        " + SelfExtract.format("helpDownloadDependencies"));
        }
    }

    /**
     * Display the license file. If an error occurs reading or writing the
     * license file, exit with a message
     *
     * @param licenseFile
     *            The license file to display
     */
    public void showLicenseFile(InputStream licenseFile) {
        Object e = SelfExtract.class;
        if (licenseFile != null) {
            e = SelfExtractUtils.showLicenseFile(licenseFile);
        }
        if (e != null) {
            SelfExtract.err("licenseNotFound");
            System.exit(ReturnCode.UNREADABLE);
        }
    }

    /**
     * This method will print out information about the license and if necessary prompt the user to accept it.
     *
     * @param licenseProvider The license provider to use to get information about the license from the archive
     * @param acceptLicense <code>true</code> if the license should be automatically accepted
     */
    public void handleLicenseAcceptance(LicenseProvider licenseProvider, boolean acceptLicense) {
        //
        // Display license requirement
        //
        SelfExtract.wordWrappedOut(SelfExtract.format("licenseStatement", new Object[] { licenseProvider.getProgramName(), licenseProvider.getLicenseName() }));
        System.out.println();

        if (acceptLicense) {
            // Indicate license acceptance via option
            SelfExtract.wordWrappedOut(SelfExtract.format("licenseAccepted", "--acceptLicense"));
            System.out.println();
        } else {
            // Check for license agreement: exit if not accepted.
            if (!obtainLicenseAgreement(licenseProvider)) {
                System.exit(0);
            }
        }
    }

    /**
     * Display and obtain agreement for the license terms
     */
    private static boolean obtainLicenseAgreement(LicenseProvider licenseProvider) {
        // Prompt for word-wrapped display of license agreement & information
        boolean view;

        SelfExtract.wordWrappedOut(SelfExtract.format("showAgreement", "--viewLicenseAgreement"));
        view = SelfExtract.getResponse(SelfExtract.format("promptAgreement"), "", "xX");
        if (view) {
            SelfExtract.showLicenseFile(licenseProvider.getLicenseAgreement());
            System.out.println();
        }

        SelfExtract.wordWrappedOut(SelfExtract.format("showInformation", "--viewLicenseInfo"));
        view = SelfExtract.getResponse(SelfExtract.format("promptInfo"), "", "xX");
        if (view) {
            SelfExtract.showLicenseFile(licenseProvider.getLicenseInformation());
            System.out.println();
        }

        System.out.println();
        SelfExtract.wordWrappedOut(SelfExtract.format("licenseOptionDescription"));
        System.out.println();

        boolean accept = SelfExtract.getResponse(SelfExtract.format("licensePrompt", new Object[] { "[1]", "[2]" }),
                                                 "1", "2");
        System.out.println();

        return accept;
    }

    /**
     * Overide the productInstallType in the WebSphereApplicationServer.properties
     *
     * @param productInstallType
     */
    public void setProductInstallTypeOveride(String productInstallType) {
        this.productInstallType = productInstallType;
    }

    /**
     * Allow to extract files to an non-empty directory
     *
     * @param allowExistingDirectory
     */
    public void allowNonEmptyInstallDirectory(Boolean allowNonEmptyInstallDirectory) {
        this.allowNonEmptyInstallDirectory = allowNonEmptyInstallDirectory.booleanValue();
    }

    /**
     * Release the jar file and null instance so that it can be deleted
     */

    public String close() {
        if (instance == null) {
            return null;
        }
        try {
            container.close();
            instance = null;
        } catch (IOException e) {
            return e.getMessage();
        }
        return null;
    }
}