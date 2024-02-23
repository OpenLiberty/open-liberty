/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.data.internal.persistence.EntityManagerBuilder;
import io.openliberty.data.internal.persistence.QueryInfo;
import io.openliberty.data.internal.persistence.provider.PUnitEMBuilder;
import io.openliberty.data.internal.persistence.service.DBStoreEMBuilder;
import jakarta.annotation.Generated;
import jakarta.data.exceptions.MappingException;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.inject.Qualifier;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

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
     * Map of repository annotated type to Repository annotation.
     * Entries are removed as they are processed to allow for the CDI extension methods to be invoked again
     * for different applications or the same application being restarted.
     */
    private final ConcurrentHashMap<AnnotatedType<?>, Repository> repositoryAnnos = new ConcurrentHashMap<>();

    /**
     * Map of entity class to list of static metamodel class.
     */
    private final Map<Class<?>, List<Class<?>>> staticMetamodels = new HashMap<>();

    @Trivial
    public <T> void annotatedRepository(@Observes @WithAnnotations(Repository.class) ProcessAnnotatedType<T> event) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        AnnotatedType<T> type = event.getAnnotatedType();
        Repository repository = type.getAnnotation(Repository.class);

        String dataProvider = repository.provider();
        boolean provide = Repository.ANY_PROVIDER.equals(dataProvider) || "OpenLiberty".equalsIgnoreCase(dataProvider); // TODO provider name

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "annotatedRepository to " + (provide ? "provide" : "ignore"),
                     repository.toString(), type.getJavaClass().getName());

        if (provide)
            repositoryAnnos.put(type, repository);
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

    @FFDCIgnore(NamingException.class)
    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager beanMgr) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // Group entities by data access provider and class loader
        Map<EntityManagerBuilder, EntityManagerBuilder> entityGroups = new HashMap<>();

        for (Iterator<AnnotatedType<?>> it = repositoryAnnos.keySet().iterator(); it.hasNext();) {
            AnnotatedType<?> repositoryType = it.next();
            it.remove();

            Repository repository = repositoryType.getAnnotation(Repository.class);
            Class<?> repositoryInterface = repositoryType.getJavaClass();
            ClassLoader loader = repositoryInterface.getClassLoader();

            EntityManagerBuilder emBuilder = null;
            String dataStore = repository.dataStore();
            boolean isConfigDisplayId;
            boolean isJNDIName;
            if (dataStore.length() == 0) {
                dataStore = "defaultDatabaseStore";
                isConfigDisplayId = false;
                isJNDIName = false;

                // Look for resource accessor method with qualifiers
                // TODO if we keep this code, make it more efficient/stable. Identification of resource accessor methods
                // is also done by discoverEntityClasses.
                for (Method method : repositoryInterface.getMethods()) {
                    if (method.getParameterCount() == 0) {
                        Class<?> returnType = method.getReturnType();
                        if (DataSource.class.equals(returnType) || EntityManager.class.equals(returnType)) {
                            ArrayList<Annotation> qualifiers = new ArrayList<>();
                            Annotation[] annos = method.getAnnotations();
                            for (Annotation anno : annos)
                                if (anno.annotationType().isAnnotationPresent(Qualifier.class))
                                    qualifiers.add(anno);
                            int numQualifiers = qualifiers.size();
                            if (numQualifiers > 0) {
                                annos = numQualifiers == annos.length ? annos : qualifiers.toArray(new Annotation[numQualifiers]);

                                if (DataSource.class.equals(returnType)) {
                                    Instance<DataSource> instance = CDI.current().select(DataSource.class, annos);
                                    DataSource resource = instance.get();

                                    isConfigDisplayId = true;
                                    isJNDIName = false;
                                    try {
                                        // force initialization by using the proxy
                                        resource.getLoginTimeout();

                                        // org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy.weld_getTargetInstance()
                                        Object wsJdbcDataSource = resource.getClass() //
                                                        .getDeclaredMethod("weld_getTargetInstance") //
                                                        .invoke(resource);

                                        // TODO would need to add getDisplayId if we want to try this approach,
                                        // but for now, we are blocked by weld_getTargetInstance returning null.
                                        // com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource.getDisplayId()
                                        dataStore = (String) wsJdbcDataSource.getClass() //
                                                        .getMethod("getDisplayId") //
                                                        .invoke(wsJdbcDataSource);
                                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException
                                                    | SQLException x) {
                                        // unexpected type of data source
                                        throw new UnsupportedOperationException //
                                        ("The " + resource.getClass() + " DataSource is not managed by the server." +
                                         " Use @DataSourceDefinition to configure a DataSource in the application " +
                                         " or configure a dataSource in the server configuration, and update the producer" +
                                         " to use @Resource. For example: @Produces @MyQualifier" +
                                         " @Resource(lookup = \"java:app/jdbc/MyDataSource\") DataSource dataSource;" +
                                         " The DataSource is used by the " + method.getName() + " resource accessor method of the " +
                                         method.getDeclaringClass().getName() + " repository.", x); // TODO NLS
                                    }

                                    if (emBuilder == null)
                                        emBuilder = new DBStoreEMBuilder(dataStore, isConfigDisplayId, isJNDIName, repositoryType, loader, provider);
                                    else
                                        throw new UnsupportedOperationException//
                                        ("The " + method.getName() + " resource accessor method of the " +
                                         method.getDeclaringClass().getName() + " repository should not be annotated with the " +
                                         qualifiers + " qualifier annotations because a repository is only permitted to have" +
                                         " one resource accessor method with qualifier annotations."); // TODO NLS
                                } else { // EntityManager/EntityManagerFactory
                                    Instance<EntityManagerFactory> instance = CDI.current().select(EntityManagerFactory.class, annos);
                                    EntityManagerFactory emf = instance.get();

                                    if (emBuilder == null)
                                        emBuilder = new PUnitEMBuilder(emf, loader);
                                    else
                                        throw new UnsupportedOperationException//
                                        ("The " + method.getName() + " resource accessor method of the " +
                                         method.getDeclaringClass().getName() + " repository should not be annotated with the " +
                                         qualifiers + " qualifier annotations because a repository is only permitted to have" +
                                         " one resource accessor method with qualifier annotations."); // TODO NLS

                                }
                            }
                        }
                    }
                }
            } else {
                isConfigDisplayId = false;
                isJNDIName = dataStore.startsWith("java:");

                if (isJNDIName) {
                    try {
                        Object resource = InitialContext.doLookup(dataStore);
                        if (resource instanceof EntityManagerFactory)
                            emBuilder = new PUnitEMBuilder((EntityManagerFactory) resource, dataStore, loader);

                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, dataStore + " is the JNDI name for " + resource);
                    } catch (NamingException x) {
                    }
                } else {
                    // Check for resource references and persistence unit references where java:comp/env/ is omitted:
                    String javaCompName = "java:comp/env/" + dataStore;
                    try {
                        Object resource = InitialContext.doLookup(javaCompName);

                        if (resource instanceof EntityManagerFactory)
                            emBuilder = new PUnitEMBuilder((EntityManagerFactory) resource, javaCompName, loader);

                        if (emBuilder != null || resource instanceof DataSource) {
                            isJNDIName = true;
                            dataStore = javaCompName;
                        }

                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, dataStore + " is the JNDI name for " + resource);
                    } catch (NamingException x) {
                    }
                }
            }

            if (emBuilder == null)
                emBuilder = new DBStoreEMBuilder(dataStore, isConfigDisplayId, isJNDIName, repositoryType, loader, provider);

            Class<?>[] primaryEntityClassReturnValue = new Class<?>[1];
            Map<Class<?>, List<QueryInfo>> queriesPerEntityClass = new HashMap<>();
            if (discoverEntityClasses(repositoryType, queriesPerEntityClass, primaryEntityClassReturnValue)) {
                EntityManagerBuilder previous = entityGroups.putIfAbsent(emBuilder, emBuilder);
                emBuilder = previous == null ? emBuilder : previous;

                for (Class<?> entityClass : queriesPerEntityClass.keySet())
                    emBuilder.add(entityClass);

                BeanAttributes<?> attrs = beanMgr.createBeanAttributes(repositoryType);
                Bean<?> bean = beanMgr.createBean(attrs, repositoryInterface, new RepositoryProducer.Factory<>( //
                                repositoryInterface, beanMgr, provider, this, //
                                emBuilder, primaryEntityClassReturnValue[0], queriesPerEntityClass));
                event.addBean(bean);
            }
        }

        for (EntityManagerBuilder builder : entityGroups.values()) {
            provider.executor.submit(builder);
        }

        for (EntityManagerBuilder builder : entityGroups.values()) {
            builder.populateStaticMetamodelClasses(staticMetamodels);
        }
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