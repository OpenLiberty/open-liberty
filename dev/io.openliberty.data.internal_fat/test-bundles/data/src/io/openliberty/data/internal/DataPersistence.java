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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

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
import io.openliberty.data.DiscriminatorColumn;
import io.openliberty.data.DiscriminatorValue;
import io.openliberty.data.Embeddable;
import io.openliberty.data.Entity;
import io.openliberty.data.Generated;
import io.openliberty.data.Id;
import io.openliberty.data.Inheritance;
import io.openliberty.data.MappedSuperclass;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = DataPersistence.class)
public class DataPersistence {
    private static final String EOLN = String.format("%n");

    // provider name --> entity class --> upper case attribute name --> properly cased/qualified JPQL attribute name
    private final Map<String, Map<Class<?>, LinkedHashMap<String, String>>> attributeNamesMap = new HashMap<>();

    private final ConcurrentHashMap<//
                    Entry<String, ClassLoader>, // (data access provider, class loader)
                    Entry<PersistenceServiceUnit, Map<Class<?>, String>>> units = new ConcurrentHashMap<>();

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
                StringBuilder xml = new StringBuilder(500)
                                .append(" <entity class=\"" + c.getName() + "\">")
                                .append(EOLN);

                if (c.getAnnotation(Inheritance.class) == null) {
                    String tableName = tablePrefix + (entity == null || entity.value() == null ? c.getSimpleName() : entity.value());
                    xml
                                    .append("  <table name=\"" + tableName + "\"/>")
                                    .append(EOLN);
                } else {
                    xml
                                    .append("  <inheritance strategy=\"SINGLE_TABLE\"/>")
                                    .append(EOLN);
                }

                DiscriminatorValue discriminatorValue = c.getAnnotation(DiscriminatorValue.class);
                if (discriminatorValue != null)
                    xml
                                    .append("  <discriminator-value>")
                                    .append(discriminatorValue.value())
                                    .append("</discriminator-value>")
                                    .append(EOLN);

                DiscriminatorColumn discriminatorColumn = c.getAnnotation(DiscriminatorColumn.class);
                if (discriminatorColumn != null)
                    xml
                                    .append("  <discriminator-column name=\"")
                                    .append(discriminatorColumn.value())
                                    .append("\"/>")
                                    .append(EOLN);

                xml
                                .append("  <attributes>")
                                .append(EOLN);

                List<Field> fields = new ArrayList<Field>();
                for (Class<?> superc = c; superc != null; superc = superc.getSuperclass()) {
                    boolean isMappedSuperclass = superc.getAnnotation(MappedSuperclass.class) != null;
                    if (isMappedSuperclass || superc == c)
                        for (Field f : superc.getFields())
                            if (isMappedSuperclass || c.equals(f.getDeclaringClass()))
                                fields.add(f);
                }

                for (Field field : fields) {
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

        EntityManager em = punit.createEntityManager();
        try {
            Metamodel model = em.getMetamodel();
            for (EntityType<?> entityType : model.getEntities()) {
                LinkedHashMap<String, String> attributeNames = new LinkedHashMap<>();
                for (Attribute<?, ?> attr : entityType.getAttributes()) {
                    String attributeName = attr.getName();
                    if (PersistentAttributeType.EMBEDDED.equals(attr.getPersistentAttributeType())) {
                        // TODO this only covers one level of embedded attributes, which is fine for now because this isn't a real implementation
                        EmbeddableType<?> embeddable = model.embeddable(attr.getJavaType());
                        for (Attribute<?, ?> embAttr : embeddable.getAttributes()) {
                            String embeddableAttributeName = embAttr.getName();
                            String fullAttributeName = attributeName + '.' + embeddableAttributeName;
                            attributeNames.put(embeddableAttributeName.toUpperCase(), fullAttributeName);
                        }
                    } else {
                        attributeNames.put(attributeName.toUpperCase(), attributeName);
                    }
                }

                Map<Class<?>, LinkedHashMap<String, String>> attrNamesPerClassMap = attributeNamesMap.get(dbStoreId);
                if (attrNamesPerClassMap == null)
                    attributeNamesMap.put(dbStoreId, attrNamesPerClassMap = new HashMap<>());

                attrNamesPerClassMap.put(entityType.getJavaType(), attributeNames);

                System.out.println(attributeNames);

                // TODO entityType.hasVersionAttribute() and entityType.getVersion(versionType) might be useful,
                // but how is the version class parameter determined?
            }
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        } finally {
            em.close();
        }

        units.put(new SimpleImmutableEntry<>(dbStoreId, loader),
                  new SimpleImmutableEntry<>(punit, entityInfo));
    }

    String getAttributeName(String name, Class<?> entityClass, String provider) {
        Map<Class<?>, LinkedHashMap<String, String>> attrNamesPerClassMap = attributeNamesMap.get(provider);
        LinkedHashMap<String, String> attributeNames = attrNamesPerClassMap == null ? null : attrNamesPerClassMap.get(entityClass);
        String attributeName = attributeNames == null ? null : attributeNames.get(name.toUpperCase());
        return attributeName == null ? name : attributeName;
    }

    Collection<String> getAttributeNames(Class<?> entityClass, String provider) {
        Map<Class<?>, LinkedHashMap<String, String>> attrNamesPerClassMap = attributeNamesMap.get(provider);
        LinkedHashMap<String, String> attributeNames = attrNamesPerClassMap == null ? null : attrNamesPerClassMap.get(entityClass);
        return attributeNames.values();
    }

    // TODO this is very inefficient, but works for now
    Entry<String, PersistenceServiceUnit> getPersistenceServiceUnit(Class<?> entityClass) {
        System.out.println("Available persistence service units: " + units);
        for (Entry<PersistenceServiceUnit, Map<Class<?>, String>> entry : units.values()) {
            Map<Class<?>, String> entityInfo = entry.getValue();
            String keyAttribute = entityInfo.get(entityClass);
            if (keyAttribute != null)
                return new SimpleImmutableEntry<>(keyAttribute, entry.getKey());
        }
        throw new RuntimeException("Persistence layer unavailable for " + entityClass);
    }

    Entry<PersistenceServiceUnit, Map<Class<?>, String>> getPersistenceInfo(String dbStoreId, ClassLoader loader) {
        System.out.println("Available persistence service units: " + units);
        Entry<String, ClassLoader> key = new SimpleImmutableEntry<>(dbStoreId, loader);
        Entry<PersistenceServiceUnit, Map<Class<?>, String>> unitInfo = units.get(key);
        System.out.println("Found " + unitInfo + " using key: " + dbStoreId + "," + loader);
        return unitInfo;
    }
}