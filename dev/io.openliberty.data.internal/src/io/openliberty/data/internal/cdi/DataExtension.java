/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

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
        repositoryTypes.add(type);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "annotatedRepository", type.getAnnotation(Repository.class).toString(), type.getJavaClass().getName());
    }

    public void afterTypeDiscovery(@Observes AfterTypeDiscovery event, BeanManager beanMgr) {
        DataExtensionMetadata svc = AccessController.doPrivileged(this);

        // Group entities by data access provider and class loader
        Map<EntityGroupKey, List<Class<?>>> entityGroups = new HashMap<>();

        for (AnnotatedType<?> repositoryType : repositoryTypes) {
            Class<?> repositoryInterface = repositoryType.getJavaClass();
            ClassLoader loader = repositoryInterface.getClassLoader();

            EntityGroupKey entityGroupKey = new EntityGroupKey("DefaultDataStore", loader); // TODO configuration of different providers in Jakarta Data
            List<Class<?>> entityClasses = entityGroups.get(entityGroupKey);
            if (entityClasses == null)
                entityGroups.put(entityGroupKey, entityClasses = new ArrayList<>());

            Class<?> entityClass = getEntityClass(repositoryInterface);
            entityClasses.add(entityClass);

            BeanAttributes<?> attrs = beanMgr.createBeanAttributes(repositoryType);
            Bean<?> bean = beanMgr.createBean(attrs, repositoryInterface, new RepositoryProducer.Factory<>(beanMgr, svc, entityClass));
            repositoryBeans.add(bean);
        }

        for (Entities anno : entitiesListsForTemplate) {
            for (Class<?> entityClass : anno.value()) {
                ClassLoader loader = entityClass.getClassLoader();
                EntityGroupKey entityGroupKey = new EntityGroupKey(anno.provider(), loader);
                List<Class<?>> entityClasses = entityGroups.get(entityGroupKey);
                if (entityClasses == null)
                    entityGroups.put(entityGroupKey, entityClasses = new ArrayList<>());

                entityClasses.add(entityClass);
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
     * @return
     */
    private Class<?> getEntityClass(Class<?> repositoryInterface) {
        Class<?> entityClass = null;

        for (Type interfaceType : repositoryInterface.getGenericInterfaces()) {
            if (interfaceType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) interfaceType;
                if (parameterizedType.getRawType().getTypeName().startsWith(DataRepository.class.getPackageName())) {
                    Type paramTypes[] = parameterizedType.getActualTypeArguments();
                    if (paramTypes.length == 2 && paramTypes[0] instanceof Class) {
                        entityClass = (Class<?>) paramTypes[0];
                    }
                }
            }
        }

        if (entityClass == null) {
            for (Method method : repositoryInterface.getMethods())
                if (method.getParameterCount() == 1 && "save".equals(method.getName())) {
                    Class<?> paramType = method.getParameterTypes()[0];
                    if (paramType.isArray())
                        paramType = paramType.getComponentType();
                    String packageName = paramType.getPackageName();
                    if (!paramType.isPrimitive() &&
                        !paramType.isInterface() &&
                        !packageName.startsWith("java") &&
                        !packageName.startsWith("jakarta")) {
                        entityClass = paramType;
                        break;
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