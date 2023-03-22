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
package io.openliberty.data.internal.cdi;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.LibertyDataProvider;
import jakarta.data.Entities;
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

public class DataExtension implements Extension, PrivilegedAction<DataExtensionMetadata> {
    private static final TraceComponent tc = Tr.register(DataExtension.class);

    private final ArrayList<Entities> entitiesListsForTemplate = new ArrayList<>();

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
        private final LibertyDataProvider provider;

        EntityGroupKey(String databaseId, ClassLoader loader, LibertyDataProvider provider) {
            this.loader = loader;
            this.databaseId = databaseId;
            this.provider = provider;
            hash = loader.hashCode() + databaseId.hashCode() + provider.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            EntityGroupKey k;
            return o instanceof EntityGroupKey
                   && databaseId.equals((k = (EntityGroupKey) o).databaseId)
                   && loader.equals(k.loader)
                   && provider.equals(k.provider);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    @Trivial
    public <T> void annotatedEntitiesForTemplate(@Observes @WithAnnotations(Entities.class) ProcessAnnotatedType<T> event) {
        AnnotatedType<T> type = event.getAnnotatedType();
        entitiesListsForTemplate.add(type.getAnnotation(Entities.class));

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "annotatedEntitiesForTemplate", type.getAnnotation(Entities.class).toString(), type.getJavaClass().getName());
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
        DataExtensionMetadata svc = AccessController.doPrivileged(this);

        // Group entities by data access provider and class loader
        Map<EntityGroupKey, List<Class<?>>> entityGroups = new HashMap<>();

        for (AnnotatedType<?> repositoryType : repositoryTypes) {
            Class<?> repositoryInterface = repositoryType.getJavaClass();
            Class<?> entityClass = getEntityClass(repositoryInterface);
            ClassLoader loader = repositoryInterface.getClassLoader();
            LibertyDataProvider provider = svc.getProvider(entityClass, repositoryType);

            if (provider != null) {
                EntityGroupKey entityGroupKey = new EntityGroupKey("defaultDatabaseStore", loader, provider); // TODO configuration of different providers in Jakarta Data
                List<Class<?>> entityClasses = entityGroups.get(entityGroupKey);
                if (entityClasses == null)
                    entityGroups.put(entityGroupKey, entityClasses = new ArrayList<>());

                entityClasses.add(entityClass);

                BeanAttributes<?> attrs = beanMgr.createBeanAttributes(repositoryType);
                Bean<?> bean = beanMgr.createBean(attrs, repositoryInterface, new RepositoryProducer.Factory<>(beanMgr, provider, entityClass));
                repositoryBeans.add(bean);
            }
        }

        for (Entities anno : entitiesListsForTemplate) {
            for (Class<?> entityClass : anno.value()) {
                ClassLoader loader = entityClass.getClassLoader();
                LibertyDataProvider provider = svc.getProvider(entityClass, null);

                if (provider != null) {
                    EntityGroupKey entityGroupKey = new EntityGroupKey("defaultDatabaseStore", loader, provider); // TODO temporarily hard coded
                    List<Class<?>> entityClasses = entityGroups.get(entityGroupKey);
                    if (entityClasses == null)
                        entityGroups.put(entityGroupKey, entityClasses = new ArrayList<>());

                    entityClasses.add(entityClass);
                }
            }
        }

        for (Entry<EntityGroupKey, List<Class<?>>> entry : entityGroups.entrySet()) {
            EntityGroupKey entityGroupKey = entry.getKey();
            List<Class<?>> entityClasses = entry.getValue();
            try {
                entityGroupKey.provider.entitiesFound(entityGroupKey.databaseId, entityGroupKey.loader, entityClasses);
            } catch (Exception x) {
                x.printStackTrace();
                System.err.println("ERROR: Unable to provide entities for " + entityGroupKey.databaseId + ": " + entityClasses);
            }
        }
        // TODO copy remaining code for this method
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
    public DataExtensionMetadata run() {
        BundleContext bundleContext = FrameworkUtil.getBundle(DataExtensionMetadata.class).getBundleContext();
        ServiceReference<DataExtensionMetadata> ref = bundleContext.getServiceReference(DataExtensionMetadata.class);
        return bundleContext.getService(ref);
    }
}