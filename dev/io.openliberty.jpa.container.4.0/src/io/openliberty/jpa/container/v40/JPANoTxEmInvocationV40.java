/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.jpa.container.v40;

import java.util.List;
import java.util.Set;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ConnectionConsumer;
import jakarta.persistence.ConnectionFunction;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityHandler;
import jakarta.persistence.FindOption;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockOption;
import jakarta.persistence.RefreshOption;
import jakarta.persistence.Statement;
import jakarta.persistence.StatementOrTypedQuery;
import jakarta.persistence.StatementReference;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.sql.ResultSetMapping;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaSelect;

import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.jpa.management.JPAEntityManager;
import com.ibm.ws.jpa.management.JPANoTxEmInvocation;


public class JPANoTxEmInvocationV40 extends JPANoTxEmInvocation{
    protected JPANoTxEmInvocationV40(UOWCoordinator uowCoord, EntityManager em, JPAEntityManager jpaEm, boolean txIsUnsynchronized) {
        super(uowCoord, em, jpaEm, txIsUnsynchronized);
        this.ivAllowPooling = false;
    }

    @Override
    public StatementOrTypedQuery createQuery(String qlString) {
        return ivEm.createQuery(qlString);
    }

    @Override
    public StatementOrTypedQuery createNamedQuery(String name) {
        return ivEm.createNamedQuery(name);
    }

    @Override
    public StatementOrTypedQuery createNativeQuery(String sqlString) {
        return ivEm.createNativeQuery(sqlString);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public TypedQuery createNativeQuery(String sqlString, Class resultClass) {
        return ivEm.createNativeQuery(sqlString, resultClass);
    }

    @Override
    public <T> TypedQuery<T> createNativeQuery(String sqlString, ResultSetMapping<T> resultSetMapping) {
        return ivEm.createNativeQuery(sqlString, resultSetMapping);
    }

    @Override
    public StatementOrTypedQuery createNativeQuery(String sqlString, String resultSetMapping) {
        return ivEm.createNativeQuery(sqlString, resultSetMapping);
    }

    @Override
    public Statement createNativeStatement(String sqlString) {
        return ivEm.createNativeStatement(sqlString);
    }

    @Override
    public Statement createStatement(StatementReference reference) {
        return ivEm.createStatement(reference);
    }

    @Override
    public Statement createStatement(String qlString) {
        return ivEm.createStatement(qlString);
    }

    @Override
    public Statement createNamedStatement(String name) {
        return ivEm.createNamedStatement(name);
    }

    @Override
    public <T> TypedQuery<T> createQuery(String qlString, EntityGraph<T> entityGraph) {
        return ivEm.createQuery(qlString, entityGraph);
    }

    @Override
    public Statement createStatement(jakarta.persistence.criteria.CriteriaStatement<?> statement) {
        return ivEm.createStatement(statement);
    }

    public Statement createQuery(jakarta.persistence.criteria.CriteriaUpdate<?> updateQuery) {
        return ivEm.createQuery(updateQuery);
    }

    public Statement createQuery(jakarta.persistence.criteria.CriteriaDelete<?> deleteQuery) {
        return ivEm.createQuery(deleteQuery);
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
    public <T> EntityGraph<T> getEntityGraph(Class<T> rootType, String graphName) {
        return ivEm.getEntityGraph(rootType, graphName);
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
    public <T> TypedQuery<T> createQuery(CriteriaSelect<T> selectQuery) {
        return ivEm.createQuery(selectQuery);
    }

    @Override
    public <T> TypedQuery<T> createQuery(TypedQueryReference<T> reference) {
        return ivEm.createQuery(reference);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, FindOption... options) {
        return ivEm.find(entityClass, primaryKey, options);
    }

    @Override
    public <T> T find(EntityGraph<T> entityGraph, Object primaryKey, FindOption... options) {
        return ivEm.find(entityGraph, primaryKey, options);
    }

    @Override
    public <T> T get(Class<T> entityClass, Object id) {
        return ivEm.get(entityClass, id);
    }

    @Override
    public <T> T get(Class<T> entityClass, Object id, FindOption... options) {
        return ivEm.get(entityClass, id, options);
    }

    @Override
    public <T> T get(EntityGraph<T> graph, Object id, FindOption... options) {
        return ivEm.get(graph, id, options);
    }

    @Override
    public <T> List<T> getMultiple(Class<T> entityClass, List<?> primaryKeys, FindOption... options) {
        return ivEm.getMultiple(entityClass, primaryKeys, options);
    }

    @Override
    public <T> List<T> getMultiple(EntityGraph<T> entityGraph, List<?> primaryKeys, FindOption... options) {
        return ivEm.getMultiple(entityGraph, primaryKeys, options);
    }

    @Override
    public <T> List<T> findMultiple(Class<T> entityClass, List<?> primaryKeys, FindOption... options) {
        return ivEm.findMultiple(entityClass, primaryKeys, options);
    }

    @Override
    public <T> List<T> findMultiple(EntityGraph<T> entityGraph, List<?> primaryKeys, FindOption... options) {
        return ivEm.findMultiple(entityGraph, primaryKeys, options);
    }

    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        return ivEm.getCacheRetrieveMode();
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        return ivEm.getCacheStoreMode();
    }

    @Override
    public <T> T getReference(T entity) {
        return ivEm.getReference(entity);
    }

    @Override
    public void lock(Object entity, LockModeType lockMode, LockOption... options) {
        ivEm.lock(entity, lockMode, options);
    }

    @Override
    public <C, T> T callWithConnection(ConnectionFunction<C, T> function) {
        return ivEm.callWithConnection(function);
    }

    @Override
    public void refresh(Object entity, RefreshOption... options) {
        ivEm.refresh(entity, options);
    }

    @Override
    public <C> void runWithConnection(ConnectionConsumer<C> action) {
        ivEm.runWithConnection(action);
    }

    @Override
    public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        ivEm.setCacheRetrieveMode(cacheRetrieveMode);
    }

    @Override
    public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        ivEm.setCacheStoreMode(cacheStoreMode);
    }

    @Override
    public void addOption(EntityManager.Option option) {
        ivEm.addOption(option);
    }

    @Override
    public Set<EntityManager.Option> getOptions() {
        return ivEm.getOptions();
    }

}
