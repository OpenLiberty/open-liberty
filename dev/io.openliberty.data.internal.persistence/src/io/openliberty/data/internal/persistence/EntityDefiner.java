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
package io.openliberty.data.internal.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.persistence.DatabaseStore;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;

import jakarta.data.exceptions.MappingException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * Runs asynchronously to supply orm.xml for entities that aren't already Jakarta Persistence entities
 * and to discover information about entities.
 */
class EntityDefiner implements Runnable {
    private static final String EOLN = String.format("%n");
    private static final TraceComponent tc = Tr.register(EntityDefiner.class);

    private final String databaseId;
    private final List<Class<?>> entities;
    private final ClassLoader loader;
    private final PersistenceDataProvider provider;

    EntityDefiner(PersistenceDataProvider provider, String databaseId, ClassLoader loader, List<Class<?>> entities) {
        this.provider = provider;
        this.databaseId = databaseId;
        this.loader = loader;
        this.entities = entities;
    }

    /**
     * It's more likely for a name ending with "Id" or "ID"
     * to be an id than a name ending with "id",
     * unless the name is "id".
     *
     * Precedence is:
     * Field/parameter/method that is named Id, or ID, or id, (2)
     * or lacking that has a name that ends with Id (3), ID (4), or id (5).
     *
     * @param entityClass entity class.
     * @return name of id property.
     * @throws MappingException if the id property cannot be inferred.
     */
    private static String getID(Class<?> entityClass) {
        int precedence = 10;
        String id = null;

        for (Field field : entityClass.getFields()) {
            String name = field.getName();

            if (precedence > 2)
                if (name.length() > 2) {
                    if (precedence > 3) {
                        char i = name.charAt(name.length() - 2);
                        if (i == 'I') {
                            char d = name.charAt(name.length() - 1);
                            if (d == 'd') {
                                id = name;
                                precedence = 3;
                            } else if (d == 'D' && precedence > 4) {
                                id = name;
                                precedence = 4;
                            }
                        } else if (i == 'i' && precedence > 5 && name.charAt(name.length() - 1) == 'd') {
                            id = name;
                            precedence = 5;
                        }
                    }
                } else if (name.equalsIgnoreCase("ID")) {
                    id = name;
                    precedence = 2;
                }
        }

        // TODO record parameters

        for (Method method : entityClass.getMethods()) {
            String name = method.getName();
            if (name.startsWith("get"))
                name = name.substring(3);
            else if (name.startsWith("is"))
                name = name.substring(2);
            else
                continue;

            if (precedence > 2)
                if (name.length() > 2) {
                    if (precedence > 3) {
                        char i = name.charAt(name.length() - 2);
                        if (i == 'I') {
                            char d = name.charAt(name.length() - 1);
                            if (d == 'd') {
                                id = name;
                                precedence = 3;
                            } else if (d == 'D' && precedence > 4) {
                                id = name;
                                precedence = 4;
                            }
                        } else if (i == 'i' && precedence > 5 && name.charAt(name.length() - 1) == 'd') {
                            id = name;
                            precedence = 5;
                        }
                    }
                } else if (name.equalsIgnoreCase("ID")) {
                    id = name;
                    precedence = 2;
                }
        }

        if (id == null)
            throw new MappingException(entityClass + " lacks public field of the form *ID or public method of the form get*ID."); // TODO NLS
        return id;
    }

