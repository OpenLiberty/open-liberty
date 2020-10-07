/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jain.protocol.ip.sip.message;

/** 
 * Version interface. 
 * It is important that the SipVersion will be immutable object.
 * (better performance in clone)
 */
public interface SipVersion 
{
	/**
	 * Get the major version number.
	 */
	public String getVersionMajor();

	/**
	 * Get the minor version number.
	 */
	public String getVersionMinor();
}
