/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading;

import static com.ibm.ws.classloading.ClassLoaderConfigHelper.Attribute.apiTypeVisibility;
import static com.ibm.ws.classloading.ClassLoaderConfigHelper.Attribute.classProviderRef;
import static com.ibm.ws.classloading.ClassLoaderConfigHelper.Attribute.commonLibraryRef;
import static com.ibm.ws.classloading.ClassLoaderConfigHelper.Attribute.delegation;
import static com.ibm.ws.classloading.ClassLoaderConfigHelper.Attribute.privateLibraryRef;
import static com.ibm.wsspi.classloading.ApiType.API;
import static com.ibm.wsspi.classloading.ApiType.IBMAPI;
import static com.ibm.wsspi.classloading.ApiType.SPEC;
import static com.ibm.wsspi.classloading.ApiType.STABLE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.classloading.internal.ClassLoadingConstants;
import com.ibm.ws.classloading.internal.ClassLoadingServiceImpl;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.classloading.ApiType;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.classloading.ClassLoadingServiceException;
import com.ibm.wsspi.classloading.GatewayConfiguration;
import com.ibm.wsspi.config.Fileset;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.library.Library;

/**
 * Convenience wrapper around the server.xml configuration for class loaders.
 *
 * The configuration processing logic in this class is duplicated in com.ibm.ws.jsp.taglib.SharedLibClassesContainerInfoAdapter
 */
public class ClassLoaderConfigHelper {
    private static final TraceComponent tc = Tr.register(ClassLoaderConfigHelper.class);

    @com.ibm.websphere.ras.annotation.Trivial
    enum Attribute {
        apiTypeVisibility, delegation, privateLibraryRef, commonLibraryRef, classProviderRef
    };

    @SuppressWarnings("serial")
    @Trivial
    private static class InitWithoutConfig extends Exception {
        public InitWithoutConfig(String because) {
            super(because);
        }
    }

    private static final EnumSet<ApiType> DEFAULT_API_TYPES = EnumSet.of(SPEC, IBMAPI, API, STABLE);

    private final List<String> sharedLibraries;
    private final List<String> commonLibraries;
    private final List<String> classProviders;

    private String[] sharedLibrariesPids;
    private String[] commonLibrariesPids;

    /* This flag must be volatile to ensure other threads do not see the pre-constructor value */
    private final boolean isDelegateLast;
    /** This set is to be updated by overwriting with a finished set. Making it volatile should enforce consistency. */
    private final EnumSet<ApiType> apiTypes; // = EnumSet.copyOf(DEFAULT_API_TYPES);
    private final Dictionary<String, Object> classLoaderConfigProps;

    @FFDCIgnore(InitWithoutConfig.class)
    public ClassLoaderConfigHelper(NestedConfigHelper configHelper, ConfigurationAdmin configAdmin, ClassLoadingService classLoadingSvc) {
        final String methodName = "ClassLoaderConfigHelper(): ";
        Configuration cfg;
        try {
            cfg = retrieveConfig(configHelper, configAdmin);
        } catch (InitWithoutConfig reason) {
            // initialize default values when we can't find the config for any reason
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + reason.getMessage());
            this.classLoaderConfigProps = null;
            this.apiTypes = DEFAULT_API_TYPES;
            this.isDelegateLast = false;
            this.sharedLibraries = Collections.emptyList();
            this.commonLibraries = Collections.emptyList();
            this.classProviders = Collections.emptyList();
            return;
        }

        this.classLoaderConfigProps = cfg.getProperties();
        // read in all the attributes
        EnumMap<Attribute, Object> values = new EnumMap<Attribute, Object>(Attribute.class);
        for (Attribute attr : Attribute.values())
            values.put(attr, classLoaderConfigProps.get(attr.name()));

        // consume them
        this.apiTypes = ApiType.createApiTypeSet((String) values.remove(apiTypeVisibility));
        Object value = values.remove(delegation);
        this.isDelegateLast = value instanceof String ? (((String) value).equalsIgnoreCase("parentLast")) : false;

