/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.utils;

import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import java.util.Map;
import java.util.Properties;

public class PasswordSuppressingProperties extends Properties implements FFDCSelfIntrospectable
{
  public PasswordSuppressingProperties()
  {
    super();
  }

  public PasswordSuppressingProperties(Properties defaults)
  {
    super(defaults);
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    boolean first = true;
    for(Map.Entry<Object,Object> entry : entrySet())
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
