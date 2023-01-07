/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package com.ibm.ws.ormdiag.example.ejb;

import java.util.stream.Stream;

import com.ibm.ws.ormdiag.example.jpa.ExampleEntity;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class ExampleEJBService {

    @PersistenceContext
    private EntityManager em;

    public void addEntity(ExampleEntity entity) {
        em.merge(entity);
    }

    public Stream<ExampleEntity> retrieveAllEntities() {
        return em.createNamedQuery("findAllEntities", ExampleEntity.class).getResultStream();
    }
}