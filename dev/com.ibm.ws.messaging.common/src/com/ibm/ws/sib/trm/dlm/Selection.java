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

package com.ibm.ws.sib.trm.dlm;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.trm.TrmConstants;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class wrappers a messaging engine uuid with additional information
 * about how the selected messaging engine was selected by the Destination
 * Location Manager.
 */

public final class Selection {

  private static final TraceComponent tc = SibTr.register(Selection.class, TrmConstants.MSG_GROUP, TrmConstants.MSG_BUNDLE);

  private SIBUuid8 uuid;
  private boolean  authoritative;
  private String nonAuthoritativeReason = null;

  public Selection (SIBUuid8 u, boolean a) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "Selection", new Object[] { u, a });

    uuid = u;
    authoritative = a;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "Selection", this);
  }

  /**
   * Alternative constructor for non-authoritative choices which allows a reason
   * string to be provided which describes why the choice was non-authoritative.
   * @param u UUID of the messaging engine that this selection represents
   * @param nonAuthDescr Description of why this choice is not authoritative.
   */
  public Selection (SIBUuid8 u, String nonAuthDescr)
  {
    this(u, false);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "Selection", new Object[] { u, nonAuthDescr });
    nonAuthoritativeReason = nonAuthDescr;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "Selection", this);
  }

  /**
   * Method used to retrieve the selected messaging engine uuid
   *
   * @return SIBUuid8 the uuid of the selected messaging engine
   */

  public SIBUuid8 getUuid() {
    return uuid;
  }

  /**
   * Method used to find out whether the selected messaging engine uuid was
   * authoritively selected or not (so is a best guess).
   *
   * @return boolean true if the answer is authoritative
   */

  public boolean isAuthoritative() {
    return authoritative;
  }

  // Utility methods

  public String toString () {
    return "uuid="+uuid.toString()+",authoritative="+authoritative+
      ((nonAuthoritativeReason == null) ? "" : ",nonAuthoritativeReason=["+nonAuthoritativeReason+"]");
  }

}
