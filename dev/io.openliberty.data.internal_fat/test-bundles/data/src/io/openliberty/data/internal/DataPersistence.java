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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    // entity class --> entity information,
    // where classes for all providers are combined together
    private final Map<Class<?>, EntityInfo> entityInfoPerClass = new HashMap<>();

    // data access provider name --> entity class --> entity information
    private final ConcurrentHashMap<String, Map<Class<?>, EntityInfo>> entityInfoPerProvider = new ConcurrentHashMap<>();

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
     * @param provider  id of databaseStore config element
     * @param loader    class loader
     * @param classList entity classes
     * @throws Exception
     */
    public void defineEntities(String provider, ClassLoader loader, List<Class<?>> entities) throws Exception {
        BundleContext bc = FrameworkUtil.getBundle(DataPersistence.class).getBundleContext();
        Collection<ServiceReference<DatabaseStore>> refs = bc.getServiceReferences(DatabaseStore.class,
                                                                                   FilterUtils.createPropertyFilter("id", provider));
        if (refs.isEmpty())
            throw new IllegalArgumentException("Not found: " + provider);

        DatabaseStore dbstore = bc.getService(refs.iterator().next());
        String tablePrefix = dbstore.getTablePrefix();
        // TODO persistence service needs to be fixed to allow the tablePrefix to be retrieved
        // prior to invoking createPersistenceServiceUnit.  For now, just blank it out:
        if (tablePrefix == null)
            tablePrefix = "";

        // Classes explicitly annotated with JPA @Entity:
        ArrayList<String> entityClassNames = new ArrayList<>(entities.size());

        // XML to make all other classes into JPA entities:
        ArrayList<String> entityClassInfo = new ArrayList<>(entities.size());

        List<Class<?>> embeddableTypes = new ArrayList<>();

        Map<Class<?>, String> keyAttributeNames = new HashMap<>();

        for (Class<?> c : entities) {
            String keyAttributeName = getID(c);
            keyAttributeNames.put(c, keyAttributeName);

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
                    boolean isCollection = Collection.class.isAssignableFrom(field.getType());

                    String columnType;
                    if (embeddable == null) {
                        columnType = id != null || keyAttributeName.equals(attributeName) ? "id" : //
                                        "version".equals(attributeName) ? "version" : //
                                                        isCollection ? "element-collection" : //
                                                                        "basic";
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
                entityType.getName();//TODO
                LinkedHashMap<String, String> attributeNames = new LinkedHashMap<>();
                Set<String> collectionAttributeNames = new HashSet<String>();
                for (Attribute<?, ?> attr : entityType.getAttributes()) {
                    String attributeName = attr.getName();
                    PersistentAttributeType attributeType = attr.getPersistentAttributeType();

                    if (PersistentAttributeType.EMBEDDED.equals(attributeType)) {
                        // TODO this only covers one level of embedded attributes, which is fine for now because this isn't a real implementation
                        EmbeddableType<?> embeddable = model.embeddable(attr.getJavaType());
                        for (Attribute<?, ?> embAttr : embeddable.getAttributes()) {
                            String embeddableAttributeName = embAttr.getName();
                            String fullAttributeName = attributeName + '.' + embeddableAttributeName;
                            attributeNames.put(embeddableAttributeName.toUpperCase(), fullAttributeName);
                        }
                    } else {
                        attributeNames.put(attributeName.toUpperCase(), attributeName);
                        if (PersistentAttributeType.ELEMENT_COLLECTION.equals(attributeType))
                            collectionAttributeNames.add(attributeName);
                    }
                }

                // This works for version Fields, and might work for version getter/setter methods
                // but is debatable whether we should do it.
                //Member versionMember = null;
                //if (entityType.hasVersionAttribute())
                //    for (SingularAttribute<?, ?> attr : entityType.getSingularAttributes())
                //        if (attr.isVersion()) {
                //            versionMember = attr.getJavaMember(); // Field or Method, which could be used to update a passed-in entity with the new version number
                //            break;
                //        }

                Class<?> entityClass = entityType.getJavaType();
                EntityInfo entityInfo = new EntityInfo(entityType.getName(), //
                                entityClass, //
                                attributeNames, //
                                collectionAttributeNames, //
                                keyAttributeNames.get(entityClass), //
                                punit);

                entityInfoPerClass.put(entityClass, entityInfo);

                Map<Class<?>, EntityInfo> entityInfoPerClassAndProvider = entityInfoPerProvider.get(provider);
                if (entityInfoPerClassAndProvider == null)
                    entityInfoPerProvider.put(provider, entityInfoPerClassAndProvider = new HashMap<>());
                entityInfoPerClassAndProvider.put(entityClass, entityInfo);

                System.out.println(attributeNames);
            }
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        } finally {
            em.close();
        }
    }

    String getAttributeName(String name, Class<?> entityClass, String provider) {
        Map<Class<?>, EntityInfo> entityInfoMap = entityInfoPerProvider.get(provider);
        EntityInfo entityInfo = entityInfoMap == null ? null : entityInfoMap.get(entityClass);
        String attributeName = entityInfo == null ? null : entityInfo.attributeNames.get(name.toUpperCase());
        return attributeName == null ? name : attributeName;
    }

    Collection<String> getAttributeNames(Class<?> entityClass, String provider) {
        Map<Class<?>, EntityInfo> entityInfoMap = entityInfoPerProvider.get(provider);
        EntityInfo entityInfo = entityInfoMap == null ? null : entityInfoMap.get(entityClass);
        LinkedHashMap<String, String> attributeNames = entityInfo == null ? null : entityInfo.attributeNames;
        return attributeNames.values();
    }

    // For use by Template only. For other patterns, use the signature that also supplies the provider name
    EntityInfo getEntityInfo(Class<?> entityClass) {
        EntityInfo entityInfo = entityInfoPerClass.get(entityClass);
        if (entityInfo == null)
            throw new RuntimeException("Persistence layer unavailable for " + entityClass);
        return entityInfo;
    }

    EntityInfo getEntityInfo(String provider, Class<?> entityClass) {
        Map<Class<?>, EntityInfo> entityInfoMap = entityInfoPerProvider.get(provider);
        return entityInfoMap == null ? null : entityInfoMap.get(entityClass);
    }

    // Moved from DataExtension
    private static String getID(Class<?> entityClass) {
        // For now, choosing "id" or any field that ends with id
        String id = null;
        String upperID = null;
        for (Field field : entityClass.getFields()) {
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
            throw new IllegalArgumentException(entityClass + " lacks public field with @Id or of the form *ID");
        return id;
    }
}