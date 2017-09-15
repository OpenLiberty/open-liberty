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

package com.ibm.ws.sib.trm.topology;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.trm.TrmConstants;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class represents a Messaging Engine within the bus. The messaging
 * engine represented may be remote.
 */

public final class MessagingEngine extends Cellule {

  private static final TraceComponent tc = SibTr.register(MessagingEngine.class, TrmConstants.MSG_GROUP, TrmConstants.MSG_BUNDLE);
  private static final TraceNLS nls = TraceNLS.getTraceNLS(TrmConstants.MSG_BUNDLE);

  private SIBUuid8 designation;

  /**
   * Constructor
   *
   * @param uuid The UUID of the messaging engine
   */

  public MessagingEngine (SIBUuid8 uuid) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "MessagingEngine", new Object[] { uuid });

    if (uuid == null) {
      throw new NullPointerException(nls.getFormattedMessage("NULL_USED_TO_CREATE_CWSIT0013", new Object[] {"SIBUuid8", "MessagingEngine"}, null));
    }

    designation = uuid;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "MessagingEngine", this);
  }

  /**
   * Constructor used to recreate a MessagingEngine from a byte[] previously
   * obtained using the getBytes() method.
   *
   * @param b saved byte array
   */

  public MessagingEngine (byte[] b) throws InvalidBytesException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "MessagingEngine", new Object[] { b });

    if (b[0] == Cellule.MESSAGINGENGINE) {
      byte[] c = new byte[b.length-1];

      for (int i=1; i < b.length; i++) {
        c[i-1] = b[i];
      }

      designation = new SIBUuid8(c);
    } else {
      throw new InvalidBytesException(nls.getFormattedMessage(
          "INVALID_BYTE_VALUE_CWSIT0054", new Object[] {
              Cellule.MESSAGINGENGINE, b[0] }, null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "MessagingEngine", this);
  }

  /**
   * Return a byte[] representation of the messaging engine. The returned bytes
   * can be used to create a new MessagingEngine object representing the same
   * messaging engine.
   *
   * @return byte representation of the messaging engine
   */

  public byte[] getBytes() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getBytes");

    byte[] b = designation.toByteArray();
    byte[] c = new byte[b.length+1];

    c[0] = Cellule.MESSAGINGENGINE;
    for (int i=0; i < b.length; i++) {
      c[i+1] = b[i];
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getBytes", c);
    return c;
  }

  // Utility methods

  public boolean equals (Object o) {
    boolean rc = false;

    if (o instanceof MessagingEngine) {
      MessagingEngine m = (MessagingEngine)o;
      rc = (designation.equals(m.designation));
    }

    return rc;
  }

  public SIBUuid8 getUuid () {
    return designation;
  }

  public int hashCode () {
    return designation.hashCode();
  }

  public String toString () {
    return designation.toString();
  }

}
