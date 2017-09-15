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

/*
 * This class contains component wide constants used by all TRM implementation
 * packages and consequently has public scope.
 */

package com.ibm.ws.sib.trm.impl;

import com.ibm.ws.sib.trm.TrmConstants;

public final class TrmConstantsImpl extends TrmConstants {

  /*
   * FFDC probe ids
   */

  public final static String PROBE_1  = "1";
  public final static String PROBE_2  = "2";
  public final static String PROBE_3  = "3";
  public final static String PROBE_4  = "4";
  public final static String PROBE_5  = "5";
  public final static String PROBE_6  = "6";
  public final static String PROBE_7  = "7";
  public final static String PROBE_8  = "8";
  public final static String PROBE_9  = "9";
  public final static String PROBE_10 = "10";
  public final static String PROBE_11 = "11";
  public final static String PROBE_12 = "12";
  public final static String PROBE_13 = "13";
  public final static String PROBE_14 = "14";
  public final static String PROBE_15 = "15";
  public final static String PROBE_16 = "16";
  public final static String PROBE_17 = "17";
  public final static String PROBE_18 = "18";
  public final static String PROBE_19 = "19";
  public final static String PROBE_20 = "20";
  public final static String PROBE_21 = "21";
  public final static String PROBE_22 = "22";
  public final static String PROBE_23 = "23";
  public final static String PROBE_24 = "24";
  public final static String PROBE_25 = "25";
  public final static String PROBE_26 = "26";
  public final static String PROBE_27 = "27";
  public final static String PROBE_28 = "28";
  public final static String PROBE_29 = "29";
  public final static String PROBE_30 = "30";

  /*
   * Reply first contact message return codes
   */

  public final static int RETURN_CODE_CLASH     = 2;
  public final static int RETURN_CODE_REDIRECT  = 1;
  public final static int RETURN_CODE_OK        = 0;

  // Unspecific failure

  public final static int RETURN_CODE_NOK                                          = -1;

  // Core exceptions

  public final static int RETURN_CODE_SIResourceException                          = -2;
  public final static int RETURN_CODE_SIConnectionLostException                    = -3;
  public final static int RETURN_CODE_SILimitExceededException                     = -4;
  public final static int RETURN_CODE_SIErrorException                             = -5;
  public final static int RETURN_CODE_SINotAuthorizedException                     = -6;
  public final static int RETURN_CODE_SINotPossibleInCurrentConfigurationException = -7;
  public final static int RETURN_CODE_SIIncorrectCallException                     = -8;
  public final static int RETURN_CODE_SIAuthenticationException                    = -9;

  /*
   * System destination prefix used by TRM
   */

  public final static String TRM_PREFIX = "TRM";

  /*
   * Client credential types
   */

  public final static String CREDENTIAL_USERID_PASSWORD = "Userid+Password";
  public final static String CREDENTIAL_USER_SUBJECT    = "User subject";
  public final static String CREDENTIAL_SIB_SUBJECT     = "SIB subject";
  public final static String CREDENTIAL_SUBJECT  		= "Subject";
 
  /*
   * Security Token types
   */
  public final static String LTPA_TOKENTYPE = "LTPA";
  
  /*
   * Bus custom properties
   */
  
  /** Bus-wide custom property to enable V6.1 style intra-bus ME authentication that does not
   *  rely on SAP being enabled. */
  public final static String INTER_ENGINE_AUTH_MODE_BUS_PROPERTY = "authentication.intrabus.mode";
  public final static String INTER_ENGINE_AUTH_MODE_DEFAULT = "default";
  public final static String INTER_ENGINE_AUTH_MODE_AUTH_ALIAS = "authalias";
}
