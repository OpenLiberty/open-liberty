/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.kerberos.auth;

/**
 * Constants for KerberosService
 */
public interface Krb5Constants {

    public final static String KEYTAB = "keytab"; // metatype key

    public final static String CONFIG_FILE = "configFile"; // metatype key

    public final static String PID_ID = "(service.pid=com.ibm.ws.security.kerberos.auth.KerberosService)"; // from the metatype.xml
}