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

        Map<String, Map<ClassLoader, List<Class<?>>>> classesPerLoaderPerDataStore = new HashMap<>();

        for (AnnotatedType<?> beanType : beanTypes) {
            Class<?> beanInterface = beanType.getJavaClass();
            ClassLoader loader = beanInterface.getClassLoader();
            Data data = beanType.getAnnotation(Data.class);
            String dataStore = data.dataStore();

            BeanAttributes<?> attrs = beanMgr.createBeanAttributes(beanType);
            beans.add(beanMgr.createBean(attrs, beanInterface, new BeanProducerFactory<>()));

            Map<ClassLoader, List<Class<?>>> classesPerLoader = classesPerLoaderPerDataStore.get(dataStore);
            if (classesPerLoader == null)
                classesPerLoaderPerDataStore.put(dataStore, classesPerLoader = new HashMap<>());
            List<Class<?>> classes = classesPerLoader.get(loader);
            if (classes == null)
                classesPerLoader.put(loader, classes = new ArrayList<>());
            for (Class<?> entityClass : data.value())
                classes.add(entityClass);
        }

        BundleContext bc = FrameworkUtil.getBundle(DataPersistence.class).getBundleContext();
        DataPersistence persistence = bc.getService(bc.getServiceReference(DataPersistence.class));

        for (Entry<String, Map<ClassLoader, List<Class<?>>>> dsEntry : classesPerLoaderPerDataStore.entrySet())
            for (Entry<ClassLoader, List<Class<?>>> clEntry : dsEntry.getValue().entrySet())
                try {
                    String dataStore = dsEntry.getKey();
                    ClassLoader loader = clEntry.getKey();
                    List<Class<?>> classes = clEntry.getValue();
                    persistence.defineEntities(dataStore, loader, classes);
                } catch (Exception x) {
                    x.printStackTrace();
                    System.err.println("ERROR: Unable to define entities for " + dsEntry.getKey() + ": " + clEntry.getValue());
                }
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager beanMgr) {
        System.out.println("afterBeanDiscovery");

        for (Bean<?> bean : beans) {
            System.out.println("    adding " + bean);
            event.addBean(bean);
        }
    }
}