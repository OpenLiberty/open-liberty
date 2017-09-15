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
import com.ibm.ws.sib.trm.TrmConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class represents an abstraction of a Cellule. LinkCellule's and
 * MessagingEngine's provide concrete implementations of this class.
 */

public abstract class Cellule {

  private static final TraceComponent tc = SibTr.register(Cellule.class, TrmConstants.MSG_GROUP, TrmConstants.MSG_BUNDLE);

  // Constants used in byte[0] for getBytes() methods

  final static byte LINKCELLULE     = 1;
  final static byte MESSAGINGENGINE = 2;

  /**
   * Test the current Cellule object to see if it is a LinkCellule
   *
   * @return true if the current object is a LinkCellule otherwise false
   */

  public boolean isLinkCellule () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "isLinkCellule");

    boolean rc = (this instanceof LinkCellule);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "isLinkCellule", Boolean.valueOf(rc));
    return rc;
  }

  /**
   * Test a byte[] to see if it represents a LinkCellule
   *
   * @param b The byte[]
   *
   * @return true if the byte[] represents a LinkCellule otherwise false
   */

  public static boolean isLinkCellule (byte[] b) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "isLinkCellule", new Object[]{ b });

    boolean rc = false;

    if (b.length > 0) {
      rc = (b[0] == LINKCELLULE);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "isLinkCellule", Boolean.valueOf(rc));
    return rc;
  }

  /**
   * Test the current Cellule object to see if it is a MessagingEngine
   *
   * @return true if the current object is a MessagingEngine otherwise false
   */

  public boolean isMessagingEngine () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "isMessagingEngine");

    boolean rc = (this instanceof MessagingEngine);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "isMessagingEngine", Boolean.valueOf(rc));
    return rc;
  }

  /**
   * Test a byte[] to see if it represents a MessagingEngine
   *
   * @param b The byte[]
   *
   * @return true if the byte[] represents a MessagingEngine otherwise false
   */

  public static boolean isMessagingEngine (byte[] b) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "isMessagingEngine", new Object[]{ b });

    boolean rc = false;

    if (b.length > 0) {
      rc = (b[0] == MESSAGINGENGINE);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "isMessagingEngine", Boolean.valueOf(rc));
    return rc;
  }

  public abstract byte[] getBytes();

  public abstract boolean equals (Object o);

  public abstract int hashCode ();

  public abstract String toString ();

}
