/*******************************************************************************n * Copyright (c) 2019 IBM Corporation and others.n * All rights reserved. This program and the accompanying materialsn * are made available under the terms of the Eclipse Public License v1.0n * which accompanies this distribution, and is available atn * http://www.eclipse.org/legal/epl-v10.htmln *n * Contributors:n *     IBM Corporation - initial API and implementationn *******************************************************************************/
package com.ibm.ws.install.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallKernelMap;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.kernel.boot.cmdline.Utils;

/**
 *
 */
public class MavenDirectoryInstall {

    private final InstallKernelMap map;
    private final File fromDir;
    private final File esaFile;
    private final String toExtension;
    private final List<String> featuresToInstall;
    private final String openLibertyVersion;
    private final Logger logger;
    private boolean tempCleanupRequired;

    private final String TEMP_DIRECTORY = Utils.getInstallDir().getAbsolutePath() + File.separatorChar + "tmp"
                                          + File.separatorChar;
    private final String OPEN_LIBERTY_PRODUCT_ID = "io.openliberty";

    public MavenDirectoryInstall(MavenDirectoryInstallBuilder builder) throws IOException, InstallException {
        this.logger = InstallLogUtils.getInstallLogger();
        this.openLibertyVersion = getLibertyVersion();

        this.fromDir = builder.fromDir;
        this.toExtension = builder.toExtension;
        this.featuresToInstall = new ArrayList<>(builder.featuresToInstall);
        this.esaFile = builder.esaFile;

        map = new InstallKernelMap();

        List<File> jsonPaths = (fromDir != null && fromDir.exists()) ? getJsons(fromDir) : getJsonsFromMavenCentral();
        initializeMap(jsonPaths);
    }

//    public MavenDirectoryInstall(Collection<String> featuresToInstall) throws IOException, InstallException {
//        this.logger = InstallLogUtils.getInstallLogger();
//        this.openLibertyVersion = getLibertyVersion();
//        this.tempCleanupRequired = false;
//
//        logger.log(Level.FINE, "The features to install from maven central are:" + featuresToInstall);
//
//        map = new InstallKernelMap();
//        List<File> jsonPaths = getJsonsFromMavenCentral();
//        initializeMap(featuresToInstall, jsonPaths);
//    }
//
//    /**
//     * Initialize a map based install kernel with a local maven directory
//     *
//     * @param featuresToInstall
//     * @param fromDir
//     * @throws IOException
//     * @throws InstallException
//     */
//    public MavenDirectoryInstall(Collection<String> featuresToInstall, File fromDir) throws IOException, InstallException {
//
//        this.fromDir = fromDir;
//        this.logger = InstallLogUtils.getInstallLogger();
//        this.openLibertyVersion = getLibertyVersion();
//        this.tempCleanupRequired = false;
//
//        logger.log(Level.FINE, "The features to install from local maven repository are:" + featuresToInstall);
//        logger.log(Level.FINE, "The local maven repository is: " + fromDir.getAbsolutePath());
//
//        List<File> jsonPaths = getJsons(fromDir);
//        map = new InstallKernelMap();
//        initializeMap(jsonPaths);
//
//    }
//
//    public MavenDirectoryInstall(Collection<String> featuresToInstall, File fromDir, String toExtension) throws IOException, InstallException {
//        this(featuresToInstall, fromDir);
//        this.toExtension = toExtension;
//    }

    /**
     * Initialize the Install kernel map.
     *
     * @param featuresToInstall
     * @throws IOException
     */
    private void initializeMap(Collection<File> jsonPaths) throws IOException {
        map.put("runtime.install.dir", Utils.getInstallDir());
        map.put("target.user.directory", new File(Utils.getInstallDir(), "tmp"));
        map.put("install.local.esa", true);
        map.put("single.json.file", jsonPaths);

        if (featuresToInstall != null) {
            map.put("features.to.resolve", featuresToInstall);

        }

        if (esaFile != null) {
            map.put("individual.esas", Arrays.asList(esaFile));
            map.put("install.individual.esas", true);
        }

        map.put("license.accept", true); // TODO: discuss later
        map.get("install.kernel.init.code");

    }

