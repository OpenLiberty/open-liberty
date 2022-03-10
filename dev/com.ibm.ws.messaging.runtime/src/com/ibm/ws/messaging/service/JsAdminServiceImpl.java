/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.messaging.service;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.management.ObjectName;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

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

@Component (configurationPolicy=REQUIRE, property="service.vendor=IBM")
public class JsAdminServiceImpl implements JsAdminService, Singleton {
  private static TraceComponent tc = SibTr.register(JsAdminServiceImpl.class, JsConstants.MSG_GROUP, JsConstants.MSG_BUNDLE);

  // Debugging aid
  static {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Source info: com/ibm/ws/messaging/service/JsAdminServiceImpl.java");
  }

  private JsMainImpl _jsmain = null;
  private boolean _multipleSet = false;

  public String quoteJmxPropertyValue(String s) {
    if (JsAdminService.isValidJmxPropertyValue(s) == true)
      return s;
    else
      return ObjectName.quote(s);
  }

  public String unquoteJmxPropertyValue(String s) {
    return ObjectName.unquote(s);
  }

  public synchronized void setAdminMain(JsMain o) {
    if (_jsmain != null) {
      // We have received a second or subsequent set request. This indicates an internal
      // programming error or some abuse of the interface. Rather than throw exceptions
      // at this point, we simply remember this and output some RAS. We defer the throwing
      // of any exception until a get request is received.
      _multipleSet = true;
      SibTr.info(tc, "ME_INITIALIZING_SIAS0001");
    } else
      _jsmain = (JsMainImpl) o;
    return;
  }

  public boolean isInitialized() {
    return _jsmain != null;
  }

  private synchronized void validateEnvironment() throws Exception {
    if (_multipleSet) {
      throw new Exception("Invalid object instance for admin service; multiple sets received");
    } else if (_jsmain == null) {
      throw new Exception("Object instance for the admin service was never set");
    }
  }

  public synchronized JsMain getAdminMain() throws Exception {
    validateEnvironment();
    return _jsmain;
  }

  public JsBus getBus(String name) throws SIBExceptionBusNotFound {
    if (!isInitialized()) {
      return null;
    }
    return _jsmain.getBus(name);
  }

  public JsBus getDefinedBus(String name) throws SIBExceptionBusNotFound {
    if (!isInitialized()) {
      return null;
    }
    return _jsmain.getDefinedBus(name);
  }

  public List<String> listDefinedBuses() {
    if (!isInitialized()) {
      return new ArrayList();
    }
    return _jsmain.listDefinedBuses();
  }

  public JsProcessComponent getProcessComponent(String className) {
    if (!isInitialized()) {
      return null;
    }
    return _jsmain.getProcessComponent(className);
  }

  public Enumeration listMessagingEngines() {
    if (!isInitialized()) {
      Vector v = new Vector();
      return v.elements();
    }
    return _jsmain.listMessagingEngines();
  }

  public Enumeration listMessagingEngines(String busName) {
    if (!isInitialized()) {
      Vector v = new Vector();
      return v.elements();
    }
    return _jsmain.listMessagingEngines(busName);
  }

  public Set getMessagingEngineSet(String busName) {
    if (!isInitialized()) {
      return new HashSet();
    }
    return _jsmain.getMessagingEngineSet(busName);
  }

  public JsMessagingEngine getMessagingEngine(String busName, String engine) {
    if (!isInitialized()) {
      return null;
    }
    return _jsmain.getMessagingEngine(busName, engine);
  }

  public void activateJMSResource() {}

  public void deactivateJMSResource() {}

  public Object getService(Class<?> c) {
    if (!isInitialized()) {
      return null;
    }
    return _jsmain.getService(c);
  }

  public boolean isStandaloneServer() {
      return true;
  }
}
