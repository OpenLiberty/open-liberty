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
import java.io.FileOutputStream;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.featureUtility.props.FeatureUtilityProperties;
import com.ibm.ws.install.internal.InstallKernelMap;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.internal.LicenseUpgradeUtility;
import com.ibm.ws.install.internal.MavenRepository;
import com.ibm.ws.install.internal.ProgressBar;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.repository.exceptions.RepositoryException;
//import com.sun.org.apache.xpath.internal.operations.Bool;

/**
 *
 */
public class FeatureUtility {

    private final InstallKernelMap map;
    private File fromDir;
    private final File esaFile;
    private final Boolean noCache;
    private Boolean isDownload;
    private Boolean isBasicInit;
    private final Boolean licenseAccepted;
    private final List<String> featuresToInstall;
    private final List<String> jsons;
    private static String openLibertyVersion;
    private final Logger logger;
    private ProgressBar progressBar;

    private final static String OPEN_LIBERTY_PRODUCT_ID = "io.openliberty";
    private final static String WEBSPHERE_LIBERTY_GROUP_ID = "com.ibm.websphere.appserver.features";


    private FeatureUtility(FeatureUtilityBuilder builder) throws IOException, InstallException {
        this.logger = InstallLogUtils.getInstallLogger();
        this.progressBar = ProgressBar.getInstance();

        this.openLibertyVersion = getLibertyVersion();

        this.fromDir = builder.fromDir; //this can be overwritten by the env prop
        // this.featuresToInstall = new ArrayList<>(builder.featuresToInstall);
        List<String> rawFeatures = new ArrayList<>(builder.featuresToInstall);
        Map<String, Set<String>> jsonsAndFeatures = getJsonsAndFeatures(rawFeatures);

        this.featuresToInstall = new ArrayList<>(jsonsAndFeatures.get("features"));
        Set<String> jsonsRequired = jsonsAndFeatures.get("jsons");
        jsonsRequired.addAll(Arrays.asList("io.openliberty.features", "com.ibm.websphere.appserver.features"));
        

        this.esaFile = builder.esaFile;
        this.noCache = builder.noCache;
        this.licenseAccepted = builder.licenseAccepted;


        map = new InstallKernelMap();
        info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_INITIALIZING"));
        map.put("req.ol.json.coord", "io.openliberty.features");
        Map<String, Object> envMap = (Map<String, Object>) map.get("environment.variable.map");
        
        if (envMap == null) {
        	throw new InstallException((String) map.get("action.error.message"));
        }

        fine("Environment variables: ");
        Set<String> envMapKeys = envMap.keySet();
        for (String key: envMapKeys) {
        	if (key.equals("FEATURE_REPO_PASSWORD")) {
        		fine("FEATURE_REPO_PASSWORD: *********");
        	} else if (key.equals("FEATURE_LOCAL_REPO") && envMap.get("FEATURE_LOCAL_REPO") != null) {
        		fine(key +": " + envMap.get(key));
        		File local_repo = new File((String) envMap.get("FEATURE_LOCAL_REPO"));
        		this.fromDir = local_repo;
        	}else {
        		fine(key +": " + (envMap.get(key)));
        	}
        }
        overrideEnvMapWithProperties();

        if (noCache != null && noCache) {
            fine("Features installed from the remote repository will not be cached locally");
        }
        map.put("cleanup.needed", noCache);
        this.jsons = (List<String>) builder.jsons; //TODO remove?
        
        //fine("these are the jsons group ids: " + this.jsons);
        //List<String> rawFeatures = new ArrayList<>(builder.featuresToInstall);
        //Map<String, Set<String>> jsonsAndFeatures = getJsonsAndFeatures(rawFeatures);
        //fine("jsonsandfeatures: " + jsonsAndFeatures.toString());
        //this.featuresToInstall = new ArrayList<>(jsonsAndFeatures.get("features"));
        //Set<String> jsonsRequired = jsonsAndFeatures.get("jsons");
        //log all the env props we find or don't find to debug
        //if (this.jsons != null) {
        //	Set<String> additionaljsons = new HashSet<String>(this.jsons);
        //    jsonsRequired.addAll(additionaljsons);
        //}
        
        List<File> jsonPaths = getJsonFiles(fromDir, jsonsRequired);
        
        updateProgress(progressBar.getMethodIncrement("fetchJsons"));
        fine("Finished finding jsons");

        initializeMap(jsonPaths);
        updateProgress(progressBar.getMethodIncrement("initializeMap"));
        fine("Initialized install kernel map");
    }

