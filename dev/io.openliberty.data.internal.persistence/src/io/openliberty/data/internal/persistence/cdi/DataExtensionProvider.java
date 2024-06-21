/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package io.openliberty.data.internal.persistence.cdi;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import javax.sql.DataSource;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.cdi.extension.CDIExtensionMetadataInternal;
import com.ibm.ws.classloading.ClassLoaderIdentifierService;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.metadata.ApplicationMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory;
import com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.wsspi.resource.ResourceConfigFactory;
import com.ibm.wsspi.resource.ResourceFactory;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.data.internal.persistence.service.DataComponentMetaData;
import io.openliberty.data.internal.persistence.service.DataModuleMetaData;
import io.openliberty.data.internal.version.DataVersionCompatibility;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.persistence.EntityManagerFactory;

/**
 * Simulates a provider for relational databases by delegating
 * JPQL queries to the Jakarta Persistence layer.
 */
@Component(configurationPid = "io.openliberty.data",
           configurationPolicy = ConfigurationPolicy.OPTIONAL,
           service = { CDIExtensionMetadata.class,
                       DataExtensionProvider.class,
                       DeferredMetaDataFactory.class,
                       ApplicationMetaDataListener.class,
                       ApplicationStateListener.class },
           property = { "deferredMetaData=DATA" })
