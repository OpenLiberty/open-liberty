/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.security;


/**
 * <p>This class contains various security related constants. I am moving away
 *   from AuthConstants because the constants are not authentication specific.
 * </p>
 *
 * <p>SIB build component: sib.security</p>
 *
 * @author nottinga
 * @version 1.5
 * @since 1.0
 */
public final class BusSecurityConstants
{
  /* ------------------------------------------------------------------------ */
  /* BusSecurityConstants method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * A Private constructor to stop instances of this class being created. 
   */
  private BusSecurityConstants()
  {
    
  }
  
  /** The security trace group name */
  public static final String TRC_GROUP       = com.ibm.ws.sib.utils.TraceGroups.TRGRP_SECURITY;
  /** The security message bundle name */
  public static final String MSG_BUNDLE      = "com.ibm.ws.sib.security.CWSIIMessages";
  
  /** The name of the SibLoginFactory implementation */
  public static final String SIB_LOGIN_CLASS = "com.ibm.ws.sib.security.auth.SibLoginFactoryImpl";
  /** The name of the SIBAccessControlFactory implementation */
  public static final String SIB_ACCESS_CONTROL_CLASS = "com.ibm.ws.sib.security.auth.SIBAccessControlFactoryImpl";
  /** The name of the AuthUtilsFactory implementation */
  public static final String AUTH_UTILS_CLASS = "com.ibm.ws.sib.security.auth.AuthUtilsFactoryImpl";
  /** The SecurityContextFactory implementation */
  public static final String SIB_CONTEXT_CLASS = "com.ibm.ws.sib.security.context.SecurityContextFactoryImpl";

  /** The permission that is checked when the SIBServerSubject is obtained */
  public static final String SIB_GET_SERVER_PERM = "SIBServerPermission";
  /** The name of the realm for the SIB server subject */
  public static final String SIB_REALM_NAME = "SIBRealm";
  /** The user id for the SIB server subject */
  public static final String SIB_SERVER_NAME = "SIBServer";
  /** The user id representing the host subject */
  public static final String HOST_ID_NAME = "HostServer";
  
  /** The name of the Everyone group */
  public static final String EVERYONE = "everyone";
  /** The name of the AllAuthenticated group */
  public static final String ALLAUTHENTICATED = "allauthenticated";
  /** The name of the host server group */
  public static final String SERVER = "server";
  
  /** The name of the setContextPermission */
  public final static String SIB_SET_CONTEXT_PERM = "PlatformMessagingComponentSetContextPermission";
  
  /** The name of the "global" domain, used for mixed version compatibility in a multi-domain world*/
  //public static final String WAS_GLOBAL_SECURITY_DOMAIN = CommonConstants.GLOBALSECURITY_DOMAIN;
  //Sharath Security: CommonConstants is not there in Liberty Security 
  public static final String WAS_GLOBAL_SECURITY_DOMAIN= "PassThroughToGlobalSecurity";
  
}
