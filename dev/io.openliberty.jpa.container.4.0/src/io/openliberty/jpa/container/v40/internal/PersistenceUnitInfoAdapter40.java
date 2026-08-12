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

import java.net.URL;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.jpa.management.JPAPUnitInfo;
import com.ibm.ws.jpa.management.PersistenceUnitInfoDelegate;

import jakarta.persistence.FetchType;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * Native Jakarta Persistence 4 provider-facing view of Liberty's
 * persistence-version-neutral persistence-unit state.
 */
final class PersistenceUnitInfoAdapter40 implements PersistenceUnitInfo, PersistenceUnitInfoDelegate {
    private final JPAPUnitInfo persistenceUnitState;
    private final J2EEName componentName;
    private DataSource jtaDataSource;
    private DataSource nonJtaDataSource;

    PersistenceUnitInfoAdapter40(JPAPUnitInfo persistenceUnitState, J2EEName componentName) {
        this.persistenceUnitState = persistenceUnitState;
        this.componentName = componentName;
    }

    @Override
    public String getPersistenceUnitName() {
        return persistenceUnitState.getPersistenceUnitName();
    }

    @Override
    public String getPersistenceProviderClassName() {
        return persistenceUnitState.getPersistenceProviderClassName();
    }

    @Override
    public String getScopeAnnotationName() {
        return persistenceUnitState.getScopeAnnotationName();
    }

    @Override
    public List<String> getQualifierAnnotationNames() {
        return persistenceUnitState.getQualifierAnnotationNames();
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return PersistenceUnitTransactionType.valueOf(persistenceUnitState.getTransactionTypeName());
    }

    @Override
    public DataSource getJtaDataSource() {
        if (jtaDataSource == null) {
            jtaDataSource = componentName == null ? persistenceUnitState.getJtaDataSource() : persistenceUnitState.lookupJtaDataSource();
        }
        return jtaDataSource;
    }

    @Override
    public DataSource getNonJtaDataSource() {
        if (nonJtaDataSource == null) {
            nonJtaDataSource = componentName == null ? persistenceUnitState.getNonJtaDataSource() : persistenceUnitState.lookupNonJtaDataSource();
        }
        return nonJtaDataSource;
    }

    @Override
    public List<String> getMappingFileNames() {
        return persistenceUnitState.getMappingFileNames();
    }

    @Override
    public List<URL> getJarFileUrls() {
        return persistenceUnitState.getJarFileUrls();
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        return persistenceUnitState.getPersistenceUnitRootUrl();
    }

    @Override
    public List<String> getManagedClassNames() {
        return persistenceUnitState.getManagedClassNames();
    }

    @Override
    public List<String> getAllClassNames() {
        return persistenceUnitState.getAllClassNames();
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return persistenceUnitState.excludeUnlistedClasses();
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return persistenceUnitState.getSharedCacheMode();
    }

    @Override
    public ValidationMode getValidationMode() {
        return persistenceUnitState.getValidationMode();
    }

    @Override
    public FetchType getDefaultToOneFetchType() {
        return persistenceUnitState.getDefaultToOneFetchType();
    }

    @Override
    public Properties getProperties() {
        return persistenceUnitState.getProperties();
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return persistenceUnitState.getPersistenceXMLSchemaVersion();
    }

    @Override
    public ClassLoader getClassLoader() {
        return persistenceUnitState.getClassLoader();
    }

    @Override
    public void addTransformer(ClassTransformer transformer) {
        persistenceUnitState.addTransformer(transformer);
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return persistenceUnitState.getNewTempClassLoader();
    }

    @Override
    public JPAPUnitInfo getPersistenceUnitState() {
        return persistenceUnitState;
    }

    @Override
    public String toString() {
        String identity = Integer.toHexString(System.identityHashCode(this));
        return "PersistenceUnitInfoAdapter40@" + identity + "[" + persistenceUnitState.getIvArchivePuId() + ", " + componentName + "]";
    }
}
