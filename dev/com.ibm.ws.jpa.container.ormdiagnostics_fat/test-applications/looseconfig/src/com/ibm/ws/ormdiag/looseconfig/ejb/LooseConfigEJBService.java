/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ormdiag.looseconfig.ejb;

import java.util.stream.Stream;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.ibm.ws.ormdiag.looseconfig.jpa.LooseConfigEntity;

@Stateless
public class LooseConfigEJBService {

    @PersistenceContext
    private EntityManager em;

    public void addEntity(LooseConfigEntity entity) {
        em.merge(entity);
    }

    public Stream<LooseConfigEntity> retrieveAllEntities() {
        return em.createNamedQuery("findAllEntities", LooseConfigEntity.class).getResultStream();
    }
}