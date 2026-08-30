/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.ola.jca;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;

import com.ibm.websphere.ola.ConnectionSpecImpl;

/**
 * Implementation of the javax.resource.cci.ConnectionFactory interface required for
 * a JCA connector.  The Connection factory is looked up in JNDI by the client, and
 * used to create connections to the connector backend.
 */
public class ConnectionFactoryImpl
	implements javax.resource.cci.ConnectionFactory {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1798258451328804007L;
	
	/**
	 * Reference to this connection factory.  This is what gets bound
	 * into JNDI.  The application (EJB) looks this up and calls getConnection() to
	 * connect to this resource.
	 */
	private javax.naming.Reference reference;

	/**
	 * Holds a reference to the ManagedConnectionFactory
	 */
	private ManagedConnectionFactory mcf;
	
	/**
	 * Holds a reference to the ConnectionManager
	 */
	private ConnectionManager cm;

	/**
	 * Holds a reference to RA Metadata
	 */
	private ResourceAdapterMetaData raMeta;                       /* F013381A*/
	

	/**
	 * Constructor that takes a connection manager and a managed connection 
	 * factory.
	 */
	public ConnectionFactoryImpl(ManagedConnectionFactory mcf,
                                  ConnectionManager cm)
    	throws ResourceException
    {
    	this.mcf = mcf;
		if (cm == null) throw new ResourceException("ConnectionManager was null");
		else this.cm = cm;
    }

	/**
	 * Gets a connection to this resource.  
	 * Added support for this method with new code for BPM/WID.        @F013381A
	 * @see javax.resource.cci.ConnectionFactory#getConnection()
	 */
	public Connection getConnection() throws ResourceException 
	{
		ConnectionSpecImpl csi = new ConnectionSpecImpl();  /* @F013381A*/
		return getConnection(csi);                          /* @F013381C*/
	}

	/**
	 * Gets a connection to this resource with the specified register name.
	 * @see javax.resource.cci.ConnectionFactory#getConnection(ConnectionSpec)
	 */
	public Connection getConnection(ConnectionSpec cspec)
		throws ResourceException 
	{
		
		// Make sure that the ConnectionSpec is one of ours.
		if ((cspec == null) || (!(cspec instanceof ConnectionSpecImpl)))
		{
			throw new ResourceException("ConnectionSpec is invalid");
		}
		
		// Change the ConnectionSpec to a ConnectionRequestInfo.
		ConnectionSpecImpl cspecImpl = (ConnectionSpecImpl)cspec;
		
		ConnectionRequestInfoImpl crii = 
			new ConnectionRequestInfoImpl(cspecImpl.getRegisterName(),
					cspecImpl.getConnectionWaitTimeout(),
					cspecImpl.getLinkTaskTranID(),
					cspecImpl.getLinkTaskReqContID(),
					cspecImpl.getLinkTaskReqContType(),
					cspecImpl.getLinkTaskRspContID(),
					cspecImpl.getLinkTaskRspContType(),
					cspecImpl.getUseCICSContainer(),
					cspecImpl.getLinkTaskChanID(),                      /* @F014448A */
					cspecImpl.getLinkTaskChanType(),                    /* @F014448A */
					cspecImpl.getUseOTMA(),
					cspecImpl.getOTMAClientName(),
					cspecImpl.getOTMAServerName(),
					cspecImpl.getOTMAGroupID(),
					cspecImpl.getOTMASyncLevel(),
					cspecImpl.getOTMAMaxSegments(),
					cspecImpl.getOTMAMaxRecvSize(),
					cspecImpl.getOTMARequestLLZZ(),
					cspecImpl.getOTMAResponseLLZZ(),    /* @670111C*/
					cspecImpl.getUsername(),                               /* @F003705A*/
					cspecImpl.getPassword(),
					cspecImpl.getRRSTransactional(),                    /* @F014447A*/
					cspecImpl.getConnectionWaitTimeoutFromCSI(),		/* @F013381A*/
					cspecImpl.getlinkTaskReqContTypeFromCSI(),			/* @F013381A*/
					cspecImpl.getlinkTaskRspContTypeFromCSI(),			/* @F013381A*/
					cspecImpl.getlinkTaskChanTypeFromCSI(),             /* @F014448A */
					cspecImpl.getuseCICSContainerFromCSI(),				/* @F013381A*/
					cspecImpl.getOTMAMaxSegmentsFromCSI(),				/* @F013381A*/
					cspecImpl.getOTMAMaxRecvSizeFromCSI(),				/* @F013381A*/
					cspecImpl.getOTMARequestLLZZFromCSI(),				/* @F013381A*/
					cspecImpl.getOTMAResponseLLZZFromCSI(),             /* @F013381A*/
					cspecImpl.getRRSTransactionalFromCSI());            /* @F014447A*/
		
		// Get the connection
		javax.resource.cci.Connection conn = null;
		conn = (Connection) cm.allocateConnection(mcf, crii);
		return conn;
	}

	/**
	 * Gets a RecordFactory for this resource.
	 * @see javax.resource.cci.ConnectionFactory#getRecordFactory()
	 */
	public RecordFactory getRecordFactory() throws ResourceException 
	{
		return new RecordFactoryImpl();
	}

	/**
	 * Gets the MetaData for this connection factory.
	 * @see javax.resource.cci.ConnectionFactory#getMetaData()
	 */
	public ResourceAdapterMetaData getMetaData() throws ResourceException {

		raMeta = new ResourceAdapterMetaDataImpl(raMeta);

		return raMeta;
	}

	/**
	 * Sets the reference that is bound into JNDI.
	 * @see javax.resource.Referenceable#setReference(Reference)
	 */
	public void setReference(Reference arg0) {
		reference = arg0;
	}

	/**
	 * Gets the reference that is bound into JNDI.
	 * @see javax.naming.Referenceable#getReference()
	 */
	public Reference getReference() throws NamingException {
		return reference;
	}

}
