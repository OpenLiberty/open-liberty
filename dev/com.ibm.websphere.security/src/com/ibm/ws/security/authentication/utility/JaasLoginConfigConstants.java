/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.utility;

/**
 * Constants for the JAAS Login Configuration
 */
public class JaasLoginConfigConstants {
    public static final String APPLICATION_WSLOGIN = "WSLogin";
    public static final String CLIENT_CONTAINER = "ClientContainer";
    public static final String SYSTEM_DEFAULT = "system.DEFAULT";
    public static final String SYSTEM_UNAUTHENTICATED = "system.UNAUTHENTICATED";
    public static final String SYSTEM_WEB_INBOUND = "system.WEB_INBOUND";
    public static final String SYSTEM_DESERIALIZE_CONTEXT = "system.DESERIALIZE_CONTEXT";
    public static final String SYSTEM_RMI_INBOUND = "system.RMI_INBOUND";
    public static final String JAASClient = "JAASClient";

    public static final String COM_IBM_SECURITY_AUTH_MODULE_KRB5LOGINMODULE_WRAPPER = "com.ibm.ws.security.authentication.jaas.modules.Krb5LoginModuleWrapper";
    public static final String COM_IBM_SECURITY_AUTH_MODULE_KRB5LOGINMODULE = "com.ibm.security.auth.module.Krb5LoginModule";
    public static final String COM_IBM_SECURITY_JGSS_KRB5_INITIATE = "com.ibm.security.jgss.krb5.initiate";
    public static final String COM_IBM_SECURITY_JGSS_KRB5_ACCEPT = "com.ibm.security.jgss.krb5.accept";

    public static final String COM_SUN_SECURITY_AUTH_MODULE_KRB5LOGINMODULE = "com.sun.security.auth.module.Krb5LoginModule";
    public static final String COM_SUN_SECURITY_JGSS_KRB5_INITIATE = "com.sun.security.jgss.krb5.initiate";
    public static final String COM_SUN_SECURITY_JGSS_KRB5_ACCEPT = "com.sun.security.jgss.krb5.accept";
}
