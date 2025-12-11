/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.persistence.orm;

import static io.openliberty.data.internal.persistence.cdi.DataExtension.exc;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.persistence.DataProvider;
import io.openliberty.data.internal.persistence.Util;
import io.openliberty.data.internal.persistence.orm.Models.AccessType;
import io.openliberty.data.internal.persistence.orm.Models.Attribute;
import io.openliberty.data.internal.persistence.orm.Models.AttributeKind;
import io.openliberty.data.internal.persistence.orm.Models.Converter;
import io.openliberty.data.internal.persistence.orm.Models.EmbeddableRecord;
import io.openliberty.data.internal.persistence.orm.Models.EntityRecord;
import io.openliberty.data.internal.persistence.orm.Models.MappedSuperclass;
import jakarta.data.exceptions.MappingException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Parses entity(ies) and creates an Object-Relational Model in memory
 * that includes managed objects that do not have the Jakarta Persistence
 * annotations required to be automatically modeled by the underlying Jakarta
 * Persistence provider.
 *
 * This is an add-on feature of the Liberty Jakarta Data provider to support
 * record entities which cannot be annotated, and for users who write Repository
 * classes for java objects that comes from third party libraries which cannot
 * be annotated.
 */
public class EntityParser {
    private static final TraceComponent tc = Tr.register(EntityParser.class);

    // ORM sets
    private final Map<Class<?>, MappedSuperclass> mappedSuperclasses;
    private final Map<Class<?>, EntityRecord> entities;
    private final Map<Class<?>, EmbeddableRecord> embeddables;
    private final Map<Class<?>, Converter> converters;

    // Relationship tracking
    private final Map<Class<?>, Class<?>> entityToRecord;
    private final Map<Class<?>, Set<Class<?>>> entitiesSuperclasses;

    // ORM associated sets
    private final Set<Class<?>> convertibles;
    private final LinkedHashSet<String> tableNames;
    private final LinkedHashSet<String> classNames;

    // Global configurations
    private final DataProvider provider;
    private final String tablePrefix;

    // State controls flow from initialization to generation
    private boolean doneParsing;

    // Current entity being parsed and found idAttributes
    private Class<?> currentEntity;
    private Set<Attribute> idAttributes;

    /**
     * Construct a new entity parser.
     *
     * @param tablePrefix the table prefix or empty string if none.
     * @param provider    OSGi service representing this Data provider.
     */
    @Trivial
    public EntityParser(String tablePrefix, DataProvider provider) {
        this.mappedSuperclasses = new HashMap<>();
        this.entities = new HashMap<>();
        this.embeddables = new HashMap<>();
        this.converters = new HashMap<>();

        this.entityToRecord = new HashMap<>();
        this.entitiesSuperclasses = new HashMap<>();

        this.convertibles = new HashSet<>();
        this.tableNames = new LinkedHashSet<>();
        this.classNames = new LinkedHashSet<>();

        this.provider = provider;
        this.tablePrefix = tablePrefix;

        this.doneParsing = false;
    }

    // ENTRY POINTS

    /**
     * Parses an annotated entity and records
     * - converters/convertables
     * - table name
     * - class name
     *
     * @param annotatedEntity the entity class
     * @return Class the annotatedEntity
     */
    public Class<?> parseAnnotatedEntity(Class<?> annotatedEntity) {
        if (doneParsing) {
            // Internal exception
            throw new IllegalStateException("Attempted to parse an entity after generating mapping");
        }

        for (Class<?> superclass = annotatedEntity; //
                        superclass != null && superclass != Object.class; //
                        superclass = superclass.getSuperclass()) {
            parseAnnotatedObject(superclass);
        }

        return annotatedEntity;
    }

    /**
     * Parses an unannotated generated entity from a record and constructs an
     * orm model for that entity.
     *
     * @param record          the record class
     * @param generatedEntity the generated entity class
     * @return Class the record class
     */
    public Class<?> parseRecord(Class<?> record, Class<?> generatedEntity) {
        if (doneParsing) {
            // Internal exception
            throw new IllegalStateException("Attempted to parse an entity after generating mapping");
        }

        entityToRecord.put(generatedEntity, record);
        parse(generatedEntity, tablePrefix + record.getSimpleName());

        return record;
    }

