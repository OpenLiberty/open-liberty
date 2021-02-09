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
package com.ibm.ws.testtooling.vehicle.resources;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;

public class JPAResource {
    private JPAPersistenceContext pcCtxInfo;

    private EntityManagerFactory emf = null;
    private EntityManager em = null;
    private TransactionJacket tj = null;

    public JPAResource(JPAPersistenceContext pcCtxInfo,
                       EntityManagerFactory emf, EntityManager em, TransactionJacket tj) {
        this.pcCtxInfo = pcCtxInfo;
        this.emf = emf;
        this.em = em;
        this.tj = tj;
    }

    public final String getName() {
        return pcCtxInfo.getName();
    }

    public final JPAPersistenceContext getPcCtxInfo() {
        return pcCtxInfo;
    }

    public final EntityManagerFactory getEmf() {
        return emf;
    }

    public final EntityManager getEm() {
        return em;
    }

    public final TransactionJacket getTj() {
        return tj;
    }

    /**
     * Close the EntityManager if the JPA resource is application managed
     */
    public void close() {
        if (pcCtxInfo.getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA ||
            pcCtxInfo.getPcType() == PersistenceContextType.APPLICATION_MANAGED_RL ||
            pcCtxInfo.getPcType() == PersistenceContextType.JSE) {
            em.close();
        }
    }

    @Override
    public String toString() {
        return "JPAResource [pcCtxInfo=" + pcCtxInfo + ", emf=" + emf + ", em="
               + em + ", tj=" + tj + "]";
    }
}