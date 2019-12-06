/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

import org.apache.aries.util.manifest.ManifestProcessor;

import com.ibm.ws.install.CancelException;
import com.ibm.ws.install.InstallConstants;
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
import com.ibm.ws.repository.resources.writeable.RepositoryResourceWritable;
import com.ibm.ws.repository.strategies.writeable.AddThenDeleteStrategy;

/**
 *
 */
@SuppressWarnings("rawtypes")
public class InstallKernelMap implements Map {

    private static final String MAP_BASED_INSTALL_KERNEL = "mapBasedInstallKernel";

    // Keys
    private static final String RUNTIME_INSTALL_DIR = "runtime.install.dir";
    private static final String LICENSE_ACCEPT = "license.accept";
    private static final String REPOSITORIES_PROPERTIES = "repositories.properties";
    private static final String DOWLOAD_EXTERNAL_DEPS = "dowload.external.deps";
    private static final String INSTALL_LOCAL_ESA = "install.local.esa";
    private static final String LOCAL_ESA_DOWNLOAD_DIR = "local.esa.download.dir";
    private static final String USER_AGENT = "user.agent";
    private static final String PROGRESS_MONITOR_MESSAGE = "progress.monitor.message";
    private static final String PROGRESS_MONITOR_CANCELLED = "progress.monitor.cancelled";
    private static final String PROGRESS_MONITOR_SIZE = "progress.monitor.size";
    private static final String TARGET_USER_DIRECTORY = "target.user.directory";
    private static final String INSTALL_KERNEL_INIT_CODE = "install.kernel.init.code";
    private static final String INSTALL_KERNEL_INIT_ERROR_MESSAGE = "install.kernel.init.error.message";
    private static final String UNINSTALL_USER_FEATURES = "uninstall.user.features";
    private static final String FORCE_UNINSTALL = "force.uninstall";
    private static final String ACTION_INSTALL = "action.install";
    private static final String ACTION_INSTALL_RESULT = "action.install.result";
    private static final String ACTION_UNINSTALL = "action.uninstall";
    private static final String ACTION_RESULT = "action.result";
    private static final String ACTION_ERROR_MESSAGE = "action.error.message";
    private static final String ACTION_EXCEPTION_STACKTRACE = "action.exception.stacktrace";
    private static final String TO_EXTENSION = "to.extension";
    private static final String DIRECTORY_BASED_REPOSITORY = "directory.based.repository";
    private static final String FEATURES_TO_RESOLVE = "features.to.resolve";
    private static final String SINGLE_JSON_FILE = "single.json.file";
    private static final String MESSAGE_LOCALE = "message.locale";
    private static final String INSTALL_INDIVIDUAL_ESAS = "install.individual.esas";
    private static final String INDIVIDUAL_ESAS = "individual.esas";
    private static final String DOWNLOAD_RESULT = "download.result";
    private static final String DOWNLOAD_INDIVIDUAL_ARTIFACT = "download.individual.artifact";
    private static final String DOWNLOAD_FILETYPE = "download.filetype";
    private static final String DOWNLOAD_LOCAL_DIR_LOCATION = "download.local.dir.location";
    private static final String DOWNLOAD_ARTIFACT_SINGLE = "download.artifact.single";
    private static final String DOWNLOAD_ARTIFACT_LIST = "download.artifact.list";
    private static final String DOWNLOAD_LOCATION = "download.location";
    private static final String FROM_REPO = "from.repo";
    private static final String CLEANUP_TEMP_LOCATION = "cleanup.temp.location";
    private static final String CLEANUP_NEEDED = "cleanup.needed";
    private static final String ENVIRONMENT_VARIABLE_MAP = "environment.variable.map";

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
    private final String JSON_ARTIFACT_ID = "features";
    private final String OPEN_LIBERTY_PRODUCT_ID = "io.openliberty";
    private final String MAVEN_CENTRAL = "http://repo.maven.apache.org/maven2/";
    private final String TEMP_DIRECTORY = Utils.getInstallDir().getAbsolutePath() + File.separator + "tmp"
                                          + File.separator;
    private static final String ETC_DIRECTORY = Utils.getInstallDir().getAbsolutePath() + File.separator + "etc"
                                                + File.separator;
    private static final String FEATURE_UTILITY_PROPS_FILE = "featureUtility.env";
    private Map<String, String> envMap = null;

