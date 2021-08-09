/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat_helper;

/**
 * Contains all the constant variables used among all the test classes.
 */
public class Constants {

    public static final boolean HAS_LDAP_SERVER = true;

    public final static String DEFAULT_REALM = "JaspiRealm";
    //Value of JASPI_GROUP must match group.name property passed in provider bnd.bnd.
    public final static String JASPI_GROUP = "JASPIGroup";

    public final static String QUERY_STRING_BASIC_AUTH_SERVLET = "/JavaEESecBasicAuthServlet/JavaEESecBasic";

    public final static String DEFAULT_BASICAUTH_SERVLET_NAME = "ServletName: JASPIBasicAuthServlet";
    public final static String DEFAULT_FORMLOGIN_SERVLET_NAME = "ServletName: JASPIFormLoginServlet";
    public final static String DEFAULT_REGISTRATION_SERVLET_NAME = "ServletName: JASPIRegistrationTestServlet";

    public final static String DEFAULT_CALLBACK_SERVLET_NAME = "ServletName: JASPICallbackTestBasicAuthServlet";
    public final static String DEFAULT_CALLBACK_FORMLOGIN_SERVLET_NAME = "ServletName: JASPICallbackTestFormLoginServlet";
    public final static String DEFAULT_CALLBACK_FORM_LOGIN_PAGE = "/JASPICallbackTestFormLoginServlet/j_security_check";

    public final static String DEFAULT_SERVLET30_NAME = "ServletName: JASPIServlet30";
    public final static String NO_NAME_VALIDATION = "NONE";
    public final static String DEFAULT_BASIC_REGISTRATION = "/JASPIBasic";
    public final static String DEFAULT_FORM_REGISTRATION = "/JASPIForm";
    public final static String DEFAULT_FORM_LOGIN_PAGE = "/JavaEEsecFormAuth/FormServlet";
    public final static String DEFAULT_CUSTOM_FORM_LOGIN_PAGE = "/JavaEESecCustomFormLoginServlet/JavaEESecAnnotatedFormLoginServlet";
    public final static String DEFAULT_REDIRECT_FORM_LOGIN_PAGE = "/JavaEEsecFormAuthRedirect/FormServlet";

    public final static String AUTH_TYPE_BASIC = "BASIC";
    public final static String AUTH_TYPE_FORM = "FORM";

    public final static String DEFAULT_APP = "JavaEESecBasicAuthServlet";
    public final static String DEFAULT_REGISTRATION_APP = "JASPIRegistrationTestServlet";
    public final static String DEFAULT_FORM_APP = "JASPIFormLoginServlet";
    public final static String DEFAULT_SERVLET30_APP = "JASPIServlet30";
    public final static String DEFAULT_CALLBACK_APP = "JASPICallbackTestBasicAuthServlet";
    public final static String DEFAULT_CALLBACK_FORM_APP = "JASPICallbackTestFormLoginServlet";
    public final static String DEFAULT_WRAPPING_APP = "JASPIWrappingServlet";

    public final static String FORM_LOGIN_PAGE = "Form Login Page";
    public final static String FORM_LOGOUT_PAGE = "Form Logout Page";
    public final static String LOGIN_ERROR_PAGE = "Form Login Error Page";
    public final static String FORM_LOGIN_JASPI_PAGE = "Form Login Page For Include and Forward";

