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

package com.ibm.ws.sib.processor.impl;

import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;

// Import required classes.

/**
 * The key of a remote browse session, at the DME, which is composed of 2 parts, the RME and the browse id.
 */
public final class AOBrowserSessionKey
{
  private final SIBUuid8 remoteMEUuid;
  private final SIBUuid12 gatheringTargetDestUuid;
  private final long browseId; // the unique Id of the browse session, wrt the remoteMEUuid

  /**
   * Constructor
   * @param remoteMEUuid The UUID of the remote ME
   * @param browseId The unique browse id created by this remote ME
   */
  public AOBrowserSessionKey(SIBUuid8 remoteMEUuid, SIBUuid12 gatheringTargetDestUuid, long browseId)
  {
    this.remoteMEUuid = remoteMEUuid;
    this.gatheringTargetDestUuid = gatheringTargetDestUuid;
    this.browseId = browseId;
  }

  public final SIBUuid8 getRemoteMEUuid()
  {
    return remoteMEUuid;
  }

  public final long getBrowseId() {
    return browseId;
  }

  /**
   * Overriding the Object.hashCode() method
   */
  public final int hashCode()
  {
    // the lower significant bits should be good enough to prevent frequent hash collisions
    return (int) (browseId % Integer.MAX_VALUE);
  }

  /**
   * Overriding the Object.equals() method
   */
  public final boolean equals(Object obj)
  {
    if (obj == null) return false;

    if (obj instanceof AOBrowserSessionKey)
    {
      AOBrowserSessionKey o = (AOBrowserSessionKey) obj;
      if (remoteMEUuid.equals(o.remoteMEUuid) &&
          browseId == o.browseId &&
          (gatheringTargetDestUuid == o.gatheringTargetDestUuid ||  // covers targetDestUuid=null
          gatheringTargetDestUuid.equals(o.gatheringTargetDestUuid)))
      {
        return true;
      }
    }

    return false;
  }
}
