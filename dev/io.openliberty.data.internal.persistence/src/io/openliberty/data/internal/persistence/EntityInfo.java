/*******************************************************************************
 * Copyright (c) 2022,2025 IBM Corporation and others.
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

import static io.openliberty.data.internal.persistence.cdi.DataExtension.exc;

import java.io.PrintWriter;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.util.UUID;

import jakarta.data.exceptions.MappingException;
import jakarta.persistence.Inheritance;

/**
 * Entity information
 */
public class EntityInfo {

    /**
     * Suffix for generated record class names. The name used for a generated
     * record entity class is: [RecordName][RECORD_ENTITY_SUFFIX]
     */
    public static final String RECORD_ENTITY_SUFFIX = "Entity";

    /**
     * Constant to use in place of an entity name to indicate that processing of
     * entity information has failed for an entity.
     */
    static final String FAILED = "ERROR!";

    // properly cased/qualified JPQL attribute name --> accessor methods or fields (multiple in the case of embeddable)
    final Map<String, List<Member>> attributeAccessors;

    // lower case attribute name --> properly cased/qualified JPQL attribute name
    final Map<String, String> attributeNames;

    /**
     * Names of attributes to use for entity update,
     * or null if em.merge must be used instead.
     * Excludes id and version.
     * Excludes inner relation attributes, such as location.address
     * when there is also a location.address.zipcode
     */
    final SortedSet<String> attributeNamesForEntityUpdate;

    // properly cased/qualified JPQL attribute name --> type
    final SortedMap<String, Class<?>> attributeTypes;

    final EntityManagerBuilder builder;

    // properly cased/qualified JPQL attribute name --> type of collection
    final Map<String, Class<?>> collectionElementTypes;

    final Class<?> entityClass; // will be a generated class for entity records
    final Class<?> idType; // type of the id, which could be a JPA IdClass for composite ids
    final SortedMap<String, Member> idClassAttributeAccessors; // null if no IdClass
    final boolean inheritance;
    final String name; // entity name to use in query language. If a record, the name will be [RecordName]Entity.
    final Class<?> recordClass; // null if not a record
    final String versionAttributeName; // null if unversioned

    // embeddable class -> fully qualified attribute names of embeddable, or
    // one-to-one entity class -> fully qualified attribute names of one-to-one entity, or
    // many-to-one entity class -> fully qualified attribute names of many-to-one entity
    final Map<Class<?>, List<String>> relationAttributeNames;

    EntityInfo(String entityName,
               Class<?> entityClass,
               Class<?> recordClass,
               Map<String, List<Member>> attributeAccessors,
               Map<String, String> attributeNames,
               SortedSet<String> attributeNamesForUpdate,
               SortedMap<String, Class<?>> attributeTypes,
               Map<String, Class<?>> collectionElementTypes,
               Map<Class<?>, List<String>> relationAttributeNames,
               Class<?> idType,
               SortedMap<String, Member> idClassAttributeAccessors,
               String versionAttributeName,
               EntityManagerBuilder entityManagerBuilder) {
        this.name = entityName;
        this.builder = entityManagerBuilder;
        this.entityClass = entityClass;
        this.attributeAccessors = attributeAccessors;
        this.attributeNames = attributeNames;
        this.attributeNamesForEntityUpdate = attributeNamesForUpdate;
        this.attributeTypes = attributeTypes;
        this.collectionElementTypes = collectionElementTypes;
        this.relationAttributeNames = relationAttributeNames;
        this.idType = idType;
        this.idClassAttributeAccessors = idClassAttributeAccessors;
        this.recordClass = recordClass;
        this.versionAttributeName = versionAttributeName;

        inheritance = entityClass.getAnnotation(Inheritance.class) != null;

        validate();
    }

    @Trivial
    Collection<String> getAttributeNames() {
        return attributeNames.values();
    }

    /**
     * Generates example method names for Query by Method Name using attribute names/types for this entity.
     *
     * @return list of example method names.
     */
    List<String> getExampleMethodNames() {
        List<String> examples = new ArrayList<>(5);
        String[] prefixes = { "find", "delete", "count", "exists" };
        String[] numSuffixes = { "LessThanEqual(max)", "Between(min, max)", "GreaterThan(exclusiveMin)", "NotIn(setOfValues)" };
        String[] strSuffixes = { "StartsWith(prefix)", "IgnoreCaseContains(pattern)", "EndsWith(suffix)", "NotLike(pattern)" };
        int b = 0, e = 0, n = 0, p = 0, s = 0;
        for (Map.Entry<String, Class<?>> attrClass : attributeTypes.entrySet()) {
            String attrName = attrClass.getKey();
            Class<?> attrType = attrClass.getValue();
            if (attrName.length() > 2
                && !attrName.toLowerCase().contains("version")
                && attrName.indexOf('.') < 0 && attrName.indexOf('_') < 0)
                if (CharSequence.class.isAssignableFrom(attrType))
                    examples.add(prefixes[p++] + "By" +
                                 Character.toUpperCase(attrName.charAt(0)) + attrName.substring(1) +
                                 strSuffixes[s++]);
                else if (boolean.class.equals(attrType) || Boolean.class.equals(attrType))
                    examples.add(prefixes[p++] + "By" +
                                 Character.toUpperCase(attrName.charAt(0)) + attrName.substring(1) +
                                 (b++ % 2 == 0 ? "False()" : "True()"));
                else if (attrType.isPrimitive()
                         || Number.class.isAssignableFrom(attrType)
                         || Temporal.class.isAssignableFrom(attrType))
                    examples.add(prefixes[p++] + "By" +
                                 Character.toUpperCase(attrName.charAt(0)) + attrName.substring(1) +
                                 numSuffixes[n++]);
                else if (attrType.isEnum())
                    examples.add(prefixes[p++] + "By" +
                                 Character.toUpperCase(attrName.charAt(0)) + attrName.substring(1) +
                                 (e++ % 2 == 0 ? "NotIn(setOfValues)" : "In(setOfValues)"));
            if (p >= 4)
                break;
        }
        if (p == 0) {
            examples.add("findById(id)");
            examples.add("deleteByIdNotIn(setOfValues)");
        }
        return examples;
    }