    public final static String ALL_CALLBACKS = "?PVCB=YES&CPCB=YES&GPCB=YES";
    public final static String CPCB_PRINCIPAL_CALLBACK = "?PVCB=NO&CPCB=YES&GPCB=NO&cpcbType=JASPI_PRINCIPAL";
    public final static String MANUAL_CALLBACK = "?PVCB=MANUAL&CPCB=NO&GPCB=NO";
    public final static String PVCB_CALLBACK = "?PVCB=YES&CPCB=NO&GPCB=NO";
    public final static String NO_CALLBACKS = "?PVCB=NO&CPCB=NO&GPCB=NO";
    public final static String CPCB_CALLBACK = "?PVCB=NO&CPCB=YES&GPCB=NO";
    public final static String GPCB_CALLBACK = "?PVCB=NO&CPCB=NO&GPCB=YES";
    public final static String CPCBGPCB_CALLBACK = "?PVCB=NO&CPCB=YES&GPCB=YES";
    public final static String PVCBGPCB_CALLBACK = "?PVCB=YES&CPCB=NO&GPCB=YES";
    public final static String CPCB_CALLBACK_DESCR = "PVCB=NO&CPCB=YES&GPCB=NO";
    public final static String GPCB_CALLBACK_DESCR = "PVCB=NO&CPCB=NO&GPCB=YES";
    public final static String ALL_CALLBACK_DESCR = "PVCB=YES&CPCB=YES&GPCB=YES";
    public final static String CPCBGPCB_CALLBACK_DESCR = "PVCB=NO&CPCB=YES&GPCB=YES";
    public final static String PVCB_CALLBACK_DESCR = "PVCB=YES&CPCB=NO&GPCB=NO";

    public final static String DEFAULT_APP_CONTEXT = "default_host " + DEFAULT_APP;
    public final static String TEST_APP1_CONTEXT = "testApp1Context";
    public final static String TEST_APP2_CONTEXT = "testApp2Context";
    public final static String PROFILE_SERVLET_MSG_LAYER = "HttpServlet";

    public final static String DEFAULT_JASPI_PROVIDER = "bob";

    public final static String DEFAULT_PROVIDER_CLASS = "com.ibm.ws.security.jaspi.test.AuthProvider";
    public final static String PERSISTENT_PROVIDER_CLASS = "com.ibm.ws.security.jaspi.test.AuthProvider_1";
    public final static String REGISTER = "?REGISTER";
    public final static String GET = "?GET";
    public final static String REMOVE = "?REMOVE";
    public final static String REGISTERINVALIDCLASS = "?REGISTERINVALIDCLASS";
    public final static String REMOVEINVALID = "?REMOVEINVALID";

    // Audit record
    public final static String DEFAULT_PROVIDER_AUDIT = "class com.ibm.ws.security.jaspi.test.AuthProvider";
    public static String BASICAUTH_public_URI = "/JASPIBasicAuthServlet" + DEFAULT_BASIC_REGISTRATION;
    public static String BASICAUTH_UNpublic_URI = "/JASPIBasicAuthServlet/JASPIUnpublic";

    // Jaspi test users
    public final static String javaeesec_basicRoleGroup = "group1";
    public final static String javaeesec_basicRoleUser = "jaspiuser1";
    public final static String javaeesec_basicRoleUser_requestscoped = "jaspiuser1_requestscoped";
    public final static String javaeesec_basicRoleUser_sessionscoped = "jaspiuser1_sessionscoped";
    public final static String javaeesec_basicRoleLDAPUser = "jaspildapuser1";
    public final static String javaeesec_basicRolePwd = "s3cur1ty";
    public final static String javaeesec_basicRoleGroupUser = "jaspiuser2";
    public final static String javaeesec_basicRoleGroupPwd = "s3cur1ty";
    public final static String javaeesec_basicRoleUserPrincipal = "jaspiuser6";
    public final static String javaeesec_formRoleGroup = "group3";
    public final static String javaeesec_formRoleUser = "jaspiuser3";
    public final static String javaeesec_formRolePwd = "s3cur1ty";
    public final static String javaeesec_formRoleGroupUser = "jaspiuser5";
    public final static String javaeesec_formRoleGroupPwd = "s3cur1ty";
    public final static String jaspi_noRoleUser = "jaspiuser4";
    public final static String jaspi_noRolePwd = "s3cur1ty";
    public final static String jaspi_servlet30User = "jaspiuser1";
    public final static String jaspi_servlet30UserPwd = "s3cur1ty";
    public final static String jaspi_invalidUser = "invalidUserName";
    public final static String jaspi_invalidPwd = "invalidPassword";
    public final static String jaspi_notInRegistryNotInRoleUser = "jaspiUser100";
    public final static String jaspi_notInRegistryNotInRolePwd = "jaspiUser100Pwd";
    public final static String jaspi_notInRegistryInBasicRoleUser = "jaspiuser101";
    public final static String jaspi_notInRegistryInBasicRolePwd = "jaspiuser101Pwd";
    public final static String jaspi_notInRegistryInFormRoleUser = "jaspiuser102";
    public final static String jaspi_notInRegistryInFormRolePwd = "jaspiuser102Pwd";

