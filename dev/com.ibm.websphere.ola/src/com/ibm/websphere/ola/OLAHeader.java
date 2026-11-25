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

package com.ibm.websphere.ola;


public interface OLAHeader {


	/**
	 * The absolute location of the BBOASHR
	 */
	public abstract Long get_ashrptr();

	/**
	 * Sets the location of the BBOASHR
	 */
	public abstract void set_ashrptr(Long _ashrptr);

	public abstract void set_eyeCatcher(String eyeCatcher) throws OLAException;

	public abstract String get_eyeCatcher();

	/**
	 * @return the _version
	 */
	public abstract Short get_version();

	/**
	 * @param _version the _version to set
	 */
	public abstract void set_version(Short _version);

	/**
	 * @return the _size
	 */
	public abstract Integer get_size();

	/**
	 * @param _size the _size to set
	 */
	public abstract void set_size(Integer _size);

	/**
	 * @return the _daemonGroupName
	 */
	public abstract String get_daemonGroupName();

	/**
	 * @param groupName the _daemonGroupName to set
	 */
	public abstract void set_daemonGroupName(String groupName);

	/**
	 * @return the _maxConn
	 */
	public abstract Integer get_maxConn();

	/**
	 * @param conn the _maxConn to set
	 */
	public abstract void set_maxConn(Integer conn);

	/**
	 * toString method for OLA BBOASHR. 
	 */
	public abstract String toString();

}