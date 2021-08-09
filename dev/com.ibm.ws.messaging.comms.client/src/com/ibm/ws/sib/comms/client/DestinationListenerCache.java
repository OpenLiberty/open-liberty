/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationListener;

/*
 * An instance of this class is used to hold a cache of listeners registered via the addDestinationListener
 * method of SICoreConnection. An 'id' value is used to refer to specific listener instances in communications
 * between client and server sides.
 */

public final class DestinationListenerCache {

  //@start_class_string_prolog@
  public static final String $sccsid = "@(#) 1.5 SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/DestinationListenerCache.java, SIB.comms, WASX.SIB, uu1215.01 07/09/27 09:50:53 [4/12/12 22:13:38]";
  //@end_class_string_prolog@

  private static final TraceComponent tc = SibTr.register(DestinationListenerCache.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);

  static {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: " + $sccsid);
  }

  private final Map<Short, DestinationListener> cache = Collections.synchronizedMap(new HashMap<Short, DestinationListener>());

  // Return a boolean indicating whether the supplied DestinationListener is contained in the cache or not

  public boolean contains (DestinationListener dl) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "contains", dl);

    final boolean rc = cache.containsValue(dl);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "contains", rc);
    return rc;
  }

  // Return a boolean indicating whether the supplied Id is contained in the cache or not

  public boolean contains (short id) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "contains", id);

    final boolean rc = cache.containsKey(id);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "contains", rc);
    return rc;
  }

  // Get the DestinationListener identified by id from the cache, return null if no match found

  public DestinationListener get (short id) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "get", id);

    final DestinationListener rc = cache.get(Short.valueOf(id));

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "get", rc);
    return rc;
  }

  // Add a listener to the cache. If the listner already exists in the cache then return the id value
  // already associated with the listener.

  public synchronized short add (DestinationListener dl) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "add", dl);

    short rc = 0;

    if (cache.containsValue(dl)) {  // Does cache already contain this listener?
      for (Short s: cache.keySet()) {
        if (cache.get(s.shortValue()).equals(dl)) {
          rc = s;
          break;
        }
      }
    } else {                      // Cache does not already contain this listener
      for (short s=1; s<=Short.MAX_VALUE; s++) { // Look for a free Id value
        if (!cache.containsKey(Short.valueOf(s))) {
          rc = s;
          break;
        }
      }

      if (rc == 0) {              // If no free Id value found throw an exception
        throw new RuntimeException("Cache full - no free Id values");
      }

      cache.put(Short.valueOf(rc), dl);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "add", rc);
    return rc;
  }

  // Add a listener to the cache with a specified Id

  public synchronized void add (short id, DestinationListener dl) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "add", new Object[]{id,dl});

    cache.put(Short.valueOf(id), dl);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "add");
  }

  public String toString () {
    return "cache="+cache.toString();
  }

}

// End of file