    // unauthenticated user
    public final static String unauthenticated_user = "UNAUTHENTICATED";
    // Jaspi roles
    public final static String BasicRole = "javaeesec_basic";
    public final static String FormRole = "javaeesec_form";

    //Values to be verified in ejb servlet response
    public final static String getEJBBeanResponse = "EJB  = ";
    public final static String ejb01Bean = "SecurityEJBA01Bean";
    public final static String ejb02Bean = "SecurityEJBA02Bean";
    public final static String ejb03Bean = "SecurityEJBA03Bean";
    public final static String ejb06Bean = "SecurityEJBA06Bean";
    public final static String ejbRunASBean = "SecurityEJBRunAsBean";
    public final static String getEjbBeanMethodName = "Method = ";
    public final static String ejbBeanMethodManager = "manager";
    public final static String ejbBeanMethodRunAsSpecified = "runAsSpecified";
    public final static String ejbBeanMethodEmployeeAndManager = "employeeAndManager";
    public final static String ejbisCallerManagerTrue = "isCallerInRole(Manager)=true";
    public final static String ejbisCallerManagerFale = "isCallerInRole(Manager)=false";
    public final static String ejbisCallerEmployeeTrue = "isCallerInRole(Employee)=true";
    public final static String ejbisCallerEmployeeFalse = "isCallerInRole(Employee)=false";
    public final static String ejbisUserManagerTrue = "isUserInRole(Manager): true";
    public final static String ejbisUserManagerFalse = "isUserInRole(Manager)= false";
    public final static String ejbisUserEmployeeTrue = "isUserInRole(Employee)= true";
    public final static String ejbisUserEmployeeFalse = "isUserInRole(Employee)= false";
    public final static String getEjbCallerPrincipal = "getCallerPrincipal()=";
    public final static String ejbAccessException = "EJBAccessException: CWWKS9400A: ";
    public final static String ejbAuthorizationFailed = "Error 403: AuthorizationFailed";

    // Values to be verified in servlet response

    public final static String RESPONSE_AUTHENTICATION_FAILED = "JASPIC Authenticated with status: SEND_FAILURE";
    public final static String RESPONSE_AUTHORIZATION_FAILED = "AuthorizationFailed";

    public final static String jaspiValidateRequest = "JASPI validateRequest called with auth provider=";
    public final static String jaspiSecureResponse = "JASPI secureResponse called with auth provider=";
    public final static String userRegistryRealm = "JaspiRealm";
    public final static String isAuthenticatedTrue = "isAuthenticated: true";
    public final static String isAuthenticatedFalse = "isAuthenticated: false";
    public final static String getRemoteUserFound = "getRemoteUser: ";
    public final static String getUserPrincipalFound = "getUserPrincipal().getName(): ";
    public final static String getUserPrincipalFoundJaspiPrincipal = "getUserPrincipal: com.ibm.ws.security.jaspi.test.AuthModule$JASPIPrincipal";
    public final static String getRemoteUserNull = "getRemoteUser: null";
    public final static String getUserPrincipalNull = "getUserPrincipal: null";
    public final static String getAuthTypeBasic = "getAuthType: BASIC";
    public final static String getAuthTypeJaspi = "getAuthType: JASPI_AUTH";
    public final static String getAuthTypeForm = "getAuthType: FORM";
    public final static String getAuthTypeNull = "getAuthType: null";
    public final static String getRunAsSubjectNull = "RunAsSubject: null";
    public final static String isManadatoryFalse = "isManadatory=false";
    public final static String isManadatoryTrue = "isManadatory=true";
    public final static String requestIsWrapped = "The httpServletRequest has been wrapped by httpServletRequestWrapper.";
    public final static String responseIsWrapped = "The httpServletRestponse has been wrapped by httpServletResponseWrapper.";
    public final static String secContextGetPrincipal = "securityContext.getCallerPrincipal():";
    public final static String secContextGetPrincipalNull = "securityContext.getCallerPrincipal(): null";
    public final static String secContextGetPrincipalName = "securityContext.getCallerPrincipal().getName():";
    public final static String secContextIsCallerInRole = "securityContext.isCallerInRole";

