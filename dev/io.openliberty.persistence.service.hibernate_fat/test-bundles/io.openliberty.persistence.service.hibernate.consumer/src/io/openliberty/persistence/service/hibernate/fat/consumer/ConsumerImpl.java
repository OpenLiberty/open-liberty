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
package io.openliberty.persistence.service.hibernate.fat.consumer;

import java.io.Writer;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import com.ibm.wsspi.persistence.PersistenceServiceUnit;

import io.openliberty.persistence.service.hibernate.fat.consumer.internal.model.Person;
import io.openliberty.persistence.service.hibernate.fat.persistence.test.Consumer;

/**
 * Jakarta EE 12 / JPA 4.0 consumer implementation backed by Hibernate 8.x.
 *
 * Deliberately simpler than the EclipseLink version: no EclipseLink-specific
 * lazy-loading checks, no IndirectList assertions.
 */
public class ConsumerImpl implements Consumer {

    private final PersistenceServiceUnit _activePu;
    private PersistenceServiceUnit _tempPu;
    private final ConsumerService _service;
    private final PersistenceServiceUnit _rootPu;

    ConsumerImpl(PersistenceServiceUnit pu, ConsumerService service) {
        _activePu = pu;
        _rootPu   = pu;
        _service  = service;
    }

    @Override
    public void createTables() {
        _activePu.createTables();
    }

    @Override
    public long persistPerson(boolean newTran, int numCars) {
        return persistPerson(newTran, numCars, null);
    }

    @Override
    public long persistPerson(boolean newTran, int numCars, String data) {
        EntityManager em = _activePu.createEntityManager();
        try {
            Person p = new Person("first", "last", data);
            em.persist(p);
            return p.getId();
        } finally {
            em.close();
        }
    }

    @Override
    public boolean personExists(long personId) {
        EntityManager em = _activePu.createEntityManager();
        try {
            return em.find(Person.class, personId) != null;
        } finally {
            em.close();
        }
    }

    @Override
    public String[] getPersonName(long personId) {
        EntityManager em = _activePu.createEntityManager();
        try {
            Person p = em.find(Person.class, personId);
            return new String[] { p.getFirst(), p.getLast() };
        } finally {
            em.close();
        }
    }

    @Override
    public String[] findByFirstName(String firstName) {
        EntityManager em = _activePu.createEntityManager();
        try {
            TypedQuery<Person> q = em.createQuery(
                "SELECT p FROM Person p WHERE p.firstName = :first", Person.class);
            q.setParameter("first", firstName);
            List<Person> results = q.getResultList();
            return results.stream().map(Person::getLast).toArray(String[]::new);
        } finally {
            em.close();
        }
    }

    @Override
    public void generateDDL(Writer out) {
        _activePu.generateDDL(out);
    }

    @Override
    public String getDatabaseTerminationToken() {
        return _activePu.getDatabaseTerminationToken();
    }

    @Override
    public void close() {
        if (_tempPu != null && _tempPu != _rootPu) {
            _tempPu.close();
        }
    }
}
