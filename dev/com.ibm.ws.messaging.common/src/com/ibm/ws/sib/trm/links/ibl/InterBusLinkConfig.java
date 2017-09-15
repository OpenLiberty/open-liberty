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

package com.ibm.ws.sib.trm.links.ibl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.admin.RuntimeEventListener;
import com.ibm.ws.sib.trm.TrmConstants;
import com.ibm.ws.sib.utils.Password;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class represents an inter-bus link to another bus from a single
 * messaging engine.
 */

public final class InterBusLinkConfig {

  private static final TraceComponent tc = SibTr.register(InterBusLinkConfig.class, TrmConstants.MSG_GROUP, TrmConstants.MSG_BUNDLE);

  private String name;            // Name of other engine
  private String bus;             // Name of other bus
  private String link;            // Name of the link (as known to both ends)
  private String transportChain;  // Required transport chain
  private String bootstrapEPs;    // Host:Port:Chain,... of bootstrap messaging engines
  private String authAlias;       // Authentication alias
  private String userid;          // Userid to use to contact engine
  private Password password;        // Password to use to contact engine
  private SIBUuid12 uuid;         // Uuid of the link in this bus
  private boolean initialState;   // Initial state of link (started = true)
  private RuntimeEventListener runtimeEventListener; // Associated event management object reference

  // Constructors

  public InterBusLinkConfig () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
      SibTr.entry(tc, "InterBusLinkConfig");
      SibTr.exit(tc, "InterBusLinkConfig", this);
    }
  }

  public InterBusLinkConfig (String n, String b, String l, String t, String eps, String aa, String uid, Password pwd, SIBUuid12 u, boolean s, RuntimeEventListener rel) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "InterBusLinkConfig", new Object[] { n, b, l, t, eps, aa, uid, pwd, u, s, rel });

    name                 = n;
    bus                  = b;
    link                 = l;
    transportChain       = t;
    authAlias            = aa;
    bootstrapEPs         = eps;
    userid               = uid;
    password             = pwd;
    uuid                 = u;
    initialState         = s;
    runtimeEventListener = rel;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "InterBusLinkConfig", this);
  }

  // Setter methods

  public void setName (String n) {
    name = n;
  }

  public void setBus (String b) {
    bus = b;
  }

  public void setLink (String l) {
    link = l;
  }

  public void setTransportChain (String t) {
    transportChain = t;
  }

  public void setAuthAlias (String a) {
    authAlias = a;
  }

  public void setBootstrapEPs (String eps) {
    bootstrapEPs = eps;
  }

  public void setUserid (String uid) {
    userid = uid;
  }

  public void setPassword (Password pwd) {
    password = pwd;
  }

  public void setUuid (SIBUuid12 u) {
    uuid = u;
  }

  public void setInitialState (boolean s) {
    initialState = s;
  }

  // Getter methods

  public String getName () {
    return name;
  }

  public String getBus () {
    return bus;
  }

  public String getLink () {
    return link;
  }

  public String getTransportChain () {
    return transportChain;
  }

  public String getBootstrapEPs() {
    return bootstrapEPs;
  }

  public String getAuthAlias() {
    return authAlias;
  }

  public String getUserid () {
    return userid;
  }

  public Password getPassword () {
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPassword");

    //Password is safe (and useful) to trace
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getPassword", password);
    return password;
  }

  public SIBUuid12 getUuid () {
    return uuid;
  }

  public boolean getInitialState () {
    return initialState;
  }

  public RuntimeEventListener getRuntimeEventListener () {
    return runtimeEventListener;
  }

  // Utility methods

  public String toString () {
    return "name="+name+",bus="+bus+",link="+link+",transportChain="+transportChain+",authAlias="+authAlias+",bootstrapEPs="+bootstrapEPs+",userid="+userid+",uuid="+uuid+",initialState="+initialState+",runtimeEventListener="+runtimeEventListener;
  }

}
