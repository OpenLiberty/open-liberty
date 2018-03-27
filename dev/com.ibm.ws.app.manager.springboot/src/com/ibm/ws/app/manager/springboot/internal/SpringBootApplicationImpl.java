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
package com.ibm.ws.app.manager.springboot.internal;

import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_APP_TYPE;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_BOOT_CONFIG_BUNDLE_PREFIX;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_BOOT_CONFIG_NAMESPACE;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_THIN_APPS_DIR;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.XMI_BND_NAME;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.XMI_VIRTUAL_HOST_END;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.XMI_VIRTUAL_HOST_START;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.XML_BND_NAME;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.XML_VIRTUAL_HOST_END;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.XML_VIRTUAL_HOST_START;
import static com.ibm.ws.app.manager.springboot.util.SpringBootThinUtil.SPRING_LIB_INDEX_FILE;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.xml.bind.JAXBException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.startlevel.BundleStartLevel;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.adaptable.module.structure.StructureHelper;
import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoBase;
import com.ibm.ws.app.manager.module.internal.ExtendedModuleInfoImpl;
import com.ibm.ws.app.manager.module.internal.ModuleClassLoaderFactory;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.app.manager.module.internal.ModuleInfoUtils;
import com.ibm.ws.app.manager.springboot.container.SpringBootConfig;
import com.ibm.ws.app.manager.springboot.container.SpringBootConfigFactory;
import com.ibm.ws.app.manager.springboot.container.config.ServerConfiguration;
import com.ibm.ws.app.manager.springboot.support.ContainerInstanceFactory;
import com.ibm.ws.app.manager.springboot.support.ContainerInstanceFactory.Instance;
import com.ibm.ws.app.manager.springboot.support.SpringBootApplication;
import com.ibm.ws.app.manager.springboot.util.SpringBootManifest;
import com.ibm.ws.app.manager.springboot.util.SpringBootThinUtil;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo.Type;
import com.ibm.ws.container.service.app.deploy.ManifestClassPathUtils;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.extended.ModuleMetaDataExtender;
import com.ibm.ws.container.service.metadata.extended.NestedModuleMetaDataFactory;
import com.ibm.ws.dynamic.bundle.BundleFactory;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.wsspi.adaptable.module.AddEntryToOverlay;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.InterpretedContainer;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.classloading.GatewayConfiguration;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

public class SpringBootApplicationImpl extends DeployedAppInfoBase implements SpringBootConfigFactory, SpringBootApplication {
    private static final TraceComponent tc = Tr.register(SpringBootApplicationImpl.class);

    final class SpringModuleContainerInfo extends ModuleContainerInfoBase {
        public SpringModuleContainerInfo(List<Container> springBootSupport, ModuleHandler moduleHandler, List<ModuleMetaDataExtender> moduleMetaDataExtenders,
                                         List<NestedModuleMetaDataFactory> nestedModuleMetaDataFactories,
                                         Container moduleContainer, Entry altDDEntry,
                                         String moduleURI, ModuleClassesInfoProvider moduleClassesInfo,
                                         List<ContainerInfo> containerInfos) throws UnableToAdaptException {
            super(moduleHandler, moduleMetaDataExtenders, nestedModuleMetaDataFactories, moduleContainer, altDDEntry, moduleURI, ContainerInfo.Type.WEB_MODULE, moduleClassesInfo, WebApp.class);
            this.classesContainerInfo.addAll(containerInfos);
        }

        @Override
        public ExtendedModuleInfoImpl createModuleInfoImpl(ApplicationInfo appInfo,
                                                           ModuleClassLoaderFactory moduleClassLoaderFactory) throws MetaDataException {
            try {
                SpringBootModuleInfo springModuleInfo = new SpringBootModuleInfo(appInfo, moduleName, name, container, altDDEntry, classesContainerInfo, moduleClassLoaderFactory, getSpringBootApplication());
                return springModuleInfo;
            } catch (UnableToAdaptException e) {
                FFDCFilter.processException(e, getClass().getName(), "createModuleInfo", this);
                return null;
            }
        }

        SpringBootApplicationImpl getSpringBootApplication() {
            return SpringBootApplicationImpl.this;
        }
    }

    static class ContainerInfoImpl implements ContainerInfo {
        private final Type type;
        private final String name;
        private final Container container;

