/*******************************************************************************
 * Copyright (c) 2008, 2026 IBM Corporation and others.
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
package com.ibm.ws.jpa.management;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.net.URL;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import jakarta.persistence.FetchType;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.JPAPuId;

/**
 * JPA 4.0 overlay of {@code JPACompPUnitInfo}.
 *
 * <p>The transformer-produced version of {@link #getTransactionType()} returns
 * {@code jakarta.persistence.spi.PersistenceUnitTransactionType}, but in JPA 4.0
 * the type moved to the top-level {@code jakarta.persistence} package and
 * {@code PersistenceUnitInfo.getTransactionType()} now requires the non-spi type.
 * This class is compiled directly against the JPA 4.0 API and is overlaid onto
 * the transformer-produced jar so the correct descriptor is used at runtime.
 */
final class JPACompPUnitInfo implements PersistenceUnitInfo {
    private static final TraceComponent tc = Tr.register(JPACompPUnitInfo.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    protected JPAPuId ivPuId;
    private final JPAPUnitInfo ivPUnitInfo;
    private final J2EEName ivJ2eeName;
    private DataSource ivJtaDataSource = null;
    private DataSource ivNonJtaDataSource = null;

    JPACompPUnitInfo(JPAPuId puId, JPAPUnitInfo puInfo, J2EEName j2eeName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init> : " + puId + ", " + j2eeName);
        ivPuId = puId;
        ivPUnitInfo = puInfo;
        ivJ2eeName = j2eeName;
    }

    @Override
    public void addTransformer(ClassTransformer transformerClass) {
        ivPUnitInfo.addTransformer(transformerClass);
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return ivPUnitInfo.excludeUnlistedClasses();
    }

    @Override
    public ClassLoader getClassLoader() {
        return ivPUnitInfo.getClassLoader();
    }

    @Override
    public List<URL> getJarFileUrls() {
        return ivPUnitInfo.getJarFileUrls();
    }

    @Override
    public DataSource getJtaDataSource() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getJtaDataSource : " + this);
        if (ivJtaDataSource == null) {
            ivJtaDataSource = ivPUnitInfo.lookupJtaDataSource();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getJtaDataSource : " + ivJtaDataSource);
        return ivJtaDataSource;
    }

    @Override
    public List<String> getManagedClassNames() {
        return ivPUnitInfo.getManagedClassNames();
    }

    @Override
    public List<String> getMappingFileNames() {
        return ivPUnitInfo.getMappingFileNames();
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return ivPUnitInfo.getNewTempClassLoader();
    }

    @Override
    public DataSource getNonJtaDataSource() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getNonJtaDataSource : " + this);
        if (ivNonJtaDataSource == null) {
            ivNonJtaDataSource = ivPUnitInfo.lookupNonJtaDataSource();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getNonJtaDataSource : " + ivNonJtaDataSource);
        return ivNonJtaDataSource;
    }

    @Override
    public final String getPersistenceProviderClassName() {
        return ivPUnitInfo.getPersistenceProviderClassName();
    }

    @Override
    public String getPersistenceUnitName() {
        return ivPUnitInfo.getPersistenceUnitName();
    }

    public final List<String> getQualifierAnnotationNames() {
        return ivPUnitInfo.getQualifierAnnotationNames();
    }

    public final String getScopeAnnotationName() {
        return ivPUnitInfo.getScopeAnnotationName();
    }

    @Override
    public final URL getPersistenceUnitRootUrl() {
        return ivPUnitInfo.getPersistenceUnitRootUrl();
    }

    @Override
    public final Properties getProperties() {
        return ivPUnitInfo.getProperties();
    }

    /**
     * Returns {@code jakarta.persistence.PersistenceUnitTransactionType} — the
     * top-level (non-spi) type introduced as the canonical location in JPA 4.0.
     */
    @Override
    public final PersistenceUnitTransactionType getTransactionType() {
        return ivPUnitInfo.getTransactionType();
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return ivPUnitInfo.getPersistenceXMLSchemaVersion();
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return ivPUnitInfo.getSharedCacheMode();
    }

    @Override
    public ValidationMode getValidationMode() {
        return ivPUnitInfo.getValidationMode();
    }

    @Override
    public List<String> getAllClassNames() {
        return ivPUnitInfo.getAllClassNames();
    }

    @Override
    public FetchType getDefaultToOneFetchType() {
        return ivPUnitInfo.getDefaultToOneFetchType();
    }

    @Override
    public String toString() {
        String identity = Integer.toHexString(System.identityHashCode(this));
        return "JPACompPUnitInfo@" + identity + "[" + ivPuId + ", " + ivJ2eeName + "]";
    }
}
