/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import static io.openliberty.data.internal.persistence.cdi.DataExtension.exc;

import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.persistence.DDLGenerationParticipant;

import io.openliberty.data.internal.persistence.DataProvider;
import io.openliberty.data.internal.persistence.EntityManagerBuilder;
import io.openliberty.data.internal.persistence.provider.PUnitEMBuilder;
import io.openliberty.data.internal.persistence.service.DBStoreEMBuilder;
import jakarta.data.exceptions.DataException;
import jakarta.persistence.EntityManagerFactory;

/**
 * A completable future for an EntityManagerBuilder that can be
 * completed by invoking the createEMBuilder method.
 */
public class FutureEMBuilder extends CompletableFuture<EntityManagerBuilder> implements DDLGenerationParticipant {
    private static final TraceComponent tc = Tr.register(FutureEMBuilder.class);
    private static final long DDLGEN_WAIT_TIME = 15;

    /**
     * These are present only if needed to disambiguate a JNDI name
     * that is in java:app, java:module, or java:comp
     */
    private final String application, module;

    /**
     * The configured dataStore value of the Repository annotation.
     */
    private final String dataStore;

    /**
     * Entity classes as seen by the user, not generated entity classes for records.
     */
    final Set<Class<?>> entityTypes = new HashSet<>();

    /**
     * Module name in which the repository interface is defined.
     * If not defined in a module, only the application name part is included.
     */
    private final J2EEName moduleName;

    /**
     * Namespace prefix (such as java:module) of the Repository dataStore.
     */
    private final Namespace namespace;

    /**
     * OSGi service component that provides the CDI extension for Data.
     */
    private final DataProvider provider;

    /**
     * The class loader for repository classes.
     */
    private final ClassLoader repositoryClassLoader;

    /**
     * Interface that is annotated with the Repository annotation.
     */
    private final Class<?> repositoryInterface;

    /**
     * Obtains entity manager instances from a persistence unit reference /
     * EntityManagerFactory.
     *
     * @param provider              OSGi service that provides the CDI extension.
     * @param repositoryClassLoader class loader of the repository interface.
     * @param emf                   entity manager factory.
     * @param dataStore             configured dataStore value of the Repository annotation.
     */
    FutureEMBuilder(DataProvider provider,
                    Class<?> repositoryInterface,
                    ClassLoader repositoryClassLoader,
                    String dataStore) {
        this.provider = provider;
        this.repositoryInterface = repositoryInterface;
        this.repositoryClassLoader = repositoryClassLoader;
        this.dataStore = dataStore;
        this.moduleName = getModuleName(repositoryInterface, repositoryClassLoader, provider);
        this.namespace = Namespace.of(dataStore);

        if (Namespace.APP.isMoreGranularThan(namespace)) { // java:global or none
            application = null;
            module = null;
        } else { // java:app, java:module, or java:comp
            application = moduleName.getApplication();
            if (Namespace.MODULE.isMoreGranularThan(namespace)) { // java:app
                module = null;
            } else { // java:module or java:comp
                module = moduleName.getModule();
                if (module == null)
                    throw exc(IllegalArgumentException.class,
                              "CWWKD1060.name.out.of.scope",
                              repositoryInterface.getName(),
                              moduleName.getApplication(),
                              dataStore,
                              "dataStore",
                              namespace,
                              "java:app");
            }
        }
    }

