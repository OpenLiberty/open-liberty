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

package com.ibm.ws.sib.mfp.impl;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.comms.CommsConnection;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.mfp.MessageEncodeFailedException;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.ProducerType;
import com.ibm.ws.sib.mfp.jmf.JMFEncapsulation;
import com.ibm.ws.sib.mfp.jmf.JMFMessage;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFNativePart;
import com.ibm.ws.sib.mfp.jmf.JMFRegistry;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.mfp.jmf.JMFException;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.impl.JSRegistry;
import com.ibm.ws.sib.mfp.jmf.tools.JSFormatter;
import com.ibm.ws.sib.mfp.schema.JsApiAccess;
import com.ibm.ws.sib.mfp.schema.JsHdrAccess;
import com.ibm.ws.sib.mfp.schema.JsPayloadAccess;
import com.ibm.ws.sib.mfp.util.ArrayUtil;
import com.ibm.ws.sib.mfp.util.HexUtil;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;

/**
 *  This class represents a Jetstream Message Object and is the key MFP
 *  message artefact. It is never accessed directly by other components - all
 *  components must operate on it via one of the containing JsXxxxMessage
 *  implementations.
 */
class JsMsgObject {
  private static TraceComponent tc = SibTr.register(JsMsgObject.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  // The length of id information (version & schemas) needed before an encoded message.
  // (2 x 16bit JMF Version, 2 x 64bit schema ids).
  private final static int IDS_LENGTH = ArrayUtil.SHORT_SIZE
                                      + ArrayUtil.LONG_SIZE
                                      + ArrayUtil.SHORT_SIZE
                                      + ArrayUtil.LONG_SIZE;

  // The length of a standard prologue which precedes an encoded message.
  // (Ids + header part length)
  private final static int PROLOGUE_LENGTH = IDS_LENGTH + ArrayUtil.INT_SIZE;

  // The offset of the header part if in a single buffer
  protected MessageImpl theMessage;            // The MFP message that contains this JMO
  protected int originalLength;                // Original message size (if known)

  private JsMsgPart headerPart;                // There is always a 'header' message part.
  private JsMsgPart payloadPart = null;        // There may not be a payload message part.


  /**
   *  Constructor for a new message consisting of a single JMF part.
   *  (To be called by the TRM First Contact Message constructor....)
   *
   *  @param schema       The access schema for the outermost message fields
   *
   *  @exception MessageDecodeFailedException will be thrown if the message can not be initialized.
   */
  JsMsgObject(JMFSchema schema) throws MessageDecodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JsMsgObject", schema);
    initialize(null
              ,0
              ,schema
              ,null
              ,0
              ,0
              ,null
              ,null
              ,0
              ,0
              ,null);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JsMsgObject");
  }


