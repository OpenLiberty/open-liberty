/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.security.jacc_fat;

import java.util.List;

/**
 *
 */
public class Constants {

    //Server Reconfigure Constants
    public final static boolean RESTART_SERVER = true;
    public final static boolean DO_NOT_RESTART_SERVER = false;

    //RoleMappings Config
    public final static String NO_ROLES_ROLE_MAPPING_PROPS = "no_roles_roleMapping.prop";
    public final static String ALL_ROLE_MAPPING_PROPS = "roleMapping.orig.props";
    public final static String APP_ROLE_ONLY_ROLE_MAPPING_PROPS = "app_role_only_roleMapping.prop";
    public final static String NO_APP_ROLE_ROLE_MAPPING_PROPS = "no_app_role_roleMapping.prop";

    //Server Config
    public final static String SERVER_EJB = "com.ibm.ws.ejbcontainer.security.jacc_fat";
    public final static String SERVER_EJBJAR = "com.ibm.ws.ejbcontainer.security.jacc_fat.ejbjar";
    public final static String SERVER_EJBJAR_MC = "com.ibm.ws.ejbcontainer.security.jacc_fat.ejbjar.mc";
    public final static String SERVER_EJBJAR_INWAR = "com.ibm.ws.ejbcontainer.security.jacc_fat.ejbjar.inwar";
    public final static String SERVER_EJB_BINDINGS = "com.ibm.ws.ejbcontainer.security.jacc_fat.bindings";
    public final static String SERVER_EJB_MERGE_BINDINGS = "com.ibm.ws.ejbcontainer.security.jacc_fat.mergebindings";
    public final static String SERVER_EJBJAR_MERGE_BINDINGS = "com.ibm.ws.ejbcontainer.security.jacc_fat.ejbjar.mergebindings";
    public final static String SERVER_JACC_DYNAMIC = "com.ibm.ws.security.authorization.jacc.dynamic_fat";
    public final static String SERVER_EJB_AUDIT = "com.ibm.ws.ejbcontainer.security.jacc_fat.audit";
    public final static String SERVER_EJBJAR_AUDIT = "com.ibm.ws.ejbcontainer.security.jacc_fat.ejbjar.audit";

    //Application Names
    public final static String APPLICATION_SECURITY_EJB = "securityejb";
    public final static String APPLICATION_SECURITY_EJB_JAR = "securityejbjar";
    public final static String APPLICATION_SECURITY_EJB_STANDALONE_WAR = "securityejbstandalonewar";
    public final static String APPLICATION_SECURITY_EJB_IN_WAR = "securityejbinwar";
    public final static String APPLICATION_SECURITY_EJB_JAR_MC = "securityejbjarMC";
    public final static String APPLICATION_SECURITY_EJB_XML = "securityejbXML";
    public final static String APPLICATION_SECURITY_EJB_XML_MERGE = "securityejbXMLmerge";
    public final static String APPLICATION_SECURITY_EJB_JAR_XMLMERGE = "securityejbjarXMLmerge";
    public final static String APPLICATION_SECURITY_EJB_INWAR_EAR_XML = "securityejbInWarEarXML";
    public final static String APPLICATION_SECURITY_EJB_INWAR_EAR_X01 = "securityejbjarInWarEarX01";
    public final static String APPLICATION_SECURITY_EJB_JAR_INSTANDALONE_M08 = "ejbjarinstandaloneM08";
    public final static String APPLICATION_SECURITY_EJB_JAR_INSTANDALONE_MC06 = "ejbjarinstandaloneMC06";
    public final static String APPLICATION_SECURITY_EJB_JAR_INSTANDALONE_X02 = "ejbjarinstandaloneX02";
    public final static String APPLICATION_SECURITY_EJB_JAR_INSTANDALONE_M02 = "ejbjarinstandaloneM02";
    public final static String APPLICATION_SECURITY_EJB_INWAR_EAR_XMLMERGE = "securityejbInWarEarXMLMerge";
    public final static String APPLICATION_SECURITY_EJB_JAR_INWAR_EAR_M01 = "securityejbjarInWarEarM01";
    public final static String APPLICATION_SECURITY_EJB_JAR_INWAR_EAR_MC06 = "securityejbjarInWarEarMC06";
    public final static String APPLICATION_SECURITY_EJB_JAR_INWAR_EAR_M07 = "securityejbjarInWarEarM07";
    public final static String APPLICATION_SECURITY_EJB_JAR_INWAR_EAR_M07_XMLMERGE = "securityejbjarInWarEarM07XMLmerge";

