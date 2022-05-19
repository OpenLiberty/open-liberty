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
package io.openliberty.data.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.persistence.Entity;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.persistence.DatabaseStore;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = DataPersistence.class)
public class DataPersistence {
    private static final String EOLN = String.format("%n");

    private static final ConcurrentHashMap<//
                    Entry<String, ClassLoader>, //
                    Entry<PersistenceServiceUnit, List<Class<?>>>> units = new ConcurrentHashMap<>();

    @Reference
    protected LocalTransactionCurrent localTranCurrent;

    @Reference
    protected EmbeddableWebSphereTransactionManager tranMgr;

    @Activate
    protected void activate(ComponentContext context) {
        System.out.println("Activate");
    }

    /**
     * Define entity classes.
     *
     * @param dbStoreId id of databaseStore config element
     * @param loader    class loader
     * @param classList entity classes
     * @throws Exception
     */
    public void defineEntities(String dbStoreId, ClassLoader loader, List<Class<?>> classList) throws Exception {
        BundleContext bc = FrameworkUtil.getBundle(DataPersistence.class).getBundleContext();
        Collection<ServiceReference<DatabaseStore>> refs = bc.getServiceReferences(DatabaseStore.class,
                                                                                   FilterUtils.createPropertyFilter("id", dbStoreId));
        if (refs.isEmpty())
            throw new IllegalArgumentException("Not found: " + dbStoreId);

        DatabaseStore dbstore = bc.getService(refs.iterator().next());
        String tablePrefix = dbstore.getTablePrefix();

        // Classes explicitly annotated with JPA @Entity:
        ArrayList<String> entityClassNames = new ArrayList<>(classList.size());

        // XML to make all other classes into JPA entities:
        ArrayList<String> entityClassInfo = new ArrayList<>(classList.size());

        for (Class<?> c : classList) {
            if (c.getAnnotation(Entity.class) == null) {
                String primaryKey = getPrimaryKey(c);
                StringBuilder xml = new StringBuilder(500)
                                .append(" <entity class=\"" + c.getName() + "\">")
                                .append(EOLN)
                                .append("  <table name=\"" + tablePrefix + c.getSimpleName() + "\"/>")
                                .append(EOLN)
                                .append("  <attributes>")
                                .append(EOLN)
                                .append("   <id name=\"" + primaryKey + "\">")
                                .append(EOLN)
                                .append("    <column name=\"" + primaryKey + "\" nullable=\"false\"/>")
                                .append(EOLN)
                                .append("   </id>")
                                .append(EOLN)
                                .append("  </attributes>")
                                .append(EOLN)
                                .append(" </entity>")
                                .append(EOLN);

                entityClassInfo.add(xml.toString());
            } else {
                entityClassNames.add(c.getName());
            }
        }

        Map<String, ?> properties = Collections.singletonMap("io.openliberty.persistence.internal.entityClassInfo",
                                                             entityClassInfo.toArray(new String[entityClassInfo.size()]));

        PersistenceServiceUnit punit = dbstore.createPersistenceServiceUnit(loader,
                                                                            properties,
                                                                            entityClassNames.toArray(new String[entityClassNames.size()]));
        units.put(new SimpleImmutableEntry<>(dbStoreId, loader),
                  new SimpleImmutableEntry<PersistenceServiceUnit, List<Class<?>>>(punit, classList));
    }

    Entry<PersistenceServiceUnit, List<Class<?>>> getPersistenceInfo(String dbStoreId, ClassLoader loader) {
        System.out.println("Available persistence service units: " + units);
        Entry<String, ClassLoader> key = new SimpleImmutableEntry<>(dbStoreId, loader);
        Entry<PersistenceServiceUnit, List<Class<?>>> unitInfo = units.get(key);
        System.out.println("Found " + unitInfo + " using key: " + dbStoreId + "," + loader);
        return unitInfo;
    }

    private static String getPrimaryKey(Class<?> c) {
        // TODO Could allow primary key to be identified on @Data annotation.
        // For now, choosing "id" or any field that ends with id
        String primaryKey = null;
        String primaryKeyUpperCase = null;
        for (Field field : c.getFields()) {
            String name = field.getName().toUpperCase();
            if ("ID".equals(name))
                return field.getName();
            else if (name.endsWith("ID"))
                if (primaryKeyUpperCase == null || name.compareTo(primaryKeyUpperCase) < 0) {
                    primaryKeyUpperCase = name;
                    primaryKey = field.getName();
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
                        if (primaryKeyUpperCase == null || name.compareTo(primaryKeyUpperCase) < 0) {
                            primaryKeyUpperCase = name;
                            primaryKey = method.getName().substring(3);
                        }
                }
            }
        }
        if (primaryKey == null)
            throw new IllegalArgumentException(c + " lacks public primary key field/method of the form *ID");
        return primaryKey;
    }
}