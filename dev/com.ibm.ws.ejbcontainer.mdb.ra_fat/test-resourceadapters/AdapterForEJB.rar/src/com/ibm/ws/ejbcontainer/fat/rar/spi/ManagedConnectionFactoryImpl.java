/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.spi;

import java.io.PrintWriter;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.resource.spi.SecurityException;
import javax.resource.spi.ValidatingManagedConnectionFactory;
import javax.resource.spi.security.GenericCredential;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import javax.sql.XADataSource;

import com.ibm.ws.ejbcontainer.fat.rar.core.AdapterUtil;
import com.ibm.ws.ejbcontainer.fat.rar.jdbc.JdbcDataSource;

/**
 * Implementation class of ManagedConnectionFactory
 */
public class ManagedConnectionFactoryImpl implements ManagedConnectionFactory, Serializable, ResourceAdapterAssociation, ValidatingManagedConnectionFactory {
    private final static String CLASSNAME = ManagedConnectionFactoryImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** This is the physical datasource */
    private transient Object dataSource = null;

    /** Whether the physical datasource is 2-phase enabled or not. */
    private boolean is2Phase = false;

    /** JDBC connection factories. */
    private transient HashMap jdbcFactories = null;

    /** Connection factory type. */
    private static final Integer FACTORY_WSJdbcDataSource = 1;
    private transient Integer connectionFactoryType = FACTORY_WSJdbcDataSource;

    /** The loginTimeout value */
    private transient int loginTimeout = 0;

    /** user name */
    private String user = null;

    /** password */
    private String password = null;

    /** datasource implementation class name which is got from config props */
    private String dataSourceClass = null;

    /** Database name which will be set to the physical datasource */
    private String databaseName = null;

    /** resource adapter instance */
    private ResourceAdapter resourceAdapter; // LIDB????

    /**
     * Indicate whether the managed connection created is lazy associatable or not.
     */
    private Boolean lazyAssociatable;

    /**
     * Indicate whether the managed connection created is lazy enlistable or not.
     */
    private Boolean lazyEnlistable;

    /**
     * Indicate whether the managed connection will thow a connection exception.
     */
    private Boolean forceCError;

    /**
     * <p>Variable indicates whether Transaction Resource Registration is
     * dynamic or static.</p>
     */
    private transient String transactionResourceRegistration = "dynamic";

    /**
     * Variable indicates whether inactive handles for connections created with this
     * ManagedConnectionFactory support implicit reactivation, also called "smart
     * handle support".
     */
    private transient boolean inactiveConnectionSupport;

    // @alvinso.1
    String propertyW = "1";

    String propertyX = "1";

    String propertyY = "1";

    String propertyZ = "1";

    String adapterName = "junk";

    private boolean hangCreateManagedConnection = false;
    private int timeToHangCreateManagedConnectionFor = 0;

    /**
     * Constructor.<p>
     */
    public ManagedConnectionFactoryImpl() {
        svLogger.info("<init>");
        jdbcFactories = new HashMap(13);

        // 313344.1
        lazyAssociatable = Boolean.TRUE;
        lazyEnlistable = Boolean.TRUE;
        svLogger.info("313344.1 - ManagedConnectionFactoryImpl constructor " + this);
    }

    /**
     * Creates a connection factory instance, in this case, a JdbcDataSource object. <p>
     */
    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        svLogger.entering(CLASSNAME, "createConnectionFactory", cm);
        Object cf = null;

        if (connectionFactoryType.equals(FACTORY_WSJdbcDataSource)) {
            cf = jdbcFactories.get(cm);

            if (cf == null) {
                cf = new JdbcDataSource(this, cm);
                jdbcFactories.put(cm, cf);
            }
        } else {
            throw new ResourceException("This resource adapter only supports JDBC datasource");
        }

