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
package io.openliberty.data.internal.persistence;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletionStage;
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
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.extension.CDIExtensionMetadataInternal;
import com.ibm.ws.classloading.ClassLoaderIdentifierService;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory;
import com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.wsspi.persistence.DDLGenerationParticipant;
import com.ibm.wsspi.resource.ResourceConfigFactory;
import com.ibm.wsspi.resource.ResourceFactory;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.data.internal.persistence.cdi.DataExtension;
import io.openliberty.data.internal.persistence.cdi.FutureEMBuilder;
import io.openliberty.data.internal.persistence.metadata.DataComponentMetaData;
import io.openliberty.data.internal.persistence.metadata.DataModuleMetaData;
import io.openliberty.data.internal.version.DataVersionCompatibility;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.persistence.EntityManagerFactory;

/**
 * Built-in Jakarta Data provider for relational databases that
 * delegates queries and operations to the Jakarta Persistence layer.
 */
@Component(configurationPid = "io.openliberty.data",
           configurationPolicy = ConfigurationPolicy.OPTIONAL,
           service = { CDIExtensionMetadata.class,
                       DataProvider.class,
                       DeferredMetaDataFactory.class,
                       ApplicationStateListener.class },
           property = { "deferredMetaData=DATA" })
public class DataProvider implements //
                CDIExtensionMetadata, //
                CDIExtensionMetadataInternal, //
                DeferredMetaDataFactory, //
                ApplicationStateListener {
    private static final TraceComponent tc = Tr.register(DataProvider.class);

    private static final Set<Class<?>> beanClasses = Set.of(DataSource.class, EntityManagerFactory.class);

    private static final Set<Class<? extends Extension>> extensions = Collections.singleton(DataExtension.class);

    @Reference
    public CDIService cdiService;

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
     * Map of application name to list of registrations of DDL generation participants that are generated by the extension.
     * Entries are removed when the application stops, at which point the services are unregistered.
     */
    public final Map<String, Queue<ServiceRegistration<DDLGenerationParticipant>>> ddlgeneratorsAllApps = new ConcurrentHashMap<>();

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
    protected LocalTransactionCurrent localTranCurrent;

    /**
     * Configured interface/method/package names for logValues.
     */
    private volatile Set<String> logValues = Set.of();

    @Reference
    public MetaDataIdentifierService metadataIdSvc;

    private final ConcurrentHashMap<String, DataComponentMetaData> metadatas = new ConcurrentHashMap<>();

    public @Reference ResourceConfigFactory resourceConfigFactory;

    @Reference
    protected EmbeddableWebSphereTransactionManager tranMgr;

    /**
     * Service that provides Jakarta Validation.
     */
    transient Object validationService;

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
            for (String item : list)
                names.add(item.trim());
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
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
        String appName = appInfo.getName();
        Collection<FutureEMBuilder> futures = futureEMBuilders.remove(appName);
        if (futures != null) {
            for (FutureEMBuilder futureEMBuilder : futures) {
                // This delays createEMBuilder until restore.
                // While this works by avoiding all connections to the data source, it does make restore much slower.
                // TODO figure out how to do more work on restore without having to make a connection to the data source
                CheckpointPhase.onRestore(() -> futureEMBuilder.completeAsync(futureEMBuilder::createEMBuilder, executor));

                // Application is ready for DDL generation; register with DDLGen MBean.
                // Only those using the Persistence Service will participate, but all will
                // be registered since that is not known until createEMBuilder completes.
                // Those not participating will return a null DDL file name and be skipped.
                futureEMBuilder.registerDDLGenerationParticipant(appName);
            }
        }
    }

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        futureEMBuilders.remove(appInfo.getName());
    }

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        String appName = appInfo.getName();

        futureEMBuilders.remove(appName);

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

        Queue<ServiceRegistration<DDLGenerationParticipant>> ddlgenRegistrations = ddlgeneratorsAllApps.remove(appName);
        if (ddlgenRegistrations != null)
            for (ServiceRegistration<DDLGenerationParticipant> ddlgenreg; (ddlgenreg = ddlgenRegistrations.poll()) != null;)
                ddlgenreg.unregister();

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

    /**
     * Create or obtain DataComponentMetaData for an application artifact,
     * such as when the repository is defined in a library of the application
     * rather than in a web component or enterprise bean.
     *
     * @param appData     ApplicationMetaData to include in the metadata hierarchy.
     * @param classloader class loader of the repository interface.
     * @return component metadata.
     */
    public ComponentMetaData createComponentMetadata(ApplicationMetaData appData,
                                                     ClassLoader classloader) {
        J2EEName jeeName = appData.getJ2EEName();
        ModuleMetaData moduleData = new DataModuleMetaData(jeeName, appData);

        String identifier = "DATA#" + jeeName;

        DataComponentMetaData componentData = new DataComponentMetaData( //
                        identifier, moduleData, classloader);

        DataComponentMetaData existing = metadatas.putIfAbsent(identifier, componentData);
        return existing == null ? componentData : existing;
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

        // Remove and unregister ddl generation services that our extension generated.
        for (Iterator<Queue<ServiceRegistration<DDLGenerationParticipant>>> it = ddlgeneratorsAllApps.values().iterator(); it.hasNext();) {
            Queue<ServiceRegistration<DDLGenerationParticipant>> ddlgenRegistrations = it.next();
            it.remove();
            for (ServiceRegistration<DDLGenerationParticipant> ddlgenreg; (ddlgenreg = ddlgenRegistrations.poll()) != null;)
                ddlgenreg.unregister();
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
     * Create an identifier for metadata that is constructed by this
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
     * Prepare values, which might include customer data, for logging.
     * If the repository class/package/method is not considered loggable
     * then return a copy of the values for logging where customer data
     * is replaced with a placeholder.
     *
     * @param repoClass repository class.
     * @param method    repository method.
     * @param values    values.
     * @return loggable values.
     */
    @Trivial
    Object[] loggable(Class<?> repoClass, Method method, Object... values) {
        String className;
        if (values == null ||
            values.length == 0 ||
            !logValues.isEmpty() &&
                                  (logValues.contains("*") ||
                                   logValues.contains(repoClass.getPackageName()) ||
                                   logValues.contains(className = repoClass.getName()) ||
                                   logValues.contains(className + '.' + method.getName())))
            return values;

        Object[] loggable = new Object[values.length];
        for (int i = 0; i < values.length; i++)
            if (values[i] == null ||
                values[i] instanceof PageRequest ||
                values[i] instanceof Order ||
                values[i] instanceof Sort ||
                values[i] instanceof Sort[] ||
                values[i] instanceof Limit)
                loggable[i] = values[i];
            else // obscure customer data
                loggable[i] = loggable(values[i]);

        return loggable;
    }

    /**
     * Prepare a value, which might include customer data, for logging.
     * If the repository class/package/method is not considered loggable
     * then return a copy of the value for logging where customer data
     * is replaced with a placeholder.
     *
     * @param repoClass repository class.
     * @param method    repository method.
     * @param value     value.
     * @return loggable value.
     */
    @Trivial
    Object loggable(Class<?> repoClass, Method method, Object value) {
        String className;
        if (value == null ||
            !logValues.isEmpty() &&
                             (logValues.contains("*") ||
                              logValues.contains(repoClass.getPackageName()) ||
                              logValues.contains(className = repoClass.getName()) ||
                              logValues.contains(className + '.' + method.getName())))
            return value;

        return loggable(value);
    }

    /**
     * Obscures a value from customer data while including some useful,
     * non-sensitive data.
     *
     * @param value customer data. Must not be null.
     * @return loggable value that does not include customer data.
     */
    @Trivial
    private Object loggable(Object value) {
        Object loggable;
        Class<?> c = value.getClass();
        Class<?> a = c.getComponentType();
        if (a != null) {
            StringBuilder s = new StringBuilder();
            int len = Array.getLength(value);
            int maxOutput = len <= 20 ? len : 20;
            s.append(a.getName()).append('[').append(len).append("]: {");
            for (int i = 0; i < maxOutput; i++) {
                Object v = loggable(Array.get(value, i));
                s.append(i == 0 ? " " : ", ").append(v);
            }
            if (len > maxOutput)
                s.append(", ...");
            s.append(" }");
            loggable = s.toString();
        } else if (value instanceof Optional) {
            Optional<?> opt = (Optional<?>) value;
            if (opt.isPresent())
                loggable = new StringBuilder().append("Optional { ") //
                                .append(loggable(opt.get())).append(" }") //
                                .toString();
            else
                loggable = value;
        } else if (value instanceof Page) {
            loggable = value; // customer values already obscured
        } else if (value instanceof CompletionStage) {
            loggable = value; // customer values already obscured
        } else if (value instanceof Iterable) {
            StringBuilder s = new StringBuilder();
            int len = value instanceof Collection ? ((Collection<?>) value).size() : -1;
            int maxOutput = 20;
            s.append(c.getName());
            if (len >= 0)
                s.append('(').append(len).append("): {");
            else
                s.append(": {");
            Iterator<?> it = ((Iterable<?>) value).iterator();
            for (int size = 0; size < maxOutput && it.hasNext(); size++) {
                Object v = loggable(it.next());
                s.append(size == 0 ? " " : ", ").append(v);
            }
            if (it.hasNext())
                s.append(", ...");
            s.append(" }");
            loggable = s.toString();
        } else {
            String name = c.getName();
            loggable = c.isPrimitive() || value instanceof Number ? //
                            name : //
                            new StringBuilder(name.length() + 9) //
                                            .append(name) //
                                            .append('@') //
                                            .append(Integer.toHexString(value.hashCode()));
        }
        return loggable;
    }

    /**
     * Appends a suffix if the repository class/package/method is considered
     * loggable. Otherwise returns only the prefix.
     *
     * @param repoClass      repository class.
     * @param method         repository method.
     * @param prefix         first part of value to always include.
     * @param possibleSuffix suffix to only include if logValues allows.
     * @return loggable value.
     */
    @Trivial
    String loggableAppend(Class<?> repoClass,
                          Method method,
                          String prefix,
                          Object... possibleSuffix) {
        StringBuilder b = new StringBuilder(prefix);
        String className;
        if (possibleSuffix != null &&
            !logValues.isEmpty() &&
            (logValues.contains("*") ||
             logValues.contains(repoClass.getPackageName()) ||
             logValues.contains(className = repoClass.getName()) ||
             logValues.contains(className + '.' + method.getName())))
            for (Object s : possibleSuffix)
                b.append(s);

        return b.toString();
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
            for (String item : list)
                names.add(item.trim());
        logValues = names;
    }

    /**
     * Arrange for the specified EntityManagerBuilders to initialize once the application is started.
     *
     * @param appName  application name.
     * @param builders list of EntityManagerBuilder.
     */
    public void onAppStarted(String appName, Collection<FutureEMBuilder> builders) {
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
}