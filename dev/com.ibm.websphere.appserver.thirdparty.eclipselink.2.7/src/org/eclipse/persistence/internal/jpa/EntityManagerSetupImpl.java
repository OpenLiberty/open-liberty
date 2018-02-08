/*******************************************************************************
 * Copyright (c) 1998, 2017 Oracle and/or its affiliates, IBM Corporation. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 *
 *     05/28/2008-1.0M8 Andrei Ilitchev
 *        - 224964: Provide support for Proxy Authentication through JPA.
 *        Added updateConnectionPolicy method to support EXCLUSIVE_CONNECTION property.
 *        Some methods, like setSessionEventListener called from deploy still used predeploy properties,
 *        that meant it was impossible to set listener through createEMF property in SE case with an agent - fixed that.
 *        Also if creating / closing the same emSetupImpl many times (24 in my case) "java.lang.OutOfMemoryError: PermGen space" resulted:
 *        partially fixed partially worked around this - see a big comment in predeploy method.
 *     12/23/2008-1.1M5 Michael O'Brien
 *        - 253701: add persistenceInitializationHelper field used by undeploy() to clear the JavaSECMPInitializer
 *     10/14/2009-2.0      Michael O'Brien
 *        - 266912: add Metamodel instance field as part of the JPA 2.0 implementation
 *     10/21/2009-2.0 Guy Pelletier
 *       - 290567: mappedbyid support incomplete
 *     cdelahun - Bug 214534: changes to allow JMSPublishingTransportManager configuration through properties
 *     05/14/2010-2.1 Guy Pelletier
 *       - 253083: Add support for dynamic persistence using ORM.xml/eclipselink-orm.xml
 *     04/01/2011-2.3 Guy Pelletier
 *       - 337323: Multi-tenant with shared schema support (part 2)
 *     06/30/2011-2.3.1 Guy Pelletier
 *       - 341940: Add disable/enable allowing native queries
 *     09/20/2011-2.3.1 Guy Pelletier
 *       - 357476: Change caching default to ISOLATED for multitenant's using a shared EMF.
 *     08/01/2012-2.5 Chris Delahunt
 *       - 371950: Metadata caching
 *     12/24/2012-2.5 Guy Pelletier
 *       - 389090: JPA 2.1 DDL Generation Support
 *     01/08/2013-2.5 Guy Pelletier
 *       - 389090: JPA 2.1 DDL Generation Support
 *     01/11/2013-2.5 Guy Pelletier
 *       - 389090: JPA 2.1 DDL Generation Support
 *     01/16/2013-2.5 Guy Pelletier
 *       - 389090: JPA 2.1 DDL Generation Support
 *     01/24/2013-2.5 Guy Pelletier
 *       - 389090: JPA 2.1 DDL Generation Support
 *     02/04/2013-2.5 Guy Pelletier
 *       - 389090: JPA 2.1 DDL Generation Support
 *     02/19/2013-2.5 Guy Pelletier
 *       - 389090: JPA 2.1 DDL Generation Support
 *     08/11/2014-2.5 Rick Curtis
 *       - 440594: Tolerate invalid NamedQuery at EntityManager creation.
 *     11/20/2014-2.5 Rick Curtis
 *       - 452187: Support multiple ClassLoaders to load properties.
 *     01/05/2015 Rick Curtis
 *       - 455683: Automatically detect target server
  *     01/13/2015 - Rick Curtis
 *       - 438871 : Add support for writing statement terminator character(s) when generating ddl to script.
 *     02/19/2015 - Rick Curtis
 *       - 458877 : Add national character support
 *     03/04/2015 - Will Dazey
 *       - 460862 : Added support for JTA schema generation without JTA-DS
 *     03/23/2015 - Rick Curtis
 *       - 462888 : SessionCustomizer instance based configuration
 *     08/24/2015 - Dalia Abo Sheasha
 *       - 475285 : Create a generic application-id property to generate unique session names
 *     09/03/2015 - Will Dazey
 *       - 456067 : Added support for defining query timeout units
 *     09/28/2015 - Will Dazey
 *       - 478331 : Added support for defining local or server as the default locale for obtaining timestamps
 *     11/05/2015 - Dalia Abo Sheasha
 *       - 480787 : Wrap several privileged method calls with a doPrivileged block
 *     12/03/2015-2.6 Dalia Abo Sheasha
 *       - 483582: Add the javax.persistence.sharedCache.mode property
 *     09/29/2016-2.7 Tomas Kraus
 *       - 426852: @GeneratedValue(strategy=GenerationType.IDENTITY) support in Oracle 12c
 *     09/14/2017-2.6 Will Dazey
 *       - 522312: Add the eclipselink.sequencing.start-sequence-at-nextval property
 *     01/16/2018-2.7 Joe Grassel
 *       - 529907: EntityManagerSetupImpl.addBeanValidationListeners() should fall back on old method for finding helperClass
 *****************************************************************************/
package org.eclipse.persistence.internal.jpa;

import static org.eclipse.persistence.config.PersistenceUnitProperties.DDL_GENERATION;
import static org.eclipse.persistence.config.PersistenceUnitProperties.NONE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_CREATE_ACTION;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_CREATE_DATABASE_SCHEMAS;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_CREATE_SCRIPT_SOURCE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_CREATE_SOURCE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_DATABASE_ACTION;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_DROP_ACTION;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_DROP_AND_CREATE_ACTION;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_DROP_SCRIPT_SOURCE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_DROP_SOURCE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_METADATA_SOURCE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_METADATA_THEN_SCRIPT_SOURCE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_NONE_ACTION;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_SCRIPTS_ACTION;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_SCRIPTS_CREATE_TARGET;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_SCRIPTS_DROP_TARGET;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_SCRIPT_SOURCE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_SCRIPT_THEN_METADATA_SOURCE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.SCHEMA_GENERATION_SQL_LOAD_SCRIPT_SOURCE;
import static org.eclipse.persistence.internal.jpa.EntityManagerFactoryProvider.generateDefaultTables;
import static org.eclipse.persistence.internal.jpa.EntityManagerFactoryProvider.getConfigProperty;
import static org.eclipse.persistence.internal.jpa.EntityManagerFactoryProvider.getConfigPropertyAsString;
import static org.eclipse.persistence.internal.jpa.EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug;
import static org.eclipse.persistence.internal.jpa.EntityManagerFactoryProvider.getConfigPropertyLogDebug;
import static org.eclipse.persistence.internal.jpa.EntityManagerFactoryProvider.hasConfigProperty;
import static org.eclipse.persistence.internal.jpa.EntityManagerFactoryProvider.login;
import static org.eclipse.persistence.internal.jpa.EntityManagerFactoryProvider.mergeMaps;
import static org.eclipse.persistence.internal.jpa.EntityManagerFactoryProvider.translateOldProperties;
import static org.eclipse.persistence.internal.jpa.EntityManagerFactoryProvider.warnOldProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.eclipse.persistence.annotations.IdValidation;
import org.eclipse.persistence.config.BatchWriting;
import org.eclipse.persistence.config.CacheCoordinationProtocol;
import org.eclipse.persistence.config.DescriptorCustomizer;
import org.eclipse.persistence.config.ExclusiveConnectionMode;
import org.eclipse.persistence.config.LoggerType;
import org.eclipse.persistence.config.ParserType;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.config.ProfilerType;
import org.eclipse.persistence.config.PropertiesUtils;
import org.eclipse.persistence.config.RemoteProtocol;
import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.MultitenantPolicy;
import org.eclipse.persistence.descriptors.SchemaPerMultitenantPolicy;
import org.eclipse.persistence.descriptors.TimestampLockingPolicy;
import org.eclipse.persistence.descriptors.partitioning.PartitioningPolicy;
import org.eclipse.persistence.dynamic.DynamicClassLoader;
import org.eclipse.persistence.eis.EISConnectionSpec;
import org.eclipse.persistence.eis.EISLogin;
import org.eclipse.persistence.eis.EISPlatform;
import org.eclipse.persistence.exceptions.ConversionException;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.exceptions.DescriptorException;
import org.eclipse.persistence.exceptions.EclipseLinkException;
import org.eclipse.persistence.exceptions.EntityManagerSetupException;
import org.eclipse.persistence.exceptions.ExceptionHandler;
import org.eclipse.persistence.exceptions.IntegrityException;
import org.eclipse.persistence.exceptions.PersistenceUnitLoadingException;
import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.internal.databaseaccess.BatchWritingMechanism;
import org.eclipse.persistence.internal.databaseaccess.DatabaseAccessor;
import org.eclipse.persistence.internal.databaseaccess.DatasourcePlatform;
import org.eclipse.persistence.internal.databaseaccess.Platform;
import org.eclipse.persistence.internal.descriptors.OptimisticLockingPolicy;
import org.eclipse.persistence.internal.descriptors.OptimisticLockingPolicy.LockOnChange;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.helper.ConcurrencyManager;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.helper.JPAClassLoaderHolder;
import org.eclipse.persistence.internal.helper.JPAConversionManager;
import org.eclipse.persistence.internal.jpa.deployment.BeanValidationInitializationHelper;
import org.eclipse.persistence.internal.jpa.deployment.PersistenceUnitProcessor;
import org.eclipse.persistence.internal.jpa.deployment.SEPersistenceUnitInfo;
import org.eclipse.persistence.internal.jpa.jdbc.DataSourceImpl;
import org.eclipse.persistence.internal.jpa.metadata.MetadataHelper;
import org.eclipse.persistence.internal.jpa.metadata.MetadataLogger;
import org.eclipse.persistence.internal.jpa.metadata.MetadataProcessor;
import org.eclipse.persistence.internal.jpa.metadata.MetadataProject;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAsmFactory;
import org.eclipse.persistence.internal.jpa.metadata.xml.XMLEntityMappingsReader;
import org.eclipse.persistence.internal.jpa.metamodel.ManagedTypeImpl;
import org.eclipse.persistence.internal.jpa.metamodel.MetamodelImpl;
import org.eclipse.persistence.internal.jpa.metamodel.proxy.AttributeProxyImpl;
import org.eclipse.persistence.internal.jpa.metamodel.proxy.CollectionAttributeProxyImpl;
import org.eclipse.persistence.internal.jpa.metamodel.proxy.ListAttributeProxyImpl;
import org.eclipse.persistence.internal.jpa.metamodel.proxy.MapAttributeProxyImpl;
import org.eclipse.persistence.internal.jpa.metamodel.proxy.SetAttributeProxyImpl;
import org.eclipse.persistence.internal.jpa.metamodel.proxy.SingularAttributeProxyImpl;
import org.eclipse.persistence.internal.jpa.weaving.ClassDetails;
import org.eclipse.persistence.internal.jpa.weaving.PersistenceWeaver;
import org.eclipse.persistence.internal.jpa.weaving.TransformerFactory;
import org.eclipse.persistence.internal.localization.ExceptionLocalization;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.security.PrivilegedClassForName;
import org.eclipse.persistence.internal.security.PrivilegedGetDeclaredField;
import org.eclipse.persistence.internal.security.PrivilegedGetDeclaredFields;
import org.eclipse.persistence.internal.security.PrivilegedGetDeclaredMethod;
import org.eclipse.persistence.internal.security.PrivilegedGetValueFromField;
import org.eclipse.persistence.internal.security.PrivilegedMethodInvoker;
import org.eclipse.persistence.internal.security.PrivilegedNewInstanceFromClass;
import org.eclipse.persistence.internal.security.SecurableObjectHolder;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.internal.sessions.DatabaseSessionImpl;
import org.eclipse.persistence.internal.sessions.PropertiesHandler;
import org.eclipse.persistence.internal.sessions.remote.RemoteConnection;
import org.eclipse.persistence.jpa.metadata.FileBasedProjectCache;
import org.eclipse.persistence.jpa.metadata.MetadataSource;
import org.eclipse.persistence.jpa.metadata.ProjectCache;
import org.eclipse.persistence.jpa.metadata.XMLMetadataSource;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.DefaultSessionLog;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.platform.database.converters.StructConverter;
import org.eclipse.persistence.platform.database.events.DatabaseEventListener;
import org.eclipse.persistence.platform.database.partitioning.DataPartitioningCallback;
import org.eclipse.persistence.platform.server.CustomServerPlatform;
import org.eclipse.persistence.platform.server.ServerPlatform;
import org.eclipse.persistence.platform.server.ServerPlatformBase;
import org.eclipse.persistence.platform.server.ServerPlatformUtils;
import org.eclipse.persistence.queries.QueryResultsCachePolicy;
import org.eclipse.persistence.sequencing.Sequence;
import org.eclipse.persistence.sessions.Connector;
import org.eclipse.persistence.sessions.DatabaseLogin;
import org.eclipse.persistence.sessions.DatasourceLogin;
import org.eclipse.persistence.sessions.DefaultConnector;
import org.eclipse.persistence.sessions.ExternalTransactionController;
import org.eclipse.persistence.sessions.JNDIConnector;
import org.eclipse.persistence.sessions.Project;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.sessions.SessionEventListener;
import org.eclipse.persistence.sessions.SessionProfiler;
import org.eclipse.persistence.sessions.broker.SessionBroker;
import org.eclipse.persistence.sessions.coordination.MetadataRefreshListener;
import org.eclipse.persistence.sessions.coordination.RemoteCommandManager;
import org.eclipse.persistence.sessions.coordination.TransportManager;
import org.eclipse.persistence.sessions.coordination.jms.JMSPublishingTransportManager;
import org.eclipse.persistence.sessions.coordination.jms.JMSTopicTransportManager;
import org.eclipse.persistence.sessions.coordination.rmi.RMITransportManager;
import org.eclipse.persistence.sessions.factories.SessionManager;
import org.eclipse.persistence.sessions.factories.XMLSessionConfigLoader;
import org.eclipse.persistence.sessions.remote.RemoteSession;
import org.eclipse.persistence.sessions.remote.rmi.RMIConnection;
import org.eclipse.persistence.sessions.remote.rmi.RMIServerSessionManager;
import org.eclipse.persistence.sessions.remote.rmi.RMIServerSessionManagerDispatcher;
import org.eclipse.persistence.sessions.serializers.JavaSerializer;
import org.eclipse.persistence.sessions.serializers.Serializer;
import org.eclipse.persistence.sessions.server.ConnectionPolicy;
import org.eclipse.persistence.sessions.server.ConnectionPool;
import org.eclipse.persistence.sessions.server.ExternalConnectionPool;
import org.eclipse.persistence.sessions.server.ReadConnectionPool;
import org.eclipse.persistence.sessions.server.ServerSession;
import org.eclipse.persistence.tools.profiler.PerformanceMonitor;
import org.eclipse.persistence.tools.profiler.PerformanceProfiler;
import org.eclipse.persistence.tools.profiler.QueryMonitor;
import org.eclipse.persistence.tools.schemaframework.SchemaManager;
import org.eclipse.persistence.tools.tuning.SafeModeTuner;
import org.eclipse.persistence.tools.tuning.SessionTuner;
import org.eclipse.persistence.tools.tuning.StandardTuner;

/**
 * INTERNAL:
 * This class handles deployment of a persistence unit.
 * In predeploy the meta-data is processed and weaver transformer is returned to allow weaving of the persistent classes.
 * In deploy the project and session are initialize and registered.
 */
public class EntityManagerSetupImpl implements MetadataRefreshListener {
    /*
     * Design Pattern in use: Builder pattern
     * EntityManagerSetupImpl, MetadataProcessor and MetadataProject
     * play the role of director, builder and product respectively.
     * See processORMetadata which is the factory method.
     *
     */

    // this name should uniquely identify the persistence unit
    protected String persistenceUnitUniqueName;
    // session name should uniquely identify the session
    protected String sessionName;

    protected MetadataProcessor processor = null;
    /** Holds a reference to the weaver class transformer so it can be cleared after login. */
    protected PersistenceWeaver weaver = null;
    protected PersistenceUnitInfo persistenceUnitInfo = null;
    // count a number of open factories that use this object.
    protected int factoryCount = 0;
    protected AbstractSession session = null;
    // true if predeploy called by createContainerEntityManagerFactory; false - createEntityManagerFactory
    protected boolean isInContainerMode = false;
    protected boolean isSessionLoadedFromSessionsXML=false;
    //project caching:
    protected ProjectCache projectCacheAccessor = null;
    protected boolean shouldBuildProject = true;
    // indicates whether weaving was used on the first run through predeploy (in STATE_INITIAL)
    protected Boolean enableWeaving = null;
    // indicates that classes have already been woven
    protected boolean isWeavingStatic = false;
    // used by static weaving
    protected StaticWeaveInfo staticWeaveInfo;
    protected SecurableObjectHolder securableObjectHolder = new SecurableObjectHolder();
    // used by deploy method
    protected ConcurrencyManager deployLock = new ConcurrencyManager();

    protected boolean requiresConnection;

    // 266912: Criteria API and Metamodel API (See Ch 5 of the JPA 2.0 Specification)
    /** Reference to the Metamodel for this deployment and session.
     * Please use the accessor and not the instance variable directly*/
    private Metamodel metaModel;

    protected List<StructConverter> structConverters = null;
    // factoryCount==0; session==null
    public static final String STATE_INITIAL        = "Initial";

    // session != null
    public static final String STATE_PREDEPLOYED    = "Predeployed";

    // factoryCount>0; session != null; session stored in SessionManager
    // for compositeMember factoryCount is always 0; session is never stored in SessionBroker
    // the session has not yet connected for the first time or failed to connect for the first time
    public static final String STATE_HALF_DEPLOYED  = "HalfDeployed";

    // factoryCount>0; session != null; session stored in SessionManager
    // for compositeMember factoryCount is always 0; session is never stored in SessionBroker
    // the session has connected for the first time
    public static final String STATE_DEPLOYED       = "Deployed";

    // factoryCount==0; session==null
    public static final String STATE_PREDEPLOY_FAILED="PredeployFailed";

    // factoryCount>0; session != null
    // for compositeMember factoryCount is always 0
    public static final String STATE_DEPLOY_FAILED  = "DeployFailed";

    // factoryCount==0; session==null
    public static final String STATE_UNDEPLOYED     = "Undeployed";

    // factoryCount==0; session==null
    // only composite member persistence unit can be in this state
    public static final String STATE_HALF_PREDEPLOYED_COMPOSITE_MEMBER = "HalfPredeployedCompositeMember";
    /**
     *     Initial -----> HalfPredeployedCompositeMember -----> PredeployFailed
     *                    |        ^                   |
     *                    V------->|                   V
     *                                                Predeployed
     */

    protected String state = STATE_INITIAL;

    /**
     *     Initial -----------&gt; PredeployFailed ---
     *           |                                |
     *           V                                |
     *         Predeployed ---&gt; DeployFailed --   |
     *           |                            |   |
     *           V                            V   V
     *         HalfDeployed --&gt; Deployed -&gt; Undeployed
     *           |                            ^
     *           V                            |
     *         DeployFailed -------------------
     *
     */


    public static final String ERROR_LOADING_XML_FILE = "error_loading_xml_file";
    public static final String EXCEPTION_LOADING_ENTITY_CLASS = "exception_loading_entity_class";

    /*
     * Properties used to generate sessionName if none is provided.
     */
    public static String[] connectionPropertyNames = {
        PersistenceUnitProperties.TRANSACTION_TYPE,
        PersistenceUnitProperties.JTA_DATASOURCE,
        PersistenceUnitProperties.NON_JTA_DATASOURCE,
        PersistenceUnitProperties.JDBC_URL,
        PersistenceUnitProperties.JDBC_USER,
        PersistenceUnitProperties.NOSQL_CONNECTION_SPEC,
        PersistenceUnitProperties.NOSQL_CONNECTION_FACTORY,
        PersistenceUnitProperties.NOSQL_USER,
        PersistenceUnitProperties.JDBC_CONNECTOR
    };

    /*
     * Composite, not null only if it's a composite member.
     */
    protected EntityManagerSetupImpl compositeEmSetupImpl;

    /*
     * Composite members, not null only if it's a composite.
     */
    protected Set<EntityManagerSetupImpl> compositeMemberEmSetupImpls;

    /*
     * In HalfPredeployedCompositeMember predeploy method called several times,
     * each call uses mode, then updating it before returning.
     * So mode value could be viewed as a substate of HalfPredeployedCompositeMember state.
     * The mode is required for staging of processing OR metadata for composite members:
     * each processing stage should be completed for ALL composite members before
     * any one of then could proceed to the next processing stage.
     */
    PersistenceUnitProcessor.Mode mode;

    boolean throwExceptionOnFail;
    boolean weaveChangeTracking;
    boolean weaveLazy;
    boolean weaveEager;
    boolean weaveFetchGroups;
    boolean weaveInternal;
    boolean weaveRest;
    boolean weaveMappedSuperClass;

    /**
     * Used to indicate that an EntityManagerFactoryImpl based on this
     * EntityManagerSetupImpl has been refreshed.  This means this EntityManagerSetupImpl
     * will no longer be associated with new EntityManagerFactories
     */
    protected boolean isMetadataExpired = false;
    /*
     * Used to distinguish the various DDL options
     */
    protected enum TableCreationType {NONE, CREATE, DROP, DROP_AND_CREATE, EXTEND};

    /*
     * PersistenceException responsible for the invalid state.
     */
    protected PersistenceException persistenceException;

    public EntityManagerSetupImpl(String persistenceUnitUniqueName, String sessionName) {
        this.persistenceUnitUniqueName = persistenceUnitUniqueName;
        this.sessionName = sessionName;
        this.requiresConnection = true;
    }

    public EntityManagerSetupImpl() {
        this("", "");
    }

    protected static String addFileSeperator(String appLocation) {
        int strLength = appLocation.length();
        if (appLocation.substring(strLength -1, strLength).equals(File.separator)) {
            return appLocation;
        } else {
            return appLocation + File.separator;
        }
    }

    /*
     * Return session name if specified.
     * Otherwise build one from the connection properties names and values.
     * Note that specifying value "" in properties causes
     * the property value specified in PersistenceUnitInfo to be ignored.
     * Never returns null.
     */
    public static String getOrBuildSessionName(Map properties, PersistenceUnitInfo puInfo, String persistenceUnitUniqueName) {
        // Weblogic server was found to prefix the file path on Windows platform with a slash, so mandating that for compatibility
        String persistenceUnitName = assertCompatiblePersistenceUnitName(persistenceUnitUniqueName);

        // if SESSION_NAME is specified in either properties or puInfo properties - use it as session name (unless it's an empty String).
        String sessionName = (String)properties.get(PersistenceUnitProperties.SESSION_NAME);
        if (sessionName == null) {
            sessionName = (String)puInfo.getProperties().get(PersistenceUnitProperties.SESSION_NAME);
        }
        // Specifying empty String in properties allows to remove SESSION_NAME specified in puInfo properties.
        if(sessionName != null && sessionName.length() > 0) {
            return sessionName;
        }

        // ELBug 355603 - Prepend the application id if present in properties.
        // This property will be set by the WebLogic Server if the persistence unit
        // is deployed as part of shared library to construct a unique session name
        String applicationId = (String)properties.get("weblogic.application-id");

        // ELBug 475285 - Added a more generic version of the weblogic.application-id property.
        if (applicationId == null) {
            applicationId = (String) properties.get("eclipselink.application-id");
        }

        if (isComposite(puInfo)) {
            // Composite doesn't use connection properties.
            if (applicationId != null) {
                return applicationId + persistenceUnitName;
            }

            return persistenceUnitName;
        } else {
            // In case no SESSION_NAME specified (or empty String) - build one
            // by concatenating persistenceUnitUniqueName and suffix build of connection properties' names and values.
            if (applicationId != null) {
                return applicationId + persistenceUnitName + buildSessionNameSuffixFromConnectionProperties(properties);
            }

            return persistenceUnitName + buildSessionNameSuffixFromConnectionProperties(properties);
        }
    }

    private static String assertCompatiblePersistenceUnitName(String persistenceUnitUniqueName) {
        if(persistenceUnitUniqueName != null && !persistenceUnitUniqueName.startsWith("/")) {
            return '/' + persistenceUnitUniqueName;
        }
        return persistenceUnitUniqueName;
    }

    protected static String buildSessionNameSuffixFromConnectionProperties(Map properties) {
        StringBuilder suffix = new StringBuilder(32);
        for (int i=0; i < connectionPropertyNames.length; i++) {
            String name = connectionPropertyNames[i];
            Object value = properties.get(name);
            if (value != null) {
                String strValue = null;
                if (value instanceof String) {
                    strValue = (String)value;
                } else {
                    if (value instanceof javax.sql.DataSource) {
                        // value of JTA_DATASOURCE / NON_JTA_DATASOURCE may be a DataSource (we would prefer DataSource name)
                        strValue = Integer.toString(System.identityHashCode(value));
                    } else if (value instanceof PersistenceUnitTransactionType) {
                        strValue = value.toString();
                    } else {
                        strValue = Integer.toString(System.identityHashCode(value));
                    }
                }
                // don't set an empty String
                if (strValue.length() > 0) {
                    suffix.append("_").append(Helper.getShortClassName(name)).append("=").append(strValue);
                }
            }
        }
        return suffix.toString();
    }

    /*
     * Should only be called when emSetupImpl created during SE initialization is set into a new EMF.
     * emSetupImpl must be in PREDEPLOYED state.
     */
    public void changeSessionName(String newSessionName) {
        if(!session.getName().equals(newSessionName)) {
            session.log(SessionLog.FINEST, SessionLog.PROPERTIES, "session_name_change", new Object[]{getPersistenceUnitInfo().getPersistenceUnitName(), session.getName(), newSessionName});
            sessionName = newSessionName;
            session.setName(newSessionName);
        }
    }

    /**
     * This method can be used to ensure the session represented by emSetupImpl
     * is removed from the SessionManager.
     */
    protected void removeSessionFromGlobalSessionManager() {
        if (this.session != null){
            try {
                if (this.session.isDatabaseSession() && this.session.isConnected()) {
                    getDatabaseSession().logout();
                }
            } finally {
                SessionManager manager = SessionManager.getManager();
                manager.getSessions().remove(this.session.getName(), this.session);
                manager.destroy();
            }
        }
    }

