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
package com.ibm.ws.sib.processor.test.utils;

import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;

/**
 * This class was copied from SIMPUtils which is part of the processor.impl
 * component, but cannot be accessed from this separate component.
 */
public class UT_SIMPUtils
{

  public static final int UUID_LENGTH_8 = 8;
  public static final int UUID_LENGTH_12 = 12;

  /**
   * Return a string padded (or truncated) to the specified length
   * 
   * @param s
   * @param length
   * @return
   */
  public static String pad(String s, int length)
  {
    if (s.length() < length)
    {
      StringBuffer a = new StringBuffer(length);
      a.append(s);
      for (int i = s.length(); i < length; i++)
      {
        a = a.append(" ");
      }
      return a.toString();
    }
    else if (s.length() > length)
    {
      return s.substring(0, length);
    }
    else
    {
      return s;
    }
  }

  public static SIBUuid8 createSIBUuid8(String s)
  {
    return new SIBUuid8(pad(s, UUID_LENGTH_8).getBytes());

  }

  public static SIBUuid12 createSIBUuid12(String s)
  {
    return new SIBUuid12(pad(s, UUID_LENGTH_12).getBytes());
  }
}