  /**
   *  Constructor for a new message consisting of two JMF parts - a header & a payload.
   *  (To be called, by normal message constructors....)
   *
   *  @param headerSchema  The outermost access schema for the header message part
   *  @param payloadSchema The outermost access schema for the payload message part
   *
   *  @exception MessageDecodeFailedException will be thrown if the message can not be initialized.
   */
  JsMsgObject(JMFSchema headerSchema, JMFSchema payloadSchema) throws MessageDecodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JsMsgObject", new Object[]{headerSchema, payloadSchema});
    initialize(null
              ,0
              ,headerSchema
              ,null
              ,0
              ,0
              ,payloadSchema
              ,null
              ,0
              ,0
              ,null);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JsMsgObject");
  }


  /**
   *  Constructor for a single part inbound message.
   *  (To be called, indirectly, by Communications for a TRM FCM message)
   *
   *  @param schema       The access schema for the outermost message fields
   *  @param rawMessage  The inbound byte array containging a complete message
   *  @param offset      The offset in the byte array at which the message begins
   *  @param length      The length of the message within the byte array
   *
   *  @exception MessageDecodeFailedException will be thrown if the message can not be initialized.
   */
  JsMsgObject(JMFSchema schema, byte[] rawMessage, int offset, int length)
      throws MessageDecodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JsMsgObject", new Object[]{schema, rawMessage, offset, length});
    initialize(null
              ,0
              ,schema
              ,rawMessage
              ,offset
              ,length
              ,null
              ,null
              ,0
              ,0
              ,null);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JsMsgObject");
  }


  /**
   *  Constructor for a two part inbound message.
   *  (To be called, indirectly, by Communications)
   *
   *  @param headerSchema  The outermost access schema for the header message part
   *  @param payloadSchema The outermost access schema for the payload message part
   *  @param rawMessage  The inbound byte array containing a complete message
   *  @param offset      The offset in the byte array at which the message begins
   *  @param length      The length of the message within the byte array
   *
   *  @exception MessageDecodeFailedException will be thrown if the message can not be initialized.
   */
  JsMsgObject(JMFSchema headerSchema, JMFSchema payloadSchema, byte[] rawMessage, int offset, int length)
      throws MessageDecodeFailedException {
    this(headerSchema, payloadSchema, rawMessage, offset, length, null);
  }

  /**
   *  Constructor for a two part inbound message.
   *  (To be called, indirectly, by Communications)
   *
   *  @param headerSchema  The outermost access schema for the header message part
   *  @param payloadSchema The outermost access schema for the payload message part
   *  @param rawMessage  The inbound byte array containging a complete message
   *  @param offset      The offset in the byte array at which the message begins
   *  @param length      The length of the message within the byte array
   *  @param conn        The CommsConnection, if any, which is associated with this message
   *
   *  @exception MessageDecodeFailedException will be thrown if the message can not be initialized.
   */
  JsMsgObject(JMFSchema headerSchema,
              JMFSchema payloadSchema,
              byte[] rawMessage,
              int offset,
              int length,
              CommsConnection conn)
      throws MessageDecodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JsMsgObject", new Object[]{headerSchema, payloadSchema, rawMessage, offset, length, conn});

    /* Sanity check the supplied parameter values to check consistency */
    if (rawMessage.length-offset < length || length < IDS_LENGTH + ArrayUtil.INT_SIZE) {
      String msg = "Invalid message buffer (buffer size="+rawMessage.length+" offset="+offset+" length="+length+"). ";

      if (rawMessage.length-offset < length) {
        msg += "Size-offset too small for requested length.";
      }
      else {
        msg += "Length is less than preamble";
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, msg);
      throw new MessageDecodeFailedException(msg);
    }

    /* Find the size of the header section (which includes the ids and a length field */
    int hdrLength = IDS_LENGTH
                  + ArrayUtil.INT_SIZE
                  + ArrayUtil.readInt(rawMessage, offset + IDS_LENGTH)
                  ;

    /* If it is less than the total length, we have a two part message - i.e. */
    /* a normal one with a separate payload */
    if (hdrLength < length ) {
      initialize(null
                ,0
                ,headerSchema
                ,rawMessage
                ,offset
                ,hdrLength
                ,payloadSchema
                ,rawMessage
                ,offset + hdrLength
                ,length - hdrLength
                ,conn);
    }

    /* Otherwise, we must have a one part message (i.e. a TRM FCM) or a problem */
    else {
      if (payloadSchema == null) {
        initialize(null
                  ,0
                  ,headerSchema
                  ,rawMessage
                  ,offset
                  ,hdrLength
                  ,null
                  ,null
                  ,0
                  ,0
                  ,conn);
      }
      else {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "decode failed: encoded payload missing");
        MessageDecodeFailedException e = new MessageDecodeFailedException();
        FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.<init>", "jmo350", this,
          new Object[] { MfpConstants.DM_BUFFER, rawMessage, Integer.valueOf(offset), Integer.valueOf(length) });
        throw e;
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JsMsgObject");
  }


  /**
   *  Constructor for a two part inbound message delivered is a List of DataSlices.
   *  (To be called by JsMessageFactoryImpl.createInboundJsMessage only.)
   *
   *  @param headerSchema  The outermost access schema for the header message part
   *  @param payloadSchema The outermost access schema for the payload message part
   *  @param slices        The List of DataSlices containing a complete message
   *  @param conn          The CommsConnection, if any, which is associated with this message
   *
   *  @exception MessageDecodeFailedException will be thrown if the message can not be initialized.
   */
  JsMsgObject(JMFSchema headerSchema,
              JMFSchema payloadSchema,
              List<DataSlice> slices,
              CommsConnection conn)
      throws MessageDecodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JsMsgObject", new Object[]{headerSchema, payloadSchema, slices, conn});

    // The slices are those returned from a previous flatten, so we know the list
    // contains the following:
    //   slice 0 : Message class name & other information OR a single slice representing the whole buffer
    //   slice 1 : Header  JSMessage
    //   slice 2 : Payload JSMessage
    // ... so we need to ensure we have at least 2 slices
    if ((slices == null) || (slices.size() < 1)) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Exception: DataSlice list was null or empty");
      MessageDecodeFailedException e = new MessageDecodeFailedException("A null or empty DataSlice List was passed to the message decode");
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.<init>", "jmo360", this);
      throw e;
    }
    else if (slices.size() < 3) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Exception: Too few DataSlices for inbound message");
      MessageDecodeFailedException e = new MessageDecodeFailedException("Too few DataSlices for inbound message");
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.initialize", "jmo365", this, new Object[] { MfpConstants.DM_SLICES, slices });
      throw e;
    }

    // Otherwise, we have 3 or more slices & we're really in the brave new world, so carry on....
    else {
      DataSlice prologueSlice = slices.get(0);
      DataSlice headerSlice   = slices.get(1);
      DataSlice payloadSlice  = slices.get(2);

      initialize(prologueSlice.getBytes()
                ,prologueSlice.getOffset()
                ,headerSchema
                ,headerSlice.getBytes()
                ,headerSlice.getOffset()
                ,headerSlice.getLength()
                ,payloadSchema
                ,payloadSlice.getBytes()
                ,payloadSlice.getOffset()
                ,payloadSlice.getLength()
                ,conn);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JsMsgObject");
  }


  /**
   *  Constructor for restoring a flattened or serialized message.
   *  (To be called by the JsMessageFactory.restoreJsMessage() and MessageImpl.readObject() methods.)
   *
   *  @param headerSchema  The outermost access schema for the header message part
   *  @param payloadSchema The outermost access schema for the payload message part
   *  @param buffers      The List of DataSlices or byte arrays containing a complete message
   *
   *  @exception MessageDecodeFailedException will be thrown if the message can not be initialized.
   */
  JsMsgObject(JMFSchema headerSchema, JMFSchema payloadSchema, List<?> buffers)
      throws MessageDecodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JsMsgObject", new Object[]{headerSchema, payloadSchema, buffers});

    // The List may contain DataSlices for a message to restore
    if (buffers.get(0) instanceof DataSlice) {

      DataSlice prologueSlice = (DataSlice)buffers.get(0);
      DataSlice headerSlice   = (DataSlice)buffers.get(1);

      if (buffers.size() > 2) {
        DataSlice payloadSlice = (DataSlice)buffers.get(2);

        initialize(prologueSlice.getBytes()
                  ,prologueSlice.getOffset()
                  ,headerSchema
                  ,headerSlice.getBytes()
                  ,headerSlice.getOffset()
                  ,headerSlice.getLength()
                  ,payloadSchema
                  ,payloadSlice.getBytes()
                  ,payloadSlice.getOffset()
                  ,payloadSlice.getLength()
                  ,null);
      }
      else {
        initialize(prologueSlice.getBytes()
                  ,prologueSlice.getOffset()
                  ,headerSchema
                  ,headerSlice.getBytes()
                  ,headerSlice.getOffset()
                  ,headerSlice.getLength()
                  ,null
                  ,null
                  ,0
                  ,0
                  ,null);
      }
    }

    // Otherwise we must be deserializing so we just have 2 buffers
    else {

      byte[] rawHeader = (byte[])buffers.get(0);

      if (buffers.size() > 1) {
        byte[] rawPayload = (byte[])buffers.get(1);
        initialize(null
                  ,0
                  ,headerSchema
                  ,rawHeader
                  ,0
                  ,rawHeader.length
                  ,payloadSchema
                  ,rawPayload
                  ,0
                  ,rawPayload.length
                  ,null);
      }

      else {
        initialize(null
                  ,0
                  ,headerSchema
                  ,rawHeader
                  ,0
                  ,rawHeader.length
                  ,null
                  ,null
                  ,0
                  ,0
                  ,null);
      }
   }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JsMsgObject");
  }


  /*
   *  Method which does the real work for constructing an inbound message.
   *  Called only by constructors.
   *
   *  @param prologue    The byte array containing the information preceding the start of the header message part
   *  @param prologueOffset The offset in the prologue byte array where the information actually starts
   *  @param headerSchema  The outermost access schema for the header message part
   *  @param rawHeader   The inbound byte array containing the header message part
   *  @param hdrOffset   The offset in the byte array at which the header message part begins
   *  @param hdrLength   The length of the header message part within the byte array
   *  @param payloadSchema The outermost access schema for the payload message part
   *  @param rawPayload  The inbound byte array containing the payload message part
   *  @param payOffset   The offset in the byte array at which the payload message part begins
   *  @param payLength   The length of the payload message part within the byte array
   *  @param conn        The CommsConnection, if any, which is associated with this message
   *
   *  @exception MessageDecodeFailedException will be thrown if the message can not be initialized.
   */
  private void initialize(byte[] prologue
                         ,int prologueOffset
                         ,JMFSchema headerSchema
                         ,byte rawHeader[]
                         ,int hdrOffset
                         ,int hdrLength
                         ,JMFSchema payloadSchema
                         ,byte rawPayload[]
                         ,int payOffset
                         ,int payLength
                         ,CommsConnection conn)
      throws MessageDecodeFailedException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "initialize", new Object[]
                                                                          {prologue
                                                                          ,prologueOffset
                                                                          ,headerSchema
                                                                          ,rawHeader
                                                                          ,hdrOffset
                                                                          ,hdrLength
                                                                          ,payloadSchema
                                                                          ,rawPayload
                                                                          ,payOffset
                                                                          ,payLength
                                                                          ,conn
                                                                          });

    // Construct the initial JMF message.  If  raw buffer(s) were provided we decode the message
    // parts from the buffer(s).........
    if (rawHeader != null) {

      boolean separatePrologue = ( prologue!=null ? true : false);

      // If debug trace, dump the buffers first
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Decoding message from byte buffer(s)");
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
        String outP = null;
        if (separatePrologue) {
          outP = SibTr.formatBytes(prologue, prologueOffset, PROLOGUE_LENGTH);
        }
        String out0 = SibTr.formatBytes(rawHeader, hdrOffset, hdrLength);
        String out1 = null;
        if (rawPayload != null) {
          out1 = SibTr.formatBytes(rawPayload, payOffset, payLength, MfpDiagnostics.getDiagnosticDataLimitInt());
        }
        SibTr.debug(this, tc, "buffers: ", new Object[] { outP, out0, out1 });
      }

      // Sanity check the length of the header section
      if (rawHeader.length-hdrOffset < hdrLength || hdrLength < IDS_LENGTH + ArrayUtil.INT_SIZE) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Message buffer size too small: "+rawHeader.length);
        throw new MessageDecodeFailedException("Invalid message buffer (buffer size too small)");
      }

      // If there is a separate prologue, the JMFVersions, schemaIds & real header length will be there
      byte[] pBuffer;
      int pOffset;
      if (separatePrologue) {
        pBuffer = prologue;
        pOffset = prologueOffset;
      }
      // otherwise, the stuff will be at the front of the the header pBuffer
      else {
        pBuffer = rawHeader;
        pOffset = hdrOffset;
      }

      // Read in the Prologue information from the appropriate pBuffer
      short jmfVer1 = ArrayUtil.readShort(pBuffer, pOffset);
      pOffset += ArrayUtil.SHORT_SIZE;
      long encId1 = ArrayUtil.readLong(pBuffer, pOffset);
      pOffset += ArrayUtil.LONG_SIZE;
      short jmfVer2 = ArrayUtil.readShort(pBuffer, pOffset);
      pOffset += ArrayUtil.SHORT_SIZE;
      long encId2 = ArrayUtil.readLong(pBuffer, pOffset);
      pOffset += ArrayUtil.LONG_SIZE;
      int realHdrLength = ArrayUtil.readInt(pBuffer, pOffset);
      pOffset += ArrayUtil.INT_SIZE;

      // If the prologue was in the header buffer, update the offset now we have read it.
      if (!separatePrologue) {
        hdrOffset = pOffset;
      }

      // Save the original encoded message length
      originalLength = PROLOGUE_LENGTH + realHdrLength + payLength;

      // Decode the header
      headerPart = new JsMsgPart(initializePart(headerSchema, jmfVer1, encId1, rawHeader, hdrOffset, realHdrLength, conn));

      try {
        // Decode the payload if present
        if (payLength > 0) {
          payloadPart = new JsMsgPart(initializePart(payloadSchema, jmfVer2, encId2, rawPayload, payOffset, payLength, conn));
        }
      }
      catch (MessageDecodeFailedException e) {
        // No FFDC code needed
        // Just write out the decoded header to any debug trace and rethrow the Exception.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Decoded JMF Message Header", debugHeaderPart());
        throw e;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Decoded JMF Message", debugMsg());
    }

    // .... If no buffers, we create new empty message parts.
    else if (headerSchema != null) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Creating new empty header message: Schema="+debugSchema(headerSchema));
      headerPart = new JsMsgPart(JMFRegistry.instance.newMessage(headerSchema));
      if (payloadSchema != null) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Creating new empty payload: Schema="+debugSchema(payloadSchema));
        payloadPart = new JsMsgPart(JMFRegistry.instance.newMessage(payloadSchema));
      }
    }

    /* It is legitimate for both the schema and the buffer to be null. The  */
    /* JMO is not initialized, which is correct for use by getCopy().       */

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "initialize");
  }

  /*
   *  Method which does the real work for constructing an inbound JMF message part.
   *  Called only by initialize.
   *
   *  @param schema      The access schema for the outermost message
   *  @param jmfVer      The JMF encoding version
   *  @param encId       The encoding schema id for the outermost message of the part
   *  @param rawMessage  The inbound byte array containing a complete message part
   *  @param offset      The offset in the byte array at which the message part begins
   *  @param length      The length of the message part within the byte array
   *  @param conn        The connection associated with this message, if any
   *
   *  @return The JMFNativePart decoded from the buffer
   *
   *  @exception MessageDecodeFailedException will be thrown if the message part can not be initialized.
   */
  private JMFNativePart initializePart(JMFSchema schema,
                                       short jmfVer,
                                       long encId,
                                       byte rawMessage[],
                                       int offset,
                                       int length,
                                       CommsConnection conn)
      throws MessageDecodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "initializePart", new Object[]
                                                                             {jmfVer
                                                                             ,debugId(encId)
                                                                             ,rawMessage
                                                                             ,offset
                                                                             ,length
                                                                             ,conn
                                                                             });

    JMFNativePart jmfPart = null;

    // Construct a JMF message part by decoding it from the buffers.
    try {

      //If no access schema was provided we use the encoding schema
      if (schema == null) {
        schema = JMFRegistry.instance.retrieve(encId);
      }

      //if a connection is available then we should check to make sure all schemas
      //are available now rather than wait until later
      if(conn != null){

        long[] unknownSchemata = JMFRegistry.instance.checkSchemata(jmfVer, rawMessage, offset);
        byte[] schemataRequest = null;

        //convert the schema ids to a byte[]
        int requestOffset = 0;

        //if we don't know the encoding schema then put that in the request also
        if(schema == null){
          schemataRequest = new byte[(unknownSchemata.length + 1)*8];
          ArrayUtil.writeLong(schemataRequest, requestOffset, encId);
          requestOffset += 8;
        }
        else if(unknownSchemata.length > 0){
          schemataRequest = new byte[(unknownSchemata.length)*8];
        }
        //add in all the unknown schemata
        for(int i=0; i<unknownSchemata.length; i++){
          ArrayUtil.writeLong(schemataRequest, requestOffset, unknownSchemata[i]);
          requestOffset += 8;
        }
        if(schemataRequest != null){
          //request the schemata via the connection
          byte[] newSchemata = null;
          try {
            newSchemata = conn.requestMFPSchemata(schemataRequest);
          }
          catch (SIConnectionDroppedException e) {
            FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.initializePart", "jmo400", this,
              new Object[] { MfpConstants.DM_BUFFER, rawMessage, Integer.valueOf(offset), Integer.valueOf(length) });
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "decode failed: " + e);
            throw new MessageDecodeFailedException(e);
          } catch (SIConnectionLostException e) {
            FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.initializePart", "jmo402", this,
              new Object[] { MfpConstants.DM_BUFFER, rawMessage, Integer.valueOf(offset), Integer.valueOf(length) });
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "decode failed: " + e);
            throw new MessageDecodeFailedException(e);
          } catch (SIConnectionUnavailableException e) {
            FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.initializePart", "jmo403", this,
              new Object[] { MfpConstants.DM_BUFFER, rawMessage, Integer.valueOf(offset), Integer.valueOf(length) });
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "decode failed: " + e);
            throw new MessageDecodeFailedException(e);
          } catch (SIErrorException e) {
            FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.initialize", "jmo404", this,
              new Object[] { MfpConstants.DM_BUFFER, rawMessage, Integer.valueOf(offset), Integer.valueOf(length) });
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "decode failed: " + e);
            throw new MessageDecodeFailedException(e);
          }
          if(newSchemata != null){
            //tell the SchemaManager about the new schemata
            SchemaManager.receiveSchemas(conn, newSchemata);
          }

          //make sure that we can get the encoding schema
          if(schema == null){
            schema = JMFRegistry.instance.retrieve(encId);
          }
        }
        if (schema == null) {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Unable to retrieve message schema "+debugId(encId));
          MessageDecodeFailedException e = new MessageDecodeFailedException("No schema registered for schema id "+debugId(encId));
          FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.initializePart", "jmo425", this,
            new Object[] { MfpConstants.DM_BUFFER, rawMessage, Integer.valueOf(offset), Integer.valueOf(length) });
          throw e;
        }
      }

      //we should now have all the schemas we need (if the connection was available)
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Decoding: JMF version="+jmfVer+" encoding schema="+debugId(encId)+" access schema="+debugId(schema.getID()));
      jmfPart = JMFRegistry.instance.decode(schema, jmfVer, encId, rawMessage, offset, length);

    }
    catch (JMFException e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.initializePart", "jmo450", this,
        new Object[] { MfpConstants.DM_BUFFER, rawMessage, Integer.valueOf(offset), Integer.valueOf(length) });
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "decode failed: " + e);
      throw new MessageDecodeFailedException(e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "initializePart");
    return jmfPart;
  }


  /**
   * Set the back-pointer to the MFP message that contains this JMO instance.
   *
   * @param msg The MessageImpl of this JMO's container
   */
  void setMessage(MessageImpl msg) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setMessage", msg);
    theMessage = msg;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setMessage");
  }


  /**
   *  Encode the message into a single DataSlice.
   *  The DataSlice will be used by the Comms component to transmit the message
   *  over the wire.
   *  This method may only be used for a single-part message.
   *
   *  @param conn - the CommsConnection over which this encoded message will be sent.
   *                This may be null if the message is not really being encoded for transmission.
   *
   *  @return The DataSlice which contains the encoded message
   *
   *  @exception MessageEncodeFailedException is thrown if the message failed to encode.
   */
  DataSlice encodeSinglePartMessage(Object conn) throws MessageEncodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "encodeSinglePartMessage");

    // We must only use this method if the message does not have a payload part
    if (payloadPart != null) {
      MessageEncodeFailedException mefe = new MessageEncodeFailedException("Invalid call to encodeSinglePartMessage");
      FFDCFilter.processException(mefe, "com.ibm.ws.sib.mfp.impl.JsMsgObject.encodeSinglePartMessage", "jmo530", this,
          new Object[] { MfpConstants.DM_MESSAGE, headerPart.jmfPart, theMessage });
      throw mefe;
    }

    // The 'conn' parameter (if supplied) is passed as an Object (for no good reason except the
    // daft build system), but it has to be a CommsConnection instance.
    if (conn != null && !(conn instanceof CommsConnection)) {
      throw new MessageEncodeFailedException("Incorrect connection object: " + conn.getClass());
    }

    byte[] buffer = null;

    // Encoding is handled by JMF.  This will be a very cheap operation if JMF already has the
    // message in assembled form (for example if it was previously decoded from a byte buffer and
    // has had no major changes).
    try {

      // We need to lock the message around the call to updateDataFields() and the
      // actual encode of the part(s), so that noone can update any JMF message data
      // during that time (because they can not get the hdr2, api, or payload part).
      // Otherwise it is possible to get an inconsistent view of the message with some updates
      // included but those to the cached values 'missing'.
      // It is still strictly possible for the top-level schema header fields to be
      // updated, but this will not happen to any fields visible to an app.
      synchronized(theMessage) {

        // Ensure any cached message data is written back
        theMessage.updateDataFields(MfpConstants.UDF_ENCODE);

        // We need to check if the receiver has all the necessary schema definitions to be able
        // to decode this message and pre-send any that are missing.
        ensureReceiverHasSchemata((CommsConnection)conn);

        // Synchronize this section on the JMF Message, as otherwise the length could
        // change between allocating the array & encoding the header part into it.
        synchronized (getPartLockArtefact(headerPart)) {

          // Allocate a buffer for the Ids and the message part
          buffer = new byte[ IDS_LENGTH
                           + ArrayUtil.INT_SIZE
                           + ((JMFMessage)headerPart.jmfPart).getEncodedLength()
                           ];

          // Write the Ids to the buffer
          int offset = encodeIds(buffer, 0);

          // Write the header part to the buffer & add it to the buffer list
          encodePartToBuffer(headerPart, true, buffer, offset);
        }
      }

    }
    catch (MessageEncodeFailedException e1) {
      // This will have been thrown by encodePartToBuffer() which will
      // already have dumped the appropriate message part.
      FFDCFilter.processException(e1, "com.ibm.ws.sib.mfp.impl.JsMsgObject.encodeSinglePartMessage", "jmo500", this,
        new Object[] { MfpConstants.DM_MESSAGE, null, theMessage });
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "encodeSinglePartMessage failed: " + e1);
      throw e1;
    }
    catch (Exception e) {
      // This is most likely to be thrown by the getEncodedLength() call on the header
      // so we pass the header part to the diagnostic module.
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.encodeSinglePartMessage", "jmo520", this,
        new Object[] { MfpConstants.DM_MESSAGE, headerPart.jmfPart, theMessage });
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "encodeSinglePartMessage failed: " + e);
      throw new MessageEncodeFailedException(e);
    }

      // If debug trace, dump the buffer before returning
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Encoded JMF Message", debugMsg());
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
      SibTr.debug(this, tc, "buffers: ", SibTr.formatBytes(buffer, 0, buffer.length));
    }

    // Wrap the buffer in a DataSlice to return
    DataSlice slice = new DataSlice(buffer);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "encodeSinglePartMessage", slice);
    return slice;
  }

  /**
   *  Encode the message into a List of DataSlices.
   *  The DataSlices will be used by the Comms component to transmit the message
   *  over the wire.
   *  This method has been substantially reworked. d348294
   *
   *  @param conn - the CommsConnection over which this encoded message will be sent.
   *                This may be null if the message is not really being encoded for transmission.
   *
   *  @return The List of DataSlices which comprise the encoded message
   *
   *  @exception MessageEncodeFailedException is thrown if the message failed to encode.
   */
  List<DataSlice> encodeFast(Object conn) throws MessageEncodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "encodeFast");

    // The 'conn' parameter (if supplied) is passed as an Object (for no good reason except the
    // daft build system), but it has to be a CommsConnection instance.
    if (conn != null && !(conn instanceof CommsConnection)) {
      throw new MessageEncodeFailedException("Incorrect connection object: " + conn.getClass());
    }

    // Initial capacity is sufficient for a 2 part message
    List<DataSlice> messageSlices = new ArrayList<DataSlice>(3);

    DataSlice slice0  = null;
    DataSlice slice1  = null;
    DataSlice slice2  = null;

    // Encoding is handled by JMF.  This will be a very cheap operation if JMF already has the
    // message in assembled form (for example if it was previously decoded from a byte buffer and
    // has had no major changes).
    try {

      // We need to check if the receiver has all the necessary schema definitions to be able
      // to decode this message and pre-send any that are missing.
      ensureReceiverHasSchemata((CommsConnection)conn);

      // Write the top-level Schema Ids & their versions to the top buffer
      byte[] buff0 = new byte[ IDS_LENGTH + ArrayUtil.INT_SIZE];
      int offset = 0;
      offset += encodeIds(buff0, 0);
      slice0  = new DataSlice(buff0, 0, buff0.length);

      // We need to lock the message around the call to updateDataFields() and the
      // actual encode of the part(s), so that noone can update any JMF message data
      // during that time (because they can not get the hdr2, api, or payload part).
      // Otherwise it is possible to get an inconsistent view of the message with some updates
      // included but those to the cached values 'missing'.
      // It is still strictly possible for the top-level schema header fields to be
      // updated, but this will not happen to any fields visible to an app.
      synchronized(theMessage) {

        // Next ensure any cached message data is written back before we get the
        // low level (JSMessageImpl) locks                                  d364050
        theMessage.updateDataFields(MfpConstants.UDF_ENCODE);

        // Write the second slice - this contains the header
        slice1 = encodeHeaderPartToSlice(headerPart);

        // Now we have to tell the buffer in the slice0 how long the header part is
        ArrayUtil.writeInt(buff0, offset, slice1.getLength());

        // Now the first 2 slices are complete we can put them in the list.
        // Slices must be added in the correct order.
        messageSlices.add(slice0);
        messageSlices.add(slice1);

        // Now for the 3rd slice, which will contain the payload (if there is one)
        if (payloadPart != null) {
          slice2 = encodePayloadPartToSlice(payloadPart, (CommsConnection)conn);
          messageSlices.add(slice2);
        }
      }

    }
    catch (MessageEncodeFailedException e1) {
      // This will have been thrown by encodeXxxxxPartToSlice() which will
      // already have dumped the appropriate message part.
      FFDCFilter.processException(e1, "com.ibm.ws.sib.mfp.impl.JsMsgObject.encodeFast", "jmo560", this,
        new Object[] { MfpConstants.DM_MESSAGE, null, theMessage });
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "encodeFast failed: " + e1);
      throw e1;
    }
    catch (Exception e) {
      // This is most likely to be thrown by the getEncodedLength() call on the header
      // so we pass the header part to the diagnostic module.
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.encodeFast", "jmo570", this,
        new Object[] { MfpConstants.DM_MESSAGE, headerPart.jmfPart, theMessage });
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "encodeFast failed: " + e);
      throw new MessageEncodeFailedException(e);
    }


    // If debug trace, dump the message & slices before returning
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Encoded JMF Message", debugMsg());
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Message DataSlices: ", SibTr.formatSlices(messageSlices, MfpDiagnostics.getDiagnosticDataLimitInt()));

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "encodeFast");

    return messageSlices;
  }


  /**
   *  Encode the message into a List of DataSlices.
   *  The DataSlices will be used by the Message Store to persist the message
   *  into a database.
   *  Reworked for LI SIB0112b to return DataSlices rather than a single byte array.
   *
   *  @param store     The message store that will be used for storing this message
   *  @param nameBytes The name of the implementation class of the owning message
   *
   *  @return The List of DataSlices which comprise the flattened message
   *
   *  @exception MessageEncodeFailedException is thrown if the message failed to encode.
   */
  List<DataSlice> flatten(Object store, byte[] nameBytes) throws MessageEncodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "flatten", new Object[] {store, nameBytes});

    // Initial capacity is sufficient for a 2 part message
    List<DataSlice> messageSlices = new ArrayList<DataSlice>(3);

    /* If we have a message store we need to ensure it contains all the       */
    /* schema defintions needed to be able decode this message sometime later */
    long[] encodeIds;
    if (store != null) {
      if (!(store instanceof MessageStore)) {
        throw new IllegalArgumentException("store is not a MessageStore instance");
      }
      JMFSchema[] encodeSchemas = getEncodingSchemas();
      SchemaStore.saveSchemas((MessageStore)store, encodeSchemas);
      encodeIds = new long[encodeSchemas.length];
      for (int i = 0; i < encodeSchemas.length; i++) {
        encodeIds[i] = encodeSchemas[i].getID();
      }
    }
    else {
      encodeIds = new long[0];
    }

    // Write the first slice - this just contains name & ids information
    int bufferLength = 0;
    bufferLength += ArrayUtil.INT_SIZE;
    bufferLength += nameBytes.length;
    bufferLength += ArrayUtil.INT_SIZE;
    bufferLength += encodeIds.length * ArrayUtil.LONG_SIZE;
    bufferLength += IDS_LENGTH;                             // for the top-level versions & schema ids
    bufferLength += ArrayUtil.INT_SIZE;                     // for the length of the header part
    byte[] buff0 = new byte[bufferLength];

    int offset = 0;
    ArrayUtil.writeInt(buff0, offset, nameBytes.length);   // the length of the class name
    offset += ArrayUtil.INT_SIZE;
    ArrayUtil.writeBytes(buff0, offset, nameBytes);        // the class name
    offset += nameBytes.length;
    ArrayUtil.writeInt(buff0, offset, encodeIds.length);   // the number of encoding schema ids
    offset += ArrayUtil.INT_SIZE;
    for (int i = 0; i < encodeIds.length; i++) {
      ArrayUtil.writeLong(buff0, offset, encodeIds[i]);    // each encoding schema id
      offset += ArrayUtil.LONG_SIZE;
    }

    // Write the top-level Schema Ids & their versions to the buffer & wrap it in a DataSlice
    offset += encodeIds(buff0, offset);
    DataSlice slice0  = new DataSlice(buff0, 0, bufferLength);

    try {
      // We need to lock the message around the call to updateDataFields() and the
      // actual encode of the part(s), so that noone can update any JMF message data
      // during that time (because they can not get the hdr2, api, or payload part).
      // Otherwise it is possible to get an inconsistent view of the message with some updates
      // included but those to the cached values 'missing'.
      // It is still strictly possible for the top-level schema header fields to be
      // updated, but this will not happen to any fields visible to an app.
      synchronized(theMessage) {

        // Next ensure any cached message data is written back before we get the
        // low level (JSMessageImpl) locks                                  d364050
        theMessage.updateDataFields(MfpConstants.UDF_FLATTEN);

        DataSlice slice1  = null;
        DataSlice slice2  = null;

        // Write the second slice - this contains the header
        slice1 = encodeHeaderPartToSlice(headerPart);

        // Now we have to tell the buffer in the slice0 how long the header part is
        ArrayUtil.writeInt(buff0, offset, slice1.getLength());

        // Now the first 2 slices are complete we can put them in the list.
        // Slices must be added in the correct order.
        messageSlices.add(slice0);
        messageSlices.add(slice1);

        // Now for the 3rd slice, which will contain the payload (if there is one)
        if (payloadPart != null) {
          slice2 = encodePayloadPartToSlice(payloadPart, null);
          messageSlices.add(slice2);
        }
      }

    }
    catch (MessageEncodeFailedException e1) {
      // This will have been thrown by encodeXxxxxPartToSlice() which will
      // already have dumped the appropriate message part.
      FFDCFilter.processException(e1, "com.ibm.ws.sib.mfp.impl.JsMsgObject.flatten", "jmo580", this,
        new Object[] { MfpConstants.DM_MESSAGE, null, theMessage });
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "flatten failed: " + e1);
      throw e1;
    }
    catch (Exception e) {
      // Not sure this can ever happen, but if it does dump the whole message.
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.flatten", "jmo590", this,
          new Object[] {
            new Object[] { MfpConstants.DM_MESSAGE, headerPart.jmfPart, theMessage },
            new Object[] { MfpConstants.DM_MESSAGE, payloadPart.jmfPart, theMessage }
          }
        );
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "flatten failed: " + e);
      throw new MessageEncodeFailedException(e);
    }

    // If debug trace, dump the message & slices before returning
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Flattened JMF Message", debugMsg());
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Message DataSlices: ", SibTr.formatSlices(messageSlices, MfpDiagnostics.getDiagnosticDataLimitInt()));

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "flatten");
    return messageSlices;
  }


  /**
   *  Encode the JMF version and schema ids into a byte array buffer.
   *  The buffer will be used when transmitting over the wire, hardening
   *  into a database and for any other need for 'serialization'
   *
   *  @param buffer  The buffer to write the Ids into.
   *  @param offset  The offset in the buffer at which to start writing the Ids.
   *
   *  @return the number of bytes written to the buffer.
   *
   *  @exception MessageEncodeFailedException is thrown if the ids could not be written.
   */
  private final int encodeIds(byte[] buffer, int offset) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "encodeIds");

    int idOffset = offset;

    // Write the JMF version number and Schema id for the header
    ArrayUtil.writeShort(buffer, idOffset, ((JMFMessage)headerPart.jmfPart).getJMFEncodingVersion());
    idOffset += ArrayUtil.SHORT_SIZE;
    ArrayUtil.writeLong(buffer, idOffset, headerPart.jmfPart.getEncodingSchema().getID());
    idOffset += ArrayUtil.LONG_SIZE;

    // Write the JMF version number and Schema id for the payload, if there is one
    if (payloadPart != null) {
      ArrayUtil.writeShort(buffer, idOffset, ((JMFMessage)payloadPart.jmfPart).getJMFEncodingVersion());
      idOffset += ArrayUtil.SHORT_SIZE;
      ArrayUtil.writeLong(buffer, idOffset, payloadPart.jmfPart.getEncodingSchema().getID());
      idOffset += ArrayUtil.LONG_SIZE;
    }

    // Otherwise we just write zeros to fill up the same space
    else {
      ArrayUtil.writeShort(buffer, idOffset, (short)0);
      idOffset += ArrayUtil.SHORT_SIZE;
      ArrayUtil.writeLong(buffer, idOffset, 0L);
      idOffset += ArrayUtil.LONG_SIZE;

    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "encodeIds", Integer.valueOf(idOffset - offset));
    return idOffset - offset;

  }


  /**
   *  Encode the message part into a byte array buffer.
   *  The buffer will be used for transmitting over the wire, hardening
   *  into a database and any other need for 'serialization'
   *  Locking: The caller MUST have already synchronized on getPartLockArtefact(jsPart)
   *           before calling this method. (This would have to be true in any case,
   *           as the caller must call getEncodedLength to determine the buffer
   *           size needed.
   *
   *  @param jsPart  The message part to be encoded.
   *  @param header  True if this is the header/first message part, otherwise false.
   *  @param buffer  The buffer to encode the part into.
   *  @param offset  The offset in the buffer at which to start writing the encoded message
   *
   *  @return the number of bytes written to the buffer.
   *
   *  @exception MessageEncodeFailedException is thrown if the message part failed to encode.
   */
  private final int encodePartToBuffer(JsMsgPart jsPart, boolean header, byte[] buffer, int offset) throws MessageEncodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "encodePartToBuffer", new Object[]{jsPart, header, buffer, offset});

    JMFMessage msg = (JMFMessage)jsPart.jmfPart;
    int length = 0;
    int newOffset = offset;

    try {

      length = msg.getEncodedLength();

      // If this is a header (or only) part it also needs a 4 byte length field at the start
      // containing the header length
      if (header) {
        ArrayUtil.writeInt(buffer, offset, length);
        newOffset += ArrayUtil.INT_SIZE;
      }

      msg.toByteArray(buffer, newOffset, length);

    }
    catch (Exception e) {
      // Dump the message part, and whatever we've managed to encode so far.
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.encodePartToBuffer", "jmo600", this,
          new Object[] {
            new Object[] { MfpConstants.DM_MESSAGE, msg, theMessage },
            new Object[] { MfpConstants.DM_BUFFER, buffer, Integer.valueOf(offset), Integer.valueOf(length+ArrayUtil.INT_SIZE) }
          }
        );

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "encodePartToBuffer encode failed: " + e);
      throw new MessageEncodeFailedException(e);
    }

    length += (newOffset - offset);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "encodePartToBuffer", Integer.valueOf(length));
    return length;

  }


  /**
   * Encode the header, or only, a message part into a DataSlice for transmitting
   * over the wire, or flattening for persistence.
   * If the message part is already 'assembled' the contents are simply be
   * wrapped in a DataSlice by the JMFMessage & returned.
   * If the message part is not already assembled, the part is encoded into a
   * new byte array which is wrapped by a DataSlice.
   *
   * @param jsPart The message part to be encoded.
   *
   * @return DataSlice The DataSlice containing the encoded message part
   *
   * @exception MessageEncodeFailedException is thrown if the message part failed to encode.
   */
  private final DataSlice encodeHeaderPartToSlice(JsMsgPart jsPart) throws MessageEncodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "encodeHeaderPartToSlice", jsPart);

    // We hope that it is already encoded so we can just get it from JMF.....
    DataSlice slice = ((JMFMessage)jsPart.jmfPart).getAssembledContent();

    // ... if not, then we have to encode it now
    if (slice == null) {
      byte[] buff;

      // We need to ensure noone updates any vital aspects of the message part
      // between the calls to getEncodedLength() and toByteArray()       d364050
      synchronized (getPartLockArtefact(jsPart)) {
        buff = encodePart(jsPart);
      }
      slice = new DataSlice(buff, 0, buff.length);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "encodeHeaderPartToSlice", slice);
    return slice;
  }


  /**
   * Encode a payload part into a DataSlice for transmitting over the wire, or
   * flattening for persistence.
   * If the message part is already 'assembled' the contents are simply be
   * wrapped in a DataSlice by the JMFMessage & returned, unless the message
   * part contains a Beans Payload.
   * If the message part is not already assembled, or contains a Beans payload,
   * the part is encoded into a new byte array which is wrapped by a DataSlice.
   *
   * @param jsPart The message part to be encoded.
   * @param conn   The CommsConnection for the encode, or null if called by flatten
   *
   * @return DataSlice The DataSlice containing the encoded message part
   *
   * @exception MessageEncodeFailedException is thrown if the message part failed to encode.
   */
  private final DataSlice encodePayloadPartToSlice(JsMsgPart jsPart, CommsConnection conn) throws MessageEncodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "encodePayloadPartToSlice", new Object[]{jsPart, conn});

    boolean beans = false;
    DataSlice slice = null;

    // For a payload which isn't Beans, we also hope that it is already encoded so we can
    // just get it from JMF. A Beans payload part always needs re-encoding as it may
    // be in SOAP where JMF is required, or vice versa.
    // Figuring it out is a bit messy:
    //    - if this is a payload part, the message must be a JsMessage
    //    - the ProducerType will be API for a Beans message, whether or not it is wrapped by JMS
    //    - but we'll still have to look at the format field to see if is Beans
    if (  (((JsMessage)theMessage).getProducerType() != ProducerType.API)
       || payloadPart.getField(JsPayloadAccess.FORMAT) == null // XMS might not set the format
       || !(((String)payloadPart.getField(JsPayloadAccess.FORMAT)).startsWith("Bean:"))
       ) {
      slice = ((JMFMessage)jsPart.jmfPart).getAssembledContent();
    }

    // If it is beans, leave slice == null and set a flag.
    else {
      beans = true;
    }

    // If we haven't already got some existing content, we have to encode it now.
    if (slice == null) {

      // We need to put the whole lot in a try/finally block to ensure we clear the
      // thread local data even if something goes wrong.
      try {

        // We need to ensure noone updates any vital aspects of the message part
        // during the call to encodePart()                                 d364050
        // In the case of a Beans payload, we also need to ensure no-one else
        // encodes the message after we unassemble it & before we encode it.
        synchronized (getPartLockArtefact(jsPart)) {

          // If we have a Beans message payload, so we need to set the
          // thread's partner level from the value in the CommsConnection, if present,
          // and unassemble the payload data to ensure that it does get re-encoded in the next step.
          if (beans) {

            // Set the thread's partner level from the value in the Connection MetaData, if present
            if (conn != null) MfpThreadDataImpl.setPartnerLevel(conn.getMetaData().getProtocolVersion());

            try {
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "unassembling Beans message");
              ((JMFNativePart)jsPart.getField(JsPayloadAccess.PAYLOAD_DATA)).unassemble();
            }
            catch (JMFException e) {
              // This shouldn't be possible. If we get an Exception something disastrous
              // must have happened so FFDC it & throw it on wrappered.
              // Dump the message part to give us some clue what's going on.
              FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.encodePayloadPartToSlice", "jmo620", this,
                    new Object[] { MfpConstants.DM_MESSAGE, jsPart.jmfPart, theMessage }
              );

              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "encodePayloadPartToSlice unassemble failed: " + e);
              throw new MessageEncodeFailedException(e);
            }
          }

          // Whatever sort of message we have, we now need to encode it.
          byte[] buff = encodePart(jsPart);
          slice = new DataSlice(buff, 0, buff.length);

        } // end of synchronized block

      }
      // We must ALWAYS reset the thread local partnerLevel once a payload encode
      // is no longer in progress, otherwise a future flatten on this thread could
      // encode to the wrong level.
      // (It doesn't matter if we didn't actually set it earlier - clearing it is harmless)
      finally {
        MfpThreadDataImpl.clearPartnerLevel();
      }

    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "encodePayloadPartToSlice", slice);
    return slice;
  }


  /**
   *  Return a byte array containing the message part encoded for transmission.
   *  Locking: The caller MUST have already synchronized on getPartLockArtefact(jsPart)
   *           before calling this method.
   *
   *  @param jmfPart The JMF message part to be encoded.
   *
   *  @return a byte array containing the encoded message part.
   *
   *  @exception MessageEncodeFailedException is thrown if the message part failed to encode.
   */
  private final byte[] encodePart(JsMsgPart jsPart) throws MessageEncodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "encodePart", jsPart);

    JMFMessage msg = (JMFMessage)jsPart.jmfPart;
    byte[] buffer = null;
    int length = 0;

    try {
        length = msg.getEncodedLength();
        buffer = msg.toByteArray(length);
    }
    catch (Exception e) {
      // Dump the message part, and whatever we've managed to encode so far.
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.encodePart", "jmo630", this,
          new Object[] {
            new Object[] { MfpConstants.DM_MESSAGE, msg, theMessage },
            new Object[] { MfpConstants.DM_BUFFER, buffer, Integer.valueOf(0), Integer.valueOf(length) }
          }
        );
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "encode failed: " + e);
      throw new MessageEncodeFailedException(e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "encodePart", Integer.valueOf(length));
    return buffer;

  }


  /**
   * Get a list of the JMF schemas needed to decode this message
   *
   * @return A list of JMFSchemas
   */
  JMFSchema[] getEncodingSchemas() throws MessageEncodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getEncodingSchemas");
    JMFSchema[] result;
    try {
      JMFSchema[] result1 = ((JMFMessage)headerPart.jmfPart).getSchemata();
      JMFSchema[] result2 = null;
      int resultSize = result1.length;
      if (payloadPart != null) {
        result2 = ((JMFMessage)payloadPart.jmfPart).getSchemata();
        resultSize += result2.length;
      }
      result = new JMFSchema[resultSize];
      System.arraycopy(result1, 0, result, 0, result1.length);
      if (payloadPart != null) {
        System.arraycopy(result2, 0, result, result1.length, result2.length);
      }
    } catch (JMFException e) {
      // Dump whatever we can of both message parts
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.getEncodingSchemas", "jmo700", this,
          new Object[] {
            new Object[] { MfpConstants.DM_MESSAGE, headerPart.jmfPart, theMessage },
            new Object[] { MfpConstants.DM_MESSAGE, payloadPart.jmfPart, theMessage }
          }
        );
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "getEncodingSchemas failed: " + e);
      throw new MessageEncodeFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getEncodingSchemas");
    return result;
  }


  /**
   * Return a copy of this JsMsgObject.
   *
   * The copy can be considered a true and independant copy of the original, but for
   * performance reasons it may start by sharing data with the original and only
   * copying if (or when) updates are made.
   *
   * @return JsMsgObject A JMO which is a copy of this.
   *
   * @exception MessageCopyFailedException will be thrown if the copy can not be made.
   */
  JsMsgObject getCopy() throws MessageCopyFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getCopy");

    JsMsgObject newJmo = null;


    try {

      // We need to lock the whole of the copy with the getHdr2, getApi & getPayload
      // methods on the owning message, otherwise someone could reinstate the partcaches
      // between clearing them and creating the new parts.               d352642
      synchronized(theMessage) {

        // Ensure any cached message data is written back and cached message parts are
        // invalidated.
        theMessage.updateDataFields(MfpConstants.UDF_GET_COPY);
        theMessage.clearPartCaches();

        // Clone this JMO and insert a copy of the underlying JMF message.  It is the JMF
        // that handles all the "lazy" copying.
        newJmo = new JsMsgObject(null);
        newJmo.headerPart = new JsMsgPart(((JMFMessage)headerPart.jmfPart).copy());
        newJmo.payloadPart = new JsMsgPart(((JMFMessage)payloadPart.jmfPart).copy());
      }

    } catch (MessageDecodeFailedException e) {
      // Dump whatever we can of both message parts
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.getCopy", "jmo800", this,
          new Object[] {
            new Object[] { MfpConstants.DM_MESSAGE, headerPart.jmfPart, theMessage },
            new Object[] { MfpConstants.DM_MESSAGE, payloadPart.jmfPart, theMessage }
          }
        );
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "copy failed: " + e);
      throw new MessageCopyFailedException(e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getCopy", newJmo);
    return newJmo;
  }

  /**
   * Transcribe this jmo in to pure JMF
   *
   * @return a pure JMF copy of this jmo
   * @throws MessageCopyFailedException
   */
  JsMsgObject transcribeToJmf() throws MessageCopyFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "transcribeToJmf");
    JsMsgObject newJmo;
    JMFEncapsulation originalApiEncap = null;
    JMFEncapsulation originalPayloadDataPart = null;

    // First, figure out if there is anything to transcribe.......
    if (headerPart.getIntField(JsHdrAccess.API) == JsHdrAccess.IS_API_DATA) {
      //get hold of the original api native part
      JsMsgPart originalApiPart = getPart(JsHdrAccess.API_DATA, JsApiAccess.schema);
      if (originalApiPart.jmfPart instanceof JMFEncapsulation) {
        JMFNativePart originalNativeApiPart = originalApiPart.jmfPart;
        originalApiEncap = (JMFEncapsulation)originalNativeApiPart;
      }
    }
    if (payloadPart != null) {
      if(payloadPart.getIntField(JsPayloadAccess.PAYLOAD) == JsPayloadAccess.IS_PAYLOAD_DATA) {
        //get hold of the original payload native part
        Object originalPayloadObject = payloadPart.getField(JsPayloadAccess.PAYLOAD_DATA);
        //check if we really need to copy ... if it is already JMF then we don't need to do anything
        if(originalPayloadObject instanceof JMFEncapsulation) {
          originalPayloadDataPart = (JMFEncapsulation)originalPayloadObject;
        }
      }
    }

    // If there is nothing needing to be transcribed, we just return this
    if ((originalApiEncap == null) && (originalPayloadDataPart == null)) {
      newJmo = this;
    }

    // Otherwise, we have a part which needs to be transcribed, so carry on.....
    else {

      // Take a simple copy of the JMO first
      newJmo = getCopy();

      // Next try to copy the api headers if they are in an Encapsulation
      if (originalApiEncap != null) {

        // Create a new native part with the right schema
        JMFNativePart newNativeApiPart = JSRegistry.instance.newNativePart(JsApiAccess.schema);

        // Use the JMFEncapsulation.transcribe method to copy the native parts
        try {
          originalApiEncap.transcribe(newNativeApiPart,true);
          // Now we need to set the new Native API Part into the message, BUT
          // it is unassembled & the rest of the header part may be assembled.
          // Therefore, we must first unassemble the header part.
          ((JMFMessage)newJmo.headerPart.jmfPart).unassemble();
          newJmo.setField(JsHdrAccess.API_DATA, newNativeApiPart);
        }
        catch (JMFSchemaViolationException e) {
          FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.transcribeToJmf", "jmo880", this,
            new Object[] { MfpConstants.DM_MESSAGE, headerPart.jmfPart, theMessage });
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "transcribe headers failed: " + e);
          throw new MessageCopyFailedException(e);
        }
        catch (JMFModelNotImplementedException e) {
          FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.transcribeToJmf", "jmo881", this,
            new Object[] { MfpConstants.DM_MESSAGE, headerPart.jmfPart, theMessage });
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "transcribe headers failed: " + e);
          throw new MessageCopyFailedException(e);
        }
        catch (JMFUninitializedAccessException e) {
          FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.transcribeToJmf", "jmo882", this,
            new Object[] { MfpConstants.DM_MESSAGE, headerPart.jmfPart, theMessage });
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "transcribe headers failed: " + e);
          throw new MessageCopyFailedException(e);
        }
        catch (JMFMessageCorruptionException e) {
          FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.transcribeToJmf", "jmo883", this,
            new Object[] { MfpConstants.DM_MESSAGE, headerPart.jmfPart, theMessage });
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "transcribe headers failed: " + e);
          throw new MessageCopyFailedException(e);
        }
      }

      // Now copy the payload ... if it exists & needs to be transcribed
      if (originalPayloadDataPart != null) {

        // Create a new native part with the right schema
        JMFSchema schema = originalPayloadDataPart.getNativePart().getEncodingSchema();
        JMFNativePart newNativePayloadPart = JSRegistry.instance.newNativePart(schema);

        //use the JMFEncapsulation.transcribe method to copy the native parts
        try {
          originalPayloadDataPart.transcribe(newNativePayloadPart, true);

          // Now we need to set the new Native Payload Data Part into the message, BUT
          // it is unassembled & the rest of the payload part may be assembled.
          // Therefore, we must first unassemble the payload part.
          ((JMFMessage)newJmo.getPayloadPart().jmfPart).unassemble();
          newJmo.getPayloadPart().setField(JsPayloadAccess.PAYLOAD_DATA, newNativePayloadPart);

        }
        catch (JMFSchemaViolationException e) {
          FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.transcribeToJmf", "jmo890", this,
              new Object[] {
                new Object[] { MfpConstants.DM_MESSAGE, headerPart.jmfPart, theMessage },
                new Object[] { MfpConstants.DM_MESSAGE, payloadPart.jmfPart, theMessage }
              }
            );
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "transcribe payload failed: " + e);
          throw new MessageCopyFailedException(e);
        }
        catch (JMFModelNotImplementedException e) {
          FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.transcribeToJmf", "jmo891", this,
              new Object[] {
                new Object[] { MfpConstants.DM_MESSAGE, headerPart.jmfPart, theMessage },
                new Object[] { MfpConstants.DM_MESSAGE, payloadPart.jmfPart, theMessage }
              }
            );
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "transcribe payload failed: " + e);
          throw new MessageCopyFailedException(e);
        }
        catch (JMFUninitializedAccessException e) {
          FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.transcribeToJmf", "jmo892", this,
              new Object[] {
                new Object[] { MfpConstants.DM_MESSAGE, headerPart.jmfPart, theMessage },
                new Object[] { MfpConstants.DM_MESSAGE, payloadPart.jmfPart, theMessage }
              }
            );
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "transcribe payload failed: " + e);
          throw new MessageCopyFailedException(e);
        }
        catch (JMFMessageCorruptionException e) {
          FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.transcribeToJmf", "jmo893", this,
              new Object[] {
                new Object[] { MfpConstants.DM_MESSAGE, headerPart.jmfPart, theMessage },
                new Object[] { MfpConstants.DM_MESSAGE, payloadPart.jmfPart, theMessage }
              }
            );
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "transcribe payload failed: " + e);
          throw new MessageCopyFailedException(e);
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "transcribeToJmf", newJmo);
    return newJmo;
  }

  /**
   * Return the original encoded message length.
   *
   * @return The original lenght of the message.
   * This will return 0 if the message was not originally constructed from an
   * inbound data stream.
   */
  int getOriginalLength() {
    return originalLength;
  }


  /**
   * Return the JsMsgPart representing the header of the message.
   *
   * @return The JsMsgPart representing the header of the message.
   */
  final JsMsgPart getHeaderPart() {
    return headerPart;
  }


  /**
   * Return the JsMsgPart representing the payload of the message.
   *
   * @return The JsMsgPart representing the payload of the message.
   */
  final JsMsgPart getPayloadPart() {
    return payloadPart;
  }


  /**
   * Return the JMF Message Lock Artefact for the given JsMsgPart
   *
   * @return The JMF Message Lock Artefact for the given JsMsgPart
   */
  private Object getPartLockArtefact(JsMsgPart part) {
    return ((JMFMessage)part.jmfPart).getMessageLockArtefact();
  }


  /**
   * We need to check if the receiver has all the necessary schema definitions to be able
   * to decode this message and pre-send any that are missing.
   *
   * @param conn  The Comms Connection object which represents the Receiver
   *
   * @exception   Any Exception needs to be thrown on to the caller who will wrap it appropriately
   */
  private final void ensureReceiverHasSchemata(CommsConnection conn) throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "ensureReceiverHasSchemata", conn);

    // We need to check if the receiver has all the necessary schema definitions to be able
    // to decode this message and pre-send any that are missing.
    if (conn != null) {
      SchemaManager.sendSchemas(conn, ((JMFMessage)headerPart.jmfPart).getSchemata());
      if (payloadPart != null) {
        SchemaManager.sendSchemas(conn, ((JMFMessage)payloadPart.jmfPart).getSchemata());
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "ensureReceiverHasSchemata");
  }


  /*
   *  Wrapper methods for the get/set of fields in the Header JMF Message Part
   */
  void setField(int accessor, Object value) {
    headerPart.setField(accessor, value);
  }

  Object getField(int accessor) {
    return headerPart.getField(accessor);
  }

  void setIntField(int accessor, int value) {
    headerPart.setIntField(accessor, value);
  }

  int getIntField(int accessor) {
    return headerPart.getIntField(accessor);
  }

  void setLongField(int accessor, long value) {
    headerPart.setLongField(accessor, value);
  }

  long getLongField(int accessor) {
    return headerPart.getLongField(accessor);
  }

  void setBooleanField(int accessor, boolean value) {
    headerPart.setBooleanField(accessor, value);
  }

  boolean getBooleanField(int accessor) {
    return headerPart.getBooleanField(accessor);
  }

  void setChoiceField(int accessor, int variant) {
    headerPart.setChoiceField(accessor, variant);
  }

  int getChoiceField(int accessor) {
    return headerPart.getChoiceField(accessor);
  }

  void setPart(int accessor, JMFSchema schema) {
    headerPart.setPart(accessor, schema);
  }

  JsMsgPart getPart(int accessor, JMFSchema schema) {
    JsMsgPart part = headerPart.getPart(accessor, schema);
    if (part == null) {
      /* null implies the message has been corrupted or the message is        */
      /* unexpectedly a Control Message. FFDC it so we can see what we have.  */
      /* FFDC needs an Exception for the call so create one.                  */
      MessageDecodeFailedException e = new MessageDecodeFailedException();
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsMsgObject.getPart", "jmo900", this,
        new Object[] { MfpConstants.DM_MESSAGE, headerPart, theMessage });
    }
    return part;
  }

  /*
   *  Debug methods
   */
  // For debug only ... Write out the JMF message details, excluding the user data
  // in the payload.
  final String debugMsg() {
    StringBuffer result;
    result = new StringBuffer(JSFormatter.format((JMFMessage)headerPart.jmfPart));
    if (payloadPart != null) {
      result.append(JSFormatter.formatWithoutPayloadData((JMFMessage)payloadPart.jmfPart));
    }
    return result.toString();
  }

  // For debug only ... Write out the JMF message details for the header part
  final String debugHeaderPart() {
    return JSFormatter.format((JMFMessage)headerPart.jmfPart);
  }

  // For debug only ... Write a schema name(id) details
  final String debugSchema(JMFSchema schema) {
    if (schema != null)
      return schema.getName() + "(" + debugId(schema.getID()) + ")";
    else
      return "<null>";
  }

  // For debug only ... Write a schema ID in hex
  final String debugId(long id) {
    byte[] buf = new byte[8];
    ArrayUtil.writeLong(buf, 0, id);
    return HexUtil.toString(buf);
  }
}