    /**
     * Parses an unannotated entities found on a Repository interface and
     * constructs an orm model for that entity.
     *
     * @param entity the entity class
     * @return class the entity class
     */
    public Class<?> parseUnannotatedEntity(Class<?> entity) {
        if (doneParsing) {
            // Internal exception
            throw new IllegalStateException("Attempted to parse an entity after generating mapping");
        }

        parse(entity, tablePrefix + entity.getSimpleName());

        return entity;
    }

    // PARSER

    /**
     * Parses the entity and creates the orm of this entity.
     *
     * @param entity    the entity class
     * @param tableName the table name, including prefix
     */
    @Trivial
    private void parse(Class<?> entity, String tableName) {
        this.currentEntity = entity;
        this.idAttributes = new HashSet<>();

        for (Class<?> superclass = currentEntity; //
                        superclass != null && superclass != Object.class; //
                        superclass = superclass.getSuperclass()) {

            // Find annotated converters
            for (Convert convert : superclass.getAnnotationsByType(Convert.class)) {
                foundConverter(convert);
            }

            // Record entity and any embeddables found along the way
            if (superclass == currentEntity && !entities.containsKey(superclass)) {
                entities.put(superclass, new EntityRecord(superclass, tableName, finalizeAttributes(superclass, findAttributes(superclass))));
                continue;
            }

            // Record entity-mappedSuperclass relationship
            Set<Class<?>> superclasses = entitiesSuperclasses.get(currentEntity);
            if (superclasses == null)
                entitiesSuperclasses.put(currentEntity, superclasses = new HashSet<>());
            superclasses.add(superclass);

            // Record the mappedSuperclass and any embeddables found along the way
            if (!mappedSuperclasses.containsKey(superclass)) {
                mappedSuperclasses.put(superclass, new MappedSuperclass(superclass, finalizeAttributes(superclass, findAttributes(superclass))));
            }
        }

        verify();
    }

    // HELPER METHODS

    /**
     * Finds attributes on a class
     *
     * Attributes returned are incomplete which means they lack
     * any metadata, such as; the kind of attribute.
     *
     * @param c a class one of [ MappedSuperclass, Entity, Embeddable ]
     * @return set of incomplete attributes
     */
    private Set<Attribute> findAttributes(Class<?> c) {
        Set<Attribute> attributes = new HashSet<>();

        if (entityToRecord.containsKey(c)) {
            Class<?> r = entityToRecord.get(c);
            for (RecordComponent rc : r.getRecordComponents())
                attributes.add(new Attribute(rc.getType(), rc.getGenericType(), rc.getName(), AccessType.FIELD));
        } else if (c.isRecord()) {
            for (RecordComponent rc : c.getRecordComponents())
                attributes.add(new Attribute(rc.getType(), rc.getGenericType(), rc.getName(), AccessType.FIELD));
        } else {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isPublic(f.getModifiers())) {
                    attributes.add(new Attribute(f.getType(), f.getGenericType(), f.getName(), AccessType.FIELD));

                    for (Convert convert : f.getAnnotationsByType(Convert.class))
                        foundConverter(convert);
                }
            }

