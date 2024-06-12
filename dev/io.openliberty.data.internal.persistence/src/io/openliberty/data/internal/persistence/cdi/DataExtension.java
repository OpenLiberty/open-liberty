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
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.data.internal.persistence.QueryInfo;
import io.openliberty.data.internal.persistence.provider.PUnitEMBuilder;
import jakarta.data.exceptions.MappingException;
import jakarta.data.repository.By;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import jakarta.data.spi.EntityDefining;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
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
public class DataExtension implements Extension {
    private static final TraceComponent tc = Tr.register(DataExtension.class);

    /**
     * Id of the default databaseStore configuration element.
     */
    static final String DEFAULT_DATA_STORE = "defaultDatabaseStore";

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
        boolean provide = Repository.ANY_PROVIDER.equals(dataProvider) || "OpenLiberty".equalsIgnoreCase(dataProvider); // TODO provider name

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "annotatedRepository to " + (provide ? "provide" : "ignore"),
                     repository.toString(), type.getJavaClass().getName());

        if (provide)
            repositoryAnnos.put(type, repository);
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager beanMgr) {
        // Obtain the service that informed CDI of this extension.
        BundleContext bundleContext = FrameworkUtil.getBundle(DataExtensionProvider.class).getBundleContext();
        ServiceReference<DataExtensionProvider> ref = bundleContext.getServiceReference(DataExtensionProvider.class);
        DataExtensionProvider provider = bundleContext.getService(ref);

        // Group entities by data access provider and class loader
        Map<FutureEMBuilder, FutureEMBuilder> entityGroups = new HashMap<>();

        for (Iterator<AnnotatedType<?>> it = repositoryAnnos.keySet().iterator(); it.hasNext();) {
            AnnotatedType<?> repositoryType = it.next();
            it.remove();

            Class<?> repositoryInterface = repositoryType.getJavaClass();
            ClassLoader loader = repositoryInterface.getClassLoader();
            Map.Entry<String, String[]> metadataInfo = getMetadata(loader, provider);
            String metadataIdentifier = metadataInfo.getKey();
            String[] appModComp = metadataInfo.getValue();

            Repository repository = repositoryType.getAnnotation(Repository.class);
            String dataStore = repository.dataStore();
            EntityManagerFactory emf = null; // TODO remove along with the following TODO
            if (dataStore.length() == 0) {
                dataStore = DEFAULT_DATA_STORE;

                // Look for resource accessor method with qualifiers
                // TODO if we keep this code, make it more efficient/stable. Identification of resource accessor methods
                // is also done by discoverEntityClasses.
                for (Method method : repositoryInterface.getMethods()) {
                    if (method.getParameterCount() == 0) {
                        Class<?> returnType = method.getReturnType();
                        if (EntityManager.class.equals(returnType)) {
                            ArrayList<Annotation> qualifiers = new ArrayList<>();
                            Annotation[] annos = method.getAnnotations();
                            for (Annotation anno : annos)
                                if (anno.annotationType().isAnnotationPresent(Qualifier.class))
                                    qualifiers.add(anno);
                            int numQualifiers = qualifiers.size();
                            if (numQualifiers > 0) {
                                annos = numQualifiers == annos.length ? annos : qualifiers.toArray(new Annotation[numQualifiers]);

                                // EntityManager/EntityManagerFactory
                                Instance<EntityManagerFactory> instance = CDI.current().select(EntityManagerFactory.class, annos);

                                if (emf == null)
                                    emf = instance.get();
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
            // else
            // Determining whether it is JNDI name for a data source or
            // persistence unit requires attempting to look up the resource.
            // This needs to be done with the correct metadata on the thread,
            // but that might not be available yet.

            FutureEMBuilder futureEMBuilder = new FutureEMBuilder(provider, loader, dataStore, metadataIdentifier, appModComp);

            Class<?>[] primaryEntityClassReturnValue = new Class<?>[1];
            Map<Class<?>, List<QueryInfo>> queriesPerEntityClass = new HashMap<>();
            if (discoverEntityClasses(repositoryType, queriesPerEntityClass, primaryEntityClassReturnValue)) {
                FutureEMBuilder previous = entityGroups.putIfAbsent(futureEMBuilder, futureEMBuilder);
                futureEMBuilder = previous == null ? futureEMBuilder : previous;

                for (Class<?> entityClass : queriesPerEntityClass.keySet())
                    if (!Query.class.equals(entityClass))
                        futureEMBuilder.add(entityClass);

                RepositoryProducer<Object> producer = new RepositoryProducer<>( //
                                repositoryInterface, beanMgr, provider, this, //
                                futureEMBuilder, primaryEntityClassReturnValue[0], queriesPerEntityClass);
                @SuppressWarnings("unchecked")
                Bean<Object> bean = beanMgr.createBean(producer, (Class<Object>) repositoryInterface, producer);
                event.addBean(bean);
            }

            // TODO These is not properly placed here, but the whole section should be removed along with earlier TODOs
            try {
                if (emf != null)
                    futureEMBuilder.complete(new PUnitEMBuilder(provider, loader, emf, futureEMBuilder.entityTypes));
            } catch (Exception x) {
                futureEMBuilder.completeExceptionally(x);
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
     * @return whether all entity types that appear on the repository interface are supported.
     */
    private boolean discoverEntityClasses(AnnotatedType<?> repositoryType,
                                          Map<Class<?>, List<QueryInfo>> queriesPerEntity,
                                          Class<?>[] primaryEntityClassReturnValue) {
        Class<?> repositoryInterface = repositoryType.getJavaClass();
        Class<?> primaryEntityClass = null;
        Set<Class<?>> lifecycleMethodEntityClasses = new HashSet<>();
        List<QueryInfo> queriesWithQueryAnno = new ArrayList<>();
        List<QueryInfo> additionalQueriesForPrimaryEntity = new ArrayList<>();

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
                if (Object.class.equals(c)) {
                    // generic parameter like BasicRepository.save(S entity) or BasicRepository.deleteById(@By(ID) K id)
                    boolean isEntity = true;
                    for (Annotation anno : method.getParameterAnnotations()[0])
                        if (By.class.equals(anno.annotationType()))
                            isEntity = false;
                    if (isEntity)
                        entityParamType = c;
                    // TODO is there any way to distinguish @Delete deleteById(K key) when @By is not present?
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

            if (entityClass == null) {
                queries = hasQueryAnno ? queriesWithQueryAnno : additionalQueriesForPrimaryEntity;
            } else {
                // TODO find better ways of determining non-entities ******** require @Entity unless found on lifecycle method!!!!!!
                String packageName = entityClass.getPackageName();
                if (packageName.startsWith("java.")
                    || packageName.startsWith("jakarta.")
                    || entityClass.isPrimitive()
                    || entityClass.isInterface()) {
                    queries = hasQueryAnno ? queriesWithQueryAnno : additionalQueriesForPrimaryEntity;
                } else {
                    queries = queriesPerEntity.get(entityClass);
                    if (queries == null)
                        queriesPerEntity.put(entityClass, queries = new ArrayList<>());
                    if (hasQueryAnno)
                        queries = queriesWithQueryAnno;
                }
            }

            queries.add(new QueryInfo(method, entityParamType, returnArrayComponentType, returnTypeAtDepth));
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

            if (!additionalQueriesForPrimaryEntity.isEmpty())
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

            if (!queriesWithQueryAnno.isEmpty())
                queriesPerEntity.put(Query.class, queriesWithQueryAnno);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, repositoryInterface.getName() + " has primary entity class " + primaryEntityClass,
                     "and methods that use the following entities:", queriesPerEntity);

        primaryEntityClassReturnValue[0] = primaryEntityClass;
        return supportsAllEntities;
    }

    /**
     * Obtains the metadata identifier and application/module/component based on
     * the class loader identifier of the repository's class loader.
     *
     * @param repositoryClassLoader class loader of the repository interface.
     * @param provider              OSGi service that provides the CDI extension.
     * @return metadata identifier as the key, and application/module/component
     *         as the value. Module and component might be null or might not be
     *         present at all.
     */
    private Map.Entry<String, String[]> getMetadata(ClassLoader repositoryClassLoader, DataExtensionProvider provider) {
        String mdIdentifier;
        String clIdentifier = provider.classloaderIdSvc.getClassLoaderIdentifier(repositoryClassLoader);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "class loader identifier: " + clIdentifier);

        int sep = clIdentifier.indexOf(':');
        String[] parts = sep < 0 ? new String[1] : clIdentifier.substring(sep + 1).split("#");
        if (parts.length < 2 || parts[1] == null) { // no module
            //  component metadata based on the application metadata
            ComponentMetaData cdata = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            MetaData adata = cdata == null ? null : cdata.getModuleMetaData().getApplicationMetaData();
            cdata = provider.createComponentMetadata(adata, repositoryClassLoader);
            J2EEName jeeName = cdata.getJ2EEName();
            mdIdentifier = provider.getMetaDataIdentifier(parts[0] = jeeName.getApplication(),
                                                          null,
                                                          null);
        } else {
            // convert classloader identifier to metadata identifier
            mdIdentifier = provider.metadataIdSvc.getMetaDataIdentifier(clIdentifier.startsWith("WebModule:") ? "WEB" : "EJB",
                                                                        parts[0], // application
                                                                        parts[1], // module
                                                                        parts.length < 3 ? null : parts[2]); // component
        }

        return new AbstractMap.SimpleImmutableEntry<>(mdIdentifier, parts);
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
                throw new MappingException("Open Liberty's built-in Jakarta Data provider cannot provide the " +
                                           repositoryType.getJavaClass().getName() + " repository because the repository's " +
                                           entityClass.getName() + " entity class includes an unrecognized entity annotation. " +
                                           " The following annotations are found on the entity class: " + Arrays.toString(entityClassAnnos) +
                                           ". Supported entity annotations are: " + Entity.class.getName() + "."); // TODO NLS
        }

        return isSupported;
    }
}