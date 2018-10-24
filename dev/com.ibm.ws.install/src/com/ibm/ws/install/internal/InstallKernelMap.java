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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
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

    private enum ActionType {
        install,
        uninstall,
        resolve
    }

    private final Map data = new HashMap();
    private InstallKernelInteractive installKernel;
    private InstallEventListener ielistener;
    private ActionType actionType = null;

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
            resolveResult = resolver.resolve((Collection<String>) data.get(FEATURES_TO_RESOLVE));

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
            InstallException ie = ExceptionUtils.create(e, (Collection<String>) data.get(FEATURES_TO_RESOLVE), (File) data.get(RUNTIME_INSTALL_DIR), false, isOpenLiberty);
            data.put(ACTION_ERROR_MESSAGE, ie.toString());
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
     * @param esa ESA file
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
     * @param shortNameMap contains features parsed from individual esa files
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

}