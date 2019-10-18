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
package com.ibm.ws.install.featureUtility;

import java.io.File;
import java.io.FileInputStream;
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
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallKernelMap;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.internal.ProgressBar;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.kernel.boot.cmdline.Utils;

/**
 *
 */
public class FeatureUtility {

    private final InstallKernelMap map;
    private final File fromDir;
    private final File esaFile;
    private Boolean isDownload;
    private final String toExtension;
    private final List<String> featuresToInstall;
    private static String openLibertyVersion;
    private final Logger logger;
    private ProgressBar progressBar;
    
    private final static String OPEN_LIBERTY_PRODUCT_ID = "io.openliberty"; //TODO

    private FeatureUtility(FeatureUtilityBuilder builder) throws IOException, InstallException {
        this.logger = InstallLogUtils.getInstallLogger();
        this.progressBar = ProgressBar.getInstance();

        info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_INITIALIZING"));

        this.openLibertyVersion = getLibertyVersion();

        
        this.fromDir = builder.fromDir;
        this.toExtension = builder.toExtension;
        this.featuresToInstall = new ArrayList<>(builder.featuresToInstall);
        this.esaFile = builder.esaFile;
        this.isDownload = builder.isDownload;

        map = new InstallKernelMap();
        
        if (isDownload == null || !isDownload) {
        	List<File> jsonPaths = (fromDir != null && fromDir.exists()) ? getJsons(fromDir) : getJsonsFromMavenCentral();
            updateProgress(progressBar.getMethodIncrement("fetchJsons"));
            fine("Finished finding jsons");

            initializeMap(jsonPaths);
            updateProgress(progressBar.getMethodIncrement("initializeMap"));
            fine("Initialized install kernel map");
        } else {
        	initializeMap();
        }
    }
    
