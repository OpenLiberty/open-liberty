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

import java.io.Serializable;

/**
 * This class represents SIP 2.0 version object.
 * @author Moti
 *
 * TODO 
 * for future version please write an appropriate class 
 * and adjust the SipVersion#CreateVersion... 
 */
class SipVersion20Impl implements SipVersion, Serializable
/*
 * Anat: SPR #RDUH6C323P Serializable added for:
 * add SipServletRequest as an attribute to the SipSession
 * or SipApplicationSession - and should be replicated
 */
{

   	public static final String SIP_VERSION = "SIP/2.0";

	public SipVersion20Impl() {}
	
	public String toString()
	{
		return SIP_VERSION;
	}
	
	public String getVersionMinor() { return "0";}
	public String getVersionMajor() { return "2";}
	
	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;
		return (obj instanceof SipVersion20Impl); 
	}
	
}
