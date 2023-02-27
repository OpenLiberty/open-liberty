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

    // embeddable class -> fully qualified attribute names of embeddable
    final Map<Class<?>, List<String>> embeddableAttributeNames;

    final Class<?> idClass; // null if no IdClass
    final SortedMap<String, Member> idClassAttributeAccessors; // null if no IdClass
    final boolean inheritance;
    final String name;
    final PersistenceServiceUnit persister;
    final Class<?> type;

    EntityInfo(String entityName, Class<?> entityClass,
               Map<String, List<Member>> attributeAccessors,
               Map<String, String> attributeNames,
               SortedMap<String, Class<?>> attributeTypes,
               Map<Class<?>, List<String>> embeddableAttributeNames,
               Class<?> idClass,
               SortedMap<String, Member> idClassAttributeAccessors,
               PersistenceServiceUnit persister) {
        this.name = entityName;
        this.type = entityClass;
        this.attributeAccessors = attributeAccessors;
        this.attributeNames = attributeNames;
        this.attributeTypes = attributeTypes;
        this.embeddableAttributeNames = embeddableAttributeNames;
        this.idClass = idClass;
        this.idClassAttributeAccessors = idClassAttributeAccessors;
        this.persister = persister;

        inheritance = entityClass.getAnnotation(Inheritance.class) != null;
    }

    String getAttributeName(String name) {
        String lowerName = name.toLowerCase();
        String attributeName = attributeNames.get(lowerName);
        if (attributeName == null)
            if ("All".equals(name))
                attributeName = null; // Special case for CrudRepository.deleteAll and CrudRepository.findAll
            else if ("id".equals(lowerName))
                if (idClass == null)
                    throw new MappingException("Entity class " + type.getName() + " does not have a property named " + name +
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
                    if (attributeName == null)
                        throw new MappingException("Entity class " + type.getName() + " does not have a property named " + name +
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
     * Creates a Sort instance with the corresponding entity attribute name
     * or returns the existing instance if it already matches.
     *
     * @param name name provided by the user to sort by.
     * @param sort the Sort to add.
     * @return a Sort instance with the corresponding entity attribute name.
     */
    @Trivial
    Sort getWithAttributeName(String name, Sort sort) {
        name = getAttributeName(name);
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
