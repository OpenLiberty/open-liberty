/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.internal;

import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.ID_HTTP_ENDPOINT;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.ID_SSL;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.ID_VIRTUAL_HOST;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.stream.XMLStreamException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.adaptable.module.structure.StructureHelper;
import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppServices;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoBase;
import com.ibm.ws.app.manager.module.internal.ExtendedModuleInfoImpl;
import com.ibm.ws.app.manager.module.internal.ModuleClassLoaderFactory;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.app.manager.module.internal.ModuleInfoUtils;
import com.ibm.ws.app.manager.springboot.container.SpringBootConfig;
import com.ibm.ws.app.manager.springboot.container.SpringBootConfigFactory;
import com.ibm.ws.app.manager.springboot.container.config.ConfigElement;
import com.ibm.ws.app.manager.springboot.container.config.KeyStore;
import com.ibm.ws.app.manager.springboot.container.config.ServerConfiguration;
import com.ibm.ws.app.manager.springboot.container.config.SpringConfiguration;
import com.ibm.ws.app.manager.springboot.container.config.VirtualHost;
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
    final CountDownLatch applicationReadyLatch = new CountDownLatch(2);

    final class SpringModuleContainerInfo extends ModuleContainerInfoBase {
        public SpringModuleContainerInfo(List<Container> springBootSupport, ModuleHandler moduleHandler, List<ModuleMetaDataExtender> moduleMetaDataExtenders,
                                         List<NestedModuleMetaDataFactory> nestedModuleMetaDataFactories,
                                         Container moduleContainer, Entry altDDEntry,
                                         String moduleURI,
                                         ModuleClassLoaderFactory moduleClassLoaderFactory,
                                         ModuleClassesInfoProvider moduleClassesInfo,
                                         List<ContainerInfo> containerInfos) throws UnableToAdaptException {
            super(moduleHandler, moduleMetaDataExtenders, nestedModuleMetaDataFactories, moduleContainer, altDDEntry, moduleURI, ContainerInfo.Type.WEB_MODULE, moduleClassLoaderFactory, moduleClassesInfo, WebApp.class);
            this.classesContainerInfo.addAll(containerInfos);
        }

        @Override
        public ExtendedModuleInfoImpl createModuleInfoImpl(ApplicationInfo appInfo, ModuleClassLoaderFactory classLoaderFactory) throws MetaDataException {
            try {
                SpringBootModuleInfo springModuleInfo = new SpringBootModuleInfo(appInfo, moduleName, name, container, altDDEntry, classesContainerInfo, classLoaderFactory, getSpringBootApplication());
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
        public <T> void configure(ServerConfiguration config, T helperParam, Class<T> type, SpringConfiguration additionalConfig) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "SpringConfiguration Info = " + additionalConfig);
            }
            if (tc.isWarningEnabled()) {
                //WRN about some configurations we don't currently support.
                if (additionalConfig.isCompression_configured_in_spring_app()) {
                    Tr.warning(tc, "warning.spring_config.ignored.compression");
                }
                if (additionalConfig.isSession_configured_in_spring_app()) {
                    Tr.warning(tc, "warning.spring_config.ignored.session");
                }
            }
            if (!config.getSsls().isEmpty() && !isSSLEnabled()) {
                throw new IllegalStateException(Tr.formatMessage(tc, "error.missing.ssl"));
            }
            ContainerInstanceFactory<T> containerInstanceFactory = factory.getContainerInstanceFactory(type);
            if (containerInstanceFactory == null) {
                throw new IllegalStateException("No configuration helper found for: " + type);
            }
            if (!serverConfig.compareAndSet(null, config)) {
                throw new IllegalStateException("Server configuration already set.");
            }

            String virtualHostId = "default_host";
            List<VirtualHost> virtualHosts = config.getVirtualHosts();
            if (!virtualHosts.isEmpty()) {
                virtualHostId = config.getVirtualHosts().iterator().next().getId();
            }

            try {
                if (!configInstance.compareAndSet(null, containerInstanceFactory.intialize(SpringBootApplicationImpl.this, id, virtualHostId, helperParam, additionalConfig))) {
                    throw new IllegalStateException("Config instance already created.");
                }
            } catch (IOException | UnableToAdaptException | MetaDataException e) {
                throw new IllegalArgumentException(e);
            }
        }

        private boolean isSSLEnabled() {
            Bundle systemBundle = factory.getBundleContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
            FrameworkWiring fwkWiring = systemBundle.adapt(FrameworkWiring.class);
            Collection<BundleCapability> packages = fwkWiring.findProviders(new Requirement() {

                @Override
                public Resource getResource() {
                    return null;
                }

                @Override
                public String getNamespace() {
                    return PackageNamespace.PACKAGE_NAMESPACE;
                }

                @Override
                public Map<String, String> getDirectives() {
                    return Collections.singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(" + PackageNamespace.PACKAGE_NAMESPACE + "=com.ibm.ws.ssl)");
                }

                @Override
                public Map<String, Object> getAttributes() {
                    return Collections.emptyMap();
                }
            });
            return !packages.isEmpty();
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

            checkExistingConfig(config);

            if (config.getVirtualHosts().isEmpty()) {
                if (!instance.isEndpointConfigured()) {
                    //use app configured port with default_host
                    virtualHostConfig.updateAndGet((b) -> installVirtualHostBundle(b, config));
                }
            } else {
                //use app configured port with custom virtual host
                virtualHostConfig.updateAndGet((b) -> installVirtualHostBundle(b, config));
            }
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

        private void checkExistingConfig(ServerConfiguration libertyConfig) {
            String requestedPort = libertyConfig.getHttpEndpoints().get(0).getId().substring(ID_HTTP_ENDPOINT.length());

            // Checks ConfigurationAdmin to see if the ID for the following
            // exist.  This is done in priority order because if a higher
            // priority configuration is found then we remove all the lower
            // priority elements as well as the element with the matching ID
            // 1. <virtualHost/> - highest priority
            if (!libertyConfig.getVirtualHosts().isEmpty()) {
                if (checkVirtualHost(libertyConfig, requestedPort)) {
                    // found matching ID for <virtualHost/> return because we cleared out the rest
                    return;
                }
            }

            // 2. <httpEndpoint/> - second priority
            if (checkHttpEndpoint(libertyConfig, requestedPort)) {
                // found matching ID for <httpEndpoint/> return because we cleared out the other
                // lower priority elements
                return;
            }
            // 3. <ssl/> - third priority
            if (checkSsl(libertyConfig, requestedPort)) {
                // found matching ID for <ssl/> return because we cleared out the other
                // lower priority elements
                return;
            }
            // 4. <keyStore/> - forth priority, this checks for both the KeyStore and TrustStore
            checkKeyStores(libertyConfig);
        }

        private boolean checkVirtualHost(ServerConfiguration sc, String requestedPort) {
            String virtualHostFilter = createFilter("com.ibm.ws.http.virtualhost", ID_VIRTUAL_HOST + requestedPort);
            return checkConfigElement(virtualHostFilter, sc.getVirtualHosts(), sc.getHttpEndpoints(), sc.getSsls(), sc.getKeyStores());
        }

        private boolean checkHttpEndpoint(ServerConfiguration sc, String requestedPort) {
            String endpointFilter = createFilter("com.ibm.ws.http", ID_HTTP_ENDPOINT + requestedPort);
            return checkConfigElement(endpointFilter, sc.getHttpEndpoints(), sc.getSsls(), sc.getKeyStores());
        }

        private boolean checkSsl(ServerConfiguration sc, String requestedPort) {
            String sslFilter = createFilter("com.ibm.ws.ssl.repertoire", ID_SSL + requestedPort);
            return checkConfigElement(sslFilter, sc.getSsls(), sc.getKeyStores());
        }

        private boolean checkConfigElement(String filter, List<? extends ConfigElement> toCheck,
                                           @SuppressWarnings("rawtypes") List... lowerPriority) {
            boolean result = false;
            try {
                if (toCheck.isEmpty()) {
                    return result = true;
                }
                Configuration[] existing = deployedAppServices.getConfigurationAdmin().listConfigurations(filter);
                return result = existing != null && existing.length > 0;
            } catch (IOException | InvalidSyntaxException e) {
                // Auto FFDC here, this will happen because of a defect
                throw new RuntimeException(e);
            } finally {
                if (result) {
                    // no toCheck elements, or found match; clear everything out
                    toCheck.clear();
                    for (List<?> toClear : lowerPriority) {
                        toClear.clear();
                    }
                }
            }
        }

        private String createFilter(String factoryPid, String id) {
            StringBuilder sb = new StringBuilder();
            sb.append("(&");
            sb.append('(').append(ConfigurationAdmin.SERVICE_FACTORYPID).append('=').append(factoryPid).append(')');
            sb.append('(').append("id=").append(id).append(')');
            sb.append(')');
            return sb.toString();
        }

        private boolean checkKeyStores(ServerConfiguration sc) {
            boolean result = false;
            List<KeyStore> keyStores = sc.getKeyStores();
            for (Iterator<KeyStore> iKeyStores = keyStores.iterator(); iKeyStores.hasNext();) {
                KeyStore keyStore = iKeyStores.next();
                if (checkKeyStore(keyStore)) {
                    iKeyStores.remove();
                    result = true;
                }
            }

            return result;
        }

        /**
         * @param keyStore
         * @return
         */
        private boolean checkKeyStore(KeyStore keyStore) {
            try {
                String filter = createFilter("com.ibm.ws.ssl.keystore", keyStore.getId());
                Configuration[] existing = deployedAppServices.getConfigurationAdmin().listConfigurations(filter);
                return existing != null && existing.length > 0;
            } catch (IOException | InvalidSyntaxException e) {
                // Auto FFDC here, this will happen because of a defect
                throw new RuntimeException(e);
            }

        }

        @FFDCIgnore(IOException.class)
        private String getDefaultInstances(ServerConfiguration libertyConfig, String configId) {
            if (libertyConfig.getVirtualHosts().size() > 1) {
                throw new IllegalStateException("Only one virtualHost is allowed: " + libertyConfig.getVirtualHosts());
            }
            if (libertyConfig.getHttpEndpoints().size() > 1) {
                throw new IllegalStateException("Only one httpEndpoint is allowed: " + libertyConfig.getHttpEndpoints());
            }
            if (libertyConfig.getSsls().size() > 1) {
                throw new IllegalStateException("Only one ssl is allowed: " + libertyConfig.getSsls());
            }
            StringWriter result = new StringWriter();
            try {
                ServerConfigurationWriter.getInstance().write(libertyConfig, result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (XMLStreamException e) {
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
    private final DeployedAppServices deployedAppServices;
    private final Throwable initError;
    private final SpringModuleContainerInfo springContainerModuleInfo;
    private final AtomicReference<ServiceRegistration<SpringBootConfigFactory>> springBootConfigReg = new AtomicReference<>();
    private final AtomicInteger nextConfigId = new AtomicInteger(0);
    private final int id;
    private final Set<Runnable> shutdownHooks = new CopyOnWriteArraySet<>();
    private final AtomicBoolean uninstalled = new AtomicBoolean();
    private final List<String> appArgs;
    private volatile AtomicReference<String> applicationActivated;

    public SpringBootApplicationImpl(ApplicationInformation<DeployedAppInfo> applicationInformation,
                                     SpringBootApplicationFactory factory,
                                     DeployedAppServices deployedAppServices, int id) throws UnableToAdaptException {
        super(applicationInformation, deployedAppServices);
        this.id = id;
        this.factory = factory;
        this.deployedAppServices = deployedAppServices;
        this.applicationInformation = applicationInformation;
        SpringBootManifest manifest = null;
        ArtifactContainer newContainer = null;
        List<ContainerInfo> infos = null;
        SpringModuleContainerInfo mci = null;
        Throwable error = null;

        try {
            newContainer = storeLibs(applicationInformation, getRawContainer(applicationInformation), manifest, factory, deployedAppServices);
            manifest = getSpringBootManifest(applicationInformation);
            infos = getContainerInfos(applicationInformation.getContainer(), factory, manifest);
            String moduleURI = ModuleInfoUtils.getModuleURIFromLocation(applicationInformation.getLocation());
            mci = new SpringModuleContainerInfo(factory.getSpringBootSupport(), factory.getModuleHandler(), deployedAppServices.getModuleMetaDataExtenders("web"), deployedAppServices.getNestedModuleMetaDataFactories("web"), applicationInformation.getContainer(), null, moduleURI, this, moduleClassesInfo, infos);
            moduleContainerInfos.add(mci);
        } catch (UnableToAdaptException e) {
            error = e;
        }
        this.springBootManifest = manifest;
        this.rawContainer = newContainer;
        this.springContainerModuleInfo = mci;
        this.initError = error;
        Object appArgsProp = applicationInformation.getConfigProperty(SpringConstants.APP_ARGS);
        if (appArgsProp instanceof String[]) {
            // make a copy of the args
            appArgs = Collections.unmodifiableList(new ArrayList<>(Arrays.asList((String[]) appArgsProp)));
        } else {
            appArgs = Collections.emptyList();
        }
    }

    private static SpringBootManifest getSpringBootManifest(ApplicationInformation<DeployedAppInfo> appInfo) throws UnableToAdaptException {
        Entry manifestEntry = appInfo.getContainer().getEntry(JarFile.MANIFEST_NAME);
        if (manifestEntry == null) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "error.no.manifest.found", appInfo.getName()));
        }
        try (InputStream mfIn = manifestEntry.adapt(InputStream.class)) {
            SpringBootManifest sbm = new SpringBootManifest(new Manifest(mfIn));
            if (sbm.getSpringStartClass() == null) {
                throw new IllegalArgumentException(Tr.formatMessage(tc, "error.no.spring.class.found"));
            }
            return sbm;
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
                                               SpringBootApplicationFactory factory,
                                               DeployedAppServices deployedAppServices) {
        String location = applicationInformation.getLocation();
        if (location.toLowerCase().endsWith(".xml")) {
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
        if (springAppFile.isDirectory()) {
            // for extracted applications; use it as-is
            // assume deployer knows what they are doing; don't interfere
            return rawContainer;
        }

        // Make sure the spring thin apps directory is available
        WsResource thinAppsDir = deployedAppServices.getLocationAdmin().resolveResource(SPRING_THIN_APPS_DIR);
        thinAppsDir.create();

        WsResource thinSpringAppResource = deployedAppServices.getLocationAdmin().resolveResource(SPRING_THIN_APPS_DIR + applicationInformation.getName() + "." + SPRING_APP_TYPE);
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
            ArtifactContainer artifactContainer = setupArtifactContainer(thinSpringAppFile, factory, deployedAppServices);
            container = setupContainer(applicationInformation.getPid(), artifactContainer, factory, deployedAppServices);
            applicationInformation.setContainer(container);
            return artifactContainer;
        } catch (NoSuchAlgorithmException | IOException e) {
            // Log warning and continue to use the container for the SPRING file
            Tr.warning(tc, "warning.could.not.thin.application", applicationInformation.getName(), e.getMessage());
        }
        return rawContainer;
    }

    private static void thinSpringApp(LibIndexCache libIndexCache, File springAppFile, File thinSpringAppFile, long lastModified) throws IOException, NoSuchAlgorithmException {
        File parent = libIndexCache.getLibIndexParent();
        File workarea = libIndexCache.getLibIndexWorkarea();
        try (SpringBootThinUtil springBootThinUtil = new SpringBootThinUtil(springAppFile, thinSpringAppFile, workarea, parent)) {
            springBootThinUtil.execute();
        }
        thinSpringAppFile.setLastModified(lastModified);
    }

    private static ArtifactContainer setupArtifactContainer(File f, SpringBootApplicationFactory factory,
                                                            DeployedAppServices deployedAppServices) throws IOException {
        File cacheDir = factory.getDataDir("cache");
        return deployedAppServices.getArtifactFactory().getContainer(cacheDir, f);
    }

    private static Container setupContainer(String pid, ArtifactContainer artifactContainer, SpringBootApplicationFactory factory,
                                            DeployedAppServices deployedAppServices) throws IOException {
        File cacheAdapt = factory.getDataDir("cacheAdapt");
        File cacheOverlay = factory.getDataDir("cacheOverlay");
        return deployedAppServices.getModuleFactory().getContainer(cacheAdapt, cacheOverlay, artifactContainer);
    }

    Throwable getError() {
        return initError;
    }

    SpringBootManifest getSpringBootManifest() {
        return springBootManifest;
    }

    List<String> getAppArgs() {
        return appArgs;
    }

    @Override
    public Container createContainerFor(String id) throws IOException, UnableToAdaptException {
        Container container = setupContainer(applicationInformation.getPid(), rawContainer, factory, deployedAppServices);
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
        ExtendedApplicationInfo appInfo = appInfoFactory.createApplicationInfo(getName(),
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
        try {
            return super.uninstallApp();
        } finally {
            AtomicReference<String> current = applicationActivated;
            if (current != null) {
                current.compareAndSet(applicationInformation.getName(), null);
            }
        }
    }

    private String getVirtualHostConfig(String start, String virtualHostId, String end) {
        StringBuilder builder = new StringBuilder(start);
        builder.append(virtualHostId);
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
            final SpringBootThinUtil.StarterFilter starterFilter = SpringBootThinUtil.getStarterFilter(stringStream(libContainer));
            if (libContainer != null) {
                for (Entry entry : libContainer) {
                    if (!starterFilter.apply(entry.getName())) {
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

    public static Stream<String> stringStream(Container container) {
        Stream<String> stream = StreamSupport.stream(container.spliterator(), false).map(entry -> entry.getName());
        return stream;
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
            // We want the first item to be at the end of the class path for a spring application
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

    @FFDCIgnore(IllegalStateException.class)
    void unregisterSpringConfigFactory() {
        try {
            springBootConfigReg.updateAndGet((r) -> {
                if (r != null) {
                    r.unregister();
                }
                return null;
            });
        } catch (IllegalStateException e) {
            // can happen if our bundle stopped first; just ignore
        }
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
        return deployedAppServices.getLocationAdmin().resolveResource(WsLocationConstants.SYMBOL_SERVER_CONFIG_DIR).asFile();
    }

    void setApplicationActivated(AtomicReference<String> applicationActivated) {
        this.applicationActivated = applicationActivated;
    }

    protected final ClassLoadingService getClassLoadingService() {
        return this.classLoadingService;
    }

    @Override
    public boolean postDeployApp(Future<Boolean> result) {
        try {
            // Ensure that the liberty module started event has time to fire before postDeploy().
            Integer waitTime = new Integer(5);
            applicationReadyLatch.await(waitTime.intValue(), TimeUnit.MINUTES);
            if (applicationReadyLatch.getCount() > 0) {
                Tr.audit(tc, "warning.application.started.event.timeout", applicationInformation.getName(),
                         "ApplicationReadyEvent", waitTime);
            }
        } catch (InterruptedException e) {
            // Allow FFDC
        }
        return super.postDeployApp(result);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.app.manager.springboot.container.SpringBootConfigFactory#getContextStartedLatch()
     */
    @Override
    public CountDownLatch getApplicationReadyLatch() {
        return applicationReadyLatch;
    }
}
