/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
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
package com.ibm.ws.install.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.aries.util.manifest.ManifestProcessor;

import com.ibm.ws.install.CancelException;
import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallConstants.VerifyOption;
import com.ibm.ws.install.InstallEventListener;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallKernel;
import com.ibm.ws.install.InstallKernelFactory;
import com.ibm.ws.install.InstallKernelInteractive;
import com.ibm.ws.install.InstallLicense;
import com.ibm.ws.install.InstallProgressEvent;
import com.ibm.ws.install.RepositoryConfigUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.feature.internal.generator.ManifestFileProcessor;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.productinfo.DuplicateProductInfoException;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.kernel.productinfo.ProductInfoReplaceException;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.repository.common.enums.ReadMode;
import com.ibm.ws.repository.connections.DirectoryRepositoryConnection;
import com.ibm.ws.repository.connections.ProductDefinition;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.SingleFileRepositoryConnection;
import com.ibm.ws.repository.connections.liberty.ProductInfoProductDefinition;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.parsers.EsaParser;
import com.ibm.ws.repository.parsers.Parser;
import com.ibm.ws.repository.resolver.RepositoryResolutionException;
import com.ibm.ws.repository.resolver.RepositoryResolver;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;
import com.ibm.ws.repository.resources.writeable.RepositoryResourceWritable;
import com.ibm.ws.repository.strategies.writeable.AddThenDeleteStrategy;

/**
 *
 */
@SuppressWarnings("rawtypes")
public class InstallKernelMap implements Map {

    private static final String MAP_BASED_INSTALL_KERNEL = "mapBasedInstallKernel";

    // Keys

    private static final String REPOSITORIES_PROPERTIES = "repositories.properties";
    private static final String DOWLOAD_EXTERNAL_DEPS = "dowload.external.deps";
    private static final String LOCAL_ESA_DOWNLOAD_DIR = "local.esa.download.dir";
    private static final String USER_AGENT = "user.agent";
    private static final String PROGRESS_MONITOR_MESSAGE = "progress.monitor.message";
    private static final String PROGRESS_MONITOR_CANCELLED = "progress.monitor.cancelled";
    private static final String PROGRESS_MONITOR_SIZE = "progress.monitor.size";
    private static final String TARGET_USER_DIRECTORY = "target.user.directory";
    private static final String INSTALL_KERNEL_INIT_ERROR_MESSAGE = "install.kernel.init.error.message";
    private static final String UNINSTALL_USER_FEATURES = "uninstall.user.features";
    private static final String FORCE_UNINSTALL = "force.uninstall";
    private static final String ACTION_INSTALL_RESULT = "action.install.result";
    private static final String ACTION_UNINSTALL = "action.uninstall";
    private static final String DIRECTORY_BASED_REPOSITORY = "directory.based.repository";
    private static final String MESSAGE_LOCALE = "message.locale";
    private static final String DOWNLOAD_FILETYPE = "download.filetype";
    private static final String DOWNLOAD_LOCAL_DIR_LOCATION = "download.local.dir.location";
    private static final String DOWNLOAD_ARTIFACT_SINGLE = "download.artifact.single";
    private static final String GENERATE_JSON = "generate.json";
    private static final String GENERATE_JSON_GROUP_ID_MAP = "generate.json.group.id.map";
    private static final String TARGET_JSON_DIR = "target.json.dir";
    private static final String LOCALLY_PRESENT_JSONS = "locally.present.jsons";

    //Headers in Manifest File
    private static final String SHORTNAME_HEADER_NAME = "IBM-ShortName";
    private static final String SYMBOLICNAME_HEADER_NAME = "Subsystem-SymbolicName";

    // Return code
    private static final Integer OK = Integer.valueOf(0);
    private static final Integer CANCELLED = Integer.valueOf(-1);
    private static final Integer ERROR = Integer.valueOf(1);

    // License identifiers
    private static final String LICENSE_EPL_PREFIX = "https://www.eclipse.org/legal/epl-";
    private static final String LICENSE_FEATURE_TERMS = "http://www.ibm.com/licenses/wlp-featureterms-v1";
    private static final String LICENSE_FEATURE_TERMS_RESTRICTED = "http://www.ibm.com/licenses/wlp-featureterms-restricted-v1";

    // Maven downloader
    private final String OPEN_LIBERTY_GROUP_ID = "io.openliberty.features";
    private final String WEBSPHERE_LIBERTY_GROUP_ID = "com.ibm.websphere.appserver.features";
    private final String JSON_ARTIFACT_ID = "features";
    private final String OPEN_LIBERTY_PRODUCT_ID = "io.openliberty";
    private final String MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2/";
    private final MavenRepository MAVEN_CENTRAL_REPOSITORY = new MavenRepository("Maven Central", MAVEN_CENTRAL, null, null);
    private final String TEMP_DIRECTORY = Utils.getInstallDir().getAbsolutePath() + File.separator + "tmp"
                                          + File.separator;
    private static final String ETC_DIRECTORY = Utils.getInstallDir().getAbsolutePath() + File.separator + "etc"
                                                + File.separator;
    private static final String WLP_DIR = Utils.getInstallDir().getAbsolutePath() + File.separator;
    private static final String LICENSE_DIRECTORY = "lafiles" + File.separator;
    private static final String LIB_VERSIONS_DIRECTORY = "lib" + File.separator + "versions" + File.separator;
    private static final String FEATURE_UTILITY_PROPS_FILE = "featureUtility.env";
    public static final String HIDDEN_PASSWORD = "********";
    private Map<String, Object> envMap = null;
    private final List<File> upgradeFiles = new ArrayList<File>();
    private final List<MavenRepository> workingRepos = new ArrayList<MavenRepository>();
    private final List<String> usrFeatures = new ArrayList<>();
    private List<File> pubKeys = new ArrayList<>();

    private enum ActionType {
        install,
        uninstall,
        resolve,
        find,
        verify
    }

    private final Map data = new HashMap();
    private InstallKernelInteractive installKernel;
    private InstallEventListener ielistener;
    private ActionType actionType = null;
    private boolean usingM2Cache = false;
    private String openLibertyVersion = null;
    private final Logger logger = InstallLogUtils.getInstallLogger();
    private final ProgressBar progressBar = ProgressBar.getInstance();
    private final boolean isWindows = (System.getProperty("os.name").toLowerCase()).indexOf("win") >= 0;
    // TODO remove this need for windwos chewcking for progress bar

    @SuppressWarnings("unchecked")
    public InstallKernelMap() {
        data.put(InstallConstants.LICENSE_ACCEPT, Boolean.FALSE);
    }

