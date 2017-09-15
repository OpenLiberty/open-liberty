/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl.rldispatcher;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.Dispatchable;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.ReceiveListener;
import com.ibm.ws.sib.jfapchannel.impl.Connection;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.util.ObjectPool;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;


/**
 * Represents an invocation of the error occurred method of a receive listener
 * implementation.  This object encapsulates all the information necessary to
 * invoke the error occurred method.
 * @see ReceiveListener#errorOccurred(SIConnectionLostException, int, int, int, Conversation)
 */
final class ReceiveListenerErrorOccurredInvocation extends AbstractInvocation
{
   private static final TraceComponent tc = SibTr.register(ReceiveListenerErrorOccurredInvocation.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);
   private SIConnectionLostException exception;
   private ObjectPool owningPool;
   private ReceiveListener listener;

   protected ReceiveListenerErrorOccurredInvocation(Connection connection,
                                                    ReceiveListener listener,
                                                    SIConnectionLostException exception,
                                                    int segmentType,
                                                    int requestNumber,
                                                    int priority,
                                                    Conversation conversation,
                                                    ObjectPool owningPool)
   {
      super(connection,
            0,
            segmentType,
            requestNumber,
            priority,
            conversation);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>",
                                           new Object[]
                                           {
                                              connection,
                                              listener,
                                              exception,
                                              ""+segmentType,
                                              ""+requestNumber,
                                              ""+priority,
                                              conversation,
                                              owningPool
                                           });

      this.listener = listener;
      this.exception = exception;
      this.owningPool = owningPool;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   // Start F201521
   /**
    * Not needed for error invocations
    *
    * @return Returns null.
    */
   protected Dispatchable getThreadContext()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getThreadContext");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getThreadContext");

      return null;
   }
   // End F201521

   /**
    * Invokes the error occurred callback of a receive listener.  The information
    * required for this invocation is encapsulated in this class.  If code in the
    * callback throws an exception then the connection is invalidated.
    */
   protected synchronized void invoke()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "invoke");
      try
      {
         listener.errorOccurred(exception,
                                segmentType,
                                requestNumber,
                                priority,
                                conversation);
      }
      catch(Throwable t)
      {
         FFDCFilter.processException
           (t, "com.ibm.ws.sib.jfapchannel.impl.rldispatcher.ReceiveListenerErrorOccurredInvocation.invoke", JFapChannelConstants.RLERROROCCURREDINVOKE_INVOKE_01);
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception thrown by receive listener error occurred implementation:");
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, t);

         connection.invalidate(true, t, "exception thrown in errorOccurred method - "+t.getMessage());   // D224570
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc , "invoke");
   }

   /** Resets the state of this object.  Used for pooling. */
   protected synchronized void reset(Connection connection,
                        ReceiveListener listener,
                        SIConnectionLostException exception,
                        int segmentType,
                        int requestNumber,
                        int priority,
                        Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "reset",
                                           new Object[]
                                           {
                                              connection,
                                              listener,
                                              exception,
                                              ""+segmentType,
                                              ""+requestNumber,
                                              ""+priority,
                                              conversation
                                           });

      this.connection = connection;
      this.listener = listener;
      this.exception = exception;
      this.segmentType = segmentType;
      this.requestNumber = requestNumber;
      this.priority = priority;
      this.conversation = conversation;
      setDispatchable(null);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "reset");
   }

   /** Returns this object to its associated object pool. */
   protected synchronized void repool()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "repool");

      // begin F181705.5
      connection = null;
      listener = null;
      exception = null;
      conversation = null;
      // end F181705.5
      owningPool.add(this);
      setDispatchable(null);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "repool");
   }
}
