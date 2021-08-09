/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.comms.client;

import java.util.Vector;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.wsspi.sib.core.SICoreConnectionListener;
import com.ibm.ws.sib.utils.ras.SibTr;

public class CatConnectionListenerGroup extends Vector<SICoreConnectionListener>
{
   private static final long serialVersionUID = -2679732549257884229L;  // LIDB3706-5.195, D274182
   
   /**
    * Register Class with Trace Component
    */
   private static final TraceComponent tc =
      SibTr.register(
         CatConnectionListenerGroup.class,
         CommsConstants.MSG_GROUP,
         CommsConstants.MSG_BUNDLE);

   /**
    * Log Source code level on static load of class
    */
   static {
      if (tc.isDebugEnabled())
         SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/CatConnectionListenerGroup.java, SIB.comms, WASX.SIB, uu1215.01 1.11");
   }
   
   public void addConnectionListener(SICoreConnectionListener listener)
   {
      this.add(listener);
   }
   
   public void removeConnectionListener(SICoreConnectionListener listener)
   {
      this.remove(listener);
   }   
   
   public SICoreConnectionListener[] getConnectionListeners()
   {
      SICoreConnectionListener[] retVal = new SICoreConnectionListener[this.size()];
      for(int i=0; i < this.size(); ++i){
         retVal[i] = (SICoreConnectionListener)this.get(i);   
      }     
      return retVal;
   }
}
