/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

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
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.persistence.DatabaseStore;
import com.ibm.wsspi.resource.ResourceFactory;

import io.openliberty.data.internal.persistence.EntityDefiner;
import io.openliberty.data.internal.persistence.QueryInfo;
import jakarta.annotation.Generated;
import jakarta.data.exceptions.MappingException;
import jakarta.data.model.StaticMetamodel;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterTypeDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;

/**
 * CDI extension to handle the injection of repository implementations
 * that this Jakarta Data provider can supply.
 */
public class DataExtension implements Extension, PrivilegedAction<DataExtensionProvider> {
    private static final TraceComponent tc = Tr.register(DataExtension.class);

    /**
     * OSGi service that registers this extension.
     */
    private final DataExtensionProvider provider = AccessController.doPrivileged(this);

    /**
     * Beans for repository interfaces.
     * Beans are removed as they are processed to allow for the CDI extension methods to be invoked again
     * for different applications or the same application being restarted.
     */
    private final Queue<Bean<?>> repositoryBeans = new ConcurrentLinkedQueue<>();

    /**
     * Map of repository type to databaseStore id.
     * Entries are removed as they are processed to allow for the CDI extension methods to be invoked again
     * for different applications or the same application being restarted.
     */
    private final ConcurrentHashMap<AnnotatedType<?>, String> repositoryTypes = new ConcurrentHashMap<>();

    /**
     * Map of entity class to list of static metamodel class.
     */
    private final Map<Class<?>, List<Class<?>>> staticMetamodels = new HashMap<>();

    /**
     * A key for a group of entities for the same backend database
     * that are loaded with the same class loader.
     */
    @Trivial
    private static class EntityGroupKey {
        private final String databaseId;
        private final int hash;
        private final ClassLoader loader;

        EntityGroupKey(String databaseId, ClassLoader loader) {
            this.loader = loader;
            this.databaseId = databaseId;
            hash = loader.hashCode() + databaseId.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            EntityGroupKey k;
            return o instanceof EntityGroupKey
                   && databaseId.equals((k = (EntityGroupKey) o).databaseId)
                   && loader.equals(k.loader);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    @Trivial
    public <T> void annotatedRepository(@Observes @WithAnnotations(Repository.class) ProcessAnnotatedType<T> event) {
        AnnotatedType<T> type = event.getAnnotatedType();

        Repository repository = type.getAnnotation(Repository.class);

        String provider = repository.provider();
        boolean provide = Repository.ANY_PROVIDER.equals(provider) || "OpenLiberty".equalsIgnoreCase(provider); // TODO provider name

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "annotatedRepository to " + (provide ? "provide" : "ignore"),
                     repository.toString(), type.getJavaClass().getName());

        if (provide) {
            String dataStore = repository.dataStore();
            if (dataStore.length() == 0)
                dataStore = "defaultDatabaseStore";
            else
                dataStore = findOrCreateDatabaseStore(dataStore, type);

            repositoryTypes.put(type, dataStore);
        }
    }

    @Trivial
    public <T> void annotatedStaticMetamodel(@Observes @WithAnnotations(StaticMetamodel.class) ProcessAnnotatedType<T> event) {
        AnnotatedType<T> type = event.getAnnotatedType();

        if (type.isAnnotationPresent(Generated.class)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "annotatedStaticMetamodel ignoring generated " + type.getJavaClass().getName());
        } else {
            StaticMetamodel staticMetamodel = type.getAnnotation(StaticMetamodel.class);

            Class<?> entityClass = staticMetamodel.value();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "annotatedStaticMetamodel for " + entityClass.getName(),
                         type.getJavaClass().getName());