    /**
     * Entity class (non-generated) or entity record class.
     *
     * @return the entity class (non-generated) or entity record class.
     */
    @Trivial
    Class<?> getType() {
        return recordClass == null ? entityClass : recordClass;
    }

    /**
     * Write information about this instance to the introspection file for
     * Jakarta Data.
     *
     * @param writer writes to the introspection file.
     * @param indent indentation for lines.
     */
    @Trivial
    public void introspect(PrintWriter writer, String indent) {
        writer.println(indent + "EntityInfo@" + Integer.toHexString(hashCode()));
        writer.println(indent + "  name: " + name);
        writer.println(indent + "  entity class: " + entityClass.getName());
        writer.println(indent + "  record class: " +
                       (recordClass == null ? null : recordClass.getName()));
        writer.println(indent + "  builder: " + builder);
        writer.println(indent + "  idType: " +
                       (idType == null ? null : idType.getName()));
        if (idClassAttributeAccessors != null)
            idClassAttributeAccessors.forEach((idAttrName, member) -> {
                writer.println(indent + "  id attribute: " + idAttrName);
                writer.println(indent + "    accessor: " + member);
            });
        writer.println(indent + "  version attribute: " + versionAttributeName);
        writer.println(indent + "  attribute types");
        attributeTypes.forEach((name, type) -> {
            writer.println(indent + "    " + name + ": " + type.getTypeName());
        });
        if (!collectionElementTypes.isEmpty()) {
            writer.println(indent + "  collection types");
            collectionElementTypes.forEach((name, type) -> {
                writer.println(indent + "    " + name + ": " + type.getTypeName());
            });
        }
        writer.println(indent + "  attribute accessors");
        attributeAccessors.forEach((name, accessors) -> {
            writer.print(indent + "    " + name + ": ");
            writer.println(accessors.size() == 1 ? accessors.get(0) : accessors);
        });
        writer.println(indent + "  lower case attribute name to JPQL attribute name:");
        attributeNames.forEach((lower, name) -> {
            writer.println(indent + "    " + lower + " -> " + name);
        });
        if (!relationAttributeNames.isEmpty()) {
            writer.println(indent + "    relation attributes:");
            relationAttributeNames.forEach((relationClass, relAttrNames) -> {
                writer.println(indent + "    " + relationClass.getName() + ": " + relAttrNames);
            });
        }
        writer.println(indent + "  attributes for entity update: " + attributeNamesForEntityUpdate);
    }

    /**
     * Creates a CompletableFuture to represent an EntityInfo in PersistenceDataProvider's entityInfoMap.
     *
     * @param entityClass
     * @return new CompletableFuture.
     */
    @Trivial
    public static CompletableFuture<EntityInfo> newFuture(Class<?> entityClass) {
        // It's okay to use Java SE's CompletableFuture here given that *Async methods are never invoked on it
        return new CompletableFuture<>();
    }

    /**
     * Converts a generated entity back to its record equivalent.
     *
     * @param entity generated entity.
     * @return record.
     * @throws Exception if an error occurs.
     */
    @Trivial
    final Object toRecord(Object entity) throws Exception {
        Method toRecord = entity.getClass().getMethod("toRecord");
        return toRecord.invoke(entity);
    }

    @Override
    @Trivial
    public String toString() {
        return new StringBuilder("EntityInfo@").append(Integer.toHexString(hashCode())).append(' ') //
                        .append(name).append(' ') //
                        .append(attributeTypes.keySet()) //
                        .toString();
    }

    /**
     * Performs validation on the entity information, such as checking for
     * unsupportable entity attribute types.
     */
    @Trivial
    private void validate() {
        for (Entry<String, Class<?>> attrType : attributeTypes.entrySet())
            if (Util.UNSUPPORTED_ATTR_TYPES.contains(attrType.getValue()))
                throw exc(MappingException.class,
                          "CWWKD1055.unsupported.entity.attr",
                          attrType.getKey(),
                          entityClass.getName(),
                          attrType.getValue().getName(),
                          List.of(Instant.class.getSimpleName(),
                                  LocalDate.class.getSimpleName(),
                                  LocalDateTime.class.getSimpleName(),
                                  LocalTime.class.getSimpleName()),
                          List.of(BigDecimal.class.getSimpleName(),
                                  BigInteger.class.getSimpleName(),
                                  Boolean.class.getSimpleName(), "boolean",
                                  Byte.class.getSimpleName(), "byte",
                                  "byte[]",
                                  Character.class.getSimpleName(), "char",
                                  Double.class.getSimpleName(), "double",
                                  Float.class.getSimpleName(), "float",
                                  Integer.class.getSimpleName(), "int",
                                  Long.class.getSimpleName(), "long",
                                  Short.class.getSimpleName(), "short",
                                  String.class.getSimpleName(),
                                  UUID.class.getSimpleName()));
    }
}
