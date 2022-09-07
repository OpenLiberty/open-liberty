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
package io.openliberty.data.internal;

import java.util.LinkedHashMap;
import java.util.Set;

import com.ibm.wsspi.persistence.PersistenceServiceUnit;

/**
 */
class EntityInfo {
    // upper case attribute name --> properly cased/qualified JPQL attribute name
    final LinkedHashMap<String, String> attributeNames;
    final Set<String> collectionAttributeNames;
    final String keyName;
    final String name;
    final PersistenceServiceUnit persister;
    final Class<?> type;

    EntityInfo(String entityName, Class<?> entityClass,
               LinkedHashMap<String, String> attributeNames,
               Set<String> collectionAttributeNames,
               String keyAttributeName,
               PersistenceServiceUnit persister) {
        this.name = entityName;
        this.type = entityClass;
        this.attributeNames = attributeNames;
        this.collectionAttributeNames = collectionAttributeNames;
        this.keyName = keyAttributeName;
        this.persister = persister;
    }
}