    /**
     * Initialize the Install kernel map.
     *
     * @throws IOException
     */
    private void initializeMap() throws IOException {
        map.put("runtime.install.dir", Utils.getInstallDir());
        map.put("target.user.directory", new File(Utils.getInstallDir(), "tmp"));
        map.put("license.accept", true); // TODO: discuss later
        map.get("install.kernel.init.code");

    }

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
        if (this.openLibertyVersion != null) {
            return this.openLibertyVersion;
        }
        File propertiesFile = new File(Utils.getInstallDir(), "lib/versions/openliberty.properties");
        String openLibertyVersion = null;
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(propertiesFile)) {
            properties.load(input);
            String productId = properties.getProperty("com.ibm.websphere.productId");
            String productVersion = properties.getProperty("com.ibm.websphere.productVersion");

            if (productId.equals(OPEN_LIBERTY_PRODUCT_ID)) {
                openLibertyVersion = productVersion;
            }

        }

        if (openLibertyVersion == null) {
            // openliberty.properties file is missing or invalidly formatted
            throw new InstallException("Could not determine the open liberty runtime version.");

        } else {
            fine("The Open Liberty runtime version is " + openLibertyVersion);
        }
        this.openLibertyVersion = openLibertyVersion;
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
        info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_RESOLVING"));
        Collection<String> resolvedFeatures = (Collection<String>) map.get("action.result");
        if (resolvedFeatures == null) {
            throw new InstallException((String) map.get("action.error.message"));
        } else if (resolvedFeatures.isEmpty()) {
            fine("The list of resolved features is empty.");
            String exceptionMessage = (String) map.get("action.error.message");
            if (exceptionMessage == null) {
                info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ALREADY_INSTALLED",
                                                                                      map.get("features.to.resolve")));
                return;
            } else if (exceptionMessage.contains("CWWKF1250I")) {
                info(exceptionMessage);
                return;
            } else {
                throw new InstallException(exceptionMessage);
            }
        }
        updateProgress(progressBar.getMethodIncrement("resolvedFeatures"));

        info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_PREPARING_ASSETS"));
        Collection<File> artifacts = fromDir != null ? downloadFeaturesFrom(resolvedFeatures, fromDir) : downloadFeatureEsas((List<String>) resolvedFeatures);
        updateProgress(progressBar.getMethodIncrement("fetchArtifacts"));

        info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_STARTING_INSTALL"));
        Collection<String> actionReturnResult = new ArrayList<String>();
        List<String> currentReturnResult;
        try {
            double increment = ((double) (progressBar.getMethodIncrement("installFeatures")) / (artifacts.size()));
            for (File esaFile : artifacts) {
                fine(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_INSTALLING",
                                                                                      extractFeature(esaFile.getName())));
                map.put("license.accept", true);
                map.put("action.install", esaFile);
                if (toExtension != null) {
                    map.put("to.extension", toExtension);
//                    fine("Installing to extension: " + toExtension);
                }

                Integer ac = (Integer) map.get("action.result");
//                fine("action.result:" + ac);
//                fine("action.error.message:" + map.get("action.error.message"));

                if (map.get("action.error.message") != null) {
                    // error with installation
                    fine("action.exception.stacktrace: " + map.get("action.error.stacktrace"));
                    String exceptionMessage = (String) map.get("action.error.message");
                    throw new InstallException(exceptionMessage);
                } else if ((currentReturnResult = (List<String>) map.get("action.install.result")) != null) {
                    // installation was successful
                    if (!currentReturnResult.isEmpty()) {
                        actionReturnResult.addAll((Collection<String>) map.get("action.install.result"));
                        updateProgress(increment);
                        info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_INSTALLED_FEATURE",
                                currentReturnResult.get(0)).replace("CWWKF1304I: ", ""));

                    }
                }
            }
            info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("TOOL_FEATURES_INSTALLATION_COMPLETED"));
        } finally {
            cleanUp();
        }
    }

    private List<File> downloadFeaturesFrom(Collection<String> resolvedFeatures, File fromDir) throws InstallException {
    	map.put("from.repo", fromDir.toString());
        return downloadFeatureEsas(resolvedFeatures);
	}
    
    private List<File> downloadFeatureEsas(Collection<String> resolvedFeatures) throws InstallException {
    	map.put("download.artifact.list", resolvedFeatures);
    	boolean singleArtifactInstall = false;
        map.put("download.inidividual.artifact", singleArtifactInstall);
        List<File> result = (List<File>) map.get("download.result");
        if (map.get("action.error.message") != null) {
            fine("action.exception.stacktrace: " + map.get("action.error.stacktrace"));
            String exceptionMessage = (String) map.get("action.error.message");
            throw new InstallException(exceptionMessage);
        }
        return result;
    }
    
    public void downloadFeatures(boolean isShortNames) throws InstallException {
    	map.put("download.location", fromDir.toString());
    	List<String> mavenCoords = new ArrayList<String>();
    	if (isShortNames) {
    		for (String shortName: featuresToInstall) {
    			mavenCoords.add(OPEN_LIBERTY_PRODUCT_ID + ".features:" + shortName + ":" + openLibertyVersion);
    		}
    		featuresToInstall.clear();
    		featuresToInstall.addAll(mavenCoords);
    	}
    	map.put("download.artifact.list", featuresToInstall);
    	boolean singleArtifactInstall = false;
        map.put("download.inidividual.artifact", singleArtifactInstall);
        List<File> result = (List<File>) map.get("download.result");
        if (map.get("action.error.message") != null) {
            fine("action.exception.stacktrace: " + map.get("action.error.stacktrace"));
            String exceptionMessage = (String) map.get("action.error.message");
            throw new InstallException(exceptionMessage);
        }
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
            } else if (f.isFile() && isFeatureJson(f)) {
                jsonFiles.add(f);
            }
        }
        fine("Found the following jsons: " + jsonFiles);
        if (jsonFiles.isEmpty()) {
            // TODO throw exception if user does not allow network connection from system
            // properties, else download from mvn central
            jsonFiles = getJsonsFromMavenCentral();
        }
        return jsonFiles;

    }
    
    private boolean isFeatureJson(File file) {
        return file.exists() && file.getName().equals("features-" + openLibertyVersion + ".json");
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
        if (map.get("action.error.message") != null) {
            fine("action.exception.stacktrace: " + map.get("action.error.stacktrace"));
            String exceptionMessage = (String) map.get("action.error.message");
            throw new InstallException(exceptionMessage);
        }
        fine("Downloaded the following jsons from maven central:" + result);
        return result;

    }

    /**
     * Clean up the temp directory
     *
     * @throws IOException
     */
    private void cleanUp() throws IOException {
    	String tempStr = (String) map.get("cleanup.temp.location");
        boolean deleted = true;

        if (tempStr != null) { //change this to a map.get("cleanup.needed")
        	File temp = new File(tempStr);
            deleted = deleteFolder(temp);
        }
        updateProgress(progressBar.getMethodIncrement("cleanUp"));
        if (deleted) {
            fine(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_CLEANUP_SUCCESS"));
        } else {
            severe(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_CANNOT_CLOSE_OBJECT"));
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
        if (!path.toFile().exists()) {
            return true;
        }
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

    private void updateProgress(double increment) {
        progressBar.updateProgress(increment);

    }

    // log message types
    private void info(String msg) {
        System.out.print("\033[2K"); // Erase line content
        logger.info(msg);
        progressBar.display();
    }

    private void fine(String msg) {
        System.out.print("\033[2K"); // Erase line content
        logger.fine(msg);
        progressBar.display();

    }

    private void severe(String msg) {
        System.out.print("\033[2K"); // Erase line content
        logger.severe(msg);
        progressBar.display();

    }

    public static class FeatureUtilityBuilder {
        File fromDir;
        String toExtension;
        Collection<String> featuresToInstall;
        File esaFile;
        boolean isDownload;

        public FeatureUtilityBuilder setFromDir(String fromDir) {
            this.fromDir = fromDir != null ? new File(fromDir) : null;
            return this;
        }

        public FeatureUtilityBuilder setToExtension(String toExtension) {
            this.toExtension = toExtension;
            return this;
        }

        public FeatureUtilityBuilder setEsaFile(File esaFile) {
            this.esaFile = esaFile;
            return this;
        }

        public FeatureUtilityBuilder setFeaturesToInstall(Collection<String> featuresToInstall) {
            this.featuresToInstall = featuresToInstall;
            return this;
        }
        
        public FeatureUtilityBuilder setIsDownload(boolean isDownload) {
            this.isDownload = isDownload;
            return this;
        }

        public FeatureUtility build() throws IOException, InstallException {
            return new FeatureUtility(this);
        }

    }
    
    public static List<String> getMissingArtifactsFromFolder(List<String> artifacts, String location, boolean isShortName) throws IOException, InstallException{
    	List<String> result = new ArrayList<String>();
    	
    	for (String id: artifacts) {
    		Path featurePath;
    		if (isShortName) {
    			String groupId = OPEN_LIBERTY_PRODUCT_ID + ".features";
    			File groupDir = new File(location, groupId.replace(".", "/")); 
    			if (!groupDir.exists()) {
                	result.add(id);
                    continue;
                }
    			String featureEsa = id + "-" + openLibertyVersion + ".esa";
    			featurePath = Paths.get(groupDir.getAbsolutePath().toString(), id, openLibertyVersion, featureEsa);
    		} else {
    			String groupId = id.split(":")[0];
            	String featureName = id.split(":")[1];
            	File groupDir = new File(location, groupId.replace(".", "/"));
            	if (!groupDir.exists()) {
            		result.add(id);
                	continue;
            	}
            	String featureEsa = featureName + "-" + openLibertyVersion + ".esa";
            	featurePath = Paths.get(groupDir.getAbsolutePath().toString(), featureName, openLibertyVersion, featureEsa);
    		}
            if (!Files.isRegularFile(featurePath)) {
            	result.add(id);
            }
    	}
    	return result;
    }

}