    /**
     * Initialize the Install kernel map.
     *
     * @param jsonPaths
     * @throws IOException
     */
    @SuppressWarnings("restriction")
	private void initializeMap(List<File> jsonPaths) throws IOException {
        map.put("is.feature.utility", true);
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

        map.put("license.accept", licenseAccepted);
        map.get("install.kernel.init.code");

    }


    /**
     * Override the environment variables with any properties we can find
     */
    private void overrideEnvMapWithProperties(){
        if(!FeatureUtilityProperties.didLoadProperties()){
            logger.fine("No featureUtility.properties detected.");
            return;
        }
        Map<String, Object> overrideMap = new HashMap<>();
        logger.fine("Overriding the environment variables using featureUtility.properties");

        // override proxy settings
        String host = FeatureUtilityProperties.getProxyHost();
        String port = FeatureUtilityProperties.getProxyPort();
        String username = FeatureUtilityProperties.getProxyUser();
        String password = FeatureUtilityProperties.getProxyPassword();

        if(FeatureUtilityProperties.canConstructHttpProxy()){
            String http_proxy = "http://" + host + ":" + "port";
            overrideMap.put("http_proxy", http_proxy);
        }
        if(FeatureUtilityProperties.canConstructHttpsProxy()) {
            String https_proxy = "https://" + username + ":" + password + "@" + host + ":" + port;
            overrideMap.put("http_proxy", https_proxy);
        }

        // override the local feature repo
        if(FeatureUtilityProperties.getFeatureLocalRepo() != null){
            overrideMap.put("FEATURE_LOCAL_REPO", FeatureUtilityProperties.getFeatureLocalRepo());
            this.fromDir = new File(FeatureUtilityProperties.getFeatureLocalRepo());
        }

        // override maven repositories
        if(!FeatureUtilityProperties.isUsingDefaultRepo()){
            overrideMap.put("FEATURE_UTILITY_MAVEN_REPOSITORIES", FeatureUtilityProperties.getMirrorRepositories());
        }

        map.put("override.environment.variables", overrideMap);
    }

    /**
     * Return a hashmap that divides the group id's and artifact id's from a list of features.
     * In the hashmap, the "jsons" key refers to the group ids and the "features" key refers to the 
     * artifact ids.
     * @param featureNames a list of feature shortnames or maven coordinates
     * @return hashmap with group ids and artifact ids seperated
     * @throws IOException
     * @throws InstallException
     */
    private Map<String, Set<String>> getJsonsAndFeatures(List<String> featureNames)
                    throws IOException, InstallException {
        Map<String, Set<String>> jsonsAndFeatures = new HashMap<>();

        Set<String> jsonsRequired = new HashSet<>();
        Set<String> featuresRequired = new HashSet<>();

        String openLibertyVersion = getLibertyVersion();
        String groupId, artifactId, version, packaging = null;
        for (String feature : featureNames) {
            String[] mavenCoords = feature.split(":");
            switch(mavenCoords.length){
                case 1: // artifactId
                    groupId = "io.openliberty.features";
                    artifactId = mavenCoords[0];
                    version = openLibertyVersion;
                    packaging = "esa";
                    break;
                case 2: // groupId:artifactId
                    groupId = mavenCoords[0];
                    artifactId = mavenCoords[1];
                    version = openLibertyVersion;
                    packaging = "esa";
                    break;
                case 3: // groupId:artifactId:version
                    groupId = mavenCoords[0];
                    artifactId = mavenCoords[1];
                    version = mavenCoords[2];
                    packaging = "esa";
                    break;
                case 4: // groupId:artifactId:version:packaging
                    groupId = mavenCoords[0];
                    artifactId = mavenCoords[1];
                    version = mavenCoords[2];
                    packaging = mavenCoords[3];
                    break;
                default:
                    throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_MAVEN_COORDINATE_INVALID", feature));
            }
            verifyMavenCoordinate(feature, groupId, artifactId, version, packaging);
            jsonsRequired.add(groupId);
            featuresRequired.add(artifactId);
        }
        jsonsRequired.add(WEBSPHERE_LIBERTY_GROUP_ID);
        jsonsAndFeatures.put("jsons", jsonsRequired);
        jsonsAndFeatures.put("features", featuresRequired);

        return jsonsAndFeatures;

    }

