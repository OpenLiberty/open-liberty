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
package com.ibm.ws.sib.mfp.impl;

import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.MfpThreadData;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 *  This class provides setter methods for the MFP Thread Local data owned
 *  by the MfpThreadData super-class.
 *  The methods all have package access so that they can be called by the other
 *  MFP implementation classes.
 */
public class MfpThreadDataImpl extends MfpThreadData {

  private static TraceComponent tc = SibTr.register(MfpThreadDataImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /*
   * Set the 'protocol version' of the connection partner for any current encode
   * on this thread. The value is a Comparable, which is actually an instance
   * of com.ibm.ws.sib.comms.ProtocolVersion.
   *
   * This method has package access as it is called by JsMessageObject
   */
  static void setPartnerLevel(Comparable level) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "setPartnerLevel", level);
    partnerLevel.set(level);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "setPartnerLevel");
  }

  /*
   * Clear the 'protocol version' of the connection partner for any current encode
   * on this thread.
   *
   * This method has package access as it is called by JsMessageObject
   */
  static void clearPartnerLevel() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "clearPartnerLevel");
    partnerLevel.set(null);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "clearPartnerLevel");
  }

}