    /**
     * Adds an entity class to be handled.
     * This is the entity class as the user sees it,
     * not a generated entity class for record entities.
     *
     * @param entityClass entity class.
     */
    @Trivial
    void add(Class<?> entityClass) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "add: " + entityClass.getName());

        entityTypes.add(entityClass);
    }

    /**
     * Registers this future with the DDL generation MBean so that the ddlgen command
     * will generate DDL for the EntityManagerBuilder produced by this future. <p>
     *
     * Not all EntityManagerBuilder instances will participate in DDL generation;
     * only those that use the Persistence Service. This is not determined until the
     * EntityManagerBuilder has been created. If not participating, a null DDL file
     * name will be provided and the DDL generation command will skip generation.
     *
     * @param appName application name
     */
    public void registerDDLGenerationParticipant(String appName) {
        // Register as a DDL generator for use by the ddlGen command and add to list for cleanup on application stop
        BundleContext thisbc = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceRegistration<DDLGenerationParticipant> ddlgenreg = thisbc.registerService(DDLGenerationParticipant.class, this, null);
        Queue<ServiceRegistration<DDLGenerationParticipant>> ddlgenRegistrations = provider.ddlgeneratorsAllApps.get(appName);
        if (ddlgenRegistrations == null) {
            Queue<ServiceRegistration<DDLGenerationParticipant>> empty = new ConcurrentLinkedQueue<>();
            if ((ddlgenRegistrations = provider.ddlgeneratorsAllApps.putIfAbsent(appName, empty)) == null)
                ddlgenRegistrations = empty;
        }
        ddlgenRegistrations.add(ddlgenreg);
    }

    @Override
    public String getDDLFileName() {
        try {
            EntityManagerBuilder builder = get(DDLGEN_WAIT_TIME, TimeUnit.SECONDS);
            if (builder instanceof DDLGenerationParticipant) {
                return ((DDLGenerationParticipant) builder).getDDLFileName();
            }
        } catch (TimeoutException e) {
            // TODO : translate message for exception & error (or warning)
            // DDL generation MBean does not log errors; participants must provide meaningful messages.
            // Log a useful error informing user to try again later after builder creation completes.
            Tr.error(tc, "CWWKD10xxE.ddlgen.timeout", dataStore, DDLGEN_WAIT_TIME);

            // Throw exception with same message so DDL generation MBean reports that a failure occurred.
            throw new DataException("DDL file not generated for Jakarta Data repositories associated with the " + dataStore
                                    + " DatabaseStore. The EntityManagerFactory was not created in " + DDLGEN_WAIT_TIME + " seconds. Try DDL generation again at a later time.");
        } catch (Throwable ex) {
            // TODO : translate message for exception & error
            // DDL generation MBean does not log errors; participants must provide meaningful messages.
            // Log a useful error informing user to correct problems reported in the cause.
            Throwable cause = (ex instanceof ExecutionException) ? ex.getCause() : ex;
            Tr.error(tc, "CWWKD10xyE.ddlgen.failed.create", dataStore, cause);

            // Throw exception with same message so DDL generation MBean reports that a failure occurred.
            throw new DataException("DDL file not generated for Jakarta Data repositories associated with the " + dataStore
                                    + " DatabaseStore. An error occurred creating the EntityManagerFactory.", cause);
        }

        // Not using persistence service; return null and DDL generation will skip this future
        return null;
    }

    @Override
    public void generate(Writer out) throws Exception {
        try {
            EntityManagerBuilder builder = get(DDLGEN_WAIT_TIME, TimeUnit.SECONDS);
            if (builder instanceof DDLGenerationParticipant) {
                ((DDLGenerationParticipant) builder).generate(out);
            }
        } catch (TimeoutException e) {
            // TODO : translate message for exception & error (or warning)
            // DDL generation MBean does not log errors; participants must provide meaningful messages.
            // Log a useful error informing user to try again later after builder creation completes.
            Tr.error(tc, "CWWKD10xxE.ddlgen.timeout", dataStore, DDLGEN_WAIT_TIME);

            // Throw exception with same message so DDL generation MBean reports that a failure occurred.
            throw new DataException("DDL file not generated for Jakarta Data repositories associated with the " + dataStore
                                    + " DatabaseStore. The EntityManagerFactory was not created in " + DDLGEN_WAIT_TIME + " seconds. Try DDL generation again at a later time.");
        } catch (Throwable ex) {
            // TODO : translate message for exception & error (or warning)
            // DDL generation MBean does not log errors; participants must provide meaningful messages.
            // Log a useful error informing user to correct problems reported in the cause.
            Throwable cause = (ex instanceof ExecutionException) ? ex.getCause() : ex;
            Tr.error(tc, "CWWKD10xyE.ddlgen.failed.create", dataStore, cause);

            // Throw exception with same message so DDL generation MBean reports that a failure occurred.
            throw new DataException("DDL file not generated for Jakarta Data repositories associated with the " + dataStore
                                    + " DatabaseStore. An error occurred creating the EntityManagerFactory.", cause);
        }
    }

    /**
     * Invoked to complete this future with the resulting EntityManagerBuilder value.
     *
     * @return PUnitEMBuilder (for persistence unit references) or
     *         DBStoreEMBuilder (data sources, databaseStore)
     * @throws Exception if an error occurs.
     */
    @FFDCIgnore(NamingException.class)
    public EntityManagerBuilder createEMBuilder() {
        String resourceName = dataStore;
        String metadataIdentifier = getMetadataIdentifier();

        // metadataIdentifier examples:
        // WEB#MyApp#MyWebModule.war
        // EJB#MyApp#MyEJBModule.jar#MyEJB
        // DATA#MyApp

        ComponentMetaDataAccessorImpl accessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        ComponentMetaData extMetadata = accessor.getComponentMetaData();
        ComponentMetaData repoMetadata = (ComponentMetaData) provider.metadataIdSvc.getMetaData(metadataIdentifier);
        boolean switchMetadata = repoMetadata != null &&
                                 (extMetadata == null || !extMetadata.getJ2EEName().equals(repoMetadata.getJ2EEName()));

        if (namespace == Namespace.COMP && metadataIdentifier.startsWith("EJB#"))
            throw exc(IllegalArgumentException.class,
                      "CWWKD1061.comp.name.in.ejb",
                      repositoryInterface.getName(),
                      repoMetadata.getJ2EEName().getModule(),
                      repoMetadata.getJ2EEName().getApplication(),
                      resourceName,
                      "dataStore",
                      "java:comp",
                      List.of("java:app", "java:module"));

        if (switchMetadata)
            accessor.beginContext(repoMetadata);
        try {
            if (namespace != null) {
                try {
                    Object resource = InitialContext.doLookup(dataStore);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, dataStore + " is the JNDI name for " + resource);

                    if (resource instanceof EntityManagerFactory)
                        return new PUnitEMBuilder(provider, repositoryClassLoader, (EntityManagerFactory) resource, //
                                        resourceName, metadataIdentifier, entityTypes);

                } catch (NamingException x) {
                    FFDCFilter.processException(x, this.getClass().getName() + ".createEMBuilder", "155", this, new Object[] { (switchMetadata ? repoMetadata : extMetadata) });
                    throw new CompletionException("Unable to find " + dataStore + " from " +
                                                  (switchMetadata ? repoMetadata : extMetadata).getJ2EEName(), x);
                }
            } else if (!DataExtension.DEFAULT_DATA_STORE.equals(resourceName)) {
                // Check for resource references and persistence unit references where java:comp/env/ is omitted:
                String javaCompName = "java:comp/env/" + resourceName;
                try {
                    Object resource = InitialContext.doLookup(javaCompName);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, javaCompName + " is the JNDI name for " + resource);

                    if (resource instanceof EntityManagerFactory)
                        return new PUnitEMBuilder(provider, repositoryClassLoader, //
                                        (EntityManagerFactory) resource, //
                                        javaCompName, metadataIdentifier, //
                                        entityTypes);

                    if (resource instanceof DataSource)
                        resourceName = javaCompName;
                } catch (NamingException x) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, javaCompName + " is not available in JNDI, ensure dataStore = "
                                           + resourceName + " refers to a resource reference or persistence unit reference. "
                                           + "Otherwise, we will assume this property refers to a datasource.");
                }
            }
        } finally {
            if (switchMetadata)
                accessor.endContext();
        }

        ComponentMetaData mdata = (ComponentMetaData) provider.metadataIdSvc.getMetaData(metadataIdentifier);
        J2EEName jeeName = mdata == null ? null : mdata.getJ2EEName();
        boolean javacolon = namespace != null || // any java: namespace
                            resourceName != dataStore; // implicit java:comp
        return new DBStoreEMBuilder(provider, repositoryClassLoader, //
                        resourceName, //
                        javacolon, //
                        metadataIdentifier, jeeName, entityTypes);
    }

    @Override
    @Trivial
    public boolean equals(Object o) {
        FutureEMBuilder b;
        return this == o || o instanceof FutureEMBuilder
                            && dataStore.equals((b = (FutureEMBuilder) o).dataStore)
                            && Objects.equals(application, b.application)
                            && Objects.equals(module, b.module)
                            && Objects.equals(repositoryClassLoader, b.repositoryClassLoader);
    }

    /**
     * Obtains the metadata identifier for the module that defines the repository
     * interface.
     *
     * @return metadata identifier as the key, and application/module/component
     *         as the value. Module and component might be null or might not be
     *         present at all.
     */
    private String getMetadataIdentifier() {
        String mdIdentifier;

        if (moduleName.getModule() == null) {
            mdIdentifier = provider.getMetaDataIdentifier(moduleName.getApplication(),
                                                          moduleName.getModule(),
                                                          null);
        } else {
            String clIdentifier = provider.classloaderIdSvc.getClassLoaderIdentifier(repositoryClassLoader);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc,
                         "defined in module: " + moduleName,
                         "class loader identifier: " + clIdentifier);
            if (clIdentifier.startsWith("WebModule:")) {
                mdIdentifier = provider.metadataIdSvc.getMetaDataIdentifier("WEB",
                                                                            moduleName.getApplication(),
                                                                            moduleName.getModule(),
                                                                            null);
            } else {
                mdIdentifier = provider.metadataIdSvc.getMetaDataIdentifier("EJB",
                                                                            moduleName.getApplication(),
                                                                            moduleName.getModule(),
                                                                            null);
            }
        }

        return mdIdentifier;
    }

    /**
     * Obtains the module name in which the repository interface is defined.
     *
     * @param repositoryInterface   the repository interface.
     * @param repositoryClassLoader class loader of the repository interface.
     * @param provider              OSGi service that provides the CDI extension.
     * @return AppName[#ModuleName] with only the application name if not defined
     *         in a module.
     */
    private J2EEName getModuleName(Class<?> repositoryInterface,
                                   ClassLoader repositoryClassLoader,
                                   DataProvider provider) {
        J2EEName moduleName;

        Optional<J2EEName> moduleNameOptional = provider.cdiService.getModuleNameForClass(repositoryInterface);

        if (moduleNameOptional.isPresent()) {
            moduleName = moduleNameOptional.get();
        } else {
            // create component and module metadata based on the application metadata
            ComponentMetaData cdata = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            ApplicationMetaData adata = cdata == null ? null : cdata.getModuleMetaData().getApplicationMetaData();
            cdata = provider.createComponentMetadata(adata, repositoryClassLoader);
            moduleName = cdata.getModuleMetaData().getJ2EEName();
        }

        return moduleName;
    }

    @Override
    @Trivial
    public int hashCode() {
        return dataStore.hashCode() +
               (application == null ? 0 : application.hashCode()) +
               (module == null ? 0 : module.hashCode());
    }

    @Override
    @Trivial
    public String toString() {
        int len = 27 + dataStore.length() +
                  (application == null ? 4 : application.length()) +
                  (module == null ? 4 : module.length());
        StringBuilder b = new StringBuilder(len) //
                        .append("FutureEMBuilder@") //
                        .append(Integer.toHexString(hashCode())) //
                        .append(":").append(dataStore) //
                        .append(' ').append(application) //
                        .append('#').append(module);
        return b.toString();
    }
}
