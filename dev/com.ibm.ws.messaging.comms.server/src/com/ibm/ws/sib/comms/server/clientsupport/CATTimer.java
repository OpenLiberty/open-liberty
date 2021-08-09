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
package com.ibm.ws.sib.comms.server.clientsupport;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.processor.MPConsumerSession;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * The listener class that will be notified when our timer expires.
 * This is used when doing a synchronous but non-blocking on the server.
 * 
 * @author Gareth Matthews
 */
public class CATTimer implements AlarmListener
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = CATTimer.class.getName();

   /**
    * The asynch reader associated with the timer
    */
   private CATSyncAsynchReader asynchReader = null;
   
   /**
    * Register our trace component
    */
   private static final TraceComponent tc = SibTr.register(CATTimer.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   static {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/CATTimer.java, SIB.comms, WASX.SIB, aa1225.01 1.27");
   }
   
   /**
    * The constructer creates the CATTimer and associates the asynch reader
    * with it. Note that constructing this class does not start the timer.
    * This class is merely the class that will be notified when the timer
    * expires.
    * 
    * @param asynchReader The asynch reader that is also waiting
    *                     for a message.
    */
   public CATTimer(CATSyncAsynchReader asynchReader)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", asynchReader);
      
      this.asynchReader = asynchReader;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }
   
   /** 
    * This method is called by the Alarm manager when the timeout
    * is exceeded.
    * 
    * @param alarmObj The alarm object - not used.
    */
   public void alarm(Object alarmObj)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "alarm", alarmObj);
      
      // Has the async reader already sent a message back to the client?
      boolean sessionAvailable = true;
      if (!asynchReader.isComplete())
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Async reader has not yet got a message");
         // If not stop the session...
         try 
         {
            try
            {
               asynchReader.stopSession();
            }
            catch (SISessionDroppedException e)
            {
               // No FFDC Code Needed
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught a SISessionDroppedException", e);
               sessionAvailable = false;
               
               // If the session was unavailable this may be because the connection has been closed 
               // while we were in a receiveWithWait(). In this case, we should try and send an error
               // back to the client, but not worry too much if we cannot
               if (asynchReader.isConversationClosed())
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                     SibTr.debug(this, tc, "The Conversation was closed - no need to panic");
               }
               else
               {
                  // The Conversation was not closed, throw the exception on so we can inform
                  // the client
                  throw e;
               }
            }
            catch (SISessionUnavailableException e)
            {
               // No FFDC Code Needed
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught a SISessionUnavailableException", e);
               sessionAvailable = false;
               
               // See the comments above...
               if (asynchReader.isConversationClosed())
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                     SibTr.debug(this, tc, "The Conversation was closed - no need to panic");
               }
               else
               {
                  throw e;
               }
            }
         }
         catch (SIException sis)
         {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if(!asynchReader.hasMETerminated())
            {
               FFDCFilter.processException(sis, CLASS_NAME + ".alarm", 
                                           CommsConstants.CATTIMER_ALARM_01, this);
            }
            
            sessionAvailable = false;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, sis.getMessage(), sis);
            
            asynchReader.sendErrorToClient(sis,
                                           CommsConstants.CATTIMER_ALARM_01);
         }
         finally
         {
            //At this point we should deregister asyncReader as a SICoreConnectionListener as the current receive with wait is pretty much finished.
            //Note that there is a bit of a timing window where asyncReader.consumeMessages could be called and we end up deregistering 
            //the listener twice but that is allowed by the API. We minimize this window by only doing a dereg if the asyncReader isn't complete.
            if(!asynchReader.isComplete())
            {
               try
               {
                  final MPConsumerSession mpSession = (MPConsumerSession) asynchReader.getCATMainConsumer().getConsumerSession();
                  mpSession.getConnection().removeConnectionListener(asynchReader);
               }
               catch(SIException e)
               {
                  //No FFDC code needed   
                  if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, e.getMessage(), e);
                  
                  //No need to send an exception back to the client in this case as we are only really performing tidy up processing.
                  //Also, if it is a really bad problem we are likely to have already send the exception back in the previous catch block.
               }
            }
         }
      }
      
      if (sessionAvailable)
      {
         // ...and check again.
         // If the async reader has still not sent a message at this point we
         // assume a timeout and inform the client
         if (asynchReader.isComplete())
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Async reader got a message");
         }
         else
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "No message received");
            
            asynchReader.sendNoMessageToClient();
         }
      }
            
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "alarm");
   }
}
