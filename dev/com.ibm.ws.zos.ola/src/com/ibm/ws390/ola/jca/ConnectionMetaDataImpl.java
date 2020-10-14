/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.ola.jca;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.spi.ManagedConnectionMetaData;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Some meta-data for the Connection.  We pull this stuff out of the 
 * ManagedConnectionMetaData.  This way we only have to implement it once.
 */
public class ConnectionMetaDataImpl implements ConnectionMetaData {
	
	private static final TraceComponent tc = Tr.register(ConnectionMetaDataImpl.class);

	/**
	 * The eisName string.
	 */
	java.lang.String eisName = null;

	/**
	 * The version of the connector.
	 */
	java.lang.String version = null;
	
	/**
	 * The username we are using.
	 */
	java.lang.String userName = null;
	
	/**
	 * Default Constructor.
	 * Copies all of the information from the ManagedConnectionMetaData.
	 * @throws ResourceException 
	 */
	public ConnectionMetaDataImpl(ManagedConnectionMetaData mData) throws ResourceException
	{
		eisName = mData.getEISProductName();
		version = mData.getEISProductVersion();
		userName = mData.getUserName();
	}

	/**
     * Gets the EIS name.
	 * @see javax.resource.cci.ConnectionMetaData#getEISProductName()
	 */
	public String getEISProductName() throws ResourceException {
		return eisName;
	}

	/**
	 * Gets the EIS version.
	 * @see javax.resource.cci.ConnectionMetaData#getEISProductVersion()
	 */
	public String getEISProductVersion() throws ResourceException {
		return version;
	}

	/**
	 * Gets the username.
	 * @see javax.resource.cci.ConnectionMetaData#getUserName()
	 */
	public String getUserName() throws ResourceException {
		return userName;
	}

}
