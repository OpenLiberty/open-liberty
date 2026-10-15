/*******************************************************************************
 * Copyright (c) 2023,2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.jpa.container.v40;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Consumer;

import jakarta.persistence.EntityAgent;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityHandler;
import jakarta.persistence.EntityListenerRegistration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.Statement;
import jakarta.persistence.StatementReference;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.sql.ResultSetMapping;


import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.ws.jpa.management.JPAEMFactory;


@SuppressWarnings("serial")
public class JPAEMFactoryV40 extends JPAEMFactory{
    public JPAEMFactoryV40(JPAPuId puId, J2EEName j2eeName, EntityManagerFactory emf) {
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
    public <R> TypedQueryReference<R> addNamedQuery(String name, TypedQuery<R> query) {
        return ivFactory.addNamedQuery(name, query);
    }

    @Override
    public EntityAgent createEntityAgent(EntityAgent.CreationOption... options) {
        return ivFactory.createEntityAgent(options);
    }

    @Override
    public EntityAgent createEntityAgent(Map<?, ?> properties) {
        return ivFactory.createEntityAgent(properties);
    }

    @Override
    public EntityManager createEntityManager(EntityManager.CreationOption... options) {
        return ivFactory.createEntityManager(options);
    }

    @Override
    public EntityManager createEntityManager(@SuppressWarnings("rawtypes") Map map) {
        return ivFactory.createEntityManager(map);
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
    public <H extends EntityHandler> void runInTransaction(Class<H> handlerClass, Consumer<H> work) {
        ivFactory.runInTransaction(handlerClass, work);
    }

    @Override
    public void runInTransaction(Consumer<EntityManager> work) {
    	 ivFactory.runInTransaction(work);
    }

    @Override
    public <R> R callInTransaction(Function<EntityManager, R> work) {
     	 return ivFactory.callInTransaction(work);
     }

    @Override
    public <R, H extends EntityHandler> R callInTransaction(Class<H> handlerClass, Function<H, R> work) {
        return ivFactory.callInTransaction(handlerClass, work);
    }

    @Override
    public <E> EntityListenerRegistration addListener(Class<E> entityClass, Class<? extends Annotation> annotation, Consumer<? super E> listener) {
        return ivFactory.addListener(entityClass, annotation, listener);
    }

    @Override
    public <R> Map<String, ResultSetMapping<R>> getResultSetMappings(Class<R> resultType) {
        return ivFactory.getResultSetMappings(resultType);
    }

    @Override
    public Map<String, StatementReference> getNamedStatements() {
        return ivFactory.getNamedStatements();
    }

    @Override
    public StatementReference addNamedStatement(String name, Statement statement) {
        return ivFactory.addNamedStatement(name, statement);
    }

}
