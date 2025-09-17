/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jpa.container.v32.cdi.internal;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.JPAAccessor;
import com.ibm.ws.jpa.JPAComponent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.spi.PersistenceUnitInfo;

public class JPACDIExtension implements Extension {

    private static final TraceComponent tc = Tr.register(JPACDIExtension.class);

    private final JPAComponent jpaComponent = JPAAccessor.getJPAComponent();

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {

        //JPA PersistenceUnit are scoped to modules, but in getCurrentAppJ2EEName I strip
        //out the app name. This is because of a limitation in our CDI implementation that
        //means we cannot scope an Extension to just a single module.
        //
        //This is a tech debt that really should be fixed.
        J2EEName j2EEName = getCurrentAppJ2EEName();
        List<PersistenceUnitInfo> persistenceUnits = jpaComponent.getPersistenceUnits(j2EEName);

        for (PersistenceUnitInfo pui : persistenceUnits) {
            try {
                createBeanForPersistenceUnit(abd, pui, j2EEName);
            } catch (ClassNotFoundException e) {
                Tr.warning(tc, "FAILED_TO_CREATE_PU_BEAN_CWWJP9994W", pui.getPersistenceUnitName(), e);
            }
        }
    }

    private void createBeanForPersistenceUnit(AfterBeanDiscovery abd, final PersistenceUnitInfo pui, final J2EEName j2eeName) throws ClassNotFoundException {

        Set<Annotation> qualifiers = getQualifiers(pui);
        Class<? extends Annotation> scopeForEntityManager = getScope(pui, jakarta.transaction.TransactionScoped.class);
        Class<? extends Annotation> scopeForEntityManagerFactory = jakarta.enterprise.context.ApplicationScoped.class;
        Class<? extends Annotation> scopeForOthers = jakarta.enterprise.context.Dependent.class;

        abd.addBean().types(EntityManager.class).addQualifiers(qualifiers).scope(scopeForEntityManager).produceWith((instance) -> jpaComponent.getEntityManager(j2eeName, pui));
        abd.addBean().types(EntityManagerFactory.class).addQualifiers(qualifiers).scope(scopeForEntityManagerFactory).produceWith((instance) -> jpaComponent.getEntityManagerFactory(j2eeName,
                                                                                                                                                                                     pui));
        abd.addBean().types(PersistenceUnitUtil.class).addQualifiers(qualifiers).scope(scopeForOthers).produceWith((instance) -> jpaComponent.getEntityManagerFactory(j2eeName,
                                                                                                                                                                      pui).getPersistenceUnitUtil());
        abd.addBean().types(CriteriaBuilder.class).addQualifiers(qualifiers).scope(scopeForOthers).produceWith((instance) -> jpaComponent.getEntityManagerFactory(j2eeName,
                                                                                                                                                                  pui).getCriteriaBuilder());
        abd.addBean().types(Cache.class).addQualifiers(qualifiers).scope(scopeForOthers).produceWith((instance) -> jpaComponent.getEntityManagerFactory(j2eeName,
                                                                                                                                                        pui).getCache());
        abd.addBean().types(Metamodel.class).addQualifiers(qualifiers).scope(scopeForOthers).produceWith((instance) -> jpaComponent.getEntityManagerFactory(j2eeName,
                                                                                                                                                            pui).getMetamodel());
        abd.addBean().types(SchemaManager.class).addQualifiers(qualifiers).scope(scopeForOthers).produceWith((instance) -> jpaComponent.getEntityManagerFactory(j2eeName,
                                                                                                                                                                pui).getSchemaManager());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            String qualifiersString = qualifiers.stream().map(Annotation::annotationType).map(Class::getName).collect(Collectors.joining(", "));
            Tr.debug(tc, "Creating beans for a persistence unit (and related) with scope " + scopeForEntityManager + " and qualifiers: " + qualifiersString);
        }

    }

    private J2EEName getCurrentAppJ2EEName() {

        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd != null) {
            J2EEName j2eeName = cmd.getJ2EEName();

            //Tech debt.
            //When CDI is able to handle per-module extensions, just return j2eeName
            return new AppOnlyJ2EEName(j2eeName.getApplication());
        }
        return null;
    }

    private Class<? extends Annotation> getScope(PersistenceUnitInfo pui, Class<? extends Annotation> defaultScope) throws ClassNotFoundException {
        String scopeName = pui.getScopeAnnotationName();

        Class<? extends Annotation> scope;
        if (scopeName == null || scopeName.equals("")) {
            scope = defaultScope;
        } else {
            scope = Class.forName(scopeName, false, pui.getClassLoader()).asSubclass(Annotation.class);
        }

        return scope;
    }

    private Set<Annotation> getQualifiers(PersistenceUnitInfo pui) throws ClassNotFoundException {
        List<String> qualifierNames = pui.getQualifierAnnotationNames();
        Set<Annotation> qualifiers = new HashSet<Annotation>();
        for (String qualifierName : qualifierNames) {

            final Class<? extends Annotation> qualifierClass = Class.forName(qualifierName, false, pui.getClassLoader()).asSubclass(Annotation.class);
            @SuppressWarnings("rawtypes")
            Annotation qualifierAnnotationLiteral = new AnnotationLiteral() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return qualifierClass;
                }
            };
            qualifiers.add(qualifierAnnotationLiteral);
        }
        return qualifiers;
    }

    private static class AppOnlyJ2EEName implements J2EEName {

        private final String appName;

        public AppOnlyJ2EEName(String appName) {
            this.appName = appName;
        }

        @Override
        public String getApplication() {
            return appName;
        }

        @Override
        public byte[] getBytes() {
            return null;
        }

        @Override
        public String getComponent() {
            return null;
        }

        @Override
        public String getModule() {
            return null;
        }

        //For pretty trace output
        @Override
        public String toString() {
            return "AppOnlyJ2EEName[" + appName + "]";
        }
    }
}