public class DataExtensionProvider implements //
                CDIExtensionMetadata, //
                CDIExtensionMetadataInternal, //
                DeferredMetaDataFactory, //
                ApplicationMetaDataListener, // TODO remove this? and use the following instead?
                ApplicationStateListener {
    private static final TraceComponent tc = Tr.register(DataExtensionProvider.class);

    private static final Set<Class<?>> beanClasses = Set.of(DataSource.class, EntityManagerFactory.class);

    private static final Set<Class<? extends Extension>> extensions = Collections.singleton(DataExtension.class);

    @Reference
    public ClassLoaderIdentifierService classloaderIdSvc;

    @Reference
    public DataVersionCompatibility compat;

    @Reference
    public ConfigurationAdmin configAdmin;

    /**
     * Configured value for createTables.
     */
    public volatile boolean createTables;

    /**
     * Map of application name to map of Repository.dataStore to databaseStore config that is generated by the extension.
     * Entries are removed when the application stops, at which point the config is removed.
     */
    public final Map<String, Map<String, Configuration>> dbStoreConfigAllApps = new ConcurrentHashMap<>();

    /**
     * Map of application name to list of registrations of delegating resource factories that are generated by the extension.
     * Entries are removed when the application stops, at which point the services are unregistered.
     */
    public final Map<String, Queue<ServiceRegistration<ResourceFactory>>> delegatorsAllApps = new ConcurrentHashMap<>();

    /**
     * Configured value for dropTables.
     */
    public volatile boolean dropTables;

    /**
     * EntityManagerBuilder futures per application, to complete once the application starts.
     */
    private final ConcurrentHashMap<String, Collection<FutureEMBuilder>> futureEMBuilders = new ConcurrentHashMap<>();

    @Reference(target = "(component.name=com.ibm.ws.threading)")
    protected ExecutorService executor;

    @Reference
    public LocalTransactionCurrent localTranCurrent;

    /**
     * Configured interface/method/package names for logValues.
     */
    public volatile Set<String> logValues;

    @Reference
    public MetaDataIdentifierService metadataIdSvc;

    private final ConcurrentHashMap<String, DataComponentMetaData> metadatas = new ConcurrentHashMap<>();

    public @Reference ResourceConfigFactory resourceConfigFactory;

    @Reference
    public EmbeddableWebSphereTransactionManager tranMgr;

    /**
     * Service that provides Jakarta Validation.
     */
    private transient Object validationService;

    /**
     * OSGi service activate.
     *
     * @param props config properties.
     */
    @Activate
    protected void activate(Map<String, Object> props) {
        createTables = Boolean.TRUE.equals(props.get("createTables"));

        dropTables = Boolean.TRUE.equals(props.get("dropTables"));

        @SuppressWarnings("unchecked")
        Collection<String> list = (Collection<String>) props.get("logValues");
        Set<String> names = list == null ? Set.of() : new HashSet<>(list.size());
        if (list != null)
            names.addAll(list);
        logValues = names;
    }

    /**
     * Makes DataSource and EntityManagerFactory beans that are produced by the application visible to our extension
     * so that we can use them to implement the repository.
     *
     * @return true to make them visible.
     */
    @Override
    public boolean applicationBeansVisible() {
        return true;
    }

    @Override
    @Trivial
    public void applicationMetaDataCreated(MetaDataEvent<ApplicationMetaData> event) throws MetaDataException {
    }

    @Override
    public void applicationMetaDataDestroyed(MetaDataEvent<ApplicationMetaData> event) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        String appName = event.getMetaData().getName();
        Map<String, Configuration> configurations = dbStoreConfigAllApps.remove(appName);
        if (configurations != null)
            for (Configuration config : configurations.values())
                try {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "deleting " + config);
                    config.delete();
                } catch (IOException x) {
                    // logged to FFDC
                }

        Queue<ServiceRegistration<ResourceFactory>> registrations = delegatorsAllApps.remove(appName);
        if (registrations != null)
            for (ServiceRegistration<ResourceFactory> reg; (reg = registrations.poll()) != null;)
                reg.unregister();

        // Remove references to component metadata that we created for this application
        for (Iterator<DataComponentMetaData> it = metadatas.values().iterator(); it.hasNext();) {
            DataComponentMetaData cdata = it.next();
            if (appName.equals(cdata.getJ2EEName().getApplication())) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "removing " + cdata);
                it.remove();
            }
        }
    }

    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
        Collection<FutureEMBuilder> futures = futureEMBuilders.remove(appInfo.getName());
        if (futures != null) {
            boolean beforeCheckpoint = !CheckpointPhase.getPhase().restored();
            for (FutureEMBuilder futureEMBuilder : futures) {
                if (beforeCheckpoint)
                    try {
                        // Run the task in the foreground if before a checkpoint.
                        // This is necessary to ensure this task completes before the checkpoint.
                        // Application startup performance is not as important before checkpoint
                        // and this ensures we don't do this work on restore side which will make
                        // restore faster.
                        futureEMBuilder.complete(futureEMBuilder.createEMBuilder());
                    } catch (Throwable x) {
                        futureEMBuilder.completeExceptionally(x);
                    }
                else
                    futureEMBuilder.completeAsync(futureEMBuilder::createEMBuilder, executor);
            }
        }
    }

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        futureEMBuilders.remove(appInfo.getName());
    }

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        futureEMBuilders.remove(appInfo.getName());
    }

    /**
     * Create or obtain DataComponentMetaData for an application artifact,
     * such as when the repository is defined in a library of the application
     * rather than in a web component or enterprise bean.
     *
     * @param metadata    ApplicatonMetaData to include in the metadata hierarchy.
     * @param classloader class loader of the repository interface.
     * @return component metadata.
     */
    public ComponentMetaData createComponentMetadata(MetaData metadata,
                                                     ClassLoader classloader) {
        J2EEName jeeName;
        ModuleMetaData moduleMetadata;

        if (metadata instanceof ApplicationMetaData) {
            ApplicationMetaData adata = (ApplicationMetaData) metadata;
            jeeName = adata.getJ2EEName();
            moduleMetadata = new DataModuleMetaData(jeeName, adata);
        } else {
            throw new IllegalArgumentException(metadata == null //
                            ? null //
                            : metadata.getClass().getName());
        }

        String identifier = "DATA#" + jeeName;

        DataComponentMetaData componentMetadata = new DataComponentMetaData(identifier, moduleMetadata, classloader);
        DataComponentMetaData existing = metadatas.putIfAbsent(identifier, componentMetadata);
        return existing == null ? componentMetadata : existing;
    }

    /**
     * Obtain metadata for the specified identifier.
     *
     * @param identifier the metadata identifier, of the form DATA#AppName
     * @return the metadata if found, otherwise null.
     */
    @Override
    public ComponentMetaData createComponentMetaData(String identifier) {
        return metadatas.get(identifier);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        // Remove and delete configurations that our extension generated.
        for (Iterator<Map<String, Configuration>> it = dbStoreConfigAllApps.values().iterator(); it.hasNext();) {
            Map<String, Configuration> configurations = it.next();
            it.remove();
            for (Configuration config : configurations.values())
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "deleting " + config);
                    config.delete();
                } catch (IOException x) {
                    // logged to FFDC
                }
        }

        // Remove and unregister services that our extension generated.
        // This is done second because some of the configurations depend on the services.
        for (Iterator<Queue<ServiceRegistration<ResourceFactory>>> it = delegatorsAllApps.values().iterator(); it.hasNext();) {
            Queue<ServiceRegistration<ResourceFactory>> registrations = it.next();
            it.remove();
            for (ServiceRegistration<ResourceFactory> reg; (reg = registrations.poll()) != null;)
                reg.unregister();
        }
    }

    @Override
    public Set<Class<?>> getBeanClasses() {
        return beanClasses;
    }

    /**
     * Obtain the classloader of DataComponentMetaData that is created
     * for an application artifact, such as when the repository is
     * defined in a library of the application rather than in a
     * web component or enterprise bean.
     *
     * @param metadata DataComponentMetaData.
     * @return the class loader, otherwise null.
     */
    @Override
    public ClassLoader getClassLoader(ComponentMetaData metadata) {
        return metadata instanceof DataComponentMetaData //
                        ? ((DataComponentMetaData) metadata).classLoader //
                        : null;
    }

    @Override
    @Trivial
    public Set<Class<? extends Extension>> getExtensions() {
        return extensions;
    }

    /**
     * Create an indentifier for metadata that is constructed by this
     * DeferredMetaDataFactory.
     *
     * @param appName       application name
     * @param moduleName    always null
     * @param componentName always null
     * @return metadata identifier.
     */
    @Override
    public String getMetaDataIdentifier(String appName,
                                        String moduleName,
                                        String componentName) {
        StringBuilder b = new StringBuilder("DATA#").append(appName);
        if (moduleName != null)
            b.append('#').append(moduleName);
        if (componentName != null)
            b.append('#').append(componentName);
        return b.toString();
    }

    /**
     * Unused because this DeferredMetaDataFactory does not opt in to
     * deferred metadata creation.
     */
    @Override
    @Trivial
    public void initialize(ComponentMetaData metadata) throws IllegalStateException {
    }

    /**
     * Invoked when configuration is modified.
     *
     * @param props config properties.
     */
    @Modified
    protected void modified(Map<String, Object> props) {
        createTables = Boolean.TRUE.equals(props.get("createTables"));

        dropTables = Boolean.TRUE.equals(props.get("dropTables"));

        @SuppressWarnings("unchecked")
        Collection<String> list = (Collection<String>) props.get("logValues");
        Set<String> names = list == null ? Set.of() : new HashSet<>(list.size());
        if (list != null)
            names.addAll(list);
        logValues = names;
    }

    /**
     * Arrange for the specified EntityManagerBuilders to initialize once the application is started.
     *
     * @param appName  application name.
     * @param builders list of EntityManagerBuilder.
     */
    void onAppStarted(String appName, Collection<FutureEMBuilder> builders) {
        Collection<FutureEMBuilder> previous = futureEMBuilders.putIfAbsent(appName, builders);
        if (previous != null)
            previous.addAll(builders);
    }

    @Reference(service = ModuleMetaDataListener.class, // also a BeanValidation.class, but that class might not be available to this bundle
               target = "(service.pid=com.ibm.ws.beanvalidation.OSGiBeanValidationImpl)",
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.STATIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setValidation(ModuleMetaDataListener svc) {
        validationService = svc;
    }

    protected void unsetValidation(ModuleMetaDataListener svc) {
        if (validationService == svc)
            validationService = null;
    }

    /**
     * @return service that provides Jakarta Validation.
     */
    public Object validationService() {
        return validationService;
    }
}