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

package com.ibm.ws.sib.comms;

public class ProtocolVersion implements Comparable<ProtocolVersion>
{
   public static final ProtocolVersion UNKNOWN       = new ProtocolVersion("UNKNOWN",         0);
   public static final ProtocolVersion VERSION_6_0   = new ProtocolVersion("VERSION_6_0",   100);
   public static final ProtocolVersion VERSION_6_0_2 = new ProtocolVersion("VERSION_6_0_2", 200);
   public static final ProtocolVersion VERSION_6_1   = new ProtocolVersion("VERSION_6_1",   300);
   public static final ProtocolVersion VERSION_7     = new ProtocolVersion("VERSION_7",     400);
   
   private final String toStringValue;
   private final int version;
   
   private ProtocolVersion(String humanReadableForm, int version)
   {
      this.toStringValue = humanReadableForm;
      this.version = version;
   }
   
   public int ordinal()
   {
      return version;
   }
   
   public boolean equals(Object other)
   {
      if(other instanceof ProtocolVersion)
      {
         return ((ProtocolVersion)other).version == version;
      }
      
      return false;
   }
   
   public int hashCode()
   {
      return version;
   }

   public int compareTo(ProtocolVersion other)
   {
      return this.version - other.version;
   }
   
   public String toString()
   {
      return toStringValue;
   }
}
