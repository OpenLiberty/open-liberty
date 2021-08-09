/*******************************************************************************
 * Copyright (c) 2004, 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapByteBuffer;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.SendListener;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBufferPool;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Represents a transmission that is to be sent over a network connection.
 */
public class TransmissionData
{
   private static final TraceComponent tc = SibTr.register(TransmissionData.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

   static   
   {
      if (tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/TransmissionData.java, SIB.comms, WASX.SIB, uu1215.01 1.17");
   }     

   // Array of byte buffers which comprise data to transmit.
   private WsByteBuffer[] xmitDataBuffers;
   
   // Current index into xmitDataBuffers.
   private int currentXmitDataBufferIndex;
   
   // The connection that this data will be transmitted over.  This is only present so
   // that the code may close this connection in the event of an error.
   private Connection connection;
   
   // The conversation that this data will be transmitted on behalf of (if any).
   private Conversation conversation;
   
   // The send listener that will be notified when this data has been transmitted (if any)
   private SendListener sendListener;
   
   // Does this data represent a user request (and hence will be subject to the restrictions
   // of not being a primary header only and having restricted segment types available).
   private boolean isUserRequest;
   
   // Does this request represent a request to terminate sending of data?
   private boolean isTerminal;

   // Scratch space used for building headers when there is insufficient transmission buffer
   // available to build them in place.
   private WsByteBuffer headerScratchSpace;
   
   // Amount of unsent data remaining for this transmission.
   private int transmissionRemaining;

   // States for the state machine used for building tramissions
   private static final int STATE_BUILDING_PRIMARY_HEADER = 0;
   private static final int STATE_BUILDING_CONVERSATION_HEADER = 1;
   private static final int STATE_BUILDING_SEGMENT_HEADER = 2;
   private static final int STATE_BUILDING_PAYLOAD = 3;
   private static final int STATE_ERROR = 4;
   private int state = STATE_BUILDING_PRIMARY_HEADER;
   
   // Have we exhaused the space in the transmission buffer we are trying to fill?
   private boolean exhausedXmitBuffer = false;
   
   // Have we successfully built the transmission that we are attempting to build.
   private boolean transmissionBuilt = false;

   // The iterator that this transmission is used by.
   private TransmissionDataIterator iterator = null;                       // D226242

   /**
    * Constructor
    */
   protected TransmissionData(TransmissionDataIterator iterator)           // D226242
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");      
      this.iterator = iterator;                                            // D226242
      int sizeofLargestHeader =
         Math.max(JFapChannelConstants.SIZEOF_PRIMARY_HEADER, JFapChannelConstants.SIZEOF_CONVERSATION_HEADER);
      sizeofLargestHeader =
         Math.max(sizeofLargestHeader, JFapChannelConstants.SIZEOF_SEGMENT_START_HEADER);
      headerScratchSpace = 
         WsByteBufferPool.getInstance().allocate(sizeofLargestHeader);  // F196678.10
      headerScratchSpace.clear();      
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * Resets the transmission data instance so that it can be reused.  This is invoked
    * as part of the TransmissionDataIterator.next() method so that it can keep hold of
    * only one instance of this class. 
    * @param jfapBuffer
    * @param isUserRequest
    * @param isTerminal
    * @param connection
    */
   protected void reset(JFapByteBuffer jfapBuffer,
                        boolean isTerminal,
                        Connection connection)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "reset", new Object[] {jfapBuffer, ""+isTerminal, connection});
      this.isTerminal = isTerminal;
      this.connection = connection;
      this.xmitDataBuffers = jfapBuffer.getBuffersForTransmission();
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "reset");
   }

   // The layout for this transmission.
   private JFapChannelConstants.TransmissionLayout layout;

   /**
    * Class which represents a set of header fields.  This class provides a framework such
    * that implementors can parse a specific header.  By using an abstract type we can
    * write general transmission buffer manipulation routines which know about buffer
    * operations and can build any header. 
    */
   private abstract class HeaderFields
   {
      private int sizeof;
      protected HeaderFields(int sizeof)
      {
         this.sizeof = sizeof;
      }
      
      /** Returns the size of this header in bytes */
      protected int sizeof()
      {
         return sizeof;
      }
      
      /** 
       * Writes this header into the buffer supplied as an argument.  The
       * buffer is guarenteed to always be large enough. 
       * @param dst
       */
      protected abstract void writeToBuffer(WsByteBuffer dst);
   }

   /** Representation of fields contained within a primary header. */
   private class PrimaryHeaderFields extends HeaderFields
   {
      int segmentLength;
      int priority;
      boolean isPooled;
      boolean isExchange;
      int packetNumber;
      int segmentType; 
      protected PrimaryHeaderFields()
      {
         super(JFapChannelConstants.SIZEOF_PRIMARY_HEADER);
      }
      protected void writeToBuffer(WsByteBuffer dst)
      {
         if (tc.isEntryEnabled()) SibTr.entry(this, tc, "PrimaryHeaderFields.writeToBuffer");
         dst.putShort(JFapChannelConstants.EYECATCHER_AS_SHORT);
         dst.putInt(segmentLength);
         short flags = (short)(priority & 0x0F);
         if (isPooled)
            flags |= 0x1000;
         if (isExchange)
            flags |= 0x4000; 
         dst.putShort(flags);
         dst.put((byte)packetNumber);
         dst.put((byte)segmentType);         
         if (tc.isEntryEnabled()) SibTr.exit(this, tc, "PrimaryHeaderFields.writeToBuffer");
      }
   }
   
   private PrimaryHeaderFields primaryHeaderFields = new PrimaryHeaderFields();

   /**
    * Sets the layout to use for this transmission to be a primary header only.
    * @param segmentLength
    * @param priority
    * @param isPooled
    * @param isExchange
    * @param packetNumber
    * @param segmentType
    * @param sendListener
    */
   protected void setLayoutToPrimary(int segmentLength,
                                     int priority,
                                     boolean isPooled,
                                     boolean isExchange,
                                     int packetNumber,
                                     int segmentType,
                                     SendListener sendListener)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "setLayoutToPrimary", new Object[] {""+segmentLength, ""+priority, ""+isPooled, ""+isExchange, ""+packetNumber, ""+segmentType, sendListener});
      primaryHeaderFields.segmentLength = segmentLength;
      primaryHeaderFields.priority = priority;
      primaryHeaderFields.isPooled = isPooled;
      primaryHeaderFields.isExchange = isExchange;
      primaryHeaderFields.packetNumber = packetNumber;
      primaryHeaderFields.segmentType = segmentType;
      this.sendListener = sendListener;
      transmissionRemaining = segmentLength;
      layout = JFapChannelConstants.XMIT_PRIMARY_ONLY;
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "setLayoutToPrimary");
   }
      
   /** Representation of conversation header fields */
   private class ConversationHeaderFields extends HeaderFields
   {
      private int conversationId;
      private int requestNumber;
      protected ConversationHeaderFields()
      {
         super(JFapChannelConstants.SIZEOF_CONVERSATION_HEADER);
      }
      protected void writeToBuffer(WsByteBuffer dst)
      {
         if (tc.isEntryEnabled()) SibTr.entry(this, tc, "ConversationHeaderFields.writeToBuffer");
         dst.putShort((short)conversationId);
         dst.putShort((short)requestNumber);
         if (tc.isEntryEnabled()) SibTr.exit(this, tc, "ConversationHeaderFields.writeToBuffer");
      }
   }
   private ConversationHeaderFields conversationHeaderFields = new ConversationHeaderFields();   

   /**
    * Set layout for transmission to build to have a conversation header.
    * @param segmentLength
    * @param priority
    * @param isPooled
    * @param isExchange
    * @param packetNumber
    * @param segmentType
    * @param conversationId
    * @param requestNumber
    * @param conversation
    * @param sendListener
    */   
   protected void setLayoutToConversation(int segmentLength,
                                          int priority,
                                          boolean isPooled,
                                          boolean isExchange,
                                          int packetNumber,
                                          int segmentType,
                                          int conversationId,
                                          int requestNumber,
                                          Conversation conversation,
                                          SendListener sendListener)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "setLayoutToConversation", new Object[]{""+segmentLength, ""+priority, ""+isPooled, ""+isExchange, ""+packetNumber, ""+segmentType, ""+conversationId, ""+requestNumber, conversation, sendListener});
      setLayoutToPrimary(segmentLength, priority, isPooled, isExchange, packetNumber, segmentType, sendListener);
      conversationHeaderFields.conversationId = conversationId;
      conversationHeaderFields.requestNumber = requestNumber;
      this.conversation = conversation;
      transmissionRemaining = segmentLength;
      layout = JFapChannelConstants.XMIT_CONVERSATION;
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "setLayoutToConversation");
   }

   /** Representation of a segmented transmission header */
   private class SegmentedTransmissionHeaderFields extends HeaderFields
   {
      private long totalLength;
      int segmentType;
      protected SegmentedTransmissionHeaderFields()
      {
         super(JFapChannelConstants.SIZEOF_SEGMENT_START_HEADER);
      }
      protected void writeToBuffer(WsByteBuffer dst)
      {
         if (tc.isEntryEnabled()) SibTr.entry(this, tc, "SegmentedTransmissionHeaderFields.writeToBuffer");
         dst.putLong(totalLength);
         dst.put((byte)segmentType);
         dst.put((byte)0x00);
         dst.putShort((short)0x00);
         if (tc.isEntryEnabled()) SibTr.exit(this, tc, "SegmentedTransmissionHeaderFields.writeToBuffer");
      }      
   }   
   private SegmentedTransmissionHeaderFields segmentedTransmissionHeaderFields = new SegmentedTransmissionHeaderFields();
   
   /** Set next transmission being built to have a segment start layout */
   protected void setLayoutToStartSegment(int segmentLength,
                                          int priority,
                                          boolean isPooled,
                                          boolean isExchange,
                                          int packetNumber,
                                          int segmentType,
                                          int conversationId,
                                          int requestNumber,
                                          long totalLength)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "setLayoutToStartSegment", new Object[]{""+segmentLength, ""+priority, ""+isPooled, ""+isExchange, ""+packetNumber, ""+segmentType, ""+conversationId, ""+requestNumber, ""+totalLength});
      setLayoutToConversation(segmentLength, priority, isPooled, isExchange, packetNumber, segmentType, conversationId, requestNumber, null, null);
      segmentedTransmissionHeaderFields.totalLength = totalLength;
      segmentedTransmissionHeaderFields.segmentType = segmentType;
      transmissionRemaining = segmentLength;
      primaryHeaderFields.segmentType = JFapChannelConstants.SEGMENT_SEGMENTED_FLOW_START;      
      layout = JFapChannelConstants.XMIT_SEGMENT_START;
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "setLayoutToStartSegment");
   }
   
   /** Set next transmission being built to have a segment middle layout */
   protected void setLayoutToMiddleSegment(int segmentLength,
                                           int priority,
                                           boolean isPooled,
                                           boolean isExchange,
                                           int packetNumber,
                                           int conversationId,
                                           int requestNumber)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "setLayoutToMiddleSegment", new Object[]{""+segmentLength, ""+priority, ""+isPooled, ""+isExchange, ""+packetNumber, ""+conversationId, ""+requestNumber});
      setLayoutToConversation(segmentLength, priority, isPooled, isExchange, packetNumber, JFapChannelConstants.SEGMENT_SEGMENTED_FLOW_MIDDLE, conversationId, requestNumber, null, null);
      layout = JFapChannelConstants.XMIT_SEGMENT_MIDDLE;
      transmissionRemaining = segmentLength;
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "setLayoutToMiddleSegment");
   }
   
   /** Set next transmission being built to have a segment end layout */
   protected void setLayoutToEndSegment(int segmentLength,
                                        int priority,
                                        boolean isPooled,
                                        boolean isExchange,
                                        int packetNumber,
                                        int conversationId,
                                        int requestNumber,
                                        Conversation conversation,
                                        SendListener sendListener)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "setLayoutToSegmentEnd", new Object[]{""+segmentLength, ""+priority, ""+isPooled, ""+isExchange, ""+packetNumber, ""+conversationId, ""+requestNumber, conversation, sendListener});
      setLayoutToConversation(segmentLength, priority, isPooled, isExchange, packetNumber, JFapChannelConstants.SEGMENT_SEGMENTED_FLOW_END, conversationId, requestNumber, conversation, sendListener);
      layout = JFapChannelConstants.XMIT_SEGMENT_END;
      transmissionRemaining = segmentLength;
   }
   
   
   /**
    * Attempts to build a transmission into the specified buffer.  The type of
    * transmission built will depend on the values passed into this object by
    * the reset() method and also the setLayoutToXXX() method.  If there is
    * insufficient space in the supplied buffer, then multiple invocations of
    * this method my be required to incrementally build the transmission.
    * @param xmitBuffer
    * @return A boolean representing whether or not a complete transmission was built.
    */
   boolean buildTransmission(WsByteBuffer xmitBuffer)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "buildTransmission", xmitBuffer);
      SIErrorException error = null;
      
      while(!exhausedXmitBuffer && !transmissionBuilt && (error == null))
      {
         if (tc.isDebugEnabled()) SibTr.debug(this, tc, "state="+state+" layout="+layout);
         switch(state)
         {
            case(STATE_BUILDING_PRIMARY_HEADER):
               if (buildHeader(primaryHeaderFields, xmitBuffer))
               {
                  if (layout == JFapChannelConstants.XMIT_PRIMARY_ONLY)        state = STATE_BUILDING_PAYLOAD;
                  else if (layout == JFapChannelConstants.XMIT_CONVERSATION)   state = STATE_BUILDING_CONVERSATION_HEADER;
                  else if (layout == JFapChannelConstants.XMIT_SEGMENT_START)  state = STATE_BUILDING_CONVERSATION_HEADER;
                  else if (layout == JFapChannelConstants.XMIT_SEGMENT_MIDDLE) state = STATE_BUILDING_CONVERSATION_HEADER;
                  else if (layout == JFapChannelConstants.XMIT_SEGMENT_END)    state = STATE_BUILDING_CONVERSATION_HEADER;
                  else
                  {
                     if (tc.isDebugEnabled()) SibTr.debug(this, tc, "unexpected layout: "+layout+" in state: "+state);
                     state = STATE_ERROR;
                     error = new SIErrorException("unexpected layout: "+layout+" in state: "+state);
                  }
               }
               break;
            case(STATE_BUILDING_CONVERSATION_HEADER):
               if (buildHeader(conversationHeaderFields, xmitBuffer))
               {
                  if (layout == JFapChannelConstants.XMIT_CONVERSATION)        state = STATE_BUILDING_PAYLOAD;
                  else if (layout == JFapChannelConstants.XMIT_SEGMENT_START)  state = STATE_BUILDING_SEGMENT_HEADER;
                  else if (layout == JFapChannelConstants.XMIT_SEGMENT_MIDDLE) state = STATE_BUILDING_PAYLOAD;
                  else if (layout == JFapChannelConstants.XMIT_SEGMENT_END)    state = STATE_BUILDING_PAYLOAD;
                  else
                  {
                     if (tc.isDebugEnabled()) SibTr.debug(this, tc, "unexpected layout: "+layout+" in state: "+state);
                     state = STATE_ERROR;
                     error = new SIErrorException("unexpected layout: "+layout+" in state: "+state);
                  }                  
               }
               break;
            case(STATE_BUILDING_SEGMENT_HEADER):
               if (buildHeader(segmentedTransmissionHeaderFields, xmitBuffer))
               {
                  if (layout == JFapChannelConstants.XMIT_SEGMENT_START)  state = STATE_BUILDING_PAYLOAD;
                  else
                  {
                     if (tc.isDebugEnabled()) SibTr.debug(this, tc, "unexpected layout: "+layout+" in state: "+state);
                     state = STATE_ERROR;
                     error = new SIErrorException("unexpected layout: "+layout+" in state: "+state);
                  }                  
               }
               break;
            case(STATE_BUILDING_PAYLOAD):
               if (buildPayload(xmitBuffer))
               {
                  transmissionBuilt = true;
                  state = STATE_BUILDING_PRIMARY_HEADER;
                  currentXmitDataBufferIndex = 0;
                  headerScratchSpace.clear();
               }
               break;
            case(STATE_ERROR):
               if (error == null) error = new SIErrorException("Entered error state without exception been set");
               connection.invalidate(true, error, "error building transmission");   // D224570
               break;
            default:
         }
      }
      
      boolean retValue = transmissionBuilt;
      if (transmissionBuilt)
         transmissionBuilt = false;
      exhausedXmitBuffer = false;
      
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "buildTransmission", ""+retValue);
      return retValue;
   }

   /**
    * Returns the conversation that this transmission is being build for - or
    * null if this transmission does not have a conversation header.
    * @return Returns the conversation that this transmission is being build for - or
    * null if this transmission does not have a conversation header.
    */
   protected Conversation getConversation()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getConversation");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getConversation", conversation);
      return conversation;
   }
   
   /**
    * Returns true iff this transmission should be received into pooled buffers.
    * @return Returns true iff this transmission should be received into pooled buffers.
    */
   protected boolean isPooledBuffers()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "isPooledBuffers");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "isPooledBuffers", ""+primaryHeaderFields.isPooled);
      return primaryHeaderFields.isPooled;
   }
   
   /**
    * Returns the priority level of this transmission.
    * @return Returns the priority level of this transmission.
    */
   protected int getPriority()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getPriority");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getPriority", ""+primaryHeaderFields.priority);
      return primaryHeaderFields.priority;
   }

   /**
    * Returns the send listener (if any) of this transmission.
    * @return Returns the send listener (if any) of this transmission.
    */     
   protected SendListener getSendListener()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getSendListener");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getSendListener", sendListener);
      return sendListener;
   }

   /**
    * Returns true iff this transmission is a user request.
    * @return Returns true iff this transmission is a user request.
    */
   protected boolean isUserRequest()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "isUserRequest");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "isUserRequest", ""+isUserRequest);
      return isUserRequest;
   }

   /**
    * Returns true if this transmission should stop this connection
    * from writing any more data to the socket.
    * @return Returns true if this transmission should stop this connection
    * from writing any more data to the socket.
    */
   protected boolean isTerminal()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "isTermainl");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "isTermainl", ""+isTerminal);
      return isTerminal;
   }
   
   /**
    * Returns the size of the data associated with this transmission
    * (in bytes)
    * @return Returns the size of the data associated with this transmission
    */
   protected int getSize()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getSize");
      int headerSize = 0;
      if (layout == JFapChannelConstants.XMIT_PRIMARY_ONLY)
         headerSize = JFapChannelConstants.SIZEOF_PRIMARY_HEADER;
      else if (layout == JFapChannelConstants.XMIT_CONVERSATION)
         headerSize = JFapChannelConstants.SIZEOF_PRIMARY_HEADER + JFapChannelConstants.SIZEOF_CONVERSATION_HEADER;
      else if (layout == JFapChannelConstants.XMIT_SEGMENT_START)
         headerSize = JFapChannelConstants.SIZEOF_PRIMARY_HEADER + JFapChannelConstants.SIZEOF_CONVERSATION_HEADER + JFapChannelConstants.SIZEOF_SEGMENT_START_HEADER;
      else if (layout == JFapChannelConstants.XMIT_SEGMENT_MIDDLE)
         headerSize = JFapChannelConstants.SIZEOF_PRIMARY_HEADER + JFapChannelConstants.SIZEOF_CONVERSATION_HEADER;
      else if (layout == JFapChannelConstants.XMIT_SEGMENT_END)
         headerSize = JFapChannelConstants.SIZEOF_PRIMARY_HEADER + JFapChannelConstants.SIZEOF_CONVERSATION_HEADER;
      else
      {
         throw new SIErrorException(TraceNLS.getFormattedMessage(JFapChannelConstants.MSG_BUNDLE, "TRANSDATA_INTERNAL_SICJ0060", null, "TRANSDATA_INTERNAL_SICJ0060")); // D226223 
      }
      int retValue = primaryHeaderFields.segmentLength - headerSize;
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getSize", ""+retValue);
      return retValue;
   }

   /**
    * Builds a header from a description of the header fields.  The header is
    * incrementally built into the supplied transmission buffer.  This may
    * require use of a scratch space in the event that there is insufficient room
    * in the transmission buffer on the first attempt.
    * @param headerFields
    * @param xmitBuffer
    * @return True if the header was completely built.
    */
   private boolean buildHeader(HeaderFields headerFields, WsByteBuffer xmitBuffer)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "buildHeader", new Object[] {headerFields, xmitBuffer});
      if (tc.isDebugEnabled()) JFapUtils.debugTraceWsByteBufferInfo(this, tc, xmitBuffer, "xmitBuffer");
      
      boolean headerFinished = false;
      WsByteBuffer headerBuffer = null;
      
      if (headerScratchSpace.position() == 0)
      {
         if (tc.isDebugEnabled()) JFapUtils.debugTraceWsByteBufferInfo(this, tc, headerScratchSpace, "headerScratchSpace");
         
         // No data in the scratch space buffer - so we must decide if we can
         // build this header in place.
         if (xmitBuffer.remaining() >= headerFields.sizeof())
         {
            // Enough space in the transmission buffer to build the header in
            // place.
            headerBuffer = xmitBuffer;
            headerFields.writeToBuffer(xmitBuffer);
            headerFinished = true;
         }
         else
         {
            // Insufficient room in the transmission buffer to build the header
            // in place.
            headerBuffer = headerScratchSpace;
            headerFields.writeToBuffer(headerScratchSpace);
            headerScratchSpace.flip();
         }
         
         // build header into buffer.
         
      }
      else
      {
         // We have already built a header into the scratch space and are
         // in the process of copying it into the transmission buffer.
         headerBuffer = headerScratchSpace;
      }

      if (!headerFinished)
      {
         // We have not finished on this header yet.  Try copying it into
         // the transmission buffer.
         int headerLeftToCopy = headerBuffer.remaining();
         int amountCopied = JFapUtils.copyWsByteBuffer(headerBuffer, xmitBuffer, headerLeftToCopy);
         headerFinished = amountCopied == headerLeftToCopy;
      }
      
      // If we finished the header - clean out anything we might have put
      // into the scratch space.
      if (headerFinished)
      {
         headerScratchSpace.clear();
         transmissionRemaining -= headerFields.sizeof();
      }
      else
         exhausedXmitBuffer = true;
      
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "buildHeader", ""+headerFinished);
      return headerFinished;
   }

   /**
    * Builds a transmission payload into the supplied buffer.  This may be done
    * incrementally by multiple invocations in the case that the supplied buffer
    * is smaller than the payload being built.
    * @param xmitBuffer
    * @return True if the payload was completely built
    */
   private boolean buildPayload(WsByteBuffer xmitBuffer)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "buildPayload", xmitBuffer);
      if (tc.isDebugEnabled()) JFapUtils.debugTraceWsByteBufferInfo(this, tc, xmitBuffer, "xmitBuffer");
      
      boolean payloadFinished = false;
      int amountCopied, amountToCopy;
      if (xmitDataBuffers.length == 0)
      {
         if (tc.isDebugEnabled()) SibTr.debug(this, tc, "payload finished");
         payloadFinished = true;
      }
      else
      {
         do
         {
            amountToCopy = xmitDataBuffers[currentXmitDataBufferIndex].remaining();
            if (amountToCopy > transmissionRemaining) amountToCopy = transmissionRemaining;
            amountCopied = JFapUtils.copyWsByteBuffer(xmitDataBuffers[currentXmitDataBufferIndex],
                                                      xmitBuffer,
                                                      amountToCopy);
            if (tc.isDebugEnabled()) SibTr.debug(this, tc, "amountToCopy="+amountToCopy+" amountCopied="+amountCopied+" currentXmitDataBufferIndex="+currentXmitDataBufferIndex);                                                      
            transmissionRemaining -= amountCopied;                                                
            if (amountCopied == amountToCopy)
            {
               ++currentXmitDataBufferIndex;
               payloadFinished = (currentXmitDataBufferIndex == xmitDataBuffers.length);
            }
            
            if ((amountCopied < amountToCopy) || (transmissionRemaining < 1))
            {
               exhausedXmitBuffer = true;
            }
         }
         while((amountCopied == amountToCopy) && (!payloadFinished) && (!exhausedXmitBuffer));
      }
      
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "buildPayload", ""+payloadFinished);
      return payloadFinished;
   }
   
   // begin D226242
   protected void release()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "release");
      iterator.release();      
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "release");
   }
   // end D226242
}
