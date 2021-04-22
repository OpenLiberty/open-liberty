/*******************************************************************************
 * Copyright (c) 1997, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.impl;

import java.io.IOException; 
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method; 
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException; 
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong; 
import java.util.concurrent.atomic.AtomicReference; 
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;
import javax.sql.CommonDataSource; 
import javax.sql.DataSource; 
import javax.sql.PooledConnection;
import javax.sql.XADataSource;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.ietf.jgss.GSSCredential;
import org.osgi.framework.Version;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.SecurityException;
import javax.resource.spi.ValidatingManagedConnectionFactory;
import javax.resource.spi.security.GenericCredential;
import javax.resource.spi.security.PasswordCredential;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.jca.adapter.WSConnectionManager;
import com.ibm.ws.jca.adapter.WSManagedConnectionFactory;
import com.ibm.ws.jca.cm.AbstractConnectionFactoryService;
import com.ibm.ws.jca.cm.ConnectorService;
import com.ibm.ws.jdbc.internal.PropertyService;
import com.ibm.ws.jdbc.osgi.JDBCRuntimeVersion;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.DSConfig;
import com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException;
import com.ibm.ws.rsadapter.jdbc.WSJdbcTracer;

/**
 * This class implements the javax.resource.spi.ManagedConnectionFactory interface. The instance
 * is a factory of both ManagedConnection and ConnectionFactory instances. Refer to the JCA spec
 * for detailed requirements of this class.
 * <p>
 * It is REQUIRED that immediately after creating the ManagedConnectionFactory, the setDataSourceProperties
 * method is called. This method is required as it creates the underlying DataSource needed for
 * relational access.
 * <p>
 * This class also provides a piece of the connection pooling mechanism. The matchManagedConnections()
 * method matches a candidate set of connections using the ConnectionRequestInfo and/or Subject as the
 * matching criteria. Part of the ConnectionSpec properties will be set in the
 * ConnectionRequestInfo object.
 */
