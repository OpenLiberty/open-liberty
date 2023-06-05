/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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

package com.ibm.ws.messaging.service;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.management.ObjectName;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.messaging.lifecycle.Singleton;
import com.ibm.ws.sib.admin.JsAdminService;
import com.ibm.ws.sib.admin.JsBus;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsMain;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.JsProcessComponent;
import com.ibm.ws.sib.admin.SIBExceptionBusNotFound;
import com.ibm.ws.sib.admin.internal.JsMainImpl;
import com.ibm.ws.sib.utils.ras.SibTr;

@Component (configurationPolicy=IGNORE, property={"type=com.ibm.ws.sib.admin.JsAdminService", "service.vendor=IBM"})
public class JsAdminServiceImpl implements JsAdminService, Singleton {
  private static TraceComponent tc = SibTr.register(JsAdminServiceImpl.class, JsConstants.MSG_GROUP, JsConstants.MSG_BUNDLE);

static {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Source info: com/ibm/ws/messaging/service/JsAdminServiceImpl.java");
  }

  private final JsMainImpl jsMain;

  @Activate
  public JsAdminServiceImpl(@Reference JsMainImpl jsMainImpl) {
	    String methodName = "<init>";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
            SibTr.entry(tc, methodName, new Object[] {this, jsMainImpl});
        
        this.jsMain = jsMainImpl;
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, methodName);
        }
  }
  
  public String quoteJmxPropertyValue(String s) {
    if (JsAdminService.isValidJmxPropertyValue(s) == true)
      return s;
    else
      return ObjectName.quote(s);
  }

  public String unquoteJmxPropertyValue(String s) {
    return ObjectName.unquote(s);
  }

  public boolean isInitialized() {
    return jsMain != null;
  }

  public JsBus getBus(String name) throws SIBExceptionBusNotFound {
    if (!isInitialized()) {
      return null;
    }
    return jsMain.getBus(name);
  }

  public JsBus getDefinedBus(String name) throws SIBExceptionBusNotFound {
    if (!isInitialized()) {
      return null;
    }
    return jsMain.getDefinedBus(name);
  }

  public List<String> listDefinedBuses() {
    if (!isInitialized()) {
      return new ArrayList();
    }
    return jsMain.listDefinedBuses();
  }

  public JsProcessComponent getProcessComponent(String className) {
    if (!isInitialized()) {
      return null;
    }
    return jsMain.getProcessComponent(className);
  }

  public Enumeration listMessagingEngines() {
    if (!isInitialized()) {
      Vector v = new Vector();
      return v.elements();
    }
    return jsMain.listMessagingEngines();
  }

  public Enumeration listMessagingEngines(String busName) {
    if (!isInitialized()) {
      Vector v = new Vector();
      return v.elements();
    }
    return jsMain.listMessagingEngines(busName);
  }

  public Set getMessagingEngineSet(String busName) {
    if (!isInitialized()) {
      return new HashSet();
    }
    return jsMain.getMessagingEngineSet(busName);
  }

  public JsMessagingEngine getMessagingEngine(String busName, String engine) {
    if (!isInitialized()) {
      return null;
    }
    return jsMain.getMessagingEngine(busName, engine);
  }

  public void activateJMSResource() {}

  public void deactivateJMSResource() {}

  public Object getService(Class<?> c) {
    if (!isInitialized()) {
      return null;
    }
    return jsMain.getService(c);
  }

  public boolean isStandaloneServer() {
      return true;
  }
}