    private void verifyMavenCoordinate(String feature, String groupId, String artifactId, String version, String packaging) throws IOException, InstallException {
        // check for any empty parameters
        if(groupId.isEmpty() || artifactId.isEmpty() || version.isEmpty() || packaging.isEmpty()){
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_MAVEN_COORDINATE_INVALID", feature));
        }

        String openLibertyVersion = getLibertyVersion();
        if (!version.equals(openLibertyVersion)) {
            throw new InstallException(
                            Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_MAVEN_COORDINATE_WRONG_VERSION", feature, openLibertyVersion));
        }
        if(!"esa".equals(packaging)){
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_MAVEN_COORDINATE_WRONG_PACKAGING", feature));
        }
//        // block closed liberty features
//        if("com.ibm.websphere.appserver.features".equals(groupId)){
//            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_FAILED_TO_RESOLVE_FEATURES_FOR_OPEN_LIBERTY", feature));
//        }

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
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_COULD_NOT_DETERMINE_RUNTIME_PROPERTIES_FILE", propertiesFile.getAbsolutePath()));

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
        if (fromDir != null) {
        	map.put("from.repo", fromDir.toString());
        }
        Collection<String> resolvedFeatures = (Collection<String>) map.get("action.result");
        // fine("resolved features: " + resolvedFeatures);
        checkResolvedFeatures(resolvedFeatures);
        boolean upgraded = (boolean) map.get("upgrade.complete");
        List<String> causedUpgrade = (List<String>) map.get("caused.upgrade");
        if (upgraded) {
        	LicenseUpgradeUtility luu = new LicenseUpgradeUtility.LicenseUpgradeUtilityBuilder().setFeatures(featuresToInstall).setAcceptLicense(licenseAccepted).build();
        	boolean isLicenseAccepted = false;
        	try {
            	isLicenseAccepted = luu.handleLicenses(featureFormat(causedUpgrade));
            } catch (InstallException e) {
            	map.get("cleanup.upgrade"); //cleans up the files we put down during upgrade
            	throw e;
            }
            if (!isLicenseAccepted) {
                map.get("cleanup.upgrade"); //cleans up the files we put down during upgrade
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_LICENSES_NOT_ACCEPTED"));
            } else {
            	luu.handleOLLicense();
            }
        	
        }
        updateProgress(progressBar.getMethodIncrement("resolvedFeatures"));

        info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_PREPARING_ASSETS"));
        Collection<File> artifacts = fromDir != null ? downloadFeaturesFrom(resolvedFeatures, fromDir) : downloadFeatureEsas((List<String>) resolvedFeatures);
        updateProgress(progressBar.getMethodIncrement("downloadArtifacts")); // expect this to be 0 after download all features

        info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_STARTING_INSTALL"));
        Collection<String> actionReturnResult = new ArrayList<String>();
        List<String> currentReturnResult;
        try {
            double increment = ((progressBar.getMethodIncrement("installFeatures")) / (artifacts.size()));
            for (File esaFile : artifacts) {
                fine(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_INSTALLING",
                                                                    extractFeature(esaFile.getName())));
                map.put("license.accept", true);
                map.put("action.install", esaFile);
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
                    } else {
                        //update progress
                        updateProgress(increment);
                        progressBar.manuallyUpdate();
                    }
                }
            }
            info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("TOOL_FEATURES_INSTALLATION_COMPLETED"));
        } finally {
            cleanUp();
        }
    }

    private String featureFormat(List<String> causedUpgrade) {
		if (causedUpgrade.size() == 1) {
			return causedUpgrade.get(0);
		}
		if (causedUpgrade.size() == 2) {
			return causedUpgrade.get(0) + " and " + causedUpgrade.get(1);
		}
		if (causedUpgrade.size() > 2) {
			String result = "";
			for (String str: causedUpgrade) {
				if (causedUpgrade.indexOf(str) == 0) {
					result += causedUpgrade.get(0);
				} else if (causedUpgrade.indexOf(str) < causedUpgrade.size() - 1) {
					result += ", " + causedUpgrade.get(causedUpgrade.indexOf(str));
				} else {
					result += ", and " + causedUpgrade.get(causedUpgrade.indexOf(str));
				}
			}
			return result;
		}
		return null;
	}

	/**
     * Check for any errors with the list of resolved features
     *
     * @param resolvedFeatures list of resolved features returned by the resolver
     * @throws InstallException
     */
    private void checkResolvedFeatures(Collection<String> resolvedFeatures) throws InstallException {
        if (resolvedFeatures == null) {
            throw new InstallException((String) map.get("action.error.message"));
        } else if (resolvedFeatures.isEmpty()) {
            String exceptionMessage = (String) map.get("action.error.message");
            if (exceptionMessage == null) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ALREADY_INSTALLED",
                                                                                          map.get("features.to.resolve")));
            } else if (exceptionMessage.contains("CWWKF1250I")) {
                throw new InstallException(exceptionMessage);

            } else {
                throw new InstallException(exceptionMessage);
            }
        }
    }

    private List<File> downloadFeaturesFrom(Collection<String> resolvedFeatures, File fromDir) throws InstallException {
        map.put("from.repo", fromDir.toString());
        return downloadFeatureEsas(resolvedFeatures);
    }

    private List<File> downloadFeatureEsas(Collection<String> resolvedFeatures) throws InstallException {
        map.put("download.artifact.list", resolvedFeatures);
        boolean singleArtifactInstall = false;
        map.put("download.individual.artifact", singleArtifactInstall);

        List<File> result = (List<File>) map.get("download.result");
        if (map.get("action.error.message") != null) {
            fine("action.exception.stacktrace: " + map.get("action.error.stacktrace"));
            String exceptionMessage = (String) map.get("action.error.message");
            throw new InstallException(exceptionMessage);
        }
        return result;
    }

    public List<String> resolveFeatures(boolean isShortNames) throws InstallException {
        map.put("download.location", fromDir.toString());

        List<String> shortNames = new ArrayList<String>();
        if (!isShortNames) {
            info("Preparing assets for installation. This process might take several minutes to complete.");
            info("Resolving features...");
            for (String mavenCoord: featuresToInstall) {
                shortNames.add(mavenCoord.split(":")[1]);
            }
            featuresToInstall.clear();
            featuresToInstall.addAll(shortNames);
        }
        if (featuresToInstall != null) {
            map.put("features.to.resolve", featuresToInstall);
        }
        List<String> resolvedFeatures = (List<String>) map.get("action.result");
        checkResolvedFeatures(resolvedFeatures);
        if (!isShortNames) {
            updateProgress(progressBar.getMethodIncrement("resolveArtifact"));
            info("Features resolved.");
        }
        return resolvedFeatures;
    }

    public void downloadFeatures(List<String> resolvedFeatures) throws InstallException {
        info("Starting Download...");
        List<File> downloadedEsas = downloadFeaturesFrom(resolvedFeatures, fromDir);
        info("\n");
        info("All assets were successfully downloaded.");
    }

    /**
     * Extracts the feature name and version from an ESA filepath. Example:
     * extractFeature(appSecurity-3.0-19.0.0.8.esa) returns appSecurity-3.0
     *
     *
     *
     * @param filename
     * @return
     */
    private String extractFeature(String filename) {
        String[] split = filename.split("-");

        return split[0] + "-" + split[1];

    }

    private List<File> getJsonFiles(File fromDir, Set<String> jsonsRequired) throws InstallException {
        if(jsonsRequired.isEmpty()) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_FEATURES_LIST_INVALID")); //
        }
        List<File> jsonFiles = new ArrayList<>();
        if (fromDir != null) {
            map.put("download.location", fromDir.toString());
        }

        fine("JSONs required: " + jsonsRequired.toString());
        jsonFiles.addAll(map.getLocalJsonFiles(jsonsRequired));
        fine("Found the following jsons locally: " + jsonFiles);
        if (jsonFiles.isEmpty() || jsonFiles.size() != jsonsRequired.size()) {
            fine("Could not find all json files from local directories, now downloading from Maven..");
            jsonFiles.addAll(map.getJsonsFromMavenCentral(jsonsRequired));
            if (map.get("action.error.message") != null) {
                // error with installation
                fine("action.exception.stacktrace: " + map.get("action.error.stacktrace"));
                String exceptionMessage = (String) map.get("action.error.message");
                throw new InstallException(exceptionMessage);
            }
        }
        return jsonFiles;
    }

    /**
     * Clean up the temp directory
     *
     * @throws IOException
     */
    private void cleanUp() throws IOException {
        String tempStr = (String) map.get("cleanup.temp.location");
        Boolean cleanupNeeded = (Boolean) map.get("cleanup.needed");
        boolean deleted = true;

        if (cleanupNeeded != null && cleanupNeeded) { //change this to a map.get("cleanup.needed")
            File temp = new File(tempStr);
            fine("Cleaning directory: " +tempStr);
            deleted = deleteFolder(temp);
        }
        updateProgress(progressBar.getMethodIncrement("cleanUp"));
        progressBar.manuallyUpdate();
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

    private File getM2Cache() { // check for maven_home specified mirror stuff
        // File m2Folder = getM2Path().toFile();

        return Paths.get(System.getProperty("user.home"), ".m2", "repository", "").toFile();

    }

    private void updateProgress(double increment) {
        progressBar.updateProgress(increment);

    }

    // log message types
    private void info(String msg) {
//        if (isWindows) {
//            logger.info(msg);
//        } else {
//            progressBar.clearProgress(); // Erase line content
            logger.info(msg);
//            progressBar.display();
//        }

    }

    private void fine(String msg) {
        logger.fine(msg);
    }

    private void severe(String msg) {
        logger.severe(msg);
    }

    public static class FeatureUtilityBuilder {
        File fromDir;
        Collection<String> featuresToInstall;
        Collection<String> jsons;
        File esaFile;
        boolean noCache;
        boolean licenseAccepted;

        public FeatureUtilityBuilder setFromDir(String fromDir) {
            this.fromDir = fromDir != null ? new File(fromDir) : null;
            return this;
        }

        public FeatureUtilityBuilder setEsaFile(File esaFile) {
            this.esaFile = esaFile;
            return this;
        }
        
        public FeatureUtilityBuilder setNoCache(Boolean noCache) {
            this.noCache = noCache;
            return this;
        }
        
        public FeatureUtilityBuilder setlicenseAccepted(Boolean licenseAccepted) {
            this.licenseAccepted = licenseAccepted;
            return this;
        }

        public FeatureUtilityBuilder setFeaturesToInstall(Collection<String> featuresToInstall) {
            this.featuresToInstall = featuresToInstall;
            return this;
        }
        
        public FeatureUtilityBuilder setJsons(Collection<String> jsons) {
            this.jsons = jsons;
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

    public List<String> getMavenCoords(List<String> artifactShortNames) {
        List<String> result = new ArrayList<String>();
        for (String shortName: artifactShortNames) {
            result.add(OPEN_LIBERTY_PRODUCT_ID + ".feature:" + shortName + ":" + openLibertyVersion);
        }
        return result;
    }

	public void generateJson(String targetJsonFile, List<String> jsonFiles) throws IOException, RepositoryException, InstallException {
		Path targetDir = Files.createTempDirectory("generatedJson");
		Map<String, String> shortNameMap = new HashMap<String, String>();
		List<File> jsons = new ArrayList<File>();
		for (String json: jsonFiles) {
			jsons.add(new File(json));
		}
		map.put("individual.esas", jsons);
		map.generateJson(targetDir, shortNameMap);
		fine("targetDir: " + targetDir.toString());
		File tempFile = new File(targetDir.toString() + "/SingleJson.json");
		fine("targetJsonFile: " + targetJsonFile.toString());
		File targetFile = new File(targetJsonFile);
		FileInputStream instream = new FileInputStream(tempFile);
		FileOutputStream outstream = new FileOutputStream(targetFile);
		byte[] buffer = new byte[1024];
		int length;
		while ((length = instream.read(buffer)) > 0){
	    	outstream.write(buffer, 0, length);
	    }
		instream.close();
	    outstream.close();
	}
    

}
