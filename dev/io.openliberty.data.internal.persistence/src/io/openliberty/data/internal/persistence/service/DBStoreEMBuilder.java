/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
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
package io.openliberty.data.internal.persistence.service;

import static io.openliberty.data.internal.persistence.cdi.DataExtension.exc;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.wsspi.application.Application;
import com.ibm.wsspi.application.ApplicationState;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.persistence.DDLGenerationParticipant;
import com.ibm.wsspi.persistence.DatabaseStore;
import com.ibm.wsspi.persistence.InMemoryMappingFile;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;
import com.ibm.wsspi.resource.ResourceConfig;
import com.ibm.wsspi.resource.ResourceFactory;

import io.openliberty.data.internal.persistence.DataProvider;
import io.openliberty.data.internal.persistence.EntityInfo;
import io.openliberty.data.internal.persistence.EntityManagerBuilder;
import io.openliberty.data.internal.persistence.Util;
import io.openliberty.data.internal.persistence.orm.EntityParser;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.MappingException;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;

/**
 * This builder is used when a data source JNDI name, id, resource reference,
 * or a dataStore id is configured as the repository dataStore.
 * It creates entity managers from a PersistenceServiceUnit from the persistence service.
 */
public class DBStoreEMBuilder extends EntityManagerBuilder implements DDLGenerationParticipant {
    private static final long MAX_WAIT_FOR_SERVICE_NS = TimeUnit.SECONDS.toNanos(60);

    private static final TraceComponent tc = Tr.register(DBStoreEMBuilder.class);

    private final ClassDefiner classDefiner = new ClassDefiner();

    /**
     * The id of a databaseStore configuration element.
     */
    private final String databaseStoreId;

    /**
     * The config display id of a databaseStore which may be the same as the databaseStoreId.
     */
    private final String configDisplayId;

    /**
     * DataSourceFactory.target property of the databaseStore configuration element.
     */
    private final String dataSourceFactoryFilter;

    /**
     * A map of generated entity class to the record class for which it was generated.
     */
    private final Map<Class<?>, Class<?>> generatedToRecordClass = new HashMap<>();

    /**
     * The persistence service unit is obtained from the persistence service during the
     * initialization that is performed by the run method.
     */
    private final PersistenceServiceUnit persistenceServiceUnit;

    /**
     * Locates an existing databaseStore or creates a new one corresponding to the
     * dataStore name that is specified on the Repository annotation.
     *
     * @param provider              OSGi service that provides the CDI extension.
     * @param repositoryClassLoader class loader of the repository interface.
     * @param repositoryInterfaces  repository interfaces that use the entities.
     * @param dataStore             dataStore value from the Repository annotation,
     *                                  or the value with java:comp/env added.
     * @param isJNDIName            indicates if the dataStore name is a JNDI name
     *                                  (begins with java: or is inferred to be java:comp/env/...)
     * @param metadata              metadata of the application artifact that
     *                                  contains the repository interface.
     *                                  Module and component might be null or absent.
     * @param entityTypes           entity classes as known by the user, not generated.
     * @throws Exception if an error occurs.
     */
    public DBStoreEMBuilder(DataProvider provider,
                            ClassLoader repositoryClassLoader,
                            Set<Class<?>> repositoryInterfaces,
                            String dataStore,
                            boolean isJNDIName,
                            ComponentMetaData metadata,
                            Set<Class<?>> entityTypes) throws Exception {
        super(provider, repositoryClassLoader, repositoryInterfaces, dataStore);
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        String qualifiedName = null;
        boolean javaApp = false, javaModule = false, javaComp = false;
        J2EEName jeeName = metadata.getJ2EEName();
        String application = jeeName == null ? null : jeeName.getApplication();
        String module = jeeName == null ? null : jeeName.getModule();

        // Qualify resource reference and DataSourceDefinition JNDI names with the application/module/component name to make them unique
        if (isJNDIName) {
            javaApp = dataStore.regionMatches(5, "app", 0, 3);
            javaModule = !javaApp && dataStore.regionMatches(5, "module", 0, 6);
            javaComp = !javaApp && !javaModule && dataStore.regionMatches(5, "comp", 0, 4);
            StringBuilder s = new StringBuilder(dataStore.length() + 80);
            if (application != null && (javaApp || javaModule || javaComp)) {
                s.append("application[").append(application).append(']').append('/');
                if (module != null && (javaModule || javaComp))
                    s.append("module[").append(module).append(']').append('/');
            }
            qualifiedName = s.append("databaseStore[").append(dataStore).append(']').toString();

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "computed qualified dataStore name from JNDI name as " + qualifiedName);
        }

