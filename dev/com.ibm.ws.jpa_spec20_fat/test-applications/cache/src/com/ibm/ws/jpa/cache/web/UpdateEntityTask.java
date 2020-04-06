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

package com.ibm.ws.jpa.cache.web;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.UserTransaction;

import com.ibm.ws.jpa.cache.model.JPA20EMEntityA;

public class UpdateEntityTask implements Callable<Void>, ManagedTask {

    private Integer id;
    private AtomicBoolean isStarted = new AtomicBoolean();

    public UpdateEntityTask(Integer id) {
        this.id = id;
    }

    @Override
    public Void call() throws Exception {

        EntityManagerFactory emf = (EntityManagerFactory) new InitialContext().lookup("java:comp/env/jpa/TaskEMF");
        EntityManager em = emf.createEntityManager();
        UserTransaction tx = InitialContext.doLookup("java:comp/UserTransaction");

        tx.begin();
        em.joinTransaction();
        em.clear();

        JPA20EMEntityA entity = em.find(JPA20EMEntityA.class, id);
        entity.setStrData("Modified by B");
        isStarted.set(true); // Let the servlet know that the data has been modified

        tx.commit();
        em.close();
        return null;
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        return null;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }

    @Override
    public String toString() {
        return super.toString() + " {id=" + id + ";" + ";  isStarted=" + isStarted.get() + "}";
    }
}