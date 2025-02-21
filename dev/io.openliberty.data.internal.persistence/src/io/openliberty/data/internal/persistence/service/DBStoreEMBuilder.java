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

import static io.openliberty.data.internal.persistence.Util.EOLN;
import static io.openliberty.data.internal.persistence.cdi.DataExtension.exc;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.sql.Connection;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
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
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
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
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.MappingException;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;

/**
 * This builder is used when a data source JNDI name, id, resource reference,
 * or a dataStore id is configured as the repository dataStore.
 * It creates entity managers from a PersistenceServiceUnit from the persistence service.
 */
public class DBStoreEMBuilder extends EntityManagerBuilder implements DDLGenerationParticipant {
    private static final long MAX_WAIT_FOR_SERVICE_NS = TimeUnit.SECONDS.toNanos(60);
    private static final Entry<String, String> ID_AND_VERSION_NOT_SPECIFIED = //
                    new SimpleImmutableEntry<>(null, null);
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
     * @param isJNDIName            indicates if the dataStore name is a JNDI name (begins with java: or is inferred to be java:comp/env/...)
     * @param metaDataIdentifier    metadata identifier for the class loader of the repository interface.
     * @param jeeName               application/module/component in which the repository interface is defined.
     *                                  Module and component might be null or absent.
     * @param entityTypes           entity classes as known by the user, not generated.
     * @throws Exception if an error occurs.
     */
    public DBStoreEMBuilder(DataProvider provider,
                            ClassLoader repositoryClassLoader,
                            Set<Class<?>> repositoryInterfaces,
                            String dataStore,
                            boolean isJNDIName,
                            String metadataIdentifier,
                            J2EEName jeeName,
                            Set<Class<?>> entityTypes) throws Exception {
        super(provider, repositoryClassLoader, repositoryInterfaces, dataStore);
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        String qualifiedName = null;
        boolean javaApp = false, javaModule = false, javaComp = false;
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
                ResourceFactory delegator = new ResRefDelegator(dataStore, metadataIdentifier, provider);
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
        for (long start = System.nanoTime(), poll_ms = 125L; ref == null; poll_ms = poll_ms < 1000L ? poll_ms * 2 : 1000L) {
            Collection<ServiceReference<DatabaseStore>> refs = bc.getServiceReferences(DatabaseStore.class,
                                                                                       FilterUtils.createPropertyFilter("id", databaseStoreId));
            if (refs.isEmpty()) {
                if (System.nanoTime() - start < MAX_WAIT_FOR_SERVICE_NS) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Wait " + poll_ms + " ms for service reference to become available...");
                    TimeUnit.MILLISECONDS.sleep(poll_ms);
                } else {
                    throw new IllegalStateException("The " + configDisplayId + " service component did not become available within " +
                                                    TimeUnit.NANOSECONDS.toSeconds(MAX_WAIT_FOR_SERVICE_NS) + " seconds.");
                }
            } else {
                ref = refs.iterator().next();
            }
        }