    /**
     * Deploy a persistence session and return an EntityManagerFactory.
     *
     * Deployment takes a session that was partially created in the predeploy call and makes it whole.
     *
     * This means doing any configuration that requires the real class definitions for the entities.  In
     * the predeploy phase we were in a stage where we were not let allowed to load the real classes.
     *
     * Deploy could be called several times - but only the first call does the actual deploying -
     * additional calls allow to update session properties (in case the session is not connected).
     *
     * Note that there is no need to synchronize deploy method - it doesn't alter factoryCount
     * and while deploy is executed no other method can alter the current state
     * (predeploy call would just increment factoryCount; undeploy call would not drop factoryCount to 0).
     * However precautions should be taken to handle concurrent calls to deploy, because those may
     * alter the current state or connect the session.
     *
     * @param realClassLoader The class loader that was used to load the entity classes. This loader
     *               will be maintained for the lifespan of the loaded classes.
     * @param additionalProperties added to persistence unit properties for updateServerSession overriding existing properties.
     *              In JSE case it allows to alter properties in main (as opposed to preMain where preDeploy is called).
     * @return An EntityManagerFactory to be used by the Container to obtain EntityManagers
     */
    public AbstractSession deploy(ClassLoader realClassLoader, Map additionalProperties) {
        if (this.state != STATE_PREDEPLOYED && this.state != STATE_DEPLOYED && this.state != STATE_HALF_DEPLOYED) {
            if (mustBeCompositeMember()) {
                throw new PersistenceException(EntityManagerSetupException.compositeMemberCannotBeUsedStandalone(this.persistenceUnitInfo.getPersistenceUnitName()));
            }
            throw new PersistenceException(EntityManagerSetupException.cannotDeployWithoutPredeploy(this.persistenceUnitInfo.getPersistenceUnitName(), this.state, this.persistenceException));
        }
        // state is PREDEPLOYED or DEPLOYED
        this.session.log(SessionLog.FINEST, SessionLog.JPA, "deploy_begin", new Object[]{getPersistenceUnitInfo().getPersistenceUnitName(), this.session.getName(), this.state, this.factoryCount});

        ClassLoader classLoaderToUse = realClassLoader;

        if (additionalProperties.containsKey(PersistenceUnitProperties.CLASSLOADER)) {
            classLoaderToUse = (ClassLoader) additionalProperties.get(PersistenceUnitProperties.CLASSLOADER);
        } else if ((this.processor != null) && (this.processor.getProject() != null) && (this.processor.getProject().hasVirtualClasses()) && (this.state == STATE_PREDEPLOYED) && (!(classLoaderToUse instanceof DynamicClassLoader))) {
            classLoaderToUse = new DynamicClassLoader(classLoaderToUse);
        }

        // indicates whether session has failed to connect, determines whether HALF_DEPLOYED state should be kept in case of exception.
        boolean isLockAcquired = false;
        try {
            Map deployProperties = mergeMaps(additionalProperties, this.persistenceUnitInfo.getProperties());
            updateTunerPreDeploy(deployProperties, classLoaderToUse);
            translateOldProperties(deployProperties, this.session);
            if (isComposite()) {
                updateCompositeMembersProperties(deployProperties);
            }
            if (this.state == STATE_PREDEPLOYED) {
                this.deployLock.acquire();
                isLockAcquired = true;
                if (this.state == STATE_PREDEPLOYED) {
                    if (this.shouldBuildProject && !this.isSessionLoadedFromSessionsXML) {
                        if (isComposite()) {
                            deployCompositeMembers(deployProperties, classLoaderToUse);
                        } else {
                            if (this.processor.getMetadataSource() != null) {
                                Map metadataProperties = this.processor.getMetadataSource().getPropertyOverrides(deployProperties, classLoaderToUse, this.session.getSessionLog());
                                if (metadataProperties != null && !metadataProperties.isEmpty()) {
                                    translateOldProperties(metadataProperties, this.session);
                                    deployProperties = mergeMaps(metadataProperties, deployProperties);
                                }
                            }
                            // listeners and queries require the real classes and are therefore built during deploy using the realClassLoader
                            this.processor.setClassLoader(classLoaderToUse);
                            this.processor.createDynamicClasses();
                            if (classLoaderToUse instanceof DynamicClassLoader){
                                this.processor.createRestInterfaces();
                            }

                            this.processor.addEntityListeners();

                            if (this.projectCacheAccessor != null) {
                                //cache the project:
                                this.projectCacheAccessor.storeProject(this.session.getProject(), deployProperties, this.session.getSessionLog());
                            }

                            // The project is initially created using class names rather than classes.  This call will make the conversion.
                            // If the session was loaded from sessions.xml this will also convert the descriptor classes to the correct class loader.
                            this.session.getProject().convertClassNamesToClasses(classLoaderToUse);

                            if (!isCompositeMember()) {
                                addBeanValidationListeners(deployProperties, classLoaderToUse);
                            }

                            // Process the customizers last.
                            this.processor.processCustomizers();
                        }

                        this.processor = null;
                    } else {
                        // The project is initially created using class names rather than classes.  This call will make the conversion.
                        // If the session was loaded from sessions.xml this will also convert the descriptor classes to the correct class loader.
                        this.session.getProject().convertClassNamesToClasses(classLoaderToUse);
                        if (!this.shouldBuildProject) {
                            //process anything that might not have been serialized/cached in the project correctly:
                            if (!isCompositeMember()) {
                                addBeanValidationListeners(deployProperties, classLoaderToUse);
                            }

                            //process Descriptor customizers:
                            processDescriptorsFromCachedProject(classLoaderToUse);
                        }
                    }
                    finishProcessingDescriptorEvents(classLoaderToUse);
                    this.structConverters = getStructConverters(classLoaderToUse);

                    updateRemote(deployProperties, classLoaderToUse);
                    initSession();

                    if (this.session.getIntegrityChecker().hasErrors()){
                        this.session.handleException(new IntegrityException(session.getIntegrityChecker()));
                    }

                    this.session.getDatasourcePlatform().getConversionManager().setLoader(classLoaderToUse);
                    this.state = STATE_HALF_DEPLOYED;
                    // keep deployLock
                } else {
                    // state is HALF_DEPLOYED or DEPLOY_FAILED
                    this.deployLock.release();
                    isLockAcquired = false;
                    if (this.state == STATE_DEPLOY_FAILED) {
                        // while this thread waited in STATE_PREDEPLOYED another thread attempted to deploy and failed.
                        // Rethrow the cache PersistenceException, which caused STATE_DEPLOYED_FAILED.
                        throw persistenceException;
                    }
                }
            }
            // state is HALF_DEPLOYED or DEPLOYED
            if (!isCompositeMember()) {
                if (this.session.isDatabaseSession() && !((DatabaseSessionImpl)session).isLoggedIn()) {
                    // If it's HALF_DEPLOYED then deployLock has been already acquired.
                    if (!isLockAcquired) {
                        this.deployLock.acquire();
                        isLockAcquired = true;
                    }
                    if (!((DatabaseSessionImpl)this.session).isLoggedIn()) {
                        if (this.state == STATE_DEPLOY_FAILED) {
                            // while this thread waited in STATE_HALF_DEPLOYED another thread attempted to connect the session and failed.
                            // Rethrow the cache PersistenceException, which caused STATE_DEPLOYED_FAILED.
                            throw persistenceException;
                        }
                        this.session.setProperties(deployProperties);
                        updateSession(deployProperties, classLoaderToUse);
                        if (isValidationOnly(deployProperties, false)) {
                            /**
                             * for 324213 we could add a session.loginAndDetectDatasource() call
                             * before calling initializeDescriptors when validation-only is True
                             * to avoid a native sequence exception on a generic DatabasePlatform
                             * by auto-detecting the correct DB platform.
                             * However, this would introduce a DB login when validation is on
                             * - in opposition to the functionality of the property (to only validate)
                             */
                            if (this.state == STATE_HALF_DEPLOYED) {
                                getDatabaseSession().initializeDescriptors();
                                this.state = STATE_DEPLOYED;
                            }
                        } else {
                            try {
                                updateTunerDeploy(deployProperties, classLoaderToUse);
                                updateFreeMemory(deployProperties);
                                if (this.isSessionLoadedFromSessionsXML) {
                                    getDatabaseSession().login();
                                } else {
                                    login(getDatabaseSession(), deployProperties, requiresConnection);
                                }
                                final Platform platform = getDatabaseSession().getDatasourcePlatform();
                                PropertiesUtils.set(platform, PersistenceUnitProperties.TARGET_DATABASE_PROPERTIES, (String) deployProperties.get(PersistenceUnitProperties.TARGET_DATABASE_PROPERTIES));

                                // Make JTA integration throw JPA exceptions.
                                if (this.session.hasExternalTransactionController()) {
                                    if (this.session.getExternalTransactionController().getExceptionHandler() == null) {
                                        this.session.getExternalTransactionController().setExceptionHandler(new ExceptionHandler() {

                                            @Override
                                            public Object handleException(RuntimeException exception) {
                                                if (exception instanceof org.eclipse.persistence.exceptions.OptimisticLockException) {
                                                    throw new OptimisticLockException(exception);
                                                } else if (exception instanceof EclipseLinkException) {
                                                    throw new PersistenceException(exception);
                                                } else {
                                                    throw exception;
                                                }
                                            }

                                        });
                                    }
                                }
                                this.state = STATE_DEPLOYED;
                            } catch (Throwable loginException) {
                                if (this.state == STATE_HALF_DEPLOYED) {
                                    if (this.session.isConnected()) {
                                        // session is connected, but postConnect has failed.
                                        // Likely this is caused by failure in initializeDescriptors:
                                        // either descriptor exception or by invalid named jpql query.
                                        // Cannot recover from that - the user has to fix the persistence unit and redeploy it.
                                        try {
                                            getDatabaseSession().logout();
                                        } catch (Throwable logoutException) {
                                            // Ignore
                                        }
                                        this.state = STATE_DEPLOY_FAILED;
                                    }
                                }
                                throw loginException;
                            }
                            if (!this.isSessionLoadedFromSessionsXML) {
                                addStructConverters();
                            }

                            // Generate the DDL using the correct connection.
                            writeDDL(deployProperties, getDatabaseSession(deployProperties), classLoaderToUse);
                        }
                    }
                    // Initialize platform specific identity sequences.
                    session.getDatasourcePlatform().initIdentitySequences(getDatabaseSession(), MetadataProject.DEFAULT_IDENTITY_GENERATOR);
                    updateTunerPostDeploy(deployProperties, classLoaderToUse);
                    this.deployLock.release();
                    isLockAcquired = false;
                }
                // 266912: Initialize the Metamodel, a login should have already occurred.
                try {
                    this.getMetamodel(classLoaderToUse);
                } catch (Exception e) {
                    this.session.log(SessionLog.FINEST, SessionLog.METAMODEL, "metamodel_init_failed", new Object[]{e.getMessage()});
                }
            }
            // Clear the weaver's reference to meta-data information, as it is held by the class loader and will never gc.
            if (this.weaver != null) {
                this.weaver.clear();
                this.weaver = null;
            }

            return this.session;
        } catch (Throwable exception) {
            // before releasing deployLock switch to the correct state
            if (this.state == STATE_PREDEPLOYED) {
                this.state = STATE_DEPLOY_FAILED;
            }
            PersistenceException persistenceEx;
            if (this.state == STATE_DEPLOY_FAILED) {
                if (exception == persistenceException) {
                    persistenceEx = new PersistenceException(EntityManagerSetupException.cannotDeployWithoutPredeploy(this.persistenceUnitInfo.getPersistenceUnitName(), this.state, this.persistenceException));
                } else {
                    // before releasing deployLock cache the exception
                    persistenceEx = createDeployFailedPersistenceException(exception);
                }
            } else {
                if (exception instanceof PersistenceException) {
                    persistenceEx = (PersistenceException)exception;
                } else {
                    persistenceEx = new PersistenceException(exception);
                }
            }
            if (isLockAcquired) {
                this.deployLock.release();
            }
            this.session.logThrowable(SessionLog.SEVERE, SessionLog.EJB, exception);
            throw persistenceEx;
        } finally {
            this.session.log(SessionLog.FINEST, SessionLog.JPA, "deploy_end", new Object[]{getPersistenceUnitInfo().getPersistenceUnitName(), this.session.getName(), this.state, this.factoryCount});
        }
    }

    /**
     * INTERNAL:
     * This method is used to resolve Descriptor Customizers that might have been stored in the project
     * for JPA project caching.
     *
     * @param realClassLoader
     * @throws ClassNotFoundException
     * @throws PrivilegedActionException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void processDescriptorsFromCachedProject(ClassLoader realClassLoader) throws ClassNotFoundException, PrivilegedActionException, IllegalAccessException, InstantiationException {
        for (ClassDescriptor descriptor: session.getProject().getDescriptors().values()) {
            //process customizers:
            if (descriptor.getDescriptorCustomizerClassName() != null) {
                Class listenerClass = this.findClass(descriptor.getDescriptorCustomizerClassName(), realClassLoader);
                DescriptorCustomizer customizer = (DescriptorCustomizer)this.buildObjectForClass(listenerClass, DescriptorCustomizer.class);
                try {
                    customizer.customize(descriptor);
                } catch (Exception e) {
                    session.getSessionLog().logThrowable(SessionLog.FINER, SessionLog.METADATA, e);
                }
            }
        }
    }

    /**
     * INTERNAL:
     * This method is used to resolve Descriptor Events processed earlier into EventHolders that now need to be
     * used to create the DescriptorEventListeners and added to the DescriptorEventManager
     *
     */
    private void finishProcessingDescriptorEvents(ClassLoader realClassLoader) {
        for (ClassDescriptor descriptor: session.getProject().getDescriptors().values()) {
            if (descriptor.hasEventManager()) {
                descriptor.getEventManager().processDescriptorEventHolders(session, realClassLoader);
            }
        }
    }

    protected PersistenceException createDeployFailedPersistenceException(Throwable ex) {
        PersistenceException perEx = new PersistenceException(EntityManagerSetupException.deployFailed(persistenceUnitInfo.getPersistenceUnitName(), ex));
        if (persistenceException == null) {
            persistenceException = perEx;
        }
        return perEx;
    }

    /**
     * Adds descriptors plus sequencing info found on the project to the session.
     */
    protected void addProjectToSession(ServerSession session, Project project) {
        DatasourcePlatform sessionPlatform = (DatasourcePlatform)session.getDatasourceLogin().getDatasourcePlatform();
        DatasourcePlatform projectPlatform = (DatasourcePlatform)project.getDatasourceLogin().getDatasourcePlatform();
        if (!sessionPlatform.hasDefaultSequence() && projectPlatform.hasDefaultSequence()) {
            sessionPlatform.setDefaultSequence(projectPlatform.getDefaultSequence());
        }
        if ((sessionPlatform.getSequences() == null) || sessionPlatform.getSequences().isEmpty()) {
            if ((projectPlatform.getSequences() != null) && !projectPlatform.getSequences().isEmpty()) {
                sessionPlatform.setSequences(projectPlatform.getSequences());
            }
        } else {
            if ((projectPlatform.getSequences() != null) && !projectPlatform.getSequences().isEmpty()) {
                Iterator itProjectSequences = projectPlatform.getSequences().values().iterator();
                while (itProjectSequences.hasNext()) {
                    Sequence sequence = (Sequence)itProjectSequences.next();
                    if (!sessionPlatform.getSequences().containsKey(sequence.getName())) {
                        sessionPlatform.addSequence(sequence);
                    }
                }
            }
        }
        session.addDescriptors(project);
    }

    /**
     * Put the given session into the session manager so it can be looked up later
     */
    protected void addSessionToGlobalSessionManager() {
        SessionManager sm = SessionManager.getManager();
        ConcurrentMap<String,Session> sessions = sm.getSessions();
        AbstractSession oldSession = (AbstractSession) sessions.get(session.getName());
        if(oldSession != null) {
            throw new PersistenceException(EntityManagerSetupException.attemptedRedeployWithoutClose(session.getName()));
        }
        sm.addSession(session);
    }

    /**
     * Add the StructConverters that were specified by annotation on the DatabasePlatform
     * This method must be called after the DatabasePlatform has been detected
     */
    public void addStructConverters(){
        if (this.compositeMemberEmSetupImpls == null) {
            for (StructConverter structConverter : structConverters){
                if (session.getPlatform().getTypeConverters().get(structConverter.getJavaType()) != null){
                    throw ValidationException.twoStructConvertersAddedForSameClass(structConverter.getJavaType().getName());
                }
                session.getPlatform().addStructConverter(structConverter);
            }
        } else {
            // composite
            for(EntityManagerSetupImpl compositeMemberEmSetupImpl : this.compositeMemberEmSetupImpls) {
                if (!compositeMemberEmSetupImpl.structConverters.isEmpty()) {
                    String compositeMemberPuName = compositeMemberEmSetupImpl.getPersistenceUnitInfo().getPersistenceUnitName();
                    // debug output added to make it easier to navigate the log because the method is called outside of composite member deploy
                    compositeMemberEmSetupImpl.session.log(SessionLog.FINEST, SessionLog.PROPERTIES, "composite_member_begin_call", new Object[]{"addStructConverters", compositeMemberPuName, state});
                    compositeMemberEmSetupImpl.addStructConverters();
                    compositeMemberEmSetupImpl.session.log(SessionLog.FINEST, SessionLog.PROPERTIES, "composite_member_end_call", new Object[]{"addStructConverters", compositeMemberPuName, state});
                }
            }
        }
    }

    /**
     * Assign a CMP3Policy to each descriptor, and sets the OptimisticLockingPolicy's LockOnChangeMode if applicable.
     */
    protected void assignCMP3Policy() {
        // all descriptors assigned CMP3Policy
        Project project = session.getProject();
        for (Iterator iterator = project.getDescriptors().values().iterator(); iterator.hasNext();){
            //bug:4406101  changed class cast to base class, which is used in projects generated from 904 xml
            ClassDescriptor descriptor = (ClassDescriptor)iterator.next();

            if(descriptor.getCMPPolicy() == null) {
                descriptor.setCMPPolicy(new CMP3Policy());
            }
            OptimisticLockingPolicy olp = descriptor.getOptimisticLockingPolicy();
            if (olp != null && olp.getLockOnChangeMode() == null){
                olp.setLockOnChangeMode(LockOnChange.OWNING);
            }
        }

        // TODO: Look into setting a CMPPolicy on the MappedSuperclass descriptors.
        // Will require some tweaking however to ensure the primary key fields are
        // set/initialized correctly. Currently rely on the descriptor initialized
        // object builder which is not available to mapped superclass descriptors.
    }

    /**
     * Updates the EclipseLink ServerPlatform class for use with this platform.
     * @return true if the ServerPlatform has changed.
     */
    protected boolean updateServerPlatform(Map m, ClassLoader loader) {
        String serverPlatformClassName =
            PropertiesHandler.getPropertyValueLogDebug(PersistenceUnitProperties.TARGET_SERVER, m, session);
        if (serverPlatformClassName == null) {
            // property is not specified - try to detect.
            serverPlatformClassName = ServerPlatformUtils.detectServerPlatform(getSession());
            if (serverPlatformClassName == null) {
                // Unable to detect what platform we're running on. Use default/NoServer.
                return false;
            }
        }

        // originalServerPlatform is always non-null - Session's constructor sets serverPlatform to NoServerPlatform
        ServerPlatform originalServerPlatform = session.getServerPlatform();
        String originalServerPlatformClassName = originalServerPlatform.getClass().getName();
        if(originalServerPlatformClassName.equals(serverPlatformClassName)) {
            // nothing to do - use the same value as before
            return false;
        }

        // the new serverPlatform
        ServerPlatform serverPlatform = null;
        // New platform - create the new instance and set it.
        Class cls = findClassForProperty(serverPlatformClassName, PersistenceUnitProperties.TARGET_SERVER, loader);
        try {
            Constructor constructor = cls.getConstructor(new Class[]{org.eclipse.persistence.sessions.DatabaseSession.class});
            serverPlatform = (ServerPlatform)constructor.newInstance(new Object[]{session});
        } catch (Exception ex) {
            if(ExternalTransactionController.class.isAssignableFrom(cls)) {
                // the new serverPlatform is CustomServerPlatform, cls is its ExternalTransactionController class
                if(originalServerPlatform.getClass().equals(CustomServerPlatform.class)) {
                    // both originalServerPlatform and the new serverPlatform are Custom,
                    // just set externalTransactionController class (if necessary) into
                    // originalServerPlatform
                    CustomServerPlatform originalCustomServerPlatform = (CustomServerPlatform)originalServerPlatform;
                    if(cls.equals(originalCustomServerPlatform.getExternalTransactionControllerClass())) {
                        // externalTransactionController classes are the same - nothing to do
                    } else {
                        originalCustomServerPlatform.setExternalTransactionControllerClass(cls);
                    }
                } else {
                    // originalServerPlatform is not custom - need a new one.
                    CustomServerPlatform customServerPlatform = new CustomServerPlatform(getDatabaseSession());
                    customServerPlatform.setExternalTransactionControllerClass(cls);
                    serverPlatform = customServerPlatform;
                }
             } else {
                 throw EntityManagerSetupException.failedToInstantiateServerPlatform(serverPlatformClassName, PersistenceUnitProperties.TARGET_SERVER, ex);
             }
         }

        if (serverPlatform != null){
            getDatabaseSession().setServerPlatform(serverPlatform);
            return true;
        }
        return false;
    }

    /**
     * Checks for partitioning properties.
     */
    protected void updatePartitioning(Map m, ClassLoader loader) {
        // Partitioning
        String partitioning = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.PARTITIONING, m, this.session);
        if (partitioning != null) {
            PartitioningPolicy partitioningPolicy = this.session.getProject().getPartitioningPolicy(partitioning);
            if (partitioningPolicy == null) {
                throw DescriptorException.missingPartitioningPolicy(partitioning, null, null);
            }
            this.session.setPartitioningPolicy(partitioningPolicy);
        }