@SuppressWarnings("deprecation")
public class WSManagedConnectionFactoryImpl extends WSManagedConnectionFactory implements
                ValidatingManagedConnectionFactory, FFDCSelfIntrospectable {
    private static final long serialVersionUID = -56589160441993572L;
    final static SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    /**
     * Count of initialized instances of this class.
     * 
     */
    private static final AtomicInteger NUM_INITIALIZED = new AtomicInteger();

    /**
     * Utility class with access to various core services.
     */
    public final ConnectorService connectorSvc;

    /**
     * A mapping of supported client information properties to their default values.
     * 
     */
    Properties defaultClientInfo = new Properties();

    /**
     * The underlying data source implementation from the JDBC vendor.
     * It could be a javax.sql.DataSource, javax.sql.ConnectionPoolDataSource, or javax.sql.XADataSource
     */
    transient private Object dataSourceOrDriver;

    /**
     * Class name of data source or driver implementation from the JDBC vendor.
     * It could be an implementation of javax.sql.DataSource, javax.sql.ConnectionPoolDataSource, javax.sql.XADataSource, or java.sql.Driver
     */
    transient Class<?> vendorImplClass;

    /**
     * The type of data source (for example, javax.sql.XADataSource) or java.sql.Driver that this managed connection factory uses to establish connections.
     */
    final Class<?> type;

    /**
     * Helps cope with differences between databases/JDBC drivers.
     */
    transient DatabaseHelper helper;

    /**
     * Unique identifier for this instance, which is used to match connection requests.
     */
    public final int instanceID;

    /**
     * Indicates if the data source is Oracle Universal Connection Pooling.
     */
    public final boolean isUCP;

    /**
     * Class loader that is capable of loading JDBC driver classes.
     */
    final ClassLoader jdbcDriverLoader;

    /**
     * The JDBC major and minor version, where the minor version is kept in the final digit.
     * For example: 42 means JDBC 4.2, 30 means JDBC 3.0, 20 means JDBC 2.0
     */
    public int jdbcDriverSpecVersion;

    /**
     * Implementation that depends on the JDBC version
     */
    public final JDBCRuntimeVersion jdbcRuntime;

    transient PrintWriter logWriter; 

    /** Indicates if any multithreaded access was detected. */
    public boolean detectedMultithreadedAccess;

    private static TraceComponent tc = Tr.register(WSManagedConnectionFactoryImpl.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * flag to indicate if logging enabled for this mcf.
     */
    boolean loggingEnabled; 

    /**
     * A counter of fatal connection errors that've occurred on connections created by this
     * ManagedConnectionFactory.
     */
    final AtomicLong fatalErrorCount = new AtomicLong();

    /**
     * Oracle RAC has an issue when running XA transactions that span multiple Oracle nodes,
     * and one of the nodes fails after a prepare happens. Oracle switches to the working
     * node before the transaction from the failing node is completely visible to the working
     * Oracle nodes, which results in indoubt transactions.
     * 
     * The data source property, oracleRACXARetryDelay, configures the amount of time after
     * detecting a stale connection during which we defer two-phase xa.commit or xa.rollback
     * requests made by the transaction manager. These XA operations are deferred by raising an
     * XAException with the XA_RETRY error code.
     * 
     */
    long oracleRACXARetryDelay; // TODO move to dsConfig if we want to support this

    /**
     * The time stamp of the most recently detected stale connection, or zero.
     * The value can be zero if no stale connections have been detected since the
     * oracleRACXARetryDelay last expired.
     * 
     */
    final AtomicLong oracleRACLastStale = new AtomicLong();

    /**
     * A warning was issued about using a nontransactional data source that isn't configured
     * to automatically commit or roll back transactions during cleanup.
     * 
     */
    boolean warnedAboutNonTransactionalDataSource;

    protected boolean loggedDbUowMessage = false; 
    protected boolean loggedImmplicitTransactionFound = false; 

    /**
     * Default to be used for transaction isolation level on new connections if neither the
     * resource reference nor the server (or app-defined resource) configuration specifies one.
     * Some of the database helpers and legacy data store helpers override this.
     */
    public int defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED;

    /**
     * Indicates if the application server should free java.sql.Array instances
     * on behalf of applications that forgot to clean them up.
     * 
     */
    public boolean doArrayCleanup = true;

    /**
     * Indicates if the application server should free java.sql.Blob instances
     * on behalf of applications that forgot to clean them up.
     * 
     */
    public boolean doBlobCleanup = true;

    /**
     * Indicates if the application server should free java.sql.Clob and java.sql.NClob instances
     * on behalf of applications that forgot to clean them up.
     * 
     */
    public boolean doClobCleanup = true;

    /**
     * Indicates if the application server should free java.sql.SQLXML instances
     * on behalf of applications that forgot to clean them up.
     * 
     */
    public boolean doXMLCleanup = true;

    /**
     * Indicates if statements retain their isolation level once created,
     * versus using whatever is currently on the connection when they run.
     */
    public boolean doesStatementCacheIsoLevel;

    /**
     * This reference should always be used when accessing configuration data,
     * in order to allow for our future implementation of dynamic updates.
     */
    public final AtomicReference<DSConfig> dsConfig;
    /**
     * Indicates whether or not the JDBC driver supports <code>java.sql.Connection.getCatalog()</code>.
     */
    public boolean supportsGetCatalog = true;

    /**
     * Indicates whether or not the JDBC driver supports <code>java.sql.Connection.getNetworkTimeout()</code>.
     */
    public boolean supportsGetNetworkTimeout;

    /**
     * Indicates whether or not the JDBC driver supports <code>java.sql.Connection.getSchema()</code>.
     */
    public boolean supportsGetSchema;

    /**
     * Indicates whether the JDBC driver supports <code>java.sql.Connection.getTypeMap()</code>.
     */
    boolean supportsGetTypeMap = true;

    /**
     * Indicates whether the JDBC driver supports <code>java.sql.Connection.isReadOnly()</code>.
     */
    boolean supportsIsReadOnly = true;

    /**
     * Indicates whether the JDBC driver supports <code>java.sql.Statement.getLargeUpdateCount()</code>.
     */
    public boolean supportsGetLargeUpdateCount = true;

    /**
     * Identifies whether or not unit-of-work detection API is supported by the backend database.
     */
    boolean supportsUOWDetection;

    /**
     * A set of known vendor-specific methods that are not found on the JDBC wrappers.
     * ConcurrentHashMap is used as a set to avoid locking for reads.
     * The keys in the map constitute the set. The values are ignored.
     * 
     * Methods are added to this set during runtime as they are invoked by an
     * application and the method is determined to be a vendor only method.
     * This speeds up use of the methods as they only need to be inspected the
     * first time they are encountered. Only "safe" methods will be added.
     */
    public final transient Set<Method> vendorMethods = Collections.newSetFromMap(new ConcurrentHashMap<Method, Boolean>());

    // Indicates whether the DataSource was used to get a connection.
    private boolean wasUsedToGetAConnection;
    
    static enum KerbUsage {
        NONE,
        USE_CREDENTIAL,
        SUBJECT_DOAS
    }

    /**
     * Constructs a managed connection factory based on configuration.
     * 
     * @param dsConfigRef reference to update to point at the new data source configuration.
     * @param ifc the type of data source, otherwise java.sql.Driver.
     * @param vendorImpl the data source or driver implementation.
     * @param jdbcRuntime version of the Liberty jdbc feature
     * @throws Exception if an error occurs.
     */
    public WSManagedConnectionFactoryImpl(AtomicReference<DSConfig> dsConfigRef, Class<?> ifc,
                                          Object vendorImpl, JDBCRuntimeVersion jdbcRuntime) throws Exception {
        dsConfig = dsConfigRef;
        DSConfig config = dsConfig.get();

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "<init>", dsConfigRef, ifc, jdbcRuntime);

        this.connectorSvc = config.connectorSvc;
        this.jdbcRuntime = jdbcRuntime;
        instanceID = NUM_INITIALIZED.incrementAndGet();
        vendorImplClass = vendorImpl.getClass();
        type = ifc;
        jdbcDriverLoader = priv.getClassLoader(vendorImplClass);

        String implClassName = vendorImplClass.getName();
        isUCP = implClassName.charAt(2) == 'a' && implClassName.startsWith("oracle.ucp.jdbc."); // 3rd char distinguishes from common names like: com, org, java

        createDatabaseHelper(config.vendorProps instanceof PropertyService ? ((PropertyService) config.vendorProps).getFactoryPID() : PropertyService.FACTORY_PID);

        if (connectorSvc.isHeritageEnabled()) {
            helper.createDataStoreHelper();
        } else {
            supportsGetNetworkTimeout = supportsGetSchema = atLeastJDBCVersion(JDBCRuntimeVersion.VERSION_4_1);
        }

        if (helper.shouldTraceBeEnabled(this))
            helper.enableJdbcLogging(this);

        if (config.supplementalJDBCTrace == null || config.supplementalJDBCTrace) {
            TraceComponent tracer = helper.getTracer();

            if (tracer != null && tracer.isDebugEnabled()) {
                try {
                    vendorImpl = getTraceable(vendorImpl);
                    Tr.debug(this, tc, "supplemental tracing set for data source or driver", vendorImpl, "Tracer: " + tracer);
                } catch (ResourceException e) {
                    Tr.debug(this, tc, "error setting supplemental trace on data source or driver", vendorImpl, e);
                }
            }
        }
        dataSourceOrDriver = vendorImpl;

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "<init>");
    }

    /**
     * Creates a javax.sql.DataSource that uses the application server provided
     * connection manager to manage its connections.
     * 
     * @param ConnectionManager connMgr - An application server provided ConnectionManager.
     * @return a new instance of WSJdbcDataSource or a subclass of it pertaining to a particular JDBC spec level.
     */
    @Override
    public final DataSource createConnectionFactory(ConnectionManager connMgr) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "createConnectionFactory", connMgr); 

        DataSource connFactory = jdbcRuntime.newDataSource(this, (WSConnectionManager) connMgr);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "createConnectionFactory", connFactory); 
        return connFactory;
    }

    public boolean atLeastJDBCVersion(Version ver) {
        return jdbcRuntime.getVersion().compareTo(ver) >= 0;
    }    

    public final boolean beforeJDBCVersion(Version ver) {
        return jdbcRuntime.getVersion().compareTo(ver) < 0;
    }

    /**
     * The Liberty profile does not support using the RRA without JCA.
     */
    public final Object createConnectionFactory() throws ResourceException {
        throw new NotSupportedException();
    }

    private void createDatabaseHelper(String vPropsPid) throws Exception {
        // Data store helper class is inferred first based on the data source or driver implementation class,
        // except for DB2 JCC and Informix JCC, which have the same data source/driver implementation classes.
        // Second, we look at the type of vendor properties list to further distinguish.
        String dsClassName = vendorImplClass.getName();
        if (dsClassName.startsWith("oracle.")) helper = new OracleHelper(this);
        else if (dsClassName.startsWith("com.ibm.as400.")) helper = new DB2iToolboxHelper(this);
        else if (dsClassName.startsWith("com.ibm.db2.jdbc.app.")) helper = new DB2iNativeHelper(this);
        else if (dsClassName.startsWith("com.microsoft.")) helper = new MicrosoftSQLServerHelper(this);
        else if (dsClassName.startsWith("com.ddtek.jdbcx.sqlserver.")) helper = new DataDirectConnectSQLServerHelper(this);
        else if (dsClassName.startsWith("com.informix.")) helper = new InformixHelper(this);
        else if (dsClassName.startsWith("com.sybase.")) helper = new SybaseHelper(this);
        else if (dsClassName.startsWith("org.apache.derby.jdbc.Client")) helper = new DerbyNetworkClientHelper(this);
        else if (dsClassName.startsWith("org.apache.derby.jdbc.Embedded")) helper = new DerbyHelper(this);
        else if (dsClassName.startsWith("org.postgresql.")) helper = new PostgreSQLHelper(this);
        else if (vPropsPid.length() > PropertyService.FACTORY_PID.length()) {
            String suffix = vPropsPid.substring(PropertyService.FACTORY_PID.length() + 1);
            if (suffix.startsWith("oracle")) helper = new OracleHelper(this);
            else if (suffix.startsWith("db2.jcc")) helper = new DB2JCCHelper(this);
            else if (suffix.startsWith("db2.i.native")) helper = new DB2iNativeHelper(this);
            else if (suffix.startsWith("db2.i.toolbox")) helper = new DB2iToolboxHelper(this);
            else if (suffix.startsWith("microsoft.sqlserver")) helper = new MicrosoftSQLServerHelper(this);
            else if (suffix.startsWith("datadirect.sqlserver")) helper = new DataDirectConnectSQLServerHelper(this);
            else if (suffix.startsWith("informix.jcc")) helper = new InformixJCCHelper(this);
            else if (suffix.startsWith("informix")) helper = new InformixHelper(this);
            else if (suffix.startsWith("sybase")) helper = new SybaseHelper(this);
            else if (suffix.startsWith("derby.client")) helper = new DerbyNetworkClientHelper(this);
            else if (suffix.startsWith("derby.embedded")) helper = new DerbyHelper(this);
            else if (suffix.startsWith("postgresql")) helper = new PostgreSQLHelper(this);
        }

        if (helper == null)
            if (dsClassName.startsWith("com.ibm.db2.jcc.")) helper = new DB2JCCHelper(this); // unable to distinguish between DB2/Informix
            else helper = new DatabaseHelper(this);
    }

    /**
     * Creates a new physical connection to the underlying resource manager,
     * ManagedConnectionFactory uses the security information (passed as Subject) and
     * additional ConnectionRequestInfo (which is specific to ResourceAdapter and opaque
     * to application server) to create this new connection.
     * <p>
     * The DataSource used to create the physical connection will be created if it does not
     * already exist using the dataSourceProperties. Then, a java.sql.Connection is obtained
     * from the dataSource. If the DataSource if of type XADataSource, the ManagedConnection
     * will be passed in the XAConnection and the Connection in the constructor. If it is of
     * type ConnectionPoolDataSource, the ManagedConnection will be passed in only the
     * Connection in the constructor.
     * 
     * @param Subject subject - Caller's security information
     * @param ConnetionRequestInfo cxRequestInfo - Additional resource adapter specific connection request
     *            information
     * @return ManagedConnection instance
     * @exception ResourceException - Possible causes for this exception are:
     *                1) getting a connection from the underlying datasource failed with a SQLException
     * @exception SecurityException - security related error
     * @exception ResourceAllocationException - failed to allocate system resources
     *                for connection request
     * @exception ResourceAdapterInternalException - Possible causes for this exception are:
     *                1) there is a missing ConnectionRequestInfo
     * @exception EISSystemException - internal error condition in EIS instance
     */

    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo)
                    throws ResourceException {
        boolean useKerb = false; // flag to indicate if kerberso is being used
        Object credential = null; 
        // cannot print the subject - causes a security violation 
        final boolean isAnyTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isAnyTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "createManagedConnection", subject == null ? null : "subject", AdapterUtil.toString(cxRequestInfo));


        // User/password from the Subject will take first priority over any values specified in
        // the CRI, as required by the JCA spec.  If neither the Subject nor the CRI contain a
        // user/password, then the backend database is allowed to reject or accept the
        // connection request. 

        // If the Subject is not null but it is empty, then we should use the backend default user/pwd.
        // we should not use the one from CRI. 

        // If we don't have a CRI, then create a blank one. A non-null CRI is required later.
        // If we do have a CRI, the values in the CRI take first priority--unless they're null.

        WSConnectionRequestInfoImpl cri = cxRequestInfo == null ?
                        new WSConnectionRequestInfoImpl() :
                        (WSConnectionRequestInfoImpl) cxRequestInfo;

        String userName = null; 
        String password = null; 

        if (subject == null) // Check the Subject first, then the CRI values. 
        {

            password = cri.getPassword(); 
            userName = cri.getUserName(); 

            if (isAnyTraceOn && tc.isEventEnabled()) 
            {
                if (userName == null) // Distinguish between option C/using DS defaults. 
                {
                    //  For recovery, we need to be able to accept if there is no CRI or Subject.
                    //  In this case, we will use the username/password on the dataSource to get
                    //  the connection.

                    Tr.event(this, tc, "Using DataSource default user/password for authentication");

                    //Leave username and password as null here.  Then, in the code below, use
                    // the null value to determine how to get the JTA/Pooled Connection.  If
                    // we try to use null, null as the username/password, the AS/400 driver
                    // will have problems.
                } else {
                    // Option C security
                    // We use the non-null user/password found in the CRI

                    Tr.event(this, tc, "Using ConnectionRequestInfo for authentication");
                }
            }
        } else // Subject is available
        {
            // Option A and B security

            if (isAnyTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "Subject found.  Will try to use either PasswordCredentials or GenericCredentials ");

            // Set default user/password that will be used     
            // if the subject has no credentials               

            userName = null; 
            password = null; 

            // If ThreadIdentitySupport for this JDBC provider        
            // is ALLOWED or REQUIRED, check if the Subject has a     
            // UTOKEN generic credential. If it does, the userid and  
            // password values will be forced to null so the datasource 
            // will default to use the userid associated with the     
            // current OS thread (i.e., the userid in the ACEE) as the
            // user that owns the connection. In this case, processing
            // checks for Basic Password Credential and for Kerberos  
            // will be skipped.                                       

            int threadIdentitySupport = helper.getThreadIdentitySupport();
            boolean subjectHasUtokenCred = false; 

            if (threadIdentitySupport != AbstractConnectionFactoryService.THREAD_IDENTITY_NOT_ALLOWED) 
            {
                if (isAnyTraceOn && tc.isDebugEnabled())
                    Tr.debug(this, tc, "The JDBC Provider supports the use of Thread Identity for authentication."); 

                // Setup to search credentials for UTOKEN Generic Credential 

                // If Java 2 Security is enabled, wrap the call to getPrivateCredentials() in a
                // doPrivileged() to prevent the calling classes from having to hold the
                // permissions necessary to access the credentials
                final Subject subj = subject; 
                Set<GenericCredential> privateGenericCredentials = null; 
                if (System.getSecurityManager() != null) 
                {
                    privateGenericCredentials = AccessController.doPrivileged(
                                    new PrivilegedAction<Set<GenericCredential>>() { 
                                        public Set<GenericCredential> run() 
                                        { 
                                            return subj.getPrivateCredentials(GenericCredential.class); 
                                        } 
                                    }); 
                } 
                else 
                {
                    privateGenericCredentials = subj.getPrivateCredentials(GenericCredential.class); 

                }

                final Iterator<GenericCredential> iterator = privateGenericCredentials.iterator(); 

                PrivilegedAction<GenericCredential> iterationAction = new PrivilegedAction<GenericCredential>() {
                    public GenericCredential run() {
                        return iterator.next();
                    } 
                }; 

                // Search credentials for UTOKEN Generic Credential     

                GenericCredential genericCredential = null; 

                while (iterator.hasNext()) 
                {
                    if (System.getSecurityManager() != null) // Java2 Sec? 
                    {
                        genericCredential = (GenericCredential) java.security.AccessController.doPrivileged(iterationAction); 
                    } 
                    else // Java 2 Security Not Enabled
                    {
                        genericCredential = (GenericCredential) iterator.next();
                    }

                    if (genericCredential.getMechType().equals("oid:1.3.18.0.2.30.1")) 
                    {
                        subjectHasUtokenCred = true;
                        break;
                    }
                }
                if (!subjectHasUtokenCred && threadIdentitySupport == AbstractConnectionFactoryService.THREAD_IDENTITY_REQUIRED) {
                    String message = "createManagedConnection() error: Jdbc Provider requires ThreadIdentitySupport, but no UTOKEN generic credential was found."; 

                    ResourceException resX = new DataStoreAdapterException("WS_INTERNAL_ERROR", null, getClass(), message);

                    throw resX;
                }
            } // end if threadIdentitySupport equals "ALLOWED" or "REQUIRED"

            if (subjectHasUtokenCred) 
            {
                // Force userName and password to null so the datasource
                // will use the OS thread security the ConnectionManager
                // established for the current thread.                  

                userName = null; 
                password = null; 
                if (isAnyTraceOn && tc.isEventEnabled()) 
                    Tr.event(this, tc, "Using thread identity for authentication by the JDBC Provider's DataSource."); 
            } 
            else // Check for PasswordCredential or Kerbros GenericCredential // also check for IdentityPrincipal
            {
                // This list of credentials may have
                // [A] PasswordCredentials for this ManagedConnection instance, or
                // [B] GenericCredentials for use with Kerberos support, or
                // [C] KerberosTicket for use with Kerberos
                // no credentials at all.
                
                final Iterator<Object> iter = subject.getPrivateCredentials().iterator();

                PrivilegedAction<Object> iterationAction = new PrivilegedAction<Object>() {
                    public Object run() {
                        return iter.next();
                    }
                };

                PasswordCredential pwcred;

                while (iter.hasNext()) {
                    // For performance, perform doPrivileged only 
                    // if Java 2 security is enabled.             
                    if (System.getSecurityManager() != null) 
                    {
                        credential = AccessController.doPrivileged(iterationAction);
                    } 
                    else // Java 2 Secutity Not Enabled
                    {
                        credential = iter.next(); 
                    } 
                    
                    if (credential instanceof PasswordCredential) {
                        //This is possibly Option A - only possibly because the PasswordCredential
                        // may not match the MC.  Then we have to keep looping.
                        pwcred = (PasswordCredential) credential;
                        if ((pwcred.getManagedConnectionFactory()).equals(this)) {
                            if (isAnyTraceOn && tc.isEventEnabled()) 
                                Tr.event(this, tc, "Using PasswordCredentials for authentication");
                            userName = pwcred.getUserName();

                            // Allow for the possibility of a null password. 
                            char[] pwdChars = pwcred.getPassword();

                            password = pwdChars == null ? null : new String(pwdChars);
                            break;
                        }
                    } else if (credential instanceof GSSCredential) {
                        useKerb = true;
                        //This is option B
                        if (isAnyTraceOn && tc.isEventEnabled()) 
                            Tr.event(this, tc, "Using GSSCredential for authentication");
                        break;
                    }
                }
            } //end else Check for PasswordCredential or Kerbros GenericCredential
        } // end Subject is available

        WSRdbManagedConnectionImpl mc = null;
        ConnectionResults results = null; 
        try {
            KerbUsage kerbUsage = useKerb ? KerbUsage.USE_CREDENTIAL : KerbUsage.NONE;
            results = getConnection(userName, password, subject, cri, kerbUsage, credential);

            mc = new WSRdbManagedConnectionImpl(this, results.pooledConnection, results.connection, subject, cri);

            if (useKerb)
                mc.setKerberosConnection();

            if (isAnyTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "createManagedConnection", mc); 

            return mc;
        } catch (ResourceException resX) {
            if (isAnyTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "createManagedConnection", resX);
            throw resX;
        } finally {
            // Close connections if we failed to create the managed connection
            if (mc == null && results != null) {
                if (results.connection != null)
                    try {
                        results.connection.close();
                    } catch (Throwable x) {}
                if (results.pooledConnection != null)
                    try {
                        results.pooledConnection.close();
                    } catch (Throwable x) {}
            }
        }
    }

    /**
     * Gets a java.sql.Connection from a PooledConnection, use the cri to extract certain information
     * if needed, if trused context is supported, ...
     * 
     * @param PooledConnection pconn - the PooledConnection
     * @param WSConnectionRequestInfoImpl -- the cri
     * @param userName the user name for the connection, or NULL if unspecified.
     * @return java.sql.Connection - Connection to the database
     * @exception ResouceException - Possible causes for this error are:
     *                1) the database threw an SQLException on PooledConnection.getConnection();
     *                2) there was an SQLException when setting a default property on the Connection
     */
    private Connection getConnection(PooledConnection pconn, WSConnectionRequestInfoImpl cri, String userName) throws ResourceException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "getConnection", AdapterUtil.toString(pconn));

        Connection conn = null;
        boolean isConnectionSetupComplete = false;
        try {
            conn = pconn.getConnection();
            postGetConnectionHandling(conn);
            isConnectionSetupComplete = true;
        } catch (SQLException se) {
            FFDCFilter.processException(se, getClass().getName(), "260", this);
            ResourceException x = AdapterUtil.translateSQLException(se, this, false, getClass());
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getConnection", se);
            throw x;
        } finally {
            // Destroy the connection if we weren't able to successfully complete the setup.
            if (!isConnectionSetupComplete) {
                if (conn != null)
                    try {
                        conn.close();
                    } catch (Throwable x) {
                    }
                if (pconn != null)
                    try {
                        pconn.close();
                    } catch (Throwable x) {
                    }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "getConnection", AdapterUtil.toString(conn));
        return conn;
    }

    /**
     * Utility method to establish pooled connection and connection.
     * 
     * @return the pooled connection, cookie, and connection
     */
    private ConnectionResults getConnection(final String userName, final String password,
                                            final Subject subject, final WSConnectionRequestInfoImpl cri, KerbUsage useKerb,
                                            final Object credential)
                    throws ResourceException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "getConnection",
                     userName, subject == null ? null : "subject", AdapterUtil.toString(cri),
                     useKerb, credential);

        ConnectionResults results;
        Connection conn;

        // Some JDBC drivers support propagation of GSS credential for kerberos via Subject.doAs
        if (useKerb == KerbUsage.USE_CREDENTIAL && helper.supportsSubjectDoAsForKerberos()) {
            try {
                // Run this method as the subject.

                final PrivilegedExceptionAction<ConnectionResults> getConnection =
                                new PrivilegedExceptionAction<ConnectionResults>()
                                {
                                    public ConnectionResults run() throws ResourceException
                                    {
                                        return getConnection(userName, password, subject, cri, KerbUsage.SUBJECT_DOAS, credential);
                                    }
                                };

                results = (ConnectionResults) AccessController.doPrivileged(
                                new PrivilegedExceptionAction<ConnectionResults>()
                                {
                                    public ConnectionResults run() throws Exception
                                    {
                                        // Work around a bug in JGSS where they add the kerberos ticket to the
                                        // private credentials, which prevents the subject from matching
                                        // connection requests and causes pooled connection to never be reused.
                                        // The work around is to copy the subject, so that the original isn't
                                        // modified.
                                        Subject copy = new Subject(subject.isReadOnly(),
                                                        subject.getPrincipals(),
                                                        subject.getPublicCredentials(),
                                                        subject.getPrivateCredentials());
                                        if (trace && tc.isDebugEnabled())
                                            Tr.debug(this, tc, "running as subject", copy);
                                        return Subject.doAs(copy, getConnection);
                                    }
                                });
            } catch (PrivilegedActionException privX) {
                Throwable x = privX.getCause();
                if (x instanceof PrivilegedActionException)
                    x = x.getCause();
                FFDCFilter.processException(x, getClass().getName(), "1734", this);
                if (trace && tc.isEntryEnabled())
                    Tr.exit(this, tc, "getConnection", x);

                if (x instanceof ResourceException)
                    throw (ResourceException) x;
                else
                    // shouldn't ever happen
                    throw new DataStoreAdapterException("GENERAL_EXCEPTION", null, getClass(), x.getMessage());
            }
        } else if (DataSource.class.equals(type))
        {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "Getting a connection using Datasource. Is UCP? " + isUCP);
            conn = getConnectionUsingDS(userName, password, cri, useKerb, credential);
            results = new ConnectionResults(null, conn);
        } else if (Driver.class.equals(type)) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "Getting a connection using Driver");
            conn = getConnectionUsingDriver(userName, password);
            results = new ConnectionResults(null, conn);
        } else {
            try {
                results = helper.getPooledConnection((CommonDataSource) dataSourceOrDriver, userName, password, 
                                                     XADataSource.class.equals(type), cri, useKerb, credential);
            } catch (DataStoreAdapterException dae) {
                throw (ResourceException) AdapterUtil.mapException(dae, null, this, false); // error can't be fired as we don't have an mc
            }
            results.connection = getConnection(results.pooledConnection, cri, userName);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "getConnection", results);
        return results;
    }
    
    /**
     * Get a Connection from a DriverManager. A null userName
     * indicates that no user name or password should be provided.
     * 
     * @param userN the user name for obtaining a Connection, or null if none.
     * @param password the password for obtaining a Connection.
     * @param cri optional information for the connection request
     * 
     * @return a Connection.
     * @throws ResourceException
     */
    private final Connection getConnectionUsingDriver(String userN, String password) throws ResourceException 
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "getConnectionUsingDriver", AdapterUtil.toString(dataSourceOrDriver), userN);

        final String user = userN == null ? null : userN.trim();
        final String pwd = password == null ? null : password.trim();

        Connection conn = null;
        boolean isConnectionSetupComplete = false;
        try {
            conn = AccessController.doPrivileged(new PrivilegedExceptionAction<Connection>() {
                public Connection run() throws Exception {
                    Hashtable<?, ?> vProps = dsConfig.get().vendorProps;
                    Properties conProps = new Properties();
                    String url = null;
                    // convert property values to String and decode passwords 
                    for (Map.Entry<?, ?> prop : vProps.entrySet()) {
                        String name = (String) prop.getKey();
                        Object value = prop.getValue();
                        if (value instanceof String) {
                            String str = (String) value;
                            if ("URL".equals(name) || "url".equals(name)) {
                                url = str;
                                if (isTraceOn && tc.isDebugEnabled())
                                    Tr.debug(this, tc, name + '=' + PropertyService.filterURL(str));
                            } else if ((user == null || !"user".equals(name)) && (pwd == null || !"password".equals(name))) {
                                // Decode passwords
                                if (PropertyService.isPassword(name)) {
                                    if (isTraceOn && tc.isDebugEnabled())
                                        Tr.debug(this, tc, "prop " + name + '=' + (str == null ? null : "***"));
                                    if (PasswordUtil.getCryptoAlgorithm(str) != null) {
                                        str = PasswordUtil.decode(str);
                                    }
                                } else {
                                    if (isTraceOn && tc.isDebugEnabled())
                                        Tr.debug(this, tc, "prop " + name + '=' + str);
                                }
                                conProps.setProperty(name, str);
                            }
                        } else {
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(this, tc, "prop " + name + '=' + value);
                            // Convert to String value
                            conProps.setProperty(name, value.toString());
                        }
                    }

                    if (user != null) {
                        conProps.setProperty("user", user);
                        conProps.setProperty("password", pwd);
                        if (isTraceOn && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "prop user=" + user);
                            Tr.debug(this, tc, "prop password=" + (pwd == null ? null : "***"));
                        }
                    }
                    Connection conn = ((Driver) dataSourceOrDriver).connect(url, conProps);
                    
                    //Although possible, this shouldn't happen since the JDBC Driver has indicated it can accept the URL
                    if(conn == null) {
                        //return beginning on JDBC url (ex jdbc:db2:
                        String urlPrefix = "";
                        int first = url.indexOf(":");
                        int second = url.indexOf(":", first+1);
                        if(first < 0 || second < 0) {
                            urlPrefix = url.substring(0, 10);
                        } else {
                            urlPrefix = url.substring(0, second);
                        }
                        throw new ResourceException(AdapterUtil.getNLSMessage("DSRA4006.null.connection", dsConfig.get().id, vendorImplClass.getName(), urlPrefix));
                    }
                    return conn;
                }
            });

            try {
                postGetConnectionHandling(conn);
            } catch (SQLException se) {
                FFDCFilter.processException(se, getClass().getName(), "871", this);
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "getConnectionUsingDriver", se);
                throw AdapterUtil.translateSQLException(se, this, false, getClass()); 
            }

            isConnectionSetupComplete = true;
        } catch (PrivilegedActionException pae) {
            FFDCFilter.processException(pae.getException(), getClass().getName(), "879");
            ResourceException resX = new DataStoreAdapterException("JAVAX_CONN_ERR", pae.getException(), getClass(), "Connection");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getConnectionUsingDriver", pae.getException());
            throw resX;
        } catch (ClassCastException castX) {
            // There's a possibility this occurred because of an error in the JDBC driver
            // itself.  The trace should allow us to determine this.
            FFDCFilter.processException(castX, getClass().getName(), "887");
            ResourceException resX = new DataStoreAdapterException("NOT_A_1_PHASE_DS", null, getClass(), castX.getMessage());
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getConnectionUsingDriver", castX);
            throw resX;
        } finally {
            // Destroy the connection if we weren't able to successfully complete the setup.
            if (conn != null && !isConnectionSetupComplete)
                try {
                    conn.close();
                } catch (Throwable x) {
                }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "getConnectionUsingDriver", AdapterUtil.toString(conn));
        return conn;
    }

    /**
     * Get a Connection from a DataSource based on what is in the cri. A null userName
     * indicates that no user name or password should be provided.
     * 
     * @param userN the user name for obtaining a Connection, or null if none.
     * @param password the password for obtaining a Connection.
     * @param cri optional information for the connection request
     * 
     * @return a Connection.
     * @throws ResourceException
     */
    private final Connection getConnectionUsingDS(String userN, String password, final WSConnectionRequestInfoImpl cri,
                                                  KerbUsage useKerb, Object gssCredential) throws ResourceException 
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "getConnectionUsingDS", AdapterUtil.toString(dataSourceOrDriver), userN);

        final String user = userN == null ? null : userN.trim();
        final String pwd = password == null ? null : password.trim();

        Connection conn = null;
        boolean isConnectionSetupComplete = false;
        try {
            conn = AccessController.doPrivilegedWithCombiner(new PrivilegedExceptionAction<Connection>() {
                public Connection run() throws Exception {
                    boolean buildConnection = cri.ivShardingKey != null || cri.ivSuperShardingKey != null;
                    if (!buildConnection && dataSourceOrDriver instanceof XADataSource) {
                        // TODO this code path is very suspect, and so we are not continuing it for connection builder path.
                        // Why convert what was requested to be a DataSource to XADataSource?
                        // And then it also leaks the XAConnection. It is not tracked, so it never gets closed! 
                        return user == null ? ((XADataSource) dataSourceOrDriver).getXAConnection().getConnection()
                                            : ((XADataSource) dataSourceOrDriver).getXAConnection(user, pwd).getConnection();
                    } else {
                        if (buildConnection) {
                            return jdbcRuntime.buildConnection((DataSource) dataSourceOrDriver, user, pwd, cri);
                        } else if (user == null) {
                            return helper.getConnectionFromDatasource((DataSource) dataSourceOrDriver, useKerb, gssCredential);
                        } else {
                            return ((DataSource) dataSourceOrDriver).getConnection(user, pwd);
                        }
                    }
                }
            });

            try {
                postGetConnectionHandling(conn);
            } catch (SQLException se) {
                FFDCFilter.processException(se, getClass().getName(), "260", this);
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, "getConnectionUsingDS", se);
                throw AdapterUtil.translateSQLException(se, this, false, getClass()); 
            }

            isConnectionSetupComplete = true;
        } catch (PrivilegedActionException pae) {
            FFDCFilter.processException(pae.getException(), getClass().getName(), "1372");
            ResourceException resX = new DataStoreAdapterException("JAVAX_CONN_ERR", pae.getException(), getClass(), "Connection");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getConnectionUsingDS", pae.getException());
            throw resX;
        } catch (ClassCastException castX) {
            // There's a possibility this occurred because of an error in the JDBC driver
            // itself.  The trace should allow us to determine this.
            FFDCFilter.processException(castX, getClass().getName(), "1312");
            ResourceException resX = new DataStoreAdapterException("NOT_A_1_PHASE_DS", null, getClass(), castX.getMessage());
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "getConnectionUsingDS", castX);
            throw resX;
        } finally {
            // Destroy the connection if we weren't able to successfully complete the setup.
            if (conn != null && !isConnectionSetupComplete)
                try {
                    conn.close();
                } catch (Throwable x) {
                }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "getConnectionUsingDS", AdapterUtil.toString(conn));
        return conn;
    }

    /**
     * This method returns a set of bad ManagedConnections found in the specified set.
     * 
     * @param connectionSet a set of ManagedConnections to validate.
     * 
     * @return a set of bad ManagedConnections.
     * 
     * @throws ResourceException if an error occurs validating the connections.
     */
    // The spec interface is defined with raw types, so we have no choice but to declare it that way, too
    @Override
    public Set<ManagedConnection> getInvalidConnections(@SuppressWarnings("rawtypes") Set connectionSet) throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "getInvalidConnections", connectionSet);

        Set<ManagedConnection> badSet = new HashSet<ManagedConnection>();
        WSRdbManagedConnectionImpl mc = null;

        // Loop through each ManagedConnection in the list, using each connection's
        // preTestSQLString to check validity. Different connections can
        // have different preTestSQLStrings if they were created by different
        // ManagedConnectionFactories.

        for (Iterator<?> it = connectionSet.iterator(); it.hasNext();) {
            mc = (WSRdbManagedConnectionImpl) it.next();
            if (!mc.validate())
                badSet.add(mc);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "getInvalidConnections", badSet); 
        return badSet;
    }

    /**
     * Enable supplemental tracing for the underlying data source or java.sql.Driver.
     * 
     * @param d the underlying data source or driver.
     * 
     * @return a data source or driver enabled for supplemental trace.
     * @throws ResourceException if an error occurs obtaining the print writer.
     */
    private Object getTraceable(Object d) throws ResourceException {
        WSJdbcTracer tracer = new WSJdbcTracer(helper.getTracer(), helper.getPrintWriter(), d, type, null, true);

        Set<Class<?>> classes = new HashSet<Class<?>>();
        for (Class<?> cl = d.getClass(); cl != null; cl = cl.getSuperclass())
            classes.addAll(Arrays.asList(cl.getInterfaces()));
        
        return Proxy.newProxyInstance(jdbcDriverLoader,
                                      classes.toArray(new Class[classes.size()]),
                                      tracer);
    }

    /**
     * Retrieve the underlying DataSource for this ManagedConnectionFactory.
     * 
     * @return the underlying DataSource, null if a Driver is used
     */
    public CommonDataSource getUnderlyingDataSource()
    {
        if(Driver.class.equals(type))
            return null;
        return (CommonDataSource) dataSourceOrDriver;
    }

    /**
     * This method is called by the J2c only when getting connections from the free pool. //GAD comment to clarify
     * This method is called in addition to comparing the hashcode of cir and subject. //GAD comment to clarify
     * Returns a matched connection from the candidate set of connections.
     * ManagedConnectionFactory uses the security info (as in Subject) and information
     * provided through ConnectionRequestInfo and additional Resource Adapter
     * specific criteria to do matching.
     * <p>
     * This method returns a ManagedConnection instance that is the best match for
     * handling the connection allocation request. If no match connection is found,
     * a NULL value is returned.
     * </p>
     * <p>
     * If Reauthentication datasource custom property is enabled, then subject and username/password
     * are ignored when matching
     * </p>
     * <p>
     * The RRA implementation of this method returns the first ManagedConnection we can find
     * which can be made to match the Subject and ConnectionRequestInfo. RRA does not support
     * reauthentication with a different user/password. However, RRA does support changing all
     * of the other connection properties: catalog, isolationLevel, isReadOnly, typeMap.
     * The actual changes to the ManagedConnection occur on the MC.getConnection(subject, cri)
     * request for the first connection handle. 
     * </p>
     * 
     * @param Set connectionSet - candidate connection set
     * @param Subject subject - caller's security information
     * @param ConnectionRequestInfo cxRequestInfo - additional resource adapter specific connection request
     *            information - this is required to be non null
     * @return a ManagedConnection if resource adapter finds an acceptable match otherwise null
     * @exception ResourceException - generic exception
     * @exception ResourceAdapterInternalException - resource adapter related error
     *                condition
     */
    public ManagedConnection matchManagedConnections(@SuppressWarnings("rawtypes") final Set connectionSet,
                                                     final Subject subject, ConnectionRequestInfo cxRequestInfo)
                    throws ResourceException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); 

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "matchManagedConnections", connectionSet == null ? null : connectionSet.size(),
                            subject == null ? null : "subject", cxRequestInfo);

        // Return null (no match) if null is specified for the set of connections. 

        if (connectionSet == null) 
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "matchManagedConnections", null); 
            return null; 
        }

        // Convert the CRI to the RRA implementation class.  If the CRI doesn't convert, then
        // we cannot find a match. 

        final WSConnectionRequestInfoImpl finalCRI;

        try {
            finalCRI = (WSConnectionRequestInfoImpl) cxRequestInfo;
        } catch (ClassCastException castX) {
            // No FFDC code needed. If it's not the RRA's CRI class, then it doesn't match.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(this, tc, "matchManagedConnections", null); 
            return null;
        }

        // Need to keep this variable because currentmc will end up not being null but
        // may not be a match
        WSRdbManagedConnectionImpl matchedmc = null;

        // the J2c component will always send us a set of one and only one mc.  Therefore, there
        // is no need to go through a loop. Just far sanity check, i am printing the size in a debug
        if (isTraceOn && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "the size of the set should be 1, and it is", connectionSet.size()); 

        WSRdbManagedConnectionImpl currentmc;
        Iterator<?> iter = connectionSet.iterator();
        currentmc = (WSRdbManagedConnectionImpl) iter.next();

        //: if the mc is claimed victim, means it never matched the initial hash test done by j2c,
        // so at this point, we know that reauthentication is enabled and connection needs to be
        // reauthenticated, so ignore the subject and do the isReconfigurable checkign only.
        if (currentmc._claimedVictim && finalCRI.isReconfigurable(currentmc.cri, true)) {
            if (isTraceOn && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "mc is claimedVictim matched", currentmc); 
            matchedmc = currentmc;
        } else if (subject == null) // no reauthentication is enabled from here on
        {

            // Compare using .isReconfigurable instead of .equals in order to return
            // any ManagedConnection that can be made to match the CRI instead of only those
            // ManagedConnections which already match. 
            // The DataSource Config ID field of the CRI is not considered reconfigurable
            // for a connection. 
            if ((currentmc.getSubject() == null) && 
                finalCRI.isReconfigurable(currentmc.cri, false)) 
            {
                if (isTraceOn && tc.isDebugEnabled()) 
                    Tr.debug(this, tc, "mc matched", currentmc);
                matchedmc = currentmc;
            }
        } else {
            Subject _sub = currentmc.getSubject(); 

            if (_sub != null) 
            {
                Equals e = new Equals(); 
                e.setSubjects(subject, _sub);
                if (((Boolean) (AccessController.doPrivileged(e))).booleanValue() &&
                    finalCRI.isReconfigurable(currentmc.cri, false)) {
                    if (isTraceOn && tc.isDebugEnabled()) 
                        Tr.debug(this, tc, "mc matched", currentmc);
                    matchedmc = currentmc;
                }
            }
        }

        //Removed a call to matchedmc.setAutoCommit(defaultAutoCommit)
        //  because it should now be reset before putting it back into
        //  the pool

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "matchManagedConnections", matchedmc);

        return matchedmc;
    }

    /**
     * Execute the onConnect SQL commands. The connection won't be enlisted in a WAS transaction yet,
     * but it's necessary to suspend and WAS global transaction in order to avoid confusing the
     * DB2 type 2 JDBC driver.
     * 
     * @param con the connection
     * @param sqlCommands ordered list of SQL commands to run on the connection.
     * @throws SQLException if any of the commands fail.
     */
    private void onConnect(Connection con, String[] sqlCommands) throws SQLException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        TransactionManager tm = connectorSvc.getTransactionManager();
        Transaction suspendedTx = null;
        String currentSQL = null;
        Throwable failure = null;
        try {
            UOWCoordinator coord = tm == null ? null : ((UOWCurrent) tm).getUOWCoord();
            if (coord != null && coord.isGlobal())
                suspendedTx = tm.suspend();

            Statement stmt = con.createStatement();
            for (String sql : sqlCommands) {
                currentSQL = sql;
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "execute onConnect SQL", sql);
                stmt.execute(sql);
            }
            stmt.close();
        } catch (Throwable x) {
            failure = x;
        }
        if (suspendedTx != null) {
            try {
                tm.resume(suspendedTx);
            } catch (Throwable x) {
                failure = x;
            }
        }

        if (failure != null) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "failed", AdapterUtil.stackTraceToString(failure));
            throw new SQLNonTransientConnectionException(
                AdapterUtil.getNLSMessage("DSRA4004.onconnect.sql", currentSQL, dsConfig.get().id), "08000", 0, failure);
        }
    }

    /**
     * utility used to gather metadata info and issue doConnectionSetup.
     */
    private void postGetConnectionHandling(Connection conn) throws SQLException {
        helper.doConnectionSetup(conn);

        String[] sqlCommands = dsConfig.get().onConnect;
        if (sqlCommands != null && sqlCommands.length > 0)
            onConnect(conn, sqlCommands);

        // Log the database and driver versions on first getConnection.
        if (!wasUsedToGetAConnection) {
            // Wait until after the connection succeeds to set the indicator.
            // This accounts for the scenario where the first connection attempt is bad.
            // The information needs to be read again on the second attempt.
            helper.gatherAndDisplayMetaDataInfo(conn, this);

            // If a legacy data store helper is used, allow it to determine the unit-of-work detection support:
            if (helper.dataStoreHelper != null)
                helper.initUOWDetection();

            wasUsedToGetAConnection = true;
        }
    }

    /**
     * Set the log writer for this ManagedConnectionFactory instance.
     * <p>
     * The log writer is a character output stream to which all logging and tracing
     * messages for this ManagedConnectionfactory instance will be printed.
     * <p>
     * ApplicationServer manages the association of output stream with the
     * ManagedConnectionFactory. When a ManagedConnectionFactory object is created the
     * log writer is initially null, in other words, logging is disabled. Once a log
     * writer is associated with a ManagedConnectionFactory, logging and tracing for
     * ManagedConnectionFactory instance is enabled.
     * <p>
     * The ManagedConnection instances created by ManagedConnectionFactory "inherits"
     * the log writer, which can be overridden by ApplicationServer using
     * ManagedConnection.setLogWriter to set ManagedConnection specific logging and
     * tracing.
     * 
     * @param PrintWriter out - an out stream for error logging and tracing
     * @exception ResourceException - Possible causes for this exception are:
     *                1) setLogWriter on the physical datasource fails
     */

    public final void setLogWriter(final PrintWriter out) throws ResourceException {
        // removed content of this method.  This method is typically called by J2c.  J2c had some special
        // logic in their code not to call this method on the RRA since RRA seperates Resource Adapter logging/tracing
        // and database backend logging/tracing .  However, J2c changed their code and removed
        // special logic and started calling it indiscriminately on al resources adapters. That meant that
        // if the RRA trace is on, the database trace is on as well which is not a desired behavior.
        //  Therefore, we am making this method a no-op.
        // normal users won't go through this leg of code when setting the logwriter on their datasource.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "setLogWriter on the mcf is a no-op when calling this method");
    }

    /**
     * This method is to be used by RRA code to set logwriter when needed
     */
    final void reallySetLogWriter(final PrintWriter out) throws ResourceException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setting the logWriter to:", out);

        if (dataSourceOrDriver != null) {
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                    public Void run() throws SQLException {
                        if(!Driver.class.equals(type)) {
                            ((CommonDataSource) dataSourceOrDriver).setLogWriter(out);
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(this, tc, "Unable to set logwriter on Driver type");
                        }
                        return null;
                    }
                });
            } catch (PrivilegedActionException pae) {
                SQLException se = (SQLException) pae.getCause();
                FFDCFilter.processException(se, getClass().getName(), "593", this);
                throw AdapterUtil.translateSQLException(se, this, false, getClass());
            }
        }

        logWriter = out; // Keep the value only if successful on the DataSource.
    }

    /**
     * @return relevant FFDC information for this class, formatted as a String array.
     */
    public String[] introspectSelf() {
        com.ibm.ws.rsadapter.FFDCLogger info = new com.ibm.ws.rsadapter.FFDCLogger(this);

        info.append(dsConfig.get().introspectSelf());

        if (dsConfig.get().enableMultithreadedAccessDetection)
            info.append("Multithreaded access was detected?", detectedMultithreadedAccess);

        info.append("Vendor implementation class:", vendorImplClass);
        info.append("Type:", type);
        info.append("Underlying DataSource or Driver Object: " + AdapterUtil.toString(dataSourceOrDriver), dataSourceOrDriver);
        info.append("Instance id:", instanceID);
        info.append("Log Writer:", logWriter);
        info.append("Counter of fatal connection errors on ManagedConnections created by this MCF:",
                    fatalErrorCount); 

        return info.toStringArray();
    }

    /**
     * Get the log writer for this ManagedConnectionFactory instance.
     * <p>
     * The log writer is a character output stream to which all logging and tracing
     * messages for this ManagedConnectionFactory instance will be printed
     * <p>
     * ApplicationServer manages the association of output stream with the
     * ManagedConnectionFactory. When a ManagedConnectionFactory object is created the
     * log writer is initially null, in other words, logging is disabled.
     * 
     * @return a PrintWriter
     * @exception ResourceException - Possible causes for this exception are:
     *                1) getLogWriter on the dataSource failed
     */

    public final PrintWriter getLogWriter() throws ResourceException {
        if (dataSourceOrDriver == null) {
            return logWriter;
        }
        try {
            if(!Driver.class.equals(type)) {
                return ((CommonDataSource) dataSourceOrDriver).getLogWriter();
            }
            //Return null for Driver since that is the default value which can't be modified
            return null;
        } catch (SQLException se) {
            FFDCFilter.processException(se, getClass().getName(), "1656", this);
            throw AdapterUtil.translateSQLException(se, this, false, getClass());
        }
    }

    /**
     * Retrieves the login timeout for the DataSource.
     * 
     * @return the login timeout for the DataSource.
     */
    public final int getLoginTimeout() throws SQLException {
        try {
            if(!Driver.class.equals(type)) {
                return ((CommonDataSource) dataSourceOrDriver).getLoginTimeout();
            }
            //Return that the default value is being used when using the Driver type
            return 0;
        } catch (SQLException sqlX) {
            FFDCFilter.processException(sqlX, getClass().getName(), "1670", this);
            throw AdapterUtil.mapSQLException(sqlX, this);
        }
    }

    /**
     * This method returns the helper for the database/JDBC driver.
     * 
     * @return the instance of the helper class.
     */
    public final DatabaseHelper getHelper() {
        return helper;
    }

    /**
     * This method is used to get a correlator from the database to log in the websphere tracing
     * 
     * @param WSRdbManagedConnectionImpl mc
     * 
     * @return String
     */
    public String getCorrelator(WSRdbManagedConnectionImpl mc) throws SQLException { 
        return helper.getCorrelator(mc); 
    }

    // code taken from the J2c component that does the subject comparison only on what counts which is
    // the private and public credentials
    static class Equals implements PrivilegedAction<Boolean> {

        Subject _s1, _s2;

        public final void setSubjects(Subject s1, Subject s2) {
            _s1 = s1;
            _s2 = s2;
        }

        public Boolean run() {
                boolean subjestsMatch = false;
                if (tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "PK69110 - Using adapter set equals");
                }
                if (checkCredentials(_s1.getPrivateCredentials(), _s2.getPrivateCredentials())) 
                { 
                  // if the private credentials match check public creds.
                    subjestsMatch = checkCredentials(_s1.getPublicCredentials(), _s2.getPublicCredentials()); 
                } 
                return subjestsMatch; 
        }

        /**
         * This method is replacing checkPrivateCredentials and checkPublicCredentials. The code in both methods
         * contained the same logic.
         * 
         * This method needs to be called two times. The first time with private credentials and the second time
         * with public credentials. Both calls must return true for the Subjects to be equal.
         * 
         * The implementation of Set.equals(Set) is synchronized for the
         * Subject object. This can not be synchronized for the J2C and RRA code implementations. We may be
         * able to code this differently, but I believe this implementation performs well and allows for trace
         * messages during subject processing. This method assumes the Subject's private and public credentials
         * are not changing during the life of a managed connection and managed connection wrapper.
         * 
         * @param s1Credentials
         * @param s2Credentials
         * @return
         */
        private boolean checkCredentials(Set<Object> s1Credentials, Set<Object> s2Credentials) {
            boolean rVal = false;

            if (s1Credentials != s2Credentials) {
                if (s1Credentials != null) {
                    if (s2Credentials != null) {
                        /*
                         * Check to see if the sizes are equal. If the first one and second one are
                         * equal, then check one of them to see if they are empty.
                         * If both are empty, they are equal, If one is empty and the other is not,
                         * they are not equal.
                         */
                        int it1size = s1Credentials.size();
                        int it2size = s2Credentials.size();
                        if (it1size == it2size) {
                            if (it1size == 0) {
                                if (TraceComponent.isAnyTracingEnabled()
                                    && tc.isDebugEnabled())
                                    Tr.debug(this, tc, "Processing credential sets, both are empty, They are equal");
                                return true;
                            }
                        } else {
                            if (TraceComponent.isAnyTracingEnabled()
                                && tc.isDebugEnabled())
                                Tr
                                                .debug(
                                                       tc,
                                                       "Processing credential sets, sets do not contain the same number of elements. They are not equal");
                            return false;
                        }

                        if (it1size > 1) {
                            /*
                             * This is the slow path. In most cases, we should not use this code path.
                             * We should have no objects or one object for each set.
                             * 
                             * This is an unsynchronized unordered equals of two Sets.
                             */
                            Iterator<Object> it1 = s1Credentials.iterator();
                            int objectsEqual = 0;
                            while (it1.hasNext()) {
                                Object s1Cred = it1.next();
                                Iterator<Object> it2 = s2Credentials.iterator();
                                while (it2.hasNext()) {
                                    Object s2Cred = it2.next();
                                    if (s1Cred != null) {
                                        if (!s1Cred.equals(s2Cred)) {
                                            // Objects are not equal
                                            continue;
                                        }
                                    } else {
                                        if (s2Cred != null) {
                                            // Objects are not equal, one object is null");
                                            continue;
                                        }
                                    }
                                    ++objectsEqual;
                                    break;
                                }
                            }
                            // have same number of private credentials, they are =
                            if (objectsEqual == it1size) {
                                // add trace at this point.
                                rVal = true;
                            }
                        } else { // optimized path since we only have one object in both
                                 // sets to compare.
                            Iterator<Object> it1 = s1Credentials.iterator();
                            Iterator<Object> it2 = s2Credentials.iterator();

                            Object s1Cred = it1.next();
                            Object s2Cred = it2.next();
                            if (s1Cred != null) {
                                if (!s1Cred.equals(s2Cred)) {
                                    if (TraceComponent.isAnyTracingEnabled()
                                        && tc.isDebugEnabled())
                                        Tr.debug(this, tc, "PK69110 - Objects are not equal");
                                    return false;
                                }
                            } else {
                                if (s2Cred != null) {
                                    if (TraceComponent.isAnyTracingEnabled()
                                        && tc.isDebugEnabled())
                                        Tr.debug(this, tc,
                                                 "PK69110 - Objects are not equal, one objest is null");
                                    return false;
                                }
                            }
                            rVal = true;
                        }
                    } // second check for null
                } // first check for null
            } else {
                rVal = true;
            }

            return rVal;
        }
    }

    /**
     * Returns the default type of branch coupling that should be used for BRANCH_COUPLING_UNSET.
     * 
     * @return the default type of branch coupling: BRANCH_COUPLING_LOOSE or BRANCH_COUPLING_TIGHT.
     *         If branch coupling is not supported or it is uncertain which type of branch coupling is default,
     *         then BRANCH_COUPLING_UNSET may be returned.
     * @see ResourceRefInfo
     */
    public final int getDefaultBranchCoupling() {
        return helper.getDefaultBranchCoupling();
    }

    /**
     * Returns the xa.start flags (if any) to include for the specified branch coupling.
     * XAResource.TMNOFLAGS should be returned if the specified branch coupling is default.
     * -1 should be returned if the specified branch coupling is not supported.
     * 
     * @param couplingType one of the BRANCH_COUPLING_* constants
     * @return the xa.start flags (if any) to include for the specified branch coupling.
     */
    public final int getXAStartFlagForBranchCoupling(int couplingType) {
        return helper.branchCouplingSupported(couplingType);
    }

    /**
     * This class doesn't support serialization/deserialization
     * 
     * @param in stream from which to deserialize
     * @throws ClassNotFoundException if a class for a field cannot be found
     * @throws IOException if there is an error reading from the stream
     */
    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * This class doesn't support serialization/deserialization.
     * 
     * @param out stream to which to serialize
     * @throws IOException if there is an error writing to the stream
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Version getJDBCRuntimeVersion(){
        return jdbcRuntime.getVersion();
    }
    
    public boolean isPooledConnectionValidationEnabled() {
        return (jdbcDriverSpecVersion >= 40 && dsConfig.get().validationTimeout > -1);
    }
}