        svLogger.exiting(CLASSNAME, "createConnectionFactory", cf);
        return cf;
    }

    /**
     * Create a connection factory with default connection manager.<p>
     */
    @Override
    public Object createConnectionFactory() throws ResourceException {
        return createConnectionFactory(new DefaultConnectionManager());
    }

    /**
     * Creates a new physical connection to the underlying resource manager,
     * ManagedConnectionFactory uses the security information (passed as Subject) and
     * additional ConnectionRequestInfo (which is specific to ResourceAdapter and opaque
     * to application server) to create this new connection.<p>
     *
     * @see javax.resource.spi.ManagedConnectionFactory#createManagedConnection(Subject, ConnectionRequestInfo)
     */
    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        svLogger.entering(CLASSNAME, "createManagedConnection", new Object[] { subject == null ? null : "subject not null", AdapterUtil.toString(cxRequestInfo) });

        if (hangCreateManagedConnection) {
            // We DO need to hang the .createConnection request...
            // Log fact that we're hanging
            svLogger.info("HANGING .createManagedConnection() for **" + timeToHangCreateManagedConnectionFor + "** miliseconds.");

            // Actually hang, and then error out
            // JCA specs says that we must throw a ResourceException here, so we have to catch whatever error comes out of this,
            // and re-throw it as a ResourceException.
            try {
                // sleep for the designated time
                Thread.sleep(timeToHangCreateManagedConnectionFor);

                // error out
                throw new Exception("The .createManagedConnection() request has been killed because the 'hang' switch was set to true.");
            } catch (Throwable e) {
                throw new ResourceException(e);
            }
        } else {
            // We do NOT need to hang the .createConnection request...
            // Log fact that we do NOT hang
            svLogger.info("Allowing .createManagedConnection() to procede without hang.");
        }

        // if the code gets this far, then we did NOT hang
        // User/password from the Subject will take first priority over any values specified in
        // the CRI, as required by the JCA spec.  If neither the Subject nor the CRI contain a
        // user/password, then the backend database is allowed to reject or accept the
        // connection request. [d155796] [d159148]

        // If the Subject is not null but it is empty, then we should use the backend default user/pwd.
        // we should not use the one from CRI. //170193

        // If we don't have a CRI, then create a blank one. A non-null CRI is required later.
        // If we do have a CRI, the values in the CRI take first priority--unless they're null.
        ConnectionRequestInfoImpl cri = cxRequestInfo == null ? new ConnectionRequestInfoImpl() : (ConnectionRequestInfoImpl) cxRequestInfo;

        String userName = null;
        String password = null;

        if (subject == null)
        // Check the Subject first, then the CRI values. [d159148]
        {
            userName = cri.getUserName();
            password = cri.getPassword();

            svLogger.info("Subject is null. User name is: " + userName);
            svLogger.info("Subject is null. Password is: " + password);

            if (userName == null) // Distinguish between option C/using DS defaults. [d159148]
            {
                // d117349
                //  For recovery, we need to be able to accept if there is no CRI or Subject.
                //  In this case, we will use the username/password on the dataSource to get
                //  the connection.

                svLogger.info("Using DataSource default user/password for authentication");

                // Leave username and password as null here.  Then, in the code below, use
                // the null value to determine how to get the JTA/Pooled Connection.  If
                // we try to use null, null as the username/password, the AS/400 driver
                // will have problems.  For more information on this, refer to defect 117710
            } else {
                // Option C security
                // We use the non-null user/password found in the CRI

                svLogger.info("Using ConnectionRequestInfo for authentication");
            }
        } else
        // Subject is available
        {
            // Option A and B security
            svLogger.info("Subject found.  Will try to use either PasswordCredentials or GenericCredentials ");

            // This list of credentials may have
            // [A] PasswordCredentials for this ManagedConnection instance, or
            // [B] GenericCredentials for use with Kerberos support, or
            // no credentials at all.
            final Iterator iter = subject.getPrivateCredentials().iterator();

            // Set the credential object to null. If the Subject contains a null
            // credential object, Test Resource Adapter should throw
            // javax.resource.spi.SecurityException according to JCA Spec, 9.1.8.2
            Object credential = null;

            // d127709 - Performance enhancements for doPriv code. Instantiate the privileged
            // action class only once, instead of each iteration through the while loop.
            PrivilegedExceptionAction iterationAction = new PrivilegedExceptionAction() {
                @Override
                public Object run() throws Exception {
                    return iter.next();
                }
            };

            PasswordCredential pwcred;

            while (iter.hasNext()) {
                // --------------d115459 wrap the .next() in a doPrivileged()
                svLogger.entering(CLASSNAME, "createManagedConnection", "Loop for PrivateCredentials");

                try {
                    credential = AccessController.doPrivileged(iterationAction);
                } catch (PrivilegedActionException pae) {
                    svLogger.exiting(CLASSNAME, "createManagedConnection", "Exception");
                    String message = "Error validating credentials.";
                    ResourceException resX = new ResourceException(message);
                    throw resX;
                }

                if (credential instanceof PasswordCredential) {
                    // This is possibly Option A - only possibly because the PasswordCredential
                    // may not match the MC.  Then we have to keep looping.
                    svLogger.info("The credential is an instance of PasswordCredential");
                    pwcred = (PasswordCredential) credential;
                    if ((pwcred.getManagedConnectionFactory()).equals(this)) {
                        svLogger.info("Using PasswordCredentials for authentication");
                        userName = pwcred.getUserName();

                        // Allow for the possibility of a null password. [d155796]
                        char[] pwdChars = pwcred.getPassword();
                        password = pwdChars == null ? null : new String(pwdChars);
                        svLogger.info("User name is: " + userName == null ? "null" : userName);
                        svLogger.info("Password is: " + password == null ? "null" : password);
                        break;
                    }
                } else if (credential instanceof GenericCredential) {
                    // This is option B
                    svLogger.info("Using GenericCredentials for authentication");
                }
            }
            // Need to handle the scenario in which the credential is null. Then
            // throw javax.resource.spi.SecurityException
            if (credential == null) {
                String message = "Null credentials with non-null Subject.";
                throw new SecurityException(message);
            }
        }

        try {
            // Not using doPrivileged call when get datasource to see if it works.
            // This works even without any special security permission.
            PooledConnection pconn = null;

            svLogger.info("createManagedConnection: Not using doPrivileged call when get datasource.");

            if (is2Phase) {
                if (userName == null) {
                    svLogger.info("Before getXAConnection: User name is null");
                } else {
                    svLogger.info("Before getXAConnection: User name is: " + userName);
                }
                if (password == null) {
                    svLogger.info("Before getXAConnection: Password is null");
                } else {
                    svLogger.info("Before getXAConnection: Password is: " + password);
                }
                pconn = userName == null ? ((XADataSource) dataSource).getXAConnection() : ((XADataSource) dataSource).getXAConnection(userName, password);
            } else {
                if (userName == null) {
                    svLogger.info("Before getConnPoolConnection: User name is null");
                } else {
                    svLogger.info("Before getConnPoolConnection: User name is: " + userName);
                }
                if (password == null) {
                    svLogger.info("Before getConnPoolConnection: Password is null");
                } else {
                    svLogger.info("Before getConnPoolConnection: Password is: " + password);
                }
                pconn = userName == null ? ((ConnectionPoolDataSource) dataSource).getPooledConnection() : ((ConnectionPoolDataSource) dataSource).getPooledConnection(userName,
                                                                                                                                                                       password);
            }

            Connection conn = pconn.getConnection();

            // delegate to the factory method
            svLogger.info("createManagedConnection: About to create MC.");
            ManagedConnection mc = createManagedConnectionImpl(this, pconn, conn, subject, cri);
            svLogger.exiting(CLASSNAME, "createManagedConnection", mc);
            return mc;
        } catch (SQLException sqle) {
            svLogger.exiting(CLASSNAME, "createManagedConnection", sqle);
            throw new ResourceException(sqle.getMessage());
        } catch (ResourceException resX) {
            svLogger.exiting(CLASSNAME, "createManagedConnection", resX);
            throw resX;
        }
    }

    /**
     * Returns a matched connection from the candidate set of connections.
     * ManagedConnectionFactory uses the security info (as in Subject) and information
     * provided through ConnectionRequestInfo and additional Resource Adapter
     * specific criteria to do matching. <p>
     *
     * This method returns a ManagedConnection instance that is the best match for
     * handling the connection allocation request. If no match connection is found,
     * a NULL value is returned.
     *
     * @param Set connectionSet - candidate connection set
     * @param Subject subject - caller's security information
     * @param ConnectionRequestInfo cxRequestInfo - additional resource adapter specific connection request
     *            information - this is required to be non null
     * @return a ManagedConnection if resource adapter finds an acceptable match otherwise null
     * @exception ResourceException
     */
    @Override
    public ManagedConnection matchManagedConnections(final Set connectionSet, final Subject subject, final ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        String subjectTrace = null;
        if (subject == null)
            subjectTrace = "null";
        else
            subjectTrace = "not null";
        svLogger.entering(CLASSNAME, "matchManagedConnections", new Object[] { subjectTrace, cxRequestInfo });

        ManagedConnectionImpl matchedmc = null;

        // Note that once the ManagedConnection is created, the subject and connection request info
        // should always be the same.  Therefore, it is legal to just check the two of these to see
        // if there is a match.

        if (subject == null) {
            ManagedConnectionImpl currentmc = null;

            for (Iterator iter = connectionSet.iterator(); iter.hasNext();) {
                currentmc = (ManagedConnectionImpl) iter.next();
                if ((currentmc.getSubject() == null)
                    && cxRequestInfo.equals(currentmc.getCRI())
                    && currentmc.isLazyAssociatable() == lazyAssociatable.booleanValue()
                    && currentmc.isLazyEnlistable() == lazyAssociatable.booleanValue()) {
                    matchedmc = currentmc;
                    break;
                }
            }
        } else {
            try {
                matchedmc = (ManagedConnectionImpl) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                    @Override
                    public Object run() throws Exception {
                        ManagedConnectionImpl currentmc = null;
                        for (Iterator iter = connectionSet.iterator(); iter.hasNext();) {
                            currentmc = (ManagedConnectionImpl) iter.next();
                            if (subject.equals(currentmc.getSubject()) && cxRequestInfo.equals(currentmc.getCRI()))
                                return currentmc;
                        }

                        return null;
                    }
                });
            } catch (PrivilegedActionException pae) {
                ResourceException resX = (ResourceException) pae.getException();

                svLogger.exiting(CLASSNAME, "matchManagedConnections", resX);
                throw resX;
            }
        }

        if (forceCError != null && forceCError == Boolean.TRUE) {
            svLogger.info("Attempting to trigger a ConnectionEvent Error");
            ConnectionEvent ce = null;
            ce = new ConnectionEvent(matchedmc, ConnectionEvent.CONNECTION_ERROR_OCCURRED);
            matchedmc.getLastActionEventListener().connectionErrorOccurred(ce);
            svLogger.info("ConnectionEvent Error procedure finished.");
            return null;
        }

        svLogger.exiting(CLASSNAME, "matchManagedConnections", matchedmc);
        return matchedmc;
    }

    /**
     * Set the LogWriter to the physical datasource. If the physical datasource
     * is null, do nothing.
     *
     * @param PrintWriter The log Writer
     */
    @Override
    public void setLogWriter(PrintWriter pw) throws ResourceException {
        if (dataSource != null) {
            try {
                if (is2Phase) {
                    ((XADataSource) dataSource).setLogWriter(pw);
                } else {
                    ((ConnectionPoolDataSource) dataSource).setLogWriter(pw);
                }
            } catch (SQLException sqle) {
                throw new ResourceException(sqle.getMessage());
            }
        }
    }

    /**
     * Get the LogWriter from the physical datasource. If the physical datasource
     * is null, do nothing.
     *
     * @return The log writer.
     */
    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        try {
            return dataSource == null ? null : (is2Phase ? ((XADataSource) dataSource).getLogWriter() : ((ConnectionPoolDataSource) dataSource).getLogWriter());
        } catch (SQLException sqle) {
            throw new ResourceException(sqle.getMessage());
        }
    }

    /**
     * Get the loginTimeout of the managed connection factory. This method
     * returns the LoginTimeout value of the physical datasource. If the physical
     * datasource is null, return the instance variable loginTimeout.<p>
     *
     * @return The LoginTimeout value of the physical datasource.
     */
    public final int getLoginTimeout() throws ResourceException {
        if (loginTimeout != -1) {
            return loginTimeout;
        } else if (dataSource == null) {
            return loginTimeout;
        } else
            try {
                return is2Phase ? ((XADataSource) dataSource).getLoginTimeout() : ((ConnectionPoolDataSource) dataSource).getLoginTimeout();
            } catch (SQLException sqle) {
                throw new ResourceException(sqle.getMessage());
            }
    }

    /**
     * Set loginTimeout. This method sets the LoginTimeout value to the
     * physical datasource. If the physical datasource is null, sets
     * the instance attribute loginTimeout.<p>
     *
     * @return The LoginTimeout value of the physical datasource.
     */
    public final void setLoginTimeout(int loginTimeout) throws ResourceException {
        if (dataSource == null) {
            this.loginTimeout = loginTimeout;
        } else
            try {
                if (is2Phase) {
                    ((XADataSource) dataSource).setLoginTimeout(loginTimeout);
                } else
                    ((ConnectionPoolDataSource) dataSource).setLoginTimeout(loginTimeout);
            } catch (SQLException sqle) {
                throw new ResourceException(sqle.getMessage());
            }
    }

    /**
     * Set the user name to the physical datasource. If the physical
     * datasource is not created, set the user name to instance variable
     * so it can be set to the datasource when datasource is created.<p>
     *
     * @param String The user name
     */
    public final void setUserName(String uid) throws ResourceException {
        svLogger.info("set user = " + uid);
        user = uid;

        // If dataSource is not null, set userName to the physical datasource.
        if (dataSource != null) {
            Properties props = new Properties();
            props.put("user", user);
            AdapterUtil.setDataSourceProperties(dataSource, props);
        }
    }

    /**
     * Set the password to the physical datasource. If the physical
     * datasource is not created, set the password to instance variable
     * so it can be set to the datasource when datasource is created.<p>
     *
     * @param String The password
     */
    public final void setPassword(String password) throws ResourceException {
        svLogger.info("set password = " + password);
        this.password = password;

        // If dataSource is not null, set password to the physical datasource.
        if (dataSource != null) {
            Properties props = new Properties();
            props.put("password", password);
            AdapterUtil.setDataSourceProperties(dataSource, props);
        }
    }

    /**
     * Set the database name to the physical datasource. If the physical
     * datasource is not created, set the database name to instance variable
     * so it can be set to the datasource when datasource is created.<p>
     *
     * @param String The database name
     */
    public final void setDatabaseName(String databaseName) throws ResourceException {
        svLogger.info("set databaseName = " + databaseName);
        this.databaseName = databaseName;

        // If dataSource is not null, set databaseName to the physical datasource.
        if (dataSource != null) {
            Properties props = new Properties();
            props.put("databaseName", databaseName);
            AdapterUtil.setDataSourceProperties(dataSource, props);
        }
    }

    /**
     * Create the physical datasource using the passed-in datasource implementation
     * class name.<p>
     *
     * @param String The database source implementation class name
     */
    public final void setDataSourceClass(String dataSourceClass) throws ResourceException {
        svLogger.info("set datasource class = " + dataSourceClass);
        this.dataSourceClass = dataSourceClass;

        try {
            if (Class.forName(dataSourceClass).newInstance() instanceof XADataSource)
                is2Phase = true;
        }
        // start 313344.1
        catch (IllegalAccessException e) {
            svLogger.info("IllegalAccessException occurred: " + e.getMessage());
            e.printStackTrace();
            throw new ResourceException(e.getMessage());
        } catch (InstantiationException e) {
            svLogger.info("InstantiationException occurred: " + e.getMessage());
            e.printStackTrace();
            throw new ResourceException(e.getMessage());
        }
        // end 313344.1
        catch (Exception e) {
            e.printStackTrace(); // 313344.1
            throw new ResourceException(e.getMessage());
        }
        dataSource = AdapterUtil.createDataSource(dataSourceClass);

        // Now set all the datasource properties to the datasource.
        setProperties();
    }

    /**
     * This method is called to set all the datasource properties after the
     * datasource is created. After the datasource is created, we don't
     * know which properties are set or not. Therefore, we need to check
     * properties one by one.
     */
    private void setProperties() throws ResourceException {
        Properties props = new Properties();

        // Check and set the property
        if (user != null)
            props.setProperty("user", user);
        if (password != null)
            props.setProperty("password", password);
        if (databaseName != null)
            props.setProperty("databaseName", databaseName);

        // 313344.1 may be able to omit this since the jacl is setting it
        props.setProperty("resultSetHoldability", "2");
        AdapterUtil.setDataSourceProperties(dataSource, props);
    }

    public boolean is2Phase() {
        return is2Phase;
    }

    @Override
    public final boolean equals(Object other) {
        svLogger.entering(CLASSNAME, "equals(Object)", new Object[] { this, other });
        return this == other;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    // LIDB???? starts
    @Override
    public void setResourceAdapter(ResourceAdapter adapter) {
        svLogger.info("setResourceAdapter: " + adapter);

        if (resourceAdapter == null) {
            resourceAdapter = adapter;
        } else {
            throw new RuntimeException("Cannot call setResourceAdapter twice");
        }
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    public String getDataSourceClass() {
        return dataSourceClass;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public ManagedConnection createManagedConnectionImpl(ManagedConnectionFactoryImpl mcf, PooledConnection pconn, Connection conn, Subject sub,
                                                         ConnectionRequestInfoImpl cxRequestInfo) throws ResourceException {
        if (lazyEnlistable.equals(Boolean.TRUE)) {
            if (lazyAssociatable.equals(Boolean.TRUE)) {
                // right here is where we die
                return new LazyEnlistableLazyAssociatableMC(mcf, pconn, conn, sub, cxRequestInfo);
            } else {
                return new LazyEnlistableMC(mcf, pconn, conn, sub, cxRequestInfo);
            }
        } else {
            if (lazyAssociatable.equals(Boolean.TRUE)) {
                return new LazyAssociatableMC(mcf, pconn, conn, sub, cxRequestInfo);
            } else {
                return new MC(mcf, pconn, conn, sub, cxRequestInfo);
            }
        }
    }

    /**
     * Returns the inactiveConnectionSupport.
     *
     * @return boolean
     */
    public boolean isInactiveConnectionSupport() {
        return inactiveConnectionSupport;
    }

    /**
     * Returns the transactionResourceRegistration.
     *
     * @return String
     */
    public String getTransactionResourceRegistration() {
        return transactionResourceRegistration;
    }

    /**
     * Sets the inactiveConnectionSupport.
     *
     * @param inactiveConnectionSupport The inactiveConnectionSupport to set
     */
    public void setInactiveConnectionSupport(boolean inactiveConnectionSupport) {
        svLogger.info("setInactiveConnectionSupport: " + new Boolean(inactiveConnectionSupport));
        this.inactiveConnectionSupport = inactiveConnectionSupport;
    }

    /**
     * Sets the transactionResourceRegistration.
     *
     * @param transactionResourceRegistration The transactionResourceRegistration to set
     */
    public void setTransactionResourceRegistration(String transactionResourceRegistration) {
        svLogger.info("setTransactionResourceRegistration: " + new Boolean(transactionResourceRegistration));
        this.transactionResourceRegistration = transactionResourceRegistration;
    }

    /**
     * Returns the lazyAssociatable.
     *
     * @return Boolean
     */
    public Boolean getLazyAssociatable() {
        return lazyAssociatable;
    }

    /**
     * Returns the lazyEnlistable.
     *
     * @return Boolean
     */
    public Boolean getLazyEnlistable() {
        return lazyEnlistable;
    }

    /**
     * Sets the hangCreateManagedConnection.
     *
     * @param hangCreateManagedConnection The hangCreateManagedConnection to set
     */
    public void setforceCError(Boolean forceCError) {
        this.forceCError = forceCError;
    }

    /**
     * Sets the lazyAssociatable.
     *
     * @param lazyAssociatable The lazyAssociatable to set
     */
    public void setLazyAssociatable(Boolean lazyAssociatable) {
        this.lazyAssociatable = lazyAssociatable;
    }

    /**
     * Sets the lazyEnlistable.
     *
     * @param lazyEnlistable The lazyEnlistable to set
     */
    public void setLazyEnlistable(Boolean lazyEnlistable) {
        this.lazyEnlistable = lazyEnlistable;
    }

    // @alvinso.1
    /**
     * @return
     */
    public String getAdapterName() {
        return adapterName;
    }

    /**
     * @return
     */
    public String getPropertyW() {
        return propertyW;
    }

    /**
     * @return
     */
    public String getPropertyX() {
        return propertyX;
    }

    /**
     * @return
     */
    public String getPropertyY() {
        return propertyY;
    }

    /**
     * @return
     */
    public String getPropertyZ() {
        return propertyZ;
    }

    /**
     * @param string
     */
    public void setAdapterName(String string) {
        adapterName = string;
    }

    /**
     * @param string
     */
    public void setPropertyW(String string) {
        propertyW = string;
    }

    /**
     * @param string
     */
    public void setPropertyX(String string) {
        propertyX = string;
    }

    /**
     * @param string
     */
    public void setPropertyY(String string) {
        propertyY = string;
    }

    /**
     * @param string
     */
    public void setPropertyZ(String string) {
        propertyZ = string;
    }

    /**
     * LIDB:2110.97 - 06/08/04 added begin by gburli
     * Implementation of getInvalidConnections(Set ConnectionSet) method
     * for ValidatingManagedConnectionFactory interface
     */
    @Override
    public Set getInvalidConnections(Set connectionSet) throws ResourceException {
        svLogger.entering(CLASSNAME, "getInvalidConnections", new Object[] { connectionSet, this });
        Set failConnSet = new HashSet();
        int numFailedConn = connectionSet.size();
        svLogger.info("Number of Failed Connections : " + numFailedConn);
        ManagedConnection mc = null;
        Iterator mcIT = connectionSet.iterator();
        svLogger.info("Iterators has " + mcIT.hasNext() + " elements");

        if (AdapterUtil.getInvalidConnFlag() == AdapterUtil.ALL_MC_INVALID) {
            svLogger.info("All MC will be marked invalid");
            while (mcIT.hasNext()) {
                try {
                    //Iterate thru each connections from the set
                    mc = (ManagedConnection) mcIT.next();

                    //Mark each connection as Invalid
                    failConnSet.add(mc);

                    //Set this to RA
                    AdapterUtil.addInvalidMCToSet(mc);
                } catch (Exception ex) {
                    svLogger.info("getInvalidConnections: Exception thrown");
                    throw new ResourceException(ex);
                }
            }
        } else if (AdapterUtil.getInvalidConnFlag() == AdapterUtil.LAST_MC_INVALID) {
            // Process the MCs for "LAST_MC_INVALID" test scenario
            svLogger.info("Only the last MC will be marked invalid");

            while (mcIT.hasNext()) {
                try {
                    // Iterate thru each connections from the set
                    mc = (ManagedConnection) mcIT.next();

                    // Mark the connection as Invalid if it is the last one
                    if (!mcIT.hasNext()) {
                        failConnSet.add(mc);
                        // Set this to RA
                        AdapterUtil.addInvalidMCToSet(mc);
                    }
                } catch (Exception ex) {
                    svLogger.info("getInvalidConnections: Exception thrown");
                    throw new ResourceException(ex);
                }
            }
        } else if (AdapterUtil.getInvalidConnFlag() == AdapterUtil.ODD_MC_INVALID) {
            // Process the MCs for "ODD_MC_INVALID" test scenario
            int i = 0;
            svLogger.info("ODD number of MC will be marked invalid");

            while (mcIT.hasNext()) {
                try {
                    // Iterate thru each connections from the set
                    mc = (ManagedConnection) mcIT.next();

                    // Mark the connection as Invalid if it is the even number MC
                    if ((i % 2) == 0) {
                        failConnSet.add(mc);
                        // Set this to RA
                        AdapterUtil.addInvalidMCToSet(mc);
                    }
                    i++;
                } catch (Exception ex) {
                    svLogger.info("getInvalidConnections: Exception thrown");
                    throw new ResourceException(ex);
                }
            }
        } else if (AdapterUtil.getInvalidConnFlag() == AdapterUtil.EXCEPTION_MC_INVALID) {
            svLogger.info("getInvalidConnections: ResourceException test scenario");
            // Throw Resource Exception
            throw new ResourceException("Test5 : Resource Exception thrown");
        } else if (AdapterUtil.getInvalidConnFlag() == AdapterUtil.DEFAULT_EMPTY_MC_SET) {
            svLogger.info("getInvalidConnections: send empty set back to testcase");
            // Already failConnSet is initialised to empty set.
        } else if (AdapterUtil.getInvalidConnFlag() == AdapterUtil.NULL_MC_INVALID) {
            svLogger.info("getInvalidConnections: send null object back to testcase");
            failConnSet = null;
        } else if (AdapterUtil.getInvalidConnFlag() == AdapterUtil.NULL_WITH_MC_INVALID) {
            svLogger.info("getInvalidConnections: send null object with all MCs back to testcase");

            while (mcIT.hasNext()) {
                try {
                    // Iterate thru each connections from the set
                    mc = (ManagedConnection) mcIT.next();

                    // Mark each connection as Invalid
                    failConnSet.add(mc);

                    // Add null object at the end
                    if (!mcIT.hasNext()) {
                        failConnSet.add(null);
                    }
                    // Set this to RA
                    AdapterUtil.addInvalidMCToSet(mc);
                } catch (Exception ex) {
                    svLogger.info("getInvalidConnections: Exception thrown");
                    throw new ResourceException(ex);
                }
            }
        }
        return failConnSet;
    }

    // LIDB:2110.97 - 06/08/04 added end by gburli
    // @GRP - BEGIN TYPE 4 XA SUPPORT
    // The following getters and setters are being added to support the usage of a Type 4 XA
    // Connection.  DB2 for distributed supports a Type 2 XA configuration, but DB2 for z/OS
    // does not, and therefore the ability to use Type 4 must be added to this adapter.
    //
    // The following properties are required for this:
    //    driverType        This can be set to Type 2 or Type 4, but Type 2 will not work on z/OS
    //    serverName        This is required for Type 4, and is the hostname or IP address to
    //                        connect to
    //    portNumber        This is required for Type 4, and is the port number to use when
    //                        connecting to serverName
    //
    // With the getters are setters added, the WebSphere Administrative Console can be used to
    // enabled Type 4 XA by defining the above three properties as Custom Properties on the
    // Connection Factory.

    private String driverType = null;

    public void setDriverType(String newDriverType) throws ResourceException {
        svLogger.entering(CLASSNAME, "setDriverType", driverType);
        driverType = newDriverType;

        Properties props = new Properties();
        props.put("driverType", driverType);
        AdapterUtil.setDataSourceProperties(dataSource, props);

        svLogger.exiting(CLASSNAME, "setDriverType");
    }

    public String getDriverType() {
        return driverType;
    }

    private String serverName = null;

    public void setServerName(String newServerName) throws ResourceException {
        svLogger.entering(CLASSNAME, "setServerName", serverName);
        serverName = newServerName;

        Properties props = new Properties();
        props.put("serverName", serverName);
        AdapterUtil.setDataSourceProperties(dataSource, props);

        svLogger.exiting(CLASSNAME, "setServerName");
    }

    public String getServerName() {
        return serverName;
    }

    private String portNumber = null;

    public void setPortNumber(String newPortNumber) throws ResourceException {
        svLogger.entering(CLASSNAME, "setPortNumber", portNumber);
        portNumber = newPortNumber;

        Properties props = new Properties();
        props.put("portNumber", portNumber);
        AdapterUtil.setDataSourceProperties(dataSource, props);

        svLogger.exiting(CLASSNAME, "setPortNumber");
    }

    public String getPortNumber() {
        return portNumber;
    }

    public boolean isHangCreateManagedConnection() {
        return hangCreateManagedConnection;
    }

    public void setHangCreateManagedConnection(boolean hangCreateManagedConnection) {
        this.hangCreateManagedConnection = hangCreateManagedConnection;
    }

    public int getTimeToHangCreateManagedConnectionFor() {
        return timeToHangCreateManagedConnectionFor;
    }

    public void setTimeToHangCreateManagedConnectionFor(int timeToHangCreateManagedConnectionFor) {
        this.timeToHangCreateManagedConnectionFor = timeToHangCreateManagedConnectionFor;
    }
    // @GRP - END TYPE 4 XA SUPPORT
}