            try {
                PropertyDescriptor[] propertyDescriptors = Introspector //
                                .getBeanInfo(c).getPropertyDescriptors();
                if (propertyDescriptors != null)
                    for (PropertyDescriptor p : propertyDescriptors) {
                        if (p.getWriteMethod() != null) {
                            //Note: p.getName() utilizes Introspector.decapitalize method
                            //      which honors acryonyms like getURL/setURL -> URL (instead of uRL)
                            //TODO error handling if someone were to have setXXX() with no parameters
                            Type genericType = p.getWriteMethod().getGenericParameterTypes()[0];
                            attributes.add(new Attribute(p.getPropertyType(), genericType, p.getName(), AccessType.PROPERTY));
                        }

                        if (p.getReadMethod() != null)
                            for (Convert convert : p.getReadMethod().getAnnotationsByType(Convert.class))
                                foundConverter(convert);
                    }

            } catch (IntrospectionException x) {
                throw new MappingException(x);
            }
        }
        return attributes;
    }

    /**
     * Takes a set of incomplete attributes and:
     * - finds the most likely to be an ID
     * - finds the most likely to be a Version
     * - finds element collection attributes
     * - finds embedded attributes
     * ---- processes overrides
     * - finds embedded collection attributes
     * ---- processes overrides and collection ids
     * - finds basic attributes
     *
     * @param c           a class one of [ MappedSuperclass, Entity, Embeddable ]
     * @param incompletes the incomplete attributes of c
     * @return set of complete attributes
     */
    private Set<Attribute> finalizeAttributes(Class<?> entity, Set<Attribute> incompletes) {
        Attribute id = null;
        Attribute version = null;

        boolean rootHolder = isRootEntityOrMappedSuperclass(entity);

        if (rootHolder) {

            // Determine which attribute is the id and version (optional).
            // Id precedence:
            // (1) name is id, ignoring case.
            // (2) name ends with _id, ignoring case.
            // (3) name ends with Id or ID.
            // (4) type is UUID.
            // Version precedence (if also a valid version type):
            // (1) name is version, ignoring case.
            // (2) name is _version, ignoring case.
            int idPrecedence = 10;
            int vPrecedence = 10;
            for (Attribute attr : incompletes) {
                String name = attr.name();
                Class<?> type = attr.type();
                int len = name.length();

                if (idPrecedence > 1 &&
                    len >= 2 &&
                    name.regionMatches(true, len - 2, "id", 0, 2)) {
                    if (name.length() == 2) {
                        id = attr;
                        idPrecedence = 1;
                    } else if (idPrecedence > 2 &&
                               name.charAt(len - 3) == '_') {
                        id = attr;
                        idPrecedence = 2;
                    } else if (idPrecedence > 3 &&
                               name.charAt(len - 2) == 'I') {
                        id = attr;
                        idPrecedence = 3;
                    }
                } else if (idPrecedence > 4 && UUID.class.equals(type)) {
                    id = attr;
                    idPrecedence = 4;
                }

                if (vPrecedence > 1 &&
                    len == 7 &&
                    Util.VERSION_TYPES.contains(type) &&
                    "version".equalsIgnoreCase(name)) {
                    version = attr;
                    vPrecedence = 1;
                } else if (vPrecedence > 2 &&
                           len == 8 &&
                           Util.VERSION_TYPES.contains(type) &&
                           "_version".equalsIgnoreCase(name)) {
                    version = attr;
                    vPrecedence = 2;
                }
            }
        }

        for (Attribute attr : incompletes) {
            Class<?> type = attr.type();

            boolean isId = attr == id;
            boolean isVersion = attr == version;
            boolean isBasic = type.isPrimitive() || //
                              type.isInterface() || //
                              Serializable.class.isAssignableFrom(type);

            boolean isCollection = Collection.class.isAssignableFrom(type);
            Class<?> collectionType = isCollection ? //
                            (Class<?>) ((ParameterizedType) attr.genericType()).getActualTypeArguments()[0] : //
                            null;
            boolean isCollectionBasic = isCollection ? collectionType.isPrimitive() || //
                                                       collectionType.isInterface() || //
                                                       Serializable.class.isAssignableFrom(collectionType) : false;

            final AttributeKind kind;
            if (isCollection) {
                attr.setCollectionType(collectionType);
                if (isCollectionBasic)
                    kind = AttributeKind.BASIC$ELEMENT_COLLECTION;
                else
                    kind = AttributeKind.EMBEDDED$ELEMENT_COLLECTION;
            } else if (isBasic) {
                if (isId)
                    kind = AttributeKind.ID;
                else if (isVersion)
                    kind = AttributeKind.VERSION;
                else
                    kind = AttributeKind.BASIC;
            } else {
                if (isId)
                    kind = AttributeKind.EMBEDDED_ID;
                else
                    kind = AttributeKind.EMBEDDED;
            }
            attr.setKind(kind);

            // overrides should be in embeddable attributes in root entity or root mapped superclass
            if (attr.isEmbedded()) {
                // Build the attributes for the embeddable type itself
                Set<Attribute> embAttrs = finalizeAttributes(type, findAttributes(type));

                // Record relationship and embeddable definition
                embeddables.put(type, new EmbeddableRecord(type, embAttrs));

                // Only root entity / mapped superclass gets attribute-override entries
                if (rootHolder) {
                    attr.setOverrides(buildOverridesForType(type));
                }
            }

            if (attr.isEmbeddedCollection()) {
                // Build the attributes for the embeddable type itself
                Set<Attribute> embAttrs = finalizeAttributes(collectionType, findAttributes(collectionType));

                // Record relationship and embeddable definition
                embeddables.put(collectionType, new EmbeddableRecord(collectionType, embAttrs));

                if (rootHolder) {
                    attr.setOverrides(buildOverridesForType(collectionType));
                    attr.setCollectionId(id);
                }
            }

            if (attr.isId()) {
                idAttributes.add(attr);
            }
        }

        return new TreeSet<Attribute>(incompletes);
    }

    /**
     * Returns true if the given holder is the current entity or one of its
     * mapped superclasses. In that case, overrides belong on its embedded
     * attributes.
     */
    @Trivial
    private boolean isRootEntityOrMappedSuperclass(Class<?> holder) {
        if (currentEntity == null || holder == null) {
            return false;
        }
        // holder == currentEntity  -> entity itself
        // holder.isAssignableFrom(currentEntity) -> mapped superclass of entity
        return holder == currentEntity || holder.isAssignableFrom(currentEntity);
    }

    /**
     * Builds the set of attribute-override entries for a given embeddable type.
     * The returned Attributes have their names flattened using dot notation:
     * Coordinate(x,y) -> x, y
     * Side(a:Coordinate,b:Coordinate) -> a.x, a.y, b.x, b.y
     */
    private SortedSet<Attribute> buildOverridesForType(Class<?> embeddableType) {
        SortedSet<Attribute> result = new TreeSet<>();
        buildOverridesForType(embeddableType, "", result, new HashSet<>());
        return result;
    }

    private void buildOverridesForType(Class<?> type,
                                       String prefix,
                                       SortedSet<Attribute> result,
                                       Set<Class<?>> visiting) {

        // Guard against accidental cycles
        if (!visiting.add(type)) {
            return;
        }

        // Reuse the embeddable we already found
        Set<Attribute> attrs;
        if (embeddables.containsKey(type)) {
            attrs = Collections.unmodifiableSet(embeddables.get(type).attributes());
        } else {
            // TODO internal error - embeddable should have already been parsed
            attrs = Set.of();
        }

        for (Attribute attr : attrs) {
            String namePrefix = prefix.isEmpty() ? "" : prefix + ".";

            if (attr.isEmbeddedCollection()) {
                // Collection of embeddables: recurse into the collectionType
                buildOverridesForType(attr.collectionType(), namePrefix + attr.name(), result, visiting);
            } else if (attr.isEmbedded()) {
                // Single embedded object: recurse into its type
                buildOverridesForType(attr.type(), namePrefix + attr.name(), result, visiting);
            } else {
                // Leaf attribute (basic or collection of basic): add as override
                String overrideName = namePrefix + attr.name();
                Class<?> leafType = attr.isCollection() ? attr.collectionType() : attr.type();
                // For overrides we only really need the type and name; access=FIELD is fine
                result.add(new Attribute(leafType, attr.genericType(), overrideName, AccessType.FIELD));
            }
        }

        visiting.remove(type);
    }

    /**
     * Records a converter for the orm.
     * Records the converter entity attribute type as a convertable type
     * Also analyzes the database column type of the AttributeConverter interface
     * to determine if the type is supported by the underlying Jakarta Persistence provider.
     *
     * @param convert the convert annotation
     * @throws MappingException if the conversion is unsupported
     */
    @Trivial
    private void foundConverter(Convert convert) {
        if (convert.converter() != null && convert.converter() != AttributeConverter.class) {
            Class<?> converterType = convert.converter();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.entry(tc, "foundConverter", converterType);

            Converter converter = new Converter(convert.converter());

            if (!converters.containsKey(converterType)) {
                for (Class<?> c = converterType; c != null; c = c.getSuperclass())
                    for (Type ifc : c.getGenericInterfaces())
                        if (ifc instanceof ParameterizedType type &&
                            ifc.getTypeName().startsWith(Util.ATTR_CONVERTER_CLASS_NAME)) {
                            Type[] typeParams = type.getActualTypeArguments();
                            if (Util.UNSUPPORTED_ATTR_TYPES.contains(typeParams[1]))
                                throw exc(MappingException.class,
                                          "CWWKD1111.unsupported.convert",
                                          converterType.getName(),
                                          typeParams[0].getTypeName(),
                                          typeParams[1].getTypeName(),
                                          Util.SUPPORTED_TEMPORAL_TYPES,
                                          Util.SUPPORTED_BASIC_TYPES);

                            if (typeParams[0] instanceof Class)
                                convertibles.add((Class<?>) typeParams[0]);
                        }
                converters.put(converterType, converter);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "foundConverter");
        }
    }

    @Trivial
    private void verify() {
        if (idAttributes.isEmpty()) {
            Class<?> recordEntityClass = entityToRecord.get(currentEntity);
            if (recordEntityClass == null) {
                throw exc(MappingException.class,
                          "CWWKD1122.entity.lacks.id",
                          currentEntity.getName(),
                          provider.compat.persistenceFeatureName());
            } else { // a Java record entity
                String recordComponentNames = Stream //
                                .of(recordEntityClass.getRecordComponents()) //
                                .map(RecordComponent::getName) //
                                .reduce("", (s, n) -> s.length() == 0 //
                                                ? n //
                                                : (s + ", " + n));
                throw exc(MappingException.class,
                          "CWWKD1121.record.lacks.id",
                          recordEntityClass.getName(),
                          recordComponentNames);
            }
        }

        if (idAttributes.size() > 1) {
            EntityRecord invalid = entities.get(currentEntity);
            Set<Class<?>> supers = entitiesSuperclasses.get(currentEntity);
            Set<MappedSuperclass> invalidSupers = supers == null || supers.isEmpty() ? //
                            Set.of() : //
                            supers.stream() //
                                            .map(c -> mappedSuperclasses.get(c))//
                                            .collect(Collectors.toSet());

            //TODO NLS
            throw new MappingException("The entity " + invalid + " had more than one id attribute due to a combination of the entity's own attributes"
                                       + " and the attributes of the entity's mapped superclasses " + invalidSupers + ". "
                                       + "The id attributes are: " + idAttributes);
        }
    }

    @Trivial
    private void parseAnnotatedObject(Class<?> c) {
        if (classNames.contains(c.getName())) {
            return;
        }

        if (c.isAnnotationPresent(Entity.class)) {
            String cn, tn;

            classNames.add(cn = c.getName());
            if (c.isAnnotationPresent(Table.class))
                tableNames.add(tn = c.getAnnotation(Table.class).name());
            else
                tableNames.add(tn = c.getSimpleName());

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "entity class " + cn + " will use table " + tn);
        }

        for (Convert convert : c.getAnnotationsByType(Convert.class))
            if (convert.converter() != null && convert.converter() != AttributeConverter.class)
                foundConverter(convert);

        for (Field f : c.getDeclaredFields()) {
            forEmbeddable(f).ifPresent(emb -> parseAnnotatedObject(emb));
            forMapping(f).ifPresent(entity -> parseAnnotatedObject(entity));
            for (Convert convert : f.getAnnotationsByType(Convert.class))
                if (convert.converter() != null && convert.converter() != AttributeConverter.class)
                    foundConverter(convert);
        }

        for (Method m : c.getDeclaredMethods()) {
            forEmbeddable(m).ifPresent(emb -> parseAnnotatedObject(emb));
            forMapping(m).ifPresent(entity -> parseAnnotatedObject(entity));
            for (Convert convert : m.getAnnotationsByType(Convert.class))
                if (convert.converter() != null && convert.converter() != AttributeConverter.class)
                    foundConverter(convert);
        }
    }

    @Trivial
    private Optional<Class<?>> forEmbeddable(Field f) {
        if (f.isAnnotationPresent(Embedded.class) ||
            f.isAnnotationPresent(EmbeddedId.class)) {
            if (f.getType().isAnnotationPresent(Embeddable.class)) {
                return Optional.of(f.getType());
            }
            return Optional.of(getEmbeddableClass(f.getGenericType()));
        }
        return Optional.empty();
    }

    @Trivial
    private Optional<Class<?>> forMapping(Field f) {
        if (f.isAnnotationPresent(ManyToOne.class) ||
            f.isAnnotationPresent(OneToOne.class)) {
            if (f.getType().isAnnotationPresent(Entity.class)) {
                return Optional.of(f.getType());
            }
        }

        if (f.isAnnotationPresent(ManyToMany.class) ||
            f.isAnnotationPresent(OneToMany.class)) {
            return Optional.of(getEntityClass(f.getGenericType()));
        }

        return Optional.empty();
    }

    @Trivial
    private Optional<Class<?>> forEmbeddable(Method m) {
        if (m.isAnnotationPresent(Embedded.class) ||
            m.isAnnotationPresent(EmbeddedId.class)) {
            if (m.getReturnType().isAnnotationPresent(Embeddable.class)) {
                return Optional.of(m.getReturnType());
            }
            return Optional.of(getEmbeddableClass(m.getGenericReturnType()));
        }
        return Optional.empty();
    }

    @Trivial
    private Optional<Class<?>> forMapping(Method m) {
        if (m.isAnnotationPresent(ManyToOne.class) ||
            m.isAnnotationPresent(OneToOne.class)) {
            if (m.getReturnType().isAnnotationPresent(Entity.class)) {
                return Optional.of(m.getReturnType());
            }
        }

        if (m.isAnnotationPresent(ManyToMany.class) ||
            m.isAnnotationPresent(OneToMany.class)) {
            return Optional.of(getEntityClass(m.getGenericReturnType()));
        }

        return Optional.empty();
    }

    @Trivial
    private Class<?> getEmbeddableClass(java.lang.reflect.Type type) {
        if (type instanceof ParameterizedType) {
            java.lang.reflect.Type[] typeParams = //
                            ((ParameterizedType) type).getActualTypeArguments();
            for (java.lang.reflect.Type t : typeParams)
                if (t instanceof Class && ((Class<?>) t).isAnnotationPresent(Embeddable.class)) {
                    return (Class<?>) t;
                }
        }
        return null;
    }

    @Trivial
    private Class<?> getEntityClass(java.lang.reflect.Type type) {
        if (type instanceof ParameterizedType) {
            java.lang.reflect.Type[] typeParams = //
                            ((ParameterizedType) type).getActualTypeArguments();
            for (java.lang.reflect.Type t : typeParams)
                if (t instanceof Class && ((Class<?>) t).isAnnotationPresent(Entity.class)) {
                    return (Class<?>) t;
                }
        }
        return null;
    }

    // GENERATORS

    public List<String> generateView() {
        doneParsing = true;

        View view = new View(mappedSuperclasses.size() + entities.size() +
                             embeddables.size() + converters.size());

        for (MappedSuperclass sc : new TreeSet<>(mappedSuperclasses.values())) {
            view.mappedSuperclass(sc);
        }

        for (EntityRecord er : new TreeSet<>(entities.values())) {
            view.entity(er);
        }

        for (EmbeddableRecord emb : new TreeSet<>(embeddables.values())) {
            view.embeddable(emb);
        }

        for (Converter con : new TreeSet<>(converters.values())) {
            view.converter(con);
        }

        return view.getAll();
    }

    public LinkedHashSet<String> getClassNames() {
        doneParsing = true;
        return classNames;
    }

    public LinkedHashSet<String> getTableNames() {
        doneParsing = true;
        return tableNames;
    }

    public Set<Class<?>> getConvertibles() {
        doneParsing = true;
        return convertibles;
    }
}
