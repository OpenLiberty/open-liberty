/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.ola.jca;                                   /* @F003691C*/

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;
import javax.resource.spi.ManagedConnectionMetaData;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo;
import com.ibm.ws.zos.channel.wola.WolaOtmaRequestInfo;

/**
 * This is the Connection that the application uses to do things to our resource.  Implements
 * the javax.resource.cci.Connection interface.
 */
public class ConnectionImpl implements Connection {

	private static final TraceComponent tc = Tr.register(ConnectionImpl.class);
	private static final TraceUtil tu = new TraceUtil(tc);

	/**
	 * Trace header.
	 */
	private String header = new String(" !!ConnectionImpl " + System.identityHashCode(this) + ": ");

	/**
	 * Holds a reference to the ManagedConnection for this Connection.  The
	 * ManagedConnection is an spi object, and lives in the server.  If the
   * ManagedConnection is null, then this Connection has been invalidatd.
	 */
	private ManagedConnectionImpl mc = null;

	/**
	 * Boolean flag indicating whether or not we issue debugging messages.
	 */
	public boolean debugMode = false;

	/**
	 * List of interactions opened by the connection
	 */
	private List<InteractionImpl> interactions = new LinkedList<InteractionImpl>();

	/**
	 * Constructor.  Called from the ManagedConnectionImpl.
	 */
	public ConnectionImpl(ManagedConnectionImpl mc)
	{
		this.mc = mc;
    this.debugMode = mc.isDebugMode();
        
    tu.debug("ConnectionImpl: In constructor mc.getOTMAGroupID() : "+mc.getOTMAGroupID(), this.debugMode);
    tu.debug("ConnectionImpl: In constructor mc.getOTMAServerName() : "+mc.getOTMAServerName(), this.debugMode);
    tu.debug("ConnectionImpl: In constructor mc.getOTMAClientName() : "+mc.getOTMAClientName(), this.debugMode);
 	}

	/**
	 * Creates an interaction with the connection.  Not implemented.
	 * @see javax.resource.cci.Connection#createInteraction()
	 */
	public Interaction createInteraction() throws ResourceException
	{
    tu.debug(header + "inside createInteraction()... new up InteractionImpl", debugMode); /* @580321C*/
		
    if (mc == null) throw new ResourceException("Connection is invalid");

		InteractionImpl interaction = new InteractionImpl(this);
		
		interactions.add(interaction);
		
		return interaction;
	}

	/**
	 * Gets the localTransaction object for this connection.  An application
	 * client would use this to demarcate local transactions.
	 * @see javax.resource.cci.Connection#getLocalTransaction()
	 */
	public LocalTransaction getLocalTransaction() throws ResourceException
	{
		// TODO: Before handing this out, we should make sure that the RGE supports
		//       local transactions.  If not, we should throw an exception.
    if (mc == null) throw new ResourceException("Connection is invalid");

		return new LocalTransactionCciImpl(mc);                      /* @F003691C*/
	}

	/**
	 * Gets the metadata for this connection.
	 * Rather than implement the metadata twice, I take the metadata from the
	 * ManagedConnection and "reinterpret" it for this metadata.  This may not be
	 * politically correct, but we're not trying to be either.
	 * @see javax.resource.cci.Connection#getMetaData()
	 */
	public ConnectionMetaData getMetaData() throws ResourceException
	{
    if (mc == null) throw new ResourceException("Connection is invalid");

		ManagedConnectionMetaData mData = mc.getMetaData();
		
		return new ConnectionMetaDataImpl(mData);	
	}

	/**
	 * Gets the results set.  Not implemented.
	 * @see javax.resource.cci.Connection#getResultSetInfo()
	 */
	public ResultSetInfo getResultSetInfo() throws ResourceException {
		throw new ResourceException("ResultSet not supported");
	}

	/**
	 * Closes the connection.  Contacts the managed connection and notifies
	 * that it is closed, then nulls out the reference to the managed
	 * connection.  This may or may not be the appropriate behavior, but
	 * for the purposes of testing our XAResource, this will do.  We'll behave.
	 * @see javax.resource.cci.Connection#close()
	 */
	public void close() throws ResourceException
	{
		if (mc == null)
		    throw new ResourceException("Connection Already Closed");
		mc.connectionClosed(this);
		mc = null;
		
		Iterator<InteractionImpl> i = interactions.iterator();
		while (i.hasNext())
		{
			InteractionImpl interaction = i.next();
			interaction.close();
			i.remove();
		}
	}

