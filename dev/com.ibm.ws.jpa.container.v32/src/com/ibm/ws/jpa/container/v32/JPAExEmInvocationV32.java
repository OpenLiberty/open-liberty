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

import java.util.List;
import java.util.function.Function;
import java.util.function.Consumer;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.ConnectionFunction;
import jakarta.persistence.ConnectionConsumer;
import jakarta.persistence.LockOption;
import jakarta.persistence.FindOption;
import jakarta.persistence.RefreshOption;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.LockModeType;

import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.jpa.management.JPAExEmInvocation;

public class JPAExEmInvocationV32 extends JPAExEmInvocation {
    public JPAExEmInvocationV32(UOWCoordinator uowCoord, EntityManager em, boolean txIsUnsynchronized) {
        super(uowCoord, em, null, txIsUnsynchronized);
    }

    @Override
    public <T> EntityGraph<T> createEntityGraph(Class<T> arg0) {
        return ivEm.createEntityGraph(arg0);
    }

    @Override
    public EntityGraph<?> createEntityGraph(String arg0) {
        return ivEm.createEntityGraph(arg0);
    }

    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(String arg0) {
        return ivEm.createNamedStoredProcedureQuery(arg0);
    }

    @Override
    public Query createQuery(@SuppressWarnings("rawtypes") CriteriaUpdate arg0) {
        return ivEm.createQuery(arg0);
    }

    @Override
    public Query createQuery(@SuppressWarnings("rawtypes") CriteriaDelete arg0) {
        return ivEm.createQuery(arg0);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String arg0) {
        return ivEm.createStoredProcedureQuery(arg0);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String arg0, @SuppressWarnings("rawtypes") Class... arg1) {
        return ivEm.createStoredProcedureQuery(arg0, arg1);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String arg0, String... arg1) {
        return ivEm.createStoredProcedureQuery(arg0, arg1);
    }

    @Override
    public EntityGraph<?> getEntityGraph(String arg0) {
        return ivEm.getEntityGraph(arg0);
    }

    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> arg0) {
        return ivEm.getEntityGraphs(arg0);
    }

    @Override
    public boolean isJoinedToTransaction() {
        return ivEm.isJoinedToTransaction();
    }
    
    @Override
    public <T> TypedQuery<T> createQuery(CriteriaSelect<T> selectQuery){
    	return ivEm.createQuery(selectQuery);
    }
    
    @Override
    public <T> TypedQuery<T> createQuery(TypedQueryReference<T> reference){
    	return ivEm.createQuery(reference);
    }
    
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey,FindOption... options) {
    	return ivEm.find(entityClass,primaryKey,options);
    }
    
    @Override
    public  <T> T find(EntityGraph<T> entityGraph, Object primaryKey,FindOption... options) {
    	return ivEm.find(entityGraph,primaryKey,options);
    }
    
    @Override
    public CacheRetrieveMode getCacheRetrieveMode(){
        return ivEm.getCacheRetrieveMode();
    }
    
    @Override
    public CacheStoreMode getCacheStoreMode(){
    	return ivEm.getCacheStoreMode();
    }
    
    @Override
    public <T> T getReference(T entity) {
    	return ivEm.getReference(entity);
    }
    
    @Override
    public void lock(Object entity, LockModeType lockMode,LockOption... options){
    	ivEm.lock(entity,lockMode,options);
    }
    
    @Override
    public <C,T> T callWithConnection(ConnectionFunction<C, T> function) {
    	return ivEm.callWithConnection(function);
    }
    
    @Override
    public void refresh(Object entity,RefreshOption... options){
    	ivEm.refresh(entity,options);
    }
    
    @Override
    public <C> void runWithConnection(ConnectionConsumer<C> action) {
    	ivEm.runWithConnection(action);
    }
    
    @Override
    public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode){
    	ivEm.setCacheRetrieveMode(cacheRetrieveMode);
    }
  
    @Override
    public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
    	ivEm.setCacheStoreMode(cacheStoreMode);
    }
    
}