    /**
     * Get the open liberty runtime version.
     *
     * @throws IOException
     * @throws InstallException
     *
     */
    private String getLibertyVersion() throws IOException, InstallException {
        File dir = new File(Utils.getInstallDir(), "lib/versions");
        File[] propertiesFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".properties");
            }
        });

        if (propertiesFiles == null) {
            throw new IOException("Could not find properties files.");
        }
        String openLibertyVersion = null;

        for (File propertiesFile : propertiesFiles) {
            Properties properties = new Properties();
            try (InputStream input = new FileInputStream(propertiesFile)) {
                properties.load(input);
                String productId = properties.getProperty("com.ibm.websphere.productId");
                String productVersion = properties.getProperty("com.ibm.websphere.productVersion");

                if (productId.equals(OPEN_LIBERTY_PRODUCT_ID)) {
                    openLibertyVersion = productVersion;
                }

            } catch (IOException e) {
                throw new IOException("Could not read the properties file " + propertiesFile.getAbsolutePath());
            }
        }
        if (openLibertyVersion == null) {
            // openliberty.properties file is missing
            throw new InstallException("Could not determine the open liberty runtime version.");
        } else {
            logger.log(Level.FINE, "The Open Liberty runtime version is " + openLibertyVersion);
        }
        return openLibertyVersion;

    }

    /**
     * Resolves and installs the features
     *
     * @throws InstallException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void installFeatures() throws InstallException, IOException {
        Collection<String> resolvedFeatures = (Collection<String>) map.get("action.result");
        logger.log(Level.FINE, "Resolved features: " + resolvedFeatures);
        if (resolvedFeatures == null) {
            throw new InstallException((String) map.get("action.error.message"));
        } else if (resolvedFeatures.isEmpty()) {
            logger.log(Level.FINE, "The list of resolved features is empty.");
            String exceptionMessage = (String) map.get("action.error.message");
            if (exceptionMessage == null) {
                logger.log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ALREADY_INSTALLED",
                                                                                      map.get("features.to.resolve")));
                return;
            } else if (exceptionMessage.contains("CWWKF1250I")) {
                logger.log(Level.INFO, exceptionMessage);
                return;
            } else {
                throw new InstallException(exceptionMessage);
            }
        }

        Collection<File> artifacts = fromDir != null ? findEsas(resolvedFeatures, fromDir) : downloadFeatureEsas((List<String>) resolvedFeatures);

        Collection<String> actionReturnResult = new ArrayList<String>();
        Collection<String> currentReturnResult;

        logger.log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_STARTING_INSTALL"));
        try {
            for (File esaFile : artifacts) {

                logger.log(Level.FINE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_INSTALLING",
                                                                                      extractFeature(esaFile.getName())));
                map.put("license.accept", true);
                map.put("action.install", esaFile);
                if (toExtension != null) {
                    map.put("to.extension", toExtension);
                    logger.log(Level.FINE, "Installing to extension: " + toExtension);
                }

                Integer ac = (Integer) map.get("action.result");
                logger.log(Level.FINE, "action.result:" + ac);
                logger.log(Level.FINE, "action.error.message:" + map.get("action.error.message"));

                if (map.get("action.error.message") != null) {
                    // error with installation
                    logger.log(Level.FINE, "action.exception.stacktrace: " + map.get("action.error.stacktrace"));
                    String exceptionMessage = (String) map.get("action.error.message");
                    throw new InstallException(exceptionMessage);
                } else if ((currentReturnResult = (Collection<String>) map.get("action.install.result")) != null) {
                    // installation was successful
                    if (!currentReturnResult.isEmpty()) {
                        actionReturnResult.addAll((Collection<String>) map.get("action.install.result"));
                        logger.log(Level.INFO,
                                   (Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_INSTALLED_FEATURE", String.join(", ", currentReturnResult)).replace("CWWKF1304I: ", ""))); // TODO: come up with new message for
                                                                                                                                                                                                          // successfully
                                                                                                                                                                                                          // installed feature

                    }
                }
            }
            logger.log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("TOOL_INSTALLATION_COMPLETED"));
        } finally {

            cleanUp();
        }
    }

    private Collection<File> findEsas(Collection<String> resolvedFeatures, File rootDir) throws InstallException {
        Collection<File> foundEsas = new HashSet<>();
        Collection<String> featuresClone = new ArrayList<>(resolvedFeatures);

        for (String feature : resolvedFeatures) {
            logger.log(Level.FINE, "Processing feature: " + feature);
            String mavenCoordinate = feature.split(":")[0];
            String featureName = feature.split(":")[1];

            File groupDir = new File(rootDir, mavenCoordinate.replace(".", "/"));
            if (!groupDir.exists()) {
                continue;
            }

            Path featurePath = Paths.get(groupDir.getAbsolutePath().toString(), featureName, openLibertyVersion,
                                         featureName + "-" + openLibertyVersion + ".esa");

            logger.log(Level.FINE, "Found feature at path: " + featurePath.toString());
            if (Files.isRegularFile(featurePath)) {
                foundEsas.add(featurePath.toFile());
                featuresClone.remove(feature);
            }

        }
        if (!featuresClone.isEmpty()) {
            logger.log(Level.INFO, "Could not find ESA's in local maven repo for " + resolvedFeatures);
            List<File> downloadedEsas = downloadFeatureEsas((List<String>) resolvedFeatures);

            logger.log(Level.INFO, "Downloaded the following features from maven central:" + downloadedEsas);
            foundEsas.addAll(downloadedEsas);
        }
        return foundEsas;

    }

    /**
     * @throws InstallException
     *
     */
    private List<File> downloadFeatureEsas(List<String> features) throws InstallException {
        map.put("download.artifact.list", features);
        map.put("download.local.dir.location", TEMP_DIRECTORY);
        map.put("download.remote.maven.repo", "http://repo.maven.apache.org/maven2/");
        boolean singleArtifactInstall = false;
        map.put("download.inidividual.artifact", singleArtifactInstall);
        List<File> result = (List<File>) map.get("download.result");

        if (map.get("action.error.message") != null) {
            logger.log(Level.FINE, "action.exception.stacktrace: " + map.get("action.error.stacktrace"));
            String exceptionMessage = (String) map.get("action.error.message");
            throw new InstallException(exceptionMessage);
        }
        this.tempCleanupRequired = true;

        return result;

    }

    /**
     * Extracts the feature name and version from an ESA filepath. Example:
     * extractFeature(appSecurity-3.0-19.0.0.8.esa) returns appSecurity-3.0
     *
     * TODO: extract runtime version
     *
     * @param filename
     * @return
     */
    private String extractFeature(String filename) {
        String[] split = filename.split("-");

        return split[0] + "-" + split[1];

    }

