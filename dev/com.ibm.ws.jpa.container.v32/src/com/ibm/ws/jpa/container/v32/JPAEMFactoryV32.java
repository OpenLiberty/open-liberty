/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.jpa.container.v32;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Consumer;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.PersistenceUnitTransactionType;


import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.ws.jpa.management.JPAEMFactory;


@SuppressWarnings("serial")
public class JPAEMFactoryV32 extends JPAEMFactory {
    public JPAEMFactoryV32(JPAPuId puId, J2EEName j2eeName, EntityManagerFactory emf) {
        super(puId, j2eeName, emf);
    }

    private Object writeReplace() {
        // jpa-2.2 might not be enabled when this is deserialized, so serialize
        // the base wrapper.  During deserialization, readResolve will rewrap
        // with this class if needed.
        return new JPAEMFactory(this);
    }

    @Override
    public <T> void addNamedEntityGraph(String arg0, EntityGraph<T> arg1) {
        ivFactory.addNamedEntityGraph(arg0, arg1);
    }

    @Override
    public void addNamedQuery(String arg0, Query arg1) {
        ivFactory.addNamedQuery(arg0, arg1);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType arg0) {
        return ivFactory.createEntityManager(arg0);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType arg0, @SuppressWarnings("rawtypes") Map arg1) {
        return ivFactory.createEntityManager(arg0, arg1);
    }
    
    @Override
    public String getName() {
    	return ivFactory.getName();
    }
    
    @Override
    public <E> Map<String, EntityGraph<? extends E>> getNamedEntityGraphs(Class<E> entityType){
    	return ivFactory.getNamedEntityGraphs(entityType);
    }
    
    @Override
    public <R> Map<String, TypedQueryReference<R>> getNamedQueries(Class<R> resultType){
    	return ivFactory.getNamedQueries(resultType);
    }
    
    @Override
    public SchemaManager getSchemaManager() {
    	return ivFactory.getSchemaManager();
    }
    
    @Override
    public PersistenceUnitTransactionType getTransactionType() {
    	return ivFactory.getTransactionType();
    }
    
    @Override
    public void runInTransaction(Consumer<EntityManager> work) {
    	 ivFactory.runInTransaction(work);
    }

}
