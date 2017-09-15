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

package com.ibm.ws.sib.trm.links;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.trm.TrmConstants;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class contains information about a selected link. For some selections
 * the inbound messaging engine value may be null.
 */

public final class LinkSelection {

  private static final TraceComponent tc = SibTr.register(LinkSelection.class, TrmConstants.MSG_GROUP, TrmConstants.MSG_BUNDLE);
  
  private SIBUuid8 outboundMeUuid;
  private SIBUuid8 inboundMeUuid;

  public LinkSelection (SIBUuid8 out, SIBUuid8 in) {
    if (tc.isEntryEnabled()) SibTr.entry(tc, "LinkSelection", new Object[] { out, in });
    
    outboundMeUuid = out;
    inboundMeUuid = in;
    
    if (tc.isEntryEnabled()) SibTr.exit(tc, "LinkSelection", this);
  }

  /**
   * Method used to retrieve the generic link outbound messaging engine uuid
   *
   * @return SIBUuid8 the uuid of the outbound messaging engine
   */

  public SIBUuid8 getOutboundMeUuid() {
    return outboundMeUuid;
  }

  /**
   * Method used to retrieve the generic link inbound messaging engine uuid
   *
   * @return SIBUuid8 the uuid of the inbound messaging engine
   */

  public SIBUuid8 getInboundMeUuid() {
    return inboundMeUuid;
  }

  // Utility methods

  public String toString () {
    return "outboundMeUuid="+outboundMeUuid+",inboundMeUuid="+inboundMeUuid;
  }

}
