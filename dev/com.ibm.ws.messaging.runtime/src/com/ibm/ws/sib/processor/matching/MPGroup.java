/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.matching;

import java.security.Principal;
import java.util.List;

import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author Neil Young
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class MPGroup extends MPPrincipal
{
  private static final TraceComponent tc =
    SibTr.register(MPGroup.class, SIMPConstants.MP_TRACE_GROUP, SIMPConstants.RESOURCE_BUNDLE);

  /* Output source info */
  static {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.processor.impl/src/com/ibm/ws/sib/processor/matching/MPGroup.java, SIB.processor, WASX.SIB, ff1246.02 1.11");
  }

    public MPGroup(String nm)
    {
      super(nm.toLowerCase());
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.entry(tc, "MPGroup", nm);
        SibTr.exit(tc, "MPGroup", this);
      }
    }

    /**
     * Returns true if the passed principal is a member of the group.
     * This method does a recursive search, so if a principal belongs to a
     * group which is a member of this group, true is returned.
     *
     * @param member the principal whose membership is to be checked.
     *
     * @return true if the principal is a member of this group,
     * false otherwise.
     */
    public boolean isMember(Principal member)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "isMember", "is: " + member + ", a member of: " + this);

      boolean result = false;

      if(member instanceof MPPrincipal)
      {
        List theGroups = ((MPPrincipal)member).getGroups();
        if(theGroups != null)
          result = theGroups.contains(getName());
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "isMember", new Boolean(result));

      return result;
    }

}
