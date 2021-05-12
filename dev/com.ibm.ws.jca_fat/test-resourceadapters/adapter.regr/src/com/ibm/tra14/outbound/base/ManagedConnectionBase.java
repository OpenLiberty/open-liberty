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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tra14.SimpleRAImpl;
import com.ibm.tra14.trace.DebugTracer;
import com.ibm.ws.j2c.InteractionMetrics;

/**
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ManagedConnectionBase implements ManagedConnection {

    //@SuppressWarnings("unchecked")
    private static Set hsCCIManagedConnections = Collections.synchronizedSet(new HashSet());

    //@SuppressWarnings("unchecked")
    private Set myListeners;
    private ConnectionRequestInfoBase cxRequestInfo;
    private ManagedConnectionFactoryBase mcf;
    private PrintWriter logWriter;
    //@SuppressWarnings("unused")
    private boolean destroyed;
    //@SuppressWarnings("unchecked")
    private Set connectionSet;
    private InteractionMetrics metricsListener;

    private final String className = "ManagedConnectionBase Ver 2";

    private static final TraceComponent tc = Tr.register(ManagedConnectionBase.class, SimpleRAImpl.RAS_GROUP, null);

    //@SuppressWarnings("unchecked")
    public ManagedConnectionBase(ManagedConnectionFactoryBase mcf, ConnectionRequestInfoBase reqInfo) {
        final String methodName = "CCIManagedConnectionImpl";
        Tr.entry(tc, methodName, new Object[] { mcf, reqInfo });

        this.mcf = mcf;
        this.cxRequestInfo = reqInfo;
        connectionSet = new HashSet();
        myListeners = new HashSet();
        destroyed = false;

        hsCCIManagedConnections.add(this);

        DebugTracer.printClassLoaderInfo(className, this);
        DebugTracer.printStackDump(className, new Exception());
        /*
         * System.out.println("***!*** Printing debug information for ManagedConnectionImpl constructor ***!***");
         * System.out.println("* Debug Resource Adapter Version: 2");
         * System.out.println("* Current ClassLoader: " + ManagedConnectionBase.class.getClassLoader().toString());
         * System.out.println("* Context ClassLoader: " + Thread.currentThread().getContextClassLoader().toString());
         * System.out.println("* Stack Dump: ");
         * Exception e = new Exception();
         * e.printStackTrace(System.out);
         * System.out.println("***!*** End debug information for ManagedConnectionImpl Constructor ***!***");
         */

        Tr.exit(tc, methodName);
    }

    /**
     * @see javax.resource.spi.ManagedConnection#getConnection(javax.security.auth.Subject, javax.resource.spi.ConnectionRequestInfo)
     */
    @Override
    public Object getConnection(Subject subj, ConnectionRequestInfo reqInfo) throws ResourceException {

        final String methodName = "getConnection";
        Tr.entry(tc, methodName, new Object[] { subj, cxRequestInfo });

        // When enabled, testDatabase will throw a resource exception to simulate a down database
        mcf.testDatabase();

        ConnectionRequestInfoBase myReqInfo = null;
        if (reqInfo != null && reqInfo instanceof ConnectionRequestInfoBase)
            myReqInfo = (ConnectionRequestInfoBase) reqInfo;
        else
            throw new ResourceException("Invalid ConnectionRequestInfo.");

        ConnectionBase connection = new ConnectionBase(this, myReqInfo);
        addCCIConnection(connection);

        Tr.debug(tc, methodName,
                 "Added a new connection to this ManagedConnection " + this.toString() + ".  Total number of connections: " + connectionSet.size());

        Tr.exit(tc, methodName, connection);
        return connection;
    }

    /**
     * Destroys the physical connection to the underlying resource manager.
     * <P>
     * To manage the size of the connection pool, an application server can
     * explictly call ManagedConnection.destroy to destroy a physical connection.
     * A resource adapter should destroy all allocated system resources for this
     * ManagedConnection instance when the method destroy is called.
     */
    @Override
    public void destroy() throws ResourceException {
        final String methodName = "destroy";
        Tr.entry(tc, methodName);

        destroyed = true;

        Tr.exit(tc, methodName);
    }

    /**
     * Application server calls this method to force any cleanup on the ManagedConnection
     * instance.
     * <P>
     * The method ManagedConnection.cleanup initiates a cleanup of the any client-specific
     * state as maintained by a ManagedConnection instance. The cleanup should invalidate
     * all connection handles that had been created using this ManagedConnection instance.
     * Any attempt by an application component to use the connection handle after cleanup
     * of the underlying ManagedConnection should result in an exception.
     * <P>
     * The cleanup of ManagedConnection is always driven by an application server.
     * An application server should not invoke ManagedConnection.cleanup when there is an
     * uncompleted transaction (associated with a ManagedConnection instance) in progress.
     * <P>
     * The invocation of ManagedConnection.cleanup method on an already cleaned-up connection
     * should not throw an exception.
     * <P>
     * The cleanup of ManagedConnection instance resets its client specific state and prepares
     * the connection to be put back in to a connection pool. The cleanup method should not
     * cause resource adapter to close the physical pipe and reclaim system resources associated
     * with the physical connection.
     */
    @Override
    public void cleanup() throws ResourceException {
        final String methodName = "cleanup";
        Tr.entry(tc, methodName);

        Tr.exit(tc, methodName);
    }

    /**
     * Used by the container to change the association of an application-level connection
     * handle with a ManagedConneciton instance. The container should find the right
     * ManagedConnection instance and call the associateConnection method.
     * <P>
     * The resource adapter is required to implement the associateConnection method.
     * The method implementation for a ManagedConnection should dissociate the connection
     * handle (passed as a parameter) from its currently associated ManagedConnection and
     * associate the new connection handle with itself.
     */
    //@SuppressWarnings("unchecked")
    @Override
    public void associateConnection(Object connection) throws ResourceException {
        final String methodName = "associateConnection";
        Tr.entry(tc, methodName, connection);

        ConnectionBase myConnection;
        if (connection instanceof ConnectionBase)
            myConnection = (ConnectionBase) connection;
        else
            throw new ResourceException("Invalid Connection handle.");

        for (Iterator it = hsCCIManagedConnections.iterator(); it.hasNext();) {
            ManagedConnectionBase myMC = (ManagedConnectionBase) it.next();

            if (myMC.hasConnection(myConnection)) {
                myMC.dissociateConnection(myConnection);
                Tr.debug(tc, methodName,
                         "Dissociated connection with ManagedConnection " + myMC.toString());
                break;
            }
        }

        connectionSet.add(myConnection);
        Tr.debug(tc, methodName,
                 "Associated connection with this ManagedConnection " + this.toString() + ". Current number of connections: " + connectionSet.size());

        Tr.exit(tc, methodName);
    }

    private boolean hasConnection(ConnectionBase connection) {
        return connectionSet.contains(connection);
    }

    private void dissociateConnection(ConnectionBase connection) {
        connectionSet.remove(connection);
    }

    /**
     * @see javax.resource.spi.ManagedConnection#addConnectionEventListener(javax.resource.spi.ConnectionEventListener)
     */
    //@SuppressWarnings("unchecked")
    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        final String methodName = "addConnectionEventListener";
        Tr.entry(tc, methodName, listener);

        myListeners.add(listener);

        if (listener instanceof InteractionMetrics) {
            System.out.println("CCIManagedConnectionImpl addConnectionEventListener InteractionMetrics listener=" + listener);
            metricsListener = (InteractionMetrics) listener;
        }

        Tr.exit(tc, methodName);
    }

    /**
     * @see javax.resource.spi.ManagedConnection#removeConnectionEventListener(javax.resource.spi.ConnectionEventListener)
     */
    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        final String methodName = "removeConnectionEventListener";
        Tr.entry(tc, methodName, listener);

        myListeners.remove(listener);

        Tr.exit(tc, methodName);
    }

    /**
     * @see javax.resource.spi.ManagedConnection#getXAResource()
     */
    @Override
    public XAResource getXAResource() throws ResourceException {
        final String methodName = "getXAResource";
        Tr.entry(tc, methodName);

        Tr.exit(tc, methodName);
        throw new NotSupportedException();
    }

    /**
     * @see javax.resource.spi.ManagedConnection#getLocalTransaction()
     *      Creating a local transaction and returning it for the user to use. Note that at this point this
     *      is a fake local transaction and really isn't going to be used to demarcate anything.
     */
    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        final String methodName = "getLocalTransaction";
        Tr.entry(tc, methodName);

        LocalTransaction localTran = new LocalTransactionBase(new ConnectionMetaDataBase(), metricsListener);

        Tr.exit(tc, methodName, localTran);
        return localTran;
    }

    /**
     * @see javax.resource.spi.ManagedConnection#getMetaData()
     */
    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        final String methodName = "getMetaData";
        Tr.entry(tc, methodName);

        ManagedConnectionMetaData data = new ManagedConnectionMetaDataBase(this);

        Tr.exit(tc, methodName, data);
        return data;
    }

    /**
     * @see javax.resource.spi.ManagedConnection#setLogWriter(java.io.PrintWriter)
     */
    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
        final String methodName = "setLogWriter";
        Tr.entry(tc, methodName);

        logWriter = out;

        Tr.exit(tc, methodName);
    }

    /**
     * @see javax.resource.spi.ManagedConnection#getLogWriter()
     */
    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        final String methodName = "getLogWriter";
        Tr.entry(tc, methodName);

        Tr.exit(tc, methodName);
        return logWriter;
    }

    String getUserName() {
        return cxRequestInfo.getUser();
    }

    String getPassword() {
        return cxRequestInfo.getPassword();
    }

    void removeCCIConnection(ConnectionBase myCon) {
        connectionSet.remove(myCon);
    }

    //@SuppressWarnings("unchecked")
    void addCCIConnection(ConnectionBase myCon) {
        connectionSet.add(myCon);
    }

    ManagedConnectionFactoryBase getManagedConnectionFactory() {
        return mcf;
    }

    //@SuppressWarnings("unchecked")
    void NotifyConnectionClosed() {
        NotifyConnectionClosed(null);
    }

    void NotifyConnectionClosed(javax.resource.cci.Connection connectionHandle) {
        final String methodName = "NotifyConnectionClosed";
        Tr.entry(tc, methodName);

        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        if (connectionHandle != null) {
            event.setConnectionHandle(connectionHandle);
        }
        for (Iterator it = myListeners.iterator(); it.hasNext();) {
            ((ConnectionEventListener) it.next()).connectionClosed(event);
        }

        Tr.exit(tc, methodName);
    }

    //@SuppressWarnings("unchecked")
    void NotifyConnectionErrorOccurred() {
        final String methodName = "NotifyConnectionErrorOccurred";
        Tr.entry(tc, methodName);

        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_ERROR_OCCURRED);
        for (Iterator it = myListeners.iterator(); it.hasNext();) {
            ((ConnectionEventListener) it.next()).connectionErrorOccurred(event);
        }

        Tr.exit(tc, methodName);
    }

    @Override
    public String toString() {
        final String ID = "CCIManagedConnectionImpl";
        final char SEP = ':';
        StringBuffer buf = new StringBuffer(ID);
        buf.append(SEP).append(getUserName()).append(SEP).append(getPassword());
        return buf.toString();
    }

    protected InteractionMetrics getInteractionListener() {
        return metricsListener;
    }

    public boolean testConnection() throws ResourceException {
        return mcf.testConnection();
    }

    public Object getMCF() {
        return mcf;
    }
}
