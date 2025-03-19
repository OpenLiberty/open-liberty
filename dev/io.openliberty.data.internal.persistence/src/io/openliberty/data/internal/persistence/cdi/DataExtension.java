/*******************************************************************************
 * Copyright (c) 2022,2025 IBM Corporation and others.
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
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.data.internal.persistence.DataProvider;
import io.openliberty.data.internal.persistence.QueryInfo;
import io.openliberty.data.internal.persistence.Util;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.EntityExistsException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.data.repository.By;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import jakarta.data.spi.EntityDefining;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
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
public class DataExtension implements Extension {
    private static final TraceComponent tc = Tr.register(DataExtension.class);

    /**
     * JNDI name of the Jakarta EE Default DataSource.
     */
    static final String DEFAULT_DATA_SOURCE = "java:comp/DefaultDataSource";

    /**
     * Name of the built-in Jakarta Data provider.
     */
    public static final String PROVIDER_NAME = "Liberty";

    /**
     * Map of repository annotated type to Repository annotation.
     * Entries are removed as they are processed to allow for the CDI extension methods to be invoked again
     * for different applications or the same application being restarted.
     */
    private final ConcurrentHashMap<AnnotatedType<?>, Repository> repositoryAnnos = new ConcurrentHashMap<>();

    @Trivial
    public <T> void annotatedRepository(@Observes @WithAnnotations(Repository.class) ProcessAnnotatedType<T> event) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        AnnotatedType<T> type = event.getAnnotatedType();
        Repository repository = type.getAnnotation(Repository.class);

        String dataProvider = repository.provider();
        boolean provide = Repository.ANY_PROVIDER.equals(dataProvider) ||
                          PROVIDER_NAME.equalsIgnoreCase(dataProvider);

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "annotatedRepository to " + (provide ? "provide" : "ignore"),
                     repository.toString(), type.getJavaClass().getName());

        if (provide)
            repositoryAnnos.put(type, repository);
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager beanMgr) {
        // Obtain the service that informed CDI of this extension.
        BundleContext bundleContext = FrameworkUtil.getBundle(DataProvider.class).getBundleContext();
        ServiceReference<DataProvider> ref = bundleContext.getServiceReference(DataProvider.class);
        DataProvider provider = bundleContext.getService(ref);

        // Group entities by data access provider and class loader
        Map<FutureEMBuilder, FutureEMBuilder> entityGroups = new HashMap<>();

        for (Iterator<AnnotatedType<?>> it = repositoryAnnos.keySet().iterator(); it.hasNext();) {
            AnnotatedType<?> repositoryType = it.next();
            it.remove();

            Class<?> repositoryInterface = repositoryType.getJavaClass();
            ClassLoader loader = repositoryInterface.getClassLoader();

            Repository repository = repositoryType.getAnnotation(Repository.class);
            String dataStore = repository.dataStore();
            if (dataStore.length() == 0)
                dataStore = DEFAULT_DATA_SOURCE;
            // else
            // Determining whether it is JNDI name for a data source or
            // persistence unit requires attempting to look up the resource.
            // This needs to be done with the correct metadata on the thread,
            // but that might not be available yet.

            FutureEMBuilder futureEMBuilder = new FutureEMBuilder( //
                            provider, repositoryInterface, loader, dataStore);

            Class<?>[] primaryEntityClassReturnValue = new Class<?>[1];
            Map<Class<?>, List<QueryInfo>> queriesPerEntityClass = new HashMap<>();
            if (discoverEntityClasses(repositoryType,
                                      queriesPerEntityClass,
                                      primaryEntityClassReturnValue,
                                      provider)) {
                FutureEMBuilder previous = entityGroups.putIfAbsent(futureEMBuilder, futureEMBuilder);

                if (previous != null) {
                    futureEMBuilder = previous;
                    futureEMBuilder.addRepositoryInterface(repositoryInterface);
                }

                for (Class<?> entityClass : queriesPerEntityClass.keySet())
                    if (!QueryInfo.ENTITY_TBD.equals(entityClass))
                        futureEMBuilder.addEntity(entityClass);

                RepositoryProducer<Object> producer = new RepositoryProducer<>( //
                                repositoryInterface, beanMgr, provider, this, //
                                futureEMBuilder, primaryEntityClassReturnValue[0], queriesPerEntityClass);
                @SuppressWarnings("unchecked")
                Bean<Object> bean = beanMgr.createBean(producer, (Class<Object>) repositoryInterface, producer);
                event.addBean(bean);
            }
        }

        String appName = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor() //
                        .getComponentMetaData().getJ2EEName().getApplication();
        provider.onAppStarted(appName, entityGroups.values());
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
     * @param provider                      OSGi service that provides the CDI extension.
     * @return whether all entity types that appear on the repository interface are supported.
     */
    private boolean discoverEntityClasses(AnnotatedType<?> repositoryType,
                                          Map<Class<?>, List<QueryInfo>> queriesPerEntity,
                                          Class<?>[] primaryEntityClassReturnValue,
                                          DataProvider provider) {
        Class<?> repositoryInterface = repositoryType.getJavaClass();
        Class<?> primaryEntityClass = getPrimaryEntityType(repositoryInterface);
        Set<Class<?>> lifecycleMethodEntityClasses = new HashSet<>();
        List<QueryInfo> queriesWithQueryAnno = new ArrayList<>();
        ArrayList<QueryInfo> additionalQueriesForPrimaryEntity = new ArrayList<>();

        for (Method method : repositoryInterface.getMethods()) {
            if (method.isDefault()) // skip default methods
                continue;

            // Check for resource accessor methods:
            Class<?> returnType = method.getReturnType();
            if (method.getParameterCount() == 0 &&
                (EntityManager.class.equals(returnType)
                 || DataSource.class.equals(returnType)
                 || Connection.class.equals(returnType))) {
                QueryInfo queryInfo = new QueryInfo( //
                                repositoryInterface, //
                                method, //
                                QueryInfo.Type.RESOURCE_ACCESS);

                List<QueryInfo> queries = queriesPerEntity.get(Void.class);
                if (queries == null)
                    queriesPerEntity.put(Void.class, queries = new ArrayList<>());
                queries.add(queryInfo);
                continue;
            }

            Find find = method.getAnnotation(Find.class);
            Class<?> entityClass = find == null //
                            ? void.class //
                            : provider.compat.getEntityClass(find);
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
                    // The built-in repositories from the spec only allow generics
                    // for the Entity class and Key/Id class, and of these only uses
                    // the Entity class in return types, not the key.
                    // Custom repository interfaces are not allowed to use generics.
                    Class<?> arrayComponentType = entityClass == void.class //
                                    ? requirePrimaryEntity(primaryEntityClass,
                                                           repositoryInterface,
                                                           method) //
                                    : entityClass;
                    returnTypeAtDepth.add(arrayComponentType.arrayType());
                    if (returnArrayComponentType == null) {
                        returnArrayComponentType = arrayComponentType;
                        returnTypeAtDepth.add(returnArrayComponentType);
                        depth++;
                    }
                    type = null;
                } else {
                    Class<?> ec = entityClass == void.class //
                                    ? requirePrimaryEntity(primaryEntityClass,
                                                           repositoryInterface,
                                                           method) //
                                    : entityClass;
                    returnTypeAtDepth.add(ec);
                    type = null;
                }
            }

            if (entityClass == void.class)
                entityClass = returnTypeAtDepth.get(returnTypeAtDepth.size() - 1);

            Class<?> entityParamType = null;

            boolean hasQueryAnno = method.isAnnotationPresent(Query.class);

            // Determine entity class from a lifecycle method parameter:
            if (method.getParameterCount() == 1
                && !method.isDefault()
                && !hasQueryAnno
                && (method.isAnnotationPresent(Insert.class)
                    || method.isAnnotationPresent(Update.class)
                    || method.isAnnotationPresent(Save.class)
                    || method.isAnnotationPresent(Delete.class))) {
                Class<?> c = method.getParameterTypes()[0];
                if (Iterable.class.isAssignableFrom(c) || Stream.class.isAssignableFrom(c)) {
                    type = method.getGenericParameterTypes()[0];
                    if (type instanceof ParameterizedType) {
                        Type[] typeParams = ((ParameterizedType) type).getActualTypeArguments();
                        if (typeParams.length == 1 && typeParams[0] instanceof Class) // for example, List<Product>
                            c = (Class<?>) typeParams[0];
                        else { // could be a method like BasicRepository.saveAll(Iterable<S> entity)
                            entityParamType = c;
                            c = null;
                        }
                    } else {
                        c = null;
                    }
                } else if (c.isArray()) {
                    c = c.getComponentType();
                }
                if (Object.class.equals(c)) {
                    // Generic parameter like BasicRepository.save(S entity)
                    // or BasicRepository.deleteById(@By(ID) K id).
                    // The specification does not allow custom repositories to use
                    // generics, such as: @Delete customDeleteMethod(K key)
                    boolean isEntity = true;
                    for (Annotation anno : method.getParameterAnnotations()[0])
                        if (By.class.equals(anno.annotationType()))
                            isEntity = false;
                    if (isEntity)
                        entityParamType = c;
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

            List<QueryInfo> queries;

            // For efficiency, detect some obvious non-entity types.
            // Other non-entity types will be detected later.
            if (Util.cannotBeEntity(entityClass)) {
                queries = hasQueryAnno //
                                ? queriesWithQueryAnno //
                                : additionalQueriesForPrimaryEntity;
            } else {
                queries = queriesPerEntity.get(entityClass);
                if (queries == null)
                    queriesPerEntity.put(entityClass, queries = new ArrayList<>());
                if (hasQueryAnno)
                    queries = queriesWithQueryAnno;
            }

            queries.add(new QueryInfo( //
                            repositoryInterface, //
                            method, //
                            entityParamType, //
                            returnArrayComponentType, //
                            returnTypeAtDepth));
        }

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
                    for (QueryInfo queryInfo : entry.getValue())
                        additionalQueriesForPrimaryEntity.add(queryInfo);
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

            if (!additionalQueriesForPrimaryEntity.isEmpty()) {
                Method method = additionalQueriesForPrimaryEntity.get(0).method;
                Class<?> entityClass = requirePrimaryEntity(primaryEntityClass,
                                                            repositoryInterface,
                                                            method);
                List<QueryInfo> queries = queriesPerEntity.get(entityClass);
                if (queries == null)
                    queriesPerEntity.put(entityClass, additionalQueriesForPrimaryEntity);
                else
                    queries.addAll(additionalQueriesForPrimaryEntity);
            }

            if (!queriesWithQueryAnno.isEmpty())
                queriesPerEntity.put(Query.class, queriesWithQueryAnno);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, repositoryInterface.getName() + " has primary entity " + primaryEntityClass,
                     "and methods that use the following entities:", queriesPerEntity);

        primaryEntityClassReturnValue[0] = primaryEntityClass;
        return supportsAllEntities;
    }

    /**
     * Construct a RuntimeException or subclass and log the error unless the
     * error is known to be an error on the part of the application using a
     * repository method, such as supplying a null PageRequest.
     *
     * @param exceptionType RuntimeException or subclass, which must have a
     *                          constructor that accepts the message as a single
     *                          String argument.
     * @param messageId     NLS message ID.
     * @param args          message arguments.
     * @return RuntimeException or subclass.
     */
    @Trivial
    public final static <T extends RuntimeException> T exc(Class<T> exceptionType,
                                                           String messageId,
                                                           Object... args) {
        if (!exceptionType.equals(EmptyResultException.class) &&
            !exceptionType.equals(EntityExistsException.class) &&
            !exceptionType.equals(IllegalArgumentException.class) &&
            !exceptionType.equals(IllegalStateException.class) &&
            !exceptionType.equals(NoSuchElementException.class) &&
            !exceptionType.equals(NullPointerException.class) &&
            !exceptionType.equals(OptimisticLockingFailureException.class))
            Tr.error(tc, messageId, args);

        String message = Tr.formatMessage(tc, messageId, args);
        try {
            return exceptionType.getConstructor(String.class).newInstance(message);
        } catch (Exception x) {
            // should never occur
            throw new DataException(messageId + ' ' + Arrays.toString(args));
        }
    }

    /**
     * Look for parameterized type variable of the repository interface.
     * For example,
     *
     * public interface MyRepository extends DataRepository<MyEntity, IdType>
     *
     * @param repositoryInterface the interface that is annotated with Repository.
     * @return primary entity type if found. Otherwise null.
     */
    static Class<?> getPrimaryEntityType(Class<?> repositoryInterface) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        Class<?>[] interfaceClasses = repositoryInterface.getInterfaces();
        for (java.lang.reflect.Type interfaceType : repositoryInterface.getGenericInterfaces()) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, "checking " + interfaceType.getTypeName());

            if (interfaceType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) interfaceType;
                for (Class<?> interfaceClass : interfaceClasses) {
                    if (interfaceClass.equals(parameterizedType.getRawType())) {
                        if (DataRepository.class.isAssignableFrom(interfaceClass)) {
                            java.lang.reflect.Type typeParams[] = parameterizedType.getActualTypeArguments();
                            Type firstParamType = typeParams.length > 0 ? typeParams[0] : null;
                            if (firstParamType != null && firstParamType instanceof Class)
                                return (Class<?>) firstParamType;
                        }
                        break;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Require that a primary entity class is specified by the repository interface.
     *
     * @param primaryEntityClass  primary entity class or null.
     * @param repositoryInterface interface that is annotated with Repository.
     * @param method              repository method requiring a primary entity class
     * @return the primary entity class.
     * @throws MappingException with a helpful error message if no primary entity
     *                              class is specified by the repository.
     */
    @Trivial
    private static Class<?> requirePrimaryEntity(Class<?> primaryEntityClass,
                                                 Class<?> repositoryInterface,
                                                 Method method) {
        if (primaryEntityClass == null)
            throw exc(MappingException.class,
                      "CWWKD1001.no.primary.entity",
                      method.getName(),
                      repositoryInterface.getName(),
                      "DataRepository<EntityClass, EntityIdClass>");
        return primaryEntityClass;
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
            else if (annoClass.getSimpleName().endsWith("Entity") // also covers Jakarta NoSQL entity
                     || annoClass.isAnnotationPresent(EntityDefining.class))
                hasEntityAnnos = true;
        }

        boolean isSupported = !hasEntityAnnos;
        if (hasEntityAnnos) {
            Repository repository = repositoryType.getAnnotation(Repository.class);
            if (!Repository.ANY_PROVIDER.equals(repository.provider()))
                throw exc(DataException.class,
                          "CWWKD1045.unknown.entity.anno",
                          repositoryType.getJavaClass().getName(),
                          entityClass.getName(),
                          Arrays.toString(entityClassAnnos),
                          Entity.class.getName());
        }

        return isSupported;
    }
}