    private enum ActionType {
        install,
        uninstall,
        resolve
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
        data.put(LICENSE_ACCEPT, Boolean.FALSE);
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
        if (INSTALL_KERNEL_INIT_CODE.equals(key)) {
            return initKernel();
        } else if (ACTION_RESULT.equals(key)) {
            if (actionType.equals(ActionType.install)) {
                Boolean localESAInstall = (Boolean) data.get(INSTALL_LOCAL_ESA);
                if (localESAInstall == null || !localESAInstall) {
                    return install();
                } else {
                    return localESAInstall();
                }
            } else if (actionType.equals(ActionType.uninstall)) {
                return uninstall();
            } else if (actionType.equals(ActionType.resolve)) {
                Boolean localESAInstall = (Boolean) data.get(INSTALL_LOCAL_ESA);
                if (localESAInstall == null || !localESAInstall) {
                    return data.get(ACTION_RESULT);
                } else {
                    return singleFileResolve();
                }
            }
        } else if (PROGRESS_MONITOR_SIZE.equals(key)) {
            return getMonitorSize();
        } else if (DOWNLOAD_RESULT.equals(key)) {
            Boolean downloadSingleArtifact = (Boolean) data.get(DOWNLOAD_INDIVIDUAL_ARTIFACT);

            if (downloadSingleArtifact != null && downloadSingleArtifact) {
                return downloadArtifact();
            } else {
                return downloadEsas();
            }
        } else if (ENVIRONMENT_VARIABLE_MAP.equals(key)) {
            envMap = getEnvMap();
            return envMap;
        }
        return data.get(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object put(Object key, Object value) {
        if (LICENSE_ACCEPT.equals(key)) {
            if (value instanceof Boolean) {
                data.put(LICENSE_ACCEPT, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (RUNTIME_INSTALL_DIR.equals(key)) {
            if (value instanceof File) {
                data.put(RUNTIME_INSTALL_DIR, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (LOCAL_ESA_DOWNLOAD_DIR.equals(key)) {
            if (value instanceof File) {
                data.put(LOCAL_ESA_DOWNLOAD_DIR, value);
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
                    data.put(ACTION_RESULT, ERROR);
                    data.put(ACTION_ERROR_MESSAGE, e.getMessage());
                    data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
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
        } else if (INSTALL_LOCAL_ESA.equals(key)) {
            if (value instanceof Boolean) {
                data.put(INSTALL_LOCAL_ESA, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (CLEANUP_TEMP_LOCATION.equals(key)) {
            if (value instanceof String) {
                data.put(CLEANUP_TEMP_LOCATION, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (CLEANUP_NEEDED.equals(key)) {
            if (value instanceof Boolean) {
                data.put(CLEANUP_NEEDED, value);
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
        } else if (TO_EXTENSION.equals(key)) {
            if (value instanceof String) {
                data.put(TO_EXTENSION, value);
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
        } else if (FROM_REPO.equals(key)) {
            if (value instanceof String) {
                data.put(FROM_REPO, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (ENVIRONMENT_VARIABLE_MAP.equals(key)) {
            if (value instanceof Map<?, ?>) {
                data.put(ENVIRONMENT_VARIABLE_MAP, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (DOWNLOAD_ARTIFACT_SINGLE.equals(key)) {
            if (value instanceof String) {
                data.put(DOWNLOAD_ARTIFACT_SINGLE, value);
            } else {
                throw new IllegalArgumentException();
            }
        }
        // TODO remove this later?
        else if (DOWNLOAD_ARTIFACT_LIST.equals(key)) {
            if (value instanceof List || value instanceof String) {
                data.put(DOWNLOAD_ARTIFACT_LIST, value);
//                if (data.containsKey(DOWNLOAD_ARTIFACT_LIST)) {
//                    if (value instanceof List) {
//                        ((List<String>) data.get(DOWNLOAD_ARTIFACT_LIST)).addAll((List<String>) value);
//                    } else {
//                        // value is string
//                        ((List<String>) data.get(DOWNLOAD_ARTIFACT_LIST)).add((String) value);
//                    }
//                } else {
//                    if (value instanceof List) {
//                        data.put(DOWNLOAD_ARTIFACT_LIST, value);
//                    } else {
//                        data.put(DOWNLOAD_ARTIFACT_LIST, new ArrayList<String>(Arrays.asList((String) value)));
//                    }
//                }
            } else {
                throw new IllegalArgumentException();
            }
        } else if (DOWNLOAD_INDIVIDUAL_ARTIFACT.equals(key)) {
            if (value instanceof Boolean) {
                data.put(DOWNLOAD_INDIVIDUAL_ARTIFACT, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (DOWNLOAD_LOCATION.equals(key)) {
            if (value instanceof String) {
                data.put(DOWNLOAD_LOCATION, value);
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
        } else if (ACTION_INSTALL.equals(key)) {
            if (value instanceof List || value instanceof File) {
                Boolean localESAInstall = (Boolean) data.get(INSTALL_LOCAL_ESA);
                if (localESAInstall == null || !localESAInstall) {
                    resolve(value);
                } else {
                    data.put(ACTION_INSTALL, value);
                }
            } else {
                throw new IllegalArgumentException();
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
        } else if (FEATURES_TO_RESOLVE.equals(key)) {
            if (value instanceof List) {
                Boolean localESAInstall = (Boolean) data.get(INSTALL_LOCAL_ESA);
                if (localESAInstall != null && localESAInstall) {
                    if ((List<File>) data.get(SINGLE_JSON_FILE) != null) {
                        //resolution requires INSTALL_LOCAL_ESA == true and a SINGLE_JSON_FILE != null
                        actionType = ActionType.resolve;
                        data.put(FEATURES_TO_RESOLVE, value);
                    } else {
                        throw new IllegalArgumentException();
                    }
                } else {
                    throw new IllegalArgumentException();
                }
            } else {
                throw new IllegalArgumentException();
            }
        } else if (SINGLE_JSON_FILE.equals(key)) {
            if (value instanceof List) {
                Boolean localESAInstall = (Boolean) data.get(INSTALL_LOCAL_ESA);
                if (localESAInstall != null && localESAInstall) {
                    data.put(SINGLE_JSON_FILE, value);
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
        } else if (INDIVIDUAL_ESAS.equals(key)) {
            if (value instanceof List) {
                data.put(INDIVIDUAL_ESAS, value);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (INSTALL_INDIVIDUAL_ESAS.equals(key)) {
            if (value instanceof Boolean) {
                data.put(INSTALL_INDIVIDUAL_ESAS, value);
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
        File installDir = (File) data.get(RUNTIME_INSTALL_DIR);
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
        data.put(ACTION_INSTALL, null);
        data.put(ACTION_RESULT, OK);
        data.put(ACTION_INSTALL_RESULT, null);
        data.put(ACTION_ERROR_MESSAGE, null);
        data.put(ACTION_EXCEPTION_STACKTRACE, null);

        actionType = ActionType.resolve;
        if (installObject instanceof List) {
            List<String> assets = (List<String>) installObject;
            try {
                installKernel.resolve(assets, false);
                checkLicense();
                actionType = ActionType.install;
                data.put(ACTION_INSTALL, installObject);
            } catch (InstallException e) {
                data.put(ACTION_RESULT, ERROR);
                data.put(ACTION_ERROR_MESSAGE, e.getMessage());
                data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
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
                    data.put(ACTION_INSTALL, installObject);
                } catch (InstallException e) {
                    data.put(ACTION_RESULT, ERROR);
                    data.put(ACTION_ERROR_MESSAGE, e.getMessage());
                    data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
                }
            } else {
                data.put(ACTION_RESULT, ERROR);
                data.put(ACTION_ERROR_MESSAGE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MAPBASED_ERROR_UNSUPPORTED_FILE", esaFile.getAbsoluteFile()));
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

    @SuppressWarnings("unchecked")
    public Collection<String> singleFileResolve() {
        data.put(ACTION_INSTALL, null);
        data.put(ACTION_RESULT, OK);
        data.put(ACTION_INSTALL_RESULT, null);
        data.put(ACTION_ERROR_MESSAGE, null);
        data.put(ACTION_EXCEPTION_STACKTRACE, null);

        Collection<ProductDefinition> productDefinitions = new HashSet<ProductDefinition>();
        Collection<List<RepositoryResource>> resolveResult = null;
        RepositoryResolver resolver = null;
        Collection<String> featuresResolved = new ArrayList<String>();
        boolean isOpenLiberty = false;
        try {
            for (ProductInfo productInfo : ProductInfo.getAllProductInfo().values()) {
                productDefinitions.add(new ProductInfoProductDefinition(productInfo));

                if (productInfo.getReplacedBy() == null && productInfo.getId().equals("io.openliberty")) {
                    isOpenLiberty = true;
                }

            }

            RepositoryConnectionList repoList = new RepositoryConnectionList();
            List<File> singleJsonRepos = (List<File>) data.get(SINGLE_JSON_FILE);
            File directoryBasedRepo = (File) data.get(DIRECTORY_BASED_REPOSITORY);
            if (directoryBasedRepo != null) {
                RepositoryConnection directRepo = new DirectoryRepositoryConnection(directoryBasedRepo);
                repoList.add(directRepo);
            }

            for (File jsonRepo : singleJsonRepos) {
                RepositoryConnection repo = new SingleFileRepositoryConnection(jsonRepo);
                repoList.add(repo);
            }

            ManifestFileProcessor m_ManifestFileProcessor = new ManifestFileProcessor();
            Collection<ProvisioningFeatureDefinition> installedFeatures = m_ManifestFileProcessor.getFeatureDefinitions().values();

            int alreadyInstalled = 0;
            Collection<String> featureToInstall = (Collection<String>) data.get(FEATURES_TO_RESOLVE);
            try {
                if (data.get(INSTALL_INDIVIDUAL_ESAS).equals(Boolean.TRUE)) {
                    Path tempDir = Files.createTempDirectory("generatedJson");
                    tempDir.toFile().deleteOnExit();
                    Map<String, String> shortNameMap = new HashMap<String, String>();
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
                data.put(ACTION_RESULT, ERROR);
                data.put(ACTION_ERROR_MESSAGE, e.getMessage());
                data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
            }

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

            resolver = new RepositoryResolver(productDefinitions, installedFeatures, Collections.<IFixInfo> emptySet(), repoList);
            resolveResult = resolver.resolveAsSet((Collection<String>) data.get(FEATURES_TO_RESOLVE));

            for (List<RepositoryResource> item : resolveResult) {
                for (RepositoryResource repoResrc : item) {
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
                            Boolean accepted = (Boolean) data.get(LICENSE_ACCEPT);
                            if (accepted == null || !accepted) {
                                featuresResolved.clear(); // clear the result since the licenses were not accepted
                                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_LICENSES_NOT_ACCEPTED"));
                            }
                        }
                    }

                    if (repoResrc.getRepositoryConnection() instanceof DirectoryRepositoryConnection) {
                        featuresResolved.add(repoResrc.getId());
                    } else {
                        featuresResolved.add(repoResrc.getMavenCoordinates());
                    }
                }
            }
            actionType = ActionType.install;
            featuresResolved = keepFirstInstance(featuresResolved);
            return featuresResolved;
        } catch (ProductInfoParseException e) {
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        } catch (DuplicateProductInfoException e) {
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        } catch (ProductInfoReplaceException e) {
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        } catch (RepositoryResolutionException e) {
            data.put(ACTION_RESULT, ERROR);
            InstallException ie = ExceptionUtils.create(e, e.getTopLevelFeaturesNotResolved(), (File) data.get(RUNTIME_INSTALL_DIR), false, isOpenLiberty);
            data.put(ACTION_ERROR_MESSAGE, ie.getMessage());
            data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(ie));
        } catch (InstallException e) {
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        } catch (RepositoryException e) {
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        } catch (Exception e) {
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        }
        actionType = ActionType.install;
        return featuresResolved;
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
            Boolean accepted = (Boolean) data.get(LICENSE_ACCEPT);
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
        data.put(ACTION_RESULT, OK);
        data.put(ACTION_INSTALL_RESULT, null);
        data.put(ACTION_ERROR_MESSAGE, null);
        data.put(ACTION_EXCEPTION_STACKTRACE, null);
        try {
            installKernel.checkResources();
            Boolean agreedToDownloadDependencies = (Boolean) data.get(DOWLOAD_EXTERNAL_DEPS);
            if (agreedToDownloadDependencies == null)
                agreedToDownloadDependencies = Boolean.TRUE;
            Map<String, Collection<String>> installedAssets = installKernel.install(InstallConstants.TO_USER, true, agreedToDownloadDependencies.booleanValue());
            data.put(ACTION_INSTALL_RESULT, installedAssets);
        } catch (CancelException e) {
            data.put(ACTION_RESULT, CANCELLED);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
            return CANCELLED;
        } catch (InstallException e) {
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
            return ERROR;
        }
        return OK;
    }

    @SuppressWarnings("unchecked")
    public List<File> downloadFeatures(List<String> featureList) {
        data.put(ACTION_ERROR_MESSAGE, null);
        data.put(ACTION_EXCEPTION_STACKTRACE, null);

        ArtifactDownloader artifactDownloader = new ArtifactDownloader();
        String fromRepo = (String) data.get(FROM_REPO);
        Boolean cleanupNeeded = (Boolean) data.get(CLEANUP_NEEDED);
        String downloadDir;
        if (cleanupNeeded != null && cleanupNeeded) {
            fine("Using temp location: " + TEMP_DIRECTORY);
            data.put(CLEANUP_TEMP_LOCATION, TEMP_DIRECTORY);
            downloadDir = TEMP_DIRECTORY;
        } else {
            downloadDir = getDownloadDir((String) data.get(DOWNLOAD_LOCATION));
        }
        String repo = getRepo(fromRepo);

        try {
            artifactDownloader.setEnvMap(envMap);
            artifactDownloader.synthesizeAndDownloadFeatures(featureList, downloadDir, repo);
        } catch (InstallException e) {
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
            return null;
        }

        return artifactDownloader.getDownloadedEsas();
    }

    @SuppressWarnings("unchecked")
    public List<File> downloadArtifact() {
        data.put(ACTION_ERROR_MESSAGE, null);
        data.put(ACTION_EXCEPTION_STACKTRACE, null);

        ArtifactDownloader artifactDownloader = new ArtifactDownloader();
        Boolean cleanupNeeded = (Boolean) data.get(CLEANUP_NEEDED);
        String downloadDir;
        if (cleanupNeeded != null && cleanupNeeded) {
            fine("Using temp location: " + TEMP_DIRECTORY);
            data.put(CLEANUP_TEMP_LOCATION, TEMP_DIRECTORY);
            downloadDir = TEMP_DIRECTORY;
        } else {
            downloadDir = getDownloadDir((String) data.get(DOWNLOAD_LOCATION));
        }
//        fine("artifact list is: " + this.get(DOWNLOAD_ARTIFACT_LIST).toString());
//        List<String> featureList = (List<String>) data.get(DOWNLOAD_ARTIFACT_LIST);
        String feature = (String) this.get(DOWNLOAD_ARTIFACT_SINGLE);
        String filetype = (String) this.get(DOWNLOAD_FILETYPE);
        String repo = getRepo(null);
        try {
            artifactDownloader.setEnvMap(envMap);
            artifactDownloader.synthesizeAndDownload(feature, filetype, downloadDir, repo, true);
            data.put(DOWNLOAD_LOCATION, null);
        } catch (InstallException e) {
            this.put(ACTION_RESULT, ERROR);
            this.put(ACTION_ERROR_MESSAGE, e.getMessage());
            this.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
            return null;
        }

        return artifactDownloader.getDownloadedFiles();
    }

    /**
     * @param fromRepo
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
            data.put(CLEANUP_TEMP_LOCATION, TEMP_DIRECTORY);
            data.put(CLEANUP_NEEDED, true);
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
            repo = envMap.get("FEATURE_REPO_URL");
        } else {
            fine("Connecting to the following repository: " + MAVEN_CENTRAL);
            repo = MAVEN_CENTRAL;
        }
        return repo;
    }

    @SuppressWarnings("unchecked")
    public File downloadSingleFeature() {
        data.put(ACTION_ERROR_MESSAGE, null);
        data.put(ACTION_EXCEPTION_STACKTRACE, null);

        ArtifactDownloader artifactDownloader = new ArtifactDownloader();
        String featureList = (String) data.get(DOWNLOAD_ARTIFACT_LIST);
        String filetype = (String) data.get(DOWNLOAD_FILETYPE);

        String fromRepo = (String) data.get(FROM_REPO);
        Boolean cleanupNeeded = (Boolean) data.get(CLEANUP_NEEDED);
        String downloadDir;
        if (cleanupNeeded != null && cleanupNeeded) {
            fine("Using temp location: " + TEMP_DIRECTORY);
            data.put(CLEANUP_TEMP_LOCATION, TEMP_DIRECTORY);
            downloadDir = TEMP_DIRECTORY;
        } else {
            downloadDir = getDownloadDir((String) data.get(DOWNLOAD_LOCATION));
        }
        String repo = getRepo(fromRepo);
        try {
            artifactDownloader.setEnvMap(envMap);
            artifactDownloader.synthesizeAndDownload(featureList, filetype, downloadDir, repo, true);
        } catch (InstallException e) {
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
            return null;
        }

        return artifactDownloader.getDownloadedFiles().get(0);
    }

    @SuppressWarnings("unchecked")
    public Integer localESAInstall() {
        data.put(ACTION_RESULT, OK);
        data.put(ACTION_INSTALL_RESULT, null);
        data.put(ACTION_ERROR_MESSAGE, null);
        data.put(ACTION_EXCEPTION_STACKTRACE, null);

        try {
            InstallKernelImpl installKernel = (InstallKernelImpl) this.installKernel;
            File esaFile = (File) data.get(ACTION_INSTALL);
            String toExtension = (String) data.get(TO_EXTENSION);
            if (toExtension == null) {
                toExtension = InstallConstants.TO_USER;
            }
            Collection<String> installedAssets = installKernel.installLocalFeatureNoResolve(esaFile.getAbsolutePath(), toExtension, true,
                                                                                            InstallConstants.ExistsAction.replace);
            data.put(ACTION_INSTALL_RESULT, installedAssets);
        } catch (InstallException e) {
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
            return ERROR;
        }
        return OK;
    }

    @SuppressWarnings("unchecked")
    private Integer uninstall() {
        List<String> uninstallFeatures = (data.containsKey(ACTION_UNINSTALL)) ? new ArrayList<String>((Collection<String>) data.get(ACTION_UNINSTALL)) : new ArrayList<String>();
        data.put(ACTION_ERROR_MESSAGE, null);
        try {
            InstallKernel installKernel = (InstallKernel) this.installKernel;

            Boolean forceUninstall = (data.containsKey(FORCE_UNINSTALL)) ? (Boolean) data.get(FORCE_UNINSTALL) : Boolean.FALSE;
            Boolean allowUninstallUserFeatures = (data.containsKey(UNINSTALL_USER_FEATURES)) ? (Boolean) data.get(UNINSTALL_USER_FEATURES) : Boolean.FALSE;

            if (forceUninstall) {
                if (uninstallFeatures.size() > 1) {
                    data.put(ACTION_ERROR_MESSAGE,
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
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
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
     * @param generatedJson path
     * @param shortNameMap  contains features parsed from individual esa files
     * @return singleJson file
     * @throws IOException
     * @throws RepositoryException
     * @throws InstallException
     */
    private File generateJsonFromIndividualESAs(Path jsonDirectory, Map<String, String> shortNameMap) throws IOException, RepositoryException, InstallException {
        String dir = jsonDirectory.toString();
        List<File> esas = (List<File>) data.get(INDIVIDUAL_ESAS);
        File singleJson = new File(dir + "/SingleJson.json");

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
                    throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_SINGLE_REPO_CONNECTION_FAILED", dir,
                                                                                              esa.getAbsolutePath()));
                }
            }
            Parser<? extends RepositoryResourceWritable> parser = new EsaParser(true);
            RepositoryResourceWritable resource = parser.parseFileToResource(esa, null, null);
            resource.updateGeneratedFields(true);
            resource.setRepositoryConnection(mySingleFileRepo);

            // Overload the Maven coordinates field with the file path, since the ESA should be installed from that path
            resource.setMavenCoordinates(esa.getAbsolutePath());

            resource.uploadToMassive(new AddThenDeleteStrategy());
        }

        return singleJson;

    }

    @SuppressWarnings("unchecked")
    private Collection<File> downloadEsas() {
        String fromRepo = getDownloadDir((String) data.get(FROM_REPO));
        File rootDir = new File(fromRepo);
        Collection<String> resolvedFeatures = (List<String>) data.get(DOWNLOAD_ARTIFACT_LIST);
        String openLibertyVersion = getLibertyVersion();

        Map<String, List> artifactsMap = fetchArtifactsFromLocalRepository(rootDir, resolvedFeatures, openLibertyVersion, ".esa");
        List<File> foundFeatures = artifactsMap.get("foundArtifacts");
        List<String> missingFeatures = artifactsMap.get("missingArtifacts");
        if (foundFeatures.size() != resolvedFeatures.size() && !missingFeatures.isEmpty()) {
            List<Integer> missingFeatureIndexes = artifactsMap.get("missingArtifactIndexes");
            List<File> downloadedFeatures = downloadFeatures(missingFeatures);
            if (downloadedFeatures == null) {
                return null;
            } else {
                insertElementsIntoList(foundFeatures, downloadedFeatures, missingFeatureIndexes);
            }
            // some increment left over
            double increment = ((progressBar.getMethodIncrement("fetchArtifacts") / resolvedFeatures.size()) * downloadedFeatures.size());
            updateProgress(increment);
            fine("Downloaded the following features from the remote maven repository:" + downloadedFeatures);

        }
        return foundFeatures;

    }

    /**
     * Returns a hashmap containing artifacts found as well as the order of the artifacts not found.
     * The found artifacts can be accessed using key: foundArtifacts
     * The missing artifacts can be accessed using key: missingArtifacts
     * The missing artifact indexees can be accessed using key: missingArtifactIndexes
     *
     * @param rootDir            the local maven repository
     * @param artifacts          a list of artifacts in the form groupId:artifactId:version
     * @param openLibertyVersion open liberty runtime version
     * @param extension          file extension
     * @return a map containing the found and not found artifacts.
     */
    private Map<String, List> fetchArtifactsFromLocalRepository(File rootDir, Collection<String> artifacts, String openLibertyVersion, String extension) {
        List<String> artifactsClone = new ArrayList<>(artifacts);
        List<File> foundArtifacts = new ArrayList<>();
        List<Integer> missingArtifactIndexes = new ArrayList<>();
        int index = 0;
        double increment = (progressBar.getMethodIncrement("fetchArtifacts") / artifacts.size());

        for (String feature : artifacts) {
            fine("Processing feature: " + feature);
            String groupId = feature.split(":")[0];
            String featureName = feature.split(":")[1];
            File groupDir = new File(rootDir, groupId.replace(".", "/"));
            if (!groupDir.exists()) {
                missingArtifactIndexes.add(index);
                continue;
            }
            String featureEsa = featureName + "-" + openLibertyVersion + extension;
            Path featurePath = Paths.get(groupDir.getAbsolutePath().toString(), featureName, openLibertyVersion, featureEsa);

            if (Files.isRegularFile(featurePath)) {
                foundArtifacts.add(featurePath.toFile());
                artifactsClone.remove(feature);
                updateProgress(increment);
                fine("Found feature at path: " + featurePath.toString());

            } else {
                missingArtifactIndexes.add(index);
            }

            index += 1;
        }
        Map<String, List> artifactsMap = new HashMap<>();
        artifactsMap.put("foundArtifacts", foundArtifacts);
        artifactsMap.put("missingArtifacts", artifactsClone);
        artifactsMap.put("missingArtifactIndexes", missingArtifactIndexes);

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
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
            data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
        }

        if (openLibertyVersion == null) {
            // openliberty.properties file is missing or invalidly formatted
            InstallException ie = new InstallException("Could not determine the open liberty runtime version.");
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, ie.getMessage());
            data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(ie));

        }
        this.openLibertyVersion = openLibertyVersion;
        return openLibertyVersion;

    }

    // TODO make these methods private, hiding them behind map.put and map.get
    public List<File> getLocalJsonFiles(Set<String> jsonsRequired) throws InstallException {
        String fromRepo = getDownloadDir((String) data.get(DOWNLOAD_LOCATION));
        File fromDir = new File(fromRepo);
        String openLibertyVersion = getLibertyVersion();
        String artifactId = "features";
        List<File> jsons = new ArrayList<>();
        for (String json : jsonsRequired) {
            String mavenCoordinateDirectory = json.replace(".", "/") + "/" + artifactId + "/" + openLibertyVersion + "/";
            String jsonFilePath = mavenCoordinateDirectory + "features-" + openLibertyVersion + ".json";
            File openLibertyJson = new File(fromDir, jsonFilePath);

            if (openLibertyJson.exists()) {
                jsons.add(openLibertyJson);
            }

        }

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
        String openLibertyVersion = getLibertyVersion();
        // get open liberty json
        List<File> result = new ArrayList<File>();
        this.put(DOWNLOAD_FILETYPE, "json");
        boolean singleArtifactInstall = true;
        this.put(DOWNLOAD_INDIVIDUAL_ARTIFACT, singleArtifactInstall);

        String artifactId = "features";
        for (String jsonGroupId : jsonsRequired) {
            String jsonCoord = jsonGroupId + ":" + artifactId + ":" + openLibertyVersion;

            // TODO TODO TODO TODO TODO TODO TODO clean this spaghetti code
            this.put(DOWNLOAD_ARTIFACT_SINGLE, jsonCoord);
            this.put(DOWNLOAD_ARTIFACT_LIST, jsonCoord);

            Object downloaded = this.get(DOWNLOAD_RESULT);
            if (this.get("action.error.message") != null) {
                fine("action.exception.stacktrace: " + this.get("action.error.stacktrace"));
                String exceptionMessage = (String) this.get("action.error.message");
                throw new InstallException(exceptionMessage);
                // convert to json exception msg
//                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_MAVEN_JSON_NOT_FOUND", jsonGroupId));
            }
            if (downloaded instanceof List) {
                result.addAll((List<File>) downloaded);
            } else if (downloaded instanceof File) {
                result.add((File) downloaded);
            }
            // TODO TODO TODO TODO TODO TODO TODO

        }

//        String OLJsonCoord = "io.openliberty.features:features:" + openLibertyVersion;
//        data.put("download.artifact.list", OLJsonCoord);
//        File OL = (File) this.get("download.result");

        // for debugging purposes (closed liberty json)
//        String CLJsonCoord = "com.ibm.websphere.appserver.features:features:" + openLibertyVersion;
//        data.put("download.artifact.list", CLJsonCoord);
//        File CL = (File) this.get("download.result");
//        result.add(CL);

        fine("Downloaded the following json files from remote: " + result);
        return result;

    }

    private Map<String, String> getEnvMap() {
        Map<String, String> envMapRet = new HashMap<String, String>();

        //parse through httpProxy env variables
        String proxyEnvVarHttp = System.getenv("http_proxy");
        if (proxyEnvVarHttp != null) {
            Map<String, String> httpProxyVariables;
            try {
                httpProxyVariables = getProxyVariables(proxyEnvVarHttp, "http");
            } catch (InstallException e) {
                data.put(ACTION_RESULT, ERROR);
                data.put(ACTION_ERROR_MESSAGE, e.getMessage());
                data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
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
                data.put(ACTION_RESULT, ERROR);
                data.put(ACTION_ERROR_MESSAGE, e.getMessage());
                data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
                return null;
            }
            Set<String> httpsProxyVarKeys = httpsProxyVariables.keySet();
            for (String key : httpsProxyVarKeys) {
                envMapRet.put(key, httpsProxyVariables.get(key));
            }
        }

        envMapRet.put("FEATURE_REPO_URL", System.getenv("FEATURE_REPO_URL"));
        envMapRet.put("FEATURE_REPO_USER", System.getenv("FEATURE_REPO_USER"));
        envMapRet.put("FEATURE_REPO_PASSWORD", System.getenv("FEATURE_REPO_PASSWORD"));
        envMapRet.put("FEATURE_LOCAL_REPO", System.getenv("FEATURE_LOCAL_REPO"));

        //search through the properties file to look for overrides if they exist TODO
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
                        data.put(ACTION_RESULT, ERROR);
                        data.put(ACTION_ERROR_MESSAGE, e.getMessage());
                        data.put(ACTION_EXCEPTION_STACKTRACE, ExceptionUtils.stacktraceToString(e));
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
        if (protocol.equals("https")) {
            String[] proxyEnvVarSplit = proxyEnvVar.split("@");
            if (proxyEnvVarSplit.length != 2) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_IMPROPER_HTTPSPROXY_FORMAT", proxyEnvVar));
            }
            String[] proxyCredentials = proxyEnvVarSplit[0].split(":"); //[https, //user, password]
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
        } else if (protocol.equals("http")) {
            String[] proxyHttpSplit = proxyEnvVar.split(":");
            if (proxyHttpSplit.length != 3) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_IMPROPER_HTTPPROXY_FORMAT", proxyEnvVar));
            }
            result.put(protocol + ".proxyHost", proxyHttpSplit[1].replace("/", "")); //prox-server
            result.put(protocol + ".proxyPort", proxyHttpSplit[2]); ////3128
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
            fine("featureUtility.env not found");
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

}