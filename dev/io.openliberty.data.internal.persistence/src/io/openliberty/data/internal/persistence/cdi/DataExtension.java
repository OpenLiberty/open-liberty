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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.persistence.EntityDefiner;
import jakarta.data.exceptions.MappingException;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Repository;
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

public class DataExtension implements Extension, PrivilegedAction<DataExtensionProvider> {
    private static final TraceComponent tc = Tr.register(DataExtension.class);

    private final ArrayList<Bean<?>> repositoryBeans = new ArrayList<>();

    private final HashSet<AnnotatedType<?>> repositoryTypes = new HashSet<>();

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
        if (Repository.ANY_PROVIDER.equals(provider) || "OpenLiberty".equalsIgnoreCase(provider)) // TODO provider name
            repositoryTypes.add(type);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "annotatedRepository", repository.toString(), type.getJavaClass().getName());
    }

    public void afterTypeDiscovery(@Observes AfterTypeDiscovery event, BeanManager beanMgr) {
        DataExtensionProvider provider = AccessController.doPrivileged(this);

        // Group entities by data access provider and class loader
        Map<EntityGroupKey, EntityDefiner> entityGroups = new HashMap<>();

        for (AnnotatedType<?> repositoryType : repositoryTypes) {
            Class<?> repositoryInterface = repositoryType.getJavaClass();
            Class<?> entityClass = getEntityClass(repositoryInterface);
            ClassLoader loader = repositoryInterface.getClassLoader();

            if (supportsEntity(entityClass, repositoryType)) {
                EntityGroupKey entityGroupKey = new EntityGroupKey("defaultDatabaseStore", loader);
                EntityDefiner entityDefiner = entityGroups.get(entityGroupKey);
                if (entityDefiner == null)
                    entityGroups.put(entityGroupKey, entityDefiner = new EntityDefiner(entityGroupKey.databaseId, loader));

                entityDefiner.add(entityClass);

                BeanAttributes<?> attrs = beanMgr.createBeanAttributes(repositoryType);
                Bean<?> bean = beanMgr.createBean(attrs, repositoryInterface, new RepositoryProducer.Factory<>(beanMgr, provider, entityDefiner, entityClass));
                repositoryBeans.add(bean);
            }
        }

        for (EntityDefiner entityDefiner : entityGroups.values()) {
            provider.executor.submit(entityDefiner);
        }
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager beanMgr) {
        for (Bean<?> bean : repositoryBeans) {
            event.addBean(bean);
        }
    }

    /**
     * Determine the entity class for the interface that is annotated as a Repository.
     *
     * Many repository interfaces will inherit from DataRepository or another built-in repository class,
     * all of which are parameterized with the entity class as the first parameter.
     *
     * Otherwise, it might be possible to infer the entity class from the method signature.
     * The following approach is used:
     * <ul>
     * <li>Look for a save(E) or save(E[]) method
     * <li>TODO Look at the return value of other methods
     * </ul>
     *
     * @param repositoryInterface
     * @return entity class for the repository.
     */
    private static Class<?> getEntityClass(Class<?> repositoryInterface) {
        Class<?> entityClass = null;

        for (Type interfaceType : repositoryInterface.getGenericInterfaces()) {
            if (interfaceType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) interfaceType;
                if (parameterizedType.getRawType().getTypeName().startsWith(DataRepository.class.getPackageName())) {
                    Type typeParams[] = parameterizedType.getActualTypeArguments();
                    if (typeParams.length == 2 && typeParams[0] instanceof Class) {
                        entityClass = (Class<?>) typeParams[0];
                        break;
                    }
                }
            }
        }

        if (entityClass == null) {
            for (Method method : repositoryInterface.getMethods())
                if (method.getParameterCount() == 1 && "save".equals(method.getName())) {
                    Type type = method.getGenericParameterTypes()[0];
                    if (type instanceof ParameterizedType) {
                        Type[] typeParams = ((ParameterizedType) type).getActualTypeArguments();
                        if (typeParams.length == 1) // for example, List<Product> vs. Map<Long, String>
                            type = typeParams[0];
                    }
                    if (type instanceof Class) {
                        Class<?> paramClass = (Class<?>) type;
                        if (paramClass.isArray())
                            paramClass = paramClass.getComponentType();
                        String packageName = paramClass.getPackageName();
                        if (!paramClass.isPrimitive() &&
                            !paramClass.isInterface() &&
                            !packageName.startsWith("java") &&
                            !packageName.startsWith("jakarta")) {
                            entityClass = paramClass;
                            break;
                        }
                    }
                }

            // TODO if still not found, look through @Query/@Select annotations that indicate an entity result class type?
            if (entityClass == null)
                throw new IllegalArgumentException("@Repository " + repositoryInterface.getName() + " does not specify an entity class." + // TODO NLS
                                                   ". To correct this, have the repository interface extend DataRepository " +
                                                   " or another built-in repository interface and supply the entity class as the first parameter.");
        }

        return entityClass;
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