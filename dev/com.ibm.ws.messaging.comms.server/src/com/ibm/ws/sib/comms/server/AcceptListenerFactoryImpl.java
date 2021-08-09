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
package com.ibm.ws.sib.comms.server;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.jfapchannel.AcceptListener;
import com.ibm.ws.sib.jfapchannel.server.AcceptListenerFactory;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The Comms implementation of an accept listener that returns the generic listener which does
 * the real work.
 */
public class AcceptListenerFactoryImpl implements AcceptListenerFactory
{
   //@start_class_string_prolog@
   public static final String $sccsid = "@(#) 1.1 SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/AcceptListenerFactoryImpl.java, SIB.comms, WASX.SIB, aa1225.01 07/09/03 09:31:43 [7/2/12 05:59:34]";
   //@end_class_string_prolog@
   
   private static final TraceComponent tc = SibTr.register(AcceptListenerFactoryImpl.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);
   
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, $sccsid);
   }
   
   public AcceptListener manufactureAcceptListener()
   {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "manufactureAcceptListener");

      final AcceptListener al = new GenericTransportAcceptListener();
      
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "manufactureAcceptListener", al);
      return al;
   }
}
