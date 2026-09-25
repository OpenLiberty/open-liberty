/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jakarta.data.v1_1.hibernate;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map.Entry;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Repository;
import jakarta.persistence.EntityAgent;
import jakarta.persistence.TypedQuery;

import test.jakarta.data.v1_1.web.Fraction;

/**
 * Repository with no entities directly used. The repository only has
 * resource accessor methods and default methods using the resource accessor
 * methods.
 */
@Repository(dataStore = "MyDataStore")
public interface ResourceAccess extends DataRepository<Fraction, String> {

    EntityAgent agent();

    default <T> Entry<EntityAgent, List<T>> runQuery(String jpql,
                                                     Class<T> entityClass) {
        EntityAgent agent = agent();
        TypedQuery<T> query = agent.createQuery(jpql, entityClass);
        return new SimpleImmutableEntry<EntityAgent, List<T>>( //
                        agent, //
                        query.getResultList());
    }
}
