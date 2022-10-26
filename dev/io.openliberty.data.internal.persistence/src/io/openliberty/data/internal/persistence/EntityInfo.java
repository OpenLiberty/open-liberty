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
package io.openliberty.data.internal.persistence;

import java.lang.reflect.Member;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;

import jakarta.data.Inheritance;

/**
 */
class EntityInfo {
    // properly cased/qualified JPQL attribute name --> accessor method or field
    final Map<String, Member> attributeAccessors; // TODO accessors won't be correct for embeddable
    // upper case attribute name --> properly cased/qualified JPQL attribute name
    final LinkedHashMap<String, String> attributeNames;
    final Set<String> collectionAttributeNames;
    final boolean inheritance;
    final Member keyAccessor;
    final String keyName;
    final String name;
    final PersistenceServiceUnit persister;
    final Class<?> type;

    EntityInfo(String entityName, Class<?> entityClass,
               Map<String, Member> attributeAccessors,
               LinkedHashMap<String, String> attributeNames,
               Set<String> collectionAttributeNames,
               String keyAttributeName,
               Member keyAccessor,
               PersistenceServiceUnit persister) {
        this.name = entityName;
        this.type = entityClass;
        this.attributeAccessors = attributeAccessors;
        this.attributeNames = attributeNames;
        this.collectionAttributeNames = collectionAttributeNames;
        this.keyName = keyAttributeName;
        this.keyAccessor = keyAccessor;
        this.persister = persister;

        inheritance = entityClass.getAnnotation(Inheritance.class) != null ||
                      entityClass.getAnnotation(jakarta.persistence.Inheritance.class) != null;

    }

    String getAttributeName(String name) {
        // TODO update per outcome of #44
        String attributeName = attributeNames.get(name.toUpperCase());
        if (attributeName == null)
            attributeName = "Id".equals(name) ? keyName : //
                            "All".equals(name) ? null : // Special case for CrudRepository.deleteAll and CrudRepository.findAll
                                            name;
        return attributeName;
    }

    Collection<String> getAttributeNames() {
        return attributeNames.values();
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
}
