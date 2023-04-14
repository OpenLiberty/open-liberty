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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.persistence.DatabaseStore;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;

import jakarta.data.exceptions.MappingException;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

/**
 * Runs asynchronously to supply orm.xml for entities that aren't already Jakarta Persistence entities
 * and to discover information about entities.
 */
public class EntityDefiner implements Runnable {
    private static final String EOLN = String.format("%n");
    private static final TraceComponent tc = Tr.register(EntityDefiner.class);

    private final String databaseId;
    private final List<Class<?>> entities = new ArrayList<>();
    final ConcurrentHashMap<Class<?>, CompletableFuture<EntityInfo>> entityInfoMap = new ConcurrentHashMap<>();
    private final ClassLoader loader;

    public EntityDefiner(String databaseId, ClassLoader loader) {
        this.databaseId = databaseId;
        this.loader = loader;
    }

    /**
     * Adds an entity class to be handled.
     *
     * @param entityClass entity class.
     */
    @Trivial
    public void add(Class<?> entityClass) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "add: " + entityClass.getName());

        entities.add(entityClass);
    }

    /**
     * Obtains the entity class (if any) for a type that might be parameterized.
     *
     * @param type a type that might be parameterized.
     * @return entity class or null.
     */
    @Trivial
    private Class<?> getEntityClass(java.lang.reflect.Type type) {
        Class<?> c = null;
        if (type instanceof ParameterizedType) {
            java.lang.reflect.Type[] typeParams = ((ParameterizedType) type).getActualTypeArguments();
            type = typeParams.length == 1 ? typeParams[0] : null;
            if (type instanceof Class && ((Class<?>) type).isAnnotationPresent(Entity.class))
                c = (Class<?>) type;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "getEntityClass from parameterized " + type + ": " + c);
        }
        return c;
    }

    /**
     * Request the Id type, allowing for an EclipseLink extension that lets the
     * Id to be located on an attribute of an Embeddable of the entity.
     *
     * @param entityType
     * @return the Id type. Null if the type of Id cannot be determined.
     */
    @FFDCIgnore(RuntimeException.class)
    @Trivial
    private static Type<?> getIdType(EntityType<?> entityType) {
        Type<?> idType;
        try {
            idType = entityType.getIdType();
        } catch (RuntimeException x) {
            // occurs with EclipseLink extension to JPA that allows @Id on an embeddable attribute
            if ("ConversionException".equals(x.getClass().getSimpleName()))
                idType = null;
            else
                throw x;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, entityType.getName() + " getIdType: " + idType);
        return idType;
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
            Set<String> entityClassNames = new HashSet<>(entities.size() * 2);

            // List of classes to inspect for the above
            Queue<Class<?>> annotatedEntityClassQueue = new LinkedList<>();

            // XML to make all other classes into JPA entities:
            ArrayList<String> entityClassInfo = new ArrayList<>(entities.size());

            Queue<Class<?>> embeddableTypesQueue = new LinkedList<>();

            for (Class<?> c : entities) {
                if (c.isAnnotationPresent(Entity.class)) {
                    annotatedEntityClassQueue.add(c);
                } else {
                    StringBuilder xml = new StringBuilder(500).append(" <entity class=\"").append(c.getName()).append("\">").append(EOLN);

                    xml.append("  <table name=\"").append(tablePrefix).append(c.getSimpleName()).append("\"/>").append(EOLN);

                    writeAttributes(xml, c, false, embeddableTypesQueue);

                    xml.append(" </entity>").append(EOLN);

                    entityClassInfo.add(xml.toString());
                }
            }

            Set<Class<?>> embeddableTypes = new HashSet<>();
            for (Class<?> type; (type = embeddableTypesQueue.poll()) != null;)
                if (embeddableTypes.add(type)) { // only write each type once
                    StringBuilder xml = new StringBuilder(500).append(" <embeddable class=\"").append(type.getName()).append("\">").append(EOLN);
                    writeAttributes(xml, type, true, embeddableTypesQueue);
                    xml.append(" </embeddable>").append(EOLN);
                    entityClassInfo.add(xml.toString());
                }

            // Discover entities that are indirectly referenced via OneToOne, ManyToMany, and so forth
            for (Class<?> c; (c = annotatedEntityClassQueue.poll()) != null;)
                if (entityClassNames.add(c.getName())) {
                    Class<?> e;
                    for (Field f : c.getFields())
                        if (f.getType().isAnnotationPresent(Entity.class))
                            annotatedEntityClassQueue.add(f.getType());
                        else if ((e = getEntityClass(f.getGenericType())) != null)
                            annotatedEntityClassQueue.add(e);
                    for (Method m : c.getMethods())
                        if (m.getReturnType().isAnnotationPresent(Entity.class))
                            annotatedEntityClassQueue.add(m.getReturnType());
                        else if ((e = getEntityClass(m.getGenericReturnType())) != null)
                            annotatedEntityClassQueue.add(e);
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
                Map<String, String> attributeNames = new HashMap<>();
                Map<String, List<Member>> attributeAccessors = new HashMap<>();
                SortedMap<String, Class<?>> attributeTypes = new TreeMap<>();
                Map<String, Class<?>> collectionElementTypes = new HashMap<>();
                Map<Class<?>, List<String>> relationAttributeNames = new HashMap<>();
                Queue<Attribute<?, ?>> relationships = new LinkedList<>();
                Queue<String> relationPrefixes = new LinkedList<>();
                Queue<List<Member>> relationAccessors = new LinkedList<>();
                Class<?> idClass = null;
                SortedMap<String, Member> idClassAttributeAccessors = null;

                for (Attribute<?, ?> attr : entityType.getAttributes()) {
                    String attributeName = attr.getName();
                    PersistentAttributeType attributeType = attr.getPersistentAttributeType();
                    if (PersistentAttributeType.EMBEDDED.equals(attributeType) ||
                        PersistentAttributeType.ONE_TO_ONE.equals(attributeType) ||
                        PersistentAttributeType.MANY_TO_ONE.equals(attributeType)) {
                        relationAttributeNames.put(attr.getJavaType(), new ArrayList<>());
                        relationships.add(attr);
                        relationPrefixes.add(attributeName);
                        relationAccessors.add(Collections.singletonList(attr.getJavaMember()));
                    }
                    attributeNames.put(attributeName.toLowerCase(), attributeName);
                    attributeAccessors.put(attributeName, Collections.singletonList(attr.getJavaMember()));
                    attributeTypes.put(attributeName, attr.getJavaType());
                    if (attr.isCollection()) {
                        if (attr instanceof PluralAttribute)
                            collectionElementTypes.put(attributeName, ((PluralAttribute<?, ?, ?>) attr).getElementType().getJavaType());
                    } else if (attr instanceof SingularAttribute && ((SingularAttribute<?, ?>) attr).isId()) {
                        attributeNames.put("id", attributeName);
                    }
                }

                // Guard against recursive processing of OneToOne (and similar) relationships
                // by tracking whether we have already processed each entity class involved.
                Set<Class<?>> entityTypeClasses = new HashSet<>();
                entityTypeClasses.add(entityType.getJavaType());

                for (Attribute<?, ?> attr; (attr = relationships.poll()) != null;) {
                    String prefix = relationPrefixes.poll();
                    List<Member> accessors = relationAccessors.poll();
                    ManagedType<?> relation = model.managedType(attr.getJavaType());
                    if (relation instanceof EntityType && !entityTypeClasses.add(attr.getJavaType()))
                        break;
                    List<String> relAttributeList = relationAttributeNames.get(attr.getJavaType());
                    for (Attribute<?, ?> relAttr : relation.getAttributes()) {
                        String relationAttributeName = relAttr.getName();
                        String fullAttributeName = prefix + '.' + relationAttributeName;
                        List<Member> relAccessors = new LinkedList<>(accessors);
                        relAccessors.add(relAttr.getJavaMember());
                        relAttributeList.add(fullAttributeName);

                        PersistentAttributeType attributeType = relAttr.getPersistentAttributeType();
                        if (PersistentAttributeType.EMBEDDED.equals(attributeType) ||
                            PersistentAttributeType.ONE_TO_ONE.equals(attributeType) ||
                            PersistentAttributeType.MANY_TO_ONE.equals(attributeType)) {
                            relationAttributeNames.put(relAttr.getJavaType(), new ArrayList<>());
                            relationships.add(relAttr);
                            relationPrefixes.add(fullAttributeName);
                            relationAccessors.add(relAccessors);
                        }

                        // Allow the simple attribute name if it doesn't overlap
                        relationAttributeName = relationAttributeName.toLowerCase();
                        attributeNames.putIfAbsent(relationAttributeName, fullAttributeName);

                        // Allow a qualified name such as @OrderBy("address.street.name")
                        relationAttributeName = fullAttributeName.toLowerCase();
                        attributeNames.put(relationAttributeName, fullAttributeName);

                        // Allow a qualified name such as findByAddress_Street_Name if it doesn't overlap
                        String relationAttributeName_ = relationAttributeName.replace('.', '_');
                        attributeNames.putIfAbsent(relationAttributeName_, fullAttributeName);

                        // Allow a qualified name such as findByAddressStreetName if it doesn't overlap
                        String relationAttributeNameUndelimited = relationAttributeName.replace(".", "");
                        attributeNames.putIfAbsent(relationAttributeNameUndelimited, fullAttributeName);

                        attributeAccessors.put(fullAttributeName, relAccessors);

                        attributeTypes.put(fullAttributeName, relAttr.getJavaType());
                        if (relAttr.isCollection()) {
                            if (relAttr instanceof PluralAttribute)
                                collectionElementTypes.put(fullAttributeName, ((PluralAttribute<?, ?, ?>) relAttr).getElementType().getJavaType());
                        } else if (relAttr instanceof SingularAttribute && ((SingularAttribute<?, ?>) relAttr).isId()) {
                            attributeNames.put("id", fullAttributeName);
                        }
                    }
                }

                if (!entityType.hasSingleIdAttribute()) {
                    // Per JavaDoc, the above means there is an IdClass.
                    // An EclipseLink extension that allows an Id on an embeddable of an entity
                    // is an exception to this, which we indicate with idClassType null.
                    Type<?> idClassType = getIdType(entityType);
                    if (idClassType != null) {
                        @SuppressWarnings("unchecked")
                        Set<SingularAttribute<?, ?>> idClassAttributes = (Set<SingularAttribute<?, ?>>) (Set<?>) entityType.getIdClassAttributes();
                        if (idClassAttributes != null) {
                            attributeNames.remove("id");
                            idClass = idClassType.getJavaType();
                            idClassAttributeAccessors = new TreeMap<>();
                            for (SingularAttribute<?, ?> attr : idClassAttributes) {
                                Member entityMember = attr.getJavaMember();
                                Member idClassMember = entityMember instanceof Field //
                                                ? idClass.getField(entityMember.getName()) //
                                                : idClass.getMethod(entityMember.getName());
                                idClassAttributeAccessors.put(attr.getName().toLowerCase(), idClassMember);
                            }
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
                                collectionElementTypes, //
                                relationAttributeNames, //
                                idClass, //
                                idClassAttributeAccessors, //
                                punit);

                entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).complete(entityInfo);
            }
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run: define entities");
        } catch (RuntimeException x) {
            for (Class<?> entityClass : entities)
                entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run: define entities", x);
            throw x;
        } catch (Exception x) {
            for (Class<?> entityClass : entities)
                entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run: define entities", x);
            throw new RuntimeException(x);
        } catch (Error x) {
            for (Class<?> entityClass : entities)
                entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run: define entities", x);
            throw x;
        } finally {
            if (em != null)
                em.close();
        }
    }

    /**
     * Write attributes for the specified entity or embeddable to XML.
     *
     * @param xml                  XML for defining the entity attributes
     * @param c                    entity class
     * @param isEmbeddable         indicates if the class is an embeddable type rather than an entity.
     * @param embeddableTypesQueue queue of embeddable types. This method adds to the queue when an embeddable type is found.
     */
    private void writeAttributes(StringBuilder xml, Class<?> c, boolean isEmbeddable, Queue<Class<?>> embeddableTypesQueue) {

        // Identify attributes
        SortedMap<String, Class<?>> attributes = new TreeMap<>();

        // TODO cover records once compiling against Java 17

        for (Field f : c.getFields())
            attributes.putIfAbsent(f.getName(), f.getType());

        try {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(c).getPropertyDescriptors();
            if (propertyDescriptors != null)
                for (PropertyDescriptor p : propertyDescriptors) {
                    Method setter = p.getWriteMethod();
                    if (setter != null)
                        attributes.putIfAbsent(p.getName(), p.getPropertyType());
                }
        } catch (IntrospectionException x) {
            throw new MappingException(x);
        }

        String keyAttributeName = null;
        if (!isEmbeddable) {
            // Determine which attribute is the id.
            // Precedence is:
            // (1) has name of Id, or ID, or id.
            // (2) name ends with Id.
            // (3) name ends with ID.
            // (4) type is UUID.
            // (5) name ends with id.
            int precedence = 10;
            for (Map.Entry<String, Class<?>> attribute : attributes.entrySet()) {
                String name = attribute.getKey();
                Class<?> type = attribute.getValue(); // TODO compare type against the repository key type if defined
                if (name.length() > 2) {
                    if (precedence > 2) {
                        char i = name.charAt(name.length() - 2);
                        if (i == 'I') {
                            char d = name.charAt(name.length() - 1);
                            if (d == 'd') {
                                keyAttributeName = name;
                                precedence = 2;
                            } else if (d == 'D' && precedence > 3) {
                                keyAttributeName = name;
                                precedence = 3;
                            }
                        } else if (i == 'i' && precedence > 5 && name.charAt(name.length() - 1) == 'd') {
                            keyAttributeName = name;
                            precedence = 5;
                        }
                    }
                } else if (name.equalsIgnoreCase("ID")) {
                    keyAttributeName = name;
                    precedence = 1;
                    break;
                } else if (precedence > 4 && UUID.class.equals(type)) {
                    keyAttributeName = name;
                    precedence = 4;
                }
            }

            if (keyAttributeName == null)
                throw new MappingException("Entity class " + c.getName() + " lacks a public field of the form *ID or public method of the form get*ID."); // TODO NLS
        }

        // Write the attributes to XML:

        xml.append("  <attributes>").append(EOLN);

        for (Map.Entry<String, Class<?>> attributeInfo : attributes.entrySet()) {
            String attributeName = attributeInfo.getKey();
            Class<?> attributeType = attributeInfo.getValue();
            boolean isCollection = Collection.class.isAssignableFrom(attributeType);
            boolean isPrimitive = attributeType.isPrimitive();

            String columnType;
            if (isPrimitive || attributeType.isInterface() || Serializable.class.isAssignableFrom(attributeType)) {
                columnType = keyAttributeName != null && keyAttributeName.equalsIgnoreCase(attributeName) ? "id" : //
                                "version".equalsIgnoreCase(attributeName) ? "version" : //
                                                isCollection ? "element-collection" : //
                                                                "basic";
            } else {
                columnType = "embedded";
                embeddableTypesQueue.add(attributeType);
            }

            xml.append("   <" + columnType + " name=\"" + attributeName + "\">").append(EOLN);

            if (isEmbeddable) {
                if (!"embedded".equals(columnType))
                    xml.append("    <column name=\"").append(c.getSimpleName().toUpperCase()).append(attributeName.toUpperCase()).append("\"/>").append(EOLN);
            } else {
                if (isPrimitive)
                    xml.append("    <column nullable=\"false\"/>").append(EOLN);
            }

            xml.append("   </" + columnType + ">").append(EOLN);
        }

        xml.append("  </attributes>").append(EOLN);
    }
}
