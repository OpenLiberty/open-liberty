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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

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
import com.ibm.ws.kernel.productinfo.DuplicateProductInfoException;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.kernel.productinfo.ProductInfoReplaceException;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.repository.connections.ProductDefinition;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.SingleFileRepositoryConnection;
import com.ibm.ws.repository.connections.liberty.ProductInfoProductDefinition;
import com.ibm.ws.repository.exceptions.RepositoryException;
//import com.ibm.ws.repository.resolver.ManifestFileProcessor;
import com.ibm.ws.repository.resolver.RepositoryResolver;
import com.ibm.ws.repository.resources.RepositoryResource;

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
    private static final String FEATURES_TO_RESOLVE = "features.to.resolve";
    private static final String SINGLE_JSON_FILE = "single.json.file";
    private static final String MESSAGE_LOCALE = "message.locale";

    // Return code
    private static final Integer OK = Integer.valueOf(0);
    private static final Integer CANCELLED = Integer.valueOf(-1);
    private static final Integer ERROR = Integer.valueOf(1);

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
                }
            } else {
                data.put(ACTION_RESULT, ERROR);
                data.put(ACTION_ERROR_MESSAGE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MAPBASED_ERROR_UNSUPPORTED_FILE", esaFile.getAbsoluteFile()));
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<String> singleFileResolve() {
        data.put(ACTION_INSTALL, null);
        data.put(ACTION_RESULT, OK);
        data.put(ACTION_INSTALL_RESULT, null);
        data.put(ACTION_ERROR_MESSAGE, null);
        Collection<ProductDefinition> productDefinitions = new HashSet<ProductDefinition>();
        Collection<List<RepositoryResource>> resolveResult = null;
        RepositoryResolver resolver = null;
        Collection<String> featuresResolved = new ArrayList<String>();

        try {
            for (ProductInfo productInfo : ProductInfo.getAllProductInfo().values()) {
                productDefinitions.add(new ProductInfoProductDefinition(productInfo));
            }
            RepositoryConnectionList repoList = new RepositoryConnectionList();
            List<File> singleJsonRepos = (List<File>) data.get(SINGLE_JSON_FILE);
            for (File jsonRepo : singleJsonRepos) {
                RepositoryConnection repo = new SingleFileRepositoryConnection(jsonRepo);
                repoList.add(repo);
            }

            ManifestFileProcessor m_ManifestFileProcessor = new ManifestFileProcessor();
            resolver = new RepositoryResolver(productDefinitions, m_ManifestFileProcessor.getFeatureDefinitions().values(), Collections.<IFixInfo> emptySet(), repoList);
            resolveResult = resolver.resolve((Collection<String>) data.get(FEATURES_TO_RESOLVE));
            for (List<RepositoryResource> item : resolveResult) {
                for (RepositoryResource repoResrc : item) {
                    featuresResolved.add(repoResrc.getMavenCoordinates());
                }
            }
            actionType = ActionType.install;
            featuresResolved = keepFirstInstance(featuresResolved);
            return featuresResolved;
        } catch (ProductInfoParseException e) {
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
        } catch (DuplicateProductInfoException e) {
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
        } catch (ProductInfoReplaceException e) {
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
        } catch (RepositoryException e) {
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
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
            return CANCELLED;
        } catch (InstallException e) {
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
            return ERROR;
        }
        return OK;
    }

    @SuppressWarnings("unchecked")
    public Integer localESAInstall() {
        data.put(ACTION_RESULT, OK);
        data.put(ACTION_INSTALL_RESULT, null);
        data.put(ACTION_ERROR_MESSAGE, null);

        try {
            InstallKernelImpl installKernel = (InstallKernelImpl) this.installKernel;
            File esaFile = (File) data.get(ACTION_INSTALL);
            Collection<String> installedAssets = installKernel.installLocalFeature(esaFile.getAbsolutePath(), InstallConstants.TO_USER, true,
                                                                                   InstallConstants.ExistsAction.replace);
            data.put(ACTION_INSTALL_RESULT, installedAssets);
        } catch (InstallException e) {
            data.put(ACTION_RESULT, ERROR);
            data.put(ACTION_ERROR_MESSAGE, e.getMessage());
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
            return ERROR;
        }
        return OK;
    }
}
