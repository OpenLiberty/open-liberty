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
package com.ibm.ws.jain.protocol.ip.sip;

/**
 * Component version constants of the SIP Stack, for reporting code versions.
 * Used when the stack is packaged inside an OSGi bundle,
 * in which case the manifest does not specify component versions.
 * 
 * @see com.ibm.ws.jain.protocol.ip.sip.SipStackImpl#SipStackImpl()
 * @author ran
 */
class VersionInfo
{
	/**
	 * The version number to report as would normally appear
	 * in the manifest "Specification-Version" attribute
	 */
	static final String SPECIFICATION_VERSION = "6.1";
	
	/**
	 * The version number to report as would normally appear
	 * in the manifest "Implementation-Version" attribute
	 */
	static final String IMPLEMENTATION_VERSION = "14";
}