            List<Class<?>> newList = new LinkedList<>();
            newList.add(type.getJavaClass());
            List<Class<?>> existingList = staticMetamodels.putIfAbsent(entityClass, newList);
            if (existingList != null)
                existingList.add(type.getJavaClass());
        }
    }

    public void afterTypeDiscovery(@Observes AfterTypeDiscovery event, BeanManager beanMgr) {
        // Group entities by data access provider and class loader
        Map<EntityGroupKey, EntityDefiner> entityGroups = new HashMap<>();

        for (Iterator<Map.Entry<AnnotatedType<?>, String>> it = repositoryTypes.entrySet().iterator(); it.hasNext();) {
            Map.Entry<AnnotatedType<?>, String> entry = it.next();
            it.remove();

            AnnotatedType<?> repositoryType = entry.getKey();
            String databaseStoreId = entry.getValue();
            Class<?> repositoryInterface = repositoryType.getJavaClass();
            ClassLoader loader = repositoryInterface.getClassLoader();

            Class<?>[] primaryEntityClassReturnValue = new Class<?>[1];
            Map<Class<?>, List<QueryInfo>> queriesPerEntityClass = new HashMap<>();
            if (discoverEntityClasses(repositoryType, queriesPerEntityClass, primaryEntityClassReturnValue)) {
                EntityGroupKey entityGroupKey = new EntityGroupKey(databaseStoreId, loader);
                EntityDefiner entityDefiner = entityGroups.get(entityGroupKey);
                if (entityDefiner == null)
                    entityGroups.put(entityGroupKey, entityDefiner = new EntityDefiner(entityGroupKey.databaseId, loader));

                for (Class<?> entityClass : queriesPerEntityClass.keySet())
                    entityDefiner.add(entityClass);

                BeanAttributes<?> attrs = beanMgr.createBeanAttributes(repositoryType);
                Bean<?> bean = beanMgr.createBean(attrs, repositoryInterface, new RepositoryProducer.Factory<>( //
                                repositoryInterface, beanMgr, provider, this, //
                                entityDefiner, primaryEntityClassReturnValue[0], queriesPerEntityClass));
                repositoryBeans.add(bean);
            }
        }

        for (EntityDefiner entityDefiner : entityGroups.values()) {
            provider.executor.submit(entityDefiner);
        }

        for (EntityDefiner entityDefiner : entityGroups.values()) {
            entityDefiner.populateStaticMetamodelClasses(staticMetamodels);
        }
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager beanMgr) {
        for (Bean<?> bean; (bean = repositoryBeans.poll()) != null;) {
            event.addBean(bean);
        }
    }

    /**
     * Locates an existing databaseStore or creates a new one corresponding to the
     * dataStore name that is specified on the Repository annotation.
     *
     * @param name dataStore name specified on the Repository annotation.
     * @param type AnnotatedType for the interface that is annotated with the Repository annotation.
     * @return id of databaseStore to use.
     */
    private String findOrCreateDatabaseStore(String name, AnnotatedType<?> type) {
        ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        J2EEName jeeName = cData == null ? null : cData.getJ2EEName();
        String application = jeeName == null ? null : jeeName.getApplication();
        String module = jeeName == null ? null : jeeName.getModule();
        String qualifiedName = null;
        boolean javaAppOrModuleOrComp = false;

        // Qualify resource reference and DataSourceDefinition JNDI names with the application/module/component name to make them unique
        if (name.startsWith("java:")) {
            boolean javaApp = name.regionMatches(5, "app", 0, 3);
            boolean javaModule = !javaApp && name.regionMatches(5, "module", 0, 6);
            boolean javaComp = !javaApp && !javaModule && name.regionMatches(5, "comp", 0, 4);
            javaAppOrModuleOrComp = javaApp || javaModule || javaComp;
            StringBuilder s = new StringBuilder(name.length() + 80);
            if (application != null && javaAppOrModuleOrComp) {
                s.append("application[").append(application).append(']').append('/');
                if (module != null && (javaModule || javaComp))
                    s.append("module[").append(module).append(']').append('/');
            }
            qualifiedName = s.append("databaseStore[").append(name).append(']').toString();
        }

        Map<String, Configuration> dbStoreConfigurations = provider.dbStoreConfigAllApps.get(application);
        Configuration dbStoreConfig = dbStoreConfigurations == null ? null : dbStoreConfigurations.get(name);
        String dbStoreId = dbStoreConfig == null ? null : (String) dbStoreConfig.getProperties().get("id");
        if (dbStoreId == null)
            try {
                BundleContext bc = FrameworkUtil.getBundle(DatabaseStore.class).getBundleContext();
                ServiceReference<ResourceFactory> dsRef = null;
                if (qualifiedName == null) {
                    // Look for databaseStore with id matching
                    String filter = FilterUtils.createPropertyFilter("id", name);
                    Collection<ServiceReference<DatabaseStore>> dbStoreRefs = bc.getServiceReferences(DatabaseStore.class, filter);
                    if (!dbStoreRefs.isEmpty()) {
                        return name;
                    } else {
                        // Look for dataSource with id matching
                        filter = "(&(service.factoryPid=com.ibm.ws.jdbc.dataSource)" + FilterUtils.createPropertyFilter("id", name) + ')';
                        Collection<ServiceReference<ResourceFactory>> dsRefs = bc.getServiceReferences(ResourceFactory.class, filter);
                        if (!dsRefs.isEmpty()) {
                            dbStoreId = name;
                            dsRef = dsRefs.iterator().next();
                        } else {
                            // Look for dataSource with jndiName matching
                            filter = "(&(service.factoryPid=com.ibm.ws.jdbc.dataSource)" + FilterUtils.createPropertyFilter("jndiName", name) + ')';
                            dsRefs = bc.getServiceReferences(ResourceFactory.class, filter);
                            if (!dsRefs.isEmpty()) {
                                dbStoreId = name;
                                dsRef = dsRefs.iterator().next();
                            } // else no databaseStore or dataSource is found
                        }
                    }
                }
                if (dbStoreId == null) {
                    // Look for DataSourceDefinition with jndiName matching
                    String filter = "(&(service.factoryPid=com.ibm.ws.jdbc.dataSource)" + //
                                    (javaAppOrModuleOrComp ? FilterUtils.createPropertyFilter("application", application) : "") + //
                                    FilterUtils.createPropertyFilter("jndiName", name) + ')';
                    Collection<ServiceReference<ResourceFactory>> dsRefs = bc.getServiceReferences(ResourceFactory.class, filter);
                    if (!dsRefs.isEmpty()) {
                        dbStoreId = qualifiedName == null ? name : qualifiedName;
                        dsRef = dsRefs.iterator().next();
                    } else {
                        // Create a ResourceFactory that can delegate back to a resource reference lookup
                        ResourceFactory delegator = new DelegatingResourceFactory(name, cData);
                        Hashtable<String, Object> svcProps = new Hashtable<String, Object>();
                        dbStoreId = qualifiedName == null ? name : qualifiedName;
                        String id = dbStoreId + "/ResourceFactory";
                        svcProps.put("id", id);
                        svcProps.put("config.displayId", id);
                        if (application != null)
                            svcProps.put("application", application);
                        ServiceRegistration<ResourceFactory> reg = bc.registerService(ResourceFactory.class, delegator, svcProps);
                        dsRef = reg.getReference();

                        Queue<ServiceRegistration<ResourceFactory>> registrations = provider.delegatorsAllApps.get(application);
                        if (registrations == null) {
                            Queue<ServiceRegistration<ResourceFactory>> empty = new ConcurrentLinkedQueue<>();
                            if ((registrations = provider.delegatorsAllApps.putIfAbsent(application, empty)) == null)
                                registrations = empty;
                        }
                        registrations.add(reg);
                    }

                    if (dbStoreConfigurations == null) {
                        Map<String, Configuration> empty = new ConcurrentHashMap<>();
                        if ((dbStoreConfigurations = provider.dbStoreConfigAllApps.putIfAbsent(application, empty)) == null)
                            dbStoreConfigurations = empty;
                    }

                    String dataSourceId = (String) dsRef.getProperty("id");
                    boolean nonJTA = Boolean.FALSE.equals(dsRef.getProperty("transactional"));

                    Hashtable<String, Object> svcProps = new Hashtable<String, Object>();
                    svcProps.put("id", dbStoreId);
                    svcProps.put("config.displayId", qualifiedName == null ? ("databaseStore[" + dbStoreId + ']') : qualifiedName);

                    svcProps.put("DataSourceFactory.target", "(id=" + dataSourceId + ')');

                    svcProps.put("AuthData.target", "(service.pid=${authDataRef})");
                    svcProps.put("AuthData.cardinality.minimum", 0);

                    if (nonJTA) {
                        svcProps.put("NonJTADataSourceFactory.target", "(id=" + dataSourceId + ')');
                    } else {
                        svcProps.put("NonJTADataSourceFactory.target", "(&(service.pid=${nonTransactionalDataSourceRef})(transactional=false))");
                    }
                    svcProps.put("NonJTADataSourceFactory.cardinality.minimum", nonJTA ? 1 : 0);

                    // TODO should the databaseStore properties be configurable somehow when DataSourceDefinition is used?
                    // The following would allow them in the annotation's properties list, as "data.createTables=true", "data.tablePrefix=TEST"
                    svcProps.put("createTables", !"FALSE".equalsIgnoreCase((String) dsRef.getProperty("properties.0.data.createTables")));
                    svcProps.put("dropTables", !"TRUE".equalsIgnoreCase((String) dsRef.getProperty("properties.0.data.dropTables")));
                    svcProps.put("tablePrefix", Objects.requireNonNullElse((String) dsRef.getProperty("properties.0.data.tablePrefix"), "DATA"));
                    svcProps.put("keyGenerationStrategy", Objects.requireNonNullElse((String) dsRef.getProperty("properties.0.data.keyGenerationStrategy"), "AUTO"));

                    dbStoreConfig = provider.configAdmin.createFactoryConfiguration("com.ibm.ws.persistence.databaseStore", bc.getBundle().getLocation());
                    dbStoreConfig.update(svcProps);
                    dbStoreConfigurations.put(name, dbStoreConfig);
                }
            } catch (InvalidSyntaxException | IOException x) {
                throw new RuntimeException(x);
            } catch (Error | RuntimeException x) {
                throw x;
            }
        return dbStoreId;
    }

    /**
     * Identifies entity classes that are referenced by an interface that is annotated as a Repository
     * and determines the primary entity class.
     *
     * Many repository interfaces will inherit from DataRepository or another built-in repository class,
     * all of which are parameterized with the entity class as the first parameter.
     *
     * @param repositoryType                the repository interface as an annotated type.
     * @param queriesPerEntity              initially empty map to populate with partially completed query information per entity.
     * @param primaryEntityClassReturnValue initially empty size 1 array for returning the primary entity class, if any.
     * @return whether all entity types that appear on the repository interface are supported.
     */
    private boolean discoverEntityClasses(AnnotatedType<?> repositoryType,
                                          Map<Class<?>, List<QueryInfo>> queriesPerEntity,
                                          Class<?>[] primaryEntityClassReturnValue) {
        Class<?> repositoryInterface = repositoryType.getJavaClass();
        Class<?> primaryEntityClass = null;
        Set<Class<?>> lifecycleMethodEntityClasses = new HashSet<>();

        // Look for parameterized type variable of the repository interface, for example,
        // public interface MyRepository extends DataRepository<MyEntity, IdType>
        for (java.lang.reflect.AnnotatedType interfaceType : repositoryInterface.getAnnotatedInterfaces()) {
            if (interfaceType instanceof AnnotatedParameterizedType) {
                AnnotatedParameterizedType parameterizedType = (AnnotatedParameterizedType) interfaceType;
                java.lang.reflect.AnnotatedType typeParams[] = parameterizedType.getAnnotatedActualTypeArguments();
                Type firstParamType = typeParams.length > 0 ? typeParams[0].getType() : null;
                if (firstParamType != null && firstParamType instanceof Class) {
                    primaryEntityClass = (Class<?>) firstParamType;
                    if (typeParams.length == 2 && parameterizedType.getType().getTypeName().startsWith(DataRepository.class.getPackageName()))
                        break; // spec-defined repository interfaces take precedence if multiple interfaces are present
                }
            }
        }

        for (Method method : repositoryInterface.getMethods()) {
            if (method.isDefault()) // skip default methods
                continue;

            // Check for resource accessor methods:
            Class<?> returnType = method.getReturnType();
            if (method.getParameterCount() == 0 &&
                (EntityManager.class.equals(returnType)
                 || DataSource.class.equals(returnType)
                 || Connection.class.equals(returnType))) {
                QueryInfo queryInfo = new QueryInfo(method, QueryInfo.Type.RESOURCE_ACCESS);

                List<QueryInfo> queries = queriesPerEntity.get(Void.class);
                if (queries == null)
                    queriesPerEntity.put(Void.class, queries = new ArrayList<>());
                queries.add(queryInfo);
                continue;
            }

            Class<?> returnArrayComponentType = null;
            List<Class<?>> returnTypeAtDepth = new ArrayList<>(5);
            Type type = method.getGenericReturnType();
            for (int depth = 0; depth < 5 && type != null; depth++) {
                if (type instanceof ParameterizedType) {
                    returnTypeAtDepth.add((Class<?>) ((ParameterizedType) type).getRawType());
                    Type[] typeParams = ((ParameterizedType) type).getActualTypeArguments();
                    type = typeParams.length == 1 ? typeParams[0] : null;
                } else if (type instanceof Class) {
                    Class<?> c = (Class<?>) type;
                    returnTypeAtDepth.add(c);
                    if (IntStream.class.equals(type)) {
                        returnTypeAtDepth.add(int.class);
                        depth++;
                    } else if (LongStream.class.equals(type)) {
                        returnTypeAtDepth.add(long.class);
                        depth++;
                    } else if (DoubleStream.class.equals(type)) {
                        returnTypeAtDepth.add(double.class);
                        depth++;
                    } else if (returnArrayComponentType == null) {
                        returnArrayComponentType = c.getComponentType();
                        if (returnArrayComponentType != null) {
                            returnTypeAtDepth.add(returnArrayComponentType);
                            depth++;
                        }
                    }
                    type = null;
                } else if (type instanceof GenericArrayType) {
                    // TODO cover the possibility that the generic type could be for something other than the entity, such as the primary key?
                    Class<?> arrayComponentType = primaryEntityClass;
                    returnTypeAtDepth.add(arrayComponentType.arrayType());
                    if (returnArrayComponentType == null) {
                        returnTypeAtDepth.add(returnArrayComponentType = arrayComponentType);
                        depth++;
                    }
                    type = null;
                } else {
                    returnTypeAtDepth.add(primaryEntityClass);
                    type = null;
                }
            }

            // Possible entity class based on the return type:
            Class<?> entityClass = returnTypeAtDepth.get(returnTypeAtDepth.size() - 1);

            Class<?> entityParamType = null;

            // Determine entity class from a lifecycle method parameter:
            if (method.getParameterCount() == 1
                && !method.isDefault()
                && method.getAnnotation(Query.class) == null
                && (method.getAnnotation(Insert.class) != null
                    || method.getAnnotation(Update.class) != null
                    || method.getAnnotation(Save.class) != null
                    || method.getAnnotation(Delete.class) != null)) {
                Class<?> c = method.getParameterTypes()[0];
                if (Iterable.class.isAssignableFrom(c) || Stream.class.isAssignableFrom(c)) {
                    type = method.getGenericParameterTypes()[0];
                    if (type instanceof ParameterizedType) {
                        Type[] typeParams = ((ParameterizedType) type).getActualTypeArguments();
                        if (typeParams.length == 1 && typeParams[0] instanceof Class) // for example, List<Product>
                            c = (Class<?>) typeParams[0];
                        else { // could be a method like BasicRepository.saveAll(Iterable<S> entity) {
                            entityParamType = c;
                            c = null;
                        }
                    } else {
                        c = null;
                    }
                } else if (c.isArray()) {
                    c = c.getComponentType();
                }
                if (Object.class.equals(c)) { // generic parameter like BasicRepository.save(S entity)
                    entityParamType = method.getParameterTypes()[0];
                } else if (c != null &&
                           !c.isPrimitive() &&
                           !c.isInterface()) {
                    String packageName = c.getPackageName();
                    if (!packageName.startsWith("java.") &&
                        !packageName.startsWith("jakarta.")) {
                        Parameter param = method.getParameters()[0];
                        entityParamType = param.getType();
                        for (Annotation anno : param.getAnnotations())
                            if (anno.annotationType().getPackageName().startsWith("jakarta.data"))
                                entityParamType = null;
                        if (entityParamType != null) {
                            entityClass = c;
                            lifecycleMethodEntityClasses.add(c);
                        }
                    }
                }
            }

            QueryInfo queryInfo = new QueryInfo(method, entityParamType, returnArrayComponentType, returnTypeAtDepth);

            if (entityClass == null) {
                entityClass = Void.class;
            } else {
                // TODO find better ways of determining non-entities ******** require @Entity unless found on lifecycle method!!!!!!
                String packageName = entityClass.getPackageName();
                if (packageName.startsWith("java.")
                    || packageName.startsWith("jakarta.")
                    || entityClass.isPrimitive()
                    || entityClass.isInterface())
                    entityClass = Void.class;
            }

            List<QueryInfo> queries = queriesPerEntity.get(entityClass);
            if (queries == null)
                queriesPerEntity.put(entityClass, queries = new ArrayList<>());
            queries.add(queryInfo);
        }

        List<QueryInfo> additionalQueriesForPrimaryEntity = queriesPerEntity.remove(Void.class);

        // Confirm which classes are actually entity classes and that all entity classes are supported
        boolean supportsAllEntities = true;

        Set<Class<?>> allEntityClasses = new HashSet<>();
        if (primaryEntityClass != null) {
            allEntityClasses.add(primaryEntityClass);
            supportsAllEntities &= supportsEntity(primaryEntityClass, repositoryType);
        }
        for (Class<?> c : lifecycleMethodEntityClasses) {
            if (allEntityClasses.add(c))
                supportsAllEntities &= supportsEntity(c, repositoryType);
        }

        for (Iterator<Entry<Class<?>, List<QueryInfo>>> it = queriesPerEntity.entrySet().iterator(); it.hasNext();) {
            Entry<Class<?>, List<QueryInfo>> entry = it.next();
            Class<?> c = entry.getKey();
            if (!allEntityClasses.contains(c))
                if (c.getAnnotation(Entity.class) == null) {
                    // Our provider doesn't recognize this as an entity class. Find out if another provider might:
                    supportsAllEntities &= supportsEntity(c, repositoryType);
                    if (additionalQueriesForPrimaryEntity == null)
                        additionalQueriesForPrimaryEntity = new ArrayList<>();
                    additionalQueriesForPrimaryEntity.addAll(entry.getValue());
                    it.remove();
                } else {
                    // The entity class is supported because it is annotated with jakarta.persistence.Entity
                    allEntityClasses.add(c);
                }
        }

        if (supportsAllEntities) {
            if (primaryEntityClass == null) {
                // Primary entity class is inferred to be the only entity class found on lifecycle methods
                if (lifecycleMethodEntityClasses.size() == 1)
                    primaryEntityClass = lifecycleMethodEntityClasses.iterator().next();
                else if (lifecycleMethodEntityClasses.isEmpty()) {
                    // Primary entity class is inferred to be the only entity class that appears on repository methods
                    if (queriesPerEntity.size() == 1)
                        primaryEntityClass = queriesPerEntity.keySet().iterator().next();
                    else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Unable to determine primary entity class because there are multiple entity classes: ",
                                 queriesPerEntity.keySet());
                }
            } else if (!queriesPerEntity.containsKey(primaryEntityClass)) {
                queriesPerEntity.put(primaryEntityClass, new ArrayList<>());
            }

            if (additionalQueriesForPrimaryEntity != null && !additionalQueriesForPrimaryEntity.isEmpty())
                if (primaryEntityClass == null) {
                    throw new MappingException("@Repository " + repositoryInterface.getName() + " does not specify an entity class." + // TODO NLS
                                               " To correct this, have the repository interface extend DataRepository" + // TODO can we include example type vars?
                                               " or another built-in repository interface and supply the entity class as the first parameter.");
                } else {
                    List<QueryInfo> queries = queriesPerEntity.get(primaryEntityClass);
                    if (queries == null)
                        queriesPerEntity.put(primaryEntityClass, additionalQueriesForPrimaryEntity);
                    else
                        queries.addAll(additionalQueriesForPrimaryEntity);
                }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, repositoryInterface.getName() + " has primary entity class " + primaryEntityClass,
                     "and methods that use the following entities:", queriesPerEntity);

        primaryEntityClassReturnValue[0] = primaryEntityClass;
        return supportsAllEntities;
    }

    /**
     * Obtain the service that informed CDI of this extension.
     */
    @Override
    @Trivial
    public DataExtensionProvider run() {
        BundleContext bundleContext = FrameworkUtil.getBundle(DataExtensionProvider.class).getBundleContext();
        ServiceReference<DataExtensionProvider> ref = bundleContext.getServiceReference(DataExtensionProvider.class);
        return bundleContext.getService(ref);
    }

    /**
     * Determine if the entity is supported by this provider based on
     * it having the JPA Entity annotation or having no entity annotations.
     *
     * @param entityClass    entity class
     * @param repositoryType repository type
     * @return whether the entity type is supported by this provider.
     */
    private boolean supportsEntity(Class<?> entityClass, AnnotatedType<?> repositoryType) {
        Annotation[] entityClassAnnos = entityClass.getAnnotations();

        boolean hasEntityAnnos = false;
        for (Annotation anno : entityClassAnnos) {
            Class<? extends Annotation> annoClass = anno.annotationType();
            if (annoClass.equals(Entity.class))
                return true;
            else if (annoClass.getSimpleName().endsWith("Entity"))
                hasEntityAnnos = true;
        }

        boolean isSupported = !hasEntityAnnos;
        if (hasEntityAnnos) {
            Repository repository = repositoryType.getAnnotation(Repository.class);
            if (!Repository.ANY_PROVIDER.equals(repository.provider()))
                throw new MappingException("Open Liberty's built-in Jakarta Data provider cannot provide the " +
                                           repositoryType.getJavaClass().getName() + " repository because the repository's " +
                                           entityClass.getName() + " entity class includes an unrecognized entity annotation. " +
                                           " The following annotations are found on the entity class: " + Arrays.toString(entityClassAnnos) +
                                           ". Supported entity annotations are: " + Entity.class.getName() + "."); // TODO NLS
        }

        return isSupported;
    }
}