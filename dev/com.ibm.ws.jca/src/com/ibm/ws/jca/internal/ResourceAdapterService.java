/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.internal;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.classloading.ClassProvider;
import com.ibm.ws.classloading.LibertyClassLoader;
import com.ibm.ws.jca.rar.ResourceAdapterBundleService;
import com.ibm.ws.jca.utils.xml.metatype.Metatype;
import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.classloading.ApiType;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.classloading.GatewayConfiguration;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

/**
 * Installs a resource adapter and generates metatype
 * based on the ra.xml, annotations, and optional wlp-ra.xml extension.
 * For example, if you configure the following in server.xml (and have the jca feature enabled),
 * <resourceAdapter id="MyAdapter" location="C:/resourceAdapters/myAdapter_V1.0.rar"/>
 * Then an instance of this class will be activated which generates the metatype for config elements with the name
 * <properties.MyAdapter.../>
 * that can be nested under resourceAdapter, connectionFactory, adminObject, activationSpec,
 * as well as under various specialized forms of the above (such as jmsTopicConnectionFactory or jmsQueue)
 */
//as documentation only at this point:
//@Component(pid="com.ibm.ws.jca.bundleResourceAdapter")
public class ResourceAdapterService extends DeferredService implements ClassProvider, MetaTypeProvider {
    private static final TraceComponent tc = Tr.register(ResourceAdapterService.class);

    /**
     * Class loader for the RAR file.
     */
    private ClassLoader classloader;

    /**
     * The class loading service.
     */
    private ClassLoadingService classloadingSvc;

    /**
     * Unique identifier for this resourceAdapter.
     */
    private volatile String id;

    /**
     * Lock for lazy initialization.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Class loader identity for the RAR file.
     */
    private ClassLoaderIdentity rarClassLoaderId;

    /**
     * Resource adapter file path
     */
    private String rarFilePath;

    /**
     * Bundle context passed to this service
     */
    private volatile BundleContext bundleContext;

    /**
     * Bundle for the bundle context passed to this service
     */
    private Bundle bundle;

    /**
     * The artifact container factory service
     */
    private ArtifactContainerFactory _acf;

    /**
     * The adaptable module factory service
     */
    private AdaptableModuleFactory _amf;

    /**
     * The generated metatype for this RAR
     */
    private Metatype metatype;

    /**
     * The component metadata for this resource adapter.
     */
    private ResourceAdapterMetaData ramd;

