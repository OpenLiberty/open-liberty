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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;

import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.Sort;
import jakarta.data.exceptions.MappingException;
import jakarta.persistence.Inheritance;

/**
 * Entity information
 */
class EntityInfo {
    // properly cased/qualified JPQL attribute name --> accessor methods or fields (multiple in the case of embeddable)
    final Map<String, List<Member>> attributeAccessors;

    // lower case attribute name --> properly cased/qualified JPQL attribute name
    final Map<String, String> attributeNames;

    // properly cased/qualified JPQL attribute name --> type
    final SortedMap<String, Class<?>> attributeTypes;

    final EntityManagerBuilder builder;

    // properly cased/qualified JPQL attribute name --> type of collection
    final Map<String, Class<?>> collectionElementTypes;

    final Class<?> entityClass; // will be a generated class for entity records
    final Class<?> idType; // type of the id, which could be a JPA IdClass for composite ids
    final SortedMap<String, Member> idClassAttributeAccessors; // null if no IdClass
    final boolean inheritance;
    final String name;
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
               EntityManagerBuilder entityManagerBuilder) {
        this.name = entityName;
        this.builder = entityManagerBuilder;
        this.entityClass = entityClass;
        this.attributeAccessors = attributeAccessors;
        this.attributeNames = attributeNames;
        this.attributeTypes = attributeTypes;
        this.collectionElementTypes = collectionElementTypes;
        this.relationAttributeNames = relationAttributeNames;
        this.idType = idType;
        this.idClassAttributeAccessors = idClassAttributeAccessors;
        this.recordClass = recordClass;
        this.versionAttributeName = versionAttributeName;

        inheritance = entityClass.getAnnotation(Inheritance.class) != null;
    }

    /**
     * Obtains the value of an entity attribute.
     *
     * @param entity        the entity from which to obtain the value.
     * @param attributeName name of the entity attribute.
     * @return the value of the attribute.
     */
    Object getAttribute(Object entity, String attributeName) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<Member> accessors = attributeAccessors.get(attributeName);
        if (accessors == null)
            throw new IllegalArgumentException(attributeName); // should never occur

        Object value = entity;
        for (Member accessor : accessors) {
            Class<?> type = accessor.getDeclaringClass();
            if (type.isInstance(value)) {
                if (accessor instanceof Method)
                    value = ((Method) accessor).invoke(value);
                else // Field
                    value = ((Field) accessor).get(value);
            } else {
                throw new MappingException("Value of type " + value.getClass().getName() + " is incompatible with attribute type " + type.getName()); // TODO NLS
            }
        }

        return value;
    }

    String getAttributeName(String name, boolean failIfNotFound) {
        String lowerName = name.toLowerCase();
        String attributeName = attributeNames.get(lowerName);
        if (attributeName == null)
            if ("id".equals(lowerName))
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
                        // TODO If attempting to parse Query by Method Name without a By keyword, then the message
                        // should also include the possibility that repository method is missing an annotation.
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
     * Returns the list of entity attribute names, suitable for use in JPQL, when updating an entity.
     * This excludes the id and version. It also excludes embeddable and relation attribute names,
     * but leaves the outermost name (for example, removes location.address, but preserves location.address.cityName).
     * TODO The above is for embeddables. Decide what to do for relations other than embeddable.
     * TODO It's inefficient to keep recomputing this. Consider doing it just once, maybe in EntityDefiner
     * where we can build the list correctly from the start rather than later excluding. Maybe the pre-computed list
     * can be null when there are relation attributes to indicate that update by entity isn't supported for that type of entity.
     * TODO updates (and probably deletes) to entities with an embeddable id is not implemented yet.
     *
     * @return list of entity attribute names.
     */
    LinkedHashSet<String> getAttributeNamesForEntityUpdate() {
        LinkedHashSet<String> names = new LinkedHashSet<>(attributeNames.size());

        for (String name : attributeTypes.keySet())
            names.add(name);

        names.remove("id");
        names.remove(attributeNames.get("id"));
        names.remove(versionAttributeName);

        for (String name : attributeTypes.keySet()) {
            int ldot = name.lastIndexOf('.');
            if (ldot > 0)
                names.remove(name.substring(0, ldot));
        }

        return names;
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
    <T> Sort<T> getWithAttributeName(String name, Sort<T> sort) {
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

    /**
     * Converts a generated entity back to its record equivalent.
     *
     * @param entity generated entity.
     * @return record.
     * @throws Exception if an error occurs.
     */
    @Trivial
    final Object toRecord(Object entity) throws Exception {
        // TODO replace this method by including a toRecord method on an interface that is implemented
        // by the generated entity, then cast to the interface and invoke it to get the record.
        RecordComponent[] components = recordClass.getRecordComponents();
        Class<?>[] argTypes = new Class<?>[components.length];
        Object[] args = new Object[components.length];
        int a = 0;
        for (RecordComponent component : components) {
            PropertyDescriptor desc = new PropertyDescriptor(component.getName(), entity.getClass());
            argTypes[a] = component.getType();
            args[a++] = desc.getReadMethod().invoke(entity);
        }
        return recordClass.getConstructor(argTypes).newInstance(args);
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
