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

package com.ibm.ws.sib.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class contains common runtime methods
 */

public final class Runtime {

  private static final TraceComponent tc = SibTr.register(Runtime.class, UtConstants.MSG_GROUP, UtConstants.MSG_BUNDLE);

  private static final ConcurrentMap<String,String> seenProperties = new ConcurrentHashMap<String, String>();
  
  /**
   * This method should be called each time a SIB property value is assigned a
   * none default value. An informational message is output for serviceability
   * reasons so that it is obvious that a property value has been changed.
   *
   * @param name the name of the property that has been changed
   * @param value the new value assigned to the changed property
   */

  public static void changedPropertyValue (String name, String value) {
    if (tc.isEntryEnabled()) SibTr.entry(tc, "changedPropertyValue");

    if (value == null) value = "null"; // Ensure that the new value is non-null (here we're only using for a message insert)

    if (!value.equals(seenProperties.put(name,value)))
    {
      // We haven't seen the property before or it's changed, so issue the message
      SibTr.info(tc, "RUNTIME_CWSIU0001", new Object[] {name,value});   //220097.0
    }

    if (tc.isEntryEnabled()) SibTr.exit(tc, "changedPropertyValue");
  }

}
