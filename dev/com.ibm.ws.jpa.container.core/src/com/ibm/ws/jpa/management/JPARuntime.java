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
package com.ibm.ws.jpa.management;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.jpa.JPAPuId;

/**
 *
 */
public interface JPARuntime {
    /**
     * Returns this JPARuntime implementation's JPA Spec version.
     * 
     * @return
     */
    public JPAVersion getJPARuntimeVersion();

    /**
     * Returns true if this is the default wrapper factory. Callers can use this
     * to avoid unnecessarily recreating wrappers during deserialization.
     */
    public boolean isDefault();

    /**
     * Creates a new EntityManagerFactory that is JEE Runtime managed.
     * 
     * @param puId
     * @param j2eeName
     * @param emf
     * @return
     */
    public EntityManagerFactory createJPAEMFactory(JPAPuId puId, J2EEName j2eeName, EntityManagerFactory emf);

    /**
     * Creates a new JPATxEntityManager instance.
     * 
     * @param puRefId
     * @param puInfo
     * @param j2eeName
     * @param refName
     * @param properties
     * @param isUnsynchronized
     * @param jpaComponent
     * @return
     */
    public JPATxEntityManager createJPATxEntityManager(JPAPuId puRefId, JPAPUnitInfo puInfo, J2EEName j2eeName,
                                                       String refName, Map<?, ?> properties, boolean isUnsynchronized,
                                                       AbstractJPAComponent jpaComponent);

    /**
     * Creates a new JPAExEntityManager instance.
     * 
     * @param puRefId
     * @param puInfo
     * @param j2eeName
     * @param refName
     * @param properties
     * @param isUnsynchronized
     * @param jpaComponent
     * @return
     */
    public JPAExEntityManager createJPAExEntityManager(JPAPuId puRefId, JPAPUnitInfo puInfo, J2EEName j2eeName,
                                                       String refName, Map<?, ?> properties, boolean isUnsynchronized,
                                                       AbstractJPAComponent jpaComponent);

    /**
     * Creates a new JPAExEmInvocation.
     * 
     * @param uowCoord
     * @param em
     * @param txIsUnsynchronized
     * @return
     */
    public JPAExEmInvocation createExEmInvocation(UOWCoordinator uowCoord, EntityManager em, boolean txIsUnsynchronized);

    /**
     * Creates a new EntityManager instance.
     * 
     * @param emf
     * @param unsynchronized
     * @return
     */
    public EntityManager createEntityManagerInstance(EntityManagerFactory emf, boolean unsynchronized);

    /**
     * Creates a new EntityManager instance.
     * 
     * @param emf
     * @param propMap
     * @param unsynchronized
     * @return
     */
    public EntityManager createEntityManagerInstance(EntityManagerFactory emf, Map<?, ?> propMap, boolean unsynchronized);

    /**
     * The JPA feature can be configured to specify a default JTA DataSource through the
     * "defaultJtaDataSourceJndiName" property. This allows a persistence unit to omit
     * the <jta-data-source> element and still be a valid and consumable persistence unit for
     * the JEE runtime. This is an optional property, and its omission indicates that there
     * is no default JTA DataSource defined for a JEE 6 runtime environment (which then, the
     * ommission of the <jta-data-source> element is a deployment error.)
     * 
     * A default JTA DataSource was introduced in JEE 7. The JPA 2.1 Specification section 8.2.1.5
     * below dictates its use:
     * 
     * In Java EE environments, the jta-data-source and non-jta-data-source elements are
     * used to specify the JNDI name of the JTA and/or non-JTA data source to be used by the persistence provider.
     * If neither is specified, the deployer must specify a JTA data source at deployment or the default
     * JTA data source must be provided by the container, and a JTA EntityManagerFactory will be created to
     * correspond to it.
     * 
     * @param jtaDataSource - The specified JTA DataSource
     * @param nonJtaDataSource - The specified Resource Local DataSource
     * 
     * @return With JEE 6 (JPA 2.0) the jtaDataSource argument is returned regardless of the arguments.
     *         With JEE 7 (JPA 2.1) or higher, if both jtaDataSource and rsDataSource are null then
     *         the JEE 7 default JTA DataSource JNDI name is returned, otherwise jtaDataSource is returned.
     */
    public String processJEE7JTADataSource(String jtaDataSource, String nonJtaDataSource);

    public boolean isIgnoreDataSourceErrors(Boolean ignoreDataSource);
}
