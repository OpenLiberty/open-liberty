/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.JPAPuId;

/**
 * Java EE Component specific implementation of a PersistenceUnitInfo. <p>
 * 
 * Most persistence unit information is static for all components. The
 * datasources (both jta and non-jta) are the exception, as these may
 * be configured to be a resource reference in the java:comp/env
 * component naming context. <p>
 * 
 * This class is used when one of the datasources has been configured
 * in java:comp/env. All methods, except the ones to obtain the datasources
 * are delegated to the 'common' PersistenceUnitInfo implementation.
 * This allows the datasources to be cached per component, rather than
 * per persistence unit. <p>
 */
final class JPACompPUnitInfo implements PersistenceUnitInfo
{
    private static final TraceComponent tc = Tr.register(JPACompPUnitInfo.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    // Persistence unit id.
    protected JPAPuId ivPuId;

    // The common (real) PUnitInfo (non component specific).
    private JPAPUnitInfo ivPUnitInfo;

    // JavaEE unique identifier for the component, identifying the
    // java:comp/env context used.
    private J2EEName ivJ2eeName;

    // Component specific JTA DataSource object used, if specificed.
    private DataSource ivJtaDataSource = null;

    // Component specific Non-JTA DataSource object used, if specificed.
    private DataSource ivNonJtaDataSource = null;

    /**
     * Constructor.
     * 
     * @param puId
     *            Persistence unit id
     * @param puInfo
     *            the common PUnitInfo
     * @param j2eeName
     *            JavaEE unique identifier for the component, identifying the
     *            java:comp/env context used.
     */
    JPACompPUnitInfo(JPAPuId puId, JPAPUnitInfo puInfo, J2EEName j2eeName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init> : " + puId + ", " + j2eeName);

        ivPuId = puId;
        ivPUnitInfo = puInfo;
        ivJ2eeName = j2eeName;
    }

    // --------------------------------------------------------------------------
    //
    // javax.persistence.spi.PersistenceUnitInfo  -  interface methods
    //
    // --------------------------------------------------------------------------

    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#addTransformer()
     */
    public void addTransformer(ClassTransformer transformerClass)
    {
        ivPUnitInfo.addTransformer(transformerClass);
    }

    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#excludeUnlistedClasses()
     */
    public boolean excludeUnlistedClasses()
    {
        return ivPUnitInfo.excludeUnlistedClasses();
    }

    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#getClassLoader()
     */
    public ClassLoader getClassLoader()
    {
        return ivPUnitInfo.getClassLoader();
    }

    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#getMappingFileNames()
     */
    public List<URL> getJarFileUrls()
    {
        return ivPUnitInfo.getJarFileUrls();
    }

    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#getJarFileUrls()
     */
    public DataSource getJtaDataSource()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getJtaDataSource : " + this);

        if (ivJtaDataSource == null)
        {
            ivJtaDataSource = ivPUnitInfo.lookupJtaDataSource();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getJtaDataSource : " + ivJtaDataSource);

        return ivJtaDataSource;
    }

    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#getManagedClassNames()
     */
    public List<String> getManagedClassNames()
    {
        return ivPUnitInfo.getManagedClassNames();
    }

    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#getMappingFileNames()
     */
    public List<String> getMappingFileNames()
    {
        return ivPUnitInfo.getMappingFileNames();
    }

    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#getNewTempClassLoader()
     */
    public ClassLoader getNewTempClassLoader()
    {
        return ivPUnitInfo.getNewTempClassLoader();
    }

    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#getNonJtaDataSource()
     */
    public DataSource getNonJtaDataSource()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getNonJtaDataSource : " + this);

        if (ivNonJtaDataSource == null)
        {
            ivNonJtaDataSource = ivPUnitInfo.lookupNonJtaDataSource();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getNonJtaDataSource : " + ivNonJtaDataSource);

        return ivNonJtaDataSource;
    }

    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceProviderClassName()
     */
    public final String getPersistenceProviderClassName()
    {
        return ivPUnitInfo.getPersistenceProviderClassName();
    }

    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceUnitName()
     */
    public String getPersistenceUnitName()
    {
        return ivPUnitInfo.getPersistenceUnitName();
    }

    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceUnitRootUrl()
     */
    public final URL getPersistenceUnitRootUrl()
    {
        return ivPUnitInfo.getPersistenceUnitRootUrl();
    }

    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#getProperties()
     */
    public final Properties getProperties()
    {
        return ivPUnitInfo.getProperties();
    }

    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#getTransactionType()
     */
    public final PersistenceUnitTransactionType getTransactionType()
    {
        return ivPUnitInfo.getTransactionType();
    }

    // New JPA 2.0 methods - F743-954.1
    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceXMLSchemaVersion()
     */
    public String getPersistenceXMLSchemaVersion() { // d603827
        return ivPUnitInfo.getPersistenceXMLSchemaVersion();
    }

    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#getSharedCacheMode()
     */
    public SharedCacheMode getSharedCacheMode() { // d602618
        return ivPUnitInfo.getSharedCacheMode(); // d602618
    }

    /**
     * @see javax.persistence.spi.PersistenceUnitInfo#getValidationMode()
     */
    public ValidationMode getValidationMode() {
        return ivPUnitInfo.getValidationMode();
    }

    // --------------------------------------------------------------------------
    //
    // internal  methods
    //
    // --------------------------------------------------------------------------

    /**
     * Overridden to provide meaningful trace output.
     */
    public String toString()
    {
        String identity = Integer.toHexString(System.identityHashCode(this));
        return "JPACompPUnitInfo@" + identity + "[" + ivPuId + ", " + ivJ2eeName + "]";
    }
}