    /**
     * The bundle service for this resource adapter.
     */
    private ResourceAdapterBundleService raBundleSvc;

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context for this component instance
     * @throws BundleException
     */
    protected void activate(ComponentContext context) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "activate", context.getProperties());

        id = (String) context.getProperties().get(Constants.ID);
        rarFilePath = (String) context.getProperties().get(Constants.LOCATION);
        bundleContext = Utils.priv.getBundleContext(context);
        bundle = bundleContext.getBundle();

        // This is here so that the RAR class loader is set before providing it
        // to the bundle service for the RAR below.
        getClassLoader();

        // Provide the classloader identity to the bundle service associated
        // with the RAR so it may complete initialization before the RAR is
        // installed.
        raBundleSvc.setClassLoaderID(rarClassLoaderId);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "activate");
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context for this component instance
     */
    protected void deactivate(ComponentContext context) throws BundleException {
        deregisterDeferredService();
    }

    /**
     * Check that resource adapter path exists
     *
     * @return true if path exists, false if it does not
     */
    public boolean rarFileExists() {
        final File zipFile = new File(rarFilePath);
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return zipFile.exists();
            }
        });
    }

    /**
     * Returns the class loader for the resource adapter.
     *
     * @return class loader
     */
    public ClassLoader getClassLoader() {
        lock.readLock().lock();
        try {
            if (classloader != null)
                return classloader;
        } finally {
            lock.readLock().unlock();
        }
        if (!rarFileExists())
            return null;
        lock.writeLock().lock();
        try {
            if (classloader == null) {
                classloader = createRarClassLoader();
            }
            return classloader;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the class loader for the resource adapter file
     *
     * @returns class loader for resource adapter file
     */
    private ClassLoader createRarClassLoader() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // Get the files needed to convert the rar file artifact containers into adaptable containers
        // Using the ID and the rar file modification date as a unique name for the cache files

        File bundleWorkareaRoot = bundle.getDataFile("");
        if (trace && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "bundleWorkareaRoot " + bundleWorkareaRoot);
        }

        // Get list of files in the bundle's data cache
        final Bundle theBundle = bundle;
        File[] cacheFiles = AccessController.doPrivileged(new PrivilegedAction<File[]>() {
            @Override
            public File[] run() {
                return theBundle.getDataFile("").listFiles();
            }
        });

        // Create unique cache suffix for this rar file
        String cacheSuffix = "-.-." + id + "--.-";
        String cacheSuffixMod = cacheSuffix + Long.valueOf(FileUtils.fileLastModified(new File(rarFilePath))).toString();
        if (trace && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "cacheSuffixMod " + cacheSuffixMod);
        }

        // Clean out cache directories that are for a different version of the rar file than the current one.
        // ONLY clean out cache directories for this ID, there may be other rar files specified for this server
        for (File f : cacheFiles) {
            if (trace && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "bundle file path: " + f.getPath());
            }
            if (f.getPath().contains(cacheSuffixMod)) {
                // keep it
                if (trace && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "This is a good cache dir for the rar file, keep it");
                }
            } else {
                // if the directory does not have the correct suffix but does have the id then it
                // must be a version different from the current rar file, delete it
                if (f.getPath().contains(cacheSuffix)) {
                    if (trace && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "This is a cache dir for " + cacheSuffix + " but the modification date is not a match, delete it");
                    }
                    if (!deleteBundleCacheDir(f)) {
                        if (trace && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "Delete failed for ", f, " check previous debug messages");
                        }
                    }
                } else {
                    if (trace && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "This is NOT a cache dir for " + cacheSuffix + " leave it alone");
                    }
                }
            }
        }

        // getDataFile will return reference to existing file or create it if it does not exist
        File rarCacheDir = bundle.getDataFile("rarCache" + cacheSuffixMod);
        if (trace && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "getDataFile rarCacheDir " + rarCacheDir);
        }
        if (!FileUtils.ensureDirExists(rarCacheDir)) {
            // TODO This should really be Tr.error(tc, "jca.msg.id", bundle, ctx);
            if (trace && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Cache failed!!!");
            }
        }
        File rarOverlayDirectory = bundle.getDataFile("rarOverlayDirectory" + cacheSuffixMod);
        if (trace && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "getDataFile rarOverlayDirectory " + rarOverlayDirectory);
        }
        if (!FileUtils.ensureDirExists(rarOverlayDirectory)) {
            // TODO This should really be Tr.error(tc, "jca.msg.id", bundle, ctx);
            if (trace && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Overlay failed!!!");
            }
        }
        File rarCacheDirForOverlayContent = bundle.getDataFile("rarCacheDirForOverlayContent" + cacheSuffixMod);
        if (trace && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "getDataFile rarCacheDirForOverlayContent " + rarCacheDirForOverlayContent);
        }
        if (!FileUtils.ensureDirExists(rarCacheDirForOverlayContent)) {
            // TODO This should really be Tr.error(tc, "jca.msg.id", bundle, ctx);
            if (trace && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Cache for overlay failed!!!");
            }
        }

        // Get artifact containers for the rar file classes and for the jars in the rar file
        ArtifactContainer c = null;

        List<ArtifactContainer> rarContainers = new ArrayList<ArtifactContainer>();

        c = _acf.getContainer(rarCacheDir, new File(rarFilePath));
        rarContainers.add(c);
        Iterator<ArtifactEntry> i = c.iterator();
        // TODO this is only working for jars that are in the root, i.e. there is no path in front of the jar
        while (i.hasNext()) {
            ArtifactEntry ae = i.next();
            if (ae.getPath().toLowerCase().endsWith(".jar")) {
                if (trace && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Artifact entry [ " + ae.getPath() + " ]");
                }
                ArtifactContainer jarEntry = ae.convertToContainer();
                rarContainers.add(jarEntry);
                if (trace && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Jar entry [ " + jarEntry.getPath() + " ]");
                }
            }
        }

        // Convert artifact containers to adaptable containers
        List<Container> classLoaderContainers = new ArrayList<Container>();

        for (ArtifactContainer ac : rarContainers) {
            classLoaderContainers.add(_amf.getContainer(rarOverlayDirectory, rarCacheDirForOverlayContent, ac));
        }

        // Create the class loader
        GatewayConfiguration gwCfg = classloadingSvc.createGatewayConfiguration().setApiTypeVisibility(ApiType.SPEC, ApiType.API, ApiType.IBMAPI,
                                                                                                       ApiType.THIRDPARTY,
                                                                                                       ApiType.STABLE).setDynamicImportPackage("*").setDelegateToSystem(true);

        rarClassLoaderId = classloadingSvc.createIdentity(DeferredService.RESOURCE_ADAPTER_DOMAIN, id);
        if (trace && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Resource adapter [ " + rarFilePath + " ]: Class loader ID [ " + rarClassLoaderId + " ]");
        }

        ClassLoaderConfiguration clCfg = classloadingSvc.createClassLoaderConfiguration().setId(rarClassLoaderId);

        ClassLoader rarClassLoader = classloadingSvc.createTopLevelClassLoader(classLoaderContainers, gwCfg, clCfg);

        if (trace && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Resource adapter [ " + rarFilePath + " ]: Class loader [ " + rarClassLoader + " ]");
        }

        return rarClassLoader;
    }

    /**
     * Deletes the directory and its contents or the file that is specified
     *
     * @param path to cache directory to delete
     * @return true if path was deleted or if it did not exist
     */
    private boolean deleteBundleCacheDir(File path) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        if (FileUtils.fileExists(path)) {
            if (trace && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Path specified exists: " + path.getPath());
            }
        } else {
            if (trace && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Path specified does not exist: " + path.getPath());
            }
            return true;
        }

        boolean deleteWorked = true;
        for (File file : FileUtils.listFiles(path)) {
            if (FileUtils.fileIsDirectory(file)) {
                if (trace && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Delete directory contents: " + file.toString());
                }
                deleteWorked &= deleteBundleCacheDir(file);
            } else {
                if (trace && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Delete file: " + file.toString());
                }
                if (!FileUtils.fileDelete(file)) {
                    deleteWorked = false;
                    if (trace && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Directory or file not deleted");
                    }
                }
            }
        }
        if (trace && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Delete path: " + path);
        }
        if (!FileUtils.fileDelete(path)) {
            deleteWorked = false;
            if (trace && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Path not deleted");
            }
        }
        return deleteWorked;
    }

    /**
     * @see com.ibm.ws.classloading.ClassProvider#getDelegateLoader()
     */
    @Override
    public LibertyClassLoader getDelegateLoader() {
        return (LibertyClassLoader) classloader;
    }

    /**
     * @see org.osgi.service.metatype.MetaTypeProvider#getLocales()
     */
    @Override
    public String[] getLocales() {
        return null; // because ra.xml doesn't have Locale-specific names/descriptions.
    }

    /**
     * @see org.osgi.service.metatype.MetaTypeProvider#getObjectClassDefinition(java.lang.String, java.lang.String)
     */
    @Override
    public ObjectClassDefinition getObjectClassDefinition(String pid, String locale) {
        if (metatype != null)
            return metatype.getOcdById(pid).getObjectClassDefinition();
        else
            return null;
    }

    /**
     * Declarative Services method for setting the class loading service
     *
     * @param svc the service
     */
    protected void setClassLoadingService(ClassLoadingService svc) {
        classloadingSvc = svc;
    }

    /**
     * Sets the generated metatype that is provided by this resource adapter.
     *
     * @param metatype generated metatype to be provided by this resource adapter.
     */
    void setMetatype(Metatype metatype) {
        this.metatype = metatype;
    }

    /**
     * Declarative Services method for unsetting the class loading service
     *
     * @param svc the service
     */
    protected void unsetClassLoadingService(ClassLoadingService svc) {
        classloadingSvc = null;
    }

    /**
     * Declarative Services method for setting the artifact container factory service
     *
     * @param svc the service
     */
    protected void setArtifactContainerFactory(ArtifactContainerFactory svc) {
        _acf = svc;
    }

    /**
     * Declarative Services method for unsetting the artifact container factory service
     *
     * @param svc the service
     */
    protected void unsetArtifactContainerFactory(ArtifactContainerFactory svc) {
        if (svc == _acf)
            _acf = null;
    }

    /**
     * Declarative Services method for setting the adaptable module factory service
     *
     * @param svc the service
     */
    protected void setAdaptableModuleFactory(AdaptableModuleFactory svc) {
        _amf = svc;
    }

    /**
     * Declarative Services method for unsetting the adaptable module factory service
     *
     * @param svc the service
     */
    protected void unsetAdaptableModuleFactory(AdaptableModuleFactory svc) {
        if (svc == _amf)
            _amf = null;
    }

    /**
     * Declarative Services method for setting the resource adapter bundle service
     *
     * @param svc the service
     */
    protected void setResourceAdapterBundleService(ResourceAdapterBundleService svc) {
        raBundleSvc = svc;
    }

    /**
     * Declarative Services method for unsetting the resource adapter bundle service
     *
     * @param svc the service
     */
    protected void unsetResourceAdapterBundleService(ResourceAdapterBundleService svc) {

        if (svc == raBundleSvc)
            raBundleSvc = null;
    }

    protected void setClassLoader(ClassLoader cl) {
        classloader = cl;
    }

    /**
     * @return the ResourceAdapterMetaData
     */
    public ResourceAdapterMetaData getResourceAdapterMetaData() {
        return ramd;
    }

    /**
     * @param ramd the ResourceAdapterMetaData to set
     */
    public void setResourceAdapterMetaData(ResourceAdapterMetaData ramd) {
        this.ramd = ramd;
    }

}
