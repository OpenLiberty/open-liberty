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
package com.ibm.ws.jain.protocol.ip.sip.extensions;

 /**
 * This was not included in Jain. 
 * 
 * When present in an INVITE request, the Alert-Info header field specifies an 
 * alternative ring tone to the UAS. When present in a 180 (Ringing) response, 
 * the Alert-Info header field specifies an alternative ringback tone to the 
 * UAC. A typical usage is for a proxy to insert this header field to provide 
 * a distinctive ring feature. 
 * 
 * Example: 
 *
 *    Alert-Info: <http://www.example.com/sounds/moo.wav>
 *
 * @author Assaf Azaria
 */
public interface AlertInfoHeader extends InfoHeader
{
	/**
	 * Name of Alert Info Header.
	 */
	public final static String name = "Alert-Info";
}