        String tablePrefix = (String) ref.getProperty("tablePrefix");

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, configDisplayId + " databaseStore reference", ref);

        // Classes explicitly annotated with JPA @Entity:
        Set<String> entityClassNames = new LinkedHashSet<>(entityTypes.size() * 2);
        Set<String> entityTableNames = new LinkedHashSet<>(entityClassNames.size());

        ArrayList<InMemoryMappingFile> generatedEntities = new ArrayList<InMemoryMappingFile>();

        // List of classes to inspect for the above
        Queue<Class<?>> annotatedEntityClassQueue = new LinkedList<>();

        // XML to make all other classes into JPA entities:
        ArrayList<String> entityClassInfo = new ArrayList<>(entityTypes.size());

        /*
         * Note: When creating a persistence unit, managed classes (such as entities) are declared in an
         * all or nothing fashion. Therefore, if we create a persistence unit with a list of entities
         * we are also required to provide a list of converters, otherwise the persistence provider
         * will not use them. Ideally, our internal persistence service unit would have a method to
         * include converter classes alongside entity classes, but the persistence provider API lacks
         * such function so the converters need to be put into the generated orm.xml file.
         */
        Set<Class<?>> converterTypes = new HashSet<>();

        Map<Class<?>, Map<String, Class<?>>> embeddableTypes = //
                        new LinkedHashMap<>();

        for (Class<?> c : entityTypes) {
            if (c.isAnnotationPresent(Entity.class)) {
                annotatedEntityClassQueue.add(c);

                for (Field field : c.getFields()) {
                    Convert convert = field.getAnnotation(Convert.class);
                    if (convert != null)
                        converterTypes.add(convert.converter());
                }

                for (Method method : c.getMethods()) {
                    Convert convert = method.getAnnotation(Convert.class);
                    if (convert != null)
                        converterTypes.add(convert.converter());
                }
            } else {
                // The table is named from the class name of the entity or
                // record that is specified by the user, not from the generated
                // entity class that is used internally in place of a record.
                String tableName = c.getSimpleName();

                if (c.isRecord()) {
                    // an entity class is generated for the record
                    String entityClassName = c.getName() + EntityInfo.RECORD_ENTITY_SUFFIX;
                    byte[] generatedEntityBytes = RecordTransformer //
                                    .generateEntityClassBytes(c,
                                                              entityClassName,
                                                              jeeName,
                                                              repositoryInterfaces);
                    String name = entityClassName.replace('.', '/') + ".class";
                    generatedEntities.add(new InMemoryMappingFile(generatedEntityBytes, name));
                    Class<?> generatedEntity = classDefiner //
                                    .findLoadedOrDefineClass(getRepositoryClassLoader(),
                                                             entityClassName,
                                                             generatedEntityBytes);
                    generatedToRecordClass.put(generatedEntity, c);
                    c = generatedEntity;
                }

                StringBuilder xml = new StringBuilder(500);

                xml.append(" <entity class=\"").append(c.getName()) //
                                .append("\">").append(EOLN);

                xml.append("  <table name=\"") //
                                .append(tablePrefix).append(tableName) //
                                .append("\"/>").append(EOLN);

                writeAttributes(xml, findAttributes(c), embeddableTypes);

                xml.append(" </entity>").append(EOLN);

                entityClassInfo.add(xml.toString());
            }
        }

        for (Entry<Class<?>, Map<String, Class<?>>> e : embeddableTypes.entrySet()) {
            Class<?> type = e.getKey();
            Map<String, Class<?>> attrs = e.getValue();

            StringBuilder xml = new StringBuilder(500) //
                            .append(" <embeddable class=\"") //
                            .append(type.getName()).append("\">") //
                            .append(EOLN);
            writeAttributes(xml, attrs, null);
            xml.append(" </embeddable>").append(EOLN);
            entityClassInfo.add(xml.toString());
        }

        for (Class<?> type : converterTypes) {
            StringBuilder xml = new StringBuilder(500) //
                            .append(" <converter class=\"") //
                            .append(type.getName()).append("\"></converter>") //
                            .append(EOLN);
            entityClassInfo.add(xml.toString());
        }

        // Discover entities that are indirectly referenced via OneToOne, ManyToMany, and so forth
        for (Class<?> c; (c = annotatedEntityClassQueue.poll()) != null;)
            if (entityClassNames.add(c.getName())) {
                Table table = c.getAnnotation(Table.class);
                entityTableNames.add(table == null || table.name().length() == 0 ? c.getSimpleName() : table.name());
                Class<?> e;
                for (Field f : c.getFields())
                    if (f.getType().isAnnotationPresent(Entity.class))
                        annotatedEntityClassQueue.add(f.getType());
                    else if ((e = getEntityClass(f.getGenericType())) != null)
                        annotatedEntityClassQueue.add(e);
                for (Method m : c.getMethods())
                    if (m.getReturnType().isAnnotationPresent(Entity.class))
                        annotatedEntityClassQueue.add(m.getReturnType());
                    else if ((e = getEntityClass(m.getGenericReturnType())) != null)
                        annotatedEntityClassQueue.add(e);
            }

        Map<String, Object> properties = new HashMap<>();

        properties.put("io.openliberty.persistence.internal.entityClassInfo",
                       entityClassInfo.toArray(new String[entityClassInfo.size()]));

        properties.put("io.openliberty.persistence.internal.tableNames", entityTableNames);

        if (!generatedEntities.isEmpty())
            properties.put("io.openliberty.persistence.internal.generatedEntities", generatedEntities);

        DatabaseStore dbstore = bc.getService(ref);
        persistenceServiceUnit = dbstore.createPersistenceServiceUnit(getRepositoryClassLoader(),
                                                                      properties,
                                                                      entityClassNames.toArray(new String[entityClassNames.size()]));

        collectEntityInfo(entityTypes);
    }

    @Override
    @Trivial
    public EntityManager createEntityManager() {
        EntityManager em = persistenceServiceUnit.createEntityManager();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "createEntityManager: " + em);
        return em;
    }

    /**
     * Find attributes of the specified class.
     * If a record, use the record components.
     * Otherwise, use fields and property descriptors.
     *
     * @param c entity class or embedded class.
     * @return attributes, sorted alphabetically.
     */
    private SortedMap<String, Class<?>> findAttributes(Class<?> c) {
        SortedMap<String, Class<?>> attributes = new TreeMap<>();

        if (c.isRecord()) {
            for (RecordComponent r : c.getRecordComponents())
                attributes.put(r.getName(), r.getType());
        } else {
            for (Field f : c.getFields())
                attributes.put(f.getName(), f.getType());

            try {
                PropertyDescriptor[] propertyDescriptors = Introspector //
                                .getBeanInfo(c).getPropertyDescriptors();
                if (propertyDescriptors != null)
                    for (PropertyDescriptor p : propertyDescriptors) {
                        Method setter = p.getWriteMethod();
                        if (setter != null)
                            attributes.putIfAbsent(p.getName(),
                                                   p.getPropertyType());
                    }
            } catch (IntrospectionException x) {
                throw new MappingException(x);
            }
        }

        return attributes;
    }

    /**
     * Find the Id and Version attributes (if any).
     *
     * @param attributes top level entity or embeddable attributes.
     * @return the Id and Version attributes (if any).
     *         Null if there is no Id.
     */
    private Entry<String, String> findIdAndVersion(Map<String, Class<?>> attrs) {
        String idAttrName = null;
        String versionAttrName = null;

        // Determine which attribute is the id and version (optional).
        // Id precedence:
        // (1) name is id, ignoring case.
        // (2) name ends with _id, ignoring case.
        // (3) name ends with Id or ID.
        // (4) type is UUID.
        // Version precedence (if also a valid version type):
        // (1) name is version, ignoring case.
        // (2) name is _version, ignoring case.
        int idPrecedence = 10;
        int vPrecedence = 10;
        for (Map.Entry<String, Class<?>> attribute : attrs.entrySet()) {
            String name = attribute.getKey();
            Class<?> type = attribute.getValue();
            int len = name.length();

            if (idPrecedence > 1 &&
                len >= 2 &&
                name.regionMatches(true, len - 2, "id", 0, 2)) {
                if (name.length() == 2) {
                    idAttrName = name;
                    idPrecedence = 1;
                } else if (idPrecedence > 2 &&
                           name.charAt(len - 3) == '_') {
                    idAttrName = name;
                    idPrecedence = 2;
                } else if (idPrecedence > 3 &&
                           name.charAt(len - 2) == 'I') {
                    idAttrName = name;
                    idPrecedence = 3;
                }
            } else if (idPrecedence > 4 && UUID.class.equals(type)) {
                idAttrName = name;
                idPrecedence = 4;
            }

            if (vPrecedence > 1 &&
                len == 7 &&
                Util.VERSION_TYPES.contains(type) &&
                "version".equalsIgnoreCase(name)) {
                versionAttrName = name;
                vPrecedence = 1;
            } else if (vPrecedence > 2 &&
                       len == 8 &&
                       Util.VERSION_TYPES.contains(type) &&
                       "_version".equalsIgnoreCase(name)) {
                versionAttrName = name;
                vPrecedence = 2;
            }
        }

        return idAttrName == null //
                        ? null //
                        : new SimpleImmutableEntry<>(idAttrName, versionAttrName);
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

            throw (DataException) exc(DataException.class,
                                      "CWWKD1064.datastore.error",
                                      repoMethod.getName(),
                                      repoInterface.getName(),
                                      databaseStoreId,
                                      x.getMessage()).initCause(x);
        }
    }

    /**
     * Obtains the entity class (if any) for a type that might be parameterized.
     *
     * @param type a type that might be parameterized.
     * @return entity class or null.
     */
    @Trivial
    private Class<?> getEntityClass(java.lang.reflect.Type type) {
        Class<?> c = null;
        if (type instanceof ParameterizedType) {
            java.lang.reflect.Type[] typeParams = ((ParameterizedType) type).getActualTypeArguments();
            type = typeParams.length == 1 ? typeParams[0] : null;
            if (type instanceof Class && ((Class<?>) type).isAnnotationPresent(Entity.class))
                c = (Class<?>) type;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "getEntityClass from parameterized " + type + ": " + c);
        }
        return c;
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
     * Write attributes for the specified entity or embeddable to XML.
     *
     * @param xml             XML for defining the entity attributes
     * @param attributes      top level entity or embeddable attributes.
     * @param embeddableTypes embeddable types list to add to. Null if the specified
     *                            attributes are already for an embeddable.
     *                            When non-null, this method adds embeddable types
     *                            that are found.
     */
    private void writeAttributes(StringBuilder xml,
                                 Map<String, Class<?>> attributes,
                                 Map<Class<?>, Map<String, Class<?>>> embeddableTypes) {
        boolean isEmbeddable = embeddableTypes == null;

        Entry<String, String> idAndVersion = isEmbeddable //
                        ? ID_AND_VERSION_NOT_SPECIFIED //
                        : findIdAndVersion(attributes);

        // Write the attributes to XML:

        xml.append("  <attributes>").append(EOLN);

        for (Map.Entry<String, Class<?>> attributeInfo : attributes.entrySet()) {
            String attributeName = attributeInfo.getKey();
            Class<?> attributeType = attributeInfo.getValue();
            boolean isCollection = Collection.class.isAssignableFrom(attributeType);
            boolean isPrimitive = attributeType.isPrimitive();
            boolean isId = attributeName.equals(idAndVersion.getKey());

            String columnType;
            if (isPrimitive || //
                attributeType.isInterface() || //
                Serializable.class.isAssignableFrom(attributeType)) {
                if (isId)
                    columnType = "id";
                else if (isCollection)
                    columnType = "element-collection";
                else if (attributeName.equals(idAndVersion.getValue()))
                    columnType = "version";
                else
                    columnType = "basic";
            } else {
                if (isId)
                    columnType = "embedded-id";
                else
                    columnType = "embedded";
            }

            xml.append("   <").append(columnType).append(" name=\"") //
                            .append(attributeName).append('"');

            // All other queries when using un-annotated entities or record entities are eager,
            // element-collections should be as well.
            if (isCollection)
                xml.append(" fetch=\"EAGER\"");

            xml.append('>').append(EOLN);

            if (!isEmbeddable && // top level entity attribute
                columnType.charAt(1) == 'm') { // embedded or embedded-id
                LinkedList<Entry<String[], Map<String, Class<?>>>> stack = //
                                new LinkedList<>();
                Map<String, Class<?>> attrs = embeddableTypes.get(attributeType);
                if (attrs == null)
                    embeddableTypes.put(attributeType,
                                        attrs = findAttributes(attributeType));
                stack.add(new SimpleImmutableEntry<>( //
                                new String[] { attributeName }, //
                                attrs));
                for (Entry<String[], Map<String, Class<?>>> emb; //
                                null != (emb = stack.pollLast());) {
                    String[] names = emb.getKey();
                    Map<String, Class<?>> embeddableAttrs = emb.getValue();
                    for (Entry<String, Class<?>> e : embeddableAttrs.entrySet()) {
                        String name = e.getKey();
                        Class<?> type = e.getValue();
                        // attribute-override is only written for leaf-level
                        if (type.isPrimitive() ||
                            type.isInterface() ||
                            Serializable.class.isAssignableFrom(type)) {
                            xml.append("    <attribute-override name=\"");
                            for (int n = 1; n < names.length; n++)
                                xml.append(names[n]).append('.');
                            xml.append(name).append("\">").append(EOLN);
                            xml.append("     <column name=\"");
                            for (int n = 0; n < names.length; n++)
                                xml.append(names[n].toUpperCase()).append('_');
                            xml.append(name.toUpperCase()) //
                                            .append("\"/>").append(EOLN);
                            xml.append("    </attribute-override>").append(EOLN);

                            // Table name collision detection is unnecessary because
                            // the same collisions would already be caught by entity
                            // attribute name collision detection.
                        } else {
                            Map<String, Class<?>> a = embeddableTypes.get(type);
                            if (a == null)
                                embeddableTypes.put(type, a = findAttributes(type));
                            String[] names2 = new String[names.length + 1];
                            System.arraycopy(names, 0, names2, 0, names.length);
                            names2[names.length] = name;
                            stack.add(new SimpleImmutableEntry<>(names2, a));
                        }
                    }
                }
            }

            if (isPrimitive)
                if (isEmbeddable) {
                    // Primitive attributes on an embeddable might need the null
                    // value to indicate that the embeddable itself is null.
                } else {
                    xml.append("    <column nullable=\"false\"/>").append(EOLN);
                }

            xml.append("   </" + columnType + ">").append(EOLN);
        }

        xml.append("  </attributes>").append(EOLN);
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