//    /**
//     * Get the single json files from a local maven repo
//     *
//     * @param filepath
//     * @param found
//     */
//    private List<File> getSingleJsonPaths(File filepath) {
//        ArrayList<File> jsonFilesFound = new ArrayList<>();
//        if (filepath == null || !filepath.exists()) {
//            return jsonFilesFound;
//        }
//        File[] list = filepath.listFiles();
//        for (File f : list) {
//            if (f.isDirectory()) {
//                jsonFilesFound.addAll(getSingleJsonPaths(f));
//            } else if (f.isFile()) {
//                if (f.getName().endsWith(".json")) {
//                    jsonFilesFound.add(f);
//                }
//            }
//        }
//
//        return jsonFilesFound;
//
//    }

    private List<File> getJsons(File file) throws InstallException {
        List<File> jsonFiles = new ArrayList<File>();

        Stack<File> stack = new Stack<>();
        stack.push(file);

        while (!stack.empty()) {
            // check if we're in directory
            File f = stack.pop();
            if (f.isDirectory()) {
                // determine if we're in the artifact section of the repository
                if (looksLikeArtifactSection(f.listFiles())) {
                    File json = retrieveJsonFileFromArtifact(f);
                    if (json.exists()) {
                        jsonFiles.add(json);
                    } else {
                        for (File current : f.listFiles()) {
                            stack.push(current);
                        }
                    }
                } else {
                    for (File current : f.listFiles()) {
                        stack.push(current);
                    }
                }
            } else if (f.isFile() && f.getName().endsWith(".json")) {
                jsonFiles.add(f);
            }
        }
        logger.log(Level.FINE, "Found the following jsons: " + jsonFiles);
        if (jsonFiles.isEmpty()) {
            throw new InstallException("Could not locate the json file");
        }
        return jsonFiles;

    }

    private File retrieveJsonFileFromArtifact(File artifact) {
        File dir = new File(artifact, "/features/");
        if (!dir.exists() && !dir.isDirectory()) {
            return null;
        }

        Path jsonPath = Paths.get(artifact.getAbsolutePath(), "features", openLibertyVersion,
                                  "features-" + openLibertyVersion + ".json");
        return jsonPath.toFile();

    }

    private boolean looksLikeArtifactSection(File[] files) {
        for (File f : files) {
            if (f.getName().equals("features")) {
                // likely found a feature
                return true;
            }
        }

        return false;

    }

    /**
     * Fetch the open liberty and closed liberty json files from maven central
     *
     * @return
     * @throws InstallException
     */
    private List<File> getJsonsFromMavenCentral() throws InstallException {
        // get open liberty json
        List<File> result = new ArrayList<File>();
        map.put("download.local.dir.location", TEMP_DIRECTORY);
        map.put("download.remote.maven.repo", "http://repo.maven.apache.org/maven2/");
        map.put("download.filetype", "json");
        boolean singleArtifactInstall = true;
        map.put("download.inidividual.artifact", singleArtifactInstall);

        String OLJsonCoord = "io.openliberty.features:features:" + openLibertyVersion;
        String CLJsonCoord = "com.ibm.websphere.appserver.features:features:" + openLibertyVersion;
        map.put("download.artifact.list", OLJsonCoord);
        File OL = (File) map.get("download.result");
        map.put("download.artifact.list", CLJsonCoord);
        File CL = (File) map.get("download.result");
        result.add(OL);
        result.add(CL);
        this.tempCleanupRequired = true;
        if (map.get("action.error.message") != null) {
            logger.log(Level.FINE, "action.exception.stacktrace: " + map.get("action.error.stacktrace"));
            String exceptionMessage = (String) map.get("action.error.message");
            throw new InstallException(exceptionMessage);
        }
        logger.log(Level.FINE,
                   "Downloaded the following jsons from maven central:" + result);
        return result;

    }

    /**
     * Clean up the temp directory
     *
     * @throws IOException
     */
    private void cleanUp() throws IOException {
        File temp = new File(TEMP_DIRECTORY);
        boolean deleted = true;

        if (tempCleanupRequired) {
            deleted = deleteFolder(temp);
        }
        if (deleted) {
            logger.log(Level.FINE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_CLEANUP_SUCCESS"));
        } else {
            logger.log(Level.FINE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_CANNOT_CLOSE_OBJECT"));
        }

    }

    /**
     * Delete the folder and all its contents.
     *
     * @param file file or folder to be deleted
     * @return true if the folder still exists at the end of this operation
     * @throws IOException
     */
    private boolean deleteFolder(File file) throws IOException {
        Path path = file.toPath();
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        return !file.exists();
    }

    public static class MavenDirectoryInstallBuilder {
        File fromDir;
        String toExtension;
        Collection<String> featuresToInstall;
        File esaFile;

        public MavenDirectoryInstallBuilder setFromDir(String fromDir) {
            this.fromDir = fromDir != null ? new File(fromDir) : null;
            return this;
        }

        public MavenDirectoryInstallBuilder setToExtension(String toExtension) {
            this.toExtension = toExtension;
            return this;
        }

        public MavenDirectoryInstallBuilder setEsaFile(File esaFile) {
            this.esaFile = esaFile;
            return this;
        }

        public MavenDirectoryInstallBuilder setFeaturesToInstall(Collection<String> featuresToInstall) {
            this.featuresToInstall = featuresToInstall;
            return this;
        }

        public MavenDirectoryInstall build() throws IOException, InstallException {
            return new MavenDirectoryInstall(this);
        }

    }

}
