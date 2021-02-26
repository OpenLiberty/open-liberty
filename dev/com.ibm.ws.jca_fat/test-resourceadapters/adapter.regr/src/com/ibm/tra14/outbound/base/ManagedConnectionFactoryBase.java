/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tra14.outbound.base;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ValidatingManagedConnectionFactory;
import javax.security.auth.Subject;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tra14.SimpleRAImpl;
import com.ibm.tra14.trace.DebugTracer;

/**
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
//@SuppressWarnings("serial")
public class ManagedConnectionFactoryBase implements ValidatingManagedConnectionFactory, ManagedConnectionFactory, Serializable {

    private PrintWriter logWriter;

    private String serverName = null;
    private Integer portNumber = null;

    private String user = null;
    private String password = null;

    protected String traceLevel = null;

    private final String className = "ManagedConnectionFactoryBase Ver 2";

    private boolean testConnectionValue = true; // good test connection

    private String poolJNDIName = "notSet";

    private boolean testDatabaseValue = true; // good test database

    private static final TraceComponent tc = Tr.register(ManagedConnectionFactoryBase.class, SimpleRAImpl.RAS_GROUP, null);

    /**
     * Default Constructor
     */
    public ManagedConnectionFactoryBase() {
        final String methodName = "CCIManagedConnectionFactoryImpl";
        Tr.entry(tc, methodName);
        DebugTracer.printClassLoaderInfo(className, this);
        DebugTracer.printStackDump(className, new Exception());
        Tr.exit(tc, methodName);
        System.out.println("Confirmation message");
    }

    /**
     * @see javax.resource.spi.ManagedConnectionFactory#createConnectionFactory(javax.resource.spi.ConnectionManager)
     */
    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        final String methodName = "createConnectionFactory";
        Tr.entry(tc, methodName, cm);
        System.out.println("LIDB3598, CCIManagedConnectionFactoryImpl createConnectionFactory with cm=" + cm);

        if (cm == null)
            throw new ResourceException("ConnectionManger must not be null.");

        ConnectionFactoryBase cf = new ConnectionFactoryBase(this, cm);

        Tr.exit(tc, methodName, cf);
        return cf;
    }

    /**
     * @see javax.resource.spi.ManagedConnectionFactory#createConnectionFactory()
     */
    @Override
    public Object createConnectionFactory() throws ResourceException {
        final String methodName = "createConnectionFactory";
        Tr.entry(tc, methodName);

        ConnectionFactoryBase cf = new ConnectionFactoryBase(this, new ConnectionManagerBase());

        Tr.exit(tc, methodName, cf);
        return cf;
    }

    /**
     * @see javax.resource.spi.ManagedConnectionFactory#createManagedConnection(javax.security.auth.Subject, javax.resource.spi.ConnectionRequestInfo)
     */
    @Override
    public ManagedConnection createManagedConnection(Subject subj, ConnectionRequestInfo reqInfo) throws ResourceException {
        final String methodName = "createManagedConnection";
        Tr.entry(tc, methodName, new Object[] { subj, reqInfo });

        ConnectionRequestInfoBase myReqInfo = null;
        if (reqInfo != null && reqInfo instanceof ConnectionRequestInfoBase)
            myReqInfo = (ConnectionRequestInfoBase) reqInfo;
        else
            throw new ResourceException("Invalid ConnectionRequestInfo.");

        ManagedConnection mc = new ManagedConnectionBase(this, myReqInfo);

        Tr.exit(tc, methodName, mc);
        return mc;
    }

    /**
     * @see javax.resource.spi.ManagedConnectionFactory#matchManagedConnections(java.util.Set, javax.security.auth.Subject, javax.resource.spi.ConnectionRequestInfo)
     */
    //@SuppressWarnings("unchecked")
    @Override
    public ManagedConnection matchManagedConnections(Set candidateSet, Subject subj, ConnectionRequestInfo reqInfo) throws ResourceException {

        final String methodName = "matchManagedConnections";
        Tr.entry(tc, methodName);

//		throw new javax.resource.NotSupportedException( "This Resource Adadpter does not support connection matching." );

        // cjn start
        if (candidateSet == null) {
            Tr.debug(tc, methodName,
                     "Set passed in to method is null, is this an error?");
        } else {
            Tr.debug(tc, methodName,
                     "Input candidateSet: " + candidateSet.toString());
        }

        if (subj == null) {
            Tr.debug(tc, methodName,
                     "Subject passed in to method is null");
        } else {
            Tr.debug(tc, methodName,
                     "Input Subject: " + subj.getClass().getName() + ":" + subj.toString());
        }

        if (reqInfo == null) {
            Tr.debug(tc, methodName,
                     "ConnectionRequestInfo passed in to method is null");
        } else {
            Tr.debug(tc, methodName,
                     "Input ConnectionRequestInfo: " + reqInfo.getClass().getName() + ":" + reqInfo.toString());
        }
        // cjn end

        // cjn start
        String userName = null;
        String password = null;
        if (reqInfo != null) {
            userName = ((ConnectionRequestInfoBase) reqInfo).getUser();
            password = ((ConnectionRequestInfoBase) reqInfo).getPassword();
        }
        // cjn end

        ManagedConnection matchedConnection = null;
        int n = 0;
        for (Iterator it = candidateSet.iterator(); it.hasNext(); n++) {
            ManagedConnectionBase mconn = (ManagedConnectionBase) it.next();
            Tr.debug(tc, methodName,
                     "candidateSet[ " + n + " ]: " + mconn.getClass().getName() + ":" + mconn.toString());
            if (mconn.getUserName().equals(userName) && mconn.getPassword().equals(password) && mconn.getManagedConnectionFactory().getJNDIName().equals(poolJNDIName)) {
                matchedConnection = mconn;
                break;
            }
        }

        Tr.exit(tc, methodName, matchedConnection);
        return matchedConnection;
    }

    /**
     * @see javax.resource.spi.ManagedConnectionFactory#setLogWriter(java.io.PrintWriter)
     */
    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
        final String methodName = "setLogWriter";
        Tr.entry(tc, methodName);

        logWriter = out;

        Tr.exit(tc, methodName);
    }

    /**
     * @see javax.resource.spi.ManagedConnectionFactory#getLogWriter()
     */
    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        final String methodName = "getLogWriter";
        Tr.entry(tc, methodName);

        Tr.exit(tc, methodName);
        return logWriter;
    }

    /**
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return
     */
    public String getUserName() {
        return user;
    }

    /**
     * @param user
     */
    public void setUserName(String user) {
        this.user = user;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setPortNumber(Integer portNumber) {
        final String methodName = "setPortNumber";
        this.portNumber = portNumber;

        Tr.debug(tc, methodName,
                 "The 'portNumber' config-property is currently set to " + this.portNumber.toString());
    }

    public Integer getPortNumber() {
        return portNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (obj instanceof ManagedConnectionFactoryBase) {
            ManagedConnectionFactoryBase other = (ManagedConnectionFactoryBase) obj;
            return isEqual(getUserName(), other.getUserName())
                   && isEqual(getPassword(), other.getPassword())
                   && isEqual(getServerName(), other.getServerName())
                   && isEqual(getPortNumber(), other.getPortNumber())
                   && isEqual(getTestConnectionValue(), other.getTestConnectionValue())
                   && isEqual(getJNDIName(), other.getJNDIName());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        String result = "" + user + password;
        return result.hashCode();
    }

    private boolean isEqual(Object o1, Object o2) {
        if (o1 == null)
            return o2 == null;
        else
            return o1.equals(o2);
    }

    @Override
    public String toString() {
        final String ID = "CCIMCFImpl";
        final char SEP = ':';
        StringBuffer buf = new StringBuffer(ID);
        buf.append(SEP).append(getServerName()).append(SEP).append(getPortNumber());
        buf.append(SEP).append(getUserName()).append(SEP).append(getPassword());
        if (poolJNDIName != null) {
            buf.append(poolJNDIName);
        }
        return buf.toString();
    }

    /**
     * This method returns a set of invalid ManagedConnection objects chosen from a specified set of
     * ManagedConnection objects.
     *
     * @see javax.resource.spi.ValidatingManagedConnectionFactory#getInvalidConnections(java.util.Set)
     */
    //@SuppressWarnings("unchecked")
    @Override
    public Set getInvalidConnections(Set connectionSet) throws ResourceException {
        final String methodName = "getInvalidConnections";
        Tr.entry(tc, methodName, connectionSet);

        Set invalidConnections = new HashSet();

        // How to tell if a ManagedConnection is valid or invalid?
        //
        for (Iterator it = connectionSet.iterator(); it.hasNext();) {
            ManagedConnection conn = (ManagedConnection) it.next();

            Tr.debug(tc, methodName,
                     "The input connnectionSet is non-empty.  Arbitrarily returning the 1st connection as invalid: " +
                                     conn.toString());

            invalidConnections.add(conn);
            break;
        }

        Tr.exit(tc, methodName);
        return invalidConnections;
    }

    public boolean testConnection() throws ResourceException {
        if (testConnectionValue) {
            return true;
        } else {
            ResourceException ee = new ResourceException("Connection is bad");
            throw ee;
        }
    }

    public void setTestConnection(boolean value) {
        if (tc.isDebugEnabled()) {
            if (testConnectionValue) {
                Tr.debug(tc, "test connection is good, no exception");
            } else {
                Tr.debug(tc, "test connection is set to throw exception");
            }
        }
        testConnectionValue = value;
        testDatabaseValue = value; // set database value to match test connection.
    }

    public void setPoolJNDIName(String poolJNDIName) {
        this.poolJNDIName = poolJNDIName;
    }

    private String getJNDIName() {
        return poolJNDIName;
    }

    private Boolean getTestConnectionValue() {
        return new Boolean(testConnectionValue);
    }

    public void testDatabase() throws ResourceException {
        if (testDatabaseValue) {
            // good connection, do nothing.
        } else {
            ResourceException ee = new ResourceException("Database is bad");
            throw ee;
        }
    }

    public void setTestDatabase(boolean value) {
        if (tc.isDebugEnabled()) {
            if (testDatabaseValue) {
                Tr.debug(tc, "test database is good, no exception");
            } else {
                Tr.debug(tc, "test database is set to throw exception");
            }
        }
        testDatabaseValue = value;
    }

}
