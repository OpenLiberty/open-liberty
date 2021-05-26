/*******************************************************************************
 * Copyright (c) 2019, 2020, 2021 IBM Corporation and others.
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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallConstants;
import com.ibm.websphere.crypto.InvalidPasswordDecodingException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallKernel;
import com.ibm.ws.install.featureUtility.props.FeatureUtilityProperties;
import com.ibm.ws.install.internal.InstallKernelMap;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.internal.LicenseUpgradeUtility;
import com.ibm.ws.install.internal.MavenRepository;
import com.ibm.ws.install.internal.ProgressBar;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.feature.internal.cmdline.NLS;
import com.ibm.ws.repository.exceptions.RepositoryException;
//import com.sun.org.apache.xpath.internal.operations.Bool;

/**
 *
 */
public class FeatureUtility {

    private final InstallKernelMap map;
    private File fromDir;
    private final List<File> esaFiles;
    private final Boolean noCache;
    private final Boolean licenseAccepted;
    private final List<String> featuresToInstall;
    private final List<String> additionalJsons;
    private static String openLibertyVersion;
    private static String openLibertyEdition;
    private final Logger logger;
    private ProgressBar progressBar;
    private Map<String, String> featureToExt;
    private final static String OPEN_LIBERTY_PRODUCT_ID = "io.openliberty";
    private final static String WEBSPHERE_LIBERTY_GROUP_ID = "com.ibm.websphere.appserver.features";
    private final static String BETA_EDITION = "EARLY_ACCESS";
    private static String to;
    private boolean isInstallServerFeature = false;


