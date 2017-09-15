/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.admin;

/**
 * Interface representing the default sibus object
 *
 */
public interface SIBus extends LWMConfig {
	/**
	 * Get the name of the bus.The default is defaultBus
	 * @return String
	 */
	public String getName();

	/**
	 * Se the name of the bus
	 * @param value
	 */
	public void setName(String value);

	/**
	 * Get the UUID of the default bus
	 * @return
	 */
	public String getUuid();

	/**
	 * Set the UUID of the default bus
	 * @param value
	 */
	public void setUuid(String value);

	/**
	 * Get the description of the default bus
	 * @return String
	 */
	public String getDescription();

	/**
	 * Set the description of the bus
	 * @param value
	 */
	public void setDescription(String value);

	

}
