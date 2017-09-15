/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This abstract class represents a dispatch to all dispactchable. Users of the JFap channel,
 * when called on their receive listener for the thread context can return the instance of this
 * class and the request will be dispatched to all dispatch queues but only invoked by the last
 * queue that finds him.
 * 
 * @author Gareth Matthews
 */
public abstract class DispatchToAllNonEmptyDispatchable implements Dispatchable
{
   /** Trace */
   private static final TraceComponent tc = SibTr.register(DispatchToAllNonEmptyDispatchable.class, 
                                                           JFapChannelConstants.MSG_GROUP, 
                                                           JFapChannelConstants.MSG_BUNDLE);
   
   /** The singleton instance */
   private static DispatchToAllNonEmptyDispatchable instance = null;
   
   /** The exception that caused any failure */
   private static Exception createException = null;
   
   /**
    * Static initialiser - creates the actual instance of the class.
    */
   static
   {
      if (tc.isDebugEnabled()) SibTr.debug(tc, "@(#)SIB/ws/code/sib.jfapchannel.client/src/com/ibm/ws/sib/jfapchannel/DispatchToAllNonEmptyDispatchable.java, SIB.comms, WASX.SIB, uu1215.01 1.4");
      if (tc.isEntryEnabled()) SibTr.entry(tc, "static <init>");
      
      try
      {
         Class disClass = Class.forName(JFapChannelConstants.DISPATCH_TO_ALL_NONEMPTY_DISPATCHER_CLASS);
         instance = (DispatchToAllNonEmptyDispatchable) disClass.newInstance();
      }
      catch (Exception e)
      {
         FFDCFilter.processException(e, "com.ibm.ws.sib.jfapchannel.DispatchToAllNonEmptyDispatchable",
                                     JFapChannelConstants.DISPATCHTOALLNONEMPTY_STINIT_01);
                                     
         createException = e;
         
         SibTr.error(tc, "NO_DISPATCHTOALL_IMPL_SICJ0034", 
                     new Object[]
                     {
                        JFapChannelConstants.DISPATCHTOALLNONEMPTY_STINIT_01,
                        e
                     });
                     
         if (tc.isEventEnabled()) SibTr.exception(tc, e);
      }
      
      if (tc.isEntryEnabled()) SibTr.exit(tc, "static <init>");
   }
   
   /**
    * @return Returns the instance of the class that was created above.
    * 
    * @throws Exception if the create failed.
    */
   public static Dispatchable getInstance() throws Exception
   {
      if (instance == null) throw createException;
      return instance;
   }
}