    private FeatureUtility(FeatureUtilityBuilder builder) throws IOException, InstallException {
        this.logger = InstallLogUtils.getInstallLogger();
        this.progressBar = ProgressBar.getInstance();

        this.openLibertyVersion = getLibertyVersion();
        
        if (this.openLibertyEdition.equals(BETA_EDITION)) {
            throw new InstallException(
                            Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_BETA_EDITION_NOT_SUPPORTED"));
        }
        this.additionalJsons = builder.additionalJsons;
        
        this.to = builder.to;

        this.fromDir = builder.fromDir; //this can be overwritten by the env prop
        // this.featuresToInstall = new ArrayList<>(builder.featuresToInstall);
        List<String> rawFeatures = new ArrayList<>(builder.featuresToInstall);
        Map<String, Set<String>> jsonsAndFeatures = getJsonsAndFeatures(rawFeatures);

        this.featuresToInstall = new ArrayList<>(jsonsAndFeatures.get("features"));
        Set<String> jsonsList = jsonsAndFeatures.get("jsons");
        Set<String> jsonsRequired = new HashSet<String>();
        for (String groupId: jsonsList) {
        	jsonsRequired.add(String.format("%s:%s:%s", groupId, "features", openLibertyVersion));
        }
        jsonsRequired.addAll(Arrays.asList(String.format("io.openliberty.features:features:%s", openLibertyVersion)));
        

        this.esaFiles = builder.esaFiles;
        this.noCache = builder.noCache;
        this.licenseAccepted = builder.licenseAccepted;
        this.featureToExt = new HashMap<String, String>();


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
        map.put("json.provided", false);
        overrideEnvMapWithProperties();
        
        fine("additional jsons: " + additionalJsons);
        if (additionalJsons != null && !additionalJsons.isEmpty()) {
        	jsonsRequired.addAll(additionalJsons);
        	map.put("json.provided", true);
        }

        boolean isOpenLiberty = (Boolean) map.get("is.open.liberty");
        if (!isOpenLiberty) {
        	jsonsRequired.add(String.format("com.ibm.websphere.appserver.features:features:%s", openLibertyVersion));
        }else { //check if user is trying to install CL feature onto OL runtime without specifying json cord in featureUtility.prop. 
        	for(String s: jsonsRequired) {
        		if(s.contains(WEBSPHERE_LIBERTY_GROUP_ID) && (additionalJsons == null || additionalJsons.isEmpty())) {
        			throw new InstallException("Incorrectly tried to install a websphere liberty feature onto open liberty");
        		}
        	}
        }

        if (noCache != null && noCache) {
            fine("Features installed from the remote repository will not be cached locally");
        }
        map.put("cleanup.needed", noCache);
        
        List<File> jsonPaths = getJsonFiles(fromDir, jsonsRequired);
        
        updateProgress(progressBar.getMethodIncrement("fetchJsons"));
        fine("Finished finding jsons");

        initializeMap(jsonPaths);
        updateProgress(progressBar.getMethodIncrement("initializeMap"));
        fine("Initialized install kernel map");
    }
    
    public void setFeatureToExt(Map<String, String> featureToExt) {
    	this.featureToExt = featureToExt;
    }
    
    public void setIsInstallServerFeature(boolean isInstallServerFeature) {
    	this.isInstallServerFeature = isInstallServerFeature;
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
        map.put("target.user.directory", new File(Utils.getInstallDir(), "usr/tmp"));
        map.put("install.local.esa", true);
        
        map.put("single.json.file", jsonPaths);
        if (featuresToInstall != null) {
            map.put("features.to.resolve", featuresToInstall);

        }
        if (esaFiles != null && !esaFiles.isEmpty()) {
            map.put("individual.esas", esaFiles);
            map.put("install.individual.esas", true);
        }

        map.put("license.accept", licenseAccepted);
        map.get("install.kernel.init.code");

    }


    /**
     * Override the environment variables with any properties we can find
     * @throws InstallException 
     */
    private void overrideEnvMapWithProperties() throws InstallException{
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

        String protocol = null;
		if (host != null && !host.isEmpty()) {
			if (host.toLowerCase().startsWith("https://")) {
				protocol = "https";
			} else {
				protocol = "http";
			}
		}

		if (protocol != null && !protocol.isEmpty()) {
			if (port != null && !port.isEmpty()) {
				overrideMap.put(protocol + ".proxyHost", host);
				overrideMap.put(protocol + ".proxyPort", port);
			} else {
				throw new InstallException(
						Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_PROXY_PORT_MISSING"),
						InstallException.MISSING_CONTENT);
			}

			String decodedPwd = password;
			if (decodedPwd != null && !decodedPwd.isEmpty()) {
				try {
					// Decode encrypted proxy server password
					decodedPwd = PasswordUtil.decode(password);
				} catch (InvalidPasswordDecodingException ipde) {
					decodedPwd = password;
					logger.log(Level.FINE, Messages.INSTALL_KERNEL_MESSAGES
							.getLogMessage("LOG_PASSWORD_NOT_ENCODED_PROXY", host + ":" + port) + InstallUtils.NEWLINE);
				} catch (UnsupportedCryptoAlgorithmException ucae) {
					throw new InstallException(
							Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_PROXY_PWD_CRYPTO_UNSUPPORTED"),
							ucae, InstallException.RUNTIME_EXCEPTION);
				}
			}
			overrideMap.put(protocol + ".proxyUser", username);
			overrideMap.put(protocol + ".proxyPassword", decodedPwd);

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
        
        //get any additional required jsons
        if(FeatureUtilityProperties.bomIdsRequired()) {
        	List<String> boms = FeatureUtilityProperties.getBomIds();
        	for (String bom: boms) {
        		String[] bomSplit = bom.split(":");
        		if (bomSplit.length != 3) {
        			throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_INVALID_FEATURE_BOM_COORDINATE", bom)); 
        		}
        		String groupId = bomSplit[0];
        		String artifactId = "features";
        		String version = bomSplit[2];
        		this.additionalJsons.add(String.format("%s:%s:%s", groupId, artifactId, version));
            	map.put("json.provided", true);
        	}
        	
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
        if (this.openLibertyEdition != null) {
            return this.openLibertyEdition;
        }
        File propertiesFile = new File(Utils.getInstallDir(), "lib/versions/openliberty.properties");
        String openLibertyVersion = null;
        String openLibertyEdition = null;
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(propertiesFile)) {
            properties.load(input);
            String productId = properties.getProperty("com.ibm.websphere.productId");
            String productVersion = properties.getProperty("com.ibm.websphere.productVersion");
            String productEdition = properties.getProperty("com.ibm.websphere.productEdition");
            if (productId.equals(OPEN_LIBERTY_PRODUCT_ID)) {
                openLibertyVersion = productVersion;
                openLibertyEdition = productEdition;
            }

        }

        if (openLibertyVersion == null || openLibertyEdition == null) {
            // openliberty.properties file is missing or invalidly formatted
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_COULD_NOT_DETERMINE_RUNTIME_PROPERTIES_FILE", propertiesFile.getAbsolutePath()));

        }
        this.openLibertyVersion = openLibertyVersion;
        this.openLibertyEdition = openLibertyEdition;
        return openLibertyVersion;
    }

    public Set<String> findFeatures(){
        String query = String.join(" ", featuresToInstall);
        map.put("action.find", query);
        Set<String> features = (Set<String>) map.get("action.result");

        if(features.isEmpty()){
            info(Messages.INSTALL_KERNEL_MESSAGES.getMessage("MSG_NO_FEATURES_FOUND"));
        }
        else {
            // display the features
            for (String feature : features){
                info(feature);
            }
        }
        return features;
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
        map.put("is.install.server.feature", isInstallServerFeature);
        Collection<String> resolvedFeatures = (Collection<String>) map.get("action.result");
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
            	
            	String featureName = extractFeature(esaFile.getName());
                fine(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_INSTALLING", featureName));
                map.put("license.accept", true);
                map.put("action.install", esaFile);
                String ext = featureToExt.get(featureName);
                if (to != null) {
                    map.put("to.extension", to);
                    fine("Installing to extension: " + to);
                }
                if (ext != null && ext != "") {
                	map.put("to.extension", ext);
                	fine("Installing to extension from server.xml: " + ext);
                }
                map.get("action.result");
                
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
                map.put("to.extension", InstallConstants.TO_USER);
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
     * @param upgraded whether or not the features triggered an license upgrade
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
        downloadFeaturesFrom(resolvedFeatures, fromDir);
        info("\n");
        info("All assets were successfully downloaded.");
    }

    /**
     * Extracts the feature name and version from an ESA filepath. Example:
     * extractFeature(appSecurity-3.0-19.0.0.8.esa) returns appSecurity-3.0
     * 
     * @param filename
     * @return
     */
    private String extractFeature(String filename) {
        return filename.replaceFirst("(-\\d\\d\\.\\d\\.\\d\\.\\d\\.esa)", "");
    }

    public List<File> getJsonFiles(File fromDir, Set<String> jsonsRequired) throws InstallException {
        if(jsonsRequired.isEmpty()) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_FEATURES_LIST_INVALID")); //
        }
        List<File> jsonFiles = new ArrayList<>();
        if (fromDir != null) {
            map.put("download.location", fromDir.toString());
        }

        fine("JSONs required: " + jsonsRequired.toString());
        jsonFiles.addAll(map.getLocalJsonFiles(jsonsRequired));
        List<String> foundJsons = (List<String>) map.get("locally.present.jsons");
        fine("Found the following jsons locally: " + jsonFiles);
        if (jsonFiles.isEmpty() || jsonFiles.size() != jsonsRequired.size()) {
            fine("Could not find all json files from local directories, now downloading from Maven..");
            jsonsRequired.removeAll(foundJsons);
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
        List<String> additionalJsons;
        List<File> esaFiles;
        boolean noCache;
        boolean licenseAccepted;
        String to;

        public FeatureUtilityBuilder setFromDir(String fromDir) {
            this.fromDir = fromDir != null ? new File(fromDir) : null;
            return this;
        }

        public FeatureUtilityBuilder setEsaFiles(List<File> esaFiles) {
            this.esaFiles = esaFiles;
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
        
        public FeatureUtilityBuilder setAdditionalJsons(List<String> additionalJsons) {
            this.additionalJsons = additionalJsons;
            return this;
        }
        
        public FeatureUtilityBuilder setTo(String to) {
            this.to = to;
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
    
}
