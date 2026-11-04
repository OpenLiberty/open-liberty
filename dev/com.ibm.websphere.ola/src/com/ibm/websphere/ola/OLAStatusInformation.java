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

import java.io.Serializable;

public class OLAStatusInformation implements Serializable {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = -1461855957530677550L;

	/** The version number */
	private Short _version;
	
	/** The daemon group name */
	private String _daemonGroupName;
	
	/** The maximum connections per RGE */
	private Integer _maxConn;
	
	/**
	 * Constructor that takes a OLAHeader as a parameter and sets
	 * all of the attributes of the current OLAStatusInformation object
	 * with the same attributes of the passed in Header object.
	 * @param bboashr The header object that the infrormation will be copied from
	 */
	public OLAStatusInformation(OLAHeader bboashr)
	{
		this._version = bboashr.get_version();
		this._daemonGroupName = bboashr.get_daemonGroupName();
		this._maxConn = bboashr.get_maxConn();
	}

	/**
	 * @return the _version
	 */
	public Short get_version() {
		return _version;
	}

	/**
	 * @return the _daemonGroupName
	 */
	public String get_daemonGroupName() {
		return _daemonGroupName;
	}

	/**
	 * @return the _maxConn
	 */
	public Integer get_maxConn() {
		return _maxConn;
	}
	
	public String toString()
	{
		try {
			StringBuffer sb = new StringBuffer();
			
			sb.append("BBOASHR@");
			sb.append(System.identityHashCode(this));
			
			sb.append(", VERSION = ");
			sb.append(this.get_version());	
			
			sb.append(", DAEMON_GROUP_NAME = ");
			sb.append(this.get_daemonGroupName());	
			
			sb.append(", MAX CONNECTIONS = ");
			sb.append(this.get_maxConn());	
			
			return sb.toString();
		} catch (Exception e) {
		}
		return ("There was an error in toString() for OLAStatusInformation");
		
	}
}
