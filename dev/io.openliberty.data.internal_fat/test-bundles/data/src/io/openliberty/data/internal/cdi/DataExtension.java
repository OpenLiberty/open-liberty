/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import io.openliberty.data.Data;
import io.openliberty.data.Entities;
import io.openliberty.data.internal.DataPersistence;

public class DataExtension implements Extension {
    private final ArrayList<Entities> entitiesList = new ArrayList<>();
    private final ArrayList<Bean<?>> repositoryBeans = new ArrayList<>();
    private final HashSet<AnnotatedType<?>> repositoryTypes = new HashSet<>();

    public <T> void processAnnotatedTypeWithData(@Observes @WithAnnotations(Data.class) ProcessAnnotatedType<T> event) {
        System.out.println("processAnnotatedTypeWithData");

        AnnotatedType<T> type = event.getAnnotatedType();
        System.out.println("    found " + type.getAnnotation(Data.class) + " on " + type.getJavaClass());
        repositoryTypes.add(type);
    }

    public <T> void processAnnotatedTypeWithEntities(@Observes @WithAnnotations(Entities.class) ProcessAnnotatedType<T> event) {
        System.out.println("processAnnotatedTypeWithEntities");

        AnnotatedType<T> type = event.getAnnotatedType();
        System.out.println("    found " + type.getAnnotation(Entities.class) + " on " + type.getJavaClass());

        entitiesList.add(type.getAnnotation(Entities.class));
    }

    public void afterTypeDiscovery(@Observes AfterTypeDiscovery event, BeanManager beanMgr) {
        System.out.println("afterTypeDiscovery");

        Map<EntityGroupKey, List<Class<?>>> entitiesMap = new HashMap<>();

        for (AnnotatedType<?> repositoryType : repositoryTypes) {
            Class<?> repositoryInterface = repositoryType.getJavaClass();
            ClassLoader loader = repositoryInterface.getClassLoader();

            Data data = repositoryType.getAnnotation(Data.class);

            EntityGroupKey entityGroupKey = new EntityGroupKey(data.provider(), loader);
            List<Class<?>> entityClasses = entitiesMap.get(entityGroupKey);
            if (entityClasses == null)
                entitiesMap.put(entityGroupKey, entityClasses = new ArrayList<>());

            Class<?> entityClass = data.value();
            if (void.class.equals(entityClass)) {
                entityClass = null;
                // infer from single-parameter methods that accept an entity class
                for (Method method : repositoryInterface.getMethods())
                    if (method.getParameterCount() == 1) {
                        // TODO there should be better ways to determine entity classes, but this is close enough for experimentation,
                        Class<?> paramType = method.getParameterTypes()[0];
                        String packageName = paramType.getPackageName();
                        if (!paramType.isPrimitive() &&
                            !paramType.isInterface() &&
                            !paramType.isArray() &&
                            !packageName.startsWith("java") &&
                            !packageName.startsWith("jakarta")) {
                            if ("save".equals(method.getName())) {
                                entityClass = paramType;
                                break;
                            } else if (entityClass == null || paramType.getName().compareTo(entityClass.getName()) < 0) {
                                entityClass = paramType;
                            }
                        }
                    }
                // TODO if still not found, look through @Query/@Select annotations that indicate an entity result class type?
                if (entityClass == null)
                    throw new IllegalArgumentException(repositoryInterface + " @Data annotation needs to specify the entity class.");
            }
            entityClasses.add(entityClass);

            BeanAttributes<?> attrs = beanMgr.createBeanAttributes(repositoryType);
            Bean<?> bean = beanMgr.createBean(attrs, repositoryInterface, new RepositoryProducerFactory<>(beanMgr, entityClass));
            repositoryBeans.add(bean);
        }

        for (Entities anno : entitiesList) {
            for (Class<?> entityClass : anno.value()) {
                ClassLoader loader = entityClass.getClassLoader();
                EntityGroupKey entityGroupKey = new EntityGroupKey(anno.provider(), loader);
                List<Class<?>> entityClasses = entitiesMap.get(entityGroupKey);
                if (entityClasses == null)
                    entitiesMap.put(entityGroupKey, entityClasses = new ArrayList<>());

                entityClasses.add(entityClass);
            }
        }

        BundleContext bc = FrameworkUtil.getBundle(DataPersistence.class).getBundleContext();
        DataPersistence persistence = bc.getService(bc.getServiceReference(DataPersistence.class));

        for (Entry<EntityGroupKey, List<Class<?>>> entry : entitiesMap.entrySet()) {
            EntityGroupKey entityGroupKey = entry.getKey();
            List<Class<?>> entityClasses = entry.getValue();
            try {
                persistence.defineEntities(entityGroupKey.provider, entityGroupKey.loader, entityClasses);
            } catch (Exception x) {
                x.printStackTrace();
                System.err.println("ERROR: Unable to define entities for " + entityGroupKey.provider + ": " + entityClasses);
            }
        }
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager beanMgr) {
        System.out.println("afterBeanDiscovery");

        for (Bean<?> bean : repositoryBeans) {
            System.out.println("    adding " + bean);
            event.addBean(bean);
        }
    }
}