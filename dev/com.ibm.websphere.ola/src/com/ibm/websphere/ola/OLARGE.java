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

package com.ibm.websphere.ola;

import java.io.UnsupportedEncodingException;
import java.util.List;


public interface OLARGE {

	/*
	 * 
	 * 	STRING VALUES
	 * 
	 */
	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeEye()
	 */
	public abstract String get_argeEye() throws Exception;

	/**
	 * Parses the arge name from shared memory, and sets the member attribute.
	 * @throws UnsupportedEncodingException if it cannot convert to codepage 1047
	 * @throws OLAException if the eye does not have the appropriate
	 * value of "BBOARGE "
	 */
	public abstract void parseArgeEye() throws UnsupportedEncodingException,
			OLAException;

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeName()
	 */
	public abstract String get_argeName() throws Exception;

	/**
	 * Parses the arge name from shared memory, and sets the member attribute.
	 * @throws UnsupportedEncodingException if it cannot convert to codepage 1047
	 */
	public abstract void parseArgeName() throws UnsupportedEncodingException;

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeJobName()
	 */
	public abstract String get_argeJobName() throws Exception;

	/**
	 * Parses the arge job name from shared memory, and sets the member attribute.
	 * @throws UnsupportedEncodingException if it cannot convert to codepage 1047
	 */
	public abstract void parseArgeJobName() throws UnsupportedEncodingException;

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeUUID()
	 */
	public abstract String get_argeUUID() throws Exception;

	/**
	 * Parses the UUID name from shared memory, and sets the member attribute.
	 * @throws UnsupportedEncodingException if it cannot convert to codepage 1047
	 */
	public abstract void parseArgeUUID() throws UnsupportedEncodingException;

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeVersion()
	 */
	public abstract Short get_argeVersion();

	/**
	 * Parses the Version from shared memory, and sets the member attribute.
	 * @ if it cannot convert to codepage 1047
	 */
	public abstract void parseArgeVersion();

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeSize()
	 */
	public abstract Integer get_argeSize();

	/**
	 * Parses the Size from shared memory, and sets the member attribute.
	 * @ if it cannot convert to codepage 1047
	 */
	public abstract void parseArgeSize();

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeJobNum()
	 */
	public abstract Integer get_argeJobNum();

	/**
	 * Parses the job number from shared memory, and sets the member attribute.
	 * @ if it cannot convert to codepage 1047
	 */
	public abstract void parseArgeJobNum();

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeConnectionsMin()
	 */
	public abstract Integer get_argeConnectionsMin();

	/**
	 * Parses the minimum connections value from shared memory, and sets the member attribute.
	 * @ if it cannot convert to codepage 1047
	 */
	public abstract void parseArgeConnectionsMin();

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeConnectionsMax()
	 */
	public abstract Integer get_argeConnectionsMax();

	/**
	 * Parses the maximum connections value from shared memory, and sets the member attribute.
	 * @ if it cannot convert to codepage 1047
	 */
	public abstract void parseArgeConnectionsMax();

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeActiveConnectionCount()
	 */
	public abstract Long get_argeActiveConnectionCount();

	/**
	 * Parses the number of current active connections from shared memory, and sets the member attribute.
	 */
	public abstract void parseArgeActiveConnectionCount();

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeActiveConnectionCount()
	 */
	public abstract Integer get_argeTraceLevel();

	/**
	 * Parses the trace level from shared memory, and sets the member attribute.
	 */
	public abstract void parseArgeTraceLevel();

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeActiveConnectionCount()
	 */
	public abstract Long get_argeState();

	/**
	 * Parses the RGE state from shared memory, and sets the member attribute.
	 */
	public abstract void parseArgeState();

	/*
	 * 
	 * BOOLEAN VALUES
	 * These values are stored in shorts so it is possible to signify if they are unset
	 * 
	 */

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeFlagDaemon()
	 */
	public abstract Boolean get_argeFlagDaemon();

	/**
	 * Parses the type, transaction, and active flags from shared memory, and sets 
	 * the member attribute.
	 * 
	 * This is done all at once because these flags are all stored in the same
	 * byte.
	 * 
	 * The bits are set as follows, from left to right:
	 * 1 - RGE daemon type flag
	 * 2 - RGE server type flag
	 * 3 - RGE external address space type flag
	 * 
	 * 7 - Transactions supported flag
	 * 8 - Active RGE flag
	 */
	public abstract void parseArgeFlags();

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeFlagServer()
	 */
	public abstract Boolean get_argeFlagServer();

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeFlagExternalAddress()
	 */
	public abstract Boolean get_argeFlagExternalAddress();

	public abstract Boolean get_argeFlagSecurity();

	public abstract Boolean get_argeFlagWLM();

	public abstract Boolean get_argeFlagTransaction();

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeFlagActive()
	 */
	public abstract Boolean get_argeFlagActive();

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeServerRGEOffset()
	 */
	public abstract long get_argeServerRGEAddress();

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeNextRGEOffset()
	 */
	public abstract long get_argeNextRGEAddress();

	/* (non-Javadoc)
	 * @see com.ibm.ws390.ola.OLARGEImpl#get_argeConnectionPoolRGEOffset()
	 */
	public abstract long get_argeConnectionPoolRGEAddress();

	public abstract long get_absoluteAddress();

	public abstract List<OLAConnectionHandle> get_connectionHandles();

	/**
	 * Sets all the values from shared memory. This method is to be called before
	 * returning an object to a user. This is because once it is sent elsewhere 
	 * (eg a different jvm) it will not have access to the shared memory byte buffer,
	 * so it retrieves all of its values while it still can.
	 * @throws Exception 
	 */
	public abstract void parseAllAttributes() throws Exception;

	public abstract void parseConnectionHandles();

	/**
	 * toString method for OLA RGE. Uses getter methods, which will
	 * parse values form the byte buffer as needed.
	 */
	public abstract String toString();

}