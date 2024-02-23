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

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.util.List;
import java.util.Map;

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

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.ws.jpa.management.AbstractJPAComponent;
import com.ibm.ws.jpa.management.JPANoTxEmInvocation;
import com.ibm.ws.jpa.management.JPAPUnitInfo;
import com.ibm.ws.jpa.management.JPATxEmInvocation;
import com.ibm.ws.jpa.management.JPATxEntityManager;

@SuppressWarnings("serial")
public class JPATxEntityManagerV32 extends JPATxEntityManager {
    private static final TraceComponent tc = Tr.register(JPATxEntityManagerV32.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    public JPATxEntityManagerV32(JPAPuId puRefId, JPAPUnitInfo puInfo, J2EEName j2eeName, String refName, Map<?, ?> properties, boolean isUnsynchronized,
                                 AbstractJPAComponent jpaComponent) {
        super(puRefId, puInfo, j2eeName, refName, properties, isUnsynchronized, jpaComponent);
    }

    private Object writeReplace() {
        // jpa-2.1 might not be enabled when this is deserialized, so serialize
        // the base wrapper.  During deserialization, readResolve will rewrap
        // with this class if needed.
        return new JPATxEntityManager(ivPuRefId, ivPuInfo, ivJ2eeName, ivRefName, ivProperties, ivUnsynchronized, ivAbstractJPAComponent);
    }

    @Override
    protected JPATxEmInvocation createJPATxEmInvocation(UOWCoordinator uowCoord, EntityManager em) {
        return new JPATxEmInvocationV32(uowCoord, em, this, isTxUnsynchronized());
    }

    @Override
    protected JPANoTxEmInvocation createJPANoTxEmInvocation(UOWCoordinator uowCoord, EntityManager em) {
        return new JPANoTxEmInvocationV32(uowCoord, em, this, ivUnsynchronized);
    }

    @Override
    public Query createQuery(@SuppressWarnings("rawtypes") CriteriaUpdate arg0) {
        return getEMInvocationInfo(false).createQuery(arg0);
    }

    @Override
    public Query createQuery(@SuppressWarnings("rawtypes") CriteriaDelete arg0) {
        return getEMInvocationInfo(false).createQuery(arg0);
    }

    @Override
    public <T> EntityGraph<T> createEntityGraph(Class<T> arg0) {
        return getEMInvocationInfo(false).createEntityGraph(arg0);
    }

    @Override
    public EntityGraph<?> createEntityGraph(String arg0) {
        return getEMInvocationInfo(false).createEntityGraph(arg0);
    }

    @Override
    public EntityGraph<?> getEntityGraph(String arg0) {
        return getEMInvocationInfo(false).getEntityGraph(arg0);
    }

    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> arg0) {
        return getEMInvocationInfo(false).getEntityGraphs(arg0);
    }

    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(String arg0) {
        return getEMInvocationInfo(false).createNamedStoredProcedureQuery(arg0);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String arg0) {
        return getEMInvocationInfo(false).createStoredProcedureQuery(arg0);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String arg0, @SuppressWarnings("rawtypes") Class... arg1) {
        return getEMInvocationInfo(false).createStoredProcedureQuery(arg0, arg1);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String arg0, String... arg1) {
        return getEMInvocationInfo(false).createStoredProcedureQuery(arg0, arg1);
    }

    @Override
    public boolean isJoinedToTransaction() {
        return getEMInvocationInfo(false).isJoinedToTransaction();
    }
    
    @Override
    public <T> TypedQuery<T> createQuery(CriteriaSelect<T> selectQuery){
    	return getEMInvocationInfo(false).createQuery(selectQuery);
    }
    
    @Override
    public <T> TypedQuery<T> createQuery(TypedQueryReference<T> reference){
    	return getEMInvocationInfo(false).createQuery(reference);
    }
    
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey,FindOption... options) {
    	return getEMInvocationInfo(true).find(entityClass,primaryKey,options);
    }
    
    @Override
    public  <T> T find(EntityGraph<T> entityGraph, Object primaryKey,FindOption... options) {
    	return getEMInvocationInfo(true).find(entityGraph,primaryKey,options);
    }
    
    @Override
    public CacheRetrieveMode getCacheRetrieveMode(){
        return getEMInvocationInfo(false).getCacheRetrieveMode();
    }
    
    @Override
    public CacheStoreMode getCacheStoreMode(){
    	return getEMInvocationInfo(false).getCacheStoreMode();
    }
    
    @Override
    public <T> T getReference(T entity) {
    	return getEMInvocationInfo(false).getReference(entity);
    }
    
    @Override
    public void lock(Object entity, LockModeType lockMode,LockOption... options){
    	getEMInvocationInfo(true).lock(entity,lockMode,options);
    }
    
    @Override
    public <C,T> T callWithConnection(ConnectionFunction<C, T> function) {
    	return getEMInvocationInfo(false).callWithConnection(function);
    }
    
    @Override
    public void refresh(Object entity,RefreshOption... options){
    	getEMInvocationInfo(true).refresh(entity,options);
    }
    
    @Override
    public <C> void runWithConnection(ConnectionConsumer<C> action) {
    	getEMInvocationInfo(false).runWithConnection(action);
    }
    
    @Override
    public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode){
    	getEMInvocationInfo(false).setCacheRetrieveMode(cacheRetrieveMode);
    }
  
    @Override
    public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
    	getEMInvocationInfo(false).setCacheStoreMode(cacheStoreMode);
    }
}