        this.sharedLibrariesPids = (String[]) values.remove(privateLibraryRef);
        this.commonLibrariesPids = (String[]) values.remove(commonLibraryRef);

        this.sharedLibraries = getIds(configAdmin, sharedLibrariesPids);
        this.commonLibraries = getIds(configAdmin, commonLibrariesPids);
        this.classProviders = getIds(configAdmin, (String[]) values.remove(classProviderRef));

        if (values.isEmpty())
            return;
        // shout if there were any we listed but forgot to code for
        throw new Error("Config value(s) not dealt with! " + values);
    }

    /** get the configuration object or die trying */
    private Configuration retrieveConfig(NestedConfigHelper configHelper, ConfigurationAdmin configAdmin) throws InitWithoutConfig {
        if (configHelper == null)
            throw new InitWithoutConfig("Configuration not found");
        // We need configAdmin to list the configurations of nested classloader elements with this application as their parent
        if (configAdmin == null)
            throw new InitWithoutConfig("ConfigurationAdmin service not found");

        String parentPid = (String) configHelper.get(Constants.SERVICE_PID);
        String sourcePid = (String) configHelper.get("ibm.extends.source.pid");
        if (sourcePid != null) {
            parentPid = sourcePid;
        }
        try {
            StringBuilder filter = new StringBuilder();
            filter.append("(&");
            filter.append(FilterUtils.createPropertyFilter("service.factoryPid", "com.ibm.ws.classloading.classloader"));
            filter.append(FilterUtils.createPropertyFilter("config.parentPID", parentPid));
            filter.append(")");
            Configuration[] configs = configAdmin.listConfigurations(filter.toString());
            if (configs == null || configs.length != 1) {
                throw new InitWithoutConfig("No classloader element found");
            }
            Configuration config = configs[0];
            if (config.getProperties() == null) {
                if (tc.isErrorEnabled())
                    Tr.error(tc, "cls.classloader.missing", config.getPid());
                config.delete();
                throw new InitWithoutConfig("Classloader config not found");
            }
            return config;
        } catch (IOException e) {
            throw new InitWithoutConfig("Configuration for classloader not found, exception " + e);
        } catch (InvalidSyntaxException e) {
            throw new InitWithoutConfig("Configuration for classloader not found, exception " + e);
        }
    }

    // Find the PID in the configAdmin and get the id corresponding to it.
    private List<String> getIds(ConfigurationAdmin ca, String[] pids) {
        final String methodName = "getIds(): ";
        if (pids == null) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<String>();
        for (String pid : pids) {
            try {
                String filter = "(" + org.osgi.framework.Constants.SERVICE_PID + "=" + pid + ")";
                Configuration[] config = ca.listConfigurations(filter);
                if (config == null) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName + "Configuration not found for pid " + pid);
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName + "Found config for " + pid);
                    String id = (String) config[0].getProperties().get("id");
                    ids.add(id);
                }
            } catch (IOException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + "Configuration not found for " + pid, e);
            } catch (InvalidSyntaxException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + "Configuration not found for " + pid, e);
            }
        }
        return Collections.unmodifiableList(ids);
    }

    private ClassLoader createGSLLoader(List<Container> classPath, ClassLoaderConfiguration config, ClassLoadingService classLoadingService, Library gsl) {
        // We found at least one file - use the global shared library.
        ClassLoader parentCL = classLoadingService.getSharedLibraryClassLoader(gsl);
        ClassLoaderIdentity globalLibId = classLoadingService.createIdentity(ClassLoadingConstants.SHARED_LIBRARY_DOMAIN, ClassLoadingConstants.GLOBAL_SHARED_LIBRARY_ID);
        config.setParentId(globalLibId);
        ClassLoader loader = classLoadingService.createChildClassLoader(classPath, config);
        // need a statement here to ensure parentCL doesn't get JITted away and GC'd before we create the child.
        if (parentCL.hashCode() > 0 || parentCL.hashCode() < 1) // always true but let's hope JIT doesn't spot that!
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Created app class loader using global shared library loader as parent");
        return loader;
    }

    /**
     * Breadth-first traversal of folder and any subdirectories looking for if any files exist
     */
    private boolean folderContainsFiles(File folder) {
        if (folder != null) {
            File[] files = folder.listFiles();
            for (File file : files)
                if (file.isFile())
                    return true;
            for (File file : files)
                if (file.isDirectory())
                    if (folderContainsFiles(file))
                        return true;
        }
        return false;
    }

    public boolean isDelegateLast() {
        return isDelegateLast;
    }

    public ClassLoader createTopLevelClassLoader(List<Container> classPath,
                                                 GatewayConfiguration gwConfig,
                                                 ClassLoaderConfiguration config,
                                                 ClassLoadingService classLoadingService,
                                                 Library globalSharedLibrary) {
        // This method uses early returns.
        // There is a very specific set of conditions under which we
        // want to use a global shared library class loader as the parent
        // for a top-level classloader:
        // - There must be no explicit <classloader> configuration element.
        // - The global shared library object must exist (FFDC if not).
        // - The global shared library object must have filesets (no FFDC since this could be a valid server configuration).
        // - The global shared library filesets must have some files (no FFDC either way).
        // Under any other circumstances we will create a top-level classloader with a
        // gateway class loader as its parent, according to the supplied gateway configuration.
        gwConfig.setApiTypeVisibility(apiTypes);
        if (classLoaderConfigProps != null) {
            // if there is some <classloader> config, we need to read it out of the helper into the gateway and classloader configuration objects
            config.addSharedLibraries(sharedLibraries);
            config.setCommonLibraries(commonLibraries);
            config.setClassProviders(classProviders);
            config.setDelegateToParentAfterCheckingLocalClasspath(isDelegateLast);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Creating class loader with parent gateway because <classloader> element config found for: " + config.getId());
            return classLoadingService.createTopLevelClassLoader(classPath, gwConfig, config);
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Using common parent with common gateway for: " + config.getId());
        Library gsl = globalSharedLibrary;
        // Ensure the global shared library object exists
        if (gsl == null) {
            // FFDC this: no apps should be brought up before we have been injected with the global shared library
            Throwable ffdcOnly = new ClassLoadingServiceException("Global shared library is null");
            FFDCFilter.processException(ffdcOnly, ClassLoadingServiceImpl.class.getSimpleName(), "Missing global shared library when creating dependent class loader");
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Could not retrieve global shared library - carrying on as if there are no global shared libraries");
            return classLoadingService.createTopLevelClassLoader(classPath, gwConfig, config);
        }

        // Ensure the global shared library has some fileset objects
        Collection<Fileset> filesets = gsl.getFilesets();
        Collection<File> folders = gsl.getFolders();

        // if there are none this is an unexpected error
        if ((filesets == null || filesets.isEmpty()) && (folders == null || folders.isEmpty())) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Could not retrieve global shared library filesets - carrying on as if there are no global shared libraries");
            return classLoadingService.createTopLevelClassLoader(classPath, gwConfig, config);
        }

        // Ensure there are some files present in the global library

        for (Fileset fileset : filesets) {
            if (fileset == null)
                continue;
            Collection<File> files = fileset.getFileset();
            if (files == null || files.isEmpty())
                continue;
            ((ClassLoadingServiceImpl) classLoadingService).setGlobalSharedLibrary(globalSharedLibrary);
            return createGSLLoader(classPath, config, classLoadingService, gsl);
        }

        for (File folder : folders) {
            if (folderContainsFiles(folder)) {
                ((ClassLoadingServiceImpl) classLoadingService).setGlobalSharedLibrary(globalSharedLibrary);
                return createGSLLoader(classPath, config, classLoadingService, gsl);
            }
        }

        // There was a global shared library object and it had filesets but there were no files.
        // This is not an error; we will just ignore the global shared library classloader.
        if (tc.isDebugEnabled())
            Tr.debug(tc, "No files found in the global shared library - ignoring global shared library");
        return classLoadingService.createTopLevelClassLoader(classPath, gwConfig, config);
    }
}