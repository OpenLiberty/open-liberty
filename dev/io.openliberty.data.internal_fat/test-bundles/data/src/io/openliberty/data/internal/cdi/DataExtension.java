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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import io.openliberty.data.Id;
import io.openliberty.data.internal.DataPersistence;

public class DataExtension implements Extension {
    private final ArrayList<Bean<?>> beans = new ArrayList<>();
    private final HashSet<AnnotatedType<?>> beanTypes = new HashSet<>();

    public <T> void processAnnotatedType(@Observes @WithAnnotations(Data.class) ProcessAnnotatedType<T> event) {
        System.out.println("processAnnotatedType");

        AnnotatedType<T> type = event.getAnnotatedType();
        System.out.println("    found " + type.getAnnotation(Data.class) + " on " + type.getJavaClass());
        beanTypes.add(type);
    }

    public void afterTypeDiscovery(@Observes AfterTypeDiscovery event, BeanManager beanMgr) {
        System.out.println("afterTypeDiscovery");

        Map<Entry<String, ClassLoader>, Map<Class<?>, String>> entitiesMap = new HashMap<>();

        for (AnnotatedType<?> beanType : beanTypes) {
            Class<?> beanInterface = beanType.getJavaClass();
            ClassLoader loader = beanInterface.getClassLoader();

            Data data = beanType.getAnnotation(Data.class);
            String dataStore = data.provider();

            Entry<String, ClassLoader> key = new SimpleImmutableEntry<>(dataStore, loader);
            Map<Class<?>, String> entities = entitiesMap.get(key);
            if (entities == null)
                entitiesMap.put(key, entities = new HashMap<>());

            Class<?> entityClass = data.value();
            if (void.class.equals(entityClass)) {
                entityClass = null;
                // infer from single-parameter methods that accept an entity class
                for (Method method : beanInterface.getMethods())
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
                    throw new IllegalArgumentException(beanInterface + " @Data annotation needs to specify the entity class.");
            }
            String keyAttribute = getID(entityClass);
            entities.put(entityClass, keyAttribute);

            BeanAttributes<?> attrs = beanMgr.createBeanAttributes(beanType);
            Bean<?> bean = beanMgr.createBean(attrs, beanInterface, new BeanProducerFactory<>(entityClass, keyAttribute));
            beans.add(bean);
        }

        BundleContext bc = FrameworkUtil.getBundle(DataPersistence.class).getBundleContext();
        DataPersistence persistence = bc.getService(bc.getServiceReference(DataPersistence.class));

        for (Entry<Entry<String, ClassLoader>, Map<Class<?>, String>> entry : entitiesMap.entrySet())
            try {
                String dataStore = entry.getKey().getKey();
                ClassLoader loader = entry.getKey().getValue();
                Map<Class<?>, String> entityInfo = entry.getValue();
                persistence.defineEntities(dataStore, loader, entityInfo);
            } catch (Exception x) {
                x.printStackTrace();
                System.err.println("ERROR: Unable to define entities for " + entry.getKey().getKey() + ": " + entry.getValue());
            }
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager beanMgr) {
        System.out.println("afterBeanDiscovery");

        for (Bean<?> bean : beans) {
            System.out.println("    adding " + bean);
            event.addBean(bean);
        }
    }

    private static String getID(Class<?> c) {
        // For now, choosing "id" or any field that ends with id
        String id = null;
        String upperID = null;
        for (Field field : c.getFields()) {
            if (field.getAnnotation(Id.class) != null)
                return field.getName();

            String name = field.getName().toUpperCase();
            if ("ID".equals(name))
                id = field.getName();
            else if ((id == null || id.length() != 2) && name.endsWith("ID"))
                if (upperID == null || name.compareTo(upperID) < 0) {
                    upperID = name;
                    id = field.getName();
                }
        }

        if (id == null)
            throw new IllegalArgumentException(c + " lacks public field with @Id or of the form *ID");
        return id;
    }
}