    @Override
    @Trivial
    public void run() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "run: define entities", entities);

        EntityManager em = null;
        try {
            BundleContext bc = FrameworkUtil.getBundle(DatabaseStore.class).getBundleContext();
            Collection<ServiceReference<DatabaseStore>> refs = bc.getServiceReferences(DatabaseStore.class,
                                                                                       FilterUtils.createPropertyFilter("id", databaseId));
            if (refs.isEmpty())
                throw new IllegalArgumentException("Not found: " + databaseId);

            ServiceReference<DatabaseStore> ref = refs.iterator().next();
            String tablePrefix = (String) ref.getProperty("tablePrefix");

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, databaseId + " databaseStore reference", ref);

            // Classes explicitly annotated with JPA @Entity:
            ArrayList<String> entityClassNames = new ArrayList<>(entities.size());

            // XML to make all other classes into JPA entities:
            ArrayList<String> entityClassInfo = new ArrayList<>(entities.size());

            for (Class<?> c : entities) {
                if (c.getAnnotation(jakarta.persistence.Entity.class) == null) {
                    StringBuilder xml = new StringBuilder(500).append(" <entity class=\"").append(c.getName()).append("\">").append(EOLN);

                    xml.append("  <table name=\"").append(tablePrefix).append(c.getSimpleName()).append("\"/>").append(EOLN);

                    writeAttributes(xml, c, getID(c));

                    xml.append(" </entity>").append(EOLN);

                    entityClassInfo.add(xml.toString());
                } else {
                    entityClassNames.add(c.getName());
                }
            }

            Map<String, ?> properties = Collections.singletonMap("io.openliberty.persistence.internal.entityClassInfo",
                                                                 entityClassInfo.toArray(new String[entityClassInfo.size()]));

            DatabaseStore dbstore = bc.getService(ref);
            PersistenceServiceUnit punit = dbstore.createPersistenceServiceUnit(loader,
                                                                                properties,
                                                                                entityClassNames.toArray(new String[entityClassNames.size()]));

            em = punit.createEntityManager();
            Metamodel model = em.getMetamodel();
            for (EntityType<?> entityType : model.getEntities()) {
                entityType.getName();//TODO
                Map<String, String> attributeNames = new HashMap<>();
                Map<String, List<Member>> attributeAccessors = new HashMap<>();
                SortedMap<String, Class<?>> attributeTypes = new TreeMap<>();
                Map<Class<?>, List<String>> embeddableAttributeNames = new HashMap<>();
                Queue<Attribute<?, ?>> embeddables = new LinkedList<>();
                Queue<String> embeddablePrefixes = new LinkedList<>();
                Queue<List<Member>> embeddableAccessors = new LinkedList<>();
                Class<?> idClass = null;
                SortedMap<String, Member> idClassAttributeAccessors = null;

                for (Attribute<?, ?> attr : entityType.getAttributes()) {
                    String attributeName = attr.getName();
                    PersistentAttributeType attributeType = attr.getPersistentAttributeType();
                    if (PersistentAttributeType.EMBEDDED.equals(attributeType)) {
                        embeddableAttributeNames.put(attr.getJavaType(), new ArrayList<>());
                        embeddables.add(attr);
                        embeddablePrefixes.add(attributeName);
                        embeddableAccessors.add(Collections.singletonList(attr.getJavaMember()));
                    }
                    attributeNames.put(attributeName.toLowerCase(), attributeName);
                    if (attr instanceof SingularAttribute && ((SingularAttribute<?, ?>) attr).isId())
                        attributeNames.put("id", attributeName);
                    attributeAccessors.put(attributeName, Collections.singletonList(attr.getJavaMember()));
                    attributeTypes.put(attributeName, PersistentAttributeType.ELEMENT_COLLECTION.equals(attributeType) //
                                    ? Collection.class //
                                    : attr.getJavaType());
                }

                for (Attribute<?, ?> attr; (attr = embeddables.poll()) != null;) {
                    String prefix = embeddablePrefixes.poll();
                    List<Member> accessors = embeddableAccessors.poll();
                    EmbeddableType<?> embeddable = model.embeddable(attr.getJavaType());
                    List<String> embAttributeList = embeddableAttributeNames.get(attr.getJavaType());
                    for (Attribute<?, ?> embAttr : embeddable.getAttributes()) {
                        String embeddableAttributeName = embAttr.getName();
                        String fullAttributeName = prefix + '.' + embeddableAttributeName;
                        List<Member> embAccessors = new LinkedList<>(accessors);
                        embAccessors.add(embAttr.getJavaMember());
                        embAttributeList.add(fullAttributeName);

                        PersistentAttributeType attributeType = embAttr.getPersistentAttributeType();
                        if (PersistentAttributeType.EMBEDDED.equals(attributeType)) {
                            embeddableAttributeNames.put(embAttr.getJavaType(), new ArrayList<>());
                            embeddables.add(embAttr);
                            embeddablePrefixes.add(fullAttributeName);
                            embeddableAccessors.add(embAccessors);
                        }

                        // Allow the simple attribute name if it doesn't overlap
                        embeddableAttributeName = embeddableAttributeName.toLowerCase();
                        attributeNames.putIfAbsent(embeddableAttributeName, fullAttributeName);

                        // Allow a qualified name such as @OrderBy("address.street.name")
                        embeddableAttributeName = fullAttributeName.toLowerCase();
                        attributeNames.put(embeddableAttributeName, fullAttributeName);

                        // Allow a qualified name such as findByAddress_Street_Name if it doesn't overlap
                        String embeddableAttributeName_ = embeddableAttributeName.replace('.', '_');
                        attributeNames.putIfAbsent(embeddableAttributeName_, fullAttributeName);

                        // Allow a qualified name such as findByAddressStreetName if it doesn't overlap
                        String embeddableAttributeNameUndelimited = embeddableAttributeName.replace(".", "");
                        attributeNames.putIfAbsent(embeddableAttributeNameUndelimited, fullAttributeName);

                        if (embAttr instanceof SingularAttribute && ((SingularAttribute<?, ?>) embAttr).isId())
                            attributeNames.put("id", fullAttributeName);

                        attributeAccessors.put(fullAttributeName, embAccessors);
                        attributeTypes.put(fullAttributeName, PersistentAttributeType.ELEMENT_COLLECTION.equals(attributeType) //
                                        ? Collection.class //
                                        : embAttr.getJavaType());
                    }
                }

                if (!entityType.hasSingleIdAttribute()) {
                    String attrName = attributeNames.get("id");
                    if (!attrName.contains(".")) { // Skip for Id on Embeddable. Only apply to IdClass
                        attributeNames.remove("id");
                        idClass = entityType.getIdType().getJavaType();
                        idClassAttributeAccessors = new TreeMap<>();
                        for (SingularAttribute<?, ?> attr : entityType.getIdClassAttributes()) {
                            Member entityMember = attr.getJavaMember();
                            Member idClassMember = entityMember instanceof Field //
                                            ? idClass.getField(entityMember.getName()) //
                                            : idClass.getMethod(entityMember.getName());
                            idClassAttributeAccessors.put(attr.getName().toLowerCase(), idClassMember);
                        }
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
                                attributeAccessors, //
                                attributeNames, //
                                attributeTypes, //
                                embeddableAttributeNames, //
                                idClass, //
                                idClassAttributeAccessors, //
                                punit);

                provider.entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).complete(entityInfo);
            }
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run: define entities");
        } catch (RuntimeException x) {
            for (Class<?> entityClass : entities)
                provider.entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run: define entities", x);
            throw x;
        } catch (Exception x) {
            for (Class<?> entityClass : entities)
                provider.entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run: define entities", x);
            throw new RuntimeException(x);
        } catch (Error x) {
            for (Class<?> entityClass : entities)
                provider.entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run: define entities", x);
            throw x;
        } finally {
            if (em != null)
                em.close();
        }
    }

    private void writeAttributes(StringBuilder xml, Class<?> c, String keyAttributeName) {
        xml.append("  <attributes>").append(EOLN);

        List<Field> fields = new ArrayList<Field>();
        for (Class<?> superc = c; superc != null; superc = superc.getSuperclass()) {
            if (superc == c)
                for (Field f : superc.getFields())
                    if (c.equals(f.getDeclaringClass()))
                        fields.add(f);
        }

        for (Field field : fields) {
            String attributeName = field.getName();
            boolean isCollection = Collection.class.isAssignableFrom(field.getType());

            String columnType = keyAttributeName != null && keyAttributeName.equals(attributeName) ? "id" : //
                            "version".equals(attributeName) ? "version" : //
                                            isCollection ? "element-collection" : //
                                                            "basic";

            xml.append("   <" + columnType + " name=\"" + attributeName + "\">").append(EOLN);
            xml.append("   </" + columnType + ">").append(EOLN);
        }

        xml.append("  </attributes>").append(EOLN);
    }
}
