/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.AcceptListener;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.ConversationReceiveListener;
import com.ibm.ws.sib.jfapchannel.Dispatchable;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.ReceiveListener;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBufferPool;
import com.ibm.ws.sib.jfapchannel.impl.rldispatcher.ReceiveListenerDispatcher;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * Parses chunks of data read from a socket into complete inbound transmissions.
 * There is a one to one association between instances of this class and Connection objects.
 * Each chunk of data read from a socket is passed to this class.  There, a state machine
 * attempts to interpret the data, build a complete transmission (in the JFAP sense) and
 * then dispatch the transmission to the appropriate method for processing.
 */
public class InboundTransmissionParser
{
   private static final TraceComponent tc = SibTr.register(InboundTransmissionParser.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

   private static final TraceNLS nls = TraceNLS.getTraceNLS(JFapChannelConstants.MSG_BUNDLE);

   // Connection that this parser is parsing data for
   private Connection connection;

   // If state == STATE_ERROR then this attribute is set to the throwable
   // that relates to the error.
   private Throwable throwable;

   // The accept listener associated with the connection for which this
   // transmission parser is parsing inbound data.
   private AcceptListener acceptListener;

   // Is this transmission parser parsing data on the client side?
   private boolean onClientSide;

   // The packet number we are expecting to be associated with the next
   // transmission
   private byte expectedPacketNumber = 0;

   // The amount of payload data in this transmission.
   private int transmissionPayloadDataLength;

   // The amount of payload data currently remaining.  This is decremented
   // as data is read / parsed.
   private int transmissionPayloadRemaining;

   // Tracks in flight transmissions at a priority level.  When a partial
   // segmented transmission is received it is added to the table at the
   // appropriate index until it can be completed.
   private WsByteBuffer[] inFlightSegmentedTransmissions =
      new WsByteBuffer[JFapChannelConstants.MAX_PRIORITY_LEVELS];

   // Flag used to determine if more data needs to be read to proceed.
   private boolean needMoreData = false;

   // State related attribtes:
   // Current state of state machine
   private int state = STATE_PARSING_PRIMARY_HEADER;
   // State: parsing the primary header
   private final static int STATE_PARSING_PRIMARY_HEADER       = 0;
   // State: parsing a conversation header
   private final static int STATE_PARSING_CONVERSATION_HEADER  = 1;
   // State: parsing a segment start header
   private final static int STATE_PARSING_SEGMENT_START_HEADER = 2;
   // State: parsing a primary conversation only user data payload
   private final static int STATE_PARSING_PRIMARY_ONLY_PAYLOAD = 3;
   // State: parsing a conversation only user data payload
   private final static int STATE_PARSE_CONVERSATION_PAYLOAD   = 4;
   // State: parsing a segment start user data payload
   private final static int STATE_PARSE_SEGMENT_START_PAYLOAD  = 5;
   // State: parsing a segment middle user data payload
   private final static int STATE_PARSE_SEGMENT_MIDDLE_PAYLOAD = 6;
   // State: parsing a segment end user data payload
   private final static int STATE_PARSE_SEGMENT_END_PAYLOAD    = 7;
   // State: parsing error occurred.
   private final static int STATE_ERROR                        = 8;

   // Buffers used as scratch areas when piecing together headers from
   // a read buffer containing only a partial header.
   // Used to piece together primary headers
   private WsByteBuffer unparsedPrimaryHeader;
   // Used to piece together conversation headers
   private WsByteBuffer unparsedConversationHeader;
   // Used to piece together first segment headers
   private WsByteBuffer unparsedFirstSegment;

   // Referenced to buffer used to hold unparsed payload for this
   // transmission.
   private WsByteBuffer unparsedPayloadData = null;

   // Fields contained within the primary header.
   private static class PrimaryHeaderFields
   {
      int segmentLength;
      int priority;
      boolean isPooled;
      boolean isExchange;
      int packetNumber;
      int segmentType;
   };
   private PrimaryHeaderFields primaryHeaderFields = new PrimaryHeaderFields();

   // Fields of conversation header (if present)
   private static class ConversationHeaderFields
   {
      private int conversationId;
      private int requestNumber;
   }
   private ConversationHeaderFields conversationHeaderFields = new ConversationHeaderFields();

   // Fields of transmission start header (if present)
   private static class SegmentedTransmissionHeaderFields
   {
      private long totalLength;
      private int segmentType;                                                      // D191832.1
   }

   // begin D191832.1
   // We keep a segmented transmission header per priority level.  We need to retain the
   // actual segment number (which is part of this header) whilst we build the whole
   // transmission.  This is because we cannot trust the contents of the primaryHeaderFields
   // at the point we receive the last part of a segmented transmission as they will have
   // been overwritten.
   private SegmentedTransmissionHeaderFields[] segmentedTransmissionHeaderFields =
      new SegmentedTransmissionHeaderFields[JFapChannelConstants.MAX_PRIORITY_LEVELS];
   // end D191832.1

   // Layout for transmission segment.
   private JFapChannelConstants.TransmissionLayout transmissionLayout = JFapChannelConstants.XMIT_LAYOUT_UNKNOWN;

   private volatile static int clientReadBytes = 0;                                 // F193735.8
   private volatile static int meReadBytes = 0;                                     // F193735.8

   
   private Conversation.ConversationType type = Conversation.UNKNOWN;               // F193735.3

   /**
    * Constructor.
    * @param connection Connection that parser will parse data for.
    * @param acceptListener Accept listener for connection.
    * @param onClientSide Does this parser reside on the client side?
    */
   public InboundTransmissionParser(Connection connection, AcceptListener acceptListener, boolean onClientSide)
   throws SIResourceException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[] {connection, acceptListener, ""+onClientSide});

      this.connection = connection;
      this.acceptListener = acceptListener;
      this.onClientSide = onClientSide;

      unparsedPrimaryHeader = allocateWsByteBuffer(JFapChannelConstants.SIZEOF_PRIMARY_HEADER, false);
      unparsedPrimaryHeader.position(0);
      unparsedPrimaryHeader.limit(JFapChannelConstants.SIZEOF_PRIMARY_HEADER);

      unparsedConversationHeader  = allocateWsByteBuffer(JFapChannelConstants.SIZEOF_CONVERSATION_HEADER, false);
      unparsedConversationHeader.position(0);
      unparsedConversationHeader.limit(JFapChannelConstants.SIZEOF_CONVERSATION_HEADER);