    //Application Context Root
    public final static String CONTEXT_ROOT_SECURITY_EJB = "/" + APPLICATION_SECURITY_EJB;
    public final static String CONTEXT_ROOT_SECURITY_EJB_JAR = "/" + APPLICATION_SECURITY_EJB_JAR;
    public final static String CONTEXT_ROOT_SECURITY_EJB_STANDALONE_WAR = "/" + APPLICATION_SECURITY_EJB_STANDALONE_WAR;
    public final static String CONTEXT_ROOT_SECURITY_EJB_IN_WAR = "/" + APPLICATION_SECURITY_EJB_IN_WAR;
    public final static String CONTEXT_ROOT_SECURITY_EJB_JAR_MC = "/" + APPLICATION_SECURITY_EJB_JAR_MC;
    public final static String CONTEXT_ROOT_SECURITY_EJB_XML = "/" + APPLICATION_SECURITY_EJB_XML;
    public final static String CONTEXT_ROOT_SECURITY_EJB_XML_MERGE = "/" + APPLICATION_SECURITY_EJB_XML_MERGE;
    public final static String CONTEXT_ROOT_SECURITY_EJB_JAR_XMLMERGE = "/" + APPLICATION_SECURITY_EJB_JAR_XMLMERGE;
    public final static String CONTEXT_ROOT_SECURITY_EJB_INWAR_EAR_XML = "/" + APPLICATION_SECURITY_EJB_INWAR_EAR_XML;
    public final static String CONTEXT_ROOT_SECURITY_EJB_INWAR_EAR_X01 = "/" + APPLICATION_SECURITY_EJB_INWAR_EAR_X01;
    public final static String CONTEXT_ROOT_SECURITY_EJB_JAR_INSTANDALONE_M08 = "/" + APPLICATION_SECURITY_EJB_JAR_INSTANDALONE_M08;
    public final static String CONTEXT_ROOT_SECURITY_EJB_JAR_INSTANDALONE_MC06 = "/" + APPLICATION_SECURITY_EJB_JAR_INSTANDALONE_MC06;
    public final static String CONTEXT_ROOT_SECURITY_EJB_JAR_INSTANDALONE_X02 = "/" + APPLICATION_SECURITY_EJB_JAR_INSTANDALONE_X02;
    public final static String CONTEXT_ROOT_SECURITY_EJB_JAR_INSTANDALONE_M02 = "/" + APPLICATION_SECURITY_EJB_JAR_INSTANDALONE_M02;
    public final static String CONTEXT_ROOT_SECURITY_EJB_INWAR_EAR_XMLMERGE = "/" + APPLICATION_SECURITY_EJB_INWAR_EAR_XMLMERGE;
    public final static String CONTEXT_ROOT_SECURITY_EJB_JAR_INWAR_EAR_M01 = "/" + APPLICATION_SECURITY_EJB_JAR_INWAR_EAR_M01;
    public final static String CONTEXT_ROOT_SECURITY_EJB_JAR_INWAR_EAR_MC06 = "/" + APPLICATION_SECURITY_EJB_JAR_INWAR_EAR_MC06;
    public final static String CONTEXT_ROOT_SECURITY_EJB_JAR_INWAR_EAR_M07 = "/" + APPLICATION_SECURITY_EJB_JAR_INWAR_EAR_M07;
    public final static String CONTEXT_ROOT_SECURITY_EJB_JAR_INWAR_EAR_M07_XMLMERGE = "/" + APPLICATION_SECURITY_EJB_JAR_INWAR_EAR_M07_XMLMERGE;

    //Servlets
    public final static String SERVLET_SECURITY_EJB = "SecurityEJBServlet";
    public final static String SERVLET_SECURITY_EJBXML = "SecurityEJBXMLServlet";
    public final static String SERVLET_SECURITY_EJB_RUNAS = "SecurityEJBRunAsServlet";
    public final static String SERVLET_SECURITY_EJBMC = "SecurityEJBMCServlet";

    //users and groups
    public final static String EMPLOYEE_USER = "user1";
    public final static String EMPLOYEE_PWD = "user1pwd";
    public final static String EMPLOYEE_GROUP = "group1";
    public final static String EMPLOYEE_CONFLICT_USER = "user4";
    public final static String EMPLOYEE_CONFLICT_PWD = "user4 pwd";
    public final static String MANAGER_USER = "user2";
    public final static String MANAGER_PWD = "user2pwd";
    public final static String RUN_AS_USER = "user99";
    public final static String RUN_AS_USER_PWD = "user99pwd";
    public final static String RUN_AS_USER2 = "user98";
    public final static String RUN_AS_USER2_PWD = "user98pwd";
    public final static String NO_ROLE_USER = "user3";
    public final static String NO_ROLE_USER_PWD = "user3pwd";
    public final static String DECLARED_ROLE_USER = "user5";
    public final static String DECLARED_ROLE_USER_PWD = "user5pwd";
    public final static String NEW_JASS_LOGIN_USER = "user3";
    public final static String NEW_JASS_LOGIN_GROUP = "group3";

