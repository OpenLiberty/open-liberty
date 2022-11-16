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

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V17;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
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

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.persistence.DatabaseStore;
import com.ibm.wsspi.persistence.InMemoryMappingFile;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;

import jakarta.data.exceptions.MappingException;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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
    private static final String JAKARTA_DATA_DIR = File.separator + "jakartaData" + File.separator;

    private final String databaseId;
    private final List<Class<?>> entities = new ArrayList<>();
    final ConcurrentHashMap<Class<?>, CompletableFuture<EntityInfo>> entityInfoMap = new ConcurrentHashMap<>();
    private final ClassLoader loader;
    private final ClassDefiner classDefiner = new ClassDefiner();

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

    /**
     * It's more likely for a name ending with "Id" or "ID"
     * to be an id than a name ending with "id",
     * unless the name is "id".
     *
     * Precedence is:
     * Select the field/parameter/method that is annotated with @Id, (1)
     * or lacking that is named Id, or ID, or id, (2)
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

            if (field.getAnnotation(Id.class) != null)
                return name;

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

            if (method.getAnnotation(Id.class) != null)
                return name;

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
        // TODO: ugly hack to make temporary progress in other areas.
        id = "id";

        if (id == null)
            throw new MappingException(entityClass + " lacks public field with @Id or of the form *ID"); // TODO
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
            Set<String> entityClassNames = new HashSet<>(entities.size() * 2);

            ArrayList<InMemoryMappingFile> generatedEntities = new ArrayList<InMemoryMappingFile>();

            // List of classes to inspect for the above
            Queue<Class<?>> annotatedEntityClassQueue = new LinkedList<>();

            Map<Class<?>, String> keyAttributeNames = new HashMap<>();

            // XML to make all other classes into JPA entities:
            ArrayList<String> entityClassInfo = new ArrayList<>(entities.size());

            Queue<Class<?>> embeddableTypesQueue = new LinkedList<>();

            for (Class<?> c : entities) {
                if (c.isRecord()) {
                    if (c.isRecord()) {
                        List<Class<?>> embeddableTypes = new ArrayList<>();
                        String entityClassName = c.getName() + "Record"; // TODO: come up with a more unique identifier than "Record".
                        byte[] generatedEntityBytes = generateEntityClassBytes(c, entityClassName);
                        generatedEntities.add(new InMemoryMappingFile(generatedEntityBytes, entityClassName.replace('.', '/') + ".class"));
                        Class<?> generatedEntity = classDefiner.findLoadedOrDefineClass(loader, entityClassName, generatedEntityBytes);
                        String keyAttributeName = getID(c);
//                        keyAttributeNames.put(generatedEntity, keyAttributeName);

                        StringBuilder xml = new StringBuilder(500).append(" <entity class=\"").append(generatedEntity.getName()).append("\">").append(EOLN);

                        xml.append("  <table name=\"").append(tablePrefix).append(generatedEntity.getSimpleName()).append("\"/>").append(EOLN);
//                        Entity entity = c.getAnnotation(Entity.class);
//                        StringBuilder xml = new StringBuilder(500).append(" <entity class=\"" + generatedEntity.getName() + "\">").append(EOLN);
//
//                        if (c.getAnnotation(Inheritance.class) == null) {
//                            String tableName = tablePrefix + (entity == null || entity.value() == null ? c.getSimpleName() : entity.value());
//                            xml.append("  <table name=\"" + tableName + "\"/>").append(EOLN);
//                        } else {
//                            xml.append("  <inheritance strategy=\"SINGLE_TABLE\"/>").append(EOLN);
//                        }

//                        DiscriminatorValue discriminatorValue = c.getAnnotation(DiscriminatorValue.class);
//                        if (discriminatorValue != null)
//                            xml.append("  <discriminator-value>").append(discriminatorValue.value()).append("</discriminator-value>").append(EOLN);
//
//                        DiscriminatorColumn discriminatorColumn = c.getAnnotation(DiscriminatorColumn.class);
//                        if (discriminatorColumn != null)
//                            xml.append("  <discriminator-column name=\"").append(discriminatorColumn.value()).append("\"/>").append(EOLN);

                        xml.append("  <attributes>").append(EOLN);

                        for (Field component : c.getDeclaredFields()) {
                            // for (Field field : fields) {
                            Id id = component.getAnnotation(Id.class);
//                            Column column = component.getAnnotation(Column.class);
                            GeneratedValue generated = component.getAnnotation(GeneratedValue.class);
                            Embeddable embeddable = component.getType().getAnnotation(Embeddable.class);

                            String attributeName = component.getName();
//                            String columnName = column == null || column.length() == 0 ? //
//                                            id == null || id.length() == 0 ? null : id.value() : //
//                                            column.value();
                            boolean isCollection = Collection.class.isAssignableFrom(component.getType());

                            String columnType;
                            if (embeddable == null) {
                                columnType = id != null || keyAttributeName.equals(attributeName) ? "id" : //
                                                "version".equals(attributeName) ? "version" : //
                                                                isCollection ? "element-collection" : //
                                                                                "basic";
                            } else {
                                columnType = "embedded";
                                embeddableTypes.add(component.getType());
                            }

                            xml.append("   <" + columnType + " name=\"" + attributeName + "\" attribute-type=\"" + component.getType().getName() + "\">").append(EOLN);
//                            if (columnName != null)
//                                xml.append("    <column name=\"" + columnName + "\"/>").append(EOLN);
                            if (generated != null)
                                xml.append("    <generated-value strategy=\"" + generated.strategy().name() + "\"/>").append(EOLN);
                            xml.append("   </" + columnType + ">").append(EOLN);
                        }

                        xml.append("  </attributes>").append(EOLN).append(" </entity>").append(EOLN);

                        entityClassInfo.add(xml.toString());
                    }

                    // This code should replace the code above once we can better handle attributes such as id and generation strategy.
                    // Currently these annotations are being lost in the switch to the ASM generated entity class.
//                    ///////////////////////////////////////////
//                    String entityClassName = c.getName() + "Record";
//                    byte[] generatedEntityBytes = generateEntityClassBytes(c, entityClassName);
//                    generatedEntities.add(new InMemoryMappingFile(generatedEntityBytes, entityClassName.replace('.', '/') + ".class"));
//                    Class<?> generatedEntity = classDefiner.findLoadedOrDefineClass(loader, entityClassName, generatedEntityBytes);
//
//                    StringBuilder xml = new StringBuilder(500).append(" <entity class=\"").append(generatedEntity.getName()).append("\">").append(EOLN);
//
//                    xml.append("  <table name=\"").append(tablePrefix).append(generatedEntity.getSimpleName()).append("\"/>").append(EOLN);
//
//                    writeAttributes(xml, generatedEntity, false, embeddableTypesQueue);
//
//                    xml.append(" </entity>").append(EOLN);
//
//                    entityClassInfo.add(xml.toString());

                } else if (c.isAnnotationPresent(Entity.class)) {
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
            // TODO: Do records need to be considered here?
            for (Class<?> c; (c = annotatedEntityClassQueue.poll()) != null;) {
                if (c.isRecord()) { //TODO: Make sure this is needed.
                    continue;
                }
                if (entityClassNames.add(c.getName())) {
                    Class<?> e;
                    for (Field f : c.getFields())
                        if (f.getType().isAnnotationPresent(Entity.class) && !(f.getType().isRecord())) //TODO: Make sure isRecord() is needed.
                            annotatedEntityClassQueue.add(f.getType());
                        else if ((e = getEntityClass(f.getGenericType())) != null)
                            annotatedEntityClassQueue.add(e);
                    for (Method m : c.getMethods())
                        if (m.getReturnType().isAnnotationPresent(Entity.class))
                            annotatedEntityClassQueue.add(m.getReturnType());
                        else if ((e = getEntityClass(m.getGenericReturnType())) != null)
                            annotatedEntityClassQueue.add(e);
                }
            }

            Map<String, Object> properties = new HashMap<>();

            properties.put("io.openliberty.persistence.internal.entityClassInfo",
                           entityClassInfo.toArray(new String[entityClassInfo.size()]));

            if (!generatedEntities.isEmpty()) {
                properties.put("io.openliberty.persistence.internal.generatedEnties", generatedEntities);
            }

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
                    } else if (Collection.class.isAssignableFrom(attr.getJavaType())) {
                        // collection attribute that is not annotated with ElementCollection
                        collectionElementTypes.put(attributeName, Object.class);
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
     * @param recordClass
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

    private byte[] generateEntityClassBytes(Class<?> recordClass, String entityClassName) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        String internal_entityClassName = entityClassName.replace('.', '/');

        // Define the Entity Class object
        cw.visit(V17, ACC_PUBLIC + ACC_SUPER,
                 internal_entityClassName,
                 null,
                 "java/lang/Object",
                 null);

        // Define the source code file and debug settings
        String sourceFileName = entityClassName.substring(entityClassName.lastIndexOf(".") + 1) + ".java";
        cw.visitSource(sourceFileName, null);

        // Add default constructor
        addDefaultCtor(cw, null);

        FieldVisitor fv;
        for (RecordComponent component : recordClass.getRecordComponents()) {
            // None of this code seems to find annotations on the record. Tried a bunch of other solutions as well...
            // Decompiling a sample record class shows the annotations being present on the field declarations.
//            Generated generated = component.getAnnotation(Generated.class);
//            Field[] fields = recordClass.getDeclaredFields();

//            Annotation[] componentAnnotations;
//            for (var f : recordClass.getDeclaredFields()) {
//                if (f.getName().equals(component.getName())) {
//                    componentAnnotations = f.getAnnotations();
//                }
//            }

            String componentName = component.getName();
            String typeDesc = org.objectweb.asm.Type.getDescriptor(component.getType());
//            String annotationDesc = org.objectweb.asm.Type.getDescriptor(componentAnnotations[i].getType());
            // --------------------------------------------------------------------
            // public <FieldType> <FieldName>;
            // --------------------------------------------------------------------
            if (trace && tc.isEntryEnabled())
                Tr.debug(tc, "     " + "adding field : " +
                             componentName + " " +
                             typeDesc);

            fv = cw.visitField(ACC_PUBLIC, componentName,
                               typeDesc,
                               null, null);
            GeneratedValue generated = component.getAnnotation(GeneratedValue.class);
//            for (Annotation annotation : componentAnnotations) {
//                String annotationDesc = org.objectweb.asm.Type.getType(annotation.annotationType()).getDescriptor();
//                AnnotationVisitor av = fv.visitAnnotation(annotationDesc, true);
//            }

            fv.visitEnd();

            // --------------------------------------------------------------------
            // public setter...
            // --------------------------------------------------------------------
//            if (trace && tc.isEntryEnabled())
//                Tr.debug(tc, "     " + "adding field : " +
//                             component.getName() + " " +
//                             component.getType().descriptorString());
            String methodName = "set" + componentName.substring(0, 1).toUpperCase() + componentName.substring(1);
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, "(" + typeDesc + ")V", null, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(org.objectweb.asm.Type.getType(component.getType()).getOpcode(ILOAD), 1);
            mv.visitFieldInsn(PUTFIELD, internal_entityClassName, componentName, typeDesc);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
//            mv.visitEnd();

            // --------------------------------------------------------------------
            // public getter...
            // --------------------------------------------------------------------
            methodName = "get" + componentName.substring(0, 1).toUpperCase() + componentName.substring(1);
            mv = cw.visitMethod(ACC_PUBLIC, methodName, "()" + typeDesc, null, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, internal_entityClassName, componentName, typeDesc);
            mv.visitInsn(org.objectweb.asm.Type.getType(component.getType()).getOpcode(Opcodes.IRETURN));
            mv.visitMaxs(1, 1);
//            mv.visitEnd();
        }

        // Add constructor that will invoke all setters with data from a Record
        addRecordCtor(recordClass, internal_entityClassName, cw, null);

        //
        //end
        //

        // Mark the end of the generated wrapper class
        cw.visitEnd();

        // Dump the class bytes out to a byte array.
        byte[] classBytes = cw.toByteArray();
//        generatedEntities.add(new InMemoryMappingFile(classBytes, entityClassName));

        if (trace && tc.isEntryEnabled()) {
            if (tc.isDebugEnabled())
                writeToClassFile(entityClassName, classBytes);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "generateClassBytes: " + classBytes.length + " bytes");
        }
        return classBytes;
    }

    /**
     * Adds the default (no arg) constructor. <p>
     *
     * @param cw     ASM ClassWriter to add the constructor to.
     * @param parent fully qualified name of the parent class
     *                   with '/' as the separator character
     *                   (i.e. internal name).
     **/
    private static void addDefaultCtor(ClassWriter cw, String parent) {
        MethodVisitor mv;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "    " + "adding method : <init> ()V");

        // -----------------------------------------------------------------------
        // public <Class Name>()
        // {
        // -----------------------------------------------------------------------
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();

        // -----------------------------------------------------------------------
        //    super();
        // -----------------------------------------------------------------------
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");

        // -----------------------------------------------------------------------
        // }
        // -----------------------------------------------------------------------
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     *
     * @param recordClass
     * @param internalEntityClassName
     * @param cw
     * @param parent
     */

    private static void addRecordCtor(Class<?> recordClass, String internalEntityClassName, ClassWriter cw, String parent) {
        org.objectweb.asm.commons.Method constructor = org.objectweb.asm.commons.Method.getMethod("void <init> (" + recordClass.getTypeName() + ")");
        GeneratorAdapter mg = new GeneratorAdapter(ACC_PUBLIC, constructor, null, null, cw);
        mg.loadThis();
        mg.invokeConstructor(org.objectweb.asm.Type.getType(Object.class), org.objectweb.asm.commons.Method.getMethod("void <init> ()"));

        for (RecordComponent component : recordClass.getRecordComponents()) {
            String componentName = component.getName();
            String typeDesc = component.getType().getTypeName();
            String methodName = "set" + componentName.substring(0, 1).toUpperCase() + componentName.substring(1);
            int componentValue = mg.newLocal(org.objectweb.asm.Type.getType(component.getType()));
            mg.loadArg(0);
            mg.invokeVirtual(org.objectweb.asm.Type.getType(recordClass),
                             org.objectweb.asm.commons.Method.getMethod(typeDesc + " " + componentName + " ()"));

            mg.storeLocal(componentValue);

            mg.loadThis();
            mg.loadLocal(componentValue);
            mg.invokeVirtual(org.objectweb.asm.Type.getObjectType(internalEntityClassName),
                             org.objectweb.asm.commons.Method.getMethod("void " + methodName + " (" + typeDesc + ")"));
        }
        mg.returnValue();
        mg.endMethod();
    }

    /**
     * Writes the in memory bytecode bytearray for a generated class
     * out to a .class file with the correct class name and in the
     * correct package directory structure. <p>
     *
     * This method is useful for debug, to determine if classes
     * are being generated properly, and may also be useful
     * when it is required to make the classes available on a
     * client without using the Remote ByteCode Server. <p>
     *
     * @param internalClassName fully qualified name of the class
     *                              with '/' as the separator.
     * @param classBytes        bytearray of the class bytecodes.
     **/
    static void writeToClassFile(final String internalClassName,
                                 final byte[] classBytes) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "writeToClassFile (" + internalClassName + ", " +
                         ((classBytes == null) ? "null" : (classBytes.length + " bytes")) + ")");
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws Exception {
                    String fileName = TrConfigurator.getLogLocation() + JAKARTA_DATA_DIR + internalClassName + ".class";
                    File file = new File(fileName);
                    File directory = file.getParentFile();
                    directory.mkdirs();
                    FileOutputStream classFile = new FileOutputStream(file);
                    classFile.write(classBytes);
                    classFile.flush();
                    classFile.close();
                    return null;
                }
            });

        } catch (PrivilegedActionException paex) {
            FFDCFilter.processException(paex.getCause(), EntityDefiner.class.getName() + ".writeToClassFile", "674");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "writeToClassFile failed for class " + internalClassName +
                             " : " + paex.getCause().getMessage());
        } catch (Throwable ex) {
            FFDCFilter.processException(ex, EntityDefiner.class.getName() + ".writeToClassFile", "463");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "writeToClassFile failed for class " + internalClassName +
                             " : " + ex.getMessage());
        }
    }
}
