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

import java.lang.reflect.Member;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;

import jakarta.data.exceptions.MappingException;
import jakarta.data.repository.Sort;
import jakarta.persistence.Inheritance;

/**
 */
class EntityInfo {
    // properly cased/qualified JPQL attribute name --> accessor methods or fields (multiple in the case of embeddable)
    final Map<String, List<Member>> attributeAccessors;

    // lower case attribute name --> properly cased/qualified JPQL attribute name
    final Map<String, String> attributeNames;

    // properly cased/qualified JPQL attribute name --> type
    final SortedMap<String, Class<?>> attributeTypes;

    // properly cased/qualified JPQL attribute name --> type of collection
    final Map<String, Class<?>> collectionElementTypes;

    final Class<?> entityClass; // will be a generated class for entity records
    final Class<?> idType; // type of the id, which could be a JPA IdClass for composite ids
    final SortedMap<String, Member> idClassAttributeAccessors; // null if no IdClass
    final boolean inheritance;
    final String name;
    final PersistenceServiceUnit persister;
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
               SortedMap<String, Class<?>> attributeTypes,
               Map<String, Class<?>> collectionElementTypes,
               Map<Class<?>, List<String>> relationAttributeNames,
               Class<?> idType,
               SortedMap<String, Member> idClassAttributeAccessors,
               String versionAttributeName,
               PersistenceServiceUnit persister) {
        this.name = entityName;
        this.entityClass = entityClass;
        this.attributeAccessors = attributeAccessors;
        this.attributeNames = attributeNames;
        this.attributeTypes = attributeTypes;
        this.collectionElementTypes = collectionElementTypes;
        this.relationAttributeNames = relationAttributeNames;
        this.idType = idType;
        this.idClassAttributeAccessors = idClassAttributeAccessors;
        this.persister = persister;
        this.recordClass = recordClass;
        this.versionAttributeName = versionAttributeName;

        inheritance = entityClass.getAnnotation(Inheritance.class) != null;
    }

    String getAttributeName(String name, boolean failIfNotFound) {
        String lowerName = name.toLowerCase();
        String attributeName = attributeNames.get(lowerName);
        if (attributeName == null)
            if ("All".equals(name))
                attributeName = null; // Special case for CrudRepository.deleteAll and CrudRepository.findAll
            else if ("id".equals(lowerName))
                if (idClassAttributeAccessors == null && failIfNotFound)
                    throw new MappingException("Entity class " + getType().getName() + " does not have a property named " + name +
                                               " or which is designated as the @Id."); // TODO NLS
                else
                    attributeName = null; // Special case for IdClass
            else if (name.length() == 0)
                throw new MappingException("Error parsing method name or entity property name is missing."); // TODO NLS
            else {
                // tolerate possible mixture of . and _ separators:
                lowerName = lowerName.replace('.', '_');
                attributeName = attributeNames.get(lowerName);
                if (attributeName == null) {
                    // tolerate possible mixture of . and _ separators with lack of separators:
                    lowerName = lowerName.replace("_", "");
                    attributeName = attributeNames.get(lowerName);
                    if (attributeName == null && failIfNotFound)
                        throw new MappingException("Entity class " + getType().getName() + " does not have a property named " + name +
                                                   ". The following are valid property names for the entity: " +
                                                   attributeTypes.keySet()); // TODO NLS
                }
            }

        return attributeName;
    }

    Collection<String> getAttributeNames() {
        return attributeNames.values();
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
     * Creates a Sort instance with the corresponding entity attribute name
     * or returns the existing instance if it already matches.
     *
     * @param name name provided by the user to sort by.
     * @param sort the Sort to add.
     * @return a Sort instance with the corresponding entity attribute name.
     */
    @Trivial
    Sort getWithAttributeName(String name, Sort sort) {
        name = getAttributeName(name, true);
        if (name == sort.property())
            return sort;
        else
            return sort.isAscending() //
                            ? sort.ignoreCase() ? Sort.ascIgnoreCase(name) : Sort.asc(name) //
                            : sort.ignoreCase() ? Sort.descIgnoreCase(name) : Sort.desc(name);
    }

    /**
     * Creates a CompletableFuture to represent an EntityInfo in PersistenceDataProvider's entityInfoMap.
     *
     * @param entityClass
     * @return new CompletableFuture.
     */
    @Trivial
    static CompletableFuture<EntityInfo> newFuture(Class<?> entityClass) {
        // It's okay to use Java SE's CompletableFuture here given that *Async methods are never invoked on it
        return new CompletableFuture<>();
    }

    @Override
    @Trivial
    public String toString() {
        return new StringBuilder("EntityInfo@").append(Integer.toHexString(hashCode())).append(' ') //
                        .append(name).append(' ') //
                        .append(attributeTypes.keySet()) //
                        .toString();
    }
}