    // Values to be verified in servlet response
    public final static String USER_REGISTRY_REALM = "BasicRealm";
    public final static String EMPLOYEE_USER_PRINCIPAL = "getCallerPrincipal()=" + EMPLOYEE_USER;
    public final static String EMPLOYEE_CONFLICT_USER_PRINCIPAL = "getCallerPrincipal()=" + EMPLOYEE_CONFLICT_USER;
    public final static String MANAGER_USER_PRINCIPAL = "getCallerPrincipal()=" + MANAGER_USER;
    public final static String NO_ROLE_USER_PRINCIPAL = "getCallerPrincipal()=" + NO_ROLE_USER;
    public final static String DECLARED_ROLE_USER_PRINCIPAL = "getCallerPrincipal()=" + DECLARED_ROLE_USER;
    public final static String RUN_AS_USER_PRINCIPAL = "getCallerPrincipal()=" + RUN_AS_USER;
    public final static String RUN_AS_USER_PRINCIPAL_CONFLICT = "getCallerPrincipal()=" + RUN_AS_USER2;
    public final static String EMPLOYEE_USER_IDENTITY = "getCallerIdentity()=" + EMPLOYEE_USER;
    public final static String EMPLOYEE_CONFLICT_USER_IDENTITY = "getCallerIdentity()=" + EMPLOYEE_CONFLICT_USER;
    public final static String MANAGER_USER_IDENTITY = "getCallerIdentity()=" + MANAGER_USER;
    public final static String NO_ROLE_USER_IDENTITY = "getCallerIdentity()=" + NO_ROLE_USER;
    public final static String DECLARED_ROLE_USER_IDENTITY = "getCallerIdentity()=" + DECLARED_ROLE_USER;
    public final static String IS_EMPLOYEE_TRUE = "isCallerInRole(Employee)=true";
    public final static String IS_EMPLOYEE_FALSE = "isCallerInRole(Employee)=false";
    public final static String IS_MANAGER_TRUE = "isCallerInRole(Manager)=true";
    public final static String IS_MANAGER_FALSE = "isCallerInRole(Manager)=false";
    public final static String IS_DECLARED_ROLE_TRUE = "isCallerInRole(DeclaredRole01)=true";
    public final static String IS_DECLARED_ROLE_FALSE = "isCallerInRole(DeclaredRole01)=false";
    public final static String IS_DECLARED_ROLE02_FALSE = "isCallerInRole(DeclaredRole02)=false";
    public final static String IS_EMP_TRUE = "isCallerInRole(Emp)=true";
    public final static String IS_EMP_FALSE = "isCallerInRole(Emp)=false";
    public final static String IS_MGR_TRUE = "isCallerInRole(Mgr)=true";
    public final static String IS_MGR_FALSE = "isCallerInRole(Mgr)=false";

    // Roles
    public final static String EMPLOYEE_ROLE = "Employee";
    public final static String MANAGER_ROLE = "Manager";
    public final static String DECLARED_ROLE = "DeclaredRole01";

    //Method Names from EJB
    public final static String EMPLOYEE_METHOD = "employee";//change to lower case for the jacc test
    public final static String MANAGER_METHOD = "manager";//change to lower case for the jacc test
    public final static String EMPLOYEE_AND_MANAGER_METHOD = "employeeAndManager";//added for jacc test
    public final static String DENY_ALL_METHOD = "denyAll";
    public final static String RUN_AS_SPECIFIED_METHOD = "runAsSpecified";

    // Server.xml config files for dynamic updates
    public final static String DEFAULT_CONFIG_FILE = "pureAnn.server.org.xml";
    public final static String BAD_RUNAS_PWD_SERVER_XML = "pureAnn.badRunasPwd.xml";
    public final static String GOOD_RUNAS_PWD_SERVER_XML = "pureAnn.goodRunasPwd.xml";
    public final static String JACC_FEATURE_NOT_ENABLED = "noJaccFeature.xml";

    public final static String MERGE_CONFLICT_RUNAS_SERVER_XML = "pureAnn.mergeconflict.xml";
    public final static String DEFAULT_MERGE_SERVER_XML = "pureAnn.mergebindings.server.org.xml";

    public final static String SERVLET_TO_EJB_RUNAS_SERVER_XML = "pureAnn.servletToEJBRunAs.xml";
    public final static String SERVLET_TO_EJB_RUNAS_MISSING = "pureAnn.servletToEJBRunAsMissing.xml";

    // RoleMappings props location
    public final static String ROLE_MAPPING_PROPS_DEFAULT_LOCATION = "/resources/security/";
    public final static String DEFAULT_ROLE_MAPPING_PROPS_LOCATION = "/resources/security/roleMapping.props";
    public final static String NEW_ROLE_MAPPING_PROPS_LOCATION = "/config/";

    //Log messages constants
    public static final int DEFAULT_LOG_SEARCH_TIMEOUT = 120 * 1000;
    public static final int MESSAGE_NOT_EXPECTED_LOG_SEARCH_TIMEOUT = 30 * 1000;
    public final static List<String> NO_MSGS = null;
}
