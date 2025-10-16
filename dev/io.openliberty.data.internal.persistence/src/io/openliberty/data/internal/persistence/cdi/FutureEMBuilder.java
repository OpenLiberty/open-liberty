/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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

import static io.openliberty.data.internal.persistence.EntityManagerBuilder.getClassNames;
import static io.openliberty.data.internal.persistence.cdi.DataExtension.exc;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.persistence.DDLGenerationParticipant;

import io.openliberty.data.internal.persistence.DataProvider;
import io.openliberty.data.internal.persistence.EntityManagerBuilder;
import io.openliberty.data.internal.persistence.Util;
import io.openliberty.data.internal.persistence.provider.PUnitEMBuilder;
import io.openliberty.data.internal.persistence.service.DBStoreEMBuilder;
import jakarta.data.exceptions.DataException;
import jakarta.persistence.EntityManagerFactory;

/**
 * A completable future for an EntityManagerBuilder that can be
 * completed by invoking the createEMBuilder method.
 */
public class FutureEMBuilder extends CompletableFuture<EntityManagerBuilder> implements DDLGenerationParticipant, Comparable<FutureEMBuilder> {
    private static final TraceComponent tc = Tr.register(FutureEMBuilder.class);
    private static final long DDLGEN_WAIT_TIME = 15;

    /**
     * These are present only if needed to disambiguate a JNDI name
     * that is in java:app, java:module, or java:comp
     */
    private final String application, module;

    /**
     * The configured dataStore value of the Repository annotation,
     * or if unspecified, then java:comp/DefaultDataSource.
     */
    final String dataStore;

    /**
     * Entity classes as seen by the user, not generated entity classes for records.
     */
    final Set<Class<?>> entityTypes = new HashSet<>();

    /**
     * Indicates if the repository class loader is from a web module.
     */
    public final boolean inWebModule;

    /**
     * Application artifact name (typically a module name) in which the repository
     * interface is defined. If not defined in a module, only the application name
     * part is included. If not defined in an application either, the name of the
     * application that uses the repository is used instead.
     */
    public final J2EEName jeeName;

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
     * Interfaces that are annotated with the Repository annotation.
     */
    private final Set<Class<?>> repositoryInterfaces = new HashSet<>();

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
        this.repositoryClassLoader = repositoryClassLoader;
        this.dataStore = dataStore;
        this.jeeName = getArtifactName(repositoryInterface, repositoryClassLoader, provider);
        this.namespace = Namespace.of(dataStore);

        if (Namespace.APP.isMoreGranularThan(namespace)) { // java:global or none
            application = null;
            module = null;
        } else { // java:app, java:module, or java:comp
            application = jeeName.getApplication();
            if (Namespace.MODULE.isMoreGranularThan(namespace)) { // java:app
                module = null;
            } else { // java:module or java:comp
                module = jeeName.getModule();
                if (module == null)
                    throw exc(IllegalArgumentException.class,
                              "CWWKD1060.name.out.of.scope",
                              repositoryInterface.getName(),
                              jeeName.getApplication(),
                              dataStore,
                              "dataStore",
                              namespace,
                              "java:app");
            }
        }

