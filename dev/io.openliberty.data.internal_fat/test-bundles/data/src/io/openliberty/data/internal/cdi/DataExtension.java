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
import io.openliberty.data.Entity;
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

        Map<Entry<String, ClassLoader>, ArrayList<Entity>> entitiesMap = new HashMap<>();

        for (AnnotatedType<?> beanType : beanTypes) {
            Class<?> beanInterface = beanType.getJavaClass();
            ClassLoader loader = beanInterface.getClassLoader();

            Data data = beanType.getAnnotation(Data.class);
            String dataStore = data.value();

            Entry<String, ClassLoader> key = new SimpleImmutableEntry<>(dataStore, loader);
            ArrayList<Entity> entities = entitiesMap.get(key);
            if (entities == null)
                entitiesMap.put(key, entities = new ArrayList<>());

            Entity entity = beanType.getAnnotation(Entity.class);
            if (entity == null) {
                Class<?> entityClass = null;
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
                // TODO if still not found, look through @Select annotations that indicate an entity result class type?
                if (entityClass == null)
                    throw new IllegalArgumentException(beanInterface + " needs to specify @Entity to identify the entity class.");

                entity = Entity.Literal.of(entityClass, getID(entityClass));
            } else if (Entity.AUTO_DETECT_ID.equals(entity.id())) {
                Class<?> entityClass = entity.value();
                entity = Entity.Literal.of(entityClass, getID(entityClass));
            }
            entities.add(entity);

            BeanAttributes<?> attrs = beanMgr.createBeanAttributes(beanType);
            Bean<?> bean = beanMgr.createBean(attrs, beanInterface, new BeanProducerFactory<>(entity));
            beans.add(bean);
        }

        BundleContext bc = FrameworkUtil.getBundle(DataPersistence.class).getBundleContext();
        DataPersistence persistence = bc.getService(bc.getServiceReference(DataPersistence.class));

        for (Entry<Entry<String, ClassLoader>, ArrayList<Entity>> entry : entitiesMap.entrySet())
            try {
                String dataStore = entry.getKey().getKey();
                ClassLoader loader = entry.getKey().getValue();
                List<Entity> classes = entry.getValue();
                persistence.defineEntities(dataStore, loader, classes);
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
            String name = field.getName().toUpperCase();
            if ("ID".equals(name))
                return field.getName();
            else if (name.endsWith("ID"))
                if (upperID == null || name.compareTo(upperID) < 0) {
                    upperID = name;
                    id = field.getName();
                }
        }
        for (Method method : c.getMethods()) {
            if (method.getParameterCount() == 0) {
                String name = method.getName();
                if (name.startsWith("get")) {
                    name = name.substring(3).toUpperCase();
                    if ("ID".equals(name))
                        return method.getName().substring(3);
                    else if (name.endsWith("ID"))
                        if (upperID == null || name.compareTo(upperID) < 0) {
                            upperID = name;
                            id = method.getName().substring(3);
                        }
                }
            }
        }
        if (id == null)
            throw new IllegalArgumentException(c + " lacks public unique identifier field/method of the form *ID");
        return id;
    }
}