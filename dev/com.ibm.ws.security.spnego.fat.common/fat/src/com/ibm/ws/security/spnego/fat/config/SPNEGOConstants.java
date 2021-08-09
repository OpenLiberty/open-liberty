/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.fat.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SPNEGOConstants {

    public final static String KERBEROS = "kerberos";

    public final static String KDC_HOST_FROM_CONSUL = "kdc-primary";
    public final static String KDC2_HOST_FROM_CONSUL = "kdc-secondary";
    public final static String KDC_SHORTNAME_FROM_CONSUL = "shortname";
    public final static String KDC_REALM_FROM_CONSUL = "realm";
    public final static String KDC2_REALM_FROM_CONSUL = "realm";
    public final static String KRB5_CONF_FROM_CONSUL = "krb5Conf";

    public final static String IBM_DOMAIN = "ibm.com";
    public final static String MS_KDC_USER_CONSUL = "user";
    public final static String MS_KDC_USER_PASSWORD_CONSUL = "password";
    public final static String MS_KDC_USER_REALM_2_CONSUL = "user";
    public final static String MS_KDC_PASSWORD_REALM_2_CONSUL = "password";
    public final static String MS_KDC_USER_BACKUP_CONSUL = "user";
    public final static String MS_KDC_PASSWORD_BACKUP_CONSUL = "password";
    public final static String S4U_BACKEND_SERVICE = "s4u_backend_service";

    public final static String Z_USER_FROM_CONSUL = "userz";
    public final static String FIRST_USER_FROM_CONSUL = "firstuser";
    public final static String SECOND_USER_FROM_CONSUL = "seconduser";
    public final static String USER_PWD_FROM_CONSUL = "userpwd";
    public final static String USER0_PWD_FROM_CONSUL = "userzpwd";

    // File names and paths
    public final static String KRB5_KEYTAB_TEMP_SUFFIX = "_HTTP_krb5.keytab";
    public final static String WINNT_DEFAULT_LOCATION = "/winnt/";
    public final static String KRB5_DEFAULT_LOCATION = "/Windows/";
    public final static String KRB5_CONF_FILE = "krb5.conf";
    public final static String KRB5_INI_FILE = "krb5.ini";
    public final static String KRB5_KEYTAB_FILE = "krb5.keytab";
    public final static String SLASH_KERBEROS_SLASH = "/kerberos/";
    public final static String KRB5_LOCAL_KEYTAB_LOCAL_FILE = SLASH_KERBEROS_SLASH + "localhost.keytab";
    public final static String KRB5_LOCAL_KEYTAB_REMOTE_FILE = "localhost_HTTP_krb5.keytab";
    public final static String CREATE_WIN_USER_REMOTE_FILE = "createWinUserOL.vbs"; // "createWinUser.vbs";
    public final static String CREATE_WIN_USER_LOCAL_FILE = SLASH_KERBEROS_SLASH + CREATE_WIN_USER_REMOTE_FILE;
    public final static String REMOVE_WIN_USER_REMOTE_FILE = "removeWinUsersOL.vbs"; // "removeWinUsers.vbs";
    public final static String REMOTE_WIN_USER_LOCAL_FILE = SLASH_KERBEROS_SLASH + REMOVE_WIN_USER_REMOTE_FILE;
    public final static String CREATE_WIN_KEYTAB_REMOTE_FILE = "createWinKeytabFileOL.bat"; // "createWinKeytabFile.bat";
    public final static String CREATE_WIN_KEYTAB_LOCAL_FILE = SLASH_KERBEROS_SLASH + CREATE_WIN_KEYTAB_REMOTE_FILE;
    public final static String CREATE_WIN_USER_SET_SPN_REMOTE_FILE = "createUserAndSetSpnOL.bat"; // "createUserAndSetSpn.bat";
    public final static String CREATE_WIN_USER_SET_SPN_LOCAL_FILE = SLASH_KERBEROS_SLASH + CREATE_WIN_USER_SET_SPN_REMOTE_FILE;
    public final static String SET_USER_SPN_REMOTE_FILE = "setUserSpnOL.bat"; // "setUserSpn.bat";
    public final static String SET_USER_SPN_LOCAL_FILE = SLASH_KERBEROS_SLASH + SET_USER_SPN_REMOTE_FILE;
    public final static String DELETE_USER_SPN_REMOTE_FILE = "deleteUserSpnOL.bat"; // "deleteUserSpn.bat";
    public final static String DELETE_USER_SPN_LOCAL_FILE = SLASH_KERBEROS_SLASH + DELETE_USER_SPN_REMOTE_FILE;
    public final static String ADD_SPN_TO_KEYTAB_REMOTE_FILE = "addSpnToKeytabOL.bat"; // "addSpnToKeytab.bat";
    public final static String ADD_SPN_TO_KEYTAB_LOCAL_FILE = SLASH_KERBEROS_SLASH + ADD_SPN_TO_KEYTAB_REMOTE_FILE;
    public final static String LOCALHOST_KEYTAB_FILE = SLASH_KERBEROS_SLASH + "localhost.keytab";
    public final static String CYGWIN_HOME_REALM_1 = "c:\\cygwin\\home\\Administrator\\";
    public final static String KRB_RESOURCE_LOCATION = "/resources/security/kerberos/";
    public final static String SERVER_KRB5_CONFIG_FILE = KRB_RESOURCE_LOCATION + "krb5.conf";
    public final static String CLIENT_JAAS_CONFIG_FILE = KRB_RESOURCE_LOCATION + "jaas.conf";
    public final static String SERVER_KRB5_CONFIG_FILE_BACKUP = KRB_RESOURCE_LOCATION + "KDCbackup-krb5.conf";

    // User access control command line parameters
    public final static String ARG_ENABLE_TRUSTED_FOR_DELEGATION = "-enableTrustedForDelegation";
    public final static String ARG_ENABLE_TRUSTED_FOR_AUTH_DELEGATION = "-enableTrustedForAuthDelegation";
    public final static String ARG_ENABLE_ALLOWED_TO_DELEGATE = "-enableAllowedToDelegate";
    public final static String ARG_ENABLE_PASSWORD_NEVER_EXPIRES = "-enablePasswordNeverExpires";
    public final static String ARG_DISABLE_KERBEROS_PRE_AUTH = "-disableKerberosAuthentication";

    public final static Map<String, String> DEFAULT_CMD_ARGS = new HashMap<String, String>();
    public final static Map<String, String> S4U2_PROXY_DEFAULT_CMD_ARGS = new HashMap<String, String>();
    public final static Map<String, String> S4U2_SELF_DEFAULT_CMD_ARGS = new HashMap<String, String>();
    public final static Map<String, String> S4U2_SELF_ALLOW_DELEGATION_FALSE_CMD_ARGS = new HashMap<String, String>();
    public final static Map<String, String> S4U2_SELF_ALLOW_ACCOUNT_TRUSTED_FALSE_CMD_ARGS = new HashMap<String, String>();
    static {
        // Enable all properties
        DEFAULT_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_TRUSTED_FOR_DELEGATION, "false");
        DEFAULT_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_TRUSTED_FOR_AUTH_DELEGATION, "true");
        DEFAULT_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_ALLOWED_TO_DELEGATE, "true");
        DEFAULT_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_PASSWORD_NEVER_EXPIRES, "true");
        DEFAULT_CMD_ARGS.put(SPNEGOConstants.ARG_DISABLE_KERBEROS_PRE_AUTH, "false");

        // Do not disable Kerberos pre-auth
        S4U2_PROXY_DEFAULT_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_TRUSTED_FOR_DELEGATION, "true");
        S4U2_PROXY_DEFAULT_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_TRUSTED_FOR_AUTH_DELEGATION, "true");
        S4U2_PROXY_DEFAULT_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_ALLOWED_TO_DELEGATE, "true");
        S4U2_PROXY_DEFAULT_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_PASSWORD_NEVER_EXPIRES, "true");
        S4U2_PROXY_DEFAULT_CMD_ARGS.put(SPNEGOConstants.ARG_DISABLE_KERBEROS_PRE_AUTH, "false");

        // Do not enable trusted for delegation
        S4U2_SELF_DEFAULT_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_TRUSTED_FOR_DELEGATION, "false");
        S4U2_SELF_DEFAULT_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_TRUSTED_FOR_AUTH_DELEGATION, "true");
        S4U2_SELF_DEFAULT_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_ALLOWED_TO_DELEGATE, "true");
        S4U2_SELF_DEFAULT_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_PASSWORD_NEVER_EXPIRES, "true");
        S4U2_SELF_DEFAULT_CMD_ARGS.put(SPNEGOConstants.ARG_DISABLE_KERBEROS_PRE_AUTH, "true");

        // Disable Delegation for Self
        S4U2_SELF_ALLOW_DELEGATION_FALSE_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_TRUSTED_FOR_DELEGATION, "false");
        S4U2_SELF_ALLOW_DELEGATION_FALSE_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_TRUSTED_FOR_AUTH_DELEGATION, "true");
        S4U2_SELF_ALLOW_DELEGATION_FALSE_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_ALLOWED_TO_DELEGATE, "false");
        S4U2_SELF_ALLOW_DELEGATION_FALSE_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_PASSWORD_NEVER_EXPIRES, "true");
        S4U2_SELF_ALLOW_DELEGATION_FALSE_CMD_ARGS.put(SPNEGOConstants.ARG_DISABLE_KERBEROS_PRE_AUTH, "true");

        // Disable Account Trusted
        S4U2_SELF_ALLOW_ACCOUNT_TRUSTED_FALSE_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_TRUSTED_FOR_DELEGATION, "false");
        S4U2_SELF_ALLOW_ACCOUNT_TRUSTED_FALSE_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_TRUSTED_FOR_AUTH_DELEGATION, "true");
        S4U2_SELF_ALLOW_ACCOUNT_TRUSTED_FALSE_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_ALLOWED_TO_DELEGATE, "false");
        S4U2_SELF_ALLOW_ACCOUNT_TRUSTED_FALSE_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_PASSWORD_NEVER_EXPIRES, "true");
        S4U2_SELF_ALLOW_ACCOUNT_TRUSTED_FALSE_CMD_ARGS.put(SPNEGOConstants.ARG_DISABLE_KERBEROS_PRE_AUTH, "true");

    }

    // System and test properties
    public final static String PROP_KRB5_PRINCIPAL = "security.spnego.krb5.principal";
    public final static String PROP_KRB5_KEYTAB = "security.spnego.krb5.keytab";
    public final static String PROP_KRB5_CONF = "security.spnego.krb5.conf";
    public final static String PROP_JAVA_HOME = "security.spnego.java.home";
    public final static String PROP_TEST_SYSTEM_HOST_NAME = "security.spnego.test.system.host.name";
    public final static String PROP_TEST_SYSTEM_SHORTHOST_NAME = "security.spnego.test.system.shorthost.name";

    // Servlet and request constants
    public final static String HEADER_AUTHORIZATION = "Authorization";
    public final static String HEADER_USER_AGENT = "User-Agent";
    public final static String HEADER_HOST = "Host";
    public final static String HEADER_REMOTE_ADDR = "";
    public final static String FIREFOX = "Firefox";
    public final static String IE = "IE";
    public final static String SIMPLE_SERVLET_NAME = "SimpleServlet";
    public final static String SIMPLE_SERVLET = "/" + SIMPLE_SERVLET_NAME;
    public final static String SPNEGO_NOT_SUPPORTED_DEFAULT_ERROR_PAGE = "SPNEGO authentication is not supported.";
    public final static String NTLM_TOKEN_RECEIVED_DEFAULT_ERROR_PAGE = "An NTLM Token was received.";
    public final static String GSS_CREDENTIAL_STRING = "GSSCredential";
    public final static String JDK11_GSS_CREDENTIAL_STRING = "sun.security.jgss.krb5.Krb5InitCredential";
    public final static String JDK11_SPNEGO_CREDENTIAL_ELEMENT_STRING = "sun.security.jgss.spnego.SpNegoCredElement";
    public final static String OWNER_STRING = "Owner:\t\t";
    public final static String JDK11_Principal_STRING = "Principal: ";
    public final static String JDK11_GSSCREDENTIAL_USER_STRING = "GSSCredential: \n";

    //Additional Supported Header Values for AuthFilterElements
    public final static String HEADER_EMAIL = "email";
    public final static String HEADER_NAME_NO_VALUE = "nameNoValue";
    public final static String HEADER_NAME_NO_VALUE_INVALID = "nameNoValueInvalid";
    public final static String COOKIE = "Cookie";

    // Constants to ease readability
    public final static List<String> NO_APPS = null;
    public final static List<String> NO_MSGS = null;
    public final static Map<String, String> NO_PROPS = null;
    public final static boolean CREATE_SSL_CLIENT = true;
    public final static boolean DONT_CREATE_SSL_CLIENT = false;
    public final static boolean CREATE_SPN_AND_KEYTAB = true;
    public final static boolean DONT_CREATE_SPN_AND_KEYTAB = false;
    public final static boolean CREATE_SPNEGO_TOKEN = true;
    public final static boolean DONT_CREATE_SPNEGO_TOKEN = false;
    public final static boolean SET_AS_COMMON_TOKEN = true;
    public final static boolean DONT_SET_AS_COMMON_TOKEN = false;
    public final static boolean USE_CANONICAL_NAME = true;
    public final static boolean DONT_USE_CANONICAL_NAME = false;
    public final static boolean USE_COMMON_KEYTAB = true;
    public final static boolean DONT_USE_COMMON_KEYTAB = false;
    public final static boolean START_SERVER = true;
    public final static boolean DONT_START_SERVER = false;
    public final static boolean RESTART_SERVER = true;
    public final static boolean DONT_RESTART_SERVER = false;
    public final static String DEFAULT_REALM = null;
    public final static boolean MESSAGE_NOT_EXPECTED = false;
    public final static boolean EXPECTED_MESSAGE = true;
    public final static boolean IGNORE_ERROR_CONTENT = true;
    public final static boolean DONT_IGNORE_ERROR_CONTENT = false;
    public final static boolean HANDLE_SSO_COOKIE = true;
    public final static boolean DONT_HANDLE_SSO_COOKIE = false;
    public final static boolean IS_EMPLOYEE = true;
    public final static boolean IS_NOT_EMPLOYEE = false;
    public final static boolean IS_MANAGER = true;
    public final static boolean IS_NOT_MANAGER = false;
    public final static boolean NO_GSS_CREDENTIALS_PRESENT = false;
    public final static boolean JDK_SPECIFIC_RECONFIG = true;
    public final static boolean USE_USER1 = true;

    // Other constants
    protected static final int DEFAULT_LOG_SEARCH_TIMEOUT = 120 * 1000;
    public static final int MESSAGE_NOT_EXPECTED_LOG_SEARCH_TIMEOUT = 15 * 1000;

    public final static String LOCALHOST_IP_ADDR_NOT_DEFAULT_VALUE = "localhost ip address is not a default vaule. We will not run the test";

}