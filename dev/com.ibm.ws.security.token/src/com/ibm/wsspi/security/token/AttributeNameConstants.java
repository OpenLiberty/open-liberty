/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.token;

/**
 * This class is used for constants that support the security attribute
 * propagation feature.
 * 
 * @ibm-spi
 */
public class AttributeNameConstants {

    /**
     * This shared state variable is used by the system login modules to find out
     * if a java.util.Hashtable has been provided to bypass the normal login. When
     * found, a java.util.Hashtable may present just a userid via WSCREDENTIAL_USERID,
     * a userid/password combination via WSCREDENTIAL_USERID and WSCREDENTIAL_PASSWORD,
     * or all of the required attributes needed to generate a WSCredential via the
     * alternate WSCREDENTIAL_* properties.
     **/
    public static final String WSCREDENTIAL_PROPERTIES_KEY = "com.ibm.wsspi.security.cred.propertiesObject"; // the key for looking for the properties object in shared state.

    /**
     * WSCREDENTIAL_USERID can be specified separately to allow WAS to create
     * a WSCredential by accessing the user registry to obtain all of the
     * required attributes.
     **/
    public static final String WSCREDENTIAL_USERID = "com.ibm.wsspi.security.cred.userId"; // the userid to login with

    /**
     * WSCREDENTIAL_PASSWORD can be specified in combination with WSCREDENTIAL_USERID to
     * in the java.util.Hashtable either via a TAI or custom login module. It can
     * be specified separately to allow WAS to create a WSCredential by accessing
     * the user registry to obtain all of the required attributes.
     **/
    public static final String WSCREDENTIAL_PASSWORD = "com.ibm.wsspi.security.cred.password"; // the password to login with

    /**
     * The following WSCREDENTIAL_* attributes can be specified within a java.util.Hashtable
     * either via a TAI.getSubject() or a custom login module via the shared state
     * variable WSCREDENTIAL_PROPERTIES_KEY. When all of the following are specified,
     * a WSCredential is created without accessing a user registry remotely.
     **/

    // AuthenticationToken attributes
    public static final String WSCREDENTIAL_UNIQUEID = "com.ibm.wsspi.security.cred.uniqueId"; // the authorization unique ID

    // AuthorizationToken attributes
    public static final String WSCREDENTIAL_REALM = "com.ibm.wsspi.security.cred.realm"; // the security realm
    public static final String WSCREDENTIAL_SECURITYNAME = "com.ibm.wsspi.security.cred.securityName"; // the securityName of the user
    public static final String WSCREDENTIAL_LONGSECURITYNAME = "com.ibm.wsspi.security.cred.longSecurityName"; // the long securty display name of the user, for LDAP this is the DN, for LocalOS this is the accessID.
    public static final String WSCREDENTIAL_PRIMARYGROUPID = "com.ibm.wsspi.security.cred.primaryGroupId"; // the primary group id
    public static final String WSCREDENTIAL_GROUPS = "com.ibm.wsspi.security.cred.groups"; // the groups with | delimiters
    public static final String WSCREDENTIAL_OID = "com.ibm.wsspi.security.cred.oid"; // the authentication mechanism OID.
    public static final String WSCREDENTIAL_FORWARDABLE = "com.ibm.wsspi.security.cred.forwardable"; // determines if the cred is forwardable
    public static final String WSCREDENTIAL_EXPIRATION = "com.ibm.wsspi.security.cred.expiration"; // the expiration time for the token

    /*
     * Cache lookup key - note: By default, the cache key is the uniqueID of the user, however, this gives you the
     * flexibility to add more dynamic data to the lookup key.
     */
    public static final String WSCREDENTIAL_CACHE_KEY = "com.ibm.wsspi.security.cred.cacheKey"; // the cache lookup key

    // One-way hash of Subject uniqueIDs gathered from implementations of 
    // com.ibm.wsspi.security.token.Token objects.  If all tokens return null,
    // the accessID is used as the uniqueID and is one-way hashed.
    public static final String WSTOKEN_UNIQUEID = "hashed_uid";

    // The expiration time of the token, added to the signed part of the token.
    public static final String WSTOKEN_EXPIRATION = "expire";

    // PropagationToken attributes
    // append each new caller at the end with | delimiter
    public static final String WSPROP_CALLERS = "com.ibm.wsspi.security.propagation.callers";
    // append each cell:directory:server at the end with | delimiter
    public static final String WSPROP_HOSTS = "com.ibm.wsspi.security.propagation.hosts";

    // PropagationToken lookup key 
    public static final String WSPROPTOKEN_KEY_V1 = "com.ibm.ws.security.token.PropagationTokenImpl:1";

    // PropagationToken default name
    public static final String WSPROPTOKEN_NAME = "com.ibm.ws.security.token.PropagationTokenImpl";

    // AuthenticationToken default name
    public static final String WSAUTHTOKEN_NAME = "com.ibm.ws.security.token.AuthenticationTokenImpl";

    // AuthorizationToken default name
    public static final String WSAUTHZTOKEN_NAME = "com.ibm.ws.security.token.AuthorizationTokenImpl";

    // SingleSignonToken default name
    public static final String WSSSOTOKEN_NAME = "LtpaToken";

    // KerberosToken default name
    public static final String WSKERBEROSTOKEN_NAME = "com.ibm.ws.security.token.KerberosTokenImpl"; // LIDB1912-1.3

    // KerberosServiceTicket default name
    public static final String WSKERBEROSTICKET_NAME = "com.ibm.ws.security.token.KerberosServiceTicketImpl"; // LIDB1912-1.3

    //@LIDB3337 - z/OS specific hashtable keys
    public static final String ZOS_USERID = "com.ibm.wsspi.security.token.zos_userid";
    public static final String ZOS_AUDIT_STRING = "com.ibm.wsspi.security.token.zos_audit_string";
    public static final String CALLER_PRINCIPAL_CLASS = "com.ibm.wsspi.security.token.caller_principal_class";

    //@LIDB3337 - z/OS specific constants
    public static final String DEFAULT_CALLER_PRINCIPAL_CLASS = "com.ibm.websphere.security.auth.WSPrincipal";
    public static final String ZOS_CALLER_PRINCIPAL_CLASS = "com.ibm.ws.security.zos.Principal";

    // Kerberos specific constants
    public static final String KERBEROS_PRINCIPAL = "javax.security.auth.kerberos.KerberosPrincipal";
    public static final String KERBEROS_KEY = "javax.security.auth.kerberos.KerberosKey";
    public static final String KERBEROS_TICKET = "javax.security.auth.kerberos.KerberosTicket";

    // 673069 Asynch login constants to control whether we should verifyUser and/or refreshGroups during asynch login
    public static final String REFRESH_GROUPS = "com.ibm.wsspi.security.cred.refreshGroups"; // 673069
    public static final String VERIFY_USER = "com.ibm.wsspi.security.cred.verifyUser"; // 673069
}