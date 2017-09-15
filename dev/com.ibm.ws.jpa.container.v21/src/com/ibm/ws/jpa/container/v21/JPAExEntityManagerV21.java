/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.v21;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaUpdate;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.ws.jpa.management.AbstractJPAComponent;
import com.ibm.ws.jpa.management.JPAExEntityManager;
import com.ibm.ws.jpa.management.JPAPUnitInfo;

@SuppressWarnings("serial")
public class JPAExEntityManagerV21 extends JPAExEntityManager {
    private static final TraceComponent tc = Tr.register(JPAExEntityManagerV21.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    public JPAExEntityManagerV21(JPAPuId puRefId,
                                 JPAPUnitInfo puInfo,
                                 J2EEName j2eeName,
                                 String refName,
                                 Map<?, ?> properties,
                                 boolean isUnsynchronized,
                                 AbstractJPAComponent jpaComponent) {
        super(puRefId, puInfo, j2eeName, refName, properties, isUnsynchronized, jpaComponent);
    }

    private Object writeReplace() {
        // jpa-2.1 might not be enabled when this is deserialized, so serialize
        // the base wrapper.  During deserialization, readResolve will rewrap
        // with this class if needed.
        return new JPAExEntityManager(ivPuRefId, ivPuInfo, ivJ2eeName, ivRefName, ivProperties, ivUnsynchronized, ivAbstractJPAComponent);
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
}