    public final static String messageLayerRuntime = "null";
    public final static String messageLayerDefault = "HttpServlet";
    public final static String appContext = "default_host /";
    public final static String appContextRuntime = "null";
    public final static String isPersistentTrue = "true";
    public final static String isPersistentFalse = "false";
    public final static String providerClass = "Provider class: ";
    public final static String providerClassDefault = "Provider class: com.ibm.ws.security.jaspi.test.AuthProvider";

    public final static String EXCEPTION_JAVA_LANG_SECURITY = "java.lang.SecurityException";
    public final static String SERVLET_EXCEPTION = "javax.servlet.ServletException";
    public final static String SERVLET_EXCEPTION_JASPI_LOGIN = "javax.servlet.ServletException: The login method may not be invoked while JASPI authentication is active.";

    // Jaspi server.xml files
    public final static String SERVLET_SECURITY_NOJASPI_SERVER_XML = "dynamicSecurityFeature/servlet31_appSecurity20_noJaspi.xml";
    public final static String SERVLET_SECURITY_JASPI_SERVER_XML = "dynamicSecurityFeature/servlet31_appSecurity20_withJaspi.xml";

    public final static String DB_USER1 = "blue1";
    public final static String DB_USER1_PWD = "thisismypwd";
    // hashed with Pbkdf2PasswordHashImpl
    public final static String DB_USER1_PWD_HASH = "PBKDF2WithHmacSHA256:2048:vHups5wO1Zws+IDirtdHjd0S6UIOnTiHrRUMKlheYzQ=:6PtLstQacpH68NbBn1F0UlzeA92LYp44Z3pCQaSBv2Q=";
    public final static String DB_USER2 = "blue2";
    public final static String DB_USER2_PWD = "thisismypwd2";
    // hashed with Pbkdf2PasswordHashImpl
    public final static String DB_USER2_PWD_HASH = "PBKDF2WithHmacSHA256:2048:1aPPQurxfie2FLiaC2HqjvUYe1IX57jJrB5bbW9sJgs=:OjT8ialvm7BB3pdfCYzEO83LypU+O/D7AQegy6JqT0Q=";
    public final static String DB_USER3 = "blue3";
    public final static String DB_USER3_PWD = "thisismypwd3";
    // hashed with Pbkdf2PasswordHashImpl
    public final static String DB_USER3_PWD_HASH = "PBKDF2WithHmacSHA256:2048:fM4/a3w9V/YEkClSXhY3LScnAZzT8MbOw/eaj7noVK8=:tKUr6l9oez55Zh5AM6PBKcGHdv2IqHuIJe0HZZ/e6Qg=";
    public final static String DB_USER_DUPE = "dupUser";
    public final static String DB_GROUP2 = "group2";
    public final static String DB_GROUP3 = "group3";
    public final static String DB_USER_NOPWD = "userNoPwd";
    public final static String DB_CUSTOM_PWD1 = "blue1";
    public final static String DB_CUSTOM_HASH = "_CUSTOM";
    public final static String DB_CUSTOM_PWD1_HASH = "blue1" + DB_CUSTOM_HASH;
}