        String clIdentifier = provider.classloaderIdSvc.getClassLoaderIdentifier(repositoryClassLoader);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "class loader identifier: " + clIdentifier);

        inWebModule = clIdentifier.startsWith("WebModule:");

        addRepositoryInterface(repositoryInterface);
    }

    /**
     * Adds an entity class to be handled.
     * This is the entity class as the user sees it,
     * not a generated entity class for record entities.
     *
     * @param entityClass entity class.
     */
    @Trivial
    void addEntity(Class<?> entityClass) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "addEntity: " + entityClass.getName());

        entityTypes.add(entityClass);
    }

    /**
     * Adds a Repository interface represented by this FutureEmBuilder
     *
     * @param repositoryInterface repository interface
     */
    @Trivial
    void addRepositoryInterface(Class<?> repositoryInterface) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "addRepositoryInterface: " + repositoryInterface.getName());

        repositoryInterfaces.add(repositoryInterface);
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
    @Trivial // defer to EntityManagerBuilder
    public String getDDLFileName() {
        try {
            EntityManagerBuilder builder = get(DDLGEN_WAIT_TIME, TimeUnit.SECONDS);
            if (builder instanceof DDLGenerationParticipant) {
                return ((DDLGenerationParticipant) builder).getDDLFileName();
            }
        } catch (TimeoutException e) {
            // DDL generation MBean does not log errors; The following covers both
            // logging the error and raising an exception with the same message.
            throw exc(DataException.class,
                      "CWWKD1067.ddlgen.emf.timeout",
                      getClassNames(repositoryInterfaces),
                      dataStore,
                      DDLGEN_WAIT_TIME,
                      entityTypes.stream().map(Class::getName).collect(Collectors.toList()));
        } catch (Throwable ex) {
            // DDL generation MBean does not log errors; The following covers both
            // logging the error and raising an exception with the same message.
            Throwable cause = (ex instanceof ExecutionException) ? ex.getCause() : ex;
            DataException dx = exc(DataException.class,
                                   "CWWKD1066.ddlgen.failed",
                                   getClassNames(repositoryInterfaces),
                                   dataStore,
                                   entityTypes.stream() //
                                                   .map(Class::getName) //
                                                   .collect(Collectors.toList()),
                                   cause.getMessage());
            throw (DataException) dx.initCause(ex);
        }

        // Not using persistence service; return null and DDL generation will skip this future
        return null;
    }

    @Override
    @Trivial // defer to EntityManagerBuilder
    public void generate(Writer out) throws Exception {
        try {
            EntityManagerBuilder builder = get(DDLGEN_WAIT_TIME, TimeUnit.SECONDS);
            if (builder instanceof DDLGenerationParticipant) {
                ((DDLGenerationParticipant) builder).generate(out);
            }
        } catch (TimeoutException e) {
            // DDL generation MBean does not log errors; The following covers both
            // logging the error and raising an exception with the same message.
            throw exc(DataException.class,
                      "CWWKD1065.ddlgen.timeout",
                      getClassNames(repositoryInterfaces),
                      dataStore,
                      DDLGEN_WAIT_TIME,
                      entityTypes.stream().map(Class::getName).collect(Collectors.toList()));
        } catch (Throwable ex) {
            // DDL generation MBean does not log errors; The following covers both
            // logging the error and raising an exception with the same message.
            Throwable cause = (ex instanceof ExecutionException) ? ex.getCause() : ex;
            DataException dx = exc(DataException.class,
                                   "CWWKD1066.ddlgen.failed",
                                   getClassNames(repositoryInterfaces),
                                   dataStore,
                                   entityTypes.stream() //
                                                   .map(Class::getName) //
                                                   .collect(Collectors.toList()),
                                   cause.getMessage());
            throw (DataException) dx.initCause(ex);
        }
    }

    /**
     * Invoked to complete this future with the resulting EntityManagerBuilder value.
     *
     * @return PUnitEMBuilder (for persistence unit references) or
     *         DBStoreEMBuilder (data sources, databaseStore)
     */
    @FFDCIgnore(Throwable.class)
    public EntityManagerBuilder createEMBuilder() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // Avoid a web container deadlock (#31106) on metadataIdSvc.getMetaData
        // by first checking for ComponentMetaData that was observed by our
        // ComponentMetaDataListener:
        ComponentMetaData metadata = //
                        provider.componentMetadatasForModules.get(jeeName);

        try {
            if (namespace == Namespace.COMP &&
                !inWebModule &&
                jeeName.getModule() != null &&
                !DataExtension.DEFAULT_DATA_SOURCE.equals(dataStore))
                throw exc(DataException.class,
                          "CWWKD1061.comp.name.in.ejb",
                          getClassNames(repositoryInterfaces),
                          jeeName.getModule(),
                          jeeName.getApplication(),
                          dataStore,
                          "dataStore",
                          "java:comp",
                          List.of("java:app", "java:module"));

            if (metadata == null) {
                // metadataIdentifier examples:
                // WEB#MyApp#MyWebModule.war
                // EJB#MyApp#MyEJBModule.jar#MyEJB
                // DATA#MyApp

                String appName = jeeName.getApplication();
                String modName = jeeName.getModule();
                String metadataIdentifier = null;
                if (modName == null) {
                    metadataIdentifier = provider.getMetaDataIdentifier(appName,
                                                                        null,
                                                                        null);
                } else if (inWebModule) {
                    metadataIdentifier = provider.metadataIdSvc //
                                    .getMetaDataIdentifier("WEB",
                                                           appName,
                                                           modName,
                                                           null);

                    if (!provider.metadataIdSvc.isMetaDataAvailable(metadataIdentifier)) {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "not available: " + metadataIdentifier);
                        metadataIdentifier = null;
                    }
                }
                if (metadataIdentifier == null) {
                    metadataIdentifier = provider.metadataIdSvc //
                                    .getMetaDataIdentifier("EJB",
                                                           appName,
                                                           modName,
                                                           null);
                }

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "metadata identifier: " + metadataIdentifier);

                metadata = (ComponentMetaData) provider.metadataIdSvc //
                                .getMetaData(metadataIdentifier);
            }

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "using metadata: " + metadata);

            if (namespace != null) {
                ComponentMetaDataAccessorImpl accessor = //
                                ComponentMetaDataAccessorImpl //
                                                .getComponentMetaDataAccessor();
                if (metadata == null)
                    accessor.beginDefaultContext();
                else
                    accessor.beginContext(metadata);
                try {
                    Object resource = InitialContext.doLookup(dataStore);

                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, dataStore + " is the JNDI name for " + resource);

                    if (resource instanceof EntityManagerFactory)
                        return new PUnitEMBuilder(provider, //
                                        repositoryClassLoader, //
                                        repositoryInterfaces, //
                                        (EntityManagerFactory) resource, //
                                        dataStore, //
                                        entityTypes);
                } finally {
                    accessor.endContext();
                }
            }

            boolean javacolon = namespace != null; // any java: namespace

            return new DBStoreEMBuilder(provider, //
                            repositoryClassLoader, //
                            repositoryInterfaces, //
                            dataStore, //
                            javacolon, //
                            metadata, //
                            entityTypes);
        } catch (Throwable x) {
            // The error is logged to Tr.error rather than FFDC
            if (x instanceof DataException) { // already logged
                throw (DataException) x;
            } else if (x instanceof NamingException) {
                if (namespace == null)
                    throw excDataStoreNotFound(jeeName, metadata, x);
                else if (DataExtension.DEFAULT_DATA_SOURCE.equals(dataStore))
                    throw excDefaultDataSourceNotFound(jeeName, x);
                else
                    throw excJNDINameNotFound(jeeName, x);
            } else {
                throw (DataException) exc(DataException.class,
                                          "CWWKD1080.datastore.general.err",
                                          getClassNames(repositoryInterfaces),
                                          jeeName,
                                          dataStore,
                                          x).initCause(x);
            }
        }
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
     * Creates an exception for a Repository that sets the dataStore to a
     * value that is not found as an id or JNDI name, or is not accessible,
     * or the resource is misconfigured.
     *
     * @param jeeName  name of the application artifact containing the repository.
     * @param metadata metadata of the application artifact.
     * @param cause    cause for the exception.
     * @return DataException.
     */
    @FFDCIgnore(NamingException.class)
    @Trivial
    private DataException excDataStoreNotFound(J2EEName jeeName,
                                               ComponentMetaData metadata,
                                               Throwable cause) {
        DataException x = null;

        // Find out if the dataStore is a resource reference or persistence unit
        // reference that is missing a java:comp/env/ prefix.
        if (metadata != null) {
            ComponentMetaDataAccessorImpl accessor = //
                            ComponentMetaDataAccessorImpl //
                                            .getComponentMetaDataAccessor();

            accessor.beginContext(metadata);
            try {
                String javaCompName = "java:comp/env/" + dataStore;
                Object resource = InitialContext.doLookup(javaCompName);

                if (tc.isDebugEnabled())
                    Tr.debug(this, tc, javaCompName + " is the JNDI name for " +
                                       resource);

                if (resource instanceof DataSource ||
                    resource instanceof EntityManagerFactory) {
                    x = exc(DataException.class,
                            "CWWKD1027.datastore.lacks.java.comp",
                            getClassNames(repositoryInterfaces),
                            jeeName,
                            dataStore,
                            "dataStore = \"" + javaCompName + "\"");
                }
            } catch (NamingException namingX) {
                // Adding java:comp/env/ to dataStore does not help.
            } finally {
                accessor.endContext();
            }
        }

        if (x == null) {
            Class<?> repoInterface = repositoryInterfaces.stream().findFirst().get();
            Class<?> primary = DataExtension.getPrimaryEntityType(repoInterface);
            String exampleEntityClassName = primary == null //
                            ? "org.example.MyEntity" //
                            : primary.getName();

            String persistenceUnitExample = """

                            <persistence-unit name="MyPersistenceUnit">
                              <jta-data-source>jdbc/ds</jta-data-source>
                              <class>""" + exampleEntityClassName + """
                            </class>
                              <properties>
                                <property name="jakarta.persistence.schema-generation.database.action" value="create"/>
                              </properties>
                            </persistence-unit>
                            """;

            String dataSourceConfigExample = """

                            <dataSource id="MyDataSource" jndiName="jdbc/ds">
                              <jdbcDriver libraryRef="PostgresLib"/>
                              <properties.postgresql databaseName="exampledb" serverName="localhost" portNumber="5432"/>
                              <containerAuthData user=*** password=***/>
                            </dataSource>
                            <library id="PostgresLib">
                              <fileset dir="${server.config.dir}/lib/postgres" includes="*.jar"/>
                            </library>
                            """;

            String databaseStoreConfigExample = """

                            <databaseStore id="MyDatabaseStore" dataSourceRef="MyDataSource" createTables="true" tablePrefix="">
                              <authData user="***" password="***"/>
                            </databaseStore>
                            """;

            x = exc(DataException.class,
                    "CWWKD1078.datastore.not.found",
                    getClassNames(repositoryInterfaces),
                    jeeName,
                    dataStore,
                    dataSourceConfigExample,
                    databaseStoreConfigExample,
                    "@Resource(name=\"java:app/env/jdbc/dsRef\",lookup=\"jdbc/ds\")",
                    "jndiName=\"jdbc/ds\"",
                    "@PersistenceUnit(name=\"java:app/env/MyPersistenceUnitRef\",unitName=\"MyPersistenceUnit\")",
                    persistenceUnitExample);
        }

        return (DataException) x.initCause(cause);
    }

    /**
     * Creates an exception for a Repository that uses java:comp/DefaultDataSource
     * (the default if unspecified) but it is not configured.
     *
     * @param jeeName name of the application artifact containing the repository.
     * @param cause   cause for the exception, typically NamingException.
     * @return DataException.
     */
    @Trivial
    private DataException excDefaultDataSourceNotFound(J2EEName jeeName,
                                                       Throwable cause) {
        Class<?> primary = DataExtension.getPrimaryEntityType(repositoryInterfaces.stream().findFirst().get());
        String exampleEntityClassName = primary == null //
                        ? "org.example.MyEntity" //
                        : primary.getName();

        String persistenceUnitExample = """

                        <persistence-unit name="MyPersistenceUnit">
                          <jta-data-source>jdbc/ds</jta-data-source>
                          <class>""" + exampleEntityClassName + """
                        </class>
                          <properties>
                            <property name="jakarta.persistence.schema-generation.database.action" value="create"/>
                          </properties>
                        </persistence-unit>
                        """;

        String dataSourceConfigExample = """

                        <dataSource id="DefaultDataSource" jndiName="jdbc/ds">
                          <jdbcDriver libraryRef="PostgresLib"/>
                          <properties.postgresql databaseName="exampledb" serverName="localhost" portNumber="5432"/>
                          <containerAuthData user=*** password=***/>
                        </dataSource>
                        <library id="PostgresLib">
                          <fileset dir="${server.config.dir}/lib/postgres" includes="*.jar"/>
                        </library>
                        """;

        String databaseStoreConfigExample = """

                        <databaseStore id="MyDatabaseStore" dataSourceRef="DefaultDataSource" createTables="true" tablePrefix="">
                          <authData user="***" password="***"/>
                        </databaseStore>
                        """;

        DataException x = exc(DataException.class,
                              "CWWKD1077.defaultds.not.found",
                              jeeName,
                              getClassNames(repositoryInterfaces),
                              dataStore,
                              dataSourceConfigExample,
                              databaseStoreConfigExample,
                              "@Resource(name=\"java:app/env/jdbc/dsRef\",lookup=\"jdbc/ds\")",
                              "jndiName=\"jdbc/ds\"",
                              "@PersistenceUnit(name=\"java:app/env/MyPersistenceUnitRef\",unitName=\"MyPersistenceUnit\")",
                              persistenceUnitExample);

        return (DataException) x.initCause(cause);
    }

    /**
     * Creates an exception for a Repository that sets the dataStore to a
     * JNDI name that is not found, not accessible, or the resource is
     * misconfigured.
     *
     * @param jeeName name of the application artifact containing the repository.
     * @param cause   cause for the exception, typically NamingException.
     * @return DataException.
     */
    @Trivial
    private DataException excJNDINameNotFound(J2EEName jeeName, Throwable cause) {
        Class<?> primary = DataExtension.getPrimaryEntityType(repositoryInterfaces.stream().findFirst().get());
        String exampleEntityClassName = primary == null //
                        ? "org.example.MyEntity" //
                        : primary.getName();

        String persistenceUnitExample = """

                        <persistence-unit name="MyPersistenceUnit">
                          <jta-data-source>jdbc/ds</jta-data-source>
                          <class>""" + exampleEntityClassName + """
                        </class>
                          <properties>
                            <property name="jakarta.persistence.schema-generation.database.action" value="create"/>
                          </properties>
                        </persistence-unit>
                        """;

        String dataSourceConfigExample = """

                        <dataSource jndiName="jdbc/ds">
                          <jdbcDriver libraryRef="PostgresLib"/>
                          <properties.postgresql databaseName="exampledb" serverName="localhost" portNumber="5432"/>
                          <containerAuthData user=*** password=***/>
                        </dataSource>
                        <library id="PostgresLib">
                          <fileset dir="${server.config.dir}/lib/postgres" includes="*.jar"/>
                        </library>
                        """;

        DataException x = exc(DataException.class,
                              "CWWKD1079.jndi.not.found",
                              getClassNames(repositoryInterfaces),
                              jeeName,
                              dataStore,
                              "@Resource(name=\"java:app/env/jdbc/dsRef\",lookup=\"jdbc/ds\")",
                              "jndiName=\"jdbc/ds\"",
                              "@PersistenceUnit(name=\"java:app/env/MyPersistenceUnitRef\",unitName=\"MyPersistenceUnit\")",
                              persistenceUnitExample,
                              dataSourceConfigExample);

        return (DataException) x.initCause(cause);
    }

    /**
     * Obtains the module name in which the repository interface is defined,
     * or lacking that, the name of the application in which it is defined or used.
     *
     * @param repositoryInterface   the repository interface.
     * @param repositoryClassLoader class loader of the repository interface.
     * @param provider              OSGi service that provides the CDI extension.
     * @return AppName[#ModuleName] with only the application name if not defined
     *         in a module.
     * @throws IllegalStateException if not running on an application thread.
     */
    private J2EEName getArtifactName(Class<?> repositoryInterface,
                                     ClassLoader repositoryClassLoader,
                                     DataProvider provider) {
        J2EEName artifactName;

        Optional<J2EEName> moduleNameOptional = //
                        provider.cdiService.getModuleNameForClass(repositoryInterface);

        if (moduleNameOptional.isPresent()) {
            artifactName = moduleNameOptional.get();
        } else {
            // create component and module metadata based on the application metadata
            ComponentMetaData cdata = ComponentMetaDataAccessorImpl //
                            .getComponentMetaDataAccessor().getComponentMetaData();
            if (cdata == null) // internal error - used outside of an application
                throw new IllegalStateException();
            ApplicationMetaData adata = //
                            cdata.getModuleMetaData().getApplicationMetaData();
            cdata = provider.createComponentMetadata(adata, repositoryClassLoader);
            artifactName = cdata.getModuleMetaData().getJ2EEName();
        }

        return artifactName;
    }

    @Override
    @Trivial
    public int hashCode() {
        return dataStore.hashCode() +
               (application == null ? 0 : application.hashCode()) +
               (module == null ? 0 : module.hashCode());
    }

    /**
     * Write information about this instance to the introspection file for
     * Jakarta Data.
     *
     * @param writer writes to the introspection file.
     * @param indent indentation for lines.
     * @return EntityManagerBuilder if available from this FutureEMBuilder.
     */
    @FFDCIgnore(Throwable.class)
    @Trivial
    public Optional<EntityManagerBuilder> introspect(PrintWriter writer, String indent) {
        writer.println(indent + "FutureEMBuilder@" + Integer.toHexString(hashCode()));
        writer.println(indent + "  dataStore: " + dataStore);
        writer.println(indent + "  namespace: " + namespace);
        writer.println(indent + "    application: " + application);
        writer.println(indent + "    module: " + module);
        writer.println(indent + "  defining artifact: " + jeeName);
        writer.println(indent + "  repository class loader: " + repositoryClassLoader);

        repositoryInterfaces.forEach(r -> {
            writer.println(indent + "  repository: " + (r == null ? null : r.getName()));
        });

        entityTypes.forEach(e -> {
            writer.println(indent + "  entity: " + (e == null ? null : e.getName()));
        });

        EntityManagerBuilder builder = null;
        writer.print(indent + "  state: ");
        if (isCancelled())
            writer.println("cancelled");
        else if (isDone())
            try {
                builder = join();
                writer.println("completed");
            } catch (Throwable x) {
                writer.println("failed");
                Util.printStackTrace(x, writer, indent + "  ", null);
            }
        else
            writer.println("not completed");

        writer.println(indent + "  builder: " + builder);
        return Optional.ofNullable(builder);
    }

    @Override
    @Trivial
    public String toString() {
        int len = 40 + dataStore.length() +
                  (application == null ? 4 : application.length()) +
                  (module == null ? 4 : module.length());
        // Both hashCode and identityHashCode are included so that we can correlate
        // output in Liberty trace, which prints toString for values and method args
        // but uses uses identityHashCode (id=...) when printing trace for a class
        StringBuilder b = new StringBuilder(len) //
                        .append("FutureEMBuilder@") //
                        .append(Integer.toHexString(hashCode())) //
                        .append("(id=") //
                        .append(Integer.toHexString(System.identityHashCode(this))) //
                        .append(") ").append(dataStore) //
                        .append(' ').append(application) //
                        .append('#').append(module);
        return b.toString();
    }

    // Ensures order of DDL generation based on fields
    @Override
    @Trivial
    public int compareTo(FutureEMBuilder o) {
        if (this.equals(o)) {
            return 0;
        }

        if (this.application != null && o.application != null && !this.application.equals(o.application)) {
            return this.application.compareTo(o.application);
        }

        if (this.module != null && o.module != null && !this.module.equals(o.module)) {
            return this.module.compareTo(o.module);
        }

        if (!this.dataStore.equals(o.dataStore)) {
            return this.dataStore.compareTo(o.dataStore);
        }

        // We know the repository classloaders are different but with no natural ording.  Therefore,
        // the only other comparison we can make would be based off of the repository interfaces
        String r0 = this.repositoryInterfaces.stream().map(c -> c.getCanonicalName()).sorted().collect(Collectors.joining());
        String r1 = o.repositoryInterfaces.stream().map(c -> c.getCanonicalName()).sorted().collect(Collectors.joining());

        return r0.compareTo(r1);
    }
}
