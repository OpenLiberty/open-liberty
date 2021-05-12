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

import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tra14.SimpleRAImpl;
import com.ibm.tra14.trace.DebugTracer;

/**
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
//@SuppressWarnings("serial")
public class ConnectionFactoryBase implements javax.resource.cci.ConnectionFactory {

    //@SuppressWarnings("unused")
    private int loginTimeout;
    private String defaultUser = "defaultUser";
    private String defaultPassword = "defaultPwd";
    private ManagedConnectionFactoryBase mcf = null;
    private ConnectionManager conManager = null;
    private Reference ref;

    private static final String _className = "ConnectionFactoryBase Vers 2";

    private static final TraceComponent tc = Tr.register(ConnectionFactoryBase.class, SimpleRAImpl.RAS_GROUP, null);

    public ConnectionFactoryBase(ManagedConnectionFactoryBase cciMcf, ConnectionManager cm) {
        System.out.println("CCIConnectionFactoryImpl constructor with CCIManagedConnectionFactoryImpl=" + cciMcf + ", cm=" + cm);
        mcf = cciMcf;
        conManager = cm;
        DebugTracer.printClassLoaderInfo(_className, this);
        DebugTracer.printStackDump(_className, new Exception());
        /*
         * System.out.println("***!*** Printing debug information for ConnectionFactoryImpl constructor ***!***");
         * System.out.println("* Debug Resource Adapter Version: 2");
         * System.out.println("* Current ClassLoader: " + ConnectionFactoryBase.class.getClassLoader().toString());
         * System.out.println("* Context ClassLoader: " + Thread.currentThread().getContextClassLoader().toString());
         * System.out.println("* Stack Dump: ");
         * Exception e = new Exception();
         * e.printStackTrace(System.out);
         * System.out.println("***!*** End debug information for ConnectionFactoryImpl Constructor ***!***");
         */
    }

    public ConnectionFactoryBase(ManagedConnectionBase mc, ConnectionRequestInfoBase cxReqInfo) {
        System.out.println("CCIConnectionFactoryImpl constructor without CCIManagedConnectionFactoryImpl");
    }

    /**
     * @see javax.sql.DataSource#getConnection()
     */
    @Override
    public Connection getConnection() throws ResourceException {
        final String methodName = "getConnection";
        Tr.entry(tc, methodName);

        System.out.println("ConnectionfactoryImpl: getConnection no arg");
        ConnectionSpecBase cxSpec = new ConnectionSpecBase();
        cxSpec.setUserName(defaultUser);
        cxSpec.setPassword(defaultPassword);
        cxSpec.setEISStatus(ConnectionBase.EIS_OK);

        Tr.debug(tc, methodName,
                 "Generating a new connection based upon the defaults ");

        // LIDB3598-34: comment out the old code
        //CCIConnectionImpl con = new fvt.cciadapter.CCIConnectionImpl();

        // LIDB3598-34: add new code to get CCIConnection
        ConnectionRequestInfo requestInfo = new ConnectionRequestInfoBase("user", "password");
        ConnectionBase con = (ConnectionBase) conManager.allocateConnection(mcf, requestInfo);

        //Set the status to OK on our fake EIS backend
        con.initialize(cxSpec);

        Tr.exit(tc, methodName, con);
        return con;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.sql.DataSource#getConnection(ConnectionSpec)
     */
    @Override
    public Connection getConnection(ConnectionSpec properties) throws ResourceException {
        final String methodName = "getConnection";
        Tr.entry(tc, methodName);

        System.out.println("ConnectionfactoryImpl: getConnection with ConnectionSpec, properties=" + properties);

        ConnectionSpecBase cxSpec = null;
        try {
            cxSpec = (ConnectionSpecBase) properties;
        } catch (ClassCastException cce) {
            Tr.debug(tc, methodName,
                     "Warning:  Properties is not a valid CCIConnectionSpecImpl.  Getting default connection. ");

            return getConnection();
        }

        Tr.debug(tc, methodName,
                 "Initializing a new connection using user specified properties");

        // LIDB3598-34: comment out the old code
        //CCIConnectionImpl con = new fvt.cciadapter.CCIConnectionImpl();

        // LIDB3598-34: add new code to get CCIConnection
        ConnectionRequestInfo requestInfo = null;
        ConnectionSpecBase specImpl = (ConnectionSpecBase) properties;
        if (specImpl == null)
            requestInfo = new ConnectionRequestInfoBase("user", "password");
        else
            requestInfo = new ConnectionRequestInfoBase(specImpl.getUserName(), specImpl.getPassword());
        ConnectionBase con = (ConnectionBase) conManager.allocateConnection(mcf, requestInfo);

        //Set the status to OK on our fake EIS backend
        con.initialize(cxSpec);

        Tr.exit(tc, methodName, con);

        return con;
    }

    /**
     * @see javax.sql.DataSource#getMetaData()
     */
    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        final String methodName = "getMetaData";
        Tr.entry(tc, methodName);

        ResourceAdapterMetaDataBase resMeta = new ResourceAdapterMetaDataBase();

        Tr.exit(tc, methodName);
        return resMeta;
    }

    /**
     * @see javax.sql.DataSource#getRecordFactory()
     */
    @Override
    public RecordFactory getRecordFactory() throws ResourceException {
        final String methodName = "getRecordFactory";
        Tr.entry(tc, methodName);

        RecordFactoryBase recFac = new RecordFactoryBase();

        Tr.exit(tc, methodName);

        return recFac;
    }

    @Override
    public void setReference(Reference reference) {
        ref = reference;
    }

    @Override
    public Reference getReference() {
        return ref;
    }

}
