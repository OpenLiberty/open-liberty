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

import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import java.util.HashMap;
import java.util.Map;

public class PasswordSuppressingHashMap<K,V> extends HashMap<K,V> implements FFDCSelfIntrospectable
{
  public PasswordSuppressingHashMap()
  {
    super();
  }

  public PasswordSuppressingHashMap(int initialCapacity)
  {
    super(initialCapacity);
  }

  public PasswordSuppressingHashMap(int initialCapacity, float loadFactor)
  {
    super(initialCapacity,loadFactor);
  }

  public PasswordSuppressingHashMap(Map<? extends K, ? extends V> m)
  {
    super(m);
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    boolean first = true;
    for(Map.Entry<K,V> entry : entrySet())
    {
      if (!first)
        sb.append(", ");
      else
        first = false;

      String keyString = "null";
      if (entry.getKey() != null) keyString = entry.getKey().toString();


      sb.append(keyString);
      sb.append("=");
      sb.append(PasswordUtils.replaceValueIfKeyIsPassword(keyString,entry.getValue()));
    }
    sb.append("}");
    return sb.toString();
  }

  public String[] introspectSelf()
  {
    return new String[] { toString() };
  }
}
