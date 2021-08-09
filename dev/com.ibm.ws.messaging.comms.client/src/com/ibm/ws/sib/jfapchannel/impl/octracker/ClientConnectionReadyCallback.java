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
 
package com.ibm.ws.sib.jfapchannel.impl.octracker;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.ConnectRequestListener;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.utils.Semaphore;

import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Callback used by clients to detect that a connection has been made.
 * Essentially posts a semaphore.
 */
class ClientConnectionReadyCallback implements ConnectRequestListener
{
   private static final TraceComponent tc = SibTr.register(ClientConnectionReadyCallback.class,
                                                           JFapChannelConstants.MSG_GROUP,
                                                           JFapChannelConstants.MSG_BUNDLE);

   static
   {
      if (tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/octracker/ClientConnectionReadyCallback.java, SIB.comms, WASX.SIB, uu1215.01 1.7");
   }

   private Semaphore semaphore;

   private Exception exception;

   protected ClientConnectionReadyCallback(Semaphore s)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", s);
      semaphore = s;
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   protected Exception getException()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getException");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getException", exception);
      return exception;
   }

   protected boolean connectionSucceeded()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "connectionSucceeded");
      boolean returnValue = exception == null;
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "connectionSucceeded", "" + returnValue);
      return returnValue;
   }

   public void connectRequestSucceededNotification(NetworkConnection vc)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "connectRequestSucceededNotification", vc);
      semaphore.post();
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "connectRequestSucceededNotification");
   }

   public void connectRequestFailedNotification(Exception e)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "connectRequestFailedNotification", e);
      if (tc.isEventEnabled() && (e != null)) SibTr.exception(this, tc, e);
      exception = e;
      semaphore.post();
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "connectRequestFailedNotification");
   }
}

