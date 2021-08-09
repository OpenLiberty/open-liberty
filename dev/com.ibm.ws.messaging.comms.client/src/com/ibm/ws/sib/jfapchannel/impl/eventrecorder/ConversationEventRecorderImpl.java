/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.jfapchannel.impl.eventrecorder;

import java.util.Calendar;

import com.ibm.ws.sib.jfapchannel.approxtime.QuickApproxTime;         // 393407

/**
 * Implementation of the conversation level event recorder. 
 */
public class ConversationEventRecorderImpl extends EventRecorderImpl implements ConversationEventRecorder
{
   // Seperate array for each field of a event record.  This isn't good OO,
   // we should have an object that encapsulates each of these fields.  However,
   // in this case, good OO has a pretty tragic impact on heap usage...
   private final byte[] eventTypeArray;
   private final long[] eventTimestampArray;
   private final int[] eventSequenceArray;
   private final int[] eventThreadHashcode;
   private final String[] eventDescriptionArray;

   private int totalEvents;
   private int currentEvent;
   private final int maxEvents;
   private final ConnectionEventRecorder connectionEventRecorder;
   private final SequenceNumberGenerator sequenceNumberGenerator;
   private final QuickApproxTime approxTime;   
   
   protected ConversationEventRecorderImpl(ConnectionEventRecorder connectionEventRecorder,
                                           SequenceNumberGenerator sequenceNumberGenerator,
                                           QuickApproxTime approxTime,
                                           int maxEvents)
   {
      this.connectionEventRecorder = connectionEventRecorder;
      this.sequenceNumberGenerator = sequenceNumberGenerator;
      this.maxEvents = maxEvents;
      this.approxTime = approxTime;
      eventTypeArray = new byte[maxEvents];
      eventTimestampArray = new long[maxEvents];
      eventSequenceArray = new int[maxEvents];
      eventThreadHashcode = new int[maxEvents];
      eventDescriptionArray = new String[maxEvents];  
   }
   
   protected synchronized void fillInNextEvent(byte type, String description) 
   {
      eventTypeArray[currentEvent] = type;
      eventTimestampArray[currentEvent] = approxTime.getApproxTime();
      eventSequenceArray[currentEvent] = sequenceNumberGenerator.getNextSequenceNumber();
      eventDescriptionArray[currentEvent] = description;
      eventThreadHashcode[currentEvent] = Thread.currentThread().hashCode();
      currentEvent = (currentEvent + 1) % maxEvents;
      ++totalEvents;
   }

   public ConnectionEventRecorder getConnectionEventRecorder() 
   {
      return connectionEventRecorder;
   }
   
   public synchronized String toString()
   {
      final StringBuffer sb = new StringBuffer(""+totalEvents);
      sb.append(" conversation events recorded in total\n");
      sb.append("timestamp/sequence/thread/type/description\n");
      
      int eventIndex = (totalEvents >= maxEvents) ? currentEvent : 0;
      int eventCount = 0;
      
      while((eventTypeArray[eventIndex] != 0x00) && (eventCount < maxEvents))
      {
         Calendar calendar = Calendar.getInstance();
         calendar.setTimeInMillis(eventTimestampArray[eventIndex]);
         sb.append(calendar.get(Calendar.HOUR_OF_DAY));
         sb.append(":");
         sb.append(calendar.get(Calendar.MINUTE));
         sb.append(":");         
         sb.append(calendar.get(Calendar.SECOND));
         sb.append(":");
         sb.append(calendar.get(Calendar.MILLISECOND));
         sb.append(" ");
         sb.append(eventSequenceArray[eventIndex]);
         sb.append(" ");
         sb.append(Integer.toHexString(eventThreadHashcode[eventIndex]));
         sb.append("\t");
         sb.append((char)eventTypeArray[eventIndex]);
         sb.append("\t");
         sb.append(eventDescriptionArray[eventIndex]);
         sb.append("\n");
         
         eventIndex = (eventIndex + 1) % maxEvents;
         ++eventCount;
      }
      
      return sb.toString();
   }
}
