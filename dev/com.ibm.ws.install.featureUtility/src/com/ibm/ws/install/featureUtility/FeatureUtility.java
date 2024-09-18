/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.featureUtility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallConstants.VerifyOption;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.featureUtility.props.FeatureUtilityProperties;
import com.ibm.ws.install.internal.InstallKernelMap;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.LicenseUpgradeUtility;
import com.ibm.ws.install.internal.ProgressBar;
import com.ibm.ws.kernel.boot.cmdline.Utils;

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
    private final Collection<String> platforms;
    private final List<String> additionalJsons;
    private String openLibertyVersion;
    private String openLibertyEdition;
    private final Logger logger;
    private ProgressBar progressBar;
    private Map<String, String> featureToExt;
    private static final String OPEN_LIBERTY_PRODUCT_ID = "io.openliberty";
    private static final String WEBSPHERE_LIBERTY_GROUP_ID = "com.ibm.websphere.appserver.features";
    private static final String BETA_EDITION = "EARLY_ACCESS";
    private static final String CONNECTION_FAILED_ERROR = "CWWKF1390E";
    private static final VerifyOption DEFAULT_VERIFY = VerifyOption.enforce;
    private static String to;

    private boolean isInstallServerFeature = false;
    private VerifyOption verifyOption;


    /*
     * Constructor for unit testing only.
     */
    protected FeatureUtility(InstallKernelMap map, File fromDir, List<File> esaFiles, Boolean noCache,
	    Boolean licenseAccepted, List<String> featuresToInstall, List<String> platforms, List<String> additionalJsons,
	    String openLibertyVersion, String openLibertyEdition, Logger logger, ProgressBar progressBar,
	    Map<String, String> featureToExt, boolean isInstallServerFeature, VerifyOption verifyOption) {
	super();
	this.map = map;
	this.fromDir = fromDir;
	this.esaFiles = esaFiles;
	this.noCache = noCache;
	this.licenseAccepted = licenseAccepted;
	this.featuresToInstall = featuresToInstall;
	this.platforms = platforms;
	this.additionalJsons = additionalJsons;
	this.openLibertyVersion = openLibertyVersion;
	this.openLibertyEdition = openLibertyEdition;
	this.logger = logger;
	this.progressBar = progressBar;
	this.featureToExt = featureToExt;
	this.isInstallServerFeature = isInstallServerFeature;
	this.verifyOption = verifyOption;
    }

    private FeatureUtility(FeatureUtilityBuilder builder) throws IOException, InstallException {
        
		this.logger = InstallLogUtils.getInstallLogger();
        this.progressBar = ProgressBar.getInstance();

        this.openLibertyVersion = getLibertyVersion();
        
        if (this.openLibertyEdition.equals(BETA_EDITION)) {
            throw new InstallException(
                            Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_BETA_EDITION_NOT_SUPPORTED"));
        }
	this.additionalJsons = builder.additionalJsons;
	this.platforms = builder.platforms;
        this.to = builder.to;
	this.fromDir = builder.fromDir; // this can be overwritten by the env prop

        List<String> rawFeatures = new ArrayList<>(builder.featuresToInstall);
        Map<String, Set<String>> jsonsAndFeatures = getJsonsAndFeatures(rawFeatures);

        this.featuresToInstall = new ArrayList<>(jsonsAndFeatures.get("features"));
        Set<String> jsonsList = jsonsAndFeatures.get("jsons");
	Set<String> jsonsRequired = new HashSet<>();
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
	Map<String, Object> envMap = (Map<String, Object>) map.get(InstallConstants.ENVIRONMENT_VARIABLE_MAP);
        
        if (envMap == null) {
	    throw new InstallException((String) map.get(InstallConstants.ACTION_ERROR_MESSAGE));
        }

        fine("Environment variables: ");
        Set<String> envMapKeys = envMap.keySet();
        for (String key: envMapKeys) {
        	if (key.equals("FEATURE_REPO_PASSWORD")) {
		    fine("FEATURE_REPO_PASSWORD: *********");
        	} else if (key.equals("FEATURE_LOCAL_REPO") && envMap.get("FEATURE_LOCAL_REPO") != null) {
		    fine(key + ": " + envMap.get(key));
		    File local_repo = new File((String) envMap.get("FEATURE_LOCAL_REPO"));
		    this.fromDir = local_repo;
        	} else {
		    fine(key + ": " + (envMap.get(key)));
        	}
        }
	map.put(InstallConstants.JSON_PROVIDED, false);
        overrideEnvMapWithProperties();
	this.verifyOption = getVerifyOption(builder.verifyOption,
		(Map<String, Object>) map.get(InstallConstants.ENVIRONMENT_VARIABLE_MAP));
        
	fine("additional jsons: " + additionalJsons);
        if (additionalJsons != null && !additionalJsons.isEmpty()) {
        	jsonsRequired.addAll(additionalJsons);
		map.put(InstallConstants.JSON_PROVIDED, true);
        }

	boolean isOpenLiberty = (Boolean) map.get(InstallConstants.IS_OPEN_LIBERTY);
        if (!isOpenLiberty) {
	    jsonsRequired.add(String.format(WEBSPHERE_LIBERTY_GROUP_ID + ":features:%s", openLibertyVersion));
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
	map.put(InstallConstants.CLEANUP_NEEDED, noCache);
        
	List<File> jsonPaths = getJsonFiles(fromDir, jsonsRequired);
        updateProgress(progressBar.getMethodIncrement("fetchJsons"));
        fine("Finished finding jsons");
	progressBar.manuallyUpdate();

	initializeMap(jsonPaths);
        updateProgress(progressBar.getMethodIncrement("initializeMap"));
        fine("Initialized install kernel map");

	if (verifyOption != VerifyOption.skip) {
	    downloadPublicKeys();
	}

	progressBar.manuallyUpdate();
    }

    /**
     * @param envMap
     * @param builder
     * @throws InstallException
     */
    protected VerifyOption getVerifyOption(String builderVerifyOption, Map<String, Object> envMap) throws InstallException {
	String verifyValue;
	String envValue = ((String) envMap.get("FEATURE_VERIFY"));

	if (builderVerifyOption == null && envValue == null) {
		verifyValue = DEFAULT_VERIFY.toString();
	    } else if (builderVerifyOption == null) {
		verifyValue = envValue.toLowerCase();
	    } else if (envValue == null) {
		verifyValue = builderVerifyOption;
	    } else {
		// If the verifyOption is set in both command line and (env var or props) than
		// the values have to match.
		if (!((String) envMap.get("FEATURE_VERIFY")).equalsIgnoreCase(builderVerifyOption)) {
		    throw new InstallException(
			    Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_VERIFY_OPTION_DOES_NOT_MATCH",
				    envMap.get("FEATURE_VERIFY"), builderVerifyOption),
			    InstallException.SIGNATURE_VERIFICATION_FAILED);
		}
		verifyValue = builderVerifyOption;
	    }


	try {
	    return VerifyOption.valueOf(verifyValue);
	} catch (IllegalArgumentException e) {
	    throw new InstallException(
		    Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_VERIFY_OPTION_NOT_VALID", verifyValue),
		    InstallException.SIGNATURE_VERIFICATION_FAILED);
	}
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
     */
    @SuppressWarnings("restriction")
    private void initializeMap(List<File> jsonPaths) {
	    map.put(InstallConstants.IS_FEATURE_UTILITY, true);
	    map.put(InstallConstants.RUNTIME_INSTALL_DIR, Utils.getInstallDir());
	    map.put(InstallConstants.INSTALL_LOCAL_ESA, true);
	    map.put(InstallConstants.SINGLE_JSON_FILE, jsonPaths);
	    if (featuresToInstall != null) {
		    map.put(InstallConstants.FEATURES_TO_RESOLVE, featuresToInstall);
	    }
        if (platforms != null) {
        	map.put(InstallConstants.PLATFORMS, platforms);
        }
        if (esaFiles != null && !esaFiles.isEmpty()) {
	    map.put(InstallConstants.INDIVIDUAL_ESAS, esaFiles);
	    map.put(InstallConstants.INSTALL_INDIVIDUAL_ESAS, true);
        }

	map.put(InstallConstants.LICENSE_ACCEPT, licenseAccepted);
	map.get(InstallConstants.INSTALL_KERNEL_INIT_CODE);
	map.put(InstallConstants.VERIFY_OPTION, verifyOption);
	Collection<Map<String, String>> keyMap = FeatureUtilityProperties.getKeyMap().values();
	map.put(InstallConstants.USER_PUBLIC_KEYS, keyMap);
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
			try {
			    URL hostURL = new URL(host);
			    protocol = hostURL.getProtocol();
			    host = hostURL.getHost();
			} catch (MalformedURLException e) {
			    // If protocol is not defined, assume http protocol.
			    logger.fine("Proxy protocol is not defined: " + e.getMessage());
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
			overrideMap.put(protocol + ".proxyUser", username);
			overrideMap.put(protocol + ".proxyPassword", password);

		}

		// override no_proxy settings
		if (FeatureUtilityProperties.getNoProxySetting() != null) {
		    overrideMap.put("http.nonProxyHosts", FeatureUtilityProperties.getNoProxySetting());
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
        
	// override feature verify option
	if (FeatureUtilityProperties.getFeatureVerifyOption() != null) {
	    overrideMap.put("FEATURE_VERIFY", FeatureUtilityProperties.getFeatureVerifyOption());
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
			map.put(InstallConstants.JSON_PROVIDED, true);
        	}
        	
        }

	map.put(InstallConstants.OVERRIDE_ENVIRONMENT_VARIABLES, overrideMap);

	if (map.get(InstallConstants.ACTION_ERROR_MESSAGE) != null) {
	    // error with installation
	    fine("action.exception.stacktrace: " + map.get(InstallConstants.ACTION_EXCEPTION_STACKTRACE));
	    String exceptionMessage = (String) map.get(InstallConstants.ACTION_ERROR_MESSAGE);
	    throw new InstallException(exceptionMessage);
	}
    }

    /**
     * Return a hashmap that divides the group id's and artifact id's from a list of features.
     * In the hashmap, the "jsons" key refers to the group ids and the "features" key refers to the 
     * artifact ids.
     * @param featureNames a list of feature shortnames or maven coordinates
     * @return hashmap with group ids and artifact ids seperated
     * @throws InstallException
     */
    private Map<String, Set<String>> getJsonsAndFeatures(List<String> featureNames)
	    throws InstallException {
        Map<String, Set<String>> jsonsAndFeatures = new HashMap<>();

        Set<String> jsonsRequired = new HashSet<>();
        Set<String> featuresRequired = new HashSet<>();

        String groupId, artifactId, version, packaging = null;
        for (String feature : featureNames) {
            String[] mavenCoords = feature.split(":");
            switch(mavenCoords.length){
                case 1: // artifactId
                    groupId = "io.openliberty.features";
                    artifactId = mavenCoords[0];
		    version = this.openLibertyVersion;
                    packaging = "esa";
                    break;
                case 2: // groupId:artifactId
                    groupId = mavenCoords[0];
                    artifactId = mavenCoords[1];
		    version = this.openLibertyVersion;
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

    private void verifyMavenCoordinate(String feature, String groupId, String artifactId, String version,
	    String packaging) throws InstallException {
        // check for any empty parameters
        if(groupId.isEmpty() || artifactId.isEmpty() || version.isEmpty() || packaging.isEmpty()){
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_MAVEN_COORDINATE_INVALID", feature));
        }
	if (!version.equals(this.openLibertyVersion)) {
            throw new InstallException(
                            Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_MAVEN_COORDINATE_WRONG_VERSION", feature, openLibertyVersion));
        }
        if(!"esa".equals(packaging)){
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_MAVEN_COORDINATE_WRONG_PACKAGING", feature));
        }
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
	String olVersion = null;
	String olEdition = null;
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(propertiesFile)) {
            properties.load(input);
            String productId = properties.getProperty("com.ibm.websphere.productId");
            String productVersion = properties.getProperty("com.ibm.websphere.productVersion");
            String productEdition = properties.getProperty("com.ibm.websphere.productEdition");
            if (productId.equals(OPEN_LIBERTY_PRODUCT_ID)) {
		olVersion = productVersion;
		olEdition = productEdition;
            }
        }

	if (olVersion == null || olEdition == null) {
            // openliberty.properties file is missing or invalidly formatted
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_COULD_NOT_DETERMINE_RUNTIME_PROPERTIES_FILE", propertiesFile.getAbsolutePath()));

        }
	this.openLibertyVersion = olVersion;
	this.openLibertyEdition = olEdition;
        return openLibertyVersion;
    }

    public Set<String> findFeatures(){
        String query = String.join(" ", featuresToInstall);
	map.put(InstallConstants.ACTION_FIND, query);
	Set<String> features = (Set<String>) map.get(InstallConstants.ACTION_RESULT);

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

    /*
     * Download public keys to verify features - only when verifyOption is
     * "enforce", "all", "warn"
     * 
     * @throws InstallException
     */

    public void downloadPublicKeys() throws InstallException {
	map.get(InstallConstants.DOWNLOAD_PUBKEYS);
	if (map.get(InstallConstants.ACTION_ERROR_MESSAGE) != null) {
	    if (map.get(InstallConstants.ACTION_EXCEPTION_STACKTRACE) != null) {
		fine("action.exception.stacktrace: " + map.get(InstallConstants.ACTION_EXCEPTION_STACKTRACE));
	    }
	    throw new InstallException((String) map.get(InstallConstants.ACTION_ERROR_MESSAGE),
		    InstallException.SIGNATURE_VERIFICATION_FAILED);
	}
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
	    map.put(InstallConstants.FROM_REPO, fromDir.toString());
        }
	map.put(InstallConstants.IS_INSTALL_SERVER_FEATURE, isInstallServerFeature);
	Collection<String> resolvedFeatures = (Collection<String>) map.get(InstallConstants.ACTION_RESULT);
        checkResolvedFeatures(resolvedFeatures);
	if (resolvedFeatures.isEmpty()) { // all features are already installed
	    return;
	}
	boolean upgraded = (boolean) map.get(InstallConstants.UPGRADE_COMPLETE);
        List<String> causedUpgrade = (List<String>) map.get(InstallConstants.CAUSED_UPGRADE);
        if (upgraded) {
        	LicenseUpgradeUtility luu = new LicenseUpgradeUtility.LicenseUpgradeUtilityBuilder().setFeatures(featuresToInstall).setAcceptLicense(licenseAccepted).build();
        	boolean isLicenseAccepted = false;
        	try {
            	isLicenseAccepted = luu.handleLicenses(featureFormat(causedUpgrade));
            } catch (InstallException e) {
		map.get(InstallConstants.CLEANUP_UPGRADE); // cleans up the files we put down during upgrade
            	throw e;
            }
            if (!isLicenseAccepted) {
		map.get(InstallConstants.CLEANUP_UPGRADE); // cleans up the files we put down during upgrade
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_LICENSES_NOT_ACCEPTED"));
            } else {
            	luu.handleOLLicense();
            }
        	
        }
        updateProgress(progressBar.getMethodIncrement("resolvedFeatures"));
        info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_PREPARING_ASSETS"));
        Collection<File> artifacts = fromDir != null ? downloadFeaturesFrom(resolvedFeatures, fromDir) : downloadFeatureEsas((List<String>) resolvedFeatures);

	if (verifyOption != null && verifyOption != VerifyOption.skip) {
	    map.put(InstallConstants.ACTION_VERIFY, artifacts);
	    map.get(InstallConstants.ACTION_RESULT);
	    if (map.get(InstallConstants.ACTION_ERROR_MESSAGE) != null) {
		// error with installation
		if (map.get(InstallConstants.ACTION_EXCEPTION_STACKTRACE) != null) {
		    fine("action.exception.stacktrace: " + map.get(InstallConstants.ACTION_EXCEPTION_STACKTRACE));
		}

		String exceptionMessage = (String) map.get(InstallConstants.ACTION_ERROR_MESSAGE);
		throw new InstallException(exceptionMessage, InstallException.SIGNATURE_VERIFICATION_FAILED);
	    }
	}


        info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_STARTING_INSTALL"));
	Collection<String> actionReturnResult = new ArrayList<>();
        List<String> currentReturnResult;
        try {

            for (File esaFile : artifacts) {
		double increment = ((progressBar.getMethodIncrement("installFeatures")) / (artifacts.size()));
            	String featureName = extractFeature(esaFile.getName());
                fine(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_INSTALLING", featureName));
		map.put(InstallConstants.LICENSE_ACCEPT, true);
		map.put(InstallConstants.ACTION_INSTALL, esaFile);
                String ext = featureToExt.get(featureName);
                if (to != null) {
		    map.put(InstallConstants.TO_EXTENSION, to);
                    fine("Installing to extension: " + to);
                }
		if (ext != null && !ext.equals("")) {
		    map.put(InstallConstants.TO_EXTENSION, ext);
                	fine("Installing to extension from server.xml: " + ext);
                }
		map.get(InstallConstants.ACTION_RESULT);
                
		if (map.get(InstallConstants.ACTION_ERROR_MESSAGE) != null) {
                    // error with installation
		    fine("action.exception.stacktrace: " + map.get(InstallConstants.ACTION_EXCEPTION_STACKTRACE));
		    String exceptionMessage = (String) map.get(InstallConstants.ACTION_ERROR_MESSAGE);
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
		map.put(InstallConstants.TO_EXTENSION, InstallConstants.TO_USER);
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
		    StringBuilder result = new StringBuilder();
			for (String str: causedUpgrade) {
				if (causedUpgrade.indexOf(str) == 0) {
				    result.append(causedUpgrade.get(0));
				} else if (causedUpgrade.indexOf(str) < causedUpgrade.size() - 1) {
				    result.append(", " + causedUpgrade.get(causedUpgrade.indexOf(str)));
				} else {
				    result.append(", and " + causedUpgrade.get(causedUpgrade.indexOf(str)));
				}
			}
			return result.toString();
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
	    throw new InstallException((String) map.get(InstallConstants.ACTION_ERROR_MESSAGE));
        } else if (resolvedFeatures.isEmpty()) {
	    String exceptionMessage = (String) map.get(InstallConstants.ACTION_ERROR_MESSAGE);
            if (exceptionMessage == null) {
				if (isInstallServerFeature) {
					logger.info(InstallLogUtils.Messages.INSTALL_KERNEL_MESSAGES
							.getMessage("MSG_SERVER_NEW_FEATURES_NOT_REQUIRED"));
				} else {
					throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ALREADY_INSTALLED",
						map.get(InstallConstants.FEATURES_TO_RESOLVE)),
						InstallException.ALREADY_EXISTS);
				}
            } else if (exceptionMessage.contains("CWWKF1250I")) {
                throw new InstallException(exceptionMessage);

            } else {
		fine("action.exception.stacktrace: " + map.get(InstallConstants.ACTION_EXCEPTION_STACKTRACE));
                throw new InstallException(exceptionMessage);
            }
        } else
        	if (!isInstallServerFeature) {
        		String installingFeature = featuresToInstall.get(0);
        		for( String aFeature : resolvedFeatures) {
        			String shortName = aFeature.split(":").length > 1 ? aFeature.split(":")[1] : "" ;
        			if (installingFeature.equals(shortName) && isBaseVersionless(aFeature)) {
        				throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_VERSIONLESS_INSTALL"), InstallException.BAD_ARGUMENT);
        			}	
        		}
        	}
    }

    /**
     * @param aFeature
     * @return if the feature coordinates represents a versionless feature
     */
    private boolean isBaseVersionless(String aFeature) {
    	String[] featureCoordinates = aFeature.split(":");
    	if (featureCoordinates.length >= 2) {
    		String groupName = featureCoordinates[0];
    		String shortName = featureCoordinates[1];
    		if (!shortName.contains("-") && (groupName.equals("io.openliberty.features")))
    			return true;
    	}
		return false;
	}

	private List<File> downloadFeaturesFrom(Collection<String> resolvedFeatures, File fromDir) throws InstallException {
	map.put(InstallConstants.FROM_REPO, fromDir.toString());
        return downloadFeatureEsas(resolvedFeatures);
    }

    private List<File> downloadFeatureEsas(Collection<String> resolvedFeatures) throws InstallException {
	map.put(InstallConstants.DOWNLOAD_ARTIFACT_LIST, resolvedFeatures);
        boolean singleArtifactInstall = false;
	map.put(InstallConstants.DOWNLOAD_INDIVIDUAL_ARTIFACT, singleArtifactInstall);
        
	List<File> result = (List<File>) map.get(InstallConstants.DOWNLOAD_RESULT);
	if (map.get(InstallConstants.ACTION_ERROR_MESSAGE) != null) {
	    fine("action.exception.stacktrace: " + map.get(InstallConstants.ACTION_EXCEPTION_STACKTRACE));
	    String exceptionMessage = (String) map.get(InstallConstants.ACTION_ERROR_MESSAGE);
	    if (exceptionMessage.contains(CONNECTION_FAILED_ERROR)) {
		throw new InstallException(exceptionMessage, InstallException.CONNECTION_FAILED);
	    }
            throw new InstallException(exceptionMessage);
        }
        return result;
    }


    public List<String> resolveFeatures(boolean isShortNames) throws InstallException {
	map.put(InstallConstants.DOWNLOAD_LOCATION, fromDir.toString());

	List<String> shortNames = new ArrayList<>();
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
	    map.put(InstallConstants.FEATURES_TO_RESOLVE, featuresToInstall);
        }
	List<String> resolvedFeatures = (List<String>) map.get(InstallConstants.ACTION_RESULT);
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
	 * Extracts the feature name from an ESA filepath. Example:
	 * extractFeature(appSecurity-3.0-19.0.0.8.esa) returns appSecurity-3.0 filename
	 * extractFeature(userFeature1-1.0.esa) returns userFeature1 filename filename
	 * cannot be null
	 * 
	 * @param filename
	 * @return returns the feature name from the esa file path
	 */
    private String extractFeature(String filename) {
		String[] split = filename.split("-");
		if (split.length > 2) {
			return split[0] + "-" + split[1];
		}

		return split[0];
    }

    public List<File> getJsonFiles(File fromDir, Set<String> jsonsRequired) throws InstallException {
        if(jsonsRequired.isEmpty()) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_FEATURES_LIST_INVALID")); //
        }
        List<File> jsonFiles = new ArrayList<>();
        if (fromDir != null) {
	    map.put(InstallConstants.DOWNLOAD_LOCATION, fromDir.toString());
        }

        fine("JSONs required: " + jsonsRequired.toString());
        jsonFiles.addAll(map.getLocalJsonFiles(jsonsRequired));
        List<String> foundJsons = (List<String>) map.get("locally.present.jsons");
        fine("Found the following jsons locally: " + jsonFiles);
        if (jsonFiles.isEmpty() || jsonFiles.size() != jsonsRequired.size()) {
            fine("Could not find all json files from local directories, now downloading from Maven..");
            jsonsRequired.removeAll(foundJsons);
            jsonFiles.addAll(map.getJsonsFromMavenCentral(jsonsRequired));
	    if (map.get(InstallConstants.ACTION_ERROR_MESSAGE) != null) {
                // error with installation
		fine("action.exception.stacktrace: " + map.get(InstallConstants.ACTION_EXCEPTION_STACKTRACE));
		String exceptionMessage = (String) map.get(InstallConstants.ACTION_ERROR_MESSAGE);
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
	String tempStr = (String) map.get(InstallConstants.CLEANUP_TEMP_LOCATION);
	Boolean cleanupNeeded = (Boolean) map.get(InstallConstants.CLEANUP_NEEDED);
        boolean deleted = true;

	if (cleanupNeeded != null && cleanupNeeded) { // change this to a map.get(InstallConstants.CLEANUP_NEEDED)
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
        return Paths.get(System.getProperty("user.home"), ".m2", "repository", "").toFile();
    }

    private void updateProgress(double increment) {
        progressBar.updateProgress(increment);

    }

    // log message types
    private void info(String msg) {
	logger.info(msg);
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
        Collection<String> platforms;
        List<String> additionalJsons;
        List<File> esaFiles;
        boolean noCache;
        boolean licenseAccepted;
        String to;
	String verifyOption;

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

		public FeatureUtilityBuilder setVerify(String verifyOption) {
		    this.verifyOption = verifyOption;
		    return this;
		}

        public FeatureUtility build() throws IOException, InstallException {
            return new FeatureUtility(this);
        }

		public FeatureUtilityBuilder setPlatforms(List<String> platformNames) {
			this.platforms = platformNames;
		    return this;
		}

    }

    public List<String> getMissingArtifactsFromFolder(List<String> artifacts, String location, boolean isShortName) {
	List<String> result = new ArrayList<>();

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
		featurePath = Paths.get(groupDir.getAbsolutePath(), id, openLibertyVersion, featureEsa);
            } else {
                String groupId = id.split(":")[0];
                String featureName = id.split(":")[1];
                File groupDir = new File(location, groupId.replace(".", "/"));
                if (!groupDir.exists()) {
                    result.add(id);
                    continue;
                }
                String featureEsa = featureName + "-" + openLibertyVersion + ".esa";
		featurePath = Paths.get(groupDir.getAbsolutePath(), featureName, openLibertyVersion, featureEsa);
            }
            if (!Files.isRegularFile(featurePath)) {
                result.add(id);
            }
        }
        return result;
    }

    public List<String> getMavenCoords(List<String> artifactShortNames) {
	List<String> result = new ArrayList<>();
        for (String shortName: artifactShortNames) {
            result.add(OPEN_LIBERTY_PRODUCT_ID + ".feature:" + shortName + ":" + openLibertyVersion);
        }
        return result;
    }
    
}