        public ContainerInfoImpl(Type type, String name, Container container) {
            super();
            this.type = type;
            this.name = name;
            this.container = container;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Container getContainer() {
            return container;
        }
    }

    final class SpringBootConfigImpl implements SpringBootConfig {
        private final String id;
        private final AtomicReference<ServerConfiguration> serverConfig = new AtomicReference<>();
        private final AtomicReference<Bundle> virtualHostConfig = new AtomicReference<>();
        private final AtomicReference<Instance> configInstance = new AtomicReference<>();

        /**
         * @param contextRoot
         * @param id
         */
        public SpringBootConfigImpl(int id) {
            this.id = SpringBootApplicationImpl.this.id + "-" + id;
        }

        @Override
        public <T> void configure(ServerConfiguration config, T helperParam, Class<T> type) {
            ContainerInstanceFactory<T> containerInstanceFactory = factory.getContainerInstanceFactory(type);
            if (containerInstanceFactory == null) {
                throw new IllegalStateException("No configuration helper found for: " + type);
            }
            if (!serverConfig.compareAndSet(null, config)) {
                throw new IllegalStateException("Server configuration already set.");
            }
            try {
                if (!configInstance.compareAndSet(null, containerInstanceFactory.intialize(SpringBootApplicationImpl.this, id, helperParam))) {
                    throw new IllegalStateException("Config instance already created.");
                }
            } catch (IOException | UnableToAdaptException | MetaDataException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public void start() {
            ServerConfiguration config = serverConfig.getAndSet(null);
            if (config == null) {
                throw new IllegalStateException("No server configuration set.");
            }
            Instance instance = configInstance.get();
            if (instance == null) {
                throw new IllegalStateException("No Config instance set.");
            }
            virtualHostConfig.updateAndGet((b) -> installVirtualHostBundle(b, config));
            instance.start();
        }

        @Override
        public void stop() {
            virtualHostConfig.getAndUpdate((b) -> {
                if (b != null) {
                    try {
                        // If the framework is stopping then we avoid uninstalling the bundle.
                        // This is necessary because config admin will no process the
                        // config bundle deletion while the framework is shutting down.
                        // Here we leave the bundle installed and we will clean it up
                        // on restart when the Spring Boot app handler is activated.
                        // This way the configurations can be removed before re-starting
                        // the spring boot applications
                        if (!FrameworkState.isStopping()) {
                            b.uninstall();
                        }
                    } catch (IllegalStateException e) {
                        // auto FFDC here
                    } catch (BundleException e) {
                        // auto FFDC here
                    }
                }
                return null;
            });
            configInstance.getAndUpdate((i) -> {
                if (i != null) {
                    i.stop();
                }
                return null;
            });
        }

        private Bundle installVirtualHostBundle(Bundle previous, ServerConfiguration libertyConfig) {
            if (previous != null) {
                try {
                    previous.uninstall();
                } catch (BundleException e) {
                    // auto FFDC here
                }
            }
            BundleContext context = SpringBootApplicationImpl.this.factory.getBundleContext();
            BundleFactory factory = new BundleFactory();
            String name = "com.ibm.ws.app.manager.springboot." + id;
            factory.setBundleName(name);
            factory.setBundleSymbolicName(name);
            factory.setBundleLocationPrefix(SPRING_BOOT_CONFIG_BUNDLE_PREFIX);
            factory.setBundleLocation("springBoot:" + id);
            factory.setDefaultInstance(getDefaultInstances(libertyConfig, id));
            factory.setBundleContext(context);
            factory.addManifestAttribute("IBM-Default-Config", Collections.singleton("OSGI-INF/wlp/defaultInstances.xml"));
            String configCap = SPRING_BOOT_CONFIG_NAMESPACE + "; " + SPRING_BOOT_CONFIG_NAMESPACE + "=\"" + id + "\"";
            factory.addManifestAttribute(Constants.PROVIDE_CAPABILITY, Collections.singleton(configCap));
            Bundle b = factory.createBundle();
            b.adapt(BundleStartLevel.class).setStartLevel(context.getBundle().adapt(BundleStartLevel.class).getStartLevel());
            try {
                b.start(Bundle.START_TRANSIENT);
            } catch (BundleException e) {
                throw new IllegalStateException(e);
            }
            return b;
        }

        @FFDCIgnore(IOException.class)
        private String getDefaultInstances(ServerConfiguration libertyConfig, String configId) {
            if (libertyConfig.getHttpEndpoints().size() != 1) {
                throw new IllegalStateException("Only one httpEndpoint is allowed: " + libertyConfig.getHttpEndpoints());
            }
            if (libertyConfig.getVirtualHosts().size() != 1) {
                throw new IllegalStateException("Only one virtualHost is allowed: " + libertyConfig.getVirtualHosts());
            }
            StringWriter result = new StringWriter();
            try {
                ServerConfigurationWriter.getInstance().write(libertyConfig, result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
            return result.toString();
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.app.manager.springboot.container.SpringBootConfig#getId()
         */
        @Override
        public String getId() {
            return this.id;
        }

    }

    private final ApplicationInformation<DeployedAppInfo> applicationInformation;
    private final ArtifactContainer rawContainer;
    private final SpringBootManifest springBootManifest;
    private final SpringBootApplicationFactory factory;
    private final Throwable initError;
    private final SpringModuleContainerInfo springContainerModuleInfo;
    private final AtomicReference<ServiceRegistration<SpringBootConfigFactory>> springBootConfigReg = new AtomicReference<>();
    private final AtomicInteger nextConfigId = new AtomicInteger(0);
    private final int id;
    private final Set<Runnable> shutdownHooks = new CopyOnWriteArraySet<>();
    private final AtomicBoolean uninstalled = new AtomicBoolean();

    public SpringBootApplicationImpl(ApplicationInformation<DeployedAppInfo> applicationInformation, SpringBootApplicationFactory factory, int id) throws UnableToAdaptException {
        super(applicationInformation, factory);
        this.id = id;
        this.factory = factory;
        this.applicationInformation = applicationInformation;
        SpringBootManifest manifest = null;
        ArtifactContainer newContainer = null;
        List<ContainerInfo> infos = null;
        SpringModuleContainerInfo mci = null;
        Throwable error = null;

        try {
            newContainer = storeLibs(applicationInformation, getRawContainer(applicationInformation), manifest, factory);
            manifest = getSpringBootManifest(applicationInformation.getContainer());
            infos = getContainerInfos(applicationInformation.getContainer(), factory, manifest);
            String moduleURI = ModuleInfoUtils.getModuleURIFromLocation(applicationInformation.getLocation());
            mci = new SpringModuleContainerInfo(factory.getSpringBootSupport(), factory.getModuleHandler(), factory.getModuleMetaDataExtenders().get("web"), factory.getNestedModuleMetaDataFactories().get("web"), applicationInformation.getContainer(), null, moduleURI, moduleClassesInfo, infos);
            moduleContainerInfos.add(mci);
        } catch (UnableToAdaptException e) {
            error = e;
        }
        this.springBootManifest = manifest;
        this.rawContainer = newContainer;
        this.springContainerModuleInfo = mci;
        this.initError = error;
    }

    private static SpringBootManifest getSpringBootManifest(Container container) throws UnableToAdaptException {
        Entry manifestEntry = container.getEntry(JarFile.MANIFEST_NAME);
        try (InputStream mfIn = manifestEntry.adapt(InputStream.class)) {
            return new SpringBootManifest(new Manifest(mfIn));
        } catch (IOException e) {
            throw new UnableToAdaptException(e);
        }
    }

    private static ArtifactContainer getRawContainer(ApplicationInformation<DeployedAppInfo> applicationInformation) throws UnableToAdaptException {
        // We are taking advantage of InterpretedContainer to get the delegate ArtifactContainer here
        // This is a bit of a hack.  Would be nicer to add an adaptor directly to Container
        // to return the ArtifactContainer: container.adapt(ArtifactContainer)
        Container container = applicationInformation.getContainer();
        AtomicReference<ArtifactContainer> rawContainer = new AtomicReference<>();
        container.adapt(InterpretedContainer.class).setStructureHelper(new StructureHelper() {
            @Override
            public boolean isValid(ArtifactContainer e, String path) {
                return false;
            }

            @Override
            public boolean isRoot(ArtifactContainer e) {
                rawContainer.set(e);
                return false;
            }
        });
        return rawContainer.get();
    }

    private static ArtifactContainer storeLibs(ApplicationInformation<DeployedAppInfo> applicationInformation, ArtifactContainer rawContainer,
                                               SpringBootManifest springBootManifest,
                                               SpringBootApplicationFactory factory) {
        String location = applicationInformation.getLocation();
        if (location.toLowerCase().endsWith("*.xml")) {
            // don't do this for loose applications
            return rawContainer;
        }

        Container container = applicationInformation.getContainer();
        Entry entry = container.getEntry(SPRING_LIB_INDEX_FILE);
        if (entry != null) {
            // pre-built index is available; use it as-is
            return rawContainer;
        }

        File springAppFile = new File(location);
        // Make sure the spring thin apps directory is available
        WsResource thinAppsDir = factory.getLocationAdmin().resolveResource(SPRING_THIN_APPS_DIR);
        thinAppsDir.create();

        WsResource thinSpringAppResource = factory.getLocationAdmin().resolveResource(SPRING_THIN_APPS_DIR + applicationInformation.getName() + "." + SPRING_APP_TYPE);
        File thinSpringAppFile = thinSpringAppResource.asFile();
        try {
            if (thinSpringAppFile.exists()) {
                // If the Spring app file has been changed, delete the thin app file
                if (thinSpringAppFile.lastModified() != springAppFile.lastModified()) {
                    thinSpringApp(factory.getLibIndexCache(), springAppFile, thinSpringAppFile, springAppFile.lastModified());
                }
            } else {
                thinSpringApp(factory.getLibIndexCache(), springAppFile, thinSpringAppFile, springAppFile.lastModified());
            }

            // Set up the new container pointing to the thin spring app file
            ArtifactContainer artifactContainer = setupArtifactContainer(thinSpringAppFile, factory);
            container = setupContainer(applicationInformation.getPid(), artifactContainer, factory);
            applicationInformation.setContainer(container);
            return artifactContainer;
        } catch (NoSuchAlgorithmException | IOException e) {
            // Log error and continue to use the container for the SPR file
            Tr.error(tc, "warning.could.not.thin.application", applicationInformation.getName(), e.getMessage());
        }
        return null;
    }

    private static void thinSpringApp(LibIndexCache libIndexCache, File springAppFile, File thinSpringAppFile, long lastModified) throws IOException, NoSuchAlgorithmException {
        File parent = libIndexCache.getLibIndexParent();
        File workarea = libIndexCache.getLibIndexWorkarea();
        SpringBootThinUtil springBootThinUtil = new SpringBootThinUtil(springAppFile, thinSpringAppFile, workarea, parent);
        springBootThinUtil.execute();
        thinSpringAppFile.setLastModified(lastModified);
    }

    private static ArtifactContainer setupArtifactContainer(File f, SpringBootApplicationFactory factory) throws IOException {
        File cacheDir = factory.getDataDir("cache");
        return factory.getArtifactFactory().getContainer(cacheDir, f);
    }

    private static Container setupContainer(String pid, ArtifactContainer artifactContainer, SpringBootApplicationFactory factory) throws IOException {
        File cacheAdapt = factory.getDataDir("cacheAdapt");
        File cacheOverlay = factory.getDataDir("cacheOverlay");
        return factory.getModuleFactory().getContainer(cacheAdapt, cacheOverlay, artifactContainer);
    }

    Throwable getError() {
        return initError;
    }

    SpringBootManifest getSpringBootManifest() {
        return springBootManifest;
    }

    @Override
    public Container createContainerFor(String id) throws IOException, UnableToAdaptException {
        Container container = setupContainer(applicationInformation.getPid(), rawContainer, factory);
        AddEntryToOverlay virtualHostBnd = container.adapt(AddEntryToOverlay.class);

        // Add both XML and XMI here incase an old web.xml file is used;
        // easier to just supply both here than figure out which to supply
        virtualHostBnd.add(XML_BND_NAME, getVirtualHostConfig(XML_VIRTUAL_HOST_START, id, XML_VIRTUAL_HOST_END));
        virtualHostBnd.add(XMI_BND_NAME, getVirtualHostConfig(XMI_VIRTUAL_HOST_START, id, XMI_VIRTUAL_HOST_END));
        return container;
    }

    @Override
    public ModuleClassesContainerInfo getSpringClassesContainerInfo() {
        return springContainerModuleInfo;
    }

    @Override
    public ClassLoader getClassLoader() {
        return springContainerModuleInfo.getClassLoader();
    }

    @Override
    public ExtendedApplicationInfo createApplicationInfo(String id, Container appContainer) {
        ExtendedApplicationInfo appInfo = appInfoFactory.createApplicationInfo(getName() + "." + id,
                                                                               springContainerModuleInfo.moduleName + "." + id,
                                                                               appContainer,
                                                                               null,
                                                                               null);
        return appInfo;
    }

    @Override
    public void destroyApplicationInfo(ExtendedApplicationInfo appInfo) {
        appInfoFactory.destroyApplicationInfo(appInfo);
    }

    @Override
    public boolean uninstallApp() {
        if (uninstalled.getAndSet(true)) {
            return true;
        }
        return super.uninstallApp();
    }

    private String getVirtualHostConfig(String start, String id, String end) {
        StringBuilder builder = new StringBuilder(start);
        builder.append("springVirtualHost-" + id);
        builder.append(end);
        return builder.toString();
    }

    private static List<ContainerInfo> getContainerInfos(Container container, SpringBootApplicationFactory factory, SpringBootManifest manifest) throws UnableToAdaptException {
        List<ContainerInfo> containerInfos = new ArrayList<>();
        Entry classesEntry = container.getEntry(manifest.getSpringBootClasses());
        if (classesEntry != null) {
            final Container classesContainer = classesEntry.adapt(Container.class);
            if (classesContainer != null) {
                ContainerInfo containerInfo = new ContainerInfoImpl(Type.WEB_INF_CLASSES, manifest.getSpringBootClasses(), classesContainer);
                containerInfos.add(containerInfo);
            }
        }
        Entry indexFile = container.getEntry(SPRING_LIB_INDEX_FILE);
        if (indexFile != null) {
            containerInfos.addAll(getStoredIndexClassesInfos(indexFile, factory.getLibIndexCache()));
        } else {
            containerInfos.addAll(getSpringBootLibs(container, manifest, new ArrayList<>()));
        }
        for (Container supportContainer : factory.getSpringBootSupport()) {
            Entry supportEntry = supportContainer.adapt(Entry.class);
            ContainerInfo containerInfo = new ContainerInfoImpl(Type.WEB_INF_LIB, manifest.getSpringBootLib() + '/' + supportEntry.getName(), supportContainer);
            containerInfos.add(containerInfo);
        }
        return Collections.unmodifiableList(containerInfos);
    }

    private static List<ContainerInfo> getSpringBootLibs(Container moduleContainer, SpringBootManifest manifest, ArrayList<String> resolved) throws UnableToAdaptException {
        List<ContainerInfo> result = new ArrayList<>();
        Entry libEntry = moduleContainer.getEntry(manifest.getSpringBootLib());
        if (libEntry != null) {
            Container libContainer = libEntry.adapt(Container.class);
            if (libContainer != null) {
                for (Entry entry : libContainer) {
                    if (!SpringBootThinUtil.isEmbeddedContainerImpl(entry.getName())) {
                        String jarEntryName = entry.getName();
                        Container jarContainer = entry.adapt(Container.class);
                        if (jarContainer != null) {
                            ContainerInfo containerInfo = new ContainerInfoImpl(Type.WEB_INF_LIB, manifest.getSpringBootLib() + '/' + jarEntryName, jarContainer);
                            result.add(containerInfo);
                            ManifestClassPathUtils.addCompleteJarEntryUrls(result, entry, resolved);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static List<ContainerInfo> getStoredIndexClassesInfos(Entry indexFile, LibIndexCache libIndexCache) throws UnableToAdaptException {
        List<ContainerInfo> result = new ArrayList<>();
        Map<String, String> indexMap = readIndex(indexFile);
        for (Map.Entry<String, String> entry : indexMap.entrySet()) {
            Container libContainer = libIndexCache.getLibraryContainer(entry);
            if (libContainer == null) {
                throw new UnableToAdaptException("No library found for:" + entry.getKey() + "=" + entry.getValue());
            }
            ContainerInfo containerInfo = new ContainerInfoImpl(Type.WEB_INF_LIB, entry.getKey(), libContainer);
            result.add(containerInfo);
        }
        return result;
    }

    private static Map<String, String> readIndex(Entry indexFile) throws UnableToAdaptException {
        Map<String, String> result = new LinkedHashMap<>();
        try (InputStream in = indexFile.adapt(InputStream.class)) {
            if (in != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split("=");
                    result.put(values[0], values[1]);
                }
            }
        } catch (IOException e) {
            throw new UnableToAdaptException(e);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo#getModuleClassesContainerInfo()
     */
    @Override
    public List<ModuleClassesContainerInfo> getModuleClassesContainerInfo() {
        return Collections.singletonList((ModuleClassesContainerInfo) springContainerModuleInfo);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.app.manager.module.internal.ModuleClassLoaderFactory#createModuleClassLoader(com.ibm.ws.container.service.app.deploy.ModuleInfo, java.util.List)
     */
    @Override
    public ClassLoader createModuleClassLoader(ModuleInfo moduleInfo, List<ContainerInfo> moduleClassesContainers) {
        if (moduleInfo instanceof SpringBootModuleInfo) {
            ApplicationInfo appInfo = moduleInfo.getApplicationInfo();
            String appName = appInfo.getDeploymentName();
            String moduleName = moduleInfo.getURI();
            ClassLoadingService cls = classLoadingService;
            List<Container> containers = new ArrayList<Container>();
            Iterator<ContainerInfo> infos = moduleClassesContainers.iterator();
            // We want the first item to be at the end of the class path for a spr
            if (infos.hasNext()) {
                infos.next();
                while (infos.hasNext()) {
                    containers.add(infos.next().getContainer());
                }
                // Add the first item to the end.
                containers.add(moduleClassesContainers.get(0).getContainer());
            }

            GatewayConfiguration gwCfg = cls.createGatewayConfiguration().setApplicationName(appName).setDynamicImportPackage("*");

            ProtectionDomain protectionDomain = getProtectionDomain();

            ClassLoaderConfiguration clCfg = cls.createClassLoaderConfiguration().setId(cls.createIdentity("SpringModule", appName + "#"
                                                                                                                           + moduleName)).setProtectionDomain(protectionDomain).setIncludeAppExtensions(true);

            return createTopLevelClassLoader(containers, gwCfg, clCfg);
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.app.manager.module.internal.DeployedAppInfoBase#createApplicationInfo()
     */
    @Override
    protected ExtendedApplicationInfo createApplicationInfo() {
        ExtendedApplicationInfo appInfo = appInfoFactory.createApplicationInfo(getName(),
                                                                               springContainerModuleInfo.moduleName,
                                                                               getContainer(),
                                                                               this,
                                                                               getConfigHelper());
        springContainerModuleInfo.moduleName = appInfo.getName();
        return appInfo;
    }

    void registerSpringConfigFactory() {
        // Register the SpringContainer service with the context
        // of the gateway bundle for the application.
        // Find the gateway bunlde by searching the hierarchy of the
        // the application classloader until a BundleReference is found.
        ClassLoader cl = springContainerModuleInfo.getClassLoader();
        while (cl != null && !(cl instanceof BundleReference)) {
            cl = cl.getParent();
        }
        if (cl == null) {
            throw new IllegalStateException("Did not find a BundleReference class loader.");
        }
        Bundle b = ((BundleReference) cl).getBundle();
        BundleContext context = b.getBundleContext();

        springBootConfigReg.updateAndGet((r) -> {
            if (r != null) {
                r.unregister();
            }
            return context.registerService(SpringBootConfigFactory.class, this, null);
        });
    }

    void unregisterSpringConfigFactory() {
        springBootConfigReg.updateAndGet((r) -> {
            if (r != null) {
                r.unregister();
            }
            return null;
        });
    }

    @Override
    public SpringBootConfig createSpringBootConfig() {
        return new SpringBootConfigImpl(nextConfigId.getAndIncrement());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.app.manager.springboot.container.SpringBootConfigFactory#addShutdownHook(java.lang.Runnable)
     */
    @Override
    public void addShutdownHook(Runnable hook) {
        shutdownHooks.add(hook);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.app.manager.springboot.container.SpringBootConfigFactory#removeShutdownHook(java.lang.Runnable)
     */
    @Override
    public void removeShutdownHook(Runnable hook) {
        shutdownHooks.remove(hook);
    }

    void callShutdownHooks() {
        for (Runnable hook : shutdownHooks) {
            try {
                hook.run();
            } catch (Throwable t) {
                // auto FFDC here and continue on
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.app.manager.springboot.container.SpringBootConfigFactory#rootContextClosed()
     */
    @Override
    public void rootContextClosed() {
        uninstallApp();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.app.manager.springboot.container.SpringBootConfigFactory#getServerDir()
     */
    @Override
    public File getServerDir() {
        return factory.getLocationAdmin().resolveResource(WsLocationConstants.SYMBOL_SERVER_CONFIG_DIR).asFile();
    }
}
