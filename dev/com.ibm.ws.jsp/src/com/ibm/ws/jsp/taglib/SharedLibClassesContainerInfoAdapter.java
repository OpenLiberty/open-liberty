/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.taglib;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.config.Fileset;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.FileUtils;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.library.Library;

/**
 * The configuration processing logic in this class duplicates that in com.ibm.ws.classloading.ClassLoaderConfigHelper
 */
public class SharedLibClassesContainerInfoAdapter implements ContainerAdapter<SharedLibClassesContainerInfo> {
    static final protected Logger logger;
    static{
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }

    private volatile BundleContext bundleContext = null;
    private volatile ConfigurationAdmin configAdmin = null;
    private volatile WsLocationAdmin locAdmin = null;
    private volatile ArtifactContainerFactory artifactFactory = null;
    private volatile AdaptableModuleFactory moduleFactory = null;

    protected void activate(ComponentContext context) {
        this.bundleContext = context.getBundleContext();
    }

    protected void deactivate(ComponentContext context) {
        this.bundleContext = null;
    }

    protected void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    protected void unsetConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = null;
    }

    protected void setLocAdmin(WsLocationAdmin locAdmin) {
        this.locAdmin = locAdmin;
    }

    protected void unsetLocAdmin(WsLocationAdmin locAdmin) {
        this.locAdmin = null;
    }

    protected void setArtifactFactory(ArtifactContainerFactory artifactFactory) {
        this.artifactFactory = artifactFactory;
    }

    protected void unsetArtifactFactory(ArtifactContainerFactory artifactFactory) {
        this.artifactFactory = null;
    }

    protected void setModuleFactory(AdaptableModuleFactory moduleFactory) {
        this.moduleFactory = moduleFactory;
    }

    protected void unsetModuleFactory(AdaptableModuleFactory moduleFactory) {
        this.moduleFactory = null;
    }

    @Override
    public SharedLibClassesContainerInfo adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        final List<ContainerInfo> sharedLibContainers = new ArrayList<ContainerInfo>();
        final List<ContainerInfo> commonLibContainers = new ArrayList<ContainerInfo>();

        ExtendedApplicationInfo appInfo = (ExtendedApplicationInfo) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), ApplicationInfo.class);
        NestedConfigHelper configHelper = appInfo.getConfigHelper();
        if (configHelper == null) {
            // probably a WAB
            return null;
        }

        try {
            String parentPid = (String) configHelper.get(Constants.SERVICE_PID);
            String sourcePid = (String) configHelper.get("ibm.extends.source.pid");
            if (sourcePid != null) {
                parentPid = sourcePid;
            }
            // Find the classloaders for the application
            StringBuilder classloaderFilter = new StringBuilder(200);
            classloaderFilter.append("(&");
            classloaderFilter.append(FilterUtils.createPropertyFilter("service.factoryPid", "com.ibm.ws.classloading.classloader"));
            classloaderFilter.append(FilterUtils.createPropertyFilter("config.parentPID", parentPid));
            classloaderFilter.append(')');

            Configuration[] classloaderConfigs = configAdmin.listConfigurations(classloaderFilter.toString());
            if (classloaderConfigs == null || classloaderConfigs.length != 1) {
                // There can be only one
                return null;
            }

            Configuration cfg = classloaderConfigs[0];
            Dictionary<String, Object> props = cfg.getProperties();
            if (props != null) {
                String[] libraryPIDs = (String[]) props.get("privateLibraryRef");
                processLibraryPIDs(sharedLibContainers, libraryPIDs);

                libraryPIDs = (String[]) props.get("commonLibraryRef");
                processLibraryPIDs(commonLibContainers, libraryPIDs);
            } else {
                cfg.delete();
                return null;
            }
        } catch (IOException e) {
            e.getCause();
            return null;
        } catch (InvalidSyntaxException e) {
            e.getCause();
            return null;
        }

        SharedLibClassesContainerInfo info = new SharedLibClassesContainerInfo() {
            @Override
            public List<ContainerInfo> getSharedLibraryClassesContainerInfo() {
                return Collections.unmodifiableList(sharedLibContainers);
            }
            @Override
            public List<ContainerInfo> getCommonLibraryClassesContainerInfo() {
                return Collections.unmodifiableList(commonLibContainers);
            }
        };
        return info;
    }

    /**
     * @param sharedLibContainers
     * @param libraryPIDs
     */
    private void processLibraryPIDs(List<ContainerInfo> sharedLibContainers, String[] libraryPIDs) throws InvalidSyntaxException {
        if (libraryPIDs != null) {
            for (String pid : libraryPIDs) {
                String libraryFilter = FilterUtils.createPropertyFilter(Constants.SERVICE_PID, pid);
                Collection<ServiceReference<Library>> libraryRefs = bundleContext.getServiceReferences(Library.class, libraryFilter);
                for (ServiceReference<Library> libraryRef : libraryRefs) {
                    Library library = bundleContext.getService(libraryRef);
                    if (library != null) {
                        String id = library.id();
                        Collection<File> files = library.getFiles();
                        addContainers(sharedLibContainers, pid, id, files);
                        Collection<File> folders = library.getFolders();
                        addContainers(sharedLibContainers, pid, id, folders);
                        Collection<Fileset> filesets = library.getFilesets();
                        for (Fileset fileset : filesets) {
                            addContainers(sharedLibContainers, pid, id, fileset.getFileset());
                        }
                    }
                }
            }
        }
    }

    private void addContainers(List<ContainerInfo> sharedLibContainers, String pid, String libName, Collection<File> files) {
        if (files != null) {
            for (File file : files) {
                final Container container = setupContainer(pid, file);
                if (container != null) {
                    final String name = "/" + libName + "/" + file.getName();
                    sharedLibContainers.add(new ContainerInfo() {
                        @Override
                        public Type getType() {
                            return Type.SHARED_LIB;
                        }
    
                        @Override
                        public String getName() {
                            return name;
                        }
    
                        @Override
                        public Container getContainer() {
                            return container;
                        }
                    });
                }
            }
        }
    }

    private Container setupContainer(String pid, File locationFile) {
        File cacheDir = new File(getCacheDir(), pid);
        if (!FileUtils.ensureDirExists(cacheDir)) {
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                logger.finer("Could not create directory at " + cacheDir.getAbsolutePath());
            }
            return null;
        }

        ArtifactContainer artifactContainer = artifactFactory.getContainer(cacheDir, locationFile);
        if (artifactContainer == null) {
            return null;
        }

        File cacheDirAdapt = new File(getCacheAdaptDir(), pid);
        if (!FileUtils.ensureDirExists(cacheDirAdapt)) {
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                logger.finer("Could not create directory at " + cacheDirAdapt.getAbsolutePath());
            }
            return null;
        }

        File cacheDirOverlay = new File(getCacheOverlayDir(), pid);
        if (!FileUtils.ensureDirExists(cacheDirOverlay)) {
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINER)) {
                logger.finer("Could not create directory at " + cacheDirOverlay.getAbsolutePath());
            }
            return null;
        }

        return moduleFactory.getContainer(cacheDirAdapt, cacheDirOverlay, artifactContainer);
    }

    private File getCacheAdaptDir() {
        return locAdmin.getBundleFile(this, "cacheAdapt");
    }

    private File getCacheOverlayDir() {
        return locAdmin.getBundleFile(this, "cacheOverlay");
    }

    private File getCacheDir() {
        return locAdmin.getBundleFile(this, "cache");
    }
}