        Map<String, Configuration> dbStoreConfigurations = provider.dbStoreConfigAllApps.get(application);
        Configuration dbStoreConfig = dbStoreConfigurations == null ? null : dbStoreConfigurations.get(isJNDIName ? qualifiedName : dataStore);
        Dictionary<String, Object> dbStoreConfigProps = dbStoreConfig == null ? null : dbStoreConfig.getProperties();
        String dbStoreId = dbStoreConfigProps == null ? null : (String) dbStoreConfigProps.get("id");
        if (dbStoreId == null) {
            String dsFactoryFilter = null;
            BundleContext bc = FrameworkUtil.getBundle(DatabaseStore.class).getBundleContext();
            ServiceReference<ResourceFactory> dsRef = null;
            if (isJNDIName) {
                // Look for DataSourceDefinition with jndiName and application/module matching
                StringBuilder filter = new StringBuilder(200) //
                                .append("(&(service.factoryPid=com.ibm.ws.jdbc.dataSource)");
                if (application != null && (javaApp || javaModule || javaComp))
                    filter.append(FilterUtils.createPropertyFilter("application", application));
                if (module != null && javaModule || javaComp)
                    filter.append(FilterUtils.createPropertyFilter("module", module));
                filter.append(FilterUtils.createPropertyFilter("jndiName", dataStore)) //
                                .append(')');

                Collection<ServiceReference<ResourceFactory>> dsRefs = bc.getServiceReferences(ResourceFactory.class,
                                                                                               filter.toString());
                if (!dsRefs.isEmpty()) {
                    dbStoreId = qualifiedName;
                    dsRef = dsRefs.iterator().next();
                }
            } else {
                // Look for databaseStore with id matching
                String filter = FilterUtils.createPropertyFilter("id", dataStore);
                Collection<ServiceReference<DatabaseStore>> dbStoreRefs = bc.getServiceReferences(DatabaseStore.class, filter);
                if (!dbStoreRefs.isEmpty()) {
                    dbStoreId = dataStore;
                    dsFactoryFilter = (String) dbStoreRefs.iterator().next().getProperty("DataSourceFactory.target");
                } else {
                    // Look for dataSource with id matching
                    filter = "(&(service.factoryPid=com.ibm.ws.jdbc.dataSource)" + FilterUtils.createPropertyFilter("id", dataStore) + ')';
                    Collection<ServiceReference<ResourceFactory>> dsRefs = bc.getServiceReferences(ResourceFactory.class, filter);
                    if (!dsRefs.isEmpty()) {
                        dbStoreId = "application[" + application + "]/databaseStore[" + dataStore + ']';
                        dsRef = dsRefs.iterator().next();
                    } else {
                        // Look for dataSource with jndiName matching
                        filter = "(&(service.factoryPid=com.ibm.ws.jdbc.dataSource)" + FilterUtils.createPropertyFilter("jndiName", dataStore) + ')';
                        dsRefs = bc.getServiceReferences(ResourceFactory.class, filter);
                        if (!dsRefs.isEmpty()) {
                            dbStoreId = "application[" + application + "]/databaseStore[" + dataStore + ']';
                            dsRef = dsRefs.iterator().next();
                        } // else no databaseStore or dataSource is found
                    }
                }
            }
            if (dbStoreId == null) {
                // Create a ResourceFactory that can delegate back to a resource reference lookup
                ResourceFactory delegator = new ResRefDelegator(dataStore, metadata);
                Hashtable<String, Object> svcProps = new Hashtable<String, Object>();
                dbStoreId = isJNDIName ? qualifiedName : ("application[" + application + "]/databaseStore[" + dataStore + ']');
                String id = dbStoreId + "/ResourceFactory";
                svcProps.put("id", id);
                svcProps.put("config.displayId", id);
                if (application != null)
                    svcProps.put("application", application);
                ServiceRegistration<ResourceFactory> reg = bc.registerService(ResourceFactory.class, delegator, svcProps);
                dsRef = reg.getReference();//

                Queue<ServiceRegistration<ResourceFactory>> registrations = provider.delegatorsAllApps.get(application);
                if (registrations == null) {
                    Queue<ServiceRegistration<ResourceFactory>> empty = new ConcurrentLinkedQueue<>();
                    if ((registrations = provider.delegatorsAllApps.putIfAbsent(application, empty)) == null)
                        registrations = empty;
                }
                registrations.add(reg);
            }

            // If we generated a databaseStore id, then create the configuration for it,
            if (dbStoreId != dataStore) {
                if (dbStoreConfigurations == null) {
                    Map<String, Configuration> empty = new ConcurrentHashMap<>();
                    if ((dbStoreConfigurations = provider.dbStoreConfigAllApps.putIfAbsent(application, empty)) == null)
                        dbStoreConfigurations = empty;
                }

                String dataSourceId = (String) dsRef.getProperty("id");
                boolean nonJTA = Boolean.FALSE.equals(dsRef.getProperty("transactional"));

                configDisplayId = dbStoreId;

                Hashtable<String, Object> svcProps = new Hashtable<String, Object>();
                svcProps.put("id", configDisplayId);
                svcProps.put("config.displayId", configDisplayId);

                if (dataSourceId == null)
                    dsFactoryFilter = "(jndiName=" + dsRef.getProperty("jndiName") + ')';
                else
                    dsFactoryFilter = "(id=" + dataSourceId + ')';

                svcProps.put("DataSourceFactory.target", dsFactoryFilter);

                svcProps.put("AuthData.target", "(service.pid=${authDataRef})");
                svcProps.put("AuthData.cardinality.minimum", 0);

                svcProps.put("NonJTADataSourceFactory.cardinality.minimum", nonJTA ? 1 : 0);
                if (nonJTA)
                    svcProps.put("NonJTADataSourceFactory.target", svcProps.get("DataSourceFactory.target"));
                else
                    svcProps.put("NonJTADataSourceFactory.target", "(&(service.pid=${nonTransactionalDataSourceRef})(transactional=false))");

                svcProps.put("createTables", provider.createTables);
                svcProps.put("dropTables", provider.dropTables);
                svcProps.put("tablePrefix", "");
                svcProps.put("keyGenerationStrategy", "AUTO");

                dbStoreConfig = provider.configAdmin.createFactoryConfiguration("com.ibm.ws.persistence.databaseStore", bc.getBundle().getLocation());
                dbStoreConfig.update(svcProps);
                dbStoreConfigurations.put(isJNDIName ? qualifiedName : dataStore, dbStoreConfig);
            } else if (dsRef != null) {
                configDisplayId = (String) dsRef.getProperty("config.displayId");
                dsFactoryFilter = "(config.displayId=" + configDisplayId + ')';
            } else {
                configDisplayId = "databaseStore[" + dbStoreId + "]";
            }
            dataSourceFactoryFilter = dsFactoryFilter;
        } else {
            dataSourceFactoryFilter = (String) dbStoreConfigProps.get("DataSourceFactory.target");
            configDisplayId = dbStoreId;
        }

