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
package com.ibm.ws.sib.mfp;

import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *  This class holds the MFP Thread Local data, which currently consists of
 *  a single Integer. It provides a public get method for other components to
 *  access the value. Set methods are provided by a subclass in the
 *  com.ibm.ws,sib.mfp.impl component as such methods are not public.
 */
public class MfpThreadData {

  private static TraceComponent tc = SibTr.register(MfpThreadData.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

 
  // MFP's thread local data.
  // partnerLevel represents the WAS level of the ME or Client at the other end
  // of a connection for which a message is currently being encoded.
  // The value is an actually a com.ibm.ws.sib.comms.ProtocolVersion, but due to the
  // vagaries of the WAS build process we can't depend on any comms components from
  // this component.
  // The value is protected so that a subclass in the impl 'sub-package' can provide
  // setter methods for use by MFP's implementation components.
  protected final static ThreadLocal<Comparable> partnerLevel = new ThreadLocal<Comparable>();

  /**
   * Return the 'protocol version' of the connection partner for any current encode
   * on this thread. The value is a Comparable, which is actually an instance
   * of com.ibm.ws.sib.comms.ProtocolVersion.
   *
   * @return Comparable A Comparable whose value is the 'protocol version' of the current
   *                 connection partner, or null if no encode is in progress.
   */
  public static Comparable getPartnerLevel() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getPartnerLevel");
    Comparable pl = partnerLevel.get();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getPartnerLevel", pl);
    return pl;
  }

}
