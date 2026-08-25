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
package io.openliberty.jpa.container.v40.internal;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.jpa.JPAPuId;
import io.openliberty.jpa.container.v40.JPAEMFactoryV40;
import io.openliberty.jpa.container.v40.JPAExEmInvocationV40;
import io.openliberty.jpa.container.v40.JPAExEntityManagerV40;
import io.openliberty.jpa.container.v40.JPATxEntityManagerV40;
import com.ibm.ws.jpa.management.AbstractJPAComponent;
import com.ibm.ws.jpa.management.JPA20Runtime;
import com.ibm.ws.jpa.management.JPAExEmInvocation;
import com.ibm.ws.jpa.management.JPAExEntityManager;
import com.ibm.ws.jpa.management.JPAPUnitInfo;
import com.ibm.ws.jpa.management.JPARuntime;
import com.ibm.ws.jpa.management.JPATxEntityManager;
import com.ibm.ws.jpa.JPAVersion;

@Component(service = JPARuntime.class,
           property = Constants.SERVICE_RANKING + ":Integer=21")
public class JPA40Runtime extends JPA20Runtime implements JPARuntime {
    private static final TraceComponent tc = Tr.register
                    (JPA40Runtime.class,
                     JPA_TRACE_GROUP,
                     JPA_RESOURCE_BUNDLE_NAME);

    private final static String JEE7_DEFAULT_JTA_DATASOURCE_JNDI = "java:comp/DefaultDataSource";

    @Override
        public JPAVersion getJPARuntimeVersion() {
        return JPAVersion.JPA40;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public EntityManagerFactory createJPAEMFactory(JPAPuId puId, J2EEName j2eeName, EntityManagerFactory emf) {
        return new JPAEMFactoryV40(puId, j2eeName, emf);
    }

    @Override
    public PersistenceUnitInfo createPersistenceUnitInfo(JPAPUnitInfo puInfo, J2EEName j2eeName) {
        return new PersistenceUnitInfoAdapter40(puInfo, j2eeName);
    }

    @Override
    public JPATxEntityManager createJPATxEntityManager(JPAPuId puRefId, JPAPUnitInfo puInfo, J2EEName j2eeName, String refName, Map<?, ?> properties,
                                                       boolean isUnsynchronized, AbstractJPAComponent jpaComponent) {
        return new JPATxEntityManagerV40(puRefId, puInfo, j2eeName, refName, properties, isUnsynchronized, jpaComponent);
    }

    @Override
    public JPAExEntityManager createJPAExEntityManager(JPAPuId puRefId, JPAPUnitInfo puInfo, J2EEName j2eeName, String refName, Map<?, ?> properties,
                                                       boolean isUnsynchronized, AbstractJPAComponent jpaComponent) {
        return new JPAExEntityManagerV40(puRefId, puInfo, j2eeName, refName, properties, isUnsynchronized, jpaComponent);
    }

    @Override
    public JPAExEmInvocation createExEmInvocation(UOWCoordinator uowCoord, EntityManager em, boolean txIsUnsynchronized) {
        return new JPAExEmInvocationV40(uowCoord, em, txIsUnsynchronized);
    }

    @Override
    public EntityManager createEntityManagerInstance(EntityManagerFactory emf, boolean unsynchronized) {
        if (emf == null) {
            return null;
        }

        SynchronizationType syncType = unsynchronized ? SynchronizationType.UNSYNCHRONIZED : SynchronizationType.SYNCHRONIZED;

        try {
            return emf.createEntityManager(syncType);
        } catch (AbstractMethodError x) {
            Tr.error(tc, "NOT_COMPLIANT_WITH_JPA21_CWWJP0054E", "EntityManagerFactory", emf.getClass().getName());
            throw x;
        }
    }

    @Override
    public EntityManager createEntityManagerInstance(EntityManagerFactory emf, Map<?, ?> propMap, boolean unsynchronized) {
        if (emf == null) {
            return null;
        }

        SynchronizationType syncType = unsynchronized ? SynchronizationType.UNSYNCHRONIZED : SynchronizationType.SYNCHRONIZED;

        try {
            return emf.createEntityManager(syncType, propMap);
        } catch (AbstractMethodError x) {
            Tr.error(tc, "NOT_COMPLIANT_WITH_JPA21_CWWJP0054E", "EntityManagerFactory", emf.getClass().getName());
            throw x;
        }
    }

    @Override
    public String processJEE7JTADataSource(String jtaDataSource, String nonJtaDataSource) {
        if (jtaDataSource == null && nonJtaDataSource == null) {
            return JEE7_DEFAULT_JTA_DATASOURCE_JNDI;
        }

        return jtaDataSource;
    }

    @Override
    public boolean isIgnoreDataSourceErrors(Boolean ignoreDataSource) {
        return ignoreDataSource != null ? ignoreDataSource : false;
    }

    @Override
    public ClassTransformer getClassTransformer(PersistenceProvider provider,
                                                PersistenceUnitInfo puInfo,
                                                Map<?, ?> properties) {
        return provider.getClassTransformer(puInfo, properties);
    }
}