  /**
   * Invalidates the connection.  Called by the ManagedConnection when
   * cleanup is invoked but this connection is still associated with it.
   */
  protected void invalidate(ManagedConnectionImpl mci)
  {
    tu.debug(header + "enter invalidate", debugMode);

    /* Only invalidate if this connection is exactly associated */
    if (mci == mc) mc = null;
  }

  /**
   * Changes the association from one ManagedConnection to another.
   */
  protected void changeAssociation(ManagedConnectionImpl mci)
  {
    tu.debug(header + "enter changeAssociation", debugMode);

    mc.dissociateConnection(this);
    mc = mci;
    debugMode = mci.isDebugMode();
  }


  /**
   * @return extra request info for invoking into CICS.
   */
  public WolaJcaRequestInfo getWolaJcaRequestInfo() {
    return mc;
  }
  
    /**
     * @return extra request info for invoking OTMA
     */
    public WolaOtmaRequestInfo getWolaOtmaRequestInfo() {
		return mc;
	}

	/**
	 * Tells us whether or not we are running in debug mode.
	 */
	protected boolean isDebugMode()
	{
		return debugMode; /* @580321C*/
	}

  /**
   * Informs the MC that we are driving an interaction
   */
  protected void aboutToDriveInteraction()                       /* @F003691A*/
  {
    mc.aboutToDriveInteraction();
  }
  
    /**
	 * Gets the Use OTMA flag. 
	 */
	protected int getUseOTMA() 
	{
		return mc.getUseOTMA();                                            /* @F003694A*/
	}
 
	/**
	 * Gets the OTMA XCF Group ID. 
	 */
	protected String getOTMAGroupID()                                      /* @F003694A*/
	{
	    tu.debug(header + "getOTMAGroupID called, " + mc.getOTMAGroupID(), debugMode);
	    return mc.getOTMAGroupID();
	}

	/**
	 * Gets the OTMA XCF Server name. 
	 */
	protected String getOTMAServerName()                                   /* @F003694A*/
	{
	    tu.debug(header + "getOTMAServerName called, " + mc.getOTMAServerName(), debugMode);
	    return mc.getOTMAServerName();
	}

	/**
	 * Gets the OTMA XCF Client name. 
	 */
	protected String getOTMAClientName()
	{
	    tu.debug(header + "getOTMAClientName called, " + mc.getOTMAClientName(), debugMode);
	    return mc.getOTMAClientName();
	}

	/**
	 * Gets the OTMA Sync Level setting 
	 */
	protected String getOTMASyncLevel()                                   /* @F003694A*/
	{
	    tu.debug(header + "getOTMASyncLevel called, " + mc.getOTMASyncLevel(), debugMode);
	    return mc.getOTMASyncLevel();
	}

    /**
	 * Gets the OTMA Request LLZZ flag. 
	 */
	protected int getOTMAReqLLZZ() 
	{
		return mc.getOTMAReqLLZZ();                                      /* @670111A*/
	}

    /**
	 * Gets the OTMA Response LLZZ flag. 
	 */
	protected int getOTMARespLLZZ() 
	{
		return mc.getOTMARespLLZZ();                                      /* @670111A*/
	}
	
	/**
	 * Gets the OTMA response segment count (max).
	 */
	protected int getOTMAMaxSegments() {
		return mc.getOTMAMaxSegments();
	}
	
	/**
	 * Gets the OTMA response size (max).
	 */
	protected int getOTMAMaxRecvSize() {
		return mc.getOTMAMaxRecvSize();
	}

    /**
	 * Gets the RRS Transactional flag. 
	 */
	protected boolean getRRSTransactional() 
	{
		return mc.getRRSTransactional();                                 /* @F014447A*/
	}
	
	/**
	 * @return the WOLA client's registration name
	 */
	protected String getRegisterName() {
	    return mc.getRegisterName();
	}

  /**
   * Gets the remote proxy information on non-z/OS platforms
   */
  public RemoteProxyInformation getRemoteProxyInformation()      /* @F003705A*/
  {
    return mc.getRemoteProxyInformation();
  }

  /**
   * Creates a new ConnectionSpecImpl based on the current state of
   * the ManagedConnection.
   */
  protected com.ibm.websphere.ola.ConnectionSpecImpl             /* @F003705A*/
    createConnectionSpecFromManagedConnection()
  {
    return mc.createConnectionSpecFromManagedConnection();
  }
}
