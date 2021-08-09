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

package com.ibm.ws.sib.admin;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Public Admin utilities
 */
public final class JsAdminUtils {

  private static final TraceComponent tc = SibTr.register(JsAdminUtils.class, JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);

  private JsAdminUtils() {
  }
  
  private static JsAdminUtilsInterface _instance = null;

  static
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "<clinit>");

    try {
      Class cls = Class.forName("com.ibm.ws.sib.admin.impl.JsAdminUtilsImpl");
      _instance = (JsAdminUtilsInterface) cls.newInstance();
    }
    catch (Exception e) {
      com.ibm.ws.ffdc.FFDCFilter.processException(e, "JsAdminUtils.<clinit>", "64", null);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Exception constructing JsAdminUtilsImpl", e);
      _instance = null;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<clinit>");
  }

  
  /*
   * Given a uuid of a messaging engine, find the name of that messaging engine
   * Use this version when getting the UUID simply to output it to an exception 
   * insert, or error message. 
   */
  public static String getMENameByUuidForMessage(String meUuid) {
  
        return _instance.getMENameByUuidForMessage(meUuid);
  
  }

  /*
   * Given a uuid of a messaging engine, find the name of that messaging engine
   */
  public static String getMENameByUuid(String meUuid) {
  
        return _instance.getMENameByUuid(meUuid);
  
  }

  
  /*
   * Given a uuid of a MQ server bus member, find the name of that MQ server bus member
   */
  public static String getMQServerBusMemberNameByUuid(String mqUuid) {
  
        return _instance.getMQServerBusMemberNameByUuid(mqUuid);
  	
  }
  
  
  /*
   * Given a uuid of a messaging engine, find the name of that messaging engine, in the given Session
   */
  public static String getMEUUIDByName(String meUuid) {
  
        return _instance.getMEUUIDByName(meUuid);  
  }
  
}

