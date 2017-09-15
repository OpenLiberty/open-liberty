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

package com.ibm.ws.sib.mfp.jmf.impl;

import java.math.BigInteger;
import java.util.HashMap;

import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 * MessageMapTable uses either a HashMap or an array purpose of storing MessageMaps.
 * Because multiChoice codes are densely assigned and the number of possible codes can be
 * predicted based on the multiChoiceCount of a schema, we can use an array rather than a
 * HashMap when the multiChoiceCount is small.
 */

public final class MessageMapTable {
  private static TraceComponent tc = JmfTr.register(MessageMapTable.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  private static final int LIMIT = 8192; // Maximum multiChoiceCount for using an array
  private MessageMap[] array; // The array if an array is used
  private HashMap hashtable;  // The hashtable if a hashtable is used

  /**
   * Construct a MessageMapTable with a particular capacity
   */
  public MessageMapTable(BigInteger capacity) {
    int intCapacity = Integer.MAX_VALUE;
    if (capacity.bitLength() < 32)
      intCapacity = capacity.intValue();

    if (intCapacity <= LIMIT)
      array = new MessageMap[intCapacity];
    else
      hashtable = new HashMap();
  }

  /**
   * Get an element from the table
   */
  public MessageMap getMap(BigInteger index) {
    if (array != null)
      return array[index.intValue()];
    else
      return (MessageMap)hashtable.get(index);
  }

  /**
   * Set an element into the table
   */
  public void set(MessageMap value) {
    if (array != null)
      array[value.multiChoice.intValue()] = value;
    else
      hashtable.put(value.multiChoice, value);
  }
}