        databaseStoreId = dbStoreId;

        BundleContext bc = FrameworkUtil.getBundle(DatabaseStore.class).getBundleContext();

        ServiceReference<DatabaseStore> ref = null;
        for (long start = System.nanoTime(), poll_ms = 125L; //
                        ref == null; //
                        poll_ms = poll_ms < 1000L ? poll_ms * 2 : 1000L) {
            String filter = FilterUtils.createPropertyFilter("id", databaseStoreId);
            Collection<ServiceReference<DatabaseStore>> refs = //
                            bc.getServiceReferences(DatabaseStore.class, filter);
            if (refs.isEmpty()) {
                if (FrameworkState.isStopping())
                    throw exc(IllegalStateException.class,
                              "CWWKD1113.server.stopping",
                              Util.names(repositoryInterfaces));

                if (application != null) {
                    filter = FilterUtils.createPropertyFilter("name", application);
                    Collection<ServiceReference<Application>> appRefs = //
                                    bc.getServiceReferences(Application.class, filter);
                    if (appRefs.isEmpty()) {
                        throw exc(IllegalStateException.class,
                                  "CWWKD1115.app.unavailable",
                                  Util.names(repositoryInterfaces),
                                  application);
                    } else {
                        Object state = appRefs.iterator().next() //
                                        .getProperty("application.state");
                        if (ApplicationState.STOPPED == state ||
                            ApplicationState.STOPPING == state)
                            throw exc(IllegalStateException.class,
                                      "CWWKD1114.app.stopping",
                                      Util.names(repositoryInterfaces),
                                      application,
                                      state);
                    }
                }

                if (System.nanoTime() - start < MAX_WAIT_FOR_SERVICE_NS) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Wait " + poll_ms +
                                           " ms for service reference to become available...");
                    TimeUnit.MILLISECONDS.sleep(poll_ms);
                } else {
                    throw exc(IllegalStateException.class,
                              "CWWKD1116.resource.unavailable",
                              Util.names(repositoryInterfaces),
                              configDisplayId,
                              TimeUnit.NANOSECONDS.toSeconds(MAX_WAIT_FOR_SERVICE_NS));
                }
            } else {
                ref = refs.iterator().next();
            }
        }

        String tablePrefix = (String) ref.getProperty("tablePrefix");

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, configDisplayId + " databaseStore reference", ref);

        ArrayList<InMemoryMappingFile> generatedEntities = new ArrayList<InMemoryMappingFile>();

        EntityParser parser = new EntityParser(tablePrefix, provider);

        for (Class<?> c : entityTypes) {
            if (c.isAnnotationPresent(Entity.class)) {
                parser.parseAnnotatedEntity(c);
            } else if (c.isRecord()) {
                disallowPersistenceAnnos(c, true);

                // an entity class is generated for the record
                String entityClassName = c.getName() + EntityInfo.RECORD_ENTITY_SUFFIX;
                byte[] generatedEntityBytes = RecordTransformer //
                                .generateEntityClassBytes(c,
                                                          entityClassName,
                                                          jeeName,
                                                          repositoryInterfaces);
                String name = entityClassName.replace('.', '/') + ".class";
                generatedEntities.add(new InMemoryMappingFile(generatedEntityBytes, name));
                Class<?> ec = classDefiner.findLoadedOrDefineClass(getRepositoryClassLoader(),
                                                                   entityClassName,
                                                                   generatedEntityBytes);
                generatedToRecordClass.put(ec, c);
                parser.parseRecord(c, ec);
            } else {
                disallowPersistenceAnnos(c, false);
                parser.parseUnannotatedEntity(c);
            }
        }

        Map<String, Object> properties = new HashMap<>();

        List<String> entityClassInfo = parser.generateView();
        LinkedHashSet<String> entityClassNames = parser.getClassNames();
        LinkedHashSet<String> entityTableNames = parser.getTableNames();
        Set<Class<?>> convertibleTypes = parser.getConvertibles();

        properties.put("io.openliberty.persistence.internal.entityClassInfo",
                       entityClassInfo.toArray(new String[entityClassInfo.size()]));

        properties.put("io.openliberty.persistence.internal.tableNames", entityTableNames);

        if (!generatedEntities.isEmpty())
            properties.put("io.openliberty.persistence.internal.generatedEntities", generatedEntities);

        DatabaseStore dbstore = bc.getService(ref);
        persistenceServiceUnit = dbstore.createPersistenceServiceUnit(getRepositoryClassLoader(),
                                                                      properties,
                                                                      entityClassNames.toArray(new String[entityClassNames.size()]));

        collectEntityInfo(entityTypes, convertibleTypes);
    }

    @Override
    @Trivial
    public EntityManager createEntityManager() {
        EntityManager em = persistenceServiceUnit.createEntityManager();
        em.setCacheRetrieveMode(CacheRetrieveMode.BYPASS);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "createEntityManager: " + em);
        return em;
    }

    /**
     * Raises an error if any method (or field if not a Java record) is annotated
     * with an annotation from Jakarta Persistence.
     *
     * @param c        class that does not have the Entity annotation.
     * @param isRecord true if the entity class is a Java record, otherwise false.
     */
    @Trivial
    private void disallowPersistenceAnnos(Class<?> c, boolean isRecord) {

        if (!isRecord)
            for (Field field : c.getDeclaredFields())
                for (Annotation anno : field.getAnnotations())
                    if (anno.annotationType().getPackageName() //
                                    .startsWith("jakarta.persistence"))
                        throw exc(MappingException.class,
                                  "CWWKD1108.missing.entity.anno",
                                  c.getName(),
                                  Entity.class.getName(),
                                  anno.annotationType().getName(),
                                  field.getName());

        for (Method method : c.getDeclaredMethods())
            for (Annotation anno : method.getAnnotations())
                if (anno.annotationType().getPackageName() //
                                .startsWith("jakarta.persistence"))
                    if (isRecord)
                        throw exc(MappingException.class,
                                  "CWWKD1109.jpa.anno.on.record",
                                  anno.annotationType().getName(),
                                  method.getName(),
                                  c.getName());
                    else
                        throw exc(MappingException.class,
                                  "CWWKD1108.missing.entity.anno",
                                  c.getName(),
                                  Entity.class.getName(),
                                  anno.annotationType().getName(),
                                  method.getName());
    }

    /**
     * Obtains the DataSource that is used by the EntityManager.
     * This method is used by resource accessor methods of a repository.
     *
     * @param repoMethod    repository resource accessor method.
     * @param repoInterface repository interface.
     * @return the DataSource that is used by the EntityManager.
     */
    @Override
    public DataSource getDataSource(Method repoMethod, Class<?> repoInterface) {
        BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();
        Collection<ServiceReference<ResourceFactory>> dsFactoryRefs;
        try {
            dsFactoryRefs = bc.getServiceReferences(ResourceFactory.class,
                                                    dataSourceFactoryFilter);
        } catch (InvalidSyntaxException x) {
            throw new RuntimeException(x); // should never happen
        }

        if (dsFactoryRefs.isEmpty())
            throw exc(IllegalStateException.class,
                      "CWWKD1062.resource.not.found",
                      repoMethod.getName(),
                      repoInterface.getName(),
                      DataSource.class.getSimpleName(),
                      dataSourceFactoryFilter);

        ResourceFactory dsFactory = bc.getService(dsFactoryRefs.iterator().next());
        try {
            ResourceConfig resRef = null;
            if (!(dsFactory instanceof ResRefDelegator)) {
                // Use a resource reference that includes the authDataRef of the databaseStore.
                resRef = provider.resourceConfigFactory.createResourceConfig(DataSource.class.getName());
                resRef.setSharingScope(ResourceConfig.SHARING_SCOPE_SHAREABLE);
                resRef.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
                resRef.setResAuthType(ResourceConfig.AUTH_CONTAINER);

                String dbStoreFilter = FilterUtils.createPropertyFilter("id", databaseStoreId);
                Collection<ServiceReference<DatabaseStore>> dbStoreRefs = //
                                bc.getServiceReferences(DatabaseStore.class,
                                                        dbStoreFilter);
                if (dbStoreRefs.isEmpty())
                    throw exc(IllegalStateException.class,
                              "CWWKD1062.resource.not.found",
                              repoMethod.getName(),
                              repoInterface.getName(),
                              "databaseStore",
                              dbStoreFilter);

                ServiceReference<DatabaseStore> ref = dbStoreRefs.iterator().next();
                if (ref.getProperty("authDataRef") != null) {
                    String authDataFilter = (String) ref.getProperty("AuthData.target");
                    ServiceReference<?>[] authDataRefs = //
                                    bc.getServiceReferences("com.ibm.websphere.security.auth.data.AuthData",
                                                            authDataFilter);
                    if (authDataRefs == null)
                        throw exc(IllegalStateException.class,
                                  "CWWKD1062.resource.not.found",
                                  repoMethod.getName(),
                                  repoInterface.getName(),
                                  "authData",
                                  authDataFilter);

                    // The following pattern is copied from DatabaseStoreImpl,
                    String authDataId = (String) authDataRefs[0].getProperty("id");
                    resRef.addLoginProperty("DefaultPrincipalMapping",
                                            authDataId.matches(".*(\\]/).*(\\[default-\\d*\\])") //
                                                            ? (String) authDataRefs[0].getProperty("config.displayId") //
                                                            : authDataId);
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "using resource reference", resRef);
            }

            return (DataSource) dsFactory.createResource(resRef);
        } catch (Exception x) {
            if (x instanceof RuntimeException &&
                x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD"))
                throw (RuntimeException) x;

            String datastore = dsFactory instanceof ResRefDelegator //
                            ? ((ResRefDelegator) dsFactory).jndiName //
                            : databaseStoreId;

            throw (DataException) exc(DataException.class,
                                      "CWWKD1064.datastore.error",
                                      repoMethod.getName(),
                                      repoInterface.getName(),
                                      datastore,
                                      x.getMessage()).initCause(x);
        }
    }

    @Override
    @Trivial
    protected Class<?> getRecordClass(Class<?> generatedEntityClass) {
        return generatedToRecordClass.get(generatedEntityClass);
    }

    /**
     * Write information about this instance to the introspection file for
     * Jakarta Data.
     *
     * @param writer writes to the introspection file.
     * @param indent indentation for lines.
     */
    @Override
    @Trivial
    public void introspect(PrintWriter writer, String indent) {
        super.introspect(writer, indent);
        writer.println(indent + "  databaseStore config.displayId: " +
                       configDisplayId);
        writer.println(indent + "  databaseStore id: " +
                       databaseStoreId);
        writer.println(indent + "  databaseStore DataSourceFactory.target: " +
                       dataSourceFactoryFilter);
        writer.println(indent + "  PersistenceServiceUnit: " +
                       persistenceServiceUnit);

        generatedToRecordClass.forEach((entityClass, recordClass) -> {
            writer.println(indent + "  Record entity:");
            writer.println(Util.toString(recordClass, indent + "    "));
            writer.println(indent + "    converted to entity class:");
            writer.println(Util.toString(entityClass, indent + "    "));
        });
    }

    /**
     * Returns true if the cause exception can be determined to be a
     * connection-related error, otherwise false.
     *
     * @param cause the cause exception.
     * @return true if known to be a connection-related error, otherise false.
     */
    @FFDCIgnore(Exception.class) // secondary error while reporting first error
    @Override
    public boolean isConnectionError(SQLException cause) {
        boolean isConnectionError = false;
        try {
            WSJdbcDataSource ds = (WSJdbcDataSource) getDataSource(null, null);
            isConnectionError = ds != null &&
                                ds.getDatabaseHelper().isConnectionError(cause);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Could not obtain DataSource to check isConnectionError");
        }

        return isConnectionError || super.isConnectionError(cause);
    }

    @Override
    @Trivial
    public String toString() {
        return new StringBuilder(26 + configDisplayId.length()) //
                        .append("DBStoreEMBuilder@") //
                        .append(Integer.toHexString(hashCode())) //
                        .append(":").append(configDisplayId) //
                        .toString();
    }

    /**
     * Generates DDL from the PersistenceServiceUnit obtained from the Persistence Service
     * for creating EntityManagers, and writes it out.
     *
     * @param out a Writer where DDL will be written
     */
    @Override
    public void generate(Writer out) throws Exception {
        // Note that exceptions thrown here or by the persistence service will be logged by the
        // direct caller (FutureEMBuilder) or the DDL generation MBean.
        if (persistenceServiceUnit == null) {
            throw new IllegalStateException("EntityManagerFactory for Jakarta Data repository has not been initialized for the " + configDisplayId + " DatabaseStore.");
        }
        persistenceServiceUnit.generateDDL(out);
    }

    @Override
    public String getDDLFileName() {
        return configDisplayId + "_JakartaData";
    }
}