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
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

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

import io.openliberty.data.Column;
import io.openliberty.data.Embeddable;
import io.openliberty.data.Entity;
import io.openliberty.data.Generated;
import io.openliberty.data.Id;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = DataPersistence.class)
public class DataPersistence {
    private static final String EOLN = String.format("%n");

    private static final ConcurrentHashMap<//
                    Entry<String, ClassLoader>, //
                    Entry<PersistenceServiceUnit, Set<Class<?>>>> units = new ConcurrentHashMap<>();

    @Reference(target = "(component.name=com.ibm.ws.threading)")
    protected ExecutorService executor;

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
    public void defineEntities(String dbStoreId, ClassLoader loader, Map<Class<?>, String> entityInfo) throws Exception {
        BundleContext bc = FrameworkUtil.getBundle(DataPersistence.class).getBundleContext();
        Collection<ServiceReference<DatabaseStore>> refs = bc.getServiceReferences(DatabaseStore.class,
                                                                                   FilterUtils.createPropertyFilter("id", dbStoreId));
        if (refs.isEmpty())
            throw new IllegalArgumentException("Not found: " + dbStoreId);

        DatabaseStore dbstore = bc.getService(refs.iterator().next());
        String tablePrefix = dbstore.getTablePrefix();
        // TODO persistence service needs to be fixed to allow the tablePrefix to be retrieved
        // prior to invoking createPersistenceServiceUnit.  For now, just blank it out:
        if (tablePrefix == null)
            tablePrefix = "";

        // Classes explicitly annotated with JPA @Entity:
        ArrayList<String> entityClassNames = new ArrayList<>(entityInfo.size());

        // XML to make all other classes into JPA entities:
        ArrayList<String> entityClassInfo = new ArrayList<>(entityInfo.size());

        List<Class<?>> embeddableTypes = new ArrayList<>();

        for (Entry<Class<?>, String> entry : entityInfo.entrySet()) {
            Class<?> c = entry.getKey();
            String keyAttribute = entry.getValue();

            if (c.getAnnotation(jakarta.persistence.Entity.class) == null) {
                Entity entity = c.getAnnotation(Entity.class);
                String tableName = tablePrefix + (entity == null || entity.value() == null ? c.getSimpleName() : entity.value());
                StringBuilder xml = new StringBuilder(500)
                                .append(" <entity class=\"" + c.getName() + "\">")
                                .append(EOLN)
                                .append("  <table name=\"" + tableName + "\"/>")
                                .append(EOLN)
                                .append("  <attributes>")
                                .append(EOLN);

                for (Field field : c.getFields()) {
                    Id id = field.getAnnotation(Id.class);
                    Column column = field.getAnnotation(Column.class);
                    Generated generated = field.getAnnotation(Generated.class);
                    Embeddable embeddable = field.getType().getAnnotation(Embeddable.class);

                    String attributeName = field.getName();
                    String columnName = column == null || column.value().length() == 0 ? //
                                    id == null || id.value().length() == 0 ? null : id.value() : //
                                    column.value();

                    String columnType;
                    if (embeddable == null) {
                        columnType = id == null && !keyAttribute.equals(attributeName) ? "basic" : "id";
                    } else {
                        columnType = "embedded";
                        embeddableTypes.add(field.getType());
                    }

                    xml
                                    .append("   <" + columnType + " name=\"" + attributeName + "\">")
                                    .append(EOLN);
                    if (columnName != null)
                        xml
                                        .append("    <column name=\"" + columnName + "\"/>")
                                        .append(EOLN);
                    if (generated != null)
                        xml
                                        .append("    <generated-value strategy=\"" + generated.value().name() + "\"/>")
                                        .append(EOLN);
                    xml
                                    .append("   </" + columnType + ">")
                                    .append(EOLN);
                }

                xml
                                .append("  </attributes>")
                                .append(EOLN)
                                .append(" </entity>")
                                .append(EOLN);

                entityClassInfo.add(xml.toString());
            } else {
                entityClassNames.add(c.getName());
            }
        }

        for (Class<?> type : embeddableTypes) {
            StringBuilder xml = new StringBuilder(100)
                            .append(" <embeddable class=\"")
                            .append(type.getName())
                            .append("\"/>")
                            .append(EOLN);
            entityClassInfo.add(xml.toString());
        }

        Map<String, ?> properties = Collections.singletonMap("io.openliberty.persistence.internal.entityClassInfo",
                                                             entityClassInfo.toArray(new String[entityClassInfo.size()]));

        PersistenceServiceUnit punit = dbstore.createPersistenceServiceUnit(loader,
                                                                            properties,
                                                                            entityClassNames.toArray(new String[entityClassNames.size()]));
        units.put(new SimpleImmutableEntry<>(dbStoreId, loader),
                  new SimpleImmutableEntry<PersistenceServiceUnit, Set<Class<?>>>(punit, entityInfo.keySet()));
    }

    Entry<PersistenceServiceUnit, Set<Class<?>>> getPersistenceInfo(String dbStoreId, ClassLoader loader) {
        System.out.println("Available persistence service units: " + units);
        Entry<String, ClassLoader> key = new SimpleImmutableEntry<>(dbStoreId, loader);
        Entry<PersistenceServiceUnit, Set<Class<?>>> unitInfo = units.get(key);
        System.out.println("Found " + unitInfo + " using key: " + dbStoreId + "," + loader);
        return unitInfo;
    }
}