        String callbackClassName = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.PARTITIONING_CALLBACK, m, this.session);
        if (callbackClassName != null) {
            Class cls = findClassForProperty(callbackClassName, PersistenceUnitProperties.PARTITIONING_CALLBACK, loader);
            DataPartitioningCallback callback = null;
            try {
                Constructor constructor = cls.getConstructor();
                callback = (DataPartitioningCallback)constructor.newInstance();
            } catch (Exception exception) {
                throw EntityManagerSetupException.failedToInstantiateProperty(callbackClassName, PersistenceUnitProperties.PARTITIONING_CALLBACK, exception);
            }
            this.session.getLogin().setPartitioningCallback(callback);
        }
    }

    /**
     * Checks for partitioning properties.
     */
    protected void updateRemote(Map m, ClassLoader loader) {
        String protocol = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.REMOTE_PROTOCOL, m, this.session);
        String serverName = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.REMOTE_SERVER_NAME, m, this.session);
        if (serverName == null) {
            // Configure as client.
            if (protocol != null) {
                RemoteConnection connection = null;
                if (protocol.equalsIgnoreCase(RemoteProtocol.RMI)) {
                    String url = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.REMOTE_URL, m, this.session);
                    if (url == null) {
                        throw EntityManagerSetupException.missingProperty(PersistenceUnitProperties.REMOTE_URL);
                    }
                    try {
                        connection = new RMIConnection(((RMIServerSessionManager)Naming.lookup(url)).createRemoteSessionController());
                    } catch (Exception exception) {
                        throw ValidationException.invalidValueForProperty(url, PersistenceUnitProperties.REMOTE_URL, exception);
                    }
                } else {
                    Class cls = findClassForProperty(protocol, PersistenceUnitProperties.REMOTE_PROTOCOL, loader);
                    try {
                        Constructor constructor = cls.getConstructor();
                        connection = (RemoteConnection)constructor.newInstance();
                    } catch (Exception exception) {
                        throw ValidationException.invalidValueForProperty(protocol, PersistenceUnitProperties.REMOTE_PROTOCOL, exception);
                    }
                }
                RemoteSession remoteSession = new RemoteSession();
                remoteSession.setIsMetadataRemote(false);
                remoteSession.setProject(this.session.getProject());
                remoteSession.setProfiler(this.session.getProfiler());
                remoteSession.setSessionLog(this.session.getSessionLog());
                remoteSession.setEventManager(this.session.getEventManager());
                remoteSession.setQueries(this.session.getQueries());
                remoteSession.setProperties(this.session.getProperties());
                remoteSession.setName(this.session.getName());
                remoteSession.setRemoteConnection(connection);
                this.session = remoteSession;
            }
        } else {
            // Configure as server.
            if (protocol.equalsIgnoreCase(RemoteProtocol.RMI)) {
                RMIServerSessionManager manager = null;
                // Make sure RMI registry is started.
                try {
                    java.rmi.registry.LocateRegistry.createRegistry(1099);
                } catch (Exception exception) {
                    System.out.println("Security violation " + exception.toString());
                }
                // Create local instance of the factory
                try {
                    manager = new RMIServerSessionManagerDispatcher(session);
                } catch (RemoteException exception) {
                    throw ValidationException.invalidValueForProperty(serverName, PersistenceUnitProperties.REMOTE_SERVER_NAME, exception);
                }
                // Put the local instance into the Registry
                try {
                    Naming.unbind(serverName);
                } catch (Exception exception) {
                    // Ignore.
                }

                // Put the local instance into the Registry
                try {
                    Naming.rebind(serverName, manager);
                } catch (Exception exception) {
                    throw ValidationException.invalidValueForProperty(serverName, PersistenceUnitProperties.REMOTE_SERVER_NAME, exception);
                }
            }
        }
    }

    /**
     * Checks for database events listener properties.
     */
    protected void updateDatabaseEventListener(Map m, ClassLoader loader) {
        String listenerClassName = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.DATABASE_EVENT_LISTENER, m, this.session);
        if (listenerClassName != null) {
            if (listenerClassName.equalsIgnoreCase("DCN") || listenerClassName.equalsIgnoreCase("QCN")) {
                listenerClassName = "org.eclipse.persistence.platform.database.oracle.dcn.OracleChangeNotificationListener";
            }
            Class cls = findClassForProperty(listenerClassName, PersistenceUnitProperties.DATABASE_EVENT_LISTENER, loader);
            DatabaseEventListener listener = null;
            try {
                Constructor constructor = cls.getConstructor();
                listener = (DatabaseEventListener)constructor.newInstance();
            } catch (Exception exception) {
                throw EntityManagerSetupException.failedToInstantiateProperty(listenerClassName, PersistenceUnitProperties.DATABASE_EVENT_LISTENER, exception);
            }
            getDatabaseSession().setDatabaseEventListener(listener);
        }
    }

    /**
     * Update loggers and settings for the singleton logger and the session logger.
     * @param persistenceProperties the properties map
     * @param serverPlatformChanged the boolean that denotes a serverPlatform change in the session.
     */
    protected void updateLoggers(Map persistenceProperties, boolean serverPlatformChanged, ClassLoader loader) {
        // Logger(SessionLog type) can be specified by the logger property or ServerPlatform.getServerLog().
        // The logger property has a higher priority to ServerPlatform.getServerLog().
        String loggerClassName = PropertiesHandler.getPropertyValueLogDebug(PersistenceUnitProperties.LOGGING_LOGGER, persistenceProperties, session);

        // The sessionLog instance should be different from the singletonLog because they have
        // different state.
        SessionLog singletonLog = null, sessionLog = null;
        if (loggerClassName != null) {
            SessionLog currentLog = session.getSessionLog();
            if(loggerClassName.equals(LoggerType.ServerLogger)){
                ServerPlatform serverPlatform = session.getServerPlatform();
                singletonLog = serverPlatform.getServerLog();
                sessionLog = serverPlatform.getServerLog();
            } else if (!currentLog.getClass().getName().equals(loggerClassName)) {
                // Logger class was specified and it's not what's already there.
                Class sessionLogClass = findClassForProperty(loggerClassName, PersistenceUnitProperties.LOGGING_LOGGER, loader);
                try {
                    singletonLog = (SessionLog)sessionLogClass.newInstance();
                    sessionLog = (SessionLog)sessionLogClass.newInstance();
                } catch (Exception ex) {
                    throw EntityManagerSetupException.failedToInstantiateLogger(loggerClassName, PersistenceUnitProperties.LOGGING_LOGGER, ex);
                }
            }
        } else if (serverPlatformChanged) {
            ServerPlatform serverPlatform = session.getServerPlatform();
            singletonLog = serverPlatform.getServerLog();
            sessionLog = serverPlatform.getServerLog();
        }

        // Don't change default loggers if the new loggers have not been created.
        if (singletonLog != null && sessionLog != null) {
            AbstractSessionLog.setLog(singletonLog);
            session.setSessionLog(sessionLog);
        }

        // Bug5389828.  Update the logging settings for the singleton logger.
        initOrUpdateLogging(persistenceProperties, AbstractSessionLog.getLog());
        initOrUpdateLogging(persistenceProperties, session.getSessionLog());
        // Set logging file.
        String loggingFileString = (String)persistenceProperties.get(PersistenceUnitProperties.LOGGING_FILE);
        if (loggingFileString != null) {
            if (!loggingFileString.trim().equals("")) {
                try {
                    if (sessionLog!=null){
                        if (sessionLog instanceof AbstractSessionLog) {
                            FileOutputStream fos = new FileOutputStream(loggingFileString);
                           ((AbstractSessionLog)sessionLog).setWriter(fos);
                        } else {
                            FileWriter fw = new FileWriter(loggingFileString);
                            sessionLog.setWriter(fw);
                        }
                    }
                } catch (IOException e) {
                    session.handleException(ValidationException.invalidLoggingFile(loggingFileString,e));
                }
            } else {
                session.handleException(ValidationException.invalidLoggingFile());
            }
        }
    }

    /**
     * Check for the PROFILER persistence or system property and set the Session's profiler.
     * This can also set the QueryMonitor.
     */
    protected void updateProfiler(Map persistenceProperties,ClassLoader loader) {
        // This must use config property as the profiler is not in the PropertiesHandler and requires
        // supporting generic profiler classes.
        String newProfilerClassName = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.PROFILER, persistenceProperties, session);

        if (newProfilerClassName == null) {
            ServerPlatformBase plaftorm = ((ServerPlatformBase)session.getServerPlatform());
            if (plaftorm != null) {
                plaftorm.configureProfiler(session);
            }
        } else {
            if (newProfilerClassName.equals(ProfilerType.NoProfiler)) {
                session.setProfiler(null);
                return;
            }
            if (newProfilerClassName.equals(ProfilerType.QueryMonitor)) {
                session.setProfiler(null);
                QueryMonitor.shouldMonitor=true;
                return;
            }
            if (newProfilerClassName.equals(ProfilerType.PerformanceProfiler)) {
                session.setProfiler(new PerformanceProfiler());
                return;
            }
            if (newProfilerClassName.equals(ProfilerType.PerformanceMonitor)) {
                session.setProfiler(new PerformanceMonitor());
                return;
            }

            if (newProfilerClassName.equals(ProfilerType.DMSProfiler)) {
                newProfilerClassName = ProfilerType.DMSProfilerClassName;
            }

            String originalProfilerClassNamer = null;
            if (session.getProfiler() != null) {
                originalProfilerClassNamer = session.getProfiler().getClass().getName();
                if (originalProfilerClassNamer.equals(newProfilerClassName)) {
                    return;
                }
            }

            // New profiler - create the new instance and set it.
            try {
                Class newProfilerClass = findClassForProperty(newProfilerClassName, PersistenceUnitProperties.PROFILER, loader);

                SessionProfiler sessionProfiler = (SessionProfiler)buildObjectForClass(newProfilerClass, SessionProfiler.class);

                if (sessionProfiler != null) {
                    session.setProfiler(sessionProfiler);
                } else {
                    session.handleException(ValidationException.invalidProfilerClass(newProfilerClassName));
                }
            } catch (IllegalAccessException e) {
                session.handleException(ValidationException.cannotInstantiateProfilerClass(newProfilerClassName,e));
            } catch (PrivilegedActionException e) {
                session.handleException(ValidationException.cannotInstantiateProfilerClass(newProfilerClassName,e));
            } catch (InstantiationException e) {
                session.handleException(ValidationException.cannotInstantiateProfilerClass(newProfilerClassName,e));
            }
        }
    }


    protected static Class findClass(String className, ClassLoader loader) throws ClassNotFoundException, PrivilegedActionException {
        if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
            return AccessController.doPrivileged(new PrivilegedClassForName(className, true, loader));
        } else {
            return org.eclipse.persistence.internal.security.PrivilegedAccessHelper.getClassForName(className, true, loader);
        }
    }

    protected static Class findClassForProperty(String className, String propertyName, ClassLoader loader) {
        ClassLoader eclipselinkLoader = EntityManagerSetupImpl.class.getClassLoader();
        boolean multipleLoaders = eclipselinkLoader != loader;
        if (multipleLoaders) {
            return findClassForPropertyInternal(className, propertyName, loader, eclipselinkLoader);
        } else {
            return findClassForPropertyInternal(className, propertyName, loader);
        }
    }

    private static Class findClassForPropertyInternal(String clsName, String propName, ClassLoader... loaders) {
        RuntimeException e = null;
        for (ClassLoader loader : loaders) {
            try {
                return findClass(clsName, loader);
            } catch (PrivilegedActionException exception1) {
                e = EntityManagerSetupException.classNotFoundForProperty(clsName, propName, exception1.getException());
            } catch (ClassNotFoundException exception2) {
                e = EntityManagerSetupException.classNotFoundForProperty(clsName, propName, exception2);
            }
        }

        throw e;
    }

    /**
     * Internal:
     * Returns a list of StructConverter instances from a list of StructConverter names stored within the project.
     *
     * @param realClassLoader
     * @return
     */
    protected List<StructConverter> getStructConverters(ClassLoader realClassLoader) {
        List<StructConverter> structConverters = new ArrayList<StructConverter>();
        if (session.getProject().getStructConverters() != null) {
            for (String converter: session.getProject().getStructConverters()) {
                Class clazz = null;
                try {
                    clazz = this.findClass(converter, realClassLoader);
                } catch (PrivilegedActionException exception) {
                    throw ValidationException.unableToLoadClass(converter, exception.getException());
                } catch (ClassNotFoundException exception) {
                    throw ValidationException.unableToLoadClass(converter, exception);
                }

                try {
                    structConverters.add((StructConverter)this.buildObjectForClass(clazz, clazz));
                } catch (PrivilegedActionException e) {
                    throw ValidationException.errorInstantiatingClass(clazz, e.getException());
                } catch (IllegalAccessException e) {
                    throw ValidationException.errorInstantiatingClass(clazz, e);
                }  catch (InstantiationException e) {
                    throw ValidationException.errorInstantiatingClass(clazz, e);
                }
            }
        }
        return structConverters;
    }

    protected boolean hasSchemaDatabaseGeneration(Map m) {
        if (hasConfigProperty(SCHEMA_GENERATION_DATABASE_ACTION, m)) {
            return getConfigPropertyAsString(SCHEMA_GENERATION_DATABASE_ACTION, m) != null && ! getConfigPropertyAsString(SCHEMA_GENERATION_DATABASE_ACTION, m).equals(SCHEMA_GENERATION_NONE_ACTION);
        }

        return false;
    }

    protected boolean hasSchemaScriptsGeneration(Map m) {
        if (hasConfigProperty(SCHEMA_GENERATION_SCRIPTS_ACTION, m)) {
            return getConfigPropertyAsString(SCHEMA_GENERATION_SCRIPTS_ACTION, m) != null && ! getConfigPropertyAsString(SCHEMA_GENERATION_SCRIPTS_ACTION, m).equals(SCHEMA_GENERATION_NONE_ACTION);
        }

        return false;
    }

    public AbstractSession getSession() {
        return session;
    }

    public DatabaseSessionImpl getDatabaseSession() {
        return (DatabaseSessionImpl)session;
    }

    /**
     * We may be provided a connection via the properties to use. Check for
     * one and build a database session around it. Otherwise return the pu
     * database session.
     */
    public DatabaseSessionImpl getDatabaseSession(Map props) {
        DatabaseSessionImpl databaseSession = getDatabaseSession();
        Object connection = getConfigProperty(PersistenceUnitProperties.SCHEMA_GENERATION_CONNECTION, props);

        if (connection == null) {
            return databaseSession;
        } else {
            // A connection was provided. Build a database session using that
            // connection and use the same log level set on the original
            // database session.
            DatabaseSessionImpl newDatabaseSession = new DatabaseSessionImpl();
            newDatabaseSession.setAccessor(new DatabaseAccessor(connection));
            newDatabaseSession.setLogLevel(databaseSession.getLogLevel());
            newDatabaseSession.setProject(databaseSession.getProject().clone());
            return newDatabaseSession;
        }
    }

    /**
     * This method will be used to validate the specified class and return it's instance.
     */
    protected static Object buildObjectForClass(Class clazz, Class mustBeImplementedInterface) throws IllegalAccessException, PrivilegedActionException,InstantiationException {
        if(clazz!=null && Helper.classImplementsInterface(clazz,mustBeImplementedInterface)){
            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                return AccessController.doPrivileged(new PrivilegedNewInstanceFromClass(clazz));
            } else {
                return PrivilegedAccessHelper.newInstanceFromClass(clazz);
            }
        } else {
            return null;
        }
    }

    protected void updateDescriptorCacheSettings(Map m, ClassLoader loader) {
        String queryCache = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.QUERY_CACHE, m, session);
        if ((queryCache != null) && queryCache.equalsIgnoreCase("true")) {
            session.getProject().setDefaultQueryResultsCachePolicy(new QueryResultsCachePolicy());
        }

        Map typeMap = PropertiesHandler.getPrefixValuesLogDebug(PersistenceUnitProperties.CACHE_TYPE_, m, session);
        Map sizeMap = PropertiesHandler.getPrefixValuesLogDebug(PersistenceUnitProperties.CACHE_SIZE_, m, session);
        Map sharedMap = PropertiesHandler.getPrefixValuesLogDebug(PersistenceUnitProperties.CACHE_SHARED_, m, session);
        if(typeMap.isEmpty() && sizeMap.isEmpty() && sharedMap.isEmpty()) {
            return;
        }

        String defaultTypeName = (String)typeMap.remove(PersistenceUnitProperties.DEFAULT);
        if (defaultTypeName != null) {
            // Always use the EclipseLink class loader, otherwise can have loader/redeployment issues.
            Class defaultType = findClassForProperty(defaultTypeName, PersistenceUnitProperties.CACHE_TYPE_DEFAULT, getClass().getClassLoader());
            session.getProject().setDefaultIdentityMapClass(defaultType);
        }

        String value = null;
        try {
            String defaultSizeString = (String)sizeMap.remove(PersistenceUnitProperties.DEFAULT);
            if (defaultSizeString != null) {
                value = defaultSizeString;
                int defaultSize = Integer.parseInt(defaultSizeString);
                session.getProject().setDefaultIdentityMapSize(defaultSize);
            }

            String defaultSharedString = (String)sharedMap.remove(PersistenceUnitProperties.DEFAULT);
            if (defaultSharedString != null) {
                boolean defaultShared = Boolean.parseBoolean(defaultSharedString);
                session.getProject().setDefaultIsIsolated(!defaultShared);
            }

            Iterator it = session.getDescriptors().values().iterator();
            while (it.hasNext() && (!typeMap.isEmpty() || !sizeMap.isEmpty() || !sharedMap.isEmpty())) {
                ClassDescriptor descriptor = (ClassDescriptor)it.next();

                if (descriptor.isDescriptorTypeAggregate()) {
                    continue;
                }

                String entityName = descriptor.getAlias();
                String className = descriptor.getJavaClass().getName();
                String name;

                name = entityName;
                String typeName = (String)typeMap.remove(name);
                if( typeName == null) {
                    name = className;
                    typeName = (String)typeMap.remove(name);
                }
                if (typeName != null) {
                    Class type = findClassForProperty(typeName, PersistenceUnitProperties.CACHE_TYPE_ + name, getClass().getClassLoader());
                    descriptor.setIdentityMapClass(type);
                }

                name = entityName;
                String sizeString = (String)sizeMap.remove(name);
                if (sizeString == null) {
                    name = className;
                    sizeString = (String)sizeMap.remove(name);
                }
                if (sizeString != null) {
                    value = sizeString;
                    int size = Integer.parseInt(sizeString);
                    descriptor.setIdentityMapSize(size);
                }

                name = entityName;
                String sharedString = (String)sharedMap.remove(name);
                if (sharedString == null) {
                    name = className;
                    sharedString = (String)sharedMap.remove(name);
                }
                if (sharedString != null) {
                    boolean shared = Boolean.parseBoolean(sharedString);
                    descriptor.setIsIsolated(!shared);
                }
            }
        } catch (NumberFormatException exception) {
            this.session.handleException(ValidationException.invalidValueForProperty(value, PersistenceUnitProperties.CACHE_SIZE_, exception));
        }
    }

    /**
     * Process all properties under "eclipselink.connection-pool.".
     * This allows for named connection pools.
     * It also processes "read", "write", "default"  and "sequence" connection pools.
     */
    protected void updateConnectionSettings(ServerSession serverSession, Map properties) {
        Map<String, Object> connectionsMap = PropertiesHandler.getPrefixValuesLogDebug(PersistenceUnitProperties.CONNECTION_POOL, properties, serverSession);
        if (connectionsMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : connectionsMap.entrySet()) {
            String poolName = "default";
            String attribute = null;
            try {
                if (entry.getKey().indexOf(".") == -1) {
                    attribute = entry.getKey();
                } else {
                    poolName = entry.getKey().substring(0, entry.getKey().indexOf("."));
                    attribute = entry.getKey().substring(entry.getKey().indexOf(".") + 1, entry.getKey().length());
                }
                ConnectionPool pool = null;
                if (poolName.equals("write")) {
                    poolName = "default";
                }
                if (poolName.equals("read")) {
                    pool = serverSession.getReadConnectionPool();
                    // By default there is no connection pool, so if the default, create a new one.
                    if ((pool == null) || (pool == serverSession.getDefaultConnectionPool())) {
                        if (this.session.getDatasourceLogin().shouldUseExternalConnectionPooling()) {
                            pool = new ExternalConnectionPool(poolName, serverSession.getDatasourceLogin(), serverSession);
                        } else {
                            pool = new ConnectionPool(poolName, serverSession.getDatasourceLogin(), serverSession);
                        }
                        serverSession.setReadConnectionPool(pool);
                    }
                } else if (poolName.equals("sequence")) {
                    pool = getDatabaseSession().getSequencingControl().getConnectionPool();
                    if (pool == null) {
                        if (this.session.getDatasourceLogin().shouldUseExternalConnectionPooling()) {
                            pool = new ExternalConnectionPool(poolName, serverSession.getDatasourceLogin(), serverSession);
                        } else {
                            pool = new ConnectionPool(poolName, serverSession.getDatasourceLogin(), serverSession);
                        }
                        getDatabaseSession().getSequencingControl().setConnectionPool(pool);
                    }
                } else {
                    pool = serverSession.getConnectionPool(poolName);
                    if (pool == null) {
                        if (this.session.getDatasourceLogin().shouldUseExternalConnectionPooling()) {
                            pool = new ExternalConnectionPool(poolName, serverSession.getDatasourceLogin(), serverSession);
                        } else {
                            pool = new ConnectionPool(poolName, serverSession.getDatasourceLogin(), serverSession);
                        }
                        serverSession.addConnectionPool(pool);
                    }
                }
                if (attribute.equals(PersistenceUnitProperties.CONNECTION_POOL_INITIAL)) {
                    pool.setInitialNumberOfConnections(Integer.parseInt((String)entry.getValue()));
                } else if (attribute.equals(PersistenceUnitProperties.CONNECTION_POOL_MIN)) {
                    pool.setMinNumberOfConnections(Integer.parseInt((String)entry.getValue()));
                } else if (attribute.equals(PersistenceUnitProperties.CONNECTION_POOL_MAX)) {
                    pool.setMaxNumberOfConnections(Integer.parseInt((String)entry.getValue()));
                } else if (attribute.equals(PersistenceUnitProperties.CONNECTION_POOL_URL)) {
                    pool.setLogin(pool.getLogin().clone());
                    ((DatabaseLogin)pool.getLogin()).setURL((String)entry.getValue());
                } else if (attribute.equals(PersistenceUnitProperties.CONNECTION_POOL_NON_JTA_DATA_SOURCE)) {
                    pool.setLogin(pool.getLogin().clone());
                    ((DatabaseLogin)pool.getLogin()).useDataSource((String)entry.getValue());
                } else if (attribute.equals(PersistenceUnitProperties.CONNECTION_POOL_JTA_DATA_SOURCE)) {
                    pool.setLogin(pool.getLogin().clone());
                    ((DatabaseLogin)pool.getLogin()).useDataSource((String)entry.getValue());
                } else if (attribute.equals(PersistenceUnitProperties.CONNECTION_POOL_USER)) {
                    pool.setLogin(pool.getLogin().clone());
                    ((DatabaseLogin)pool.getLogin()).setUserName((String)entry.getValue());
                } else if (attribute.equals(PersistenceUnitProperties.CONNECTION_POOL_PASSWORD)) {
                    pool.setLogin(pool.getLogin().clone());
                    ((DatabaseLogin)pool.getLogin()).setPassword((String)entry.getValue());
                } else if (attribute.equals(PersistenceUnitProperties.CONNECTION_POOL_WAIT)) {
                    pool.setWaitTimeout(Integer.parseInt((String)entry.getValue()));
                } else if (attribute.equals(PersistenceUnitProperties.CONNECTION_POOL_FAILOVER)) {
                    String failoverPools = (String)entry.getValue();
                    if ((failoverPools.indexOf(',') != -1) || (failoverPools.indexOf(' ') != -1)) {
                        StringTokenizer tokenizer = new StringTokenizer(failoverPools, " ,");
                        while (tokenizer.hasMoreTokens()) {
                            pool.addFailoverConnectionPool(tokenizer.nextToken());
                        }
                    } else {
                        pool.addFailoverConnectionPool((String)entry.getValue());
                    }
                } else if (poolName.equals("read") && attribute.equals(PersistenceUnitProperties.CONNECTION_POOL_SHARED)) {
                    boolean shared = Boolean.parseBoolean((String)entry.getValue());
                    if (shared) {
                        ReadConnectionPool readPool = new ReadConnectionPool(poolName, serverSession.getDatasourceLogin(), serverSession);
                        readPool.setInitialNumberOfConnections(pool.getInitialNumberOfConnections());
                        readPool.setMinNumberOfConnections(pool.getMinNumberOfConnections());
                        readPool.setMaxNumberOfConnections(pool.getMaxNumberOfConnections());
                        readPool.setWaitTimeout(pool.getWaitTimeout());
                        readPool.setLogin(pool.getLogin());
                        serverSession.setReadConnectionPool(readPool);
                    }
                }
            } catch (RuntimeException exception) {
                this.session.handleException(ValidationException.invalidValueForProperty(entry.getValue(), entry.getKey(), exception));
            }
        }
    }

    protected void updateConnectionPolicy(ServerSession serverSession, Map m) {
        String isLazyString = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.EXCLUSIVE_CONNECTION_IS_LAZY, m, session);
        if(isLazyString != null) {
            serverSession.getDefaultConnectionPolicy().setIsLazy(Boolean.parseBoolean(isLazyString));
        }
        ConnectionPolicy.ExclusiveMode exclusiveMode = getConnectionPolicyExclusiveModeFromProperties(m, session, true);
        if(exclusiveMode != null) {
            serverSession.getDefaultConnectionPolicy().setExclusiveMode(exclusiveMode);
        }
    }

    public static ConnectionPolicy.ExclusiveMode getConnectionPolicyExclusiveModeFromProperties(Map m, AbstractSession abstractSession, boolean useSystemAsDefault) {
        String exclusiveConnectionModeString = PropertiesHandler.getPropertyValueLogDebug(PersistenceUnitProperties.EXCLUSIVE_CONNECTION_MODE, m, abstractSession, useSystemAsDefault);
        if(exclusiveConnectionModeString != null) {
            if(exclusiveConnectionModeString == ExclusiveConnectionMode.Isolated) {
                return ConnectionPolicy.ExclusiveMode.Isolated;
            } else if(exclusiveConnectionModeString == ExclusiveConnectionMode.Always) {
                return ConnectionPolicy.ExclusiveMode.Always;
            } else {
                return ConnectionPolicy.ExclusiveMode.Transactional;
            }
        } else {
            return null;
        }
    }

    /**
     * Perform any steps necessary prior to actual deployment.  This includes any steps in the session
     * creation that do not require the real loaded domain classes.
     *
     * The first call to this method caches persistenceUnitInfo which is reused in the following calls.
     *
     * Note that in JSE case factoryCount is NOT incremented on the very first call
     * (by JavaSECMPInitializer.callPredeploy, typically in preMain).
     * That provides 1 to 1 correspondence between factoryCount and the number of open factories.
     *
     * In case factoryCount &gt; 0 the method just increments factoryCount.
     * factory == 0 triggers creation of a new session.
     *
     * This method and undeploy - the only methods altering factoryCount - should be synchronized.
     *
     * @return A transformer (which may be null) that should be plugged into the proper
     *         classloader to allow classes to be transformed as they get loaded.
     * @see #deploy(ClassLoader, Map)
     */
    public synchronized ClassTransformer predeploy(PersistenceUnitInfo info, Map extendedProperties) {
        ClassLoader classLoaderToUse = null;
        if (state == STATE_DEPLOY_FAILED || state == STATE_UNDEPLOYED) {
            throw new PersistenceException(EntityManagerSetupException.cannotPredeploy(persistenceUnitInfo.getPersistenceUnitName(), state, persistenceException));
        }
        if (state == STATE_PREDEPLOYED || state == STATE_DEPLOYED || state == STATE_HALF_DEPLOYED) {
            session.log(SessionLog.FINEST, SessionLog.JPA, "predeploy_begin", new Object[]{getPersistenceUnitInfo().getPersistenceUnitName(), session.getName(), state, factoryCount});
            factoryCount++;
            session.log(SessionLog.FINEST, SessionLog.JPA, "predeploy_end", new Object[]{getPersistenceUnitInfo().getPersistenceUnitName(), session.getName(), state, factoryCount});
            return null;
        } else if (state == STATE_INITIAL) {
            persistenceUnitInfo = info;
            if (!isCompositeMember()) {
                if (mustBeCompositeMember(persistenceUnitInfo)) {
                    if (this.staticWeaveInfo == null) {
                        return null;
                    } else {
                        // predeploy is used for static weaving
                        throw new PersistenceException(EntityManagerSetupException.compositeMemberCannotBeUsedStandalone(persistenceUnitInfo.getPersistenceUnitName()));
                    }
                }
            }
        } else if (state == STATE_HALF_PREDEPLOYED_COMPOSITE_MEMBER) {
            session.log(SessionLog.FINEST, SessionLog.JPA, "predeploy_begin", new Object[]{getPersistenceUnitInfo().getPersistenceUnitName(), session.getName(), state + " " + mode, factoryCount});
        }

        // state is INITIAL or PREDEPLOY_FAILED or STATE_HALF_PREDEPLOYED_COMPOSITE_MEMBER
        try {
            // properties not used in STATE_HALF_PREDEPLOYED_COMPOSITE_MEMBER
            Map predeployProperties = null;
            // composite can't be in STATE_HALF_PREDEPLOYED_COMPOSITE_MEMBER
            boolean isComposite = false;
            if(state != STATE_HALF_PREDEPLOYED_COMPOSITE_MEMBER) {
                //set the claasloader early on and change it if needed
                classLoaderToUse = persistenceUnitInfo.getClassLoader();

                predeployProperties = mergeMaps(extendedProperties, persistenceUnitInfo.getProperties());
                // Translate old properties.
                // This should be done before using properties (i.e. ServerPlatform).
                translateOldProperties(predeployProperties, null);

                String sessionsXMLStr = (String)predeployProperties.get(PersistenceUnitProperties.SESSIONS_XML);
                if (sessionsXMLStr != null) {
                    isSessionLoadedFromSessionsXML = true;
                }

                // Create session (it needs to be done before initializing ServerPlatform and logging).
                // If a sessions-xml is used this will get replaced later, but is required for logging.
                isComposite = isComposite(persistenceUnitInfo);
                if (isComposite) {
                    if (isSessionLoadedFromSessionsXML) {
                        throw EntityManagerSetupException.compositeIncompatibleWithSessionsXml(persistenceUnitInfo.getPersistenceUnitName());
                    }
                    session = new SessionBroker();
                    ((SessionBroker)session).setShouldUseDescriptorAliases(true);
                } else {
                    session = new ServerSession(new Project(new DatabaseLogin()));

                    //set the listener to process RCM metadata refresh commands
                    session.setRefreshMetadataListener(this);
                }
                session.setName(this.sessionName);
                updateTunerPreDeploy(predeployProperties, classLoaderToUse);
                updateTolerateInvalidJPQL(predeployProperties);

                if (this.compositeEmSetupImpl == null) {
                    // session name and ServerPlatform must be set prior to setting the loggers.
                    if (this.staticWeaveInfo == null) {
                        updateServerPlatform(predeployProperties, classLoaderToUse);
                        // Update loggers and settings for the singleton logger and the session logger.
                        updateLoggers(predeployProperties, true, classLoaderToUse);
                        // log the server platform being used by the session
                        if (session.getSessionLog().shouldLog(SessionLog.FINE)) {
                            session.getSessionLog().log(SessionLog.FINE, SessionLog.SERVER,
                                    "configured_server_platform", session.getServerPlatform().getClass().getName()); // NOI18N
                        }
                        // Get the temporary classLoader based on the platform

                        //Update performance profiler
                        updateProfiler(predeployProperties, classLoaderToUse);
                    } else {
                        // predeploy is used for static weaving
                        Writer writer = this.staticWeaveInfo.getLogWriter();
                        if (writer != null) {
                            ((DefaultSessionLog)session.getSessionLog()).setWriter(writer);
                        }
                        session.setLogLevel(this.staticWeaveInfo.getLogLevel());
                    }
                } else {
                    // composite member
                    session.setSessionLog(this.compositeEmSetupImpl.session.getSessionLog());
                    session.setProfiler(this.compositeEmSetupImpl.session.getProfiler());
                }

                // Cannot start logging until session and log and initialized, so log start of predeploy here.
                session.log(SessionLog.FINEST, SessionLog.JPA, "predeploy_begin", new Object[]{getPersistenceUnitInfo().getPersistenceUnitName(), session.getName(), state, factoryCount});

                //Project Cache accessor processing
                updateProjectCache(predeployProperties, classLoaderToUse);

                if (projectCacheAccessor!=null) {
                    //get the project from the cache
                    Project project = projectCacheAccessor.retrieveProject(predeployProperties, classLoaderToUse, session.getSessionLog());

                    if (project!=null) {
                        try {
                            DatabaseSessionImpl tempSession = (DatabaseSessionImpl)project.createServerSession();

                            tempSession.setName(this.sessionName);
                            tempSession.setSessionLog(session.getSessionLog());
                            tempSession.getSessionLog().setSession(tempSession);
                            if (this.staticWeaveInfo != null) {
                                tempSession.setLogLevel(this.staticWeaveInfo.getLogLevel());
                            }
                            tempSession.setProfiler(session.getProfiler());
                            tempSession.setRefreshMetadataListener(this);

                            session = tempSession;
                            // reusing the serverPlatform from the existing session would have been preferred,
                            //  but its session is only set through the ServerPlatform constructor.
                            updateServerPlatform(predeployProperties, classLoaderToUse);
                            shouldBuildProject = false;
                        } catch (Exception e) {
                            //need a better exception here
                            throw new PersistenceException(e);
                        }
                    }
                }

                if (isSessionLoadedFromSessionsXML) {
                    if (this.compositeEmSetupImpl == null && this.staticWeaveInfo == null) {
                        JPAClassLoaderHolder privateClassLoaderHolder = session.getServerPlatform().getNewTempClassLoader(persistenceUnitInfo);
                        classLoaderToUse = privateClassLoaderHolder.getClassLoader();
                    } else {
                        classLoaderToUse = persistenceUnitInfo.getNewTempClassLoader();
                    }
                    // Loading session from sessions-xml.
                    String tempSessionName = sessionName;
                    if (isCompositeMember()) {
                        // composite member session name is always the same as puName
                        // need the session name specified in properties to read correct session from sessions.xml
                        tempSessionName = (String)predeployProperties.get(PersistenceUnitProperties.SESSION_NAME);
                    }
                    session.log(SessionLog.FINEST, SessionLog.PROPERTIES, "loading_session_xml", sessionsXMLStr, tempSessionName);
                    if (tempSessionName == null) {
                        throw EntityManagerSetupException.sessionNameNeedBeSpecified(persistenceUnitInfo.getPersistenceUnitName(), sessionsXMLStr);
                    }
                    XMLSessionConfigLoader xmlLoader = new XMLSessionConfigLoader(sessionsXMLStr);
                    // Do not register the session with the SessionManager at this point, create temporary session using a local SessionManager and private class loader.
                    // This allows for the project to be accessed without loading any of the classes to allow weaving.
                    // Note that this method assigns sessionName to session.
                    Session tempSession = new SessionManager().getSession(xmlLoader, tempSessionName, classLoaderToUse, false, false);
                    // Load path of sessions-xml resource before throwing error so user knows which sessions-xml file was found (may be multiple).
                    session.log(SessionLog.FINEST, SessionLog.PROPERTIES, "sessions_xml_path_where_session_load_from", xmlLoader.getSessionName(), xmlLoader.getResourcePath());
                    if (tempSession == null) {
                        throw ValidationException.noSessionFound(sessionName, sessionsXMLStr);
                    }
                    // Currently the session must be either a ServerSession or a SessionBroker, cannot be just a DatabaseSessionImpl.
                    if (tempSession.isServerSession() || tempSession.isSessionBroker()) {
                       session = (DatabaseSessionImpl) tempSession;
                       if (tempSessionName != sessionName) {
                           // set back the original session name
                           session.setName(sessionName);
                       }
                    } else {
                        throw EntityManagerSetupException.sessionLoadedFromSessionsXMLMustBeServerSession(persistenceUnitInfo.getPersistenceUnitName(), (String)predeployProperties.get(PersistenceUnitProperties.SESSIONS_XML), tempSession);
                    }
                    if (this.staticWeaveInfo == null) {
                        // Must now reset logging and server-platform on the loaded session.
                        // ServerPlatform must be set prior to setting the loggers.
                        updateServerPlatform(predeployProperties, classLoaderToUse);
                        // Update loggers and settings for the singleton logger and the session logger.
                        updateLoggers(predeployProperties, true, classLoaderToUse);
                    }
                } else {
                    classLoaderToUse = persistenceUnitInfo.getClassLoader();
                }

                warnOldProperties(predeployProperties, session);
                session.getPlatform().setConversionManager(new JPAConversionManager());

                if (this.staticWeaveInfo == null) {
                    if (!isComposite) {
                        PersistenceUnitTransactionType transactionType=null;
                        //bug 5867753: find and override the transaction type
                        String transTypeString = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.TRANSACTION_TYPE, predeployProperties, session);
                        if (transTypeString != null && transTypeString.length() > 0) {
                            transactionType=PersistenceUnitTransactionType.valueOf(transTypeString);
                        } else if (persistenceUnitInfo!=null){
                            transactionType=persistenceUnitInfo.getTransactionType();
                        }

                        if (!isValidationOnly(predeployProperties, false) && persistenceUnitInfo != null && transactionType == PersistenceUnitTransactionType.JTA) {
                            if (predeployProperties.get(PersistenceUnitProperties.JTA_DATASOURCE) == null && persistenceUnitInfo.getJtaDataSource() == null) {
                                if (predeployProperties.get(PersistenceUnitProperties.SCHEMA_DATABASE_PRODUCT_NAME) == null ||
                                        predeployProperties.get(PersistenceUnitProperties.SCHEMA_DATABASE_MAJOR_VERSION) == null ||
                                        predeployProperties.get(PersistenceUnitProperties.SCHEMA_DATABASE_MINOR_VERSION) == null) {
                                    throw EntityManagerSetupException.jtaPersistenceUnitInfoMissingJtaDataSource(persistenceUnitInfo.getPersistenceUnitName());
                                }
                            }
                        }
                    }

                    // this flag is used to disable work done as a result of the LAZY hint on OneToOne and ManyToOne mappings
                    if(state == STATE_INITIAL) {
                        if (compositeEmSetupImpl == null) {
                            if(null == enableWeaving) {
                                enableWeaving = Boolean.TRUE;
                            }
                            isWeavingStatic = false;
                            String weaving = getConfigPropertyAsString(PersistenceUnitProperties.WEAVING, predeployProperties);

                            if (weaving != null && weaving.equalsIgnoreCase("false")) {
                                enableWeaving = Boolean.FALSE;
                            }else if (weaving != null && weaving.equalsIgnoreCase("static")) {
                                isWeavingStatic = true;
                            }
                        } else {
                            // composite member
                            // no weaving for composite forces no weaving for members
                            if (!compositeEmSetupImpl.enableWeaving) {
                                enableWeaving = Boolean.FALSE;
                            } else {
                                if(null == enableWeaving) {
                                    enableWeaving = Boolean.TRUE;
                                }
                                String weaving = getConfigPropertyAsString(PersistenceUnitProperties.WEAVING, predeployProperties);
                                if (weaving != null && weaving.equalsIgnoreCase("false")) {
                                    enableWeaving = Boolean.FALSE;
                                }
                            }
                            // static weaving is dictated by composite
                            isWeavingStatic = compositeEmSetupImpl.isWeavingStatic;
                        }
                    }

                    if (compositeEmSetupImpl == null) {
                        throwExceptionOnFail = "true".equalsIgnoreCase(
                                EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.THROW_EXCEPTIONS, predeployProperties, "true", session));
                    } else {
                        // composite member
                        throwExceptionOnFail = compositeEmSetupImpl.throwExceptionOnFail;
                    }
                } else {
                    // predeploy is used for static weaving
                    enableWeaving = Boolean.TRUE;
                }

                weaveChangeTracking = false;
                weaveLazy = false;
                weaveEager = false;
                weaveFetchGroups = false;
                weaveInternal = false;
                weaveRest = false;
                weaveMappedSuperClass = false;
                if (enableWeaving) {
                    weaveChangeTracking = "true".equalsIgnoreCase(EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.WEAVING_CHANGE_TRACKING, predeployProperties, "true", session));
                    weaveLazy = "true".equalsIgnoreCase(EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.WEAVING_LAZY, predeployProperties, "true", session));
                    weaveEager = "true".equalsIgnoreCase(EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.WEAVING_EAGER, predeployProperties, "false", session));
                    weaveFetchGroups = "true".equalsIgnoreCase(EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.WEAVING_FETCHGROUPS, predeployProperties, "true", session));
                    weaveInternal = "true".equalsIgnoreCase(EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.WEAVING_INTERNAL, predeployProperties, "true", session));
                    weaveRest = "true".equalsIgnoreCase(EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.WEAVING_REST, predeployProperties, "true", session));
                    weaveMappedSuperClass = "true".equalsIgnoreCase(EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.WEAVING_MAPPEDSUPERCLASS, predeployProperties, "true", session));
                }

            }
            if (shouldBuildProject && !isSessionLoadedFromSessionsXML ) {
                if (isComposite) {
                    predeployCompositeMembers(predeployProperties, classLoaderToUse);
                } else {
                    MetadataProcessor compositeProcessor = null;
                    if (compositeEmSetupImpl == null) {
                        mode = PersistenceUnitProcessor.Mode.ALL;
                    } else {
                        // composite member
                        if (state != STATE_HALF_PREDEPLOYED_COMPOSITE_MEMBER) {
                            state = STATE_HALF_PREDEPLOYED_COMPOSITE_MEMBER;
                            mode = PersistenceUnitProcessor.Mode.COMPOSITE_MEMBER_INITIAL;
                        }
                        compositeProcessor = compositeEmSetupImpl.processor;
                    }

                    if (mode == PersistenceUnitProcessor.Mode.ALL || mode == PersistenceUnitProcessor.Mode.COMPOSITE_MEMBER_INITIAL) {
                        boolean usesMultitenantSharedEmf = "true".equalsIgnoreCase(EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.MULTITENANT_SHARED_EMF, predeployProperties, "true", session));
                        boolean usesMultitenantSharedCache = "true".equalsIgnoreCase(EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.MULTITENANT_SHARED_CACHE, predeployProperties, "false", session));

                        // Create an instance of MetadataProcessor for specified persistence unit info
                        processor = new MetadataProcessor(persistenceUnitInfo, session, classLoaderToUse, weaveLazy, weaveEager, weaveFetchGroups, usesMultitenantSharedEmf, usesMultitenantSharedCache, predeployProperties, compositeProcessor);

                        //need to use the real classloader to create the repository class
                        updateMetadataRepository(predeployProperties, classLoaderToUse);

                        //bug:299926 - Case insensitive table / column matching with native SQL queries
                        EntityManagerSetupImpl.updateCaseSensitivitySettings(predeployProperties, processor.getProject(), session);
                    }

                    // Set the shared cache mode to the javax.persistence.sharedCache.mode property value.
                    updateSharedCacheMode(predeployProperties);

                    // Process the Object/relational metadata from XML and annotations.
                    // If Java Security is enabled, surround this call with a doPrivileged block.
                    if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
                        AccessController.doPrivileged(new PrivilegedAction<Void>() {
                            @Override
                            public Void run() {
                                PersistenceUnitProcessor.processORMetadata(processor, throwExceptionOnFail, mode);
                                return null;
                            }
                        });
                    } else {
                        PersistenceUnitProcessor.processORMetadata(processor, throwExceptionOnFail, mode);
                    }

                    if (mode == PersistenceUnitProcessor.Mode.COMPOSITE_MEMBER_INITIAL) {
                        mode = PersistenceUnitProcessor.Mode.COMPOSITE_MEMBER_MIDDLE;
                        session.log(SessionLog.FINEST, SessionLog.JPA, "predeploy_end", new Object[]{getPersistenceUnitInfo().getPersistenceUnitName(), session.getName(), state + " " + mode , factoryCount});
                        return null;
                    } else if (mode == PersistenceUnitProcessor.Mode.COMPOSITE_MEMBER_MIDDLE) {
                        mode = PersistenceUnitProcessor.Mode.COMPOSITE_MEMBER_FINAL;
                        session.log(SessionLog.FINEST, SessionLog.JPA, "predeploy_end", new Object[]{getPersistenceUnitInfo().getPersistenceUnitName(), session.getName(), state + " " + mode , factoryCount});
                        return null;
                    }
                    // mode == PersistenceUnitProcessor.Mode.ALL || mode == PersistenceUnitProcessor.Mode.COMPOSITE_MEMBER_FINAL
                    // clear mode and proceed
                    mode = null;

                    if (session.getIntegrityChecker().hasErrors()){
                        session.handleException(new IntegrityException(session.getIntegrityChecker()));
                    }

                    // The transformer is capable of altering domain classes to handle a LAZY hint for OneToOne mappings.  It will only
                    // be returned if we we are mean to process these mappings
                    if (enableWeaving) {
                        // build a list of entities the persistence unit represented by this EntityManagerSetupImpl will use
                        Collection entities = PersistenceUnitProcessor.buildEntityList(processor, classLoaderToUse);
                        this.weaver = TransformerFactory.createTransformerAndModifyProject(session, entities, classLoaderToUse, weaveLazy, weaveChangeTracking, weaveFetchGroups, weaveInternal, weaveRest, weaveMappedSuperClass);
                        session.getProject().setClassNamesForWeaving(new ArrayList(processor.getProject().getWeavableClassNames()));
                    }

                    //moved from deployment:
                    processor.addNamedQueries();
                    processor.addStructConverterNames();
                }
            } else {
                //This means this session is from sessions.xml or a cached project

                // The transformer is capable of altering domain classes to handle a LAZY hint for OneToOne mappings.  It will only
                // be returned if we we are meant to process these mappings.
                if (enableWeaving) {
                    Collection persistenceClasses = new ArrayList();
                    MetadataAsmFactory factory = new MetadataAsmFactory(new MetadataLogger(session), classLoaderToUse);
                    if (shouldBuildProject) {
                        // If deploying from a sessions-xml it is still desirable to allow the classes to be weaved.
                        // build a list of entities the persistence unit represented by this EntityManagerSetupImpl will use
                        for (Iterator iterator = session.getProject().getDescriptors().keySet().iterator(); iterator.hasNext(); ) {
                            persistenceClasses.add(factory.getMetadataClass(((Class)iterator.next()).getName()));
                        }
                    } else {
                        // build a list of entities the persistence unit represented by this EntityManagerSetupImpl will use
                        for (String className : session.getProject().getClassNamesForWeaving()) {
                            persistenceClasses.add(factory.getMetadataClass(className));
                        }
                    }
                    this.weaver = TransformerFactory.createTransformerAndModifyProject(session, persistenceClasses, classLoaderToUse, weaveLazy, weaveChangeTracking, weaveFetchGroups, weaveInternal, weaveRest, weaveMappedSuperClass);
                }
            }

            // composite member never has a factory - it is predeployed by the composite.
            if (!isCompositeMember()) {
                // factoryCount is not incremented only in case of a first call to preDeploy
                // in non-container mode: this call is not associated with a factory
                // but rather done by JavaSECMPInitializer.callPredeploy (typically in preMain).
                if(state != STATE_INITIAL || this.isInContainerMode()) {
                    factoryCount++;
                }
                preInitializeMetamodel();
            }

            state = STATE_PREDEPLOYED;
            session.log(SessionLog.FINEST, SessionLog.JPA, "predeploy_end", new Object[]{getPersistenceUnitInfo().getPersistenceUnitName(), session.getName(), state, factoryCount});
            //gf3146: if static weaving is used, we should not return a transformer.  Transformer should still be created though as it modifies descriptors
            if (isWeavingStatic) {
                return null;
            } else {
                return this.weaver;
            }
        } catch (Throwable ex) {
            state = STATE_PREDEPLOY_FAILED;
            // cache this.persistenceException before slow logging
            PersistenceException persistenceEx = createPredeployFailedPersistenceException(ex);
            session.log(SessionLog.FINEST, SessionLog.JPA, "predeploy_end", new Object[]{getPersistenceUnitInfo().getPersistenceUnitName(), session.getName(), state, factoryCount});
            session = null;
            mode = null;
            throw persistenceEx;
        }
    }

    protected PersistenceException createPredeployFailedPersistenceException(Throwable ex) {
        PersistenceException perEx = new PersistenceException(EntityManagerSetupException.predeployFailed(persistenceUnitInfo.getPersistenceUnitName(), ex));
        if (persistenceException == null) {
            persistenceException = perEx;
        }
        return perEx;
    }

    /**
     * Return the name of the session this SetupImpl is building. The session name is only known at deploy
     * time and if this method is called prior to that, this method will return null.
     */
    public String getDeployedSessionName(){
        return session != null ? session.getName() : null;
    }

    public PersistenceUnitInfo getPersistenceUnitInfo(){
        return persistenceUnitInfo;
    }

    public boolean isValidationOnly(Map m) {
        return isValidationOnly(m, true);
    }

    protected boolean isValidationOnly(Map m, boolean shouldMergeMap) {
        if (shouldMergeMap) {
            m = mergeWithExistingMap(m);
        }
        String validationOnlyString = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.VALIDATION_ONLY_PROPERTY, m, session);
        if (validationOnlyString != null) {
            return Boolean.parseBoolean(validationOnlyString);
        } else {
            return false;
        }
    }

    /**
     * Return if the session should be deployed and connected during the creation of the EntityManagerFactory,
     * or if it should be deferred until createEntityManager().
     * The default is to defer, but is only validating, or can be configured to deploy upfront to avoid hanging the
     * application at runtime.
     */
    public boolean shouldGetSessionOnCreateFactory(Map m) {
        m = mergeWithExistingMap(m);
        if (isValidationOnly(m, false)) {
            return true;
        }

        String deployString = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.DEPLOY_ON_STARTUP, m, this.session);
        if (deployString != null) {
            return Boolean.parseBoolean(deployString);
        } else {
            // If DDL schame generation is turned on, we need to deploy.
            return hasSchemaDatabaseGeneration(m) || hasSchemaScriptsGeneration(m);
        }
    }

    protected Map mergeWithExistingMap(Map m) {
        if(persistenceUnitInfo != null) {
            return mergeMaps(m, persistenceUnitInfo.getProperties());
        } else {
            return m;
        }
    }

    public boolean isInContainerMode(){
        return isInContainerMode;
    }

    /**
     * Configure cache coordination using properties.
     */
    protected void updateCacheCoordination(Map m, ClassLoader loader) {
        String protocol = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_PROTOCOL, m, this.session);
        String value = "";
        String property = "";
        try {
            if (protocol != null) {
                RemoteCommandManager rcm = new RemoteCommandManager(this.session);
                if (protocol.equalsIgnoreCase(CacheCoordinationProtocol.JGROUPS)) {
                    property = PersistenceUnitProperties.COORDINATION_PROTOCOL;
                    value = "org.eclipse.persistence.sessions.coordination.jgroups.JGroupsTransportManager";
                    // Avoid compile and runtime dependency.
                    Class transportClass = findClassForProperty(value, PersistenceUnitProperties.COORDINATION_PROTOCOL, loader);
                    TransportManager transport = (TransportManager)transportClass.newInstance();
                    rcm.setTransportManager(transport);
                    String config = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_JGROUPS_CONFIG, m, this.session);
                    if (config != null) {
                        transport.setConfig(config);
                    }
                } else if (protocol.equalsIgnoreCase(CacheCoordinationProtocol.JMS) || protocol.equalsIgnoreCase(CacheCoordinationProtocol.JMSPublishing)) {
                    JMSPublishingTransportManager transport = null;
                    if (protocol.equalsIgnoreCase(CacheCoordinationProtocol.JMS)) {
                         transport = new JMSTopicTransportManager(rcm);
                    } else {
                        transport = new JMSPublishingTransportManager(rcm);
                    }
                    rcm.setTransportManager(transport);

                    String host = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_JMS_HOST, m, this.session);
                    if (host != null) {
                        transport.setTopicHostUrl(host);
                    }
                    String topic = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_JMS_TOPIC, m, this.session);
                    if (topic != null) {
                        transport.setTopicName(topic);
                    }
                    String factory = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_JMS_FACTORY, m, this.session);
                    if (factory != null) {
                        transport.setTopicConnectionFactoryName(factory);
                    }

                    String reuse_publisher = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_JMS_REUSE_PUBLISHER, m, this.session);
                    if (reuse_publisher != null) {
                        transport.setShouldReuseJMSTopicPublisher(reuse_publisher.equalsIgnoreCase("true"));
                    }

                } else if (protocol.equalsIgnoreCase(CacheCoordinationProtocol.RMI) || protocol.equalsIgnoreCase(CacheCoordinationProtocol.RMIIIOP)) {
                    if (protocol.equalsIgnoreCase(CacheCoordinationProtocol.RMIIIOP)) {
                        ((RMITransportManager)rcm.getTransportManager()).setIsRMIOverIIOP(true);
                    }
                    // Default protocol.
                    String delay = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_RMI_ANNOUNCEMENT_DELAY, m, this.session);
                    property = PersistenceUnitProperties.COORDINATION_RMI_ANNOUNCEMENT_DELAY;
                    value = delay;
                    if (delay != null) {
                        rcm.getDiscoveryManager().setAnnouncementDelay(Integer.parseInt(delay));
                    }
                    String multicast = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_RMI_MULTICAST_GROUP, m, this.session);
                    if (multicast != null) {
                        rcm.getDiscoveryManager().setMulticastGroupAddress(multicast);
                    }
                    String port = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_RMI_MULTICAST_GROUP_PORT, m, this.session);
                    property = PersistenceUnitProperties.COORDINATION_RMI_MULTICAST_GROUP_PORT;
                    value = port;
                    if (port != null) {
                        rcm.getDiscoveryManager().setMulticastPort(Integer.parseInt(port));
                    }
                    String timeToLive = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_RMI_PACKET_TIME_TO_LIVE, m, this.session);
                    property = PersistenceUnitProperties.COORDINATION_RMI_PACKET_TIME_TO_LIVE;
                    value = timeToLive;
                    if (timeToLive != null) {
                        rcm.getDiscoveryManager().setPacketTimeToLive(Integer.parseInt(timeToLive));
                    }
                    String url = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_RMI_URL, m, this.session);
                    if (url != null) {
                        rcm.setUrl(url);
                    }
                } else {
                    property = PersistenceUnitProperties.COORDINATION_PROTOCOL;
                    value = protocol;
                    Class transportClass = findClassForProperty(protocol, PersistenceUnitProperties.COORDINATION_PROTOCOL, loader);
                    rcm.setTransportManager((TransportManager)transportClass.newInstance());
                }
                String serializer = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_SERIALIZER, m, this.session);
                if (serializer != null) {
                    property = PersistenceUnitProperties.COORDINATION_SERIALIZER;
                    value = serializer;
                    Class transportClass = findClassForProperty(serializer, PersistenceUnitProperties.COORDINATION_SERIALIZER, loader);
                    rcm.setSerializer((Serializer)transportClass.newInstance());
                }

                String naming = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_NAMING_SERVICE, m, this.session);
                if (naming != null) {
                    if (naming.equalsIgnoreCase("jndi")) {
                        rcm.getTransportManager().setNamingServiceType(TransportManager.JNDI_NAMING_SERVICE);
                    } else if (naming.equalsIgnoreCase("rmi")) {
                        rcm.getTransportManager().setNamingServiceType(TransportManager.REGISTRY_NAMING_SERVICE);
                    }
                }
                String user = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_JNDI_USER, m, this.session);
                if (user != null) {
                    rcm.getTransportManager().setUserName(user);
                }
                String password = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_JNDI_PASSWORD, m, this.session);
                if (password != null) {
                    rcm.getTransportManager().setPassword(password);
                }
                String context = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_JNDI_CONTEXT, m, this.session);
                if (context != null) {
                    rcm.getTransportManager().setInitialContextFactoryName(context);
                }
                String removeOnError = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_REMOVE_CONNECTION, m, this.session);
                if (removeOnError != null) {
                    rcm.getTransportManager().setShouldRemoveConnectionOnError(removeOnError.equalsIgnoreCase("true"));
                }
                String asynch = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_ASYNCH, m, this.session);
                if (asynch != null) {
                    rcm.setShouldPropagateAsynchronously(asynch.equalsIgnoreCase("true"));
                }
                String threadPoolSize = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_THREAD_POOL_SIZE, m, this.session);
                property = PersistenceUnitProperties.COORDINATION_THREAD_POOL_SIZE;
                value = threadPoolSize;
                if (threadPoolSize != null) {
                    this.session.getServerPlatform().setThreadPoolSize(Integer.parseInt(threadPoolSize));
                }
                String channel = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.COORDINATION_CHANNEL, m, this.session);
                if (channel != null) {
                    rcm.setChannel(channel);
                }
                this.session.setCommandManager(rcm);
                this.session.setShouldPropagateChanges(true);
            }
        } catch (ReflectiveOperationException | NumberFormatException exception) {
            this.session.handleException(ValidationException.invalidValueForProperty(value, property, exception));
        }
    }

    /**
     * Update session serializer.
     */
    protected void updateSerializer(Map m, ClassLoader loader) {
        String serializer = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.SERIALIZER, m, this.session);
        if (serializer != null) {
            if (serializer.length() > 0) {
                try {
                    Class transportClass = findClassForProperty(serializer, PersistenceUnitProperties.SERIALIZER, loader);
                    this.session.setSerializer((Serializer)transportClass.newInstance());
                } catch (Exception exception) {
                    this.session.handleException(ValidationException.invalidValueForProperty(serializer, PersistenceUnitProperties.SERIALIZER, exception));
                }
            } else {
                this.session.setSerializer(JavaSerializer.instance);
            }
        }
    }

    /**
     * Update whether session ShouldOptimizeResultSetAccess.
     */
    protected void updateShouldOptimizeResultSetAccess(Map m) {
       String resultSetAccessOptimization = PropertiesHandler.getPropertyValueLogDebug(PersistenceUnitProperties.JDBC_RESULT_SET_ACCESS_OPTIMIZATION, m, this.session);
       if (resultSetAccessOptimization != null) {
          this.session.setShouldOptimizeResultSetAccess(resultSetAccessOptimization.equals("true"));
       }
    }

    /**
     * Update whether session should use externally defined multi tenancy.
     */
    protected void updateTenancy(Map m, ClassLoader loader) {
        String tenantStrategy = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.MULTITENANT_STRATEGY, m, this.session);
        if(tenantStrategy != null) {
            if ("external".equalsIgnoreCase(tenantStrategy)) {
                SchemaPerMultitenantPolicy policy = new SchemaPerMultitenantPolicy();
                String prop = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.MULTITENANT_SHARED_EMF, m, session);
                if (prop != null) {
                    policy.setShouldUseSharedEMF(Boolean.valueOf(prop));
                }
                prop = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.MULTITENANT_SHARED_CACHE, m, session);
                if (prop != null) {
                    policy.setShouldUseSharedCache(Boolean.valueOf(prop));
                }
                session.getProject().setMultitenantPolicy(policy);
            } else {
                //assume it is a class with default constructor implementing existing interface
                Class cls = findClassForProperty(tenantStrategy, PersistenceUnitProperties.MULTITENANT_STRATEGY, loader);
                MultitenantPolicy policy = null;
                try {
                    Constructor constructor = cls.getConstructor();
                    policy = (MultitenantPolicy) constructor.newInstance();
                } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    throw EntityManagerSetupException.failedToInstantiateProperty(tenantStrategy, PersistenceUnitProperties.MULTITENANT_STRATEGY, ex);
                }
                session.getProject().setMultitenantPolicy(policy);
            }
        }
    }

    /**
     * Update whether session should tolerate invalid JPQL at creation time.
     */
    protected void updateTolerateInvalidJPQL(Map m) {
        String config =
            PropertiesHandler.getPropertyValueLogDebug(PersistenceUnitProperties.JPQL_TOLERATE, m, this.session);
        // Tolerate invalid JPQL is ignored if running in validation only mode
        if (config != null && isValidationOnly(m) == false) {
            this.session.setTolerateInvalidJPQL(config.equals("true"));
        }
    }

    /**
     * Override the default login creation method.
     * If persistenceInfo is available, use the information from it to setup the login
     * and possibly to set readConnectionPool.
     */
    protected void updateLogins(Map m){
        DatasourceLogin login = (DatasourceLogin)this.session.getDatasourceLogin();

        String eclipselinkPlatform = PropertiesHandler.getPropertyValueLogDebug(PersistenceUnitProperties.TARGET_DATABASE, m, this.session);
        if (eclipselinkPlatform != null) {
            login.setPlatformClassName(eclipselinkPlatform, this.persistenceUnitInfo.getClassLoader());
        }
        // Check for EIS platform, need to use an EIS login.
        boolean isEIS = false;
        if (login.getDatasourcePlatform() instanceof EISPlatform) {
            isEIS = true;
            EISLogin newLogin = new EISLogin();
            newLogin.setDatasourcePlatform(login.getDatasourcePlatform());
            this.session.setDatasourceLogin(newLogin);
            if (this.session.isServerSession()) {
               for (ConnectionPool pool : ((ServerSession)this.session).getConnectionPools().values()) {
                   pool.setLogin(newLogin);
               }
            }
            login = newLogin;
        }

        // Check for EIS or custom (JDBC) Connector class.
        Object connectorValue = getConfigPropertyLogDebug(PersistenceUnitProperties.NOSQL_CONNECTION_SPEC, m, this.session);
        String connectorProperty = PersistenceUnitProperties.NOSQL_CONNECTION_SPEC;
        if (connectorValue == null) {
            connectorValue = getConfigPropertyLogDebug(PersistenceUnitProperties.JDBC_CONNECTOR, m, this.session);
            connectorProperty = PersistenceUnitProperties.JDBC_CONNECTOR;
        }
        if (connectorValue instanceof Connector) {
            login.setConnector((Connector)connectorValue);
        } else if (connectorValue instanceof String) {
            Class cls = null;
            // Try both class loaders.
            try {
                cls = findClassForProperty((String)connectorValue, connectorProperty, this.persistenceUnitInfo.getClassLoader());
            } catch (Throwable failed) {
                cls = findClassForProperty((String)connectorValue, connectorProperty, getClass().getClassLoader());
            }
            Connector connector = null;
            try {
                Constructor constructor = cls.getConstructor();
                connector = (Connector)constructor.newInstance();
            } catch (Exception exception) {
                throw EntityManagerSetupException.failedToInstantiateProperty((String)connectorValue, connectorProperty, exception);
            }
            if (connector != null) {
                login.setConnector(connector);
            }
        } else if (connectorValue != null) {
            // Assume JCA connection spec.
            ((EISConnectionSpec)login.getConnector()).setConnectionSpecObject(connectorValue);
        }

        // Check for EIS ConnectionFactory.
        Object factoryValue = getConfigPropertyLogDebug(PersistenceUnitProperties.NOSQL_CONNECTION_FACTORY, m, this.session);
        if (factoryValue instanceof String) {
            // JNDI name.
            ((EISConnectionSpec)login.getConnector()).setName((String)factoryValue);
        } else if (factoryValue != null) {
            ((EISConnectionSpec)login.getConnector()).setConnectionFactoryObject(factoryValue);
        }

        // Process EIS or JDBC connection properties.
        Map propertiesMap = PropertiesHandler.getPrefixValuesLogDebug(PersistenceUnitProperties.NOSQL_PROPERTY, m, session);
        if (propertiesMap.isEmpty()) {
            propertiesMap = PropertiesHandler.getPrefixValuesLogDebug(PersistenceUnitProperties.JDBC_PROPERTY, m, session);
        }
        for (Iterator iterator = propertiesMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry)iterator.next();
            String property = (String)entry.getKey();
            Object value = entry.getValue();
            login.setProperty(property, value);
        }

        // Note: This call does not checked the stored persistenceUnitInfo or extended properties because
        // the map passed into this method should represent the full set of properties we expect to process

        String user = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.JDBC_USER, m, this.session);
        String password = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.JDBC_PASSWORD, m, this.session);
        if(user != null) {
            login.setUserName(user);
        }
        if (password != null) {
            login.setPassword(this.securableObjectHolder.getSecurableObject().decryptPassword(password));
        }

        PersistenceUnitTransactionType transactionType = this.persistenceUnitInfo.getTransactionType();
        //bug 5867753: find and override the transaction type using properties
        String transTypeString = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.TRANSACTION_TYPE, m, this.session);
        if (transTypeString != null && transTypeString.length() > 0) {
            transactionType = PersistenceUnitTransactionType.valueOf(transTypeString);
        }
        //find the jta datasource
        javax.sql.DataSource jtaDatasource = getDatasourceFromProperties(m, PersistenceUnitProperties.JTA_DATASOURCE, this.persistenceUnitInfo.getJtaDataSource());

        //find the non jta datasource
        javax.sql.DataSource nonjtaDatasource = getDatasourceFromProperties(m, PersistenceUnitProperties.NON_JTA_DATASOURCE, this.persistenceUnitInfo.getNonJtaDataSource());

        if (isValidationOnly(m, false) && transactionType == PersistenceUnitTransactionType.JTA && jtaDatasource == null) {
            updateLoginDefaultConnector(login, m);
            return;
        }

        login.setUsesExternalTransactionController(transactionType == PersistenceUnitTransactionType.JTA);

        // Avoid processing data-source if EIS, as container may pass in a default one.
        if (isEIS) {
            return;
        }

        javax.sql.DataSource mainDatasource = null;
        javax.sql.DataSource readDatasource = null;
        if (login.shouldUseExternalTransactionController()) {
            // JtaDataSource is guaranteed to be non null - otherwise exception would've been thrown earlier
            mainDatasource = jtaDatasource;
            // only define readDatasource if there is jta mainDatasource
            readDatasource = nonjtaDatasource;
        } else {
            // JtaDataSource will be ignored because transactionType is RESOURCE_LOCAL
            if (jtaDatasource != null) {
                session.log(SessionLog.WARNING, SessionLog.TRANSACTION, "resource_local_persistence_init_info_ignores_jta_data_source", this.persistenceUnitInfo.getPersistenceUnitName());
            }
            if (nonjtaDatasource != null) {
                mainDatasource = nonjtaDatasource;
            } else {
                updateLoginDefaultConnector(login, m);
                return;
            }
        }

        // mainDatasource is guaranteed to be non null - TODO: No it is not, if they did not set one it is null, should raise error, not null-pointer.
        if (!(login.getConnector() instanceof JNDIConnector)) {
            JNDIConnector jndiConnector;
            if (mainDatasource instanceof DataSourceImpl) {
                //Bug5209363  Pass in the datasource name instead of the dummy datasource
                jndiConnector = new JNDIConnector(((DataSourceImpl)mainDatasource).getName());
            } else {
                jndiConnector = new JNDIConnector(mainDatasource);
            }
            login.setConnector(jndiConnector);
            String useInternalConnectionPool = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.CONNECTION_POOL_INTERNALLY_POOL_DATASOURCE, m, this.session);
            if (!"true".equalsIgnoreCase(useInternalConnectionPool)){
                login.setUsesExternalConnectionPooling(true);
            }
        }

        if (this.session.isServerSession()) {
            // set readLogin
            if (readDatasource != null) {
                DatasourceLogin readLogin = login.clone();
                readLogin.dontUseExternalTransactionController();
                JNDIConnector jndiConnector;
                if (readDatasource instanceof DataSourceImpl) {
                    //Bug5209363  Pass in the datasource name instead of the dummy datasource
                    jndiConnector = new JNDIConnector(((DataSourceImpl)readDatasource).getName());
                } else {
                    jndiConnector = new JNDIConnector(readDatasource);
                }
                readLogin.setConnector(jndiConnector);
                ((ServerSession)this.session).setReadConnectionPool(readLogin);
            }
        }

    }

    /**
     * This is used to return either the defaultDatasource or, if one exists, a datasource
     * defined under the property from the Map m.  This method will build a DataSourceImpl
     * object to hold the url if the property in Map m defines a string instead of a datasource.
     */
    protected javax.sql.DataSource getDatasourceFromProperties(Map m, String property, javax.sql.DataSource defaultDataSource){
        Object datasource = getConfigPropertyLogDebug(property, m, session);
        if ( datasource == null ){
            return defaultDataSource;
        }
        if ( datasource instanceof String){
            if(((String)datasource).length() > 0) {
                // Create a dummy DataSource that will throw an exception on access
                return new DataSourceImpl((String)datasource, null, null, null);
            } else {
                // allow an empty string data source property passed to createEMF to cancel data source specified in persistence.xml
                return null;
            }
        }
        if ( !(datasource instanceof javax.sql.DataSource) ){
            //A warning should be enough.  Though an error might be better, the properties passed in could contain anything
            session.log(SessionLog.WARNING, SessionLog.PROPERTIES, "invalid_datasource_property_value", property, datasource);
            return defaultDataSource;
        }
        return (javax.sql.DataSource)datasource;
    }

    /**
     * In cases where there is no data source, we will use properties to configure the login for
     * our session.  This method gets those properties and sets them on the login.
     */
    protected void updateLoginDefaultConnector(DatasourceLogin login, Map m){
        //Login info might be already set with sessions.xml and could be overridden by session customizer after this
        //If login has default connector then JDBC properties update(override) the login info
        if ((login.getConnector() instanceof DefaultConnector)) {
            DatabaseLogin dbLogin = (DatabaseLogin)login;
            // Note: This call does not checked the stored persistenceUnitInfo or extended properties because
            // the map passed into this method should represent the full set of properties we expect to process
            String jdbcDriver = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.JDBC_DRIVER, m, session);
            String connectionString = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.JDBC_URL, m, session);
            if(connectionString != null) {
                dbLogin.setConnectionString(connectionString);
            }
            if(jdbcDriver != null) {
                dbLogin.setDriverClassName(jdbcDriver);
            }
        }
    }

    /**
     * Configure the internal connection pooling parameters.
     * By default if nothing is configured a default shared (exclusive) read/write pool is used with 32 min/max connections and 1 initial.
     */
    @SuppressWarnings("deprecation")
    protected void updatePools(ServerSession serverSession, Map m) {
        String value = null;
        String property = null;
        try {
            // Configure default/write connection pool.
            // Sizes are irrelevant for external connection pool
            if (!serverSession.getDefaultConnectionPool().getLogin().shouldUseExternalConnectionPooling()) {
                // CONNECTION and WRITE_CONNECTION properties both configure the default pool (mean the same thing, but WRITE normally used with READ).
                property = PersistenceUnitProperties.JDBC_CONNECTIONS_MIN;
                value = getConfigPropertyAsStringLogDebug(property, m, serverSession);
                if (value != null) {
                    serverSession.getDefaultConnectionPool().setMinNumberOfConnections(Integer.parseInt(value));
                }
                property = PersistenceUnitProperties.JDBC_CONNECTIONS_MAX;
                value = getConfigPropertyAsStringLogDebug(property, m, serverSession);
                if (value != null) {
                    serverSession.getDefaultConnectionPool().setMaxNumberOfConnections(Integer.parseInt(value));
                }
                property = PersistenceUnitProperties.JDBC_CONNECTIONS_INITIAL;
                value = getConfigPropertyAsStringLogDebug(property, m, serverSession);
                if (value != null) {
                    serverSession.getDefaultConnectionPool().setInitialNumberOfConnections(Integer.parseInt(value));
                }
                property = PersistenceUnitProperties.JDBC_WRITE_CONNECTIONS_MIN;
                value = getConfigPropertyAsStringLogDebug(property, m, serverSession);
                if (value != null) {
                    serverSession.getDefaultConnectionPool().setMinNumberOfConnections(Integer.parseInt(value));
                }
                property = PersistenceUnitProperties.JDBC_WRITE_CONNECTIONS_MAX;
                value = getConfigPropertyAsStringLogDebug(property, m, serverSession);
                if (value != null) {
                    serverSession.getDefaultConnectionPool().setMaxNumberOfConnections(Integer.parseInt(value));
                }
                property = PersistenceUnitProperties.JDBC_WRITE_CONNECTIONS_INITIAL;
                value = getConfigPropertyAsStringLogDebug(property, m, serverSession);
                if (value != null) {
                    serverSession.getDefaultConnectionPool().setInitialNumberOfConnections(Integer.parseInt(value));
                }
            }

            // Configure read connection pool if set.
            // Sizes and shared option are irrelevant for external connection pool
            if (!serverSession.getReadConnectionPool().getLogin().shouldUseExternalConnectionPooling()) {
                String shared = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.JDBC_READ_CONNECTIONS_SHARED, m, serverSession);
                boolean isShared = false;
                if (shared != null) {
                    isShared = Boolean.parseBoolean(shared);
                }
                ConnectionPool pool = null;
                if (isShared) {
                    pool = new ReadConnectionPool("read", serverSession.getReadConnectionPool().getLogin(), serverSession);
                } else {
                    pool = new ConnectionPool("read", serverSession.getReadConnectionPool().getLogin(), serverSession);
                }
                String min = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.JDBC_READ_CONNECTIONS_MIN, m, serverSession);
                if (min != null) {
                    value = min;
                    property = PersistenceUnitProperties.JDBC_READ_CONNECTIONS_MIN;
                    pool.setMinNumberOfConnections(Integer.parseInt(min));
                }
                String max = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.JDBC_READ_CONNECTIONS_MAX, m, serverSession);
                if (max != null) {
                    value = max;
                    property = PersistenceUnitProperties.JDBC_READ_CONNECTIONS_MAX;
                    pool.setMaxNumberOfConnections(Integer.parseInt(max));
                }
                String initial = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.JDBC_READ_CONNECTIONS_INITIAL, m, serverSession);
                if (initial != null) {
                    value = initial;
                    property = PersistenceUnitProperties.JDBC_READ_CONNECTIONS_INITIAL;
                    pool.setInitialNumberOfConnections(Integer.parseInt(initial));
                }
                // Only set the read pool if they configured it, otherwise use default shared read/write.
                if (isShared || (min != null) || (max != null) || (initial != null)) {
                    serverSession.setReadConnectionPool(pool);
                }
                String wait = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.JDBC_CONNECTIONS_WAIT, m, serverSession);
                if (wait != null) {
                    value = wait;
                    property = PersistenceUnitProperties.JDBC_CONNECTIONS_WAIT;
                    serverSession.getDefaultConnectionPool().setWaitTimeout(Integer.parseInt(wait));
                    pool.setWaitTimeout(Integer.parseInt(wait));
                }
            }

            // Configure sequence connection pool if set.
            String sequence = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.JDBC_SEQUENCE_CONNECTION_POOL, m, serverSession);
            if (sequence != null) {
                serverSession.getSequencingControl().setShouldUseSeparateConnection(Boolean.parseBoolean(sequence));
            }
            String sequenceDataSource = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.JDBC_SEQUENCE_CONNECTION_POOL_DATASOURCE, m, serverSession);
            if (sequenceDataSource != null) {
                DatasourceLogin login = this.session.getLogin().clone();
                login.dontUseExternalTransactionController();
                JNDIConnector jndiConnector = new JNDIConnector(sequenceDataSource);
                login.setConnector(jndiConnector);
                serverSession.getSequencingControl().setLogin(login);
            }
            // Sizes and shared option are irrelevant for external connection pool
            if (!serverSession.getReadConnectionPool().getLogin().shouldUseExternalConnectionPooling()) {
                value = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.JDBC_SEQUENCE_CONNECTION_POOL_MIN, m, serverSession);
                if (value != null) {
                    property = PersistenceUnitProperties.JDBC_SEQUENCE_CONNECTION_POOL_MIN;
                    serverSession.getSequencingControl().setMinPoolSize(Integer.parseInt(value));
                }
                value = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.JDBC_SEQUENCE_CONNECTION_POOL_MAX, m, serverSession);
                if (value != null) {
                    property = PersistenceUnitProperties.JDBC_SEQUENCE_CONNECTION_POOL_MAX;
                    serverSession.getSequencingControl().setMaxPoolSize(Integer.parseInt(value));
                }
                value = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.JDBC_SEQUENCE_CONNECTION_POOL_INITIAL, m, serverSession);
                if (value != null) {
                    property = PersistenceUnitProperties.JDBC_SEQUENCE_CONNECTION_POOL_INITIAL;
                    serverSession.getSequencingControl().setInitialPoolSize(Integer.parseInt(value));
                }
            }
        } catch (NumberFormatException exception) {
            serverSession.handleException(ValidationException.invalidValueForProperty(value, property, exception));
        }
    }

    /**
     * Normally when a property is missing nothing should be applied to the session.
     * However there are several session attributes that defaulted in EJB3 to the values
     * different from EclipseLink defaults.
     * This function applies defaults for such properties and registers the session.
     * All other session-related properties are applied in updateServerSession.
     * Note that updateSession may be called several times on the same session
     * (before login), but initSession is called just once - before the first call
     * to updateSession.
     */
    protected void initSession() {
        assignCMP3Policy();

        if(!isCompositeMember()) {
            // Register session that has been created earlier.
            addSessionToGlobalSessionManager();
        }
    }

    /**
     * Make any changes to our ServerSession that can be made after it is created.
     */
    protected void updateSession(Map m, ClassLoader loader) {
        if (session == null || (session.isDatabaseSession() && ((DatabaseSessionImpl)session).isLoggedIn())) {
            return;
        }

        // In deploy ServerPlatform could've changed which will affect the loggers.
        boolean serverPlatformChanged = updateServerPlatform(m, loader);
        updateJPQLParser(m);

        if (!session.hasBroker()) {
            updateLoggers(m, serverPlatformChanged, loader);
            updateProfiler(m,loader);
        }

        // log the server platform being used by the session if it has been changed
        if (serverPlatformChanged && session.getSessionLog().shouldLog(SessionLog.FINE)) {
            session.getSessionLog().log(SessionLog.FINE, SessionLog.SERVER,
                    "configured_server_platform", session.getServerPlatform().getClass().getName()); // NOI18N
        }

        if(session.isBroker()) {
            PersistenceUnitTransactionType transactionType = persistenceUnitInfo.getTransactionType();
            //bug 5867753: find and override the transaction type using properties
            String transTypeString = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.TRANSACTION_TYPE, m, session);
            if (transTypeString != null) {
                transactionType = PersistenceUnitTransactionType.valueOf(transTypeString);
            }
            ((DatasourceLogin)session.getDatasourceLogin()).setUsesExternalTransactionController(transactionType == PersistenceUnitTransactionType.JTA);
        } else {
            String shouldBindString = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.JDBC_BIND_PARAMETERS, m, session);
            if (shouldBindString != null) {
                session.getPlatform().setShouldBindAllParameters(Boolean.parseBoolean(shouldBindString));
            }
            updateLogins(m);
        }
        if (!session.getDatasourceLogin().shouldUseExternalTransactionController()) {
            session.getServerPlatform().disableJTA();
        }

        if(session.isServerSession()) {
            updatePools((ServerSession)session, m);
            updateConnectionSettings((ServerSession)session, m);
            if (!isSessionLoadedFromSessionsXML) {
                updateDescriptorCacheSettings(m, loader);
            }
            updateConnectionPolicy((ServerSession)session, m);
        }

        if(session.isBroker()) {
            if (this.compositeMemberEmSetupImpls != null) {
                // composite
                Map compositeMemberMapOfProperties = (Map)getConfigProperty(PersistenceUnitProperties.COMPOSITE_UNIT_PROPERTIES, m);
                for(EntityManagerSetupImpl compositeMemberEmSetupImpl : this.compositeMemberEmSetupImpls) {
                    // the properties guaranteed to be non-null after updateCompositeMemberProperties call
                    String compositeMemberPuName = compositeMemberEmSetupImpl.getPersistenceUnitInfo().getPersistenceUnitName();
                    Map compositeMemberProperties = (Map)compositeMemberMapOfProperties.get(compositeMemberPuName);
                    // debug output added to make it easier to navigate the log because the method is called outside of composite member deploy
                    compositeMemberEmSetupImpl.session.log(SessionLog.FINEST, SessionLog.PROPERTIES, "composite_member_begin_call", new Object[]{"updateSession", compositeMemberPuName, state});
                    compositeMemberEmSetupImpl.updateSession(compositeMemberProperties, loader);
                    compositeMemberEmSetupImpl.session.log(SessionLog.FINEST, SessionLog.PROPERTIES, "composite_member_end_call", new Object[]{"updateSession", compositeMemberPuName, state});
                }
            }
            setSessionEventListener(m, loader);
            setExceptionHandler(m, loader);

            updateAllowZeroIdSetting(m);
            updateCacheCoordination(m, loader);
            processSessionCustomizer(m, loader);
        } else {
            setSessionEventListener(m, loader);
            setExceptionHandler(m, loader);

            updateBatchWritingSetting(m, loader);

            updateNativeSQLSetting(m);
            updateSequencing(m);
            updateSequencingStart(m);
            updateAllowNativeSQLQueriesSetting(m);
            updateSQLCastSetting(m);
            updateUppercaseSetting(m);
            updateCacheStatementSettings(m);
            updateTemporalMutableSetting(m);
            updateTableCreationSettings(m);
            updateIndexForeignKeys(m);
            if (!session.hasBroker()) {
                updateAllowZeroIdSetting(m);
            }
            updateIdValidation(m);
            updatePessimisticLockTimeout(m);
            updateQueryTimeout(m);
            updateQueryTimeoutUnit(m);
            updateLockingTimestampDefault(m);
            if (!session.hasBroker()) {
                updateCacheCoordination(m, loader);
            }
            updatePartitioning(m, loader);
            updateDatabaseEventListener(m, loader);
            updateSerializer(m, loader);
            updateShouldOptimizeResultSetAccess(m);
            updateTolerateInvalidJPQL(m);
            updateTenancy(m, loader);

            // Customizers should be processed last
            processDescriptorCustomizers(m, loader);
            processSessionCustomizer(m, loader);

            setDescriptorNamedQueries(m);
        }
    }

    /**
     * This sets the isInContainerMode flag.
     * "true" indicates container case, "false" - SE.
     */
    public void setIsInContainerMode(boolean isInContainerMode) {
        this.isInContainerMode = isInContainerMode;
    }

    /**
     * Used to indicate that an EntityManagerFactoryImpl based on this
     * EntityManagerSetupImpl has been refreshed.  This means this EntityManagerSetupImpl
     * will no longer be associated with new EntityManagerFactories
     */
    public void setIsMetadataExpired(boolean hasExpiredMetadata) {
        this.isMetadataExpired = hasExpiredMetadata;
    }

    protected void processSessionCustomizer(Map m, ClassLoader loader) {
        SessionCustomizer sessionCustomizer;
        Object customizer = getConfigPropertyLogDebug(PersistenceUnitProperties.SESSION_CUSTOMIZER, m, session);
        if (customizer == null) {
            return;
        }
        if (customizer instanceof String) {
            Class sessionCustomizerClass = findClassForProperty((String) customizer, PersistenceUnitProperties.SESSION_CUSTOMIZER, loader);
            try {
                sessionCustomizer = (SessionCustomizer) sessionCustomizerClass.newInstance();
            } catch (Exception ex) {
                throw EntityManagerSetupException.failedWhileProcessingProperty(PersistenceUnitProperties.SESSION_CUSTOMIZER, (String) customizer, ex);
            }
        } else {
            sessionCustomizer = (SessionCustomizer) customizer;
        }
        try {
            sessionCustomizer.customize(session);
        } catch (Exception ex) {
            throw EntityManagerSetupException.failedWhileProcessingProperty(PersistenceUnitProperties.SESSION_CUSTOMIZER, customizer.toString(), ex);
        }
    }

    protected void initOrUpdateLogging(Map m, SessionLog log) {
        String logLevelString = PropertiesHandler.getPropertyValueLogDebug(PersistenceUnitProperties.LOGGING_LEVEL, m, session);
        if (logLevelString != null) {
            log.setLevel(AbstractSessionLog.translateStringToLoggingLevel(logLevelString));
        }
        // category-specific logging level
        Map categoryLogLevelMap = PropertiesHandler.getPrefixValuesLogDebug(PersistenceUnitProperties.CATEGORY_LOGGING_LEVEL_, m, session);
        if(!categoryLogLevelMap.isEmpty()) {
            Iterator it = categoryLogLevelMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                String category = (String)entry.getKey();
                String value = (String)entry.getValue();
                log.setLevel(AbstractSessionLog.translateStringToLoggingLevel(value), category);
            }
        }

        String tsString = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.LOGGING_TIMESTAMP, m, session);
        if (tsString != null) {
            log.setShouldPrintDate(Boolean.parseBoolean(tsString));
        }
        String threadString = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.LOGGING_THREAD, m, session);
        if (threadString != null) {
            log.setShouldPrintThread(Boolean.parseBoolean(threadString));
        }
        String sessionString = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.LOGGING_SESSION, m, session);
        if (sessionString != null) {
            log.setShouldPrintSession(Boolean.parseBoolean(sessionString));
        }
        String connectionString = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.LOGGING_CONNECTION, m, session);
        if (connectionString != null) {
            log.setShouldPrintConnection(Boolean.parseBoolean(connectionString));
        }
        String exString = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.LOGGING_EXCEPTIONS, m, session);
        if (exString != null) {
            log.setShouldLogExceptionStackTrace(Boolean.parseBoolean(exString));
        }
        String shouldDisplayData = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.LOGGING_PARAMETERS, m, session);
        if (shouldDisplayData != null) {
            log.setShouldDisplayData(Boolean.parseBoolean(shouldDisplayData));
        }
    }

    protected void processDescriptorCustomizers(Map m, ClassLoader loader) {
        Map customizerMap = PropertiesHandler.getPrefixValuesLogDebug(PersistenceUnitProperties.DESCRIPTOR_CUSTOMIZER_, m, session);
        if (customizerMap.isEmpty()) {
            return;
        }

        Iterator it = customizerMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            String name = (String)entry.getKey();
            String customizerClassName = (String)entry.getValue();

            ClassDescriptor descriptor = session.getDescriptorForAlias(name);
            if (descriptor == null) {
                try {
                    Class javaClass = findClass(name, loader);
                    descriptor = session.getDescriptor(javaClass);
                } catch (Exception ex) {
                    throw EntityManagerSetupException.failedWhileProcessingProperty(PersistenceUnitProperties.DESCRIPTOR_CUSTOMIZER_ + name, customizerClassName, ex);
                }
            }
            if (descriptor != null) {
                Class customizerClass = findClassForProperty(customizerClassName, PersistenceUnitProperties.DESCRIPTOR_CUSTOMIZER_ + name, loader);
                try {
                    DescriptorCustomizer customizer = (DescriptorCustomizer)customizerClass.newInstance();
                    customizer.customize(descriptor);
                } catch (Exception ex) {
                    throw EntityManagerSetupException.failedWhileProcessingProperty(PersistenceUnitProperties.DESCRIPTOR_CUSTOMIZER_ + name, customizerClassName, ex);
                }
            } else {
                // TODO throw a better error, missing descriptor for property.
                throw EntityManagerSetupException.failedWhileProcessingProperty(PersistenceUnitProperties.DESCRIPTOR_CUSTOMIZER_ + name, customizerClassName, null);
            }
        }
    }

    public boolean isInitial() {
        return state == STATE_INITIAL;
    }

    /**
     * Used to indicate that an EntityManagerFactoryImpl based on this
     * EntityManagerSetupImpl has been refreshed.  This means this EntityManagerSetupImpl
     * will no longer be associated with new EntityManagerFactories
     */
    public boolean isMetadataExpired() {
        return isMetadataExpired;
    }

    public boolean isPredeployed() {
        return state == STATE_PREDEPLOYED;
    }

    public boolean isDeployed() {
        return state == STATE_DEPLOYED;
    }

    public boolean isHalfDeployed() {
        return state == STATE_HALF_DEPLOYED;
    }

    public boolean isUndeployed() {
        return state == STATE_UNDEPLOYED;
    }

    public boolean isPredeployFailed() {
        return state == STATE_PREDEPLOY_FAILED;
    }

    public boolean isDeployFailed() {
        return state == STATE_DEPLOY_FAILED;
    }

    public boolean isHalfPredeployedCompositeMember() {
        return state == STATE_HALF_PREDEPLOYED_COMPOSITE_MEMBER;
    }

    public String getPersistenceUnitUniqueName() {
        return this.persistenceUnitUniqueName;
    }

    public int getFactoryCount() {
        return factoryCount;
    }

    public String getSessionName() {
        return this.sessionName;
    }

    public boolean shouldRedeploy() {
        return state == STATE_UNDEPLOYED || state == STATE_PREDEPLOY_FAILED;
    }

    /**
     * Return if MetadataSource refresh commands should be sent when refresh is called
     * Checks the PersistenceUnitProperties.METADATA_SOURCE_RCM_COMMAND property and defaults to true.
     */
    public boolean shouldSendMetadataRefreshCommand(Map m) {
        String sendCommand = getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.METADATA_SOURCE_RCM_COMMAND, m, this.session);
        if (sendCommand != null) {
            return Boolean.parseBoolean(sendCommand);
        } else {
            return true;
        }
    }

    /**
     * Undeploy may be called several times, but only the call that decreases
     * factoryCount to 0 disconnects the session and removes it from the session manager.
     * This method and predeploy - the only methods altering factoryCount - should be synchronized.
     * After undeploy call that turns factoryCount to 0:
     *   session==null;
     *   PREDEPLOYED, DEPLOYED and DEPLOYED_FAILED states change to UNDEPLOYED state.
     */
    public synchronized void undeploy() {
        if (state == STATE_INITIAL || state == STATE_PREDEPLOY_FAILED || state == STATE_UNDEPLOYED) {
            // must already have factoryCount==0 and session==null
            return;
        }
        // state is PREDEPLOYED, DEPLOYED or DEPLOY_FAILED
        session.log(SessionLog.FINEST, SessionLog.JPA, "undeploy_begin", new Object[]{getPersistenceUnitInfo().getPersistenceUnitName(), session.getName(), state, factoryCount});
        try {
            factoryCount--;
            if(factoryCount > 0) {
                return;
            }
            synchronized (EntityManagerFactoryProvider.emSetupImpls) {
                state = STATE_UNDEPLOYED;
                removeSessionFromGlobalSessionManager();
                // remove undeployed emSetupImpl from the map
                EntityManagerSetupImpl emSetupImpl = EntityManagerFactoryProvider.emSetupImpls.get(sessionName);
                if ((emSetupImpl != null) && (emSetupImpl.equals(this))) {
                    EntityManagerFactoryProvider.emSetupImpls.remove(sessionName);
                }
            }
        } finally {
            session.log(SessionLog.FINEST, SessionLog.JPA, "undeploy_end", new Object[]{getPersistenceUnitInfo().getPersistenceUnitName(), session.getName(), state, factoryCount});
            if(state == STATE_UNDEPLOYED) {
                session = null;
            }
        }
    }

    /**
     * INTERNAL:
     * By default we require a connection to the database. However, when
     * generating schema to scripts only, this is not required.
     */
    public void setRequiresConnection(boolean requiresConnection) {
        this.requiresConnection = requiresConnection;
    }

    /**
     * Allow customized session event listener to be added into session.
     * The method needs to be called in deploy stage.
     */
    protected void setSessionEventListener(Map m, ClassLoader loader){
        //Set event listener if it has been specified.
        String sessionEventListenerClassName = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.SESSION_EVENT_LISTENER_CLASS, m, session);
        if(sessionEventListenerClassName!=null){
            Class sessionEventListenerClass = findClassForProperty(sessionEventListenerClassName,PersistenceUnitProperties.SESSION_EVENT_LISTENER_CLASS, loader);
            try {
                SessionEventListener sessionEventListener = (SessionEventListener)buildObjectForClass(sessionEventListenerClass, SessionEventListener.class);
                if(sessionEventListener!=null){
                    session.getEventManager().addListener(sessionEventListener);
                } else {
                    session.handleException(ValidationException.invalidSessionEventListenerClass(sessionEventListenerClassName));
                }
            } catch (IllegalAccessException e) {
                session.handleException(ValidationException.cannotInstantiateSessionEventListenerClass(sessionEventListenerClassName,e));
            } catch (PrivilegedActionException e) {
                session.handleException(ValidationException.cannotInstantiateSessionEventListenerClass(sessionEventListenerClassName,e));
            } catch (InstantiationException e) {
                session.handleException(ValidationException.cannotInstantiateSessionEventListenerClass(sessionEventListenerClassName,e));
            }
        }
    }

    /**
     * Allow customized exception handler to be added into session.
     * The method needs to be called in deploy and pre-deploy stage.
     */
    protected void setExceptionHandler(Map m, ClassLoader loader){
        //Set exception handler if it was specified.
        String exceptionHandlerClassName = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.EXCEPTION_HANDLER_CLASS, m, session);
        if(exceptionHandlerClassName!=null){
            Class exceptionHandlerClass = findClassForProperty(exceptionHandlerClassName,PersistenceUnitProperties.EXCEPTION_HANDLER_CLASS, loader);
            try {
                ExceptionHandler exceptionHandler = (ExceptionHandler)buildObjectForClass(exceptionHandlerClass, ExceptionHandler.class);
                if (exceptionHandler!=null){
                    session.setExceptionHandler(exceptionHandler);
                } else {
                    session.handleException(ValidationException.invalidExceptionHandlerClass(exceptionHandlerClassName));
                }
            } catch (IllegalAccessException e) {
                session.handleException(ValidationException.cannotInstantiateExceptionHandlerClass(exceptionHandlerClassName,e));
            } catch (PrivilegedActionException e) {
                session.handleException(ValidationException.cannotInstantiateExceptionHandlerClass(exceptionHandlerClassName,e));
            } catch (InstantiationException e) {
                session.handleException(ValidationException.cannotInstantiateExceptionHandlerClass(exceptionHandlerClassName,e));
            }
        }
    }

    /**
     * Update batch writing setting.
     * The method needs to be called in deploy stage.
     */
    protected void updateBatchWritingSetting(Map persistenceProperties, ClassLoader loader) {
        String batchWritingSettingString = PropertiesHandler.getPropertyValueLogDebug(PersistenceUnitProperties.BATCH_WRITING, persistenceProperties, this.session);
        if (batchWritingSettingString != null) {
             this.session.getPlatform().setUsesBatchWriting(batchWritingSettingString != BatchWriting.None);
             if (batchWritingSettingString == BatchWriting.JDBC) {
                 this.session.getPlatform().setUsesJDBCBatchWriting(true);
                 this.session.getPlatform().setUsesNativeBatchWriting(false);
             } else if (batchWritingSettingString == BatchWriting.Buffered) {
                 this.session.getPlatform().setUsesJDBCBatchWriting(false);
                 this.session.getPlatform().setUsesNativeBatchWriting(false);
             } else if (batchWritingSettingString == BatchWriting.OracleJDBC) {
                 this.session.getPlatform().setUsesNativeBatchWriting(true);
                 this.session.getPlatform().setUsesJDBCBatchWriting(true);
             } else if (batchWritingSettingString == BatchWriting.None) {
                 // Nothing required.
             } else {
                 if (batchWritingSettingString.equalsIgnoreCase("ExaLogic")) {
                     batchWritingSettingString = "oracle.toplink.exalogic.batch.DynamicParameterizedBatchWritingMechanism";
                 }
                 Class cls = findClassForProperty(batchWritingSettingString, PersistenceUnitProperties.BATCH_WRITING, loader);
                 BatchWritingMechanism mechanism = null;
                 try {
                     Constructor constructor = cls.getConstructor();
                     mechanism = (BatchWritingMechanism)constructor.newInstance();
                 } catch (Exception exception) {
                     if (batchWritingSettingString.indexOf('.') == -1) {
                         throw new IllegalArgumentException(ExceptionLocalization.buildMessage("ejb30-illegal-property-value", new Object[]{PersistenceUnitProperties.BATCH_WRITING, batchWritingSettingString}));
                     } else {
                         throw EntityManagerSetupException.failedToInstantiateProperty(batchWritingSettingString, PersistenceUnitProperties.BATCH_WRITING, exception);
                     }
                 }
                 this.session.getPlatform().setBatchWritingMechanism(mechanism);
             }
        }
        // Set batch size.
        String sizeString = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.BATCH_WRITING_SIZE, persistenceProperties, this.session);
        if (sizeString != null) {
            try {
                this.session.getPlatform().setMaxBatchWritingSize(Integer.parseInt(sizeString));
            } catch (NumberFormatException invalid) {
                session.handleException(ValidationException.invalidValueForProperty(sizeString, PersistenceUnitProperties.BATCH_WRITING_SIZE, invalid));
            }
        }
    }

    /**
     * Load the Metadata Repository for Extensibility
     */
    protected void updateMetadataRepository(Map m, ClassLoader loader){
        Object metadataSource = EntityManagerFactoryProvider.getConfigPropertyLogDebug(PersistenceUnitProperties.METADATA_SOURCE, m, session);
        if (metadataSource != null && metadataSource instanceof MetadataSource){
            processor.setMetadataSource((MetadataSource)metadataSource);
        } else {
            if (metadataSource!=null) {
                String repository = (String)metadataSource;
                if (repository.equalsIgnoreCase("XML")) {
                    processor.setMetadataSource(new XMLMetadataSource());
                } else {
                    Class transportClass = findClassForProperty(repository, PersistenceUnitProperties.METADATA_SOURCE, loader);
                    try {
                        processor.setMetadataSource((MetadataSource)transportClass.newInstance());
                    } catch (Exception invalid) {
                        session.handleException(EntityManagerSetupException.failedToInstantiateProperty(repository, PersistenceUnitProperties.METADATA_SOURCE,invalid));
                    }
                }
            }
        }
    }

    /**
     * Check for a tuning property and run the tuner preDeploy.
     */
    protected void updateTunerPreDeploy(Map m, ClassLoader loader) {
        String tuning = (String)EntityManagerFactoryProvider.getConfigPropertyLogDebug(PersistenceUnitProperties.TUNING, m, this.session);
        if (tuning != null) {
            SessionTuner tuner = null;
            if (tuning.equalsIgnoreCase("Safe")) {
                tuner = new SafeModeTuner();
            } else if (tuning.equalsIgnoreCase("Standard")) {
                tuner = new StandardTuner();
            } else {
                if (tuning.equalsIgnoreCase("ExaLogic")) {
                    tuning = "oracle.toplink.exalogic.tuning.ExaLogicTuner";
                }
                Class tunerClass = findClassForProperty(tuning, PersistenceUnitProperties.TUNING, loader);
                try {
                    tuner = (SessionTuner)tunerClass.newInstance();
                } catch (Exception invalid) {
                    this.session.handleException(EntityManagerSetupException.failedToInstantiateProperty(tuning, PersistenceUnitProperties.TUNING, invalid));
                }
            }
            getDatabaseSession().setTuner(tuner);
            if (tuner != null) {
                tuner.tunePreDeploy(m);
            }
        }
    }

    /**
     * Check for a tuning property and run the tuner deploy.
     */
    protected void updateTunerDeploy(Map m, ClassLoader loader) {
        if (getDatabaseSession().getTuner() != null) {
            getDatabaseSession().getTuner().tuneDeploy(getDatabaseSession());
        }
    }

    /**
     * Check for a tuning property and run the tuner deploy.
     */
    protected void updateTunerPostDeploy(Map m, ClassLoader loader) {
        if (getDatabaseSession().getTuner() != null) {
            getDatabaseSession().getTuner().tunePostDeploy(getDatabaseSession());
        }
    }

    /**
     * Allow the deployment metadata to be freed post-deploy to conserve memory.
     */
    protected void updateFreeMemory(Map m) {
        String freeMemory = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.FREE_METADATA, m, session);
        if (freeMemory != null) {
           if (freeMemory.equalsIgnoreCase("true")) {
               XMLEntityMappingsReader.clear();
           } else if (freeMemory.equalsIgnoreCase("false")) {
               // default.
           } else {
               session.handleException(ValidationException.invalidBooleanValueForProperty(freeMemory, PersistenceUnitProperties.FREE_METADATA));
           }
        }
    }

    /**
     * Enable or disable the capability of Native SQL function.
     * The method needs to be called in deploy stage.
     */
    protected void updateNativeSQLSetting(Map m){
        //Set Native SQL flag if it was specified.
        String nativeSQLString = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.NATIVE_SQL, m, session);
        if(nativeSQLString!=null){
           if(nativeSQLString.equalsIgnoreCase("true") ){
                 session.getProject().getLogin().useNativeSQL();
           }else if (nativeSQLString.equalsIgnoreCase("false")){
                 session.getProject().getLogin().dontUseNativeSQL();
           }else{
                 session.handleException(ValidationException.invalidBooleanValueForSettingNativeSQL(nativeSQLString));
           }
        }
    }

    /**
     * Configure sequencing settings.
     */
    protected void updateSequencing(Map m){
        String useTable = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.SEQUENCING_SEQUENCE_DEFAULT, m, session);
        if (useTable != null) {
           if (useTable.equalsIgnoreCase("true")) {
               this.session.getPlatform().setDefaultNativeSequenceToTable(true);
           } else if (useTable.equalsIgnoreCase("false")) {
               this.session.getPlatform().setDefaultNativeSequenceToTable(false);
           } else {
               this.session.handleException(ValidationException.invalidBooleanValueForProperty(useTable, PersistenceUnitProperties.SEQUENCING_SEQUENCE_DEFAULT));
           }
        }
    }

    protected void updateSequencingStart(Map m) {
        String local = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.SEQUENCING_START_AT_NEXTVAL, m, session);
        try {
            if (local != null) {
                this.session.getPlatform().setDefaultSeqenceAtNextValue(Boolean.parseBoolean(local));
            }
        } catch (NumberFormatException exception) {
            this.session.handleException(ValidationException.invalidValueForProperty(local, PersistenceUnitProperties.USE_LOCAL_TIMESTAMP, exception));
        }
    }

    /**
     * Load the projectCacheAccessor for JPA project caching
     */
    protected void updateProjectCache(Map m, ClassLoader loader){
        Object accessor = EntityManagerFactoryProvider.getConfigPropertyLogDebug(PersistenceUnitProperties.PROJECT_CACHE, m, session);
        if (accessor != null ) {
            if (accessor instanceof ProjectCache) {
                projectCacheAccessor = (ProjectCache)accessor;
            } else {
                String accessorType = (String)accessor;
                if (accessorType.equalsIgnoreCase("java-serialization")) {
                    projectCacheAccessor = new FileBasedProjectCache();
                } else {
                    Class transportClass = findClassForProperty(accessorType, PersistenceUnitProperties.PROJECT_CACHE, loader);
                    try {
                        projectCacheAccessor = (ProjectCache)transportClass.newInstance();
                    } catch (Exception invalid) {
                        session.handleException(EntityManagerSetupException.failedToInstantiateProperty(accessorType, PersistenceUnitProperties.METADATA_SOURCE,invalid));
                    }
                }
            }
        }
    }

    /**
     * Enable or disable the capability of Native SQL function.
     * The method needs to be called in deploy stage.
     */
    protected void updateJPQLParser(Map m) {
        // Set JPQL parser if it was specified.
        String parser = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.JPQL_PARSER, m, this.session);
        if (parser != null) {
            if (parser.equalsIgnoreCase(ParserType.Hermes)) {
                parser = "org.eclipse.persistence.internal.jpa.jpql.HermesParser";
            } else if (parser.equalsIgnoreCase(ParserType.ANTLR)) {
                parser = "org.eclipse.persistence.queries.ANTLRQueryBuilder";
            }
            this.session.setProperty(PersistenceUnitProperties.JPQL_PARSER, parser);
        }
        // Set JPQL parser validation mode if it was specified.
        String validation = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.JPQL_VALIDATION, m, this.session);
        if (validation != null) {
            this.session.setProperty(PersistenceUnitProperties.JPQL_VALIDATION, validation);
        }
    }

    /**
     * Enable or disable the capability of Native SQL function.
     * The method needs to be called in deploy stage.
     */
    protected void updateAllowNativeSQLQueriesSetting(Map m){
        // Set allow native SQL queries flag if it was specified.
        String allowNativeSQLQueriesString = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.ALLOW_NATIVE_SQL_QUERIES, m, session);

        if (allowNativeSQLQueriesString != null) {
           if (allowNativeSQLQueriesString.equalsIgnoreCase("true")) {
               session.getProject().setAllowNativeSQLQueries(true);
           } else if (allowNativeSQLQueriesString.equalsIgnoreCase("false")) {
               session.getProject().setAllowNativeSQLQueries(false);
           } else {
               session.handleException(ValidationException.invalidBooleanValueForSettingAllowNativeSQLQueries(allowNativeSQLQueriesString));
           }
        }
    }

    /**
     * Enable or disable SQL casting.
     */
    protected void updateSQLCastSetting(Map m) {
        //Set Native SQL flag if it was specified.
        String sqlCastString = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.SQL_CAST, m, session);
        if (sqlCastString != null) {
           if (sqlCastString.equalsIgnoreCase("true")) {
                 session.getProject().getLogin().getPlatform().setIsCastRequired(true);
           } else if (sqlCastString.equalsIgnoreCase("false")) {
               session.getProject().getLogin().getPlatform().setIsCastRequired(false);
           } else {
                 session.handleException(ValidationException.invalidBooleanValueForProperty(sqlCastString, PersistenceUnitProperties.SQL_CAST));
           }
        }
    }

    /**
     * Enable or disable forcing field names to uppercase.
     * The method needs to be called in deploy stage.
     */
    protected void updateUppercaseSetting(Map m){
        //Set Native SQL flag if it was specified.
        String uppercaseString = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.NATIVE_QUERY_UPPERCASE_COLUMNS, m, session);
        if (uppercaseString != null) {
           if (uppercaseString.equalsIgnoreCase("true") ){
               this.session.getProject().getLogin().setShouldForceFieldNamesToUpperCase(true);
           } else if (uppercaseString.equalsIgnoreCase("false")) {
               this.session.getProject().getLogin().setShouldForceFieldNamesToUpperCase(false);
           } else {
               this.session.handleException(ValidationException.invalidBooleanValueForProperty(uppercaseString, PersistenceUnitProperties.NATIVE_QUERY_UPPERCASE_COLUMNS));
           }
        }
    }

    /**

    /**
     * Enable or disable forcing field names to be case insensitive.  Implementation of case insensitive column handling relies on setting
     * both sides to uppercase (the column names from annotations/xml as well as what is returned from the JDBC/statement)
     * The method needs to be called in deploy stage.
     */
    public static void updateCaseSensitivitySettings(Map m, MetadataProject project, AbstractSession session){
        //Set Native SQL flag if it was specified.
        String insensitiveString = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.UPPERCASE_COLUMN_NAMES, m, session);
        if (insensitiveString == null || insensitiveString.equalsIgnoreCase("true")) {
            // Set or default to case in-sensitive.
           project.setShouldForceFieldNamesToUpperCase(true);
           session.getProject().getLogin().setShouldForceFieldNamesToUpperCase(true);
        } else if (insensitiveString.equalsIgnoreCase("false")) {
            project.setShouldForceFieldNamesToUpperCase(false);
            session.getProject().getLogin().setShouldForceFieldNamesToUpperCase(false);
        } else {
            session.handleException(ValidationException.invalidBooleanValueForProperty(insensitiveString, PersistenceUnitProperties.UPPERCASE_COLUMN_NAMES));
        }
    }

    /**
     * Update the default pessimistic lock timeout value.
     * @param persistenceProperties the properties map
     */
    protected void updatePessimisticLockTimeout(Map persistenceProperties) {
        String pessimisticLockTimeout = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.PESSIMISTIC_LOCK_TIMEOUT, persistenceProperties, session);

        if (pessimisticLockTimeout != null) {
            try {
                session.setPessimisticLockTimeoutDefault(Integer.parseInt(pessimisticLockTimeout));
            } catch (NumberFormatException invalid) {
                session.handleException(ValidationException.invalidValueForProperty(pessimisticLockTimeout, PersistenceUnitProperties.PESSIMISTIC_LOCK_TIMEOUT, invalid));
            }
        }
    }

    /**
     * Enable or disable statements cached, update statements cache size.
     * The method needs to be called in deploy stage.
     */
    protected void updateCacheStatementSettings(Map m){
        // Cache statements if flag was specified.
        String statmentsNeedBeCached = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.CACHE_STATEMENTS, m, session);
        if (statmentsNeedBeCached!=null) {
            if (statmentsNeedBeCached.equalsIgnoreCase("true")) {
                if (session.isServerSession() && ((ServerSession)session).getConnectionPools().isEmpty()){
                    session.log(SessionLog.WARNING, SessionLog.PROPERTIES, "persistence_unit_ignores_statments_cache_setting", new Object[]{null});
                 } else {
                     session.getProject().getLogin().setShouldCacheAllStatements(true);
                 }
            } else if (statmentsNeedBeCached.equalsIgnoreCase("false")) {
                session.getProject().getLogin().setShouldCacheAllStatements(false);
            } else {
                session.handleException(ValidationException.invalidBooleanValueForEnableStatmentsCached(statmentsNeedBeCached));
            }
        }

        // Set statement cache size if specified.
        String cacheStatementsSize = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.CACHE_STATEMENTS_SIZE, m, session);
        if (cacheStatementsSize!=null) {
            try {
                session.getProject().getLogin().setStatementCacheSize(Integer.parseInt(cacheStatementsSize));
            } catch (NumberFormatException e) {
                session.handleException(ValidationException.invalidCacheStatementsSize(cacheStatementsSize,e.getMessage()));
            }
        }
    }

    /**
     * Enable or disable default allowing 0 as an id.
     */
    @SuppressWarnings("deprecation")
    protected void updateAllowZeroIdSetting(Map m) {
        String allowZero = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.ALLOW_ZERO_ID, m, this.session);
        if (allowZero != null) {
            if (allowZero.equalsIgnoreCase("true")) {
               Helper.isZeroValidPrimaryKey = true;
            } else if (allowZero.equalsIgnoreCase("false")) {
                Helper.isZeroValidPrimaryKey = false;
            } else {
                session.handleException(ValidationException.invalidBooleanValueForProperty(allowZero, PersistenceUnitProperties.ALLOW_ZERO_ID));
            }
        }
    }

    /**
     * Enable or disable default allowing 0 as an id.
     */
    protected void updateIdValidation(Map m) {
        String idValidationString = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.ID_VALIDATION, m, session);
        if (idValidationString != null) {
            session.getProject().setDefaultIdValidation(IdValidation.valueOf(idValidationString));
        }
    }


    /**
     * Sets the SharedCacheMode with values from the javax.persistence.sharedCache.mode property. If
     * user enters an invalid caching type, valueOf will throw an illegal argument exception, e.g.
     * java.lang.IllegalArgumentException: No enum const class
     * javax.persistence.SharedCacheMode.ALLBOGUS
     */
    protected void updateSharedCacheMode(Map m) {
        String sharedCacheMode = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.SHARED_CACHE_MODE, m, session);
        if (sharedCacheMode != null) {
            processor.getProject().setSharedCacheMode(SharedCacheMode.valueOf(sharedCacheMode));
        }
    }

    /**
     * sets the TABLE_CREATION_SUFFIX property on the session's project to be applied to all table creation statements (DDL)
     */
    protected void updateTableCreationSettings(Map m) {
        String tableCreationSuffix = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.TABLE_CREATION_SUFFIX, m, session);
        if (tableCreationSuffix != null && tableCreationSuffix.length()>0) {
            session.getPlatform().setTableCreationSuffix(tableCreationSuffix);
        }
    }

    /**
     * Sets shouldCreateIndicesOnForeignKeys DDL generation option.
     */
    protected void updateIndexForeignKeys(Map m) {
        String indexForeignKeys = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.DDL_GENERATION_INDEX_FOREIGN_KEYS, m, this.session);
        if (indexForeignKeys != null && (indexForeignKeys.length() > 0)) {
            if (indexForeignKeys.equalsIgnoreCase("true") ){
                this.session.getProject().getLogin().setShouldCreateIndicesOnForeignKeys(true);
            } else if (indexForeignKeys.equalsIgnoreCase("false")){
                this.session.getProject().getLogin().setShouldCreateIndicesOnForeignKeys(false);
            } else {
                this.session.handleException(ValidationException.invalidBooleanValueForProperty(indexForeignKeys, PersistenceUnitProperties.DDL_GENERATION_INDEX_FOREIGN_KEYS));
            }
        }
    }

    /**
     * Enable or disable default temporal mutable setting.
     * The method needs to be called in deploy stage.
     */
    protected void updateTemporalMutableSetting(Map m) {
        // Cache statements if flag was specified.
        String temporalMutable = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.TEMPORAL_MUTABLE, m, session);
        if (temporalMutable != null) {
            if (temporalMutable.equalsIgnoreCase("true")) {
               session.getProject().setDefaultTemporalMutable(true);
            } else if (temporalMutable.equalsIgnoreCase("false")) {
               session.getProject().setDefaultTemporalMutable(false);
            } else {
                session.handleException(ValidationException.invalidBooleanValueForProperty(temporalMutable, PersistenceUnitProperties.TEMPORAL_MUTABLE));
            }
        }
    }

    /**
     * Copy named queries defined in EclipseLink descriptor into the session if it was indicated to do so.
     */
    protected void setDescriptorNamedQueries(Map m) {
        // Copy named queries to session if the flag has been specified.
        String addNamedQueriesString  = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.INCLUDE_DESCRIPTOR_QUERIES, m, session);
        if (addNamedQueriesString!=null) {
            if (addNamedQueriesString.equalsIgnoreCase("true")) {
                session.copyDescriptorNamedQueries(false);
            } else {
                if (!addNamedQueriesString.equalsIgnoreCase("false")) {
                   session.handleException(ValidationException.invalidBooleanValueForAddingNamedQueries(addNamedQueriesString));
                }
            }
        }
    }

    private void updateQueryTimeout(Map persistenceProperties) {
        String timeout = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.QUERY_TIMEOUT, persistenceProperties, session);
        try {
            if (timeout != null) {
                session.setQueryTimeoutDefault(Integer.parseInt(timeout));
            }
        } catch (NumberFormatException exception) {
            this.session.handleException(ValidationException.invalidValueForProperty(timeout, PersistenceUnitProperties.QUERY_TIMEOUT, exception));
        }
    }

    //Bug #456067: Added persistence unit support for timeout units
    private void updateQueryTimeoutUnit(Map persistenceProperties) {
        String timeoutUnit = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.QUERY_TIMEOUT_UNIT, persistenceProperties, session);
        try {
            if (timeoutUnit != null) {
                TimeUnit unit = TimeUnit.valueOf(timeoutUnit);
                session.setQueryTimeoutUnitDefault(unit);
            }
        } catch (IllegalArgumentException exception) {
            this.session.handleException(ValidationException.invalidValueForProperty(timeoutUnit, PersistenceUnitProperties.QUERY_TIMEOUT_UNIT, exception));
        }
    }

    private void updateLockingTimestampDefault(Map persistenceProperties) {
        String local = EntityManagerFactoryProvider.getConfigPropertyAsStringLogDebug(PersistenceUnitProperties.USE_LOCAL_TIMESTAMP, persistenceProperties, session);
        try {
            if (local != null) {
                for (ClassDescriptor descriptor: session.getProject().getDescriptors().values()) {
                    OptimisticLockingPolicy policy = descriptor.getOptimisticLockingPolicy();
                    if (policy instanceof TimestampLockingPolicy) {
                        ((TimestampLockingPolicy)policy).setUsesServerTime(!Boolean.parseBoolean(local));
                    }
                }
            }
        } catch (NumberFormatException exception) {
            this.session.handleException(ValidationException.invalidValueForProperty(local, PersistenceUnitProperties.USE_LOCAL_TIMESTAMP, exception));
        }
    }

    /**
     * If Bean Validation is enabled, bootstraps Bean Validation on descriptors.
     * @param puProperties merged properties for this persistence unit
     */
    private void addBeanValidationListeners(Map puProperties, ClassLoader appClassLoader) {
        ValidationMode validationMode = getValidationMode(persistenceUnitInfo, puProperties);
        if (validationMode == ValidationMode.AUTO || validationMode == ValidationMode.CALLBACK) {
            // BeanValidationInitializationHelper contains static reference to javax.validation.* classes. We need to support
            // environment where these classes are not available.
            // To guard against some vms that eagerly resolve, reflectively load class to prevent any static reference to it
            String helperClassName = "org.eclipse.persistence.internal.jpa.deployment.BeanValidationInitializationHelper$BeanValidationInitializationHelperImpl";
            Class helperClass;
            try {
                // LIBERTY CHANGE (Open Liberty #1257/Eclipselink #529907) BEGIN
                if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
                    try {
                        helperClass = AccessController.doPrivileged(
                            new PrivilegedClassForName(helperClassName, true, appClassLoader));
                    } catch (Throwable t) {
                        // Try the ClassLoader that loaded Eclipselink classes
                        ClassLoader eclipseLinkClassLoader = EntityManagerSetupImpl.class.getClassLoader();
                        helperClass = AccessController.doPrivileged(
                                new PrivilegedClassForName(helperClassName, true, eclipseLinkClassLoader));
                    }
                } else {
                    try {
                        helperClass = PrivilegedAccessHelper.getClassForName(helperClassName, true, appClassLoader);
                    } catch (Throwable t) {
                        // Try the ClassLoader that loaded Eclipselink classes
                        ClassLoader eclipseLinkClassLoader = EntityManagerSetupImpl.class.getClassLoader();
                        helperClass = PrivilegedAccessHelper.getClassForName(helperClassName, true, eclipseLinkClassLoader);
                    }

                }
                // LIBERTY CHANGE (Open Liberty #1257/Eclipselink #529907) END
                BeanValidationInitializationHelper beanValidationInitializationHelper = (BeanValidationInitializationHelper)helperClass.newInstance();
                beanValidationInitializationHelper.bootstrapBeanValidation(puProperties, session, appClassLoader);
            } catch (Throwable e) {  //Catching Throwable to catch any linkage errors on vms that resolve eagerly
                if (validationMode == ValidationMode.CALLBACK) {
                    throw PersistenceUnitLoadingException.exceptionObtainingRequiredBeanValidatorFactory(e);
                } // else validationMode == ValidationMode.AUTO. Log a message, Ignore the exception
                this.session.log(SessionLog.FINEST, SessionLog.JPA, "validation_factory_not_initialized", new Object[]{ e.getMessage() });
            }
        }
    }

    /**
     * Validation mode from information in persistence.xml and properties specified at EMF creation
     * @param persitenceUnitInfo PersitenceUnitInfo instance for this persistence unit
     * @param puProperties merged properties for this persistence unit
     * @return validation mode
     */
    private static ValidationMode getValidationMode(PersistenceUnitInfo persitenceUnitInfo, Map puProperties) {
        //Check if overridden at emf creation
        String validationModeAtEMFCreation = (String) puProperties.get(PersistenceUnitProperties.VALIDATION_MODE);
        if(validationModeAtEMFCreation != null) {
            // User will receive IllegalArgumentException if an invalid mode has been specified
            return ValidationMode.valueOf(validationModeAtEMFCreation.toUpperCase());
        }

        //otherwise:
        ValidationMode validationMode = null;
        // Initialize with value in persitence.xml
        // Using reflection to call getValidationMode to prevent blowing up while we are running in JPA 1.0 environment
        // (This would be all JavaEE5 appservers) where PersistenceUnitInfo does not implement method getValidationMode().
        try {
            Method method = null;
            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
                method = AccessController.doPrivileged(new PrivilegedGetDeclaredMethod(PersistenceUnitInfo.class, "getValidationMode", null));
                validationMode = (ValidationMode) AccessController.doPrivileged(new PrivilegedMethodInvoker(method, persitenceUnitInfo));
            } else {
                method = PrivilegedAccessHelper.getDeclaredMethod(PersistenceUnitInfo.class, "getValidationMode", null);
                validationMode = (ValidationMode) PrivilegedAccessHelper.invokeMethod(method, persitenceUnitInfo, null);
            }
        } catch (Throwable exception) {
            // We are running in JavaEE5 environment. Catch and swallow any exceptions and return null.
        }

        if(validationMode == null) {
            // Default to AUTO as specified in JPA spec.
            validationMode = ValidationMode.AUTO;
        }
        return validationMode;
    }

    /**
     * INTERNAL:
     * Return an instance of Metamodel interface for access to the
     * metamodel of the persistence unit.
     * This method will complete any initialization done in the predeploy phase
     * of deployment.
     * @return Metamodel instance
     * @since Java Persistence 2.0
     */
    public Metamodel getMetamodel(ClassLoader classLoader) {
        preInitializeMetamodel();
        if (!((MetamodelImpl)metaModel).isInitialized()){
            ((MetamodelImpl)metaModel).initialize(classLoader);
            // If the canonical metamodel classes exist, initialize them
            initializeCanonicalMetamodel(metaModel);
        }
        return metaModel;
    }

    /**
     * INTERNAL:
     * Initialize the Canonical Metamodel classes generated by EclipseLink
     * @since Java Persistence 2.0
     */
    protected void initializeCanonicalMetamodel(Metamodel model) {
        // 338837: verify that the collection is not empty - this would mean entities did not make it into the search path
        if(null == model.getManagedTypes() || model.getManagedTypes().isEmpty()) {
            getSession().log(SessionLog.FINER, SessionLog.METAMODEL, "metamodel_type_collection_empty");
        }
        for (ManagedType manType : model.getManagedTypes()) {
            boolean classInitialized = false;
            String className = MetadataHelper.getQualifiedCanonicalName(manType.getJavaType().getName(), getSession());
            try {
                Class clazz = (Class)this.getSession().getDatasourcePlatform().convertObject(className, ClassConstants.CLASS);
                classInitialized=true;
                this.getSession().log(SessionLog.FINER, SessionLog.METAMODEL, "metamodel_canonical_model_class_found", className);
                String fieldName = "";
                for(Object attribute : manType.getDeclaredAttributes()) {
                    try {
                        fieldName = ((Attribute)attribute).getName();
                        if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                            AccessController.doPrivileged(new PrivilegedGetDeclaredField(clazz, fieldName, false)).set(clazz, attribute);
                        } else {
                            PrivilegedAccessHelper.getDeclaredField(clazz, fieldName, false).set(clazz, attribute);
                        }
                    } catch (NoSuchFieldException nsfe) {
                        // Ignore fields missing in canonical model (dclarke bug 346106)
                    } catch (Exception e) {
                       ValidationException v = ValidationException.invalidFieldForClass(fieldName, clazz);
                       v.setInternalException(e);
                       throw v;
                    }
                }
            } catch (ConversionException exception){
            }
            if (!classInitialized) {
                getSession().log(SessionLog.FINER, SessionLog.METAMODEL, "metamodel_canonical_model_class_not_found", className);
            }
        }
    }

    /**
     * INTERNAL:
     * Convenience function to allow us to reset the Metamodel
     * in the possible case that we want to regenerate it.
     * This function is outside of the JPA 2.0 specification.
     * @param aMetamodel
     * @since Java Persistence 2.0
     */
    public void setMetamodel(Metamodel aMetamodel) {
        this.metaModel = aMetamodel;
    }

    public boolean mustBeCompositeMember() {
        return mustBeCompositeMember(this.persistenceUnitInfo);
    }

    public boolean isCompositeMember() {
        return this.compositeEmSetupImpl != null;
    }

    public boolean isComposite() {
        return this.compositeMemberEmSetupImpls != null;
    }

    public static boolean mustBeCompositeMember(PersistenceUnitInfo puInfo) {
        String mustBeCompositeMemberStr = PropertiesHandler.getPropertyValue(PersistenceUnitProperties.COMPOSITE_UNIT_MEMBER, puInfo.getProperties(), false);
        if(mustBeCompositeMemberStr != null) {
            return mustBeCompositeMemberStr.equals("true");
        } else {
            return false;
        }
    }
    public static boolean isComposite(PersistenceUnitInfo puInfo) {
        String isCompositeString = PropertiesHandler.getPropertyValue(PersistenceUnitProperties.COMPOSITE_UNIT, puInfo.getProperties(), false);
        if(isCompositeString != null) {
            return isCompositeString.equals("true");
        } else {
            return false;
        }
    }

    public void setCompositeEmSetupImpl(EntityManagerSetupImpl compositeEmSetupImpl) {
        this.compositeEmSetupImpl = compositeEmSetupImpl;
    }

    public EntityManagerSetupImpl getCompositeEmSetupImpl() {
        return this.compositeEmSetupImpl;
    }

    // predeploy method will be used for static weaving
    public void setStaticWeaveInfo(StaticWeaveInfo staticWeaveInfo) {
        this.staticWeaveInfo = staticWeaveInfo;
    }

    protected void predeployCompositeMembers(Map predeployProperties, ClassLoader tempClassLoader) {
        // get all puInfos found in jar-files specified in composite's persistence.xml
        // all these puInfos are not composite because composites are recursively "taken apart", too.
        Set<SEPersistenceUnitInfo> compositeMemberPuInfos = getCompositeMemberPuInfoSet(persistenceUnitInfo, predeployProperties);
        // makes sure each member has a non-null property, overrides where required properties with composite's predeploy properties.
        updateCompositeMembersProperties(compositeMemberPuInfos, predeployProperties);
        // Don't log these properties - may contain passwords. The properties will be logged by contained persistence units.
        Map compositeMemberMapOfProperties = (Map)getConfigProperty(PersistenceUnitProperties.COMPOSITE_UNIT_PROPERTIES, predeployProperties);
        this.compositeMemberEmSetupImpls = new HashSet(compositeMemberPuInfos.size());
        this.processor = new MetadataProcessor();
        if (enableWeaving) {
            this.weaver = new PersistenceWeaver(new HashMap<String, ClassDetails>());
        }

        // create containedEmSetupImpls and predeploy them for the first time.
        // predeploy divided in three stages (modes):
        // all composite members should complete a stage before any of them can move to the next one.
        for (SEPersistenceUnitInfo compositeMemberPuInfo : compositeMemberPuInfos) {
            // set composite's temporary classloader
            compositeMemberPuInfo.setNewTempClassLoader(tempClassLoader);
            String containedPuName = compositeMemberPuInfo.getPersistenceUnitName();
            EntityManagerSetupImpl containedEmSetupImpl = new EntityManagerSetupImpl(containedPuName, containedPuName);
            // set composite
            containedEmSetupImpl.setCompositeEmSetupImpl(this);
            // non-null only in case predeploy is used for static weaving
            containedEmSetupImpl.setStaticWeaveInfo(this.staticWeaveInfo);
            // the properties guaranteed to be non-null after updateCompositeMemberProperties call
            Map compositeMemberProperties = (Map)compositeMemberMapOfProperties.get(containedPuName);
            containedEmSetupImpl.predeploy(compositeMemberPuInfo, compositeMemberProperties);
            // reset temporary classloader back to the original
            compositeMemberPuInfo.setNewTempClassLoader(compositeMemberPuInfo.getClassLoader());
            this.compositeMemberEmSetupImpls.add(containedEmSetupImpl);
        }

        // after the first loop containedEmSetupImpls are in HalfPredeployed state,
        // mode = COMPOSITE_MEMBER_MIDDLE mode
        for(EntityManagerSetupImpl containedEmSetupImpl : this.compositeMemberEmSetupImpls) {
            // properties not used, puInfo already set
            containedEmSetupImpl.predeploy(null, null);
        }

        // after the second loop containedEmSetupImpls are still in HalfPredeployed state,
        // mode = COMPOSITE_MEMBER_FINAL mode
        for(EntityManagerSetupImpl containedEmSetupImpl : this.compositeMemberEmSetupImpls) {
            // properties not used, puInfo already set
            PersistenceWeaver containedWeaver = (PersistenceWeaver)containedEmSetupImpl.predeploy(null, null);
            // containedEmSetupImpl is finally in Predeployed state.
            // if both composite and composite member weavings enabled copy class details from member's weaver to composite's one.
            if(enableWeaving && containedWeaver != null) {
                this.weaver.getClassDetailsMap().putAll(containedWeaver.getClassDetailsMap());
            }
        }

        if(enableWeaving && this.weaver.getClassDetailsMap().isEmpty()) {
            this.weaver = null;
        }
    }

    protected void deployCompositeMembers(Map deployProperties, ClassLoader realClassLoader) {
        // Don't log these properties - may contain passwords. The properties will be logged by contained persistence units.
        Map compositeMemberMapOfProperties = (Map)getConfigProperty(PersistenceUnitProperties.COMPOSITE_UNIT_PROPERTIES, deployProperties);
        for(EntityManagerSetupImpl compositeMemberEmSetupImpl : this.compositeMemberEmSetupImpls) {
            // the properties guaranteed to be non-null after updateCompositeMemberProperties call
            Map compositeMemberProperties = (Map)compositeMemberMapOfProperties.get(compositeMemberEmSetupImpl.getPersistenceUnitInfo().getPersistenceUnitName());
            compositeMemberEmSetupImpl.deploy(realClassLoader, compositeMemberProperties);
            AbstractSession containedSession = compositeMemberEmSetupImpl.getSession();
            ((SessionBroker)session).registerSession(containedSession.getName(), containedSession);
        }
    }

    /**
     * INTERNAL:
     * Cause the first phase of metamodel initialization.  This phase involves detecting the classes involved
     * and build metamodel instances for them.
     */
    public void preInitializeMetamodel(){
        // perform lazy initialisation
        Metamodel tempMetaModel = null;
        if (null == metaModel){
            // 338837: verify that the collection is not empty - this would mean entities did not make it into the search path
            tempMetaModel = new MetamodelImpl(this);
            // set variable after init has executed without exception
            metaModel = tempMetaModel;
        }
    }

    /**
     * INTERNAL:
     * First phase of canonical metamodel initialization.  For each class the metamodel is aware of, check
     * for a canonical metamodel class and initialize each attribute in it with a proxy that can cause the
     * rest of the metamodel population.  Attributes are found reflectively rather than through the metamodel
     * to avoid having to further initialize the metamodel.
     * @param factory
     */
    public void preInitializeCanonicalMetamodel(EntityManagerFactoryImpl factory){
        // 338837: verify that the collection is not empty - this would mean entities did not make it into the search path
        if(null == metaModel.getManagedTypes() || metaModel.getManagedTypes().isEmpty()) {
            getSession().log(SessionLog.FINER, SessionLog.METAMODEL, "metamodel_type_collection_empty");
        }
        for (ManagedType manType : metaModel.getManagedTypes()) {
            boolean classInitialized = false;
            String className = MetadataHelper.getQualifiedCanonicalName(((ManagedTypeImpl)manType).getJavaTypeName(), getSession());
            try {
                Class clazz = (Class)this.getSession().getDatasourcePlatform().convertObject(className, ClassConstants.CLASS);
                classInitialized=true;
                this.getSession().log(SessionLog.FINER, SessionLog.METAMODEL, "metamodel_canonical_model_class_found", className);
                Field[] fields = null;
                if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                    fields = AccessController.doPrivileged(new PrivilegedGetDeclaredFields(clazz));
                } else {
                    fields = PrivilegedAccessHelper.getDeclaredFields(clazz);
                }
                for(Field attribute : fields) {
                    if (Attribute.class.isAssignableFrom(attribute.getType())){
                        Object assignedAttribute = null;
                        if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                            assignedAttribute = AccessController.doPrivileged(new PrivilegedGetValueFromField(attribute, null));
                        } else {
                            assignedAttribute =PrivilegedAccessHelper.getValueFromField(attribute, null);
                        }
                        AttributeProxyImpl proxy = null;
                        if (assignedAttribute == null){
                            if (SingularAttribute.class.isAssignableFrom(attribute.getType())){
                                proxy = new SingularAttributeProxyImpl();
                            } else if (MapAttribute.class.isAssignableFrom(attribute.getType())){
                                proxy = new MapAttributeProxyImpl();
                            } else if (SetAttribute.class.isAssignableFrom(attribute.getType())){
                                proxy = new SetAttributeProxyImpl();
                            } else if (ListAttribute.class.isAssignableFrom(attribute.getType())){
                                proxy = new ListAttributeProxyImpl();
                            } else if (CollectionAttribute.class.isAssignableFrom(attribute.getType())){
                                proxy = new CollectionAttributeProxyImpl();
                            }
                            if (proxy != null){
                                attribute.setAccessible(true);
                                attribute.set(null, proxy);
                            }
                        } else if (assignedAttribute instanceof AttributeProxyImpl){
                            proxy = (AttributeProxyImpl)assignedAttribute;
                        }
                        if (proxy != null){
                            proxy.addFactory(factory);
                        }
                    }
                }
            } catch (PrivilegedActionException pae){
                getSession().logThrowable(SessionLog.FINEST,  SessionLog.METAMODEL, pae);
            } catch (IllegalAccessException iae){
                getSession().logThrowable(SessionLog.FINEST,  SessionLog.METAMODEL, iae);
            } catch (ConversionException ce){
            }
            if (!classInitialized) {
                getSession().log(SessionLog.FINER, SessionLog.METAMODEL, "metamodel_canonical_model_class_not_found", className);
            }
        }
    }

    /*
     * Overide composite member properties' map with a new one, which
     * has (possibly empty but non-null) properties for each composite member,
     * for required properties overrides values with those from composite properties.
     */
    protected void updateCompositeMembersProperties(Map compositeProperties) {
        Set<SEPersistenceUnitInfo> compositePuInfos = new HashSet(compositeMemberEmSetupImpls.size());
        for (EntityManagerSetupImpl compositeMemberEmSetupImpl : compositeMemberEmSetupImpls) {
            compositePuInfos.add((SEPersistenceUnitInfo)compositeMemberEmSetupImpl.persistenceUnitInfo);
        }
        updateCompositeMembersProperties(compositePuInfos, compositeProperties);
    }

    /*
     * Overide composite member properties' map with a new one, which
     * has (possibly empty but non-null) properties for each composite member,
     * for required properties overrides values with those from composite properties.
     * Parameter compositePuInfo indicates whether compositeMemberPreoperties should be merged (overriding) with its puInfo properties
     * (false for predeploy, true for deploy).
     */
    protected void updateCompositeMembersProperties(Set<SEPersistenceUnitInfo> compositePuInfos, Map compositeProperties) {
        // Don't log these properties - may contain passwords. The properties will be logged by contained persistence units.
        Map compositeMemberMapOfProperties = (Map)getConfigProperty(PersistenceUnitProperties.COMPOSITE_UNIT_PROPERTIES, compositeProperties);
        Map newCompositeMemberMapOfProperties;
        if (compositeMemberMapOfProperties == null) {
            newCompositeMemberMapOfProperties = new HashMap(compositePuInfos.size());
        } else {
            // Don't alter user-supplied properties' map - create a copy instead
            newCompositeMemberMapOfProperties = new HashMap(compositeMemberMapOfProperties);
        }

        for (SEPersistenceUnitInfo compositePuInfo : compositePuInfos) {
            String compositeMemberPuName = compositePuInfo.getPersistenceUnitName();
            Map compositeMemberProperties = (Map)newCompositeMemberMapOfProperties.get(compositeMemberPuName);
            Map newCompositeMemberProperties;
            if (compositeMemberProperties == null) {
                newCompositeMemberProperties = new HashMap();
            } else {
                // Don't alter user-supplied properties - create a copy instead
                newCompositeMemberProperties = new HashMap(compositeMemberProperties);
            }
            overrideMemberProperties(newCompositeMemberProperties, compositeProperties);
            newCompositeMemberProperties = mergeMaps(newCompositeMemberProperties, compositePuInfo.getProperties());
            translateOldProperties(newCompositeMemberProperties, session);
            newCompositeMemberMapOfProperties.put(compositeMemberPuName, newCompositeMemberProperties);
        }

        // set the new COMPOSITE_PROPERTIES into compositeProperties
        compositeProperties.put(PersistenceUnitProperties.COMPOSITE_UNIT_PROPERTIES, newCompositeMemberMapOfProperties);
    }

    /*
     * For required properties overrides values with those from composite properties.
     */
    protected static void overrideMemberProperties(Map memberProperties, Map compositeProperties) {
        String transactionTypeProp =  (String)compositeProperties.get(PersistenceUnitProperties.TRANSACTION_TYPE);
        if (transactionTypeProp != null) {
            memberProperties.put(PersistenceUnitProperties.TRANSACTION_TYPE, transactionTypeProp);
        } else {
            memberProperties.remove(PersistenceUnitProperties.TRANSACTION_TYPE);
        }

        String serverPlatformProp =  (String)compositeProperties.get(PersistenceUnitProperties.TARGET_SERVER);
        if (serverPlatformProp != null) {
            memberProperties.put(PersistenceUnitProperties.TARGET_SERVER, serverPlatformProp);
        } else {
            memberProperties.remove(PersistenceUnitProperties.TARGET_SERVER);
        }

        Boolean isValidationOnly =  (Boolean)compositeProperties.get(PersistenceUnitProperties.VALIDATION_ONLY_PROPERTY);
        if (isValidationOnly != null) {
            memberProperties.put(PersistenceUnitProperties.VALIDATION_ONLY_PROPERTY, isValidationOnly);
        } else {
            memberProperties.remove(PersistenceUnitProperties.VALIDATION_ONLY_PROPERTY);
        }
    }

    /*
     * If a member is composite then add its members instead.
     * All members' puNames must be unique.
     * Return a Map of composite member SEPersistenceUnitInfo keyed by persistence unit name.
     */
    protected static Map<String, SEPersistenceUnitInfo> getCompositeMemberPuInfoMap(PersistenceUnitInfo puInfo, Map predeployProperties) {
        Set<SEPersistenceUnitInfo> memeberPuInfoSet = PersistenceUnitProcessor.getPersistenceUnits(puInfo.getClassLoader(), predeployProperties, puInfo.getJarFileUrls());
        HashMap<String, SEPersistenceUnitInfo> memberPuInfoMap = new HashMap(memeberPuInfoSet.size());
        for (SEPersistenceUnitInfo memberPuInfo : memeberPuInfoSet) {
            // override transaction type with composite's transaction type
            memberPuInfo.setTransactionType(puInfo.getTransactionType());
            // override properties that should be overridden by composit's properties
            overrideMemberProperties(memberPuInfo.getProperties(), puInfo.getProperties());
            if (isComposite(memberPuInfo)) {
                Map<String, SEPersistenceUnitInfo> containedMemberPuInfoMap = getCompositeMemberPuInfoMap(memberPuInfo, memberPuInfo.getProperties());
                Iterator<Map.Entry<String, SEPersistenceUnitInfo>> it = containedMemberPuInfoMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, SEPersistenceUnitInfo> entry = it.next();
                    String containedMemberPuName = entry.getKey();
                    SEPersistenceUnitInfo containedMemberPuInfo = entry.getValue();
                    SEPersistenceUnitInfo anotherMemeberPuInfo = memberPuInfoMap.get(containedMemberPuName);
                    if (anotherMemeberPuInfo == null) {
                        memberPuInfoMap.put(containedMemberPuName, containedMemberPuInfo);
                    } else {
                        throwPersistenceUnitNameAlreadyInUseException(containedMemberPuName, containedMemberPuInfo, anotherMemeberPuInfo);
                    }
                }
            } else {
                String memberPuName = memberPuInfo.getPersistenceUnitName();
                SEPersistenceUnitInfo anotherMemeberPuInfo = memberPuInfoMap.get(memberPuName);
                if (anotherMemeberPuInfo == null) {
                    memberPuInfoMap.put(memberPuName, memberPuInfo);
                } else {
                    throwPersistenceUnitNameAlreadyInUseException(memberPuName, memberPuInfo, anotherMemeberPuInfo);
                }
            }
        }
        return memberPuInfoMap;
    }

    /*
     * If a member is composite then add its members instead.
     * All members' puNames must be unique.
     * Return a Set of composite member SEPersistenceUnitInfo.
     */
    protected static Set<SEPersistenceUnitInfo> getCompositeMemberPuInfoSet(PersistenceUnitInfo puInfo, Map predeployProperties) {
        return new HashSet(getCompositeMemberPuInfoMap(puInfo, predeployProperties).values());
    }

    public static void throwPersistenceUnitNameAlreadyInUseException(String puName, PersistenceUnitInfo newPuInfo, PersistenceUnitInfo exsitingPuInfo) {
        String puUrl;
        String anotherPuUrl;
        try {
            puUrl = URLDecoder.decode(newPuInfo.getPersistenceUnitRootUrl().toString(), "UTF8");
            anotherPuUrl = URLDecoder.decode(exsitingPuInfo.getPersistenceUnitRootUrl().toString(), "UTF8");
        } catch (UnsupportedEncodingException e) {
            puUrl = newPuInfo.getPersistenceUnitRootUrl().toString();
            anotherPuUrl = exsitingPuInfo.getPersistenceUnitRootUrl().toString();
        }
        throw PersistenceUnitLoadingException.persistenceUnitNameAlreadyInUse(puName, puUrl, anotherPuUrl);
    }

    /**
     * Create a new version of this EntityManagerSetupImpl and cache it.  Prepare "this" EntityManagerSetupImpl
     * for garbage collection.
     *
     * This call will mean any users of this EntityManagerSetupImpl will get the new version the next time
     * they look it up (for instance and EntityManager creation time)
     * @param properties
     * @return
     */
    public EntityManagerSetupImpl refreshMetadata(Map properties){
        String sessionName = getSessionName();
        String uniqueName = getPersistenceUnitUniqueName();
        EntityManagerSetupImpl newSetupImpl = new EntityManagerSetupImpl(uniqueName, sessionName);
        newSetupImpl.setIsInContainerMode(isInContainerMode);
        newSetupImpl.enableWeaving = enableWeaving;
        Map refreshProperties = new HashMap();
        refreshProperties.putAll(getSession().getProperties());
        if (properties != null){
            refreshProperties.putAll(properties);
        }
        newSetupImpl.predeploy(getPersistenceUnitInfo(), refreshProperties);
        // newSetupImpl has been already predeployed, predeploy will just increment factoryCount.
        if (!isInContainerMode) {
            newSetupImpl.predeploy(getPersistenceUnitInfo(), refreshProperties);
        }
        synchronized (EntityManagerFactoryProvider.emSetupImpls) {
            SessionManager.getManager().getSessions().remove(sessionName, getSession());
            if (EntityManagerFactoryProvider.emSetupImpls.get(sessionName).equals(this)){
                EntityManagerFactoryProvider.emSetupImpls.remove(sessionName);
            }
            setIsMetadataExpired(true);
            //stop this EntityManagerSetupImpl's session from processing refresh commands. The new session should listen instead.
            getSession().setRefreshMetadataListener(null);

            EntityManagerFactoryProvider.addEntityManagerSetupImpl(sessionName, newSetupImpl);
        }
        return newSetupImpl;
    }

    /**
     * This method is just a wrapper on refreshMetadata so that core does not need a dependency on JPA
     * due to the EntityManagerSetupImpl return value.  This method satisfies the MetedataRefreshListener implementation
     * and is called by incoming RCM refresh commands
     *
     * @see refreshMetadata
     */
    @Override
    public void triggerMetadataRefresh(Map properties){
        refreshMetadata(properties);
    }

    /**
     * INTERNAL:
     * Generate the DDL per the properties specified.
     */
    public void writeDDL(Map props, DatabaseSessionImpl session, ClassLoader classLoader) {
        if (this.compositeMemberEmSetupImpls == null) {
            // Generate the DDL if we find either EclipseLink or JPA DDL generation properties.
            // Note, we do one or the other, that is, we do not mix the properties by default
            // but we may use some with both, e.g. APP_LOCATION. EclipseLink properties
            // override JPA properties.

            if (hasConfigProperty(DDL_GENERATION, props)) {
                // We have EclipseLink DDL generation properties.
                String ddlGeneration = getConfigPropertyAsString(DDL_GENERATION, props).toLowerCase();

                if (! ddlGeneration.equals(NONE)) {
                    writeDDL(ddlGeneration, props, session, classLoader);
                }
            } else {
                // Look for JPA schema generation properties.

                // Schema generation for the database.
                if (hasConfigProperty(SCHEMA_GENERATION_DATABASE_ACTION, props)) {
                    String databaseGenerationAction = getConfigPropertyAsString(SCHEMA_GENERATION_DATABASE_ACTION, props).toLowerCase();

                    if (! databaseGenerationAction.equals(SCHEMA_GENERATION_NONE_ACTION)) {
                        if (databaseGenerationAction.equals(SCHEMA_GENERATION_CREATE_ACTION)) {
                            writeDDL(SCHEMA_GENERATION_CREATE_SOURCE, SCHEMA_GENERATION_CREATE_SCRIPT_SOURCE, TableCreationType.CREATE, props, session, classLoader);
                        } else if (databaseGenerationAction.equals(PersistenceUnitProperties.CREATE_OR_EXTEND)) {
                            writeDDL(SCHEMA_GENERATION_CREATE_SOURCE, SCHEMA_GENERATION_CREATE_SCRIPT_SOURCE, TableCreationType.EXTEND, props, session, classLoader);
                        } else if (databaseGenerationAction.equals(SCHEMA_GENERATION_DROP_ACTION)) {
                            writeDDL(SCHEMA_GENERATION_DROP_SOURCE, SCHEMA_GENERATION_DROP_SCRIPT_SOURCE, TableCreationType.DROP, props, session, classLoader);
                        } else if (databaseGenerationAction.equals(SCHEMA_GENERATION_DROP_AND_CREATE_ACTION)) {
                            writeDDL(SCHEMA_GENERATION_DROP_SOURCE, SCHEMA_GENERATION_DROP_SCRIPT_SOURCE, TableCreationType.DROP, props, session, classLoader);
                            writeDDL(SCHEMA_GENERATION_CREATE_SOURCE, SCHEMA_GENERATION_CREATE_SCRIPT_SOURCE, TableCreationType.CREATE, props, session, classLoader);
                        } else {
                            String validOptions = SCHEMA_GENERATION_CREATE_ACTION + ", " + SCHEMA_GENERATION_DROP_ACTION + ", " +  SCHEMA_GENERATION_DROP_AND_CREATE_ACTION + ", " + PersistenceUnitProperties.CREATE_OR_EXTEND;
                            session.log(SessionLog.WARNING, SessionLog.PROPERTIES, "ddl_generation_unknown_property_value", new Object[] {SCHEMA_GENERATION_DATABASE_ACTION, databaseGenerationAction, persistenceUnitInfo.getPersistenceUnitName(), validOptions});
                        }
                    }
                }

                // Schema generation for target scripts.
                if (hasConfigProperty(SCHEMA_GENERATION_SCRIPTS_ACTION, props)) {
                    String scriptsGenerationAction = getConfigPropertyAsString(SCHEMA_GENERATION_SCRIPTS_ACTION, props).toLowerCase();

                    if (! scriptsGenerationAction.equals(SCHEMA_GENERATION_NONE_ACTION)) {
                        if (scriptsGenerationAction.equals(SCHEMA_GENERATION_CREATE_ACTION)) {
                            writeMetadataDDLToScript(TableCreationType.CREATE, props, session, classLoader);
                        } else if (scriptsGenerationAction.equals(PersistenceUnitProperties.CREATE_OR_EXTEND)) {
                            writeMetadataDDLToScript(TableCreationType.EXTEND, props, session, classLoader);
                        } else if (scriptsGenerationAction.equals(SCHEMA_GENERATION_DROP_ACTION)) {
                            writeMetadataDDLToScript(TableCreationType.DROP, props, session, classLoader);
                        } else if (scriptsGenerationAction.equals(SCHEMA_GENERATION_DROP_AND_CREATE_ACTION)) {
                            writeMetadataDDLToScript(TableCreationType.DROP_AND_CREATE, props, session, classLoader);
                        } else {
                            String validOptions = SCHEMA_GENERATION_CREATE_ACTION + ", " + SCHEMA_GENERATION_DROP_ACTION + ", " +  SCHEMA_GENERATION_DROP_AND_CREATE_ACTION;
                            session.log(SessionLog.WARNING, SessionLog.PROPERTIES, "ddl_generation_unknown_property_value", new Object[] {SCHEMA_GENERATION_SCRIPTS_ACTION, scriptsGenerationAction, persistenceUnitInfo.getPersistenceUnitName(), validOptions});
                        }
                    }
                }

                // Once we've generated any and all DDL, check for load scripts.
                writeSourceScriptToDatabase(getConfigProperty(SCHEMA_GENERATION_SQL_LOAD_SCRIPT_SOURCE, props), session, classLoader);
            }
        } else {
            // composite
            Map compositeMemberMapOfProperties = (Map)getConfigProperty(PersistenceUnitProperties.COMPOSITE_UNIT_PROPERTIES, props);
            for(EntityManagerSetupImpl compositeMemberEmSetupImpl : this.compositeMemberEmSetupImpls) {
                // the properties guaranteed to be non-null after updateCompositeMemberProperties call
                Map compositeMemberProperties = (Map)compositeMemberMapOfProperties.get(compositeMemberEmSetupImpl.getPersistenceUnitInfo().getPersistenceUnitName());
                compositeMemberEmSetupImpl.writeDDL(compositeMemberProperties, session, classLoader);
            }
        }
    }

    /**
     * INTERNAL:
     * Generate the DDL from the persistence unit metadata. This DDL generation
     * utilizes the EclipseLink DDL properties.
     */
    protected void writeDDL(String ddlGeneration, Map props, DatabaseSessionImpl session, ClassLoader classLoader) {
        // By default the table creation type will be 'none'.
        TableCreationType ddlType = TableCreationType.NONE;

        if (ddlGeneration.equals(PersistenceUnitProperties.CREATE_ONLY)) {
            ddlType = TableCreationType.CREATE;
        } else if (ddlGeneration.equals(PersistenceUnitProperties.DROP_ONLY)) {
            ddlType = TableCreationType.DROP;
        } else if (ddlGeneration.equals(PersistenceUnitProperties.DROP_AND_CREATE)) {
            ddlType = TableCreationType.DROP_AND_CREATE;
        } else if (ddlGeneration.equals(PersistenceUnitProperties.CREATE_OR_EXTEND)) {
            ddlType = TableCreationType.EXTEND;
        } else {
            // Log a warning if we have an unknown ddl generation.
            String validOptions = PersistenceUnitProperties.NONE + ", " +
                                  PersistenceUnitProperties.CREATE_ONLY + ", " +
                                  PersistenceUnitProperties.DROP_ONLY + ", " +
                                  PersistenceUnitProperties.DROP_AND_CREATE + ", " +
                                  PersistenceUnitProperties.CREATE_OR_EXTEND;
            session.log(SessionLog.WARNING, SessionLog.PROPERTIES, "ddl_generation_unknown_property_value", new Object[] {PersistenceUnitProperties.DDL_GENERATION, ddlGeneration, persistenceUnitInfo.getPersistenceUnitName(), validOptions});
        }

        if (ddlType != TableCreationType.NONE) {
            String ddlGenerationMode = getConfigPropertyAsString(PersistenceUnitProperties.DDL_GENERATION_MODE, props, PersistenceUnitProperties.DEFAULT_DDL_GENERATION_MODE);

            // Optimize for cases where the value is explicitly set to NONE
            if (! ddlGenerationMode.equals(NONE)) {
                if (isCompositeMember()) {
                    // debug output added to make it easier to navigate the log because the method is called outside of composite member deploy
                    session.log(SessionLog.FINEST, SessionLog.PROPERTIES, "composite_member_begin_call", new Object[]{"generateDDL", persistenceUnitInfo.getPersistenceUnitName(), state});
                }

                SchemaManager mgr = new SchemaManager(session);

                if (ddlGenerationMode.equals(PersistenceUnitProperties.DDL_DATABASE_GENERATION) || ddlGenerationMode.equals(PersistenceUnitProperties.DDL_BOTH_GENERATION)) {
                    writeDDLToDatabase(mgr, ddlType);
                }

                if (ddlGenerationMode.equals(PersistenceUnitProperties.DDL_SQL_SCRIPT_GENERATION)|| ddlGenerationMode.equals(PersistenceUnitProperties.DDL_BOTH_GENERATION)) {
                    String appLocation = getConfigPropertyAsString(PersistenceUnitProperties.APP_LOCATION, props, PersistenceUnitProperties.DEFAULT_APP_LOCATION);

                    // These could be a string (file name urls) or actual writers.
                    Object createDDLJdbc = getConfigProperty(PersistenceUnitProperties.CREATE_JDBC_DDL_FILE, props, PersistenceUnitProperties.DEFAULT_CREATE_JDBC_FILE_NAME);
                    Object dropDDLJdbc = getConfigProperty(PersistenceUnitProperties.DROP_JDBC_DDL_FILE, props, PersistenceUnitProperties.DEFAULT_DROP_JDBC_FILE_NAME);
                    writeDDLToFiles(mgr, appLocation,  createDDLJdbc,  dropDDLJdbc, ddlType, props);
                }

                // Log a warning if we have an unknown ddl generation mode.
                if ( (! ddlGenerationMode.equals(PersistenceUnitProperties.DDL_DATABASE_GENERATION)) && (! ddlGenerationMode.equals(PersistenceUnitProperties.DDL_SQL_SCRIPT_GENERATION)) && (! ddlGenerationMode.equals(PersistenceUnitProperties.DDL_BOTH_GENERATION))) {
                    String validOptions = PersistenceUnitProperties.DDL_DATABASE_GENERATION + ", " +
                                          PersistenceUnitProperties.DDL_SQL_SCRIPT_GENERATION + ", " +
                                          PersistenceUnitProperties.DDL_BOTH_GENERATION;
                    session.log(SessionLog.WARNING, SessionLog.PROPERTIES, "ddl_generation_unknown_property_value", new Object[] {PersistenceUnitProperties.DDL_GENERATION_MODE, ddlGenerationMode, persistenceUnitInfo.getPersistenceUnitName(), validOptions});
                }

                if (isCompositeMember()) {
                    // debug output added to make it easier to navigate the log because the method is called outside of composite member deploy
                    session.log(SessionLog.FINEST, SessionLog.PROPERTIES, "composite_member_end_call", new Object[]{"generateDDL", persistenceUnitInfo.getPersistenceUnitName(), state});
                }
            }
        }
    }

    /**
     * INTERNAL:
     * Generate the DDL per the properties given.
     */
    protected void writeDDL(String generationSourceProperty, String scriptGenerationSourceProperty, TableCreationType tableCreationType, Map props, DatabaseSessionImpl session, ClassLoader loader) {
        String generationSource = getConfigPropertyAsString(generationSourceProperty, props);
        Object scriptGenerationSource = getConfigProperty(scriptGenerationSourceProperty, props);

        if (generationSource == null) {
            if (scriptGenerationSource == null) {
                writeMetadataDDLToDatabase(tableCreationType, props, session, loader);
            } else {
                writeSourceScriptToDatabase(scriptGenerationSource, session, loader);
            }
        } else if (generationSource.equals(SCHEMA_GENERATION_METADATA_SOURCE)) {
            writeMetadataDDLToDatabase(tableCreationType, props, session, loader);
        } else if (generationSource.equals(SCHEMA_GENERATION_SCRIPT_SOURCE)) {
            writeSourceScriptToDatabase(scriptGenerationSource, session, loader);
        } else if (generationSource.equals(SCHEMA_GENERATION_METADATA_THEN_SCRIPT_SOURCE)) {
            writeMetadataDDLToDatabase(tableCreationType, props, session, loader);
            writeSourceScriptToDatabase(scriptGenerationSource, session, loader);
        } else if (generationSource.equals(SCHEMA_GENERATION_SCRIPT_THEN_METADATA_SOURCE)) {
            writeSourceScriptToDatabase(scriptGenerationSource, session, loader);
            writeMetadataDDLToDatabase(tableCreationType, props, session, loader);
        } else {
            String validOptions = SCHEMA_GENERATION_METADATA_SOURCE + ", " +
                                  SCHEMA_GENERATION_SCRIPT_SOURCE + ", " +
                                  SCHEMA_GENERATION_METADATA_THEN_SCRIPT_SOURCE + ", " +
                                  SCHEMA_GENERATION_SCRIPT_THEN_METADATA_SOURCE;
            session.log(SessionLog.WARNING, SessionLog.PROPERTIES, "ddl_generation_unknown_property_value", new Object[] {generationSourceProperty, generationSource, persistenceUnitInfo.getPersistenceUnitName(), validOptions});
        }
    }

    /**
     * INTERNAL:
     * Generate and write DDL from the persistence unit metadata to the database.
     */
    protected void writeDDLToDatabase(SchemaManager mgr, TableCreationType ddlType) {
        String str = getConfigPropertyAsString(PersistenceUnitProperties.JAVASE_DB_INTERACTION, null ,"true");
        boolean interactWithDB = Boolean.valueOf(str.toLowerCase()).booleanValue();
        if (!interactWithDB){
            return;
        }

        generateDefaultTables(mgr, ddlType);
    }

    /**
     * @deprecated Extenders should now use
     *             {@link #writeDDLToFiles(SchemaManager, String, Object, Object, TableCreationType, Map)}
     */
    @Deprecated
    protected void writeDDLToFiles(SchemaManager mgr, String appLocation, Object createDDLJdbc, Object dropDDLJdbc,
            TableCreationType ddlType) {
        writeDDLToFiles(mgr, appLocation, createDDLJdbc, dropDDLJdbc, ddlType, Collections.EMPTY_MAP);
    }

    /**
     * Write the DDL to the files provided.
     */
    protected void writeDDLToFiles(SchemaManager mgr, String appLocation, Object createDDLJdbc, Object dropDDLJdbc, TableCreationType ddlType, Map props) {
        // Ensure that the appLocation string ends with File.separator if
        // specified. In JPA there is no default for app location, however, if
        // the user did specify one, we'll use it.
        String appLocationPrefix = (appLocation == null) ? "" : addFileSeperator(appLocation);

        if (ddlType.equals(TableCreationType.CREATE) || ddlType.equals(TableCreationType.DROP_AND_CREATE) || ddlType.equals(TableCreationType.EXTEND)) {
            if (createDDLJdbc == null) {
                // Using EclipseLink properties, the create script has a default.
                // Using JPA properties, the user must specify the target else an exception must be thrown.
                throw new IllegalArgumentException(ExceptionLocalization.buildMessage("jpa21-ddl-create-script-target-not-specified"));
            } else if (createDDLJdbc instanceof Writer) {
                mgr.outputCreateDDLToWriter((Writer) createDDLJdbc);
            } else {
                // Assume it is a String file name.
                mgr.outputCreateDDLToFile(appLocationPrefix + createDDLJdbc);
            }
        }

        if (ddlType.equals(TableCreationType.DROP) || ddlType.equals(TableCreationType.DROP_AND_CREATE)) {
            if (dropDDLJdbc == null) {
                // Using EclipseLink properties, the drop script has a default.
                // Using JPA properties, the user must specify the target else an exception must be thrown.
                throw new IllegalArgumentException(ExceptionLocalization.buildMessage("jpa21-ddl-drop-script-target-not-specified"));
            } else if (dropDDLJdbc instanceof Writer) {
                mgr.outputDropDDLToWriter((Writer) dropDDLJdbc);
            } else if (dropDDLJdbc instanceof String) {
                mgr.outputDropDDLToFile(appLocationPrefix + dropDDLJdbc);
            } else {
                throw new PersistenceException(ExceptionLocalization.buildMessage("jpa21-ddl-invalid-target-script-type", new Object[]{ dropDDLJdbc , dropDDLJdbc.getClass()}));
            }
        }

        String propString = getConfigPropertyAsString(PersistenceUnitProperties.SCHEMA_GENERATION_SCRIPT_TERMINATE_STATEMENTS, props);
        boolean terminator = Boolean.parseBoolean(propString);
        mgr.setCreateSQLFiles(terminator);
        generateDefaultTables(mgr, ddlType);
        mgr.closeDDLWriter();
    }

    /**
     * INTERNAL:
     * Generate and write DDL from the persistence unit metadata to the database.
     */
    protected void writeMetadataDDLToDatabase(TableCreationType tableCreationType, Map props, DatabaseSessionImpl session, ClassLoader classLoader) {
        SchemaManager mgr = new SchemaManager(session);

        // Set the create database schemas flag on the schema manager.
        String createSchemas = getConfigPropertyAsString(SCHEMA_GENERATION_CREATE_DATABASE_SCHEMAS, props);
        mgr.setCreateDatabaseSchemas(createSchemas != null && createSchemas.equalsIgnoreCase("true"));

        writeDDLToDatabase(mgr, tableCreationType);
    }

    /**
     * INTERNAL:
     * Generate and write DDL from the persistence unit metadata to scripts.
     */
    protected void writeMetadataDDLToScript(TableCreationType tableCreationType, Map props, DatabaseSessionImpl session, ClassLoader classLoader) {
        SchemaManager mgr = new SchemaManager(session);

        // Set the create database schemas flag on the schema manager.
        String createSchemas = getConfigPropertyAsString(SCHEMA_GENERATION_CREATE_DATABASE_SCHEMAS, props);
        mgr.setCreateDatabaseSchemas(createSchemas != null && createSchemas.equalsIgnoreCase("true"));

        writeDDLToFiles(mgr, getConfigPropertyAsString(PersistenceUnitProperties.APP_LOCATION, props),  getConfigProperty(SCHEMA_GENERATION_SCRIPTS_CREATE_TARGET, props),  getConfigProperty(SCHEMA_GENERATION_SCRIPTS_DROP_TARGET, props), tableCreationType, props);
    }

    /**
     * This method will read SQL from a reader or URL and send it through
     * to the database. This could open up the database to SQL injection if
     * not careful.
     */
    protected void writeSourceScriptToDatabase(Object source, DatabaseSessionImpl session, ClassLoader loader) {
        if (source != null) {
            Reader reader = null;

            try {
                if (source instanceof Reader) {
                    reader = (Reader) source;
                } else if (source instanceof String) {
                    // Try to load the resource first, if not assume it as a well formed URL. If not, throw an exception.
                    URL sourceURL = loader.getResource((String) source);

                    if (sourceURL == null) {
                        try {
                            sourceURL = new URL((String) source);
                        }  catch (MalformedURLException e) {
                            throw new PersistenceException(ExceptionLocalization.buildMessage("jpa21-ddl-source-script-not-found", new Object[]{ source }), e);
                        }
                    }

                    URLConnection connection = sourceURL.openConnection();
                    // Set to false to prevent locking of jar files on Windows. EclipseLink issue 249664
                    connection.setUseCaches(false);
                    reader = new InputStreamReader(connection.getInputStream(), "UTF-8");
                } else {
                    throw new PersistenceException(ExceptionLocalization.buildMessage("jpa21-ddl-invalid-source-script-type", new Object[]{ source , source.getClass()}));
                }

                if (reader != null) {
                    StringBuffer sqlBuffer;
                    int data = reader.read();

                    // While there is still data to read, read line by line.
                    while (data != -1) {
                        sqlBuffer = new StringBuffer();
                        char aChar = (char) data;

                        // Read till the end of the line or maybe even file.
                        while (aChar != '\n' && data != -1) {
                            sqlBuffer.append(aChar);

                            // Read next character.
                            data = reader.read();
                            aChar = (char) data;
                        }

                        // Remove trailing and leading white space characters.
                        String sqlString = sqlBuffer.toString().trim();

                        // If the string isn't empty, then fire it.
                        if ((! sqlString.equals("")) && (! sqlString.startsWith("#"))) {
                            try {
                                session.executeNonSelectingSQL(sqlString);
                            } catch (DatabaseException e) {
                                // Swallow any database exceptions as these could
                                // be drop failures of a table that doesn't exist etc.
                                // SQLExceptions will be thrown to the user.
                            }
                        }

                        data = reader.read();
                    }

                    reader.close();
                }
            } catch (IOException e) {
                throw new PersistenceException(ExceptionLocalization.buildMessage("jpa21-ddl-source-script-io-exception", new Object[]{source}), e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        // ignore.
                    }
                }
            }
        }
    }
}

