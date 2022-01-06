/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package ejbpassivation.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import ejbpassivation.data.PassivateEntity;
import ejbpassivation.data.SessionIdCache;

/**
 *
 */
@Stateful
@TransactionManagement(TransactionManagementType.CONTAINER)
public class StatefulSessionPUBean implements StatefulSessionPUBeanLocal {
    @PersistenceUnit(unitName = "EJBPassivationPU")
    private EntityManagerFactory emf;

    @Resource
    SessionContext ejbcontext;

    private String sessionId = "StatefulSessionPUBean:" + Long.toString(System.nanoTime());

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public PassivateEntity newEntity(String description) throws Exception {
        PassivateEntity entity = new PassivateEntity();
        entity.setDescription(description);
        EntityManager em = emf.createEntityManager();
        try {
            em.joinTransaction();
            em.persist(entity);
            return entity;
        } finally {
            em.flush();
            em.close();
        }

    }

    @Override
    public PassivateEntity findEntity(int id) {
        EntityManager em = emf.createEntityManager();
        try {
            em.joinTransaction();
            return em.find(PassivateEntity.class, id);
        } finally {
            em.flush();
            em.close();
        }

    }

    @Override
    public PassivateEntity updateEntity(PassivateEntity entity) throws Exception {
        if (entity == null) {
            throw new NullPointerException("Bad test.");
        }

        EntityManager em = emf.createEntityManager();
        try {
            em.joinTransaction();
            PassivateEntity merged = em.merge(entity);
            return merged;
        } finally {
            em.flush();
            em.close();
        }
    }

    @Override
    public void removeEntity(PassivateEntity entity) throws Exception {
        if (entity == null) {
            return;
        }

        EntityManager em = emf.createEntityManager();
        try {
            em.joinTransaction();
            PassivateEntity findEntity = em.contains(entity) ? entity : em.find(PassivateEntity.class, entity.getId());
            if (findEntity == null) {
                return;
            }
            em.remove(findEntity);
        } finally {
            em.flush();
            em.close();
        }
    }

    @PostConstruct
    public void postConstruct() {
        SessionIdCache.sessionList.add(sessionId);
        System.out.println(toString() + " with sessionId=" + sessionId + " Has Been Constructed.");
    }

    @Override
    @Remove
    public void remove() {
        SessionIdCache.sessionList.remove(sessionId);
        System.out.println(toString() + " with sessionId=" + sessionId + " Has Been Removed.");
    }

    @PrePassivate
    public void prePassivate() {
        SessionIdCache.passivateList.add(sessionId);
        System.out.println(toString() + " with sessionId=" + sessionId + " About to Passivate.");
    }

    @PostActivate
    public void postActivate() {
        SessionIdCache.activateList.add(sessionId);
        System.out.println(toString() + " with sessionId=" + sessionId + " Has Activated.");
    }
}