      unparsedFirstSegment = allocateWsByteBuffer(JFapChannelConstants.SIZEOF_SEGMENT_START_HEADER, false);
      unparsedFirstSegment.position(0);
      unparsedFirstSegment.limit(JFapChannelConstants.SIZEOF_SEGMENT_START_HEADER);

      // begin D191832.1
      for (int i=0; i < segmentedTransmissionHeaderFields.length; ++i)
         segmentedTransmissionHeaderFields[i] = new SegmentedTransmissionHeaderFields();
      // end D191832.1

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }


   /**
    * Parses a chunk of inbound data into discrete transmissions.  The
    * headers for these transmissions are decoded and the payload data
    * is dispatched to the appropriate method for processing.
    * @param transmissionData A chunk of transmission data to be parsed.
    */
   public void parse(WsByteBuffer transmissionData)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "parse", transmissionData);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JFapUtils.debugTraceWsByteBufferInfo(this, tc, transmissionData, "transmissionBuffer");

      needMoreData = false;
      boolean encounteredError = false;

      while(!needMoreData && !encounteredError)
      {
         switch(state)
         {
            case(STATE_PARSING_PRIMARY_HEADER):
               parsePrimaryHeader(transmissionData);
               break;
            case(STATE_PARSING_CONVERSATION_HEADER):
               parseConversationHeader(transmissionData);
               break;
            case(STATE_PARSING_SEGMENT_START_HEADER):
               parseSegmentStartHeader(transmissionData);
               break;
            case(STATE_PARSING_PRIMARY_ONLY_PAYLOAD):
               parsePrimaryOnlyPayload(transmissionData);
               break;
            case(STATE_PARSE_CONVERSATION_PAYLOAD):
               parseConversationPayload(transmissionData);
               break;
            case(STATE_PARSE_SEGMENT_START_PAYLOAD):
               parseSegmentStartPayload(transmissionData);
               break;
            case(STATE_PARSE_SEGMENT_MIDDLE_PAYLOAD):
               parseSegmentMiddlePayload(transmissionData);
               break;
            case(STATE_PARSE_SEGMENT_END_PAYLOAD):
               parseSegmentEndPayload(transmissionData);
               break;
            case(STATE_ERROR):
               encounteredError = true;
               break;
            default:
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "got into default branch of parse() method case statement");
               throwable = new SIErrorException("Should not have entered default branch of parse() method case statement");
               FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_PARSE_01);
               state = STATE_ERROR;
               break;
         }
      }

      if (encounteredError)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "encountered error parsing");
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, throwable);
         connection.invalidate(false, throwable, "parse error while parsing transmission");  // D224570
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "parse");
   }

   /**
    * Invoked to parse a primary header structure from the supplied buffer.
    * May be invoked multiple times to incrementally parse the structure.
    * Once the structure has been fully parsed, transitions the state machine
    * into the appropriate next state based on the layout of the transmission.
    * @param contextBuffer
    */
   private void parsePrimaryHeader(WsByteBuffer contextBuffer)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "parsePrimaryHeader", contextBuffer);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JFapUtils.debugTraceWsByteBufferInfo(this, tc, contextBuffer, "contextBuffer");
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JFapUtils.debugTraceWsByteBufferInfo(this, tc, unparsedPrimaryHeader, "unparsedPrimaryHeader");

      int initialPrimaryHeaderPosition = unparsedPrimaryHeader.position();
      WsByteBuffer parseHeaderBuffer = readData(contextBuffer, unparsedPrimaryHeader);

      if (parseHeaderBuffer != null)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "parse header buffer not null");

         short eyecatcher = parseHeaderBuffer.getShort();
         if (eyecatcher != (short)0xBEEF)
         {
            // bad eyecatcher
            state = STATE_ERROR;
            throwable = new SIConnectionLostException(nls.getFormattedMessage("TRANSPARSER_PROTOCOLERROR_SICJ0053", new Object[] {connection.remoteHostAddress, connection.chainName}, "TRANSPARSER_PROTOCOLERROR_SICJ0053"));   // D226223
            // This FFDC was generated because our peer sent us an invalid eyecatcher.
            FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_PARSEPRIMHDR_01, getFormattedBytes(contextBuffer));  // D267629
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "bad eyecatcer (as short): "+eyecatcher);
         }
         else
         {
            primaryHeaderFields.segmentLength = parseHeaderBuffer.getInt();
            if (primaryHeaderFields.segmentLength < 0) primaryHeaderFields.segmentLength += 4294967296L;

            // Reject lengths greater than our maximum transmission length.
            if (primaryHeaderFields.segmentLength > connection.getMaxTransmissionSize())
            {
               state = STATE_ERROR;
               throwable = new SIConnectionLostException(nls.getFormattedMessage("TRANSPARSER_PROTOCOLERROR_SICJ0053", new Object[] {connection.remoteHostAddress, connection.chainName}, "TRANSPARSER_PROTOCOLERROR_SICJ0053"));   // D226223
               // This FFDC was generated because our peer has exceeded the maximum segment size
               // that was agreed at handshake time.
               FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_PARSEPRIMHDR_02, getFormattedBytes(contextBuffer));  // D267629
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "max transmission size exceeded");
            }
            else
            {
               transmissionPayloadRemaining = primaryHeaderFields.segmentLength - JFapChannelConstants.SIZEOF_PRIMARY_HEADER;
               short flags = parseHeaderBuffer.getShort();
               primaryHeaderFields.priority = flags & 0x000F;
               primaryHeaderFields.isPooled = (flags & 0x1000) == 0x1000;
               primaryHeaderFields.isExchange = (flags & 0x4000) == 0x4000;            // D190023
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "flags: "+flags);
               primaryHeaderFields.packetNumber = parseHeaderBuffer.get();
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "packet number: "+primaryHeaderFields.packetNumber+" expected: "+expectedPacketNumber);

              
               if (primaryHeaderFields.packetNumber != expectedPacketNumber)
               {
                  state = STATE_ERROR;
                  throwable = new SIConnectionLostException(nls.getFormattedMessage("TRANSPARSER_PROTOCOLERROR_SICJ0053", new Object[] {connection.remoteHostAddress, connection.chainName}, "TRANSPARSER_PROTOCOLERROR_SICJ0053"));   // D226223
                  // This FFDC was generated because our peer sent us a transmission containing
                  // a sequence number that did not match the one we expected.
                  final Object[] ffdcData = new Object[] {
                        "expected packet number="+expectedPacketNumber,
                        "received packet number="+primaryHeaderFields.packetNumber,
                        getFormattedBytes(contextBuffer)
                  };
                  FFDCFilter.processException(throwable,
                                              "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser",
                                              JFapChannelConstants.INBOUNDXMITPARSER_PARSEPRIMHDR_03,
                                              ffdcData);
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "sequence number mis-match - expected:"+expectedPacketNumber+" got:"+primaryHeaderFields.packetNumber);
               }
               else
               {
                  ++expectedPacketNumber;
                  primaryHeaderFields.segmentType = parseHeaderBuffer.get();
                  if (primaryHeaderFields.segmentType < 0) primaryHeaderFields.segmentType += 256;

                  transmissionLayout = JFapChannelConstants.segmentToLayout(primaryHeaderFields.segmentType);

                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "layout = "+transmissionLayout);
                  if (transmissionLayout == JFapChannelConstants.XMIT_PRIMARY_ONLY)
                  {
                     transmissionPayloadDataLength = primaryHeaderFields.segmentLength - JFapChannelConstants.SIZEOF_PRIMARY_HEADER;
                     state = STATE_PARSING_PRIMARY_ONLY_PAYLOAD;
                  }
                  else if ( (transmissionLayout == JFapChannelConstants.XMIT_CONVERSATION) ||
                            (transmissionLayout == JFapChannelConstants.XMIT_SEGMENT_START) ||
                            (transmissionLayout == JFapChannelConstants.XMIT_SEGMENT_MIDDLE) ||
                            (transmissionLayout == JFapChannelConstants.XMIT_SEGMENT_END))
                  {
                     state = STATE_PARSING_CONVERSATION_HEADER;
                  }
                  else if (transmissionLayout == JFapChannelConstants.XMIT_LAYOUT_UNKNOWN)
                  {
                     throwable = new SIErrorException(nls.getFormattedMessage("TRANSPARSER_INTERNAL_SICJ0054", null, "TRANSPARSER_INTERNAL_SICJ0054")); // D226223
                     // This FFDC was generated because the segment type of the transmission doesn't match any of
                     // the segment types that we know the layout for.
                     FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_PARSEPRIMHDR_04, getFormattedBytes(contextBuffer));  // D267629
                     state = STATE_ERROR;
                     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "invalid layout");
                  }
                  else
                  {
                     throwable = new SIErrorException(nls.getFormattedMessage("TRANSPARSER_INTERNAL_SICJ0054", null, "TRANSPARSER_INTERNAL_SICJ0054")); // D226223
                     // This FFDC was generated because the JFapChannelConstants.segmentToLayout method
                     // returned a transmission layout we didn't expect.
                     FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_PARSEPRIMHDR_05, getFormattedBytes(contextBuffer));  // D267629
                     state = STATE_ERROR;
                     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "JFapChannelConstants.segmentToLayout method returned unknown enumeration value");
                  }
               }
            }
         }
      }
      else
      {
         // Optimisation for early rejection of bad eyecatcher
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JFapUtils.debugTraceWsByteBufferInfo(this, tc, unparsedPrimaryHeader, "unparsedPrimaryHEader");
         int unparsedPrimaryHeaderPosition = unparsedPrimaryHeader.position();
         if ((initialPrimaryHeaderPosition < JFapChannelConstants.SIZEOF_EYECATCHER) &&
             (unparsedPrimaryHeaderPosition > initialPrimaryHeaderPosition))
         {
            int eyecatcherPresent = unparsedPrimaryHeaderPosition - initialPrimaryHeaderPosition;
            if (eyecatcherPresent > JFapChannelConstants.SIZEOF_EYECATCHER) eyecatcherPresent = JFapChannelConstants.SIZEOF_EYECATCHER;
            int eyecatcherOffset = initialPrimaryHeaderPosition;

            unparsedPrimaryHeader.position(eyecatcherOffset);
            boolean reject = false;
            for (int i=eyecatcherOffset; (i < eyecatcherPresent) && (!reject); ++i)
            {
               reject = unparsedPrimaryHeader.get() != JFapChannelConstants.EYECATCHER_AS_BYTES[i];
            }

            if (reject)
            {
               throwable = new SIConnectionLostException(nls.getFormattedMessage("TRANSPARSER_PROTOCOLERROR_SICJ0053", new Object[] {connection.remoteHostAddress, connection.chainName}, "TRANSPARSER_PROTOCOLERROR_SICJ0053"));   // D226223
               // This FFDC was generated because our peer sent us bad eyecathcer data.
               FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_PARSEPRIMHDR_06, getFormattedBytes(contextBuffer));  // D267629
               state = STATE_ERROR;
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "invalid eyecatcher");
            }
            else
            {
               unparsedPrimaryHeader.position(unparsedPrimaryHeaderPosition);
            }
         }

         if (state != STATE_ERROR)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "need more data");
            needMoreData = true;
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "parsePrimaryHeader");
   }

   /**
    * Invoked to parse a conversation header structure from the supplied buffer.
    * May be invoked multiple times to incrementally parse the structure.
    * Once the structure has been fully parsed, transitions the state machine
    * into the appropriate next state based on the layout of the transmission.
    * @param contextBuffer
    */
   private void parseConversationHeader(WsByteBuffer contextBuffer)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "parseConversationHeader", contextBuffer);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JFapUtils.debugTraceWsByteBufferInfo(this, tc, contextBuffer, "contextBuffer");

      WsByteBuffer parseConversationBuffer = readData(contextBuffer, unparsedConversationHeader);

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JFapUtils.debugTraceWsByteBufferInfo(this, tc, parseConversationBuffer, "parseConversationBuffer");

      if (parseConversationBuffer != null)
      {
         conversationHeaderFields.conversationId = parseConversationBuffer.getShort();
         conversationHeaderFields.requestNumber = parseConversationBuffer.getShort();
         transmissionPayloadRemaining -= JFapChannelConstants.SIZEOF_CONVERSATION_HEADER;

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "conversationId:"+conversationHeaderFields.conversationId+" requestNumber:"+conversationHeaderFields.requestNumber);
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "transmissionLayout:"+transmissionLayout);

         if (transmissionLayout == JFapChannelConstants.XMIT_CONVERSATION)
         {
            transmissionPayloadDataLength = primaryHeaderFields.segmentLength -
               (JFapChannelConstants.SIZEOF_PRIMARY_HEADER + JFapChannelConstants.SIZEOF_CONVERSATION_HEADER);
            state = STATE_PARSE_CONVERSATION_PAYLOAD;
         }
         else if (transmissionLayout == JFapChannelConstants.XMIT_SEGMENT_MIDDLE)
         {
            transmissionPayloadDataLength = primaryHeaderFields.segmentLength -
               (JFapChannelConstants.SIZEOF_PRIMARY_HEADER + JFapChannelConstants.SIZEOF_CONVERSATION_HEADER);
            state = STATE_PARSE_SEGMENT_MIDDLE_PAYLOAD;
         }
         else if (transmissionLayout == JFapChannelConstants.XMIT_SEGMENT_END)
         {
            transmissionPayloadDataLength = primaryHeaderFields.segmentLength -
               (JFapChannelConstants.SIZEOF_PRIMARY_HEADER + JFapChannelConstants.SIZEOF_CONVERSATION_HEADER);

            state = STATE_PARSE_SEGMENT_END_PAYLOAD;
         }
         else if (transmissionLayout == JFapChannelConstants.XMIT_SEGMENT_START)
         {
            state = STATE_PARSING_SEGMENT_START_HEADER;
         }
         else if (transmissionLayout == JFapChannelConstants.XMIT_PRIMARY_ONLY)
         {
            throwable = new SIErrorException(nls.getFormattedMessage("TRANSPARSER_INTERNAL_SICJ0054", null, "TRANSPARSER_INTERNAL_SICJ0054")); // D226223
            // This FFDC was generated because we entered the method to parse conversation
            // headers when the transmission appears to only have a primary header.
            FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_PARSECONVHDR_01, getFormattedBytes(contextBuffer));  // D267629
            state = STATE_ERROR;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invalid state detected - Entered parseConversationHeader method when transmission only contained a primary header");
         }
         else if (transmissionLayout == JFapChannelConstants.XMIT_LAYOUT_UNKNOWN)
         {
            throwable = new SIErrorException(nls.getFormattedMessage("TRANSPARSER_INTERNAL_SICJ0054", null, "TRANSPARSER_INTERNAL_SICJ0054")); // D226223
            // This FFDC was generated because we encountered a segment type for which
            // we were unable to determine the layout of the segment.
            FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_PARSECONVHDR_02, getFormattedBytes(contextBuffer));  // D267629
            state = STATE_ERROR;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invalid state detected - Entered parseConversationHeader method when transmission layout could not be determined");
         }
         else
         {
            throwable = new SIErrorException(nls.getFormattedMessage("TRANSPARSER_INTERNAL_SICJ0054", null, "TRANSPARSER_INTERNAL_SICJ0054")); // D226223
            // This FFDC was generated because the method used to determine transmission
            // layout returned a value that we were not expecting.
            FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_PARSECONVHDR_03, getFormattedBytes(contextBuffer));  // D267629
            state = STATE_ERROR;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Unknown transmission layout detected in parseConversationHeader");
         }
      }
      else
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "need more data");
         needMoreData = true;
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "parseConversationHeader");
   }

   /**
    * Invoked to parse a start segment header structure from the supplied buffer.
    * May be invoked multiple times to incrementally parse the structure.
    * Once the structure has been fully parsed, transitions the state machine
    * into the appropriate next state based on the layout of the transmission.
    * @param contextBuffer
    */
   private void parseSegmentStartHeader(WsByteBuffer contextBuffer)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "parseSegmentStartHeader", contextBuffer);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JFapUtils.debugTraceWsByteBufferInfo(this, tc, contextBuffer, "contextBuffer");

      WsByteBuffer rawData = readData(contextBuffer, unparsedFirstSegment);

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JFapUtils.debugTraceWsByteBufferInfo(this, tc, rawData, "rawData");
      if (rawData != null)
      {
         segmentedTransmissionHeaderFields[primaryHeaderFields.priority].totalLength = rawData.getLong();                                                // D191832.1
         transmissionPayloadRemaining -= JFapChannelConstants.SIZEOF_SEGMENT_START_HEADER;
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "totalLenght: "+segmentedTransmissionHeaderFields[primaryHeaderFields.priority].totalLength);    // D191832.1
         if (segmentedTransmissionHeaderFields[primaryHeaderFields.priority].totalLength < 0)                                                            // D191832.1
         {
            throwable = new SIConnectionLostException(nls.getFormattedMessage("TRANSPARSER_PROTOCOLERROR_SICJ0053", new Object[] {connection.remoteHostAddress, connection.chainName}, "TRANSPARSER_PROTOCOLERROR_SICJ0053"));   // D226223
            // This FFDC was generated because our peer sent a the start of a segmented transmission
            // with a segment size greater than the size suggested by the transmission size field.
            FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_PARSESSHDR_01, getFormattedBytes(contextBuffer)); // D267629
            state = STATE_ERROR;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Conversation lost after peer transmitted more data than initially indicated in first part of segmented transmission");
         }
         else
         {
            // begin D191832.1
            segmentedTransmissionHeaderFields[primaryHeaderFields.priority].segmentType = rawData.get();
            if (segmentedTransmissionHeaderFields[primaryHeaderFields.priority].segmentType < 0)
               segmentedTransmissionHeaderFields[primaryHeaderFields.priority].segmentType += 256;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "segmentType: "+segmentedTransmissionHeaderFields[primaryHeaderFields.priority].segmentType);
            // end D191832.1
            rawData.position(rawData.position()+3); // skip padding.
            transmissionPayloadDataLength = primaryHeaderFields.segmentLength -
               (JFapChannelConstants.SIZEOF_PRIMARY_HEADER +
                JFapChannelConstants.SIZEOF_CONVERSATION_HEADER +
                JFapChannelConstants.SIZEOF_SEGMENT_START_HEADER);
            state = STATE_PARSE_SEGMENT_START_PAYLOAD;
         }
      }
      else
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "need more data");
         needMoreData = true;
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "parseSegmentStartHeader");
   }

   /**
    * Invoked to parse a primary header payload structure from the supplied buffer.
    * May be invoked multiple times to incrementally parse the structure.
    * Once the structure has been fully parsed, transitions the state machine
    * into the appropriate next state based on the layout of the transmission.
    * @param contextBuffer
    */
   private void parsePrimaryOnlyPayload(WsByteBuffer contextBuffer)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "parsePrimaryOnlyPayload", contextBuffer);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JFapUtils.debugTraceWsByteBufferInfo(this, tc, contextBuffer, "contextBuffer");

      WsByteBuffer dispatchToConnectionData = null;
      if ((unparsedPayloadData == null) &&
          (transmissionPayloadDataLength > contextBuffer.remaining()))
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "allocating unparsed payload data area, size="+transmissionPayloadDataLength);

         unparsedPayloadData = allocateWsByteBuffer(transmissionPayloadDataLength, false);
         unparsedPayloadData.position(0);
         unparsedPayloadData.limit(transmissionPayloadDataLength);
      }

      if (state != STATE_ERROR)
      {
         if (unparsedPayloadData != null)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JFapUtils.debugTraceWsByteBufferInfo(this, tc, unparsedPayloadData, "unparsedPayloadData");
            dispatchToConnectionData = readData(contextBuffer, unparsedPayloadData);
            if (dispatchToConnectionData != null)
            {
               final boolean closed = dispatchToConnection(dispatchToConnectionData);

               if(closed)
               {
                  //If this dispatch resulted in the connection closing we shouldn't expect any more data so break out of loop.
                  needMoreData = true;
                  if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Connection closed, breaking out of loop");
               }

               reset();
            }
            else
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "need more data");
               needMoreData = true;
            }
         }
         else
         {
            int contextBufferPosition = contextBuffer.position();
            int contextBufferLimit = contextBuffer.limit();
            contextBuffer.limit(contextBufferPosition+transmissionPayloadDataLength);
            final boolean closed = dispatchToConnection(contextBuffer);

            if(closed)
            {
               //If this dispatch resulted in the connection closing we shouldn't expect any more data so break out of loop.
               needMoreData = true;
               if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Connection closed, breaking out of loop");
            }
            else
            {
               contextBuffer.limit(contextBufferLimit);
               contextBuffer.position(contextBufferPosition+transmissionPayloadDataLength);
            }

            reset();
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "parsePrimaryOnlyPayload");
   }

   /**
    * Invoked to parse a conversation payload structure from the supplied buffer.
    * May be invoked multiple times to incrementally parse the structure.
    * Once the structure has been fully parsed, transitions the state machine
    * into the appropriate next state based on the layout of the transmission.
    * @param contextBuffer
    */
   private void parseConversationPayload(WsByteBuffer contextBuffer)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "parseConversationPayload", contextBuffer);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JFapUtils.debugTraceWsByteBufferInfo(this, tc, contextBuffer, "contextBuffer");

      if (unparsedPayloadData == null)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "allocating unparsed data buffer, size="+transmissionPayloadDataLength);

         unparsedPayloadData = allocateWsByteBuffer(transmissionPayloadDataLength, primaryHeaderFields.isPooled);
         unparsedPayloadData.position(0);
         unparsedPayloadData.limit(transmissionPayloadDataLength);
      }

      if (state != STATE_ERROR)
      {
         int unparsedDataRemaining = unparsedPayloadData.remaining();
         int amountCopied =
            JFapUtils.copyWsByteBuffer(contextBuffer, unparsedPayloadData, unparsedDataRemaining);

         if (amountCopied == unparsedDataRemaining)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "dispatching to conversation - amount cpoied = "+amountCopied);
            dispatchToConversation(unparsedPayloadData);
            if (state != STATE_ERROR)
               reset();
         }
         else
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "need more data");
            needMoreData = true;
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "parseConversationPayload");
   }

   /**
    * Invoked to parse a segmented transmission start payload from the supplied buffer.
    * May be invoked multiple times to incrementally parse the structure.
    * Once the structure has been fully parsed, transitions the state machine
    * into the appropriate next state based on the layout of the transmission.
    * @param contextBuffer
    */
   private void parseSegmentStartPayload(WsByteBuffer contextBuffer)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "parseSegmentStartPayload", contextBuffer);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JFapUtils.debugTraceWsByteBufferInfo(this, tc, contextBuffer, "contextBuffer");
      if (unparsedPayloadData == null)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "allocating unparsed payload data buffe, size="+segmentedTransmissionHeaderFields[primaryHeaderFields.priority].totalLength);  // D191832.1

         unparsedPayloadData =
            allocateWsByteBuffer((int)segmentedTransmissionHeaderFields[primaryHeaderFields.priority].totalLength, primaryHeaderFields.isPooled);                                      // D191832.1
         unparsedPayloadData.position(0);
         unparsedPayloadData.limit((int)segmentedTransmissionHeaderFields[primaryHeaderFields.priority].totalLength);                                                                  // D191832.1
      }

      if (state != STATE_ERROR)
      {
         int amountCopied =
            JFapUtils.copyWsByteBuffer(contextBuffer, unparsedPayloadData, transmissionPayloadRemaining);
         transmissionPayloadRemaining -= amountCopied;

         if (inFlightSegmentedTransmissions[primaryHeaderFields.priority] != null)
         {
            throwable = new SIConnectionLostException(nls.getFormattedMessage("TRANSPARSER_PROTOCOLERROR_SICJ0053", new Object[] {connection.remoteHostAddress, connection.chainName}, "TRANSPARSER_PROTOCOLERROR_SICJ0053"));   // D226223
            // This FFDC was generated because our peer sent us a duplicate start of a segmented
            // transmission.  I.e. we already had a partial segmented transmission built for a
            // given priority level when our peer sent us the start of another.
            FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_PARSESSPAYLOAD_01, getFormattedBytes(contextBuffer));   // D267629
            state = STATE_ERROR;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Received the start of a segmented transmission whilst already processing a segmented transmission at the same priority level");
         }
         else
         {
            needMoreData = (contextBuffer.remaining() == 0);
            if (!needMoreData)
            {
               inFlightSegmentedTransmissions[primaryHeaderFields.priority] = unparsedPayloadData;
               // begin F193735.3
               if (type == Conversation.ME)
                  meReadBytes += unparsedPayloadData.remaining();
               else if (type == Conversation.CLIENT)
                  clientReadBytes -= unparsedPayloadData.remaining();
               // end F193735.3
               reset();
            }
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "parseSegmentStartPayload");
   }

   /**
    * Invoked to parse a segmented transmission middle payload from the supplied buffer.
    * May be invoked multiple times to incrementally parse the structure.
    * Once the structure has been fully parsed, transitions the state machine
    * into the appropriate next state based on the layout of the transmission.
    * @param contextBuffer
    */
   private void parseSegmentMiddlePayload(WsByteBuffer contextBuffer)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "parseSegmentMiddlePayload", contextBuffer);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JFapUtils.debugTraceWsByteBufferInfo(this, tc, contextBuffer, "contextBuffer");
      WsByteBuffer partialTransmission = inFlightSegmentedTransmissions[primaryHeaderFields.priority];
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "partial transmission in slot "+primaryHeaderFields.priority+" = "+partialTransmission);
      int contextBufferRemaining = contextBuffer.remaining();
      if (partialTransmission == null)
      {
         throwable = new SIConnectionLostException(nls.getFormattedMessage("TRANSPARSER_PROTOCOLERROR_SICJ0053", new Object[] {connection.remoteHostAddress, connection.chainName}, "TRANSPARSER_PROTOCOLERROR_SICJ0053"));   // D226223
         // This FFDC was generated because our peer sent the middle segment of a segmented
         // transmission without first sending the first segment.
         FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_PARSESMPAYLOAD_01, getFormattedBytes(contextBuffer));   // D267629
         state = STATE_ERROR;
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Received the middle segment of a segmented transmission prior to receiving a start segment.");
      }
      else if (partialTransmission.remaining() < transmissionPayloadRemaining)
      {
         throwable = new SIConnectionLostException(nls.getFormattedMessage("TRANSPARSER_PROTOCOLERROR_SICJ0053", new Object[] {connection.remoteHostAddress, connection.chainName}, "TRANSPARSER_PROTOCOLERROR_SICJ0053"));   // D226223
         // This FFDC was generated because our peer sent a middle segment of a segmented
         // transmission that would have made the overall transmitted data length greater
         // than that suggested in the first segment.
         FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_PARSESMPAYLOAD_02, getFormattedBytes(contextBuffer)); // D267629
         state = STATE_ERROR;
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Received a middle segment for a segmented transmission which makes the transmission larger than the peer indicated in the first segment.");
      }
      else
      {
         int amountCopied = JFapUtils.copyWsByteBuffer(contextBuffer, partialTransmission, transmissionPayloadRemaining);
         transmissionPayloadRemaining -= amountCopied;
         // begin F193735.3
         if (type == Conversation.ME)
            meReadBytes -= amountCopied;
         else if (type == Conversation.CLIENT)
            clientReadBytes -= amountCopied;
         // end F193735.3
         needMoreData = (amountCopied == contextBufferRemaining);
         if (!needMoreData)
            reset();
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "parseSegmentMiddlePayload");
   }

   /**
    * Invoked to parse a segmented transmission end payload from the supplied buffer.
    * May be invoked multiple times to incrementally parse the structure.
    * Once the structure has been fully parsed, transitions the state machine
    * into the appropriate next state based on the layout of the transmission.
    * @param contextBuffer
    */
   private void parseSegmentEndPayload(WsByteBuffer contextBuffer)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "parseSegmentEndPayload", contextBuffer);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JFapUtils.debugTraceWsByteBufferInfo(this, tc, contextBuffer, "contextBuffer");
      WsByteBuffer partialTransmission = inFlightSegmentedTransmissions[primaryHeaderFields.priority];
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "partial transmission in slot "+primaryHeaderFields.priority+" = "+partialTransmission);

      if (partialTransmission == null)
      {
         throwable = new SIConnectionLostException(nls.getFormattedMessage("TRANSPARSER_PROTOCOLERROR_SICJ0053", new Object[] {connection.remoteHostAddress, connection.chainName}, "TRANSPARSER_PROTOCOLERROR_SICJ0053"));   // D226223
         // This FFDC was generated because our peer sent us the end of a segmented transmission
         // when we have not received a start segment for the transmission.
         FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_PARSESEPAYLOAD_01, getFormattedBytes(contextBuffer));   // D267629
         state = STATE_ERROR;
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Peer sent a segment end transmission prior to sending a segment start transmission for the segmented transmission.");
      }
      else
      {
         int partialTransmissionRemaining = partialTransmission.remaining();

         if (partialTransmissionRemaining > transmissionPayloadRemaining)
         {
            throwable = new SIConnectionLostException(nls.getFormattedMessage("TRANSPARSER_PROTOCOLERROR_SICJ0053", new Object[] {connection.remoteHostAddress, connection.chainName}, "TRANSPARSER_PROTOCOLERROR_SICJ0053"));   // D226223
            // This FFDC was generated because our peer sent us a the end of a segmented transmission
            // whos total size would be larger than that suggested in the first segment of the
            // segmented transmission.
            FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_PARSESEPAYLOAD_02, getFormattedBytes(contextBuffer));   // D267629
            state = STATE_ERROR;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Peer sent the end segment of a segmented transmission which contains more data than origioinally suggested by first segment in segmented transmission.");
         }
         else
         {
            int amountCopied =
               JFapUtils.copyWsByteBuffer(contextBuffer, partialTransmission, transmissionPayloadRemaining);
            transmissionPayloadRemaining -= amountCopied;

            // begin F193735.3
            if (type == Conversation.ME)
               meReadBytes -= amountCopied;
            else if (type == Conversation.CLIENT)
               clientReadBytes -= amountCopied;
            // end F193735.3

            needMoreData = (contextBuffer.remaining() == 0);

            if (transmissionPayloadRemaining == 0)
            {
               inFlightSegmentedTransmissions[primaryHeaderFields.priority] = null;
               primaryHeaderFields.segmentType = segmentedTransmissionHeaderFields[primaryHeaderFields.priority].segmentType;
               dispatchToConversation(partialTransmission);
               reset();
            }
         }
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "parseSegmentEndPayload");
   }

   /**
    * Dispatches a payload to the appropriate connection for processing.  The
    * implication of dispatching to a connection is that the payload did not
    * have a conversation header on its transmission.
    *
    * @param data
    * @returns true if this data resulted in the connection being closed and hence releasing its buffers
    */
   private boolean dispatchToConnection(WsByteBuffer data)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dispatchToConnection", data);
      //@stoptracescan@
      if (TraceComponent.isAnyTracingEnabled()) JFapUtils.debugSummaryMessage(tc, connection, null, "received connection data with segment "+Integer.toHexString(primaryHeaderFields.segmentType)+" ("+JFapChannelConstants.getSegmentName(primaryHeaderFields.segmentType)+")");
      //@starttracescan@
      final boolean closed = connection.processData(primaryHeaderFields.segmentType, primaryHeaderFields.priority, primaryHeaderFields.isPooled, primaryHeaderFields.isExchange, data);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dispatchToConnection", Boolean.valueOf(closed));
      return closed;
   }

   /**
    * Dispatches a transmission payload to the appropriate conversation
    * method.  The implication of dispatching to a conversation is that
    * the transmission had a conversation header.
    * @param data
    */
   private void dispatchToConversation(WsByteBuffer data)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dispatchToConversation", data);
      ConversationImpl conversation =
         connection.getConversationById(conversationHeaderFields.conversationId);

      //@stoptracescan@
      if (TraceComponent.isAnyTracingEnabled()) JFapUtils.debugSummaryMessage(tc, connection, conversation, "received conversation data with segment "+Integer.toHexString(primaryHeaderFields.segmentType)+" ("+JFapChannelConstants.getSegmentName(primaryHeaderFields.segmentType)+")", conversationHeaderFields.requestNumber);
      //@starttracescan@
      if (conversation != null) conversation.logDataReceivedEvent(primaryHeaderFields.segmentType,
                                                                  conversationHeaderFields.requestNumber);

      switch(primaryHeaderFields.segmentType)
      {
         case(JFapChannelConstants.SEGMENT_LOGICAL_CLOSE):
            if (conversation == null)
            {
               state = STATE_ERROR;
               throwable = new SIConnectionLostException(nls.getFormattedMessage("TRANSPARSER_PROTOCOLERROR_SICJ0053", new Object[] {connection.remoteHostAddress, connection.chainName}, "TRANSPARSER_PROTOCOLERROR_SICJ0053"));   // D226223
               // This FFDC was generated because we could not locate a conversation corresponding
               // to the conversation ID supplied by our peer.
               FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_DISPCONV_01, getFormattedBytes(data));   // D267629
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Cannot locate conversation corresponding to conversation identifier in transmission ("+conversationHeaderFields.conversationId+")");
            }
            else
               conversation.processLogicalClose(data);
            break;
         case(JFapChannelConstants.SEGMENT_PING):
            if (conversation == null)
            {
               state = STATE_ERROR;
               throwable = new SIConnectionLostException(nls.getFormattedMessage("TRANSPARSER_PROTOCOLERROR_SICJ0053", new Object[] {connection.remoteHostAddress, connection.chainName}, "TRANSPARSER_PROTOCOLERROR_SICJ0053"));   // D226223
               // This FFDC was generated because we could not locate a conversation corresponding
               // to the conversation ID supplied by our peer.
               FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_DISPCONV_02, getFormattedBytes(data));   // D267629
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Cannot locate conversation corresponding to conversation identifier in transmission ("+conversationHeaderFields.conversationId+")");
            }
            else
               conversation.processPing(conversationHeaderFields.requestNumber, primaryHeaderFields.priority, data);
            break;
         case(JFapChannelConstants.SEGMENT_PING_RESPONSE):
            if (conversation == null)
            {
               state = STATE_ERROR;
               throwable = new SIConnectionLostException(nls.getFormattedMessage("TRANSPARSER_PROTOCOLERROR_SICJ0053", new Object[] {connection.remoteHostAddress, connection.chainName}, "TRANSPARSER_PROTOCOLERROR_SICJ0053"));   // D226223
               // This FFDC was generated because we could not locate a conversation corresponding
               // to the conversation ID supplied by our peer.
               FFDCFilter.processException(throwable, "com.ibm.ws.sib.jfapchannel.impl.InboundTransmissionParser", JFapChannelConstants.INBOUNDXMITPARSER_DISPCONV_03, getFormattedBytes(data));   // D267629
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Cannot locate conversation corresponding to conversation identifier in transmission ("+conversationHeaderFields.conversationId+")");
            }
            else
               conversation.processPingResponse(data);
            break;
         default:
            dispatchToConversationListenerMethod(data, conversation);
            break;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dispatchToConversation");
   }

   /**
    * Dispatches a transmissions payload to a conversation listerner method.
    * @param data
    */
   private void dispatchToConversationListenerMethod(WsByteBuffer data, ConversationImpl conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dispatchToConversationListenerMethod", new Object[]{data, conversation});
      boolean bailOut = false;

      if (conversation == null)
      {
         // Must be an attempt to start a new conversation.
         if (onClientSide)
         {
            // This is an error!
            bailOut = true;
         }
         else
         {
            // On the server side we assume any previously unseend
            // conversation id means that the client is stricking up a
            // conversation with us.
            conversation = new ConversationImpl((short)conversationHeaderFields.conversationId, false, connection, null);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "accepting new conversation");

            ConversationReceiveListener rl = null;
            boolean badReceiveListener = false;
            try
            {
               rl = acceptListener.acceptConnection(conversation);
            }
            catch(Throwable t)
            {
               FFDCFilter.processException
                  (t, "com.ibm.ws.sib.jfapchannel.impl.ConnectionReadCompletedCallback", JFapChannelConstants.INBOUNDXMITPARSER_DISPCONVLST_01);
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception thrown from acceptConnection");
               if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, t.getMessage());

               // Oh dear, user managed to throw an exception in their callback.
               badReceiveListener = true;
               bailOut = true;
            }

            // If the user did not supply us with a receive listener (either through
            // incompetence or throwning an exception) make our own.  We need one before
            // we can finish establishing the conversation and the cleanest way to dump the
            // conversation is to finish establishing it then close it.
            if (rl == null)
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "bad receive listener");
               badReceiveListener = true;
               bailOut = true;
               rl = new BadReceiveListener();
            }

            conversation.setDefaultReceiveListener(rl);
            try
            {
               conversation = connection.startNewConversation(conversation);

               if (badReceiveListener)
               {
                  try
                  {
                     conversation.close();
                  }
                  catch (SIResourceException e)
                  {
                     FFDCFilter.processException
                        (e, "com.ibm.ws.sib.jfapchannel.impl.ConnectionReadCompletedCallback", JFapChannelConstants.INBOUNDXMITPARSER_DISPCONVLST_02);
                     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception thrown closing conversation");
                     if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, e);

                     // Hmmn, something went wrong trying to close the logical conversation.
                     // Bring down the whole conneciton.
                     connection.invalidate(false, e, "SIResourceError when closing conversation - "+e.getMessage()); // D224570
                     bailOut = true;
                  }
               }
            }
            catch(SIResourceException e)
            {
               // No FFDC code needed
               // (FFDC taken at source of exception)
               try
               {
                  conversation.close();
               }
               catch(Exception e2)
               {
                  // No FFDC code needed
                  // (already broken)
               }

               connection.invalidate(true, e, "SIResourceError when creating conversation - "+e.getMessage());
            }
         }
      }

      if (!bailOut)
      {

         RequestIdTable reqIdTable = null;
         if (conversation != null)
            reqIdTable = conversation.getRequestIdTable();

         // Is this request something we were expecting a reply for?
         if ((reqIdTable != null) && reqIdTable.containsId(conversationHeaderFields.requestNumber))
         {
            ReceiveListener listener = reqIdTable.getListener(conversationHeaderFields.requestNumber);
            reqIdTable.remove(conversationHeaderFields.requestNumber);

            if (listener != null)
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "listener: "+listener+" conversaion: "+conversation);

               ReceiveListenerDispatcher.getInstance(conversation.getConversationType(), conversation.isOnClientSide()).        // D242116
                queueDataReceivedInvocation
                  (connection, listener, data, primaryHeaderFields.segmentType,
                     conversationHeaderFields.requestNumber, primaryHeaderFields.priority, primaryHeaderFields.isPooled,
                     primaryHeaderFields.isExchange, conversation);
            }
            else
            {
               // It should be impossible to get to here and discover that
               // the associated entry in the request table has a null
               // receive listener...
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Entry "+conversationHeaderFields.requestNumber+" in request table has null listener");

               // Probably better stop talking to our peer - just to be on the safe side.
               connection.invalidate(true, null, "no receive listener for conversation "+conversationHeaderFields.requestNumber); // F176003, D224570
            }

         }
         else
         {
            // We weren't expecting this reply - notify the 'default' request
            // listener associated with this conversation...
            ConversationReceiveListener listener =                                 // F174776
               conversation.getDefaultReceiveListener();
            //if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "invokeCallback", "default receive listener: "+listener);
            if (listener == null)
            {
               // Earlier error checking should prevent us ever getting into
               // this condition.
               //if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "No default receive listener");

               // Since we can't really continue, probably best to stop talking
               // to our peer now.
               connection.invalidate(true, null, "no default receive listener");    // F176003, D224570
            }
            else
            {
               ReceiveListenerDispatcher.getInstance(conversation.getConversationType(), conversation.isOnClientSide()).        // D242116
                queueDataReceivedInvocation
                  (connection, listener, data, primaryHeaderFields.segmentType,
                  conversationHeaderFields.requestNumber, primaryHeaderFields.priority, primaryHeaderFields.isPooled,
                  primaryHeaderFields.isExchange, conversation);
            }
         }

      }//if(!bailOut)

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dispatchToConversationListenerMethod");
   }

   /**
    * Resets the state of the parsing state machine so that it is read to
    * parse another transmission.
    */
   private void reset()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "reset");
      throwable = null;

      state = STATE_PARSING_PRIMARY_HEADER;

      unparsedPrimaryHeader.position(0);
      unparsedPrimaryHeader.limit(JFapChannelConstants.SIZEOF_PRIMARY_HEADER);

      unparsedConversationHeader.position(0);
      unparsedConversationHeader.limit(JFapChannelConstants.SIZEOF_CONVERSATION_HEADER);

      unparsedFirstSegment.position(0);
      unparsedFirstSegment.limit(JFapChannelConstants.SIZEOF_SEGMENT_START_HEADER);

      unparsedPayloadData = null;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "reset");
   }

   /**
    * Reads data from a buffer into a scratch area.
    * @param unparsedData
    * @param scratchArea
    * @return
    */
   private WsByteBuffer readData(WsByteBuffer unparsedData,
                                  WsByteBuffer scratchArea)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readData", new Object[] {unparsedData, scratchArea});
      int scratchAreaRemaining = scratchArea.remaining();
      int scratchAreaUsed = scratchArea.position();
      WsByteBuffer retBuffer = null;

      if ((scratchAreaUsed == 0) &&
          (unparsedData.remaining() >= scratchAreaRemaining))
      {
         retBuffer = unparsedData;
      }
      else
      {
         int amountCopied = JFapUtils.copyWsByteBuffer(unparsedData,
                                                       scratchArea,
                                                       scratchAreaRemaining);
         if (amountCopied >= scratchAreaRemaining)
         {
            retBuffer = scratchArea;
            retBuffer.flip();
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readData", retBuffer);
      return retBuffer;
   }


   private WsByteBuffer allocateWsByteBuffer(int size, boolean fromPool)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "allocateWsByteBuffer", new Object[] {""+size, ""+fromPool});

      WsByteBuffer retBuffer = null;

      if (fromPool)
      {
         retBuffer = WsByteBufferPool.getInstance().allocate(size);       // F196678.10
      }
      else
      {
         byte[] wrapArray = new byte[size];
         retBuffer = WsByteBufferPool.getInstance().wrap(wrapArray);      // F196678.10
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "allocateWsByteBuffer", retBuffer);
      return retBuffer;
   }

   private static class BadReceiveListener implements ConversationReceiveListener
   {
      public ConversationReceiveListener dataReceived(WsByteBuffer data,
                                                      int segmentType,
                                                      int requestNumber,
                                                      int priority,
                                                      boolean allocatedFromBufferPool,
                                                      boolean partOfExchange,
                                                      Conversation conversation)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"BadReceiveListener.dataReceived");
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc,"BadReceiveListener.dataReceived");
         return null;
      }

      public void errorOccurred(SIConnectionLostException exception, int segmentType, int requestNumber, int priority, Conversation conversation)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"BadReceiveListener.errorOccurred");
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"BadReceiveListener.errorOccurred");
      }

      // Start F201521
      public Dispatchable getThreadContext(Conversation conversation, WsByteBuffer data, int segmentType)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "BadReceiveListener.getThreadContext");
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "BadReceiveListener.getThreadContext");
         return null;
      }
      // End F201521
   }

   // begin F193735.3
   protected void setType(Conversation.ConversationType type)
   {
      this.type = type;
   }
   // end F193735.3

   // Start D267629
   /**
    * NOTE: This method is only used to dump the bytes on error in FFDC's. The method will also dump
    * out the _entire_ buffer regardless of it's current position
    *
    * @return Returns the bytes in the byte buffer as a formatted String
    */
   private String getFormattedBytes(WsByteBuffer buff)
   {
          // Note we do not really want to trace the buffer since we are about to dump the bytes in the buffer in an FFDC anyway.
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getFormattedBytes", null);

          int currentPosition = buff.position();
      int currentLimit = buff.limit();
      buff.rewind();

      try
      {
         // If the buffer is backed by an array, just get the bytes straight from the buffer
         if (buff.hasArray())
         {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getFormattedBytes", null);
                return SibTr.formatBytes(buff.array(), buff.arrayOffset() + buff.position(), buff.remaining());
         }

         // Otherwise we need to make a copy of it
         byte[] dataArray = new byte[buff.remaining()];
         buff.get(dataArray);
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getFormattedBytes", null);
         return SibTr.formatBytes(dataArray, 0, dataArray.length);
      }
      finally
      {
         buff.position(currentPosition);
         buff.limit(currentLimit);
      }

   }
   // End D267629
}