    /**
     * Unsupported operation
     */
    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return data.containsKey(key);
    }

    /**
     * Unsupported operation
     */
    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation
     */
    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation
     */
    @Override
    public void putAll(Map m) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation
     */
    @Override
    public Set keySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation
     */
    @Override
    public Collection values() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation
     */
    @Override
    public Set entrySet() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public Object get(Object key) {
        if (InstallConstants.INSTALL_KERNEL_INIT_CODE.equals(key)) {
            return initKernel();
        } else if (InstallConstants.ACTION_RESULT.equals(key)) {
            if (actionType.equals(ActionType.install)) {
                Boolean localESAInstall = (Boolean) data.get(InstallConstants.INSTALL_LOCAL_ESA);
                if (localESAInstall == null || !localESAInstall) {
                    return install();
                } else {
                    return localESAInstall();
                }
            } else if (actionType.equals(ActionType.uninstall)) {
                return uninstall();
            } else if (actionType.equals(ActionType.resolve)) {
                Boolean localESAInstall = (Boolean) data.get(InstallConstants.INSTALL_LOCAL_ESA);
                if (localESAInstall == null || !localESAInstall) {
                    return data.get(InstallConstants.ACTION_RESULT);
                } else {
                    return singleFileResolve();
                }
            } else if (actionType.equals(ActionType.find)) {
                return findFeatures();
            } else if (actionType.equals(ActionType.verify)) {
                return verifySignatures();
            }
        } else if (InstallConstants.IS_FEATURE_UTILITY.equals(key)) {
            if (data.get(InstallConstants.IS_FEATURE_UTILITY) == null) {
                return false;
            } else {
                return data.get(InstallConstants.IS_FEATURE_UTILITY);
            }
        } else if (InstallConstants.IS_INSTALL_SERVER_FEATURE.equals(key)) {
            if (data.get(InstallConstants.IS_INSTALL_SERVER_FEATURE) == null) {
                return false;
            } else {
                return data.get(InstallConstants.IS_INSTALL_SERVER_FEATURE);
            }
        } else if (InstallConstants.JSON_PROVIDED.equals(key)) {
            if (data.get(InstallConstants.JSON_PROVIDED) == null) {
                return false;
            } else {
                return data.get(InstallConstants.JSON_PROVIDED);
            }
        } else if (PROGRESS_MONITOR_SIZE.equals(key)) {
            return getMonitorSize();
        } else if (InstallConstants.DOWNLOAD_RESULT.equals(key)) {
            Boolean downloadSingleArtifact = (Boolean) data.get(InstallConstants.DOWNLOAD_INDIVIDUAL_ARTIFACT);

            if (downloadSingleArtifact != null && downloadSingleArtifact) {
                return downloadArtifact();
            } else {
                return downloadEsas();
            }
        } else if (InstallConstants.ENVIRONMENT_VARIABLE_MAP.equals(key)) {
            if (envMap != null) {
                return envMap;
            }
            envMap = getEnvMap();
            if (envMap != null) { //set proxy if there were no errors
                //set proxy system properties
                setProxy();
            }

            return envMap;
        } else if (InstallConstants.USER_PUBLIC_KEYS.equals(key)) {
            return data.get(InstallConstants.USER_PUBLIC_KEYS);
        } else if (InstallConstants.CLEANUP_UPGRADE.equals(key)) {
            return cleanupUpgrade();
        } else if (InstallConstants.IS_OPEN_LIBERTY.equals(key)) {
            return isOpenLiberty();
        } else if (GENERATE_JSON.equals(key)) {
            return generateJson();
        } else if (InstallConstants.DOWNLOAD_PUBKEYS.equals(key)) {
            return downloadPublicKeys();
        }
        return data.get(key);
    }

    /**
     * Searches through the json files in the InstallConstants.SINGLE_JSON_FILE property of this map for the query specified by InstallConstants.ACTION_FIND
     *
     * @return a list of features matching the query in the following format:
     *         <Type> : <shortName> : <fullName>
     */
    private Set<String> findFeatures() {
        Set<String> returnedFeatures = new LinkedHashSet<>();
        List<File> jsons = (List<File>) get(InstallConstants.SINGLE_JSON_FILE);
        String query = ((String) data.get(InstallConstants.ACTION_FIND)).toLowerCase();

        double individualSize = progressBar.getMethodIncrement("findFeatures") / (jsons.size());

        for (File jsonFile : jsons) {
            try (InputStream is = new FileInputStream(jsonFile)) {

                JsonReader jsonReader = Json.createReader(is);
                JsonArray jsonArray = jsonReader.readArray();
                jsonReader.close();

                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject json = jsonArray.getJsonObject(i);
                    // todo use a constants class to get the json attributes
                    JsonObject wlpInfo = json.getJsonObject("wlpInformation");

                    String visibility = null;
                    try {
                        visibility = wlpInfo.getJsonString("visibility").getString();
                    } catch (NullPointerException e) {

                    }
                    if (visibility == null || !visibility.equals("PUBLIC")) {
                        continue;
                    }

                    String name = null;
                    try {
                        name = json.getJsonString("name").getString();
                    } catch (NullPointerException e) {

                    }
                    String type = wlpInfo.getJsonString("typeLabel").getString();

                    if (wlpInfo.getJsonString("shortName") != null && json.getJsonString("shortDescription") != null) {
                        String shortname = wlpInfo.getJsonString("shortName").getString();
                        String description = json.getJsonString("shortDescription").getString();

                        if (query.isEmpty() || shortname.toLowerCase().contains(query) || description.toLowerCase().contains(query)) {
                            returnedFeatures.add(String.format("%s : %s : %s", type, shortname, name));
                        }
                    } else { //user feature that doesn't have shortname and/or description
                        if (query.isEmpty() || name.contains(query)) {
                            returnedFeatures.add(String.format("%s : %s ", type, name));
                        }
                    }

                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            updateProgress(individualSize);
            progressBar.manuallyUpdate();
            fine("Finished processing " + jsonFile.getName());
        }

        return returnedFeatures;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object put(Object key, Object value) {
        if (InstallConstants.LICENSE_ACCEPT.equals(key)) {
            if (value instanceof Boolean) {
                data.put(InstallConstants.LICENSE_ACCEPT, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.RUNTIME_INSTALL_DIR.equals(key)) {
            if (value instanceof File) {
                data.put(InstallConstants.RUNTIME_INSTALL_DIR, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (LOCAL_ESA_DOWNLOAD_DIR.equals(key)) {
            if (value instanceof File) {
                data.put(LOCAL_ESA_DOWNLOAD_DIR, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (TARGET_JSON_DIR.equals(key)) {
            if (value instanceof File) {
                data.put(TARGET_JSON_DIR, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (REPOSITORIES_PROPERTIES.equals(key)) {
            if (value instanceof File) {
                data.put(REPOSITORIES_PROPERTIES, value);
                System.setProperty(InstallConstants.OVERRIDE_PROPS_LOCATION_ENV_VAR, ((File) value).getAbsolutePath());
                Properties repoProperties;
                try {
                    repoProperties = RepositoryConfigUtils.loadRepoProperties();
                    if (repoProperties != null) {
                        //Set the repository properties instance in Install Kernel
                        installKernel.setRepositoryProperties(repoProperties);
                    }
                } catch (InstallException e) {
                    data.put(InstallConstants.ACTION_RESULT, ERROR);
                    data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
                    data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
                    throw new RuntimeException(e);
                }
            } else {
                throw new IllegalArgumentException();
            }
        } else if (DOWLOAD_EXTERNAL_DEPS.equals(key)) {
            if (value instanceof Boolean) {
                data.put(DOWLOAD_EXTERNAL_DEPS, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.INSTALL_LOCAL_ESA.equals(key)) {
            if (value instanceof Boolean) {
                data.put(InstallConstants.INSTALL_LOCAL_ESA, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.CLEANUP_TEMP_LOCATION.equals(key)) {
            if (value instanceof String) {
                data.put(InstallConstants.CLEANUP_TEMP_LOCATION, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.CLEANUP_NEEDED.equals(key)) {
            if (value instanceof Boolean) {
                data.put(InstallConstants.CLEANUP_NEEDED, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (USER_AGENT.equals(key)) {
            if (value instanceof String) {
                data.put(USER_AGENT, value);
                if (installKernel != null)
                    installKernel.setUserAgent((String) value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.JSON_PROVIDED.equals(key)) {
            if (value instanceof Boolean) {
                data.put(InstallConstants.JSON_PROVIDED, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (GENERATE_JSON.equals(key)) {
            if (value instanceof Boolean) {
                data.put(GENERATE_JSON, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.IS_FEATURE_UTILITY.equals(key)) {
            if (value instanceof Boolean) {
                data.put(InstallConstants.IS_FEATURE_UTILITY, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.TO_EXTENSION.equals(key)) {
            if (value instanceof String) {
                data.put(InstallConstants.TO_EXTENSION, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.IS_INSTALL_SERVER_FEATURE.equals(key)) {
            if (value instanceof Boolean) {
                data.put(InstallConstants.IS_INSTALL_SERVER_FEATURE, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.PLATFORMS.equals(key)) {
            if (value instanceof Collection) {
                data.put(InstallConstants.PLATFORMS, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.VERIFY_OPTION.equals(key)) {
            if (value instanceof VerifyOption) {
                data.put(InstallConstants.VERIFY_OPTION, value);
            } else if (value instanceof String) {
                data.put(InstallConstants.VERIFY_OPTION, VerifyOption.valueOf((String) value));
            } else {
                throw new IllegalArgumentException();
            }
        } else if (DOWNLOAD_FILETYPE.equals(key)) {
            if (value instanceof String) {
                data.put(DOWNLOAD_FILETYPE, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (DOWNLOAD_LOCAL_DIR_LOCATION.equals(key)) {
            if (value instanceof String) {
                data.put(DOWNLOAD_LOCAL_DIR_LOCATION, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.FROM_REPO.equals(key)) {
            if (value instanceof String) {
                data.put(InstallConstants.FROM_REPO, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.ENVIRONMENT_VARIABLE_MAP.equals(key)) {
            if (value instanceof Map<?, ?>) {
                data.put(InstallConstants.ENVIRONMENT_VARIABLE_MAP, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.OVERRIDE_ENVIRONMENT_VARIABLES.equals(key)) {
            if (value instanceof Map<?, ?>) {
                overrideEnvMap((Map<String, Object>) value);
                //override proxy system properties
                setProxy();
            } else {
                throw new IllegalArgumentException();
            }
        } else if (DOWNLOAD_ARTIFACT_SINGLE.equals(key)) {
            if (value instanceof String) {
                data.put(DOWNLOAD_ARTIFACT_SINGLE, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.DOWNLOAD_ARTIFACT_LIST.equals(key)) {
            if (value instanceof List || value instanceof String) {
                data.put(InstallConstants.DOWNLOAD_ARTIFACT_LIST, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.DOWNLOAD_INDIVIDUAL_ARTIFACT.equals(key)) {
            if (value instanceof Boolean) {
                data.put(InstallConstants.DOWNLOAD_INDIVIDUAL_ARTIFACT, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.DOWNLOAD_LOCATION.equals(key)) {
            if (value instanceof String) {
                data.put(InstallConstants.DOWNLOAD_LOCATION, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (GENERATE_JSON_GROUP_ID_MAP.equals(key)) {
            if (value instanceof Map) {
                data.put(GENERATE_JSON_GROUP_ID_MAP, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (TARGET_USER_DIRECTORY.equals(key)) {
            if (value instanceof File) {
                data.put(TARGET_USER_DIRECTORY, value);
                Utils.setUserDir((File) value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (MESSAGE_LOCALE.equals(key)) {
            if (value instanceof Locale) {
                data.put(MESSAGE_LOCALE, value);
                Messages.setLocale((Locale) value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (PROGRESS_MONITOR_MESSAGE.equals(key)) {
            if (value instanceof List) {
                data.put(PROGRESS_MONITOR_MESSAGE, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (PROGRESS_MONITOR_CANCELLED.equals(key)) {
            if (value instanceof List) {
                data.put(PROGRESS_MONITOR_CANCELLED, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.ACTION_INSTALL.equals(key)) {
            if (value instanceof List || value instanceof File) {
                Boolean localESAInstall = (Boolean) data.get(InstallConstants.INSTALL_LOCAL_ESA);
                if (localESAInstall == null || !localESAInstall) {
                    resolve(value);
                } else {
                    data.put(InstallConstants.ACTION_INSTALL, value);
                }
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.ACTION_FIND.equals(key)) {
            if (value instanceof String) {
                data.put(InstallConstants.ACTION_FIND, value);
                actionType = ActionType.find;
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.ACTION_VERIFY.equals(key)) {
            if (value instanceof List) {
                data.put(InstallConstants.ACTION_VERIFY, value);
                actionType = ActionType.verify;
            }
        } else if (UNINSTALL_USER_FEATURES.equals(key)) {
            if (value instanceof Boolean) {
                data.put(UNINSTALL_USER_FEATURES, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (FORCE_UNINSTALL.equals(key)) {
            if (value instanceof Boolean) {
                data.put(FORCE_UNINSTALL, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (ACTION_UNINSTALL.equals(key)) {
            if (value instanceof List) {
                data.put(ACTION_UNINSTALL, value);
                actionType = ActionType.uninstall;
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.FEATURES_TO_RESOLVE.equals(key)) {
            if (value instanceof List) {
                Boolean localESAInstall = (Boolean) data.get(InstallConstants.INSTALL_LOCAL_ESA);
                if (localESAInstall != null && localESAInstall) {
                    if ((List<File>) data.get(InstallConstants.SINGLE_JSON_FILE) != null) {
                        //resolution requires InstallConstants.INSTALL_LOCAL_ESA == true and a InstallConstants.SINGLE_JSON_FILE != null
                        actionType = ActionType.resolve;
                        data.put(InstallConstants.FEATURES_TO_RESOLVE, value);
                    } else {
                        throw new IllegalArgumentException();
                    }
                } else {
                    throw new IllegalArgumentException();
                }
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.SINGLE_JSON_FILE.equals(key)) {
            if (value instanceof List) {
                Boolean localESAInstall = (Boolean) data.get(InstallConstants.INSTALL_LOCAL_ESA);
                if (localESAInstall != null && localESAInstall) {
                    data.put(InstallConstants.SINGLE_JSON_FILE, value);
                } else {
                    throw new IllegalArgumentException();
                }
            } else {
                throw new IllegalArgumentException();
            }

        } else if (DIRECTORY_BASED_REPOSITORY.equals(key)) {
            if (value instanceof File) {
                data.put(DIRECTORY_BASED_REPOSITORY, value);
            }
        } else if (InstallConstants.INDIVIDUAL_ESAS.equals(key)) {
            if (value instanceof List) {
                data.put(InstallConstants.INDIVIDUAL_ESAS, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (LOCALLY_PRESENT_JSONS.equals(key)) {
            if (value instanceof List) {
                data.put(LOCALLY_PRESENT_JSONS, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.INSTALL_INDIVIDUAL_ESAS.equals(key)) {
            if (value instanceof Boolean) {
                data.put(InstallConstants.INSTALL_INDIVIDUAL_ESAS, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.USER_PUBLIC_KEYS.equals(key)) {
            if (value instanceof Collection) {
                data.put(InstallConstants.USER_PUBLIC_KEYS, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.ACTION_ERROR_MESSAGE.equals(key)) {
            if (value instanceof String) {
                data.put(InstallConstants.ACTION_ERROR_MESSAGE, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (InstallConstants.ACTION_EXCEPTION_STACKTRACE.equals(key)) {
            if (value instanceof String) {
                data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (key.equals("debug")) {
            if (value instanceof Level) {
                data.put("debug", value);
                ((InstallKernelImpl) installKernel).enableConsoleLog((Level) value);
            } else {
                throw new IllegalArgumentException();
            }
        }
        return data.get(key);
    }

    private InstallEventListener getListener() {
        if (ielistener == null) {
            ielistener = new InstallEventListener() {
                @SuppressWarnings({ "unchecked" })
                @Override
                public void handleInstallEvent(InstallProgressEvent event) throws Exception {
                    if (actionType != null) {
                        if (actionType.equals(ActionType.install)) {
                            List messages = (List) data.get(PROGRESS_MONITOR_MESSAGE);
                            if (messages != null) {
                                messages.add(event.message);
                                List cancelledList = (List) data.get(PROGRESS_MONITOR_CANCELLED);
                                if ((Boolean) cancelledList.get(0)) {
                                    throw new CancelException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("MSG_CANCEL_INSTALL"), CANCELLED.intValue());
                                }
                            }
                        } else if (actionType.equals(ActionType.uninstall)) {
                            if (event.state == InstallProgressEvent.COMPLETE) {
                                List messages = (List) data.get(PROGRESS_MONITOR_MESSAGE);
                                if (messages != null)
                                    messages.add(event.message);
                            }
                        }
                    }
                }
            };
        }
        return ielistener;
    }

    @SuppressWarnings("unchecked")
    private Integer initKernel() {
        File installDir = (File) data.get(InstallConstants.RUNTIME_INSTALL_DIR);
        Utils.setInstallDir(installDir);
        installKernel = InstallKernelFactory.getInteractiveInstance();
        String userAgent = (String) data.get(USER_AGENT);
        installKernel.setUserAgent(userAgent != null && !userAgent.isEmpty() ? userAgent : MAP_BASED_INSTALL_KERNEL);
        installKernel.setFirePublicAssetOnly(false);
        installKernel.addListener(getListener(), InstallConstants.EVENT_TYPE_PROGRESS);
        data.put(INSTALL_KERNEL_INIT_ERROR_MESSAGE, null);
        return OK;
    }

    @SuppressWarnings("unchecked")
    private void resolve(Object installObject) {
        data.put(InstallConstants.ACTION_INSTALL, null);
        data.put(InstallConstants.ACTION_RESULT, OK);
        data.put(ACTION_INSTALL_RESULT, null);
        data.put(InstallConstants.ACTION_ERROR_MESSAGE, null);
        data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, null);

        actionType = ActionType.resolve;
        if (installObject instanceof List) {
            List<String> assets = (List<String>) installObject;
            try {
                installKernel.resolve(assets, false);
                checkLicense();
                actionType = ActionType.install;
                data.put(InstallConstants.ACTION_INSTALL, installObject);
            } catch (InstallException e) {
                data.put(InstallConstants.ACTION_RESULT, ERROR);
                data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
                data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
            }
        } else if (installObject instanceof File) {
            File esaFile = (File) installObject;
            boolean isESA = ArchiveUtils.ArchiveFileType.ESA.isType(esaFile.getName());
            if (isESA) {
                try {
                    String feature = InstallUtils.getFeatureName(esaFile);
                    installKernel.resolve(feature, esaFile, InstallConstants.TO_USER);
                    checkLicense();
                    actionType = ActionType.install;
                    data.put(InstallConstants.ACTION_INSTALL, installObject);
                } catch (InstallException e) {
                    data.put(InstallConstants.ACTION_RESULT, ERROR);
                    data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
                    data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
                }
            } else {
                data.put(InstallConstants.ACTION_RESULT, ERROR);
                data.put(InstallConstants.ACTION_ERROR_MESSAGE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MAPBASED_ERROR_UNSUPPORTED_FILE", esaFile.getAbsoluteFile()));
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static boolean containsIgnoreCase(Collection<String> featureToInstall, String existingFeature) {
        for (String current : featureToInstall) {
            if (current.equalsIgnoreCase(existingFeature)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOpenLiberty() {
        try {
            for (ProductInfo productInfo : ProductInfo.getAllProductInfo().values()) {
                if (productInfo.getReplacedBy() == null && productInfo.getId().equals("io.openliberty")) {
                    return true;
                }

            }
        } catch (ProductInfoParseException e) {
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        } catch (DuplicateProductInfoException e) {
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        } catch (ProductInfoReplaceException e) {
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public Collection<String> singleFileResolve() {
        data.put(InstallConstants.ACTION_INSTALL, null);
        data.put(InstallConstants.ACTION_RESULT, OK);
        data.put(ACTION_INSTALL_RESULT, null);
        data.put(InstallConstants.ACTION_ERROR_MESSAGE, null);
        data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, null);

        Collection<ProductDefinition> productDefinitions = new HashSet<ProductDefinition>();
        Collection<List<RepositoryResource>> resolveResult = null;
        RepositoryResolver resolver = null;
        Collection<String> featuresResolved = new ArrayList<String>();
        boolean isOpenLiberty = false;
        openLibertyVersion = getLibertyVersion();
        try {
            for (ProductInfo productInfo : ProductInfo.getAllProductInfo().values()) {
                productDefinitions.add(new ProductInfoProductDefinition(productInfo));

                if (productInfo.getReplacedBy() == null && productInfo.getId().equals("io.openliberty")) {
                    isOpenLiberty = true;
                }

            }

            RepositoryConnectionList repoList = new RepositoryConnectionList();
            List<File> singleJsonRepos = (List<File>) data.get(InstallConstants.SINGLE_JSON_FILE);
            File directoryBasedRepo = (File) data.get(DIRECTORY_BASED_REPOSITORY);
            if (directoryBasedRepo != null) {
                RepositoryConnection directRepo = new DirectoryRepositoryConnection(directoryBasedRepo, ReadMode.ASSUME_UNCHANGED);
                repoList.add(directRepo);
            }

            File websphereJson = null;
            for (File jsonRepo : singleJsonRepos) {
                if (jsonRepo.getAbsolutePath().contains(WEBSPHERE_LIBERTY_GROUP_ID.replace(".", "/"))) {
                    websphereJson = jsonRepo;
                }
                RepositoryConnection repo = new SingleFileRepositoryConnection(jsonRepo);
                repoList.add(repo);
            }
            ManifestFileProcessor m_ManifestFileProcessor = new ManifestFileProcessor();
            Collection<ProvisioningFeatureDefinition> installedFeatures = m_ManifestFileProcessor.getFeatureDefinitions().values();

            int alreadyInstalled = 0;
            Collection<String> featureToInstall = (Collection<String>) data.get(InstallConstants.FEATURES_TO_RESOLVE);
            Map<String, String> shortNameMap = new HashMap<String, String>();
            if (data.get(InstallConstants.INSTALL_INDIVIDUAL_ESAS) != null) {
                try {
                    if (data.get(InstallConstants.INSTALL_INDIVIDUAL_ESAS).equals(Boolean.TRUE)) {
                        Path tempDir = Files.createTempDirectory("generatedJson");
                        tempDir.toFile().deleteOnExit();

                        File individualEsaJson = generateJsonFromIndividualESAs(tempDir, shortNameMap);

                        RepositoryConnection repo = new SingleFileRepositoryConnection(individualEsaJson);
                        repoList.add(repo);

                        List<String> shortNamesToInstall = new ArrayList<String>();
                        Iterator<String> it = featureToInstall.iterator();
                        while (it.hasNext()) {
                            String feature = it.next();
                            if (feature.endsWith(".esa") && shortNameMap.containsKey(feature)) {
                                it.remove();
                                shortNamesToInstall.add(shortNameMap.get(feature));
                            }
                        }
                        featureToInstall.addAll(shortNamesToInstall);
                    }
                } catch (NullPointerException e) {
                    data.put(InstallConstants.ACTION_RESULT, ERROR);
                    data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
                    data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
                }
            }

            boolean isInstallServerFeature = (Boolean) this.get(InstallConstants.IS_INSTALL_SERVER_FEATURE);
            if (!isInstallServerFeature) {
                Collection<String> featuresAlreadyPresent = new ArrayList<String>();
                for (ProvisioningFeatureDefinition feature : installedFeatures) {
                    if (containsIgnoreCase(featureToInstall, feature.getIbmShortName()) || featureToInstall.contains(feature.getFeatureName())) {
                        alreadyInstalled += 1;
                        if (feature.getIbmShortName() == null) {
                            featuresAlreadyPresent.add(feature.getFeatureName());
                        } else {
                            featuresAlreadyPresent.add(feature.getIbmShortName());
                        }
                    }
                }
                if (alreadyInstalled == featureToInstall.size()) {
                    throw ExceptionUtils.createByKey(InstallException.ALREADY_EXISTS, "ASSETS_ALREADY_INSTALLED", featuresAlreadyPresent);
                }
            }

            boolean isFeatureUtility = (Boolean) this.get(InstallConstants.IS_FEATURE_UTILITY);
            data.put(InstallConstants.UPGRADE_COMPLETE, false);
            if (isOpenLiberty && isFeatureUtility && websphereJson != null && upgradeRequired(websphereJson)) {
                upgradeOL(websphereJson);
                RepositoryConnection repo = new SingleFileRepositoryConnection(websphereJson);
                repoList.add(repo);
                for (ProductInfo productInfo : ProductInfo.getAllProductInfo().values()) {
                    productDefinitions.add(new ProductInfoProductDefinition(productInfo));
                }
                data.put(InstallConstants.UPGRADE_COMPLETE, true);
            }

            resolver = new RepositoryResolver(productDefinitions, installedFeatures, Collections.<IFixInfo> emptySet(), repoList);

            if (!isInstallServerFeature) {
                resolveResult = resolver.resolve((Collection<String>) data.get(InstallConstants.FEATURES_TO_RESOLVE));
            } else {
                resolveResult = resolver.resolveAsSet((Collection<String>) data.get(InstallConstants.FEATURES_TO_RESOLVE),
                                                      (Collection<String>) data.get(InstallConstants.PLATFORMS));
            }

            if (!resolveResult.isEmpty()) {
                for (List<RepositoryResource> item : resolveResult) {
                    for (RepositoryResource repoResrc : item) {
                        String ibmInstallTo = "";
                        if (repoResrc instanceof EsaResourceImpl) {
                            ibmInstallTo = ((EsaResourceImpl) repoResrc).getIBMInstallTo();
                        }

                        String license = repoResrc.getLicenseId();
                        if (license != null) {
                            // determine whether the runtime is ND
                            boolean isNDRuntime = false;
                            for (ProductInfo productInfo : ProductInfo.getAllProductInfo().values()) {
                                if ("com.ibm.websphere.appserver".equals(productInfo.getId()) && "ND".equals(productInfo.getEdition())) {
                                    isNDRuntime = true;
                                    break;
                                }
                            }

                            // determine whether the license should be auto accepted
                            boolean autoAcceptLicense = license.startsWith(LICENSE_EPL_PREFIX) || license.equals(LICENSE_FEATURE_TERMS)
                                                        || (isNDRuntime && license.equals(LICENSE_FEATURE_TERMS_RESTRICTED));

                            if (!autoAcceptLicense) {
                                // check whether the license has been accepted
                                Boolean accepted = (Boolean) data.get(InstallConstants.LICENSE_ACCEPT);
                                if (accepted == null || !accepted) {
                                    featuresResolved.clear(); // clear the result since the licenses were not accepted
                                    throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_LICENSES_NOT_ACCEPTED"));
                                }
                            }
                        }

                        if (repoResrc.getRepositoryConnection() instanceof DirectoryRepositoryConnection) {
                            featuresResolved.add(repoResrc.getId());
                        } else {
                            String featureCoord = repoResrc.getMavenCoordinates();
                            String featurePath = null;
                            if (featureCoord == null) { //if user specified ESA file path
                                if (repoResrc instanceof EsaResourceImpl) {
                                    String name = ((EsaResourceImpl) repoResrc).getShortName() == null ? repoResrc.getName() : ((EsaResourceImpl) repoResrc).getShortName();
                                    for (String k : shortNameMap.keySet()) {
                                        if (shortNameMap.get(k).equals(name)) {
                                            featuresResolved.add(k);
                                            featurePath = k;
                                        }
                                    }
                                }
                            } else {
                                featuresResolved.add(featureCoord);
                                featurePath = ArtifactDownloaderUtils.getfilename(featureCoord).toLowerCase() + ".esa";
                            }

                            if (ibmInstallTo == null || ibmInstallTo.equals("usr")) { //add both featurePath and featureCoord to consider install of   local esa features and features available in Maven repository.
                                usrFeatures.add(featurePath);
                                usrFeatures.add(featureCoord);
                            }
                        }
                    }
                }
            }
            if (data.get(InstallConstants.VERIFY_OPTION) != null && (VerifyOption) data.get(InstallConstants.VERIFY_OPTION) != VerifyOption.skip) {
                actionType = ActionType.verify;
            } else {
                actionType = ActionType.install;
            }
            featuresResolved = keepFirstInstance(featuresResolved);
            return featuresResolved;
        } catch (ProductInfoParseException e) {
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        } catch (DuplicateProductInfoException e) {
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        } catch (ProductInfoReplaceException e) {
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        } catch (RepositoryResolutionException e) {
            boolean isFeatureUtility = (Boolean) this.get(InstallConstants.IS_FEATURE_UTILITY);
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            InstallException ie = ExceptionUtils.create(e, e.getTopLevelFeaturesNotResolved(), (File) data.get(InstallConstants.RUNTIME_INSTALL_DIR), false, isOpenLiberty,
                                                        isFeatureUtility);
            if (ie != null) {
                data.put(InstallConstants.ACTION_ERROR_MESSAGE, ie.getMessage());
                data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(ie));
            }
        } catch (InstallException e) {
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        } catch (RepositoryException e) {
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        } catch (Exception e) {
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        }
        actionType = ActionType.install;
        return featuresResolved;
    }

    /**
     * @return
     * @throws InstallException
     */
    private boolean upgradeRequired(File websphereJson) throws InstallException {
        Collection<String> features = (Collection<String>) data.get(InstallConstants.FEATURES_TO_RESOLVE);
        boolean upgradeRequired = false;
        try (JsonReader reader = Json.createReader(new FileInputStream(websphereJson))) {
            JsonArray assetList = reader.readArray();
            int i = 0;
            int lstSize = assetList.size();
            while (i < lstSize && upgradeRequired == false) {
                if (assetList.get(i).getValueType() == ValueType.OBJECT) {
                    JsonObject featureObject = (JsonObject) assetList.get(i);
                    JsonObject wlpFeatureInfo = featureObject.getJsonObject("wlpInformation");
                    String lowerCaseShortName = wlpFeatureInfo.getString("lowerCaseShortName", null);
                    String name = featureObject.getString("name", null);
                    if (lowerCaseShortName != null && containsStr(lowerCaseShortName, features)) {
                        upgradeRequired = true;
                    } else if (name != null && containsStr(name, features)) {
                        upgradeRequired = true;
                    }
                }
                i = i + 1;
            }
        } catch (FileNotFoundException e) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FAILED_TO_FIND_WEBSPHERE_JSON", websphereJson.getAbsolutePath()));
        }
        return upgradeRequired;
    }

    /**
     * override the environmental variable values map
     *
     * @param overrideMap
     */
    public void overrideEnvMap(Map<String, Object> overrideMap) {
        logger.fine("envmap before:");
        if (overrideMap == null) {
            return;
        }
        if (envMap == null) {
            envMap = new HashMap<>();
        }
        logger.fine(this.envMap.toString());
        envMap.putAll(overrideMap);
        logger.fine("printing envmap after");

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Entry<String, Object> entry : envMap.entrySet()) {
            sb.append(entry.getKey());
            sb.append("=");
            //Encrypt proxy password
            sb.append(entry.getKey().contains(".proxyPassword") ? HIDDEN_PASSWORD : entry.getValue());
            sb.append(", ");
        }
        sb.append("}");
        logger.fine(sb.toString());

    }

    /**
     * set proxy jvm system properties
     * http.nonProxyHosts:a list of hosts that should be reached directly, bypassing the proxy. This is a list of patterns separated by '|'. The patterns may start or end with a
     * '*' for wildcards. Any host matching one of these patterns will be reached through a direct connection instead of through a proxy.
     */
    protected void setProxy() {
        //set up basic auth HTTP tunnel
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");

        try {
            if (envMap.get("https.proxyHost") != null) {
                checkValidProxy("https");
                System.setProperty("https.proxyHost", (String) envMap.get("https.proxyHost"));
                System.setProperty("https.proxyPort", (String) envMap.get("https.proxyPort"));
            }
            if (envMap.get("http.proxyHost") != null) {
                checkValidProxy("http");
                System.setProperty("http.proxyHost", (String) envMap.get("http.proxyHost"));
                System.setProperty("http.proxyPort", (String) envMap.get("http.proxyPort"));
                if (envMap.get("https.proxyHost") == null) {
                    System.setProperty("https.proxyHost", (String) envMap.get("http.proxyHost"));
                    System.setProperty("https.proxyPort", (String) envMap.get("http.proxyPort"));
                }
            }
            if (envMap.get("http.nonProxyHosts") != null) {
                String noProxyHosts = (String) envMap.get("http.nonProxyHosts");
                //if users provide list of hosts using ",", replace to "|"
                noProxyHosts = noProxyHosts.replace(",", "|");
                System.setProperty("http.nonProxyHosts", noProxyHosts);
            }
            logger.fine("http.proxyHost " + System.getProperty("http.proxyHost"));
            logger.fine("https.proxyHost " + System.getProperty("https.proxyHost"));
            logger.fine("proxy exclusion list: " + System.getProperty("http.nonProxyHosts"));
        } catch (InstallException e) {
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        }

    }

    /**
     * @param protocol
     * @throws InstallException
     */
    protected void checkValidProxy(String protocol) throws InstallException {
        String proxyPort = (String) envMap.get(protocol + ".proxyPort");
        if (protocol != null) {
            int proxyPortnum = Integer.parseInt(proxyPort);
            if (((String) envMap.get(protocol + ".proxyHost")).isEmpty()) {
                throw ExceptionUtils.createByKey("ERROR_TOOL_PROXY_HOST_MISSING");
            } else if (proxyPortnum < 0 || proxyPortnum > 65535) {
                throw ExceptionUtils.createByKey("ERROR_TOOL_INVALID_PROXY_PORT", proxyPort);
            } else if (envMap.get(protocol + "proxyUser") != null && (envMap.get(protocol + ".proxyPassword") == null
                                                                      || ((String) envMap.get(protocol + ".proxyPassword")).isEmpty())) {
                throw ExceptionUtils.createByKey("ERROR_TOOL_PROXY_PWD_MISSING");
            }
        }
    }

    private static Collection<String> keepFirstInstance(Collection<String> dupStrCollection) {
        Collection<String> uniqueStrCollection = new ArrayList<String>();
        for (String str : dupStrCollection) {
            if (!uniqueStrCollection.contains(str)) {
                uniqueStrCollection.add(str);
            }
        }
        return uniqueStrCollection;
    }

    private void checkLicense() throws InstallException {
        Set<InstallLicense> licenses = installKernel.getFeatureLicense(Locale.getDefault());
        if (!licenses.isEmpty()) {
            Boolean accepted = (Boolean) data.get(InstallConstants.LICENSE_ACCEPT);
            if (accepted == null || !accepted) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_LICENSES_NOT_ACCEPTED"));
            }
        }
    }

    private Integer getMonitorSize() {
        if (actionType != null) {
            if (actionType.equals(ActionType.install)) {
                int numInstallResources = installKernel.getInstallResourcesSize();
                int numInstallAssets = installKernel.getLocalInstallAssetsSize();
                return Integer.valueOf(numInstallResources * 2 + numInstallAssets + 1);
            } else if (actionType.equals(ActionType.uninstall)) {
                return Integer.valueOf(1);
            }
        }
        return Integer.valueOf(0);
    }

    @SuppressWarnings("unchecked")
    public Integer install() {
        data.put(InstallConstants.ACTION_RESULT, OK);
        data.put(ACTION_INSTALL_RESULT, null);
        data.put(InstallConstants.ACTION_ERROR_MESSAGE, null);
        data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, null);
        try {
            installKernel.checkResources();
            Boolean agreedToDownloadDependencies = (Boolean) data.get(DOWLOAD_EXTERNAL_DEPS);
            if (agreedToDownloadDependencies == null)
                agreedToDownloadDependencies = Boolean.TRUE;
            Map<String, Collection<String>> installedAssets = installKernel.install(InstallConstants.TO_USER, true, agreedToDownloadDependencies.booleanValue());
            data.put(ACTION_INSTALL_RESULT, installedAssets);
        } catch (CancelException e) {
            data.put(InstallConstants.ACTION_RESULT, CANCELLED);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
            return CANCELLED;
        } catch (InstallException e) {
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
            return ERROR;
        }
        return OK;
    }

    @SuppressWarnings("unchecked")
    public List<File> downloadFeatures(List<String> featureList, List<String> signatureList) {
        data.put(InstallConstants.ACTION_ERROR_MESSAGE, null);
        data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, null);

        try (ArtifactDownloader artifactDownloader = new ArtifactDownloader()) {
            String fromRepo = (String) data.get(InstallConstants.FROM_REPO);
            Boolean cleanupNeeded = (Boolean) data.get(InstallConstants.CLEANUP_NEEDED);
            String downloadDir;
            List<MavenRepository> repos = getMavenRepo(fromRepo);
            List<String> copyFeatureList = new ArrayList<>();
            List<String> copySignatureList = new ArrayList<>();
            VerifyOption verifyOption = (VerifyOption) data.get(InstallConstants.VERIFY_OPTION);

            if (cleanupNeeded != null && cleanupNeeded) {
                fine("Using temp location: " + TEMP_DIRECTORY);
                data.put(InstallConstants.CLEANUP_TEMP_LOCATION, TEMP_DIRECTORY);
                downloadDir = TEMP_DIRECTORY;
            } else {
                downloadDir = getDownloadDir((String) data.get(InstallConstants.DOWNLOAD_LOCATION));
            }

            copyFeatureList.addAll(featureList);
            copySignatureList.addAll(signatureList);
            artifactDownloader.setEnvMap(envMap);

            for (MavenRepository repo : repos) {
                try {
                    if (!copyFeatureList.isEmpty()) {
                        Set<String> missingFeatures = artifactDownloader.getMissingFeaturesFromRepo(copyFeatureList, usrFeatures, repo, verifyOption, false);
                        copyFeatureList.removeAll(missingFeatures);
                        artifactDownloader.synthesizeAndDownloadFeatures(copyFeatureList, usrFeatures, downloadDir, repo, verifyOption, false);
                        //Try downloading missing features from next repo
                        copyFeatureList = new ArrayList<>(missingFeatures);
                    }
                    if (!copySignatureList.isEmpty()) {
                        Set<String> missingSignatures = artifactDownloader.getMissingFeaturesFromRepo(copySignatureList, usrFeatures, repo, verifyOption, true);
                        copySignatureList.removeAll(missingSignatures);
                        artifactDownloader.synthesizeAndDownloadFeatures(copySignatureList, usrFeatures, downloadDir, repo, verifyOption, true);
                        //Try downloading missing signatures from next repo
                        copySignatureList = new ArrayList<>(missingSignatures);
                    }
                } catch (InstallException e) {
                    this.put(InstallConstants.ACTION_RESULT, ERROR);
                    this.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
                    this.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
                    return null;
                }

            }

            if (copyFeatureList != null && !copyFeatureList.isEmpty()) {
                this.put(InstallConstants.ACTION_RESULT, ERROR);
                this.put(InstallConstants.ACTION_ERROR_MESSAGE,
                         ExceptionUtils.createByKey("ERROR_FAILED_TO_DOWNLOAD_ASSETS_FROM_REPO", "required", "feature(s)", repos).getMessage());

                return null;
            }

            if (copySignatureList != null && !copySignatureList.isEmpty()) {
                if (verifyOption == VerifyOption.all) {
                    this.put(InstallConstants.ACTION_RESULT, ERROR);
                    this.put(InstallConstants.ACTION_ERROR_MESSAGE,
                             ExceptionUtils.createByKey("ERROR_FAILED_TO_DOWNLOAD_ASSETS_FROM_REPO", "required", "feature signatures(s)", repos).getMessage());
                    return null;
                }
            }

            fine("Downloaded the following feature signautres from the remote maven repository:" + artifactDownloader.getDownloadedAscs());

            return artifactDownloader.getDownloadedEsas(featureList);
        }
    }

    @SuppressWarnings("unchecked")
    public List<File> downloadArtifact() {
        data.put(InstallConstants.ACTION_ERROR_MESSAGE, null);
        data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, null);

        try (ArtifactDownloader artifactDownloader = new ArtifactDownloader()) {
            String fromRepo = (String) data.get(InstallConstants.FROM_REPO);
            Boolean cleanupNeeded = (Boolean) data.get(InstallConstants.CLEANUP_NEEDED);
            String downloadDir;
            if (cleanupNeeded != null && cleanupNeeded) {
                fine("Using temp location: " + TEMP_DIRECTORY);
                data.put(InstallConstants.CLEANUP_TEMP_LOCATION, TEMP_DIRECTORY);
                downloadDir = TEMP_DIRECTORY;
            } else {
                downloadDir = getDownloadDir((String) data.get(InstallConstants.DOWNLOAD_LOCATION));
            }
            String artifact = (String) this.get(DOWNLOAD_ARTIFACT_SINGLE);
            String filetype = (String) this.get(DOWNLOAD_FILETYPE);

            List<MavenRepository> repos = getMavenRepo(fromRepo);
            InstallException encounteredException = null;
            artifactDownloader.setEnvMap(envMap);
            for (MavenRepository repo : repos) {
                try {
                    artifactDownloader.synthesizeAndDownload(artifact, filetype, downloadDir, repo, true);
                    return artifactDownloader.getDownloadedFiles();
                } catch (InstallException e) {
                    fine(artifact + " not found on the following repo: " + repo);
                    encounteredException = e;
                }
            }

            this.put(InstallConstants.ACTION_RESULT, ERROR);
            if (encounteredException != null) {
                this.put(InstallConstants.ACTION_ERROR_MESSAGE, encounteredException.getMessage());
                this.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(encounteredException));
            }

            return null;

        }
    }

    /**
     * @param
     * @return
     */
    @SuppressWarnings("unchecked")
    private String getDownloadDir(String fromDir) {
        String result;

        if (fromDir != null) {
            result = fromDir;
        } else if ((fromDir = getM2Cache()) != null) {
            usingM2Cache = true;
            result = getM2Cache();
        } else if (checkM2Writable()) {
            File newM2 = new File(getM2Path().toString());
            result = newM2.toString();
        } else {
            fine("Using temp location: " + TEMP_DIRECTORY);
            data.put(InstallConstants.CLEANUP_TEMP_LOCATION, TEMP_DIRECTORY);
            data.put(InstallConstants.CLEANUP_NEEDED, true);
            result = TEMP_DIRECTORY;
        }
        return result;
    }

    private String getM2Cache() { //check for maven_home specified mirror stuff
        Path m2Path = getM2Path();
        if (Files.exists(m2Path) && Files.isWritable(m2Path)) {
            return m2Path.toString();
        }
        return null;

    }

    private Path getM2Path() {
//        return Paths.get(System.getProperty("user.home"), ".m2", "repository", "");
        return Paths.get(System.getProperty("user.home"), ".m2", "repository", "");

    }

    private boolean checkM2Writable() {
        String userhome = System.getProperty("user.home");
        Path userhomePath = Paths.get(userhome);
        if (!Files.exists(userhomePath) || !Files.isWritable(userhomePath)) {
            return false;
        }

        Path withM2 = Paths.get(userhome, "/.m2");
        Path withRepository = Paths.get(userhome, "/.m2/repository");

        if (Files.exists(withM2)) {
            if (Files.exists(withRepository)) {
                return Files.isWritable(withRepository);
            } else {
                return withRepository.toFile().mkdir();
            }
        } else if (withM2.toFile().mkdir()) { //create .m2 and recurse.
            return checkM2Writable();
        } else {
            return false;
        }
    }

    /**
     * @return
     */
    private String getRepo(String fromRepo) {
        String repo;

        if (envMap.get("FEATURE_REPO_URL") != null) {
            fine("Connecting to the following repository: " + envMap.get("FEATURE_REPO_URL"));
            repo = (String) envMap.get("FEATURE_REPO_URL");
        } else {
            fine("Connecting to the following repository: " + MAVEN_CENTRAL);
            repo = MAVEN_CENTRAL;
        }
        return repo;
    }

    /**
     * @return list of all working maven repository
     */
    private List<MavenRepository> getMavenRepo(String fromRepo) {
        // working repo should at least have one repo (maven central)
        if (workingRepos != null && !workingRepos.isEmpty()) {
            return workingRepos;
        }
        checkWorkingRepository();
        return workingRepos;
    }

    private void checkWorkingRepository() {
        List<MavenRepository> repositories = (List<MavenRepository>) envMap.get("FEATURE_UTILITY_MAVEN_REPOSITORIES");

        if (repositories != null) {
            try (ArtifactDownloader artifactDownloader = new ArtifactDownloader()) {
                artifactDownloader.setEnvMap(envMap);
                for (MavenRepository repository : repositories) {
                    logger.fine("Testing connection for repository: " + repository);
                    if (artifactDownloader.testConnection(repository)) {
                        workingRepos.add(repository);
                    }
                }
            }
        }

        if (workingRepos.isEmpty()) {
            workingRepos.add(MAVEN_CENTRAL_REPOSITORY);
        }
    }

    @SuppressWarnings("unchecked")
    public File downloadSingleFeature() {
        data.put(InstallConstants.ACTION_ERROR_MESSAGE, null);
        data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, null);

        try (ArtifactDownloader artifactDownloader = new ArtifactDownloader()) {
            String featureList = (String) data.get(InstallConstants.DOWNLOAD_ARTIFACT_LIST);
            String filetype = (String) data.get(DOWNLOAD_FILETYPE);

            String fromRepo = (String) data.get(InstallConstants.FROM_REPO);
            Boolean cleanupNeeded = (Boolean) data.get(InstallConstants.CLEANUP_NEEDED);
            String downloadDir;
            if (cleanupNeeded != null && cleanupNeeded) {
                fine("Using temp location: " + TEMP_DIRECTORY);
                data.put(InstallConstants.CLEANUP_TEMP_LOCATION, TEMP_DIRECTORY);
                downloadDir = TEMP_DIRECTORY;
            } else {
                downloadDir = getDownloadDir((String) data.get(InstallConstants.DOWNLOAD_LOCATION));
            }

            List<MavenRepository> repos = getMavenRepo(fromRepo);
            InstallException encounteredException = null;
            for (MavenRepository repo : repos) {
                try {
                    artifactDownloader.setEnvMap(envMap);
                    artifactDownloader.synthesizeAndDownload(featureList, filetype, downloadDir, repo, true);
                    return artifactDownloader.getDownloadedFiles().get(0);
                } catch (InstallException e) {
                    fine(featureList + " not found on the following repo: " + repo);
                    encounteredException = e;
                }
            }

            this.put(InstallConstants.ACTION_RESULT, ERROR);
            if (encounteredException != null) {
                this.put(InstallConstants.ACTION_ERROR_MESSAGE, encounteredException.getMessage());
                this.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(encounteredException));
            }

            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Integer localESAInstall() {
        data.put(InstallConstants.ACTION_RESULT, OK);
        data.put(ACTION_INSTALL_RESULT, null);
        data.put(InstallConstants.ACTION_ERROR_MESSAGE, null);
        data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, null);

        try {
            InstallKernelImpl installKernel = (InstallKernelImpl) this.installKernel;
            File esaFile = (File) data.get(InstallConstants.ACTION_INSTALL);
            String toExtension = (String) data.get(InstallConstants.TO_EXTENSION);
            if (toExtension == null) {
                toExtension = InstallConstants.TO_USER;
            }
            Collection<String> installedAssets = installKernel.installLocalFeatureNoResolve(esaFile.getAbsolutePath(), toExtension, true,
                                                                                            InstallConstants.ExistsAction.replace);
            data.put(ACTION_INSTALL_RESULT, installedAssets);
        } catch (InstallException e) {
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
            return ERROR;
        }
        return OK;
    }

    @SuppressWarnings("unchecked")
    private Integer uninstall() {
        List<String> uninstallFeatures = (data.containsKey(ACTION_UNINSTALL)) ? new ArrayList<String>((Collection<String>) data.get(ACTION_UNINSTALL)) : new ArrayList<String>();
        data.put(InstallConstants.ACTION_ERROR_MESSAGE, null);
        try {
            InstallKernel installKernel = (InstallKernel) this.installKernel;

            Boolean forceUninstall = (data.containsKey(FORCE_UNINSTALL)) ? (Boolean) data.get(FORCE_UNINSTALL) : Boolean.FALSE;
            Boolean allowUninstallUserFeatures = (data.containsKey(UNINSTALL_USER_FEATURES)) ? (Boolean) data.get(UNINSTALL_USER_FEATURES) : Boolean.FALSE;

            if (forceUninstall) {
                if (uninstallFeatures.size() > 1) {
                    data.put(InstallConstants.ACTION_ERROR_MESSAGE,
                             Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_INVALID_NUMBER_OF_FEATURES_FORCE_UNINSTALL"));
                    return ERROR;
                } else {
                    installKernel.uninstallFeaturePrereqChecking(uninstallFeatures.get(0), allowUninstallUserFeatures, forceUninstall);
                    installKernel.uninstallFeature(uninstallFeatures.get(0), forceUninstall);
                }
            } else {
                if (allowUninstallUserFeatures != null && allowUninstallUserFeatures)
                    installKernel.uninstallFeaturePrereqChecking(uninstallFeatures);
                else
                    installKernel.uninstallCoreFeaturePrereqChecking(uninstallFeatures);

                installKernel.uninstallFeature(uninstallFeatures);
            }
        } catch (InstallException e) {
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
            return ERROR;
        }
        return OK;
    }

    /**
     * Populate the feature name (short name if available, otherwise symbolic name) from the ESA's manifest into the shortNameMap.
     *
     * @param esa          ESA file
     * @param shortNameMap Map to populate with keys being ESA canonical paths and values being feature names (short name or symbolic name)
     * @throws IOException If the ESA's canonical path cannot be resolved or the ESA cannot be read
     */
    private static void populateFeatureNameFromManifest(File esa, Map<String, String> shortNameMap) throws IOException {
        String esaLocation = esa.getCanonicalPath();
        ZipFile zip = null;
        try {
            zip = new ZipFile(esaLocation);
            Enumeration<? extends ZipEntry> zipEntries = zip.entries();
            ZipEntry subsystemEntry = null;
            while (zipEntries.hasMoreElements()) {
                ZipEntry nextEntry = zipEntries.nextElement();
                if ("OSGI-INF/SUBSYSTEM.MF".equalsIgnoreCase(nextEntry.getName())) {
                    subsystemEntry = nextEntry;
                    break;
                }
            }
            if (subsystemEntry != null) {
                Manifest m = ManifestProcessor.parseManifest(zip.getInputStream(subsystemEntry));
                Attributes manifestAttrs = m.getMainAttributes();
                String featureName = manifestAttrs.getValue(SHORTNAME_HEADER_NAME);
                if (featureName == null) {
                    // Symbolic name field has ";" as delimiter between the actual symbolic name and other tokens such as visibility
                    featureName = manifestAttrs.getValue(SYMBOLICNAME_HEADER_NAME).split(";")[0];
                }
                shortNameMap.put(esa.getCanonicalPath(), featureName);
            }
        } finally {
            if (zip != null) {
                zip.close();
            }
        }
    }

    /**
     * generate a JSON from provided individual ESA files
     *
     * @param jsonDirectory path
     * @param shortNameMap  contains features parsed from individual esa files
     * @return singleJson file
     * @throws IOException
     * @throws RepositoryException
     * @throws InstallException
     */
    private File generateJsonFromIndividualESAs(Path jsonDirectory, Map<String, String> shortNameMap) throws IOException, RepositoryException, InstallException {
        String dir = jsonDirectory.toString();
        List<File> esas = (List<File>) data.get(InstallConstants.INDIVIDUAL_ESAS);
        File singleJson = new File(dir + File.separator + "SingleJson.json");

        return createJson(singleJson, shortNameMap, esas);

    }

    /**
     * generate a JSON from provided individual ESA files
     *
     * @param jsonDirectory path
     * @param shortNameMap  contains features parsed from individual esa files
     * @return singleJson file
     * @throws IOException
     * @throws RepositoryException
     * @throws InstallException
     */
    private File generateJsonFromESAList(Path jsonDirectory, Map<String, String> shortNameMap, List<File> esas) throws IOException, RepositoryException, InstallException {
        String dir = jsonDirectory.toString();
        File singleJson = new File(dir + File.separator + "SingleJson.json");

        return createJson(singleJson, shortNameMap, esas);

    }

    private File createJson(File singleJson, Map<String, String> shortNameMap, List<File> esas) throws InstallException, RepositoryException {

        for (File esa : esas) {
            try {
                populateFeatureNameFromManifest(esa, shortNameMap);
            } catch (IOException e) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_ESA_NOT_FOUND", esa.getAbsolutePath()));
            }

            SingleFileRepositoryConnection mySingleFileRepo = null;
            if (singleJson.exists()) {
                mySingleFileRepo = new SingleFileRepositoryConnection(singleJson);
            } else {
                try {
                    mySingleFileRepo = SingleFileRepositoryConnection.createEmptyRepository(singleJson);
                } catch (IOException e) {
                    throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_SINGLE_REPO_CONNECTION_FAILED", singleJson.toString(),
                                                                                              esa.getAbsolutePath()));
                }
            }
            Parser<? extends RepositoryResourceWritable> parser = new EsaParser(true);
            RepositoryResourceWritable resource = parser.parseFileToResource(esa, null, null);
            resource.updateGeneratedFields(true);
            resource.setRepositoryConnection(mySingleFileRepo);

            // Overload the Maven coordinates field with the file path, since the ESA should be installed from that path
            Map<File, String> fileToGroupIdMap = (Map<File, String>) data.get(GENERATE_JSON_GROUP_ID_MAP);
            if (fileToGroupIdMap != null) {
                resource.setMavenCoordinates(ArtifactDownloaderUtils.getMavenCoordFromPath(esa.getAbsolutePath(), fileToGroupIdMap.get(esa)));
            }
            resource.uploadToMassive(new AddThenDeleteStrategy());
        }

        return singleJson;
    }

    @SuppressWarnings("unchecked")
    private Collection<File> downloadEsas() {
        String fromRepo = getDownloadDir((String) data.get(InstallConstants.FROM_REPO));
        File rootDir = new File(fromRepo);
        Collection<String> resolvedFeatures = (List<String>) data.get(InstallConstants.DOWNLOAD_ARTIFACT_LIST);

        Map<String, List> artifactsMap = fetchArtifactsFromLocalRepository(rootDir, resolvedFeatures, ".esa");
        List<File> foundFeatures = artifactsMap.get("foundArtifacts");
        List<String> missingFeatures = artifactsMap.get("missingArtifacts");
        List<String> missingSignatures = artifactsMap.get("missingSignatures");
        if (foundFeatures.size() != resolvedFeatures.size() && !missingFeatures.isEmpty() || !missingSignatures.isEmpty()) {
            List<Integer> missingFeatureIndexes = artifactsMap.get("missingArtifactIndexes");
            List<File> downloadedFeatures = downloadFeatures(missingFeatures, missingSignatures);
            if (downloadedFeatures == null) {
                return null;
            } else {
                insertElementsIntoList(foundFeatures, downloadedFeatures, missingFeatureIndexes);
            }
            // some increment left over
            double increment = ((progressBar.getMethodIncrement("fetchArtifacts") / resolvedFeatures.size()) * downloadedFeatures.size());
            updateProgress(increment);
            fine("Downloaded the following features from the remote maven repository:" + downloadedFeatures);
        } else {
            data.put(InstallConstants.CLEANUP_NEEDED, false);
        }
        return foundFeatures;

    }

    /**
     * Returns a hashmap containing artifacts found as well as the order of the artifacts not found.
     * The found artifacts can be accessed using key: foundArtifacts
     * The missing artifacts can be accessed using key: missingArtifacts
     * The missing artifact indexes can be accessed using key: missingArtifactIndexes
     *
     * If the feature ESA is missing, then the maven coord will be added to missingArtifacts.
     * If the feature ESA exists, but not the signature, then the maven coord will be added to missingSignatures
     *
     * @param rootDir   the local maven repository
     * @param artifacts a list of artifacts in the form groupId:artifactId:version or esa file
     * @param extension file extension
     * @return a map containing the found and not found artifacts.
     */
    private Map<String, List> fetchArtifactsFromLocalRepository(File rootDir, Collection<String> artifacts, String extension) {
        List<File> foundArtifacts = new ArrayList<>();
        List<String> artifactsClone = new ArrayList<>(artifacts);
        List<Integer> missingArtifactIndexes = new ArrayList<>();
        List<String> missingSignatures = new ArrayList<>();
        String sigExtension = ".asc";
        int index = 0;
        double increment = (progressBar.getMethodIncrement("fetchArtifacts") / artifacts.size());
        VerifyOption verify = (VerifyOption) data.get(InstallConstants.VERIFY_OPTION);

        for (String artifact : artifacts) {
            fine("Processing artifact: " + artifact);
            Path artifactPath;
            Path signaturePath = null;
            if (isValidEsa(artifact)) {
                artifactPath = Paths.get(artifact);
            } else {
                String[] coord = artifact.split(":");
                String groupId = coord[0];
                String artifactName = coord[1];
                String version = coord[2];
                File groupDir = new File(rootDir, groupId.replace(".", "/"));
                if (!groupDir.exists()) {
                    missingArtifactIndexes.add(index);
                    index += 1;
                    continue;
                }

                String artifactFileName = artifactName + "-" + version + extension;
                artifactPath = Paths.get(groupDir.getAbsolutePath().toString(), artifactName, version, artifactFileName);

                if (verify != null) {
                    String signatureFileName = artifactFileName + sigExtension;
                    signaturePath = Paths.get(groupDir.getAbsolutePath().toString(), artifactName, version, signatureFileName);
                }
            }

            if (Files.isRegularFile(artifactPath)) {
                fine("Found Artifact at path: " + artifactPath.toString());
                foundArtifacts.add(artifactPath.toFile());
                artifactsClone.remove(artifact);
                //if verify is set, check if the signatures are downloaded.
                if (verify != null && verify != VerifyOption.skip && signaturePath != null) {
                    if (Files.isRegularFile(signaturePath)) {
                        fine("Found signature at path: " + signaturePath.toString());
                    } else {
                        missingSignatures.add(artifact);
                    }
                }
                updateProgress(increment);
            } else {
                missingArtifactIndexes.add(index);
            }

            index += 1;

        }
        Map<String, List> artifactsMap = new HashMap<>();
        artifactsMap.put("foundArtifacts", foundArtifacts);
        artifactsMap.put("missingArtifacts", artifactsClone);
        artifactsMap.put("missingArtifactIndexes", missingArtifactIndexes);
        artifactsMap.put("missingSignatures", missingSignatures);

        return artifactsMap;
    }

    private <T> void insertElementsIntoList(List<T> target, List<T> elements, List<Integer> indexes) {
        int index = 0;
        for (T obj : elements) {
            // insert the element into its respective position
            target.add(indexes.get(index), obj);
            index += 1;
        }

    }

    private boolean isValidEsa(String fileName) {
        return ArchiveUtils.ArchiveFileType.ESA.isType(fileName);
    }

    /**
     * Get the open liberty runtime version.
     *
     * @throws IOException
     * @throws InstallException
     *
     */
    @SuppressWarnings("unchecked")
    private String getLibertyVersion() {
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

        } catch (IOException e) {
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        }

        if (openLibertyVersion == null) {
            // openliberty.properties file is missing or invalidly formatted
            InstallException ie = new InstallException("Could not determine the open liberty runtime version.");
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, ie.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(ie));

        }
        this.openLibertyVersion = openLibertyVersion;
        return openLibertyVersion;

    }

    // TODO make these methods private, hiding them behind map.put and map.get
    public List<File> getLocalJsonFiles(Set<String> jsonsRequired) throws InstallException {
        String fromRepo = getDownloadDir((String) data.get(InstallConstants.DOWNLOAD_LOCATION));
        File fromDir = new File(fromRepo);
        List<File> jsons = new ArrayList<>();
        List<String> foundJsons = new ArrayList<String>();
        for (String jsonCoord : jsonsRequired) {
            String[] coords = jsonCoord.split(":");
            String groupId = coords[0];
            String artifactId = coords[1];
            String version = coords[2];
            String mavenCoordinateDirectory = groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/";
            String jsonFilePath = mavenCoordinateDirectory + "features-" + version + ".json";
            File foundJson = new File(fromDir, jsonFilePath);

            if (foundJson.exists()) {
                jsons.add(foundJson);
                foundJsons.add(jsonCoord);
            }

        }
        this.put(LOCALLY_PRESENT_JSONS, foundJsons);
        return jsons;

    }

    /**
     *
     * @param jsonsRequired a list of group id's for which a JSON file is required, i.e [io.openliberty.features, com.ibm.websphere.appserver.features]
     * @return list of the downloaded JSON files
     * @throws InstallException
     */
    @SuppressWarnings("unchecked")
    public List<File> getJsonsFromMavenCentral(Set<String> jsonsRequired) throws InstallException {
        // get open liberty json
        List<File> result = new ArrayList<File>();
        List<String> jsonsNotFound = new ArrayList<>();
        this.put(DOWNLOAD_FILETYPE, "json");
        boolean singleArtifactInstall = true;
        this.put(InstallConstants.DOWNLOAD_INDIVIDUAL_ARTIFACT, singleArtifactInstall);
        for (String jsonCoord : jsonsRequired) {
            this.put(DOWNLOAD_ARTIFACT_SINGLE, jsonCoord);
            this.put(InstallConstants.DOWNLOAD_ARTIFACT_LIST, jsonCoord);

            Object downloaded = this.get(InstallConstants.DOWNLOAD_RESULT);
            String exceptionMessage = (String) this.get("action.error.message");
            if (exceptionMessage != null) {
                if (!exceptionMessage.contains("CWWKF1285E")) { //ERROR_FAILED_TO_DOWNLOAD_ASSETS_FROM_REPO.. will throw json not found error below.
                    fine("action.exception.stacktrace: " + this.get(InstallConstants.ACTION_EXCEPTION_STACKTRACE));
                    throw new InstallException(exceptionMessage);
                }
            }
            if (downloaded == null) {
                fine("Could not download this json with maven coordinate: " + jsonCoord);
                jsonsNotFound.add(jsonCoord);
            } else if (downloaded instanceof List) {
                if (((List) downloaded).isEmpty()) {
                    jsonsNotFound.add(jsonCoord);
                } else {
                    result.addAll((List<File>) downloaded);
                }
            } else if (downloaded instanceof File) {
                if (!((File) downloaded).exists()) {
                    jsonsNotFound.add(jsonCoord);
                } else {
                    result.add((File) downloaded);
                }
            }

        }
        fine("Downloaded the following json files from remote: " + result);

        if (!jsonsNotFound.isEmpty()) {
            InstallException ie = new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_FAILED_TO_LOCATE_AND_DOWNLOAD_JSONS", jsonsNotFound));
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, ie.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(ie));
        }

        return result;

    }

    private Map<String, Object> getEnvMap() {
        Map<String, Object> envMapRet = new HashMap<String, Object>();
        //parse through httpProxy env variables
        String proxyEnvVarHttp = System.getenv("http_proxy");
        if (proxyEnvVarHttp != null) {
            Map<String, String> httpProxyVariables;
            try {
                httpProxyVariables = getProxyVariables(proxyEnvVarHttp, "http");
            } catch (InstallException e) {
                data.put(InstallConstants.ACTION_RESULT, ERROR);
                data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
                data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
                return null;
            }
            Set<String> httpProxyVarKeys = httpProxyVariables.keySet();
            for (String key : httpProxyVarKeys) {
                envMapRet.put(key, httpProxyVariables.get(key));
            }
        }

        String proxyEnvVarHttps = System.getenv("https_proxy");
        if (proxyEnvVarHttps != null) {
            Map<String, String> httpsProxyVariables;
            try {
                httpsProxyVariables = getProxyVariables(proxyEnvVarHttps, "https");
            } catch (InstallException e) {
                data.put(InstallConstants.ACTION_RESULT, ERROR);
                data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
                data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
                return null;
            }
            Set<String> httpsProxyVarKeys = httpsProxyVariables.keySet();
            for (String key : httpsProxyVarKeys) {
                envMapRet.put(key, httpsProxyVariables.get(key));
            }
        }

        envMapRet.put("http.nonProxyHosts", System.getenv("no_proxy"));
        envMapRet.put("FEATURE_REPO_URL", System.getenv("FEATURE_REPO_URL"));
        envMapRet.put("FEATURE_REPO_USER", System.getenv("FEATURE_REPO_USER"));
        envMapRet.put("FEATURE_REPO_PASSWORD", System.getenv("FEATURE_REPO_PASSWORD"));
        List<MavenRepository> repos = new ArrayList<>();
        if (System.getenv("FEATURE_REPO_URL") != null) {
            repos.add(new MavenRepository("Environment Variables Repo", System.getenv("FEATURE_REPO_URL"), System.getenv("FEATURE_REPO_USER"), System.getenv("FEATURE_REPO_PASSWORD")));
        }
        envMapRet.put("FEATURE_UTILITY_MAVEN_REPOSITORIES", repos);

        envMapRet.put("FEATURE_LOCAL_REPO", System.getenv("FEATURE_LOCAL_REPO"));

        envMapRet.put("FEATURE_VERIFY", System.getenv("FEATURE_VERIFY"));

        //search through the properties file to look for overrides if they exist
        //TODO remove? - Do we use featureUtility.env ?
        Map<String, String> propsFileMap = getFeatureUtilEnvProps();
        if (!propsFileMap.isEmpty()) {
            fine("The properties found in featureUtility.env will override latent environment variables of the same name");
            info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_READING_ENV_PROPS_FILE", Utils.getInstallDir().toString()));
            Set<String> keys = propsFileMap.keySet();
            for (String key : keys) {
                //if key is http_proxy or https_proxy then call getProxyVariables
                if (key.equals("http_proxy") || key.equals("https_proxy")) {
                    Map<String, String> proxyVar;
                    try {
                        proxyVar = getProxyVariables(propsFileMap.get(key), key.split("_")[0]);
                    } catch (InstallException e) {
                        data.put(InstallConstants.ACTION_RESULT, ERROR);
                        data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
                        data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
                        return null;
                    }
                    Set<String> proxyVarKeys = proxyVar.keySet();
                    for (String k : proxyVarKeys) {
                        envMapRet.put(k, proxyVar.get(k));
                    }
                } else {
                    envMapRet.put(key, propsFileMap.get(key));
                }
            }
            if (propsFileMap.containsKey("FEATURE_LOCAL_REPO")) {
                envMapRet.put("FEATURE_LOCAL_REPO", propsFileMap.get("FEATURE_LOCAL_REPO"));
            }
            String url = propsFileMap.get("FEATURE_REPO_URL");
            String user = propsFileMap.get("FEATURE_REPO_USER");
            String pass = propsFileMap.get("FEATURE_REPO_PASSWORD");
            if (url != null) {
                MavenRepository repo = new MavenRepository("featureUtility.env repo", url, user, pass);
                repos = new ArrayList<>();
                repos.add(repo);
                envMapRet.put("FEATURE_UTILITY_MAVEN_REPOSITORIES", repos);
            }

        }

        return envMapRet;
    }

    /**
     * parses the following format for http/https proxy environment variables:
     * https://user:password@proxy-server:3128
     * http://proxy-server:3128
     * and returns a map with proxyUser, proxyHost, proxyPort, and proxyPassword
     *
     * @param string
     * @return
     * @throws InstallException
     */
    private Map<String, String> getProxyVariables(String proxyEnvVar, String protocol) throws InstallException {
        Map<String, String> result = new HashMap<String, String>();

        String[] proxyEnvVarSplit = proxyEnvVar.split("@");
        if (proxyEnvVarSplit.length != 1 && proxyEnvVarSplit.length != 2) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_IMPROPER_" + protocol.toUpperCase() + "PROXY_FORMAT", proxyEnvVar));
        }

        if (proxyEnvVarSplit.length == 1) { //without username and password
            String[] proxyHttpSplit = proxyEnvVar.split(":");
            if (proxyHttpSplit.length != 3) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_IMPROPER_" + protocol.toUpperCase() + "PROXY_FORMAT", proxyEnvVar));
            }
            result.put(protocol + ".proxyHost", proxyHttpSplit[1].replace("/", "")); //proxy-server
            result.put(protocol + ".proxyPort", proxyHttpSplit[2]); ////3128
        } else {
            String[] proxyCredentials = proxyEnvVarSplit[0].split(":"); //[http(s), //user, password]
            if (proxyCredentials.length != 3) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_IMPROPER_HTTPSPROXY_FORMAT", proxyEnvVar));
            }
            String[] proxyHostPort = proxyEnvVarSplit[1].split(":"); //[prox-server, 3128]
            if (proxyHostPort.length != 2) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_IMPROPER_HTTPSPROXY_FORMAT", proxyEnvVar));
            }
            result.put(protocol + ".proxyHost", proxyHostPort[0]); //prox-server
            result.put(protocol + ".proxyPort", proxyHostPort[1]); //3128
            result.put(protocol + ".proxyUser", proxyCredentials[1].replace("/", "")); //user
            result.put(protocol + ".proxyPassword", proxyCredentials[2]); //password
        }

        return result;
    }

    /**
     * @return
     */
    private Map<String, String> getFeatureUtilEnvProps() {
        File featureUtilEnvFile = new File(ETC_DIRECTORY + FEATURE_UTILITY_PROPS_FILE);
        Map<String, String> propEnvMap = new HashMap<String, String>();

        try {
            Scanner scanner = new Scanner(featureUtilEnvFile);
            fine("featureUtility.env exists");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] splitLine = line.split("=");
                propEnvMap.put(splitLine[0], splitLine[1]);
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            //fine("featureUtility.env not found");
        }

        return propEnvMap;
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
//        if (isWindows) {
//            logger.fine(msg);
//        } else {
//            progressBar.clearProgress(); // Erase line content
        logger.fine(msg);
//            progressBar.display();
//        }
    }

    private void severe(String msg) {
//        if (isWindows) {
//            logger.severe(msg);
//        } else {
//            System.out.print("\033[2K"); // Erase line content
//        progressBar.clearProgress(); // Erase line content
        logger.severe(msg);
//        progressBar.display();
//        }

    }

    private void upgradeOL(File websphereJson) throws InstallException {
        //to get to here the kernel must be OL and we must be attempting to install non OL/usr features
        List<String> featureList = (List<String>) data.get(InstallConstants.FEATURES_TO_RESOLVE);
        String fromRepo = getDownloadDir((String) data.get(InstallConstants.FROM_REPO));
        File rootDir = new File(fromRepo);
        String licenseCoord = getLicenseToUpgrade(fromRepo, featureList, websphereJson);
        fine("licenseCoord to upgrade to: " + licenseCoord);
        //download that license zip if it isn't in the repo and unpack it to the license folder
        Collection<String> upgradeFileObjects = new ArrayList<String>();

        upgradeFileObjects.add(licenseCoord);

        Map<String, List> artifactsMap = fetchArtifactsFromLocalRepository(rootDir, upgradeFileObjects, ".zip");
        fine("missing license files: " + artifactsMap.get("missingArtifacts").toString());
        fine("found license files: " + artifactsMap.get("foundArtifacts").toString());

        if (!artifactsMap.get("missingArtifacts").isEmpty()) {
            //if license related object is in missing artifacts then we have to go download it
            this.put(DOWNLOAD_ARTIFACT_SINGLE, licenseCoord);
            this.put(DOWNLOAD_FILETYPE, "zip");
            //download artifact to downloadDir
            downloadArtifact();
        } else {
            data.put(InstallConstants.CLEANUP_NEEDED, false);
        }

        Boolean cleanupNeeded = (Boolean) data.get(InstallConstants.CLEANUP_NEEDED);
        String downloadDir;
        if (cleanupNeeded != null && cleanupNeeded) {
            fine("Using temp location: " + TEMP_DIRECTORY);
            data.put(InstallConstants.CLEANUP_TEMP_LOCATION, TEMP_DIRECTORY);
            downloadDir = TEMP_DIRECTORY;
        } else {
            downloadDir = getDownloadDir((String) data.get(InstallConstants.DOWNLOAD_LOCATION));
        }

        try {
            unpackLicenseObject(downloadDir, licenseCoord, WLP_DIR);
        } catch (IOException e) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_FAILED_TO_UNPACK_LICENSE", licenseCoord, downloadDir.toString()));
        }
    }

    /**
     * @param fromRepo
     * @param featureList
     * @return
     * @throws InstallException
     */
    private String getLicenseToUpgrade(String fromRepo, List<String> featureList, File websphereJson) throws InstallException {
        String result = null;
        String baseLicenseCoord = WEBSPHERE_LIBERTY_GROUP_ID + ":wlp-base-license:" + openLibertyVersion;
        String NDLicenseCoord = WEBSPHERE_LIBERTY_GROUP_ID + ":wlp-nd-license:" + openLibertyVersion;
        Set<String> minimalApplicableLicenses = getMinLicenses(fromRepo, featureList, baseLicenseCoord, websphereJson);
        fine("featurelist: " + featureList.toString());
        fine("minlicenses: " + minimalApplicableLicenses.toString());
        if (containsStr(NDLicenseCoord, minimalApplicableLicenses)) {
            result = NDLicenseCoord;
        } else {
            result = baseLicenseCoord;
        }
        return result;
    }

    /**
     * @param fromRepo
     * @param featureList
     * @return
     * @throws InstallException
     */
    private Set<String> getMinLicenses(String fromRepo, List<String> featureList, String defaultLicense, File websphereJson) throws InstallException {
        fine("parsing websphere json for minimal license coordinates");
        String baseLicenseCoord = WEBSPHERE_LIBERTY_GROUP_ID + ":wlp-base-license:" + openLibertyVersion;
        String NDLicenseCoord = WEBSPHERE_LIBERTY_GROUP_ID + ":wlp-nd-license:" + openLibertyVersion;
        boolean isND = false;
        Set<String> result = new HashSet<String>();
        List<String> causedUpgradeND = new ArrayList<String>();
        List<String> causedUpgradeBASE = new ArrayList<String>();
        try (JsonReader reader = Json.createReader(new FileInputStream(websphereJson))) {
            JsonArray assetList = reader.readArray();
            for (JsonValue val : assetList) {
                if (val.getValueType() == ValueType.OBJECT) {
                    JsonObject featureObject = (JsonObject) val;
                    JsonObject wlpFeatureInfo = featureObject.getJsonObject("wlpInformation");
                    String lowerCaseShortName = wlpFeatureInfo.getString("lowerCaseShortName", null);
                    String name = featureObject.getString("name", null);
                    String licenseMavenCoordinate = wlpFeatureInfo.getString("licenseMavenCoordinate", defaultLicense);
                    if (lowerCaseShortName != null && containsStr(lowerCaseShortName, featureList)) {
                        if (licenseMavenCoordinate.contains(NDLicenseCoord)) {
                            causedUpgradeND.add(lowerCaseShortName);
                            isND = true;
                        } else {
                            causedUpgradeBASE.add(lowerCaseShortName);
                        }
                        result.add(licenseMavenCoordinate);
                    } else if (name != null && containsStr(name, featureList)) {
                        result.add(licenseMavenCoordinate);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_MAVEN_JSON_NOT_FOUND", WEBSPHERE_LIBERTY_GROUP_ID));
        }
        if (isND) {
            data.put(InstallConstants.CAUSED_UPGRADE, causedUpgradeND);
        } else {
            data.put(InstallConstants.CAUSED_UPGRADE, causedUpgradeBASE);
        }
        return result;
    }

    /**
     * returns true if str is in list
     *
     * @param str
     * @param list
     */
    private boolean containsStr(String str, Collection<String> cl) {
        for (String s : cl) {
            if (s.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param downloadDir
     * @param licenseCoord
     * @param targetDir
     * @throws IOException
     */
    private void unpackLicenseObject(String downloadDir, String licenseCoord, String targetDir) throws IOException {
        String groupId = ArtifactDownloaderUtils.getGroupId(licenseCoord).replace(".", "/") + "/";
        String artifactId = ArtifactDownloaderUtils.getartifactId(licenseCoord);
        String version = ArtifactDownloaderUtils.getVersion(licenseCoord);
        if (!downloadDir.endsWith(File.separator)) {
            downloadDir += File.separator;
        }
        String filename = ArtifactDownloaderUtils.getfilename(licenseCoord) + ".zip";
        File zipFile = new File(downloadDir + groupId + artifactId + "/" + version + "/" + filename);

        FileInputStream fis = new FileInputStream(zipFile);
        ZipInputStream zis = new ZipInputStream(fis);
        try {
            ZipEntry ze = zis.getNextEntry();
            byte[] buf = new byte[2048];
            while (ze != null) {
                if (ze.isDirectory()) {
                    File dir = new File(targetDir + ze.getName());
                    dir.mkdirs();
                } else {
                    File unzippedFile = new File(targetDir + ze.getName());
                    upgradeFiles.add(unzippedFile);
                    FileOutputStream fos = new FileOutputStream(unzippedFile);
                    try {
                        int numBytes;
                        while ((numBytes = zis.read(buf)) > 0) {
                            fos.write(buf, 0, numBytes);
                        }
                    } finally {
                        fos.close();
                    }
                }
                ze = zis.getNextEntry();
            }
        } finally {
            zis.closeEntry();
            zis.close();
        }
    }

    private boolean cleanupUpgrade() {
        boolean cleanupSuccess = true;
        for (File f : upgradeFiles) {
            if (f.exists()) {
                File parent = f.getParentFile();
                if (f.delete() && parent.isDirectory()) {
                    parent.delete();
                }
            }
        }
        return cleanupSuccess;
    }

    /**
     * @param targetJsonDir
     * @param shortNameMap
     * @throws InstallException
     * @throws RepositoryException
     * @throws IOException
     */
    private File generateJson() {
        Map<String, String> shortNameMap = new HashMap<String, String>();
        File targetJsonDir = (File) data.get(TARGET_JSON_DIR);
        File result = null;
        try {
            result = generateJsonFromIndividualESAs(targetJsonDir.toPath(), shortNameMap);
        } catch (IOException e) {
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        } catch (RepositoryException e) {
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        } catch (InstallException e) {
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        }
        return result;
    }

    public File generateJson(File targetJsonDir, List<File> esas) throws IOException, RepositoryException, InstallException {
        Map<String, String> shortNameMap = new HashMap<String, String>();
        File result = generateJsonFromESAList(targetJsonDir.toPath(), shortNameMap, esas);
        return result;
    }

    private List<File> downloadPublicKeys() {
        VerifySignatureUtility verifyUtility = new VerifySignatureUtility();
        VerifyOption verify = (VerifyOption) data.get(InstallConstants.VERIFY_OPTION);
        Collection<Map<String, String>> usrkeys = (Collection<Map<String, String>>) data.get(InstallConstants.USER_PUBLIC_KEYS);
        try {
            pubKeys = verifyUtility.downloadPublicKeys(usrkeys, verify, envMap);
        } catch (InstallException e) {
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        }
        return pubKeys;
    }

    private Integer verifySignatures() {
        VerifyOption verifyOption = (VerifyOption) data.get(InstallConstants.VERIFY_OPTION);
        List<File> failedFeatures = new ArrayList<>();

        try {
            VerifySignatureUtility verifyUtility = new VerifySignatureUtility();
            verifyUtility.verifySignatures((List<File>) data.get(InstallConstants.ACTION_VERIFY), pubKeys, failedFeatures);
            if (failedFeatures.isEmpty()) {
                info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("TOOL_FEATURES_VERIFICATION_COMPLETED"));
                actionType = ActionType.install;
                return OK;
            } else {
                if (verifyOption == VerifyOption.all) {
                    data.put(InstallConstants.ACTION_RESULT, ERROR);
                    data.put(InstallConstants.ACTION_ERROR_MESSAGE, Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_FAILED_TO_VERIFY_FEATURE_SIGNATURE", failedFeatures));
                    return ERROR;
                } else if (verifyOption == VerifyOption.enforce) {
                    boolean libertyFeatureFailure = false;
                    //check if the failed feature is user feature
                    for (File f : failedFeatures) {
                        if (!(usrFeatures.contains(f.getName().toLowerCase()) || usrFeatures.contains(f.getAbsolutePath()))) {
                            libertyFeatureFailure = true;
                        }
                    }
                    if (libertyFeatureFailure) {
                        data.put(InstallConstants.ACTION_RESULT, ERROR);
                        data.put(InstallConstants.ACTION_ERROR_MESSAGE, Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_FAILED_TO_VERIFY_FEATURE_SIGNATURE", failedFeatures));
                        return ERROR;
                    }
                } else {
                    logger.warning(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_FAILED_TO_VERIFY_FEATURE_SIGNATURE", failedFeatures));
                }
                actionType = ActionType.install;
                return OK;

            }
        } catch (InstallException e) {
            data.put(InstallConstants.ACTION_RESULT, ERROR);
            data.put(InstallConstants.ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(InstallConstants.ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        }

        return OK;

    }

}