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
 * Interface representing the Localization point of the destination
 *
 */
public interface SIBLocalizationPoint extends LWMConfig {

	/**
	 * @return the uuid
	 */
	public String getUuid();

	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(String uuid);

	/**
	 * @return the identifier
	 */
	public String getIdentifier();

	/**
	 * @param identifier the identifier to set
	 */
	public void setIdentifier(String identifier);

	
	/**
	 * @return the sendAllowed
	 */
	public boolean isSendAllowed();

	/**
	 * @param sendAllowed
	 *            the sendAllowed to set
	 */
	public void setSendAllowed(boolean sendAllowed);

	/**
	 * @return the highMsgThreshold
	 */
	public long getHighMsgThreshold();

	/**
	 * @param highMsgThreshold the highMsgThreshold to set
	 */
	public void setHighMsgThreshold(long highMsgThreshold);

	/**
	 * @return the targetUuid
	 */
	public String getTargetUuid();

	/**
	 * @param targetUuid the targetUuid to set
	 */
	public void setTargetUuid(String targetUuid);

	


}
