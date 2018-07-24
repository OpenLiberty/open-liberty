/*******************************************************************************
 * Copyright (c) 2005,2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.management;

import static com.ibm.ws.jpa.management.JPAConstants.JNDI_NAMESPACE_JAVA_APP_ENV;
import static com.ibm.ws.jpa.management.JPAConstants.JNDI_NAMESPACE_JAVA_COMP_ENV;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jpa.JPAComponent;
import com.ibm.ws.jpa.JPAProviderIntegration;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.ws.util.ThreadContextAccessor;

/**
 * Internal representation of a persistence unit in the form of a PersistenceUnitInfo object.
 */
public abstract class JPAPUnitInfo implements PersistenceUnitInfo {
    private static final String CLASS_NAME = JPAPUnitInfo.class.getName();

    private static final TraceComponent tc = Tr.register(JPAPUnitInfo.class, JPA_TRACE_GROUP, JPA_RESOURCE_BUNDLE_NAME);
    private static final TraceComponent tcTransformer = Tr.register(CLASS_NAME + "_Transformer", JPAPUnitInfo.class, JPA_TRACE_GROUP + ".Transformer", JPA_RESOURCE_BUNDLE_NAME);

    private static final ThreadContextAccessor svThreadContextAccessor = AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction()); // PM27213

    protected final JPAApplInfo ivApplInfo;

    // ---- PersistenceUnitInfo required attributes. ----
    // Name of this persistence unit.  getModJarName returns an archive name,
    // not a module name.
    protected final JPAPuId ivArchivePuId;

    // Transaction Type, i.e. JTA or ResourceLocal
    private PersistenceUnitTransactionType ivTxType = null;

    // Persistence unit description.
    private String ivDesc = null;

    // Fully package qualified class name of the persistence provider used for this persistence
    // unit.
    private String ivProviderClassName = null;

    // JTA DataSource object used, if specified.
    private DataSource ivJtaDataSource = null;

    // Non-JTA DataSource object used, if specified.
    private DataSource ivNonJtaDataSource = null;

    // Collection of the mapping file names, if specified.
    private List<String> ivMappingFileNames = null;

    // Collection of the jar file URLs, if specified.
    private List<URL> ivJarFileURLs = null;

    // Collection of the managed POJO entity class names, if specified.
    private List<String> ivManagedClassNames = null;

    // Indicator to exclude unlist classes for managed POJO entity search.
    private boolean ivExcludeUnlistedClasses = false;

    // Collection of additional properties used to create the EntityManager.
    private Properties ivProperties = null;

    // ---- JPAPUnitInfo extended information. ----
    // JNDI Name of the JTA DataSource, if specified.
    private String ivJtaDataSourceJNDIName = null;

    // JNDI Name of the non-JTA DataSource, if specified.
    private String ivNonJtaDataSourceJNDIName = null;

    // URL of the root of this persistence unit.
    private URL ivPUnitRootURL = null;

    // Application class loader used by the persistence provider.
    protected final ClassLoader ivClassLoader;

    // Temporary application class loader used by the persistence provider.
    private ClassLoader tempClassLoader = null;

    // List of provider transformers registered to be called when application classes are loaded.
    private List<ClassTransformer> ivTransformers = null;

    // XML Schema version string
    private String xmlSchemaVersion = null;

    // Caching
    private SharedCacheMode ivCaching = null; // F743-8705

    // ValidataionMode
    private ValidationMode ivValidationMode = null; // F743-8705

    // EntityManagerFactory associated with this persistence unit (non java:comp/env).
    private EntityManagerFactory ivEMFactory = null; // d510184

    /**
     * The failure that occurred while creating {@link #ivEMFactory}.
     */
    private RuntimeException ivEMFactoryException; // d743091

    // Creation of EntityManager factories for this PU is allowed.
    private boolean ivCreateEMFAllowed = true; // d510184

    // EntityManager for every java:comp, when Datasource is in java:comp.
    private Map<J2EEName, EntityManagerFactory> ivEMFMap = null; // d510184

    // An EntityManager pool for every persistence context reference defined.
    private Map<String, JPAEMPool> ivEMPoolMap = null; // d510184

    // EntityManager pool capacity for this persistence unit.
    private int ivEMPoolCapacity = -1; // d510184

    // A regular expression uses to filter class name that does not require JPA class
    // transformation.
    //
    // Note: Entity_xxxxxxxx.java is a valid entity but very unlikely class name. Agreed to
    //  defer to remove this reg ex pattern until problem arise.
    private static String[] transformExclusionRegEx = { ".*_(Stub|Tie)$", ".*_(\\p{XDigit}){8}$" };

    // A one to one mapping to the transformExclusionRegEx in the form of a java.util.regex.Pattern
    // objects.
    private static final Pattern[] transformExclusionPatterns;

    // When this Class object is loaded, determine the default JPA provider used.
    static {
        // Regular expression filters to avoid calling provider's class transformer.
        transformExclusionPatterns = new Pattern[transformExclusionRegEx.length];
        for (int i = 0; i < transformExclusionRegEx.length; ++i) {
            transformExclusionPatterns[i] = Pattern.compile(transformExclusionRegEx[i]);
        }
    }

    /**
     * Constructor.
     *
     * @param string
     *
     * @param pUnitName
     */
    protected JPAPUnitInfo(JPAApplInfo applInfo, JPAPuId puId, ClassLoader loader) // d416151 d458689 d473432.1
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "<init> : " + puId);

        ivApplInfo = applInfo;
        ivArchivePuId = puId;
        ivTxType = PersistenceUnitTransactionType.JTA;
        ivJarFileURLs = new ArrayList<URL>();
        ivManagedClassNames = new ArrayList<String>();
        ivMappingFileNames = new ArrayList<String>();
        ivTransformers = new CopyOnWriteArrayList<ClassTransformer>(); // PM77840
        ivClassLoader = loader; // d473432.1
        ivEMPoolMap = new HashMap<String, JPAEMPool>(); // d510184

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "<init>");
    }

    protected AbstractJPAComponent getJPAComponent() // d646413.1
    {
        return ivApplInfo.getJPAComponent();
    }

    /**
     * Remove all leading and trailing white spaces comes from parsing <persistence-unit> in
     * persistence.xml.
     */
    private final String trim(String str) {
        if (str != null) {
            str = str.trim();
        }
        return str;
    }

    /**
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceUnitName()
     */
    @Override
    public final String getPersistenceUnitName() {
        return ivArchivePuId.getPuName();
    }

    /**
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getTransactionType()
     */
    @Override
    public final PersistenceUnitTransactionType getTransactionType() {
        return ivTxType;
    }

    final void setTransactionType(PersistenceUnitTransactionType newValue) {
        if (newValue == null) {
            // if newValue is not specified, default to PersistenceUnitTransactionType.JTA
            // if running on the server environment, to PersistenceUnitTransactionType.RESOURCE_LOCAL
            // if running on the client environment.
            boolean serverRT = ivApplInfo.getJPAComponent().isServerRuntime();

            ivTxType = (serverRT) ? PersistenceUnitTransactionType.JTA : PersistenceUnitTransactionType.RESOURCE_LOCAL;
        } else {
            ivTxType = newValue;
        }
    }

    /**
     * Returns this persistence unit description
     */
    public final String getPersistenceUnitDescription() {
        return ivDesc;
    }

    final void setPersistenceUnitDescription(String newValue) {
        ivDesc = (newValue == null) ? "" : trim(newValue);
    }

    /**
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceProviderClassName()
     */
    @Override
    public final String getPersistenceProviderClassName() {
        return ivProviderClassName;
    }

    final void setPersistenceProviderClassName(String newValue) {
        // If no newValue is specified, use the system default persistence provider.
        // Need to get the default JPA Provider Class name from JPAComponent as that takes into
        // account the configuration property for the default provider and if that isn't set it
        // will call to the integration layer for the default.
        ivProviderClassName = trim(newValue == null ? getJPAComponent().getDefaultJPAProviderClassName() : newValue);
    }

    // d473432.1 Ends

    /*
     * 6.2.1.5 jta-data-source, non-jta-data-source
     *
     * "In Java EE environments, the jta-data-source and non-jta-data-source elements are used to
     * specify the global JNDI name of the JTA and/or non-JTA data source to be used by the
     * persistence provider. If neither is specified, the deployer must specify a JTA data source at
     * deployment or a JTA data source must be provided by the container, and a JTA
     * EntityManagerFactory will be created to correspond to it. These elements name the data source
     * in the local environment; the format of these names and the ability to specify the names are
     * product specific. In Java SE environments, these elements may be used or the data source
     * information may be specified by other means, depending upon the requirements of the provider."
     *
     * 8.2.1.5 jta-data-source, non-jta-data-source (updated for JPA 2.1)
     *
     * "In Java EE environments, the jta-data-source and non-jta-data-source elements are used to
     * specify the JNDI name of the JTA and/or non-JTA data source to be used by the persistence provider.
     * If neither is specified, the deployer must specify a JTA data source at deployment or the default
     * JTA data source must be provided by the container, and a JTA EntityManagerFactory will be created to
     * correspond to it."
     *
     * 7.1.1 Responsibilities of the Container
     *
     * Provider or data source information not specified in the persistence.xml file must be
     * provided at deployment time or defaulted by the container.
     *
     * We have extended the ability to use Resource reference under java:comp/env namespace to allow
     * data source to be configured dynamically using existing capability.
     *
     * @param dataSourceName
     *
     * @param defDataSourceName
     *
     * @return DataSource associates with dataSourceName or null if none is found.
     */
    private DataSource getJPADataSource(String dsName) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getJPADataSource : " + dsName + ", " + ivArchivePuId);

        DataSource ds = null;

        if (dsName != null) {
            try {
                // If the 'base' EM Factory has not been set yet, then the
                // application is starting and a java:comp/env datasource cannot
                // be resolved. So, just return a 'generic' datasource, that should
                // satisfy the provider, though will never actually be used. d510184
                if (ivEMFactory == null &&
                    (dsName.startsWith(JNDI_NAMESPACE_JAVA_COMP_ENV) ||
                     dsName.startsWith(JNDI_NAMESPACE_JAVA_APP_ENV))) {
                    ds = new GenericDataSource(ivArchivePuId, dsName);
                }

                // Otherwise, the component context should be set, or this is a
                // datasource in the global namespace; look it up now.       d510184
                else {
                    ds = lookupDataSource(dsName);
                }
            } catch (NamingException ex) {
                // If the 'base' EM Factory has not been set yet, then the
                // application is starting and we will defer the logging of
                // the missing DataSource until the PU is first used...      d543082
                if (ivEMFactory == null) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "getJPADataSource : failed, " +
                                     "returning GenericDataSource : " + ex);
                    ds = new GenericDataSource(ivArchivePuId, dsName);
                } else if (getJPAComponent().isIgnoreDataSourceErrors()) {
                    Tr.error(tc, "UNABLE_TO_GET_DATASOURCE_FOR_PU_CWWJP0013E",
                             dsName, ivArchivePuId.getPuName(), ex.toString());
                } else {
                    // Throw an exception, which will hopefully propagate through the
                    // persistence provider.                                RTC114812
                    String message = Tr.formatMessage(tc, "UNABLE_TO_GET_DATASOURCE_FOR_PU_CWWJP0013E",
                                                      dsName, ivArchivePuId.getPuName(), ex.toString());
                    PersistenceException ex2 = new PersistenceException(message, ex);

                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "getJPADataSource", ex2);
                    throw ex2;
                }
            }
        } //if (dsName != null) {

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getJPADataSource : " + ds);
        return ds;
    }

    protected abstract DataSource lookupDataSource(String dsName) throws NamingException;

    /**
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getJtaDataSource()
     */
    @Override
    public final DataSource getJtaDataSource() {
        if (ivJtaDataSource == null || ivJtaDataSource instanceof GenericDataSource) { // d455055
            ivJtaDataSource = getJPADataSource(ivJtaDataSourceJNDIName);
        }
        return ivJtaDataSource;
    }

    final void setJtaDataSource(String jndiName) {
        ivJtaDataSourceJNDIName = jndiName != null ? jndiName.trim() : null;
    }

    /**
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getNonJtaDataSource()
     */
    @Override
    public final DataSource getNonJtaDataSource() {
        if (ivNonJtaDataSource == null || ivNonJtaDataSource instanceof GenericDataSource) { // d455055
            ivNonJtaDataSource = getJPADataSource(ivNonJtaDataSourceJNDIName);
        }
        return ivNonJtaDataSource;
    }

    final void setNonJtaDataSource(String jndiName) {
        ivNonJtaDataSourceJNDIName = jndiName != null ? jndiName.trim() : null;
    }

    /**
     * Returns the JTA-enabled data source to be used by the persistence
     * provider. The data source corresponds to the <jta-data-source>
     * element in the persistence.xml file or is provided at deployment
     * or by the container. <p>
     *
     * Same function as getJtaDataSource, except will never return a cached
     * data source. This is used when the data source has been configured
     * in the component context, java:comp/env. <p>
     *
     * @see com.ibm.ws.jpa.management.JPACompPUnitInfo
     **/
    // d510184
    DataSource lookupJtaDataSource() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "lookupJtaDataSource : " + ivArchivePuId);

        DataSource jpaDS = getJPADataSource(ivJtaDataSourceJNDIName);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "lookupJtaDataSource : " + jpaDS);

        return jpaDS;
    }

    /**
     * Returns the non-JTA-enabled data source to be used by the persistence
     * provider. The data source corresponds to the <non-jta-data-source>
     * element in the persistence.xml file or is provided at deployment
     * or by the container. <p>
     *
     * Same function as getNonJtaDataSource, except will never return a cached
     * data source. This is used when the data source has been configured
     * in the component context, java:comp/env. <p>
     *
     * @see com.ibm.ws.jpa.management.JPACompPUnitInfo
     **/
    // d510184
    DataSource lookupNonJtaDataSource() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "lookupNonJtaDataSource : " + ivArchivePuId);

        DataSource jpaDS = getJPADataSource(ivNonJtaDataSourceJNDIName);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "lookupNonJtaDataSource : " + jpaDS);

        return jpaDS;
    }

    /**
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getMappingFileNames()
     */
    @Override
    public final List<String> getMappingFileNames() {
        return ivMappingFileNames;
    }

    final void setMappingFileNames(List<String> newValues) {
        ivMappingFileNames.clear();
        addMappingFileNames(newValues);
    }

    private void addMappingFileNames(List<String> newValues) {
        // 6.2.1.6 mapping-file, jar-file, class, exclude-unlisted-classes
        //
        // An object/relational mapping XML file contains mapping information for the classes listed
        // in it. A object/relational mapping XML file named orm.xml may be specified in the
        // META-INF directory in the root of the persistence unit or in the META-INF directory of
        // any jar file referenced by the persistence.xml. Alternatively, or in addition, other
        // mapping files may be referenced by the mapping-file elements of the persistence-unit
        // element, and may be present anywhere on the class path. An orm.xml file or other mapping
        // file is loaded as a resource by the persistence provider.
        for (String ormFName : newValues) {
            ivMappingFileNames.add(trim(ormFName));
        }
    }

    /**
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getJarFileUrls()
     */
    @Override
    public final List<URL> getJarFileUrls() {
        return ivJarFileURLs;
    }

    /**
     * Set the URLs for the jar-file entries in the persistence.xml
     * This method accommodates both standard and RAD/loose-config
     * environment.
     *
     * @param jarFileValues List of jar file paths from <jar-file> in persistence.xml
     * @param looseConfig class holding loose config mappings
     */
    //PK62950
    final void setJarFileUrls(List<String> jarFilePaths, JPAPXml pxml) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "setJarFileUrls: root=" + ivPUnitRootURL.toExternalForm());

        // 6.2.1.6 mapping-file, jar-file, class, exclude-unlisted-classes
        //
        // One or more JAR files may be specified using the jar-file elements instead of, or in
        // addition to the mapping files specified in the mapping-file elements. If specified,
        // these JAR files will be searched for managed persistence classes, and any mapping
        // metadata annotations found on them will be processed, or they will be mapped using
        // the mapping annotation defaults defined by this specification. Such JAR files are
        // specified relative to the root of the persistence unit (e.g., utils/myUtils.jar).
        //
        // Note: See defect 413031 details for clarifications by spec owner regarding
        //       "relative to the root of the persistence unit" semantics

        // The following code will loop through all entries in the <jar-file> stanza in
        // the persistence.xml and determine the URL to the jar file regardless of whether
        // we are running in standard or RAD/loose-config environments.

        ivJarFileURLs.clear();
        for (String jarFilePath : jarFilePaths) {
            if (!addJarFileUrls(trim(jarFilePath), pxml)) {
                Tr.error(tc, "INCORRECT_PU_JARFILE_URL_SPEC_CWWJP0024E", jarFilePath, getPersistenceUnitName());
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            URL[] allURLs = ivJarFileURLs.toArray(new URL[0]);
            Tr.exit(tc, "setJarFileUrls : " + Arrays.toString(allURLs));
        }
    }

    protected abstract boolean addJarFileUrls(String jarPath, JPAPXml pxml);

    protected void addJarFileUrl(URL url) {
        ivJarFileURLs.add(url);
    }

    /**
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getManagedClassNames()
     */
    @Override
    public final List<String> getManagedClassNames() {
        return ivManagedClassNames;
    }

    final void setManagedClassNames(List<String> newValues) {
        ivManagedClassNames.clear();
        addManagedClassNames(newValues);
    }

    void addManagedClassNames(List<String> newValues) {
        for (String className : newValues) {
            ivManagedClassNames.add(trim(className));
        }
    }

    /**
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#excludeUnlistedClasses()
     */
    @Override
    public final boolean excludeUnlistedClasses() {
        return ivExcludeUnlistedClasses;
    }

    final void setExcludeUnlistedClasses(boolean newValue) {
        ivExcludeUnlistedClasses = newValue;
    }

    /**
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getProperties()
     */
    @Override
    public final Properties getProperties() {
        return ivProperties;
    }

    final void setProperties(Properties newValues) {
        ivProperties = new Properties();

        if (newValues != null) {
            for (Map.Entry<Object, Object> entry : newValues.entrySet()) {
                // We probably shouldn't trim the value, but we can't change
                // that now without risking breaking backwards compatibility.
                ivProperties.put(entry.getKey(), trim((String) entry.getValue()));
            }
        }

        getJPAComponent().getJPAProviderIntegration().updatePersistenceUnitProperties(ivProviderClassName, ivProperties);
    }

    /**
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceUnitRootUrl()
     */
    @Override
    public final URL getPersistenceUnitRootUrl() {
        return ivPUnitRootURL;
    }

    protected void setPersistenceUnitRootUrl(URL newValue) {
        ivPUnitRootURL = newValue;
    }

    final JPAPuId getPuId() // d689596
    {
        return ivArchivePuId;
    }

    public final String getApplName() {
        return ivApplInfo.getApplName();
    }

    /**
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getClassLoader()
     */
    @Override
    public final ClassLoader getClassLoader() {
        return ivClassLoader;
    }

    protected abstract boolean registerClassFileTransformer(ClassLoader classLoader);

    protected abstract void unregisterClassFileTransformer(ClassLoader classLoader);

    /**
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getNewTempClassLoader()
     */
    @Override
    public final synchronized ClassLoader getNewTempClassLoader() {
        if (tempClassLoader == null) {
            tempClassLoader = createTempClassLoader(ivClassLoader);
        }

        return tempClassLoader;
    }

    protected abstract ClassLoader createTempClassLoader(ClassLoader classLoader);

    /**
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#addTransformer(javax.persistence.spi.ClassTransformer)
     */
    @Override
    public final void addTransformer(ClassTransformer transformerClass) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "addTransformer: PUID = " + ivArchivePuId + ", transformer = " + transformerClass); //d454146
        }

        if (transformerClass != null) {
            JPAComponent jpaComp = getJPAComponent();
            if (jpaComp != null && jpaComp.getCaptureEnhancedEntityClassBytecode()) {
                transformerClass = new CapturingClassTransformer(transformerClass, ivApplInfo.getApplName(), jpaComp.getServerLogDirectory());
            }
        }

        ivTransformers.add(transformerClass);

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "addTransformer : # registered transfromer = " + ivTransformers.size());
        }
    }

    /**
     * Creates an EntityManagerFactory for this Persistence Unit. <p>
     *
     * This method must be called only once, and must be called when
     * the application is starting. Other functions on JPAPUnitInfo
     * will not work until an EntityManagerFactory has been created. <p>
     *
     * An EntityManagerFactory must be created when an application starts,
     * so that the JPA Provider has the opportunity to register a class
     * transformer. <p>
     *
     * Note: When the datasource for this PU is in the java:comp/env
     * name space, the EntityManagerFactory created by this method
     * will never be used. Instead, additional EMFs will be created
     * as needed, for each java:comp/env name space. This allows the
     * provider to register a class transformer during application start,
     * and still have access to the proper database for EMFs that
     * will actually be used. <p>
     **/
    // d510184
    void initialize() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "createEntityManagerFactory : " + ivArchivePuId);

        if (ivEMFactory != null) {
            throw new IllegalStateException("EntityManagerFactory already created for PU : " + ivArchivePuId);
        }

        if (!registerClassFileTransformer(ivClassLoader)) {
            Tr.warning(tc, "APPL_CLASSLOADER_USE_HAS_NO_JPA_SUPPORT_CWWJP0005W", ivArchivePuId.getPuName(), ivPUnitRootURL, ivClassLoader.getClass().getName());
            tempClassLoader = ivClassLoader;
        }

        // First, determine the final data source JNDI names.
        AbstractJPAComponent jpaComponent = getJPAComponent();
        ivJtaDataSourceJNDIName = jpaComponent.getDataSourceBindingName(ivJtaDataSourceJNDIName, true);
        ivNonJtaDataSourceJNDIName = jpaComponent.getDataSourceBindingName(ivNonJtaDataSourceJNDIName, false);

        // JPA 2.1 Spec 8.2.1.5
        // If neither is specified, the deployer must specify a JTA data source at deployment or the default
        // JTA data source must be provided by the container, and a JTA EntityManagerFactory will be created to
        // correspond to it.
        ivJtaDataSourceJNDIName = jpaComponent.getJPARuntime().processJEE7JTADataSource(ivJtaDataSourceJNDIName, ivNonJtaDataSourceJNDIName);

        // Before creating the 'base' EntityManagerFactory which will
        // register any class transformers, check to see if a different
        // EMF is needed per component, and if so, create the map to hold
        // them.  When 'java:comp/env' is used for a datasource, then
        // every component may map to a different database, and thus
        // a different EMF is required for each.
        if ((ivJtaDataSourceJNDIName != null && ivJtaDataSourceJNDIName.startsWith("java:comp/env")) ||
            (ivNonJtaDataSourceJNDIName != null && ivNonJtaDataSourceJNDIName.startsWith("java:comp/env"))) {
            ivEMFMap = new HashMap<J2EEName, EntityManagerFactory>();
        } else {
            // If neither datasource is in the 'java:comp/env' namespace, then
            // make sure any configured datasource can be found. If not, then
            // a GenericDataSource will be used to allow the EM Factory to
            // create... which cannot be used later, so an EM Factory Map must
            // be created to allow a valid EM Factory to be created later, on
            // first use.  If the datasource is still not valid at that point,
            // then the lookup failure will occur at that time. The real intent
            // here is to allow an application with a persistence.xml file that
            // is never used to start without error, yet still provide a
            // meaningful exception for those applications that are just
            // configured incorrectly.                                      d543082
            if ((ivJtaDataSourceJNDIName != null && getJtaDataSource() instanceof GenericDataSource) ||
                (ivNonJtaDataSourceJNDIName != null && getNonJtaDataSource() instanceof GenericDataSource)) {
                ivEMFMap = new HashMap<J2EEName, EntityManagerFactory>();
            }
        }

        // Also determine the EM Pool capacity.  If not set (-1), then a default
        // will be assigned for an openjpa provider, but not other providers, as
        // tests have shown others not to work well with pooling.          d510184
        ivEMPoolCapacity = jpaComponent.getEntityManagerPoolCapacity(); // F743-18776

        if (ivEMPoolCapacity < 0 && jpaComponent.getJPAProviderIntegration().supportsEntityManagerPooling()) {
            ivEMPoolCapacity = JPAConstants.DEFAULT_EM_POOL_CAPACITY;
        }

        try {
            ivEMFactory = createEMFactory(this, true);
        } catch (RuntimeException ex) {
            ivEMFactoryException = ex; // d743091
        }
    }

    /**
     * Returns a container EntityManagerFactory associated with this
     * persistence unit for the specified Java EE component. <p>
     *
     * When the datasources for this persistence unit are in the global
     * naming context, the same EntityManagerFactory will be returned for
     * all Java EE components using this persistence unit. <p>
     *
     * However, when either datasource is defined in the component naming
     * context (java:comp/env), a different instance will be returned for
     * every Java EE component that has a reference defined for this
     * persistence unit. <p>
     *
     * When a datasource is defined in the component naming context, this
     * method may result in a new EntityManagerFactory instance being
     * created. <p>
     *
     * @param j2eeName
     *            JavaEE unique identifier for the component, identifying the
     *            java:comp/env context used.
     *
     * @return EntityManager factory associated with this persistence unit.
     **/
    // d510184
    EntityManagerFactory getEntityManagerFactory(J2EEName j2eeName) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getEntityManagerFactory : " + ivArchivePuId + ", " + j2eeName);

        // This is an internal error if it occurs, so not going to provide
        // a system log or elaborate message.
        if (ivEMFactoryException != null) // d743091
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getEntityManagerFactory : " + ivEMFactoryException);
            // Wrap in another RuntimeException to capture the current stack trace.
            throw new RuntimeException(ivEMFactoryException); // d743091, RTC114812
        }

        // This is an internal error if it occurs, so not going to provide
        // a system log or elaborate message.
        if (j2eeName == null) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getEntityManagerFactory : IllegalArgumentException");
            throw new IllegalArgumentException("Component identifier must be provided  : " + ivArchivePuId);
        }

        // Assume the EMF to be returned is the one created during app start.
        EntityManagerFactory emf = ivEMFactory;

        // An EntityManagerFactory Map is only created if one of the datasrouces
        // has been defined in java:comp/env.  When this is true, a component
        // specific EMF needs to be obtained from the map, or created and added
        // to the map.
        if (ivEMFMap != null) {
            synchronized (ivEMFMap) {
                emf = ivEMFMap.get(j2eeName);

                if (emf == null) {
                    if (ivCreateEMFAllowed) {
                        PersistenceUnitInfo puInfo = new JPACompPUnitInfo(ivArchivePuId, this, j2eeName);
                        emf = createEMFactory(puInfo, false);
                        ivEMFMap.put(j2eeName, emf);

                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "new emf added to EMF Map for : " + j2eeName);
                    } else {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "PU closed; base emf returned for : " + j2eeName);

                        emf = ivEMFactory;
                    }
                } else {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "emf obtained from EMF Map for : " + j2eeName);
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getEntityManagerFactory : " + emf);

        return emf;
    }

    /**
     * Common internal method to create an EntityManagerFactory for this
     * Persistence Unit. <p>
     *
     * Contains the actual logic to invoke the provider to create a container
     * EntityManagerFactory. <p>
     *
     * Called from both createEntityManagerFactory (during application start)
     * and as needed when the datasource is identifed in java:comp/env. <p>
     *
     * The specified PersistenceUnitInfo will typically be 'this' object,
     * except when one of the datasources has been configured in the
     * component context (java:comp/env); in which case it will be a
     * component specific instance of JPACompPUnitInfo. <p>
     *
     * @param puInfo persistence unit information to pass on the call to
     *            createEntityManagerFactory.
     * @param ignoreProviderCNFE whether or not a CNFE should be logged as an FFDC
     *            (provider CNFE's are tolerated for WABs per defect 152577)
     * @throws RuntimeException if an error occurs while creating the EMF
     **/
    // d510184
    private EntityManagerFactory createEMFactory(PersistenceUnitInfo puInfo, boolean ignoreProviderCNFE) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "createEMFactory : " + puInfo);
        }
        Map<String, Object> integrationProperties = new HashMap<String, Object>();
        JPAProviderIntegration integration = getJPAComponent().getJPAProviderIntegration();
        integration.updatePersistenceProviderIntegrationProperties(puInfo, integrationProperties);

        // When creating the base EntityManagerFactory with GenericDataSource
        // (for java:comp/env), suppress the info and warning messages
        // that indicate the GenericDataSource is not supported.        d510184
        if (ivEMFactory == null && ivEMFMap != null) {
            integration.disablePersistenceUnitLogging(integrationProperties);
        }

        // Starting with JPA 2.0, the application server must make the
        // ValidatorFactory available to the provider.          F743-12524 PM65716
        getJPAComponent().addIntegrationProperties(xmlSchemaVersion,
                                                   integrationProperties);

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(tc, "createContainerEMF properties:" + this.toString());
            Tr.debug(tc, "createContainerEMF integration-properties:" +
                         integrationProperties);
        }

        EntityManagerFactory emfactory;
        Object oldClassLoader = svThreadContextAccessor.pushContextClassLoaderForUnprivileged(ivClassLoader);
        try {
            Class<?> providerClass = ivClassLoader.loadClass(ivProviderClassName);
            PersistenceProvider provider = (PersistenceProvider) providerClass.newInstance();

            // Use properties defined in default persistence providers in factory creation.
            // Properties defined in PU are used in createEntityManager to override factory settings.
            emfactory = provider.createContainerEntityManagerFactory(puInfo,
                                                                     integrationProperties);
        } catch (PersistenceException e) {
            FFDCFilter.processException(e, CLASS_NAME + ".createEMFactory",
                                        "759", this);
            Tr.error(tc,
                     "CREATE_CONTAINER_ENTITYMANAGER_FACTORY_ERROR_CWWJP0015E",
                     ivProviderClassName, ivArchivePuId.getPuName(), e.getLocalizedMessage());
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "createEMFactory : null", e);
            throw e; // d743091
        } catch (ClassNotFoundException cnfe) {
            // ClassNotFoundException is expected during module start for WABs since they don't actually use JPAPUnitInfo.
            // Perhaps the module start code flow should be completely disabled for them instead?
            if (!ignoreProviderCNFE) {
                FFDCFilter.processException(cnfe, CLASS_NAME + ".createEMFactory", "1168", this);
                Tr.error(tc, "JPA_PROVIDER_NOT_FOUND_CWWJP0050E", ivProviderClassName);
            }
            String nlsMessage = Tr.formatMessage(tc, "JPA_PROVIDER_NOT_FOUND_CWWJP0050E", ivProviderClassName);
            throw new RuntimeException(nlsMessage, cnfe);
        } catch (Exception e) {
            // Combined catch clause for IllegalAccessException and InstantiationException
            FFDCFilter.processException(e, CLASS_NAME + ".createEMFactory", "773", this);
            Tr.error(tc,
                     "CREATE_CONTAINER_ENTITYMANAGER_FACTORY_ERROR_CWWJP0015E",
                     ivProviderClassName, ivArchivePuId.getPuName(), e);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "createEMFactory", e);
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e); // d743091
        } finally {
            if (isTraceOn && tc.isDebugEnabled() && oldClassLoader != ThreadContextAccessor.UNCHANGED)
                Tr.debug(tc, "reverting class loader to " + oldClassLoader);
            svThreadContextAccessor.popContextClassLoaderForUnprivileged(oldClassLoader);
        }

        // Indicates an error to log with problem creating a factory, post once only.
        if (emfactory == null) {
            Tr.error(tc, "UNABLE_TO_CREATE_ENTITY_MANAGER_FACTORY_CWWJP0009E",
                     ivArchivePuId.getPuName(), ivProviderClassName, ivPUnitRootURL);
            String message = "EntityManagerFactory has not been created for PU : " + ivArchivePuId;
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "createEMFactory : IllegalStateException");
            throw new IllegalStateException(message); // d743091
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createEMFactory : " + emfactory);

        return emfactory;
    }

    /**
     * Returns the pool of EntityManagers that may be used for the specified
     * Java EE component and PersistenceContext reference. <p>
     *
     * When the PU datasource is defined in java:comp/env, each component
     * may may use a different datasource, and therefore a different
     * EntityManagerFactory must be used per component. <p>
     *
     * Also, since each PersistenceContext reference may define custom
     * properties, it may not be possible to pool EntityManager instances
     * accross PersistenceContext references. <p>
     *
     * @param j2eeName
     *            JavaEE unique identifier for the component, identifying the
     *            java:comp/env context used.
     * @param refName
     *            Name of the PersistenceContext reference.
     * @param properties
     *            additional properties to create the EntityManager
     *
     * @return EntityManager pool for the specified component and reference.
     **/
    // d510184
    JPAEMPool getEntityManagerPool(J2EEName j2eeName,
                                   String refName,
                                   Map<?, ?> properties) {
        JPAEMPool emPool = null;
        String poolKey = j2eeName.toString() + "#" + refName;

        synchronized (ivEMPoolMap) {
            emPool = ivEMPoolMap.get(poolKey);

            if (emPool == null) {
                EntityManagerFactory emf = getEntityManagerFactory(j2eeName);
                emPool = new JPAEMPool(emf, properties, ivEMPoolCapacity, this, getJPAComponent()); //d638095.1, d743325
                ivEMPoolMap.put(poolKey, emPool);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getEntityManagerPool : " + poolKey + " : " + emPool);

        return emPool;
    }

    /**
     * Close the entity manager factory if it exists and is in open state per JPA Spec section
     * 5.8.1.
     */
    final void close() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "close : " + ivArchivePuId, this);

        // First, prevent any further EntityManager factories from being
        // created. EMFs may be created for each java:comp/env context.
        if (ivEMFMap != null) {
            synchronized (ivEMFMap) {
                ivCreateEMFAllowed = false;
            }
        }

        // Next, shutdown any EntityManager pools that exist.  This will close
        // all of the pooled EMs and prevent the pools from adding any more EMs.
        // This does NOT close any EMs that customers may be actively using...
        // and it is also possible a new pool may be created prior to closing
        // the EMFs below, but the EMF close is gauranteed to close all EMs
        // associated with it.                                             d510184
        synchronized (ivEMPoolMap) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "shutting down EM pools : " + ivEMPoolMap.size());

            for (JPAEMPool emPool : ivEMPoolMap.values()) {
                emPool.shutdown();
            }
        }

        unregisterClassFileTransformer(ivClassLoader);

        // Finally, close the base EMF, and any EMFs that have been created
        // for each java:comp/env.
        if (ivEMFactory != null) {
            // d455363 Begins
            if (ivEMFactory.isOpen()) {
                try {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "closing base EMF");
                    ivEMFactory.close();
                } catch (Exception e) {
                    FFDCFilter.processException(e, CLASS_NAME + ".close",
                                                "934", this);

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Caught unexpected exception on factory.close():" + e);
                }
            }
            // d455363 Ends

            // Close all of the factories for each J2EEName when the datasource
            // is defined in java:comp/env.                                 d510184
            if (ivEMFMap != null) {
                for (EntityManagerFactory emFactory : ivEMFMap.values()) {
                    if (emFactory.isOpen()) {
                        try {
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "closing component EMF");
                            emFactory.close();
                        } catch (Exception e) {
                            FFDCFilter.processException(e, CLASS_NAME + ".close",
                                                        "934", this);

                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "Caught unexpected exception on factory.close():" + e);
                        }
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "close : " + ivArchivePuId);
    }

    /**
    *
    */
    StringBuilder toStringBuilder(StringBuilder sbuf) {
        return sbuf.append(ivArchivePuId.getPuName());
    }

    /**
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return (getClass().getSimpleName() + "(" + ivArchivePuId + ")@" + Integer.toHexString(hashCode()));
    }

    /**
     * Dumps the contents of an instance to a String for selective trace. <p>
     *
     * A full dump of this object can be quite large in trace, so the normal
     * toString provides important tracking information, and this dump method
     * is provided for use one time when the object is first created. <p>
     **/
    // F1879-16302
    String dump() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("\n").append(toString());
        sbuf.append("\n PersistenceUnit name : ").append(ivArchivePuId.getPuName());
        sbuf.append("\n Schema Version       : ").append(xmlSchemaVersion); //F743-8064
        sbuf.append("\t Archive name         : ").append(ivArchivePuId.getModJarName());
        sbuf.append("\t Application name     : ").append(ivArchivePuId.getApplName());
        sbuf.append("\n Root URL             : ").append(ivPUnitRootURL);
        sbuf.append("\n Transaction Type     : ").append(ivTxType);
        sbuf.append("\n Description          : ").append(ivDesc);
        sbuf.append("\n Provider class name  : ").append(ivProviderClassName);
        sbuf.append("\n JTA Data Source      : ").append(ivJtaDataSourceJNDIName).append(" | ").append(ivJtaDataSource);
        sbuf.append("\n Non JTA Data Source  : ").append(ivNonJtaDataSourceJNDIName).append(" | ").append(ivNonJtaDataSource);
        sbuf.append("\n ExcludeUnlistedClass : ").append(ivExcludeUnlistedClasses);
        sbuf.append("\n SharedCacheMode      : ").append(ivCaching); // d597764
        sbuf.append("\n ValidationMode       : ").append(ivValidationMode); // d597764
        sbuf.append("\n Properties           : ").append(ivProperties);

        boolean first;
        sbuf.append("\n Mapping Files        : [");
        if (ivMappingFileNames != null) {
            first = true;
            for (String fname : ivMappingFileNames) {
                sbuf.append(first ? "" : ",").append(fname);
                first = false;
            }
        }
        sbuf.append(']');

        sbuf.append("\n Jar Files            : [");
        if (ivJarFileURLs != null) {
            first = true;
            for (URL jarUrl : ivJarFileURLs) {
                sbuf.append(first ? "" : ",").append(jarUrl);
                first = false;
            }
        }
        sbuf.append(']');

        sbuf.append("\n ManagedClasses       : [");
        if (ivManagedClassNames != null) {
            first = true;
            for (String className : ivManagedClassNames) {
                sbuf.append(first ? "" : ",").append(className);
                first = false;
            }
        }
        sbuf.append(']');

        sbuf.append("\n ClassLoader          : ").append(ivClassLoader);
        sbuf.append("\n Temp ClassLoader     : ").append(tempClassLoader);

        sbuf.append("\n Transformer          : [");
        if (ivTransformers != null) {
            first = true;
            for (ClassTransformer transformer : ivTransformers) {
                sbuf.append(first ? "" : ",").append(transformer);
                first = false;
            }
        }
        sbuf.append(']');

        return sbuf.toString();
    }

    /**
     * Determine if the input class needs persistence provider class transformation using
     * a pre-defined regular expression filter.
     */
    private final boolean classNeedsTransform(String className) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "classNeedsTransform : PUID = " + ivArchivePuId + ", class name = " + className); //d454146
        }

        boolean rtnVal = true;
        for (Pattern regex : transformExclusionPatterns) {
            if (regex.matcher(className).matches()) {
                rtnVal = false;
                break;
            }
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "classNeedsTransform : " + className
                        + (rtnVal ? " needs" : " does not need") + " transform.");
        return rtnVal;
    }

    public byte[] transformClass(String className,
                                 byte[] classBytes,
                                 CodeSource codeSource,
                                 ClassLoader classloader) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tcTransformer.isEntryEnabled()) {
            Tr.entry(tcTransformer, "transformClass: PUID = " + ivArchivePuId + ", class name = " + className,
                     codeSource, classloader); //d454146
        }

        int numTransform = 0;
        if (ivTransformers.size() > 0 && classNeedsTransform(className)) {
            // perform the class transformation by the persistence provider only if it is
            // defined as a POJO entity class.
            ProtectionDomain pd = new ProtectionDomain(codeSource, new Permissions(), classloader, null);
            int oldClassBytesLength;

            // Future performance optimizatin:
            //  In same application that has more than 1 Pu, multiple transformers may be added to
            //  the same classloader. Multi-calls to the provider can be optimized to a single
            //  invocation.
            for (ClassTransformer transformer : ivTransformers) {
                oldClassBytesLength = classBytes.length;
                boolean isTransformed = false;
                try {
                    byte[] transformedClassBytes = transformer.transform(classloader,
                                                                         className,
                                                                         null,
                                                                         pd,
                                                                         classBytes);
                    if (transformedClassBytes != null) {
                        // replace and return the transformed classBytes back to the caller.
                        isTransformed = true;
                        classBytes = transformedClassBytes;
                        ++numTransform;
                    }
                    if (isTraceOn && ((tc.isDebugEnabled() && isTransformed) || tcTransformer.isDebugEnabled())) {
                        TraceComponent tcActive = tcTransformer.isDebugEnabled() ? tcTransformer : tc;
                        Tr.debug(tcActive, "transformer:" + transformer + ", " // d440322
                                           + className + " is " + (isTransformed ? "" : "NOT ")
                                           + "transformed. Byte length(old/new)=" + oldClassBytesLength + "/"
                                           + classBytes.length);
                    }
                } catch (IllegalClassFormatException icfe) {
                    FFDCFilter.processException(icfe, CLASS_NAME + ".transformClass",
                                                "1169", this);
                    // Ignore the exception and the original classBytes is returned.
                    Tr.error(tc,
                             "ILLEGAL_CLASS_FORMAT_IN_CLASS_TRANSFORMATION_CWWJP0014E",
                             className);
                }
            }
        }

        if (isTraceOn && tcTransformer.isEntryEnabled()) {
            Tr.exit(tcTransformer, "transformClass: " + numTransform + "/" + ivTransformers.size()); //d454146
        }
        return classBytes;
    }

    /**
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getPersistenceXMLSchemaVersion()
     **/
    // F743-954.1
    @Override
    public final String getPersistenceXMLSchemaVersion() // d603827
    {
        return xmlSchemaVersion;
    }

    /**
     * Internal method used to populate the Persistence Unit Info metadata.
     **/
    // F743-954.1
    void setPersistenceXMLSchemaVersion(String version) {
        xmlSchemaVersion = version;
    }

    /**
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getSharedCacheMode()
     **/
    // F743-8705
    @Override
    public final SharedCacheMode getSharedCacheMode() {
        return ivCaching;
    }

    /**
     * Internal method used to populate the Persistence Unit Info metadata.
     **/
    // F743-8705
    void setSharedCacheMode(SharedCacheMode value) {
        ivCaching = value;
    }

    /**
     * (non-Javadoc)
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getValidationMode()
     **/
    // F743-8705
    @Override
    public final ValidationMode getValidationMode() {
        return ivValidationMode;
    }

    /**
     * Internal method used to populate the Persistence Unit Info metadata.
     **/
    // F743-8705
    void setValidationMode(ValidationMode mode) {
        ivValidationMode = mode;
    }
}
