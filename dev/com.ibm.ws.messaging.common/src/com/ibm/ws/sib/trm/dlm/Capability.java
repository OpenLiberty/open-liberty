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
import com.ibm.ws.sib.utils.ras.SibTr;

public class Capability {

  private static final TraceComponent tc = SibTr.register(Capability.class, TrmConstants.MSG_GROUP, TrmConstants.MSG_BUNDLE);
  
  private static final String PRE_MEDIATION_PUT_STRING  = "pre.mediation.put";
  private static final String POST_MEDIATION_PUT_STRING = "post.mediation.put";
  private static final String GET_STRING                = "get";

  public static final Capability PRE_MEDIATION_PUT  = new Capability(PRE_MEDIATION_PUT_STRING);
  public static final Capability POST_MEDIATION_PUT = new Capability(POST_MEDIATION_PUT_STRING);
  public static final Capability GET                = new Capability(GET_STRING);

  public String toString () {
    return name;
  }

  private final String name;

  // Private constructor prevents this class being extended so there is no need
  // to make this class final

  private Capability (String name) {
    if (tc.isEntryEnabled()) SibTr.entry(tc, "Capability", new Object[] { name });
    
    this.name = name;
    
    if (tc.isEntryEnabled()) SibTr.exit(tc, "Capability", this);
  }

  public static Capability get (String name) {
    if (tc.isEntryEnabled()) SibTr.entry(tc, "get", new Object[] { name });
    
    Capability rc;

    if (name.equals(PRE_MEDIATION_PUT_STRING)) {
      rc = PRE_MEDIATION_PUT;
    } else if (name.equals(POST_MEDIATION_PUT_STRING)) {
      rc = POST_MEDIATION_PUT;
    } else if (name.equals(GET_STRING)) {
      rc = GET;
    } else {
      throw new IllegalArgumentException(name);
    }

    if (tc.isEntryEnabled()) SibTr.exit(tc, "get", rc);
    return rc;
  }

}
