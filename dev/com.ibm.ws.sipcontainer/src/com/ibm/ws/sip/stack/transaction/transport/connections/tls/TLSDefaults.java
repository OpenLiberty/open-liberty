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
package com.ibm.ws.sip.stack.transaction.transport.connections.tls;

/**
 * @author sipuser
 *
 */
public interface TLSDefaults
{
	public static final int DEFAULT_SESSION_TIMEOUT = 30; // seconds  
    
	public static final String DEFAULT_PROTOCOL = "TLS";
	public static final String DEFAULT_KEY_MANAGER_NAME = "IbmX509";
	public static final String DEFAULT_TRUST_MANAGER_NAME = "IbmX509";
	public static final String DEFAULT_KEYSTORE_TYPE = "JKS";
	public static final String DEFAULT_KEYSTORE_PROVIDER = "IBMJCE";
	public static final String DEFAULT_TRUSTSTORE_TYPE = "JKS";
	public static final String DEFAULT_TRUSTSTORE_PROVIDER = "IBMJCE";
	public static final String DEFAULT_CONTEXT_PROVIDER = "IBMJSSE";
	public static final String DEFAULT_CLIENT_AUTHENTICATION = "false";
	public static String DEFAULT_JSSE_PROVIDER_CLASS_NAME= "com.ibm.jsse.IBMJSSEProvider";
}
