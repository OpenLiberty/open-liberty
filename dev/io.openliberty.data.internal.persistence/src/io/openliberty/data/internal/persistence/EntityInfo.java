/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
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

    // names of attributes to use for entity update.
    // excludes id and version.
    // excludes inner relation attributes, such as location.address when there is also a location.address.zipcode
    // TODO updates (and probably deletes) of entities with an embeddable id is not implemented yet.
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
            // ZonedDateTime is not one of the supported Temporal types
            // Jakarta Data and Jakarta Persistence and does not behave
            // correctly in EclipseLink where we have observed reading back
            // a different value from the database than was persisted.
            // If proper support is added for it in the future, then this
            // can be removed.
            if (ZonedDateTime.class.equals(attrType.getValue()))
                throw exc(MappingException.class,
                          "CWWKD1055.unsupported.entity.prop",
                          attrType.getKey(),
                          entityClass.getName(),
                          attrType.getValue(),
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
